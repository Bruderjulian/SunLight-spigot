package su.nightexpress.sunlight.moduleImpl.nametags.hook;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.hook.HookId;
import su.nightexpress.sunlight.utils.LegacyText;
import su.nightexpress.sunlight.utils.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Team contribution from UltimateTeams, driven entirely through reflection.
 * <p>
 * UltimateTeams publishes no Maven artifact that resolves, so a compile-time dependency is
 * impossible. Every reflective access is resolved once and cached, and every call is
 * guarded, so a version bump degrades the feature instead of breaking the plugin.
 * <p>
 * UltimateTeams' {@code Team} model exposes only an id, name, prefix and motd - there is no
 * team colour and no team suffix. The colour is therefore derived from the last colour code
 * embedded in the team name, and the suffix is always empty.
 */
public class UltimateTeamsSource implements TeamSource {

    private static final String API_CLASS = "dev.xf3d3.ultimateteams.api.UltimateTeamsAPI";
    private static final String TEAM_CLASS = "dev.xf3d3.ultimateteams.models.Team";
    private static final String EVENTS_PACKAGE = "dev.xf3d3.ultimateteams.api.events.";

    /** Events that can change which team a player belongs to. */
    private static final List<String> WATCHED_EVENTS = List.of(
            "TeamMemberJoinEvent",
            "TeamMemberLeaveEvent",
            "TeamCreateEvent",
            "TeamDisbandEvent",
            "TeamTransferOwnershipEvent"
    );

    private final SunLightPlugin plugin;

    private Method apiGetInstance;
    private Method apiIsLoaded;
    private Method apiFindTeamByMember;
    private Method teamGetId;
    private Method teamGetName;
    private Method teamGetPrefix;
    private Method teamGetMembers;

    private boolean resolved;
    private final Map<UUID, TeamInfo> cache = new ConcurrentHashMap<>();

    public UltimateTeamsSource(@NotNull SunLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getId() {
        return "ultimateteams";
    }

    @Override
    public boolean isAvailable() {
        return Utils.isLoaded(HookId.ULTIMATE_TEAMS) && this.resolve();
    }

    @Override
    public @NotNull CompletableFuture<TeamInfo> resolveTeam(@NotNull Player player) {
        if (!this.isAvailable()) return CompletableFuture.completedFuture(TeamInfo.EMPTY);

        UUID id = player.getUniqueId();
        TeamInfo cached = this.cache.get(id);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        TeamInfo info;
        try {
            Object api = this.apiGetInstance.invoke(null);
            info = this.readTeam(api, id);
        } catch (Throwable throwable) {
            Utils.printStacktrace(this, throwable);
            return CompletableFuture.completedFuture(TeamInfo.EMPTY);
        }

        this.cache.put(id, info);
        return CompletableFuture.completedFuture(info);
    }

    @Override
    public void registerListeners(@NotNull Runnable onChange) {
        if (!this.isAvailable()) return;

        Listener listener = new Listener() { };
        for (String name : WATCHED_EVENTS) {
            Class<? extends Event> type = this.eventClass(name);
            if (type == null) continue;

            this.plugin.getPluginManager().registerEvent(type,
                    listener,
                    EventPriority.MONITOR,
                    (listener0, event) -> {
                        Set<UUID> affected = this.affectedPlayers(event);
                        if (affected.isEmpty()) {
                            this.cache.clear();
                        } else {
                            affected.forEach(this.cache::remove);
                        }
                        this.plugin.runTask(onChange);
                    },
                    this.plugin,
                    true
            );
        }
    }

    @Override
    public void shutdown() {
        this.cache.clear();
    }

    // -----------------------------------------------------
    // Reflection
    // -----------------------------------------------------

    private boolean resolve() {
        if (this.resolved) return true;

        try {
            Class<?> apiClass = Class.forName(API_CLASS);
            Class<?> teamClass = Class.forName(TEAM_CLASS);

            this.apiGetInstance = apiClass.getMethod("getInstance");
            this.apiIsLoaded = apiClass.getMethod("isLoaded");
            this.apiFindTeamByMember = apiClass.getMethod("findTeamByMember", UUID.class);

            this.teamGetId = teamClass.getMethod("getId");
            this.teamGetName = teamClass.getMethod("getName");
            this.teamGetPrefix = teamClass.getMethod("getPrefix");
            this.teamGetMembers = teamClass.getMethod("getMembers");

            this.resolved = true;
            return true;
        } catch (Throwable throwable) {
            this.plugin.getLogger().warning("UltimateTeams is installed but its API could not be resolved: "
                    + throwable.getMessage());
            return false;
        }
    }

    private @Nullable Class<? extends Event> eventClass(@NotNull String name) {
        try {
            return Class.forName(EVENTS_PACKAGE + name).asSubclass(Event.class);
        } catch (Throwable throwable) {
            return null; // Event renamed or removed in this UltimateTeams version.
        }
    }

    private @NotNull TeamInfo readTeam(@Nullable Object api, @NotNull UUID playerId) throws Exception {
        if (api == null) return TeamInfo.EMPTY;
        if (!(boolean) this.apiIsLoaded.invoke(api)) return TeamInfo.EMPTY;

        Object result = this.apiFindTeamByMember.invoke(api, playerId);
        if (!(result instanceof Optional<?> optional) || optional.isEmpty()) return TeamInfo.EMPTY;

        Object team = optional.get();
        String name = (String) this.teamGetName.invoke(team);
        String prefix = (String) this.teamGetPrefix.invoke(team);
        int id = (Integer) this.teamGetId.invoke(team);

        return TeamInfo.of(String.valueOf(id), name, prefix, "", LegacyText.extractLastColor(name));
    }

    /**
     * Collects every player a team event could have affected, by inspecting the event's
     * fields for UUIDs, players and teams. Field-name based dispatch would break on a
     * rename, whereas value inspection does not.
     */
    private @NotNull Set<UUID> affectedPlayers(@NotNull Object event) {
        Set<UUID> ids = new LinkedHashSet<>();

        for (Class<?> type = event.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                field.setAccessible(true);
                Object value;
                try {
                    value = field.get(event);
                } catch (Throwable throwable) {
                    continue;
                }
                this.collect(value, ids);
            }
        }
        return ids;
    }

    private void collect(@Nullable Object value, @NotNull Set<UUID> target) {
        switch (value) {
            case UUID id -> target.add(id);
            case Player player -> target.add(player.getUniqueId());
            case OfflinePlayer offline -> target.add(offline.getUniqueId());
            case Map<?, ?> map -> map.keySet().forEach(key -> this.collect(key, target));
            default -> {
                if (value == null) return;
                // A Team instance: every member of it is affected.
                try {
                    Object members = value.getClass().getMethod("getMembers").invoke(value);
                    if (members instanceof Map<?, ?> map) {
                        map.keySet().forEach(key -> this.collect(key, target));
                    }
                } catch (Throwable ignored) {
                    // Not a team, nothing to expand.
                }
            }
        }
    }
}
