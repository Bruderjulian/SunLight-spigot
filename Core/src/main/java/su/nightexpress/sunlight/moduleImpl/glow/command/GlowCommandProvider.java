package su.nightexpress.sunlight.moduleImpl.glow.command;

import net.kyori.adventure.text.format.NamedTextColor;
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
import su.nightexpress.sunlight.moduleImpl.glow.GlowHandler;
import su.nightexpress.sunlight.moduleImpl.glow.GlowModule;
import su.nightexpress.sunlight.moduleImpl.glow.GlowPhase;
import su.nightexpress.sunlight.moduleImpl.glow.config.GlowLang;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.UserManager;
import su.nightexpress.sunlight.utils.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GlowCommandProvider extends CommandProvider {

    private static final List<String> VANILLA_COLORS = List.of(
            "white", "gray", "dark_gray", "black", "red", "dark_red", "gold", "yellow",
            "green", "dark_green", "aqua", "dark_aqua", "blue", "dark_blue",
            "light_purple", "dark_purple");

    private static final List<String> PHASE_ACTIONS = List.of("create", "add", "remove", "duration", "list", "clear");
    private static final List<String> PRESET_ACTIONS = List.of("save", "load", "delete", "list");

    private final GlowModule module;
    private final UserManager userManager;

    public GlowCommandProvider(final SunLightPlugin plugin, final GlowModule module, final UserManager userManager) {
        super(plugin);
        this.module = module;
        this.userManager = userManager;
    }

    @Override
    public void registerDefaults() {
        this.registerLiteral("color", true, new String[] { "" },
                builder -> builder
                        .description(GlowLang.COMMAND_GLOW_COLOR_DESC)
                        .permission("sunlight.glow.command.glow.color")
                        .withArguments(
                                Arguments.string(CommandArguments.NAME)
                                        .localized(CoreLang.COMMAND_ARGUMENT_NAME_NAME)
                                        .suggestions((context, arguments) -> this.colorSuggestions()),
                                Arguments.playerName(CommandArguments.PLAYER)
                                        .permission("sunlight.glow.command.glow.color.others").optional())
                        .withFlags(CommandArguments.FLAG_SILENT)
                        .executes(this::setGlow));

        this.registerLiteral("set", true, new String[] { "" },
                builder -> this.buildSet(builder));

        this.registerLiteral("clear", true, new String[] { "unglow" },
                builder -> builder
                        .description(GlowLang.COMMAND_GLOW_CLEAR_DESC)
                        .permission("sunlight.glow.command.glow.clear")
                        .withArguments(Arguments.playerName(CommandArguments.PLAYER)
                                .permission("sunlight.glow.command.glow.clear.others").optional())
                        .withFlags(CommandArguments.FLAG_SILENT)
                        .executes(this::clearGlow));

        this.registerLiteral("list", true, new String[] { "glowlist" },
                builder -> builder
                        .description(GlowLang.COMMAND_GLOW_LIST_DESC)
                        .permission("sunlight.glow.command.glow.list")
                        .executes(this::listGlows));

        this.registerLiteral("phase", true, new String[] { "" },
                builder -> builder
                        .playerOnly()
                        .description(GlowLang.COMMAND_GLOW_PHASE_DESC)
                        .permission("sunlight.glow.command.glow.phase")
                        .withArguments(
                                Arguments.string("action")
                                        .suggestions((context, arguments) -> PHASE_ACTIONS),
                                Arguments.greedyString("data")
                                        .suggestions((context, arguments) -> new ArrayList<>(VANILLA_COLORS))
                                        .optional())
                        .executes(this::phase));

        this.registerLiteral("preset", true, new String[] { "" },
                builder -> builder
                        .playerOnly()
                        .description(GlowLang.COMMAND_GLOW_PRESET_DESC)
                        .permission("sunlight.glow.command.glow.preset")
                        .withArguments(
                                Arguments.string("action")
                                        .suggestions((context, arguments) -> PRESET_ACTIONS),
                                Arguments.string("name")
                                        .suggestions((reader, context) -> this.presetSuggestions(context)).optional())
                        .executes(this::preset));

        this.registerLiteral("on", true, new String[] { "glow-on" },
                builder -> builder
                        .description(GlowLang.COMMAND_GLOW_ON_DESC)
                        .permission("sunlight.glow.command.glow.on")
                        .withArguments(Arguments.playerName(CommandArguments.PLAYER)
                                .permission("sunlight.glow.command.glow.on.others").optional())
                        .withFlags(CommandArguments.FLAG_SILENT)
                        .executes(this::glowOn));

        this.registerLiteral("off", true, new String[] { "glow-off" },
                builder -> builder
                        .description(GlowLang.COMMAND_GLOW_OFF_DESC)
                        .permission("sunlight.glow.command.glow.off")
                        .withArguments(Arguments.playerName(CommandArguments.PLAYER)
                                .permission("sunlight.glow.command.glow.off.others").optional())
                        .withFlags(CommandArguments.FLAG_SILENT)
                        .executes(this::glowOff));

        this.registerRoot("glow", true, new String[] { "" },
                Map.of(
                        "color", "color",
                        "list", "list",
                        "phase", "phase",
                        "preset", "preset",
                        "on", "on",
                        "off", "off",
                        "set", "set",
                        "clear", "clear"),
                builder -> builder.description(GlowLang.COMMAND_GLOW_ROOT_DESC)
                        .permission("sunlight.glow.command.glow.root")
                        .executes(this::openMenu));
    }

    private void buildSet(final LiteralNodeBuilder builder) {
        builder.description(GlowLang.COMMAND_GLOW_SET_DESC)
                .permission("sunlight.glow.command.glow.set")
                .withArguments(
                        Arguments.string(CommandArguments.NAME)
                                .localized(CoreLang.COMMAND_ARGUMENT_NAME_NAME)
                                .suggestions((context, arguments) -> this.colorSuggestions()),
                        Arguments.playerName(CommandArguments.PLAYER)
                                .permission("sunlight.glow.command.glow.set.others").optional())
                .withFlags(CommandArguments.FLAG_SILENT)
                .executes(this::setGlow);
    }

    private List<String> colorSuggestions() {
        final Set<String> suggestions = new LinkedHashSet<>(this.module.handler().getEffects().keySet());
        suggestions.addAll(VANILLA_COLORS);
        return suggestions.stream().sorted().toList();
    }

    private List<String> presetSuggestions(final CommandContext context) {
        final Player player = context.getPlayer();
        if (player == null)
            return List.of();
        final SunUser user = this.userManager.getOrFetch(player);
        return new ArrayList<>(this.module.handler().getPresets(user).keySet().stream().sorted().toList());
    }

    private boolean openMenu(final CommandContext context, final ParsedArguments arguments) {
        if (!context.isPlayer()) {
            context.printUsage();
            return false;
        }
        this.module.openGlowMenu(context.getPlayerOrThrow());
        return true;
    }

    private boolean setGlow(final CommandContext context, final ParsedArguments arguments) {
        final String effectId = Utils.lowercase(arguments.getString(CommandArguments.NAME));
        final GlowEffect effect = this.module.handler().getEffect(effectId);
        final NamedTextColor singleColor = effect == null ? GlowEffect.parseColor(effectId) : null;

        if (effect == null && singleColor == null) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_INVALID, context.getSender(),
                    replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> effectId));
            return false;
        }

        final String accessId = effect == null ? Utils.lowercase(singleColor.toString()) : effect.getId();
        final String accessName = effect == null ? singleColor.toString().toLowerCase() : effect.getName();
        if (!context.hasPermission("sunlight.glow.bypass.color")
                && !context.hasPermission("sunlight.glow.color." + accessId)
                && !context.hasPermission("sunlight.glow.color.*")) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_NO_ACCESS, context.getSender(),
                    replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> accessName));
            return false;
        }

        final GlowEffect target = effect;
        final NamedTextColor color = singleColor;
        return this.loadPlayerOrSenderWithDataAndRunInMainThread(context, arguments, this.module, this.userManager,
                (user, targetPlayer) -> {
                    if (target != null) {
                        this.module.handler().setGlow(user, targetPlayer, target.getId());
                    } else {
                        this.module.handler().setSingleColorGlow(user, targetPlayer, color);
                    }

                    final String display = target != null ? target.getName() : color.toString().toLowerCase();
                    if (context.getSender() != targetPlayer) {
                        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_SET_TARGET, context.getSender(),
                                replacer -> replacer
                                        .with(SLPlaceholders.GENERIC_NAME, () -> display)
                                        .with(SLPlaceholders.PLAYER_NAME, user::getName));
                    }

                    if (!context.hasFlag(CommandArguments.FLAG_SILENT)) {
                        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_SET_NOTIFY, targetPlayer,
                                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> display));
                    }
                });
    }

    private boolean clearGlow(final CommandContext context, final ParsedArguments arguments) {
        return this.loadPlayerOrSenderWithDataAndRunInMainThread(context, arguments, this.module, this.userManager,
                (user, target) -> {
                    this.module.handler().setGlow(user, target, null);

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

    private boolean listGlows(final CommandContext context, final ParsedArguments arguments) {
        final List<GlowEffect> effects = this.module.handler().getEffects().values().stream()
                .sorted(Comparator.comparing(GlowEffect::getId))
                .toList();

        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_LIST_TITLE, context.getSender());
        effects.forEach(effect -> this.module.sendPrefixed(GlowLang.COMMAND_GLOW_LIST_ENTRY, context.getSender(),
                replacer -> replacer
                        .with(SLPlaceholders.GENERIC_NAME, effect::getName)
                        .with(SLPlaceholders.GENERIC_VALUE, effect::getId)));
        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_LIST_OPTIONS, context.getSender());

        return true;
    }

    private boolean phase(final CommandContext context, final ParsedArguments arguments) {
        final Player player = context.getPlayerOrThrow();
        final String action = Utils.lowercase(arguments.getString("action"));
        final String data = arguments.contains("data") ? arguments.getString("data") : null;
        final SunUser user = this.userManager.getOrFetch(player);
        final GlowHandler handler = this.module.handler();

        return switch (action) {
            case "create" -> this.phaseCreate(context, player, user, data);
            case "add" -> this.phaseAdd(context, player, user, data);
            case "remove" -> this.phaseRemove(context, player, user, data);
            case "duration" -> this.phaseDuration(context, player, user, data);
            case "list" -> this.phaseList(player, user);
            case "clear" -> {
                handler.setGlow(user, player, null);
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_CLEAR_DONE, player);
                yield true;
            }
            default -> {
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PHASE_USAGE, context.getSender());
                yield false;
            }
        };
    }

    private List<String> splitData(final String data) {
        if (data == null || data.isBlank())
            return List.of();
        return java.util.Arrays.stream(data.replace(",", " ").split("\\s+"))
                .filter(token -> !token.isBlank())
                .toList();
    }

    private boolean checkPhaseAccess(final CommandContext context, final List<GlowPhase> phases) {
        if (context.hasPermission("sunlight.glow.bypass.color")
                || context.hasPermission("sunlight.glow.color.*")) {
            return true;
        }
        for (final GlowPhase phase : phases) {
            final String id = Utils.lowercase(phase.color().toString());
            if (!context.hasPermission("sunlight.glow.color." + id)) {
                final String bad = phase.color().toString().toLowerCase();
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_NO_ACCESS, context.getSender(),
                        replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> bad));
                return false;
            }
        }
        return true;
    }

    private String describePhases(final List<GlowPhase> phases) {
        return phases.stream()
                .map(phase -> phase.color().toString().toLowerCase() + " " + phase.durationTicks() + "t")
                .collect(Collectors.joining(" → "));
    }

    private boolean phaseCreate(final CommandContext context, final Player player, final SunUser user,
            final String data) {
        final List<String> tokens = this.splitData(data);
        if (tokens.size() < 2 || tokens.size() % 2 != 0) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_CREATE_USAGE, context.getSender());
            return false;
        }

        final List<GlowPhase> phases = new ArrayList<>();
        for (int index = 0; index < tokens.size(); index += 2) {
            final NamedTextColor color = GlowEffect.parseColor(tokens.get(index));
            if (color == null) {
                final String bad = tokens.get(index);
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_COLOR, context.getSender(),
                        replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> bad));
                return false;
            }
            final long duration;
            try {
                duration = Long.parseLong(tokens.get(index + 1));
            } catch (final NumberFormatException exception) {
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_DURATION, context.getSender());
                return false;
            }
            if (duration < GlowPhase.MIN_DURATION || duration > GlowPhase.MAX_DURATION) {
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_DURATION, context.getSender());
                return false;
            }
            phases.add(new GlowPhase(color, duration));
        }

        if (phases.size() > GlowHandler.MAX_PHASES) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_TOO_MANY_PHASES, context.getSender(),
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE,
                            () -> String.valueOf(GlowHandler.MAX_PHASES)));
            return false;
        }
        if (!this.checkPhaseAccess(context, phases))
            return false;

        this.module.handler().setCustomGlow(user, player, phases);
        final String display = this.describePhases(phases);
        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PHASE_CREATED, context.getSender(),
                replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> display));
        return true;
    }

    private boolean phaseAdd(final CommandContext context, final Player player, final SunUser user, final String data) {
        final List<String> tokens = this.splitData(data);
        if (tokens.isEmpty() || tokens.size() > 2) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_CREATE_USAGE, context.getSender());
            return false;
        }
        final NamedTextColor color = GlowEffect.parseColor(tokens.get(0));
        if (color == null) {
            final String bad = tokens.get(0);
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_COLOR, context.getSender(),
                    replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> bad));
            return false;
        }
        long duration = GlowPhase.DEFAULT_DURATION;
        if (tokens.size() == 2) {
            try {
                duration = Long.parseLong(tokens.get(1));
            } catch (final NumberFormatException exception) {
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_DURATION, context.getSender());
                return false;
            }
            if (duration < GlowPhase.MIN_DURATION || duration > GlowPhase.MAX_DURATION) {
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_DURATION, context.getSender());
                return false;
            }
        }

        final GlowPhase phase = new GlowPhase(color, duration);
        if (!this.checkPhaseAccess(context, List.of(phase)))
            return false;
        if (!this.module.handler().addPhase(user, player, phase)) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_TOO_MANY_PHASES, context.getSender(),
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE,
                            () -> String.valueOf(GlowHandler.MAX_PHASES)));
            return false;
        }
        final String colorName = color.toString().toLowerCase();
        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PHASE_ADDED, context.getSender(),
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> colorName)
                        .with(SLPlaceholders.GENERIC_VALUE, () -> String.valueOf(phase.durationTicks())));
        return true;
    }

    private boolean phaseRemove(final CommandContext context, final Player player, final SunUser user,
            final String data) {
        final List<String> tokens = this.splitData(data);
        if (tokens.size() != 1) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PHASE_USAGE, context.getSender());
            return false;
        }
        final int index;
        try {
            index = Integer.parseInt(tokens.get(0)) - 1;
        } catch (final NumberFormatException exception) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_INDEX, context.getSender(),
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> tokens.get(0)));
            return false;
        }
        if (!this.module.handler().removePhase(user, player, index)) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_INDEX, context.getSender(),
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> tokens.get(0)));
            return false;
        }
        final String display = tokens.get(0);
        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PHASE_REMOVED, context.getSender(),
                replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> display));
        return true;
    }

    private boolean phaseDuration(final CommandContext context, final Player player, final SunUser user,
            final String data) {
        final List<String> tokens = this.splitData(data);
        if (tokens.size() != 2) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PHASE_USAGE, context.getSender());
            return false;
        }
        final int index;
        final long duration;
        try {
            index = Integer.parseInt(tokens.get(0)) - 1;
        } catch (final NumberFormatException exception) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_INDEX, context.getSender(),
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> tokens.get(0)));
            return false;
        }
        try {
            duration = Long.parseLong(tokens.get(1));
        } catch (final NumberFormatException exception) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_DURATION, context.getSender());
            return false;
        }
        if (duration < GlowPhase.MIN_DURATION || duration > GlowPhase.MAX_DURATION) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_DURATION, context.getSender());
            return false;
        }
        if (!this.module.handler().setPhaseDuration(user, player, index, duration)) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_INDEX, context.getSender(),
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> tokens.get(0)));
            return false;
        }
        final String displayIndex = tokens.get(0);
        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PHASE_DURATION, context.getSender(),
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> displayIndex)
                        .with(SLPlaceholders.GENERIC_VALUE, () -> String.valueOf(duration)));
        return true;
    }

    private boolean phaseList(final Player player, final SunUser user) {
        final List<GlowPhase> phases = this.module.handler().getWorkingPhases(user);
        if (phases.isEmpty()) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_NO_PHASES, player);
            return false;
        }
        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PHASE_LIST_TITLE, player);
        for (int index = 0; index < phases.size(); index++) {
            final GlowPhase phase = phases.get(index);
            final String number = String.valueOf(index + 1);
            final String detail = phase.color().toString().toLowerCase() + " (" + phase.durationTicks() + " ticks)";
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PHASE_LIST_ENTRY, player,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> number)
                            .with(SLPlaceholders.GENERIC_NAME, () -> detail));
        }
        return true;
    }

    private boolean preset(final CommandContext context, final ParsedArguments arguments) {
        final Player player = context.getPlayerOrThrow();
        final String action = Utils.lowercase(arguments.getString("action"));
        final String name = arguments.contains("name") ? arguments.getString("name") : null;
        final SunUser user = this.userManager.getOrFetch(player);
        final GlowHandler handler = this.module.handler();

        return switch (action) {
            case "save" -> {
                if (name == null || name.isBlank()) {
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PRESET_USAGE, context.getSender());
                    yield false;
                }
                if (GlowHandler.normalizePresetName(name) == null) {
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PRESET_NAME, context.getSender());
                    yield false;
                }
                final List<GlowPhase> working = handler.getWorkingPhases(user);
                if (working.isEmpty()) {
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_NO_PHASES, context.getSender());
                    yield false;
                }
                if (!this.checkPhaseAccess(context, working))
                    yield false;
                if (!handler.savePreset(user, name, working)) {
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PRESET_LIMIT, context.getSender(),
                            replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE,
                                    () -> String.valueOf(GlowHandler.MAX_PRESETS)));
                    yield false;
                }
                final String display = GlowHandler.normalizePresetName(name);
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PRESET_SAVED, context.getSender(),
                        replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> display));
                yield true;
            }
            case "load" -> {
                if (name == null || name.isBlank()) {
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PRESET_USAGE, context.getSender());
                    yield false;
                }
                final List<GlowPhase> phases = handler.loadPreset(user, name);
                if (phases == null) {
                    final String bad = name;
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PRESET_UNKNOWN, context.getSender(),
                            replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> bad));
                    yield false;
                }
                if (!this.checkPhaseAccess(context, phases))
                    yield false;
                handler.setCustomGlow(user, player, phases);
                final String display = GlowHandler.normalizePresetName(name);
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PRESET_LOADED, context.getSender(),
                        replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> display));
                yield true;
            }
            case "delete" -> {
                if (name == null || name.isBlank()) {
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PRESET_USAGE, context.getSender());
                    yield false;
                }
                if (!handler.deletePreset(user, name)) {
                    final String bad = name;
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PRESET_UNKNOWN, context.getSender(),
                            replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> bad));
                    yield false;
                }
                final String display = GlowHandler.normalizePresetName(name);
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PRESET_DELETED, context.getSender(),
                        replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> display));
                yield true;
            }
            case "list" -> {
                final Map<String, List<GlowPhase>> presets = handler.getPresets(user);
                if (presets.isEmpty()) {
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PRESET_LIST_EMPTY, context.getSender());
                    yield true;
                }
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PRESET_LIST_TITLE, context.getSender());
                presets.forEach(
                        (presetName, phases) -> this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PRESET_LIST_ENTRY,
                                context.getSender(), replacer -> replacer
                                        .with(SLPlaceholders.GENERIC_NAME, () -> presetName)
                                        .with(SLPlaceholders.GENERIC_VALUE, () -> describePhases(phases))));
                yield true;
            }
            default -> {
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PRESET_USAGE, context.getSender());
                yield false;
            }
        };
    }

    private boolean glowOn(final CommandContext context, final ParsedArguments arguments) {
        return this.loadPlayerOrSenderWithDataAndRunInMainThread(context, arguments, this.module, this.userManager,
                (user, target) -> {
                    if (!this.module.handler().setGlowEnabled(user, target, true)) {
                        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_NO_SELECTION, context.getSender());
                        return;
                    }

                    if (context.getSender() != target) {
                        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ON_TARGET, context.getSender(),
                                replacer -> replacer.with(SLPlaceholders.PLAYER_NAME, user::getName));
                    }

                    if (!context.hasFlag(CommandArguments.FLAG_SILENT)) {
                        if (context.getSender() == target) {
                            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ON_DONE, target);
                        } else {
                            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ON_NOTIFY, target);
                        }
                    }
                });
    }

    private boolean glowOff(final CommandContext context, final ParsedArguments arguments) {
        return this.loadPlayerOrSenderWithDataAndRunInMainThread(context, arguments, this.module, this.userManager,
                (user, target) -> {
                    this.module.handler().setGlowEnabled(user, target, false);

                    if (context.getSender() != target) {
                        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_OFF_TARGET, context.getSender(),
                                replacer -> replacer.with(SLPlaceholders.PLAYER_NAME, user::getName));
                    }

                    if (!context.hasFlag(CommandArguments.FLAG_SILENT)) {
                        if (context.getSender() == target) {
                            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_OFF_DONE, target);
                        } else {
                            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_OFF_NOTIFY, target);
                        }
                    }
                });
    }
}
