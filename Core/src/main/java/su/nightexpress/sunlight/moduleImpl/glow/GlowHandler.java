package su.nightexpress.sunlight.moduleImpl.glow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.format.NamedTextColor;
import su.nightexpress.sunlight.moduleImpl.glow.event.PlayerGlowChangeEvent;
import su.nightexpress.sunlight.user.SunUser;
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
   * Per-player custom effect, encoded as {@code PHASES:COLOR@DURATION,COLOR@DURATION}.
   * Empty means no custom effect; the
   * {@link su.nightexpress.sunlight.moduleImpl.glow.GlowModule#CUSTOM_ID}
   * selection points at this payload.
   */
  public static final UserProperty<String> PROPERTY_GLOW_CUSTOM = UserProperty.create("glow_custom", String.class,
      "",
      true);
  /**
   * Per-player named presets, encoded as
   * {@code name=COLOR@DURATION,COLOR@DURATION;name2=...}.
   */
  public static final UserProperty<String> PROPERTY_GLOW_PRESETS = UserProperty.create("glow_presets", String.class,
      "",
      true);
  /**
   * Reserved selection id pointing at the per-player custom payload in
   * {@link GlowProperties#GLOW_CUSTOM}.
   */
  public static final String CUSTOM_ID = "custom";

  /**
   * Current animation frame per player, so colour changes are pushed once per
   * step.
   */
  private final Map<UUID, GlowState> states = new ConcurrentHashMap<>();
  private final GlowModule module;

  public GlowHandler(final GlowModule module) {
    this.module = module;
  }

  public void init() {
    module.plugin().runTask(
        () -> Utils.onlinePlayers().forEach(player -> applyGlow(module.userManager().getOrFetch(player), player)));
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
  }

  public boolean hasGlow(final SunUser user) {
    return this.isGlowEnabled(user) && this.getStoredGlow(user) != null;
  }

  public String getStoredGlow(final SunUser user) {
    String id = user.getPropertyOrDefault(PROPERTY_GLOW);
    if (id == null || id.isBlank()) {
      return null;
    }
    id = Utils.lowercase(id);
    if (CUSTOM_ID.equals(id)) {
      return this.getCustomEffect(user) == null ? null : CUSTOM_ID;
    }
    if (module.settings().getEffects().get(id) == null) {
      return null;
    }
    return id;
  }

  public boolean isGlowEnabled(final SunUser user) {
    return user.getPropertyOrDefault(PROPERTY_GLOW_ENABLED);
  }

  public boolean isCustomGlow(final SunUser user) {
    return CUSTOM_ID.equals(this.getStoredGlow(user));
  }

  /**
   * Decodes the per-player custom payload ({@code PHASES:COLOR@DURATION,...}).
   * Legacy {@code TYPE:COLORS:INTERVAL} payloads no longer decode and return null.
   */
  public GlowEffect getCustomEffect(final SunUser user) {
    final String raw = user.getPropertyOrDefault(PROPERTY_GLOW_CUSTOM);
    return decodePhasesEffect(CUSTOM_ID, "Custom", raw);
  }

  public static GlowEffect decodePhasesEffect(final String id, final String name, final String raw) {
    if (raw == null || raw.isBlank()) return null;
    try {
      final String[] parts = raw.split(":", 2);
      if (parts.length != 2) return null;
      final String tag = parts[0].trim().toUpperCase();
      if (!tag.equals("PHASES") && !tag.equals(GlowType.STATIC.name()) && !tag.equals(GlowType.PHASED.name())) {
        return null; // Legacy payload (e.g. FLASH:...): clean break, treated as absent.
      }
      final List<GlowPhase> phases = new ArrayList<>();
      for (final String token : parts[1].split(",")) {
        if (token.isBlank()) continue;
        final GlowPhase phase = GlowPhase.parse(token.trim());
        if (phase == null) return null;
        phases.add(phase);
        if (phases.size() > MAX_PHASES) return null;
      }
      if (phases.isEmpty()) return null;
      final GlowType type = phases.size() > 1 ? GlowType.PHASED : GlowType.STATIC;
      return new GlowEffect(id, name, type, phases);
    } catch (final Exception exception) {
      return null;
    }
  }

  public static String encodePhases(final List<GlowPhase> phases) {
    final StringBuilder builder = new StringBuilder("PHASES:");
    for (int index = 0; index < phases.size(); index++) {
      if (index > 0) builder.append(',');
      builder.append(phases.get(index).encode());
    }
    return builder.toString();
  }

  public GlowEffect getEffect(final String id) {
    if (id == null || id.isBlank())
      return null;
    return module.settings().getEffects().get(Utils.lowercase(id));
  }

  public Map<String, GlowEffect> getEffects() {
    return module.settings().getEffects();
  }

  public GlowEffect getEffectiveEffect(final SunUser user) {
    final String stored = this.getStoredGlow(user);
    if (stored == null)
      return null;
    if (CUSTOM_ID.equals(stored))
      return this.getCustomEffect(user);
    return getEffect(stored);
  }

  /** Current working phase list: custom payload, or the effective effect's phases. */
  public List<GlowPhase> getWorkingPhases(final SunUser user) {
    final GlowEffect effective = this.getEffectiveEffect(user);
    if (effective == null) return List.of();
    return List.copyOf(effective.getPhases());
  }

  private void writeCustomEffect(final SunUser user, final List<GlowPhase> phases) {
    user.setProperty(PROPERTY_GLOW_CUSTOM, encodePhases(phases));
  }

  private void clearCustomEffect(final SunUser user) {
    user.removeProperty(PROPERTY_GLOW_CUSTOM);
  }

  /**
   * The colour name the current glow frame renders as, e.g. {@code gold}.
   * The nametags module reads this to append the final colour token.
   */
  public String getGlowColor(final Player player) {
    final GlowState state = this.states.get(player.getUniqueId());
    if (state == null)
      return null;
    return state.lastColor == null ? null : state.lastColor.toString();
  }

  public void setGlow(final SunUser user, final String effectId) {
    final Player target = user.player().orElse(null);
    if (target != null) {
      this.setGlow(user, target, effectId);
      return;
    }

    final String normalized = effectId == null ? null : Utils.lowercase(effectId);
    if (normalized == null) {
      user.removeProperty(PROPERTY_GLOW);
      this.clearCustomEffect(user);
    } else if (CUSTOM_ID.equals(normalized)) {
      if (this.getCustomEffect(user) == null)
        return;
      user.setProperty(PROPERTY_GLOW, CUSTOM_ID);
    } else {
      if (getEffect(normalized) == null)
        return;
      user.setProperty(PROPERTY_GLOW, normalized);
      this.clearCustomEffect(user);
    }
    user.setProperty(PROPERTY_GLOW_ENABLED, true);
  }

  public void setGlow(final SunUser user, final Player player, final String effectId) {
    final String oldId = this.getStoredGlow(user);
    final String normalized = (effectId == null || effectId.isBlank()) ? null : Utils.lowercase(effectId);

    if (normalized != null && !CUSTOM_ID.equals(normalized) && getEffect(normalized) == null)
      return;
    if (CUSTOM_ID.equals(normalized) && this.getCustomEffect(user) == null)
      return;

    final boolean same = (oldId == null && normalized == null) || (oldId != null && oldId.equals(normalized));
    if (same && normalized != null) {
      user.setProperty(PROPERTY_GLOW_ENABLED, true);
      this.applyGlow(user, player);
      return;
    }

    final PlayerGlowChangeEvent event = new PlayerGlowChangeEvent(player, oldId, normalized);
    module.plugin().getPluginManager().callEvent(event);
    if (event.isCancelled())
      return;

    String effective = event.getNewEffect();
    if (effective != null && !effective.isBlank()) {
      effective = Utils.lowercase(effective);
      if (CUSTOM_ID.equals(effective)) {
        if (this.getCustomEffect(user) == null)
          return;
        user.setProperty(PROPERTY_GLOW, CUSTOM_ID);
      } else {
        if (getEffect(effective) == null)
          return;
        user.setProperty(PROPERTY_GLOW, effective);
        this.clearCustomEffect(user);
      }
      user.setProperty(PROPERTY_GLOW_ENABLED, true);
    } else {
      user.removeProperty(PROPERTY_GLOW);
      this.clearCustomEffect(user);
    }

    this.applyGlow(user, player);
  }

  /**
   * Applies a fully custom phased effect and selects it.
   */
  public void setCustomGlow(final SunUser user, final Player player, final List<GlowPhase> phases) {
    if (phases == null || phases.isEmpty() || phases.size() > MAX_PHASES)
      return;
    this.writeCustomEffect(user, phases);
    this.setGlow(user, player, CUSTOM_ID);
  }

  public void setCustomGlow(final SunUser user, final List<GlowPhase> phases) {
    final Player target = user.player().orElse(null);
    if (target != null) {
      this.setCustomGlow(user, target, phases);
      return;
    }
    if (phases == null || phases.isEmpty() || phases.size() > MAX_PHASES)
      return;
    this.writeCustomEffect(user, phases);
    this.setGlow(user, CUSTOM_ID);
  }

  /** Single-color glow: degenerates to a one-phase static custom effect. */
  public void setSingleColorGlow(final SunUser user, final Player player, final NamedTextColor color) {
    this.setCustomGlow(user, player, List.of(new GlowPhase(color, DEFAULT_PHASE_DURATION)));
  }

  public boolean addPhase(final SunUser user, final Player player, final GlowPhase phase) {
    final List<GlowPhase> working = new ArrayList<>(this.getWorkingPhases(user));
    if (working.size() >= MAX_PHASES) return false;
    working.add(phase);
    this.setCustomGlow(user, player, working);
    return true;
  }

  public boolean removePhase(final SunUser user, final Player player, final int index) {
    final List<GlowPhase> working = new ArrayList<>(this.getWorkingPhases(user));
    if (index < 0 || index >= working.size()) return false;
    working.remove(index);
    if (working.isEmpty()) {
      this.setGlow(user, player, null);
      return true;
    }
    this.setCustomGlow(user, player, working);
    return true;
  }

  public boolean setPhaseDuration(final SunUser user, final Player player, final int index, final long duration) {
    final List<GlowPhase> working = new ArrayList<>(this.getWorkingPhases(user));
    if (index < 0 || index >= working.size()) return false;
    working.set(index, new GlowPhase(working.get(index).color(), duration));
    this.setCustomGlow(user, player, working);
    return true;
  }

  // --- Presets (per-player) ---

  public static String normalizePresetName(final String raw) {
    if (raw == null) return null;
    final String name = Utils.lowercase(raw.trim());
    if (name.isBlank() || name.length() > MAX_PRESET_NAME_LENGTH) return null;
    if (!name.matches("[a-z0-9_\\-]+")) return null;
    return name;
  }

  public Map<String, List<GlowPhase>> getPresets(final SunUser user) {
    final Map<String, List<GlowPhase>> map = new LinkedHashMap<>();
    final String raw = user.getPropertyOrDefault(PROPERTY_GLOW_PRESETS);
    if (raw == null || raw.isBlank()) return map;
    for (final String entry : raw.split(";")) {
      if (entry.isBlank()) continue;
      final int eq = entry.indexOf('=');
      if (eq <= 0) continue;
      final String name = normalizePresetName(entry.substring(0, eq));
      if (name == null) continue;
      final List<GlowPhase> phases = new ArrayList<>();
      boolean bad = false;
      for (final String token : entry.substring(eq + 1).split(",")) {
        if (token.isBlank()) continue;
        final GlowPhase phase = GlowPhase.parse(token.trim());
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
      if (bad || phases.isEmpty()) continue;
      map.put(name, List.copyOf(phases));
    }
    return map;
  }

  private void writePresets(final SunUser user, final Map<String, List<GlowPhase>> presets) {
    if (presets.isEmpty()) {
      user.removeProperty(PROPERTY_GLOW_PRESETS);
      return;
    }
    final StringBuilder builder = new StringBuilder();
    presets.forEach((name, phases) -> {
      if (!builder.isEmpty()) builder.append(';');
      builder.append(name).append('=');
      for (int i = 0; i < phases.size(); i++) {
        if (i > 0) builder.append(',');
        builder.append(phases.get(i).encode());
      }
    });
    user.setProperty(PROPERTY_GLOW_PRESETS, builder.toString());
  }

  /** Saves the current working phases under the given preset name. Returns false on invalid name/limit. */
  public boolean savePreset(final SunUser user, final String rawName) {
    return this.savePreset(user, rawName, this.getWorkingPhases(user));
  }

  public boolean savePreset(final SunUser user, final String rawName, final List<GlowPhase> phases) {
    final String name = normalizePresetName(rawName);
    if (name == null || phases == null || phases.isEmpty() || phases.size() > MAX_PHASES) return false;
    final Map<String, List<GlowPhase>> presets = new LinkedHashMap<>(this.getPresets(user));
    if (!presets.containsKey(name) && presets.size() >= MAX_PRESETS) return false;
    presets.put(name, List.copyOf(phases));
    this.writePresets(user, presets);
    return true;
  }

  public List<GlowPhase> loadPreset(final SunUser user, final String rawName) {
    final String name = normalizePresetName(rawName);
    if (name == null) return null;
    return this.getPresets(user).get(name);
  }

  public boolean applyPreset(final SunUser user, final Player player, final String rawName) {
    final List<GlowPhase> phases = this.loadPreset(user, rawName);
    if (phases == null) return false;
    this.setCustomGlow(user, player, phases);
    return true;
  }

  public boolean deletePreset(final SunUser user, final String rawName) {
    final String name = normalizePresetName(rawName);
    if (name == null) return false;
    final Map<String, List<GlowPhase>> presets = new LinkedHashMap<>(this.getPresets(user));
    if (presets.remove(name) == null) return false;
    this.writePresets(user, presets);
    return true;
  }

  public boolean setGlowEnabled(final SunUser user, final Player player, final boolean enabled) {
    user.setProperty(PROPERTY_GLOW_ENABLED, enabled);
    if (enabled && this.getStoredGlow(user) == null) {
      return false;
    }
    this.applyGlow(user, player);
    return true;
  }

  public NamedTextColor getBaseColor(final SunUser user) {
    final GlowEffect effect = this.getEffectiveEffect(user);
    if (effect == null || effect.getPhases().isEmpty())
      return NamedTextColor.WHITE;
    return effect.getPhases().getFirst().color();
  }

  public void applyGlow(final SunUser user, final Player player) {
    final GlowEffect effect = this.isGlowEnabled(user) ? this.getEffectiveEffect(user) : null;
    if (effect == null) {
      this.states.remove(player.getUniqueId());
      player.setGlowing(false);
      this.notifyNametags(player);
      return;
    }

    final String stateId = CUSTOM_ID.equals(this.getStoredGlow(user)) ? CUSTOM_ID : effect.getId();
    this.states.put(player.getUniqueId(), new GlowState(stateId, effect.getPhase(0)));
    player.setGlowing(true);
    this.notifyNametags(player);
  }

  public void handleQuit(final Player player) {
    this.states.remove(player.getUniqueId());
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

  private void tickAnimations() {
    if (this.states.isEmpty())
      return;

    final long step = module.settings().getUpdateInterval();
    for (final Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
      final GlowState state = this.states.get(player.getUniqueId());
      if (state == null) {
        continue;
      }
      final SunUser user = module.userManager().getOrFetch(player);

      final GlowEffect effect = this.getEffectiveEffect(user);
      if (effect == null || !this.isGlowEnabled(user)) {
        this.states.remove(player.getUniqueId());
        player.setGlowing(false);
        this.notifyNametags(player);
        continue;
      }
      if (!effect.isAnimated())
        continue; // Static: already applied.

      state.ticksInPhase += step;
      final GlowPhase current = effect.getPhase(state.phaseIndex);
      if (state.ticksInPhase < current.durationTicks())
        continue;

      state.ticksInPhase = 0L;
      state.phaseIndex++;

      final NamedTextColor color = effect.getPhase(state.phaseIndex).color();
      if (color.equals(state.lastColor))
        continue; // Skip redundant recomputes.

      state.lastColor = color;
      this.notifyNametags(player);
    }
  }

  private static final class GlowState {

    private final String effectId;
    private int phaseIndex;
    private long ticksInPhase;
    private NamedTextColor lastColor;

    private GlowState(final String effectId, final GlowPhase initial) {
      this.effectId = effectId;
      this.phaseIndex = 0;
      this.ticksInPhase = 0L;
      this.lastColor = initial.color();
    }
  }
}
