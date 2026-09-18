package su.nightexpress.sunlight.module.scoreboard.board.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.score.ScoreFormat;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisplayScoreboard;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerResetScore;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateScore;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.bridge.paper.PaperBridge;
import su.nightexpress.nightcore.util.Version;
import su.nightexpress.nightcore.util.bridge.Software;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.text.night.NightMessage;
import su.nightexpress.sunlight.module.scoreboard.board.BoardDefinition;

import java.util.Optional;

public class PacketBoard extends AbstractBoard<PacketWrapper<?>> {

    public PacketBoard(Player player, PlaceholderContext placeholderContext, BoardDefinition boardDefinition) {
        super(player, placeholderContext, boardDefinition);
    }

    @Override

    protected WrapperPlayServerScoreboardObjective createObjectivePacket(ObjectiveMode mode, String displayName) {
        WrapperPlayServerScoreboardObjective.ObjectiveMode objectiveMode = switch (mode) {
            case CREATE -> WrapperPlayServerScoreboardObjective.ObjectiveMode.CREATE;
            case REMOVE -> WrapperPlayServerScoreboardObjective.ObjectiveMode.REMOVE;
            case UPDATE -> WrapperPlayServerScoreboardObjective.ObjectiveMode.UPDATE;
        };

        return new WrapperPlayServerScoreboardObjective(
                this.identifier,
                objectiveMode,
                adaptComponent(displayName),
                WrapperPlayServerScoreboardObjective.RenderType.INTEGER,
                ScoreFormat.blankScore());
    }

    @Override

    protected WrapperPlayServerResetScore createResetScorePacket(String scoreId) {
        return new WrapperPlayServerResetScore(scoreId, this.identifier);
    }

    @Override

    protected WrapperPlayServerUpdateScore createScorePacket(String scoreId, int score, String text) {
        WrapperPlayServerUpdateScore scorePacket = new WrapperPlayServerUpdateScore(
                scoreId,
                WrapperPlayServerUpdateScore.Action.CREATE_OR_UPDATE_ITEM,
                this.identifier,
                Optional.of(score));

        scorePacket.setEntityDisplayName(adaptComponent(text));
        scorePacket.setScoreFormat(ScoreFormat.blankScore());

        return scorePacket;
    }

    @Override

    protected WrapperPlayServerDisplayScoreboard createDisplayPacket() {
        return new WrapperPlayServerDisplayScoreboard(1, this.identifier);
    }

    @Override
    protected void sendPacket(Player player, PacketWrapper<?> wrapper) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, wrapper);
    }

    private static Component adaptComponent(String string) {
        if (Version.isPaper()) {
            return ((PaperBridge) Software.get()).getTextComponentAdapter().adaptComponent(NightMessage.parse(string));
        } else {
            return GsonComponentSerializer.gson().deserialize(NightMessage.asJson(string));
        }
    }
}
