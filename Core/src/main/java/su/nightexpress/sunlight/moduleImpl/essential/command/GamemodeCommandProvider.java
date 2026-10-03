package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;
import su.nightexpress.sunlight.nms.SunNMS;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_TYPE;
import static su.nightexpress.sunlight.SLPlaceholders.PLAYER_DISPLAY_NAME;

public class GamemodeCommandProvider extends CommandProvider<EssentialModule> {

    private static final String COMMAND_SURVIVAL = "survival";
    private static final String COMMAND_SPECTATOR = "spectator";
    private static final String COMMAND_ADVENTURE = "adventure";
    private static final String COMMAND_CREATIVE = "creative";

    private static final Permission PERM_SURVIVAL = EssentialPerms.COMMAND.permission("gamemode.survival");
    private static final Permission PERM_SPECTATOR = EssentialPerms.COMMAND.permission("gamemode.spectator");
    private static final Permission PERM_ADVENTURE = EssentialPerms.COMMAND.permission("gamemode.adventure");
    private static final Permission PERM_CREATIVE = EssentialPerms.COMMAND.permission("gamemode.creative");
    private static final Permission PERM_OTEHRS = EssentialPerms.COMMAND.permission("gamemode.others");
    private static final Permission PERM_ROOT = EssentialPerms.COMMAND.permission("gamemode.root");

    private static final TextLocale DESCRIPTION_ROOT = LangEntry.builder("Command.GameMode.Root.Desc")
            .text("Game Mode commands.");
    private static final TextLocale DESCRIPTION_TYPE = LangEntry.builder("Command.GameMode.Type.Desc")
            .text("Set Game Mode to " + GENERIC_TYPE + ".");

    private static final MessageLocale MESSAGE_SET_NOTIFY = LangEntry.builder("Command.GameMode.Notify")
            .chatMessage(
                    GRAY.wrap("Your Game Mode has been set to " + SOFT_YELLOW.wrap(GENERIC_TYPE)
                            + "."));

    private static final MessageLocale MESSAGE_SET_FEEDBACK = LangEntry
            .builder("Command.GameMode.Target")
            .chatMessage(
                    GRAY.wrap("You have set " + WHITE.wrap(PLAYER_DISPLAY_NAME) + "'s Game Mode to "
                            + SOFT_YELLOW.wrap(GENERIC_TYPE) + "."));

    public GamemodeCommandProvider(final EssentialModule module) {
        super(module, "gamemode");
    }

    @Override
    public void setup() {
        this.register(COMMAND_SURVIVAL, List.of(), command -> this.buildTypeCommand(command, GameMode.SURVIVAL));
        this.register(COMMAND_SPECTATOR, List.of(), command -> this.buildTypeCommand(command, GameMode.SPECTATOR));
        this.register(COMMAND_ADVENTURE, List.of(), command -> this.buildTypeCommand(command, GameMode.ADVENTURE));
        this.register(COMMAND_CREATIVE, List.of(), command -> this.buildTypeCommand(command, GameMode.CREATIVE));

        this.registerRoot("gm", command -> command
                .withFullDescription(DESCRIPTION_ROOT.text())
                .withPermission(PERM_ROOT.getName()));
    }

    private void buildTypeCommand(final CommandAPICommand command, final GameMode mode) {
        final Permission permission = switch (mode) {
            case CREATIVE -> PERM_CREATIVE;
            case SURVIVAL -> PERM_SURVIVAL;
            case ADVENTURE -> PERM_ADVENTURE;
            case SPECTATOR -> PERM_SPECTATOR;
        };

        command
                .withFullDescription(DESCRIPTION_TYPE.text().replace(SLPlaceholders.GENERIC_TYPE,
                        Lang.GAME_MODE.getLocalized(mode)))
                .withPermission(permission.getName())
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.changeGameMode(sender, arguments, mode);
                });
    }

    private int changeGameMode(final CommandSender sender, final CommandArguments arguments, final GameMode mode) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, PERM_OTEHRS.getName(), (user, player) -> {
            if (player.isOnline()) {
                player.setGameMode(mode);
            } else {
                final SunNMS internals = this.module.plugin().getInternals();
                if (internals == null) {
                    this.module.sendPrefixed(Lang.ERROR_NO_INTERNALS_HANDLER, sender);
                    return;
                }
                internals.setGameMode(player, mode);
            }

            if (sender != player) {
                this.module.sendPrefixed(MESSAGE_SET_FEEDBACK, sender,
                        builder -> builder
                                .with(CommonPlaceholders.PLAYER.resolver(player))
                                .with(SLPlaceholders.GENERIC_TYPE, () -> Lang.GAME_MODE.getLocalized(mode)));
            }

            if (!target.silent()) {
                this.module.sendPrefixed(MESSAGE_SET_NOTIFY, player, builder -> builder
                        .with(SLPlaceholders.GENERIC_TYPE, () -> Lang.GAME_MODE.getLocalized(mode)));
            }
        });
    }
}
