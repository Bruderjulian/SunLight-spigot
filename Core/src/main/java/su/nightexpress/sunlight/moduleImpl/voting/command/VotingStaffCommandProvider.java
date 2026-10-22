package su.nightexpress.sunlight.moduleImpl.voting.command;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;

import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.voting.VotingModule;
import su.nightexpress.sunlight.moduleImpl.voting.config.VotingLang;
import su.nightexpress.sunlight.moduleImpl.voting.config.VotingPerms;
import su.nightexpress.sunlight.moduleImpl.voting.model.GateType;
import su.nightexpress.sunlight.moduleImpl.voting.model.MoneyMode;
import su.nightexpress.sunlight.moduleImpl.voting.model.OverMaxMode;
import su.nightexpress.sunlight.moduleImpl.voting.model.TieBreak;
import su.nightexpress.sunlight.moduleImpl.voting.model.Vote;
import su.nightexpress.sunlight.moduleImpl.voting.model.WeightCombine;
import su.nightexpress.sunlight.moduleImpl.voting.model.WeightSource;
import su.nightexpress.sunlight.moduleImpl.voting.model.WorldMode;
import su.nightexpress.sunlight.utils.TimeUtil;
import su.nightexpress.sunlight.utils.Utils;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.GRAY;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.YELLOW;

/**
 * The staff-facing half of the module.
 * <p>
 * Poll rules are edited with {@code /voting rules <vote> <setting> <value>} rather than one
 * sub-command per rule. There are eighteen of them, a fixed set, and they are the same for every
 * poll: a flat list of sub-commands would bury the ones people actually use.
 */
public class VotingStaffCommandProvider extends CommandProvider<VotingModule> {

    private static final String COMMAND_ROOT   = VotingCommands.ROOT_NAME;
    private static final String COMMAND_OPTION = "option";
    private static final String COMMAND_RULES  = "rules";
    private static final String COMMAND_CREATE = "create";
    private static final String COMMAND_CLOSE  = "close";
    private static final String COMMAND_CANCEL = "cancel";
    private static final String COMMAND_DELETE = "delete";
    private static final String COMMAND_EXTEND = "extend";
    private static final String COMMAND_INFO   = "info";
    private static final String COMMAND_ALL    = "all";
    private static final String COMMAND_BROADCAST = "broadcast";
    private static final String COMMAND_CLONE  = "clone";
    private static final String COMMAND_RENAME = "rename";
    private static final String COMMAND_DESCRIBE = "describe";
    private static final String COMMAND_START  = "start";

    private static final String SUBCOMMAND_ADD    = "add";
    private static final String SUBCOMMAND_REMOVE = "remove";
    private static final String SUBCOMMAND_LIST   = "list";
    private static final String SUBCOMMAND_GATE   = "gate";

    private static final String ARGUMENT_POLL     = "poll";
    private static final String ARGUMENT_OPTION   = "option";
    private static final String ARGUMENT_DISPLAY  = "display";
    private static final String ARGUMENT_ICON     = "icon";
    private static final String ARGUMENT_DURATION = "duration";
    private static final String ARGUMENT_DELAY    = "delay";
    private static final String ARGUMENT_TEXT     = "text";
    private static final String ARGUMENT_NAME     = "name";
    private static final String ARGUMENT_SETTING  = "setting";
    private static final String ARGUMENT_VALUE    = "value";
    private static final String ARGUMENT_GATE     = "gate";

    private static final String FLAG_FORCE = "-f";
    private static final String FLAG_FORCE_LONG = "--force";

    public VotingStaffCommandProvider(final VotingModule module) {
        super(module, "voting-staff");
    }

    @Override
    public void setup() {
        this.register(COMMAND_CREATE, builder -> builder
                .withFullDescription(VotingLang.COMMAND_CREATE_DESC.text())
                .withPermission(VotingPerms.COMMAND_CREATE)
                // Suggests nothing that already exists: every listed name would be rejected as a
                // duplicate key, so examples are more useful than the open polls here.
                .withArguments(
                        CommandArgumentConstants.string(ARGUMENT_NAME, info -> List.of()),
                        CommandArgumentConstants.string(ARGUMENT_DURATION, info -> List.of("1h", "30m", "7d")))
                .withOptionalArguments(
                        CommandArgumentConstants.string(ARGUMENT_DELAY, info -> List.of("0", "10m", "1h")),
                        CommandArgumentConstants.greedy(ARGUMENT_TEXT, info -> List.of()))
                .executes(this::create))
                .under(COMMAND_ROOT);

        this.register(COMMAND_OPTION, builder -> builder
                .withFullDescription(VotingLang.COMMAND_OPTION_ADD_DESC.text())
                .withPermission(VotingPerms.COMMAND_OPTION_ADD)
                .executes((sender, arguments) -> {
                    this.module.sendPrefixed(CoreLang.COMMAND_SYNTAX_GENERIC_ERROR, sender);
                    return 0;
                })
                .withSubcommand(this.optionAdd())
                .withSubcommand(this.optionRemove())
                .withSubcommand(this.optionList())
                .withSubcommand(this.optionGate()))
                .under(COMMAND_ROOT);

        this.register(COMMAND_RULES, builder -> builder
                .withFullDescription(VotingLang.COMMAND_RULES_DESC.text())
                .withPermission(VotingPerms.COMMAND_RULES)
                .withArguments(this.voteArgument(), this.settingArgument(), this.valueArgument())
                .executes(this::rules))
                .under(COMMAND_ROOT);

        this.register(COMMAND_INFO, builder -> builder
                .withFullDescription(VotingLang.COMMAND_INFO_DESC.text())
                .withPermission(VotingPerms.COMMAND_INFO)
                .withArguments(this.voteArgument())
                .executes(this::info))
                .under(COMMAND_ROOT);

        this.register(COMMAND_CLOSE, builder -> builder
                .withFullDescription(VotingLang.COMMAND_CLOSE_DESC.text())
                .withPermission(VotingPerms.COMMAND_CLOSE)
                .withArguments(this.voteArgument())
                .executes(this::close))
                .under(COMMAND_ROOT);

        this.register(COMMAND_CANCEL, builder -> builder
                .withFullDescription(VotingLang.COMMAND_CANCEL_DESC.text())
                .withPermission(VotingPerms.COMMAND_CANCEL)
                .withArguments(this.voteArgument())
                .executes(this::cancel))
                .under(COMMAND_ROOT);

        this.register(COMMAND_DELETE, builder -> builder
                .withFullDescription(VotingLang.COMMAND_DELETE_DESC.text())
                .withPermission(VotingPerms.COMMAND_DELETE)
                .withArguments(this.voteArgument())
                .executes(this::delete))
                .under(COMMAND_ROOT);

        this.register(COMMAND_EXTEND, builder -> builder
                .withFullDescription(VotingLang.COMMAND_EXTEND_DESC.text())
                .withPermission(VotingPerms.COMMAND_EXTEND)
                .withArguments(this.voteArgument(),
                        CommandArgumentConstants.string(ARGUMENT_DURATION, info -> List.of("1h", "10m", "1d")))
                .executes(this::extend))
                .under(COMMAND_ROOT);

        this.register(COMMAND_ALL, builder -> builder
                .withFullDescription(VotingLang.COMMAND_ALL_DESC.text())
                .withPermission(VotingPerms.COMMAND_ALL)
                .executes(this::all))
                .under(COMMAND_ROOT);

        this.register(COMMAND_BROADCAST, builder -> builder
                .withFullDescription(VotingLang.COMMAND_BROADCAST_DESC.text())
                .withPermission(VotingPerms.COMMAND_BROADCAST)
                .withArguments(this.voteArgument())
                .executes(this::broadcast))
                .under(COMMAND_ROOT);

        this.register(COMMAND_CLONE, builder -> builder
                .withFullDescription(VotingLang.COMMAND_CLONE_DESC.text())
                .withPermission(VotingPerms.COMMAND_CLONE)
                .withArguments(this.voteArgument(),
                        CommandArgumentConstants.string(ARGUMENT_NAME, info -> List.of()))
                .executes(this::clonePoll))
                .under(COMMAND_ROOT);

        this.register(COMMAND_RENAME, builder -> builder
                .withFullDescription(VotingLang.COMMAND_RENAME_DESC.text())
                .withPermission(VotingPerms.COMMAND_RENAME)
                .withArguments(this.voteArgument(),
                        CommandArgumentConstants.greedy(ARGUMENT_DISPLAY, info -> List.of()))
                .executes(this::renamePoll))
                .under(COMMAND_ROOT);

        this.register(COMMAND_DESCRIBE, builder -> builder
                .withFullDescription(VotingLang.COMMAND_DESCRIBE_DESC.text())
                .withPermission(VotingPerms.COMMAND_DESCRIBE)
                .withArguments(this.voteArgument())
                .withOptionalArguments(
                        CommandArgumentConstants.greedy(ARGUMENT_TEXT, info -> List.of()))
                .executes(this::describePoll))
                .under(COMMAND_ROOT);

        this.register(COMMAND_START, builder -> builder
                .withFullDescription(VotingLang.COMMAND_START_DESC.text())
                .withPermission(VotingPerms.COMMAND_START)
                .withArguments(this.voteArgument())
                .executes(this::startPoll))
                .under(COMMAND_ROOT);
    }

    // -- Option sub-commands ----------------------------------------------------------------------

    /*
     * The choice sub-commands are built by hand rather than through this provider's own node
     * mechanism, which only knows roots and their direct children. That also means they carry no
     * per-command cooldown or cost, which is the right trade: they are staff tools on a poll that
     * is already open and its own limits are enforced by the module.
     */

    private CommandAPICommand optionAdd() {
        return new CommandAPICommand(SUBCOMMAND_ADD)
                .withFullDescription(VotingLang.COMMAND_OPTION_ADD_DESC.text())
                .withPermission(VotingPerms.COMMAND_OPTION_ADD)
                .withArguments(this.voteArgument(),
                        CommandArgumentConstants.string(ARGUMENT_OPTION, info -> List.of()),
                        CommandArgumentConstants.string(ARGUMENT_DISPLAY, info -> List.of()),
                        CommandArgumentConstants.string(ARGUMENT_ICON, info -> List.of()))
                .executes((sender, arguments) -> {
                            return this.optionAdd(sender, arguments);
                        });
    }

    private CommandAPICommand optionRemove() {
        return new CommandAPICommand(SUBCOMMAND_REMOVE)
                .withFullDescription(VotingLang.COMMAND_OPTION_REMOVE_DESC.text())
                .withPermission(VotingPerms.COMMAND_OPTION_REMOVE)
                .withArguments(this.voteArgument(),
                        CommandArgumentConstants.greedy(ARGUMENT_OPTION, info -> List.of()))
                .executes((sender, arguments) -> {
                            return this.optionRemove(sender, arguments);
                        });
    }

    private CommandAPICommand optionList() {
        return new CommandAPICommand(SUBCOMMAND_LIST)
                .withFullDescription(VotingLang.COMMAND_OPTION_LIST_DESC.text())
                .withPermission(VotingPerms.COMMAND_OPTION_LIST)
                .withArguments(this.voteArgument())
                .executes((sender, arguments) -> {
                            return this.optionList(sender, arguments);
                        });
    }

    private CommandAPICommand optionGate() {
        return new CommandAPICommand(SUBCOMMAND_GATE)
                .withFullDescription(VotingLang.COMMAND_OPTION_GATE_DESC.text())
                .withPermission(VotingPerms.COMMAND_OPTION_GATE)
                .withArguments(this.voteArgument(),
                        CommandArgumentConstants.string(ARGUMENT_OPTION, info -> List.of()),
                        new StringArgument(ARGUMENT_GATE)
                                .replaceSuggestions(ArgumentSuggestions
                                        .strings(info -> Utils.getEnumNamesArray(GateType.class))))
                .executes((sender, arguments) -> {
                            return this.optionGate(sender, arguments);
                        });
    }

    // -- Arguments --------------------------------------------------------------------------------

    private Argument<String> voteArgument() {
        return CommandArgumentConstants.string(ARGUMENT_POLL,
                info -> this.module.getRepository().getVotes().stream().map(Vote::getKey).toList());
    }

    private Argument<String> settingArgument() {
        return new StringArgument(ARGUMENT_SETTING)
                .replaceSuggestions(ArgumentSuggestions.strings(info -> SETTINGS.toArray(new String[0])));
    }

    private Argument<String> valueArgument() {
        // Greedy so multi-word values survive: "worlds a, b" would otherwise arrive as just "a,".
        return CommandArgumentConstants.greedy(ARGUMENT_VALUE, info -> List.of());
    }

    private static final List<String> SETTINGS = List.of(
            "multi-vote", "max-votes",
            "min-playtime", "world-mode", "worlds",
            "money-mode", "min-balance", "max-balance", "over-max", "currency-per-weight", "max-money-weight",
            "weighting", "weight-source", "weight-combine", "base-weight", "max-multiplier", "max-weight",
            "min-participants", "pass-percent", "neutral-counts", "secret", "silent",
            "tie-break", "allow-change", "change-lock",
            "recurrence", "recurrence-interval", "recurrence-limit",
            "runoff", "runoff-top", "runoff-duration");

    // -- Handlers ---------------------------------------------------------------------------------

    private int create(final CommandSender sender, final CommandArguments arguments) {
        final String name = this.string(arguments, ARGUMENT_NAME);
        final String durationRaw = this.string(arguments, ARGUMENT_DURATION);
        if (name == null || durationRaw == null) {
            this.module.sendPrefixed(CoreLang.COMMAND_SYNTAX_GENERIC_ERROR, sender);
            return 0;
        }

        final long seconds = TimeUtil.parseDurationSeconds(durationRaw);
        if (seconds <= 0L) {
            this.module.sendPrefixed(VotingLang.ERROR_BAD_DURATION, sender,
                    b -> b.with(SLPlaceholders.GENERIC_TEXT, () -> durationRaw));
            return 0;
        }

        int delaySeconds = 0;
        final String delayRaw = this.string(arguments, ARGUMENT_DELAY);
        if (delayRaw != null) {
            final long delay = TimeUtil.parseDurationSeconds(delayRaw);
            if (delay < 0L) {
                this.module.sendPrefixed(VotingLang.ERROR_BAD_DURATION, sender,
                        b -> b.with(SLPlaceholders.GENERIC_TEXT, () -> delayRaw));
                return 0;
            }
            delaySeconds = (int) Math.min(delay, Integer.MAX_VALUE);
        }
        final String description = this.string(arguments, ARGUMENT_TEXT);
        return this.module.create(sender, name, name, description == null ? "" : description,
                (int) Math.min(seconds, Integer.MAX_VALUE), delaySeconds) != null ? 1 : 0;
    }

    private int close(final CommandSender sender, final CommandArguments arguments) {
        final Vote vote = this.require(sender, arguments);
        return vote == null ? 0 : this.module.close(sender, vote) ? 1 : 0;
    }

    private int cancel(final CommandSender sender, final CommandArguments arguments) {
        final Vote vote = this.require(sender, arguments);
        return vote == null ? 0 : this.module.cancel(sender, vote) ? 1 : 0;
    }

    private int delete(final CommandSender sender, final CommandArguments arguments) {
        final Vote vote = this.require(sender, arguments);
        return vote == null ? 0 : this.module.delete(sender, vote) ? 1 : 0;
    }

    private int info(final CommandSender sender, final CommandArguments arguments) {
        final Vote vote = this.require(sender, arguments);
        if (vote == null) {
            return 0;
        }
        this.module.showInfo(sender, vote);
        return 1;
    }

    private int broadcast(final CommandSender sender, final CommandArguments arguments) {
        final Vote vote = this.require(sender, arguments);
        if (vote == null) {
            return 0;
        }
        this.module.broadcast(vote);
        this.module.sendPrefixed(VotingLang.BROADCAST, sender, b -> b.with(vote.placeholders()));
        return 1;
    }

    private int clonePoll(final CommandSender sender, final CommandArguments arguments) {
        final Vote vote = this.require(sender, arguments);
        final String name = this.string(arguments, ARGUMENT_NAME);
        if (vote == null || name == null) {
            return 0;
        }
        return this.module.cloneVote(sender, vote, name) != null ? 1 : 0;
    }

    private int renamePoll(final CommandSender sender, final CommandArguments arguments) {
        final Vote vote = this.require(sender, arguments);
        final String display = this.string(arguments, ARGUMENT_DISPLAY);
        if (vote == null || display == null) {
            this.module.sendPrefixed(CoreLang.COMMAND_SYNTAX_GENERIC_ERROR, sender);
            return 0;
        }
        return this.module.rename(sender, vote, display) ? 1 : 0;
    }

    private int describePoll(final CommandSender sender, final CommandArguments arguments) {
        final Vote vote = this.require(sender, arguments);
        if (vote == null) {
            return 0;
        }
        final String text = this.string(arguments, ARGUMENT_TEXT);
        return this.module.describe(sender, vote, text == null ? "" : text) ? 1 : 0;
    }

    private int startPoll(final CommandSender sender, final CommandArguments arguments) {
        final Vote vote = this.require(sender, arguments);
        if (vote == null) {
            return 0;
        }
        return this.module.startNow(sender, vote) ? 1 : 0;
    }

    private int extend(final CommandSender sender, final CommandArguments arguments) {
        final Vote vote = this.require(sender, arguments);
        final String durationRaw = this.string(arguments, ARGUMENT_DURATION);
        if (vote == null || durationRaw == null) {
            return 0;
        }

        final long seconds = TimeUtil.parseDurationSeconds(durationRaw);
        if (seconds <= 0L) {
            this.module.sendPrefixed(VotingLang.ERROR_BAD_DURATION, sender,
                    b -> b.with(SLPlaceholders.GENERIC_TEXT, () -> durationRaw));
            return 0;
        }
        return this.module.extend(sender, vote, (int) Math.min(seconds, Integer.MAX_VALUE)) ? 1 : 0;
    }

    private int all(final CommandSender sender, final CommandArguments arguments) {
        final List<Vote> votes = this.module.getAllVotes();
        if (votes.isEmpty()) {
            this.module.sendPrefixed(VotingLang.ERROR_NO_VOTES, sender);
            return 0;
        }

        votes.forEach(vote -> sender.sendMessage("<dark_gray>• <white>" + vote.getDisplay()
                + " <gray>(" + VotingLang.STATUS.getLocalized(vote.getStatus()) + ")"));
        return 1;
    }

    private int optionAdd(final CommandSender sender, final CommandArguments arguments) {
        final Vote vote = this.require(sender, arguments);
        final String optionKey = this.string(arguments, ARGUMENT_OPTION);
        if (vote == null || optionKey == null) {
            return 0;
        }

        final String display = this.string(arguments, ARGUMENT_DISPLAY);
        final String icon = this.string(arguments, ARGUMENT_ICON);
        return this.module.addOption(sender, vote, optionKey, display, icon) ? 1 : 0;
    }

    private int optionRemove(final CommandSender sender, final CommandArguments arguments) {
        final Vote vote = this.require(sender, arguments);
        if (vote == null) {
            return 0;
        }

        // Greedy so that the force flag can sit after the choice without needing its own argument.
        // Only a trailing flag counts: a choice literally named "-f" must stay removable, and an
        // exact-case match keeps "--Force" as part of the name rather than a flag.
        final String raw = this.string(arguments, ARGUMENT_OPTION);
        if (raw == null) {
            this.module.sendPrefixed(CoreLang.COMMAND_SYNTAX_GENERIC_ERROR, sender);
            return 0;
        }

        boolean force = false;
        String optionKey = raw.trim();
        final String[] tokens = optionKey.split("\\s+");
        if (tokens.length > 1) {
            final String last = tokens[tokens.length - 1];
            if (last.equals(FLAG_FORCE) || last.equals(FLAG_FORCE_LONG)) {
                force = true;
                optionKey = String.join(" ", java.util.Arrays.copyOf(tokens, tokens.length - 1));
            }
        }
        if (optionKey.isBlank()) {
            this.module.sendPrefixed(CoreLang.COMMAND_SYNTAX_GENERIC_ERROR, sender);
            return 0;
        }

        return this.module.removeOption(sender, vote, optionKey, force) ? 1 : 0;
    }

    private int optionList(final CommandSender sender, final CommandArguments arguments) {
        final Vote vote = this.require(sender, arguments);
        if (vote == null) {
            return 0;
        }

        final var tally = this.module.tally(vote);
        vote.getOptionList().forEach(option -> {
            final String gates = option.isGated()
                    ? " <dark_gray>[" + YELLOW + "{" + option.getGates().stream()
                    .map(Enum::name).reduce((a, c) -> a + ", " + c).orElse("") + "}" + GRAY + "]"
                    : "";
            sender.sendMessage("<dark_gray>• <white>" + option.getDisplay()
                    + " <dark_gray>(" + option.getKey() + ")"
                    + " <gray>— " + tally.count(option.getKey()) + " votes, "
                    + VotingModule.format(tally.share(option.getKey())) + "%"
                    + gates);
        });
        return 1;
    }

    private int optionGate(final CommandSender sender, final CommandArguments arguments) {
        final Vote vote = this.require(sender, arguments);
        final String optionKey = this.string(arguments, ARGUMENT_OPTION);
        final String gateRaw = this.string(arguments, ARGUMENT_GATE);
        if (vote == null || optionKey == null || gateRaw == null) {
            return 0;
        }

        final GateType gate;
        try {
            gate = GateType.valueOf(gateRaw.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            this.module.sendPrefixed(CoreLang.COMMAND_SYNTAX_GENERIC_ERROR, sender);
            return 0;
        }
        return this.module.toggleGate(sender, vote, optionKey, gate) ? 1 : 0;
    }

    /**
     * {@code /voting rules <vote> <setting> <value>}.
     * <p>
     * Editing the rules never rewrites a ballot that has already been cast: weight is frozen when
     * a vote is cast, so tightening a cap afterwards changes what future votes are worth and
     * leaves the standing tally as it honestly is.
     */
    private int rules(final CommandSender sender, final CommandArguments arguments) {
        final Vote vote = this.require(sender, arguments);
        final String setting = this.string(arguments, ARGUMENT_SETTING);
        final String value = this.string(arguments, ARGUMENT_VALUE);
        if (vote == null || setting == null || value == null) {
            return 0;
        }

        if (!applyRule(vote, setting.trim().toLowerCase(Locale.ROOT), value.trim())) {
            this.module.sendPrefixed(CoreLang.COMMAND_SYNTAX_GENERIC_ERROR, sender);
            return 0;
        }
        this.module.saveRules(sender, vote);
        return 1;
    }

    private static boolean applyRule(final Vote vote, final String setting, final String value) {
        final var rules = vote.getRules();
        try {
            switch (setting) {
                case "multi-vote" -> rules.setMultiVote(parseBoolean(value));
                // A negative here would silently mean "unlimited" via the maxVotes<=0 convention,
                // so a typo'd "-5" is rejected instead of opening the poll up.
                case "max-votes" -> rules.setMaxVotes(parseNonNegativeInt(value));
                case "min-playtime" -> rules.setMinPlaytimeHours(parseNonNegativeInt(value));
                case "world-mode" -> rules.setWorldMode(enumOf(value, WorldMode.class));
                case "worlds" -> rules.setWorlds(splitList(value));
                case "money-mode" -> rules.setMoneyMode(enumOf(value, MoneyMode.class));
                case "min-balance" -> rules.setMinBalance(parseNonNegativeDouble(value));
                case "max-balance" -> rules.setMaxBalance(parseNonNegativeDouble(value));
                case "over-max" -> rules.setOverMaxMode(enumOf(value, OverMaxMode.class));
                case "currency-per-weight" -> rules.setCurrencyPerWeight(parseNonNegativeDouble(value));
                case "max-money-weight" -> rules.setMaxMoneyWeight(parseNonNegativeDouble(value));
                case "weighting" -> rules.setWeightingEnabled(parseBoolean(value));
                case "weight-source" -> rules.setWeightSource(enumOf(value, WeightSource.class));
                case "weight-combine" -> rules.setWeightCombine(enumOf(value, WeightCombine.class));
                case "base-weight" -> rules.setBaseWeight(parseNonNegativeDouble(value));
                case "max-multiplier" -> rules.setMaxMultiplier(parseNonNegativeDouble(value));
                case "max-weight" -> rules.setMaxWeight(parseNonNegativeDouble(value));
                case "min-participants" -> rules.setMinParticipants(parseNonNegativeInt(value));
                case "pass-percent" -> rules.setPassPercent(parseNonNegativeDouble(value));
                case "neutral-counts" -> rules.setNeutralCountsTowardTotal(parseBoolean(value));
                case "secret" -> rules.setSecretBallot(parseBoolean(value));
                case "silent" -> rules.setSilent(parseBoolean(value));
                case "tie-break" -> rules.setTieBreak(enumOf(value, TieBreak.class));
                case "allow-change" -> rules.setAllowChange(parseBoolean(value));
                case "change-lock" -> rules.setChangeLockSeconds(parseNonNegativeInt(value));
                case "recurrence" -> rules.setRecurrenceEnabled(parseBoolean(value));
                case "recurrence-interval" -> {
                    final long seconds = TimeUtil.parseDurationSeconds(value);
                    if (seconds < 0L) {
                        return false;
                    }
                    rules.setRecurrenceIntervalSeconds((int) Math.min(seconds, Integer.MAX_VALUE));
                }
                case "recurrence-limit" -> rules.setRecurrenceLimit(parseNonNegativeInt(value));
                case "runoff" -> rules.setRunoffEnabled(parseBoolean(value));
                case "runoff-top" -> rules.setRunoffTopN(Math.max(2, parseNonNegativeInt(value)));
                case "runoff-duration" -> {
                    final long seconds = TimeUtil.parseDurationSeconds(value);
                    if (seconds <= 0L) {
                        return false;
                    }
                    rules.setRunoffDurationSeconds((int) Math.min(seconds, Integer.MAX_VALUE));
                }
                default -> {
                    return false;
                }
            }
        } catch (final NumberFormatException exception) {
            return false;
        }
        return true;
    }

    private static int parseNonNegativeInt(final String value) {
        final int parsed = Integer.parseInt(value.trim());
        if (parsed < 0) {
            throw new NumberFormatException("Negative value: " + value);
        }
        return parsed;
    }

    private static double parseNonNegativeDouble(final String value) {
        final double parsed = Double.parseDouble(value.trim());
        if (!Double.isFinite(parsed) || parsed < 0D) {
            throw new NumberFormatException("Negative value: " + value);
        }
        return parsed;
    }

    private static boolean parseBoolean(final String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("y")
                || value.equalsIgnoreCase("on") || value.equals("1")) {
            return true;
        }
        if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("n")
                || value.equalsIgnoreCase("off") || value.equals("0")) {
            return false;
        }
        throw new NumberFormatException("Not a boolean: " + value);
    }

    private static <E extends Enum<E>> E enumOf(final String value, final Class<E> type) {
        for (final E constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) {
                return constant;
            }
        }
        throw new NumberFormatException("Not a " + type.getSimpleName() + ": " + value);
    }

    private static Set<String> splitList(final String value) {
        if (value.isEmpty() || value.equals("-")) {
            return Set.of();
        }
        final Set<String> values = new LinkedHashSet<>();
        for (final String entry : value.split(",")) {
            if (!entry.isBlank()) {
                values.add(entry.trim());
            }
        }
        return values;
    }

    // -- Helpers ----------------------------------------------------------------------------------

    private @Nullable Vote require(final CommandSender sender, final CommandArguments arguments) {
        final String key = this.string(arguments, ARGUMENT_POLL);
        if (key == null) {
            this.module.sendPrefixed(CoreLang.COMMAND_SYNTAX_GENERIC_ERROR, sender);
            return null;
        }

        final Vote vote = this.module.findVote(key);
        if (vote == null) {
            this.module.sendPrefixed(VotingLang.ERROR_UNKNOWN_VOTE, sender,
                    b -> b.with(SLPlaceholders.GENERIC_TARGET, () -> key));
        }
        return vote;
    }

    private @Nullable String string(final CommandArguments arguments, final String name) {
        final Object raw = arguments.getOrDefaultUnchecked(name, null);
        return raw instanceof final String value && !value.isBlank() ? value : null;
    }
}