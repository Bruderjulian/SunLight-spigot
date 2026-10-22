package su.nightexpress.sunlight.moduleImpl.nick.command;


import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.nick.NickModule;
import su.nightexpress.sunlight.moduleImpl.nick.config.NickConfig;
import su.nightexpress.sunlight.moduleImpl.nick.config.NickLang;
import su.nightexpress.sunlight.moduleImpl.nick.config.NickPerms;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.EconomyUtils;
import su.nightexpress.sunlight.utils.TimeUtil;

public class NickCommandProvider extends CommandProvider<NickModule> {

    private static final String COMMAND_CHANGE = "change";
    private static final String COMMAND_CLEAR = "clear";
    private static final String COMMAND_SET = "set";

    public NickCommandProvider(final NickModule module) {
        super(module, "nickname");
    }

    @Override
    public void setup() {
        this.register(COMMAND_CHANGE, builder -> builder
                .withFullDescription(NickLang.COMMAND_NICK_CHANGE_DESC.text())
                .withPermission(NickPerms.COMMAND_NICK_CHANGE)
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(new GreedyStringArgument(CommandArgumentConstants.NAME))
                .executes(this::changeNick))
            .under("nickname");

        this.register(COMMAND_CLEAR, builder -> builder
                .withFullDescription(NickLang.COMMAND_NICK_CLEAR_DESC.text())
                .withPermission(NickPerms.COMMAND_NICK_CLEAR)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::clearNick))
            .under("nickname");

        this.register(COMMAND_SET, builder -> builder
                .withFullDescription(NickLang.COMMAND_NICK_SET_DESC.text())
                .withPermission(NickPerms.COMMAND_NICK_SET)
                .withArguments(
                        CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                                info -> CommandArgumentConstants.onlinePlayerNames()),
                        new GreedyStringArgument(CommandArgumentConstants.NAME))
                .executes(this::setNickForPlayer))
            .under("nickname");

        this.registerRoot("nickname", builder -> builder
                .withFullDescription(NickLang.COMMAND_NICK_ROOT_DESC.text())
                .withPermission(NickPerms.COMMAND_NICK_ROOT)).aliases("nick");
    }

    private int changeNick(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }

        final Object nameObj = arguments.get(CommandArgumentConstants.NAME);
        if (!(nameObj instanceof final String nick)) {
            return 0;
        }

        final String effective = this.module.sanitizeNickname(sender, player, player, nick);
        if (effective == null) {
            return 0;
        }

        final SunUser user = this.module.userManager().getOrFetch(player);

        final double cost = NickConfig.CHANGE_COST.get();
        final boolean charge = cost > 0D && !EconomyUtils.hasBypass(player, NickPerms.BYPASS_CHANGE_COST)
                && EconomyUtils.hasCurrency();
        if (charge && !EconomyUtils.canAfford(player, cost)) {
            this.module.sendPrefixed(NickLang.COMMAND_NICK_CHANGE_ERROR_COST, player,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_AMOUNT, () -> EconomyUtils.format(cost)));
            return 0;
        }

        final int cooldown = NickConfig.CHANGE_COOLDOWN.get();
        final boolean cooldownActive = cooldown != 0
                && !EconomyUtils.hasCooldownBypass(player, NickPerms.BYPASS_CHANGE_COOLDOWN);
        if (cooldownActive) {
            final Long expireDate = user.getCommandCooldown(NickModule.CHANGE_COOLDOWN_KEY);
            if (expireDate != null && !TimeUtil.isPassed(expireDate)) {
                final MessageLocale locale = expireDate < 0 ? NickLang.COMMAND_NICK_CHANGE_ERROR_COOLDOWN_ONE_TIME
                        : NickLang.COMMAND_NICK_CHANGE_ERROR_COOLDOWN;
                this.module.sendPrefixed(locale, player,
                        replacer -> replacer.with(SLPlaceholders.GENERIC_TIME,
                                () -> TimeFormats.formatDuration(expireDate, TimeFormatType.LITERAL)));
                return 0;
            }
        }

        if (!this.module.setNickname(user, effective)) {
            return 0;
        }

        if (charge) {
            EconomyUtils.withdraw(player, cost);
        }
        if (cooldownActive) {
            user.setCommandCooldown(NickModule.CHANGE_COOLDOWN_KEY, TimeUtil.createFutureTimestamp(cooldown));
            user.markDirty();
        }

        this.module.sendPrefixed(NickLang.COMMAND_NICK_CHANGE_DONE, player,
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> effective));

        return 1;
    }

    private int clearNick(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            return 0;
        }

        return target.runAs(this.module, sender, NickPerms.COMMAND_NICK_CLEAR_OTHERS,
                (user, targetPlayer) -> {
                    this.module.setNickname(user, null);

                    if (sender != targetPlayer) {
                        this.module.sendPrefixed(NickLang.COMMAND_NICK_CLEAR_TARGET, sender,
                                replacer -> replacer.with(CommonPlaceholders.PLAYER.resolver(targetPlayer)));
                    }
                    if (!target.silent()) {
                        this.module.sendPrefixed(NickLang.COMMAND_NICK_CLEAR_NOTIFY, targetPlayer);
                    }
                });
    }

    private int setNickForPlayer(final CommandSender sender, final CommandArguments arguments) {
        final Object nameObj = arguments.get(CommandArgumentConstants.NAME);
        if (!(nameObj instanceof final String raw)) {
            return 0;
        }
        // The nickname is greedy, so a trailing silent flag ends up inside it and is stripped here.
        final ParsedNick parsed = this.parseNick(raw);

        return this.loadPlayerWithDataAndRunInMainThread(sender, arguments, (user, target) -> {
            final Player authority = sender instanceof final Player player ? player : null;

            final String effective = this.module.sanitizeNickname(sender, authority, target, parsed.nick());
            if (effective == null) {
                return;
            }

            this.module.setNickname(user, effective);

            if (sender != user.player().orElse(null)) {
                this.module.sendPrefixed(NickLang.COMMAND_NICK_SET_TARGET, sender,
                        replacer -> replacer
                                .with(SLPlaceholders.GENERIC_NAME, () -> effective)
                                .with(SLPlaceholders.PLAYER_NAME, user::getName));
            }

            if (!parsed.silent()) {
                this.module.sendPrefixed(NickLang.COMMAND_NICK_SET_NOTIFY, target,
                        replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> effective));
            }
        }) ? 1 : 0;
    }

    private record ParsedNick(String nick, boolean silent) {
    }

    private ParsedNick parseNick(final String raw) {
        final String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return new ParsedNick(trimmed, false);
        }

        final StringBuilder builder = new StringBuilder();
        boolean silent = false;
        for (final String token : trimmed.split("\\s+")) {
            if (CommandArgumentConstants.FLAG_SILENT.equalsIgnoreCase(token)
                    || CommandArgumentConstants.FLAG_SILENT_LONG.equalsIgnoreCase(token)) {
                silent = true;
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(token);
        }
        return new ParsedNick(builder.toString(), silent);
    }
}
