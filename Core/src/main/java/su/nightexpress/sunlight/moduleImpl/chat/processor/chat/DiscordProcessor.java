package su.nightexpress.sunlight.moduleImpl.chat.processor.chat;

import su.nightexpress.sunlight.moduleImpl.chat.ChatModule;
import su.nightexpress.sunlight.moduleImpl.chat.context.MessageContext;
import su.nightexpress.sunlight.moduleImpl.chat.discord.DiscordHandler;
import su.nightexpress.sunlight.moduleImpl.chat.processor.MessageProcessor;

public class DiscordProcessor implements MessageProcessor {

    private final DiscordHandler discordHandler;

    public DiscordProcessor(DiscordHandler discordHandler) {
        this.discordHandler = discordHandler;
    }

    @Override
    public void preProcess(ChatModule module, MessageContext context) {

    }

    @Override
    public void postProcess(ChatModule module, MessageContext context) {
        this.discordHandler.sendToChannel(context.getChannel(), context.getMessage());
    }
}
