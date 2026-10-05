package su.nightexpress.sunlight.moduleImpl.extras.chairs;


import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.extras.ExtrasModule;
import su.nightexpress.sunlight.moduleImpl.extras.config.ExtrasLang;
import su.nightexpress.sunlight.moduleImpl.extras.config.ExtrasPerms;

public class ChairsCommands extends CommandProvider<ExtrasModule> {

    private static final String COMMAND_ROOT = "chairs";
    private static final String COMMAND_TOGGLE = "toggle";
    private static final String COMMAND_SIT = "sit";

    public ChairsCommands(final ExtrasModule module) {
        super(module, "chairs");
    }

    @Override
    public void setup() {
        this.register(COMMAND_TOGGLE, builder -> builder
                .withFullDescription(ExtrasLang.COMMAND_CHAIRS_DESC.text())
                .withPermission(ExtrasPerms.COMMAND_CHAIRS)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.toggleChairs(sender, arguments);
                }))
                .under(COMMAND_ROOT);

        this.register(COMMAND_SIT, builder -> builder
                .withFullDescription(ExtrasLang.COMMAND_SIT_DESC.text())
                .withPermission(ExtrasPerms.COMMAND_SIT)
                .withArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.sit(sender, arguments);
                }))
                .under(COMMAND_ROOT);

        this.registerRoot(COMMAND_ROOT, builder -> builder
                .withFullDescription(ExtrasLang.COMMAND_CHAIRS_DESC.text())
                .withPermission(ExtrasPerms.COMMAND_CHAIRS)
                .executes((sender, arguments) -> {
                    return this.toggleChairs(sender, arguments);
                }));
    }

    private int toggleChairs(final CommandSender sender, final CommandArguments arguments) {
        // TODO
        return 1;
    }

    private int sit(final CommandSender sender, final CommandArguments arguments) {
        // TODO
        return 1;
    }
}
