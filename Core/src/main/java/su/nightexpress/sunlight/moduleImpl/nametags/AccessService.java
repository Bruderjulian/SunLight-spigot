package su.nightexpress.sunlight.moduleImpl.nametags;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.sunlight.moduleImpl.nametags.config.NametagsPerms;
import su.nightexpress.sunlight.moduleImpl.nametags.model.GrantRecord;
import su.nightexpress.sunlight.moduleImpl.nametags.model.PriceMode;
import su.nightexpress.sunlight.moduleImpl.nametags.model.PurchaseResult;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagAccessMode;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagDefinition;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.UserManager;
import su.nightexpress.sunlight.utils.EconomyUtils;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Decides whether a player may use a tag, and owns every mutation of a tag entitlement.
 * <p>
 * Entitlements live in the user's {@code properties} column, so they follow the player
 * across a name change and are saved by the existing user save cycle.
 */
public class AccessService {

    private final UserManager userManager;

    public AccessService(@NotNull UserManager userManager) {
        this.userManager = userManager;
    }

    // -----------------------------------------------------
    // Queries
    // -----------------------------------------------------

    /**
     * Whether the player may currently use the tag. Paid modes need an active grant, so an
     * expired subscription correctly reports {@code false}.
     */
    public boolean hasAccess(@NotNull Player player, @NotNull TagDefinition tag) {
        return this.hasAccess(player, this.userManager.getOrFetch(player), tag, System.currentTimeMillis());
    }

    public boolean hasAccess(@NotNull Player player,
            @NotNull SunUser user,
            @NotNull TagDefinition tag,
            long nowMillis
    ) {
        if (EconomyUtils.hasBypass(player, NametagsPerms.BYPASS_ACCESS)) return true;
        if (player.hasPermission(NametagsPerms.ADMIN)) return true;

        return switch (tag.getAccessMode()) {
            case FREE -> true;
            case PERMISSION -> this.hasTagPermission(player, tag);
            case PURCHASE, SUBSCRIPTION -> this.hasValidGrant(user, tag.getId(), nowMillis);
        };
    }

    public boolean hasTagPermission(@NotNull Player player, @NotNull TagDefinition tag) {
        if (tag.hasExtraPermission() && !player.hasPermission(tag.getPermission())) return false;
        return NametagsPerms.hasTagAccess(player, tag.getId());
    }

    public boolean hasValidGrant(@NotNull SunUser user, @NotNull String tagId, long nowMillis) {
        GrantRecord grant = this.getGrant(user, tagId);
        return grant != null && grant.isActive(nowMillis);
    }

    public @Nullable GrantRecord getGrant(@NotNull SunUser user, @NotNull String tagId) {
        return this.getGrants(user).get(tagId);
    }

    public @NotNull Map<String, GrantRecord> getGrants(@NotNull SunUser user) {
        Map<String, GrantRecord> grants = user.getPropertyOrDefault(NametagsProperties.GRANTS);
        return grants == null ? Map.of() : grants;
    }

    /** Whether the tag's own glow colour is allowed to be applied. */
    public boolean allowsGlow(@NotNull SunUser user, @NotNull TagDefinition tag, long nowMillis) {
        return tag.isGlow() && (tag.requiresGrant() ? this.hasValidGrant(user, tag.getId(), nowMillis) : true);
    }

    // -----------------------------------------------------
    // Mutations
    // -----------------------------------------------------

    /**
     * Grants a tag permanently or for one subscription period.
     * An existing active subscription is extended rather than replaced.
     *
     * @return {@code true} when the grant was stored
     */
    public boolean grant(@NotNull SunUser user, @NotNull TagDefinition tag, long nowMillis) {
        Map<String, GrantRecord> grants = new HashMap<>(this.getGrants(user));
        GrantRecord existing = grants.get(tag.getId());

        GrantRecord updated;
        if (tag.isSubscription() && existing != null && existing.getMode() == TagAccessMode.SUBSCRIPTION
                && existing.isActive(nowMillis)) {
            updated = existing.extend(tag.getSubscriptionPeriod(), nowMillis);
        } else {
            updated = tag.isSubscription()
                    ? new GrantRecord(tag.getId(), TagAccessMode.SUBSCRIPTION, nowMillis,
                            nowMillis + tag.getSubscriptionPeriod().toMillis(), "")
                    : GrantRecord.purchase(tag.getId(), nowMillis);
        }
        if (updated == null) return false;

        grants.put(tag.getId(), updated);
        user.setProperty(NametagsProperties.GRANTS, grants);
        return true;
    }

    /** @return {@code true} when a grant existed and was removed */
    public boolean revoke(@NotNull SunUser user, @NotNull String tagId) {
        Map<String, GrantRecord> grants = new HashMap<>(this.getGrants(user));
        if (grants.remove(tagId) == null) return false;

        user.setProperty(NametagsProperties.GRANTS, grants);
        return true;
    }

    /**
     * Buys or subscribes to a tag, withdrawing the configured price. An active subscription
     * is extended rather than restarted.
     * <p>
     * A cost bypass skips the withdrawal but still grants the tag, so staff can hand out
     * paid tags without a balance change. A tag without a price is granted for free.
     *
     * @return {@link PurchaseResult#SUCCESS} or the reason the purchase was refused
     */
    public @NotNull PurchaseResult purchase(@NotNull Player player, @NotNull TagDefinition tag) {
        if (tag.getPriceMode() == PriceMode.EXTERNAL) return PurchaseResult.EXTERNAL;

        if (tag.hasPrice()) {
            boolean bypassCost = EconomyUtils.hasBypass(player, NametagsPerms.BYPASS_COST);
            if (!bypassCost) {
                if (!EconomyUtils.hasCurrency()) return PurchaseResult.NO_ECONOMY;
                if (!EconomyUtils.canAfford(player, tag.getPrice())) return PurchaseResult.NO_FUNDS;
                EconomyUtils.withdraw(player, tag.getPrice());
            }
        }

        SunUser user = this.userManager.getOrFetch(player);
        if (!this.grant(user, tag, System.currentTimeMillis())) return PurchaseResult.REFUSED;
        return PurchaseResult.SUCCESS;
    }

    /**
     * Drops every expired grant from a user's stored map.
     *
     * @return the tag ids whose entitlement just lapsed, so the caller can tell the player
     */
    public @NotNull Set<String> sweepExpired(@NotNull SunUser user, long nowMillis) {
        Map<String, GrantRecord> grants = new HashMap<>(this.getGrants(user));
        Set<String> expired = new LinkedHashSet<>();
        grants.forEach((tagId, grant) -> {
            if (grant.isExpired(nowMillis)) expired.add(tagId);
        });
        if (expired.isEmpty()) return Set.of();

        expired.forEach(grants::remove);
        user.setProperty(NametagsProperties.GRANTS, grants);
        return expired;
    }
}
