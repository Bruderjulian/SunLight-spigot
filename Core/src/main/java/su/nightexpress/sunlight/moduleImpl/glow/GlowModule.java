package su.nightexpress.sunlight.moduleImpl.glow;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.api.provider.GlowProvider;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.glow.command.GlowCommandProvider;
import su.nightexpress.sunlight.moduleImpl.glow.config.GlowLang;
import su.nightexpress.sunlight.moduleImpl.glow.config.GlowPerms;
import su.nightexpress.sunlight.moduleImpl.glow.event.PlayerGlowChangeEvent;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;
import su.nightexpress.sunlight.utils.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GlowModule extends Module implements GlowProvider {

    private static final String TEAM_PREFIX = "slglow_";

    private final GlowSettings settings;
    private final Map<UUID, GlowAnimation> animations;
    private final Map<UUID, String> previousTeams;

    public GlowModule(ModuleDefinition<GlowModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new GlowSettings();
        this.animations = new ConcurrentHashMap<>();
        this.previousTeams = new ConcurrentHashMap<>();
    }

    @Override
    protected void loadModule(FileConfig config) {
        this.settings.load(config);
        this.plugin.injectLang(GlowLang.class);
        UserPropertyRegistry.register(GlowProperties.GLOW);

        this.addListener(new GlowListener(this.plugin, this));
        this.addTask(this::tickAnimations, this.settings.getUpdateInterval());

        this.plugin.runTask(() -> Utils.onlinePlayers().forEach(player -> {
            if (this.settings.isRestoreOnJoin()) {
                this.applyGlow(player);
            }
        }));
    }

    @Override
    protected void unloadModule() {
        Utils.onlinePlayers().forEach(player -> this.removeGlowVisuals(player, false));
        this.animations.clear();
        this.previousTeams.clear();
    }

    private void removeGlowVisuals(Player player, boolean unused) {
        this.animations.remove(player.getUniqueId());
        this.removeFromGlowTeams(player);
        player.setGlowing(false);
        this.restorePreviousTeam(player);
    }

    @Override
    protected void registerPermissions(PermissionTree root) {
        root.merge(GlowPerms.MODULE);
    }

    @Override
    protected void registerCommands() {
        this.commandRegistry.addProvider("glow", new GlowCommandProvider(this.plugin, this, this.userManager), this);
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        registry.register("glow_state", (player, payload) -> CoreLang.STATE_YES_NO.get(this.hasGlow(player)));
        registry.register("glow_color", (player, payload) -> {
            String id = this.getGlow(player);
            if (id == null) return "";
            GlowEffect effect = this.getEffect(id);
            return effect == null ? "" : effect.getName();
        });
    }

    public GlowSettings getSettings() {
        return this.settings;
    }

    public GlowEffect getEffect(@Nullable String id) {
        if (id == null || id.isBlank()) return null;
        return this.settings.getEffects().get(Utils.lowercase(id));
    }

    public Map<String, GlowEffect> getEffects() {
        return this.settings.getEffects();
    }

    @Override
    public boolean hasGlow(@NotNull Player player) {
        return this.getGlow(player) != null;
    }

    @Override
    public @Nullable String getGlow(@NotNull Player player) {
        SunUser user = this.userManager.getOrFetch(player);
        String id = user.getPropertyOrDefault(GlowProperties.GLOW);
        if (id == null || id.isBlank()) return null;
        if (this.getEffect(id) == null) return null;
        return Utils.lowercase(id);
    }

    public @Nullable String getStoredGlow(@NotNull SunUser user) {
        String id = user.getPropertyOrDefault(GlowProperties.GLOW);
        if (id == null || id.isBlank()) return null;
        return Utils.lowercase(id);
    }

    @Override
    public void setGlow(@NotNull Player player, @Nullable String effectId) {
        SunUser user = this.userManager.getOrFetch(player);
        this.setGlow(user, player, effectId);
    }

    @Override
    public void clearGlow(@NotNull Player player) {
        this.setGlow(player, null);
    }

    public void setGlow(@NotNull SunUser user, @Nullable String effectId) {
        Player target = user.player().orElse(null);
        if (target != null) {
            this.setGlow(user, target, effectId);
        } else {
            String normalized = effectId == null ? null : Utils.lowercase(effectId);
            if (normalized == null) {
                user.removeProperty(GlowProperties.GLOW);
            } else {
                user.setProperty(GlowProperties.GLOW, normalized);
            }
            user.markDirty();
        }
    }

    public void setGlow(@NotNull SunUser user, @NotNull Player player, @Nullable String effectId) {
        String oldId = this.getStoredGlow(user);
        String normalized = (effectId == null || effectId.isBlank()) ? null : Utils.lowercase(effectId);

        boolean same = (oldId == null && normalized == null) || (oldId != null && oldId.equals(normalized));
        if (same && normalized != null) {
            this.applyGlow(player);
            return;
        }

        PlayerGlowChangeEvent event = new PlayerGlowChangeEvent(player, oldId, normalized);
        this.plugin.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        String effective = event.getNewEffect();
        if (effective != null && !effective.isBlank()) {
            effective = Utils.lowercase(effective);
            if (this.getEffect(effective) == null) return;
            user.setProperty(GlowProperties.GLOW, effective);
        } else {
            effective = null;
            user.removeProperty(GlowProperties.GLOW);
        }
        user.markDirty();

        if (effective == null) {
            this.removeGlowVisuals(player, false);
        } else {
            this.applyGlow(player);
        }
    }

    public void applyGlow(@NotNull Player player) {
        SunUser user = this.userManager.getOrFetch(player);
        String id = this.getStoredGlow(user);
        GlowEffect effect = this.getEffect(id);
        if (effect == null) {
            this.removeGlowVisuals(player, false);
            return;
        }

        this.rememberPreviousTeam(player);

        if (effect.isAnimated()) {
            this.animations.put(player.getUniqueId(), new GlowAnimation(effect.getId()));
            this.applyColor(player, effect.getFrame(0));
        } else {
            this.animations.remove(player.getUniqueId());
            this.applyColor(player, effect.getFrame(0));
        }
    }

    public void handleQuit(@NotNull Player player) {
        this.animations.remove(player.getUniqueId());
        this.removeFromGlowTeams(player);
        player.setGlowing(false);
    }

    private void tickAnimations() {
        if (this.animations.isEmpty()) return;

        Map.copyOf(this.animations).forEach((uuid, animation) -> {
            Player player = Utils.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                this.animations.remove(uuid);
                return;
            }

            GlowEffect effect = this.getEffect(animation.effectId());
            if (effect == null || !effect.isAnimated()) {
                this.animations.remove(uuid);
                return;
            }

            animation.ticksPassed += this.settings.getUpdateInterval();
            if (animation.ticksPassed < effect.getInterval()) return;

            animation.ticksPassed = 0L;
            animation.frame++;

            this.applyColor(player, effect.getFrame(animation.frame));
        });
    }

    private void applyColor(@NotNull Player player, @NotNull ChatColor color) {
        Scoreboard scoreboard = this.plugin.getServer().getScoreboardManager().getMainScoreboard();
        Team team = this.getOrCreateTeam(scoreboard, color);

        String entry = player.getName();
        Team current = scoreboard.getEntryTeam(entry);
        if (current != null && !current.getName().equals(team.getName()) && !this.isGlowTeam(current)) {
            this.rememberPreviousTeam(player);
        }

        this.removeFromGlowTeams(player);
        team.addEntry(entry);
        player.setGlowing(true);
    }

    private void removeFromGlowTeams(@NotNull Player player) {
        Scoreboard scoreboard = this.plugin.getServer().getScoreboardManager().getMainScoreboard();
        String entry = player.getName();
        Team current = scoreboard.getEntryTeam(entry);
        if (current != null && this.isGlowTeam(current)) {
            current.removeEntry(entry);
        }
    }

    private void rememberPreviousTeam(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        if (this.previousTeams.containsKey(uuid)) return;

        Scoreboard scoreboard = this.plugin.getServer().getScoreboardManager().getMainScoreboard();
        Team current = scoreboard.getEntryTeam(player.getName());
        if (current != null && !this.isGlowTeam(current)) {
            this.previousTeams.put(uuid, current.getName());
        }
    }

    private void restorePreviousTeam(@NotNull Player player) {
        String teamName = this.previousTeams.remove(player.getUniqueId());
        if (teamName == null) return;

        Scoreboard scoreboard = this.plugin.getServer().getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(teamName);
        if (team != null && !team.hasEntry(player.getName())) {
            try {
                team.addEntry(player.getName());
            } catch (IllegalStateException ignored) {
            }
        }
    }

    private boolean isGlowTeam(@NotNull Team team) {
        return team.getName().startsWith(TEAM_PREFIX);
    }

    private @NotNull Team getOrCreateTeam(@NotNull Scoreboard scoreboard, @NotNull ChatColor color) {
        String name = TEAM_PREFIX + Utils.lowercase(color.name());
        Team team = scoreboard.getTeam(name);
        if (team == null) {
            team = scoreboard.registerNewTeam(name);
        }
        if (team.getColor() != color) {
            try {
                team.setColor(color);
            } catch (IllegalStateException ignored) {
            }
        }
        return team;
    }

    private static final class GlowAnimation {
        private final String effectId;
        private int frame;
        private long ticksPassed;

        private GlowAnimation(String effectId) {
            this.effectId = effectId;
        }

        private String effectId() {
            return this.effectId;
        }
    }
}
