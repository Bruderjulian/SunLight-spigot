package su.nightexpress.sunlight.moduleImpl.rtp.command;

import org.bukkit.Bukkit;

import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.provider.type.AbstractCommandProvider;
import su.nightexpress.sunlight.moduleImpl.rtp.RTPModule;
import su.nightexpress.sunlight.moduleImpl.rtp.config.RTPLang;
import su.nightexpress.sunlight.moduleImpl.rtp.config.RTPPerms;

public class RTPCommandProvider extends AbstractCommandProvider {

    private final RTPModule module;

    public RTPCommandProvider(final SunLightPlugin plugin, final RTPModule module) {
        super(plugin);
        this.module = module;
    }

    @Override
    public void registerDefaults() {
        this.registerLiteral("rtp", true, new String[] { "rtp", "wild" }, builder -> builder
                .description(RTPLang.COMMAND_RTP_DESC)
                .permission(RTPPerms.COMMAND_RTP)
                .withArguments(Arguments.world("world").optional())
                .withArguments(Arguments.playerName("player")
                        .permission(RTPPerms.COMMAND_RTP_OTHERS)
                        .optional())
                .executes(this::execute));
    }

    private boolean execute(final CommandContext context, final ParsedArguments arguments) {
        return this.runForOnlinePlayer(context, arguments, this.module, target -> {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                final boolean result = this.module.getEngine().teleportToRandomPlace(target,
                        arguments.getWorld("world"));

                if (result && context.getSender() != target) {
                    this.module.sendPrefixed(RTPLang.COMMAND_RTP_OTHERS_SUCCESS, context.getSender(),
                            builder -> builder.with(CommonPlaceholders.PLAYER.resolver(target)));
                }
            });
            return true;
        });
    }
}