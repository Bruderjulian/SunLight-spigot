package su.nightexpress.sunlight.moduleImpl.glow.command;

import org.bukkit.entity.Player;
import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.builder.LiteralNodeBuilder;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandArguments;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.glow.GlowEffect;
import su.nightexpress.sunlight.moduleImpl.glow.GlowModule;
import su.nightexpress.sunlight.moduleImpl.glow.config.GlowLang;
import su.nightexpress.sunlight.moduleImpl.glow.config.GlowPerms;
import su.nightexpress.sunlight.user.UserManager;
import su.nightexpress.sunlight.utils.Utils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class GlowCommandProvider extends CommandProvider {

    private static final String COMMAND_SET = "set";
    private static final String COMMAND_CLEAR = "clear";
    private static final String COMMAND_LIST = "list";

    private final GlowModule module;
    private final UserManager userManager;

    public GlowCommandProvider(SunLightPlugin plugin, GlowModule module, UserManager userManager) {
        super(plugin);
        this.module = module;
        this.userManager = userManager;
    }

    @Override
    public void registerDefaults() {
        this.registerLiteral(COMMAND_SET, true, new String[]{"glow"},
            builder -> this.buildSet(builder));

        this.registerLiteral(COMMAND_CLEAR, true, new String[]{"unglow", "glowoff", "glow-clear"},
            builder -> builder
                .description(GlowLang.COMMAND_GLOW_CLEAR_DESC)
                .permission(GlowPerms.COMMAND_GLOW_CLEAR)
                .withArguments(Arguments.playerName(CommandArguments.PLAYER)
                    .permission(GlowPerms.COMMAND_GLOW_CLEAR_OTHERS).optional())
                .withFlags(CommandArguments.FLAG_SILENT)
                .executes(this::clearGlow));

        this.registerLiteral(COMMAND_LIST, true, new String[]{"glowlist", "glows"},
            builder -> builder
                .description(GlowLang.COMMAND_GLOW_LIST_DESC)
                .permission(GlowPerms.COMMAND_GLOW_LIST)
                .executes(this::listGlows));

        this.registerRoot("glow", true, new String[]{"glowmenu"},
            Map.of(COMMAND_SET, "set", COMMAND_CLEAR, "clear", COMMAND_LIST, "list"),
            builder -> builder.description(GlowLang.COMMAND_GLOW_ROOT_DESC)
                .permission(GlowPerms.COMMAND_GLOW_ROOT));
    }

    private void buildSet(LiteralNodeBuilder builder) {
        builder.description(GlowLang.COMMAND_GLOW_SET_DESC)
            .permission(GlowPerms.COMMAND_GLOW_SET)
            .withArguments(
                Arguments.string(CommandArguments.NAME)
                    .localized(CoreLang.COMMAND_ARGUMENT_NAME_NAME)
                    .suggestions((context, arguments) -> this.module.getEffects().keySet().stream().sorted().toList()),
                Arguments.playerName(CommandArguments.PLAYER)
                    .permission(GlowPerms.COMMAND_GLOW_SET_OTHERS).optional())
            .withFlags(CommandArguments.FLAG_SILENT)
            .executes(this::setGlow);
    }

    private boolean setGlow(CommandContext context, ParsedArguments arguments) {
        String effectId = Utils.lowercase(arguments.getString(CommandArguments.NAME));
        GlowEffect effect = this.module.getEffect(effectId);

        if (effect == null) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_INVALID, context.getSender(),
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> effectId));
            return false;
        }

        if (!context.hasPermission(GlowPerms.BYPASS_COLOR)
            && !context.hasPermission(GlowPerms.colorNode(effect.getId()))
            && !context.hasPermission(GlowPerms.COLOR.childrenNode("*"))) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_NO_ACCESS, context.getSender(),
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, effect::getName));
            return false;
        }

        this.loadPlayerOrSenderWithDataAndRunInMainThread(context, arguments, this.module, this.userManager,
            (user, target) -> {
                this.module.setGlow(user, target, effect.getId());

                if (context.getSender() != target) {
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_SET_TARGET, context.getSender(),
                        replacer -> replacer
                            .with(SLPlaceholders.GENERIC_NAME, effect::getName)
                            .with(SLPlaceholders.PLAYER_NAME, user::getName));
                }

                if (!context.hasFlag(CommandArguments.FLAG_SILENT)) {
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_SET_NOTIFY, target,
                        replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, effect::getName));
                }
            });

        return true;
    }

    private boolean clearGlow(CommandContext context, ParsedArguments arguments) {
        return this.loadPlayerOrSenderWithDataAndRunInMainThread(context, arguments, this.module, this.userManager,
            (user, target) -> {
                this.module.setGlow(user, target, null);

                if (context.getSender() != target) {
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_CLEAR_TARGET, context.getSender(),
                        replacer -> replacer.with(CommonPlaceholders.PLAYER.resolver(target)));
                }

                if (!context.hasFlag(CommandArguments.FLAG_SILENT)) {
                    if (context.getSender() == target) {
                        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_CLEAR_DONE, target);
                    } else {
                        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_CLEAR_NOTIFY, target);
                    }
                }
            });
    }

    private boolean listGlows(CommandContext context, ParsedArguments arguments) {
        List<GlowEffect> effects = this.module.getEffects().values().stream()
            .sorted(Comparator.comparing(GlowEffect::getId))
            .toList();

        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_LIST_TITLE, context.getSender());
        effects.forEach(effect -> this.module.sendPrefixed(GlowLang.COMMAND_GLOW_LIST_ENTRY, context.getSender(),
            replacer -> replacer
                .with(SLPlaceholders.GENERIC_NAME, effect::getName)
                .with(SLPlaceholders.GENERIC_VALUE, effect::getId)));

        return true;
    }
}
