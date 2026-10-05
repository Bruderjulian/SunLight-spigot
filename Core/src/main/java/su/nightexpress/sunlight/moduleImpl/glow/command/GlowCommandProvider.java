package su.nightexpress.sunlight.moduleImpl.glow.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.format.NamedTextColor;

import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.glow.GlowEffect;
import su.nightexpress.sunlight.moduleImpl.glow.GlowHandler;
import su.nightexpress.sunlight.moduleImpl.glow.GlowModule;
import su.nightexpress.sunlight.moduleImpl.glow.GlowPhase;
import su.nightexpress.sunlight.moduleImpl.glow.config.GlowLang;
import su.nightexpress.sunlight.utils.Utils;

/**
 * Glow commands on top of CommandAPI.
 * <p>
 * The {@code enabled} / {@code aliases} / {@code cooldown} / {@code cost}
 * handling
 * lives in {@link su.nightexpress.sunlight.command.api.CommandApiRegistry}, so
 * this
 * class only declares the tree and implements the behaviour.
 */
public class GlowCommandProvider extends CommandProvider<GlowModule> {

    private static final String PERM_ROOT = "sunlight.glow.command.glow.root";
    private static final String PERM_COLOR = "sunlight.glow.command.glow.color";
    private static final String PERM_CLEAR = "sunlight.glow.command.glow.clear";
    private static final String PERM_LIST = "sunlight.glow.command.glow.list";
    private static final String PERM_PHASE = "sunlight.glow.command.glow.phase";
    private static final String PERM_PRESET = "sunlight.glow.command.glow.preset";
    private static final String PERM_ON = "sunlight.glow.command.glow.on";
    private static final String PERM_OFF = "sunlight.glow.command.glow.off";

    public GlowCommandProvider(final GlowModule module) {
        super(module, "glow");
    }

    @Override
    public void setup() {
        this.register("color", command -> command
                .withFullDescription(GlowLang.COMMAND_GLOW_COLOR_DESC.text())
                .withPermission(PERM_COLOR)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.NAME,
                        info -> this.module.settings().getEffectsKeys()))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::setGlow))
                .under("glow");

        this.register("clear", command -> command
                .withFullDescription(GlowLang.COMMAND_GLOW_CLEAR_DESC.text())
                .withPermission(PERM_CLEAR)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::clearGlow))
                .aliases("unglow")
                .under("glow");

        this.register("list", command -> command
                .withFullDescription(GlowLang.COMMAND_GLOW_LIST_DESC.text())
                .withPermission(PERM_LIST)
                .executes(this::listGlows))
                .aliases("glowlist")
                .under("glow");

        this.register("on", command -> command
                .withFullDescription(GlowLang.COMMAND_GLOW_ON_DESC.text())
                .withPermission(PERM_ON)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::glowOn))
                .aliases("glow-on")
                .under("glow");

        this.register("off", command -> command
                .withFullDescription(GlowLang.COMMAND_GLOW_OFF_DESC.text())
                .withPermission(PERM_OFF)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::glowOff))
                .aliases("glow-off")
                .under("glow");

        this.register("phase", command -> command
                .withFullDescription(GlowLang.COMMAND_GLOW_PHASE_DESC.text())
                .withPermission(PERM_PHASE)
                .withRequirement(sender -> sender instanceof Player)
                .withSubcommand(new CommandAPICommand("create")
                        .executes(this::phaseCreate))
                .withSubcommand(new CommandAPICommand("add").withArguments(new GreedyStringArgument("tokens"))
                        .executes(this::phaseAdd))
                .withSubcommand(new CommandAPICommand("remove").withArguments(new IntegerArgument("index"))
                        .executes(this::phaseRemove))
                .withSubcommand(new CommandAPICommand("duration")
                        .executes(this::phaseDuration))
                .withSubcommand(new CommandAPICommand("clear")
                        .executes(this::phaseClear))
                .withSubcommand(new CommandAPICommand("list").executes(this::phaseList))
                .executes((executor, arguments) -> {
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PHASE_USAGE, executor);
                    return 1;
                }))
                .under("glow");

        this.register("preset", command -> command
                .withFullDescription(GlowLang.COMMAND_GLOW_PRESET_DESC.text())
                .withPermission(PERM_PRESET)
                .withRequirement(sender -> sender instanceof Player)
                .withSubcommand(new CommandAPICommand("load")
                        .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PRESET,
                                info -> this.presetSuggestions((Player) info.sender())))
                        .executes(this::presetLoad))
                .withSubcommand(new CommandAPICommand("save")
                        .withOptionalArguments(CommandArgumentConstants.string(CommandArgumentConstants.PRESET,
                                info -> this.presetSuggestions((Player) info.sender())))
                        .executes(this::presetSave))
                .withSubcommand(new CommandAPICommand("delete")
                        .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PRESET,
                                info -> this.presetSuggestions((Player) info.sender())))
                        .executes(this::presetDelete))
                .withSubcommand(new CommandAPICommand("list").executes(this::presetList))
                .executes((executor, arguments) -> {
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PRESET_USAGE, executor);
                    return 1;
                }))
                .under("glow");

        this.registerRoot("glow", command -> command
                .withFullDescription(GlowLang.COMMAND_GLOW_ROOT_DESC.text())
                .withPermission(PERM_ROOT)
                .executes((sender, arguments) -> {
                    if (!(sender instanceof final Player player)) {
                        this.module.sendPrefixed(
                                CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
                        return 1;
                    }
                    this.module.openGlowMenu(player);
                    return 1;
                }));
    }

    private List<String> presetSuggestions(final Player player) {
        return this.module.handler().getPresets(player).keySet().stream().sorted().toList();
    }

    // --- set / color ---

    private int setGlow(final CommandSender sender, final CommandArguments arguments) {
        final String effectId = Utils.lowercase(arguments.getUnchecked(CommandArgumentConstants.NAME));
        final GlowEffect effect = this.module.handler().getEffect(effectId);
        final NamedTextColor singleColor = effect == null ? GlowEffect.parseColor(effectId) : null;

        if (effect == null && singleColor == null) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_INVALID, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> effectId));
            return 0;
        }

        final String accessId = effect == null ? Utils.lowercase(singleColor.toString()) : effect.getId();
        final String accessName = effect == null ? singleColor.toString().toLowerCase() : effect.getName();
        if (!this.hasColorAccess(sender, accessId)) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_NO_ACCESS, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> accessName));
            return 0;
        }

        final GlowEffect effect2 = effect;
        final NamedTextColor color = singleColor;
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, PERM_COLOR + ".others", (user, targetPlayer) -> {
            if (effect2 != null) {
                this.module.handler().setEffect(targetPlayer, effect2);
            } else {
                this.module.handler().setSingleColorGlow(targetPlayer, color);
            }

            final String display = effect2 != null ? effect2.getName() : color.toString().toLowerCase();
            if (sender != targetPlayer) {
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_SET_TARGET, sender, replacer -> replacer
                        .with(SLPlaceholders.GENERIC_NAME, () -> display)
                        .with(SLPlaceholders.PLAYER_NAME, user::getName));
            }

            if (!target.silent()) {
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_SET_NOTIFY, targetPlayer,
                        replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> display));
            }
        });
    }

    // --- clear ---

    private int clearGlow(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, PERM_CLEAR + ".others", (user, targetUser) -> {
            this.module.handler().setEffect(targetUser, null);

            if (sender != targetUser) {
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_CLEAR_TARGET, sender,
                        replacer -> replacer.with(CommonPlaceholders.PLAYER.resolver(targetUser)));
            }

            if (!target.silent()) {
                if (sender == targetUser) {
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_CLEAR_DONE, targetUser);
                } else {
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_CLEAR_NOTIFY, targetUser);
                }
            }
        });
    }

    // --- list ---

    private int listGlows(final CommandSender sender, final CommandArguments arguments) {
        final List<GlowEffect> effects = this.module.handler().getEffects().values().stream()
                .sorted(Comparator.comparing(GlowEffect::getId))
                .toList();

        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_LIST_TITLE, sender);
        effects.forEach(effect -> this.module.sendPrefixed(GlowLang.COMMAND_GLOW_LIST_ENTRY, sender,
                replacer -> replacer
                        .with(SLPlaceholders.GENERIC_NAME, effect::getName)
                        .with(SLPlaceholders.GENERIC_VALUE, effect::getId)));
        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_LIST_OPTIONS, sender);
        return 1;
    }

    // --- on / off ---

    private int glowOn(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, PERM_ON + ".others", (user, targetUser) -> {
            if (!this.module.handler().setGlowEnabled(targetUser, true)) {
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_NO_SELECTION, sender);
                return;
            }

            if (sender != targetUser) {
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ON_TARGET, sender,
                        replacer -> replacer.with(SLPlaceholders.PLAYER_NAME, user::getName));
            }

            if (!target.silent()) {
                if (sender == targetUser) {
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ON_DONE, targetUser);
                } else {
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ON_NOTIFY, targetUser);
                }
            }
        });
    }

    private int glowOff(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, PERM_OFF + ".others", (user, targetUser) -> {
            this.module.handler().setGlowEnabled(targetUser, false);

            if (sender != targetUser) {
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_OFF_TARGET, sender,
                        replacer -> replacer.with(SLPlaceholders.PLAYER_NAME, user::getName));
            }

            if (!target.silent()) {
                if (sender == targetUser) {
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_OFF_DONE, targetUser);
                } else {
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_OFF_NOTIFY, targetUser);
                }
            }
        });
    }

    // --- phase ---

    private int phaseClear(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        this.module.handler().setEffect(player, null);
        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_CLEAR_DONE, player);
        return 1;
    }

    private int phaseCreate(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final Object tokensArg = arguments.get("tokens");
        if (tokensArg == null || !(tokensArg instanceof final String tokensStr)) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_CREATE_USAGE, sender);
            return 0;
        }
        final List<GlowPhase> phases = new ArrayList<>();
        for (final String token : splitData(tokensStr)) {
            final GlowPhase phase = GlowPhase.parse(token);
            if (phase == null) {
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_PHASE, sender);
            }
            phases.add(phase);
        }

        if (phases.size() > GlowHandler.MAX_PHASES) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_TOO_MANY_PHASES, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE,
                            () -> String.valueOf(GlowHandler.MAX_PHASES)));
            return 0;
        }

        final GlowEffect effect = new GlowEffect("", "", phases);
        if (!this.checkEffectAccess(sender, effect)) {
            return 0;
        }

        this.module.handler().setEffect(player, effect);
        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PHASE_CREATED, sender,
                replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> describePhases(phases)));
        return 1;
    }

    private int phaseAdd(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final Object tokensArg = arguments.get("tokens");
        if (tokensArg == null || !(tokensArg instanceof final String tokensStr)) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_CREATE_USAGE, sender);
            return 0;
        }
        final GlowPhase phase = GlowPhase.parse(tokensStr);
        if (phase == null) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_PHASE, sender);
        }
        if (!this.checkPhaseAccess(sender, phase)) {
            return 0;
        }

        final GlowHandler handler = this.module.handler();
        final GlowEffect current = handler.getEffectiveEffect(player);
        if (current == null) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_NO_PHASES, sender);
            return 0;
        }

        // Work on a copy: the effective effect may be a shared configured effect.
        final List<GlowPhase> phases = new ArrayList<>(Arrays.asList(current.getPhases()));
        if (phases.size() >= GlowHandler.MAX_PHASES) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_TOO_MANY_PHASES, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE,
                            () -> String.valueOf(GlowHandler.MAX_PHASES)));
            return 0;
        }
        phases.add(phase);

        handler.setEffect(player, new GlowEffect(current.getId(), current.getName(), phases));
        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PHASE_ADDED, sender,
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> phase.toString().toLowerCase())
                        .with(SLPlaceholders.GENERIC_VALUE, () -> String.valueOf(phase.durationTicks())));
        return 1;
    }

    private int phaseRemove(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final Object indexObj = arguments.get("index");
        if (indexObj == null || !(indexObj instanceof final Integer index) || index == null) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_INDEX, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> String.valueOf(indexObj)));
            return 0;
        }

        final GlowHandler handler = this.module.handler();
        final GlowEffect current = handler.getEffectiveEffect(player);
        if (current == null) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_NO_PHASES, sender);
            return 0;
        }

        final List<GlowPhase> phases = new ArrayList<>(Arrays.asList(current.getPhases()));
        if (index < 0 || index >= phases.size()) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_INDEX, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> String.valueOf(index)));
            return 0;
        }

        phases.remove(phases.get(index));
        handler.setEffect(player, new GlowEffect(current.getId(), current.getName(), phases));
        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PHASE_REMOVED, sender,
                replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> String.valueOf(index)));
        return 1;
    }

    private int phaseDuration(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final Object indexObj = arguments.get("index");
        if (indexObj == null || !(indexObj instanceof final Integer index) || index == null) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_INDEX, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> String.valueOf(indexObj)));
            return 0;
        }
        final Object durationObj = arguments.get("duration");
        if (durationObj == null || !(durationObj instanceof final Long duration) || duration == null
                || duration < GlowPhase.MIN_DURATION
                || duration > GlowPhase.MAX_DURATION) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_DURATION, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> String.valueOf(durationObj)));
            return 0;
        }

        final GlowHandler handler = this.module.handler();
        final GlowEffect current = handler.getEffectiveEffect(player);
        if (current == null) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_NO_PHASES, sender);
            return 0;
        }

        final List<GlowPhase> phases = new ArrayList<>(Arrays.asList(current.getPhases()));
        if (index < 0 || index >= phases.size()) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_BAD_INDEX, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> String.valueOf(duration)));
            return 0;
        }

        final GlowPhase phase = phases.get(index);
        phases.set(index, new GlowPhase(phase.color(), duration));
        handler.setEffect(player, new GlowEffect(current.getId(), current.getName(), phases));
        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PHASE_DURATION, sender,
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> String.valueOf(duration))
                        .with(SLPlaceholders.GENERIC_VALUE, () -> String.valueOf(duration)));
        return 1;
    }

    private int phaseList(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final GlowEffect effect = this.module.handler().getEffectiveEffect(player);
        if (effect == null) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_NO_SELECTION, player);
            return 0;
        }

        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PHASE_LIST_TITLE, player);
        for (int index = 0; index < effect.phaseCount(); index++) {
            final GlowPhase phase = effect.getPhase(index);
            final String number = String.valueOf(index + 1);
            final String detail = phase.color().toString().toLowerCase() + " (" + phase.durationTicks() + "t)";
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PHASE_LIST_ENTRY, player,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> number)
                            .with(SLPlaceholders.GENERIC_NAME, () -> detail));
        }
        return 1;
    }

    // --- preset ---

    private int presetSave(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final Object nameArg = arguments.get(CommandArgumentConstants.PRESET);
        if (nameArg == null || !(nameArg instanceof final String name) || name.isBlank()) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PRESET_USAGE, sender);
            return 0;
        }
        if (GlowHandler.normalizePresetName(name) == null) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PRESET_NAME, sender);
            return 0;
        }

        final GlowHandler handler = this.module.handler();
        final GlowEffect effect = handler.getEffectiveEffect(player);
        if (effect == null) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_NO_SELECTION, sender);
            return 0;
        }
        if (!this.checkEffectAccess(sender, effect)) {
            return 0;
        }
        if (!handler.savePreset(player, name, effect)) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PRESET_LIMIT, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE,
                            () -> String.valueOf(GlowHandler.MAX_PRESETS)));
            return 0;
        }
        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PRESET_SAVED, sender,
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME,
                        () -> GlowHandler.normalizePresetName(name)));
        return 1;
    }

    private int presetLoad(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final Object nameArg = arguments.get(CommandArgumentConstants.PRESET);
        if (nameArg == null || !(nameArg instanceof final String name) || name.isBlank()) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PRESET_USAGE, sender);
            return 0;
        }

        final GlowHandler handler = this.module.handler();
        final GlowEffect effect = handler.loadPreset(player, name);
        if (effect == null) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PRESET_UNKNOWN, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> name));
            return 0;
        }
        if (!this.checkEffectAccess(sender, effect)) {
            return 0;
        }
        handler.setEffect(player, effect);
        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PRESET_LOADED, sender,
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME,
                        () -> GlowHandler.normalizePresetName(name)));
        return 1;
    }

    private int presetDelete(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final Object nameArg = arguments.get(CommandArgumentConstants.PRESET);
        if (nameArg == null || !(nameArg instanceof final String name) || name.isBlank()) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PRESET_USAGE, sender);
            return 0;
        }

        if (!this.module.handler().deletePreset(player, name)) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_PRESET_UNKNOWN, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> name));
            return 0;
        }
        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PRESET_DELETED, sender,
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME,
                        () -> GlowHandler.normalizePresetName(name)));
        return 1;
    }

    private int presetList(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final Map<String, GlowEffect> presets = this.module.handler().getPresets(player);
        if (presets.isEmpty()) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PRESET_LIST_EMPTY, sender);
            return 1;
        }

        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_PRESET_LIST_TITLE, sender);
        presets.forEach((presetName, presetEffect) -> this.module.sendPrefixed(
                GlowLang.COMMAND_GLOW_PRESET_LIST_ENTRY, sender, replacer -> replacer
                        .with(SLPlaceholders.GENERIC_NAME, () -> presetName)
                        .with(SLPlaceholders.GENERIC_VALUE,
                                () -> describePhases(List.of(presetEffect.getPhases())))));
        return 1;
    }

    // --- Helpers ---

    private boolean hasColorAccess(final CommandSender sender, final String accessId) {
        return sender.hasPermission("sunlight.glow.bypass.color")
                || sender.hasPermission("sunlight.glow.color.*")
                || sender.hasPermission("sunlight.glow.color." + Utils.lowercase(accessId));
    }

    private boolean checkEffectAccess(final CommandSender sender, final GlowEffect effect) {
        for (int index = 0; index < effect.phaseCount(); index++) {
            final String id = effect.getPhase(index).color().toString();
            if (!this.hasColorAccess(sender, id)) {
                this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_NO_ACCESS, sender,
                        replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> Utils.lowercase(id)));
                return false;
            }
        }
        return true;
    }

    private boolean checkPhaseAccess(final CommandSender sender, final GlowPhase phase) {
        if (this.hasColorAccess(sender, phase.color().toString())) {
            return true;
        }
        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_NO_ACCESS, sender,
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME,
                        () -> Utils.lowercase(phase.color().toString())));
        return false;
    }

    private static List<String> splitData(final String data) {
        if (data == null || data.isBlank()) {
            return List.of();
        }
        return Arrays.stream(data.replace(",", " ").split("\\s+"))
                .filter(token -> !token.isBlank())
                .toList();
    }

    private static String describePhases(final List<GlowPhase> phases) {
        return phases.stream()
                .map(phase -> phase.color().toString().toLowerCase() + " " + phase.durationTicks() + "t")
                .collect(Collectors.joining(" → "));
    }
}
