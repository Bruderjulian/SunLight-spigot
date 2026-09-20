package su.nightexpress.sunlight.moduleImpl.glow.handler;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.sunlight.SunLightPlugin;

import java.util.Collection;
import java.util.List;

public class GlowPacketsHandler extends GlowPacketHandler {

    public GlowPacketsHandler(@NotNull SunLightPlugin plugin) {
        super(plugin);
    }

    @Override
    public void sendGlow(@NotNull Player target,
                         @NotNull NamedTextColor color,
                         @NotNull Collection<? extends Player> viewers) {
        if (viewers.isEmpty()) return;

        String teamId = teamId(target);
        WrapperPlayServerTeams.ScoreBoardTeamInfo info = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
            Component.text(teamId),
            Component.empty(),
            Component.empty(),
            WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
            WrapperPlayServerTeams.CollisionRule.ALWAYS,
            color,
            WrapperPlayServerTeams.OptionData.NONE);

        // CREATE on an existing client-side team acts as an update, so no REMOVE-then-ADD churn.
        WrapperPlayServerTeams packet = new WrapperPlayServerTeams(
            teamId, WrapperPlayServerTeams.TeamMode.CREATE, info, List.of(target.getName()));

        for (Player viewer : viewers) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
        }
    }

    @Override
    public void removeGlow(@NotNull Player target,
                           @NotNull Collection<? extends Player> viewers) {
        if (viewers.isEmpty()) return;

        WrapperPlayServerTeams packet = new WrapperPlayServerTeams(
            teamId(target), WrapperPlayServerTeams.TeamMode.REMOVE,
            (WrapperPlayServerTeams.ScoreBoardTeamInfo) null, List.of(target.getName()));

        for (Player viewer : viewers) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
        }
    }
}
