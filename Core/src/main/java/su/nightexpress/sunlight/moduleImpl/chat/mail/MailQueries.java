package su.nightexpress.sunlight.moduleImpl.chat.mail;

import su.nightexpress.nightcore.db.statement.RowMapper;
import su.nightexpress.nightcore.db.statement.template.InsertStatement;
import su.nightexpress.nightcore.db.statement.template.SelectStatement;

import java.sql.SQLException;
import java.util.UUID;

public class MailQueries {

    public static final RowMapper<MailData> MAIL_LOADER = resultSet -> {
        try {
            UUID id = UUID.fromString(resultSet.getString(MailDataManager.COLUMN_ID.getName()));
            UUID senderId = UUID.fromString(resultSet.getString(MailDataManager.COLUMN_SENDER_ID.getName()));
            String senderName = resultSet.getString(MailDataManager.COLUMN_SENDER_NAME.getName());
            UUID recipientId = UUID.fromString(resultSet.getString(MailDataManager.COLUMN_RECIPIENT_ID.getName()));
            String message = resultSet.getString(MailDataManager.COLUMN_MESSAGE.getName());
            long dateCreated = resultSet.getLong(MailDataManager.COLUMN_DATE.getName());

            return new MailData(id, senderId, senderName, recipientId, message, dateCreated);
        }
        catch (SQLException exception) {
            exception.printStackTrace();
            return null;
        }
    };

    public static final SelectStatement<MailData> SELECT_MAILS = SelectStatement.builder(MAIL_LOADER).build();

    public static final InsertStatement<MailData> INSERT_MAIL = InsertStatement.builder(MailData.class)
        .setUUID(MailDataManager.COLUMN_ID, MailData::getId)
        .setUUID(MailDataManager.COLUMN_SENDER_ID, MailData::getSenderId)
        .setString(MailDataManager.COLUMN_SENDER_NAME, MailData::getSenderName)
        .setUUID(MailDataManager.COLUMN_RECIPIENT_ID, MailData::getRecipientId)
        .setString(MailDataManager.COLUMN_MESSAGE, MailData::getMessage)
        .setLong(MailDataManager.COLUMN_DATE, MailData::getDateCreated)
        .build();
}
