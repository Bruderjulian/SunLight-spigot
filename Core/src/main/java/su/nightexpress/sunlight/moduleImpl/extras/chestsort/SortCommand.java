package su.nightexpress.sunlight.moduleImpl.extras.chestsort;


import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.extras.ExtrasModule;
import su.nightexpress.sunlight.moduleImpl.extras.config.ExtrasLang;
import su.nightexpress.sunlight.moduleImpl.extras.config.ExtrasPerms;

public class SortCommand extends CommandProvider<ExtrasModule> {

    private static final String COMMAND_ROOT = "chestsort";
    private static final String COMMAND_TOGGLE = "toggle";

    public SortCommand(final ExtrasModule module) {
        super(module, "chestsort");
    }

    @Override
    public void setup() {
        this.register(COMMAND_TOGGLE, builder -> builder
                .withFullDescription(ExtrasLang.COMMAND_CHEST_SORT_DESC.text())
                .withPermission(ExtrasPerms.COMMAND_CHEST_SORT)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.toggleSorting(sender, arguments);
                }))
                .under(COMMAND_ROOT);

        this.registerRoot(COMMAND_ROOT, builder -> builder
                .withFullDescription(ExtrasLang.COMMAND_CHEST_SORT_DESC.text())
                .withPermission(ExtrasPerms.COMMAND_CHEST_SORT)
                .executes((sender, arguments) -> {
                    return this.toggleSorting(sender, arguments);
                }));
    }

    private int toggleSorting(final CommandSender sender, final CommandArguments arguments) {
        // TODO
        return 1;
    }
}
