package su.nightexpress.sunlight.moduleImpl.reports;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.Strings;
import su.nightexpress.nightcore.util.bukkit.NightSound;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.event.PlayerReportDeleteEvent;
import su.nightexpress.sunlight.api.event.PlayerReportEvent;
import su.nightexpress.sunlight.api.event.PlayerReportResolvedEvent;
import su.nightexpress.sunlight.api.provider.ReportsProvider;
import su.nightexpress.sunlight.command.CommandKey;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.bans.BansModule;
import su.nightexpress.sunlight.moduleImpl.bans.event.PlayerPunishEvent;
import su.nightexpress.sunlight.moduleImpl.bans.punishment.PunishmentType;
import su.nightexpress.sunlight.moduleImpl.playtime.PlaytimeProperties;
import su.nightexpress.sunlight.moduleImpl.reports.command.ReportsStaffCommandProvider;
import su.nightexpress.sunlight.moduleImpl.reports.command.ReportsSubmitCommandProvider;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsConfig;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsLang;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsPerms;
import su.nightexpress.sunlight.moduleImpl.reports.data.ReportRepository;
import su.nightexpress.sunlight.moduleImpl.reports.data.ReportsDataManager;
import su.nightexpress.sunlight.moduleImpl.reports.dialog.ReportsDialogKeys;
import su.nightexpress.sunlight.moduleImpl.reports.dialog.impl.ReportNoteDialog;
import su.nightexpress.sunlight.moduleImpl.reports.dialog.impl.ReportOutcomeDialog;
import su.nightexpress.sunlight.moduleImpl.reports.listener.ReportsPunishListener;
import su.nightexpress.sunlight.moduleImpl.reports.menu.ReportViewMenu;
import su.nightexpress.sunlight.moduleImpl.reports.menu.ReportsMenu;
import su.nightexpress.sunlight.moduleImpl.reports.model.Report;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportCategory;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportFilter;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportNote;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportStatus;
import su.nightexpress.sunlight.teleport.TeleportContext;
import su.nightexpress.sunlight.teleport.TeleportFlag;
import su.nightexpress.sunlight.teleport.TeleportType;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.EconomyUtils;
import su.nightexpress.sunlight.utils.TimeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ReportsModule extends Module implements ReportsProvider {

    private static final CommandKey COOLDOWN_KEY = new CommandKey("reports", "submit");
    private static final String PLAYTIME_MODULE_ID = "playtime";

    private final ReportRepository repository = new ReportRepository();
    private final ReportsDataManager dataManager;
    private final Map<String, ReportCategory> categories = new LinkedHashMap<>();

    private ReportsMenu menu;
    private ReportViewMenu viewMenu;

    private volatile boolean dataLoaded;

    public ReportsModule(ModuleDefinition<ReportsModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.dataManager = new ReportsDataManager(this.dataHandler, this);
    }

    @Override
    protected void loadModule(FileConfig config) {
        config.initializeOptions(ReportsConfig.class);

        this.categories.clear();
        this.categories.putAll(ReportsConfig.readCategories(config));

        this.plugin.injectLang(ReportsLang.class);

        this.dataManager.init(ReportsConfig.DATA_TABLE_PREFIX.get());

        this.loadMenus();
        this.loadDialogs();
        this.loadData();

        if (this.plugin.moduleManager().isPresent(BansModule.class)) {
            this.addListener(new ReportsPunishListener(this));
        }

        this.commandRegistry.addProvider("reports-submit", new ReportsSubmitCommandProvider(this.plugin, this), this);
        this.commandRegistry.addProvider("reports-staff", new ReportsStaffCommandProvider(this.plugin, this), this);
    }

    @Override
    protected void unloadModule() {
        this.repository.clear();
        this.categories.clear();
        this.dataLoaded = false;
        this.menu = null;
        this.viewMenu = null;
    }

    @Override
    protected void registerPermissions(PermissionTree root) {
        root.merge(ReportsPerms.ROOT);
    }

    @Override
    protected void registerCommands() {
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        registry.register("reports_open", (player, payload) -> String.valueOf(this.getOpenReportCount()));
        registry.register("reports_open_mine",
                (player, payload) -> String.valueOf(this.getOpenReportCount(player.getUniqueId())));
        registry.register("reports_total_mine",
                (player, payload) -> String.valueOf(this.getConcludedReportCount(player.getUniqueId())));
        registry.register("reports_on_me",
                (player, payload) -> String.valueOf(this.hasOpenReportAgainst(player.getUniqueId())));
    }

    private void loadMenus() {
        this.menu = new ReportsMenu(this);
        this.menu.load(this.plugin, FileConfig.load(this.getLocalUIPath(), "reports.yml"));

        this.viewMenu = new ReportViewMenu(this);
        this.viewMenu.load(this.plugin, FileConfig.load(this.getLocalUIPath(), "report-view.yml"));
    }

    private void loadDialogs() {
        this.dialogRegistry.register(ReportsDialogKeys.REPORT_NOTE, () -> new ReportNoteDialog(this));
        this.dialogRegistry.register(ReportsDialogKeys.REPORT_OUTCOME, () -> new ReportOutcomeDialog(this));
    }

    private void loadData() {
        this.dataLoaded = false;
        this.plugin.runTaskAsync(task -> {
            for (Report report : this.dataManager.getReports()) {
                this.stampCategoryDisplay(report);
                this.repository.upsertReport(report);
            }
            for (ReportNote note : this.dataManager.getNotes()) {
                this.repository.addNote(note);
            }
            this.dataLoaded = true;
        });
    }

    public boolean isDataLoaded() {
        return this.dataLoaded;
    }

    public ReportRepository getRepository() {
        return this.repository;
    }

    public ReportsDataManager getDataManager() {
        return this.dataManager;
    }

    // Categories

    public Map<String, ReportCategory> getCategories() {
        return Collections.unmodifiableMap(this.categories);
    }

    public @Nullable ReportCategory getCategory(@Nullable String id) {
        if (id == null)
            return null;

        return this.categories.get(id.toLowerCase(Locale.ROOT));
    }

    public List<String> getCategoryIds() {
        return List.copyOf(this.categories.keySet());
    }

    public List<String> getVisibleCategoryIds(@NotNull CommandSender sender) {
        List<String> ids = new ArrayList<>();
        this.categories.forEach((id, category) -> {
            if (category.hasAccess(sender))
                ids.add(id);
        });
        return ids;
    }

    public void stampCategoryDisplay(@NotNull Report report) {
        ReportCategory category = this.getCategory(report.getCategoryId());
        report.setCategoryDisplay(category == null ? report.getCategoryId() : category.getDisplay());
    }

    // Submission

    public boolean submit(@NotNull Player sender, @NotNull String targetNameRaw, @NotNull String categoryIdRaw,
            @Nullable String detailsRaw) {
        if (!this.dataLoaded) {
            this.sendPrefixed(ReportsLang.ERROR_DATA_NOT_LOADED, sender);
            return false;
        }
        if (this.isExempt(sender)) {
            this.sendPrefixed(ReportsLang.ERROR_EXEMPT, sender);
            return false;
        }
        if (this.isBlacklistedWorld(sender)) {
            this.sendPrefixed(ReportsLang.ERROR_BLACKLIST_WORLD, sender);
            return false;
        }

        String targetName = targetNameRaw.trim();
        if (targetName.isEmpty() || targetName.length() > 32 || Strings.varStyle(targetName).isEmpty()) {
            this.sendPrefixed(ReportsLang.ERROR_INVALID_NAME, sender);
            return false;
        }
        if (targetName.equalsIgnoreCase(sender.getName())) {
            this.sendPrefixed(ReportsLang.ERROR_SELF_REPORT, sender);
            return false;
        }

        String categoryId = categoryIdRaw.toLowerCase(Locale.ROOT);
        ReportCategory category = this.getCategory(categoryId);
        if (category == null) {
            this.sendPrefixed(ReportsLang.ERROR_UNKNOWN_CATEGORY, sender,
                    b -> b.with(SLPlaceholders.GENERIC_LIST, () -> String.join(", ", this.getCategoryIds())));
            return false;
        }
        if (!category.hasAccess(sender)) {
            this.sendPrefixed(ReportsLang.ERROR_CATEGORY_LOCKED, sender);
            return false;
        }

        String details = detailsRaw == null ? "" : detailsRaw.trim();
        if (details.isEmpty()) {
            if (ReportsConfig.LIMIT_DETAILS_REQUIRED.get()) {
                this.sendPrefixed(ReportsLang.ERROR_DETAILS_REQUIRED, sender);
                return false;
            }
            details = category.getDefaultDetailOr(category.getDisplay());
        }
        int minLength = ReportsConfig.LIMIT_DETAILS_MIN.get();
        int maxLength = ReportsConfig.LIMIT_DETAILS_MAX.get();
        if (details.length() < minLength) {
            this.sendPrefixed(ReportsLang.ERROR_DETAILS_TOO_SHORT, sender,
                    b -> b.with(SLPlaceholders.GENERIC_MIN, () -> String.valueOf(minLength)));
            return false;
        }
        if (details.length() > maxLength) {
            this.sendPrefixed(ReportsLang.ERROR_DETAILS_TOO_LONG, sender,
                    b -> b.with(SLPlaceholders.GENERIC_MAX, () -> String.valueOf(maxLength)));
            return false;
        }

        SunUser user = this.userManager.getOrFetch(sender);

        int cooldown = ReportsConfig.COOLDOWN_SUBMIT.get();
        boolean bypassCooldown = EconomyUtils.hasCooldownBypass(sender, this);
        if (cooldown > 0 && !bypassCooldown) {
            Long expireDate = user.getCommandCooldown(COOLDOWN_KEY);
            if (expireDate != null) {
                this.sendPrefixed(ReportsLang.ERROR_COOLDOWN, sender, b -> b.with(SLPlaceholders.GENERIC_TIME,
                        () -> TimeFormats.formatDuration(expireDate, TimeFormatType.LITERAL)));
                return false;
            }
        }

        int maxOpen = ReportsConfig.LIMIT_MAX_OPEN.get();
        if (maxOpen >= 0) {
            int open = this.repository.getPendingReportsByReporter(sender.getUniqueId()).size();
            if (open >= maxOpen) {
                this.sendPrefixed(ReportsLang.ERROR_TOO_MANY_OPEN, sender, b -> b
                        .with(SLPlaceholders.GENERIC_CURRENT, () -> String.valueOf(open))
                        .with(SLPlaceholders.GENERIC_MAX, () -> String.valueOf(maxOpen)));
                return false;
            }
        }

        UUID targetId = this.userManager.getRepository().getAssociatedId(targetName);
        Report duplicate = this.findDuplicate(sender.getUniqueId(), targetId, targetName);
        if (duplicate != null && !sender.hasPermission(ReportsPerms.COMMAND_REPORT_DUPLICATE)) {
            this.sendPrefixed(ReportsLang.ERROR_ALREADY_REPORTED, sender, b -> b
                    .with(SLPlaceholders.GENERIC_TARGET, duplicate::getTargetName)
                    .with(SLPlaceholders.GENERIC_TIME, () -> TimeFormats.formatSince(duplicate.getCreateDate(),
                            TimeFormatType.LITERAL)));
            return false;
        }

        Report report = Report.create(sender.getUniqueId(), sender.getName(), targetId, targetName, categoryId, details);
        Player target = this.resolveTarget(report);
        if (target != null) {
            Location location = target.getLocation();
            report.setLastLocation(location.getWorld() == null ? null : location.getWorld().getName(), location.getX(),
                    location.getY(), location.getZ());
        }
        this.stampCategoryDisplay(report);

        this.dataManager.insertReport(report);
        this.repository.upsertReport(report);

        PlayerReportEvent event = new PlayerReportEvent(report);
        this.plugin.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.dataManager.deleteReport(report);
            this.repository.removeReport(report);
            this.sendPrefixed(ReportsLang.REPORT_REJECTED, sender);
            return false;
        }

        if (cooldown > 0 && !bypassCooldown) {
            user.setCommandCooldown(COOLDOWN_KEY, TimeUtil.createFutureTimestamp(cooldown));
        }

        this.sendPrefixed(ReportsLang.REPORT_CREATED, sender, b -> b.with(report.placeholders()));
        this.notifyStaff(report);
        this.notifyTarget(report);
        return true;
    }

    private @Nullable Report findDuplicate(UUID reporterId, @Nullable UUID targetId, String targetName) {
        for (Report report : this.repository.getPendingReportsByReporter(reporterId)) {
            if (report.matchesTarget(targetId, targetName))
                return report;
        }
        return null;
    }

    private boolean isExempt(Player player) {
        for (String permission : ReportsConfig.BLACKLIST_EXEMPT_PERMISSIONS.get()) {
            if (player.hasPermission(permission))
                return true;
        }
        return false;
    }

    private boolean isBlacklistedWorld(Player player) {
        Set<String> worlds = ReportsConfig.BLACKLIST_WORLDS.get();
        if (worlds.isEmpty() || player.getWorld() == null)
            return false;

        return worlds.stream().anyMatch(world -> world.equalsIgnoreCase(player.getWorld().getName()));
    }

    // Lifecycle

    public boolean claim(@NotNull Report report, @NotNull CommandSender staff) {
        if (report.isTerminal()) {
            this.sendConcludedError(report, staff);
            return false;
        }

        UUID staffId = staff instanceof Player player ? player.getUniqueId() : null;
        if (report.getStaffId() != null) {
            if (report.getStaffId().equals(staffId))
                return true;

            this.sendPrefixed(ReportsLang.ERROR_CLAIMED_BY_OTHER, staff,
                    b -> b.with(SLPlaceholders.GENERIC_TARGET, () -> report.getStaffName()));
            return false;
        }

        Report before = this.snapshot(report);
        report.setStatus(ReportStatus.CLAIMED);
        report.setStaff(staffId, staff.getName());
        this.commit(report, before);

        this.addSystemNote(report, ReportsLang.SYSTEM_CLAIMED, staff.getName());
        this.sendPrefixed(ReportsLang.REPORT_CLAIMED, staff, b -> b.with(report.placeholders()));
        return true;
    }

    public boolean release(@NotNull Report report, @NotNull CommandSender staff) {
        if (report.getStatus() != ReportStatus.CLAIMED) {
            this.sendPrefixed(ReportsLang.ERROR_NOT_CLAIMED, staff);
            return false;
        }

        Report before = this.snapshot(report);
        report.setStatus(ReportStatus.OPEN);
        report.setStaff(null, null);
        this.commit(report, before);

        this.addSystemNote(report, ReportsLang.SYSTEM_RELEASED, staff.getName());
        this.sendPrefixed(ReportsLang.REPORT_RELEASED, staff, b -> b.with(report.placeholders()));
        return true;
    }

    public boolean conclude(@NotNull Report report, @NotNull ReportStatus status, @NotNull CommandSender staff,
            @Nullable String note) {
        if (!status.isTerminal()) {
            return false;
        }
        if (report.isTerminal()) {
            this.sendConcludedError(report, staff);
            return false;
        }

        // The note lands before the status flips, so a failure here cannot leave a concluded report
        // with a silently missing explanation.
        if (note != null && !note.isBlank()) {
            this.addNoteInternal(report, new ReportNote(report.getId(), this.idOf(staff), staff.getName(),
                    note.trim()));
        }
        this.addSystemNote(report,
                status == ReportStatus.RESOLVED ? ReportsLang.SYSTEM_RESOLVED : ReportsLang.SYSTEM_DENIED,
                staff.getName());

        Report before = this.snapshot(report);
        report.setStatus(status);
        this.commit(report, before);

        this.sendPrefixed(ReportsLang.REPORT_CONCLUDED, staff, b -> b
                .with(SLPlaceholders.GENERIC_STATUS, () -> status.name())
                .with(report.placeholders()));

        boolean rewarded = false;
        if (status == ReportStatus.RESOLVED) {
            rewarded = this.payReward(report);
        }

        this.plugin.getPluginManager().callEvent(new PlayerReportResolvedEvent(report, status, rewarded, false));
        return true;
    }

    /**
     * The punishment path. A report against a player who has just been punished is justified by
     * definition, so it is closed here instead of waiting for someone to do paperwork — and the
     * honest reporter is paid without a staff member having to remember to.
     */
    public boolean concludeByPunishment(@NotNull Report report, @NotNull PunishmentType type,
            @NotNull CommandSender executor) {
        if (report.isTerminal()) {
            return false;
        }

        this.addSystemNote(report, ReportsLang.SYSTEM_PUNISHED, b -> b
                .with(ReportsPlaceholders.SYSTEM_TYPE, () -> type.name())
                .with(ReportsPlaceholders.SYSTEM_STAFF, executor::getName));

        Report before = this.snapshot(report);
        report.setStatus(ReportStatus.RESOLVED);
        this.commit(report, before);

        boolean rewarded = this.paysRewardFor(type) && this.payReward(report);

        this.plugin.getPluginManager().callEvent(new PlayerReportResolvedEvent(report, ReportStatus.RESOLVED,
                rewarded, true));
        return true;
    }

    private boolean paysRewardFor(PunishmentType type) {
        Set<String> allowed = ReportsConfig.REWARDS_REQUIRE_PUNISHMENT_TYPE.get();
        return allowed.isEmpty() || allowed.contains(type.name());
    }

    public boolean addNote(@NotNull Report report, @NotNull CommandSender author, @NotNull String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            this.sendPrefixed(ReportsLang.ERROR_NOTE_EMPTY, author);
            return false;
        }
        int maxLength = ReportsConfig.NOTES_MAX_LENGTH.get();
        if (trimmed.length() > maxLength) {
            this.sendPrefixed(ReportsLang.ERROR_NOTE_TOO_LONG, author,
                    b -> b.with(SLPlaceholders.GENERIC_MAX, () -> String.valueOf(maxLength)));
            return false;
        }

        this.addNoteInternal(report, new ReportNote(report.getId(), this.idOf(author), author.getName(), trimmed));
        this.sendPrefixed(ReportsLang.REPORT_NOTE_ADDED, author, b -> b.with(report.placeholders()));
        return true;
    }

    private void addNoteInternal(@NotNull Report report, @NotNull ReportNote note) {
        this.dataManager.insertNote(note);
        this.repository.addNote(note);
        report.setNoteCount(this.repository.getNotes(report.getId()).size());
        this.dataManager.updateReport(report);
    }

    private void addSystemNote(@NotNull Report report, @NotNull TextLocale locale, String staffName) {
        this.addSystemNote(report, locale, b -> b.with(ReportsPlaceholders.SYSTEM_STAFF, () -> staffName));
    }

    private void addSystemNote(@NotNull Report report, @NotNull TextLocale locale,
            Consumer<PlaceholderContext.Builder> consumer) {
        PlaceholderContext.Builder builder = PlaceholderContext.builder();
        consumer.accept(builder);
        this.addNoteInternal(report, ReportNote.system(report.getId(), builder.build().apply(locale.text())));
    }

    public boolean deleteReport(@NotNull Report report, @NotNull CommandSender staff) {
        Report before = this.snapshot(report);
        List<ReportNote> notes = this.repository.getNotes(report.getId());

        this.dataManager.deleteReport(before);
        this.repository.removeReport(before);

        this.plugin.getPluginManager().callEvent(new PlayerReportDeleteEvent(before, notes));
        this.sendPrefixed(ReportsLang.REPORT_DELETED, staff, b -> b.with(before.placeholders()));
        return true;
    }

    public boolean teleportToTarget(@NotNull Player staff, @NotNull Report report) {
        Player target = this.resolveTarget(report);
        if (target == null) {
            this.sendPrefixed(ReportsLang.ERROR_TARGET_OFFLINE, staff,
                    b -> b.with(SLPlaceholders.GENERIC_TARGET, report::getTargetName));
            return false;
        }
        if (target.getUniqueId().equals(staff.getUniqueId()))
            return true;

        this.plugin.teleportManager().teleport(TeleportContext.builder(this, staff, target.getLocation())
                .withFlagIf(TeleportFlag.BYPASS_WARMUP,
                        () -> staff.hasPermission(ReportsPerms.COMMAND_REPORT_TELEPORT_BYPASS_WARMUP))
                .build(), TeleportType.OTHER);

        this.addSystemNote(report, ReportsLang.SYSTEM_TELEPORT, staff.getName());
        return true;
    }

    public @Nullable Player resolveTarget(@NotNull Report report) {
        if (report.getTargetId() != null) {
            Player online = Bukkit.getPlayer(report.getTargetId());
            if (online != null)
                return online;
        }
        return Bukkit.getPlayerExact(report.getTargetName());
    }

    public void handlePunish(@NotNull PlayerPunishEvent event) {
        if (!this.dataLoaded)
            return;

        UUID targetId = event.getVictimInfo().id();
        String targetName = event.getVictimInfo().name();

        List<Report> reports = new ArrayList<>(
                this.repository.getPendingReportsByTargetId(targetId));
        if (reports.isEmpty()) {
            // A player punished by name without a resolvable UUID can still match by name.
            this.repository.getPendingReportsByTargetName(targetName)
                    .stream()
                    .filter(report -> !reports.contains(report))
                    .forEach(reports::add);
        }

        for (Report report : reports) {
            this.concludeByPunishment(report, event.getType(), event.getExecutor());
        }
    }

    // Rewards

    /**
     * Pays the reporter's reward, at most once.
     * <p>
     * Three guards in order: an in-memory compare-and-set for same-server races, a database
     * read-back for cross-server races, and a write of {@code rewarded = true} before the commands
     * are dispatched so a crash between the two loses the reward rather than double-paying.
     *
     * @return whether a payout was started. The commands themselves run later, off-thread.
     */
    private boolean payReward(@NotNull Report report) {
        if (!ReportsConfig.REWARDS_ENABLED.get())
            return false;
        if (report.isRewarded())
            return false;
        if (!this.meetsRewardRequirements(report))
            return false;
        if (!report.tryClaimReward())
            return false;

        this.plugin.runTaskAsync(task -> {
            if (!this.dataManager.isUnrewardedInDatabase(report.getId())) {
                report.releaseRewardClaim();
                return;
            }

            report.setRewarded(true);
            this.write(report);
            this.dispatchReward(report);
        });
        return true;
    }

    private boolean meetsRewardRequirements(Report report) {
        int hours = ReportsConfig.REWARDS_MIN_PLAYTIME_HOURS.get();
        if (hours <= 0)
            return true;

        // Reports must not require the Playtime module to exist.
        if (!this.plugin.moduleManager().isPresent(PLAYTIME_MODULE_ID))
            return true;

        return this.getTotalPlaytimeMs(report.getReporterId()) >= TimeUnit.HOURS.toMillis(hours);
    }

    private void dispatchReward(@NotNull Report report) {
        List<String> commands = ReportsConfig.REWARDS_COMMANDS.get();
        if (commands.isEmpty())
            return;

        PlaceholderContext context = PlaceholderContext.builder()
                .with(report.placeholders())
                .with(SLPlaceholders.PLAYER_NAME, report::getReporterName)
                .with(SLPlaceholders.GENERIC_SOURCE, report::getReporterName)
                .with(SLPlaceholders.GENERIC_TARGET, report::getTargetName)
                .with(SLPlaceholders.GENERIC_CATEGORY, report::getCategoryDisplay)
                .with(SLPlaceholders.GENERIC_TEXT, report::getDetails)
                .build();

        // Applied per line so a report body containing a line break cannot split one command
        // into two.
        List<String> resolved = new ArrayList<>(commands);
        resolved.replaceAll(context::apply);

        CommandSender console = this.plugin.getServer().getConsoleSender();
        this.plugin.runTask(() -> resolved.forEach(
                command -> this.plugin.getServer().dispatchCommand(console, command)));

        if (ReportsConfig.REWARDS_NOTIFY.get()) {
            Player reporter = Bukkit.getPlayer(report.getReporterId());
            if (reporter != null) {
                this.sendPrefixed(ReportsLang.NOTIFY_REWARDED, reporter);
            }
        }
    }

    // Notifications

    private void notifyStaff(@NotNull Report report) {
        if (!ReportsConfig.NOTIFY_ENABLED.get())
            return;

        String permission = ReportsConfig.NOTIFY_PERMISSION.get();
        NightSound sound = this.readSound();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission(permission))
                continue;

            this.sendPrefixed(ReportsLang.NOTIFY_STAFF, online, b -> b
                    .with(SLPlaceholders.GENERIC_TARGET, report::getTargetName)
                    .with(SLPlaceholders.GENERIC_SOURCE, report::getReporterName)
                    .with(report.placeholders()));

            if (sound != null) {
                sound.play(online);
            }
        }
    }

    private @Nullable NightSound readSound() {
        String raw = ReportsConfig.NOTIFY_SOUND.get();
        if (raw == null || raw.isBlank())
            return null;

        try {
            return NightSound.of(raw.trim().toUpperCase(Locale.ROOT), 1F, 1F);
        } catch (Exception exception) {
            return null;
        }
    }

    private void notifyTarget(@NotNull Report report) {
        if (!ReportsConfig.NOTIFY_TARGET_ENABLED.get())
            return;

        Player target = this.resolveTarget(report);
        if (target != null) {
            this.sendPrefixed(ReportsLang.NOTIFY_TARGET, target);
        }
    }

    // Menus

    public boolean openMenu(@NotNull Player player, @NotNull ReportFilter filter) {
        if (!this.dataLoaded) {
            this.sendPrefixed(ReportsLang.ERROR_DATA_NOT_LOADED, player);
            return false;
        }
        return this.menu != null && this.menu.open(player, filter);
    }

    public boolean openView(@NotNull Player player, @NotNull Report report) {
        return this.viewMenu != null && this.viewMenu.open(player, report);
    }

    public void openNoteDialog(@NotNull Player player, @NotNull Report report, @NotNull Runnable callback) {
        this.plugin.showDialog(player, ReportsDialogKeys.REPORT_NOTE, report, callback);
    }

    public void openOutcomeDialog(@NotNull Player player, @NotNull Report report, @NotNull Runnable callback) {
        this.plugin.showDialog(player, ReportsDialogKeys.REPORT_OUTCOME, report, callback);
    }

    // Queries

    public List<Report> getVisibleReports(@NotNull Player viewer, @NotNull ReportFilter filter,
            @Nullable String targetName) {
        List<Report> reports = switch (filter) {
            case OPEN -> this.repository.getReports(ReportStatus.OPEN);
            case CLAIMED -> this.repository.getReports(ReportStatus.CLAIMED);
            case MINE -> this.repository.getReportsByReporter(viewer.getUniqueId());
            case ALL -> this.repository.getReports();
            case TARGET -> this.repository.getReportsByTargetName(targetName);
        };

        // No ORDER BY exists in the query API, so ordering happens here.
        reports.sort((first, second) -> Long.compare(second.getCreateDate(), first.getCreateDate()));
        return reports;
    }

    public List<ReportNote> getNotesForViewer(@NotNull Player viewer, @NotNull Report report) {
        boolean own = report.getReporterId().equals(viewer.getUniqueId());
        if (own && !viewer.hasPermission(ReportsPerms.ADMIN)
                && !ReportsConfig.NOTES_VISIBLE_TO_REPORTER.get()) {
            return List.of();
        }
        return this.repository.getNotes(report.getId());
    }

    // Providers

    @Override
    public int getOpenReportCount() {
        return this.repository.getPendingCount();
    }

    @Override
    public int getOpenReportCount(@NotNull UUID playerId) {
        return this.repository.getPendingReportsByReporter(playerId).size();
    }

    @Override
    public int getConcludedReportCount(@NotNull UUID playerId) {
        return (int) this.repository.getReportsByReporter(playerId).stream().filter(Report::isTerminal).count();
    }

    @Override
    public boolean hasOpenReportAgainst(@NotNull UUID playerId) {
        return !this.repository.getPendingReportsByTargetId(playerId).isEmpty();
    }

    @Override
    public @NotNull List<String> getOpenReportTargets(@NotNull UUID playerId) {
        return this.repository.getPendingReportsByTargetId(playerId).stream()
                .map(Report::getTargetName)
                .distinct()
                .toList();
    }

    @Override
    public long getTotalPlaytimeMs(@NotNull UUID playerId) {
        return this.userManager.getOrFetch(playerId)
                .map(user -> user.getPropertyOrDefault(PlaytimeProperties.TOTAL))
                .orElse(0L);
    }

    // Internals

    private @Nullable UUID idOf(CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId() : null;
    }

    private void write(Report report) {
        report.setUpdateDate(System.currentTimeMillis());
        this.dataManager.updateReport(report);
    }

    /**
     * Status and staff are part of the repository's indexes, so a mutation has to move the report
     * between buckets. This is the pre-mutation stand-in that {@code applyTransition} needs in
     * order to unindex the old keys.
     */
    private Report snapshot(Report report) {
        return new Report(report.getId(), report.getReporterId(), report.getReporterName(), report.getTargetId(),
                report.getTargetName(), report.getCategoryId(), report.getDetails(), report.getStatus(),
                report.getStaffId(), report.getStaffName(), report.getCreateDate(), report.getUpdateDate(),
                report.isRewarded(), report.getLastWorld(), report.getLastX(), report.getLastY(), report.getLastZ(),
                report.getNoteCount());
    }

    private void commit(Report report, Report before) {
        this.write(report);
        this.repository.applyTransition(before, report);
    }

    private void sendConcludedError(Report report, CommandSender staff) {
        this.sendPrefixed(ReportsLang.ERROR_ALREADY_CONCLUDED, staff, b -> b
                .with(SLPlaceholders.GENERIC_STATUS, () -> report.getStatus().name())
                .with(report.placeholders()));
    }
}
