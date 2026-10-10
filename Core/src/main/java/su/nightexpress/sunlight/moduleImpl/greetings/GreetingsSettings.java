package su.nightexpress.sunlight.moduleImpl.greetings;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.AbstractConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigType;
import su.nightexpress.nightcore.configuration.ConfigTypes;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.Plugins;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.api.provider.dto.GreetingMessageType;
import su.nightexpress.sunlight.moduleImpl.greetings.message.GreetingMessage;

import java.util.*;

import static su.nightexpress.nightcore.util.Placeholders.DEFAULT;
import static su.nightexpress.nightcore.util.Placeholders.PLAYER_DISPLAY_NAME;
import static su.nightexpress.nightcore.util.Placeholders.PLAYER_PREFIX;
import static su.nightexpress.nightcore.util.Placeholders.WILDCARD;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

public class GreetingsSettings extends AbstractConfig {

    private static final ConfigType<GreetingMessage> MESSAGE_CONFIG_TYPE = ConfigType.of(
            GreetingMessage::read,
            FileConfig::set);

    private final ConfigProperty<Map<String, GreetingMessage>> joinMessages = this.addProperty(
            ConfigTypes.forMapWithLowerKeys(MESSAGE_CONFIG_TYPE),
            "Messages.Join",
            getDefaultJoins(),
            "Create custom join messages here.",
            "[>] Text Formations: " + SLPlaceholders.URL_WIKI_TEXT,
            "[>] Placeholders to use in messages:",
            "- " + CommonPlaceholders.PLAYER_NAME + " -> Player name.",
            "- " + CommonPlaceholders.PLAYER_DISPLAY_NAME + " -> Player display (custom) name.",
            "- " + CommonPlaceholders.PLAYER_PREFIX + " -> Player prefix (from permissions plugin).",
            "- " + CommonPlaceholders.PLAYER_SUFFIX + " -> Player name (from permissions plugin).",
            "- " + Plugins.PLACEHOLDER_API);

    private final ConfigProperty<Map<String, GreetingMessage>> quitMessages = this.addProperty(
            ConfigTypes.forMapWithLowerKeys(MESSAGE_CONFIG_TYPE),
            "Messages.Quit",
            getDefaultQuits(),
            "Create custom quit messages here.",
            "[>] Text Formations: " + SLPlaceholders.URL_WIKI_TEXT,
            "[>] Placeholders to use in messages:",
            "- " + CommonPlaceholders.PLAYER_NAME + " -> Player name.",
            "- " + CommonPlaceholders.PLAYER_DISPLAY_NAME + " -> Player display (custom) name.",
            "- " + CommonPlaceholders.PLAYER_PREFIX + " -> Player prefix (from permissions plugin).",
            "- " + CommonPlaceholders.PLAYER_SUFFIX + " -> Player name (from permissions plugin).",
            "- PlaceholderAPI");

    private final ConfigProperty<Map<String, GreetingMessage>> firstJoinMessages = this.addProperty(
            ConfigTypes.forMapWithLowerKeys(MESSAGE_CONFIG_TYPE),
            "Messages.First_Join",
            getDefaultFirstJoins(),
            "Create custom first-join messages here. Shown instead of regular join messages when a player joins for the first time.",
            "[*] Falls back to regular join messages if no first-join message is applicable.",
            "[>] Text Formations: " + SLPlaceholders.URL_WIKI_TEXT,
            "[>] Placeholders to use in messages:",
            "- " + CommonPlaceholders.PLAYER_NAME + " -> Player name.",
            "- " + CommonPlaceholders.PLAYER_DISPLAY_NAME + " -> Player display (custom) name.",
            "- " + CommonPlaceholders.PLAYER_PREFIX + " -> Player prefix (from permissions plugin).",
            "- " + CommonPlaceholders.PLAYER_SUFFIX + " -> Player name (from permissions plugin).",
            "- PlaceholderAPI");

    private final ConfigProperty<Boolean> joinCommandsEnabled = this.addProperty(ConfigTypes.BOOLEAN,
            "JoinCommands.Enabled",
            false,
            "Controls whether commands are executed when a player joins the server.");

    private final ConfigProperty<List<String>> joinCommandsFirst = this.addProperty(ConfigTypes.STRING_LIST,
            "JoinCommands.First-Join",
            Lists.newList("broadcast Welcome new player: " + SLPlaceholders.PLAYER_NAME + "!"),
            "List of commands to execute when a player joins the server for the first time.",
            "Use '" + Players.PLAYER_COMMAND_PREFIX + "' prefix to run command by a player.",
            "Use '" + SLPlaceholders.PLAYER_NAME + "' for a player name.",
            "You can use " + Plugins.PLACEHOLDER_API + " here.");

    private final ConfigProperty<List<String>> joinCommandsDefault = this.addProperty(ConfigTypes.STRING_LIST,
            "JoinCommands.Default",
            Lists.newList(),
            "List of commands to execute when a player joins the server and has played before.",
            "Use '" + Players.PLAYER_COMMAND_PREFIX + "' prefix to run command by a player.",
            "Use '" + SLPlaceholders.PLAYER_NAME + "' for a player name.",
            "You can use " + Plugins.PLACEHOLDER_API + " here.");

    private static Map<String, GreetingMessage> getDefaultJoins() {
        Map<String, GreetingMessage> map = new HashMap<>();

        map.put(DEFAULT, new GreetingMessage(0,
                GRAY.wrap("[" + GREEN.wrap("+") + "]" + " " + PLAYER_PREFIX + PLAYER_DISPLAY_NAME), Set.of(WILDCARD)));

        return map;
    }

    private static Map<String, GreetingMessage> getDefaultQuits() {
        Map<String, GreetingMessage> map = new HashMap<>();

        map.put(DEFAULT, new GreetingMessage(0,
                GRAY.wrap("[" + RED.wrap("-") + "]" + " " + PLAYER_PREFIX + PLAYER_DISPLAY_NAME), Set.of(WILDCARD)));

        return map;
    }

    private static Map<String, GreetingMessage> getDefaultFirstJoins() {
        Map<String, GreetingMessage> map = new HashMap<>();

        map.put(DEFAULT, new GreetingMessage(0, GRAY.wrap("[" + GREEN.wrap("+") + "]" + " " + PLAYER_PREFIX
                + PLAYER_DISPLAY_NAME + YELLOW.wrap(" joined for the first time!")), Set.of(WILDCARD)));

        return map;
    }

    public Map<String, GreetingMessage> getMessages(GreetingMessageType type) {
        return switch (type) {
            case JOIN -> this.getJoinMessages();
            case FIRST_JOIN -> this.getFirstJoinMessages();
            case QUIT -> this.getQuitMessages();
        };
    }

    public Map<String, GreetingMessage> getJoinMessages() {
        return this.joinMessages.get();
    }

    public Map<String, GreetingMessage> getFirstJoinMessages() {
        return this.firstJoinMessages.get();
    }

    public Map<String, GreetingMessage> getQuitMessages() {
        return this.quitMessages.get();
    }

    public boolean isJoinCommandsEnabled() {
        return this.joinCommandsEnabled.get();
    }

    public List<String> getJoinCommandsFirst() {
        return this.joinCommandsFirst.get();
    }

    public List<String> getJoinCommandsDefault() {
        return this.joinCommandsDefault.get();
    }
}
