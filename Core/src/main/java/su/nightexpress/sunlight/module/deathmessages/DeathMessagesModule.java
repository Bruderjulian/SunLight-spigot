package su.nightexpress.sunlight.module.deathmessages;

import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.*;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.text.night.NightMessage;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.exception.ModuleLoadException;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DeathMessagesModule extends Module {

    private static final long ANVIL_TRACK_TTL_MILLIS = TimeUnit.MINUTES.toMillis(10);
    private static final double ANVIL_TRACK_RADIUS_SQUARED = 8D * 8D;
    private static final int ANVIL_TRACK_MAX_SIZE = 1024;

    private final DeathMessagesSettings settings;
    private final Map<UUID, TrackedAnvil> anvilPlacements;

    public DeathMessagesModule(@NotNull ModuleContext context) {
        super(context);
        this.settings = new DeathMessagesSettings();
        this.anvilPlacements = new ConcurrentHashMap<>();
    }

    @Override
    protected void loadModule(@NotNull FileConfig config) throws ModuleLoadException {
        this.settings.load(config);

        this.addListener(new DeathMessagesListener(this.plugin, this));
    }

    @Override
    protected void unloadModule() {

    }

    @Override
    protected void registerCommands() {

    }

    @Override
    protected void registerPermissions(@NotNull PermissionTree root) {

    }

    @Override
    public void registerPlaceholders(@NotNull PlaceholderRegistry registry) {

    }

    public void handleDeathEvent(@NotNull PlayerDeathEvent event) {
        EventUtils.getAdapter().setDeathMessage(event, null);

        Player player = event.getEntity();

        EntityDamageEvent lastEvent = player.getLastDamageCause();
        if (lastEvent == null) return;

        DamageSource damageSource = lastEvent.getDamageSource();

        Entity causingEntity = damageSource.getCausingEntity();
        Entity directEnttiy = damageSource.getDirectEntity();
        ItemStack weapon = null;

        if (causingEntity instanceof LivingEntity livingEntity) {
            weapon = EntityUtil.getItemInSlot(livingEntity, EquipmentSlot.HAND);
        }

        // Resolve player attribution for player-caused message variants.
        Player playerCause = causingEntity instanceof Player causingPlayer ? causingPlayer : player.getKiller();
        String sourceName = causingEntity == null ? null : getName(causingEntity);

        if (playerCause == null && damageSource.getDamageType() == DamageType.FALLING_ANVIL && directEnttiy instanceof FallingBlock fallingBlock) {
            TrackedAnvil tracked = this.findAnvilPlacer(fallingBlock.getLocation());
            if (tracked != null) {
                Player onlinePlacer = this.plugin.getServer().getPlayer(tracked.placerId());
                if (onlinePlacer != null) {
                    playerCause = onlinePlacer;
                    sourceName = getName(onlinePlacer);
                }
                else {
                    sourceName = tracked.placerName();
                }
            }
        }

        if (sourceName == null && playerCause != null) {
            sourceName = getName(playerCause);
        }

        DeathContext context = new DeathContext(player, damageSource, causingEntity, directEnttiy, weapon);

        DeathMessage message = this.getMessage(context);
        if (message == null) return;

        String rawMessage = message.selectMessage(playerCause != null);
        if (rawMessage == null) return;

        PlaceholderContext.Builder builder = PlaceholderContext.builder()
            .with(CommonPlaceholders.PLAYER.resolver(context.player()))
            .andThen(CommonPlaceholders.forPlaceholderAPI(context.player()));

        if (sourceName != null) {
            String finalSourceName = sourceName;
            Entity itemHolder = causingEntity == null ? playerCause : causingEntity;
            builder.with(SLPlaceholders.GENERIC_SOURCE, () -> finalSourceName);
            builder.with(SLPlaceholders.GENERIC_ITEM, () -> itemHolder == null ? "" : getItemName(itemHolder));
        }

        String deathMessage = builder.build().apply(rawMessage);

        EventUtils.getAdapter().setDeathMessage(event, NightMessage.parse(deathMessage));
    }

    public void trackAnvilPlacement(@NotNull Location location, @NotNull Player player) {
        if (this.anvilPlacements.size() >= ANVIL_TRACK_MAX_SIZE) {
            this.purgeExpiredPlacements();
        }
        this.anvilPlacements.put(UUID.randomUUID(), new TrackedAnvil(location.clone(), player.getUniqueId(), player.getName(), System.currentTimeMillis()));
    }

    @Nullable
    public TrackedAnvil findAnvilPlacer(@NotNull Location location) {
        TrackedAnvil best = null;
        long now = System.currentTimeMillis();

        for (var entry : this.anvilPlacements.entrySet()) {
            TrackedAnvil tracked = entry.getValue();
            if (now - tracked.timestamp() > ANVIL_TRACK_TTL_MILLIS) {
                this.anvilPlacements.remove(entry.getKey());
                continue;
            }
            if (tracked.location().getWorld() == null || !tracked.location().getWorld().equals(location.getWorld())) continue;
            if (tracked.location().distanceSquared(location) > ANVIL_TRACK_RADIUS_SQUARED) continue;
            if (best == null || tracked.timestamp() > best.timestamp()) {
                best = tracked;
            }
        }
        return best;
    }

    private void purgeExpiredPlacements() {
        long now = System.currentTimeMillis();
        this.anvilPlacements.values().removeIf(tracked -> now - tracked.timestamp() > ANVIL_TRACK_TTL_MILLIS);
    }

    public record TrackedAnvil(@NotNull Location location, @NotNull UUID placerId, @NotNull String placerName, long timestamp) {

    }

    @Nullable
    public DeathMessage getMessage(@NotNull DeathContext context) {
        DeathMessage deathMessage = null;

        Entity causingEntity = context.causingEntity();
        Entity directEnttiy = context.directEntity();

        if (directEnttiy != null) {
            deathMessage = this.settings.getEntityTypeMessages().get(directEnttiy.getType());
        }
        if (causingEntity != null) {
            if (deathMessage == null) {
                deathMessage = this.settings.getEntityTypeMessages().get(causingEntity.getType());
            }
        }
        if (deathMessage == null) {
            deathMessage = this.settings.getDamageTypeMessages().get(context.damageSource().getDamageType());
        }
        return deathMessage;
    }

    @NotNull
    private static String getName(@NotNull Entity entity) {
        if (entity instanceof Player player) {
            return Players.getDisplayNameSerialized(player);
        }

        return EntityUtil.getNameSerialized(entity);
    }

    @NotNull
    private static String getItemName(@NotNull Entity entity) {
        if (!(entity instanceof LivingEntity living)) return "";

        EntityEquipment equipment = living.getEquipment();
        if (equipment == null) return "";

        ItemStack item = equipment.getItemInMainHand();
        return ItemUtil.getNameSerialized(item);
    }
}
