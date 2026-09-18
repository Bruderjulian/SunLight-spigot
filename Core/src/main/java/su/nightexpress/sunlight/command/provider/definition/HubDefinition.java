package su.nightexpress.sunlight.command.provider.definition;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record HubDefinition(boolean enabled, String[] aliases, String name,
    Map<String, String> childrenAliases) {

}
