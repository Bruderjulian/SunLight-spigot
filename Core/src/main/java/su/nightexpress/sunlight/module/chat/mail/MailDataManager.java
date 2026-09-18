package su.nightexpress.sunlight.module.chat.mail;

import su.nightexpress.nightcore.db.column.Column;
import su.nightexpress.nightcore.db.statement.condition.Operator;
import su.nightexpress.nightcore.db.statement.condition.Wheres;
import su.nightexpress.nightcore.db.table.Table;
import su.nightexpress.sunlight.data.DataHandler;

import java.util.List;
import java.util.UUID;

public class MailDataManager {

    static final Column<UUID> COLUMN_ID = Column.uuidType("mailId").primaryKey().build();
    static final Column<UUID> COLUMN_SENDER_ID = Column.uuidType("senderId").build();
    static final Column<String> COLUMN_SENDER_NAME = Column.stringType("senderName", 32).build();
    static final Column<UUID> COLUMN_RECIPIENT_ID = Column.uuidType("recipientId").build();
    static final Column<String> COLUMN_MESSAGE = Column.mediumText("message").build();
    static final Column<Long> COLUMN_DATE = Column.longType("dateCreated").build();

    private final DataHandler dataHandler;

    private Table mailsTable;

    public MailDataManager(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    public void init(String tablePrefix) {
        this.mailsTable = Table.builder(tablePrefix + "_mails")
                .withColumn(
                        COLUMN_ID,
                        COLUMN_SENDER_ID,
                        COLUMN_SENDER_NAME,
                        COLUMN_RECIPIENT_ID,
                        COLUMN_MESSAGE,
                        COLUMN_DATE)
                .build();

        this.dataHandler.createTable(this.mailsTable);
    }

    public List<MailData> getMails(UUID recipientId) {
        return this.dataHandler.selectWhere(this.mailsTable, MailQueries.SELECT_MAILS,
                Wheres.whereUUID(COLUMN_RECIPIENT_ID, o -> recipientId));
    }

    public void insertMail(MailData mail) {
        this.dataHandler.insert(this.mailsTable, MailQueries.INSERT_MAIL, mail);
    }

    public void deleteMails(UUID recipientId) {
        this.dataHandler.delete(this.mailsTable, Wheres.whereUUID(COLUMN_RECIPIENT_ID, o -> recipientId));
    }

    public void purgeOldEntries(long expiryMillis) {
        if (expiryMillis <= 0)
            return;

        long deadline = System.currentTimeMillis() - expiryMillis;
        this.dataHandler.delete(this.mailsTable, Wheres.where(COLUMN_DATE, Operator.SMALLER, o -> deadline));
    }
}
