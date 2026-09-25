package su.nightexpress.sunlight.moduleImpl.reports.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.Commands;
import su.nightexpress.nightcore.commands.builder.ArgumentNodeBuilder;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.nightcore.commands.exceptions.CommandSyntaxException;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandArguments;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsModule;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsLang;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsPerms;
import su.nightexpress.sunlight.moduleImpl.reports.model.Report;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportFilter;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportStatus;
import su.nightexpress.sunlight.utils.Utils;

import java.util.Optional;
import java.util.UUID;

public class ReportsStaffCommandProvider extends CommandProvider {

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

    private final ReportsModule module;

    public ReportsStaffCommandProvider(SunLightPlugin plugin, ReportsModule module) {
        super(plugin);
        this.module = module;
    }

    @Override
    public void registerDefaults() {
        this.registerLiteral(COMMAND_LIST, true, new String[] { "l", "gui" }, builder -> builder
                .playerOnly()
                .description(ReportsLang.COMMAND_LIST_DESC)
                .permission(ReportsPerms.COMMAND_REPORTS)
                .withArguments(Arguments.string(ARG_FILTER)
                        .optional()
                        .localized(ReportsLang.COMMAND_ARGUMENT_NAME_FILTER)
                        .suggestions((reader, context) -> Utils.getEnumNames(ReportFilter.class)))
                .executes(this::list));

        this.registerLiteral(COMMAND_VIEW, true, new String[] { "v" }, builder -> builder
                .playerOnly()
                .description(ReportsLang.COMMAND_VIEW_DESC)
                .permission(ReportsPerms.COMMAND_REPORTS)
                .withArguments(this.reportArgument())
                .executes(this::view));

        this.registerLiteral(COMMAND_CLAIM, true, new String[] {}, builder -> builder
                .playerOnly()
                .description(ReportsLang.COMMAND_CLAIM_DESC)
                .permission(ReportsPerms.COMMAND_REPORT_CLAIM)
                .withArguments(this.reportArgument())
                .executes((context, arguments) -> this.withReport(context, arguments, this.module::claim)));

        this.registerLiteral(COMMAND_RELEASE, true, new String[] {}, builder -> builder
                .playerOnly()
                .description(ReportsLang.COMMAND_RELEASE_DESC)
                .permission(ReportsPerms.COMMAND_REPORT_CLAIM)
                .withArguments(this.reportArgument())
                .executes((context, arguments) -> this.withReport(context, arguments, this.module::release)));

        this.registerLiteral(COMMAND_RESOLVE, true, new String[] {}, builder -> builder
                .playerOnly()
                .description(ReportsLang.COMMAND_RESOLVE_DESC)
                .permission(ReportsPerms.COMMAND_REPORT_RESOLVE)
                .withArguments(this.reportArgument(), this.noteArgument())
                .executes((context, arguments) -> this.conclude(context, arguments, ReportStatus.RESOLVED)));

        this.registerLiteral(COMMAND_DENY, true, new String[] {}, builder -> builder
                .playerOnly()
                .description(ReportsLang.COMMAND_DENY_DESC)
                .permission(ReportsPerms.COMMAND_REPORT_DENY)
                .withArguments(this.reportArgument(), this.noteArgument())
                .executes((context, arguments) -> this.conclude(context, arguments, ReportStatus.DENIED)));

        this.registerLiteral(COMMAND_NOTE, true, new String[] {}, builder -> builder
                .playerOnly()
                .description(ReportsLang.COMMAND_NOTE_DESC)
                .permission(ReportsPerms.COMMAND_REPORT_NOTE)
                .withArguments(this.reportArgument(),
                        Arguments.greedyString(ARG_NOTE).localized(ReportsLang.COMMAND_ARGUMENT_NAME_NOTE))
                .executes(this::note));

        this.registerLiteral(COMMAND_TELEPORT, true, new String[] { "tp" }, builder -> builder
                .playerOnly()
                .description(ReportsLang.COMMAND_TELEPORT_DESC)
                .permission(ReportsPerms.COMMAND_REPORT_TELEPORT)
                .withArguments(this.reportArgument())
                .executes(this::teleport));

        this.registerLiteral(COMMAND_DELETE, true, new String[] {}, builder -> builder
                .playerOnly()
                .description(ReportsLang.COMMAND_DELETE_DESC)
                .permission(ReportsPerms.COMMAND_REPORT_DELETE)
                .withArguments(this.reportArgument())
                .executes((context, arguments) -> this.withReport(context, arguments, this.module::deleteReport)));

        this.registerRoot("reports", true, new String[] { "report" },
                map -> {
                    map.put(COMMAND_LIST, "list");
                    map.put(COMMAND_VIEW, "view");
                    map.put(COMMAND_CLAIM, "claim");
                    map.put(COMMAND_RELEASE, "release");
                    map.put(COMMAND_RESOLVE, "resolve");
                    map.put(COMMAND_DENY, "deny");
                    map.put(COMMAND_NOTE, "note");
                    map.put(COMMAND_TELEPORT, "teleport");
                    map.put(COMMAND_DELETE, "delete");
                },
                builder -> builder.description(ReportsLang.COMMAND_REPORTS_ROOT_DESC)
                        .permission(ReportsPerms.COMMAND_REPORTS));
    }

    private ArgumentNodeBuilder<Report> reportArgument() {
        return Commands.argument(ARG_REPORT_ID,
                        (context, string) -> parse(string)
                                .map(id -> this.module.getRepository().getReport(id))
                                .orElseThrow(() -> CommandSyntaxException.custom(ReportsLang.ERROR_INVALID_REPORT_ID)))
                .localized(ReportsLang.COMMAND_ARGUMENT_NAME_REPORT_ID)
                .suggestions((reader, context) -> this.module.getRepository().getReports().stream()
                        .map(report -> report.getId().toString())
                        .toList());
    }

    private ArgumentNodeBuilder<String> noteArgument() {
        return Arguments.greedyString(ARG_NOTE)
                .optional()
                .localized(ReportsLang.COMMAND_ARGUMENT_NAME_NOTE);
    }

    private static Optional<UUID> parse(String string) {
        try {
            return Optional.of(UUID.fromString(string));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private boolean list(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();

        ReportFilter filter = ReportFilter.OPEN;
        if (arguments.contains(ARG_FILTER)) {
            ReportFilter parsed = ReportFilter.fromString(arguments.getString(ARG_FILTER));
            if (parsed != null)
                filter = parsed;
        }
        return this.module.openMenu(player, filter);
    }

    private boolean view(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        return this.module.openView(player, arguments.get(ARG_REPORT_ID, Report.class));
    }

    private boolean conclude(CommandContext context, ParsedArguments arguments, ReportStatus status) {
        Report report = arguments.get(ARG_REPORT_ID, Report.class);
        String note = arguments.contains(ARG_NOTE) ? arguments.getString(ARG_NOTE) : null;

        return this.module.conclude(report, status, context.getSender(), note);
    }

    private boolean note(CommandContext context, ParsedArguments arguments) {
        Report report = arguments.get(ARG_REPORT_ID, Report.class);
        return this.module.addNote(report, context.getSender(), arguments.getString(ARG_NOTE));
    }

    private boolean teleport(CommandContext context, ParsedArguments arguments) {
        Report report = arguments.get(ARG_REPORT_ID, Report.class);
        return this.module.teleportToTarget(context.getPlayerOrThrow(), report);
    }

    private interface ReportAction {
        boolean apply(Report report, CommandSender sender);
    }

    private boolean withReport(CommandContext context, ParsedArguments arguments, ReportAction action) {
        Report report = arguments.get(ARG_REPORT_ID, Report.class);
        return action.apply(report, context.getSender());
    }
}
