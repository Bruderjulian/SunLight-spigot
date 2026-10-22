package su.nightexpress.sunlight.api.provider.dto;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A location cached by the Back Location module, as handed out by
 * {@link su.nightexpress.sunlight.api.provider.BackLocationProvider}.
 * <p>
 * Coordinates only, never a prebuilt {@code Location}: the stored world may be unloaded, and a
 * caller holding a Bukkit location would have no way to tell that it points nowhere.
 *
 * @param type       the slot this location occupies
 * @param worldName  the name of the world, which may not currently be loaded
 * @param x          block x coordinate
 * @param y          block y coordinate
 * @param z          block z coordinate
 * @param expireDate epoch milliseconds after which the entry is dropped
 */
public record StoredLocationHandle(@NotNull BackLocationType type,
                                   @NotNull String worldName,
                                   double x,
                                   double y,
                                   double z,
                                   long expireDate
) {

    /** Whether the world this location points at is currently loaded. */
    public boolean valid() {
        return Bukkit.getWorld(this.worldName) != null;
    }

    /** Whether the entry has neither expired nor lost its world. */
    public boolean resolvable() {
        return !this.expired() && this.valid();
    }

    /** Whether the expiry moment has already passed. */
    public boolean expired() {
        return System.currentTimeMillis() >= this.expireDate;
    }

    /** The resolved location, or {@code null} when the world is not loaded. */
    @Nullable
    public Location toLocation() {
        final World world = Bukkit.getWorld(this.worldName);
        return world == null ? null : new Location(world, this.x, this.y, this.z);
    }
}