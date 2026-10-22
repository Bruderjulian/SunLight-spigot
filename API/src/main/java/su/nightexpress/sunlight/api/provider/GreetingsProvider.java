package su.nightexpress.sunlight.api.provider;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import su.nightexpress.nightcore.util.bridge.wrapper.NightComponent;
import su.nightexpress.sunlight.api.provider.dto.GreetingMessageType;

/**
 * View of the Greetings module's message pools. Obtained via
 * {@code SunLightPlugin#greetingsProvider()}, which returns an empty {@link java.util.Optional} when
 * the module is disabled, so callers must treat this as a soft dependency.
 * <p>
 * Selection here is deterministic — highest applicable priority, not random — so asking twice for
 * the same player gives the same answer, and it matches what the module itself would have sent.
 * Nothing is sent as a side effect; the module does that on join and quit.
 * <p>
 * Messages come back as {@link NightComponent}, already resolved for the given player and parsed,
 * so their formatting is intact. Send them with a {@code CommandSender}, or convert with
 * {@code toLegacy()} / {@code toJson()}.
 */
public interface GreetingsProvider {

    /** How many messages are configured for that type, including ones no rank can match. */
    int getMessageCount(@NotNull GreetingMessageType type);

    /** Whether any configured message of that type applies to that player. */
    boolean hasMessage(@NotNull Player player, @NotNull GreetingMessageType type);

    /**
     * The message the module would pick for that player of that type, resolved for them and parsed.
     *
     * @return {@code null} when no configured message applies to that player
     */
    @Nullable NightComponent getMessage(@NotNull Player player, @NotNull GreetingMessageType type);

    /** As {@link #getMessage(Player, GreetingMessageType)} for {@link GreetingMessageType#JOIN}. */
    @Nullable NightComponent getJoinMessage(@NotNull Player player);

    /**
     * As {@link #getMessage(Player, GreetingMessageType)} for
     * {@link GreetingMessageType#FIRST_JOIN}, which only applies to a player who has never joined
     * before.
     */
    @Nullable NightComponent getFirstJoinMessage(@NotNull Player player);

    /** As {@link #getMessage(Player, GreetingMessageType)} for {@link GreetingMessageType#QUIT}. */
    @Nullable NightComponent getQuitMessage(@NotNull Player player);
}