package su.nightexpress.sunlight.moduleImpl.homes.command;

import java.util.List;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.homes.HomeDefaults;
import su.nightexpress.sunlight.moduleImpl.homes.HomePlaceholders;
import su.nightexpress.sunlight.moduleImpl.homes.HomesModule;
import su.nightexpress.sunlight.moduleImpl.homes.config.HomesLang;
import su.nightexpress.sunlight.moduleImpl.homes.config.HomesPerms;
import su.nightexpress.sunlight.moduleImpl.homes.impl.Home;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.Utils;

public class HomeAdminCommandProvider extends CommandProvider<HomesModule> {

    private static final String COMMAND_CREATE = "create";
    private static final String COMMAND_DELETE = "delete";

    public HomeAdminCommandProvider(final HomesModule module) {
        super(module, "homes-admin");
    }

    @Override
    public void setup() {
        this.register(COMMAND_DELETE, List.of(), command -> command
                .withFullDescription(HomesLang.COMMAND_ADMIN_DELETE_HOME_DESC.text())
                .withPermission(HomesPerms.COMMAND_HOMES_DELETE_OTHERS.getName())
                .withArguments(
                        CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                                info -> CommandArgumentConstants.onlinePlayerNames()),
                        CommandArgumentConstants.string(CommandArgumentConstants.NAME,
                                info -> this.allHomeIdSuggestions()))
                .executes(this::deleteHome));

        this.register(COMMAND_CREATE, List.of(), command -> command
                .withFullDescription(HomesLang.COMMAND_ADMIN_CREATE_HOME_DESC.text())
                .withPermission(HomesPerms.COMMAND_HOMES_SET_OTHERS.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> CommandArgumentConstants.onlinePlayerNames()))
                .withOptionalArguments(CommandArgumentConstants.string(CommandArgumentConstants.NAME,
                        info -> this.allHomeIdSuggestions()))
                .executes(this::createHome));

        this.registerRoot("homesadmin", command -> command
                .withFullDescription(HomesLang.COMMAND_ADMIN_ROOT_DESC.text())
                .withPermission(HomesPerms.COMMAND_HOMES_ADMIN_ROOT.getName()));
    }

    private List<String> allHomeIdSuggestions() {
        return this.module.getRepository().getAll().stream().map(Home::getId).distinct().toList();
    }

    private int createHome(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final Object userObj = arguments.get(CommandArgumentConstants.PLAYER);
        if (!(userObj instanceof final String userName)) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        final Object homeObj = arguments.get(CommandArgumentConstants.NAME);
        final String homeId = homeObj instanceof final String value ? value : HomeDefaults.DEFAULT_HOME_ID;

        final UUID playerId = this.module.userManager().getRepository().getAssociatedId(userName);
        if (playerId == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        final Home home = this.module.getHome(playerId, homeId);
        if (home == null) {
            this.module.createHome(homeId, new UserInfo(playerId, userName), player.getLocation());
            this.module.sendPrefixed(HomesLang.ADMIN_HOME_CREATE_FEEDBACK, player, builder -> builder
                    .with(HomePlaceholders.HOME_ID, () -> homeId)
                    .with(CommonPlaceholders.PLAYER_NAME, () -> userName));
            return 1;
        }

        home.updateLocation(player.getLocation());
        home.markDirty();

        this.module.sendPrefixed(HomesLang.ADMIN_HOME_MOVE_FEEDBACK, player, builder -> builder
                .with(HomePlaceholders.HOME_ID, () -> homeId)
                .with(CommonPlaceholders.PLAYER_NAME, () -> userName));

        return 1;
    }

    private int deleteHome(final CommandSender sender, final CommandArguments arguments) {
        final Object userObj = arguments.get(CommandArgumentConstants.PLAYER);
        if (!(userObj instanceof final String userName)) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        final Object homeObj = arguments.get(CommandArgumentConstants.NAME);
        if (!(homeObj instanceof final String homeId)) {
            this.module.sendPrefixed(HomesLang.COMMAND_SYNTAX_INVALID_HOME, sender);
            return 0;
        }

        this.module.userManager().loadByNameAsync(userName).thenAccept(userOptional -> {
            final SunUser user = userOptional.orElse(null);
            if (user == null) {
                this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                return;
            }

            final Home home = this.module.getHome(user.getId(), homeId);
            if (home == null) {
                this.module.sendPrefixed(HomesLang.ADMIN_HOME_DELETE_ERROR_NO_HOME, sender,
                        builder -> builder
                                .with(HomePlaceholders.HOME_ID, () -> homeId));
                return;
            }

            this.module.deleteHome(home);

            this.module.sendPrefixed(HomesLang.ADMIN_HOME_DELETE_FEEDBACK, sender, replacer -> replacer
                    .with(home.placeholders())
                    .with(CommonPlaceholders.PLAYER_NAME, user::getName));
        }).whenComplete(Utils::printStacktrace);

        return 1;
    }
}
