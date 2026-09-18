package su.nightexpress.sunlight.nms;

import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import su.nightexpress.sunlight.api.PortableContainer;

public interface SunNMS {

    Object fineChatPacket(
            Object packet);

    Player loadPlayerData(UUID id, String name);

    Inventory getPlayerInventory(Player player);

    void openPlayerInventory(Player player, Player owner);

    Inventory getPlayerEnderChest(Player player);

    void setGameMode(Player player, GameMode mode);

    void teleport(Player player, Location location);

    void openContainer(Player player, PortableContainer menuType);

    void dropFallingContent(FallingBlock fallingBlock);
}
