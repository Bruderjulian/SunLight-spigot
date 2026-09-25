package su.nightexpress.sunlight.moduleImpl.nametags.hook;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.hook.HookId;
import su.nightexpress.sunlight.utils.Utils;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Reads group membership from LuckPerms.
 * <p>
 * {@code loadUser} may hit storage, so every answer is produced on the LuckPerms
 * executor and handed back as a future. Notifications are bounced onto the main thread
 * before the consumer touches Bukkit state.
 */
public class LuckPermsGroupSource implements GroupSource {

    private final SunLightPlugin plugin;
    private final Set<EventSubscription<?>> subscriptions = new LinkedHashSet<>();

    public LuckPermsGroupSource(@NotNull SunLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getId() {
        return "luckperms";
    }

    @Override
    public boolean isAvailable() {
        return Utils.isLoaded(HookId.LUCKPERMS) && this.api() != null;
    }

    @Override
    public @NotNull CompletableFuture<Set<String>> resolveGroups(@NotNull Player player) {
        LuckPerms api = this.api();
        if (api == null) return CompletableFuture.completedFuture(Set.of());

        return api.getUserManager().loadUser(player.getUniqueId(), player.getName())
                .thenApply(this::toGroupNames)
                .exceptionally(throwable -> Set.of());
    }

    @Override
    public void registerListeners(@NotNull Runnable onChange) {
        LuckPerms api = this.api();
        if (api == null) return;

        // Recalculation is the one event that covers group adds, removes and data reloads.
        this.subscriptions.add(api.getEventBus().subscribe(this,
                UserDataRecalculateEvent.class,
                event -> this.plugin.runTask(onChange)
        ));
    }

    @Override
    public void shutdown() {
        this.subscriptions.forEach(EventSubscription::close);
        this.subscriptions.clear();
    }

    private @NotNull Set<String> toGroupNames(@Nullable User user) {
        if (user == null) return Set.of();

        Set<String> names = new LinkedHashSet<>();
        String primary = user.getPrimaryGroup();
        if (primary != null && !primary.isBlank()) names.add(primary.toLowerCase(Locale.ROOT));

        try {
            for (Group group : user.getInheritedGroups(user.getQueryOptions())) {
                String name = group.getName();
                if (name != null && !name.isBlank()) names.add(name.toLowerCase(Locale.ROOT));
            }
        } catch (Throwable ignored) {
            // Group walking can fail mid-recalculation; the primary group stays usable.
        }
        return names;
    }

    private @Nullable LuckPerms api() {
        if (!Utils.isLoaded(HookId.LUCKPERMS)) return null;
        try {
            return LuckPermsProvider.get();
        } catch (Throwable throwable) {
            return null;
        }
    }
}
