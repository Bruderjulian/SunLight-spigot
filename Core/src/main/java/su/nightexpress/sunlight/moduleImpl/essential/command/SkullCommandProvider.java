package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.permissions.Permission;
import su.nightexpress.nightcore.bridge.wrap.NightProfile;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.ItemUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.bridge.Software;
import su.nightexpress.nightcore.util.profile.CachedProfile;
import su.nightexpress.nightcore.util.profile.PlayerProfiles;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;
import su.nightexpress.sunlight.utils.Utils;

import java.util.Base64;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.PLAYER_NAME;

public class SkullCommandProvider extends CommandProvider<EssentialModule> {

    private static final String COMMAND_SKULL = "skull";

    private static final Permission PERMISSION = EssentialPerms.COMMAND.permission("skull");
    private static final Permission PERMISSION_OTHERS = EssentialPerms.COMMAND.permission("skull.others");
    private static final Permission PERMISSION_CUSTOM = EssentialPerms.COMMAND.permission("skull.custom");

    private static final TextLocale DESCRIPTION = LangEntry.builder("Command.Skull.Custom.Desc")
            .text("Get player's head.");

    private static final MessageLocale MESSAGE_GET_OWN_NOTIFY = LangEntry.builder("Command.Skull.GetOwn").chatMessage(
            GRAY.wrap("You have got a copy of your head."));

    private static final MessageLocale MESSAGE_GET_OTHERS_NOTIFY = LangEntry.builder("Command.Skull.GetOther")
            .chatMessage(
                    GRAY.wrap("You have got " + SOFT_YELLOW.wrap(PLAYER_NAME) + "'s head."));

    private static final MessageLocale MESSAGE_GET_CUSTOM_NOTIFY = LangEntry.builder("Command.Skull.GetCustom")
            .chatMessage(
                    GRAY.wrap("You have got custom " + SOFT_YELLOW.wrap(PLAYER_NAME) + " head."));

    private static final MessageLocale MESSAGE_INVALID_SKULL_DATA = LangEntry.builder("Command.Skull.InvalidData")
            .chatMessage(
                    SOFT_RED.wrap("Invalid skull data provided! You must provide either: "
                            + SOFT_YELLOW.wrap("Player name") + ", a " + SOFT_YELLOW.wrap("URL") + " or "
                            + SOFT_YELLOW.wrap("Base 64") + " value."));

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final Pattern URL_VALUE_PATTERN = Pattern.compile("^[0-9a-fA-F]{64}$");
    private static final Pattern BASE_64_PATTERN = Pattern.compile("^[A-Za-z0-9+/=]{180}$");

    public SkullCommandProvider(final EssentialModule module) {
        super(module, "skull");
    }

    @Override
    public void setup() {
        this.register(COMMAND_SKULL, List.of(), command -> command
                .withRequirement(sender -> sender instanceof Player)
                .withFullDescription(DESCRIPTION.text())
                .withPermission(PERMISSION.getName())
                .withOptionalArguments(CommandArgumentConstants.string(CommandArgumentConstants.VALUE,
                        info -> CommandArgumentConstants.onlinePlayerNames()))
                .executes((sender, arguments) -> {
                    return this.createSkull(sender, arguments);
                }));
    }

    private int createSkull(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }

        final NightProfile profile;
        final MessageLocale locale;

        final Object valueArg = arguments.get(CommandArgumentConstants.VALUE);
        if (valueArg instanceof final String raw && !raw.isBlank()) {

            String input = raw;

            if (BASE_64_PATTERN.matcher(input).matches()) {
                if (!player.hasPermission(PERMISSION_CUSTOM.getName())) {
                    this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                    return 0;
                }

                final String decoded = new String(Base64.getDecoder().decode(input));
                final JsonObject jsonObject = JsonParser.parseString(decoded).getAsJsonObject();

                final String url = Optional.ofNullable(jsonObject)
                        .flatMap(obj -> Optional.ofNullable(obj.getAsJsonObject("textures")))
                        .flatMap(textures -> Optional.ofNullable(textures.getAsJsonObject("SKIN")))
                        .flatMap(skin -> Optional.ofNullable(skin.get("url")))
                        .map(JsonElement::getAsString)
                        .orElse(null);

                if (url == null) {
                    MESSAGE_INVALID_SKULL_DATA.message().send(sender);
                    return 0;
                }

                input = url.substring(url.lastIndexOf("/") + 1);
            }

            if (URL_VALUE_PATTERN.matcher(input).matches()) {
                if (!player.hasPermission(PERMISSION_CUSTOM.getName())) {
                    this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                    return 0;
                }

                profile = Optional.ofNullable(PlayerProfiles.createProfileBySkinURL(input))
                        .map(CachedProfile::queryNoUpdate).orElse(null);
                locale = MESSAGE_GET_CUSTOM_NOTIFY;
            } else if (input.length() <= 16 && NAME_PATTERN.matcher(input).matches()) {
                profile = Software.get().createProfile(null, input);
                locale = MESSAGE_GET_OTHERS_NOTIFY;
            } else {
                MESSAGE_INVALID_SKULL_DATA.message().send(sender);
                return 0;
            }
        } else {
            profile = PlayerProfiles.getProfile(player).query();
            locale = MESSAGE_GET_OWN_NOTIFY;
        }

        if (profile == null) {
            return 0;
        }

        final NightProfile finalProfile = profile;
        final MessageLocale finalLocale = locale;
        finalProfile.update().thenAcceptAsync(updated -> {
            final ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
            ItemUtil.editMeta(itemStack, SkullMeta.class, finalProfile::apply);
            Players.addItem(player, itemStack);
            this.module.sendPrefixed(finalLocale, player,
                    builder -> builder.with(PLAYER_NAME, () -> String.valueOf(updated.getName())));

        }, this.module.plugin()::runTask).whenComplete(Utils::printStacktrace);

        return 1;
    }
}