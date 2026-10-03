package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.Replacer;
import su.nightexpress.sunlight.SLUtils;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;
import su.nightexpress.sunlight.utils.Direction;
import su.nightexpress.sunlight.utils.Utils;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;

public class NearCommandProvider extends CommandProvider<EssentialModule> {

    private static final String COMMAND_NEAR = "near";

    private static final Permission PERMISSION_COMMAND = EssentialPerms.COMMAND.permission("near");
    private static final Permission PERMISSION_OTHERS = EssentialPerms.COMMAND.permission("near.others");
    private static final Permission PERMISSION_EXCLUDE = EssentialPerms.COMMAND.permission("near.exclude");

    private static final TextLocale DESCRIPTION = LangEntry.builder("Command.Near.Desc").text("Show nearest players.");

    private static final MessageLocale MESSAGE_NOTHING_NOTIFY = LangEntry.builder("Command.Near.Nothing.Notify")
            .chatMessage(
                    GRAY.wrap("There are no players in a " + ORANGE.wrap(GENERIC_RADIUS) + " block radius."));

    private static final MessageLocale MESSAGE_NOTHING_FEEDBACK = LangEntry.builder("Command.Near.Nothing.Feedback")
            .chatMessage(
                    GRAY.wrap("There are no players around " + WHITE.wrap(PLAYER_DISPLAY_NAME) + " in a " + ORANGE.wrap(
                            GENERIC_RADIUS) + " block radius."));

    public NearCommandProvider(final EssentialModule module) {
        super(module, "near");
    }

    private record NearbyPlayer(Player player, int distance, Direction direction) {
    }

    @Override
    public void setup() {
        this.register(COMMAND_NEAR, List.of(), command -> command
                .withRequirement(sender -> sender instanceof Player)
                .withFullDescription(DESCRIPTION.text())
                .withPermission(PERMISSION_COMMAND.getName())
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.showNearbyPlayers(sender, arguments);
                }));
    }

    private String formatEntry(final NearbyPlayer nearby) {
        return Replacer.create()
                .replace(forPlayerWithPAPI(nearby.player()))
                .replace(GENERIC_DISTANCE, () -> NumberUtil.format(nearby.distance()))
                .replace(GENERIC_DIRECTION, () -> this.getDirectionText(nearby.direction()))
                .apply(this.module.settings().nearEntryFormat.get());
    }

    private String getDirectionText(final Direction direction) {
        if (this.module.settings().nearUseArrows.get()) {
            final String arrow = this.module.settings().nearDirectionArrows.get()
                    .get(Utils.lowercase(direction.name()));
            if (arrow != null) {
                return arrow;
            }
        }
        return Lang.DIRECTION.getLocalized(direction);
    }

    private int showNearbyPlayers(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, PERMISSION_OTHERS.getName(), (user, source) -> {
            final Player executor = sender instanceof final Player player ? player : null;
            final List<NearbyPlayer> nearbyPlayers = new ArrayList<>();

            final World sourceWorld = source.getWorld();
            final Location sourceLocation = source.getLocation();

            final int radius = this.module.settings().nearRadius.get();
            final int distanceLookup = radius * radius;
            final boolean isOthers = sender != source;

            Utils.onlinePlayers().forEach(other -> {
                if (other == source) {
                    return;
                }
                if (other.getWorld() != sourceWorld) {
                    return;
                }
                if (other.hasPermission(PERMISSION_EXCLUDE.getName())) {
                    return;
                }
                if (executor != null && !executor.canSee(other)) {
                    return;
                }

                final Location location = other.getLocation();
                if (location == null) {
                    return;
                }

                final int delta = (int) location.distanceSquared(sourceLocation);
                if (delta > distanceLookup) {
                    return;
                }

                final Direction direction = SLUtils.getDirection(sourceLocation, location);

                final int distance = (int) Math.sqrt(delta);

                nearbyPlayers.add(new NearbyPlayer(other, distance, direction));
            });

            if (nearbyPlayers.isEmpty()) {
                this.module.sendPrefixed(isOthers ? MESSAGE_NOTHING_FEEDBACK : MESSAGE_NOTHING_NOTIFY, sender,
                        replacer -> replacer
                                .with(GENERIC_RADIUS, () -> String.valueOf(radius))
                                .with(CommonPlaceholders.PLAYER.resolver(source)));
                return;
            }

            final String entries = nearbyPlayers.stream()
                    .sorted(Comparator.comparingDouble(NearbyPlayer::distance))
                    .map(this::formatEntry)
                    .collect(Collectors.joining(BR));

            final String text = String.join("\n", Replacer.create()
                    .replace(GENERIC_AMOUNT, () -> String.valueOf(nearbyPlayers.size()))
                    .replace(GENERIC_RADIUS, () -> String.valueOf(radius))
                    .replace(GENERIC_ENTRY, entries)
                    .replace(forPlayerWithPAPI(source))
                    .apply(isOthers ? this.module.settings().nearFormatOthers.get()
                            : this.module.settings().nearFormatNormal.get()));

            Players.sendMessage(sender, text);
        });
    }
}