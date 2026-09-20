package su.nightexpress.sunlight.moduleImpl.nick;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.bridge.wrapper.NightComponent;
import su.nightexpress.nightcore.util.text.NightMessage;
import su.nightexpress.nightcore.util.text.tag.TagPool;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.api.provider.NickProvider;
import su.nightexpress.sunlight.command.CommandKey;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.nick.command.NickCommandProvider;
import su.nightexpress.sunlight.moduleImpl.nick.config.NickConfig;
import su.nightexpress.sunlight.moduleImpl.nick.config.NickLang;
import su.nightexpress.sunlight.moduleImpl.nick.config.NickPerms;
import su.nightexpress.sunlight.moduleImpl.nick.event.PlayerNickChangeEvent;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

public class NickModule extends Module implements NickProvider {

    private static final String LEGACY_TABLIST_KEY = "Nick.Apply_To_Tablist";

    private static final String COOLDOWN_PROVIDER = "nickname";
    private static final String COOLDOWN_NODE = "module_change";

    public static final CommandKey CHANGE_COOLDOWN_KEY = new CommandKey(COOLDOWN_PROVIDER, COOLDOWN_NODE);

    private Pattern regexPattern;

    public NickModule(ModuleDefinition<NickModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
    }

    @Override
    protected void loadModule(FileConfig config) {
        this.migrateConfigPaths(config);
        config.initializeOptions(NickConfig.class);
        this.migrateLegacyEssentialSettings(config);
        this.compileRegexPattern();
        this.plugin.injectLang(NickLang.class);
        UserPropertyRegistry.register(NickProperties.CUSTOM_NAME);

        this.addListener(new NickListener(this.plugin, this));

        // Re-apply nicknames to players that are already online (e.g. after a plugin reload).
        Players.getOnline().forEach(this::applyNickname);
    }

    @Override
    protected void unloadModule() {
        // Reset player names so a nick does not leak after the module is disabled/reloaded.
        Players.getOnline().forEach(player -> {
            Players.setDisplayName(player, (NightComponent) null);
            if (NickConfig.APPLY_TO_TABLIST.get()) {
                Players.setPlayerListName(player, (NightComponent) null);
            }
        });
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

    /**
     * Migrates settings that changed their config path across versions.
     */
    private void migrateConfigPaths(FileConfig config) {
        if (config.contains(LEGACY_TABLIST_KEY) && !config.contains("Nick.Tablist.Enabled")) {
            config.set("Nick.Tablist.Enabled", config.getBoolean(LEGACY_TABLIST_KEY));
            config.remove(LEGACY_TABLIST_KEY);
            this.info("Migrated '" + LEGACY_TABLIST_KEY + "' to 'Nick.Tablist.Enabled'.");
        }
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

    private void compileRegexPattern() {
        try {
            this.regexPattern = Pattern.compile(NickConfig.REGEX_PATTERN.get());
        } catch (PatternSyntaxException exception) {
            this.regexPattern = null;
            this.warn("Invalid 'Nick.Regex-Pattern' setting: " + exception.getDescription()
                    + ". The regex check is disabled until the pattern is fixed.");
        }
    }

    /**
     * Validates a nickname input and computes the value to store. On rejection an
     * error message is sent to the {@code sender} and {@code null} is returned.
     *
     * @param sender    Sender that receives error messages.
     * @param authority Player whose permissions are checked for bypasses and colors;
     *                  {@code null} means a trusted sender (e.g. console) that bypasses
     *                  every restriction and may use colors.
     * @param owner     Player the nickname belongs to (excluded from the impersonation
     *                  check); may be {@code null}.
     * @param input     Raw nickname input.
     * @return The sanitized nickname to store, or {@code null} when rejected.
     */
    public @Nullable String sanitizeNickname(@NotNull CommandSender sender, @Nullable Player authority,
            @Nullable Player owner, @NotNull String input) {
        String raw = NightMessage.stripTags(input);

        boolean trusted = authority == null;
        boolean bypassLength = trusted || authority.hasPermission(NickPerms.BYPASS_NICK_LENGTH);
        boolean bypassWords = trusted || authority.hasPermission(NickPerms.BYPASS_NICK_WORDS);
        boolean bypassRegex = trusted || authority.hasPermission(NickPerms.BYPASS_NICK_REGEX);
        boolean allowColors = trusted || authority.hasPermission(NickPerms.COMMAND_NICK_COLORS);

        if (!bypassLength) {
            int length = raw.codePointCount(0, raw.length());
            if (length < NickConfig.MIN_LENGTH.get()) {
                int minLength = NickConfig.MIN_LENGTH.get();
                this.sendPrefixed(NickLang.COMMAND_NICK_CHANGE_ERROR_TOO_SHORT, sender,
                        replacer -> replacer.with(SLPlaceholders.GENERIC_AMOUNT, () -> String.valueOf(minLength)));
                return null;
            }
            if (length > NickConfig.MAX_LENGTH.get()) {
                int maxLength = NickConfig.MAX_LENGTH.get();
                this.sendPrefixed(NickLang.COMMAND_NICK_CHANGE_ERROR_TOO_LONG, sender,
                        replacer -> replacer.with(SLPlaceholders.GENERIC_AMOUNT, () -> String.valueOf(maxLength)));
                return null;
            }
        }

        if (!bypassWords) {
            if (this.containsBannedWord(raw)) {
                this.sendPrefixed(NickLang.COMMAND_NICK_CHANGE_ERROR_BAD_WORDS, sender);
                return null;
            }
            if (this.isTaken(owner, raw)) {
                this.sendPrefixed(NickLang.COMMAND_NICK_CHANGE_ERROR_TAKEN, sender);
                return null;
            }
        }

        if (!bypassRegex && this.regexPattern != null && !this.regexPattern.matcher(raw).matches()) {
            this.sendPrefixed(NickLang.COMMAND_NICK_CHANGE_ERROR_REGEX, sender);
            return null;
        }

        return allowColors ? NightMessage.stripTags(input, TagPool.ALL_COLORS_AND_STYLES) : raw;
    }

    private boolean containsBannedWord(String raw) {
        if (raw.isEmpty()) {
            return false;
        }

        String lower = raw.toLowerCase(Locale.ROOT);
        if (NickConfig.BANNED_WORDS_EXACT.get()) {
            return Stream.of(lower.split("[^\\p{L}\\p{N}_]+"))
                    .filter(word -> !word.isEmpty())
                    .anyMatch(word -> NickConfig.BANNED_WORDS.get().stream()
                            .filter(banned -> !banned.isBlank())
                            .anyMatch(banned -> word.equals(banned.toLowerCase(Locale.ROOT))));
        }

        return NickConfig.BANNED_WORDS.get().stream()
                .filter(banned -> !banned.isBlank())
                .anyMatch(banned -> lower.contains(banned.toLowerCase(Locale.ROOT)));
    }

    private boolean isTaken(@Nullable Player owner, String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        return this.plugin.getServer().getOnlinePlayers().stream()
                .filter(other -> owner == null || !owner.getUniqueId().equals(other.getUniqueId()))
                .anyMatch(other -> {
                    if (lower.equals(other.getName().toLowerCase(Locale.ROOT))) {
                        return true;
                    }
                    String nickname = this.getNickname(other);
                    if (nickname.isEmpty() || nickname.equals(other.getName())) {
                        return false;
                    }
                    return lower.equals(NightMessage.stripTags(nickname).toLowerCase(Locale.ROOT));
                });
    }

    @Override
    public boolean hasNickname(@NotNull Player player) {
        SunUser user = this.userManager.getOrFetch(player);
        return user.hasProperty(NickProperties.CUSTOM_NAME);
    }

    @Override
    public @NotNull String getNickname(@NotNull Player player) {
        SunUser user = this.userManager.getOrFetch(player);
        if (!user.hasProperty(NickProperties.CUSTOM_NAME)) {
            return user.getName();
        }

        String nickname = user.getPropertyOrDefault(NickProperties.CUSTOM_NAME);
        return nickname.isEmpty() ? user.getName() : nickname;
    }

    @Override
    public void setNickname(@NotNull Player player, @NotNull String nickname) {
        this.setNickname(this.userManager.getOrFetch(player), nickname);
    }

    @Override
    public void clearNickname(@NotNull Player player) {
        this.setNickname(this.userManager.getOrFetch(player), null);
    }

    /**
     * Applies a nickname change to a (possibly offline) user.
     *
     * @param user     The user to modify.
     * @param nickname The new nickname, or {@code null} to clear.
     * @return {@code true} if something changed and was applied, {@code false} when the
     *         change was a no-op or was cancelled by {@link PlayerNickChangeEvent}.
     */
    public boolean setNickname(@NotNull SunUser user, @Nullable String nickname) {
        String oldNickname = user.hasProperty(NickProperties.CUSTOM_NAME)
                ? user.getPropertyOrDefault(NickProperties.CUSTOM_NAME)
                : null;

        // An effective empty nickname is meaningless; treat it as a clear.
        if (nickname != null && NightMessage.stripTags(nickname).isEmpty()) {
            nickname = null;
        }

        if (Objects.equals(oldNickname, nickname)) {
            return false;
        }

        Optional<Player> online = user.player();
        if (online.isEmpty()) {
            // No online player to fire an event for; apply directly to the stored data.
            this.applyProperty(user, nickname);
            user.markDirty();
            return true;
        }

        Player player = online.get();
        PlayerNickChangeEvent event = new PlayerNickChangeEvent(player, oldNickname, nickname);
        this.plugin.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        this.applyProperty(user, event.getNewNickname());
        user.markDirty();
        this.applyNickname(player);

        return true;
    }

    private void applyProperty(@NotNull SunUser user, @Nullable String nickname) {
        if (nickname == null) {
            user.removeProperty(NickProperties.CUSTOM_NAME);
        } else {
            user.setProperty(NickProperties.CUSTOM_NAME, nickname);
        }
    }

    public void applyNickname(@NotNull Player player) {
        SunUser user = this.userManager.getOrFetch(player);

        // The stored nickname is never deleted because of a temporarily missing
        // permission (e.g. permission call lag) - it is merely hidden until restored.
        boolean visible = player.hasPermission(NickPerms.COMMAND_NICK_CHANGE);
        boolean hasNickname = visible && user.hasProperty(NickProperties.CUSTOM_NAME);

        if (hasNickname) {
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