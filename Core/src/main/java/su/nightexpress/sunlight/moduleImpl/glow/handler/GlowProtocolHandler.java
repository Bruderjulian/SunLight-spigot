package su.nightexpress.sunlight.moduleImpl.glow.handler;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedTeamParameters;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.utils.Utils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class GlowProtocolHandler extends GlowPacketHandler {

    public GlowProtocolHandler(@NotNull SunLightPlugin plugin) {
        super(plugin);
    }

    @Override
    public void sendGlow(@NotNull Player target,
                         @NotNull NamedTextColor color,
                         @NotNull Collection<? extends Player> viewers) {
        if (viewers.isEmpty()) return;

        String teamId = teamId(target);
        EnumWrappers.ChatFormatting formatting = Utils
            .enumOptionalValueOf(color.examinableName(), EnumWrappers.ChatFormatting.class)
            .orElse(EnumWrappers.ChatFormatting.WHITE);

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, teamId);
        packet.getIntegers().write(0, 0); // CREATE (updates in place when the team exists client-side)
        packet.getSpecificModifier(Collection.class).write(0, List.of(target.getName()));

        WrappedTeamParameters parameters = WrappedTeamParameters.newBuilder()
            .displayName(WrappedChatComponent.fromText(teamId))
            .prefix(WrappedChatComponent.fromText(""))
            .suffix(WrappedChatComponent.fromText(""))
            .color(formatting)
            .nametagVisibility(EnumWrappers.TeamVisibility.ALWAYS)
            .collisionRule(EnumWrappers.TeamCollisionRule.ALWAYS)
            .options(0)
            .build();
        packet.getOptionalTeamParameters().write(0, Optional.of(parameters));

        for (Player viewer : viewers) {
            ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, packet);
        }
    }

    @Override
    public void removeGlow(@NotNull Player target,
                           @NotNull Collection<? extends Player> viewers) {
        if (viewers.isEmpty()) return;

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, teamId(target));
        packet.getIntegers().write(0, 1); // REMOVE
        packet.getSpecificModifier(Collection.class).write(0, List.of(target.getName()));

        for (Player viewer : viewers) {
            ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, packet);
        }
    }
}
