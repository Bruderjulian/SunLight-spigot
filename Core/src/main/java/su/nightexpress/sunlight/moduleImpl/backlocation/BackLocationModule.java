package su.nightexpress.sunlight.moduleImpl.backlocation;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleContext;
import su.nightexpress.sunlight.moduleImpl.backlocation.command.BackCommandProvider;
import su.nightexpress.sunlight.moduleImpl.backlocation.command.DeathBackCommandProvider;
import su.nightexpress.sunlight.moduleImpl.backlocation.config.BackLocationLang;
import su.nightexpress.sunlight.moduleImpl.backlocation.config.BackLocationPerms;
import su.nightexpress.sunlight.moduleImpl.backlocation.config.BackLocationSettings;
import su.nightexpress.sunlight.moduleImpl.backlocation.data.LocationType;
import su.nightexpress.sunlight.moduleImpl.backlocation.data.StoredLocation;
import su.nightexpress.sunlight.moduleImpl.backlocation.listener.BackLocationListener;
import su.nightexpress.sunlight.teleport.*;
import su.nightexpress.sunlight.utils.EconomyUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BackLocationModule extends Module {

    private final TeleportManager teleportManager;
    private final BackLocationSettings settings;

    private final Map<UUID, Map<LocationType, StoredLocation>> locationMap;

    public BackLocationModule(ModuleContext context, TeleportManager teleportManager) {
        super(context);
        this.teleportManager = teleportManager;
        this.settings = new BackLocationSettings();
        this.locationMap = new HashMap<>();
    }

    @Override
    protected void loadModule(FileConfig config) {
        this.settings.load(config);
        this.plugin.injectLang(BackLocationLang.class);
        this.registerCommands();

        this.addListener(new BackLocationListener(this.plugin, this));
    }

    @Override
    protected void unloadModule() {
        this.locationMap.clear();
    }

    @Override
    protected void registerPermissions(PermissionTree root) {
        root.merge(BackLocationPerms.ROOT);
    }

    @Override

    public String getPermissionNamespace() {
        return "backlocation";
    }

    protected void registerCommands() {
        if (this.settings.cacheTeleports.get()) {
            this.commandRegistry.addProvider("back", new BackCommandProvider(this.plugin, this, this.userManager),
                    this);
        }
        if (this.settings.cacheDeaths.get()) {
            this.commandRegistry.addProvider("deathback",
                    new DeathBackCommandProvider(this.plugin, this, this.userManager), this);
        }
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        registry.register("backlocation_has_previous",
                (player, payload) -> CoreLang.STATE_YES_NO.get(this.hasLocation(player, LocationType.PREVIOUS)));
        registry.register("backlocation_has_death",
                (player, payload) -> CoreLang.STATE_YES_NO.get(this.hasLocation(player, LocationType.DEATH)));

        registry.register("backlocation_previous_expire_in",
                (player, payload) -> this.formatExpireInPlaceholder(player, LocationType.PREVIOUS));
        registry.register("backlocation_death_expire_in",
                (player, payload) -> this.formatExpireInPlaceholder(player, LocationType.DEATH));
    }

    private String formatExpireInPlaceholder(Player player, LocationType type) {
        if (this.hasLocation(player, type)) {
            return TimeFormats.formatDuration(this.getExpireDate(player, type), TimeFormatType.LITERAL);
        }
        return CoreLang.OTHER_NONE.text();
    }

    public void handleTeleport(Player player, PlayerTeleportEvent event) {
        if (!this.settings.cacheTeleports.get())
            return;

        Location destination = event.getTo();
        World world = player.getWorld();

        if (!player.hasPermission(BackLocationPerms.BYPASS_PREVIOUS_CAUSES) && this.isDisabledCause(event.getCause())) {
            return;
        }
        if (!player.hasPermission(BackLocationPerms.BYPASS_PREVIOUS_WORLDS)
                && this.isDisabledWorld(world, LocationType.PREVIOUS)) {
            return;
        }

        Location from = event.getFrom();
        World toWorld = destination.getWorld();
        int minDifference = this.settings.teleportMinDistanceDifference.get();
        int requiredDistance = minDifference * minDifference;

        if (world == toWorld && from.distanceSquared(destination) < requiredDistance) {
            if (this.hasLocation(player, LocationType.PREVIOUS))
                return;
        }

        this.saveLocation(player, from, LocationType.PREVIOUS);
    }

    public void handleDeath(Player player, PlayerDeathEvent event) {
        if (!this.settings.cacheDeaths.get())
            return;

        if (!player.hasPermission(BackLocationPerms.BYPASS_DEATH_WORLDS)
                && this.isDisabledWorld(player.getWorld(), LocationType.DEATH)) {
            return;
        }

        this.saveLocation(player, player.getLocation(), LocationType.DEATH);
    }

    public boolean isDisabledWorld(World world, LocationType type) {
        return this.isDisabledWorld(world.getName(), type);
    }

    public boolean isDisabledWorld(String name, LocationType type) {
        Set<String> disabled = type == LocationType.PREVIOUS ? this.settings.teleportWorldBlacklist.get()
                : this.settings.deathWorldBlacklist.get();
        return disabled.contains(name);
    }

    public boolean isDisabledCause(PlayerTeleportEvent.TeleportCause cause) {
        return this.settings.ignoredTeleportCauses.get().contains(cause);
    }

    public int getDuration(LocationType type) {
        return type == LocationType.PREVIOUS ? this.settings.teleportCacheExpireTime.get()
                : this.settings.deathCacheExpireTime.get();
    }

    public boolean hasLocation(Player player, LocationType type) {
        return this.hasLocation(player.getUniqueId(), type);
    }

    public boolean hasLocation(UUID playerId, LocationType type) {
        return this.getLocation(playerId, type) != null;
    }

    public long getExpireDate(Player player, LocationType type) {
        StoredLocation location = this.getLocation(player, type);
        return location == null ? 0L : location.getExpireDate();
    }

    public Map<LocationType, StoredLocation> getLocationMap(Player player) {
        return this.getLocationMap(player.getUniqueId());
    }

    public Map<LocationType, StoredLocation> getLocationMap(UUID playerId) {
        var map = this.locationMap.computeIfAbsent(playerId, k -> new HashMap<>());
        map.values().removeIf(location -> location.isExpired() || !location.isValid());

        return map;
    }

    public StoredLocation getLocation(Player player, LocationType type) {
        return this.getLocation(player.getUniqueId(), type);
    }

    public StoredLocation getLocation(UUID playerId, LocationType type) {
        return this.getLocationMap(playerId).get(type);
    }

    public void saveLocation(Player player, Location location, LocationType type) {
        this.saveLocation(player.getUniqueId(), location, type);
    }

    public void saveLocation(UUID playerId, Location location, LocationType type) {
        World world = location.getWorld();
        if (world == null)
            return;

        int duration = this.getDuration(type);

        StoredLocation storedLocation = new StoredLocation(world.getName(), location.getX(), location.getY(),
                location.getZ(), duration);
        this.getLocationMap(playerId).put(type, storedLocation);
    }

    public boolean teleportToLocation(Player player, LocationType type) {
        return this.teleportToLocation(player, type, false);
    }

    public boolean teleportToLocation(Player player, LocationType type, boolean silent) {
        boolean isPrevious = type == LocationType.PREVIOUS;
        StoredLocation storedLocation = this.getLocation(player, type);
        Location location = storedLocation == null ? null : storedLocation.toLocation();

        if (storedLocation == null || location == null) {
            if (!silent)
                this.sendPrefixed(isPrevious ? BackLocationLang.PREVIOUS_ERROR_NOTHING_NOTIFY
                        : BackLocationLang.DEATH_ERROR_NOTHING_NOTIFY, player);
            return false;
        }

        TeleportType teleportType = type == LocationType.DEATH ? TeleportType.DEATH_LOCATION
                : TeleportType.PREVIOUS_LOCATION;

        double cost = this.settings.getTeleportCost(type);
        boolean charge = cost > 0D && !EconomyUtils.hasBypass(player, BackLocationPerms.BYPASS_COST) && EconomyUtils
                .hasCurrency();

        if (charge && !EconomyUtils.canAfford(player, cost)) {
            if (!silent)
                this.sendPrefixed(Lang.COST_ERROR_NOT_ENOUGH_FUNDS, player, builder -> builder
                        .with(SLPlaceholders.GENERIC_AMOUNT, () -> EconomyUtils.format(cost)));
            return false;
        }

        TeleportContext teleportContext = TeleportContext.builder(this, player, location)
                .withFlag(TeleportFlag.KEEP_DIRECTION)
                .callback(() -> {
                    if (charge)
                        EconomyUtils.withdraw(player, cost);

                    if (!silent)
                        this.sendPrefixed(isPrevious ? BackLocationLang.PREVIOUS_TELEPORT_NOTIFY
                                : BackLocationLang.DEATH_TELEPORT_NOTIFY, player);
                })
                .build();

        return this.teleportManager.teleport(teleportContext, teleportType);
    }

    public BackLocationSettings getSettings() {
        return this.settings;
    }
}
