package su.nightexpress.sunlight.moduleImpl.nerfphantoms;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.provider.PhantomsProvider;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.nerfphantoms.command.PhantomsCommandProvider;
import su.nightexpress.sunlight.moduleImpl.nerfphantoms.config.PhantomsConfig;
import su.nightexpress.sunlight.moduleImpl.nerfphantoms.config.PhantomsLang;
import su.nightexpress.sunlight.moduleImpl.nerfphantoms.listener.PhantomsListener;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;

public class PhantomsModule extends Module implements PhantomsProvider {

    public PhantomsModule(ModuleDefinition<PhantomsModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
    }

    @Override
    protected void loadModule(FileConfig config) {
        config.initializeOptions(PhantomsConfig.class);
        this.plugin.injectLang(PhantomsLang.class);
        UserPropertyRegistry.register(PhantomsProperties.ANTI_PHANTOM);

        this.addListener(new PhantomsListener(this.plugin, this));
        this.commandRegistry.addProvider(new PhantomsCommandProvider(this));
        this.addAsyncTask(this::resetRestTime, 600); // TODO Config
    }

    @Override
    protected void unloadModule() {

    }

    @Override
    public String getPermissionNamespace() {
        return "nerfphantoms";
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        registry.register("phantoms_antiphantom_state", (player, payload) -> {
            return CoreLang.STATE_YES_NO.get(this.userManager.getOrFetch(player).getPropertyOrDefault(
                    PhantomsProperties.ANTI_PHANTOM));
        });
    }

    @Override
    public boolean isPhantomSpawnPrevented(@NotNull Player player) {
        return this.userManager.getOrFetch(player).getPropertyOrDefault(PhantomsProperties.ANTI_PHANTOM);
    }

    @Override
    public void setPhantomSpawnPrevented(@NotNull Player player, boolean prevented) {
        this.userManager.getOrFetch(player).setProperty(PhantomsProperties.ANTI_PHANTOM, prevented);
    }

    private void resetRestTime() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            SunUser user = plugin.userManager().getOrFetch(player);
            if (!user.getPropertyOrDefault(PhantomsProperties.ANTI_PHANTOM))
                continue;

            player.setStatistic(Statistic.TIME_SINCE_REST, 0);
        }
    }
}
