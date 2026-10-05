package su.nightexpress.sunlight.moduleImpl.bans.command;

import java.net.InetAddress;

import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.bans.BansModule;
import su.nightexpress.sunlight.moduleImpl.bans.config.BansLang;
import su.nightexpress.sunlight.moduleImpl.bans.config.BansPerms;
import su.nightexpress.sunlight.moduleImpl.bans.punishment.InetPunishment;
import su.nightexpress.sunlight.moduleImpl.bans.punishment.PlayerPunishment;
import su.nightexpress.sunlight.moduleImpl.bans.punishment.PunishmentType;
import su.nightexpress.sunlight.utils.Utils;

public class PardonCommandsProvider extends CommandProvider<BansModule> {

    public PardonCommandsProvider(BansModule module) {
        super(module, "bans-pardon");
    }

    @Override
    public void setup() {
        this.register("unban",
                builder -> this.builderPlayer(builder, PunishmentType.BAN));
        this.register("unmute",
                builder -> this.builderPlayer(builder, PunishmentType.MUTE));
        this.register("unwarn",
                builder -> this.builderPlayer(builder, PunishmentType.WARN));

        this.register("unbanip", this::builderInet);
    }

    private void builderInet(dev.jorel.commandapi.CommandAPICommand builder) {
        builder
                .withFullDescription(BansLang.COMMAND_UNBAN_IP_DESC.text())
                .withPermission(BansPerms.COMMAND_UNBAN_IP)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.INET_ADDRESS,
                        info -> this.module.getPunishmentRepository(PunishmentType.BAN).getActiveInetPunishments()
                                .stream().map(InetPunishment::getRawAddress).toList()))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::pardonInet);
    }

    private void builderPlayer(dev.jorel.commandapi.CommandAPICommand builder, PunishmentType type) {
        String description = switch (type) {
            case BAN -> BansLang.COMMAND_UNBAN_DESC.text();
            case MUTE -> BansLang.COMMAND_UNMUTE_DESC.text();
            case WARN -> BansLang.COMMAND_UNWARN_DESC.text();
        };

        String permission = switch (type) {
            case BAN -> BansPerms.COMMAND_UNBAN;
            case MUTE -> BansPerms.COMMAND_UNMUTE;
            case WARN -> BansPerms.COMMAND_UNWARN;
        };

        builder
                .withFullDescription(description)
                .withPermission(permission)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> this.module.getPunishmentRepository(type).getActivePlayerPunishments().stream()
                                .map(PlayerPunishment::getPlayerName).toList()))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.pardonPlayer(sender, arguments, type);
                });
    }

    private int pardonInet(CommandSender sender, CommandArguments arguments) {
        final String addressRaw = (String) arguments.get(CommandArgumentConstants.INET_ADDRESS);
        InetAddress address = null;
        if (addressRaw != null && !addressRaw.isBlank()) {
            try {
                address = InetAddress.getByName(addressRaw.trim());
            } catch (Exception exception) {
                address = null;
            }
        }
        if (address == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        final boolean silent = target != null && target.silent();

        return this.module.pardonInet(address, sender, silent) ? 1 : 0;
    }

    private int pardonPlayer(CommandSender sender, CommandArguments arguments, PunishmentType type) {
        final String targetName = (String) arguments.get(CommandArgumentConstants.PLAYER);
        if (targetName == null || targetName.isBlank()) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        final boolean silent = target != null && target.silent();

        this.module.userManager().loadTargetProfile(targetName).thenAcceptAsync(profile -> {
            if (profile == null) {
                this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                return;
            }

            this.module.pardonPlayer(profile, sender, type, silent);
        }, this.module.plugin()::runTask).whenComplete(Utils::printStacktrace);

        return 1;
    }
}
