package su.nightexpress.sunlight.moduleImpl.profiles;

import java.io.File;
import java.util.List;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.sunlight.SLPlaceholders;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.provider.ProfilesProvider;
import su.nightexpress.sunlight.hook.luckperms.ProfileContextHook;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.profiles.command.ProfilesCommandProvider;
import su.nightexpress.sunlight.config.Config;
import su.nightexpress.sunlight.hook.combat.CombatTracker;
import su.nightexpress.sunlight.moduleImpl.profiles.config.ProfilesLang;
import su.nightexpress.sunlight.moduleImpl.profiles.config.ProfilesPerms;
import su.nightexpress.sunlight.moduleImpl.profiles.dialog.ProfilesDialogKeys;
import su.nightexpress.sunlight.moduleImpl.profiles.dialog.impl.ProfileDescribeDialog;
import su.nightexpress.sunlight.moduleImpl.profiles.dialog.impl.ProfileIconDialog;
import su.nightexpress.sunlight.moduleImpl.profiles.dialog.impl.ProfileRenameDialog;
import su.nightexpress.sunlight.moduleImpl.profiles.menu.ProfileListMenu;
import su.nightexpress.sunlight.moduleImpl.profiles.menu.ProfileOptionsMenu;
import su.nightexpress.sunlight.moduleImpl.profiles.listener.ProfilesListener;
import su.nightexpress.sunlight.moduleImpl.profiles.warmup.ProfileWarmupManager;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserProperty;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;
import su.nightexpress.sunlight.utils.EconomyUtils;

public class ProfilesModule extends Module implements ProfilesProvider {

    /**
     * Active profile id. Global-only: resolved to {@code main} when absent and
     * never included in per-profile snapshots.
     */
    public static final UserProperty<String> ACTIVE_PROFILE = UserProperty.create("profile_active", String.class, "", true);

    private final ProfilesSettings settings;
    private final ProfileScopeRegistry scopes;
    private final CombatTracker combat;
    private final ProfileContextHook luckPerms;
    private ProfileManager manager;
    private ProfileWarmupManager warmup;
    private ProfileListMenu listMenu;

    public ProfilesModule(ModuleDefinition<ProfilesModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new ProfilesSettings();
        this.scopes = new ProfileScopeRegistry();
        this.combat = CombatTracker.withGlobalConfig();
        this.luckPerms = new ProfileContextHook(player -> {
            try {
                return this.userManager.getOrFetch(player).getPropertyOr(ACTIVE_PROFILE, "");
            } catch (Exception exception) {
                return "";
            }
        });
    }

    @Override
    protected void loadModule(FileConfig config) {
        this.settings.load(config);
        this.scopes.load(config);
        this.combat.setConfiguredHook(Config.COMBAT_HOOK.get());
        config.saveChanges();

        this.plugin.injectLang(ProfilesLang.class);
        UserPropertyRegistry.register(ACTIVE_PROFILE);

        File moduleFolder = new File(this.getSystemPath());
        this.manager = new ProfileManager(this.plugin, this.settings, this.scopes, this.combat, moduleFolder);
        this.warmup = new ProfileWarmupManager(this.plugin, this);
        this.listMenu = new ProfileListMenu(this.plugin, this);

        this.dialogRegistry.register(ProfilesDialogKeys.PROFILE_RENAME, () -> new ProfileRenameDialog(this));
        this.dialogRegistry.register(ProfilesDialogKeys.PROFILE_ICON, () -> new ProfileIconDialog(this));
        this.dialogRegistry.register(ProfilesDialogKeys.PROFILE_DESCRIBE, () -> new ProfileDescribeDialog(this));

        this.addListener(new ProfilesListener(this.plugin, this));
        this.commandRegistry.addProvider(new ProfilesCommandProvider(this));

        if (this.settings.isLuckPermsContext()) {
            this.luckPerms.register();
            if (this.luckPerms.isAvailable()) {
                this.info("LuckPerms context 'profile' registered.");
            }
        }

        if (this.combat.isHooked()) {
            this.info("Combat hook: " + this.combat.getActiveHook().getId() + ".");
        } else {
            this.info("No combat plugin hooked; tag-guard inactive until PvPManager or CombatLogX is installed.");
        }
    }

    @Override
    protected void unloadModule() {
        if (this.warmup != null) this.warmup.shutdown();
        this.luckPerms.unregister();
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        registry.register("profile_active", (player, payload) -> {
            SunUser user = this.userManager.getOrFetch(player);
            String activeId = this.manager.getActiveId(user);
            PlayerProfile profile = this.manager.getProfiles(player.getUniqueId()).get(activeId);
            return profile == null ? activeId : profile.getName();
        });
        registry.register("profile_count", (player, payload) -> {
            return String.valueOf(this.manager.getProfiles(player.getUniqueId()).size());
        });
        registry.register("profile_max", (player, payload) -> {
            return String.valueOf(this.manager.maxSlots(player));
        });
        registry.register("profile_in_combat", (player, payload) -> {
            return String.valueOf(this.combat.isInCombat(player));
        });
        registry.register("profile_cooldown", (player, payload) -> {
            SunUser cooldownUser = this.userManager.getOrFetch(player);
            return String.valueOf(this.manager.cooldownLeftMs(cooldownUser) / 1000L);
        });
    }

    public ProfilesSettings getSettings() {
        return this.settings;
    }

    public ProfileScopeRegistry getScopes() {
        return this.scopes;
    }

    public ProfileManager getManager() {
        return this.manager;
    }

    public CombatTracker getCombat() {
        return this.combat;
    }

    public ProfileWarmupManager getWarmup() {
        return this.warmup;
    }

    public ProfileContextHook getLuckPerms() {
        return this.luckPerms;
    }

    public boolean openProfilesMenu(Player player) {
        return this.listMenu.open(player);
    }

    public boolean openProfileOptions(Player player, String profileId) {
        return new ProfileOptionsMenu(this.plugin, this, profileId).open(player);
    }

    /**
     * Requests a profile switch: validates guards and affordability, then
     * either starts the warmup or executes immediately. Shared by the
     * command and the GUI so both behave identically.
     *
     * @return true when the switch started (or completed instantly).
     */
    public boolean requestSwitch(Player player, String query) {
        SunUser user = this.userManager.getOrFetch(player);
        PlayerProfile target = this.manager.findByIdOrName(player.getUniqueId(), query);
        if (target == null) {
            this.sendPrefixed(ProfilesLang.NOT_FOUND, player,
                builder -> builder.with(SLPlaceholders.GENERIC_NAME, () -> query));
            return false;
        }
        if (this.manager.getActiveId(user).equals(target.getId())) {
            this.sendPrefixed(ProfilesLang.ALREADY_ACTIVE, player,
                builder -> builder.with(SLPlaceholders.GENERIC_NAME, () -> target.getName()));
            return false;
        }

        ProfileManager.SwitchBlock guard = this.manager.checkGuards(player, user);
        if (guard != ProfileManager.SwitchBlock.NONE) {
            long left = guard == ProfileManager.SwitchBlock.COOLDOWN ? this.manager.cooldownLeftMs(user) : 0L;
            this.sendSwitchFeedback(player, query, new ProfileManager.SwitchResult(guard, left));
            return false;
        }

        double cost = this.getSwitchCost(player);
        if (cost > 0D && !this.canAffordSwitch(player, cost)) {
            this.sendPrefixed(ProfilesLang.SWITCH_CANT_AFFORD, player,
                builder -> builder.with(SLPlaceholders.GENERIC_AMOUNT, () -> EconomyUtils.format(cost)));
            return false;
        }

        int warmupSeconds = this.getSwitchWarmup(player);
        if (warmupSeconds > 0) {
            if (this.warmup.hasPending(player)) {
                this.sendPrefixed(ProfilesLang.WARMUP_ALREADY, player);
                return false;
            }
            this.warmup.request(player, target.getId(), warmupSeconds);
            return true;
        }
        return this.executeSwitch(player, target.getId());
    }

    /**
     * Executes a guarded switch immediately (cost is charged, effects play).
     * Called directly and by the warmup on completion, which re-validates
     * every guard inside {@code ProfileManager.switchTo}.
     */
    public boolean executeSwitch(Player player, String query) {
        SunUser user = this.userManager.getOrFetch(player);
        boolean bypassSafety = player.hasPermission(ProfilesPerms.BYPASS_SAFETY);

        double cost = this.getSwitchCost(player);
        boolean charged = false;
        if (cost > 0D && !bypassSafety) {
            if (!this.canAffordSwitch(player, cost)) {
                this.sendPrefixed(ProfilesLang.SWITCH_CANT_AFFORD, player,
                    builder -> builder.with(SLPlaceholders.GENERIC_AMOUNT, () -> EconomyUtils.format(cost)));
                return false;
            }
            EconomyUtils.withdraw(player, cost);
            charged = true;
        }

        ProfileManager.SwitchResult result = this.manager.switchTo(player, user, query, bypassSafety);
        if (!result.isSuccess() && charged) {
            try {
                su.nightexpress.nightcore.integration.currency.EconomyBridge.api().deposit(player, cost);
            } catch (Throwable ignored) {
            }
            return this.sendSwitchFeedback(player, query, result);
        }

        if (result.isSuccess()) {
            if (charged) {
                PlayerProfile target = this.manager.findByIdOrName(player.getUniqueId(), query);
                String name = target == null ? query : target.getName();
                this.sendPrefixed(ProfilesLang.SWITCH_CHARGED, player, builder -> builder
                    .with(SLPlaceholders.GENERIC_AMOUNT, () -> EconomyUtils.format(cost))
                    .with(SLPlaceholders.GENERIC_NAME, () -> name));
            }
            this.playSwitchEffects(player, query);
            this.luckPerms.signalUpdate(player);
        }
        return this.sendSwitchFeedback(player, query, result);
    }

    /**
     * Legacy alias kept for the command layer; routes through the pipeline.
     */
    public boolean switchToProfile(Player player, String query) {
        return this.requestSwitch(player, query);
    }

    /**
     * Forces another (online) player's profile, bypassing guards, warmup and
     * cost. Used by the admin command.
     */
    public boolean adminSwitch(org.bukkit.command.CommandSender admin, Player target, String query) {
        SunUser user = this.userManager.getOrFetch(target);
        ProfileManager.SwitchResult result = this.manager.switchTo(target, user, query, true);
        if (result.isSuccess()) {
            this.playSwitchEffects(target, query);
            this.luckPerms.signalUpdate(target);
            PlayerProfile switched = this.manager.findByIdOrName(target.getUniqueId(), query);
            String name = switched == null ? query : switched.getName();
            this.sendPrefixed(ProfilesLang.ADMIN_SWITCH_NOTIFY, target,
                builder -> builder.with(SLPlaceholders.GENERIC_NAME, () -> name));
            this.sendPrefixed(ProfilesLang.SWITCH_NOTIFY, admin,
                builder -> builder.with(SLPlaceholders.GENERIC_NAME, () -> name));
            return true;
        }
        if (result.block() == ProfileManager.SwitchBlock.NOT_FOUND) {
            this.sendPrefixed(ProfilesLang.NOT_FOUND, admin,
                builder -> builder.with(SLPlaceholders.GENERIC_NAME, () -> query));
        } else {
            this.sendPrefixed(ProfilesLang.SWITCH_FAILED, admin);
        }
        return false;
    }

    private boolean sendSwitchFeedback(Player player, String query, ProfileManager.SwitchResult result) {
        switch (result.block()) {
            case NONE -> {
                PlayerProfile target = this.manager.findByIdOrName(player.getUniqueId(), query);
                String name = target == null ? query : target.getName();
                this.sendPrefixed(ProfilesLang.SWITCH_NOTIFY, player,
                    builder -> builder.with(SLPlaceholders.GENERIC_NAME, () -> name));
                return true;
            }
            case NOT_FOUND -> this.sendPrefixed(ProfilesLang.NOT_FOUND, player,
                builder -> builder.with(SLPlaceholders.GENERIC_NAME, () -> query));
            case ALREADY_ACTIVE -> {
                PlayerProfile target = this.manager.findByIdOrName(player.getUniqueId(), query);
                String name = target == null ? query : target.getName();
                this.sendPrefixed(ProfilesLang.ALREADY_ACTIVE, player,
                    builder -> builder.with(SLPlaceholders.GENERIC_NAME, () -> name));
            }
            case COOLDOWN -> this.sendPrefixed(ProfilesLang.SWITCH_COOLDOWN, player,
                builder -> builder.with(SLPlaceholders.GENERIC_TIME, () -> formatSeconds(result.cooldownLeftMs())));
            case DEAD -> this.sendPrefixed(ProfilesLang.SWITCH_BLOCKED_DEAD, player);
            case COMBAT -> this.sendPrefixed(ProfilesLang.SWITCH_BLOCKED_COMBAT, player);
            case VANISHED -> this.sendPrefixed(ProfilesLang.SWITCH_BLOCKED_VANISHED, player);
            case FROZEN -> this.sendPrefixed(ProfilesLang.SWITCH_BLOCKED_FROZEN, player);
            case FLYING -> this.sendPrefixed(ProfilesLang.SWITCH_BLOCKED_FLYING, player);
            case CANCELLED, FAILED -> this.sendPrefixed(ProfilesLang.SWITCH_FAILED, player);
        }
        return false;
    }

    public double getSwitchCost(Player player) {
        double cost = this.settings.getSwitchCost();
        if (cost <= 0D) return 0D;
        if (!EconomyUtils.hasCurrency()) return 0D;
        if (EconomyUtils.hasBypass(player, this)) return 0D;
        return cost;
    }

    public int getSwitchWarmup(Player player) {
        int warmupSeconds = this.settings.getSwitchWarmup();
        if (warmupSeconds <= 0) return 0;
        if (player.hasPermission(ProfilesPerms.BYPASS_WARMUP)) return 0;
        return warmupSeconds;
    }

    private boolean canAffordSwitch(Player player, double cost) {
        if (!EconomyUtils.hasCurrency()) return true;
        try {
            return EconomyUtils.canAfford(player, cost);
        } catch (Exception exception) {
            return true;
        }
    }

    private void playSwitchEffects(Player player, String query) {
        PlayerProfile target = this.manager.findByIdOrName(player.getUniqueId(), query);
        String name = target == null ? query : target.getName();

        if (this.settings.isEffectsTitle()) {
            String title = colorize(this.settings.getEffectsTitleText().replace("%name%", name));
            String subtitle = colorize(this.settings.getEffectsSubtitleText().replace("%name%", name));
            try {
                player.sendTitle(title, subtitle.isBlank() ? null : subtitle, 10, 40, 10);
            } catch (Exception ignored) {
            }
        }

        String soundName = this.settings.getEffectsSound();
        if (soundName != null && !soundName.isBlank()) {
            try {
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName.trim().toUpperCase(java.util.Locale.ROOT));
                player.playSound(player.getLocation(), sound,
                    this.settings.getEffectsVolume(), this.settings.getEffectsPitch());
            } catch (Exception ignored) {
            }
        }

        String particleName = this.settings.getEffectsParticles();
        int count = this.settings.getEffectsParticleCount();
        if (particleName != null && !particleName.isBlank() && count > 0) {
            try {
                org.bukkit.Particle particle = org.bukkit.Particle.valueOf(particleName.trim().toUpperCase(java.util.Locale.ROOT));
                player.spawnParticle(particle, player.getLocation().clone().add(0D, 1D, 0D), count);
            } catch (Exception ignored) {
            }
        }
    }

    private static String colorize(String text) {
        if (text == null) return "";
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }

    private static String formatSeconds(long millis) {
        long seconds = Math.max(0L, millis + 999L) / 1000L;
        if (seconds < 60L) return seconds + "s";
        long minutes = seconds / 60L;
        long rest = seconds % 60L;
        return rest == 0L ? minutes + "m" : minutes + "m " + rest + "s";
    }

    @Override
    public @NotNull String getActiveProfileId(@NotNull Player player) {
        return this.manager.getActiveId(this.userManager.getOrFetch(player));
    }

    @Override
    public @NotNull List<String> getProfileNames(@NotNull Player player) {
        return this.manager.profileNames(player.getUniqueId());
    }

    @Override
    public int getProfileCount(@NotNull Player player) {
        return this.manager.getProfiles(player.getUniqueId()).size();
    }
}
