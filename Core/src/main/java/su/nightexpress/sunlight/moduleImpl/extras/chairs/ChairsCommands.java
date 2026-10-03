package su.nightexpress.sunlight.moduleImpl.extras.chairs;

import java.util.List;

import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.extras.ExtrasModule;
import su.nightexpress.sunlight.moduleImpl.extras.config.ExtrasLang;
import su.nightexpress.sunlight.moduleImpl.extras.config.ExtrasPerms;

public class ChairsCommands extends CommandProvider<ExtrasModule> {

    public static final String NODE_TOGGLE = "chairs_toggle";
    public static final String NODE_SIT = "chairs_sit";

    public ChairsCommands(final ExtrasModule module) {
        super(module, "chairs");
    }

    @Override
    public void setup() {
        this.register("chairs", List.of(), builder -> builder
                .withFullDescription(ExtrasLang.COMMAND_CHAIRS_DESC.text())
                .withPermission(ExtrasPerms.COMMAND_CHAIRS.getName())
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.toggleChairs(sender, arguments);
                }));

        this.register("sit", List.of(), builder -> builder
                .withFullDescription(ExtrasLang.COMMAND_SIT_DESC.text())
                .withPermission(ExtrasPerms.COMMAND_SIT.getName())
                .withArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.sit(sender, arguments);
                }));

        this.registerRoot("mode", builder -> builder
                .withFullDescription("TODO")
                .withPermission("TODO")); // TODO
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
