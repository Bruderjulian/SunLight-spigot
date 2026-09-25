package su.nightexpress.sunlight.moduleImpl.nametags.command;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandArguments;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule;
import su.nightexpress.sunlight.moduleImpl.nametags.config.NametagsLang;
import su.nightexpress.sunlight.moduleImpl.nametags.config.NametagsPerms;
import su.nightexpress.sunlight.moduleImpl.nametags.model.NameplateVisibility;
import su.nightexpress.sunlight.moduleImpl.nametags.model.Profile;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagDefinition;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.UserManager;
import su.nightexpress.sunlight.utils.Utils;

import java.util.List;
import java.util.Map;

public class NametagsCommandProvider extends CommandProvider {

    private static final String COMMAND_TAGS = "tags";
    private static final String COMMAND_TAG = "tag";
    private static final String COMMAND_PROFILE = "profile";
    private static final String COMMAND_PROFILES = "profiles";
    private static final String COMMAND_RANK_TOGGLE = "rank";
    private static final String COMMAND_TAG_TOGGLE = "tagtoggle";
    private static final String COMMAND_TEAM_TOGGLE = "team";
    private static final String COMMAND_HIDE = "hide";
    private static final String COMMAND_SHOW = "show";
    private static final String COMMAND_SET = "set";
    private static final String COMMAND_GRANT = "grant";
    private static final String COMMAND_ADMIN = "admin";
    private static final String COMMAND_RELOAD = "reload";
    private static final String ARG_NONE = "none";

    private final NametagsModule module;
    private final UserManager userManager;

    public NametagsCommandProvider(SunLightPlugin plugin, NametagsModule module, UserManager userManager) {
        super(plugin);
        this.module = module;
        this.userManager = userManager;
    }

    @Override
    public void registerDefaults() {
        this.registerLiteral(COMMAND_TAGS, true, new String[]{"tags"},
            builder -> builder
                .description(NametagsLang.COMMAND_TAGS_DESC)
                .permission(NametagsPerms.COMMAND_TAGS)
                .executes(this::openTagsMenu));

        this.registerLiteral(COMMAND_TAG, true, new String[]{"tag"},
            builder -> builder
                .description(NametagsLang.COMMAND_TAG_DESC)
                .permission(NametagsPerms.COMMAND_TAG_SELECT)
                .withArguments(Arguments.string(CommandArguments.NAME).suggestions((reader, context) -> this.tagSuggestions()))
                .executes(this::selectTag));

        // Two literals: the bare form opens the browser, the argument form picks directly.
        this.registerLiteral(COMMAND_PROFILES, true, new String[]{"profiles"},
            builder -> builder
                .description(NametagsLang.COMMAND_PROFILES_DESC)
                .permission(NametagsPerms.COMMAND_PROFILES)
                .executes(this::openProfilesMenu));

        this.registerLiteral(COMMAND_PROFILE, true, new String[]{"profile"},
            builder -> builder
                .description(NametagsLang.COMMAND_PROFILE_DESC)
                .permission(NametagsPerms.COMMAND_PROFILE_SELECT)
                .withArguments(Arguments.string(CommandArguments.NAME)
                    .suggestions((reader, context) -> this.profileSuggestions()))
                .executes(this::selectProfile));

        this.registerLiteral(COMMAND_RANK_TOGGLE, true, new String[]{"rank"},
            builder -> builder
                .description(NametagsLang.COMMAND_RANK_DESC)
                .permission(NametagsPerms.COMMAND_NAMETAG_TOGGLE)
                .executes(this::toggleRank));

        this.registerLiteral(COMMAND_TAG_TOGGLE, true, new String[]{"tagtoggle"},
            builder -> builder
                .description(NametagsLang.COMMAND_TAG_TOGGLE_DESC)
                .permission(NametagsPerms.COMMAND_TAG_TOGGLE)
                .executes(this::toggleTag));

        this.registerLiteral(COMMAND_TEAM_TOGGLE, true, new String[]{"team"},
            builder -> builder
                .description(NametagsLang.COMMAND_TEAM_TOGGLE_DESC)
                .permission(NametagsPerms.COMMAND_TEAM_TOGGLE)
                .executes(this::toggleTeam));

        this.registerLiteral(COMMAND_HIDE, true, new String[]{"hide"},
            builder -> builder
                .description(NametagsLang.COMMAND_HIDE_DESC)
                .permission(NametagsPerms.COMMAND_HIDE)
                .executes(this::hideNameplate));

        this.registerLiteral(COMMAND_SHOW, true, new String[]{"show"},
            builder -> builder
                .description(NametagsLang.COMMAND_SHOW_DESC)
                .permission(NametagsPerms.COMMAND_HIDE)
                .executes(this::showNameplate));

        this.registerLiteral(COMMAND_SET, true, new String[]{"namesettag"},
            builder -> builder
                .description(NametagsLang.COMMAND_SET_DESC)
                .permission(NametagsPerms.COMMAND_NAMETAG_SET)
                .withArguments(
                    Arguments.playerName(CommandArguments.PLAYER),
                    Arguments.string(CommandArguments.NAME).suggestions((reader, context) -> this.tagSuggestions()))
                .executes(this::setTag));

        this.registerLiteral(COMMAND_GRANT, true, new String[]{"grant"},
            builder -> builder
                .description(NametagsLang.COMMAND_GRANT_DESC)
                .permission(NametagsPerms.COMMAND_GRANT)
                .withArguments(
                    Arguments.playerName(CommandArguments.PLAYER),
                    Arguments.string(CommandArguments.NAME).suggestions((reader, context) -> this.tagSuggestions()))
                .executes(this::grantTag));

        this.registerLiteral(COMMAND_ADMIN, true, new String[]{"admin", "edit"},
            builder -> builder
                .description(NametagsLang.COMMAND_ADMIN_DESC)
                .permission(NametagsPerms.ADMIN)
                .executes(this::openAdminMenu));

        this.registerLiteral(COMMAND_RELOAD, true, new String[]{"nametagsreload"},
            builder -> builder
                .description(NametagsLang.COMMAND_RELOAD_DESC)
                .permission(NametagsPerms.COMMAND_RELOAD)
                .executes(this::reloadModule));

        this.registerRoot("nametag", true, new String[]{"nametag", "nt"},
            Map.ofEntries(
                Map.entry(COMMAND_TAGS, "tags"),
                Map.entry(COMMAND_TAG, "tag"),
                Map.entry(COMMAND_PROFILE, "profile"),
                Map.entry(COMMAND_PROFILES, "profiles"),
                Map.entry(COMMAND_RANK_TOGGLE, "rank"),
                Map.entry(COMMAND_TAG_TOGGLE, "tagtoggle"),
                Map.entry(COMMAND_TEAM_TOGGLE, "team"),
                Map.entry(COMMAND_HIDE, "hide"),
                Map.entry(COMMAND_SHOW, "show"),
                Map.entry(COMMAND_SET, "set"),
                Map.entry(COMMAND_GRANT, "grant"),
                Map.entry(COMMAND_ADMIN, "admin"),
                Map.entry(COMMAND_RELOAD, "reload")),
            builder -> builder
                .description(NametagsLang.COMMAND_NAMETAG_DESC)
                .permission(NametagsPerms.COMMAND_TAGS)
                .executes(this::openTagsMenu));
    }

    private @NotNull List<String> tagSuggestions() {
        List<String> ids = new java.util.ArrayList<>(
                this.module.getCatalog().getTagsSorted().stream().map(TagDefinition::getId).toList());
        ids.add(ARG_NONE);
        return ids;
    }

    private @NotNull List<String> profileSuggestions() {
        List<String> ids = new java.util.ArrayList<>(this.module.getCatalog().getProfiles().stream()
                .map(profile -> profile.getId())
                .toList());
        ids.add(ARG_NONE);
        return ids;
    }

    private boolean openTagsMenu(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        this.module.openTagsMenu(player);
        return true;
    }

    private boolean openAdminMenu(CommandContext context, ParsedArguments arguments) {
        this.module.openAdminMenu(context.getPlayerOrThrow());
        return true;
    }

    private boolean selectTag(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        String raw = Utils.lowercase(arguments.getString(CommandArguments.NAME));
        boolean clear = ARG_NONE.equals(raw);

        TagDefinition tag = clear ? null : this.module.getCatalog().getTag(raw);
        if (!clear && tag == null) {
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_ERROR_INVALID, context.getSender(),
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> raw));
            return false;
        }

        if (tag != null && !this.module.getAccess().hasAccess(player, tag)) {
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_ERROR_NO_ACCESS, context.getSender(),
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, tag::getDisplay));
            return false;
        }

        if (!this.module.selectTag(player, clear ? null : raw)) {
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_ERROR_CANCELLED, context.getSender());
            return false;
        }

        if (clear) {
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_CLEARED, context.getSender());
        } else {
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_SELECTED, context.getSender(),
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, tag::getDisplay));
        }
        return true;
    }

    private boolean openProfilesMenu(CommandContext context, ParsedArguments arguments) {
        this.module.openProfilesMenu(context.getPlayerOrThrow());
        return true;
    }

    private boolean selectProfile(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        String raw = Utils.lowercase(arguments.getString(CommandArguments.NAME));
        boolean clear = ARG_NONE.equals(raw);

        if (!this.module.selectProfile(player, clear ? null : raw)) {
            this.module.sendPrefixed(NametagsLang.COMMAND_PROFILE_ERROR_INVALID, context.getSender(),
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> raw));
            return false;
        }

        if (clear) {
            this.module.sendPrefixed(NametagsLang.COMMAND_PROFILE_CLEARED, context.getSender());
            return true;
        }

        Profile profile = this.module.getCatalog().getProfile(raw);
        this.module.sendPrefixed(NametagsLang.COMMAND_PROFILE_SELECTED, context.getSender(),
            replacer -> replacer.with(SLPlaceholders.GENERIC_NAME,
                () -> profile == null ? raw : profile.getDisplay()));
        return true;
    }

    private boolean toggleRank(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        NameplateVisibility visibility = this.module.toggleRank(player);

        this.module.sendPrefixed(NametagsLang.NAMETAG_RANK_CHANGED, player,
            replacer -> replacer.with(SLPlaceholders.GENERIC_STATE, () -> NametagsLang.COMMAND_TAG_ENABLED.get(visibility.wantsRank())));
        return true;
    }

    private boolean toggleTag(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        NameplateVisibility visibility = this.module.toggleTag(player);

        this.module.sendPrefixed(NametagsLang.NAMETAG_TAG_CHANGED, player,
            replacer -> replacer.with(SLPlaceholders.GENERIC_STATE, () -> NametagsLang.COMMAND_TAG_ENABLED.get(visibility.wantsTag())));
        return true;
    }

    private boolean toggleTeam(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        NameplateVisibility visibility = this.module.toggleTeam(player);

        this.module.sendPrefixed(NametagsLang.NAMETAG_TEAM_CHANGED, player,
            replacer -> replacer.with(SLPlaceholders.GENERIC_STATE, () -> NametagsLang.COMMAND_TAG_ENABLED.get(visibility.wantsTeam())));
        return true;
    }

    private boolean hideNameplate(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        this.module.setVisibility(player, this.module.getVisibility(player).withAll(false));
        this.module.sendPrefixed(NametagsLang.NAMETAG_HIDE_CHANGED, player);
        return true;
    }

    private boolean showNameplate(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        this.module.setVisibility(player, this.module.getVisibility(player).withAll(true));
        this.module.sendPrefixed(NametagsLang.NAMETAG_SHOW_DONE, player);
        return true;
    }

    private boolean setTag(CommandContext context, ParsedArguments arguments) {
        String raw = Utils.lowercase(arguments.getString(CommandArguments.NAME));
        boolean clear = ARG_NONE.equals(raw);
        TagDefinition tag = clear ? null : this.module.getCatalog().getTag(raw);

        if (!clear && tag == null) {
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_ERROR_INVALID, context.getSender(),
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> raw));
            return false;
        }

        return this.loadPlayerOrSenderWithDataAndRunInMainThread(context, arguments, this.module, this.userManager,
            (user, target) -> {
                if (!this.module.selectTag(user, clear ? null : tag.getId())) return;

                if (clear) {
                    this.module.sendPrefixed(NametagsLang.COMMAND_SET_CLEAR, context.getSender(),
                        replacer -> replacer.with(SLPlaceholders.PLAYER_NAME, user::getName));
                    if (target.isOnline()) {
                        this.module.sendPrefixed(NametagsLang.COMMAND_TAG_CLEAR_NOTIFY, target);
                    }
                    return;
                }

                this.module.sendPrefixed(NametagsLang.COMMAND_SET_DONE, context.getSender(),
                    replacer -> replacer
                        .with(SLPlaceholders.PLAYER_NAME, user::getName)
                        .with(SLPlaceholders.GENERIC_NAME, () -> tag.getDisplay()));

                if (target.isOnline()) {
                    this.module.sendPrefixed(NametagsLang.COMMAND_TAG_SET_NOTIFY, target,
                        replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> tag.getDisplay()));
                }
            });
    }

    private boolean grantTag(CommandContext context, ParsedArguments arguments) {
        String raw = Utils.lowercase(arguments.getString(CommandArguments.NAME));
        boolean revoke = ARG_NONE.equals(raw);
        TagDefinition tag = revoke ? null : this.module.getCatalog().getTag(raw);

        if (!revoke && tag == null) {
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_ERROR_INVALID, context.getSender(),
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> raw));
            return false;
        }

        return this.loadPlayerOrSenderWithDataAndRunInMainThread(context, arguments, this.module, this.userManager,
            (user, target) -> {
                SunUser online = this.userManager.getOrFetch(target);
                boolean done = revoke
                        ? this.module.getAccess().revoke(online, raw)
                        : this.module.getAccess().grant(online, tag, System.currentTimeMillis());
                if (!done) return;

                this.module.recompute(target);

                this.module.sendPrefixed(revoke ? NametagsLang.COMMAND_GRANT_REVOKED : NametagsLang.COMMAND_GRANT_DONE,
                    context.getSender(),
                    replacer -> replacer
                        .with(SLPlaceholders.PLAYER_NAME, user::getName)
                        .with(SLPlaceholders.GENERIC_NAME, () -> revoke ? raw : tag.getDisplay()));
            });
    }

    private boolean reloadModule(CommandContext context, ParsedArguments arguments) {
        this.module.reload();
        this.module.sendPrefixed(NametagsLang.COMMAND_RELOAD_DONE, context.getSender());
        return true;
    }
}
