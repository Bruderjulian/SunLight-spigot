package su.nightexpress.sunlight.moduleImpl.nametags;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.exception.ModuleLoadException;
import su.nightexpress.sunlight.hook.HookId;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleContext;
import su.nightexpress.sunlight.moduleImpl.nametags.handler.NametagHandler;
import su.nightexpress.sunlight.moduleImpl.nametags.handler.PacketsTagHandler;
import su.nightexpress.sunlight.moduleImpl.nametags.handler.ProtocolTagHandler;
import su.nightexpress.sunlight.moduleImpl.nametags.listener.NametagsListener;
import su.nightexpress.sunlight.utils.Utils;

import java.util.Comparator;

public class NametagsModule extends Module {

    private final NametagsSettings settings;

    private NametagHandler tagHandler;

    public NametagsModule(ModuleContext context) {
        super(context);
        this.settings = new NametagsSettings();
    }

    @Override
    protected void loadModule(FileConfig config) throws ModuleLoadException {
        this.settings.load(config);

        this.loadTagHandler();

        this.addListener(new NametagsListener(this.plugin, this));
    }

    private void loadTagHandler() {
        if (Utils.isInstalled(HookId.PACKET_EVENTS)) {
            this.tagHandler = new PacketsTagHandler(this.plugin);
        } else if (Utils.isInstalled(HookId.PROTOCOL_LIB)) {
            this.tagHandler = new ProtocolTagHandler(this.plugin);
        }

        if (this.tagHandler != null) {
            this.tagHandler.setup();
            this.addAsyncTask(this::updatePlayerNameTags, this.settings.getNameTagUpdateInterval());
        }
    }

    @Override
    protected void unloadModule() {

    }

    @Override
    protected void registerPermissions(PermissionTree root) {

    }

    @Override
    protected void registerCommands() {

    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {

    }

    public NameTagFormat getPlayerNameTagFormat(Player player) {
        return this.settings.getNameTagFormatsMap().values().stream()
                .filter(entry -> entry.isRankAvailable(player))
                .max(Comparator.comparingInt(NameTagFormat::getPriority))
                .orElse(null);
    }

    public void handleJoin(PlayerJoinEvent event) {
        this.updatePlayerNameTag(event.getPlayer());
    }

    public void updatePlayerNameTag(Player player) {
        if (this.tagHandler == null)
            return;

        NameTagFormat tag = this.getPlayerNameTagFormat(player);
        if (tag == null)
            return;

        PlaceholderContext placeholderContext = PlaceholderContext.builder()
                .with(CommonPlaceholders.PLAYER.resolver(player))
                .andThen(CommonPlaceholders.forPlaceholderAPI(player))
                .build();

        this.tagHandler.sendTeamPacket(player, tag, placeholderContext);
    }

    public void updatePlayerNameTags() {
        Utils.onlinePlayers().forEach(this::updatePlayerNameTag);
    }
}
