package su.nightexpress.sunlight.moduleImpl.homes.command;

import java.util.List;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.homes.HomeDefaults;
import su.nightexpress.sunlight.moduleImpl.homes.HomesModule;
import su.nightexpress.sunlight.moduleImpl.homes.config.HomesLang;
import su.nightexpress.sunlight.moduleImpl.homes.config.HomesPerms;
import su.nightexpress.sunlight.moduleImpl.homes.impl.Home;

public class HomeCommonCommandProvider extends CommandProvider<HomesModule> {

    private static final String ARG_HOME = "home";

    private static final String COMMAND_DELETE = "delete";
    private static final String COMMAND_LIST = "list";
    private static final String COMMAND_SET = "set";
    private static final String COMMAND_TELEPORT = "teleport";
    private static final String COMMAND_VISIT = "visit";
    private static final String COMMAND_INVITE = "invite";

    public HomeCommonCommandProvider(final HomesModule module) {
        super(module, "homes-common");
    }

    @Override
    public void setup() {
        this.register(COMMAND_DELETE, List.of(), command -> command
                .withFullDescription(HomesLang.COMMAND_DELETE_HOME_DESC.text())
                .withPermission(HomesPerms.COMMAND_HOMES_DELETE.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withOptionalArguments(CommandArgumentConstants.string(ARG_HOME,
                        info -> this.ownHomeSuggestions(info.sender())))
                .executes(this::deleteHome));

        this.register(COMMAND_LIST, List.of(), command -> command
                .withFullDescription(HomesLang.COMMAND_HOME_LIST_DESC.text())
                .withPermission(HomesPerms.COMMAND_HOMES_LIST.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::listHomes));

        this.register(COMMAND_SET, List.of(), command -> command
                .withFullDescription(HomesLang.COMMAND_SET_HOME_DESC.text())
                .withPermission(HomesPerms.COMMAND_HOMES_SET.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withOptionalArguments(CommandArgumentConstants.string(CommandArgumentConstants.NAME,
                        info -> this.ownHomeSuggestions(info.sender())))
                .executes(this::setHome));

        this.register(COMMAND_TELEPORT, List.of(), command -> command
                .withFullDescription(HomesLang.COMMAND_TELEPORT_HOME_DESC.text())
                .withPermission(HomesPerms.COMMAND_HOMES_TELEPORT.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withOptionalArguments(CommandArgumentConstants.string(ARG_HOME,
                        info -> this.ownHomeSuggestions(info.sender())))
                .executes(this::teleportToHome));

        this.register(COMMAND_VISIT, List.of(), command -> command
                .withFullDescription(HomesLang.COMMAND_VISIT_HOME_DESC.text())
                .withPermission(HomesPerms.COMMAND_HOMES_VISIT.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(
                        CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                                info -> this.visitOwnerSuggestions(info.sender())),
                        CommandArgumentConstants.string(ARG_HOME,
                                info -> this.visitHomeSuggestions(info.sender())))
                .executes(this::visitHome));

        this.register(COMMAND_INVITE, List.of(), command -> command
                .withFullDescription(HomesLang.COMMAND_HOME_INVITE_DESC.text())
                .withPermission(HomesPerms.COMMAND_HOMES_INVITE.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> CommandArgumentConstants.onlinePlayerNames()))
                .withOptionalArguments(CommandArgumentConstants.string(ARG_HOME,
                        info -> this.ownHomeSuggestions(info.sender())))
                .executes(this::inviteHome));

        this.registerRoot("homes", command -> command
                .withFullDescription(HomesLang.COMMAND_HOMES_ROOT_DESC.text())
                .withPermission(HomesPerms.COMMAND_HOMES_ROOT.getName()));
    }

    private List<String> ownHomeSuggestions(final CommandSender sender) {
        if (!(sender instanceof final Player player)) {
            return List.of();
        }
        return this.module.getHomes(player).stream().map(Home::getId).toList();
    }

    private List<String> visitOwnerSuggestions(final CommandSender sender) {
        if (!(sender instanceof final Player player)) {
            return List.of();
        }
        return this.module.getRepository().getAll(home -> home.canVisit(player)).stream()
                .map(Home::getOwner).map(UserInfo::name).distinct().toList();
    }

    private List<String> visitHomeSuggestions(final CommandSender sender) {
        if (!(sender instanceof final Player player)) {
            return List.of();
        }
        return this.module.getRepository().getAll(home -> home.canVisit(player)).stream()
                .map(Home::getId).distinct().toList();
    }

    private Home getHomeOrDefault(final Player player, final CommandArguments arguments,
            final CommandSender sender) {
        final Object homeObj = arguments.get(ARG_HOME);
        if (homeObj instanceof final String homeId) {
            final Home home = this.module.getHome(player.getUniqueId(), homeId);
            if (home == null) {
                this.module.sendPrefixed(HomesLang.COMMAND_SYNTAX_INVALID_HOME, sender);
                return null;
            }
            return home;
        }

        final Home home = this.module.getFavoriteHome(player);
        if (home == null) {
            this.module.sendPrefixed(HomesLang.ERROR_NO_HOMES, sender);
            return null;
        }
        return home;
    }

    private int deleteHome(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final Home home = this.getHomeOrDefault(player, arguments, sender);
        if (home == null) {
            return 0;
        }

        return this.module.removeHome(player, home) ? 1 : 0;
    }

    private int listHomes(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, HomesPerms.COMMAND_HOMES_LIST_OTHERS.getName(),
                (user, targetPlayer) -> {
                    this.module.openHomes(player, user.getId());
                });
    }

    private int setHome(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final Object nameObj = arguments.get(CommandArgumentConstants.NAME);
        final String name = nameObj instanceof final String value ? value : HomeDefaults.DEFAULT_HOME_ID;

        return this.module.setHome(player, name, false) ? 1 : 0;
    }

    private int teleportToHome(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final Home home = this.getHomeOrDefault(player, arguments, sender);
        if (home == null) {
            return 0;
        }

        return this.module.teleportToHome(player, home) ? 1 : 0;
    }

    private int visitHome(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final Object ownerObj = arguments.get(CommandArgumentConstants.PLAYER);
        if (!(ownerObj instanceof final String ownerName)) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        final UUID ownerId = this.module.userManager().getRepository().getAssociatedId(ownerName);
        if (ownerId == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        final Object homeObj = arguments.get(ARG_HOME);
        if (!(homeObj instanceof final String homeId)) {
            this.module.sendPrefixed(HomesLang.COMMAND_SYNTAX_INVALID_HOME, sender);
            return 0;
        }

        final Home home = this.module.getHome(ownerId, homeId);
        if (home == null) {
            this.module.sendPrefixed(HomesLang.COMMAND_SYNTAX_INVALID_HOME, sender);
            return 0;
        }

        return this.module.visitHome(player, home) ? 1 : 0;
    }

    private int inviteHome(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }

        final Home home = this.getHomeOrDefault(player, arguments, sender);
        if (home == null) {
            return 0;
        }

        final Object targetObj = arguments.get(CommandArgumentConstants.PLAYER);
        if (!(targetObj instanceof final String targetName)) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        final UUID targetId = this.module.userManager().getRepository().getAssociatedId(targetName);
        if (targetId == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return this.module.inviteToHome(player, home, new UserInfo(targetId, targetName)) ? 1 : 0;
    }
}
