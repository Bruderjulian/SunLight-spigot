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
}
