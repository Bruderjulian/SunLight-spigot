# Reports Module — Feature Spec

Module ID `reports` · optional, default `Enabled: false` · no NMS changes · Paper/Spigot only.

Supersedes `plan.md` §5. `plan.md` predates the current tree: `socials` and `nametags` are now
fully implemented and registered, and `playtime` is a config-only scaffold with no module class.
**Bans is the template**, and that stands.

---

## 1. Purpose

Players report other players for rule violations. Staff triage reports from a GUI, claim them,
teleport to the offender, add notes, and conclude them. When a report turns out to be justified —
either because a staff member marked it so, or because the reported player was punished — the
reporter is rewarded. Reports replicate across servers in a MySQL network so staff on any server
see and work the same queue.

### Goals

- Player-facing submission that is hard to spam.
- Staff queue that is fast to work through: filter, claim, inspect, teleport, note, conclude.
- Every state change leaves an audit trail.
- Cross-server visibility with no local-server assumption.
- Rewards that pay out at most once, on either success path.

### Non-goals

- No automatic punishment. Reports never punish anyone; the Bans module remains the only
  punishment entry point.
- No report *threading* (one report, many comments from other players).
- No web panel or external moderation API.
- No anon/alt-account-safe reporting (a report always records the real UUID behind the profile).

---

## 2. Concepts

| Term | Meaning |
| --- | --- |
| **Report** | One submission: reporter, target, category, details, status, timestamps. |
| **Category** | Config-defined reason id (`griefing`, `cheating`, …) the reporter picks from. |
| **Details** | Free-text message the reporter types. Required by default. |
| **Conclusion** | Terminal state: `RESOLVED` or `DENIED`. |
| **Reward** | Console commands paid to the reporter when a report is concluded as justified. |

### 2.1 Statuses

```
OPEN ──claim──> CLAIMED ──resolve──> RESOLVED   (justified → reward eligible)
  │                 │             └─deny──> DENIED
  │                 └─────────────────────────> RESOLVED | DENIED   (staff may skip claiming)
  └────────────────────────────────────────────> RESOLVED | DENIED
```

`RESOLVED` and `DENIED` are the only terminal states. Legal transitions are enforced in one place
(`ReportsModule#conclude`) so the GUI, commands, and the Bans hook cannot diverge:

- `OPEN → CLAIMED` — `claim`. Claiming a claimed report by someone else is rejected; claiming your
  own is a no-op success.
- `OPEN|CLAIMED → RESOLVED|DENIED` — `conclude`.
- Any transition out of a terminal state is rejected.

Releasing a claim (`CLAIMED → OPEN`) is a staff convenience for when someone goes AFK; it is
exposed as shift-right-click in the GUI and `/reportrelease <id>`. It appends a note.

### 2.2 Justified vs. unjustified

A report is **justified** when it ends `RESOLVED`, and **unjustified** when it ends `DENIED`. Only
justified reports pay a reward. Justified-ness is recorded, never inferred, so that the reward
decision is auditable after the fact.

---

## 3. Player flow

### 3.1 Submission

```
/report <player> <category> <details...>
```

Aliases: `rep`. Player-only. Requires `sunlight.reports.command.report`.

Argument resolution, in order — the first failure aborts with a message and no state change:

1. **Self-report** — target equals the sender (case-insensitive) → `ERROR_SELF_REPORT`.
2. **Target name** — must be a valid player name pattern (`Strings.varStyle`, same check
   `WarpsModule#create` uses). Length capped at 32. The target **does not need to have joined
   before** and does not need to be online: the name is the canonical key. If a UUID is known for
   the name (`userManager.getRepository().getAssociatedId(name)`), it is stored too.
3. **Category** — must be a configured category id. Unknown → `ERROR_UNKNOWN_CATEGORY` with
   suggestions. Hidden categories (no permission) are not suggested and are rejected.
4. **Details** — length must be within `Min-Length`..`Max-Length` after trimming. Below minimum →
   `ERROR_DETAILS_TOO_SHORT` (with the minimum stated). Above maximum → `ERROR_DETAILS_TOO_LONG`.
5. **Cooldown** — see §3.2.
6. **Open-report cap** — see §3.3.
7. **Duplicate lock** — see §3.4.
8. **Blacklist** — sender or target on the configured world/permission blacklist → rejected.

On success:

- The report row is inserted **synchronously** (see §6.4) and added to the in-memory repository.
- `PlayerReportEvent` is fired (§8.1). If cancelled, the insert is rolled back by deleting the row
  and the sender is told the report was rejected.
- The reporter gets `REPORT_CREATED` including the report id, so they can quote it in chat.
- Staff holding `sunlight.reports.notify` receive `NOTIFY_STAFF` (broadcast, configurable sound)
  with a clickable `/reports` hint.
- If `Notify-Target.Enabled` is on, the target gets `NOTIFY_TARGET`. Off by default: it leaks
  who reported them. When on, the message deliberately omits the reporter's identity and the
  details — it says only that the report exists.

### 3.2 Cooldown (per-reporter)

Stored in the existing `SunUser` command-cooldown map under
`CommandKey("reports", "submit")`, so it is per-player, survives relog, and reuses the persistence
already in place. Value from `Reports.Cooldown.Submit-Seconds`; `0` disables.

Written via `TimeUtil.createFutureTimestamp(seconds)`; the message is our own lang entry showing
`TimeFormats.formatDuration(expireDate, LITERAL)` — not the generic command-cooldown text,
because the player deserves to know it is a *report* cooldown.

Staff bypass: `EconomyUtils.hasCooldownBypass(player, this)` resolves
`sunlight.bypass.cooldown` **or** `sunlight.reports.bypass.cooldown`. Both nodes are declared in
`ReportsPerms` so they are actually grantable.

### 3.3 Open-report cap

`Reports.Limits.Max-Open-Per-Player` (default `3`, `-1` = unlimited). Counts reports the sender
filed whose status is `OPEN` **or** `CLAIMED` — a claimed report is still being worked and still
occupies the reporter's quota. Exceeding → `ERROR_TOO_MANY_OPEN`.

This is the guard that actually bounds abuse volume; the cooldown only rate-limits it.

### 3.4 Duplicate lock

If the sender already has a report against the same target with status `OPEN` or `CLAIMED`, the
submission is rejected with `ERROR_ALREADY_REPORTED`, which includes the age of the existing
report. Staff with `sunlight.reports.command.report.duplicate` may override.

Matching is by target UUID when both are known, otherwise by lower-cased target name.

### 3.5 Reporter status view

`/reportstatus` (alias `rstatus`, player-only, perm `sunlight.reports.command.report.status`)
opens the same `ReportsMenu` the staff use, but locked to the `MINE` filter with no filter
switching and no staff actions. The reporter sees status, category, their own details, the
timestamps, and the conclusion — but **not the staff note trail** (notes may contain internal
discussion; `sunlight.reports.notes.visible-to-reporter` decides, default `false`).

---

## 4. Staff flow

### 4.1 The queue

`/reports` opens `ReportsMenu`. Filters, one MenuItem per state, all persisted per-viewer in the
menu's bound `Data` record rather than in config:

| Filter | Shows |
| --- | --- |
| `OPEN` | Unclaimed, unresolved. Default view. |
| `CLAIMED` | Claimed by someone, unresolved. |
| `MINE` | Reports the viewer filed. Non-staff see only their own. |
| `ALL` | Everything, including concluded. |
| `TARGET` | Reports against a specific player — opened by shift-right-clicking a player head in any other filter. |

The target of each row is rendered as a skull via `DisplayModifiers.VIEWER_SKULL`; the reporter
and target names are clickable lore. The `targetId` column is nullable, so rows for never-seen
names render a fallback icon instead of a skull.

Sorting is newest-first, done in Java. **NightCore's `SelectStatement.Builder` has no `orderBy`
and no `limit`** — every select returns the whole table and pagination is in-memory. Reports are
low-volume so this is fine, but it is the reason the repository is loaded eagerly at boot (§6.3)
rather than queried per page.

### 4.2 The report view

Clicking a row opens `ReportViewMenu`, bound to the `Report`. Contents:

- Head: reporter skull + name, target skull + name, category icon.
- Lore: status, category, details (wrapped), age, last update, note count, whether a reward was
  paid.
- Buttons: `Claim`, `Conclude` (opens `ReportOutcomeDialog`), `Teleport`, `Add Note` (opens
  `ReportNoteDialog`), `Release Claim` (shift-right), `Delete` (requires
  `sunlight.reports.command.report.delete`, drop-click to confirm).
- Concluded reports hide `Claim` and `Teleport` and show the outcome + reward status.

### 4.3 Claim

`/reportclaim <id>` and the GUI `Claim` button both call `ReportsModule#claim(report, staff)`.

Guards: not terminal, not already claimed by a *different* staff member, staff has
`sunlight.reports.command.report.claim`. The claimant is stored as `staffId`/`staffName` and the
note trail gets a system entry `Reports.System.CLAIMED`.

Claim notifies the previous claimant (`REPORT_CLAIM_TAKEN`) so two staff do not duplicate work
across servers — this is where `addTableSync` earns its keep.

### 4.4 Teleport to target

`/reporttp <id>` and the GUI `Teleport` button teleport the staff member to the reported player.

- Only for online targets. Offline → `ERROR_TARGET_OFFLINE`.
- Guarded by `sunlight.reports.command.report.teleport`; a staff member may always teleport to
  themselves (idempotent no-op).
- Routed through `TeleportManager` with `TeleportContext`, so **warmups, world redirects, and
  teleport safety all apply** — reports must not become a bypass for those. Explicitly passes
  `TeleportFlag.BYPASS_WARMUP` only when the caller also holds
  `sunlight.reports.command.report.teleport.bypass-warmup`.
- Target's last known location is stored on the report (`lastKnownWorld`, `lastKnownX/Y/Z`) at
  submission time so a staff member can be told where the player was last seen, even if they have
  since logged out. Coordinates are informational only; teleport always goes to the live player.
- Teleporting records a system note.

### 4.5 Notes

`ReportNoteDialog` is a NightCore dialog with a single `DialogInputs.text` capped at
`Reports.Notes.Max-Length`. **NightCore dialogs have no declarative validator**, so the check
happens in `handleResponse`: empty or over-length input is rejected with a prefixed error and the
menu refreshes without writing.

Each note is a row: author UUID + name, text, timestamp. Notes are append-only — there is no edit
or delete, deliberately, because the trail is the audit record.

### 4.6 Conclude

`/reportresolve <id> [note...]` and `/reportdeny <id> [note...]`; the GUI's `Conclude` button
opens `ReportOutcomeDialog`, which is a single-option input for the outcome plus an optional note.

The optional note is written as a staff note before the status change, in that order, so a failed
write cannot leave a concluded report with a silent note.

If the outcome is `RESOLVED` and a reward is configured and unpaid, the reward is claimed (§5)
after the status write succeeds. `PlayerReportResolvedEvent` is fired.

### 4.7 Delete

`/reportdelete <id>` and drop-click. Requires
`sunlight.reports.command.report.delete`. Deletes the report row **and** its notes, and fires
`PlayerReportDeleteEvent`. This is a purge action, not a moderation action — a wrongly concluded
report should be denied or resolved differently, not deleted, so the audit trail survives.

---

## 5. Rewards

### 5.1 Configuration

```yaml
Reports:
  Rewards:
    Enabled: true
    # Console commands run when a report is concluded as RESOLVED (justified).
    Commands:
      - 'eco give %reporter_name% 500'
      - 'say %reporter_name% was rewarded for a helpful report!'
    Notify-Rewarded: true
    Minimum-Reporter-Playtime-Hours: 0   # 0 = no requirement
```

Commands-only, no money field. This matches the existing reward idiom in the repo
(`PlaytimeSettings.Rewards.*`, `BansSettings.warnAutoCommands`) and avoids depending on a Vault
*deposit* call, which `EconomyUtils` does not expose — it only has `withdraw`.

Placeholders available in reward commands: `%reporter%`, `%reporter_name%`, `%target%`,
`%target_name%`, `%category%`, `%details%`, `%report_id%`, `%date%`, plus `SLPlaceholders.PLAYER_NAME`.

Dispatch copies `BansModule:657-666` exactly: build a `PlaceholderContext` from
`report.placeholders()`, `commands.replaceAll(context::apply)` **per line** (so a detail
containing a line break cannot split one command into two), then on the next tick
`plugin.getServer().dispatchCommand(console, command)`.

If the reporter is offline, the commands still run as console — so reward commands must be
offline-safe (`eco give` and `say` are; `give` is not). This is documented in the config comment.

### 5.2 Two success paths

A report is rewarded when it concludes `RESOLVED`, and `RESOLVED` is reached two ways:

1. **Staff-driven** — a staff member concludes it as `RESOLVED` (§4.6). Default path.
2. **Punishment-driven** — the reported player is punished by the Bans module while the report is
   `OPEN` or `CLAIMED`. `ReportsModule` listens for a new `PlayerPunishEvent` (see §8.2) and
   concludes every matching non-terminal report as `RESOLVED` with a system note
   `Reports.System.PUNISHED`.

Path 2 is the reason we do not require a claim: if staff punish a cheater directly, the queue
clears itself and the honest reporter is paid without anyone doing paperwork.

**Scope limit:** path 2 matches on **any** punishment type, including `WARN`. A `WARN` is a
punishment but a weak signal. `Reports.Rewards.Require-Punishment-Type` (default `BAN,MUTE`) lets
a server exclude `WARN` from triggering rewards while still using it to close the report.
Off by default means a `WARN` pays out.

### 5.3 Paying at most once

Two paths can converge on the same report, and in a network two servers can converge on the same
report simultaneously. The claim is therefore guarded at three levels:

1. **In-memory CAS** — `Report#claimReward()` is an `AtomicBoolean` on the model; only the thread
   that flips `false → true` proceeds. Handles same-server races.
2. **DB read-back** — the winner, on an async task, re-reads the row with
   `selectFirst(table, SELECT_REWARD_CLAIM, Wheres.where(REWARDED, EQUALS, o -> false).and(id))`.
   Empty means someone else already paid; abort silently. Handles cross-server races down to the
   sync interval's resolution time.
3. **Write before pay** — `rewarded = true` and the final status are written **before** the
   commands are dispatched, so a crash between the two loses the reward rather than double-paying.

**Known limitation, documented in the config comment:** `AbstractDatabaseManager#update` returns
`void` — NightCore exposes no affected-row count, so a true `UPDATE … WHERE rewarded = 0` CAS is
impossible. Levels 1–2 give at-most-once for every realistic case; a genuine simultaneous resolve
on two servers within the same sync window can in principle pay twice. Closing this fully requires
a raw `executeStatement` plus a follow-up read, which trades the guarantee for a busy-wait and is
not worth it. The window is `Database.Sync-Interval`; set it low on a reporting network.

### 5.4 Reporter eligibility

`Minimum-Reporter-Playtime-Hours` guards against alt-boosting. It reads the playtime module's
`TOTAL` user property when that module is enabled, and is skipped (treated as satisfied) when it
is not — reports must not require playtime to exist. `ReportsProvider#getTotalPlaytimeMs(UUID)`
is the accessor for this, so the dependency stays one-directional and soft.

---

## 6. Data model

### 6.1 Table: `<prefix>_reports`

Prefix from `Reports.Data.Table-Prefix`, default `sunlight_reports`, giving
`sunlight_reports_reports`. Declared in `ReportsDataManager` as `static final Column` so
`ReportsQueries` can reference them, exactly as `BansDataManager` does.

| Java constant | SQL | Type | Notes |
| --- | --- | --- | --- |
| `COLUMN_REPORT_ID` | `reportId` | UUID | `primaryKey()` |
| `COLUMN_REPORTER_ID` | `reporterId` | UUID | not null — the sender is always a real player |
| `COLUMN_REPORTER_NAME` | `reporterName` | `varchar(32)` | updated on login, like `PlayerPunishment#updateName` |
| `COLUMN_TARGET_ID` | `targetId` | UUID | **nullable** — target may never have joined |
| `COLUMN_TARGET_NAME` | `targetName` | `varchar(32)` | not null; the canonical key when `targetId` is null |
| `COLUMN_CATEGORY` | `category` | `varchar(32)` | category id |
| `COLUMN_DETAILS` | `details` | MEDIUMTEXT | free text |
| `COLUMN_STATUS` | `status` | `varchar(16)` | `ReportStatus.name()` |
| `COLUMN_STAFF_ID` | `staffId` | UUID | nullable — null unless claimed |
| `COLUMN_STAFF_NAME` | `staffName` | `varchar(64)` | nullable |
| `COLUMN_CREATE_DATE` | `createDate` | `BIGINT` | epoch ms |
| `COLUMN_UPDATE_DATE` | `updateDate` | `BIGINT` | epoch ms |
| `COLUMN_REWARDED` | `rewarded` | `BOOLEAN` | reward claim guard |
| `COLUMN_LAST_WORLD` | `lastWorld` | `varchar(64)` | nullable — last known world of target |
| `COLUMN_LAST_X` / `_Y` / `_Z` | `lastX`,`lastY`,`lastZ` | `DOUBLE` | nullable — last known location |

### 6.2 Table: `<prefix>_report_notes`

| Java constant | SQL | Type | Notes |
| --- | --- | --- | --- |
| `COLUMN_NOTE_ID` | `noteId` | UUID | `primaryKey()` |
| `COLUMN_REPORT_ID` | `reportId` | UUID | indexed in practice; no FK (NightCore FKs are advisory) |
| `COLUMN_AUTHOR_ID` | `authorId` | UUID | null for system notes |
| `COLUMN_AUTHOR_NAME` | `authorName` | `varchar(64)` | `"CONSOLE"` for system notes |
| `COLUMN_NOTE` | `note` | MEDIUMTEXT | |
| `COLUMN_DATE` | `date` | `BIGINT` | epoch ms |

`init(prefix)` builds both tables, `createTable`s them (which also `addColumn`s anything missing, so
schema evolution is automatic), then `purgeOldEntries()`, then registers two `addTableSync`
consumers.

Notes are a separate table rather than a JSON column because they are unbounded and append-only;
a JSON blob would rewrite the whole report row on every note and grow past MySQL's comfortable row
size.

### 6.3 Repository

`ReportRepository` mirrors `PunishmentRepository`: `ConcurrentHashMap` indexes guarded by
`synchronized` mutators, because `addTableSync` delivers rows from the DB sync thread while the
main thread reads and writes.

```
Map<UUID, Report>                      byId
Map<UUID, Set<UUID>>                   byReporter   (reporterId -> reportIds)
Map<String, Set<UUID>>                 byTargetName (lowercased -> reportIds)
Map<UUID, Set<UUID>>                   byTargetId   (only for rows with a known target)
Map<ReportStatus, Set<UUID>>           byStatus
```

Query methods: `getReport(UUID)`, `getReports()`, `getReports(ReportStatus)`,
`getOpenReports()` (`OPEN ∪ CLAIMED`), `getReportsByStatus(ReportStatus)`,
`getReporterReports(UUID reporterId)`, `getReporterOpenReports(UUID reporterId)`,
`getReportsByTarget(UUID targetId, String targetName)`, `getCount(ReportStatus)`.

`add`/`remove` maintain every index in one place so they cannot drift. Because there is no
`orderBy` in the statement API, every accessor returns an insertion-independent set and the menu
does the sorting.

`loadData()` runs async at boot, fills the repository, then `plugin.runTask(...)` to notify
anything that needs a live player (there is no kick-on-load equivalent here, so this is a no-op
hook kept for symmetry with `BansModule#loadData`).

### 6.4 Write strategy: write-through, no dirty flags

`BansModule` uses dirty flags plus a save-interval task because punishments are numerous and
mutated constantly. Reports are neither: a report is written once and updated a handful of times
in its life. So every mutation does an immediate `dataHandler.insert` / `dataHandler.update`, both
of which are synchronous JDBC on the calling thread.

Consequences, all of them good here:

- A report survives a crash immediately, with no interval window of loss.
- No `dataLoaded` save task, and no "unsaved reports" failure mode.
- `ReportsModule#isDataLoaded()` still guards command execution, matching the `ERROR_DATA_NOT_LOADED`
  message Bans uses — during the boot load, submissions are rejected rather than racing the
  duplicate/cap checks against an incomplete repository.

Cost: a handful of writes per report, on the main thread. A report submission is a rare,
player-initiated action, so this is not a tick-budget concern. It must **not** become the pattern
for high-volume modules.

### 6.5 Purge

`purgeOldEntries()` — called from `init(prefix)`, matching `BansDataManager`:

- Delete reports where `updateDate < now - Database.Purge.For_Period days` **and** status is
  `RESOLVED` or `DENIED`. **Open and claimed reports are never purged**, so a neglected queue
  cannot silently lose evidence.
- Delete notes where `date < the same deadline`. Notes older than the window belong to reports
  that were just deleted or expired long ago; there is no `IN` operator in `Wheres`, so a
  date-based sweep is the available primitive.

`Wheres` has no `IN`, so the status filter is expressed as two `delete` calls, one per terminal
status, each with `.and(status, EQUALS, …)`.

---

## 7. Cross-server behaviour

`addTableSync` on both tables, consumers mirroring `BansDataManager#syncPlayerPunishments`:
map the `ResultSet` to a model, `remove` then `add` into the repository. That is idempotent and
handles out-of-order and duplicate delivery.

Two things `addTableSync` is **not**, and the spec must not pretend otherwise:

- It is a **no-op** unless the database is MySQL **and** `Database.Sync-Interval > 0`. On SQLite
  the module is single-server. The config comment says so.
- It does not load rows on local boot. `loadData()` does that, once, from the same table.

`tableSync` delivery is one-directional in this codebase's usage (it is used to *receive* rows
written elsewhere). Local writes are already visible locally because they happen in-process, so
no reverse channel is needed.

---

## 8. Events

### 8.1 `PlayerReportEvent` — `Core/…/api/event/`

`extends PlayerEvent`, **not** cancellable-by-default: fired *after* the row is inserted so that a
cancelling listener cannot leave a half-created report. Payload: `Report`, plus `getCategoryId()`,
`getDetails()`, `getTargetId()` (nullable), `getTargetName()`.

Making it cancellable at all is a deliberate choice: it lets a moderation plugin veto a report
outright. The rollback (§3.1) is a synchronous `delete` of the just-inserted row plus repository
removal, which is safe because nothing else can observe the report yet.

### 8.2 `PlayerPunishEvent` — new, in `bans/event/`

**This is a required change to the Bans module**, and it is the only cross-module edit in the spec.
Fired from `BansModule#punishPlayer` after the punishment is added to its repository and the
player is notified, at the end of the success path. Payload: target `Player`, `UserInfo`,
`PunishmentType`, `PunishmentReason`, `CommandSender` executor, `boolean silent`.

`PlayerPardonEvent` is not needed — a pardon does not retroactively make a report unjustified.

`ReportsModule` registers a listener for it and concludes matching reports. The dependency is
one-directional (bans knows nothing about reports) and soft (a listener for a class that may not
be loaded is a no-op, and the handler is only registered when the module is enabled).

### 8.3 `PlayerReportResolvedEvent` — `Core/…/api/event/`

`extends PlayerEvent` when the reporter is online, plain `Event` otherwise — see the pattern in
`PlayerTagPurchaseEvent`. Payload: `Report`, `ReportStatus`, `boolean rewarded`, `boolean
punishmentTriggered`.

### 8.4 `PlayerReportDeleteEvent` — `Core/…/api/event/`

Payload: the deleted `Report` and its notes, for audit-log plugins.

### 8.5 Discord relay

`SocialsConfig.ANNOUNCE_REPORTS` already exists and is **dead** — nothing reads it. Wiring it:

- Add `SocialsConfig.FORMAT_REPORT` (MiniMessage, placeholders as above) and
  `SocialsModule#relayReport(Report)`, guarding on `ANNOUNCE_REPORTS.get() && isDiscordAvailable()`
  exactly like `relayDeath`.
- `SocialsListener` gains one `MONITOR` handler for `PlayerReportEvent` that calls it.

The listener lives in the **socials** module, not the reports module, so reports has no knowledge
of Discord. It is worth noting that no listener in this repo currently subscribes to any of the
13 existing custom events — this would be the first, and it is a good proof that the event
surface actually works.

### 8.6 `ReportsProvider` — `API/…/api/provider/`

`API` is a separate Gradle module that must not depend on `Core`, so the interface cannot expose
`Report`. Primitives only:

```java
public interface ReportsProvider {
    int getOpenReportCount();
    int getOpenReportCount(UUID playerId);
    int getConcludedReportCount(UUID playerId);
    boolean hasOpenReportAgainst(UUID playerId);
    List<String> getOpenReportTargets(UUID playerId);
    long getTotalPlaytimeMs(UUID playerId);   // delegates to playtime, 0 if absent
}
```

Added to `SunlightAPI` and implemented on `SunLightPlugin` as
`this.moduleManager.getByType(ReportsModule.class)` — which yields an empty `Optional` when the
module is disabled, giving the soft-dependency behaviour for free.

---

## 9. Commands

Two providers, two `commands/*.yml` files, registered with the module passed to
`addProvider` so `EconomyUtils.hasBypass(player, module)` resolves `sunlight.reports.bypass.*`.

### `reports-submit` → `plugins/SunLight/commands/reports-submit.yml`

| Node | Aliases | Args | Perm |
| --- | --- | --- | --- |
| `report` | `rep` | `player`, `category`, `details` (greedy) | `sunlight.reports.command.report` |
| `reportstatus` | `rstatus` | — | `sunlight.reports.command.report.status` |

The `player` argument uses `Arguments.playerName(CommandArguments.PLAYER)` with
`suggestions((reader, ctx) -> online player names)`. The `category` argument is a custom
`ArgumentType` resolving against the config map, throwing
`CommandSyntaxException.custom(ReportsLang.ERROR_UNKNOWN_CATEGORY)` on miss, with
`.suggestions(...)` returning visible category ids. This is the
`HomeCommonCommandProvider#homeArgument` pattern.

The `report` node's `Cooldown` in the yml is left at `0`: the module owns the cooldown (§3.2) so it
can present a domain-specific message. Documented in a config comment so nobody "fixes" it by
setting a value there and creating two competing cooldowns.

### `reports-staff` → `plugins/SunLight/commands/reports-staff.yml`

Root `reports` (aliases `report`) with children:

| Child | Aliases | Args | Perm |
| --- | --- | --- | --- |
| `list` | `l`, `gui` | `[filter]` | `sunlight.reports.command.reports` |
| `view` | `v` | `reportId` | `sunlight.reports.command.reports` |
| `claim` | — | `reportId` | `sunlight.reports.command.report.claim` |
| `release` | — | `reportId` | `sunlight.reports.command.report.claim` |
| `resolve` | — | `reportId`, `[note]` | `sunlight.reports.command.report.resolve` |
| `deny` | — | `reportId`, `[note]` | `sunlight.reports.command.report.deny` |
| `note` | — | `reportId`, `note` | `sunlight.reports.command.report.note` |
| `teleport` | `tp` | `reportId` | `sunlight.reports.command.report.teleport` |
| `delete` | — | `reportId` | `sunlight.reports.command.report.delete` |

The `reportId` argument is a custom `ArgumentType` parsing a UUID, with suggestions from the
visible reports. All `.playerOnly()`. The `[filter]` argument is an enum argument over
`ReportFilter`, localized.

`/reports` with no child opens the GUI, so the root behaves like the Bans `/banlist` family.

---

## 10. Configuration

Style: static `ConfigValue` fields in a `ReportsConfig` holder, loaded with
`config.initializeOptions(ReportsConfig.class)` at the top of `loadModule` — matching
`SocialsConfig`/`NametagsConfig`, the two most recently written modules. (Bans' `AbstractConfig`
+ `addProperty` style is the older convention.) Read at runtime via `ReportsConfig.X.get()`.

```yaml
Reports:
  Data:
    Table-Prefix: 'sunlight_reports'
    # Purge runs on startup only. Reports that are still OPEN or CLAIMED are never purged.

  Cooldown:
    # Seconds a player must wait between two reports. 0 = no cooldown.
    Submit-Seconds: 60

  Limits:
    # Reports a player may have OPEN or CLAIMED at once. -1 = unlimited.
    Max-Open-Per-Player: 3
    Details-Min-Length: 10
    Details-Max-Length: 300
    # When true, /report <player> <category> without details is rejected.
    Details-Required: true

  Categories:
    # id: { Display, Icon, Permission, Default-Detail }
    # Permission '' = everyone. Default-Detail is pre-filled in the GUI and shown as the
    # report text when the player supplies no details of their own.
    griefing:
      Display: '<green>Griefing'
      Icon: 'DIRT'
      Permission: ''
      Default-Detail: 'They broke blocks that were not theirs.'
    cheating:
      Display: '<red>Cheating'
      Icon: 'DIAMOND_SWORD'
      Permission: ''
      Default-Detail: ''
    harassment:
      Display: '<yellow>Harassment'
      Icon: 'PAPER'
      Permission: 'sunlight.reports.category.harassment'
      Default-Detail: ''

  Notify:
    Enabled: true
    Permission: 'sunlight.reports.notify'
    Sound: 'BLOCK_NOTE_BLOCK_PLING'
    Target:
      # Off by default. When on, the target is told a report exists but not who filed it.
      Enabled: false
      Message: '<gray>You have been reported. Staff will review it.'

  Blacklist:
    # Reporters in these worlds cannot use /report at all — a staff-only world is a
    # natural place to police from without the reports themselves becoming noise.
    Worlds: []
    # Reporters holding any of these permissions are exempt from the whole module.
    Exempt-Permissions: [ 'sunlight.reports.exempt' ]

  Notes:
    Max-Length: 500
    Visible-To-Reporter: false

  Rewards:
    Enabled: true
    Commands:
      - 'eco give %reporter_name% 500'
    Notify-Rewarded: true
    # 0 = no requirement. Requires the playtime module; skipped if it is not enabled.
    Minimum-Reporter-Playtime-Hours: 0
    # Which punishment types close a report as justified. WARN pays out unless excluded here.
    Require-Punishment-Type: [ 'BAN', 'MUTE' ]
```

Categories are read via `ConfigValue.forMapById(...)` with a `ReportCategory implements Writeable`
value object, the same shape as `BansSettings` uses for `General.Reasons` — so a new category can
be added to the file by hand and picked up on reload.

The `Reports` root key is namespaced by the module, matching every other module's settings layout.

---

## 11. Permissions

`ReportsPerms` as a detached tree merged in `registerPermissions()`, following `BansPerms`:

```
sunlight.reports.notify
sunlight.reports.exempt
sunlight.reports.category.<id>            (generated per category, not declared in code)
sunlight.reports.command.report
sunlight.reports.command.report.status
sunlight.reports.command.report.duplicate
sunlight.reports.command.reports
sunlight.reports.command.report.claim
sunlight.reports.command.report.resolve
sunlight.reports.command.report.deny
sunlight.reports.command.report.note
sunlight.reports.command.report.teleport
sunlight.reports.command.report.teleport.bypass-warmup
sunlight.reports.command.report.delete
sunlight.reports.bypass.cooldown
sunlight.reports.bypass.cost
sunlight.reports.admin
```

`BYPASS_COOLDOWN` is declared explicitly even though `EconomyUtils.hasCooldownBypass(player,
module)` would resolve the node for us: the node has to exist in the tree to be grantable.

`reports.admin` is a convenience parent granting everything staff-level. It is granted nowhere by
default; LuckPerms wildcard handles real installs.

Per-category permissions are read from the category's `Permission` field at check time, not
declared in the tree, since the set is user-defined. The comment in the tree says so.

---

## 12. Placeholders

`ReportsPlaceholders extends SLPlaceholders`, with a
`TypedPlaceholder<Report>` following `BansPlaceholders`, and registration in
`registerPlaceholders(PlaceholderRegistry)`:

| Placeholder | Resolves to |
| --- | --- |
| `%report_id%` | report UUID |
| `%report_status%` | localized status name |
| `%report_category%` | category display name |
| `%report_reason%` | details |
| `%report_reporter%` | reporter name |
| `%report_reporter_uuid%` | reporter UUID |
| `%report_target%` | target name, or `"unknown"` if name is blank |
| `%report_target_uuid%` | target UUID, or empty |
| `%report_date_created%` | `TimeFormats.formatDateTime` |
| `%report_date_updated%` | `TimeFormats.formatDateTime` |
| `%report_age%` | `TimeFormats.formatSince` |
| `%report_staff%` | claimant name, or empty |
| `%report_rewarded%` | localized yes/no |
| `%report_notes%` | note count |

Plus viewer-scoped, resolved without a bound `Report`:

| Placeholder | Resolves to |
| --- | --- |
| `%reports_open%` | global open count |
| `%reports_open_mine%` | viewer's open count |
| `%reports_total_mine%` | viewer's lifetime count |
| `%reports_on_me%` | open reports against the viewer |

`%reports_on_me%` is the one with real third-party value: other plugins can warn a staff member
that they have been reported. It deliberately exposes no reporter identity.

`BansModule#registerPlaceholders` is an empty `// TODO`. Reports is not a template for fixing
that, but the `TypedPlaceholder` pattern in `BansPlaceholders` is still the right reference for
building the resolvers.

---

## 13. Lang

`ReportsLang implements LangContainer`, built with `LangEntry.builder(id).chatMessage(...)` /
`.text(...)`, following `PlaytimeLang`/`BansLang`. Injected via `plugin.injectLang`.

Grouped as: `ERROR.*` (self-report, unknown category, details too short/long, too many open,
already reported, cooldown, data not loaded, target offline, no permission, not claimable, already
concluded, invalid report id, already rewarded), `REPORT.*` (created, status-changed, claimed,
claim-taken, concluded, note-added, released, deleted), `NOTIFY.*` (staff, target, rewarded),
`SYSTEM.*` (claimed, released, resolved, denied, punished, deleted, teleport), `COMMAND.*`
(descriptions, argument names, syntax errors), `MENU.*` and `GUI.*` (titles, button labels, lore,
empty-list, status enum locale), `CATEGORY.*` (visible when a category has no display override).

`SYSTEM.*` entries are the audit strings written into the note trail. They are staff-facing and
never shown to the reporter.

---

## 14. File layout

```
Core/src/main/java/su/nightexpress/sunlight/
  api/event/PlayerReportEvent.java
  api/event/PlayerReportDeleteEvent.java
  api/event/PlayerReportResolvedEvent.java
  moduleImpl/reports/
    ReportsModule.java
    ReportsProperties.java           (empty unless a UserProperty is needed; see below)
    ReportsConfig.java
    ReportsPlaceholders.java
    config/ReportsLang.java
    config/ReportsPerms.java
    model/Report.java
    model/ReportStatus.java
    model/ReportCategory.java        (Writeable)
    model/ReportNote.java
    model/ReportFilter.java
    data/ReportsDataManager.java
    data/ReportsQueries.java
    data/ReportRepository.java
    listener/ReportsListener.java
    listener/ReportsPunishListener.java
    command/ReportsSubmitCommandProvider.java
    command/ReportsStaffCommandProvider.java
    menu/ReportsMenu.java
    menu/ReportViewMenu.java
    dialog/ReportsDialogKeys.java
    dialog/impl/ReportNoteDialog.java
    dialog/impl/ReportOutcomeDialog.java
  moduleImpl/bans/event/PlayerPunishEvent.java     (new — fired by BansModule)

API/src/main/java/su/nightexpress/sunlight/api/provider/ReportsProvider.java
```

**`ReportsProperties` is expected to stay empty, and that is a decision.** The cooldown reuses
`SunUser`'s existing command-cooldown map, and the spam guards derive from the report rows
themselves. No new `UserProperty` is needed. This is the deliberate contrast with
`PlaytimeProperties`, which has 16 — report state is relational (it involves two players and
staff), so it belongs in a table, not in a per-user JSON blob. Adding a `UserProperty` here would
mean querying every user to answer "how many open reports does this player have", which does not
scale the way an indexed table does.

`ReportsLang`, `ReportsConfig`, `ReportsPerms` go under `config/` for consistency with `socials`
and `bans`; `PlaytimeLang`/`PlaytimePerms` use the same layout.

Registration: `manager.register("reports", "Reports", ReportsModule::new)` in
`SunLightPlugin#registerModules`, plus the `reportsProvider()` getter and the `SunlightAPI`
interface method. `modules.yml` gets `Reports.Enabled: false` written on first run by
`ModuleManager` — it is generated, not checked in, so there is nothing to hand-edit.

`loadModule` order, following `BansModule`:

```java
config.initializeOptions(ReportsConfig.class);
this.settings.load(config);                    // only if a non-ConfigValue settings holder is used
this.plugin.injectLang(ReportsLang.class);
this.dataManager.init(ReportsConfig.DATA_TABLE_PREFIX.get());
this.loadMenus();
this.loadDialogs();
this.loadData();                               // async
this.addListener(new ReportsListener(this.plugin, this));
this.addListener(new ReportsPunishListener(this));  // only if bans is enabled
this.commandRegistry.addProvider("reports-submit", new ReportsSubmitCommandProvider(...), this);
this.commandRegistry.addProvider("reports-staff",  new ReportsStaffCommandProvider(...), this);
```

`unloadModule()` clears the repository, sets `dataLoaded = false`, and nulls the menu and dialog
handles. Listener unregistration and task teardown are handled by `AbstractManager#shutdown`.

The punish listener is only registered when `moduleManager.getByType(BansModule.class).isPresent()`,
so there is no dead listener when Bans is off.

---

## 15. Build and verification

```bash
./gradlew :Core:compileJava     # after each skeleton
./gradlew :API:compileJava
./gradlew build -x test
```

There is no test harness in this repo, so verification is a manual matrix. Enable one module at a
time.

| Scenario | Expected |
| --- | --- |
| Module disabled | `/report` unregistered, no listener, no placeholder, no table created |
| Submit with details | Row inserted, staff notified, `/reportstatus` shows `OPEN` |
| Submit twice, same target, fast | Second rejected: cooldown, then `ERROR_ALREADY_REPORTED` |
| Submit 4 open reports | 4th rejected by `Max-Open-Per-Player` |
| Details of 5 chars | Rejected with the minimum stated |
| Self-report | Rejected |
| Unknown target name | Accepted, `targetId` null, staff see a fallback icon not a skull |
| Staff claim from two servers | First succeeds; second gets `REPORT_CLAIM_TAKEN` |
| Punish the target | Report auto-concludes `RESOLVED`, system note, reporter rewarded once |
| Resolve manually, then punish | Reward paid on resolve; the punish path finds no non-terminal report |
| Reward with reporter offline | Commands run as console, row shows `rewarded = true` |
| Conclude as `DENIED` | No reward, reporter sees the outcome |
| Purge an old concluded report | Row and its notes gone; an `OPEN` report of the same age survives |
| SQLite + `Sync-Interval` set | No sync, no errors, single-server behaviour |
| Warmups on, no bypass perm | `/reporttp` shows the warmup Boss Bar |
| Staff without `teleport.bypass-warmup` | Cannot skip the warmup |

Edge cases to check by hand: report submitted then reporter relogs mid-cooldown; two staff
conclude the same report in the same tick; a report whose target is later renamed; a details string
containing a newline (must not split a reward command — `replaceAll` per line); a details string
containing MiniMessage tags; claim released by a different staff member than the claimant.

---

## 16. Open questions

1. **Punish-driven resolution scope.** Should a `WARN` close and reward a report (§5.2), or only
   `BAN`/`MUTE`? Default in this spec is that it does, with `Require-Punishment-Type` to opt out.
2. **Target notification.** Default off. Turning it on is a real behavioural change that players
   will notice; worth confirming the message wording.
3. **Report cancellation.** There is no self-withdraw. Should a reporter be able to cancel their
   own open report, or does the queue own it once filed?
4. **Category default detail.** When a player supplies no details and `Details-Required` is off,
   the category's `Default-Detail` becomes the report text. Is a canned reason acceptable
   evidence, or should a report with no player-written text be marked low-confidence in the GUI?
