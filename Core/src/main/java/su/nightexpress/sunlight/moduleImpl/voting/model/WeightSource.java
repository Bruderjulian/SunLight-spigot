package su.nightexpress.sunlight.moduleImpl.voting.model;

public enum WeightSource {

    PERMISSION,
    RANK,
    BOTH;

    public boolean hasPermission() {
        return this == PERMISSION || this == BOTH;
    }

    public boolean hasRank() {
        return this == RANK || this == BOTH;
    }
}