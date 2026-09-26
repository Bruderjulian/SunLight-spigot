package su.nightexpress.sunlight.moduleImpl.links;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.provider.LinksProvider;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.links.command.LinksAdminCommandProvider;
import su.nightexpress.sunlight.moduleImpl.links.command.LinksCommandProvider;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksConfig;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksLang;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksPerms;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksSettings;
import su.nightexpress.sunlight.moduleImpl.links.dialog.LinksDialogKeys;
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkCommandDialog;
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkCreationDialog;
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkDeletionDialog;
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkDisplayNameDialog;
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkPermissionDialog;
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkPriorityDialog;
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkUrlDialog;
import su.nightexpress.sunlight.moduleImpl.links.menu.LinkIconSelectionMenu;
import su.nightexpress.sunlight.moduleImpl.links.menu.LinkSettingsMenu;
import su.nightexpress.sunlight.moduleImpl.links.menu.LinksEditorMenu;
import su.nightexpress.sunlight.moduleImpl.links.menu.LinksMenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LinksModule extends Module implements LinksProvider {

    public static final String PLACEHOLDER_PLAYER_UUID = "%player_uuid%";

    private final LinksSettings settings;
    private final Map<String, Link> links;

    private FileConfig config;

    private LinksMenu menu;
    private LinksEditorMenu editorMenu;
    private LinkSettingsMenu settingsMenu;
    private LinkIconSelectionMenu iconMenu;

    public LinksModule(ModuleDefinition<LinksModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new LinksSettings();
        this.links = new LinkedHashMap<>();
    }

    @Override
    protected void loadModule(FileConfig config) {
        // Held for the module's lifetime: getConfig() hands out a fresh instance on every call, so link
        // edits would otherwise be written to a throwaway copy.
        this.config = config;

        this.settings.load(config);
        this.links.clear();
        this.links.putAll(LinksConfig.readLinks(config, this::warn));

        this.plugin.injectLang(LinksLang.class);

        this.registerDialogs();

        this.menu = new LinksMenu(this.plugin, this);
        this.editorMenu = new LinksEditorMenu(this.plugin, this);
        this.settingsMenu = new LinkSettingsMenu(this.plugin, this);
        this.iconMenu = new LinkIconSelectionMenu(this.plugin, this);

        this.addAsyncTask(this::saveDirtyLinks, 300);
    }

    @Override
    protected void unloadModule() {
        this.saveDirtyLinks();

        this.links.clear();
        this.config = null;
        this.menu = null;
        this.editorMenu = null;
        this.settingsMenu = null;
        this.iconMenu = null;
    }

    @Override
    protected void registerPermissions(PermissionTree root) {
        root.merge(LinksPerms.MODULE);
    }

    @Override
    protected void registerCommands() {
        this.commandRegistry.addProvider("links-common", new LinksCommandProvider(this.plugin, this), this);
        this.commandRegistry.addProvider("links-admin", new LinksAdminCommandProvider(this.plugin, this), this);
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        this.links.forEach((id, link) -> registry.register("links_" + id, (player, payload) -> {
            if (payload.isEmpty()) {
                return link.getUrl();
            }

            return switch (payload) {
                case "url" -> link.getUrl();
                case "name" -> link.getDisplay();
                case "command" -> link.getCommand();
                case "clicks" -> String.valueOf(link.getClicks());
                case "enabled" -> String.valueOf(link.isEnabled());
                default -> null;
            };
        }));

        registry.register("links_count", (player, payload) -> String.valueOf(this.links.size()));
    }

    private void registerDialogs() {
        this.dialogRegistry.register(LinksDialogKeys.LINK_CREATION, LinkCreationDialog::new);
        this.dialogRegistry.register(LinksDialogKeys.LINK_DISPLAY_NAME, () -> new LinkDisplayNameDialog(this));
        this.dialogRegistry.register(LinksDialogKeys.LINK_URL, () -> new LinkUrlDialog(this));
        this.dialogRegistry.register(LinksDialogKeys.LINK_COMMAND, () -> new LinkCommandDialog(this));
        this.dialogRegistry.register(LinksDialogKeys.LINK_PERMISSION, () -> new LinkPermissionDialog(this));
        this.dialogRegistry.register(LinksDialogKeys.LINK_PRIORITY, () -> new LinkPriorityDialog(this));
        this.dialogRegistry.register(LinksDialogKeys.LINK_DELETION, () -> new LinkDeletionDialog(this));
    }

    // Links

    public @NotNull Map<String, Link> getLinks() {
        return Collections.unmodifiableMap(this.links);
    }

    public @NotNull Set<String> getLinkIds() {
        return Collections.unmodifiableSet(this.links.keySet());
    }

    public @Nullable Link getLink(@NotNull String id) {
        return this.links.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean isPresent(@NotNull String id) {
        return this.getLink(id) != null;
    }

    public @NotNull List<Link> getSortedLinks() {
        return this.links.values()
                .stream()
                .filter(Link::isEnabled)
                .sorted(Comparator.comparingInt(Link::getPriority).reversed().thenComparing(Link::getDisplay))
                .toList();
    }

    /**
     * @return Every link, including disabled ones. For admin-facing views only.
     */
    public @NotNull List<Link> getAllLinks() {
        return this.links.values()
                .stream()
                .sorted(Comparator.comparingInt(Link::getPriority).reversed().thenComparing(Link::getDisplay))
                .toList();
    }

    public @NotNull List<Link> getVisibleLinks(@NotNull CommandSender sender) {
        return this.getSortedLinks().stream().filter(link -> link.hasAccess(sender)).toList();
    }

    // Editing

    /**
     * @return The created link, or {@code null} when the ID is invalid or already taken.
     */
    public @Nullable Link createLink(@NotNull String rawId) {
        String id = LinkDefaults.normalizeId(rawId);
        if (id == null || !LinkDefaults.isValidId(id) || this.isPresent(id)) {
            return null;
        }

        Link link = new Link(id, id, "");
        link.setIcon(NightItem.fromType(Material.PAPER));
        link.markDirty();

        this.links.put(id, link);
        this.saveLink(link);
        return link;
    }

    /**
     * Creates a link and reports the outcome to the sender, including the notice that the new link's
     * own command only becomes available after a restart.
     */
    public boolean createLinkAndReport(@NotNull Player player, @NotNull String rawId) {
        String id = LinkDefaults.normalizeId(rawId);
        if (id == null) {
            this.sendPrefixed(LinksLang.ADMIN_CREATE_ERROR_ID, player,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> rawId));
            return false;
        }

        Link link = this.createLink(rawId);
        if (link == null) {
            this.sendPrefixed(LinksLang.ADMIN_CREATE_ERROR_EXISTS, player,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> id));
            return false;
        }

        this.sendPrefixed(LinksLang.ADMIN_CREATE_FEEDBACK, player,
                replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, link::getId));
        this.sendPrefixed(LinksLang.ADMIN_RELOAD_REQUIRED, player);
        return true;
    }

    public boolean deleteLink(@NotNull Link link) {
        if (this.links.remove(link.getId()) == null) {
            return false;
        }

        FileConfig config = this.config;
        if (config != null) {
            config.remove(LinksConfig.PATH_LINKS + "." + link.getId());
            config.saveChanges();
        }

        return true;
    }

    public void saveLink(@NotNull Link link) {
        FileConfig config = this.config;
        if (config == null) {
            return;
        }

        link.write(config, LinksConfig.PATH_LINKS + "." + link.getId());
        config.saveChanges();
        link.markClean();
    }

    private void saveDirtyLinks() {
        // Copied first: saveLink() mutates the link state that the filter reads.
        this.links.values().stream().filter(Link::isDirty).toList().forEach(this::saveLink);
    }

    // Activation

    public void openLinks(@NotNull Player player) {
        List<Link> links = this.getVisibleLinks(player);
        if (links.isEmpty()) {
            this.sendPrefixed(LinksLang.LIST_EMPTY, player);
            return;
        }

        if (!this.settings.isMenuEnabled() || this.menu == null) {
            this.sendLinkList(player, links);
            return;
        }

        this.menu.show(player);
    }

    public void sendLinkList(@NotNull CommandSender sender) {
        this.sendLinkList(sender, this.getVisibleLinks(sender));
    }

    private void sendLinkList(@NotNull CommandSender sender, @NotNull List<Link> links) {
        if (links.isEmpty()) {
            this.sendPrefixed(LinksLang.LIST_EMPTY, sender);
            return;
        }

        this.sendPrefixed(LinksLang.LIST_HEADER, sender);
        links.forEach(link -> this.sendLink(sender, link));
    }

    public void sendLink(@NotNull CommandSender sender, @NotNull Link link) {
        if (!link.hasUrl()) {
            this.sendPrefixed(LinksLang.LINK_COMMAND_ONLY, sender);
            return;
        }

        String url = link.getUrl();
        StringBuilder chunk = new StringBuilder(TagWrappers.OPEN_URL.with(url).wrap(link.getDisplay()));
        if (this.settings.isShowUrl()) {
            chunk.append(TagWrappers.DARK_GRAY.wrap(" ")).append(TagWrappers.GRAY.wrap(url));
        }

        this.sendPrefixed(LinksLang.LINK_LINE, sender,
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, chunk::toString));
    }

    /**
     * The single activation path shared by {@code /links <id>}, the per-link command and the menu item,
     * so all three can never drift apart.
     */
    public void activateLink(@NotNull Player player, @NotNull Link link) {
        if (!link.hasAccess(player)) {
            this.sendPrefixed(LinksLang.ERROR_NO_PERMISSION, player);
            return;
        }

        if (!link.isActionable()) {
            this.sendPrefixed(LinksLang.ERROR_NOT_ACTIONABLE, player);
            return;
        }

        if (this.settings.isExecuteCommandsEnabled() && link.hasCommand()) {
            this.executeLinkCommand(player, link);
        }

        link.addClick();
        this.saveLink(link);

        this.sendLink(player, link);
    }

    @Override
    public boolean activateLink(@NotNull Player player, @NotNull String id) {
        Link link = this.getLink(id);
        if (link == null) {
            this.sendPrefixed(LinksLang.ERROR_NO_PERMISSION, player,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> id));
            return false;
        }

        this.activateLink(player, link);
        return true;
    }

    private void executeLinkCommand(@NotNull Player player, @NotNull Link link) {
        PlaceholderContext context = PlaceholderContext.builder()
                .with(link.placeholders())
                .with(CommonPlaceholders.PLAYER.resolver(player))
                .with(PLACEHOLDER_PLAYER_UUID, () -> player.getUniqueId().toString())
                .build();

        // Resolved as a single command so a value containing a line break cannot split it in two.
        List<String> commands = new ArrayList<>(1);
        commands.add(context.apply(link.getCommand()));

        if (link.getExecutor() == LinkExecutor.PLAYER) {
            Players.dispatchCommands(player, commands);
            return;
        }

        CommandSender console = this.plugin.getServer().getConsoleSender();
        this.plugin.runTask(() -> commands.forEach(command -> this.plugin.getServer()
                .dispatchCommand(console, command)));
    }

    // Menus

    public void openEditor(@NotNull Player player) {
        this.editorMenu.show(player);
    }

    public void openLinkSettings(@NotNull Player player, @NotNull Link link) {
        this.settingsMenu.show(player, link);
    }

    public void openIconSelection(@NotNull Player player, @NotNull Link link) {
        this.iconMenu.show(player, link);
    }

    @Override
    public @NotNull Map<String, String> getLinkMap() {
        return this.links.entrySet()
                .stream()
                .filter(entry -> entry.getValue().isEnabled())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getUrl(), (a, b) -> a,
                        LinkedHashMap::new));
    }
}
