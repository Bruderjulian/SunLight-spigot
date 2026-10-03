package su.nightexpress.sunlight.moduleImpl.spawns.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.spawns.Spawn;
import su.nightexpress.sunlight.moduleImpl.spawns.SpawnsModule;
import su.nightexpress.sunlight.moduleImpl.spawns.config.SpawnsLang;
import su.nightexpress.sunlight.moduleImpl.spawns.config.SpawnsPerms;
import su.nightexpress.sunlight.utils.Utils;

public class SpawnCommands extends CommandProvider<SpawnsModule> {

    private static final String COMMAND_CREATE = "spawn_create";
    private static final String COMMAND_DELETE = "spawn_delete";
    private static final String COMMAND_EDITOR = "spawn_editor";
    private static final String COMMAND_TELEPORT = "spawn_teleport";

    public static final String DEF_EDITOR_ALIAS = "editspawn";

    public SpawnCommands(final SpawnsModule module) {
        super(module, "spawn");
    }

    @Override
    public void setup() {
        this.register(COMMAND_CREATE, List.of(), command -> command
                .withFullDescription(SpawnsLang.COMMAND_SPAWN_SET_DESC.text())
                .withPermission(SpawnsPerms.COMMAND_SPAWNS_CREATE.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withOptionalArguments(CommandArgumentConstants.string(CommandArgumentConstants.NAME,
                        info -> this.module.getSpawnIds()))
                .executes(this::setSpawn));

        this.register(COMMAND_DELETE, List.of(), command -> command
                .withFullDescription(SpawnsLang.COMMAND_SPAWN_DELETE_DESC.text())
                .withPermission(SpawnsPerms.COMMAND_SPAWNS_DELETE.getName())
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.NAME,
                        info -> this.module.getSpawnIds()))
                .executes(this::deleteSpawn));

        this.register(COMMAND_EDITOR, List.of(), command -> command
                .withFullDescription(SpawnsLang.COMMAND_SPAWN_EDITOR_DESC.text())
                .withPermission(SpawnsPerms.COMMAND_SPAWNS_EDITOR.getName())
                .withRequirement(sender -> sender instanceof Player)
                .executes(this::openEditor));

        this.register(COMMAND_TELEPORT, List.of(), command -> command
                .withFullDescription(SpawnsLang.COMMAND_SPAWN_TELEPORT_DESC.text())
                .withPermission(SpawnsPerms.COMMAND_SPAWNS_TELEPORT.getName())
                .withOptionalArguments(
                        CommandArgumentConstants.string(CommandArgumentConstants.NAME,
                                info -> this.module.getSpawnIds()),
                        CommandArgumentConstants.targetArgument())
                .executes(this::teleport));
    }

    private int setSpawn(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final Object spawnObj = arguments.get(CommandArgumentConstants.NAME);
        final String spawnId = spawnObj instanceof final String value ? value : SLPlaceholders.DEFAULT;
        return this.module.createSpawn(player, spawnId) ? 1 : 0;
    }

    private int deleteSpawn(final CommandSender sender, final CommandArguments arguments) {
        final Object spawnObj = arguments.get(CommandArgumentConstants.NAME);
        final Spawn spawn = spawnObj instanceof final String spawnId ? this.module.getSpawn(spawnId) : null;
        if (spawn == null) {
            this.module.sendPrefixed(SpawnsLang.COMMAND_SYNTAX_INVALID_SPAWN, sender);
            return 0;
        }

        this.module.sendPrefixed(SpawnsLang.SPAWN_DELETE_FEEDBACK, sender,
                replacer -> replacer.with(spawn.placeholders()));

        return this.module.deleteSpawn(spawn) ? 1 : 0;
    }

    private int openEditor(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        this.module.openEditor(player);
        return 1;
    }

    private int teleport(final CommandSender sender, final CommandArguments arguments) {
        final Object spawnObj = arguments.get(CommandArgumentConstants.NAME);
        final Spawn spawn;
        if (spawnObj == null) {
            spawn = this.module.getDefaultSpawn();
            if (spawn == null) {
                this.module.sendPrefixed(SpawnsLang.ERROR_NO_DEFAULT_SPAWN, sender);
                return 0;
            }
        } else if (spawnObj instanceof final String spawnId) {
            spawn = this.module.getSpawn(spawnId);
            if (spawn == null) {
                this.module.sendPrefixed(SpawnsLang.COMMAND_SYNTAX_INVALID_SPAWN, sender);
                return 0;
            }
        } else {
            this.module.sendPrefixed(SpawnsLang.ERROR_NO_DEFAULT_SPAWN, sender);
            return 0;
        }

        final TargetWithForce parsed = TargetWithForce.parse(arguments);
        if (parsed == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        final boolean force = parsed.force();
        final boolean silent = parsed.target().silent();
        return parsed.target().runAs(this.module, sender, SpawnsPerms.COMMAND_SPAWNS_TELEPORT_OTHERS.getName(),
                (user, targetPlayer) -> {
                    if (!this.module.teleport(spawn, targetPlayer, force, silent)) {
                        return;
                    }

                    if (force) {
                        this.module.sendPrefixed(SpawnsLang.SPAWN_TELEPORT_FEEDBACK, sender, replacer -> replacer
                                .with(spawn.placeholders())
                                .with(CommonPlaceholders.PLAYER.resolver(targetPlayer)));
                    }
                });
    }

    private record TargetWithForce(CommandArgumentConstants.Target target, boolean force) {

        static TargetWithForce parse(final CommandArguments arguments) {
            final Object rawObj = arguments.get(CommandArgumentConstants.TARGET);
            if (!(rawObj instanceof final String raw) || raw.isBlank()) {
                return new TargetWithForce(new CommandArgumentConstants.Target(null, false), false);
            }

            String playerName = null;
            boolean silent = false;
            boolean force = false;
            for (final String token : raw.trim().split("\\s+")) {
                if (token.isBlank()) {
                    continue;
                }
                final String lower = Utils.lowercase(token);
                if (lower.equals(CommandArgumentConstants.FLAG_SILENT)
                        || lower.equals(CommandArgumentConstants.FLAG_SILENT_LONG)) {
                    silent = true;
                    continue;
                }
                if (lower.equals(CommandArgumentConstants.FLAG_FORCE)) {
                    force = true;
                    continue;
                }
                if (playerName != null) {
                    return null;
                }
                playerName = token;
            }
            return new TargetWithForce(new CommandArgumentConstants.Target(playerName, silent), force);
        }
    }
}
