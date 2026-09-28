package su.nightexpress.sunlight.moduleImpl.glow;

public enum GlowType {

    STATIC,
    PHASED;

    public boolean isAnimated() {
        return this == PHASED;
    }
}
