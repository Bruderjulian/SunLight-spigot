package su.nightexpress.sunlight.module.ptp.request;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import su.nightexpress.nightcore.util.Players;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TeleportRequest {

    private final UUID targetId;
    private final UUID senderId;
    private final TeleportMode mode;

    private long expireDate;

    public TeleportRequest(Player sender, Player target, TeleportMode mode, int timeOut) {
        this(sender.getUniqueId(), target.getUniqueId(), mode, timeOut);
    }

    public TeleportRequest(UUID senderId, UUID targetId, TeleportMode mode, int timeOut) {
        this.senderId = senderId;
        this.targetId = targetId;
        this.mode = mode;
        this.expireDate = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeOut, TimeUnit.SECONDS);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= this.expireDate;
    }

    public void setExpired() {
        this.expireDate = System.currentTimeMillis();
    }

    public boolean isSender(String name) {
        return Players.getPlayer(name) == this.getSender();
        // return this.senderInfo.getName().equalsIgnoreCase(name);
    }

    public boolean isTarget(String name) {
        return Players.getPlayer(name) == this.getTarget();
        // return this.targetInfo.getName().equalsIgnoreCase(name);
    }

    public Player getSender() {
        return Bukkit.getPlayer(this.senderId);
    }

    public Player getTarget() {
        return Bukkit.getPlayer(this.targetId);
    }

    public UUID getSenderId() {
        return this.senderId;
    }

    public UUID getTargetId() {
        return this.targetId;
    }

    public TeleportMode getMode() {
        return this.mode;
    }

    public long getExpireDate() {
        return this.expireDate;
    }
}
