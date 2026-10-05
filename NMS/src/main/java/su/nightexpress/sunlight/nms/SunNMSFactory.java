package su.nightexpress.sunlight.nms;

import java.util.Map;

import su.nightexpress.nightcore.util.Version;

/**
 * Resolves the {@link SunNMS} implementation for the running server version.
 * <p>
 * The lookup is reflective on purpose. Every version-specific implementation is
 * compiled against a different Minecraft server and cannot even be linked on a
 * version it was not built for, so the plugin must not reference them directly:
 * as soon as a class constant pool names one of them, class loading or
 * verification can fail with {@link NoClassDefFoundError} before the right
 * implementation has been chosen. Resolving by name means only the
 * implementation matching the current server is ever loaded.
 */
public final class SunNMSFactory {

    private static final String NMS_1_21_11 = "su.nightexpress.sunlight.nms.mc_1_21_11.MC_1_21_11";
    private static final String NMS_26_1     = "su.nightexpress.sunlight.nms.v26p1.NMSv26p1";

    /**
     * Version implementations to load. A version that is absent falls back to the
     * newest implementation, which is how a newer server version keeps working
     * until an explicit mapping is added.
     */
    private static final Map<Version, String> IMPLEMENTATIONS = Map.of(
        Version.MC_1_21_11, NMS_1_21_11
    );

    private static final String FALLBACK = NMS_26_1;

    private SunNMSFactory() {
    }

    /**
     * Creates the implementation for the given server version.
     *
     * @param version the current server version
     * @return a new implementation instance
     * @throws ReflectiveOperationException if the implementation is absent from the artifact
     *                                       or cannot be instantiated
     */
    public static SunNMS create(Version version) throws ReflectiveOperationException {
        return (SunNMS) Class.forName(resolve(version)).getDeclaredConstructor().newInstance();
    }

    private static String resolve(Version version) {
        return IMPLEMENTATIONS.getOrDefault(version, FALLBACK);
    }
}
