package su.nightexpress.sunlight.moduleImpl.profiles;

/**
 * Declares whether a piece of game state is stored once per account
 * ({@code GLOBAL}) or snapshotted separately for every profile
 * ({@code PER_PROFILE}).
 */
public enum ProfileScope {

    GLOBAL,
    PER_PROFILE;

    public boolean isPerProfile() {
        return this == PER_PROFILE;
    }

    public static ProfileScope fromString(String raw, ProfileScope fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        String normalized = raw.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "per_profile", "per-profile", "profile", "perprofile" -> PER_PROFILE;
            case "global", "shared", "account" -> GLOBAL;
            default -> fallback;
        };
    }
}
