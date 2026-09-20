package su.nightexpress.sunlight.moduleImpl.nick.command;

import org.bukkit.entity.Player;
import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandArguments;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.nick.NickModule;
import su.nightexpress.sunlight.moduleImpl.nick.config.NickConfig;
import su.nightexpress.sunlight.moduleImpl.nick.config.NickLang;
import su.nightexpress.sunlight.moduleImpl.nick.config.NickPerms;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.UserManager;
import su.nightexpress.sunlight.utils.EconomyUtils;
import su.nightexpress.sunlight.utils.TimeUtil;

import java.util.Map;

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

    private boolean changeNick(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        String nick = arguments.getString(CommandArguments.NAME);

        String effective = this.module.sanitizeNickname(context.getSender(), player, player, nick);
        if (effective == null) {
            return false;
        }

        SunUser user = this.userManager.getOrFetch(player);

        double cost = NickConfig.CHANGE_COST.get();
        boolean charge = cost > 0D && !EconomyUtils.hasBypass(player, NickPerms.BYPASS_CHANGE_COST)
                && EconomyUtils.hasCurrency();
        if (charge && !EconomyUtils.canAfford(player, cost)) {
            this.module.sendPrefixed(NickLang.COMMAND_NICK_CHANGE_ERROR_COST, player,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_AMOUNT, () -> EconomyUtils.format(cost)));
            return false;
        }

        int cooldown = NickConfig.CHANGE_COOLDOWN.get();
        boolean cooldownActive = cooldown != 0
                && !EconomyUtils.hasCooldownBypass(player, NickPerms.BYPASS_CHANGE_COOLDOWN);
        if (cooldownActive) {
            Long expireDate = user.getCommandCooldown(NickModule.CHANGE_COOLDOWN_KEY);
            if (expireDate != null && !TimeUtil.isPassed(expireDate)) {
                MessageLocale locale = expireDate < 0 ? NickLang.COMMAND_NICK_CHANGE_ERROR_COOLDOWN_ONE_TIME
                        : NickLang.COMMAND_NICK_CHANGE_ERROR_COOLDOWN;
                this.module.sendPrefixed(locale, player,
                        replacer -> replacer.with(SLPlaceholders.GENERIC_TIME,
                                () -> TimeFormats.formatDuration(expireDate, TimeFormatType.LITERAL)));
                return false;
            }
        }

        boolean applied = this.module.setNickname(user, effective);
        if (!applied) {
            return false;
        }

        if (charge) {
            EconomyUtils.withdraw(player, cost);
        }
        if (cooldownActive) {
            user.setCommandCooldown(NickModule.CHANGE_COOLDOWN_KEY, TimeUtil.createFutureTimestamp(cooldown));
            user.markDirty();
        }

        String finalNick = effective;
        this.module.sendPrefixed(NickLang.COMMAND_NICK_CHANGE_DONE, player,
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
                    Player authority = context.getPlayer();

                    String effective = this.module.sanitizeNickname(context.getSender(), authority, target, nick);
                    if (effective == null) {
                        return;
                    }

                    this.module.setNickname(user, effective);

                    if (context.getSender() != user.player().orElse(null)) {
                        String nickToUse = effective;
                        this.module.sendPrefixed(NickLang.COMMAND_NICK_SET_TARGET, context.getSender(),
                                replacer -> replacer
                                        .with(SLPlaceholders.GENERIC_NAME,
                                                () -> nickToUse)
                                        .with(SLPlaceholders.PLAYER_NAME,
                                                user::getName));
                    }

                    if (!context.hasFlag(CommandArguments.FLAG_SILENT)) {
                        String nickToUse = effective;
                        this.module.sendPrefixed(NickLang.COMMAND_NICK_SET_NOTIFY, target,
                                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME,
                                        () -> nickToUse));
                    }
                });
    }
}