package su.nightexpress.sunlight.moduleImpl.friends;

import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.placeholder.TypedPlaceholder;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.moduleImpl.friends.model.FriendRecord;
import su.nightexpress.sunlight.moduleImpl.friends.repository.FriendsRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Placeholder tokens used inside the module's own messages, menus and icons.
 * <p>
 * The PAPI-facing keys are registered separately in
 * {@link FriendsModule#registerPlaceholders(su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry)}:
 * those take a payload, which is why they cannot live in this table.
 */
public class FriendsPlaceholders {

    public static final String FRIEND_NAME        = "%friend_name%";
    public static final String FRIEND_UUID        = "%friend_uuid%";
    public static final String FRIEND_OWNER       = "%friend_owner%";
    public static final String FRIEND_OWNER_UUID  = "%friend_owner_uuid%";
    public static final String FRIEND_ONLINE      = "%friend_online%";
    public static final String FRIEND_CREATE_DATE = "%friend_create_date%";
    public static final String FRIEND_AGE         = "%friend_age%";
    public static final String FRIEND_FAVORITE    = "%friend_favorite%";
    public static final String FRIEND_STATUS      = "%friend_status%";

    /**
     * Resolved per viewer rather than per record, so it has no entry in {@link #FRIEND}: the menus
     * and the admin message supply it themselves.
     */
    public static final String FRIEND_LIST      = "%friend_list%";
    public static final String FRIEND_LAST_SEEN = "%friend_last_seen%";

    public static final TypedPlaceholder<FriendRecord> FRIEND = TypedPlaceholder
            .builder(FriendRecord.class)
            .with(FRIEND_NAME, record -> record.getFriend().name())
            .with(FRIEND_UUID, record -> record.getFriendId().toString())
            .with(FRIEND_OWNER, record -> record.getOwner().name())
            .with(FRIEND_OWNER_UUID, record -> record.getOwnerId().toString())
            .with(FRIEND_ONLINE, record -> CoreLang.STATE_YES_NO.get(FriendsRepository.isOnline(record.getFriendId())))
            .with(FRIEND_CREATE_DATE, record -> TimeFormats.formatDateTime(record.getCreateDate()))
            .with(FRIEND_AGE, record -> formatAge(record.getCreateDate()))
            .with(FRIEND_FAVORITE, record -> CoreLang.STATE_YES_NO.get(record.isFavorite()))
            .with(FRIEND_STATUS, record -> FriendsRepository.isOnline(record.getFriendId()) ? "Online" : "Offline")
            .build();

    private FriendsPlaceholders() {
    }

    /**
     * Shared formatting so a menu icon and a chat message never disagree on how an age reads.
     * A zero or negative stamp means "never", which is what a never-seen friend has.
     */
    public static String formatAge(long sinceMillis) {
        if (sinceMillis <= 0L) {
            return CoreLang.OTHER_NEVER.text();
        }
        return TimeFormats.formatSince(sinceMillis, TimeFormatType.LITERAL);
    }

    public static String formatLastSeen(long lastSeenMillis) {
        if (lastSeenMillis <= 0L) {
            return CoreLang.OTHER_NEVER.text();
        }
        return TimeFormats.formatSince(lastSeenMillis, TimeFormatType.LITERAL) + " ago";
    }

    /** Comma-separated names, or "None" when the list is empty, for a scoreboard line. */
    public static String names(List<FriendRecord> records) {
        if (records.isEmpty()) {
            return CoreLang.OTHER_NONE.text();
        }
        return records.stream().map(record -> record.getFriend().name()).collect(Collectors.joining(", "));
    }
}