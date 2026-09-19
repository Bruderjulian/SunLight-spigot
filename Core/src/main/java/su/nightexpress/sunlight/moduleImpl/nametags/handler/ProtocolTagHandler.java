package su.nightexpress.sunlight.moduleImpl.nametags.handler;

import java.util.Collection;
import java.util.Optional;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.EnumWrappers.ChatFormatting;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedTeamParameters;

import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.text.night.NightMessage;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.utils.Utils;

public class ProtocolTagHandler extends NametagHandler {

    public ProtocolTagHandler(SunLightPlugin plugin) {
        super(plugin);
    }

    @Override
    protected void onLoad() {

    }

    @Override
    protected void onShutdown() {

    }

    @Override
    protected void sendPacket(NametagHandler.TeamMode mode,
            String teamId,
            String teamPrefix,
            String teamSuffix,
            String teamColorRaw,
            Player playerOfTeam,
            Collection<? extends Player> receivers) {
        PacketContainer packetTeam = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
        Collection<String> entities = Lists.newList(playerOfTeam.getName());
        ChatFormatting teamColor = Utils.enumOptionalValueOf(teamColorRaw, ChatFormatting.class)
                .orElse(ChatFormatting.GRAY);

        packetTeam.getStrings().write(0, teamId); // Name
        packetTeam.getIntegers().write(0, mode.index); // Mode. 1 - Remove, 0 - Create
        packetTeam.getSpecificModifier(Collection.class).write(0, entities);

        if (mode == TeamMode.CREATE) {
            WrappedTeamParameters parameters = WrappedTeamParameters.newBuilder()
                    .displayName(WrappedChatComponent.fromText(teamId))
                    .prefix(WrappedChatComponent.fromJson(NightMessage.asJson(teamPrefix)))
                    .suffix(WrappedChatComponent.fromJson(NightMessage.asJson(teamSuffix)))
                    .color(teamColor)
                    .nametagVisibility(EnumWrappers.TeamVisibility.ALWAYS)
                    .collisionRule(EnumWrappers.TeamCollisionRule.ALWAYS)
                    .options(0)
                    .build();
            packetTeam.getOptionalTeamParameters().write(0, Optional.of(parameters));
        }

        for (Player playerReceiver : receivers) {
            ProtocolLibrary.getProtocolManager().sendServerPacket(playerReceiver, packetTeam);
        }
    }
}
