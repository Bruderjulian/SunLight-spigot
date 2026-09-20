package su.nightexpress.sunlight.nms.v26p1;

import java.lang.reflect.Method;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftFallingBlock;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import com.mojang.authlib.GameProfile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import su.nightexpress.nightcore.util.Reflex;
import su.nightexpress.sunlight.api.PortableContainer;
import su.nightexpress.sunlight.nms.SunNMS;
import su.nightexpress.sunlight.nms.v26p1.container.PlayerEnderChest;
import su.nightexpress.sunlight.nms.v26p1.container.PlayerInventory;

public class NMSv26p1 implements SunNMS {

    private static final Method SET_GAME_MODE = Reflex.safeMethod(ServerPlayerGameMode.class,
            "setGameModeForPlayer", "a", GameType.class, GameType.class);

    private static final Method OPEN_CUSTOM_INVENTORY = Reflex.safeMethod(CraftHumanEntity.class, "openCustomInventory",
            Inventory.class, ServerPlayer.class, net.minecraft.world.inventory.MenuType.class);

    @Override
    public void dropFallingContent(final FallingBlock fallingBlock) {
        final CraftFallingBlock craftBlock = (CraftFallingBlock) fallingBlock;
        final FallingBlockEntity nmsBlock = craftBlock.getHandle();

        nmsBlock.spawnAtLocation((ServerLevel) nmsBlock.level(), nmsBlock.getBlockState().getBlock());
    }

    public Object fineChatPacket(final Object packet) {
        final ClientboundPlayerChatPacket chatPacket = (ClientboundPlayerChatPacket) packet;
        final Component component = chatPacket.unsignedContent() == null ? Component.literal(chatPacket.body()
                .content()) : chatPacket.unsignedContent();

        final ChatType.Bound decorator = new ChatType.Bound(chatPacket.chatType().chatType(),
                chatPacket.chatType().name(),
                chatPacket.chatType()
                        .targetName());

        return new ClientboundSystemChatPacket(decorator.decorate(component), false);
    }

    @Override

    public Player loadPlayerData(final UUID id, final String name) {
        final CraftServer craftServer = (CraftServer) Bukkit.getServer();
        final DedicatedServer server = craftServer.getServer();
        final ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null)
            throw new IllegalStateException("Server level is null");

        final ServerPlayer serverPlayer = new ServerPlayer(server, level, new GameProfile(id, name),
                ClientInformation.createDefault());

        final var input = craftServer.getHandle().playerIo.load(new NameAndId(id, name)).orElse(new CompoundTag());
        final var value = TagValueInput.create(new ProblemReporter.Collector(), serverPlayer.registryAccess(), input);
        serverPlayer.load(value);
        // serverPlayer.loadGameTypes(input); // Save GameMode on load data

        return serverPlayer.getBukkitEntity();
    }

    @Override
    public void setGameMode(final Player player, final org.bukkit.GameMode mode) {
        final CraftPlayer craftPlayer = (CraftPlayer) player;
        final ServerPlayer serverPlayer = craftPlayer.getHandle();

        final GameType gameType = GameType.byName(mode.name().toLowerCase());
        final GameType previous = serverPlayer.gameMode.getPreviousGameModeForPlayer();

        Reflex.invokeMethod(SET_GAME_MODE, serverPlayer.gameMode, gameType, previous);
        craftPlayer.saveData();
    }

    @Override
    public void teleport(final Player player, final Location location) {
        final CraftPlayer craftPlayer = (CraftPlayer) player;
        final ServerPlayer serverPlayer = craftPlayer.getHandle();
        serverPlayer.setPosRaw(location.getX(), location.getY(), location.getZ());
        if (player.getWorld() != location.getWorld() && location.getWorld() != null) {
            final CraftWorld craftWorld = (CraftWorld) location.getWorld();
            serverPlayer.setServerLevel(craftWorld.getHandle());
        }
        craftPlayer.saveData();
    }

    @Override

    public Inventory getPlayerEnderChest(final Player player) {
        return new PlayerEnderChest((CraftPlayer) player).getInventory();
    }

    @Override

    public Inventory getPlayerInventory(final Player player) {
        return new PlayerInventory((CraftPlayer) player).getInventory();
    }

    @Override
    public void openPlayerInventory(final Player player, final Player owner) {
        // There is a "wrong" menu type obtained in the CraftHumanEntity#openInventory
        // -> CraftContainer#getNotchInventoryType(inventory)
        // This is caused by CraftInventory wrapper with the PlayerInventory container
        // inside, which getNotchInventoryType takes it into an account and returns
        // wrong MenuType.
        // We have to hardcode the 'windowType' variable here as 9X5 menu type to
        // prevent Network Protocol Error due to slots size mismatch.
        Reflex.invokeMethod(OPEN_CUSTOM_INVENTORY, null, this.getPlayerInventory(owner),
                ((CraftPlayer) player).getHandle(),
                net.minecraft.world.inventory.MenuType.GENERIC_9x5);
    }

    @Override
    public void openContainer(final Player player, final PortableContainer menuType) {

        player.openInventory(this.createContainer(menuType, player).getBukkitView());
    }

    private AbstractContainerMenu createContainer(final PortableContainer type, final Player player) {
        final ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
        final int contId = nmsPlayer.nextContainerCounter();
        final ContainerLevelAccess access = ContainerLevelAccess.create(nmsPlayer.level(), nmsPlayer.blockPosition());
        final net.minecraft.world.entity.player.Inventory inventory = nmsPlayer.getInventory();

        final AbstractContainerMenu menu = switch (type) {
            case ANVIL -> new AnvilMenu(contId, inventory, access);
            case WORKBENCH -> new CraftingMenu(contId, inventory, access);
            case ENCHANTING_TABLE -> new EnchantmentMenu(contId, inventory, access);
            case LOOM -> new LoomMenu(contId, inventory, access);
            case SMITHING_TABLE -> new SmithingMenu(contId, inventory, access);
            case GRINDSTONE -> new GrindstoneMenu(contId, inventory, access);
            case STONECUTTER -> new StonecutterMenu(contId, inventory, access);
            case CARTOGRAPHY_TABLE -> new CartographyTableMenu(contId, inventory, access);
        };
        menu.checkReachable = false;

        return menu;
    }
}
