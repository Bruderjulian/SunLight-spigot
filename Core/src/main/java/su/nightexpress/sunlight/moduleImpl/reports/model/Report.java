package su.nightexpress.sunlight.moduleImpl.reports.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.util.placeholder.PlaceholderResolvable;
import su.nightexpress.nightcore.util.placeholder.PlaceholderResolver;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsPlaceholders;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class Report implements PlaceholderResolvable {

    private final UUID id;

    private final UUID reporterId;
    private String reporterName;
    private final UUID targetId;
    private final String targetName;
    private final String categoryId;
    private String categoryDisplay;

    private String details;
    private ReportStatus status;
    private UUID staffId;
    private String staffName;

    private final long createDate;
    private long updateDate;

    private volatile boolean rewarded;

    private String lastWorld;
    private double lastX;
    private double lastY;
    private double lastZ;

    private int noteCount;

    private boolean reminded;
    private long claimedDate;
    private UUID caseId;

    /**
     * Guards the reward payout against two success paths racing on this server. The database
     * read-back in {@code ReportsModule#payReward} is the second line of defence for cross-server
     * races; this one is what makes same-server races cheap.
     */
    private final AtomicBoolean rewardClaimed = new AtomicBoolean(false);

    public Report(UUID id,
            UUID reporterId,
            String reporterName,
            @Nullable UUID targetId,
            String targetName,
            String categoryId,
            String details,
            ReportStatus status,
            @Nullable UUID staffId,
            @Nullable String staffName,
            long createDate,
            long updateDate,
            boolean rewarded,
            @Nullable String lastWorld,
            double lastX,
            double lastY,
            double lastZ,
            int noteCount
    ) {
        this(id, reporterId, reporterName, targetId, targetName, categoryId, details, status, staffId, staffName,
                createDate, updateDate, rewarded, lastWorld, lastX, lastY, lastZ, noteCount, false, 0L, null);
    }

    public Report(UUID id,
            UUID reporterId,
            String reporterName,
            @Nullable UUID targetId,
            String targetName,
            String categoryId,
            String details,
            ReportStatus status,
            @Nullable UUID staffId,
            @Nullable String staffName,
            long createDate,
            long updateDate,
            boolean rewarded,
            @Nullable String lastWorld,
            double lastX,
            double lastY,
            double lastZ,
            int noteCount,
            boolean reminded,
            long claimedDate,
            @Nullable UUID caseId
    ) {
        this.id = id;
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.targetId = targetId;
        this.targetName = targetName;
        this.categoryId = categoryId;
        this.details = details;
        this.status = status;
        this.staffId = staffId;
        this.staffName = staffName;
        this.createDate = createDate;
        this.updateDate = updateDate;
        this.rewarded = rewarded;
        this.lastWorld = lastWorld;
        this.lastX = lastX;
        this.lastY = lastY;
        this.lastZ = lastZ;
        this.noteCount = noteCount;
        this.reminded = reminded;
        this.claimedDate = claimedDate;
        this.caseId = caseId;
    }

    public static Report create(@NotNull UUID reporterId, @NotNull String reporterName, @Nullable UUID targetId,
            @NotNull String targetName, @NotNull String categoryId, @NotNull String details) {
        long now = System.currentTimeMillis();
        return new Report(UUID.randomUUID(), reporterId, reporterName, targetId, targetName, categoryId, details,
                ReportStatus.OPEN, null, null, now, now, false, null, 0, 0, 0, 0);
    }

    @Override
    public @NotNull PlaceholderResolver placeholders() {
        return ReportsPlaceholders.REPORT.resolver(this);
    }

    public UUID getId() {
        return this.id;
    }

    public UUID getReporterId() {
        return this.reporterId;
    }

    public String getReporterName() {
        return this.reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public @Nullable UUID getTargetId() {
        return this.targetId;
    }

    public String getTargetName() {
        return this.targetName;
    }

    /**
     * A report may target a player who never joined, in which case there is no UUID and the
     * lower-cased name is the only identity available. The duplicate lock and the punishment hook
     * both have to agree on which of the two to match on, so this is the single place that decides.
     */
    public boolean matchesTarget(@Nullable UUID uuid, @Nullable String name) {
        if (uuid != null && this.targetId != null) {
            return this.targetId.equals(uuid);
        }
        if (name == null) {
            return false;
        }
        return this.targetName.toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT));
    }

    public String getCategoryId() {
        return this.categoryId;
    }

    /**
     * The category's configured display text, stamped on when the report enters the repository.
     * Keeping it on the model rather than resolving it through a static lookup means placeholder
     * resolution stays pure and a config reload cannot leave a stale map behind.
     */
    public String getCategoryDisplay() {
        return this.categoryDisplay == null || this.categoryDisplay.isBlank() ? this.categoryId
                : this.categoryDisplay;
    }

    public void setCategoryDisplay(String categoryDisplay) {
        this.categoryDisplay = categoryDisplay;
    }

    public String getDetails() {
        return this.details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public ReportStatus getStatus() {
        return this.status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public boolean isPending() {
        return this.status.isPending();
    }

    public boolean isTerminal() {
        return this.status.isTerminal();
    }

    public @Nullable UUID getStaffId() {
        return this.staffId;
    }

    public @Nullable String getStaffName() {
        return this.staffName;
    }

    public void setStaff(@Nullable UUID staffId, @Nullable String staffName) {
        this.staffId = staffId;
        this.staffName = staffName;
    }

    public long getCreateDate() {
        return this.createDate;
    }

    public long getUpdateDate() {
        return this.updateDate;
    }

    public void setUpdateDate(long updateDate) {
        this.updateDate = updateDate;
    }

    public boolean isRewarded() {
        return this.rewarded;
    }

    public void setRewarded(boolean rewarded) {
        this.rewarded = rewarded;
    }

    /**
     * @return {@code true} for exactly one caller, {@code false} for everyone who lost the race.
     */
    public boolean tryClaimReward() {
        if (this.rewarded) {
            return false;
        }
        return this.rewardClaimed.compareAndSet(false, true);
    }

    /** Used when a synced row proves another server already paid. */
    public void releaseRewardClaim() {
        this.rewardClaimed.set(false);
    }

    public @Nullable String getLastWorld() {
        return this.lastWorld;
    }

    public void setLastLocation(@Nullable String world, double x, double y, double z) {
        this.lastWorld = world;
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
    }

    public boolean hasLastLocation() {
        return this.lastWorld != null && !this.lastWorld.isBlank();
    }

    public double getLastX() {
        return this.lastX;
    }

    public double getLastY() {
        return this.lastY;
    }

    public double getLastZ() {
        return this.lastZ;
    }

    /** Localized status, for placeholders that must not depend on the module's lang being loaded. */
    public String getStatusText() {
        return this.status.name();
    }

    public long getAge() {
        return System.currentTimeMillis() - this.createDate;
    }

    public String getAgeText() {
        return su.nightexpress.nightcore.util.time.TimeFormats.formatSince(this.createDate,
                su.nightexpress.nightcore.util.time.TimeFormatType.LITERAL);
    }

    public int getNoteCount() {
        return this.noteCount;
    }

    public void setNoteCount(int noteCount) {
        this.noteCount = noteCount;
    }

    /** Whether staff have already been nudged about this report sitting unclaimed. */
    public boolean isReminded() {
        return this.reminded;
    }

    public void setReminded(boolean reminded) {
        this.reminded = reminded;
    }

    /** Epoch millis of the current claim, or 0 when unclaimed. Drives the time-to-claim stat. */
    public long getClaimedDate() {
        return this.claimedDate;
    }

    public void setClaimedDate(long claimedDate) {
        this.claimedDate = claimedDate;
    }

    public @Nullable UUID getCaseId() {
        return this.caseId;
    }

    public void setCaseId(@Nullable UUID caseId) {
        this.caseId = caseId;
    }
}
