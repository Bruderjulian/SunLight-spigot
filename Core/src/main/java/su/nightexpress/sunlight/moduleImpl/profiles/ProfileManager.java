package su.nightexpress.sunlight.moduleImpl.profiles;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import su.nightexpress.nightcore.integration.currency.EconomyBridge;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandKey;
import su.nightexpress.sunlight.moduleImpl.freeze.FreezeModule;
import su.nightexpress.sunlight.moduleImpl.glow.GlowModule;
import su.nightexpress.sunlight.moduleImpl.nick.NickModule;
import su.nightexpress.sunlight.hook.combat.CombatTracker;
import su.nightexpress.sunlight.moduleImpl.profiles.config.ProfilesPerms;
import su.nightexpress.sunlight.moduleImpl.profiles.event.ProfileSwitchEvent;
import su.nightexpress.sunlight.moduleImpl.vanish.VanishModule;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserProperty;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;
import su.nightexpress.sunlight.utils.TimeUtil;
import su.nightexpress.sunlight.utils.Utils;

/**
 * Owns all profiles: in-memory cache, file persistence, validation and the
 * safe-switch pipeline.
 */
public class ProfileManager {

    public static final int MAX_SLOTS_HARD_CAP = 20;
    public static final String DEFAULT_PROFILE_ID = "main";
    public static final String DEFAULT_PROFILE_NAME = "Main";

    private static final String NAME_PATTERN = "[a-zA-Z0-9_]{3,16}";

    private final SunLightPlugin plugin;
    private final ProfilesSettings settings;
    private final ProfileScopeRegistry scopes;
    private final ProfileStore store;
    private final CombatTracker combat;

    private final Map<UUID, Map<String, PlayerProfile>> cache = new ConcurrentHashMap<>();

    public ProfileManager(SunLightPlugin plugin, ProfilesSettings settings, ProfileScopeRegistry scopes,
                          CombatTracker combat, File moduleFolder) {
        this.plugin = plugin;
        this.settings = settings;
        this.scopes = scopes;
        this.combat = combat;
        this.store = new ProfileStore(moduleFolder);
    }

    // ------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------

    public Map<String, PlayerProfile> getProfiles(Player player) {
        return this.getProfiles(player.getUniqueId());
    }

    public synchronized Map<String, PlayerProfile> getProfiles(UUID ownerId) {
        return this.cache.computeIfAbsent(ownerId, key -> {
            Map<String, PlayerProfile> loaded = this.store.load(key);
            return new LinkedHashMap<>(loaded);
        });
    }

    public List<PlayerProfile> getProfileList(UUID ownerId) {
        return new ArrayList<>(this.getProfiles(ownerId).values());
    }

    public CombatTracker getCombat() {
        return this.combat;
    }
    public PlayerProfile findByIdOrName(UUID ownerId, String query) {
        if (query == null) return null;
        Map<String, PlayerProfile> profiles = this.getProfiles(ownerId);
        PlayerProfile direct = profiles.get(Utils.lowercase(query));
        if (direct != null) return direct;
        for (PlayerProfile profile : profiles.values()) {
            if (profile.getName().equalsIgnoreCase(query)) return profile;
        }
        return null;
    }

    public List<String> profileNames(UUID ownerId) {
        List<String> names = new ArrayList<>();
        for (PlayerProfile profile : this.getProfileList(ownerId)) {
            names.add(profile.getName());
        }
        return names;
    }

    private void persist(UUID ownerId) {
        Map<String, PlayerProfile> profiles = this.cache.get(ownerId);
        if (profiles == null) return;
        this.store.save(ownerId, new LinkedHashMap<>(profiles));
    }

    // ------------------------------------------------------------------
    // Active pointer (stored globally on SunUser, never sliced per-profile)
    // ------------------------------------------------------------------

    public String getActiveId(SunUser user) {
        String active = user.getPropertyOr(ProfilesModule.ACTIVE_PROFILE, "");
        if (active == null || active.isBlank()) return DEFAULT_PROFILE_ID;
        return Utils.lowercase(active);
    }

    public PlayerProfile getActive(UUID ownerId, SunUser user) {
        return this.getProfiles(ownerId).get(this.getActiveId(user));
    }

    public void setActiveId(SunUser user, String profileId) {
        user.setProperty(ProfilesModule.ACTIVE_PROFILE, Utils.lowercase(profileId));
        user.markDirty();
    }

    public int maxSlots(Player player) {
        int max = this.settings.getMaxPerPlayer();
        for (int slot = MAX_SLOTS_HARD_CAP; slot > max; slot--) {
            if (player.hasPermission(ProfilesPerms.MODULE + ".slots." + slot)) {
                return Math.min(slot, MAX_SLOTS_HARD_CAP);
            }
        }
        return Math.min(Math.max(1, max), MAX_SLOTS_HARD_CAP);
    }

    // ------------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------------

    public enum CreateResult {
        OK, LIMIT, EXISTS, INVALID_NAME
    }

    public synchronized CreateResult create(Player player, SunUser user, String name) {
        if (name == null || !name.matches(NAME_PATTERN)) return CreateResult.INVALID_NAME;
        Map<String, PlayerProfile> profiles = this.getProfiles(player.getUniqueId());
        if (profiles.size() >= this.maxSlots(player)) return CreateResult.LIMIT;
        for (PlayerProfile profile : profiles.values()) {
            if (profile.getName().equalsIgnoreCase(name)) return CreateResult.EXISTS;
        }
        String id = Utils.lowercase(name);
        if (profiles.containsKey(id)) return CreateResult.EXISTS;

        // Snapshot the current live state into the new profile so it starts
        // as a copy of "now" rather than an empty inventory.
        PlayerProfile profile = new PlayerProfile(player.getUniqueId(), id, name);
        profile.captureFrom(player, user, this.scopes, this.queryBalance(player), this.isEconomyAvailable());
        profiles.put(id, profile);
        this.persist(player.getUniqueId());
        return CreateResult.OK;
    }

    public enum RenameResult {
        OK, NOT_FOUND, EXISTS, INVALID_NAME
    }

    public synchronized RenameResult rename(UUID ownerId, String query, String newName) {
        if (newName == null || !newName.matches(NAME_PATTERN)) return RenameResult.INVALID_NAME;
        Map<String, PlayerProfile> profiles = this.getProfiles(ownerId);
        PlayerProfile profile = this.findByIdOrName(ownerId, query);
        if (profile == null) return RenameResult.NOT_FOUND;
        for (PlayerProfile other : profiles.values()) {
            if (other != profile && other.getName().equalsIgnoreCase(newName)) return RenameResult.EXISTS;
        }
        profiles.remove(profile.getId());
        profile.setName(newName);
        PlayerProfile renamed = new PlayerProfile(ownerId, Utils.lowercase(newName),
            newName, profile.getIcon(), profile.getDescription(),
            profile.getCreatedAt(), profile.getLastPlayed(), profile.getState(),
            profile.getPropertiesRaw());
        profiles.put(renamed.getId(), renamed);
        this.persist(ownerId);
        return RenameResult.OK;
    }

    public enum CloneResult {
        OK, NOT_FOUND, LIMIT, EXISTS, INVALID_NAME
    }

    public synchronized CloneResult cloneProfile(UUID ownerId, int maxSlots, String sourceQuery, String newName) {
        if (newName == null || !newName.matches(NAME_PATTERN)) return CloneResult.INVALID_NAME;
        Map<String, PlayerProfile> profiles = this.getProfiles(ownerId);
        PlayerProfile source = this.findByIdOrName(ownerId, sourceQuery);
        if (source == null) return CloneResult.NOT_FOUND;
        if (profiles.size() >= maxSlots) return CloneResult.LIMIT;
        for (PlayerProfile other : profiles.values()) {
            if (other.getName().equalsIgnoreCase(newName)) return CloneResult.EXISTS;
        }
        String id = Utils.lowercase(newName);
        if (profiles.containsKey(id)) return CloneResult.EXISTS;
        PlayerProfile copy = source.copyTo(id, newName);
        profiles.put(id, copy);
        this.persist(ownerId);
        return CloneResult.OK;
    }

    public synchronized boolean setIcon(UUID ownerId, String query, String icon) {
        PlayerProfile profile = this.findByIdOrName(ownerId, query);
        if (profile == null) return false;
        profile.setIcon(icon);
        this.persist(ownerId);
        return true;
    }

    public synchronized boolean setDescription(UUID ownerId, String query, String description) {
        PlayerProfile profile = this.findByIdOrName(ownerId, query);
        if (profile == null) return false;
        profile.setDescription(description);
        this.persist(ownerId);
        return true;
    }

    /**
     * Deletes every profile of a player. The next join (or an explicit
     * {@code ensureLoaded}) recreates a fresh {@code Main} profile.
     */
    public synchronized void resetAll(UUID ownerId) {
        this.cache.remove(ownerId);
        this.store.delete(ownerId);
    }

    public enum DeleteResult {
        OK, NOT_FOUND, ACTIVE, LAST
    }

    public synchronized DeleteResult delete(Player player, SunUser user, String query) {
        return this.deleteById(player.getUniqueId(), this.getActiveId(user), query);
    }

    /**
     * File-level delete that also works for offline players when their
     * active profile id is known (may be {@code null}).
     */
    public synchronized DeleteResult deleteById(UUID ownerId, String activeId, String query) {
        Map<String, PlayerProfile> profiles = this.getProfiles(ownerId);
        PlayerProfile profile = this.findByIdOrName(ownerId, query);
        if (profile == null) return DeleteResult.NOT_FOUND;
        if (profiles.size() <= 1) return DeleteResult.LAST;
        if (profile.getId().equals(activeId)) return DeleteResult.ACTIVE;
        profiles.remove(profile.getId());
        this.persist(ownerId);
        return DeleteResult.OK;
    }

    // ------------------------------------------------------------------
    // Join / quit
    // ------------------------------------------------------------------

    /**
     * Ensures the player has at least one profile. First-ever join migrates
     * the live state into {@code Main} so nothing is lost.
     */
    public synchronized void ensureLoaded(Player player, SunUser user) {
        Map<String, PlayerProfile> profiles = this.getProfiles(player.getUniqueId());
        if (profiles.isEmpty()) {
            PlayerProfile main = new PlayerProfile(player.getUniqueId(), DEFAULT_PROFILE_ID, DEFAULT_PROFILE_NAME);
            main.captureFrom(player, user, this.scopes, this.queryBalance(player), this.isEconomyAvailable());
            profiles.put(DEFAULT_PROFILE_ID, main);
            this.setActiveId(user, DEFAULT_PROFILE_ID);
            this.persist(player.getUniqueId());
            return;
        }
        String activeId = this.getActiveId(user);
        if (!profiles.containsKey(activeId)) {
            this.setActiveId(user, profiles.keySet().iterator().next());
        }
    }

    public synchronized void saveCurrent(Player player, SunUser user) {
        Map<String, PlayerProfile> profiles = this.getProfiles(player.getUniqueId());
        PlayerProfile active = profiles.get(this.getActiveId(user));
        if (active == null) return;
        active.captureFrom(player, user, this.scopes, this.queryBalance(player), this.isEconomyAvailable());
        this.persist(player.getUniqueId());
    }

    // ------------------------------------------------------------------
    // Switch pipeline
    // ------------------------------------------------------------------

    public enum SwitchBlock {
        NONE, NOT_FOUND, ALREADY_ACTIVE, COOLDOWN, DEAD, VANISHED, FROZEN, FLYING, COMBAT, CANCELLED, FAILED
    }

    public record SwitchResult(SwitchBlock block, long cooldownLeftMs) {
        public boolean isSuccess() {
            return this.block == SwitchBlock.NONE;
        }
    }

    public SwitchResult switchTo(Player player, SunUser user, String query, boolean bypassSafety) {
        Map<String, PlayerProfile> profiles = this.getProfiles(player.getUniqueId());
        PlayerProfile target = this.findByIdOrName(player.getUniqueId(), query);
        if (target == null) return new SwitchResult(SwitchBlock.NOT_FOUND, 0L);

        String activeId = this.getActiveId(user);
        if (activeId.equals(target.getId())) return new SwitchResult(SwitchBlock.ALREADY_ACTIVE, 0L);

        if (!bypassSafety) {
            SwitchBlock guard = this.checkGuards(player, user);
            if (guard != SwitchBlock.NONE) {
                return new SwitchResult(guard, guard == SwitchBlock.COOLDOWN ? this.cooldownLeftMs(user) : 0L);
            }
        }

        PlayerProfile from = profiles.get(activeId);
        ProfileSwitchEvent event = new ProfileSwitchEvent(player, from, target);
        this.plugin.getPluginManager().callEvent(event);
        if (event.isCancelled()) return new SwitchResult(SwitchBlock.CANCELLED, 0L);

        // 1. Snapshot current live state into the old profile first, so a
        // failed load can always roll back without item loss.
        if (from != null) {
            from.captureFrom(player, user, this.scopes, this.queryBalance(player), this.isEconomyAvailable());
        }

        try {
            // 2. Swap the per-profile UserProperty slice.
            target.applyPropertiesTo(user, this.scopes);
            this.applyResetPolicy(user);
            user.markDirty();

            // 3. Apply vanilla snapshot (inventory, vitals, gamemode...).
            Location destination = target.getState().apply(player, this.scopes);

            // 4. Economy delta towards the target balance.
            this.applyEconomy(player, target);

            // 5. Teleport last so position is exact after inventory is set.
            if (destination != null) {
                player.teleport(destination);
            }

            // 6. Re-apply visual modules from the new property values.
            this.refreshVisuals(player);

            this.setActiveId(user, target.getId());
            target.touch();
            this.persist(player.getUniqueId());

            int cooldown = this.settings.getSwitchCooldown();
            if (cooldown > 0 && !player.hasPermission(ProfilesPerms.BYPASS_COOLDOWN)) {
                user.setCommandCooldown(new CommandKey("profiles", "switch"), TimeUtil.createFutureTimestamp(cooldown));
            }
            return new SwitchResult(SwitchBlock.NONE, 0L);
        } catch (Exception exception) {
            exception.printStackTrace();
            // Roll back: restore the snapshot we took in step 1.
            try {
                if (from != null) {
                    from.applyPropertiesTo(user, this.scopes);
                    from.getState().apply(player, this.scopes);
                    this.refreshVisuals(player);
                }
            } catch (Exception rollbackError) {
                rollbackError.printStackTrace();
            }
            return new SwitchResult(SwitchBlock.FAILED, 0L);
        }
    }

    public static CommandKey switchCooldownKey() {
        return new CommandKey("profiles", "switch");
    }

    public long cooldownLeftMs(SunUser user) {
        Long expire = user.getCommandCooldown(switchCooldownKey());
        if (expire == null || TimeUtil.isPassed(expire)) return 0L;
        return Math.max(0L, expire - System.currentTimeMillis());
    }

    public SwitchBlock checkGuards(Player player, SunUser user) {
        if (this.settings.isBlockWhileDead() && player.isDead()) return SwitchBlock.DEAD;

        if (this.settings.isBlockWhileTagged() && !player.hasPermission(ProfilesPerms.BYPASS_SAFETY)
            && this.combat.isInCombat(player)) return SwitchBlock.COMBAT;

        if (this.settings.isBlockWhileFrozen() && !player.hasPermission(ProfilesPerms.BYPASS_SAFETY)) {
            var freeze = this.plugin.moduleManager().getByType(FreezeModule.class);
            if (freeze.isPresent() && freeze.get().isFrozen(player)) return SwitchBlock.FROZEN;
        }
        if (this.settings.isBlockWhileVanished() && !player.hasPermission(ProfilesPerms.BYPASS_SAFETY)) {
            var vanish = this.plugin.moduleManager().getByType(VanishModule.class);
            if (vanish.isPresent() && vanish.get().isVanished(player)) return SwitchBlock.VANISHED;
        }
        if (this.settings.isBlockWhileFlying() && player.isFlying() && !player.isOnGround()
            && !player.hasPermission(ProfilesPerms.BYPASS_SAFETY)) {
            return SwitchBlock.FLYING;
        }
        int cooldown = this.settings.getSwitchCooldown();
        if (cooldown > 0 && !player.hasPermission(ProfilesPerms.BYPASS_COOLDOWN)) {
            Long expire = user.getCommandCooldown(new CommandKey("profiles", "switch"));
            if (expire != null && !TimeUtil.isPassed(expire)) return SwitchBlock.COOLDOWN;
        }
        return SwitchBlock.NONE;
    }

    private void applyResetPolicy(SunUser user) {
        if (this.settings.isResetVanishOnSwitch()) {
            UserProperty<?> vanish = UserPropertyRegistry.getByName("vanish");
            if (vanish != null) user.removeProperty(vanish.getName());
        }
        if (this.settings.isResetFreezeOnSwitch()) {
            UserProperty<?> freeze = UserPropertyRegistry.getByName("freeze");
            if (freeze != null) user.removeProperty(freeze.getName());
        }
        if (this.settings.isResetGodOnSwitch()) {
            UserProperty<?> god = UserPropertyRegistry.getByName("god");
            if (god != null) user.removeProperty(god.getName());
        }
    }

    private void refreshVisuals(Player player) {
        this.plugin.runTask(() -> {
            try {
                this.plugin.moduleManager().getByType(VanishModule.class).ifPresent(module -> {
                    SunUser user = this.plugin.userManager().getOrFetch(player);
                    UserProperty<?> vanish = UserPropertyRegistry.getByName("vanish");
                    boolean state = vanish != null && Boolean.TRUE.equals(user.getPropertyOrDefault(
                        (UserProperty<Boolean>) vanish));
                    module.vanish(player, state);
                });
            } catch (Exception ignored) {
            }
            try {
                this.plugin.moduleManager().getByType(GlowModule.class).ifPresent(module -> module.refresh(player));
            } catch (Exception ignored) {
            }
            try {
                this.plugin.moduleManager().getByType(NickModule.class).ifPresent(module -> module.applyNickname(player));
            } catch (Exception ignored) {
            }
            try {
                this.plugin.nametagsProvider().ifPresent(provider -> provider.recompute(player));
            } catch (Exception ignored) {
            }
        });
    }

    // ------------------------------------------------------------------
    // Economy (best-effort via the shared Vault bridge)
    // ------------------------------------------------------------------

    private boolean isEconomyAvailable() {
        try {
            return EconomyBridge.api().hasVaultCurrency()
                && this.scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_ECONOMY);
        } catch (Exception exception) {
            return false;
        }
    }

    private double queryBalance(Player player) {
        if (!this.isEconomyAvailable()) return 0D;
        try {
            return EconomyBridge.api().queryBalance(player);
        } catch (Exception exception) {
            return 0D;
        }
    }

    private void applyEconomy(Player player, PlayerProfile target) {
        if (!this.scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_ECONOMY)) return;
        if (!target.getState().isEconomyTracked()) return;
        double current;
        try {
            if (!EconomyBridge.api().hasVaultCurrency()) return;
            current = EconomyBridge.api().queryBalance(player);
        } catch (Exception exception) {
            return;
        }
        double wanted = target.getState().getEconomyBalance();
        double delta = wanted - current;
        if (Math.abs(delta) < 0.005D) return;
        try {
            if (delta < 0D) {
                EconomyBridge.api().withdraw(player, -delta);
            } else {
                EconomyBridge.api().deposit(player, delta);
            }
        } catch (NoSuchMethodError | Exception exception) {
            this.plugin.warn("[Profiles] Economy deposit is not supported by the currency provider; "
                + "balance increase to " + wanted + " for " + player.getName() + " was skipped.");
        }
    }

    public String formatLastPlayed(long timestamp) {
        long ago = System.currentTimeMillis() - timestamp;
        if (ago < 60_000L) return "just now";
        long minutes = ago / 60_000L;
        if (minutes < 60L) return minutes + "m ago";
        long hours = minutes / 60L;
        if (hours < 24L) return hours + "h ago";
        return (hours / 24L) + "d ago";
    }
}
