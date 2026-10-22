package su.nightexpress.sunlight.moduleImpl.voting.model;

public enum VoteStatus {

    OPEN,
    SCHEDULED,
    CONCLUDED,
    CANCELLED;

    public boolean isOpen() {
        return this == OPEN;
    }

    public boolean isScheduled() {
        return this == SCHEDULED;
    }

    public boolean isTerminal() {
        return this == CONCLUDED || this == CANCELLED;
    }
}