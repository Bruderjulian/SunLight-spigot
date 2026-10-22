package su.nightexpress.sunlight.module;

import su.nightexpress.sunlight.hook.HookId;
import su.nightexpress.sunlight.SLUtils;
import su.nightexpress.sunlight.utils.Utils;

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

    /**
     * Satisfies a TAB requirement in either supported layout.
     * <p>
     * When TAB is not here, it is on the proxy and TAB-Bridge carries this server's
     * values across. Bridge does not proxy the TAB API, so the values travel as
     * placeholders and Bridge resolves them through PlaceholderAPI, which makes PAPI a
     * hard requirement of that layout rather than a nicety.
     */
    public static LoadCondition tabOrBridge() {
        if (HookId.hasTAB()) {
            return LoadCondition.success();
        }
        if (HookId.hasTabBridge()) {
            return Utils.hasPlaceholderAPI() ? LoadCondition.success()
                    : LoadCondition.failure("%s is installed without %s, so it cannot render nametags. %s resolves %s placeholders on the proxy, so PlaceholderAPI is required when TAB runs on the proxy."
                            .formatted(HookId.TAB_BRIDGE, HookId.TAB, HookId.TAB_BRIDGE, HookId.TAB_BRIDGE));
        }
        return LoadCondition.failure("The %s plugin is required, either here or as %s when TAB runs on a proxy. Install TAB from https://github.com/NEZNAMY/TAB."
                .formatted(HookId.TAB, HookId.TAB_BRIDGE));
    }

    public boolean isSuccess() {
        return this.success;
    }

    public Optional<String> reason() {
        return Optional.ofNullable(this.reason);
    }
}
