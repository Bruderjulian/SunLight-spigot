package su.nightexpress.sunlight.command.provider.definition;

import org.jetbrains.annotations.NotNull;

public record LiteralDefinition(boolean enabled, String[] aliases, int cooldown, double cost) {

}
