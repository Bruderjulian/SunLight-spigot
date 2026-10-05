package su.nightexpress.sunlight.moduleImpl.bans.command;


import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.bans.BansModule;
import su.nightexpress.sunlight.moduleImpl.bans.config.BansLang;
import su.nightexpress.sunlight.moduleImpl.bans.config.BansPerms;
import su.nightexpress.sunlight.moduleImpl.bans.punishment.PunishmentType;

public class ListCommandsProvider extends CommandProvider<BansModule> {

    public static final String NODE_BAN = "banlist";
    public static final String NODE_MUTE = "mutelist";
    public static final String NODE_WARN = "warnlist";

    public ListCommandsProvider(BansModule module) {
        super(module, "bans-list");
    }

    @Override
    public void setup() {
        this.register("banlist",
                builder -> this.build(builder, PunishmentType.BAN));
        this.register("mutelist",
                builder -> this.build(builder, PunishmentType.MUTE));
        this.register("warnlist",
                builder -> this.build(builder, PunishmentType.WARN));
    }

    private void build(dev.jorel.commandapi.CommandAPICommand builder, PunishmentType type) {
        String description = switch (type) {
            case BAN -> BansLang.COMMAND_BAN_LIST_DESC.text();
            case MUTE -> BansLang.COMMAND_MUTE_LIST_DESC.text();
            case WARN -> BansLang.COMMAND_WARN_LIST_DESC.text();
        };

        String permission = switch (type) {
            case BAN -> BansPerms.COMMAND_BAN_LIST;
            case MUTE -> BansPerms.COMMAND_MUTE_LIST;
            case WARN -> BansPerms.COMMAND_WARN_LIST;
        };

        builder
                .withFullDescription(description)
                .withPermission(permission)
                .withRequirement(sender -> sender instanceof Player)
                .executes((sender, arguments) -> {
                    return this.showMenu(sender, arguments, type);
                });
    }

    private int showMenu(CommandSender sender, CommandArguments arguments, PunishmentType type) {
        if (!(sender instanceof Player player)) {
            return 0;
        }
        return this.module.openPunishments(player, type) ? 1 : 0;
    }
}
