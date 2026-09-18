package su.nightexpress.sunlight.moduleImpl.bans.punishment;

import org.bukkit.entity.Player;

import su.nightexpress.nightcore.util.placeholder.PlaceholderResolver;
import su.nightexpress.sunlight.moduleImpl.bans.BansPlaceholders;

import java.util.UUID;

public class PlayerPunishment extends AbstractPunishment {

    private final UUID playerId;

    private String playerName;

    public PlayerPunishment(UUID playerId, String playerName, PunishmentData data, boolean active) {
        super(data, active);
        this.playerId = playerId;
        this.playerName = playerName;
    }

    @Override

    public PlaceholderResolver placeholders() {
        return BansPlaceholders.PLAYER_PUNISHMENT.resolver(this);
    }

    public void updateName(String name) {
        if (!this.playerName.equalsIgnoreCase(name)) {
            this.playerName = name;
            this.markDirty();
        }
    }

    @Override
    public boolean isApplicable(Player player) {
        return this.playerName.equalsIgnoreCase(player.getName());
    }

    @Override

    public String getName() {
        return this.playerName;
    }

    public UUID getPlayerId() {
        return this.playerId;
    }

    public String getPlayerName() {
        return this.playerName;
    }
}
