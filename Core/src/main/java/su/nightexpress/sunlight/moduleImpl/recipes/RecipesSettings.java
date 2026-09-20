package su.nightexpress.sunlight.moduleImpl.recipes;

import su.nightexpress.nightcore.configuration.AbstractConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigTypes;

public class RecipesSettings extends AbstractConfig {

    public final ConfigProperty<String> defaultNamespace = this.addProperty(ConfigTypes.STRING,
            "Default_Namespace",
            "sunlight_recipes",
            "The namespace used for newly created recipes (ADD/MODIFY) when the configured recipe key has no namespace part.",
            "[Defaults: sunlight_recipes]");

    public RecipesSettings() {

    }
}