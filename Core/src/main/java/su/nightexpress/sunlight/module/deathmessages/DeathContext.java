package su.nightexpress.sunlight.module.deathmessages;

import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public record DeathContext(Player player,
        DamageSource damageSource,
        Entity causingEntity,
        Entity directEntity,
        ItemStack weapon) {

}
