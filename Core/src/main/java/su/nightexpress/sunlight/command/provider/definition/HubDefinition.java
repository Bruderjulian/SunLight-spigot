package su.nightexpress.sunlight.command.provider.definition;

import java.util.Map;

public record HubDefinition(boolean enabled, String[] aliases, String name,
        Map<String, String> childrenAliases) {

}
