package su.nightexpress.sunlight.moduleImpl.freeze.config;

import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.util.Lists;

import java.util.List;

public class FreezeConfig {

    public static final ConfigValue<Boolean> BLOCK_MOVEMENT = ConfigValue.create("Restrictions.Block_Movement",
        true,
        "Sets whether frozen players can not move.",
        "Rotation (looking around) is still allowed."
    );

    public static final ConfigValue<Boolean> BLOCK_TELEPORT = ConfigValue.create("Restrictions.Block_Teleport",
        true,
        "Sets whether frozen players can not teleport (ender pearls, chorus fruit, etc.).",
        "Teleports caused by other plugins (e.g. staff teleporting the player) are still allowed."
    );

    public static final ConfigValue<Boolean> BLOCK_DAMAGE_INCOMING = ConfigValue.create("Restrictions.Block_Damage.Incoming",
        true,
        "Sets whether frozen players take no damage."
    );

    public static final ConfigValue<Boolean> BLOCK_DAMAGE_OUTGOING = ConfigValue.create("Restrictions.Block_Damage.Outgoing",
        true,
        "Sets whether frozen players can not deal damage."
    );

    public static final ConfigValue<Boolean> BLOCK_BLOCK_BREAK = ConfigValue.create("Restrictions.Block_Break",
        true,
        "Sets whether frozen players can not break blocks."
    );

    public static final ConfigValue<Boolean> BLOCK_BLOCK_PLACE = ConfigValue.create("Restrictions.Block_Place",
        true,
        "Sets whether frozen players can not place blocks."
    );

    public static final ConfigValue<Boolean> BLOCK_INTERACT = ConfigValue.create("Restrictions.Block_Interact",
        true,
        "Sets whether frozen players can not interact (doors, chests, attack entities, etc.)."
    );

    public static final ConfigValue<Boolean> BLOCK_INVENTORY = ConfigValue.create("Restrictions.Block_Inventory",
        true,
        "Sets whether frozen players can not click/drag items in inventories."
    );

    public static final ConfigValue<Boolean> BLOCK_ITEM_DROP = ConfigValue.create("Restrictions.Block_Item.Drop",
        true,
        "Sets whether frozen players can not drop items."
    );

    public static final ConfigValue<Boolean> BLOCK_ITEM_PICKUP = ConfigValue.create("Restrictions.Block_Item.Pickup",
        true,
        "Sets whether frozen players can not pick up items."
    );

    public static final ConfigValue<Boolean> BLOCK_COMMANDS = ConfigValue.create("Restrictions.Block_Commands",
        true,
        "Sets whether frozen players can not use commands."
    );

    public static final ConfigValue<List<String>> ALLOWED_COMMANDS = ConfigValue.create("Restrictions.Allowed_Commands",
        Lists.newList(),
        "Commands frozen players are still allowed to use.",
        "Entries without leading slash, e.g. 'msg', 'r', 'rules'."
    );

    public static final ConfigValue<Boolean> BLOCK_CHAT = ConfigValue.create("Restrictions.Block_Chat",
        false,
        "Sets whether frozen players can not chat."
    );

    public static final ConfigValue<Boolean> UNFREEZE_ON_QUIT = ConfigValue.create("Unfreeze_On_Quit",
        false,
        "Sets whether players should be automatically unfrozen when they leave the server."
    );
}
