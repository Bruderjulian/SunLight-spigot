package su.nightexpress.sunlight.moduleImpl.chat.processor;

import su.nightexpress.sunlight.moduleImpl.chat.ChatModule;
import su.nightexpress.sunlight.moduleImpl.chat.context.ChatContext;

public interface ChatProcessor<T extends ChatContext> {

    void preProcess(ChatModule module, T context);

    void postProcess(ChatModule module, T context);
}
