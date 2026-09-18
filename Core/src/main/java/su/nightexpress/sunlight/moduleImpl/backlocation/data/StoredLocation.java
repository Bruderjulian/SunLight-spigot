package su.nightexpress.sunlight.moduleImpl.backlocation.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.concurrent.TimeUnit;

public class StoredLocation {

    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final long expireDate;

    public StoredLocation(String worldName, double x, double y, double z, int duration) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.expireDate = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(duration, TimeUnit.SECONDS);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= this.expireDate;
    }

    public boolean isValid() {
        return this.getWorld() != null;
    }

    public World getWorld() {
        return Bukkit.getWorld(this.worldName);
    }

    public Location toLocation() {
        World world = this.getWorld();
        if (world == null)
            return null;

        return new Location(world, x, y, z);
    }

    public String getWorldName() {
        return worldName;
    }

    public long getExpireDate() {
        return expireDate;
    }
}
