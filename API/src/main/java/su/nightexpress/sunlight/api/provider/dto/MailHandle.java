package su.nightexpress.sunlight.api.provider.dto;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * One stored mail message, as handed out by {@link su.nightexpress.sunlight.api.provider.ChatProvider}.
 * <p>
 * A record of JDK types rather than the module's own model, because the API module must not depend
 * on Core.
 *
 * @param id         the mail's id
 * @param senderId   the sender's id
 * @param senderName the sender's last known name
 * @param message    the message body
 * @param dateCreated epoch milliseconds of delivery
 */
public record MailHandle(@NotNull UUID id,
                         @NotNull UUID senderId,
                         @NotNull String senderName,
                         @NotNull String message,
                         long dateCreated
) {
}