package su.nightexpress.sunlight.module;

import su.nightexpress.sunlight.hook.HookId;
import su.nightexpress.sunlight.SLUtils;

import java.util.Optional;

public class LoadCondition {

    private static final LoadCondition SUCCESS = new LoadCondition(true, null);

    private final boolean success;
    private final String reason;

    private LoadCondition(boolean success, String reason) {
        this.success = success;
        this.reason = reason;
    }

    public static LoadCondition success() {
        return SUCCESS;
    }

    public static LoadCondition failure(String reason) {
        return new LoadCondition(false, reason);
    }

    public static LoadCondition packetLibrary() {
        return SLUtils.hasPacketLibrary() ? LoadCondition.success()
                : LoadCondition.failure("No packet library plugin installed. Install %s or %s for the module to work."
                        .formatted(HookId.PACKET_EVENTS, HookId.PROTOCOL_LIB));
    }

    public static LoadCondition tab() {
        return HookId.hasTAB() ? LoadCondition.success()
                : LoadCondition.failure("The %s plugin is required. Install TAB from https://github.com/NEZNAMY/TAB."
                        .formatted(HookId.TAB));
    }

    public boolean isSuccess() {
        return this.success;
    }

    public Optional<String> reason() {
        return Optional.ofNullable(this.reason);
    }
}
