package su.nightexpress.sunlight.module.chat.processor.chat;

import org.jspecify.annotations.NonNull;

import su.nightexpress.sunlight.module.chat.ChatModule;
import su.nightexpress.sunlight.module.chat.context.MessageContext;
import su.nightexpress.sunlight.module.chat.discord.DiscordHandler;
import su.nightexpress.sunlight.module.chat.processor.MessageProcessor;

public class DiscordProcessor implements MessageProcessor {

    private final DiscordHandler discordHandler;

    public DiscordProcessor( DiscordHandler discordHandler) {
        this.discordHandler = discordHandler;
    }

    @Override
    public void preProcess( ChatModule module,  MessageContext context) {

    }

    @Override
    public void postProcess( ChatModule module,  MessageContext context) {
        this.discordHandler.sendToChannel(context.getChannel(), context.getMessage());
    }
}
