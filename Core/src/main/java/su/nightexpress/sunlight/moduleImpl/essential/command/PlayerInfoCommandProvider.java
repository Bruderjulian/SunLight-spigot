package su.nightexpress.sunlight.moduleImpl.essential.command;


import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.BooleanLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.EntityUtil;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.placeholder.Replacer;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPlaceholders;

import java.net.InetAddress;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.GREEN;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.RED;
import static su.nightexpress.sunlight.SLPlaceholders.forLocation;
import static su.nightexpress.sunlight.SLPlaceholders.forPlayerWithPAPI;

public class PlayerInfoCommandProvider extends CommandProvider<EssentialModule> {

    private static final String COMMAND_PLAYER_INFO = "playerinfo";

    private static final String PERMISSION = EssentialPerms.COMMAND + ".playerinfo";

    private static final TextLocale DESCRIPTION = LangEntry.builder("Command.PlayerInfo.Desc").text(
            "Show player info.");

    private static final BooleanLocale STATUS = LangEntry.builder("Command.PlayerInfo.Status").bool(GREEN.wrap(
            "Online"), RED.wrap("Offline"));

    public PlayerInfoCommandProvider(final EssentialModule module) {
        super(module, "playerinfo");
    }

    @Override
    public void setup() {
        this.register(COMMAND_PLAYER_INFO, command -> command
                .withFullDescription(DESCRIPTION.text())
                .withPermission(PERMISSION)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.showPlayerInfo(sender, arguments);
                })).aliases("seen", "whois");
    }

    private int showPlayerInfo(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        target.runAs(this.module, sender, PERMISSION, (user, player) -> {
            final Location location = player.getLocation();
            final Replacer replacer = Replacer.create()
                    .replace(forPlayerWithPAPI(player))
                    .replace(forLocation(location))
                    .replace(EssentialPlaceholders.PLAYER_STATUS, () -> STATUS.get(user.isOnline()))
                    .replace(EssentialPlaceholders.PLAYER_INET_ADDRESS, () -> user.getLatestAddress()
                            .map(InetAddress::getHostAddress)
                            .orElse("null"))
                    .replace(EssentialPlaceholders.PLAYER_LAST_SEEN, () -> TimeFormats.formatSince(
                            user.getLastOnline(),
                            TimeFormatType.LITERAL))
                    .replace(EssentialPlaceholders.PLAYER_LEVEL, () -> NumberUtil.format(player.getLevel()))
                    .replace(EssentialPlaceholders.PLAYER_CAN_FLY,
                            () -> CoreLang.STATE_YES_NO.get(player.getAllowFlight()))
                    .replace(EssentialPlaceholders.PLAYER_FOOD_LEVEL, () -> NumberUtil.format(player.getFoodLevel()))
                    .replace(EssentialPlaceholders.PLAYER_SATURATION, () -> NumberUtil.format(player.getSaturation()))
                    .replace(EssentialPlaceholders.PLAYER_MAX_HEALTH, () -> NumberUtil.format(EntityUtil
                            .getAttributeValue(player,
                                    Attribute.MAX_HEALTH)))
                    .replace(EssentialPlaceholders.PLAYER_HEALTH, () -> NumberUtil.format(player.getHealth()))
                    .replace(EssentialPlaceholders.PLAYER_GAME_MODE,
                            () -> Lang.GAME_MODE.getLocalized(player.getGameMode()))
                    .replace(EssentialPlaceholders.PLAYER_VANISHED,
                            () -> CoreLang.STATE_YES_NO.get(this.module.plugin().vanishProvider()
                                    .map(provider -> provider.isVanished(player))
                                    .orElse(false)));

            final String text = String.join("\n",
                    replacer.apply(this.module.settings().playerInfoFormat.get()));
            Players.sendMessage(sender, text);
        });
        return 1;
    }
}