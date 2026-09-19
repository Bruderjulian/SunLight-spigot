package su.nightexpress.sunlight.moduleImpl.nick;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.bridge.wrapper.NightComponent;
import su.nightexpress.sunlight.api.provider.NickProvider;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleContext;
import su.nightexpress.sunlight.moduleImpl.nick.command.NickCommandProvider;
import su.nightexpress.sunlight.moduleImpl.nick.config.NickConfig;
import su.nightexpress.sunlight.moduleImpl.nick.config.NickLang;
import su.nightexpress.sunlight.moduleImpl.nick.config.NickPerms;
import su.nightexpress.sunlight.moduleImpl.nick.event.PlayerNickChangeEvent;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;

public class NickModule extends Module implements NickProvider {

    public NickModule(ModuleContext context) {
        super(context);
    }

    @Override
    protected void loadModule(FileConfig config) {
        config.initializeOptions(NickConfig.class);
        this.migrateLegacyEssentialSettings(config);
        this.plugin.injectLang(NickLang.class);
        UserPropertyRegistry.register(NickProperties.CUSTOM_NAME);

        this.addListener(new NickListener(this.plugin, this));
    }

    @Override
    protected void unloadModule() {

    }

    @Override
    protected void registerPermissions(PermissionTree root) {
        root.merge(NickPerms.MODULE);
    }

    @Override
    protected void registerCommands() {
        this.commandRegistry.addProvider("nickname", new NickCommandProvider(this.plugin, this, this.userManager), this);
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        registry.register("nick_name", (player, payload) -> this.getNickname(player));
        registry.register("essential_custom_name", (player, payload) -> this.getNickname(player));
    }

    private void migrateLegacyEssentialSettings(FileConfig config) {
        if (config.contains("Nick.Length.Min")) return;

        FileConfig essentialConfig = FileConfig.load(this.path.getParent().resolve("essential").toString(), "settings.yml");
        if (!essentialConfig.contains("Nick.Length.Min")) return;

        config.set("Nick.Length.Min", essentialConfig.getInt("Nick.Length.Min"));
        config.set("Nick.Length.Max", essentialConfig.getInt("Nick.Length.Max"));
        config.set("Nick.Banned-Words", essentialConfig.getStringList("Nick.Banned-Words"));
        config.set("Nick.Regex-Pattern", essentialConfig.getString("Nick.Regex-Pattern"));
        config.saveChanges();

        this.info("Migrated nickname settings from the Essential module.");
    }

    @Override
    public boolean hasNickname(@NotNull Player player) {
        SunUser user = this.userManager.getOrFetch(player);
        return user.hasProperty(NickProperties.CUSTOM_NAME);
    }

    @Override
    public @NotNull String getNickname(@NotNull Player player) {
        SunUser user = this.userManager.getOrFetch(player);
        return user.getPropertyOr(NickProperties.CUSTOM_NAME, user.getName());
    }

    @Override
    public void setNickname(@NotNull Player player, @NotNull String nickname) {
        SunUser user = this.userManager.getOrFetch(player);
        this.setNickname(user, nickname);
    }

    @Override
    public void clearNickname(@NotNull Player player) {
        SunUser user = this.userManager.getOrFetch(player);
        this.setNickname(user, null);
    }

    public void setNickname(@NotNull SunUser user, @Nullable String nickname) {
        String oldNickname = user.hasProperty(NickProperties.CUSTOM_NAME)
            ? user.getPropertyOrDefault(NickProperties.CUSTOM_NAME)
            : null;

        if (nickname == null) {
            user.removeProperty(NickProperties.CUSTOM_NAME);
        } else {
            user.setProperty(NickProperties.CUSTOM_NAME, nickname);
        }
        user.markDirty();

        user.player().ifPresent(target -> {
            PlayerNickChangeEvent event = new PlayerNickChangeEvent(target, oldNickname, nickname);
            this.plugin.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;

            String effective = event.getNewNickname();
            if (effective == null) {
                user.removeProperty(NickProperties.CUSTOM_NAME);
                user.markDirty();
            } else if (!effective.equals(nickname)) {
                user.setProperty(NickProperties.CUSTOM_NAME, effective);
                user.markDirty();
            }
            this.applyNickname(target);
        });
    }

    public void applyNickname(@NotNull Player player) {
        SunUser user = this.userManager.getOrFetch(player);

        if (!player.hasPermission(NickPerms.COMMAND_NICK_CHANGE)) {
            user.removeProperty(NickProperties.CUSTOM_NAME);
            user.markDirty();
        }

        if (user.hasProperty(NickProperties.CUSTOM_NAME)) {
            String nickname = user.getPropertyOrDefault(NickProperties.CUSTOM_NAME);
            Players.setDisplayName(player, nickname);
            if (NickConfig.APPLY_TO_TABLIST.get()) {
                Players.setPlayerListName(player, nickname);
            }
        } else {
            Players.setDisplayName(player, (NightComponent) null);
            if (NickConfig.APPLY_TO_TABLIST.get()) {
                Players.setPlayerListName(player, (NightComponent) null);
            }
        }
    }
}
