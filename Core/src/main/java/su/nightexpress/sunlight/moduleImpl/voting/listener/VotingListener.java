package su.nightexpress.sunlight.moduleImpl.voting.listener;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.voting.VotingModule;
import su.nightexpress.sunlight.moduleImpl.voting.config.VotingLang;
import su.nightexpress.sunlight.moduleImpl.voting.model.Vote;

public class VotingListener extends AbstractListener<SunLightPlugin> {

    private final VotingModule module;

    public VotingListener(final SunLightPlugin plugin, final VotingModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        // Delayed by one sweep tick so the boot load cannot greet a player with half a
        // list.
        // The module itself guards on dataLoaded, so this is only ordering, not
        // correctness.
        this.plugin.runTask(task -> nudgeOnJoin(player));
    }

    public void nudgeOnJoin(final Player player) {
        if (!module.isDataLoaded() || !module.settings().notifyOnJoin.get()) {
            return;
        }
        if (!module.isReminderEnabled(player.getUniqueId())) {
            return;
        }
        // Silent polls never nudge: they stay votable via GUI/commands but never push.
        final List<Vote> pending = module.getRepository().getOpenVotes().stream()
                .filter(vote -> !vote.isSilent())
                .filter(vote -> !module.getBallotRepository().hasVoted(vote.getId(), player.getUniqueId()))
                .toList();
        if (pending.isEmpty()) {
            return;
        }
        final String permission = module.settings().notifyPermission.get();
        if (permission != null && !permission.isBlank() && !player.hasPermission(permission)) {
            return;
        }
        module.sendPrefixed(VotingLang.JOIN_NUDGE, player, b -> b
                .with(SLPlaceholders.GENERIC_AMOUNT, () -> String.valueOf(pending.size()))
                .with(SLPlaceholders.GENERIC_LIST,
                        () -> String.join(", ", pending.stream().map(Vote::getKey).toList())));
    }
}
