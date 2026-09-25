package su.nightexpress.sunlight.moduleImpl.glow;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.provider.GlowProvider;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.exception.ModuleLoadException;
import su.nightexpress.sunlight.hook.HookId;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.glow.command.GlowCommandProvider;
import su.nightexpress.sunlight.moduleImpl.glow.config.GlowLang;
import su.nightexpress.sunlight.moduleImpl.glow.config.GlowPerms;
import su.nightexpress.sunlight.moduleImpl.glow.event.PlayerGlowChangeEvent;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;
import su.nightexpress.sunlight.utils.Utils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies the vanilla glow outline and contributes its colour to the nametag.
 * <p>
 * The glowing flag itself is plain Bukkit API ({@code Player#setGlowing}), so no packet
 * library is needed any more. The colour is handed to the nametags module, which appends it
 * as the last colour token of the nameplate, so the two features never fight over teams.
 */
public class GlowModule extends Module implements GlowProvider {

    private final GlowSettings settings;

    /** Current animation frame per player, so colour changes are pushed once per step. */
    private final Map<UUID, GlowState> states = new ConcurrentHashMap<>();

    public GlowModule(ModuleDefinition<GlowModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new GlowSettings();
    }

    @Override
    protected void loadModule(FileConfig config) throws ModuleLoadException {
        this.settings.load(config);
        this.plugin.injectLang(GlowLang.class);
        UserPropertyRegistry.register(GlowProperties.GLOW);

        this.addListener(new GlowListener(this.plugin, this));
        this.addTask(this::tickAnimations, this.settings.getUpdateInterval());

        this.plugin.runTask(() -> Utils.onlinePlayers().forEach(this::applyGlow));
    }

    @Override
    protected void unloadModule() {
        this.states.forEach((uuid, state) -> {
            Player player = Utils.getPlayer(uuid);
            if (player != null && player.isOnline()) player.setGlowing(false);
        });
        this.states.clear();
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
        return this.getStoredGlow(user);
    }

    public @Nullable String getStoredGlow(@NotNull SunUser user) {
        String id = user.getPropertyOrDefault(GlowProperties.GLOW);
        if (id == null || id.isBlank()) return null;
        if (this.getEffect(id) == null) return null;
        return Utils.lowercase(id);
    }

    /**
     * The colour name the current glow frame renders as, e.g. {@code gold}.
     * The nametags module reads this to append the final colour token.
     */
    public @Nullable String getGlowColor(@NotNull Player player) {
        GlowState state = this.states.get(player.getUniqueId());
        if (state == null) return null;
        return state.lastColor.examinableName();
    }

    @Override
    public void setGlow(@NotNull Player player, @Nullable String effectId) {
        this.setGlow(this.userManager.getOrFetch(player), player, effectId);
    }

    @Override
    public void clearGlow(@NotNull Player player) {
        this.setGlow(player, null);
    }

    public void setGlow(@NotNull SunUser user, @Nullable String effectId) {
        Player target = user.player().orElse(null);
        if (target != null) {
            this.setGlow(user, target, effectId);
            return;
        }

        String normalized = effectId == null ? null : Utils.lowercase(effectId);
        if (normalized == null) {
            user.removeProperty(GlowProperties.GLOW);
        } else {
            user.setProperty(GlowProperties.GLOW, normalized);
        }
        user.markDirty();
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

        this.applyGlow(player);
    }

    public void applyGlow(@NotNull Player player) {
        GlowEffect effect = this.getEffect(this.getStoredGlow(this.userManager.getOrFetch(player)));
        if (effect == null) {
            this.states.remove(player.getUniqueId());
            player.setGlowing(false);
            this.notifyNametags(player);
            return;
        }

        this.states.put(player.getUniqueId(), new GlowState(effect.getId(), effect.getFrame(0)));
        player.setGlowing(true);
        this.notifyNametags(player);
    }

    public void handleQuit(@NotNull Player player) {
        this.states.remove(player.getUniqueId());
    }

    /**
     * Asks the nametags module to rebuild, because the glow colour is part of the nameplate.
     * Silent when the nametags module is not loaded, so glow still works on its own.
     */
    private void notifyNametags(@NotNull Player player) {
        this.plugin.nametagsProvider().ifPresent(provider -> provider.recompute(player));
    }

    private void tickAnimations() {
        if (this.states.isEmpty()) return;

        long step = this.settings.getUpdateInterval();
        for (Player player : Bukkit.getOnlinePlayers()) {
            GlowState state = this.states.get(player.getUniqueId());
            if (state == null) continue;

            GlowEffect effect = this.getEffect(state.effectId);
            if (effect == null) {
                this.states.remove(player.getUniqueId());
                player.setGlowing(false);
                this.notifyNametags(player);
                continue;
            }
            if (!effect.isAnimated()) continue; // Static: already applied.

            state.ticksPassed += step;
            if (state.ticksPassed < effect.getInterval()) continue;

            state.ticksPassed = 0L;
            state.frame++;

            NamedTextColor color = effect.getFrame(state.frame);
            if (color.equals(state.lastColor)) continue; // Skip redundant recomputes.

            state.lastColor = color;
            this.notifyNametags(player);
        }
    }

    private static final class GlowState {

        private final String effectId;
        private int frame;
        private long ticksPassed;
        private NamedTextColor lastColor;

        private GlowState(String effectId, NamedTextColor lastColor) {
            this.effectId = effectId;
            this.lastColor = lastColor;
        }
    }
}
