0. What I found in your codebase
- Module pattern: XxxModule extends Module, loadModule(FileConfig) → settings.load(config) + plugin.injectLang(XxxLang.class) + UserPropertyRegistry.register(...) + addListener(...) + commandRegistry.addProvider(name, provider, this) + addTask/addAsyncTask. References: moduleImpl/afk/AfkModule.java, moduleImpl/nick/NickModule.java, moduleImpl/glow/GlowModule.java.
- Perms: detached tree per module merged in registerPermissions(). See nick/config/NickPerms.java.
- Persistence options:
- Small per-player scalars → UserProperty<T>(persistent=true) on SunUser, auto-saved as JSON in _users.properties. See SunUser.java:95-105, user/property/UserPropertyRegistry.java.
- History/audit lists → own Table via DataHandler.createTable() + XxxDataManager + addTableSync for cross-server. Reference: bans/data/BansDataManager.java.
- AFK hook: AfkModule implements AfkProvider, exposed via SunlightAPI.afkProvider() / moduleManager.getByType(AfkModule.class). Playtime must reuse isAfk(Player), not build a second tracker.
- Packet gate: LoadCondition.packetLibrary() + dual handler in GlowModule (packetevents vs ProtocolLib). Reuse for Nametags.
- Discord: hook/HookId.DISCORD_SRV, compileOnly com.discordsrv:discordsrv:1.28.0 already in root build.gradle, zero usages yet. moduleImpl/socials/SocialsModule.java is an empty stub and not registered in SunLightPlugin.registerModules() — greenfield.
- Bans is the template for Reports: in-memory repo + own tables + HistoryMenu/PunishmentsMenu + chat handler + save-interval task.
1. Scope / module IDs
All optional, default-disabled via modules.yml, registered in SunLightPlugin.registerModules():
ID
socials
nametags
playtime
reports
Add provider interfaces in api/provider/ (PlaytimeProvider, ReportsProvider, NametagsProvider) + getters on SunLightPlugin, following AfkProvider/NickProvider/GlowProvider.
2. Socials / Discord
Files: moduleImpl/socials/ → rewrite SocialsModule.java, new config/SocialsConfig.java, config/SocialsLang.java, config/SocialsPerms.java, hook/DiscordHook.java, command/SocialsCommandProvider.java, listener/SocialsListener.java.
Config:
Socials:
  Links:
    discord: {Display: "<...>", URL: "...", Permission: ""}
    site: {...}
    store: {...}
    vote: {...}
  Discord:
    Relay: {Join: true, Quit: true, Death: true, Advancement: false, Chat: false, Report: true, Punishment: true}
    RequireLinked: false
Behavior:
- Guard everything with Utils.isInstalled(HookId.DISCORD_SRV) (same style as WorldGuard guard in AfkModule). Never hard-import DiscordSRV classes in module load path — isolate in DiscordHook.
- Bukkit events at MONITOR in SocialsListener, forward MiniMessage→plain text via DiscordSRV API. Incoming Discord→MC behind a flag, default off.
- /discord, /site, /vote… + /socials GUI menu of links (reuse KitsMenu/WarpListMenu pattern). Links also work with zero DiscordSRV.
- Placeholders: socials_discord_link, etc. Perms: socials.command.<link>, socials.admin, socials.discord.bypass-link.
- unloadModule(): unregister listener, clear menus.
3. Nametags (Prefix, Suffix, Namecolor, per-player tags)
Files: moduleImpl/nametags/ → NametagsModule.java, config/NametagsConfig.java, config/NametagsLang.java, config/NametagsPerms.java, model/NametagTag.java, model/NametagRule.java, handler/NametagPacketHandler.java (+ PacketsHandler/ProtocolHandler split mirroring glow/handler/), command/NametagsCommandProvider.java, menu/TagsMenu.java, event/PlayerNametagChangeEvent.java, NametagProperties.java.
Data: one persistent property only:
- UserProperty<String> TAG_SELECTED (lowercase tag id, nullable = none).
Prefix/suffix/namecolor are resolved, not stored: Nametags.Ranks.<group> = {Priority, Prefix, Suffix, NameColor} resolved via the same “greatest perm” helper AfkSettings uses. Avoids stale data on rank change.
Tags:
Nametags:
  TagOverridesRank: false
  Tags:
    warrior: {Display: "...", Prefix: "...", Suffix: "...", Permission: "nametags.tag.warrior", Cost: 0, Description: "..."}
Rendering: scoreboard-team packets (client-side, no vanilla /team clash). Reuse GlowModule viewer snapshot + same-world filter + refresh on join/world-change/perm-change. Interop decision needed: if Glow team + Nametag team conflict on the same packet lib, merge into a single team (documented follow-up).
Commands: /tags (GUI), /tag <id|none>, /nametag set <player> <tag> (admin), /nametag reload.
Validation: reuse NickModule.sanitizeNickname approach — strip tags for length, banned-words list, nametags.tag.<id> + nametags.colors perms.
Placeholders: nametag_prefix, nametag_suffix, nametag_tag, nametag_namecolor, nametag_formatted_name.
4. Playtime
Files: moduleImpl/playtime/ → PlaytimeModule.java, config/PlaytimeConfig.java, config/PlaytimeLang.java, config/PlaytimePerms.java, tracker/PlaytimeTracker.java, command/PlaytimeCommandProvider.java, menu/PlaytopMenu.java, listener/PlaytimeListener.java, event/PlayerPlaytimeRewardEvent.java.
Data (all persistent UserProperty, no new table in v1):
- playtime_total_ms: long
- playtime_day_ms: long + playtime_day_key: String (yyyy-MM-dd)
- playtime_week_ms + playtime_week_key (yyyy-ww), same for month/year
- streak_daily_count: int + streak_daily_last: String
- playtime_rewards_claimed: Set<String> (register with TypeToken via UserPropertyRegistry.register(name, Type, runtimeType, ...))
Tick logic: single addTask(this::tick, 20L*5) (5s). For each online player: look up AfkModule via moduleManager.getByType; if isAfk() or exempt world → skip accumulation but update lastSeen. On rollover (key mismatch) reset bucket + evaluate daily-streak continuity (yesterday → +1 else → 1). Writes go through SunUser.markDirty() + existing saver; no direct SQL.
Commands:
- /playtime [player] [alltime|year|month|week|day] — offline-capable via userManager/dataHandler.loadProfile.
- /playtop [period] — paginated chat + PlaytopMenu GUI, async sort, 60s cache. v1 = online + cached users only (full-global top needs an indexed column — v2).
- /playrewards — claim GUI. /playtime reset <player> <period> admin.
Streaks/rewards:
Playtime:
  Rewards:
    weekly16h: {Type: WEEKLY_TIME, RequireMs: 57600000, Commands: ["eco give %player% 500"], Message: "...", Repeatable: true}
    streak7d: {Type: DAILY_STREAK, Require: 7, Commands: [...], Repeatable: false}
Dispatcher runs on rollover/quit, fires PlayerPlaytimeRewardEvent, dispatches console commands with %player%. Claim-guard = rewards_claimed set + week-key suffix for repeatables.
Placeholders: playtime_alltime/year/month/week/day(_formatted), playtime_top_1_name/time, playtime_streak_daily, playtime_weekly_progress.
5. Reports
Modeled 1:1 on Bans.
Files: moduleImpl/reports/ → ReportsModule.java, config/ReportsConfig.java, config/ReportsLang.java, config/ReportsPerms.java, model/Report.java, model/ReportStatus.java (OPEN/CLAIMED/RESOLVED/DENIED), data/ReportsDataManager.java, data/ReportsQueries.java, menu/ReportsMenu.java, menu/ReportViewMenu.java, command/ReportsCommandProvider.java, listener/ReportsListener.java, event/PlayerReportEvent.java.
Table (<prefix>_reports, mirror BansDataManager column style): id UUID PK, reporter UUID+name, reported UUID+name, reason mediumtext, status(16), handler(nullable), createDate, updateDate, notes. init(tablePrefix) + addTableSync for cross-server visibility + purge via TimeUtil deadline like BansDataManager.purgeOldEntries().
Flow: /report <player> <reason…> (cooldown + max-open-per-player + no self-report + offline via loadProfile) → notify reports.notify holders with clickable message → /reports GUI (filter OPEN/MINE/ALL) → claim/teleport/resolve/deny with note dialog (reuse warps/dialog/ pattern) → fire PlayerReportEvent so Socials/Discord relay can pick it up without a hard dependency.
6. Cross-cutting (all 4)
- ConfigValue.create(...) with comments, detached PermissionTree, injectLang, commandRegistry.addProvider(...) honoring global CommandKey cooldowns.
- registerPlaceholders(PlaceholderRegistry) + PAPI passthrough.
- Offline-first: userManager.getOrFetch / dataHandler.loadProfile; only live-apply when Player is online (see NickModule.setNickname offline branch).
- unloadModule() must clean teams/listeners/tasks (see Glow/Afk/Nick unload methods).
- No NMS changes. Update README.md module list + modules.yml defaults.
7. Build / test
- gradlew :Core:compileJava after each skeleton; full ./gradlew build -x test.
- Manual matrix: Paper 1.21.11 + packetevents (then ProtocolLib) + DiscordSRV; enable one module at a time. Verify: links GUI, tag select survives relog, /playtime buckets roll over, AFK pauses accumulation, report round-trip + staff notify + Discord message.
- Edge: module disabled → no listener/placeholder leak; packet-lib absent → nametags fails with clear reason; DiscordSRV absent → socials limited mode; /playtop with empty data.
8. Order
1. Reports (2–3d, self-contained, Bans template) → 2. Nametags (3–5d, packet risk) → 3. Playtime (3–4d, time logic) → 4. Socials (2–3d, external API). Parallelizable after skeleton + perms are agreed.
9. Questions I need from you
1. Discord: DiscordSRV-only, or also raw webhook mode without DiscordSRV?
2. Nametags: scoreboard-team packets OK? Tag overrides rank, or combines?
3. Playtime: /playtop global (new indexed table) vs online/cached-only v1? Weekly 16h repeatable weekly or once-ever?
4. Reports: cross-server sync like Bans, or single-server v1? Desired cooldown / max-open values?

Implement all of it. Use multiple Agents. discordsrv-only, fully packetbased nametags, combine rank and tag (player can disable both), cross-server reports with configurable cooldown and reward on success, block report spamming, /playtop needs to be performant, daily/weekly/monthly goals must configurable ingame/config, playtime goal should give rewards and remainders