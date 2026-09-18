package su.nightexpress.sunlight.moduleImpl.inventories;

import su.nightexpress.nightcore.config.ConfigValue;

public class InventoriesSettings {

    public static final ConfigValue<Boolean> CLEAR_REQUIRE_CONFIRMATION = ConfigValue.create(
            "Clear.Require_Confirmation",
            true,
            "If enabled, players must confirm via dialog before clearing their own inventory or Ender Chest.");

    public static final ConfigValue<Boolean> CLEAR_CONFIRM_SELF_ONLY = ConfigValue.create("Clear.Confirm_Self_Only",
            true,
            "If enabled, confirmation is only required when clearing your own inventory or Ender Chest.",
            "Clearing other players will always be instant.");
}
