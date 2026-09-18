package su.nightexpress.sunlight.command;

import su.nightexpress.nightcore.command.experimental.builder.SimpleFlagBuilder;
import su.nightexpress.nightcore.command.experimental.flag.FlagTypes;

public class CommandFlags {

    @Deprecated
    public static SimpleFlagBuilder silent() {
        return FlagTypes.simple(CommandArguments.FLAG_SILENT);
    }
}
