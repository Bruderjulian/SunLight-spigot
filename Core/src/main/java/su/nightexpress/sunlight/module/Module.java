package su.nightexpress.sunlight.module;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.function.Consumer;

import org.bukkit.command.CommandSender;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.message.LangMessage;
import su.nightexpress.nightcore.manager.AbstractManager;
import su.nightexpress.nightcore.ui.dialog.wrap.DialogRegistry;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandRegistry;
import su.nightexpress.sunlight.config.Config;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.config.Perms;
import su.nightexpress.sunlight.data.DataHandler;
import su.nightexpress.sunlight.exception.ModuleLoadException;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.user.UserManager;

public abstract class Module extends AbstractManager<SunLightPlugin> {

    private final String id;
    protected final Path path;
    protected final ModuleDefinition<?> definition;

    protected final DataHandler dataHandler;
    protected final UserManager userManager;
    protected final CommandRegistry commandRegistry;
    protected final DialogRegistry dialogRegistry;

    private final String logPrefix;

    protected Module(ModuleDefinition<?> definition, SunLightPlugin plugin) {
        super(plugin);
        this.id = definition.id();
        this.path = definition.path(plugin);
        this.definition = definition;

        this.dataHandler = plugin.dataHandler();
        this.userManager = plugin.userManager();
        this.commandRegistry = plugin.commandRegistry();
        this.dialogRegistry = plugin.dialogRegistry();

        this.logPrefix = "[" + this.definition.name() + "] ";
    }

    public void init() {
        this.initModule();

    }

    @Override
    protected final void onLoad() throws ModuleLoadException {
        FileConfig config = this.getConfig();

        this.loadModule(config);
        this.registerCommands();
        this.registerPermissions(Perms.ROOT);

        config.saveChanges();
    }

    @Override
    protected final void onShutdown() {
        this.unloadModule();
    }

    protected void initModule() {

    }

    protected abstract void loadModule(FileConfig config) throws ModuleLoadException;

    protected abstract void unloadModule();

    protected abstract void registerPermissions(PermissionTree root);

    protected abstract void registerCommands();

    public abstract void registerPlaceholders(PlaceholderRegistry registry);

    public final FileConfig getConfig() {
        return FileConfig.load(this.path.toString(), "settings.yml");
    }

    public final String getId() {
        return this.id;
    }

    public String getPermissionNamespace() {
        return this.id;
    }

    public final String getName() {
        return this.definition.name();
    }

    public final String getSystemPath() {
        return this.path.toString();
    }

    @Deprecated
    public final String getLocalPath() {
        return this.path.toString();
    }

    public final String getLocalUIPath() {
        return Paths.get(this.getLocalPath(), Config.DIR_MENU).toString();
    }

    @Deprecated
    public final String getAbsolutePath() {
        return this.getSystemPath();
        // return this.plugin.getDataFolder() + this.getLocalPath();
    }

    public final void debug(String msg) {
        this.plugin.debug(this.logPrefix + msg);
    }

    public final void info(String msg) {
        this.plugin.info(this.logPrefix + msg);
    }

    public final void warn(String msg) {
        this.plugin.warn(this.logPrefix + msg);
    }

    public final void error(String msg) {
        this.plugin.error(this.logPrefix + msg);
    }

    public LangMessage getPrefixed(MessageLocale locale) {
        return locale.withPrefix(this.definition.prefix());
    }

    public void sendPrefixed(MessageLocale locale, CommandSender sender) {
        this.getPrefixed(locale).send(sender);
    }

    public void sendPrefixed(MessageLocale locale, CommandSender sender,
            Consumer<PlaceholderContext.Builder> consumer) {
        this.getPrefixed(locale).sendWith(sender, consumer);
    }

    public void sendPrefixed(MessageLocale locale, CommandSender sender, PlaceholderContext context) {
        this.getPrefixed(locale).sendWith(sender, context);
    }

    public void sendPrefixed(MessageLocale locale, Collection<? extends CommandSender> receivers) {
        this.getPrefixed(locale).send(receivers);
    }

    public void sendPrefixed(MessageLocale locale, Collection<? extends CommandSender> receivers,
            Consumer<PlaceholderContext.Builder> consumer) {
        this.getPrefixed(locale).sendWith(receivers, consumer);
    }

    public void sendPrefixed(MessageLocale locale, Collection<? extends CommandSender> receivers,
            PlaceholderContext context) {
        this.getPrefixed(locale).sendWith(receivers, context);
    }

    public void broadcastPrefixed(MessageLocale locale) {
        this.getPrefixed(locale).broadcast();
    }

    public void broadcastPrefixed(MessageLocale locale, Consumer<PlaceholderContext.Builder> consumer) {
        this.getPrefixed(locale).broadcastWith(consumer);
    }

    public void broadcastPrefixed(MessageLocale locale, PlaceholderContext context) {
        this.getPrefixed(locale).broadcastWith(context);
    }
}
