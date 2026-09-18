package su.nightexpress.sunlight.moduleImpl.chat.processor.global;

import su.nightexpress.nightcore.util.text.night.NightMessage;
import su.nightexpress.sunlight.moduleImpl.chat.ChatModule;
import su.nightexpress.sunlight.moduleImpl.chat.context.ChatContext;
import su.nightexpress.sunlight.moduleImpl.chat.processor.ChatProcessor;

public class ColorProcessor implements ChatProcessor<ChatContext> {

    @Override
    public void preProcess(ChatModule module, ChatContext context) {
        context.setMessage(NightMessage.stripTags(context.getMessage())); // Strip all legacy colors (+ all possible
                                                                          // tags)
    }

    @Override
    public void postProcess(ChatModule module, ChatContext context) {

    }
}
