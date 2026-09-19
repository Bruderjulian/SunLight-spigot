package su.nightexpress.sunlight.moduleImpl.nick.command;

import org.bukkit.entity.Player;
import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.regex.TimedMatcher;
import su.nightexpress.nightcore.util.text.NightMessage;
import su.nightexpress.nightcore.util.text.tag.TagPool;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandArguments;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.nick.NickModule;
import su.nightexpress.sunlight.moduleImpl.nick.config.NickConfig;
import su.nightexpress.sunlight.moduleImpl.nick.config.NickLang;
import su.nightexpress.sunlight.moduleImpl.nick.config.NickPerms;
import su.nightexpress.sunlight.user.UserManager;

import java.util.Map;
import java.util.regex.Pattern;

public class NickCommandProvider extends CommandProvider {

    private static final String COMMAND_CHANGE = "change";
    private static final String COMMAND_CLEAR = "clear";
    private static final String COMMAND_SET = "set";

    private final NickModule module;
    private final UserManager userManager;

    public NickCommandProvider(SunLightPlugin plugin, NickModule module, UserManager userManager) {
        super(plugin);
        this.module = module;
        this.userManager = userManager;
    }

    @Override
    public void registerDefaults() {
        this.registerLiteral(COMMAND_CHANGE, true, new String[] { "changenick" }, builder -> builder
                .playerOnly()
                .description(NickLang.COMMAND_NICK_CHANGE_DESC)
                .permission(NickPerms.COMMAND_NICK_CHANGE)
                .withArguments(
                        Arguments.greedyString(CommandArguments.NAME)
                                .localized(CoreLang.COMMAND_ARGUMENT_NAME_NAME))
                .executes(this::changeNick));

        this.registerLiteral(COMMAND_CLEAR, true, new String[] { "clearnick" }, builder -> builder
                .description(NickLang.COMMAND_NICK_CLEAR_DESC)
                .permission(NickPerms.COMMAND_NICK_CLEAR)
                .withArguments(Arguments.playerName(CommandArguments.PLAYER).optional()
                        .permission(NickPerms.COMMAND_NICK_CLEAR_OTHERS))
                .withFlags(CommandArguments.FLAG_SILENT)
                .executes(this::clearNick));

        this.registerLiteral(COMMAND_SET, true, new String[] { "setnick" }, builder -> builder
                .description(NickLang.COMMAND_NICK_SET_DESC)
                .permission(NickPerms.COMMAND_NICK_SET)
                .withArguments(
                        Arguments.playerName(CommandArguments.PLAYER),
                        Arguments.greedyString(CommandArguments.NAME)
                                .localized(CoreLang.COMMAND_ARGUMENT_NAME_NAME))
                .withFlags(CommandArguments.FLAG_SILENT)
                .executes(this::setNickForPlayer));

        this.registerRoot("nickname", true, new String[] { "nickname" },
                Map.of(
                        COMMAND_CHANGE, "change",
                        COMMAND_CLEAR, "clear",
                        COMMAND_SET, "set"),
                builder -> builder.description(NickLang.COMMAND_NICK_ROOT_DESC)
                        .permission(NickPerms.COMMAND_NICK_ROOT));
    }

    private static String filterColors(String name) {
        return NightMessage.stripTags(name, TagPool.ALL_COLORS_AND_STYLES);
    }

    private boolean changeNick(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();

        String nick = arguments.getString(CommandArguments.NAME);

        String raw = NightMessage.stripTags(nick);
        if (!player.hasPermission(NickPerms.BYPASS_NICK_LENGTH)) {
            if (raw.length() < NickConfig.MIN_LENGTH.get()) {
                this.module.sendPrefixed(NickLang.COMMAND_NICK_CHANGE_ERROR_TOO_SHORT, context.getSender(),
                        replacer -> replacer
                                .with(SLPlaceholders.GENERIC_AMOUNT, () -> String
                                        .valueOf(NickConfig.MIN_LENGTH.get())));
                return false;
            }
            if (raw.length() > NickConfig.MAX_LENGTH.get()) {
                this.module.sendPrefixed(NickLang.COMMAND_NICK_CHANGE_ERROR_TOO_LONG, context.getSender(),
                        replacer -> replacer
                                .with(SLPlaceholders.GENERIC_AMOUNT, () -> String
                                        .valueOf(NickConfig.MAX_LENGTH.get())));
                return false;
            }
        }

        if (!player.hasPermission(NickPerms.BYPASS_NICK_WORDS)) {
            if (NickConfig.BANNED_WORDS.get().stream()
                    .anyMatch(word -> raw.toLowerCase().contains(word.toLowerCase()))) {
                this.module.sendPrefixed(NickLang.COMMAND_NICK_CHANGE_ERROR_BAD_WORDS, context.getSender());
                return false;
            }
            if (plugin.getServer().getPlayerExact(raw) != null) {
                this.module.sendPrefixed(NickLang.COMMAND_NICK_CHANGE_ERROR_BAD_WORDS, context.getSender());
                return false;
            }
        }

        if (!player.hasPermission(NickPerms.BYPASS_NICK_REGEX)) {
            if (!TimedMatcher.create(Pattern.compile(NickConfig.REGEX_PATTERN.get()), raw).matches()) {
                this.module.sendPrefixed(NickLang.COMMAND_NICK_CHANGE_ERROR_REGEX, context.getSender());
                return false;
            }
        }

        if (player.hasPermission(NickPerms.COMMAND_NICK_COLORS)) {
            nick = filterColors(nick);
        } else {
            nick = raw;
        }

        this.module.setNickname(this.plugin.getUserManager().getOrFetch(player), nick);
        String finalNick = nick;
        this.module.sendPrefixed(NickLang.COMMAND_NICK_CHANGE_DONE, context.getSender(),
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> finalNick));

        return true;
    }

    private boolean clearNick(CommandContext context, ParsedArguments arguments) {
        return this.loadPlayerOrSenderWithDataAndRunInMainThread(context, arguments, this.module,
                this.userManager,
                (user, target) -> {
                    this.module.setNickname(user, null);

                    if (context.getSender() != target) {
                        this.module.sendPrefixed(NickLang.COMMAND_NICK_CLEAR_TARGET, context.getSender(),
                                replacer -> replacer.with(CommonPlaceholders.PLAYER
                                        .resolver(target)));
                    }
                    if (!context.hasFlag(CommandArguments.FLAG_SILENT)) {
                        this.module.sendPrefixed(NickLang.COMMAND_NICK_CLEAR_NOTIFY, target);
                    }
                });
    }

    private boolean setNickForPlayer(CommandContext context, ParsedArguments arguments) {
        return this.loadPlayerWithDataAndRunInMainThread(context, arguments, this.module, this.userManager,
                (user, target) -> {
                    String nick = arguments.getString(CommandArguments.NAME);
                    String raw = NightMessage.stripAll(nick);
                    if (context.hasPermission(NickPerms.COMMAND_NICK_COLORS)) {
                        nick = filterColors(nick);
                    } else {
                        nick = raw;
                    }

                    this.module.setNickname(user, nick);

                    if (context.getSender() != user.player().orElse(null)) {
                        String finalNick1 = nick;
                        this.module.sendPrefixed(NickLang.COMMAND_NICK_SET_TARGET, context.getSender(),
                                replacer -> replacer
                                        .with(SLPlaceholders.GENERIC_NAME,
                                                () -> finalNick1)
                                        .with(SLPlaceholders.PLAYER_NAME,
                                                user::getName));
                    }

                    if (!context.hasFlag(CommandArguments.FLAG_SILENT)) {
                        String finalNick = nick;
                        this.module.sendPrefixed(NickLang.COMMAND_NICK_SET_NOTIFY, target,
                                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME,
                                        () -> finalNick));
                    }
                });
    }
}
