package su.nightexpress.sunlight.moduleImpl.profiles;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.AbstractConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigTypes;

public class ProfilesSettings extends AbstractConfig {

    private final ConfigProperty<Integer> maxPerPlayer = this.addProperty(ConfigTypes.INT,
        "Profiles.Max-Per-Player",
        3,
        "How many profiles a player may own. Extra slots via 'sunlight.profiles.slots.<n>' permission.");

    private final ConfigProperty<Boolean> allowRename = this.addProperty(ConfigTypes.BOOLEAN,
        "Profiles.Allow-Rename",
        true,
        "Whether players may rename their profiles.");

    private final ConfigProperty<Boolean> allowDelete = this.addProperty(ConfigTypes.BOOLEAN,
        "Profiles.Allow-Delete",
        true,
        "Whether players may delete their profiles (the active and the last one are protected).");

    private final ConfigProperty<Integer> switchCooldown = this.addProperty(ConfigTypes.INT,
        "Switch.Cooldown-Seconds",
        30,
        "Cooldown between profile switches. 0 or negative disables it.",
        "Bypass with 'sunlight.profiles.bypass.cooldown'.");

    private final ConfigProperty<Double> switchCost = this.addProperty(ConfigTypes.DOUBLE,
        "Switch.Cost",
        0D,
        "Money charged for every profile switch. 0 disables it.",
        "Bypass with 'sunlight.bypass.cost' or 'sunlight.profiles.bypass.cost'.");

    private final ConfigProperty<Integer> switchWarmup = this.addProperty(ConfigTypes.INT,
        "Switch.Warmup-Seconds",
        0,
        "Stand-still warmup before a switch applies. 0 disables it.",
        "Moving, taking damage or leaving cancels it. Bypass with 'sunlight.profiles.bypass.warmup'.");

    private final ConfigProperty<Boolean> switchConfirmGui = this.addProperty(ConfigTypes.BOOLEAN,
        "Switch.Confirm-In-GUI",
        true,
        "Sets whether the GUI asks for confirmation (from -> to, cost, warmup) before switching.");

    private final ConfigProperty<Boolean> blockWhileVanished = this.addProperty(ConfigTypes.BOOLEAN,
        "Switch.Block-While-Vanished",
        true,
        "Block switching while vanished (prevents stealth state leaking across profiles).");

    private final ConfigProperty<Boolean> blockWhileFrozen = this.addProperty(ConfigTypes.BOOLEAN,
        "Switch.Block-While-Frozen",
        true,
        "Block switching while frozen.");

    private final ConfigProperty<Boolean> blockWhileFlying = this.addProperty(ConfigTypes.BOOLEAN,
        "Switch.Block-While-Flying-Midair",
        true,
        "Block switching while flying and airborne (prevents fall/glitch exploits).");

    private final ConfigProperty<Boolean> blockWhileDead = this.addProperty(ConfigTypes.BOOLEAN,
        "Switch.Block-While-Dead",
        true,
        "Block switching while dead.");

    private final ConfigProperty<Boolean> resetVanishOnSwitch = this.addProperty(ConfigTypes.BOOLEAN,
        "Switch.Reset-On-Switch.Vanish",
        true,
        "Force vanish OFF when a switch happens (recommended: staff duty state must not cross profiles).");

    private final ConfigProperty<Boolean> resetFreezeOnSwitch = this.addProperty(ConfigTypes.BOOLEAN,
        "Switch.Reset-On-Switch.Freeze",
        true,
        "Force freeze OFF when a switch happens.");

    private final ConfigProperty<Boolean> resetGodOnSwitch = this.addProperty(ConfigTypes.BOOLEAN,
        "Switch.Reset-On-Switch.God",
        false,
        "Force god OFF when a switch happens. Off by default: god is a regular per-profile toggle.");

    private final ConfigProperty<Boolean> blockWhileTagged = this.addProperty(ConfigTypes.BOOLEAN,
        "Combat.Block-While-Tagged",
        true,
        "Block profile switching while combat-tagged (source: global config.yml 'Combat.Hook').",
        "Bypass with 'sunlight.profiles.bypass.safety'.");

    public void load(FileConfig config) {
        super.load(config);
    }

    public int getMaxPerPlayer() {
        return Math.max(1, this.maxPerPlayer.get());
    }

    public boolean isRenameAllowed() {
        return this.allowRename.get();
    }

    public boolean isDeleteAllowed() {
        return this.allowDelete.get();
    }

    public int getSwitchCooldown() {
        return Math.max(0, this.switchCooldown.get());
    }

    public double getSwitchCost() {
        return Math.max(0D, this.switchCost.get());
    }

    public int getSwitchWarmup() {
        return Math.max(0, this.switchWarmup.get());
    }

    public boolean isSwitchConfirmGui() {
        return this.switchConfirmGui.get();
    }

    public boolean isBlockWhileVanished() {
        return this.blockWhileVanished.get();
    }

    public boolean isBlockWhileFrozen() {
        return this.blockWhileFrozen.get();
    }

    public boolean isBlockWhileFlying() {
        return this.blockWhileFlying.get();
    }

    public boolean isBlockWhileDead() {
        return this.blockWhileDead.get();
    }

    public boolean isResetVanishOnSwitch() {
        return this.resetVanishOnSwitch.get();
    }

    public boolean isResetFreezeOnSwitch() {
        return this.resetFreezeOnSwitch.get();
    }

    public boolean isResetGodOnSwitch() {
        return this.resetGodOnSwitch.get();
    }

    private final ConfigProperty<Boolean> effectsTitle = this.addProperty(ConfigTypes.BOOLEAN,
        "Switch.Effects.Title-Enabled",
        true,
        "Show a title when a profile switch completes.");

    private final ConfigProperty<String> effectsTitleText = this.addProperty(ConfigTypes.STRING,
        "Switch.Effects.Title-Text",
        "%name%",
        "Main title. Placeholders: %name% (profile name). '&' color codes allowed.");

    private final ConfigProperty<String> effectsSubtitleText = this.addProperty(ConfigTypes.STRING,
        "Switch.Effects.Subtitle-Text",
        "Profile loaded",
        "Subtitle below the title. Empty disables it.");

    private final ConfigProperty<String> effectsSound = this.addProperty(ConfigTypes.STRING,
        "Switch.Effects.Sound",
        "ENTITY_PLAYER_LEVELUP",
        "Sound played on switch. Empty disables it. Any Bukkit Sound name.");

    private final ConfigProperty<Double> effectsVolume = this.addProperty(ConfigTypes.DOUBLE,
        "Switch.Effects.Volume",
        1D,
        "Sound volume.");

    private final ConfigProperty<Double> effectsPitch = this.addProperty(ConfigTypes.DOUBLE,
        "Switch.Effects.Pitch",
        1D,
        "Sound pitch.");

    private final ConfigProperty<String> effectsParticles = this.addProperty(ConfigTypes.STRING,
        "Switch.Effects.Particles",
        "HAPPY_VILLAGER",
        "Particles burst on switch. Empty disables it. Any Bukkit Particle name.");

    private final ConfigProperty<Integer> effectsParticleCount = this.addProperty(ConfigTypes.INT,
        "Switch.Effects.Particle-Count",
        30,
        "How many particles to spawn.");

    private final ConfigProperty<Boolean> luckPermsContext = this.addProperty(ConfigTypes.BOOLEAN,
        "Integration.LuckPerms-Context",
        true,
        "Expose the active profile as LuckPerms context 'profile' (e.g. for per-profile permissions).");

    public boolean isEffectsTitle() {
        return this.effectsTitle.get();
    }

    public String getEffectsTitleText() {
        return this.effectsTitleText.get();
    }

    public String getEffectsSubtitleText() {
        return this.effectsSubtitleText.get();
    }

    public String getEffectsSound() {
        return this.effectsSound.get();
    }

    public float getEffectsVolume() {
        return (float) Math.max(0D, this.effectsVolume.get());
    }

    public float getEffectsPitch() {
        return (float) Math.max(0D, this.effectsPitch.get());
    }

    public String getEffectsParticles() {
        return this.effectsParticles.get();
    }

    public int getEffectsParticleCount() {
        return Math.max(0, this.effectsParticleCount.get());
    }

    public boolean isLuckPermsContext() {
        return this.luckPermsContext.get();
    }

    public boolean isBlockWhileTagged() {
        return this.blockWhileTagged.get();
    }
}
