package su.nightexpress.sunlight.moduleImpl.essential.command;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;
import su.nightexpress.sunlight.teleport.TeleportContext;
import su.nightexpress.sunlight.teleport.TeleportFlag;
import su.nightexpress.sunlight.teleport.TeleportType;

public class JumpCommandProvider extends CommandProvider<EssentialModule> {

    private static final String PERMISSION = EssentialPerms.COMMAND + ".jump";

    private static final TextLocale DESCRIPTION = LangEntry.builder("Command.Jump.Desc").text(
            "Teleport to the block you are looking at.");

    public JumpCommandProvider(final EssentialModule module) {
        super(module, "jump");
    }

    @Override
    public void setup() {
        this.register("jump", command -> command
                .withFullDescription(DESCRIPTION.text())
                .withPermission(PERMISSION)
                .withRequirement(sender -> sender instanceof Player)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::execute)).aliases("j");
    }

    private int execute(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof Player executor)) return 0;
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(su.nightexpress.nightcore.core.config.CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        Block sight = executor.getTargetBlockExact(100);
        if (sight == null) return 0;
        Location destination = sight.getLocation().add(0.5, 1.0, 0.5);
        destination.setYaw(executor.getLocation().getYaw());
        destination.setPitch(executor.getLocation().getPitch());
        final Location dest = destination.clone();
        return target.runAs(this.module, sender, PERMISSION, (user, player) -> {
            TeleportContext context = TeleportContext.builder(this.module, player, dest)
                    .sender(sender)
                    .withFlag(TeleportFlag.LOOK_FOR_SURFACE)
                    .withFlag(TeleportFlag.AVOID_LAVA)
                    .withFlag(TeleportFlag.CENTERED)
                    .build();
            this.module.teleportManager().teleportAsync(context, TeleportType.OTHER);
        });
    }
}
