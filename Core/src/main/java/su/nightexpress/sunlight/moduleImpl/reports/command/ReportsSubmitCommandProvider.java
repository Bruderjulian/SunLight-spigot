package su.nightexpress.sunlight.moduleImpl.reports.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandArguments;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsConfig;
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
    private static final String COMMAND_TOGGLE = "toggle";

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
                                .optional()
                                .localized(ReportsLang.COMMAND_ARGUMENT_NAME_PLAYER),
                        Arguments.string(ARG_CATEGORY)
                                .optional()
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

        this.registerLiteral(COMMAND_TOGGLE, true, new String[] {}, builder -> builder
                .playerOnly()
                .description(ReportsLang.COMMAND_TOGGLE_DESC)
                .permission(ReportsPerms.COMMAND_REPORT_TOGGLE)
                .executes((context, arguments) -> {
                    this.module.toggleOptOut(context.getPlayerOrThrow());
                    return true;
                }));
    }

    private boolean report(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();

        // Bare /report opens the guided flow, but only when every part of it is actually
        // available: a player who cannot see any category would otherwise get a dead-end menu.
        if (!arguments.contains(CommandArguments.PLAYER)) {
            if (ReportsConfig.COMMAND_GUI.get() && !this.module.getVisibleCategoryIds(player).isEmpty()) {
                return this.module.openTargetMenu(player);
            }
            this.module.sendPrefixed(ReportsLang.ERROR_GUI_UNAVAILABLE, player);
            return false;
        }

        String targetName = arguments.getString(CommandArguments.PLAYER);
        String categoryId = arguments.contains(ARG_CATEGORY) ? arguments.getString(ARG_CATEGORY) : null;
        String details = arguments.contains(ARG_DETAILS) ? arguments.getString(ARG_DETAILS) : null;

        if (categoryId == null) {
            // A target with no category: pick the category in the guided flow rather than
            // guessing one, since the wrong category sends the report to the wrong staff.
            return ReportsConfig.COMMAND_GUI.get() ? this.startGuiAt(player, targetName) : this.reportMissingCategory(player);
        }

        return this.module.submit(player, targetName, categoryId, details);
    }

    private boolean startGuiAt(Player player, String targetName) {
        this.module.openCategoryDialog(player, targetName);
        return true;
    }

    private boolean reportMissingCategory(Player player) {
        this.module.sendPrefixed(ReportsLang.ERROR_UNKNOWN_CATEGORY, player, b -> b
                .with(SLPlaceholders.GENERIC_LIST, () -> String.join(", ", this.module.getCategoryIds())));
        return false;
    }

    private boolean showStatus(CommandContext context, ParsedArguments arguments) {
        return this.module.openMenu(context.getPlayerOrThrow(), ReportFilter.MINE);
    }
}
