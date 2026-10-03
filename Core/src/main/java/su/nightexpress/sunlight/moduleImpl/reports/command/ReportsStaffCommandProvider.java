package su.nightexpress.sunlight.moduleImpl.reports.command;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsModule;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsLang;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsPerms;
import su.nightexpress.sunlight.moduleImpl.reports.model.Report;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportFilter;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportStatus;
import su.nightexpress.sunlight.utils.Utils;

public class ReportsStaffCommandProvider extends CommandProvider<ReportsModule> {

    private static final String ARG_REPORT_ID = "report_id";
    private static final String ARG_NOTE = "note";
    private static final String ARG_FILTER = "filter";

    private static final String COMMAND_LIST = "list";
    private static final String COMMAND_VIEW = "view";
    private static final String COMMAND_CLAIM = "claim";
    private static final String COMMAND_RELEASE = "release";
    private static final String COMMAND_RESOLVE = "resolve";
    private static final String COMMAND_DENY = "deny";
    private static final String COMMAND_NOTE = "note";
    private static final String COMMAND_TELEPORT = "teleport";
    private static final String COMMAND_DELETE = "delete";
    private static final String COMMAND_STATS = "stats";
    private static final String COMMAND_CASE = "case";

    public ReportsStaffCommandProvider(final ReportsModule module) {
        super(module, "reports-staff");
    }

    @Override
    public void setup() {
        this.register(COMMAND_LIST, List.of(), builder -> builder
                .withFullDescription(ReportsLang.COMMAND_LIST_DESC.text())
                .withPermission(ReportsPerms.COMMAND_REPORTS.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withOptionalArguments(CommandArgumentConstants.string(ARG_FILTER,
                        info -> Utils.getEnumNames(ReportFilter.class)))
                .executes(this::list));

        this.register(COMMAND_VIEW, List.of(), builder -> builder
                .withFullDescription(ReportsLang.COMMAND_VIEW_DESC.text())
                .withPermission(ReportsPerms.COMMAND_REPORTS.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(this.reportArgument())
                .executes(this::view));

        this.register(COMMAND_CLAIM, List.of(), builder -> builder
                .withFullDescription(ReportsLang.COMMAND_CLAIM_DESC.text())
                .withPermission(ReportsPerms.COMMAND_REPORT_CLAIM.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(this.reportArgument())
                .executes((sender, arguments) -> {
                    return this.withReport(sender, arguments, this.module::claim);
                }));

        this.register(COMMAND_RELEASE, List.of(), builder -> builder
                .withFullDescription(ReportsLang.COMMAND_RELEASE_DESC.text())
                .withPermission(ReportsPerms.COMMAND_REPORT_CLAIM.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(this.reportArgument())
                .executes((sender, arguments) -> {
                    return this.withReport(sender, arguments, this.module::release);
                }));

        this.register(COMMAND_RESOLVE, List.of(), builder -> builder
                .withFullDescription(ReportsLang.COMMAND_RESOLVE_DESC.text())
                .withPermission(ReportsPerms.COMMAND_REPORT_RESOLVE.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(this.reportArgument())
                .withOptionalArguments(CommandArgumentConstants.greedy(ARG_NOTE, info -> List.of()))
                .executes((sender, arguments) -> {
                    return this.conclude(sender, arguments, ReportStatus.RESOLVED);
                }));

        this.register(COMMAND_DENY, List.of(), builder -> builder
                .withFullDescription(ReportsLang.COMMAND_DENY_DESC.text())
                .withPermission(ReportsPerms.COMMAND_REPORT_DENY.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(this.reportArgument())
                .withOptionalArguments(CommandArgumentConstants.greedy(ARG_NOTE, info -> List.of()))
                .executes((sender, arguments) -> {
                    return this.conclude(sender, arguments, ReportStatus.DENIED);
                }));

        this.register(COMMAND_NOTE, List.of(), builder -> builder
                .withFullDescription(ReportsLang.COMMAND_NOTE_DESC.text())
                .withPermission(ReportsPerms.COMMAND_REPORT_NOTE.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(this.reportArgument(), new GreedyStringArgument(ARG_NOTE))
                .executes(this::note));

        this.register(COMMAND_TELEPORT, List.of(), builder -> builder
                .withFullDescription(ReportsLang.COMMAND_TELEPORT_DESC.text())
                .withPermission(ReportsPerms.COMMAND_REPORT_TELEPORT.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(this.reportArgument())
                .executes(this::teleport));

        this.register(COMMAND_DELETE, List.of(), builder -> builder
                .withFullDescription(ReportsLang.COMMAND_DELETE_DESC.text())
                .withPermission(ReportsPerms.COMMAND_REPORT_DELETE.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(this.reportArgument())
                .executes((sender, arguments) -> {
                    return this.withReport(sender, arguments, this.module::deleteReport);
                }));

        this.register(COMMAND_STATS, List.of(), builder -> builder
                .withFullDescription(ReportsLang.COMMAND_STATS_DESC.text())
                .withPermission(ReportsPerms.COMMAND_REPORTS_STATS.getName())
                .withRequirement(sender -> sender instanceof Player)
                .executes(this::stats));

        this.register(COMMAND_CASE, List.of(), builder -> builder
                .withFullDescription(ReportsLang.COMMAND_CASE_DESC.text())
                .withPermission(ReportsPerms.COMMAND_REPORTS.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.NAME,
                        info -> CommandArgumentConstants.onlinePlayerNames()))
                .executes(this::openCase));

        this.registerRoot("reports", builder -> builder
                .withFullDescription(ReportsLang.COMMAND_REPORTS_ROOT_DESC.text())
                .withPermission(ReportsPerms.COMMAND_REPORTS.getName()));
    }

    private int stats(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }
        return this.module.openStats(player) ? 1 : 0;
    }

    private int openCase(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }

        final Object nameObj = arguments.get(CommandArgumentConstants.NAME);
        if (!(nameObj instanceof final String name)) {
            return 0;
        }

        return this.module.openCaseFor(player, name) ? 1 : 0;
    }

    private Argument<String> reportArgument() {
        return CommandArgumentConstants.string(ARG_REPORT_ID, info -> this.module.getRepository().getReports()
                .stream()
                .map(report -> report.getId().toString())
                .toList());
    }

    private static Optional<UUID> parse(String string) {
        try {
            return Optional.of(UUID.fromString(string));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Report getReport(final CommandSender sender, final CommandArguments arguments) {
        final Object idObj = arguments.get(ARG_REPORT_ID);
        if (!(idObj instanceof final String id)) {
            return null;
        }

        final Report report = parse(id).map(reportId -> this.module.getRepository().getReport(reportId)).orElse(null);
        if (report == null) {
            this.module.sendPrefixed(ReportsLang.ERROR_INVALID_REPORT_ID, sender);
        }
        return report;
    }

    private int list(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }

        ReportFilter filter = ReportFilter.OPEN;
        final Object filterObj = arguments.get(ARG_FILTER);
        if (filterObj instanceof final String raw) {
            final ReportFilter parsed = ReportFilter.fromString(raw);
            if (parsed != null) {
                filter = parsed;
            }
        }
        return this.module.openMenu(player, filter) ? 1 : 0;
    }

    private int view(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }

        final Report report = this.getReport(sender, arguments);
        if (report == null) {
            return 0;
        }
        return this.module.openView(player, report) ? 1 : 0;
    }

    private int conclude(final CommandSender sender, final CommandArguments arguments, final ReportStatus status) {
        final Report report = this.getReport(sender, arguments);
        if (report == null) {
            return 0;
        }

        final Object noteObj = arguments.get(ARG_NOTE);
        final String note = noteObj instanceof final String value ? value : null;

        return this.module.conclude(report, status, sender, note) ? 1 : 0;
    }

    private int note(final CommandSender sender, final CommandArguments arguments) {
        final Report report = this.getReport(sender, arguments);
        if (report == null) {
            return 0;
        }

        final Object noteObj = arguments.get(ARG_NOTE);
        if (!(noteObj instanceof final String text)) {
            return 0;
        }

        return this.module.addNote(report, sender, text) ? 1 : 0;
    }

    private int teleport(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }

        final Report report = this.getReport(sender, arguments);
        if (report == null) {
            return 0;
        }

        return this.module.teleportToTarget(player, report) ? 1 : 0;
    }

    private interface ReportAction {
        boolean apply(Report report, CommandSender sender);
    }

    private int withReport(final CommandSender sender, final CommandArguments arguments, final ReportAction action) {
        final Report report = this.getReport(sender, arguments);
        if (report == null) {
            return 0;
        }
        return action.apply(report, sender) ? 1 : 0;
    }
}
