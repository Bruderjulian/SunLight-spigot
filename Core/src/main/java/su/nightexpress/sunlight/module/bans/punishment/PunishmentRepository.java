package su.nightexpress.sunlight.module.bans.punishment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PunishmentRepository {

    private final Map<UUID, Map<UUID, PlayerPunishment>> playerPunishmentMap;
    private final Map<InetAddress, Map<UUID, InetPunishment>> inetPunishmentMap;

    private final Map<UUID, Set<PlayerPunishment>> activePlayerPunishmentMap;
    private final Map<InetAddress, Set<InetPunishment>> activeInetPunishmentMap;

    public PunishmentRepository() {
        this.playerPunishmentMap = new ConcurrentHashMap<>();
        this.inetPunishmentMap = new ConcurrentHashMap<>();
        this.activePlayerPunishmentMap = new ConcurrentHashMap<>();
        this.activeInetPunishmentMap = new ConcurrentHashMap<>();
    }

    public synchronized void clear() {
        this.playerPunishmentMap.clear();
        this.inetPunishmentMap.clear();
        this.activePlayerPunishmentMap.clear();
        this.activeInetPunishmentMap.clear();
    }

    public synchronized void addPlayerPunishment(PlayerPunishment punishment) {
        this.addPunishment(punishment, punishment.getPlayerId(), this.playerPunishmentMap,
                this.activePlayerPunishmentMap);
    }

    public synchronized void addInetPunishment(InetPunishment punishment) {
        this.addPunishment(punishment, punishment.getAddress(), this.inetPunishmentMap, this.activeInetPunishmentMap);
    }

    private synchronized <T extends AbstractPunishment, K> void addPunishment(T punishment, K key,
            Map<K, Map<UUID, T>> globalMap,
            Map<K, Set<T>> activeMap) {
        if (punishment.isValid()) {
            activeMap.computeIfAbsent(key, k -> new HashSet<>()).add(punishment);
        }

        globalMap.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(punishment.getId(), punishment);
    }

    public synchronized void removePlayerPunishment(PlayerPunishment punishment) {
        this.removePunishment(punishment, punishment.getPlayerId(), this.playerPunishmentMap,
                this.activePlayerPunishmentMap);
    }

    public synchronized void removeInetPunishment(InetPunishment punishment) {
        this.removePunishment(punishment, punishment.getAddress(), this.inetPunishmentMap,
                this.activeInetPunishmentMap);
    }

    private synchronized <T extends AbstractPunishment, K> void removePunishment(T punishment, K key,
            Map<K, Map<UUID, T>> globalMap,
            Map<K, Set<T>> activeMap) {
        activeMap.getOrDefault(key, Collections.emptySet()).removeIf(active -> active.getId().equals(punishment
                .getId()));
        globalMap.getOrDefault(key, Collections.emptyMap()).remove(punishment.getId());
    }

    public synchronized void updatePlayerPunishmentReferences(PlayerPunishment punishment) {
        this.updatePunishmentReferences(punishment, punishment.getPlayerId(), this.playerPunishmentMap,
                this.activePlayerPunishmentMap);
    }

    public synchronized void updateInetPunishmentReferences(InetPunishment punishment) {
        this.updatePunishmentReferences(punishment, punishment.getAddress(), this.inetPunishmentMap,
                this.activeInetPunishmentMap);
    }

    private synchronized <T extends AbstractPunishment, K> void updatePunishmentReferences(T punishment,
            K key,
            Map<K, Map<UUID, T>> globalMap,
            Map<K, Set<T>> activeMap) {
        this.removePunishment(punishment, key, globalMap, activeMap);
        this.addPunishment(punishment, key, globalMap, activeMap);
    }

    public AbstractPunishment getActivePlayerOrInetPunishment(UUID playerId, InetAddress address) {
        PlayerPunishment playerPunishment = this.getActivePlayerPunishment(playerId);
        if (playerPunishment != null)
            return playerPunishment;

        if (address != null) {
            return this.getActiveInetPunishment(address);
        }

        return null;
    }

    public Optional<PlayerPunishment> activePlayerPunishment(UUID playerId) {
        return Optional.ofNullable(this.getActivePlayerPunishment(playerId));
    }

    public PlayerPunishment getActivePlayerPunishment(UUID playerId) {
        return this.getActivePunishment(playerId, this.activePlayerPunishmentMap);
    }

    public Optional<InetPunishment> activeInetPunishment(InetAddress address) {
        return Optional.ofNullable(this.getActiveInetPunishment(address));
    }

    public InetPunishment getActiveInetPunishment(InetAddress address) {
        return this.getActivePunishment(address, this.activeInetPunishmentMap);
    }

    private <T extends AbstractPunishment, K> T getActivePunishment(K key, Map<K, Set<T>> activeMap) {
        return this.getActivePunishments(key, activeMap).stream().max(Comparator.comparingLong(
                AbstractPunishment::getCreationDate)).orElse(null);
    }

    public Set<PlayerPunishment> getActivePlayerPunishments(UUID playerId) {
        return this.getActivePunishments(playerId, this.activePlayerPunishmentMap);
    }

    public Set<InetPunishment> getActiveInetPunishments(InetAddress address) {
        return this.getActivePunishments(address, this.activeInetPunishmentMap);
    }

    public Set<PlayerPunishment> getActivePlayerPunishments() {
        return this.activePlayerPunishmentMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    public Set<InetPunishment> getActiveInetPunishments() {
        return this.activeInetPunishmentMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    private <T extends AbstractPunishment, K> Set<T> getActivePunishments(K key,
            Map<K, Set<T>> activeMap) {
        Set<T> punishments = activeMap.get(key);
        if (punishments == null)
            return Collections.emptySet();

        punishments.removeIf(Predicate.not(AbstractPunishment::isValid));

        return Set.copyOf(punishments);
    }

    public Set<PlayerPunishment> getPlayerPunishments(UUID playerId) {
        return this.getPunishments(playerId, this.playerPunishmentMap);
    }

    public Set<PlayerPunishment> getPlayerPunishments() {
        return this.playerPunishmentMap.values().stream().flatMap(map -> map.values().stream()).collect(Collectors
                .toSet());
    }

    public Set<InetPunishment> getInetPunishments(InetAddress address) {
        return this.getPunishments(address, this.inetPunishmentMap);
    }

    public Set<InetPunishment> getInetPunishments() {
        return this.inetPunishmentMap.values().stream().flatMap(map -> map.values().stream()).collect(Collectors
                .toSet());
    }

    private <T extends AbstractPunishment, K> Set<T> getPunishments(K key,
            Map<K, Map<UUID, T>> globalMap) {
        Map<UUID, T> punishments = globalMap.get(key);
        return punishments == null ? Collections.emptySet() : Set.copyOf(punishments.values());
    }
}
