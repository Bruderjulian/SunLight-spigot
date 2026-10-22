package su.nightexpress.sunlight.moduleImpl.voting.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import su.nightexpress.nightcore.util.placeholder.PlaceholderResolvable;
import su.nightexpress.nightcore.util.placeholder.PlaceholderResolver;
import su.nightexpress.sunlight.moduleImpl.voting.VotingPlaceholders;

/**
 * A poll: a named question, a set of {@link VoteOption choices}, an end time, and the
 * {@link VoteRules rules} that govern it.
 * <p>
 * The key is what players type and is unique across the server; the id is the database key. The
 * two are stored separately because a poll's history outlives any rename a player might want to
 * make to its display name.
 */
public class Vote implements PlaceholderResolvable {

    /**
     * Choices are held as a concurrent map plus an explicit key order rather than a plain
     * {@code LinkedHashMap}. Table sync delivers a choice from the database thread while the main
     * thread is iterating the menu that lists them, and an ordinary map would throw
     * {@code ConcurrentModificationException} from the middle of a render.
     */
    private final Map<String, VoteOption> options;
    private final List<String> optionOrder;

    private final UUID id;
    private final String key;

    private String display;
    private String description;
    private UUID creatorId;
    private String creatorName;

    private final long createDate;
    private volatile long startDate;
    private volatile long endDate;

    private volatile VoteStatus status;

    private final VoteRules rules;

    private volatile String outcome;
    private volatile long concludeDate;

    private volatile int recurrenceCount;
    private volatile String recurrenceSeries;

    public Vote(final UUID id,
            final String key,
            final String display,
            final UUID creatorId,
            final String creatorName,
            final long createDate,
            final long endDate,
            final VoteStatus status,
            final Collection<VoteOption> options,
            final VoteRules rules,
            final String outcome,
            final long concludeDate) {
        this(id, key, display, "", creatorId, creatorName, createDate, createDate, endDate, status,
                options, rules, outcome, concludeDate);
    }

    public Vote(final UUID id,
            final String key,
            final String display,
            final String description,
            final UUID creatorId,
            final String creatorName,
            final long createDate,
            final long startDate,
            final long endDate,
            final VoteStatus status,
            final Collection<VoteOption> options,
            final VoteRules rules,
            final String outcome,
            final long concludeDate) {
        this.id = java.util.Objects.requireNonNull(id, "vote id");
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("vote key");
        }
        if (options == null) {
            throw new IllegalArgumentException("vote options");
        }
        this.key = key.toLowerCase(Locale.ROOT);
        this.display = display;
        this.description = description == null ? "" : description;
        this.creatorId = creatorId;
        this.creatorName = creatorName;
        this.createDate = createDate;
        this.startDate = startDate <= 0L ? createDate : startDate;
        this.endDate = endDate;
        this.status = status;
        this.rules = rules == null ? VoteRules.defaults() : rules;
        this.outcome = outcome;
        this.concludeDate = concludeDate;

        final Map<String, VoteOption> map = new ConcurrentHashMap<>();
        final List<String> order = new CopyOnWriteArrayList<>();
        for (final VoteOption option : options) {
            final String optionKey = option.getKey();
            if (map.putIfAbsent(optionKey, option) == null) {
                order.add(optionKey);
            }
        }
        this.options = map;
        this.optionOrder = order;
    }

    public static Vote create(final String key,
            final String display,
            final UUID creatorId,
            final String creatorName,
            final int durationSeconds,
            final Collection<VoteOption> options,
            final VoteRules rules) {
        return create(key, display, "", creatorId, creatorName, durationSeconds, 0, options, rules);
    }

    public static Vote create(final String key,
            final String display,
            final String description,
            final UUID creatorId,
            final String creatorName,
            final int durationSeconds,
            final int startDelaySeconds,
            final Collection<VoteOption> options,
            final VoteRules rules) {
        final long now = System.currentTimeMillis();
        final long start = now + TimeUnit.SECONDS.toMillis(Math.max(0, startDelaySeconds));
        final long end = Math.max(start, now)
                + TimeUnit.SECONDS.toMillis(Math.max(1, durationSeconds));
        final VoteStatus status = startDelaySeconds > 0 ? VoteStatus.SCHEDULED : VoteStatus.OPEN;
        return new Vote(UUID.randomUUID(), key, display, description, creatorId, creatorName, now,
                start, end, status, options, rules, null, 0L);
    }

    // -- Options --------------------------------------------------------------------------------

    public @NotNull Collection<VoteOption> getOptions() {
        final List<VoteOption> list = new ArrayList<>(this.optionOrder.size());
        for (final String optionKey : this.optionOrder) {
            final VoteOption option = this.options.get(optionKey);
            if (option != null) {
                list.add(option);
            }
        }
        return list;
    }

    public @NotNull List<VoteOption> getOptionList() {
        return new ArrayList<>(this.getOptions());
    }

    public @NotNull List<String> getOptionKeys() {
        return new ArrayList<>(this.optionOrder);
    }

    public @Nullable VoteOption getOption(final String optionKey) {
        return optionKey == null ? null : this.options.get(optionKey.toLowerCase(Locale.ROOT));
    }

    public boolean hasOption(final String optionKey) {
        return this.getOption(optionKey) != null;
    }

    /**
     * Synchronized so the map and the order list change atomically. Table sync adds choices from
     * the database thread while menus iterate them on the main thread; two half-applied steps
     * would leave a phantom key in the order with no map entry, or vice versa.
     */
    public synchronized boolean addOption(final VoteOption option) {
        if (option == null) {
            return false;
        }
        if (this.options.putIfAbsent(option.getKey(), option) != null) {
            return false;
        }
        if (!this.optionOrder.contains(option.getKey())) {
            this.optionOrder.add(option.getKey());
        }
        return true;
    }

    public synchronized @Nullable VoteOption removeOption(final String optionKey) {
        if (optionKey == null) {
            return null;
        }
        final String key = optionKey.toLowerCase(Locale.ROOT);
        this.optionOrder.remove(key);
        return this.options.remove(key);
    }

    /**
     * @return the neutral option, or {@code null} when this poll does not have one.
     */
    public @Nullable VoteOption getNeutralOption() {
        return this.getOption(VoteOption.NEUTRAL);
    }

    // -- State ----------------------------------------------------------------------------------

    public boolean isOpen() {
        return this.status.isOpen();
    }

    public boolean isScheduled() {
        return this.status.isScheduled();
    }

    public boolean isStartDue() {
        return this.isScheduled() && System.currentTimeMillis() >= this.startDate;
    }

    public boolean isExpired() {
        return this.isOpen() && System.currentTimeMillis() >= this.endDate;
    }

    /**
     * @return whether results may be shown to voters right now. A secret ballot hides
     *         live numbers while it is open; finished polls always reveal.
     */
    public boolean isExposed() {
        return !this.rules.isSecretBallot() || !this.isOpen();
    }

    /**
     * @return whether this poll suppresses every push notification (announce, result,
     *         reminders, join-nudge). Silent polls stay votable via GUI/commands.
     */
    public boolean isSilent() {
        return this.rules.isSilent();
    }

    /**
     * @return whether this poll spawns a follow-up instance when it concludes.
     */
    public boolean isRecurring() {
        return this.rules.isRecurrenceEnabled();
    }

    /**
     * @return whether vote changes are still allowed. A poll that forbids changes is
     *         final on first cast; otherwise changes lock when little time remains.
     */
    public boolean isChangeAllowed() {
        if (!this.rules.isAllowChange()) {
            return false;
        }
        final int lock = this.rules.getChangeLockSeconds();
        return lock <= 0 || this.getTimeLeft() > TimeUnit.SECONDS.toMillis(lock);
    }

    public long getTimeToStart() {
        return this.isScheduled() ? Math.max(0L, this.startDate - System.currentTimeMillis()) : 0L;
    }

    public void open(final long date) {
        this.status = VoteStatus.OPEN;
        if (this.endDate < date) {
            this.endDate = date;
        }
    }

    public long getTimeLeft() {
        return this.isOpen() ? Math.max(0L, this.endDate - System.currentTimeMillis()) : 0L;
    }

    public void conclude(final String outcomeKey, final long date) {
        this.status = VoteStatus.CONCLUDED;
        this.outcome = outcomeKey;
        this.concludeDate = date;
    }

    public void cancel(final long date) {
        this.status = VoteStatus.CANCELLED;
        this.concludeDate = date;
    }

    // -- Accessors ------------------------------------------------------------------------------

    @Override
    public @NotNull PlaceholderResolver placeholders() {
        return VotingPlaceholders.VOTE.resolver(this);
    }

    public UUID getId() {
        return this.id;
    }

    public String getKey() {
        return this.key;
    }

    public String getDisplay() {
        return this.display == null || this.display.isBlank() ? this.key : this.display;
    }

    public void setDisplay(final String display) {
        this.display = display;
    }

    public @NotNull String getDescription() {
        return this.description == null ? "" : this.description;
    }

    public void setDescription(final String description) {
        this.description = description == null ? "" : description;
    }

    public boolean hasDescription() {
        return this.description != null && !this.description.isBlank();
    }

    public @Nullable UUID getCreatorId() {
        return this.creatorId;
    }

    public @Nullable String getCreatorName() {
        return this.creatorName;
    }

    public long getCreateDate() {
        return this.createDate;
    }

    public long getStartDate() {
        return this.startDate;
    }

    public void setStartDate(final long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return this.endDate;
    }

    public void setEndDate(final long endDate) {
        this.endDate = endDate;
    }

    public VoteStatus getStatus() {
        return this.status;
    }

    public void setStatus(final VoteStatus status) {
        this.status = status;
    }

    public VoteRules getRules() {
        return this.rules;
    }

    public @Nullable String getOutcome() {
        return this.outcome;
    }

    public @Nullable VoteOption getOutcomeOption() {
        return this.getOption(this.outcome);
    }

    public boolean hasOutcome() {
        return this.outcome != null;
    }

    public long getConcludeDate() {
        return this.concludeDate;
    }

    public int getRecurrenceCount() {
        return this.recurrenceCount;
    }

    public void setRecurrenceCount(final int recurrenceCount) {
        this.recurrenceCount = Math.max(0, recurrenceCount);
    }

    public @Nullable String getRecurrenceSeries() {
        return this.recurrenceSeries;
    }

    public void setRecurrenceSeries(final @Nullable String recurrenceSeries) {
        this.recurrenceSeries = recurrenceSeries == null || recurrenceSeries.isBlank() ? null
                : recurrenceSeries.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return this.key;
    }
}