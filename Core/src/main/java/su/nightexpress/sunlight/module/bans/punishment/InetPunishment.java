package su.nightexpress.sunlight.module.bans.punishment;

import org.bukkit.entity.Player;

import su.nightexpress.nightcore.util.placeholder.PlaceholderResolver;
import su.nightexpress.sunlight.module.bans.BansPlaceholders;
import su.nightexpress.sunlight.SLUtils;

import java.net.InetAddress;

public class InetPunishment extends AbstractPunishment {

    private final InetAddress address;

    public InetPunishment(InetAddress address, PunishmentData data, boolean active) {
        super(data, active);
        this.address = address;
    }

    @Override

    public PlaceholderResolver placeholders() {
        return BansPlaceholders.INET_PUNISHMENT.resolver(this);
    }

    @Override
    public boolean isApplicable(Player player) {
        return SLUtils.getInetAddress(player).map(address -> address.equals(this.address)).orElse(false);
    }

    @Override

    public String getName() {
        return this.getRawAddress();
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public String getRawAddress() {
        return this.address.getHostAddress();
    }
}
