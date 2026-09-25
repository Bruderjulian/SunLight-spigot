package su.nightexpress.sunlight.moduleImpl.nametags.command;

import org.bukkit.entity.Player;
import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandArguments;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.command.mode.ToggleMode;
import su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule;
import su.nightexpress.sunlight.moduleImpl.nametags.config.NametagsLang;
import su.nightexpress.sunlight.moduleImpl.nametags.config.NametagsPerms;
import su.nightexpress.sunlight.moduleImpl.nametags.model.NametagTag;
import su.nightexpress.sunlight.user.UserManager;
import su.nightexpress.sunlight.utils.Utils;

import java.util.Map;

public class NametagsCommandProvider extends CommandProvider {

    private static final String COMMAND_TAGS = "tags";
    private static final String COMMAND_TAG = "tag";
    private static final String COMMAND_TAG_TOGGLE = "tagtoggle";
    private static final String COMMAND_RANK = "rank";
    private static final String COMMAND_SET = "set";
    private static final String COMMAND_RELOAD = "reload";

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
                .withArguments(Arguments.string(CommandArguments.NAME)
                    .suggestions((context, arguments) -> this.module.getTags().keySet().stream().sorted().toList()))
                .executes(this::selectTag));

        this.registerLiteral(COMMAND_RANK, true, new String[]{"nametagrank"},
            builder -> builder
                .description(NametagsLang.COMMAND_RANK_DESC)
                .permission(NametagsPerms.COMMAND_NAMETAG_TOGGLE)
                .withArguments(CommandArguments.toggleMode(CommandArguments.MODE))
                .executes(this::toggleRank));

        this.registerLiteral(COMMAND_TAG_TOGGLE, true, new String[]{"nametagtoggle", "tagtoggle"},
            builder -> builder
                .description(NametagsLang.COMMAND_TAG_TOGGLE_DESC)
                .permission(NametagsPerms.COMMAND_TAG_TOGGLE)
                .withArguments(CommandArguments.toggleMode(CommandArguments.MODE))
                .executes(this::toggleTagPart));

        this.registerLiteral(COMMAND_SET, true, new String[]{"namesettag"},
            builder -> builder
                .description(NametagsLang.COMMAND_SET_DESC)
                .permission(NametagsPerms.COMMAND_NAMETAG_SET)
                .withArguments(
                    Arguments.playerName(CommandArguments.PLAYER),
                    Arguments.string(CommandArguments.NAME)
                        .suggestions((context, arguments) -> this.module.getTags().keySet().stream().sorted().toList()))
                .executes(this::setTag));

        this.registerLiteral(COMMAND_RELOAD, true, new String[]{"nametagsreload"},
            builder -> builder
                .description(NametagsLang.COMMAND_RELOAD_DESC)
                .permission(NametagsPerms.COMMAND_RELOAD)
                .executes(this::reloadModule));

        this.registerRoot("nametag", true, new String[]{"nametag", "nt"},
            Map.of(COMMAND_TAGS, "tags", COMMAND_TAG, "tag", COMMAND_TAG_TOGGLE, "tagtoggle",
                COMMAND_RANK, "rank", COMMAND_SET, "set", COMMAND_RELOAD, "reload"),
            builder -> builder
                .description(NametagsLang.COMMAND_NAMETAG_DESC)
                .permission(NametagsPerms.COMMAND_TAGS)
                .executes(this::openTagsMenu));
    }

    private boolean openTagsMenu(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        this.module.openTagsMenu(player);
        return true;
    }

    private boolean selectTag(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        String tagId = Utils.lowercase(arguments.getString(CommandArguments.NAME));
        return this.module.selectTag(player, tagId);
    }

    private boolean toggleRank(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        ToggleMode mode = arguments.get(CommandArguments.MODE, ToggleMode.class);
        boolean enabled = mode.apply(this.module.hasRankPartEnabled(player));
        this.module.setRankEnabled(player, enabled);
        return true;
    }

    private boolean toggleTagPart(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        ToggleMode mode = arguments.get(CommandArguments.MODE, ToggleMode.class);
        boolean enabled = mode.apply(this.module.hasTagPartEnabled(player));
        this.module.setTagEnabled(player, enabled);
        return true;
    }

    private boolean setTag(CommandContext context, ParsedArguments arguments) {
        String tagId = Utils.lowercase(arguments.getString(CommandArguments.NAME));
        boolean clear = "none".equals(tagId);
        NametagTag tag = clear ? null : this.module.getTag(tagId);

        if (!clear && tag == null) {
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_ERROR_INVALID, context.getSender(),
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> tagId));
            return false;
        }

        return this.loadPlayerOrSenderWithDataAndRunInMainThread(context, arguments, this.module, this.userManager,
            (user, target) -> {
                if (!this.module.setTag(user, target, clear ? null : tag.getId())) return;

                NametagTag selected = this.module.getSelectedTag(target);
                if (selected == null) {
                    this.module.sendPrefixed(NametagsLang.COMMAND_SET_CLEAR, context.getSender(),
                        replacer -> replacer.with(SLPlaceholders.PLAYER_NAME, user::getName));
                    return;
                }

                this.module.sendPrefixed(NametagsLang.COMMAND_SET_DONE, context.getSender(),
                    replacer -> replacer
                        .with(SLPlaceholders.PLAYER_NAME, user::getName)
                        .with(SLPlaceholders.GENERIC_NAME, selected::getDisplay));
            });
    }

    private boolean reloadModule(CommandContext context, ParsedArguments arguments) {
        this.module.reload();
        this.module.sendPrefixed(NametagsLang.COMMAND_RELOAD_DONE, context.getSender());
        return true;
    }
}