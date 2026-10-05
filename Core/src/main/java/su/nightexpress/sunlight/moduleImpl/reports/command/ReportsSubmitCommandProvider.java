package su.nightexpress.sunlight.moduleImpl.reports.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsConfig;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsModule;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsLang;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsPerms;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportFilter;

public class ReportsSubmitCommandProvider extends CommandProvider<ReportsModule> {

    private static final String ARG_CATEGORY = "category";
    private static final String ARG_DETAILS = "details";

    private static final String COMMAND_ROOT = "report";
    private static final String COMMAND_SUBMIT = "submit";
    private static final String COMMAND_STATUS = "status";
    private static final String COMMAND_TOGGLE = "toggle";

    public ReportsSubmitCommandProvider(final ReportsModule module) {
        super(module, "reports-submit");
    }

    @Override
    public void setup() {
        this.register(COMMAND_SUBMIT, builder -> builder
                .withFullDescription(ReportsLang.COMMAND_REPORT_DESC.text())
                .withPermission(ReportsPerms.COMMAND_REPORT)
                .withRequirement(sender -> sender instanceof Player)
                .withOptionalArguments(
                        CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                                info -> CommandArgumentConstants.onlinePlayerNames()),
                        this.categoryArgument(),
                        CommandArgumentConstants.greedy(ARG_DETAILS, info -> List.of()))
                .executes(this::report))
                .under(COMMAND_ROOT);

        this.register(COMMAND_STATUS, builder -> builder
                .withFullDescription(ReportsLang.COMMAND_REPORT_STATUS_DESC.text())
                .withPermission(ReportsPerms.COMMAND_REPORT_STATUS)
                .withRequirement(sender -> sender instanceof Player)
                .executes(this::showStatus))
                .aliases("reportstatus")
                .under(COMMAND_ROOT);

        this.register(COMMAND_TOGGLE, builder -> builder
                .withFullDescription(ReportsLang.COMMAND_TOGGLE_DESC.text())
                .withPermission(ReportsPerms.COMMAND_REPORT_TOGGLE)
                .withRequirement(sender -> sender instanceof Player)
                .executes(this::toggle))
                .under(COMMAND_ROOT);

        this.registerRoot(COMMAND_ROOT, builder -> builder
                .withFullDescription(ReportsLang.COMMAND_REPORT_DESC.text())
                .withPermission(ReportsPerms.COMMAND_REPORT)
                .executes(this::report));
    }

    private Argument<String> categoryArgument() {
        return CommandArgumentConstants.string(ARG_CATEGORY, info -> this.module
                .getVisibleCategoryIds(info.sender()));
    }

    private int report(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }

        // Bare /report opens the guided flow, but only when every part of it is actually
        // available: a player who cannot see any category would otherwise get a dead-end menu.
        final Object targetObj = arguments.get(CommandArgumentConstants.PLAYER);
        if (!(targetObj instanceof final String targetName)) {
            if (ReportsConfig.COMMAND_GUI.get() && !this.module.getVisibleCategoryIds(player).isEmpty()) {
                return this.module.openTargetMenu(player) ? 1 : 0;
            }
            this.module.sendPrefixed(ReportsLang.ERROR_GUI_UNAVAILABLE, player);
            return 0;
        }

        final Object categoryObj = arguments.get(ARG_CATEGORY);
        final String categoryId = categoryObj instanceof final String value ? value : null;

        final Object detailsObj = arguments.get(ARG_DETAILS);
        final String details = detailsObj instanceof final String value ? value : null;

        if (categoryId == null) {
            // A target with no category: pick the category in the guided flow rather than
            // guessing one, since the wrong category sends the report to the wrong staff.
            return ReportsConfig.COMMAND_GUI.get() ? this.startGuiAt(player, targetName)
                    : this.reportMissingCategory(player);
        }

        return this.module.submit(player, targetName, categoryId, details) ? 1 : 0;
    }

    private int startGuiAt(final Player player, final String targetName) {
        this.module.openCategoryDialog(player, targetName);
        return 1;
    }

    private int reportMissingCategory(final Player player) {
        this.module.sendPrefixed(ReportsLang.ERROR_UNKNOWN_CATEGORY, player, b -> b
                .with(SLPlaceholders.GENERIC_LIST, () -> String.join(", ", this.module.getCategoryIds())));
        return 0;
    }

    private int showStatus(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }
        return this.module.openMenu(player, ReportFilter.MINE) ? 1 : 0;
    }

    private int toggle(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }
        this.module.toggleOptOut(player);
        return 1;
    }
}
