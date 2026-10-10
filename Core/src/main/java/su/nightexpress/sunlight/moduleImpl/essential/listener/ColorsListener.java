package su.nightexpress.sunlight.moduleImpl.essential.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.nightcore.util.ItemUtil;
import su.nightexpress.nightcore.util.text.NightMessage;
import su.nightexpress.nightcore.util.text.tag.TagPool;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialSettings;

public class ColorsListener extends AbstractListener<SunLightPlugin> {

    private final EssentialSettings settings;

    public ColorsListener(SunLightPlugin plugin, EssentialModule module) {
        super(plugin);
        this.settings = module.settings();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignsColor(SignChangeEvent event) {
        if (!this.settings.isSignColorsEnabled())
            return;

        Player player = event.getPlayer();
        if (!player.hasPermission(EssentialPerms.SIGNS_COLOR))
            return;

        for (int index = 0; index < event.getLines().length; index++) {
            String line = event.getLine(index);
            if (line != null) {
                event.setLine(index, NightMessage.from(line, TagPool.BASE_COLORS_AND_STYLES).toLegacy());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnvilColor(PrepareAnvilEvent event) {
        if (!this.settings.isAnvilColorsEnabled())
            return;
        if (event.getViewers().isEmpty())
            return;

        ItemStack result = event.getResult();
        if (result == null || result.getType().isAir())
            return;

        Player player = (Player) event.getViewers().getFirst();
        if (!player.hasPermission(EssentialPerms.ANVILS_COLOR))
            return;

        ItemUtil.editMeta(result, meta -> {
            meta.setDisplayName(NightMessage.from(meta.getDisplayName(), TagPool.BASE_COLORS_AND_STYLES).toLegacy());
        });
        event.setResult(result);
    }
}