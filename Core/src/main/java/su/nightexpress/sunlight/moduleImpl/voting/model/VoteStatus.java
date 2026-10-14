package su.nightexpress.sunlight.moduleImpl.voting.model;

public enum VoteStatus {

    OPEN,
    CONCLUDED,
    CANCELLED;

    public boolean isOpen() {
        return this == OPEN;
    }

    public boolean isTerminal() {
        return this != OPEN;
    }
}