package su.nightexpress.sunlight.moduleImpl.chat.processor.command;

import org.bukkit.entity.Player;

import su.nightexpress.nightcore.util.TimeUtil;
import su.nightexpress.sunlight.moduleImpl.chat.ChatModule;
import su.nightexpress.sunlight.moduleImpl.chat.cache.UserChatCache;
import su.nightexpress.sunlight.moduleImpl.chat.context.CommandContext;
import su.nightexpress.sunlight.moduleImpl.chat.core.ChatLang;
import su.nightexpress.sunlight.moduleImpl.chat.processor.ChatProcessor;

public class CommandCooldownProcessor implements ChatProcessor<CommandContext> {

    @Override
    public void preProcess(ChatModule module, CommandContext context) {
        Player player = context.getPlayer();
        UserChatCache cache = context.getCache();

        long nextCommandTimestamp = cache.getNextCommandTimestamp();
        if (TimeUtil.isPassed(nextCommandTimestamp))
            return;

        module.sendPrefixed(ChatLang.ANTI_FLOOD_COMMAND_COOLDOWN, player);
        context.cancel();
    }

    @Override
    public void postProcess(ChatModule module, CommandContext context) {
        context.getCache().setNextCommandTimestamp(module.getSettings().getAntiFloodCommandCooldown());
    }
}
