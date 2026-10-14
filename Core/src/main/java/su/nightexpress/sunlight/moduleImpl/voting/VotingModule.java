package su.nightexpress.sunlight.moduleImpl.voting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.event.VoteCastEvent;
import su.nightexpress.sunlight.api.event.VoteConcludedEvent;
import su.nightexpress.sunlight.api.event.VoteCreateEvent;
import su.nightexpress.sunlight.api.provider.VoteHandle;
import su.nightexpress.sunlight.api.provider.VotingProvider;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.voting.command.VotingCastCommandProvider;
import su.nightexpress.sunlight.moduleImpl.voting.command.VotingStaffCommandProvider;
import su.nightexpress.sunlight.moduleImpl.voting.listener.VotingListener;
import su.nightexpress.sunlight.moduleImpl.voting.config.VotingLang;
import su.nightexpress.sunlight.moduleImpl.voting.config.VotingPerms;
import su.nightexpress.sunlight.moduleImpl.voting.data.BallotRepository;
import su.nightexpress.sunlight.moduleImpl.voting.data.VoteRepository;
import su.nightexpress.sunlight.moduleImpl.voting.data.VotingDataManager;
import su.nightexpress.sunlight.moduleImpl.voting.menu.VoteViewMenu;
import su.nightexpress.sunlight.moduleImpl.voting.menu.VotesMenu;
import su.nightexpress.sunlight.moduleImpl.voting.model.GateType;
import su.nightexpress.sunlight.moduleImpl.voting.model.OverMaxMode;
import su.nightexpress.sunlight.moduleImpl.voting.model.Vote;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteBallot;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteOption;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteRules;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteStatus;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteTally;
import su.nightexpress.sunlight.moduleImpl.voting.model.WeightCombine;
import su.nightexpress.sunlight.utils.EconomyUtils;

public class VotingModule extends Module implements VotingProvider {

    private static final String PLAYTIME_MODULE_ID = "playtime";

    private final VotingSettings settings;
    private final VotingDataManager dataManager;
    private final VoteRepository repository;
    private final BallotRepository ballotRepository;

    private VotesMenu menu;
    private VoteViewMenu viewMenu;

    private volatile boolean dataLoaded;

    public VotingModule(final ModuleDefinition<VotingModule> definition, final SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new VotingSettings();
        this.dataManager = new VotingDataManager(this.dataHandler, this);
        this.repository = new VoteRepository();
        this.ballotRepository = new BallotRepository();
    }

    @Override
    protected void loadModule(final FileConfig config) {
        this.settings.load(config);

        this.plugin.injectLang(VotingLang.class);

        this.dataManager.init(this.settings.dataTablePrefix.get());

        this.loadMenus();
        this.loadData();
        this.loadTasks();
        this.addListener(new VotingListener(this.plugin, this));

        this.commandRegistry.addProvider(new VotingCastCommandProvider(this));
        this.commandRegistry.addProvider(new VotingStaffCommandProvider(this));
    }

    @Override
    protected void unloadModule() {
        this.repository.clear();
        this.ballotRepository.clear();
        this.dataLoaded = false;
        this.menu = null;
        this.viewMenu = null;
    }

    // -- Loading ----------------------------------------------------------------------------------

    private void loadMenus() {
        this.menu = new VotesMenu(this);
        this.menu.load(this.plugin, FileConfig.load(this.getLocalUIPath(), "votes.yml"));

        this.viewMenu = new VoteViewMenu(this);
        this.viewMenu.load(this.plugin, FileConfig.load(this.getLocalUIPath(), "vote-view.yml"));
    }

    /**
     * Loads everything off-thread. Choices are indexed by vote id rather than pushed onto a poll,
     * so a row order where a choice arrives before its poll still resolves correctly.
     */
    private void loadData() {
        this.dataLoaded = false;
        this.plugin.runTaskAsync(task -> {
            this.dataManager.getVotes().forEach(this.repository::upsertVote);
            this.dataManager.getOptions().forEach(row -> this.repository.upsertOption(row.option(), row.voteId()));
            this.dataManager.getBallots().forEach(this.ballotRepository::upsert);
            this.dataLoaded = true;
        });
    }

    private void loadTasks() {
        final int seconds = this.settings.sweepSeconds.get();
        if (seconds > 0) {
            this.addAsyncTask(this::sweep, seconds);
        }
        final int remind = this.settings.remindSeconds.get();
        if (remind > 0) {
            this.addAsyncTask(this::remindUnvoted, remind);
        }
    }

    /**
     * Ends polls whose time is up, opens scheduled polls whose time has come,
     * and clears out old history. Runs off-thread, so anything
     * Bukkit-facing hops back to the main thread.
     */
    private void sweep() {
        if (!this.dataLoaded) {
            return;
        }

        this.dataManager.purgeFinished(this.settings.keepConcludedDays.get());

        final List<Vote> due = this.repository.getScheduledVotes().stream()
                .filter(Vote::isStartDue)
                .toList();
        if (!due.isEmpty()) {
            this.plugin.runTask(task -> due.forEach(this::activateScheduled));
        }

        final List<Vote> expired = this.repository.getOpenVotes().stream()
                .filter(Vote::isExpired)
                .toList();
        if (expired.isEmpty()) {
            return;
        }

        this.plugin.runTask(task -> expired.forEach(vote -> this.conclude(vote, false, null)));
    }

    private void activateScheduled(final Vote vote) {
        if (!vote.isScheduled()) {
            return;
        }
        vote.open(System.currentTimeMillis());
        this.repository.applyTransition(vote);
        this.dataManager.updateVote(vote);
        if (this.settings.notifyScheduled.get()) {
            this.announce(vote);
        }
    }

    /**
     * Re-announces every open vote to the players who have not voted in it yet.
     * Runs off-thread on its own interval; delivery hops to the main thread.
     */
    private void remindUnvoted() {
        if (!this.dataLoaded) {
            return;
        }
        final List<Vote> open = this.repository.getOpenVotes();
        if (open.isEmpty()) {
            return;
        }
        this.plugin.runTask(task -> {
            for (final Vote vote : open) {
                if (!vote.isOpen()) {
                    continue;
                }
                for (final Player online : Bukkit.getOnlinePlayers()) {
                    if (this.ballotRepository.hasVoted(vote.getId(), online.getUniqueId())) {
                        continue;
                    }
                    final String permission = this.settings.notifyPermission.get();
                    if (permission != null && !permission.isBlank()
                            && !online.hasPermission(permission)) {
                        continue;
                    }
                    this.sendPrefixed(VotingLang.REMINDER, online, b -> b.with(vote.placeholders()));
                }
            }
        });
    }

    public void nudgeOnJoin(final Player player) {
        if (!this.dataLoaded || !this.settings.notifyOnJoin.get()) {
            return;
        }
        final List<Vote> pending = this.repository.getOpenVotes().stream()
                .filter(vote -> !this.ballotRepository.hasVoted(vote.getId(), player.getUniqueId()))
                .toList();
        if (pending.isEmpty()) {
            return;
        }
        final String permission = this.settings.notifyPermission.get();
        if (permission != null && !permission.isBlank() && !player.hasPermission(permission)) {
            return;
        }
        this.sendPrefixed(VotingLang.JOIN_NUDGE, player, b -> b
                .with(SLPlaceholders.GENERIC_AMOUNT, () -> String.valueOf(pending.size()))
                .with(SLPlaceholders.GENERIC_LIST,
                        () -> String.join(", ", pending.stream().map(Vote::getKey).toList())));
    }

    // -- Accessors --------------------------------------------------------------------------------

    public VotingSettings settings() {
        return this.settings;
    }

    public VoteRepository getRepository() {
        return this.repository;
    }

    public BallotRepository getBallotRepository() {
        return this.ballotRepository;
    }

    public VotingDataManager getDataManager() {
        return this.dataManager;
    }

    public boolean isDataLoaded() {
        return this.dataLoaded;
    }

    /** Recomputed on demand; nothing here is worth caching against a poll that can change at any time. */
    public VoteTally tally(final Vote vote) {
        return new VoteTally(this.ballotRepository.getBallots(vote.getId()), vote.getRules());
    }

    public @Nullable Vote findVote(final String key) {
        return this.repository.getVote(key);
    }

    // -- Placeholders -----------------------------------------------------------------------------

    @Override
    public void registerPlaceholders(final PlaceholderRegistry registry) {
        // Counters are prefixed 'count_' for the reason the Reports module does the same: the
        // registry resolves the longest matching key and hands the remainder on as a payload, so
        // a key like 'voting_active' would swallow '%sunlight_voting_active_<key>%' and answer
        // with the global number instead of failing.
        registry.register("voting_count_active",
                (player, payload) -> String.valueOf(this.repository.getOpenCount()));
        registry.register("voting_count_total",
                (player, payload) -> String.valueOf(this.repository.getVotes().size()));
        registry.register("voting_count_scheduled",
                (player, payload) -> String.valueOf(this.repository.getScheduledVotes().size()));
        registry.register("voting_count_finished",
                (player, payload) -> String.valueOf(this.repository.getFinishedVotes().size()));

        // What this player has voted on, taken from their most recent open poll — the one a
        // scoreboard or chat line is almost always talking about.
        registry.register("voting_name", (player, payload) -> this.latest(player, Vote::getDisplay));
        registry.register("voting_key", (player, payload) -> this.latest(player, Vote::getKey));
        registry.register("voting_status",
                (player, payload) -> this.latest(player, vote -> VotingLang.STATUS.getLocalized(vote.getStatus())));
        registry.register("voting_option", (player, payload) -> this.latestOptions(player));
        registry.register("voting_weight", (player, payload) -> this.latestWeight(player));

        // Per-poll figures, payload = vote key. An unknown key yields "" rather than null so a
        // scoreboard shows a blank instead of a raw placeholder.
        registry.register("voting_time_left", (player, payload) -> this.field(payload,
                vote -> TimeFormats.formatDuration(vote.getEndDate(), TimeFormatType.LITERAL)));
        registry.register("voting_time_end", (player, payload) -> this.field(payload,
                vote -> TimeFormats.formatDateTime(vote.getEndDate())));
        registry.register("voting_time_to_start", (player, payload) -> this.field(payload,
                vote -> vote.isScheduled()
                        ? TimeFormats.formatDuration(vote.getStartDate(), TimeFormatType.LITERAL)
                        : ""));
        registry.register("voting_participants", (player, payload) -> this.field(payload,
                vote -> this.exposed(vote) ? String.valueOf(this.tally(vote).getParticipantCount())
                        : VotingLang.SECRET_HIDDEN.text()));
        registry.register("voting_total_score", (player, payload) -> this.field(payload,
                vote -> this.exposed(vote) ? format(this.tally(vote).getTotalScore())
                        : VotingLang.SECRET_HIDDEN.text()));
        registry.register("voting_creator", (player, payload) -> this.field(payload,
                vote -> vote.getCreatorName() == null ? "" : vote.getCreatorName()));
        registry.register("voting_description", (player, payload) -> this.field(payload, Vote::getDescription));
        registry.register("voting_desc", (player, payload) -> this.field(payload, Vote::getDescription));
        registry.register("voting_status_of", (player, payload) -> this.field(payload,
                vote -> VotingLang.STATUS.getLocalized(vote.getStatus())));
        registry.register("voting_outcome", (player, payload) -> this.field(payload, vote -> {
            final VoteOption outcome = vote.getOutcomeOption();
            return outcome == null ? "" : outcome.getDisplay();
        }));
        registry.register("voting_outcome_key", (player, payload) -> this.field(payload,
                vote -> vote.getOutcome() == null ? "" : vote.getOutcome()));
        registry.register("voting_options", (player, payload) -> this.field(payload,
                vote -> String.join(", ",
                        vote.getOptionList().stream().map(VoteOption::getDisplay).toList())));
        registry.register("voting_secret", (player, payload) -> this.field(payload,
                vote -> VotingLang.STATE_YES_NO.get(vote.getRules().isSecretBallot())));
        registry.register("voting_leader", (player, payload) -> {
            final Vote vote = this.repository.getVote(payload);
            if (vote == null) {
                return "";
            }
            if (!this.exposed(vote)) {
                return VotingLang.SECRET_HIDDEN.text();
            }
            final VoteOption leader = vote.getOption(this.tally(vote).leader());
            return leader == null ? "" : leader.getDisplay();
        });
        registry.register("voting_leader_key", (player, payload) -> {
            final Vote vote = this.repository.getVote(payload);
            if (vote == null || !this.exposed(vote)) {
                return vote == null ? "" : VotingLang.SECRET_HIDDEN.text();
            }
            final String leader = this.tally(vote).leader();
            return leader == null ? "" : leader;
        });
        registry.register("voting_leader_score", (player, payload) -> {
            final Vote vote = this.repository.getVote(payload);
            if (vote == null) {
                return "";
            }
            if (!this.exposed(vote)) {
                return VotingLang.SECRET_HIDDEN.text();
            }
            final String leader = this.tally(vote).leader();
            return leader == null ? "0" : format(this.tally(vote).score(leader));
        });
        registry.register("voting_leader_share", (player, payload) -> {
            final Vote vote = this.repository.getVote(payload);
            if (vote == null) {
                return "";
            }
            if (!this.exposed(vote)) {
                return VotingLang.SECRET_HIDDEN.text();
            }
            final String leader = this.tally(vote).leader();
            return leader == null ? "0" : format(this.tally(vote).share(leader));
        });
        registry.register("voting_has_voted", (player, payload) -> {
            final Vote vote = this.repository.getVote(payload);
            if (vote == null || player == null) {
                return "";
            }
            return VotingLang.STATE_YES_NO.get(
                    this.ballotRepository.hasVoted(vote.getId(), player.getUniqueId()));
        });
        registry.register("voting_mine", (player, payload) -> {
            final Vote vote = this.repository.getVote(payload);
            if (vote == null || player == null) {
                return "";
            }
            return this.ballotRepository.getBallots(vote.getId(), player.getUniqueId()).stream()
                    .map(ballot -> vote.getOption(ballot.optionKey()))
                    .filter(Objects::nonNull)
                    .map(VoteOption::getDisplay)
                    .reduce((first, second) -> first + ", " + second)
                    .orElse("");
        });

        // Headcount per built-in choice, payload = vote key. The *_score and *_share variants
        // carry the weighted figures, because on a weighted poll the headcount alone misleads.
        registry.register("voting_option_yes", (player, payload) -> this.optionField(payload, VoteOption.YES));
        registry.register("voting_option_no", (player, payload) -> this.optionField(payload, VoteOption.NO));
        registry.register("voting_option_neutral", (player, payload) -> this.optionField(payload, VoteOption.NEUTRAL));
        registry.register("voting_option_yes_score",
                (player, payload) -> this.optionScore(payload, VoteOption.YES));
        registry.register("voting_option_no_score",
                (player, payload) -> this.optionScore(payload, VoteOption.NO));
        registry.register("voting_option_neutral_score",
                (player, payload) -> this.optionScore(payload, VoteOption.NEUTRAL));
        registry.register("voting_option_yes_share",
                (player, payload) -> this.optionShare(payload, VoteOption.YES));
        registry.register("voting_option_no_share",
                (player, payload) -> this.optionShare(payload, VoteOption.NO));
        registry.register("voting_option_neutral_share",
                (player, payload) -> this.optionShare(payload, VoteOption.NEUTRAL));

        // Generic per-choice figures for custom options, payload = <voteKey>:<optionKey>.
        // The vote is resolved by longest-prefix match so vote keys may contain underscores.
        registry.register("voting_option_votes", (player, payload) -> this.genericOption(payload,
                (vote, tally, option) -> String.valueOf(tally.count(option.getKey()))));
        registry.register("voting_option_count", (player, payload) -> this.genericOption(payload,
                (vote, tally, option) -> String.valueOf(tally.count(option.getKey()))));
        registry.register("voting_option_score", (player, payload) -> this.genericOption(payload,
                (vote, tally, option) -> format(tally.score(option.getKey()))));
        registry.register("voting_option_share", (player, payload) -> this.genericOption(payload,
                (vote, tally, option) -> format(tally.share(option.getKey()))));
    }

    private boolean exposed(final Vote vote) {
        return vote != null && vote.isExposed();
    }

    /**
     * Resolves a {@code <voteKey>:<optionKey>} payload. The vote key may itself contain
     * underscores, so the vote is matched by longest prefix: every known vote key that the
     * payload starts with is a candidate and the longest one wins.
     */
    private String genericOption(final String payload,
            final OptionExtractor extractor) {
        if (payload == null || payload.isBlank()) {
            return "";
        }
        final int separator = payload.lastIndexOf(':');
        if (separator <= 0 || separator >= payload.length() - 1) {
            return "";
        }
        final String voteKey = payload.substring(0, separator).toLowerCase(java.util.Locale.ROOT);
        final String optionKey = payload.substring(separator + 1);
        final Vote vote = this.repository.getVote(voteKey);
        if (vote == null) {
            return "";
        }
        final VoteOption option = vote.getOption(optionKey);
        if (option == null) {
            return "";
        }
        if (!this.exposed(vote)) {
            return VotingLang.SECRET_HIDDEN.text();
        }
        return extractor.extract(vote, this.tally(vote), option);
    }

    private interface OptionExtractor {
        String extract(Vote vote, VoteTally tally, VoteOption option);
    }

    private String field(final String voteKey, final Function<Vote, String> extractor) {
        final Vote vote = this.repository.getVote(voteKey);
        return vote == null ? "" : extractor.apply(vote);
    }

    private String optionField(final String voteKey, final String optionKey) {
        final Vote vote = this.repository.getVote(voteKey);
        if (vote == null || !vote.hasOption(optionKey)) {
            return "";
        }
        if (!vote.isExposed()) {
            return VotingLang.SECRET_HIDDEN.text();
        }
        return String.valueOf(this.tally(vote).count(optionKey));
    }

    private String optionScore(final String voteKey, final String optionKey) {
        final Vote vote = this.repository.getVote(voteKey);
        if (vote == null || !vote.hasOption(optionKey)) {
            return "";
        }
        if (!vote.isExposed()) {
            return VotingLang.SECRET_HIDDEN.text();
        }
        return format(this.tally(vote).score(optionKey));
    }

    private String optionShare(final String voteKey, final String optionKey) {
        final Vote vote = this.repository.getVote(voteKey);
        if (vote == null || !vote.hasOption(optionKey)) {
            return "";
        }
        if (!vote.isExposed()) {
            return VotingLang.SECRET_HIDDEN.text();
        }
        return format(this.tally(vote).share(optionKey));
    }

    /**
     * @return the open poll the player voted in most recently. The open set has no inherent
     *         order, so "latest" is defined by the newest ballot date — reducing over the raw
     *         collection order would make the placeholders nondeterministic with several polls.
     */
    private @Nullable Vote latestVote(final Player player) {
        if (player == null) {
            return null;
        }
        Vote latest = null;
        long latestDate = Long.MIN_VALUE;
        for (final Vote vote : this.repository.getOpenVotes()) {
            long newest = Long.MIN_VALUE;
            boolean voted = false;
            for (final VoteBallot ballot : this.ballotRepository.getBallots(vote.getId(), player.getUniqueId())) {
                voted = true;
                newest = Math.max(newest, ballot.date());
            }
            if (voted && (latest == null || newest > latestDate
                    || (newest == latestDate && vote.getCreateDate() > latest.getCreateDate()))) {
                latest = vote;
                latestDate = newest;
            }
        }
        return latest;
    }

    private String latest(final Player player, final Function<Vote, String> extractor) {
        final Vote vote = this.latestVote(player);
        return vote == null ? "" : extractor.apply(vote);
    }

    private String latestOptions(final Player player) {
        final Vote vote = this.latestVote(player);
        if (vote == null) {
            return "";
        }
        return this.ballotRepository.getBallots(vote.getId(), player.getUniqueId()).stream()
                .map(ballot -> vote.getOption(ballot.optionKey()))
                .filter(Objects::nonNull)
                .map(VoteOption::getDisplay)
                .reduce((first, second) -> first + ", " + second)
                .orElse("");
    }

    private String latestWeight(final Player player) {
        final Vote vote = this.latestVote(player);
        if (vote == null) {
            return "";
        }
        final double total = this.ballotRepository.getBallots(vote.getId(), player.getUniqueId()).stream()
                .mapToDouble(VoteBallot::weight)
                .sum();
        return total > 0D ? format(total) : "";
    }

    // -- Creation ---------------------------------------------------------------------------------

    public @Nullable Vote create(final CommandSender creator, final String keyRaw, final String displayRaw,
            final int durationSeconds) {
        return this.create(creator, keyRaw, displayRaw, "", durationSeconds, 0);
    }

    public @Nullable Vote create(final CommandSender creator, final String keyRaw, final String displayRaw,
            final String descriptionRaw, final int durationSeconds, final int startDelaySeconds) {
        final String key = keyRaw == null ? "" : keyRaw.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) {
            this.sendPrefixed(VotingLang.ERROR_UNKNOWN_VOTE, creator,
                    b -> b.with(SLPlaceholders.GENERIC_TARGET, () -> keyRaw == null ? "" : keyRaw.trim()));
            return null;
        }
        if (this.repository.containsKey(key) || this.dataManager.existsKey(key)) {
            this.sendPrefixed(VotingLang.ERROR_DUPLICATE_KEY, creator,
                    b -> b.with(SLPlaceholders.GENERIC_TARGET, () -> key));
            return null;
        }

        final int max = this.settings.maxActiveVotes.get();
        if (max > 0 && this.repository.getActiveCount() >= max) {
            this.sendPrefixed(VotingLang.ERROR_LIMIT_GLOBAL, creator,
                    b -> b.with(SLPlaceholders.GENERIC_MAX, () -> String.valueOf(max)));
            return null;
        }

        final UUID creatorId = creator instanceof Player player ? player.getUniqueId() : null;
        final int perCreator = this.settings.maxVotesPerCreator.get();
        if (creatorId != null && perCreator > 0 && this.repository.getOpenCount(creatorId) >= perCreator) {
            this.sendPrefixed(VotingLang.ERROR_LIMIT_CREATOR, creator,
                    b -> b.with(SLPlaceholders.GENERIC_MAX, () -> String.valueOf(perCreator)));
            return null;
        }

        final Collection<VoteOption> options = this.buildDefaultOptions();

        final Vote vote = Vote.create(key,
                displayRaw == null || displayRaw.isBlank() ? key : displayRaw.trim(),
                descriptionRaw == null ? "" : descriptionRaw.trim(),
                creatorId, creator.getName(), durationSeconds, Math.max(0, startDelaySeconds), options,
                this.buildDefaultRules());

        final VoteCreateEvent event = new VoteCreateEvent(vote);
        this.plugin.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return null;
        }

        this.dataManager.insertVote(vote);
        options.forEach(option -> this.dataManager.insertOption(vote.getId(), option));
        this.repository.upsertVote(vote);
        options.forEach(option -> this.repository.upsertOption(option, vote.getId()));

        this.sendPrefixed(VotingLang.CREATED, creator, b -> b.with(vote.placeholders()));
        if (vote.isScheduled()) {
            // Scheduled polls announce on opening, not on creation, so players are not
            // asked to vote in something they cannot vote in yet.
            return vote;
        }
        if (this.settings.announceOnCreate.get()) {
            this.announce(vote);
        }
        return vote;
    }

    private Collection<VoteOption> buildDefaultOptions() {
        final Collection<String> enabled = this.settings.builtInOptions.get().stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        final List<VoteOption> options = new ArrayList<>(enabled.size());

        if (enabled.contains(VoteOption.YES)) {
            options.add(new VoteOption(VoteOption.YES, "Yes", this.settings.iconYes.get()));
        }
        if (enabled.contains(VoteOption.NO)) {
            options.add(new VoteOption(VoteOption.NO, "No", this.settings.iconNo.get()));
        }
        if (enabled.contains(VoteOption.NEUTRAL)) {
            options.add(new VoteOption(VoteOption.NEUTRAL, "Neutral", this.settings.iconNeutral.get()));
        }

        // A poll with every built-in choice switched off still needs something to vote on. Rather
        // than refuse the creation, fall back to a bare yes/no pair.
        if (options.isEmpty()) {
            options.add(new VoteOption(VoteOption.YES, "Yes", this.settings.iconYes.get()));
            options.add(new VoteOption(VoteOption.NO, "No", this.settings.iconNo.get()));
        }
        return options;
    }

    /**
     * @return the server-wide defaults as a fresh rule set, so editing one poll can never write
     *         through into another poll or back into the config.
     */
    private VoteRules buildDefaultRules() {
        final VoteRules rules = new VoteRules();
        rules.setMultiVote(this.settings.multiVote.get());
        rules.setMaxVotes(this.settings.maxVotes.get());
        rules.setMinPlaytimeHours(this.settings.minPlaytimeHours.get());
        rules.setWorldMode(this.settings.worldMode.get());
        rules.setWorlds(this.settings.worlds.get());
        rules.setMoneyMode(this.settings.moneyMode.get());
        rules.setMinBalance(this.settings.minBalance.get());
        rules.setMaxBalance(this.settings.maxBalance.get());
        rules.setOverMaxMode(this.settings.overMaxMode.get());
        rules.setCurrencyPerWeight(this.settings.currencyPerWeight.get());
        rules.setMaxMoneyWeight(this.settings.maxMoneyWeight.get());
        rules.setWeightingEnabled(this.settings.weightingEnabled.get());
        rules.setWeightSource(this.settings.weightSource.get());
        rules.setWeightCombine(this.settings.weightCombine.get());
        rules.setBaseWeight(this.settings.baseWeight.get());
        rules.setMaxMultiplier(this.settings.maxMultiplier.get());
        rules.setMaxWeight(this.settings.maxWeight.get());
        rules.setMinParticipants(this.settings.minParticipants.get());
        rules.setPassPercent(this.settings.passPercent.get());
        rules.setNeutralCountsTowardTotal(this.settings.neutralCounts.get());
        rules.setSecretBallot(this.settings.secretBallot.get());
        rules.setTieBreak(this.settings.tieBreak.get());
        rules.setAllowChange(this.settings.allowChange.get());
        rules.setChangeLockSeconds(this.settings.changeLockSeconds.get());
        rules.setRunoffEnabled(this.settings.runoffEnabled.get());
        rules.setRunoffTopN(this.settings.runoffTopN.get());
        rules.setRunoffDurationSeconds(this.settings.runoffDurationSeconds.get());
        return rules;
    }

    public VoteRules defaultRules() {
        return this.buildDefaultRules();
    }

    // -- Choices ----------------------------------------------------------------------------------

    public boolean addOption(final CommandSender sender, final Vote vote, final String keyRaw,
            final String displayRaw, final String iconRaw) {
        if (!this.requireEditable(sender, vote)) {
            return false;
        }
        if (keyRaw == null || keyRaw.isBlank()) {
            this.sendPrefixed(VotingLang.ERROR_UNKNOWN_OPTION, sender, b -> b
                    .with(vote.placeholders())
                    .with(SLPlaceholders.GENERIC_TARGET, () -> "")
                    .with(SLPlaceholders.GENERIC_LIST, () -> String.join(", ", vote.getOptionKeys())));
            return false;
        }
        final String key = keyRaw.trim().toLowerCase(Locale.ROOT);
        if (vote.hasOption(key)) {
            this.sendPrefixed(VotingLang.ERROR_OPTION_EXISTS, sender, b -> b
                    .with(vote.placeholders())
                    .with(SLPlaceholders.GENERIC_TARGET, () -> key));
            return false;
        }

        final int max = this.settings.maxOptions.get();
        if (max > 0 && vote.getOptions().size() >= max) {
            this.sendPrefixed(VotingLang.ERROR_TOO_MANY_OPTIONS, sender, b -> b
                    .with(vote.placeholders())
                    .with(SLPlaceholders.GENERIC_MAX, () -> String.valueOf(max)));
            return false;
        }

        final VoteOption option = new VoteOption(key, displayRaw,
                iconRaw == null || iconRaw.isBlank() ? this.settings.iconDefault.get() : iconRaw.trim());

        this.dataManager.insertOption(vote.getId(), option);
        vote.addOption(option);
        this.repository.upsertOption(option, vote.getId());

        this.sendPrefixed(VotingLang.OPTION_ADDED, sender, b -> b
                .with(vote.placeholders())
                .with(option.placeholders()));
        return true;
    }

    /**
     * @param force When true, ballots on the removed choice are moved to neutral rather than the
     *              removal being refused.
     */
    public boolean removeOption(final CommandSender sender, final Vote vote, final String optionKeyRaw,
            final boolean force) {
        if (!this.requireEditable(sender, vote)) {
            return false;
        }
        final VoteOption option = vote.getOption(optionKeyRaw);
        if (option == null) {
            this.reportUnknownOption(sender, vote, optionKeyRaw);
            return false;
        }
        if (option.isBuiltIn()) {
            this.sendPrefixed(VotingLang.ERROR_OPTION_BUILTIN, sender,
                    b -> b.with(SLPlaceholders.GENERIC_TARGET, option::getKey));
            return false;
        }
        if (vote.getOptions().size() <= this.settings.minOptions.get()) {
            this.sendPrefixed(VotingLang.ERROR_LAST_OPTION, sender);
            return false;
        }

        final List<VoteBallot> ballots = this.ballotRepository.getBallots(vote.getId()).stream()
                .filter(ballot -> ballot.optionKey().equalsIgnoreCase(option.getKey()))
                .toList();
        if (!ballots.isEmpty() && !force) {
            this.sendPrefixed(VotingLang.ERROR_OPTION_IN_USE, sender,
                    b -> b.with(SLPlaceholders.GENERIC_TARGET, option::getKey));
            return false;
        }

        this.moveBallots(vote, option, ballots);

        this.dataManager.deleteOption(vote.getId(), option.getKey());
        vote.removeOption(option.getKey());
        this.repository.removeOption(vote.getId(), option.getKey());

        this.sendPrefixed(VotingLang.OPTION_REMOVED, sender, b -> b
                .with(vote.placeholders())
                .with(SLPlaceholders.GENERIC_TARGET, option::getKey));
        if (!ballots.isEmpty()) {
            this.sendPrefixed(VotingLang.OPTION_MOVED, sender,
                    b -> b.with(SLPlaceholders.GENERIC_TARGET, option::getKey));
        }
        return true;
    }

    /**
     * Re-homes ballots onto the poll's neutral choice, refunding whatever they paid.
     * <p>
     * Dropping the ballots would silently strip those players of their say, and dropping the
     * choice without a home for them would invalidate a vote they had no way to change.
     */
    private void moveBallots(final Vote vote, final VoteOption removed, final List<VoteBallot> ballots) {
        if (ballots.isEmpty()) {
            return;
        }

        final VoteOption neutral = vote.getNeutralOption();
        for (final VoteBallot ballot : ballots) {
            this.refund(ballot);

            this.dataManager.deleteBallot(vote.getId(), ballot.playerId(), ballot.optionKey());
            this.ballotRepository.remove(ballot);

            if (neutral == null) {
                continue;
            }
            final VoteBallot moved = VoteBallot.create(vote.getId(), neutral.getKey(), ballot.playerId(),
                    ballot.playerName(), ballot.weight(), 0D);
            this.dataManager.insertBallot(moved);
            this.ballotRepository.upsert(moved);
        }
    }

    private void reportUnknownOption(final CommandSender sender, final Vote vote, final String optionKeyRaw) {
        this.sendPrefixed(VotingLang.ERROR_UNKNOWN_OPTION, sender, b -> b
                .with(vote.placeholders())
                .with(SLPlaceholders.GENERIC_TARGET, () -> optionKeyRaw)
                .with(SLPlaceholders.GENERIC_LIST, () -> String.join(", ", vote.getOptionKeys())));
    }

    public boolean toggleGate(final CommandSender sender, final Vote vote, final String optionKeyRaw,
            final GateType gate) {
        if (!this.requireEditable(sender, vote)) {
            return false;
        }
        final VoteOption option = vote.getOption(optionKeyRaw);
        if (option == null) {
            this.reportUnknownOption(sender, vote, optionKeyRaw);
            return false;
        }
        if (!this.settings.availableGates.get().contains(gate)) {
            this.sendPrefixed(VotingLang.ERROR_GATE_UNAVAILABLE, sender,
                    b -> b.with(SLPlaceholders.GENERIC_TARGET, () -> gate.name()));
            return false;
        }

        option.toggleGate(gate);
        this.dataManager.updateOption(vote.getId(), option);

        this.sendPrefixed(VotingLang.GATE_SET, sender, b -> b
                .with(vote.placeholders())
                .with(option.placeholders())
                .with(SLPlaceholders.GENERIC_LIST,
                        () -> option.getGates().stream().map(Enum::name).reduce((a, c) -> a + ", " + c).orElse("none")));
        return true;
    }

    public void saveOption(final VoteOption option) {
        if (option == null) {
            return;
        }
        final Vote vote = this.repository.getVotes().stream()
                .filter(candidate -> candidate.getOption(option.getKey()) == option)
                .findFirst()
                .orElse(null);
        // Silently skips closed polls: rule edits must never rewrite a decided result.
        if (vote != null && (vote.isOpen() || vote.isScheduled())) {
            this.dataManager.updateOption(vote.getId(), option);
        }
    }

    public void saveRules(final CommandSender sender, final Vote vote) {
        if (!this.requireEditable(sender, vote)) {
            return;
        }
        this.dataManager.updateVote(vote);
        this.sendPrefixed(VotingLang.RULES_SET, sender, b -> b.with(vote.placeholders()));
    }

    // -- Lifecycle --------------------------------------------------------------------------------

    public boolean extend(final CommandSender sender, final Vote vote, final int seconds) {
        if (!vote.isOpen() && !vote.isScheduled()) {
            this.sendPrefixed(VotingLang.ERROR_NOT_OPEN, sender, b -> b.with(vote.placeholders()));
            return false;
        }
        final long millis = TimeUnit.SECONDS.toMillis(seconds);
        if (vote.isScheduled()) {
            // A scheduled poll has not started counting down yet, so pushing its end
            // out from now would silently shorten it. Extend from its planned end.
            vote.setEndDate(vote.getEndDate() + millis);
        } else {
            vote.setEndDate(System.currentTimeMillis() + millis);
        }
        this.dataManager.updateVote(vote);
        this.sendPrefixed(VotingLang.EXTENDED, sender, b -> b.with(vote.placeholders()));
        return true;
    }

    public boolean close(final CommandSender sender, final Vote vote) {
        if (!this.requireOpen(sender, vote)) {
            return false;
        }
        this.conclude(vote, true, sender);
        this.sendPrefixed(VotingLang.CLOSED, sender, b -> b.with(vote.placeholders()));
        return true;
    }

    public boolean cancel(final CommandSender sender, final Vote vote) {
        if (!vote.isOpen() && !vote.isScheduled()) {
            this.sendPrefixed(VotingLang.ERROR_NOT_OPEN, sender, b -> b.with(vote.placeholders()));
            return false;
        }
        // Deliberately not routed through conclude(): a staff member stopping a poll is not the
        // same act as deciding it, and going through the resolver would announce a winner for a
        // poll somebody just decided to kill.
        this.finish(vote, null, VoteStatus.CANCELLED, true, this.tally(vote), VotingLang.RESULT_CANCELLED);
        this.sendPrefixed(VotingLang.CANCELLED, sender, b -> b.with(vote.placeholders()));
        return true;
    }

    public boolean delete(final CommandSender sender, final Vote vote) {
        // Only an open poll can still owe refunds. A concluded poll already refunded its losers
        // in finish(), and refunding again here would pay every paid ballot a second time.
        if (vote.isOpen()) {
            this.refundAll(vote);
        }
        this.dataManager.deleteVote(vote);
        this.repository.removeVote(vote);
        this.ballotRepository.removeVote(vote.getId());
        this.sendPrefixed(VotingLang.DELETED, sender, b -> b.with(vote.placeholders()));
        return true;
    }

    public boolean rename(final CommandSender sender, final Vote vote, final String displayRaw) {
        if (displayRaw == null || displayRaw.isBlank()) {
            return false;
        }
        vote.setDisplay(displayRaw.trim());
        this.dataManager.updateVote(vote);
        this.sendPrefixed(VotingLang.RENAMED, sender, b -> b
                .with(vote.placeholders())
                .with(SLPlaceholders.GENERIC_TARGET, vote::getDisplay));
        return true;
    }

    public boolean describe(final CommandSender sender, final Vote vote, final String descriptionRaw) {
        vote.setDescription(descriptionRaw == null ? "" : descriptionRaw.trim());
        this.dataManager.updateVote(vote);
        this.sendPrefixed(VotingLang.DESCRIBED, sender, b -> b.with(vote.placeholders()));
        return true;
    }

    public boolean startNow(final CommandSender sender, final Vote vote) {
        if (!vote.isScheduled()) {
            this.sendPrefixed(VotingLang.ERROR_NOT_OPEN, sender, b -> b.with(vote.placeholders()));
            return false;
        }
        final int max = this.settings.maxActiveVotes.get();
        if (max > 0 && this.repository.getOpenCount() >= max) {
            this.sendPrefixed(VotingLang.ERROR_LIMIT_GLOBAL, sender,
                    b -> b.with(SLPlaceholders.GENERIC_MAX, () -> String.valueOf(max)));
            return false;
        }
        this.activateScheduled(vote);
        this.sendPrefixed(VotingLang.STARTED, sender, b -> b.with(vote.placeholders()));
        return true;
    }

    /**
     * Copies a poll's display, description, choices and rules under a new key.
     * Ballots never carry over: the clone is a fresh question, not a reopened count.
     */
    public @Nullable Vote cloneVote(final CommandSender sender, final Vote source, final String keyRaw) {
        final String key = keyRaw == null ? "" : keyRaw.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) {
            this.sendPrefixed(VotingLang.ERROR_UNKNOWN_VOTE, sender,
                    b -> b.with(SLPlaceholders.GENERIC_TARGET, () -> keyRaw == null ? "" : keyRaw.trim()));
            return null;
        }
        if (this.repository.containsKey(key) || this.dataManager.existsKey(key)) {
            this.sendPrefixed(VotingLang.ERROR_DUPLICATE_KEY, sender,
                    b -> b.with(SLPlaceholders.GENERIC_TARGET, () -> key));
            return null;
        }

        final int max = this.settings.maxActiveVotes.get();
        if (max > 0 && this.repository.getActiveCount() >= max) {
            this.sendPrefixed(VotingLang.ERROR_LIMIT_GLOBAL, sender,
                    b -> b.with(SLPlaceholders.GENERIC_MAX, () -> String.valueOf(max)));
            return null;
        }

        final List<VoteOption> options = new ArrayList<>();
        for (final VoteOption from : source.getOptionList()) {
            final VoteOption copy = new VoteOption(from.getKey(), from.getDisplay(), from.getIcon());
            copy.setGates(from.getGates());
            copy.setCost(from.getCost());
            copy.setPermission(from.getPermission());
            copy.setRank(from.getRank());
            copy.setWorlds(from.getWorlds());
            copy.setMinPlaytimeHours(from.getMinPlaytimeHours());
            options.add(copy);
        }
        if (options.size() < this.settings.minOptions.get()) {
            this.sendPrefixed(VotingLang.ERROR_LAST_OPTION, sender);
            return null;
        }

        final UUID creatorId = sender instanceof Player player ? player.getUniqueId() : source.getCreatorId();
        final long sourceLength = Math.max(1000L, source.getEndDate() - source.getStartDate());
        final Vote vote = Vote.create(key, source.getDisplay(), source.getDescription(), creatorId,
                sender.getName(), (int) Math.min(sourceLength / 1000L, Integer.MAX_VALUE), 0, options,
                source.getRules().copy());

        final VoteCreateEvent event = new VoteCreateEvent(vote);
        this.plugin.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return null;
        }

        this.dataManager.insertVote(vote);
        options.forEach(option -> this.dataManager.insertOption(vote.getId(), option));
        this.repository.upsertVote(vote);
        options.forEach(option -> this.repository.upsertOption(option, vote.getId()));

        this.sendPrefixed(VotingLang.CLONED, sender, b -> b
                .with(source.placeholders())
                .with(SLPlaceholders.GENERIC_TARGET, vote::getKey));
        if (this.settings.announceOnCreate.get()) {
            this.announce(vote);
        }
        return vote;
    }

    private boolean requireOpen(final CommandSender sender, final Vote vote) {
        if (vote.isOpen()) {
            return true;
        }
        this.sendPrefixed(VotingLang.ERROR_NOT_OPEN, sender, b -> b.with(vote.placeholders()));
        return false;
    }

    /**
     * @return whether staff may still reconfigure the poll. Scheduled polls are editable
     *         like open ones: they have not taken any votes yet, so nothing decided can
     *         be rewritten by a rule change.
     */
    private boolean requireEditable(final CommandSender sender, final Vote vote) {
        if (vote.isOpen() || vote.isScheduled()) {
            return true;
        }
        this.sendPrefixed(VotingLang.ERROR_NOT_OPEN, sender, b -> b.with(vote.placeholders()));
        return false;
    }

    /**
     * Pays back one ballot. Offline players are paid by UUID rather than skipped: the ballot rows
     * outlive the session, so "they logged off" must not mean "they lose the money". Only the
     * chat notice requires them to be online.
     */
    private void refund(final VoteBallot ballot) {
        if (ballot == null || ballot.costPaid() <= 0D) {
            return;
        }
        EconomyUtils.deposit(ballot.playerId(), ballot.costPaid());
        final Player player = Bukkit.getPlayer(ballot.playerId());
        if (player != null) {
            this.sendPrefixed(VotingLang.VOTE_REFUNDED, player,
                    b -> b.with(SLPlaceholders.GENERIC_AMOUNT, () -> EconomyUtils.format(ballot.costPaid())));
        }
    }

    /**
     * Refunds every paid ballot in a poll.
     * <p>
     * Somebody who paid for a vote that then never happens should get the money back, online or
     * not — the deposit goes through by UUID either way.
     */
    private void refundAll(final Vote vote) {
        for (final VoteBallot ballot : this.ballotRepository.getBallots(vote.getId())) {
            this.refund(ballot);
        }
    }

    /**
     * Decides a poll: cancels it if the quorum was not met, otherwise picks the highest-scoring
     * choice that reached the pass share and announces the result.
     * <p>
     * Tied leaders are resolved by the poll's tie-break rule. A failed poll with runoffs
     * enabled spawns a follow-up vote over its strongest choices instead of simply ending.
     */
    public void conclude(final Vote vote, final boolean manual, final CommandSender actor) {
        if (!vote.isOpen()) {
            return;
        }

        final VoteTally tally = this.tally(vote);
        final int quorum = vote.getRules().getMinParticipants();
        if (quorum > 0 && tally.getParticipantCount() < quorum) {
            this.finish(vote, null, VoteStatus.CANCELLED, manual, tally, VotingLang.RESULT_QUORUM);
            return;
        }

        final List<String> tied = tally.tiedLeaders();
        if (tied.isEmpty()) {
            this.finish(vote, null, VoteStatus.CONCLUDED, manual, tally, VotingLang.RESULT_FAILED);
            this.maybeRunoff(vote, tally);
            return;
        }

        if (tied.size() > 1) {
            final String resolved = this.resolveTie(tied, vote);
            if (resolved == null) {
                this.finish(vote, null, VoteStatus.CONCLUDED, manual, tally, VotingLang.RESULT_FAILED);
                this.announceTie(vote, tally, tied);
                this.maybeRunoff(vote, tally);
                return;
            }
            this.finish(vote, resolved, VoteStatus.CONCLUDED, manual, tally, VotingLang.RESULT_PASSED);
            return;
        }

        this.finish(vote, tied.getFirst(), VoteStatus.CONCLUDED, manual, tally, VotingLang.RESULT_PASSED);
    }

    /**
     * @return the winning key, or {@code null} when the tie means no winner.
     */
    private @Nullable String resolveTie(final List<String> tied, final Vote vote) {
        return switch (vote.getRules().getTieBreak()) {
            case FAIL -> null;
            case RANDOM -> tied.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(tied.size()));
            case FIRST -> tied.getFirst();
        };
    }

    private void announceTie(final Vote vote, final VoteTally tally, final List<String> tied) {
        if (!this.settings.notifyStaffResult.get()) {
            return;
        }
        final String names = tied.stream()
                .map(key -> {
                    final VoteOption option = vote.getOption(key);
                    return option == null ? key : option.getDisplay();
                })
                .reduce((first, second) -> first + ", " + second)
                .orElse("");
        this.broadcastPrefixed(VotingLang.RESULT_TIED, b -> {
            b.with(vote.placeholders());
            b.with(SLPlaceholders.GENERIC_LIST, () -> names);
            b.with(VotingPlaceholders.VOTE_TOTAL_SCORE, () -> format(tally.getTotalScore()));
            b.with(VotingPlaceholders.VOTE_PARTICIPANTS, () -> String.valueOf(tally.getParticipantCount()));
        });
    }

    /**
     * Opens a follow-up poll over the strongest choices of a failed vote.
     * The child copies the parent's rules with runoffs switched off, so a runoff
     * can never spawn a runoff of its own.
     */
    private void maybeRunoff(final Vote vote, final VoteTally tally) {
        if (!vote.getRules().isRunoffEnabled()) {
            return;
        }
        final int top = Math.max(2, vote.getRules().getRunoffTopN());
        final List<String> strongest = tally.topOptions(top).stream()
                .filter(key -> vote.getOption(key) != null)
                .toList();
        if (strongest.size() < 2) {
            return;
        }

        final int max = this.settings.maxActiveVotes.get();
        if (max > 0 && this.repository.getActiveCount() >= max) {
            return;
        }

        String base = vote.getKey() + "-runoff";
        String key = base;
        int suffix = 2;
        while (this.repository.containsKey(key) || this.dataManager.existsKey(key)) {
            key = base + "-" + suffix++;
        }

        final List<VoteOption> options = new ArrayList<>();
        for (final String optionKey : strongest) {
            final VoteOption source = vote.getOption(optionKey);
            if (source == null) {
                continue;
            }
            final VoteOption copy = new VoteOption(source.getKey(), source.getDisplay(), source.getIcon());
            copy.setGates(source.getGates());
            copy.setCost(source.getCost());
            copy.setPermission(source.getPermission());
            copy.setRank(source.getRank());
            copy.setWorlds(source.getWorlds());
            copy.setMinPlaytimeHours(source.getMinPlaytimeHours());
            options.add(copy);
        }
        if (options.size() < 2) {
            return;
        }

        final VoteRules rules = vote.getRules().copy();
        rules.setRunoffEnabled(false);

        final Vote runoff = Vote.create(key, vote.getDisplay() + " (runoff)", vote.getDescription(),
                vote.getCreatorId(), vote.getCreatorName(), vote.getRules().getRunoffDurationSeconds(),
                0, options, rules);

        final VoteCreateEvent event = new VoteCreateEvent(runoff);
        this.plugin.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        this.dataManager.insertVote(runoff);
        options.forEach(option -> this.dataManager.insertOption(runoff.getId(), option));
        this.repository.upsertVote(runoff);
        options.forEach(option -> this.repository.upsertOption(option, runoff.getId()));

        if (this.settings.notifyScheduled.get() || this.settings.announceOnCreate.get()) {
            this.announce(runoff);
        }
        this.broadcastPrefixed(VotingLang.RESULT_RUNOFF, b -> {
            b.with(vote.placeholders());
            b.with(SLPlaceholders.GENERIC_TARGET, runoff::getKey);
        });
        this.dispatchRunoffCommands(runoff);
    }

    private void dispatchRunoffCommands(final Vote runoff) {
        final List<String> commands = this.settings.commandsOnRunoff.get();
        if (commands.isEmpty()) {
            return;
        }
        final PlaceholderContext.Builder builder = PlaceholderContext.builder();
        builder.with(runoff.placeholders());
        final PlaceholderContext context = builder.build();
        final List<String> resolved = new ArrayList<>(commands);
        resolved.replaceAll(context::apply);
        final CommandSender console = this.plugin.getServer().getConsoleSender();
        this.plugin.runTask(task -> resolved.forEach(command -> this.plugin.getServer()
                .dispatchCommand(console, command)));
    }

    /**
     * The single place a poll stops accepting votes: refunds it when nothing won, stores the
     * terminal state, announces the result and runs the matching command set.
     * <p>
     * Whether a refund is owed is decided by the outcome rather than by the status, because a
     * failed poll leaves the payer with nothing for their money just as a cancelled one does. A
     * poll that actually decided something pays out nothing back.
     */
    private void finish(final Vote vote, final @Nullable String outcomeKey, final VoteStatus status,
            final boolean manual, final VoteTally tally, final MessageLocale announcement) {
        // Refunded before the status flips: the refund is a consequence of there being no winner,
        // and a poll already closed is one nobody can still be charged against.
        if (outcomeKey == null) {
            this.refundAll(vote);
        }

        final long now = System.currentTimeMillis();
        if (status == VoteStatus.CANCELLED) {
            vote.cancel(now);
        } else {
            vote.conclude(outcomeKey, now);
        }
        // The vote mutated in place, so the repository's status index still points at OPEN.
        // Without this the finished poll keeps counting as open everywhere: menus, counts,
        // placeholders and the creation caps.
        this.repository.applyTransition(vote);
        this.dataManager.updateVote(vote);

        final VoteOption option = vote.getOption(outcomeKey);
        if (this.settings.notifyStaffResult.get()) {
            this.broadcastPrefixed(announcement, b -> {
                b.with(vote.placeholders());
                if (option != null) {
                    b.with(option.placeholders());
                }
                b.with(VotingPlaceholders.PERCENT,
                        () -> option == null ? "0" : format(tally.share(option.getKey())));
                b.with(VotingPlaceholders.SCORE, () -> option == null ? "0" : format(tally.score(option.getKey())));
                b.with(VotingPlaceholders.COUNT,
                        () -> option == null ? "0" : String.valueOf(tally.count(option.getKey())));
                b.with(VotingPlaceholders.VOTE_TOTAL_SCORE, () -> format(tally.getTotalScore()));
                b.with(VotingPlaceholders.VOTE_PARTICIPANTS, () -> String.valueOf(tally.getParticipantCount()));
            });
        }

        this.dispatchOutcome(vote, option, tally, this.commandsFor(status, outcomeKey));

        this.plugin.getPluginManager().callEvent(new VoteConcludedEvent(vote, outcomeKey, manual));
    }

    /**
     * Picks the command list that matches how the poll ended. The three outcomes get three
     * separate lists rather than a shared "no winner" one: a staff member stopping a poll and a
     * poll nobody agreed on are different events, and a config that needs to react differently
     * should not have to dig through {@code %vote_status%} inside one list to do it.
     */
    private List<String> commandsFor(final VoteStatus status, final @Nullable String outcomeKey) {
        if (outcomeKey != null) {
            return this.settings.commandsOnPass.get();
        }
        return status == VoteStatus.CANCELLED ? this.settings.commandsOnCancel.get()
                : this.settings.commandsOnFail.get();
    }

    /**
     * Runs a poll's outcome commands as the console.
     * <p>
     * Resolution is applied per line rather than to the joined list, so that a choice display name
     * containing a line break cannot split one configured command into two — the same trap the
     * Reports module guards against for its reward commands.
     */
    private void dispatchOutcome(final Vote vote, final @Nullable VoteOption option, final VoteTally tally,
            final List<String> commands) {
        if (commands.isEmpty()) {
            return;
        }

        final PlaceholderContext.Builder builder = PlaceholderContext.builder();
        builder.with(vote.placeholders());
        builder.with(VotingPlaceholders.VOTE_TOTAL_SCORE, () -> format(tally.getTotalScore()));
        builder.with(VotingPlaceholders.VOTE_PARTICIPANTS, () -> String.valueOf(tally.getParticipantCount()));
        builder.with(VotingPlaceholders.SCORE,
                () -> option == null ? "0" : format(tally.score(option.getKey())));
        builder.with(VotingPlaceholders.PERCENT,
                () -> option == null ? "0" : format(tally.share(option.getKey())));
        builder.with(VotingPlaceholders.COUNT,
                () -> option == null ? "0" : String.valueOf(tally.count(option.getKey())));
        if (option != null) {
            builder.with(option.placeholders());
        }

        final PlaceholderContext context = builder.build();
        final List<String> resolved = new ArrayList<>(commands);
        resolved.replaceAll(context::apply);

        final CommandSender console = this.plugin.getServer().getConsoleSender();
        this.plugin.runTask(task -> resolved.forEach(command -> this.plugin.getServer()
                .dispatchCommand(console, command)));
    }

    // -- Casting ----------------------------------------------------------------------------------

    /**
     * The single funnel every voting route goes through: commands, menus and the API all land
     * here, so a rule can never be enforced on one path and forgotten on another.
     *
     * @return whether the poll was changed. A refusal has already been explained to the player.
     */
    public boolean cast(final Player player, final Vote vote, final String optionKeyRaw) {
        if (!this.dataLoaded) {
            this.sendPrefixed(VotingLang.ERROR_DATA_NOT_LOADED, player);
            return false;
        }
        // The menus hold the live object, so a poll deleted while a menu is open would otherwise
        // pass the isOpen() check below on an orphan and mint ghost ballots for a poll that no
        // longer exists.
        if (this.repository.getVote(vote.getId()) == null) {
            this.sendPrefixed(VotingLang.ERROR_UNKNOWN_VOTE, player,
                    b -> b.with(SLPlaceholders.GENERIC_TARGET, () -> vote.getKey()));
            return false;
        }
        if (!vote.isOpen()) {
            if (vote.isScheduled()) {
                this.sendPrefixed(VotingLang.ERROR_NOT_STARTED, player, b -> b.with(vote.placeholders()));
            } else {
                this.sendPrefixed(VotingLang.ERROR_NOT_OPEN, player, b -> b.with(vote.placeholders()));
            }
            return false;
        }

        final VoteOption option = vote.getOption(optionKeyRaw);
        if (option == null) {
            if (VoteOption.isBuiltInKey(optionKeyRaw)) {
                // Same situation as /yes on a poll without yes: the owner switched the built-ins
                // off, which deserves its own message rather than a generic unknown-choice one.
                this.sendPrefixed(VotingLang.ERROR_BUILTIN_DISABLED, player, b -> b
                        .with(vote.placeholders())
                        .with(SLPlaceholders.GENERIC_TARGET, () -> optionKeyRaw)
                        .with(SLPlaceholders.GENERIC_LIST, () -> String.join(", ", vote.getOptionKeys())));
            } else {
                this.reportUnknownOption(player, vote, optionKeyRaw);
            }
            return false;
        }

        // Voting for the choice you already hold takes it back, which makes a click on a chosen
        // option a complete undo without needing a separate command.
        final VoteBallot existing = this.findBallot(vote, player, option);
        if (existing != null) {
            if (!this.checkChangeAllowed(player, vote)) {
                return false;
            }
            return this.withdraw(player, vote, option, existing);
        }

        if (!this.checkEligibility(player, vote)) {
            return false;
        }
        // Every ballot this cast gives up is refunded first, so affordability is judged on the
        // net cost rather than the gross: a player whose balance covers charge-minus-refund must
        // not be refused for not covering the charge alone.
        final List<VoteBallot> replaced = this.findReplacedBallots(vote, player, option);
        if (!replaced.isEmpty() && !this.checkChangeAllowed(player, vote)) {
            return false;
        }
        final double refundable = replaced.stream().mapToDouble(VoteBallot::costPaid).sum();
        if (!this.checkOptionGate(player, vote, option, refundable)) {
            return false;
        }
        if (!this.checkVoteLimit(player, vote)) {
            return false;
        }

        final double weight = this.resolveWeight(player, vote);
        final double cost = this.resolveCost(option, player);
        final VoteOption replacedOption = replaced.stream()
                .map(ballot -> vote.getOption(ballot.optionKey()))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        final VoteCastEvent event = new VoteCastEvent(vote, option, replacedOption, player.getUniqueId(),
                player.getName(), weight, cost, false);
        this.plugin.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        // Refunded before the new charge lands, so the balance the charge sees already includes
        // what the player is owed back. Charged only after the event, so a veto can never leave
        // somebody out of pocket for a vote that did not happen.
        for (final VoteBallot old : replaced) {
            this.refund(old);
            this.dropBallot(vote, player.getUniqueId(), old.optionKey());
        }
        if (cost > 0D) {
            EconomyUtils.withdraw(player, cost);
        }

        final VoteBallot ballot = VoteBallot.create(vote.getId(), option.getKey(), player.getUniqueId(),
                player.getName(), weight, cost);
        this.dataManager.insertBallot(ballot);
        this.ballotRepository.upsert(ballot);

        this.sendPrefixed(replaced.isEmpty() ? VotingLang.VOTE_CAST : VotingLang.VOTE_CHANGED, player, b -> b
                .with(vote.placeholders())
                .with(option.placeholders())
                .with(VotingPlaceholders.SCORE, () -> format(weight)));
        this.dispatchParticipation(player, vote, option, weight);
        return true;
    }

    /**
     * Whether this player may still change their vote. Exempt staff skip the lock like
     * every other restriction; everyone else is bound by the poll's own setting.
     */
    private boolean checkChangeAllowed(final Player player, final Vote vote) {
        if (this.isExempt(player) || this.hasBypass(player, VotingPerms.BYPASS_REQUIREMENTS)) {
            return true;
        }
        if (!vote.getRules().isAllowChange()) {
            this.sendPrefixed(VotingLang.ERROR_NO_CHANGE, player, b -> b.with(vote.placeholders()));
            return false;
        }
        if (!vote.isChangeAllowed()) {
            this.sendPrefixed(VotingLang.ERROR_CHANGE_LOCKED, player, b -> b.with(vote.placeholders()));
            return false;
        }
        return true;
    }

    /**
     * Runs the per-vote participation commands as the console. Unlike outcome commands,
     * these are about the voter rather than the result, so the player's name is
     * available to every line.
     */
    private void dispatchParticipation(final Player player, final Vote vote, final VoteOption option,
            final double weight) {
        final List<String> commands = this.settings.commandsOnVote.get();
        if (commands.isEmpty()) {
            return;
        }
        final VoteTally tally = this.tally(vote);
        final PlaceholderContext.Builder builder = PlaceholderContext.builder();
        builder.with(vote.placeholders());
        builder.with(option.placeholders());
        builder.with(VotingPlaceholders.SCORE, () -> format(weight));
        builder.with(VotingPlaceholders.COUNT, () -> String.valueOf(tally.count(option.getKey())));
        builder.with(VotingPlaceholders.PERCENT, () -> format(tally.share(option.getKey())));
        builder.with(VotingPlaceholders.VOTE_TOTAL_SCORE, () -> format(tally.getTotalScore()));
        builder.with(VotingPlaceholders.VOTE_PARTICIPANTS, () -> String.valueOf(tally.getParticipantCount()));
        builder.with("%player%", player::getName);
        builder.with("%player_name%", player::getName);
        builder.with("%uuid%", () -> player.getUniqueId().toString());
        final PlaceholderContext context = builder.build();
        final List<String> resolved = new ArrayList<>(commands);
        resolved.replaceAll(context::apply);
        final CommandSender console = this.plugin.getServer().getConsoleSender();
        this.plugin.runTask(task -> resolved.forEach(command -> this.plugin.getServer()
                .dispatchCommand(console, command)));
    }

    private boolean withdraw(final Player player, final Vote vote, final VoteOption option,
            final VoteBallot ballot) {
        final VoteCastEvent event = new VoteCastEvent(vote, option, null, player.getUniqueId(), player.getName(),
                ballot.weight(), ballot.costPaid(), true);
        this.plugin.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        // Paid back before the ballot is dropped, matching the replacement path. Neither order is
        // transactional across a crash, but money-first means a failure leaves the player still
        // holding the vote (retryable) rather than holding neither the vote nor the money.
        if (ballot.costPaid() > 0D) {
            EconomyUtils.deposit(player, ballot.costPaid());
            this.sendPrefixed(VotingLang.VOTE_REFUNDED, player,
                    b -> b.with(SLPlaceholders.GENERIC_AMOUNT, () -> EconomyUtils.format(ballot.costPaid())));
        }
        this.dropBallot(vote, player.getUniqueId(), option.getKey());

        this.sendPrefixed(VotingLang.VOTE_WITHDRAWN, player, b -> b
                .with(vote.placeholders())
                .with(option.placeholders()));
        return true;
    }

    private void dropBallot(final Vote vote, final UUID playerId, final String optionKey) {
        this.dataManager.deleteBallot(vote.getId(), playerId, optionKey);
        this.ballotRepository.remove(vote.getId(), playerId, optionKey);
    }

    private @Nullable VoteBallot findBallot(final Vote vote, final Player player, final VoteOption option) {
        return this.ballotRepository.getBallots(vote.getId(), player.getUniqueId()).stream()
                .filter(ballot -> ballot.optionKey().equalsIgnoreCase(option.getKey()))
                .findFirst()
                .orElse(null);
    }

    /**
     * @return every ballot this cast gives up. In a multi-vote poll nothing is replaced and the
     *         list is empty; in a single-vote poll it is every other ballot the player holds.
     *         Returning all of them matters because a poll tightened from multi to single can
     *         leave a player holding several — replacing only the first would strand the rest,
     *         still counting, in a poll that is supposed to hold one vote each.
     */
    private @NotNull List<VoteBallot> findReplacedBallots(final Vote vote, final Player player,
            final VoteOption option) {
        if (vote.getRules().allowsMultipleVotes()) {
            return List.of();
        }
        return this.ballotRepository.getBallots(vote.getId(), player.getUniqueId()).stream()
                .filter(ballot -> !ballot.optionKey().equalsIgnoreCase(option.getKey()))
                .toList();
    }

    /**
     * The poll-level bar a voter has to clear. Exempt players and requirement bypassers skip all
     * of it, so staff are never locked out of a poll they are being asked about.
     */
    private boolean checkEligibility(final Player player, final Vote vote) {
        if (this.isExempt(player) || this.hasBypass(player, VotingPerms.BYPASS_REQUIREMENTS)) {
            return true;
        }

        final VoteRules rules = vote.getRules();
        if (!this.hasBypass(player, VotingPerms.BYPASS_WORLDS) && !rules.isWorldAllowed(this.worldOf(player))) {
            this.sendPrefixed(VotingLang.ERROR_WORLD, player, b -> b
                    .with(vote.placeholders())
                    .with(SLPlaceholders.GENERIC_WORLD, () -> this.worldOf(player)));
            return false;
        }

        if (rules.getMinPlaytimeHours() > 0 && this.getPlaytimeHours(player) < rules.getMinPlaytimeHours()) {
            this.sendPrefixed(VotingLang.ERROR_PLAYTIME, player, b -> b
                    .with(vote.placeholders())
                    .with(SLPlaceholders.GENERIC_MIN, () -> String.valueOf(rules.getMinPlaytimeHours())));
            return false;
        }

        if (!rules.getMoneyMode().hasGate()) {
            return true;
        }
        if (rules.getMinBalance() <= 0D && !rules.hasMaxBalance()) {
            return true;
        }
        if (!EconomyUtils.hasCurrency()) {
            this.sendPrefixed(VotingLang.ERROR_NO_CURRENCY, player, b -> b.with(vote.placeholders()));
            return false;
        }

        final double balance = EconomyUtils.getBalance(player);
        if (rules.getMinBalance() > 0D && balance < rules.getMinBalance()) {
            this.sendPrefixed(VotingLang.ERROR_MIN_BALANCE, player, b -> b
                    .with(vote.placeholders())
                    .with(SLPlaceholders.GENERIC_MIN, () -> EconomyUtils.format(rules.getMinBalance())));
            return false;
        }
        if (rules.hasMaxBalance() && balance >= rules.getMaxBalance()
                && rules.getOverMaxMode() == OverMaxMode.REJECT) {
            this.sendPrefixed(VotingLang.ERROR_MAX_BALANCE, player, b -> b
                    .with(vote.placeholders())
                    .with(SLPlaceholders.GENERIC_MAX, () -> EconomyUtils.format(rules.getMaxBalance())));
            return false;
        }
        return true;
    }

    /**
     * The gates on one choice. All of them have to pass.
     *
     * @param refundable What the player gets back from replaced ballots in the same cast. The
     *                   affordability check is run against charge-minus-refund rather than the
     *                   gross charge, which is the balance the player will actually end up with.
     */
    private boolean checkOptionGate(final Player player, final Vote vote, final VoteOption option,
            final double refundable) {
        if (!option.isGated()) {
            return true;
        }
        if (this.isExempt(player) || this.hasBypass(player, VotingPerms.BYPASS_REQUIREMENTS)) {
            return true;
        }

        final double cost = this.resolveCost(option, player);
        if (option.hasGate(GateType.PAYMENT) && !option.isFree()) {
            if (!EconomyUtils.hasCurrency()) {
                this.sendPrefixed(VotingLang.ERROR_NO_CURRENCY, player, b -> b
                        .with(vote.placeholders())
                        .with(option.placeholders()));
                return false;
            }
            // Checked against the net position after the replaced ballots are refunded, not the
            // gross charge: refusing a player whose balance covers the difference would be wrong.
            final double net = Math.max(0D, cost - Math.max(0D, refundable));
            if (net > 0D && EconomyUtils.getBalance(player) < net) {
                this.sendPrefixed(VotingLang.ERROR_COST_FUNDS, player, b -> b
                        .with(vote.placeholders())
                        .with(option.placeholders())
                        .with(SLPlaceholders.GENERIC_AMOUNT, () -> EconomyUtils.format(cost))
                        .with(SLPlaceholders.GENERIC_TOTAL,
                                () -> EconomyUtils.format(EconomyUtils.getBalance(player))));
                return false;
            }
        }

        if (option.hasGate(GateType.PERMISSION) && option.requiresPermission()
                && !this.hasOptionPermission(player, option.getPermission())) {
            this.sendPrefixed(VotingLang.ERROR_GATE_PERMISSION, player, b -> b
                    .with(vote.placeholders())
                    .with(option.placeholders()));
            return false;
        }

        if (option.hasGate(GateType.RANK) && option.requiresRank()) {
            final String group = Players.getPrimaryGroupOrDefault(player);
            if (group == null || !group.equalsIgnoreCase(option.getRank())) {
                this.sendPrefixed(VotingLang.ERROR_GATE_RANK, player, b -> b
                        .with(vote.placeholders())
                        .with(option.placeholders())
                        .with(SLPlaceholders.GENERIC_TARGET, option::getRank));
                return false;
            }
        }

        // A choice's world gate is a plain whitelist: these are the worlds it may be picked from.
        // The poll's own black/white switch already decides where voting happens at all, so the
        // only meaningful question a per-choice restriction can ask is "only here".
        if (option.hasGate(GateType.WORLD) && !option.getWorlds().isEmpty()
                && !this.hasBypass(player, VotingPerms.BYPASS_WORLDS)) {
            final String world = this.worldOf(player);
            final boolean listed = world != null
                    && option.getWorlds().stream().anyMatch(name -> name.equalsIgnoreCase(world));
            if (!listed) {
                this.sendPrefixed(VotingLang.ERROR_GATE_WORLD, player, b -> b
                        .with(vote.placeholders())
                        .with(option.placeholders())
                        .with(SLPlaceholders.GENERIC_WORLD, () -> world == null ? "unknown" : world));
                return false;
            }
        }

        if (option.hasGate(GateType.PLAYTIME) && option.getMinPlaytimeHours() > 0
                && this.getPlaytimeHours(player) < option.getMinPlaytimeHours()) {
            this.sendPrefixed(VotingLang.ERROR_GATE_PLAYTIME, player, b -> b
                    .with(vote.placeholders())
                    .with(option.placeholders())
                    .with(SLPlaceholders.GENERIC_MIN, () -> String.valueOf(option.getMinPlaytimeHours())));
            return false;
        }
        return true;
    }

    private boolean checkVoteLimit(final Player player, final Vote vote) {
        if (this.isExempt(player) || this.hasBypass(player, VotingPerms.BYPASS_LIMITS)) {
            return true;
        }

        final int max = this.settings.maxParticipatingVotes.get();
        // Only applies to a poll this player is not already in. Counting one they have a ballot in
        // would refuse a second choice in a multi-vote poll the moment they hit the limit, which
        // is not what a cap on how many polls somebody takes part in means.
        if (max > 0 && !this.ballotRepository.hasVoted(vote.getId(), player.getUniqueId())) {
            final int participating = this.ballotRepository.getParticipationCount(player.getUniqueId(),
                    this.repository.getOpenVotes().stream().map(Vote::getId).toList());
            if (participating >= max) {
                this.sendPrefixed(VotingLang.ERROR_PARTICIPATION, player,
                        b -> b.with(SLPlaceholders.GENERIC_MAX, () -> String.valueOf(max)));
                return false;
            }
        }

        // Only meaningful once the poll actually allows more than one. In a single-vote poll every
        // option looks full, because casting a second one replaces the first.
        if (!vote.getRules().allowsMultipleVotes()) {
            return true;
        }

        final int held = this.ballotRepository.getBallots(vote.getId(), player.getUniqueId()).size();
        if (vote.getRules().votesLeft(held) <= 0) {
            this.sendPrefixed(VotingLang.ERROR_TOO_MANY_VOTES, player, b -> b
                    .with(vote.placeholders())
                    .with(VotingPlaceholders.VOTE_MAX_VOTES,
                            () -> String.valueOf(vote.getRules().getMaxVotes())));
            return false;
        }
        return true;
    }

    private boolean isExempt(final Player player) {
        for (final String permission : this.settings.exemptPermissions.get()) {
            if (player.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBypass(final Player player, final String bypass) {
        return player.hasPermission(VotingPerms.BYPASS + ".*") || player.hasPermission(bypass);
    }

    /**
     * @return whether the player clears a choice's permission gate. The module-wide
     *         {@code sunlight.voting.option} wildcard covers every per-choice node, so staff
     *         holding it are never locked out of a single gated choice.
     */
    private boolean hasOptionPermission(final Player player, final String node) {
        return player.hasPermission(VotingPerms.OPTION) || player.hasPermission(VotingPerms.OPTION + ".*")
                || player.hasPermission(node);
    }

    private @Nullable String worldOf(final Player player) {
        return player.getWorld() == null ? null : player.getWorld().getName();
    }

    /**
     * @return the player's total playtime in hours, or {@link Integer#MAX_VALUE} when the
     *         Playtime module is not installed. Voting must not require another module to exist,
     *         so an absent requirement is satisfied rather than failed.
     */
    private double getPlaytimeHours(final Player player) {
        if (!this.plugin.moduleManager().isPresent(PLAYTIME_MODULE_ID)) {
            return Integer.MAX_VALUE;
        }
        final long millis = this.plugin.playtimeProvider()
                .map(provider -> provider.getTotalPlaytimeMs(player.getUniqueId()))
                .orElse(0L);
        return TimeUnit.MILLISECONDS.toHours(millis);
    }

    /**
     * What this player's one ballot is worth.
     * <p>
     * The order of the two clamps is the whole point: the multiplier is capped first and the total
     * weight second, so a player stacking several permissions under ADDITIVE cannot jump over the
     * multiplier ceiling and land above the weight ceiling on the way through.
     */
    public double resolveWeight(final Player player, final Vote vote) {
        final VoteRules rules = vote.getRules();
        double weight = rules.getBaseWeight();

        if (rules.isWeightingEnabled() && !this.hasBypass(player, VotingPerms.BYPASS_WEIGHTING)) {
            weight *= rules.clampMultiplier(this.resolveMultiplier(player, rules));
        }

        final double money = this.moneyWeight(player, rules);
        if (money > 0D) {
            weight += money;
        }
        return rules.clampWeight(weight);
    }

    private double resolveMultiplier(final Player player, final VoteRules rules) {
        final var table = this.settings.getMultiplierTable();

        final double permission = rules.getWeightSource().hasPermission()
                ? table.permissionMultiplier(player, rules.getWeightCombine(), 1D)
                : 1D;
        final double rank = rules.getWeightSource().hasRank() ? table.rankMultiplier(player, 1D) : 1D;

        if (rules.getWeightSource().hasPermission() && rules.getWeightSource().hasRank()
                && rules.getWeightCombine() == WeightCombine.ADDITIVE) {
            // Both sources report 1.0 for "matched nothing", so their bonuses are added and the
            // base restored once. Adding the two multipliers directly would hand a free x2 to
            // everybody who matched neither.
            return (permission - 1D) + (rank - 1D) + 1D;
        }
        return Math.max(permission, rank);
    }

    private double moneyWeight(final Player player, final VoteRules rules) {
        if (!rules.getMoneyMode().hasWeight() || rules.getCurrencyPerWeight() <= 0D) {
            return 0D;
        }
        if (!EconomyUtils.hasCurrency()) {
            return 0D;
        }

        double balance = EconomyUtils.getBalance(player);
        if (rules.hasMaxBalance() && balance > rules.getMaxBalance()) {
            if (rules.getOverMaxMode() == OverMaxMode.REJECT) {
                // Refused outright in checkEligibility; contribute nothing rather than let the
                // balance speak on a path that should not have been reached.
                return 0D;
            }
            balance = rules.getMaxBalance();
        }

        double weight = balance / rules.getCurrencyPerWeight();
        if (rules.getMaxMoneyWeight() > 0D) {
            weight = Math.min(weight, rules.getMaxMoneyWeight());
        }
        return weight;
    }

    /**
     * @return what this choice costs this player, which is zero for an ungated choice, for a
     *         bypasser and when there is no economy to charge. Asked once to decide and once to
     *         charge, and both go through here, so the two can never disagree.
     */
    public double resolveCost(final VoteOption option, final Player player) {
        if (!option.hasGate(GateType.PAYMENT) || option.isFree()) {
            return 0D;
        }
        // Honors the global sunlight.bypass.cost as well as the module node.
        if (this.isExempt(player) || EconomyUtils.hasBypass(player, VotingPerms.BYPASS_COST)) {
            return 0D;
        }
        return option.getCost();
    }

    // -- Notification -----------------------------------------------------------------------------

    public void announce(final Vote vote) {
        this.sendAnnouncement(vote);
    }

    public void broadcast(final Vote vote) {
        this.sendAnnouncement(vote);
    }

    private void sendAnnouncement(final Vote vote) {
        final String permission = this.settings.notifyPermission.get();
        if (permission == null || permission.isBlank()) {
            this.broadcastPrefixed(VotingLang.ANNOUNCEMENT, b -> b.with(vote.placeholders()));
            return;
        }
        for (final Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(permission)) {
                this.sendPrefixed(VotingLang.ANNOUNCEMENT, online, b -> b.with(vote.placeholders()));
            }
        }
    }

    // -- Queries for menus ------------------------------------------------------------------------

    public enum Filter {
        OPEN,
        MINE,
        FINISHED
    }

    public List<Vote> getOpenVotes() {
        return this.repository.getOpenVotes();
    }

    public List<Vote> getScheduledVotes() {
        return this.repository.getScheduledVotes();
    }

    public List<Vote> getFinishedVotes() {
        final List<Vote> votes = this.repository.getFinishedVotes();
        votes.sort((first, second) -> Long.compare(second.getConcludeDate(), first.getConcludeDate()));
        return votes;
    }

    public List<Vote> getVisibleVotes(final Player player, final boolean mineOnly) {
        return this.getVisibleVotes(player, mineOnly ? Filter.MINE : Filter.OPEN);
    }

    public List<Vote> getVisibleVotes(final Player player, final Filter filter) {
        return switch (filter) {
            case MINE -> this.repository.getOpenVotes().stream()
                    .filter(vote -> this.ballotRepository.hasVoted(vote.getId(), player.getUniqueId()))
                    .toList();
            case FINISHED -> this.getFinishedVotes();
            case OPEN -> this.repository.getOpenVotes();
        };
    }

    public List<Vote> getAllVotes() {
        final List<Vote> votes = new ArrayList<>(this.repository.getVotes());
        votes.sort((first, second) -> Long.compare(second.getCreateDate(), first.getCreateDate()));
        return votes;
    }

    /**
     * @return the open poll a bare {@code /vote yes} should apply to, or {@code null} when the
     *         choice is ambiguous. Never guesses: voting in the wrong poll is worse than asking.
     */
    public @Nullable Vote resolveSoleOpenVote() {
        final List<Vote> open = this.repository.getOpenVotes();
        return open.size() == 1 ? open.getFirst() : null;
    }

    /**
     * Prints the full breakdown for somebody who asked for it. Headcount and weighted score are
     * both shown: they answer different questions and a single blended number would hide that.
     * Staff always see real numbers, even for secret ballots still open.
     */
    public void showInfo(final CommandSender sender, final Vote vote) {
        final VoteTally tally = this.tally(vote);
        final PlaceholderContext context = PlaceholderContext.builder()
                .with(vote.placeholders())
                .with(VotingPlaceholders.VOTE_PARTICIPANTS, () -> String.valueOf(tally.getParticipantCount()))
                .build();

        sender.sendMessage(context.apply(VotingLang.INFO_HEADER.text()));
        if (vote.hasDescription()) {
            sender.sendMessage(context.apply("<dark_gray>" + VotingPlaceholders.VOTE_DESC + "</dark_gray>"));
        }
        vote.getOptionList().forEach(option -> sender.sendMessage(PlaceholderContext.builder()
                .with(vote.placeholders())
                .with(option.placeholders())
                .with(VotingPlaceholders.COUNT, () -> String.valueOf(tally.count(option.getKey())))
                .with(VotingPlaceholders.PERCENT, () -> format(tally.share(option.getKey())))
                .with(VotingPlaceholders.SCORE, () -> format(tally.score(option.getKey())))
                .build()
                .apply(VotingLang.RESULT_LINE.text())));

        final MessageLocale footer = vote.hasOutcome() ? VotingLang.RESULT_PASSED
                : vote.getStatus() == VoteStatus.CANCELLED ? VotingLang.RESULT_CANCELLED
                        : VotingLang.RESULT_FAILED;

        this.sendPrefixed(footer, sender, b -> {
            b.with(vote.placeholders());
            final VoteOption winner = vote.getOutcomeOption();
            if (winner != null) {
                b.with(winner.placeholders());
                b.with(VotingPlaceholders.PERCENT, () -> format(tally.share(winner.getKey())));
                b.with(VotingPlaceholders.SCORE, () -> format(tally.score(winner.getKey())));
                b.with(VotingPlaceholders.VOTE_TOTAL_SCORE, () -> format(tally.getTotalScore()));
            }
            b.with(VotingPlaceholders.VOTE_PARTICIPANTS, () -> String.valueOf(tally.getParticipantCount()));
        });
    }

    // -- Menus ------------------------------------------------------------------------------------

    public boolean openMenu(final Player player) {
        return this.menu != null && this.menu.open(player);
    }

    public boolean openMyVotes(final Player player) {
        return this.menu != null && this.menu.openMyVotes(player);
    }

    public boolean openHistory(final Player player) {
        return this.menu != null && this.menu.openHistory(player);
    }

    public boolean openView(final Player player, final Vote vote) {
        return this.viewMenu != null && this.viewMenu.open(player, vote);
    }

    // -- VotingProvider ---------------------------------------------------------------------------

    /**
     * Until the async boot load finishes the repositories hold a partial picture. API consumers
     * get emptiness rather than half a poll list, so a scoreboard refreshing during startup does
     * not flicker through wrong numbers.
     */
    private boolean isReady() {
        return this.dataLoaded;
    }

    @Override
    public @NotNull List<String> getOpenVoteKeys() {
        if (!this.isReady()) {
            return List.of();
        }
        return this.repository.getOpenVotes().stream().map(Vote::getKey).toList();
    }

    @Override
    public int getOpenVoteCount() {
        return this.isReady() ? this.repository.getOpenCount() : 0;
    }

    @Override
    public VoteHandle getVote(final String voteKey) {
        return this.isReady() ? this.handle(this.repository.getVote(voteKey)) : null;
    }

    @Override
    public VoteHandle getVote(final UUID voteId) {
        return this.isReady() ? this.handle(this.repository.getVote(voteId)) : null;
    }

    @Override
    public int getOptionVotes(final UUID voteId, final String optionKey) {
        return this.tallyOrEmpty(voteId).count(optionKey);
    }

    @Override
    public double getOptionScore(final UUID voteId, final String optionKey) {
        return this.tallyOrEmpty(voteId).score(optionKey);
    }

    @Override
    public double getOptionShare(final UUID voteId, final String optionKey) {
        return this.tallyOrEmpty(voteId).share(optionKey);
    }

    @Override
    public int getParticipantCount(final UUID voteId) {
        return this.tallyOrEmpty(voteId).getParticipantCount();
    }

    @Override
    public boolean hasVoted(final UUID voteId, final UUID playerId) {
        return this.isReady() && this.ballotRepository.hasVoted(voteId, playerId);
    }

    @Override
    public @NotNull List<String> getPlayerVotes(final UUID voteId, final UUID playerId) {
        if (!this.isReady()) {
            return List.of();
        }
        return this.ballotRepository.getBallots(voteId, playerId).stream()
                .map(VoteBallot::optionKey).toList();
    }

    @Override
    public double getPlayerWeight(final UUID voteId, final UUID playerId, final String optionKey) {
        if (!this.isReady()) {
            return 0D;
        }
        return this.ballotRepository.getBallots(voteId, playerId).stream()
                .filter(ballot -> ballot.optionKey().equalsIgnoreCase(optionKey))
                .mapToDouble(VoteBallot::weight)
                .findFirst()
                .orElse(0D);
    }

    /**
     * @return an empty tally for an unknown poll rather than throwing. A placeholder expansion or
     *         a scoreboard refresh should degrade to zeroes, not take the caller down with it.
     */
    private VoteTally tallyOrEmpty(final UUID voteId) {
        final Vote vote = this.repository.getVote(voteId);
        return vote == null ? new VoteTally(List.of(), new VoteRules()) : this.tally(vote);
    }

    private @Nullable VoteHandle handle(final Vote vote) {
        if (vote == null) {
            return null;
        }

        final List<VoteHandle.Option> options = vote.getOptionList().stream()
                .map(option -> new VoteHandle.Option(option.getKey(), option.getDisplay(), option.isBuiltIn(),
                        option.isGated(), option.getCost()))
                .toList();

        return new VoteHandle(vote.getId(), vote.getKey(), vote.getDisplay(), vote.getDescription(),
                vote.getCreatorId(), vote.getCreatorName(), vote.getCreateDate(), vote.getStartDate(),
                vote.getEndDate(), vote.getStatus().name(), vote.isOpen(), vote.getRules().allowsMultipleVotes(),
                vote.getRules().getMaxVotes(), vote.getRules().isSecretBallot(),
                vote.getRules().getTieBreak().name(), options, vote.getOutcome(), vote.getConcludeDate());
    }

    @Override
    public @NotNull List<String> getScheduledVoteKeys() {
        if (!this.isReady()) {
            return List.of();
        }
        return this.repository.getScheduledVotes().stream().map(Vote::getKey).toList();
    }

    public static String format(final double value) {
        return value == Math.rint(value) ? String.valueOf((long) value)
                : String.format(Locale.ROOT, "%.2f", value);
    }

    /**
     * @return a consumer that applies the module's usual placeholders to a poll, for callers that
     *         need to build a context of their own.
     */
    public Consumer<PlaceholderContext.Builder> contextFor(final Vote vote, final VoteTally tally,
            final @Nullable VoteOption option) {
        return b -> {
            b.with(vote.placeholders());
            b.with(VotingPlaceholders.VOTE_TOTAL_SCORE, () -> format(tally.getTotalScore()));
            b.with(VotingPlaceholders.VOTE_PARTICIPANTS, () -> String.valueOf(tally.getParticipantCount()));
            if (option != null) {
                b.with(option.placeholders());
                b.with(VotingPlaceholders.COUNT, () -> String.valueOf(tally.count(option.getKey())));
                b.with(VotingPlaceholders.PERCENT, () -> format(tally.share(option.getKey())));
                b.with(VotingPlaceholders.SCORE, () -> format(tally.score(option.getKey())));
            }
        };
    }
}