package su.nightexpress.sunlight.module.chat.mail;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class MailData {

    private final UUID   id;
    private final UUID   senderId;
    private final String senderName;
    private final UUID   recipientId;
    private final String message;
    private final long   dateCreated;

    public MailData(@NotNull UUID id,
                    @NotNull UUID senderId,
                    @NotNull String senderName,
                    @NotNull UUID recipientId,
                    @NotNull String message,
                    long dateCreated) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.recipientId = recipientId;
        this.message = message;
        this.dateCreated = dateCreated;
    }

    @NotNull
    public UUID getId() {
        return this.id;
    }

    @NotNull
    public UUID getSenderId() {
        return this.senderId;
    }

    @NotNull
    public String getSenderName() {
        return this.senderName;
    }

    @NotNull
    public UUID getRecipientId() {
        return this.recipientId;
    }

    @NotNull
    public String getMessage() {
        return this.message;
    }

    public long getDateCreated() {
        return this.dateCreated;
    }
}
