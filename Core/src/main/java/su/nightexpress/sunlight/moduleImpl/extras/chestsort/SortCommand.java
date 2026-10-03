package su.nightexpress.sunlight.moduleImpl.extras.chestsort;

import java.util.List;

import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.extras.ExtrasModule;
import su.nightexpress.sunlight.moduleImpl.extras.config.ExtrasLang;
import su.nightexpress.sunlight.moduleImpl.extras.config.ExtrasPerms;

public class SortCommand extends CommandProvider<ExtrasModule> {

    public SortCommand(final ExtrasModule module) {
        super(module, "chestsort");
    }

    @Override
    public void setup() {
        this.register("chestsort", List.of(), builder -> builder
                .withFullDescription(ExtrasLang.COMMAND_CHEST_SORT_DESC.text())
                .withPermission(ExtrasPerms.COMMAND_CHEST_SORT.getName())
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.toggleSorting(sender, arguments);
                }));

        this.registerRoot("mode", builder -> builder
                .withFullDescription("TODO")
                .withPermission("TODO")); // TODO
    }

    private int toggleSorting(final CommandSender sender, final CommandArguments arguments) {
        // TODO
        return 1;
    }
}
