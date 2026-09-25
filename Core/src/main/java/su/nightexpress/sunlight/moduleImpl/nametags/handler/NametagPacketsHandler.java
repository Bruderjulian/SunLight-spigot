package su.nightexpress.sunlight.moduleImpl.nametags.handler;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.util.bridge.Software;
import su.nightexpress.nightcore.util.text.NightMessage;
import su.nightexpress.sunlight.SunLightPlugin;

import java.util.Collection;
import java.util.List;

public class NametagPacketsHandler extends NametagPacketHandler {

    public NametagPacketsHandler(@NotNull SunLightPlugin plugin) {
        super(plugin);
    }

    @Override
    public void applyTeam(@NotNull Player target, @NotNull String prefix, @NotNull String suffix,
            @NotNull NamedTextColor color, @NotNull Collection<? extends Player> viewers) {
        if (viewers.isEmpty()) return;

        Component prefixComponent = adaptComponent(prefix);
        Component suffixComponent = adaptComponent(suffix);

        WrapperPlayServerTeams.ScoreBoardTeamInfo info = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
            Component.text(teamId(target)),
            prefixComponent,
            suffixComponent,
            WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
            WrapperPlayServerTeams.CollisionRule.ALWAYS,
            color,
            WrapperPlayServerTeams.OptionData.NONE);

        // CREATE on an existing client-side team acts as an update, so no REMOVE-then-ADD churn.
        WrapperPlayServerTeams packet = new WrapperPlayServerTeams(
            teamId(target), WrapperPlayServerTeams.TeamMode.CREATE, info, List.of(target.getName()));

        for (Player viewer : viewers) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
        }
    }

    @Override
    public void removeTeam(@NotNull Player target, @NotNull Collection<? extends Player> viewers) {
        if (viewers.isEmpty()) return;

        WrapperPlayServerTeams packet = new WrapperPlayServerTeams(
            teamId(target), WrapperPlayServerTeams.TeamMode.REMOVE,
            (WrapperPlayServerTeams.ScoreBoardTeamInfo) null, List.of(target.getName()));

        for (Player viewer : viewers) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
        }
    }

    @SuppressWarnings("unchecked")
    private static @NotNull Component adaptComponent(@NotNull String text) {
        if (text.isBlank()) return Component.empty();
        return (Component) Software.get().getTextComponentAdapter().adaptComponent(NightMessage.parse(text));
    }
}