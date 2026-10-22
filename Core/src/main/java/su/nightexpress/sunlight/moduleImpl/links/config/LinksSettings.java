package su.nightexpress.sunlight.moduleImpl.links.config;

import su.nightexpress.nightcore.configuration.AbstractConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigTypes;

public class LinksSettings extends AbstractConfig {

    private final ConfigProperty<Boolean> menuEnabled = this.addProperty(ConfigTypes.BOOLEAN, "Settings.Menu-Enabled",
            true,
            "Sets whether '/links' opens the link menu for players.",
            "When disabled, the link list is always printed in chat instead."
    );

    private final ConfigProperty<Boolean> executeCommands = this.addProperty(ConfigTypes.BOOLEAN,
            "Settings.Execute-Commands", true,
            "Sets whether a link's command is executed when the link is opened.",
            "When disabled, every link behaves as URL-only, regardless of its 'Command' value."
    );

    private final ConfigProperty<Boolean> showUrl = this.addProperty(ConfigTypes.BOOLEAN, "Settings.Show-Url", true,
            "Sets whether the raw URL is printed in the chat output and in the link item's lore.",
            "The URL is always clickable either way; this only controls the plain text copy."
    );

    private final ConfigProperty<Boolean> cooldownsEnabled = this.addProperty(ConfigTypes.BOOLEAN,
            "Settings.Cooldowns-Enabled", true,
            "Master switch for per-link cooldowns. When disabled, every link behaves as if its",
            "'Cooldown' was 0, regardless of the configured value."
    );

    private final ConfigProperty<Boolean> costsEnabled = this.addProperty(ConfigTypes.BOOLEAN,
            "Settings.Costs-Enabled", true,
            "Master switch for per-link Vault costs. When disabled, every link behaves as if its",
            "'Cost' was 0, regardless of the configured value."
    );

    public boolean isMenuEnabled() {
        return this.menuEnabled.get();
    }

    public boolean isExecuteCommandsEnabled() {
        return this.executeCommands.get();
    }

    public boolean isShowUrl() {
        return this.showUrl.get();
    }

    public boolean isCooldownsEnabled() {
        return this.cooldownsEnabled.get();
    }

    public boolean isCostsEnabled() {
        return this.costsEnabled.get();
    }
}
