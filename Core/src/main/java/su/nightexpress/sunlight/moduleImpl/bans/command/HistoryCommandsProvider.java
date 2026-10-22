package su.nightexpress.sunlight.moduleImpl.bans.command;


import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.bans.BansModule;
import su.nightexpress.sunlight.moduleImpl.bans.config.BansLang;
import su.nightexpress.sunlight.moduleImpl.bans.config.BansPerms;
import su.nightexpress.sunlight.moduleImpl.bans.punishment.PlayerPunishment;
import su.nightexpress.sunlight.moduleImpl.bans.punishment.PunishmentType;
import su.nightexpress.sunlight.utils.Utils;

public class HistoryCommandsProvider extends CommandProvider<BansModule> {

    public HistoryCommandsProvider(BansModule module) {
        super(module, "bans-history");
    }

    @Override
    public void setup() {
        this.register("banhistory",
                builder -> this.builder(builder, PunishmentType.BAN));
        this.register("mutehistory",
                builder -> this.builder(builder, PunishmentType.MUTE));
        this.register("warnhistory",
                builder -> this.builder(builder, PunishmentType.WARN));
    }

    private void builder(dev.jorel.commandapi.CommandAPICommand builder, PunishmentType type) {
        String description = switch (type) {
            case BAN -> BansLang.COMMAND_BAN_HISTORY_DESC.text();
            case MUTE -> BansLang.COMMAND_MUTE_HISTORY_DESC.text();
            case WARN -> BansLang.COMMAND_WARN_HISTORY_DESC.text();
        };

        String permission = switch (type) {
            case BAN -> BansPerms.COMMAND_BAN_HISTORY;
            case MUTE -> BansPerms.COMMAND_MUTE_HISTORY;
            case WARN -> BansPerms.COMMAND_WARN_HISTORY;
        };

        builder
                .withFullDescription(description)
                .withPermission(permission)
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> this.module.getPlayerPunishments(type).stream()
                                .map(PlayerPunishment::getPlayerName).toList()))
                .executes((sender, arguments) -> {
                    return this.showHistory(sender, arguments, type);
                });
    }

    private int showHistory(CommandSender sender, CommandArguments arguments, PunishmentType type) {
        if (!(sender instanceof Player viewer)) {
            return 0;
        }
        final String targetName = (String) arguments.get(CommandArgumentConstants.PLAYER);
        if (targetName == null || targetName.isBlank()) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        this.module.userManager().loadTargetProfile(targetName).thenAcceptAsync(profile -> {
            if (profile == null) {
                this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                return;
            }

            this.module.openHistory(viewer, profile, type);
        }, this.module.plugin()::runTask).whenComplete(Utils::printStacktrace);

        return 1;
    }
}
