package su.nightexpress.sunlight.moduleImpl.nametagsOld.handler;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedTeamParameters;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.util.text.NightMessage;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.utils.Utils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class NametagProtocolHandler extends NametagPacketHandler {

    public NametagProtocolHandler(@NotNull SunLightPlugin plugin) {
        super(plugin);
    }

    @Override
    public void applyTeam(@NotNull Player target, @NotNull String prefix, @NotNull String suffix,
            @NotNull NamedTextColor color, @NotNull Collection<? extends Player> viewers) {
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
            .prefix(adaptComponent(prefix))
            .suffix(adaptComponent(suffix))
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
    public void removeTeam(@NotNull Player target, @NotNull Collection<? extends Player> viewers) {
        if (viewers.isEmpty()) return;

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, teamId(target));
        packet.getIntegers().write(0, 1); // REMOVE
        packet.getSpecificModifier(Collection.class).write(0, List.of(target.getName()));

        for (Player viewer : viewers) {
            ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, packet);
        }
    }

    private static @NotNull WrappedChatComponent adaptComponent(@NotNull String text) {
        if (text.isBlank()) return WrappedChatComponent.fromText("");
        return WrappedChatComponent.fromJson(NightMessage.asJson(text));
    }
}