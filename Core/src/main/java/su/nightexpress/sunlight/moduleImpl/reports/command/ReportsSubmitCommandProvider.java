package su.nightexpress.sunlight.moduleImpl.reports.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandArguments;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsModule;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsLang;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsPerms;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportFilter;

import java.util.Collections;

public class ReportsSubmitCommandProvider extends CommandProvider {

    private static final String ARG_CATEGORY = "category";
    private static final String ARG_DETAILS = "details";

    private static final String COMMAND_REPORT = "report";
    private static final String COMMAND_REPORT_STATUS = "reportstatus";

    private final ReportsModule module;

    public ReportsSubmitCommandProvider(SunLightPlugin plugin, ReportsModule module) {
        super(plugin);
        this.module = module;
    }

    @Override
    public void registerDefaults() {
        this.registerLiteral(COMMAND_REPORT, true, new String[] { "rep" }, builder -> builder
                .playerOnly()
                .description(ReportsLang.COMMAND_REPORT_DESC)
                .permission(ReportsPerms.COMMAND_REPORT)
                .withArguments(
                        Arguments.playerName(CommandArguments.PLAYER)
                                .localized(ReportsLang.COMMAND_ARGUMENT_NAME_PLAYER),
                        Arguments.string(ARG_CATEGORY)
                                .localized(ReportsLang.COMMAND_ARGUMENT_NAME_CATEGORY)
                                .suggestions((reader, context) -> {
                                    CommandSender sender = context.getSender();
                                    if (sender == null)
                                        return Collections.emptyList();

                                    return this.module.getVisibleCategoryIds(sender);
                                }),
                        Arguments.greedyString(ARG_DETAILS)
                                .optional()
                                .localized(ReportsLang.COMMAND_ARGUMENT_NAME_DETAILS))
                .executes(this::report));

        this.registerLiteral(COMMAND_REPORT_STATUS, true, new String[] { "rstatus" }, builder -> builder
                .playerOnly()
                .description(ReportsLang.COMMAND_REPORT_STATUS_DESC)
                .permission(ReportsPerms.COMMAND_REPORT_STATUS)
                .executes(this::showStatus));
    }

    private boolean report(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();

        String targetName = arguments.getString(CommandArguments.PLAYER);
        String categoryId = arguments.getString(ARG_CATEGORY);
        String details = arguments.contains(ARG_DETAILS) ? arguments.getString(ARG_DETAILS) : null;

        return this.module.submit(player, targetName, categoryId, details);
    }

    private boolean showStatus(CommandContext context, ParsedArguments arguments) {
        return this.module.openMenu(context.getPlayerOrThrow(), ReportFilter.MINE);
    }
}
