package su.nightexpress.sunlight.api.provider;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import su.nightexpress.sunlight.api.provider.dto.MailHandle;
import su.nightexpress.sunlight.api.provider.dto.ChatSpyType;

import java.util.List;
import java.util.UUID;

/**
 * View of the Chat module: channels, private messages, stored mail, and per-player chat toggles.
 * Obtained via {@code SunLightPlugin#chatProvider()}, which returns an empty
 * {@link java.util.Optional} when the module is disabled, so callers must treat this as a soft
 * dependency.
 * <p>
 * Most of the module's methods send the usual localized feedback to the players involved, so a
 * caller driving these directly will produce messages in the module's voice rather than its own.
 * There are no silent variants here: filter, format and spy settings are not exposed, because a
 * third-party sender should not be able to route around them.
 * <p>
 * Stored mail needs the module's SQL storage to be enabled; when it is not, mail counts are
 * {@code 0} and sending falls back to a direct private message or is dropped, as the module does.
 * <p>
 * Every type here is a primitive, a JDK type, a Bukkit type, or a type declared in this package,
 * because the API module must not depend on Core. Channels are addressed by id — they are
 * user-defined, so there is no closed set to model as an enum.
 */
public interface ChatProvider {

    /** Ids of every loaded channel, in no particular order. */
    @NotNull List<String> getChannelIds();

    /** The channel with that id, or {@code null} when there is none. Ids are matched case-insensitively. */
    @Nullable String getChannelId(@NotNull String channelId);

    /** The channel a player's message goes to when they type no prefix. */
    @Nullable String getDefaultChannelId();

    /**
     * The channel a player's message would go to, given the prefix they typed.
     *
     * @param prefix the typed prefix, or {@code null} when they typed none
     * @return the channel with that prefix when they may speak there, otherwise the default channel
     */
    @Nullable String getEffectiveChannelId(@NotNull Player player, @Nullable Character prefix);

    /**
     * The ids of the channels that player is allowed to listen in.
     * <p>
     * Named {@code getListenableChannelIds} because the module keeps the name
     * {@code getChannelsAllowedToListen} for its own lookup, which returns live channel objects.
     */
    @NotNull List<String> getListenableChannelIds(@NotNull Player player);

    /**
     * Joins a player to a channel, sending the usual success or permission message.
     *
     * @return {@code false} when there is no such channel, the player may not use it, or they were
     * already in it
     */
    boolean joinChannel(@NotNull Player player, @NotNull String channelId);

    /**
     * Removes a player from a channel, sending the usual success message.
     *
     * @return {@code false} when there is no such channel, or the player was not in it
     */
    boolean leaveChannel(@NotNull Player player, @NotNull String channelId);

    /**
     * Sends a private message, honouring the recipient's conversation opt-out and firing the
     * module's cancellable event.
     *
     * @return {@code false} when sender and recipient are the same, the recipient has opted out,
     * or the event was cancelled
     */
    boolean sendPrivateMessage(@NotNull Player sender, @NotNull Player target, @NotNull String message);

    /**
     * Sends stored mail to a player, resolved from the id. Delivers instantly as a private message
     * when the recipient is online and accepts it, and stores it otherwise, so it is never lost.
     *
     * @return {@code false} when the recipient is unknown to the server
     */
    boolean sendMail(@NotNull Player sender, @NotNull UUID recipientId, @NotNull String message);

    /** Stored mail addressed to that player, oldest first. Empty when mail storage is disabled. */
    @NotNull List<MailHandle> getMails(@NotNull UUID recipientId);

    /**
     * Shows the player's stored mail to them.
     *
     * @return {@code false} when mail storage is disabled
     */
    boolean readMails(@NotNull Player player);

    /** Deletes all stored mail addressed to that player. */
    void clearMails(@NotNull UUID playerId);

    /** How many players are currently spying, for the given kind of traffic. */
    int getSpyCount(@NotNull ChatSpyType spyType);

    /** Whether the player accepts private messages. */
    boolean isConversationsEnabled(@NotNull UUID playerId);

    /** Records the player's private-message preference, persisting the choice. */
    void setConversationsEnabled(@NotNull Player player, boolean enabled);

    /** Whether the player sees a highlight when they are mentioned. */
    boolean isMentionsEnabled(@NotNull UUID playerId);

    /** Records the player's mention preference, persisting the choice. */
    void setMentionsEnabled(@NotNull Player player, boolean enabled);
}