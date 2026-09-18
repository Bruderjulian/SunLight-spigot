package su.nightexpress.sunlight.moduleImpl.chat.processor.conversation;

import org.bukkit.entity.Player;

import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.moduleImpl.chat.ChatModule;
import su.nightexpress.sunlight.moduleImpl.chat.context.ConversationContext;
import su.nightexpress.sunlight.moduleImpl.chat.processor.ChatProcessor;

public class ConversationProcessor implements ChatProcessor<ConversationContext> {

    @Override
    public void preProcess(ChatModule module, ConversationContext context) {

    }

    @Override
    public void postProcess(ChatModule module, ConversationContext context) {
        String rawIncoming = module.getSettings().getConversationIncomingFormat();
        String rawOutgoing = module.getSettings().getConversationOutgoingFormat();

        String inFormatted = this.format(context, context.getPlayer(), rawIncoming);
        String outFormatted = this.format(context, context.getTarget(), rawOutgoing);

        Players.sendMessage(context.getTarget(), inFormatted);
        Players.sendMessage(context.getPlayer(), outFormatted);

        if (module.getSettings().isConversationSoundsEnabled()) {
            module.getSettings().getConversationIncomingSound().play(context.getTarget());
            module.getSettings().getConversationOutgoingSound().play(context.getPlayer());
        }

        context.getCache().setLastConversationWith(context.getTarget().getUniqueId());
        module.getChatCache(context.getTarget()).setLastConversationWith(context.getPlayer().getUniqueId());
    }

    private String format(ConversationContext context, Player player, String rawFormat) {
        PlaceholderContext messageContext = PlaceholderContext.builder()
                .with(SLPlaceholders.GENERIC_MESSAGE, context::getMessage)
                .build();

        PlaceholderContext formatContext = PlaceholderContext.builder()
                .maxRecursion(1) // To apply player placeholders for proxy format as well.
                .with(SLPlaceholders.GENERIC_FORMAT, context::getFormat)
                .with(CommonPlaceholders.PLAYER.resolver(player))
                .andThen(CommonPlaceholders.forPlaceholderAPI(player))
                .build();

        String formatted = formatContext.apply(rawFormat);

        return messageContext.apply(formatted);
    }
}
