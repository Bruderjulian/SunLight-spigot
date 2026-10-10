package su.nightexpress.sunlight.moduleImpl.glow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.format.NamedTextColor;
import su.nightexpress.sunlight.moduleImpl.glow.event.PlayerGlowChangeEvent;
import su.nightexpress.sunlight.user.property.UserProperty;
import su.nightexpress.sunlight.utils.Utils;

public class GlowHandler {

  public static final long DEFAULT_PHASE_DURATION = GlowPhase.DEFAULT_DURATION;

  public static final int MAX_PHASES = 16;
  public static final int MAX_PRESETS = 20;
  public static final int MAX_PRESET_NAME_LENGTH = 24;

  public static final UserProperty<String> PROPERTY_GLOW = UserProperty.create("glow", String.class, "", true);
  public static final UserProperty<Boolean> PROPERTY_GLOW_ENABLED = UserProperty.create("glow_enabled",
      Boolean.class,
      true,
      true);
  /**
   * Per-player named presets, encoded as
   * {@code id:name:COLOR@DURATION,...;...}.
   * Legacy {@code name=COLOR@DURATION,...} entries are still read.
   */
  public static final UserProperty<String> PROPERTY_GLOW_PRESETS = UserProperty.create("glow_presets", String.class,
      "",
      true);

  /**
   * Current animation frame per player, so colour changes are pushed once per
   * step.
   */
  private final Map<UUID, GlowState> states = new ConcurrentHashMap<>();
  private final Map<UUID, GlowEffect> currentEffects = new ConcurrentHashMap<>();
  private final GlowModule module;

  private static final Pattern PRESET_NAME_PATTERN = Pattern.compile("[a-z0-9_\\-]+");

  private static final class GlowState {

    private int phaseIndex;
    private long ticksInPhase;
    private NamedTextColor lastColor;

    private GlowState(final GlowPhase initial) {
      this.phaseIndex = 0;
      this.ticksInPhase = 0L;
      this.lastColor = initial.color();
    }
  }

  public GlowHandler(final GlowModule module) {
    this.module = module;
  }

  public void init() {
    module.plugin().runTask(
        () -> Utils.onlinePlayers().forEach(player -> {
          this.refreshCache(player);
          this.applyGlow(player);
        }));
    module.addTask(this::tickAnimations, module.settings().getUpdateInterval());

  }

  public void shutdown() {
    for (final UUID uuid : this.states.keySet()) {
      final Player player = Utils.getPlayer(uuid);
      if (player != null && player.isOnline()) {
        player.setGlowing(false);
      }
    }
    this.states.clear();
    this.currentEffects.clear();
  }

  // --- Lookups ---

  public Map<String, GlowEffect> getEffects() {
    return module.settings().getEffects();
  }

  public GlowEffect getEffect(final String id) {
    if (id == null || id.isEmpty()) {
      return null;
    }
    return module.settings().getEffects().get(Utils.lowercase(id));
  }

  public boolean isGlowEnabled(final Player player) {
    return module.userManager().getOrFetch(player).getPropertyOrDefault(PROPERTY_GLOW_ENABLED);
  }

  /**
   * Colour of the phase that is currently shown, i.e. the phase the animation sits
   * on rather than the effect's first phase.
   *
   * @return null when the player has no active glow.
   */
  public NamedTextColor getCurrentColor(final Player player) {
    if (!this.isGlowEnabled(player)) {
      return null;
    }
    final GlowEffect effect = this.getEffectiveEffect(player);
    if (effect == null) {
      return null;
    }
    final GlowState state = this.states.get(player.getUniqueId());
    return state == null ? effect.getBaseColor() : effect.getPhase(state.phaseIndex).color();
  }

  /** Stored selection id, or null when nothing is selected. */
  public String getEffectiveEffectId(final Player player) {
    final GlowEffect effect = this.getEffectiveEffect(player);
    return effect == null ? null : effect.getId();
  }

  public GlowEffect getEffectiveEffect(final Player player) {
    final GlowEffect cached = currentEffects.get(player.getUniqueId());
    if (cached != null) {
      return cached;
    }
    final GlowEffect loaded = this.loadPersistedEffect(player);
    if (loaded != null) {
      currentEffects.put(player.getUniqueId(), loaded);
    }
    return loaded;
  }

  private GlowEffect loadPersistedEffect(final Player player) {
    final String str = module.userManager().getOrFetch(player).getPropertyOrDefault(PROPERTY_GLOW);
    final GlowEffect parsed = GlowEffect.parse(str);
    if (parsed == null) {
      return null;
    }
    final GlowEffect global = this.getEffect(parsed.getId());
    return global != null ? global : parsed;
  }

  private void refreshCache(final Player player) {
    final GlowEffect loaded = this.loadPersistedEffect(player);
    if (loaded == null) {
      currentEffects.remove(player.getUniqueId());
    } else {
      currentEffects.put(player.getUniqueId(), loaded);
    }
  }

  // --- Mutations ---

  public void setEffect(final Player player, final GlowEffect newEffect) {
    final GlowEffect oldEffect = this.getEffectiveEffect(player);
    if (oldEffect == newEffect) {
      return;
    }
    final PlayerGlowChangeEvent event = new PlayerGlowChangeEvent(player, oldEffect, newEffect);
    module.plugin().getPluginManager().callEvent(event);
    if (event.isCancelled()) {
      return;
    }

    final GlowEffect effective = event.getNewEffect();
    if (effective == null) {
      module.userManager().getOrFetch(player).removeProperty(PROPERTY_GLOW);
      this.clearGlow(player);
      return;
    }
    currentEffects.put(player.getUniqueId(), effective);
    module.userManager().getOrFetch(player).setProperty(PROPERTY_GLOW, effective.encode());
    this.applyGlow(player, effective);
  }

  /** Single-color glow: a one-phase static custom effect. */
  public void setSingleColorGlow(final Player player, final NamedTextColor color) {
    if (color == null) {
      return;
    }
    final GlowEffect effect = this.getEffect(color.toString());
    if (effect != null) {
      this.setEffect(player, effect);
    }
  }

  public boolean setGlowEnabled(final Player player, final boolean enabled) {
    module.userManager().getOrFetch(player).setProperty(PROPERTY_GLOW_ENABLED, enabled);
    if (enabled) {
      final GlowEffect effect = this.getEffectiveEffect(player);
      if (effect == null) {
        clearGlow(player);
        return false;
      }
      this.applyGlow(player, effect);
    } else {
      clearGlow(player);
    }
    return true;
  }

  public void applyGlow(final Player player) {
    if (!isGlowEnabled(player)) {
      return;
    }
    final GlowEffect effect = getEffectiveEffect(player);
    if (effect == null) {
      clearGlow(player);
    }

    this.states.put(player.getUniqueId(), new GlowState(effect.getPhase(0)));
    player.setGlowing(true);
    this.notifyNametags(player);
  }

  public void applyGlow(final Player player, final GlowEffect effect) {
    if (!isGlowEnabled(player)) {
      return;
    }
    if (effect == null) {
      clearGlow(player);
    }

    this.states.put(player.getUniqueId(), new GlowState(effect.getPhase(0)));
    player.setGlowing(true);
    this.notifyNametags(player);
  }

  public void clearGlow(final Player player) {
    if (currentEffects.remove(player.getUniqueId()) != null) {
      this.states.remove(player.getUniqueId());
      player.setGlowing(false);
      this.notifyNametags(player);
    }
  }

  private void tickAnimations() {
    if (this.states.isEmpty()) {
      return;
    }

    final long step = module.settings().getUpdateInterval();
    // Iterate only animated players instead of all online players.
    for (var entry : this.states.entrySet()) {
      final UUID playerId = entry.getKey();
      final GlowState state = entry.getValue();
      if (state == null) continue;
      final Player player = org.bukkit.Bukkit.getPlayer(playerId);
      if (player == null || !player.isOnline()) {
        this.states.remove(playerId);
        this.currentEffects.remove(playerId);
        continue;
      }

      final GlowEffect effect = this.getEffectiveEffect(player);
      if (effect == null || !this.isGlowEnabled(player)) {
        this.states.remove(player.getUniqueId());
        player.setGlowing(false);
        this.notifyNametags(player);
        continue;
      }
      if (!effect.isAnimated()) {
        continue; // Static: already applied.
      }

      state.ticksInPhase += step;
      final GlowPhase current = effect.getPhase(state.phaseIndex);
      if (state.ticksInPhase < current.durationTicks()) {
        continue;
      }

      state.ticksInPhase = 0L;
      state.phaseIndex++;

      final NamedTextColor color = effect.getPhase(state.phaseIndex).color();
      if (color.equals(state.lastColor)) {
        continue;
      }

      state.lastColor = color;
      this.notifyNametags(player);
    }
  }

  /**
   * Asks the nametags module to rebuild, because the glow colour is part of the
   * nameplate.
   * Silent when the nametags module is not loaded, so glow still works on its
   * own.
   */
  private void notifyNametags(final Player player) {
    module.plugin().nametagsProvider().ifPresent(provider -> provider.recompute(player));
  }

  public void handleQuit(final Player player) {
    this.states.remove(player.getUniqueId());
    this.currentEffects.remove(player.getUniqueId());
  }

  // --- Presets (per-player) ---

  public static String normalizePresetName(final String raw) {
    if (raw == null) {
      return null;
    }
    final String name = Utils.lowercase(raw.trim());
    if (name.isEmpty() || name.length() > MAX_PRESET_NAME_LENGTH) {
      return null;
    }
    if (!PRESET_NAME_PATTERN.matcher(name).matches()) {
      return null;
    }
    return name;
  }

  public Map<String, GlowEffect> getPresets(final Player player) {
    final String raw = module.userManager().getOrFetch(player).getPropertyOrDefault(PROPERTY_GLOW_PRESETS);
    if (raw == null || raw.isBlank()) {
      return Map.of();
    }
    final Map<String, GlowEffect> map = new LinkedHashMap<>();
    for (final String entry : raw.split(";")) {
      if (entry == null || entry.isBlank()) {
        continue;
      }
      // New format: full encode "id:name:phases".
      final GlowEffect effect = entry.contains(":") ? GlowEffect.parse(entry.trim()) : null;
      if (effect != null) {
        map.put(Utils.lowercase(effect.getId()), effect);
        continue;
      }
      // Legacy format: "name=phase,phase".
      final int eq = entry.indexOf('=');
      if (eq <= 0) {
        continue;
      }
      final String name = normalizePresetName(entry.substring(0, eq));
      if (name == null) {
        continue;
      }
      final List<GlowPhase> phases = new ArrayList<>();
      boolean bad = false;
      for (final String token : entry.substring(eq + 1).split(",")) {
        final GlowPhase phase = GlowPhase.parse(token);
        if (phase == null) {
          bad = true;
          break;
        }
        phases.add(phase);
        if (phases.size() > MAX_PHASES) {
          bad = true;
          break;
        }
      }
      if (bad || phases.isEmpty()) {
        continue;
      }
      map.put(name, new GlowEffect(name, name, phases));
    }
    return map;
  }

  private void writePresets(final Player player, final Map<String, GlowEffect> presets) {
    if (presets.isEmpty()) {
      module.userManager().getOrFetch(player).removeProperty(PROPERTY_GLOW_PRESETS);
      return;
    }
    final StringBuilder builder = new StringBuilder();
    for (final GlowEffect effect : presets.values()) {
      if (!builder.isEmpty()) {
        builder.append(';');
      }
      builder.append(effect.encode());
    }
    module.userManager().getOrFetch(player).setProperty(PROPERTY_GLOW_PRESETS, builder.toString());
  }

  /**
   * Saves the current working phases under the given preset name. Returns false
   * on invalid name/limit.
   */
  public boolean savePreset(final Player player, final String rawName) {
    return this.savePreset(player, rawName, getEffectiveEffect(player));
  }

  public boolean savePreset(final Player player, final String rawName, final GlowEffect effect) {
    final String name = normalizePresetName(rawName);
    if (name == null || effect == null) {
      return false;
    }
    final Map<String, GlowEffect> presets = new LinkedHashMap<>(this.getPresets(player));
    if (!presets.containsKey(name) && presets.size() >= MAX_PRESETS) {
      return false;
    }
    presets.put(name, new GlowEffect(name, effect.getName(), List.of(effect.getPhases())));
    this.writePresets(player, presets);
    return true;
  }

  public boolean savePreset(final Player player, final String rawName, final List<GlowPhase> phases) {
    final String name = normalizePresetName(rawName);
    if (name == null || phases == null || phases.isEmpty() || phases.size() > MAX_PHASES) {
      return false;
    }
    return this.savePreset(player, name, new GlowEffect(name, name, new ArrayList<>(phases)));
  }

  public GlowEffect loadPreset(final Player player, final String rawName) {
    final String name = normalizePresetName(rawName);
    if (name == null) {
      return null;
    }
    return this.getPresets(player).get(name);
  }

  public boolean applyPreset(final Player player, final String rawName) {
    final GlowEffect effect = this.loadPreset(player, rawName);
    if (effect == null) {
      return false;
    }
    setEffect(player, effect);
    return true;
  }

  public boolean deletePreset(final Player player, final String rawName) {
    final String name = normalizePresetName(rawName);
    if (name == null) {
      return false;
    }
    final Map<String, GlowEffect> presets = new LinkedHashMap<>(this.getPresets(player));
    if (presets.remove(name) == null) {
      return false;
    }
    this.writePresets(player, presets);
    return true;
  }
}
