package su.nightexpress.sunlight.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Fired after a player reached a daily, weekly or monthly playtime goal and the corresponding
 * rewards have been dispatched.
 * <p>
 * Not cancellable: the goal is already marked as rewarded and the reward commands are on their way.
 * This is the hook for achievements, announcements or leaderboards — the reward itself is a list of
 * console commands, which is of little use to a plugin that wants to grant something else.
 * <p>
 * Each goal pays at most once per calendar period; the bookkeeping key is reset on rollover.
 */
public class PlaytimeGoalReachedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String goalName;
    private final long playtime;
    private final List<String> rewards;

    /**
     * @param player   the player who reached the goal
     * @param goalName which goal was reached — {@code Daily}, {@code Weekly} or {@code Monthly}
     * @param playtime the playtime accumulated in that period, in milliseconds
     * @param rewards  the console commands dispatched as a reward, possibly empty
     */
    public PlaytimeGoalReachedEvent(@NotNull Player player, @NotNull String goalName, long playtime,
            @NotNull List<String> rewards) {
        this.player = player;
        this.goalName = goalName;
        this.playtime = playtime;
        this.rewards = rewards;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public @NotNull Player getPlayer() {
        return this.player;
    }

    public @NotNull String getGoalName() {
        return this.goalName;
    }

    public long getPlaytime() {
        return this.playtime;
    }

    /** The console commands dispatched as a reward; may be empty. */
    public @NotNull List<String> getRewards() {
        return this.rewards;
    }
}