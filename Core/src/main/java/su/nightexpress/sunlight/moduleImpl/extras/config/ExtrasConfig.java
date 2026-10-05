package su.nightexpress.sunlight.moduleImpl.extras.config;

import org.bukkit.Material;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.util.*;
import su.nightexpress.sunlight.SLPlaceholders;

import java.util.List;

public class ExtrasConfig {

    public static final ConfigValue<Boolean> KEEP_INVENTORY_ENABLED = ConfigValue.create("KeepInventory.Enabled",
        true,
        "Sets whether or not Keep Inventory feature is enabled.");

    public static final ConfigValue<List<String>> KEEP_INVENTORY_ITEMS_RANKS = ConfigValue.create("KeepInventory.Items_Ranks",
        Lists.newList("vip", "admin"),
        "Players from the following permission groups will keep their items on death.");

    public static final ConfigValue<List<String>> KEEP_INVENTORY_XP_RANKS = ConfigValue.create("KeepInventory.Exp_Ranks",
        Lists.newList("vip", "admin"),
        "Players from the following permission groups will keep their XP on death.");

    public static final ConfigValue<Boolean> JOIN_COMMANDS_ENABLED = ConfigValue.create("JoinCommands.Enabled",
        false,
        "Sets whether or not Join Commands feature is enabled.");

    public static final ConfigValue<List<String>> JOIN_COMMANDS_FIRST = ConfigValue.create("JoinCommands.FirstJoin",
        Lists.newList("broadcast Welcome new player: " + SLPlaceholders.PLAYER_NAME + "!"),
        "List of commands to execute when player joined server for the first time.",
        "Use '" + Players.PLAYER_COMMAND_PREFIX + "' prefix to run command by a player.",
        "Use '" + SLPlaceholders.PLAYER_NAME + "' for a player name.",
        "You can use " + Plugins.PLACEHOLDER_API + " here.");

    public static final ConfigValue<List<String>> JOIN_COMMANDS_DEFAULT = ConfigValue.create("JoinCommands.Default",
        Lists.newList(),
        "List of commands to execute when player joins the server and has played before.",
        "Use '" + Players.PLAYER_COMMAND_PREFIX + "' prefix to run command by a player.",
        "Use '" + SLPlaceholders.PLAYER_NAME + "' for a player name.",
        "You can use " + Plugins.PLACEHOLDER_API + " here.");

    public static final ConfigValue<Boolean> ANVIL_COLORS_ENABLED = ConfigValue.create("Anvil_Colors.Enabled",
        true,
        "Sets whether or not Anvil Colors feature is enabled.",
        "Players with '" + ExtrasPerms.ANVILS_COLOR + "' permission will be able to use colors on anvils.");

    public static final ConfigValue<Boolean> SIGN_COLORS_ENABLED = ConfigValue.create("Sign_Colors.Enabled",
        false,
        "Sets whether or not Sign Colors feature is enabled.",
        "Players with '" + ExtrasPerms.SIGNS_COLOR + "' permission will be able to use colors on signs.");

    public static final ConfigValue<Boolean> PHYSIC_EXPLOSIONS_ENABLED = ConfigValue.create("Physic_Explosions.Enabled",
        true,
        "Sets whether or not Physic Explosions feature is enabled.",
        "This will make blocks flying away on explosions.");

    public static final ConfigValue<Boolean> ANTI_FARM_ENDERMITE_MINECART = ConfigValue.create("AntiFarm.Endermite_Minecart",
        false,
        "Sets whether or not Enderman farms using minecart with endermite will be disabled.");

    public static final ConfigValue<Boolean> ANTI_FARM_AUTO_FISHING = ConfigValue.create("AntiFarm.AutoFishing",
        false,
        "Sets whether or not Auto Fishing farms using note blocks (and not only them) will be disabled.");
}
