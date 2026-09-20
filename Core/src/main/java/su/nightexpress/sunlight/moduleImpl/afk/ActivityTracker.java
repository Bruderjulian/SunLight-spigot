package su.nightexpress.sunlight.moduleImpl.afk;

import su.nightexpress.sunlight.utils.TimeUtil;
import su.nightexpress.nightcore.util.geodata.pos.BlockPos;
import su.nightexpress.sunlight.moduleImpl.afk.core.AfkSettings;

public class ActivityTracker {

    private final AfkSettings settings;

    private BlockPos lastPos;
    private float lastYaw;
    private float lastPitch;

    private int idleTime;
    private int idleThreshold;

    private long afkEnterTimestamp;

    private int wakeUpThreshold;
    private long wakeUpEndTimestamp;

    private long lastKickWarningSent;

    public ActivityTracker(AfkSettings settings) {
        this.settings = settings;
        this.resetCounters();
    }

    public void resetCounters() {
        this.resetIdleCounter();
        this.resetIdleThreshold();
        this.resetWakeUpCounter();
        this.resetAfkTimestamp();
        this.lastKickWarningSent = 0L;
    }

    public void tick() {
        if (this.isAfk()) {
            if (this.isWakingUp() && this.isWakeUpTimeout()) {
                this.resetWakeUpCounter();
            }
        }

        if (!this.isAfk() && this.idleThreshold > 0) {
            this.idleThreshold--;
            return;
        }

        this.countIdleTime();
    }

    public void updatePosition(BlockPos newPos, float yaw, float pitch) {
        if (this.lastPos == null) {
            this.lastPos = newPos;
            this.lastYaw = yaw;
            this.lastPitch = pitch;
            return;
        }

        boolean blockChanged = !newPos.equals(this.lastPos);
        boolean rotationChanged = this.lastYaw != yaw || this.lastPitch != pitch;
        this.lastPos = newPos;
        this.lastYaw = yaw;
        this.lastPitch = pitch;

        if (blockChanged) {
            this.countActivity(ActivityType.MOVEMENT);
        } else if (rotationChanged && this.settings.movementTrackRotation.get()) {
            this.countActivity(ActivityType.ROTATION);
        }
    }

    public void countActivity(ActivityType type) {
        this.countActivity(this.settings.getActivityPoints(type));
    }

    public void countActivity(int amount) {
        if (amount <= 0)
            return;

        if (!this.isAfk()) {
            this.resetIdleCounter();
            this.resetIdleThreshold();
            return;
        }

        if (this.isWakingUp() && this.isWakeUpTimeout()) {
            this.resetWakeUpCounter();
        }

        this.wakeUpEndTimestamp = TimeUtil.createFutureTimestamp(this.settings.wakeUpTimer.get());
        this.wakeUpThreshold -= amount;
    }

    public void countIdleTime() {
        this.idleTime++;
    }

    private void resetWakeUpCounter() {
        this.wakeUpThreshold = this.settings.wakeUpThreshold.get();
        this.wakeUpEndTimestamp = 0L;
    }

    private void resetIdleCounter() {
        this.idleTime = 0;
    }

    private void resetIdleThreshold() {
        this.idleThreshold = this.settings.idleThreshold.get();
    }

    public void setAfkTimestamp() {
        this.afkEnterTimestamp = System.currentTimeMillis();
    }

    public void resetAfkTimestamp() {
        this.afkEnterTimestamp = 0L;
    }

    public boolean isAfk() {
        return this.afkEnterTimestamp > 0L;
    }

    public boolean isWakingUp() {
        return this.wakeUpEndTimestamp > 0L;
    }

    public boolean isWakeUpTimeout() {
        return TimeUtil.isPassed(this.wakeUpEndTimestamp);
    }

    public boolean isEnoughActivity() {
        return this.wakeUpThreshold <= 0;
    }

    public int getWakeUpProgress() {
        return this.wakeUpThreshold;
    }

    public long getWakeUpEndTimestamp() {
        return this.wakeUpEndTimestamp;
    }

    public int getWakeUpTimeLeft() {
        if (this.wakeUpEndTimestamp <= 0L)
            return 0;

        long left = this.wakeUpEndTimestamp - System.currentTimeMillis();
        return (int) Math.max(0D, Math.ceil(left / 1000D));
    }

    public long getLastKickWarningSent() {
        return this.lastKickWarningSent;
    }

    public void setLastKickWarningSent(long timestamp) {
        this.lastKickWarningSent = timestamp;
    }

    public long getAfkEnterTimestamp() {
        return this.afkEnterTimestamp;
    }

    public int getIdleTime() {
        return this.idleTime;
    }
}