package su.nightexpress.sunlight.moduleImpl.nametags.render;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.nametag.NameTagManager;
import me.neznamy.tab.api.placeholder.Placeholder;
import me.neznamy.tab.api.placeholder.PlaceholderManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.hook.HookId;
import su.nightexpress.sunlight.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * The only writer of nametag data in the plugin.
 * <p>
 * TAB's {@code NameTagManager} is the single source of truth for nametags; nothing here
 * touches packets directly. Values are cached per player so a recompute that produces an
 * identical nameplate costs no TAB calls, which matters because placeholders are
 * re-resolved on every recompute.
 */
public class TabNameTagBackend {

    private static final String PREFIX_PLACEHOLDER = "%sunlight_nametags_prefix%";
    private static final String SUFFIX_PLACEHOLDER = "%sunlight_nametags_suffix%";

    private final SunLightPlugin plugin;
    private final Function<UUID, String> prefixSupplier;
    private final Function<UUID, String> suffixSupplier;

    private final Map<UUID, Applied> applied = new HashMap<>();
    private final List<String> registeredPlaceholders = new ArrayList<>();

    public TabNameTagBackend(@NotNull SunLightPlugin plugin,
            @NotNull Function<UUID, String> prefixSupplier,
            @NotNull Function<UUID, String> suffixSupplier
    ) {
        this.plugin = plugin;
        this.prefixSupplier = prefixSupplier;
        this.suffixSupplier = suffixSupplier;
    }

    // -----------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------

    /** Registers the TAB placeholders that feed the composed nameplate. */
    public void registerPlaceholders() {
        PlaceholderManager manager = this.placeholderManager();
        if (manager == null) return;

        this.registeredPlaceholders.add(PREFIX_PLACEHOLDER);
        manager.registerPlayerPlaceholder(PREFIX_PLACEHOLDER, -1, tabPlayer -> {
            UUID id = this.uuidOf(tabPlayer);
            return id == null ? "" : this.prefixSupplier.apply(id);
        });

        this.registeredPlaceholders.add(SUFFIX_PLACEHOLDER);
        manager.registerPlayerPlaceholder(SUFFIX_PLACEHOLDER, -1, tabPlayer -> {
            UUID id = this.uuidOf(tabPlayer);
            return id == null ? "" : this.suffixSupplier.apply(id);
        });

        this.plugin.debug("Registered " + this.registeredPlaceholders.size() + " TAB nametag placeholders.");
    }

    public void unregisterPlaceholders() {
        PlaceholderManager manager = this.placeholderManager();
        if (manager == null) return;

        for (String placeholder : this.registeredPlaceholders) {
            Placeholder existing = manager.getPlaceholder(placeholder);
            if (existing != null) manager.unregisterPlaceholder(existing);
        }
        this.registeredPlaceholders.clear();
    }

    // -----------------------------------------------------
    // Applying
    // -----------------------------------------------------

    /**
     * Applies a nameplate to TAB, skipping the call entirely when the value is unchanged.
     * Must run on the main thread.
     */
    public void apply(@NotNull Player player, @NotNull Nameplate nameplate) {
        TabPlayer tabPlayer = this.tabPlayer(player);
        if (tabPlayer == null || !tabPlayer.isLoaded()) return;

        NameTagManager manager = this.nameTagManager();
        if (manager == null) return;

        UUID id = player.getUniqueId();
        Applied previous = this.applied.get(id);
        if (previous != null && previous.matches(nameplate)) return;

        this.applied.put(id, new Applied(nameplate));
        manager.setPrefix(tabPlayer, nameplate.prefix());
        manager.setSuffix(tabPlayer, nameplate.suffix());
    }

    /** Removes any custom nametag from TAB and forgets the cached value. */
    public void remove(@NotNull UUID playerId) {
        this.applied.remove(playerId);

        TabPlayer tabPlayer = this.tabPlayer(playerId);
        NameTagManager manager = this.nameTagManager();
        if (tabPlayer == null || manager == null || !tabPlayer.isLoaded()) return;

        manager.setPrefix(tabPlayer, "");
        manager.setSuffix(tabPlayer, "");
    }

    public void clearCache() {
        this.applied.clear();
    }

    // -----------------------------------------------------
    // Internals
    // -----------------------------------------------------

    private @Nullable TabAPI api() {
        if (!Utils.isLoaded(HookId.TAB)) return null;
        try {
            return TabAPI.getInstance();
        } catch (Throwable throwable) {
            // TAB throws when its API instance is not (yet) ready.
            return null;
        }
    }

    private @Nullable TabPlayer tabPlayer(@NotNull Player player) {
        return this.tabPlayer(player.getUniqueId());
    }

    private @Nullable TabPlayer tabPlayer(@NotNull UUID playerId) {
        TabAPI api = this.api();
        return api == null ? null : api.getPlayer(playerId);
    }

    private @Nullable NameTagManager nameTagManager() {
        TabAPI api = this.api();
        return api == null ? null : api.getNameTagManager();
    }

    private @Nullable PlaceholderManager placeholderManager() {
        TabAPI api = this.api();
        return api == null ? null : api.getPlaceholderManager();
    }

    private @Nullable UUID uuidOf(@Nullable TabPlayer tabPlayer) {
        return tabPlayer == null ? null : tabPlayer.getUniqueId();
    }

    private record Applied(Nameplate nameplate) {

        private boolean matches(Nameplate other) {
            return this.nameplate.equals(other);
        }
    }
}
