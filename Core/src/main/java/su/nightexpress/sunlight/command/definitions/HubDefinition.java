package su.nightexpress.sunlight.command.definitions;

import java.util.Map;

public record HubDefinition(boolean enabled, String[] aliases, String name,
        Map<String, String> childrenAliases) {

}
