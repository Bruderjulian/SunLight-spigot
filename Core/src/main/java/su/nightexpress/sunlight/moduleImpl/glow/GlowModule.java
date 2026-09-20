package su.nightexpress.sunlight.moduleImpl.glow;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.api.provider.GlowProvider;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.exception.ModuleLoadException;
import su.nightexpress.sunlight.hook.HookId;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.glow.command.GlowCommandProvider;
import su.nightexpress.sunlight.moduleImpl.glow.config.GlowLang;
import su.nightexpress.sunlight.moduleImpl.glow.config.GlowPerms;
import su.nightexpress.sunlight.moduleImpl.glow.event.PlayerGlowChangeEvent;
import su.nightexpress.sunlight.moduleImpl.glow.handler.GlowPacketHandler;
import su.nightexpress.sunlight.moduleImpl.glow.handler.GlowPacketsHandler;
import su.nightexpress.sunlight.moduleImpl.glow.handler.GlowProtocolHandler;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;
import su.nightexpress.sunlight.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GlowModule extends Module implements GlowProvider {

    private final GlowSettings settings;
    private final Map<UUID, GlowState> states;

    private GlowPacketHandler packetHandler;

    public GlowModule(ModuleDefinition<GlowModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new GlowSettings();
        this.states = new ConcurrentHashMap<>();
    }

    @Override
    protected void loadModule(FileConfig config) throws ModuleLoadException {
        this.settings.load(config);
        this.plugin.injectLang(GlowLang.class);
        UserPropertyRegistry.register(GlowProperties.GLOW);

        if (Utils.isInstalled(HookId.PACKET_EVENTS)) {
            this.packetHandler = new GlowPacketsHandler(this.plugin);
        } else if (Utils.isInstalled(HookId.PROTOCOL_LIB)) {
            this.packetHandler = new GlowProtocolHandler(this.plugin);
        } else {
            throw new ModuleLoadException("No packet library installed. Install packetevents or ProtocolLib.");
        }

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
        if (this.packetHandler != null) {
            Collection<Player> viewers = snapshotViewers();
            this.states.keySet().forEach(uuid -> {
                Player player = Utils.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    this.packetHandler.removeGlow(player, viewers);
                    player.setGlowing(false);
                }
            });
        }
        this.states.clear();
    }

    private void removeGlowVisuals(@NotNull Player player) {
        this.states.remove(player.getUniqueId());
        if (this.packetHandler != null) {
            this.packetHandler.removeGlow(player, snapshotViewersOf(player));
        }
        player.setGlowing(false);
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
            this.removeGlowVisuals(player);
        } else {
            this.applyGlow(player);
        }
    }

    public void applyGlow(@NotNull Player player) {
        SunUser user = this.userManager.getOrFetch(player);
        String id = this.getStoredGlow(user);
        GlowEffect effect = this.getEffect(id);
        if (effect == null) {
            this.removeGlowVisuals(player);
            return;
        }

        NamedTextColor color = effect.getFrame(0);
        this.states.put(player.getUniqueId(), new GlowState(effect.getId(), color));
        player.setGlowing(true);
        this.packetHandler.sendGlow(player, color, snapshotViewersOf(player));
    }

    public void handleQuit(@NotNull Player player) {
        this.states.remove(player.getUniqueId());
        if (this.packetHandler != null) {
            this.packetHandler.removeGlow(player, snapshotViewers());
        }
    }

    /**
     * (Re)sends all active glow teams to a viewer. Needed on join / world change,
     * as team packets are client-side and a fresh client knows none of them.
     */
    public void sendAllGlowsTo(@NotNull Player viewer) {
        if (this.packetHandler == null || this.states.isEmpty()) return;

        World world = viewer.getWorld();
        List<Player> single = List.of(viewer);
        this.states.forEach((uuid, state) -> {
            if (uuid.equals(viewer.getUniqueId())) return;
            Player target = Utils.getPlayer(uuid);
            if (target == null || !target.isOnline()) return;
            if (!target.getWorld().equals(world)) return;
            this.packetHandler.sendGlow(target, state.lastColor, single);
        });
    }

    private void tickAnimations() {
        if (this.states.isEmpty() || this.packetHandler == null) return;

        long step = this.settings.getUpdateInterval();
        List<Player> viewers = snapshotViewers();
        if (viewers.isEmpty()) return;

        this.states.entrySet().removeIf(entry -> {
            UUID uuid = entry.getKey();
            GlowState state = entry.getValue();

            Player player = Utils.getPlayer(uuid);
            if (player == null || !player.isOnline()) return true;

            GlowEffect effect = this.getEffect(state.effectId);
            if (effect == null) {
                this.packetHandler.removeGlow(player, viewers);
                player.setGlowing(false);
                return true;
            }
            if (!effect.isAnimated()) return false; // Static: sent once on apply, nothing to tick.

            state.ticksPassed += step;
            if (state.ticksPassed < effect.getInterval()) return false;

            state.ticksPassed = 0L;
            state.frame++;

            NamedTextColor color = effect.getFrame(state.frame);
            if (color.equals(state.lastColor)) return false; // Dirty check: skip redundant packets.
            state.lastColor = color;

            this.packetHandler.sendGlow(player, color, viewersIn(viewers, player));
            return false;
        });
    }

    private static @NotNull List<Player> snapshotViewers() {
        return List.copyOf(Bukkit.getServer().getOnlinePlayers());
    }

    private static @NotNull List<Player> snapshotViewersOf(@NotNull Player target) {
        World world = target.getWorld();
        List<Player> viewers = new ArrayList<>();
        for (Player viewer : Bukkit.getServer().getOnlinePlayers()) {
            if (viewer.getWorld().equals(world)) {
                viewers.add(viewer);
            }
        }
        return viewers;
    }

    private static @NotNull List<Player> viewersIn(@NotNull List<Player> viewers, @NotNull Player target) {
        World world = target.getWorld();
        List<Player> result = null;
        for (Player viewer : viewers) {
            if (!viewer.getWorld().equals(world)) continue;
            if (result == null) result = new ArrayList<>();
            result.add(viewer);
        }
        return result == null ? List.of() : result;
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
