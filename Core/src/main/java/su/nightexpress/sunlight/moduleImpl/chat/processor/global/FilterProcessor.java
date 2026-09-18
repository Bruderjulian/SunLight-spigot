package su.nightexpress.sunlight.moduleImpl.chat.processor.global;

import su.nightexpress.sunlight.moduleImpl.chat.ChatModule;
import su.nightexpress.sunlight.moduleImpl.chat.context.ChatContext;
import su.nightexpress.sunlight.moduleImpl.chat.processor.ChatProcessor;
import su.nightexpress.sunlight.moduleImpl.chat.rule.WordFilter;

public class FilterProcessor implements ChatProcessor<ChatContext> {

    private final WordFilter filter;

    public FilterProcessor(WordFilter filter) {
        this.filter = filter;
    }

    @Override
    public void preProcess(ChatModule module, ChatContext context) {
        String message = context.getMessage();

        context.setMessage(this.filter.censor(message, '*'));
    }

    @Override
    public void postProcess(ChatModule module, ChatContext context) {

    }
}
