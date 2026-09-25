package su.nightexpress.sunlight.moduleImpl.nametags.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagDefinition;

/**
 * Fired after a player successfully buys a tag or starts or renews a subscription.
 * Not fired for admin grants, which never charge anything.
 */
public class PlayerTagPurchaseEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final TagDefinition tag;
    private final double price;
    private final long expiresAt;

    public PlayerTagPurchaseEvent(@NotNull Player player,
            @NotNull TagDefinition tag,
            double price,
            long expiresAt
    ) {
        super(player);
        this.tag = tag;
        this.price = price;
        this.expiresAt = expiresAt;
    }

    public @NotNull TagDefinition getTag() {
        return this.tag;
    }

    /** What the player paid. Zero when a cost bypass applied. */
    public double getPrice() {
        return this.price;
    }

    /** Expiry timestamp, or {@code 0} for a permanent purchase. */
    public long getExpiresAt() {
        return this.expiresAt;
    }

    public boolean isSubscription() {
        return this.tag.isSubscription();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
