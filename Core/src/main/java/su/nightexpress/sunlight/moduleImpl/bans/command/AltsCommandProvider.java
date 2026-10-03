package su.nightexpress.sunlight.moduleImpl.bans.command;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.bans.BansModule;
import su.nightexpress.sunlight.moduleImpl.bans.config.BansLang;
import su.nightexpress.sunlight.moduleImpl.bans.config.BansPerms;
import su.nightexpress.sunlight.utils.Utils;

public class AltsCommandProvider extends CommandProvider<BansModule> {

    public AltsCommandProvider(BansModule module) {
        super(module, "bans-alts");
    }

    @Override
    public void setup() {
        this.register("alts", List.of(), command -> command
                .withFullDescription(BansLang.COMMAND_ALTS_DESC.text())
                .withPermission(BansPerms.COMMAND_ALTS.getName())
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> CommandArgumentConstants.onlinePlayerNames()))
                .executes(this::showAlts));
    }

    private int showAlts(CommandSender sender, CommandArguments arguments) {
        final String playerName = (String) arguments.get(CommandArgumentConstants.PLAYER);
        if (playerName == null || playerName.isBlank()) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        this.module.userManager().loadTargetProfile(playerName).thenCompose(profile -> {
            if (profile == null) {
                this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                return CompletableFuture.completedFuture(null);
            }

            return this.module.userManager().loadInetAddress(profile.id()).thenCompose(address -> {
                if (address == null) {
                    this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                    return CompletableFuture.completedFuture(null);
                }

                return this.module.lookupAltProfiles(address).thenAcceptAsync(alts -> {
                    this.module.notifyAltProfiles(sender, profile, address, alts);
                }, this.module.plugin()::runTask);
            });
        }).whenComplete(Utils::printStacktrace);

        return 1;
    }
}
