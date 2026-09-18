package su.nightexpress.sunlight.moduleImpl.chat.mail;

import java.util.UUID;

public class MailData {

    private final UUID id;
    private final UUID senderId;
    private final String senderName;
    private final UUID recipientId;
    private final String message;
    private final long dateCreated;

    public MailData(UUID id,
            UUID senderId,
            String senderName,
            UUID recipientId,
            String message,
            long dateCreated) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.recipientId = recipientId;
        this.message = message;
        this.dateCreated = dateCreated;
    }

    public UUID getId() {
        return this.id;
    }

    public UUID getSenderId() {
        return this.senderId;
    }

    public String getSenderName() {
        return this.senderName;
    }

    public UUID getRecipientId() {
        return this.recipientId;
    }

    public String getMessage() {
        return this.message;
    }

    public long getDateCreated() {
        return this.dateCreated;
    }
}
