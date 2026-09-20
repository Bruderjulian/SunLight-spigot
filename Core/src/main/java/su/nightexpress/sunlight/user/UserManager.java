package su.nightexpress.sunlight.user;

import org.bukkit.entity.Player;

import su.nightexpress.nightcore.user.AbstractUserManager;
import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.nightcore.user.data.DefaultUserDataAccessor;
import su.nightexpress.sunlight.SLUtils;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandKey;
import su.nightexpress.sunlight.data.DataHandler;
import su.nightexpress.sunlight.utils.Utils;

import java.net.InetAddress;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager extends AbstractUserManager<SunLightPlugin, SunUser> {

    private final DataHandler dataHandler;

    public UserManager(SunLightPlugin plugin, DataHandler dataHandler) {
        super(plugin, new DefaultUserDataAccessor<>(dataHandler, dataHandler));
        this.dataHandler = dataHandler;
    }

    @Override
    protected void onLoad() {
        super.onLoad();

        this.addListener(new UserListener(this.plugin, this));

        // TODO Placeholders
        /*
         * if (params.startsWith("command_is_on_cooldown_")) {
         * String name = params.substring("command_is_on_cooldown_".length());
         * return CoreLang.getYesOrNo(user.getCooldown(CooldownType.COMMAND,
         * name).isPresent());
         * }
         * if (params.startsWith("command_cooldown_")) {
         * String name = params.substring("command_cooldown_".length());
         * return user.getCooldown(CooldownType.COMMAND, name).map(c ->
         * Utils.formatDuration(c.getExpireDate())).orElse("-");
         * }
         */
    }

    @Override
    protected void synchronize(SunUser fetched, SunUser cached) {
        cached.updateFrom(fetched);
    }

    @Override

    protected SunUser create(UUID uuid, String name, InetAddress address) {
        long timestamp = System.currentTimeMillis();
        Map<CommandKey, Long> commandCooldowns = new ConcurrentHashMap<>();
        Map<String, Object> properties = new ConcurrentHashMap<>();

        SunUser user = new SunUser(uuid, name, timestamp, timestamp, address, commandCooldowns, properties);
        user.setFirstTimeJoined(true);

        return user;
    }

    public CompletableFuture<Player> loadTargetPlayer(String playerName) {
        return this.loadTargetProfile(playerName).thenCompose(this::loadTargetPlayer);
    }

    public CompletableFuture<Player> loadTargetPlayer(UserInfo profile) {
        return this.loadTargetPlayer(profile.id(), profile.name());
    }

    public CompletableFuture<Player> loadTargetPlayer(SunUser user) {
        return this.loadTargetPlayer(user.getId(), user.getName());
    }

    public CompletableFuture<Player> loadTargetPlayer(UUID id, String name) {
        Player target = Utils.getPlayer(id);
        if (target != null)
            return CompletableFuture.completedFuture(target);

        return CompletableFuture
                .supplyAsync(() -> this.plugin.internals().map(nms -> nms.loadPlayerData(id, name)).orElse(null));
    }

    public CompletableFuture<UserInfo> loadTargetProfile(String playerName) {
        Player target = Utils.getPlayer(playerName);
        if (target != null)
            return CompletableFuture.completedFuture(UserInfo.of(target));

        Optional<SunUser> cached = this.getRepository().getByName(playerName);
        if (cached.isPresent()) {
            SunUser user = cached.get();
            return CompletableFuture.completedFuture(new UserInfo(user.getId(), user.getName()));
        }

        return CompletableFuture.supplyAsync(() -> this.dataHandler.loadProfile(playerName).orElse(null));
    }

    public CompletableFuture<InetAddress> loadInetAddress(UUID playerId) {
        Player target = Utils.getPlayer(playerId);
        if (target != null)
            return CompletableFuture.completedFuture(SLUtils.getInetAddress(target).orElse(null));

        Optional<SunUser> cached = this.getRepository().getById(playerId);
        if (cached.isPresent())
            return CompletableFuture.completedFuture(cached.get().getLatestAddress().orElse(null));

        return CompletableFuture.supplyAsync(() -> this.dataHandler.loadInetAddress(playerId).orElse(null));
    }

}
