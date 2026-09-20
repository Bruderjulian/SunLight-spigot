package su.nightexpress.sunlight.moduleImpl.afk.core;

import org.bukkit.Sound;

import su.nightexpress.nightcore.bridge.wrap.NightSound;
import su.nightexpress.nightcore.configuration.AbstractConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigTypes;
import su.nightexpress.nightcore.util.*;
import su.nightexpress.nightcore.util.sound.VanillaSound;
import su.nightexpress.sunlight.SLConfigTypes;
import su.nightexpress.sunlight.moduleImpl.afk.ActivityType;

import java.util.*;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;

public class AfkSettings extends AbstractConfig {

        public final ConfigProperty<Integer> wakeUpThreshold = this.addProperty(ConfigTypes.INT, "WakeUp.Threshold", 3,
                        "Sets the activity threshold that a player must reach to lose the AFK mode.");

        public final ConfigProperty<Integer> wakeUpTimer = this.addProperty(ConfigTypes.INT, "WakeUp.Timeout", 3,
                        "The duration (in seconds) a player has to meet the activity threshold to leave AFK mode.",
                        "If the player does not meet the threshold within this time, the timer and activity points reset, and they remain AFK until the next cycle.");

        public final ConfigProperty<Map<ActivityType, Integer>> wakeUpActivityPoints = this.addProperty(
                        ConfigTypes.forMap(str -> Enums.get(str, ActivityType.class), Enum::name, ConfigTypes.INT),
                        "WakeUp.Activity-Points",
                        Map.ofEntries(
                                        Map.entry(ActivityType.MOVEMENT, 1),
                                        Map.entry(ActivityType.ROTATION, 1),
                                        Map.entry(ActivityType.CHAT, 5),
                                        Map.entry(ActivityType.COMMAND, 3),
                                        Map.entry(ActivityType.INTERACT, 1),
                                        Map.entry(ActivityType.BLOCK_BREAK, 2),
                                        Map.entry(ActivityType.BLOCK_PLACE, 2),
                                        Map.entry(ActivityType.ITEM_PICKUP, 1),
                                        Map.entry(ActivityType.CONTAINER, 2),
                                        Map.entry(ActivityType.FISH, 2),
                                        Map.entry(ActivityType.VEHICLE, 1)),
                        "Sets how much activity points produced by certain player actions.");

        public final ConfigProperty<List<String>> ignoredCommands = this.addProperty(ConfigTypes.STRING_LIST,
                        "WakeUp.Ignored-Commands",
                        List.of(),
                        "List of commands (with or without leading '/') that will NOT count as activity points.",
                        "This is useful to ignore 'social' commands like '/msg' that players use to stay online while not actually playing.",
                        "Use '*' at the end to ignore all commands that start with the given prefix, e.g. 'msg*'.");

        public final ConfigProperty<Boolean> movementTrackRotation = this.addProperty(ConfigTypes.BOOLEAN,
                        "WakeUp.Track-Rotation",
                        false,
                        "Controls whether rotating the view (head turning) without moving counts as activity.",
                        "When 'false', players must actually change their block position to produce movement-based activity points, so spinning in place will NOT keep them awake.");

        public final ConfigProperty<List<String>> wakeUpCommands = this.addProperty(ConfigTypes.STRING_LIST,
                        "WakeUp.Commands",
                        List.of(),
                        "The following commands will be dispatched right before player leave AFK mode.",
                        "[*] Placeholders: '%s', %s".formatted(PLAYER_NAME, Plugins.PLACEHOLDER_API));

        public final ConfigProperty<Boolean> afkStatusBarEnabled = this.addProperty(ConfigTypes.BOOLEAN,
                        "AFK.Status-Bar.Enabled",
                        true,
                        "Controls whether AFK status bar is enabled for AFK players.");

        public final ConfigProperty<String> afkStatusBarText = this.addProperty(ConfigTypes.STRING,
                        "AFK.Status-Bar.Text",
                        DARK_GRAY.wrap("[" + ORANGE.wrap("AFK") + "]") + " " + GRAY.wrap("You're being AFK for ")
                                        + ORANGE.wrap(GENERIC_TIME),
                        "Set AFK status bar message for AFK players.",
                        "[*] Placeholders: '%s', %s".formatted(GENERIC_TIME, Plugins.PLACEHOLDER_API));

        public final ConfigProperty<Boolean> afkWakeUpBarEnabled = this.addProperty(ConfigTypes.BOOLEAN,
                        "AFK.Status-Bar.Wake-Up-Enabled",
                        true,
                        "Controls whether a separate status bar is shown while the player is waking up from AFK.");

        public final ConfigProperty<String> afkWakeUpBarText = this.addProperty(ConfigTypes.STRING,
                        "AFK.Status-Bar.Wake-Up-Text",
                        GREEN.wrap("[" + GRAY.wrap("Waking up") + "]") + " " + GREEN.wrap(GENERIC_REMAIN)
                                        + GRAY.wrap(" point(s) left - ") + ORANGE.wrap(GENERIC_TIME_LEFT),
                        "Set the status bar message shown while the player is waking up from AFK.",
                        "[*] Placeholders: '%s' (remaining points), '%s' (time left to meet the threshold), %s"
                                        .formatted(GENERIC_REMAIN, GENERIC_TIME_LEFT, Plugins.PLACEHOLDER_API));

        public final ConfigProperty<Integer> idleThreshold = this.addProperty(ConfigTypes.INT, "AFK.Cooldown",
                        60,
                        "The cooldown period (in seconds) after a player leaves AFK mode.",
                        "During this time, the player will be immune to being marked as inactive, even if they produce no activity.");

        public final ConfigProperty<Integer> rejoinKickAfter = this.addProperty(ConfigTypes.INT, "AFK.Kick.Rejoin-Window",
                        0,
                        "Sets the time window (in seconds) after a player was kicked for idling, during which they will be kicked again immediately upon joining the server.",
                        "Use '-1' to always re-kick until the server restarts, and '0' to disable this feature.");

        public final ConfigProperty<Integer> kickWarningAhead = this.addProperty(ConfigTypes.INT, "AFK.Kick.Warning-Ahead",
                        30,
                        "Sets how many seconds before the idle-kick the player will be warned about the upcoming kick.",
                        "Use '0' to disable kick warnings.");

        public final ConfigProperty<Integer> kickWarningInterval = this.addProperty(ConfigTypes.INT,
                        "AFK.Kick.Warning-Interval",
                        5,
                        "Sets how often (in seconds) the kick warning can be repeated.");

        public final ConfigProperty<RankTable> idleAfkTimes = this.addProperty(SLConfigTypes.RANK_TABLE,
                        "AFK.Idle_Time",
                        RankTable.builder(RankTable.Mode.RANK, 600)
                                        .addRankValue("vip", 900)
                                        .addRankValue("admin", -1)
                                        .build(),
                        "Here you can set the inactivity time (in seconds) after which a player will be placed into AFK mode.",
                        "You can set different times for different ranks or permissions (read comments).",
                        "If multiple times are available, the greatest (or negative) one will be used.",
                        "Use '-1' to make players immune to the auto-AFK mode.");

        public final ConfigProperty<RankTable> idleKickTimes = this.addProperty(SLConfigTypes.RANK_TABLE,
                        "AFK.Kick_Time",
                        RankTable.builder(RankTable.Mode.RANK, 1200)
                                        .addRankValue("vip", -1)
                                        .addRankValue("admin", -1)
                                        .build(),
                        "Here you can set the total inactivity time (in seconds) after which a player will be kicked from the server.",
                        "[*] IMPORTANT: This value sets the TOTAL time (Time before AFK + Time after AFK), not just the time starting from when the player entered AFK mode.",
                        "If multiple times are available, the greatest (or negative) one will be used.",
                        "Use '-1' to make players immune from being kicked due to AFK.");

        public final ConfigProperty<RankTable> afkKickTimes = this.addProperty(SLConfigTypes.RANK_TABLE,
                        "AFK.Afk_Kick_Time",
                        RankTable.builder(RankTable.Mode.RANK, -1).build(),
                        "Here you can set the time (in seconds) a player may stay in AFK mode before being kicked.",
                        "This value counts only the time the player spends in AFK, regardless of the total idle time.",
                        "If multiple times are available, the greatest (or negative) one will be used.",
                        "Use '-1' to never kick the player for staying in AFK mode (auto-AFK still may kick due to the total idle time).");

        public final ConfigProperty<List<String>> kickText = this.addProperty(ConfigTypes.STRING_LIST,
                        "AFK.Kick_Message",
                        List.of(
                                        SOFT_RED.wrap("You have been kicked for being AFK too long: "
                                                        + SOFT_YELLOW.wrap(GENERIC_TIME)),
                                        "",
                                        GREEN.and(UNDERLINED).wrap("You can join back now.")),
                        "Kick message for players kicked for being AFK long enough.",
                        "[*] Placeholders: '%s', %s".formatted(GENERIC_TIME, Plugins.PLACEHOLDER_API));

        public final ConfigProperty<List<String>> afkCommands = this.addProperty(ConfigTypes.STRING_LIST,
                        "AFK.Commands",
                        List.of(),
                        "The following commands will be dispatched right before player enter AFK mode.",
                        "[*] Placeholders: '%s', %s".formatted(PLAYER_NAME, Plugins.PLACEHOLDER_API));

        public final ConfigProperty<Set<String>> exemptWorlds = this.addProperty(ConfigTypes.STRING_SET,
                        "AFK.Exempt-Worlds",
                        Set.of(),
                        "Player in these worlds will never be marked as AFK and never kicked for idling.");

        public final ConfigProperty<Set<String>> exemptRegions = this.addProperty(ConfigTypes.STRING_SET,
                        "AFK.Exempt-Regions",
                        Set.of(),
                        "Players standing inside these WorldGuard regions will never be marked as AFK and never kicked for idling.");

        public final ConfigProperty<Boolean> blockMovement = this.addProperty(ConfigTypes.BOOLEAN,
                        "AFK.Block-Actions.Movement",
                        false,
                        "Cancels movement of AFK players. Note that with this option enabled the player cannot wake up by moving; wake up must come from chat, commands or other activity.");

        public final ConfigProperty<Boolean> blockBlockBreak = this.addProperty(ConfigTypes.BOOLEAN,
                        "AFK.Block-Actions.Block-Break",
                        true,
                        "Cancels block breaking of AFK players.");

        public final ConfigProperty<Boolean> blockBlockPlace = this.addProperty(ConfigTypes.BOOLEAN,
                        "AFK.Block-Actions.Block-Place",
                        true,
                        "Cancels block placing of AFK players.");

        public final ConfigProperty<Boolean> blockInteract = this.addProperty(ConfigTypes.BOOLEAN,
                        "AFK.Block-Actions.Interact",
                        true,
                        "Cancels interactions (clicking blocks and entities) of AFK players.");

        public final ConfigProperty<Boolean> blockInventory = this.addProperty(ConfigTypes.BOOLEAN,
                        "AFK.Block-Actions.Inventory",
                        true,
                        "Cancels inventory clicks of AFK players.");

        public final ConfigProperty<Boolean> blockItemDrop = this.addProperty(ConfigTypes.BOOLEAN,
                        "AFK.Block-Actions.Item-Drop",
                        true,
                        "Cancels item dropping of AFK players.");

        public final ConfigProperty<Boolean> blockItemPickup = this.addProperty(ConfigTypes.BOOLEAN,
                        "AFK.Block-Actions.Item-Pickup",
                        true,
                        "Cancels item pickup of AFK players.");

        public final ConfigProperty<Boolean> blockChat = this.addProperty(ConfigTypes.BOOLEAN,
                        "AFK.Block-Actions.Chat",
                        false,
                        "Cancels chatting of AFK players.");

        public final ConfigProperty<Boolean> blockCommands = this.addProperty(ConfigTypes.BOOLEAN,
                        "AFK.Block-Actions.Commands",
                        false,
                        "Cancels command usage of AFK players.");

        public final ConfigProperty<Boolean> blockDamageIncoming = this.addProperty(ConfigTypes.BOOLEAN,
                        "AFK.Block-Actions.Damage-Incoming",
                        true,
                        "Cancels incoming damage on AFK players.");

        public final ConfigProperty<Boolean> blockDamageOutgoing = this.addProperty(ConfigTypes.BOOLEAN,
                        "AFK.Block-Actions.Damage-Outgoing",
                        true,
                        "Cancels damage dealt by AFK players.");

        public final ConfigProperty<Boolean> blockMobTargeting = this.addProperty(ConfigTypes.BOOLEAN,
                        "AFK.Block-Actions.Mob-Targeting",
                        true,
                        "Prevents mobs from targeting AFK players.");

        public final ConfigProperty<NightSound> soundEnter = this.addProperty(ConfigTypes.NIGHT_SOUND,
                        "Sound.Enter",
                        VanillaSound.of(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f),
                        "Sound played to the player when they enter AFK mode.",
                        "Format: 'sound_name;volume;pitch'. Use volume '0' to mute it.");

        public final ConfigProperty<NightSound> soundExit = this.addProperty(ConfigTypes.NIGHT_SOUND,
                        "Sound.Exit",
                        VanillaSound.of(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.5f),
                        "Sound played to the player when they leave AFK mode.",
                        "Format: 'sound_name;volume;pitch'. Use volume '0' to mute it.");

        public int getActivityPoints(ActivityType type) {
                return this.wakeUpActivityPoints.get().getOrDefault(type, 0);
        }

        public boolean isCommandIgnored(String commandLine) {
                String input = commandLine;
                if (input.startsWith("/")) {
                        input = input.substring(1);
                }
                input = input.trim().toLowerCase(Locale.ROOT);

                for (String ignored : this.ignoredCommands.get()) {
                        String pattern = ignored.startsWith("/") ? ignored.substring(1) : ignored;
                        pattern = pattern.trim().toLowerCase(Locale.ROOT);
                        if (pattern.isEmpty())
                                continue;

                        if (pattern.endsWith("*")) {
                                if (input.startsWith(pattern.substring(0, pattern.length() - 1)))
                                        return true;
                        } else if (input.equals(pattern) || input.startsWith(pattern + " ")) {
                                return true;
                        }
                }
                return false;
        }
}