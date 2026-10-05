package su.nightexpress.sunlight.moduleImpl.profiles;

import java.io.File;
import java.util.List;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.provider.ProfilesProvider;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.profiles.command.ProfilesCommandProvider;
import su.nightexpress.sunlight.moduleImpl.profiles.config.ProfilesLang;
import su.nightexpress.sunlight.moduleImpl.profiles.listener.ProfilesListener;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserProperty;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;

public class ProfilesModule extends Module implements ProfilesProvider {

    /**
     * Active profile id. Global-only: resolved to {@code main} when absent and
     * never included in per-profile snapshots.
     */
    public static final UserProperty<String> ACTIVE_PROFILE = UserProperty.create("profile_active", String.class, "", true);

    private final ProfilesSettings settings;
    private final ProfileScopeRegistry scopes;
    private ProfileManager manager;

    public ProfilesModule(ModuleDefinition<ProfilesModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new ProfilesSettings();
        this.scopes = new ProfileScopeRegistry();
    }

    @Override
    protected void loadModule(FileConfig config) {
        this.settings.load(config);
        this.scopes.load(config);
        config.saveChanges();

        this.plugin.injectLang(ProfilesLang.class);
        UserPropertyRegistry.register(ACTIVE_PROFILE);

        File moduleFolder = new File(this.getSystemPath());
        this.manager = new ProfileManager(this.plugin, this.settings, this.scopes, moduleFolder);

        this.addListener(new ProfilesListener(this.plugin, this));
        this.commandRegistry.addProvider(new ProfilesCommandProvider(this));
    }

    @Override
    protected void unloadModule() {
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        registry.register("profile_active", (player, payload) -> {
            SunUser user = this.userManager.getOrFetch(player);
            String activeId = this.manager.getActiveId(user);
            PlayerProfile profile = this.manager.getProfiles(player.getUniqueId()).get(activeId);
            return profile == null ? activeId : profile.getName();
        });
        registry.register("profile_count", (player, payload) -> {
            return String.valueOf(this.manager.getProfiles(player.getUniqueId()).size());
        });
        registry.register("profile_max", (player, payload) -> {
            return String.valueOf(this.manager.maxSlots(player));
        });
    }

    public ProfilesSettings getSettings() {
        return this.settings;
    }

    public ProfileScopeRegistry getScopes() {
        return this.scopes;
    }

    public ProfileManager getManager() {
        return this.manager;
    }

    @Override
    public @NotNull String getActiveProfileId(@NotNull Player player) {
        return this.manager.getActiveId(this.userManager.getOrFetch(player));
    }

    @Override
    public @NotNull List<String> getProfileNames(@NotNull Player player) {
        return this.manager.profileNames(player.getUniqueId());
    }

    @Override
    public int getProfileCount(@NotNull Player player) {
        return this.manager.getProfiles(player.getUniqueId()).size();
    }
}
