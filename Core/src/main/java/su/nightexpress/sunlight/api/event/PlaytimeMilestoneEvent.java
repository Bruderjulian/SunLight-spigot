package su.nightexpress.sunlight.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.sunlight.moduleImpl.playtime.model.PlaytimeMilestone;

import java.util.List;

/**
 * Fired after a player reached a total-playtime milestone and its rewards have been dispatched.
 * <p>
 * Not cancellable — the milestone is marked as reached and the reward commands already ran. Use it
 * to react to the achievement rather than to grant it.
 * <p>
 * Repeatable milestones fire once per multiple of the threshold, so the same milestone id can appear
 * many times over a player's lifetime; {@link #getTimesReached()} tells you which one this was.
 */
public class PlaytimeMilestoneEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PlaytimeMilestone milestone;
    private final int timesReached;

    /**
     * @param player       the player who reached the milestone
     * @param milestone    the milestone definition
     * @param timesReached how many multiples of the threshold have been claimed; always {@code 1}
     *                     for non-repeatable milestones
     */
    public PlaytimeMilestoneEvent(@NotNull Player player, @NotNull PlaytimeMilestone milestone, int timesReached) {
        this.player = player;
        this.milestone = milestone;
        this.timesReached = timesReached;
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

    public @NotNull PlaytimeMilestone getMilestone() {
        return this.milestone;
    }

    public int getTimesReached() {
        return this.timesReached;
    }

    /** Convenience accessor for the milestone reward commands, possibly empty. */
    public @NotNull List<String> getRewards() {
        return this.milestone.rewards();
    }
}