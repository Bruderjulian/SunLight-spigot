package su.nightexpress.sunlight.moduleImpl.links;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.StringUtil;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.bukkit.NightSound;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.event.PlayerLinkActivateEvent;
import su.nightexpress.sunlight.api.provider.LinksProvider;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;
import su.nightexpress.sunlight.utils.EconomyUtils;
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
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkCooldownDialog;
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkCostDialog;
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkCreationDialog;
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkDeletionDialog;
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkDisplayNameDialog;
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkFeedbackDialog;
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkParticleDialog;
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkPermissionDialog;
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkPriorityDialog;
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkRewardDialog;
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkUrlDialog;
import su.nightexpress.sunlight.moduleImpl.links.dialog.impl.LinkUsePermissionDialog;
import su.nightexpress.sunlight.moduleImpl.links.menu.LinkIconSelectionMenu;
import su.nightexpress.sunlight.moduleImpl.links.menu.LinkSettingsMenu;
import su.nightexpress.sunlight.moduleImpl.links.menu.LinksEditorMenu;
import su.nightexpress.sunlight.moduleImpl.links.menu.LinksMenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LinksModule extends Module implements LinksProvider {

    public static final String PLACEHOLDER_PLAYER_UUID = "%player_uuid%";

    private final LinksSettings settings;
    private final Map<String, Link> links;
    private final Map<UUID, Map<String, Long>> cooldowns;

    private FileConfig config;

    private LinksMenu menu;
    private LinksEditorMenu editorMenu;
    private LinkSettingsMenu settingsMenu;
    private LinkIconSelectionMenu iconMenu;

    public LinksModule(ModuleDefinition<LinksModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new LinksSettings();
        // Synchronized: the async click saver iterates off the main thread while
        // commands and menus
        // mutate the map on it. Compound actions still synchronize on the map
        // explicitly.
        this.links = Collections.synchronizedMap(new LinkedHashMap<>());
        this.cooldowns = new ConcurrentHashMap<>();
    }

    @Override
    protected void loadModule(FileConfig config) {
        // Held for the module's lifetime: getConfig() hands out a fresh instance on
        // every call, so link
        // edits would otherwise be written to a throwaway copy.
        this.config = config;

        this.settings.load(config);
        this.links.clear();
        this.links.putAll(LinksConfig.readLinks(config, this::warn));

        this.plugin.injectLang(LinksLang.class);
        UserPropertyRegistry.register(LinksProperties.CLAIMED_REWARDS);

        this.registerDialogs();

        this.addListener(new LinksListener(this.plugin, this));

        this.menu = new LinksMenu(this.plugin, this);
        this.editorMenu = new LinksEditorMenu(this.plugin, this);
        this.settingsMenu = new LinkSettingsMenu(this.plugin, this);
        this.iconMenu = new LinkIconSelectionMenu(this.plugin, this);

        this.addAsyncTask(this::saveDirtyLinks, 300);

        this.commandRegistry.addProvider(new LinksCommandProvider(this));
        this.commandRegistry.addProvider(new LinksAdminCommandProvider(this));
    }

    @Override
    protected void unloadModule() {
        this.saveDirtyLinks();

        this.links.clear();
        this.cooldowns.clear();
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
    public void registerPlaceholders(PlaceholderRegistry registry) {
        // Resolved by ID on every evaluation: the menu and commands mutate the same
        // Link instances,
        // but a reload or a deletion replaces them, and a captured reference would go
        // stale.
        this.links.keySet().forEach(id -> registry.register("links_" + id, (player, payload) -> {
            Link link = this.getLink(id);
            if (link == null) {
                return "";
            }

            if (payload.isEmpty()) {
                return link.getUrl();
            }

            return switch (payload) {
                case "url" -> link.getUrl();
                case "name" -> link.getDisplay();
                case "command" -> link.getCommand();
                case "clicks" -> String.valueOf(link.getClicks());
                case "unique_clicks", "unique" -> String.valueOf(link.getUniqueClicks());
                case "cooldown" -> String.valueOf(link.getCooldown());
                case "cost" -> String.valueOf(link.getCost());
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
        this.dialogRegistry.register(LinksDialogKeys.LINK_USE_PERMISSION, () -> new LinkUsePermissionDialog(this));
        this.dialogRegistry.register(LinksDialogKeys.LINK_PRIORITY, () -> new LinkPriorityDialog(this));
        this.dialogRegistry.register(LinksDialogKeys.LINK_COOLDOWN, () -> new LinkCooldownDialog(this));
        this.dialogRegistry.register(LinksDialogKeys.LINK_COST, () -> new LinkCostDialog(this));
        this.dialogRegistry.register(LinksDialogKeys.LINK_REWARD, () -> new LinkRewardDialog(this));
        this.dialogRegistry.register(LinksDialogKeys.LINK_SOUND, () -> new LinkFeedbackDialog.Sound(this));
        this.dialogRegistry.register(LinksDialogKeys.LINK_ACTIONBAR, () -> new LinkFeedbackDialog.Actionbar(this));
        this.dialogRegistry.register(LinksDialogKeys.LINK_PARTICLE, () -> new LinkParticleDialog(this));
        this.dialogRegistry.register(LinksDialogKeys.LINK_DELETION, () -> new LinkDeletionDialog(this));
    }

    // Links

    public @NotNull LinksSettings getSettings() {
        return this.settings;
    }

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

    /**
     * Player-facing links: enabled and actionable. Misconfigured links (no URL and
     * no command) stay
     * loaded so admins can fix them in the editor, but they are hidden here until
     * repaired.
     * <p>
     * The tiebreaker is the plain ID, not the display name: displays carry
     * MiniMessage tags, so
     * comparing them would sort by formatting codes instead of visible text.
     */
    public @NotNull List<Link> getSortedLinks() {
        synchronized (this.links) {
            return this.links.values()
                    .stream()
                    .filter(Link::isEnabled)
                    .filter(Link::isActionable)
                    .sorted(Comparator.comparingInt(Link::getPriority).reversed().thenComparing(Link::getId))
                    .toList();
        }
    }

    /**
     * @return Every link, including disabled ones. For admin-facing views only.
     */
    public @NotNull List<Link> getAllLinks() {
        synchronized (this.links) {
            return this.links.values()
                    .stream()
                    .sorted(Comparator.comparingInt(Link::getPriority).reversed().thenComparing(Link::getId))
                    .toList();
        }
    }

    public @NotNull List<Link> getVisibleLinks(@NotNull CommandSender sender) {
        return this.getSortedLinks().stream().filter(link -> link.canSee(sender)).toList();
    }

    public @NotNull List<Link> getTopLinks(int limit) {
        synchronized (this.links) {
            return this.links.values().stream()
                    .sorted(Comparator.comparingLong(Link::getClicks).reversed().thenComparing(Link::getId))
                    .limit(Math.max(1, limit))
                    .toList();
        }
    }

    // Editing

    /**
     * @return The created link, or {@code null} when the ID is invalid or already
     *         taken.
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
     * Creates a link and reports the outcome to the sender, including the notice
     * that the new link's
     * own command only becomes available after a restart.
     */
    public boolean createLinkAndReport(@NotNull Player player, @NotNull String rawId) {
        String normalized = LinkDefaults.normalizeId(rawId);
        String strict = rawId.trim().toLowerCase(Locale.ROOT);
        if (normalized == null || !LinkDefaults.isValidId(strict)) {
            this.sendPrefixed(LinksLang.ADMIN_CREATE_ERROR_ID, player,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> rawId));
            return false;
        }

        String id = strict;
        if (this.isPresent(id)) {
            this.sendPrefixed(LinksLang.ADMIN_CREATE_ERROR_EXISTS, player,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> id));
            return false;
        }

        Link link = this.createLink(id);
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
        // Snapshot under lock: the async saver iterates off the main thread while
        // commands and menus
        // mutate the map on it. saveLink() also clears the dirty flag, so the snapshot
        // must be taken
        // before any writes happen.
        List<Link> dirty;
        synchronized (this.links) {
            dirty = this.links.values().stream().filter(Link::isDirty).toList();
        }
        dirty.forEach(this::saveLink);
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
     * The single activation path shared by {@code /links <id>}, the per-link
     * command and the menu item,
     * so all three can never drift apart.
     */
    public void activateLink(@NotNull Player player, @NotNull Link link) {
        if (!link.canSee(player)) {
            this.sendPrefixed(LinksLang.ERROR_NO_PERMISSION, player);
            return;
        }

        if (!link.canUse(player)) {
            this.sendPrefixed(LinksLang.ERROR_NO_USE_PERMISSION, player);
            return;
        }

        if (!link.isActionable()) {
            this.sendPrefixed(LinksLang.ERROR_NOT_ACTIONABLE, player);
            return;
        }

        // Pre-activation state for the event. Reward eligibility is tracked separately
        // via per-user
        // claims (see claimReward), so even a listener that re-enters activation cannot
        // cause a
        // double grant: the claim, not this flag, is the single source of truth for
        // rewards.
        boolean firstClick = !link.hasSeen(player.getUniqueId());

        PlayerLinkActivateEvent event = new PlayerLinkActivateEvent(player, link, firstClick);
        this.plugin.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        boolean commandsEnabled = this.settings.isExecuteCommandsEnabled();

        // A command-only link with command execution disabled does nothing at all: fail
        // before any
        // state (cooldown, cost, clicks) is touched, instead of pretending it was
        // activated.
        if (!link.hasUrl() && (!commandsEnabled || !link.hasCommand())) {
            this.sendPrefixed(LinksLang.ERROR_NOT_ACTIONABLE, player);
            return;
        }

        if (link.getCooldown() > 0 && this.settings.isCooldownsEnabled()
                && !EconomyUtils.hasCooldownBypass(player, LinksPerms.BYPASS_COOLDOWN)) {
            long cooldownLeft = this.getCooldownLeftMillis(player, link);
            if (cooldownLeft > 0L) {
                this.sendPrefixed(LinksLang.ERROR_COOLDOWN, player, replacer -> replacer
                        .with(SLPlaceholders.GENERIC_TIME,
                                () -> TimeFormats.formatAmount(cooldownLeft, TimeFormatType.LITERAL))
                        .with(link.placeholders()));
                return;
            }
        }

        double cost = link.getCost();
        boolean charge = this.settings.isCostsEnabled() && cost > 0D
                && EconomyUtils.hasCurrency()
                && !EconomyUtils.hasBypass(player, LinksPerms.BYPASS_COST);
        if (charge && !EconomyUtils.canAfford(player, cost)) {
            this.sendPrefixed(LinksLang.ERROR_COST, player, replacer -> replacer
                    .with(SLPlaceholders.GENERIC_AMOUNT, () -> EconomyUtils.format(cost))
                    .with(link.placeholders()));
            return;
        }

        // Stamped before any command runs so a command that re-enters activation (e.g.
        // dispatching
        // '/links <id>') is blocked instead of recursing.
        if (link.getCooldown() > 0 && this.settings.isCooldownsEnabled()
                && !EconomyUtils.hasCooldownBypass(player, LinksPerms.BYPASS_COOLDOWN)) {
            this.setCooldown(player, link);
        }

        if (link.hasCommand() && commandsEnabled) {
            this.executeLinkCommand(player, link);
        }

        if (charge) {
            EconomyUtils.withdraw(player, cost);
        }

        // Seen = activation history for the unique-clicks stat. Reward eligibility is
        // per-user and
        // survives stat resets (see claimReward), so resetting clicks never re-grants
        // rewards.
        link.markSeen(player.getUniqueId());

        if (commandsEnabled && link.hasFirstReward() && this.claimReward(player, link)) {
            this.sendPrefixed(LinksLang.FIRST_REWARD_NOTIFY, player, replacer -> replacer.with(link.placeholders()));
        }

        // Clicks are batched to disk by the async saver; a synchronous config write on
        // every click
        // would stall the main thread.
        link.addClick();

        this.playFeedback(player, link);
        this.sendLink(player, link);
    }

    /**
     * Grants the link's first-click reward if the player never claimed it.
     *
     * @return {@code true} when the reward was granted by this call.
     */
    private boolean claimReward(@NotNull Player player, @NotNull Link link) {
        SunUser user = this.userManager.getOrFetch(player);

        // Copy-on-write: the default instance is shared, so it must never be mutated in
        // place.
        Set<String> claimed = new HashSet<>(user.getPropertyOrDefault(LinksProperties.CLAIMED_REWARDS));
        if (!claimed.add(link.getId())) {
            return false;
        }

        user.setProperty(LinksProperties.CLAIMED_REWARDS, claimed);
        this.executeCommands(player, link, link.getFirstRewardCommands());
        return true;
    }

    public long getCooldownLeftMillis(@NotNull Player player, @NotNull Link link) {
        if (link.getCooldown() <= 0)
            return 0L;
        Map<String, Long> map = this.cooldowns.get(player.getUniqueId());
        if (map == null)
            return 0L;
        Long expires = map.get(link.getId());
        if (expires == null)
            return 0L;
        long left = expires - System.currentTimeMillis();
        if (left <= 0L) {
            map.remove(link.getId());
            return 0L;
        }
        return left;
    }

    public void setCooldown(@NotNull Player player, @NotNull Link link) {
        if (link.getCooldown() <= 0)
            return;
        this.cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(link.getId(), System.currentTimeMillis() + link.getCooldown() * 1000L);
    }

    public void clearCooldowns(@NotNull Player player) {
        this.cooldowns.remove(player.getUniqueId());
    }

    public void playFeedback(@NotNull Player player, @NotNull Link link) {
        if (link.hasSound()) {
            NightSound sound = parseSound(link.getSound());
            if (sound != null && !sound.isEmpty()) {
                try {
                    sound.play(player);
                } catch (IllegalArgumentException ignored) {
                    // Unknown sound name (e.g. a resource-pack sound the client lacks). Load-time
                    // validation already warned about non-vanilla names; never fail the activation.
                }
            }
        }

        if (link.hasActionbar()) {
            PlaceholderContext context = PlaceholderContext.builder()
                    .with(link.placeholders())
                    .with(CommonPlaceholders.PLAYER.resolver(player))
                    .with(PLACEHOLDER_PLAYER_UUID, () -> player.getUniqueId().toString())
                    .build();
            Players.sendActionBar(player, context.apply(link.getActionbar()));
        }

        if (link.hasParticle() && link.getParticleCount() > 0) {
            link.getParticle().play(player, player.getLocation().add(0D, 1D, 0D), 0.3D, 0.05D,
                    link.getParticleCount());
        }
    }

    /**
     * Parses a {@code NAME;volume;pitch} sound string. Lenient by design: volume
     * and pitch fall back
     * to {@code 1} when absent or malformed, mirroring
     * {@link NightSound#deserialize(String)}.
     * Custom (resource-pack) sound names are allowed; use
     * {@link #isVanillaSound(String)} when a
     * strict vanilla check is needed.
     *
     * @return The sound, or {@code null} when there is no name to play.
     */
    public static @Nullable NightSound parseSound(@NotNull String raw) {
        String input = raw.trim();
        if (input.isEmpty() || "none".equalsIgnoreCase(input))
            return null;

        String[] split = input.split(";", -1);
        String name = split[0].trim();
        if (name.isEmpty())
            return null;

        float volume;
        float pitch;
        try {
            volume = split.length > 1 && !split[1].isBlank() ? Float.parseFloat(split[1].trim()) : 1F;
            pitch = split.length > 2 && !split[2].isBlank() ? Float.parseFloat(split[2].trim()) : 1F;
        } catch (NumberFormatException exception) {
            return null;
        }

        return NightSound.of(name, volume, pitch);
    }

    /**
     * @return {@code true} when the value is a syntactically valid
     *         {@code NAME[;volume[;pitch]]}
     *         sound. The name itself is not checked against the vanilla registry,
     *         so custom
     *         resource-pack sounds pass.
     */
    public static boolean isValidSound(@NotNull String raw) {
        if (raw.isBlank() || "none".equalsIgnoreCase(raw.trim()))
            return true;
        return parseSound(raw) != null;
    }

    /**
     * @return {@code true} when the name is a vanilla {@link Sound}. In this API
     *         version sounds are
     *         an interface, not an enum, so the check goes through
     *         {@link Registry#SOUNDS} with the
     *         enum-style name ({@code ENTITY_PLAYER_LEVELUP}) mapped to its key
     *         ({@code entity.player.levelup}). Namespaced or dotted names are
     *         treated as custom
     *         sounds and return {@code false} without implying they are broken.
     */
    public static boolean isVanillaSound(@NotNull String name) {
        String value = name.trim();
        if (value.isEmpty() || value.contains(":") || value.contains("."))
            return false;
        NamespacedKey key = NamespacedKey.minecraft(value.toLowerCase(Locale.ROOT).replace('_', '.'));
        return Registry.SOUNDS.get(key) != null;
    }

    public static @NotNull String soundName(@NotNull String raw) {
        return raw.split(";", -1)[0].trim();
    }

    public static boolean isValidParticle(@NotNull String raw) {
        if (raw == null || raw.isBlank() || "none".equalsIgnoreCase(raw.trim()))
            return true;
        // Server-side particles are enum-only: unlike sounds, there is no custom
        // namespace.
        return StringUtil.getEnum(raw.trim().toUpperCase(Locale.ROOT), org.bukkit.Particle.class).isPresent();
    }

    @Override
    public boolean activateLink(@NotNull Player player, @NotNull String id) {
        Link link = this.getLink(id);
        if (link == null) {
            this.sendPrefixed(LinksLang.COMMAND_SYNTAX_INVALID_LINK, player,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_INPUT, () -> id));
            return false;
        }

        this.activateLink(player, link);
        return true;
    }

    private void executeLinkCommand(@NotNull Player player, @NotNull Link link) {
        this.executeCommands(player, link, List.of(link.getCommand()));
    }

    private void executeCommands(@NotNull Player player, @NotNull Link link, @NotNull List<String> rawCommands) {
        PlaceholderContext context = PlaceholderContext.builder()
                .with(link.placeholders())
                .with(CommonPlaceholders.PLAYER.resolver(player))
                .with(PLACEHOLDER_PLAYER_UUID, () -> player.getUniqueId().toString())
                .build();

        // Resolved as single commands so a value containing a line break cannot split
        // one in two.
        List<String> commands = new ArrayList<>(rawCommands.size());
        rawCommands.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(context::apply)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .forEach(commands::add);
        if (commands.isEmpty())
            return;

        if (link.getExecutor() == LinkExecutor.PLAYER) {
            Players.dispatchCommands(player, commands);
            return;
        }

        CommandSender console = this.plugin.getServer().getConsoleSender();
        this.plugin.runTask(() -> commands.forEach(command -> this.plugin.getServer()
                .dispatchCommand(console, command)));
    }

    public void sendStats(@NotNull CommandSender sender, int limit) {
        List<Link> top = this.getTopLinks(limit);
        if (top.isEmpty() || this.getTotalClicks() <= 0L) {
            this.sendPrefixed(LinksLang.STATS_EMPTY, sender);
            return;
        }

        this.sendPrefixed(LinksLang.STATS_HEADER, sender);
        top.forEach(link -> this.sendPrefixed(LinksLang.STATS_LINE, sender, replacer -> replacer
                .with(SLPlaceholders.GENERIC_NAME, link::getDisplay)
                .with(SLPlaceholders.GENERIC_VALUE, () -> String.valueOf(link.getClicks()))
                .with(SLPlaceholders.GENERIC_TOTAL, () -> String.valueOf(link.getUniqueClicks()))
                .with(link.placeholders())));
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
        synchronized (this.links) {
            return this.links.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().isEnabled())
                    .filter(entry -> entry.getValue().isActionable())
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getUrl(), (a, b) -> a,
                            LinkedHashMap::new));
        }
    }

    @Override
    public long getTotalClicks() {
        synchronized (this.links) {
            return this.links.values().stream().mapToLong(Link::getClicks).sum();
        }
    }

    @Override
    public long getClicks(@NotNull String id) {
        Link link = this.getLink(id);
        return link == null ? -1L : link.getClicks();
    }
}
