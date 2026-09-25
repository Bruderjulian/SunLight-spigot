package su.nightexpress.sunlight.moduleImpl.reports.model;

/**
 * Which conclusions pay the reporter a reward.
 * <p>
 * {@link #ANY_RESOLVED} is the lenient mode and the shipped default: trusting staff to mark a
 * report justified is enough. {@link #PUNISHMENT_ONLY} is the strict mode, for servers that want
 * money to move only when somebody was actually punished, which makes the reward expensive to farm
 * with staged or bogus reports.
 */
public enum RewardMode {
    ANY_RESOLVED,
    PUNISHMENT_ONLY;

    public boolean isPunishmentRequired() {
        return this == PUNISHMENT_ONLY;
    }
}
