package su.nightexpress.sunlight.moduleImpl.glow;

public enum GlowType {

    STATIC,
    CYCLE,
    GRADIENT,
    RAINBOW,
    FLASH;

    public boolean isAnimated() {
        return this != STATIC;
    }

    public boolean isPingPong() {
        return this == GRADIENT;
    }
}
