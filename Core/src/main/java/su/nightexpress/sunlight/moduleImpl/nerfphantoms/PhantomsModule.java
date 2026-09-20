package su.nightexpress.sunlight.moduleImpl.nerfphantoms;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.nerfphantoms.command.PhantomsCommandProvider;
import su.nightexpress.sunlight.moduleImpl.nerfphantoms.config.PhantomsConfig;
import su.nightexpress.sunlight.moduleImpl.nerfphantoms.config.PhantomsLang;
import su.nightexpress.sunlight.moduleImpl.nerfphantoms.config.PhantomsPerms;
import su.nightexpress.sunlight.moduleImpl.nerfphantoms.listener.PhantomsListener;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;

public class PhantomsModule extends Module {

    public PhantomsModule(ModuleDefinition<PhantomsModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
    }

    @Override
    protected void loadModule(FileConfig config) {
        config.initializeOptions(PhantomsConfig.class);
        this.plugin.injectLang(PhantomsLang.class);
        UserPropertyRegistry.register(PhantomsProperties.ANTI_PHANTOM);

        this.addListener(new PhantomsListener(this.plugin, this));
        this.addAsyncTask(this::resetRestTime, 600); // TODO Config
    }

    @Override
    protected void unloadModule() {

    }

    @Override
    protected void registerPermissions(PermissionTree root) {
        root.merge(PhantomsPerms.ROOT);
    }

    protected void registerCommands() {
        this.commandRegistry.addProvider("nophantom", new PhantomsCommandProvider(this.plugin, this, this.userManager),
                this);
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        registry.register("phantoms_antiphantom_state", (player, payload) -> {
            return CoreLang.STATE_YES_NO.get(this.userManager.getOrFetch(player).getPropertyOrDefault(
                    PhantomsProperties.ANTI_PHANTOM));
        });
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
