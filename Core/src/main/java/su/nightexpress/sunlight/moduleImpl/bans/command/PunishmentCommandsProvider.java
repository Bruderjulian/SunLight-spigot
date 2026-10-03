package su.nightexpress.sunlight.moduleImpl.bans.command;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.bans.BansModule;
import su.nightexpress.sunlight.moduleImpl.bans.config.BansLang;
import su.nightexpress.sunlight.moduleImpl.bans.config.BansPerms;
import su.nightexpress.sunlight.moduleImpl.bans.punishment.PunishmentContext;
import su.nightexpress.sunlight.moduleImpl.bans.punishment.PunishmentReason;
import su.nightexpress.sunlight.moduleImpl.bans.punishment.PunishmentType;
import su.nightexpress.sunlight.moduleImpl.bans.time.BanTime;
import su.nightexpress.sunlight.moduleImpl.bans.time.BanTimeUnit;
import su.nightexpress.sunlight.utils.Utils;

public class PunishmentCommandsProvider extends CommandProvider<BansModule> {

        private static final String ARG_REASON = "reason";

        public PunishmentCommandsProvider(BansModule module) {
                super(module, "bans-punish");
        }

        @Override
        public void setup() {
                this.register("ban", List.of(),
                                builder -> this.buildPunishment(builder, PunishmentType.BAN, true));
                this.register("mute", List.of(),
                                builder -> this.buildPunishment(builder, PunishmentType.MUTE, true));
                this.register("warn", List.of(),
                                builder -> this.buildPunishment(builder, PunishmentType.WARN, true));
                this.register("tempban", List.of(),
                                builder -> this.buildPunishment(builder, PunishmentType.BAN, false));
                this.register("tempmute", List.of(),
                                builder -> this.buildPunishment(builder, PunishmentType.MUTE, false));
                this.register("tempwarn", List.of(),
                                builder -> this.buildPunishment(builder, PunishmentType.WARN, false));

                this.register("banip", List.of(), command -> command
                                .withFullDescription(BansLang.COMMAND_BAN_IP_DESC.text())
                                .withPermission(BansPerms.COMMAND_BAN_IP.getName())
                                .withArguments(CommandArgumentConstants.string(
                                                CommandArgumentConstants.INET_ADDRESS, info -> List.of()))
                                .withOptionalArguments(new GreedyStringArgument(ARG_REASON)
                                                .replaceSuggestions(dev.jorel.commandapi.arguments.ArgumentSuggestions
                                                                .stringCollection(
                                                                                info -> this.module.getReasonIds())))
                                .executes(this::banInet));

                this.register("kick", List.of(), command -> command
                                .withFullDescription(BansLang.COMMAND_KICK_DESC.text())
                                .withPermission(BansPerms.COMMAND_KICK.getName())
                                .withArguments(CommandArgumentConstants.string(
                                                CommandArgumentConstants.PLAYER,
                                                info -> CommandArgumentConstants.onlinePlayerNames()))
                                .withOptionalArguments(new GreedyStringArgument(ARG_REASON)
                                                .replaceSuggestions(dev.jorel.commandapi.arguments.ArgumentSuggestions
                                                                .stringCollection(
                                                                                info -> this.module.getReasonIds())))
                                .executes(this::kick));
        }

        private void buildPunishment(CommandAPICommand builder, PunishmentType type, boolean isPermanent) {
                String description = switch (type) {
                        case BAN -> BansLang.COMMAND_BAN_DESC.text();
                        case MUTE -> BansLang.COMMAND_MUTE_DESC.text();
                        case WARN -> BansLang.COMMAND_WARN_DESC.text();
                };

                Permission permission = switch (type) {
                        case BAN -> BansPerms.COMMAND_BAN;
                        case MUTE -> BansPerms.COMMAND_MUTE;
                        case WARN -> BansPerms.COMMAND_WARN;
                };

                builder
                                .withFullDescription(description)
                                .withPermission(permission.getName())
                                .executes((sender, arguments) -> {
                                        return this.punishPlayer(sender, arguments, type, isPermanent);
                                });

                if (isPermanent) {
                        builder.withArguments(CommandArgumentConstants.string(
                                        CommandArgumentConstants.PLAYER,
                                        info -> CommandArgumentConstants.onlinePlayerNames()));
                        builder.withOptionalArguments(new GreedyStringArgument(ARG_REASON)
                                        .replaceSuggestions(
                                                        dev.jorel.commandapi.arguments.ArgumentSuggestions
                                                                        .stringCollection(
                                                                                        info -> this.module
                                                                                                        .getReasonIds())));
                } else {
                        builder.withArguments(CommandArgumentConstants.string(
                                        CommandArgumentConstants.PLAYER,
                                        info -> CommandArgumentConstants.onlinePlayerNames()),
                                        new IntegerArgument(CommandArgumentConstants.AMOUNT, 1),
                                        CommandArgumentConstants.string(
                                        CommandArgumentConstants.TYPE,
                                        info -> this.module.getTimeUnitAliases()));
                        builder.withOptionalArguments(new GreedyStringArgument(ARG_REASON)
                                        .replaceSuggestions(
                                                        dev.jorel.commandapi.arguments.ArgumentSuggestions
                                                                        .stringCollection(
                                                                                        info -> this.module
                                                                                                        .getReasonIds())));
                }
        }

        private int punishPlayer(CommandSender sender, CommandArguments arguments, PunishmentType type,
                        boolean isPermanent) {
                final String playerName = (String) arguments.get(CommandArgumentConstants.PLAYER);
                if (playerName == null || playerName.isBlank()) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }

                final String reasonRaw = (String) arguments.get(ARG_REASON);
                final ParsedReason parsed = this.parseReason(sender, reasonRaw);
                if (parsed == null) {
                        return 0;
                }

                final BanTime banTime;
                if (isPermanent) {
                        banTime = BanTime.permanent();
                } else {
                        final Integer duration = (Integer) arguments.get(CommandArgumentConstants.AMOUNT);
                        final String unitRaw = (String) arguments.get(CommandArgumentConstants.TYPE);
                        final BanTimeUnit timeUnit = unitRaw == null ? null
                                        : this.module.getTimeUnitByAlias(unitRaw);
                        if (duration == null || timeUnit == null) {
                                this.module.sendPrefixed(BansLang.COMMAND_SYNTAX_INVALID_TIME_UNIT, sender);
                                return 0;
                        }
                        banTime = BanTime.temporary(timeUnit, duration);
                }

                this.module.userManager().loadTargetProfile(playerName).thenCompose(profile -> {
                        if (profile == null) {
                                this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                                return CompletableFuture.completedFuture(null);
                        }

                        PunishmentContext punishmentContext = new PunishmentContext(type, parsed.reason(), banTime,
                                        parsed.silent());

                        return this.module.punishPlayer(sender, profile, punishmentContext);
                });
                return 1;
        }

        private int banInet(CommandSender sender, CommandArguments arguments) {
                final String addressRaw = (String) arguments.get(CommandArgumentConstants.INET_ADDRESS);
                final InetAddress address = parseAddress(addressRaw);
                if (address == null) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }

                final ParsedReason parsed = this.parseReason(sender, (String) arguments.get(ARG_REASON));
                if (parsed == null) {
                        return 0;
                }

                BanTime banTime = BanTime.permanent();

                return module.banInet(sender, address, parsed.reason(), banTime, parsed.silent()) ? 1 : 0;
        }

        private int kick(CommandSender sender, CommandArguments arguments) {
                final String playerName = (String) arguments.get(CommandArgumentConstants.PLAYER);
                Player player = playerName == null ? null : Utils.getPlayer(playerName);
                if (player == null) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }

                final ParsedReason parsed = this.parseReason(sender, (String) arguments.get(ARG_REASON));
                if (parsed == null) {
                        return 0;
                }

                return this.module.kick(sender, player, parsed.reason(), parsed.silent()) ? 1 : 0;
        }

        private record ParsedReason(PunishmentReason reason, boolean silent) {
        }

        /**
         * Resolves the greedy reason string (which may carry a trailing
         * {@code -s / --silent} token) to a {@link PunishmentReason}.
         *
         * @return null when the reason id is invalid (error already sent).
         */
        private ParsedReason parseReason(CommandSender sender, String raw) {
                if (raw == null || raw.isBlank()) {
                        return new ParsedReason(this.module.getDefaultReason(), false);
                }

                boolean silent = false;
                List<String> kept = new ArrayList<>();
                for (String token : raw.trim().split("\\s+")) {
                        String lower = Utils.lowercase(token);
                        if (lower.equals(CommandArgumentConstants.FLAG_SILENT)
                                        || lower.equals(CommandArgumentConstants.FLAG_SILENT_LONG)) {
                                silent = true;
                                continue;
                        }
                        kept.add(token);
                }

                if (kept.isEmpty()) {
                        return new ParsedReason(this.module.getDefaultReason(), silent);
                }

                String id = String.join(" ", kept);
                PunishmentReason reason = this.module.getReasonById(id);
                if (reason == null) {
                        this.module.sendPrefixed(BansLang.COMMAND_SYNTAX_INVALID_REASON, sender);
                        return null;
                }
                return new ParsedReason(reason, silent);
        }

        private static InetAddress parseAddress(String raw) {
                if (raw == null || raw.isBlank()) {
                        return null;
                }
                try {
                        return InetAddress.getByName(raw.trim());
                } catch (Exception exception) {
                        return null;
                }
        }
}
