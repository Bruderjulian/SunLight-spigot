package su.nightexpress.sunlight.hook.placeholder;

import org.bukkit.entity.Player;

import su.nightexpress.nightcore.util.LowerCase;

import java.util.HashMap;
import java.util.Map;

public class PlaceholderRegistry {

    private final Map<String, PlaceholderHandler> handlerMap = new HashMap<>();

    public record ParsedPlaceholder(String key, PlaceholderHandler handler, String payload) {
    }

    public void register(String key, PlaceholderHandler handler) {
        this.handlerMap.put(LowerCase.INTERNAL.apply(key), handler);
    }

    public String onPlaceholderRequest(Player player, String params) {
        ParsedPlaceholder parsed = this.findHandler(params);
        if (parsed == null)
            return null;

        return parsed.handler().handle(player, parsed.payload());
    }

    private ParsedPlaceholder findHandler(String params) {
        String currentKey = params;
        StringBuilder currentPayload = new StringBuilder();

        while (true) {
            if (this.handlerMap.containsKey(currentKey)) {
                return new ParsedPlaceholder(currentKey, this.handlerMap.get(currentKey), currentPayload.toString());
            }

            int lastUnderscoreIndex = currentKey.lastIndexOf('_');
            if (lastUnderscoreIndex == -1)
                return null;

            String suffix = currentKey.substring(lastUnderscoreIndex + 1);

            if (currentPayload.isEmpty()) {
                currentPayload = new StringBuilder(suffix);
            } else {
                currentPayload.insert(0, suffix + "_");
            }

            currentKey = currentKey.substring(0, lastUnderscoreIndex);
        }
    }
}
