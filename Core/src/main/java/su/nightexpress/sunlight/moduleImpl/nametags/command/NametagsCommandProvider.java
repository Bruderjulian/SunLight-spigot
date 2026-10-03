package su.nightexpress.sunlight.moduleImpl.nametags.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule;
import su.nightexpress.sunlight.moduleImpl.nametags.config.NametagsLang;
import su.nightexpress.sunlight.moduleImpl.nametags.config.NametagsPerms;
import su.nightexpress.sunlight.moduleImpl.nametags.model.NameplateVisibility;
import su.nightexpress.sunlight.moduleImpl.nametags.model.Profile;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagDefinition;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.Utils;

public class NametagsCommandProvider extends CommandProvider<NametagsModule> {

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

    public NametagsCommandProvider(final NametagsModule module) {
        super(module, "nametags");
    }

    @Override
    public void setup() {
        this.register(COMMAND_TAGS, List.of(), builder -> builder
                .withFullDescription(NametagsLang.COMMAND_TAGS_DESC.text())
                .withPermission(NametagsPerms.COMMAND_TAGS.getName())
                .withRequirement(sender -> sender instanceof Player)
                .executes(this::openTagsMenu));

        this.register(COMMAND_TAG, List.of(), builder -> builder
                .withFullDescription(NametagsLang.COMMAND_TAG_DESC.text())
                .withPermission(NametagsPerms.COMMAND_TAG_SELECT.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.NAME,
                        info -> this.tagSuggestions()))
                .executes(this::selectTag));

        this.register(COMMAND_PROFILES, List.of(), builder -> builder
                .withFullDescription(NametagsLang.COMMAND_PROFILES_DESC.text())
                .withPermission(NametagsPerms.COMMAND_PROFILES.getName())
                .withRequirement(sender -> sender instanceof Player)
                .executes(this::openProfilesMenu));

        this.register(COMMAND_PROFILE, List.of(), builder -> builder
                .withFullDescription(NametagsLang.COMMAND_PROFILE_DESC.text())
                .withPermission(NametagsPerms.COMMAND_PROFILE_SELECT.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.NAME,
                        info -> this.profileSuggestions()))
                .executes(this::selectProfile));

        this.register(COMMAND_RANK_TOGGLE, List.of(), builder -> builder
                .withFullDescription(NametagsLang.COMMAND_RANK_DESC.text())
                .withPermission(NametagsPerms.COMMAND_NAMETAG_TOGGLE.getName())
                .withRequirement(sender -> sender instanceof Player)
                .executes(this::toggleRank));

        this.register(COMMAND_TAG_TOGGLE, List.of(), builder -> builder
                .withFullDescription(NametagsLang.COMMAND_TAG_TOGGLE_DESC.text())
                .withPermission(NametagsPerms.COMMAND_TAG_TOGGLE.getName())
                .withRequirement(sender -> sender instanceof Player)
                .executes(this::toggleTag));

        this.register(COMMAND_TEAM_TOGGLE, List.of(), builder -> builder
                .withFullDescription(NametagsLang.COMMAND_TEAM_TOGGLE_DESC.text())
                .withPermission(NametagsPerms.COMMAND_TEAM_TOGGLE.getName())
                .withRequirement(sender -> sender instanceof Player)
                .executes(this::toggleTeam));

        this.register(COMMAND_HIDE, List.of(), builder -> builder
                .withFullDescription(NametagsLang.COMMAND_HIDE_DESC.text())
                .withPermission(NametagsPerms.COMMAND_HIDE.getName())
                .withRequirement(sender -> sender instanceof Player)
                .executes(this::hideNameplate));

        this.register(COMMAND_SHOW, List.of(), builder -> builder
                .withFullDescription(NametagsLang.COMMAND_SHOW_DESC.text())
                .withPermission(NametagsPerms.COMMAND_HIDE.getName())
                .withRequirement(sender -> sender instanceof Player)
                .executes(this::showNameplate));

        this.register(COMMAND_SET, List.of(), builder -> builder
                .withFullDescription(NametagsLang.COMMAND_SET_DESC.text())
                .withPermission(NametagsPerms.COMMAND_NAMETAG_SET.getName())
                .withArguments(
                        CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                                info -> CommandArgumentConstants.onlinePlayerNames()),
                        CommandArgumentConstants.string(CommandArgumentConstants.NAME,
                                info -> this.tagSuggestions()))
                .executes(this::setTag));

        this.register(COMMAND_GRANT, List.of(), builder -> builder
                .withFullDescription(NametagsLang.COMMAND_GRANT_DESC.text())
                .withPermission(NametagsPerms.COMMAND_GRANT.getName())
                .withArguments(
                        CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                                info -> CommandArgumentConstants.onlinePlayerNames()),
                        CommandArgumentConstants.string(CommandArgumentConstants.NAME,
                                info -> this.tagSuggestions()))
                .executes(this::grantTag));

        this.register(COMMAND_ADMIN, List.of(), builder -> builder
                .withFullDescription(NametagsLang.COMMAND_ADMIN_DESC.text())
                .withPermission(NametagsPerms.ADMIN.getName())
                .withRequirement(sender -> sender instanceof Player)
                .executes(this::openAdminMenu));

        this.register(COMMAND_RELOAD, List.of(), builder -> builder
                .withFullDescription(NametagsLang.COMMAND_RELOAD_DESC.text())
                .withPermission(NametagsPerms.COMMAND_RELOAD.getName())
                .executes(this::reloadModule));

        this.registerRoot("nametag", builder -> builder
                .withFullDescription(NametagsLang.COMMAND_NAMETAG_DESC.text())
                .withPermission(NametagsPerms.COMMAND_TAGS.getName())
                .executes(this::openTagsMenu));
    }

    private @NotNull List<String> tagSuggestions() {
        List<String> ids = new ArrayList<>(
                this.module.getCatalog().getTagsSorted().stream().map(TagDefinition::getId).toList());
        ids.add(ARG_NONE);
        return ids;
    }

    private @NotNull List<String> profileSuggestions() {
        List<String> ids = new ArrayList<>(this.module.getCatalog().getProfiles().stream()
                .map(Profile::getId)
                .toList());
        ids.add(ARG_NONE);
        return ids;
    }

    private int openTagsMenu(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }
        this.module.openTagsMenu(player);
        return 1;
    }

    private int openAdminMenu(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }
        this.module.openAdminMenu(player);
        return 1;
    }

    private int selectTag(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }

        String raw = this.readName(arguments);
        boolean clear = ARG_NONE.equals(raw);

        TagDefinition tag = clear ? null : this.module.getCatalog().getTag(raw);
        if (!clear && tag == null) {
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_ERROR_INVALID, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> raw));
            return 0;
        }

        if (tag != null && !this.module.getAccess().hasAccess(player, tag)) {
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_ERROR_NO_ACCESS, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, tag::getDisplay));
            return 0;
        }

        if (!this.module.selectTag(player, clear ? null : raw)) {
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_ERROR_CANCELLED, sender);
            return 0;
        }

        if (clear) {
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_CLEARED, sender);
        } else {
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_SELECTED, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, tag::getDisplay));
        }
        return 1;
    }

    private int openProfilesMenu(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }
        this.module.openProfilesMenu(player);
        return 1;
    }

    private int selectProfile(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }

        String raw = this.readName(arguments);
        boolean clear = ARG_NONE.equals(raw);

        if (!this.module.selectProfile(player, clear ? null : raw)) {
            this.module.sendPrefixed(NametagsLang.COMMAND_PROFILE_ERROR_INVALID, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> raw));
            return 0;
        }

        if (clear) {
            this.module.sendPrefixed(NametagsLang.COMMAND_PROFILE_CLEARED, sender);
            return 1;
        }

        Profile profile = this.module.getCatalog().getProfile(raw);
        this.module.sendPrefixed(NametagsLang.COMMAND_PROFILE_SELECTED, sender,
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME,
                        () -> profile == null ? raw : profile.getDisplay()));
        return 1;
    }

    private int toggleRank(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }

        NameplateVisibility visibility = this.module.toggleRank(player);
        this.module.sendPrefixed(NametagsLang.NAMETAG_RANK_CHANGED, player,
                replacer -> replacer.with(SLPlaceholders.GENERIC_STATE,
                        () -> NametagsLang.COMMAND_TAG_ENABLED.get(visibility.wantsRank())));
        return 1;
    }

    private int toggleTag(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }

        NameplateVisibility visibility = this.module.toggleTag(player);
        this.module.sendPrefixed(NametagsLang.NAMETAG_TAG_CHANGED, player,
                replacer -> replacer.with(SLPlaceholders.GENERIC_STATE,
                        () -> NametagsLang.COMMAND_TAG_ENABLED.get(visibility.wantsTag())));
        return 1;
    }

    private int toggleTeam(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }

        NameplateVisibility visibility = this.module.toggleTeam(player);
        this.module.sendPrefixed(NametagsLang.NAMETAG_TEAM_CHANGED, player,
                replacer -> replacer.with(SLPlaceholders.GENERIC_STATE,
                        () -> NametagsLang.COMMAND_TAG_ENABLED.get(visibility.wantsTeam())));
        return 1;
    }

    private int hideNameplate(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }

        this.module.setVisibility(player, this.module.getVisibility(player).withAll(false));
        this.module.sendPrefixed(NametagsLang.NAMETAG_HIDE_CHANGED, player);
        return 1;
    }

    private int showNameplate(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }

        this.module.setVisibility(player, this.module.getVisibility(player).withAll(true));
        this.module.sendPrefixed(NametagsLang.NAMETAG_SHOW_DONE, player);
        return 1;
    }

    private int setTag(final CommandSender sender, final CommandArguments arguments) {
        String raw = this.readName(arguments);
        boolean clear = ARG_NONE.equals(raw);
        TagDefinition tag = clear ? null : this.module.getCatalog().getTag(raw);

        if (!clear && tag == null) {
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_ERROR_INVALID, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> raw));
            return 0;
        }

        return this.loadPlayerWithDataAndRunInMainThread(sender, arguments, (user, target) -> {
            if (!this.module.selectTag(user, clear ? null : tag.getId())) {
                return;
            }

            if (clear) {
                this.module.sendPrefixed(NametagsLang.COMMAND_SET_CLEAR, sender,
                        replacer -> replacer.with(SLPlaceholders.PLAYER_NAME, user::getName));
                if (target.isOnline()) {
                    this.module.sendPrefixed(NametagsLang.COMMAND_TAG_CLEAR_NOTIFY, target);
                }
                return;
            }

            this.module.sendPrefixed(NametagsLang.COMMAND_SET_DONE, sender,
                    replacer -> replacer
                            .with(SLPlaceholders.PLAYER_NAME, user::getName)
                            .with(SLPlaceholders.GENERIC_NAME, () -> tag.getDisplay()));

            if (target.isOnline()) {
                this.module.sendPrefixed(NametagsLang.COMMAND_TAG_SET_NOTIFY, target,
                        replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> tag.getDisplay()));
            }
        }) ? 1 : 0;
    }

    private int grantTag(final CommandSender sender, final CommandArguments arguments) {
        String raw = this.readName(arguments);
        boolean revoke = ARG_NONE.equals(raw);
        TagDefinition tag = revoke ? null : this.module.getCatalog().getTag(raw);

        if (!revoke && tag == null) {
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_ERROR_INVALID, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> raw));
            return 0;
        }

        return this.loadPlayerWithDataAndRunInMainThread(sender, arguments, (user, target) -> {
            SunUser online = this.module.userManager().getOrFetch(target);
            boolean done = revoke
                    ? this.module.getAccess().revoke(online, raw)
                    : this.module.getAccess().grant(online, tag, System.currentTimeMillis());
            if (!done) {
                return;
            }

            this.module.recompute(target);

            this.module.sendPrefixed(revoke ? NametagsLang.COMMAND_GRANT_REVOKED : NametagsLang.COMMAND_GRANT_DONE,
                    sender,
                    replacer -> replacer
                            .with(SLPlaceholders.PLAYER_NAME, user::getName)
                            .with(SLPlaceholders.GENERIC_NAME, () -> revoke ? raw : tag.getDisplay()));
        }) ? 1 : 0;
    }

    private int reloadModule(final CommandSender sender, final CommandArguments arguments) {
        this.module.reload();
        this.module.sendPrefixed(NametagsLang.COMMAND_RELOAD_DONE, sender);
        return 1;
    }

    private String readName(final CommandArguments arguments) {
        final Object nameObj = arguments.get(CommandArgumentConstants.NAME);
        return nameObj instanceof final String value ? Utils.lowercase(value) : "";
    }
}
