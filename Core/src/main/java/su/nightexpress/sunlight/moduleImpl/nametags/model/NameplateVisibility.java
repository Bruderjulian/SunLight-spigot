package su.nightexpress.sunlight.moduleImpl.nametags.model;

/**
 * Which parts of a player's own nameplate they want rendered.
 * <p>
 * A toggle only ever suppresses what this player sees of their own plate; it never hides
 * the player from anyone else. With every part switched off the client falls back to the
 * bare player name.
 * <p>
 * {@code showAll} is the master switch: the per-part flags are ANDed with it, so turning
 * the master off and on again restores the individual choices.
 * <p>
 * Gson friendly: a plain record with primitive components round-trips through the user
 * {@code properties} JSON column. A stored value from before this existed, or a partial
 * one, deserialises to {@code false} components, so {@link #normalize()} repairs it.
 */
public record NameplateVisibility(boolean showRank, boolean showTag, boolean showTeam, boolean showAll) {

    /** Everything visible, which is what a player gets until they change something. */
    public static final NameplateVisibility DEFAULT = new NameplateVisibility(true, true, true, true);

    /**
     * Repairs a value that may have come from an incomplete or absent stored record.
     * A player who has never touched a toggle must not end up with a blank nametag.
     */
    public @org.jetbrains.annotations.NotNull NameplateVisibility normalize() {
        return this.showRank || this.showTag || this.showTeam || this.showAll ? this : DEFAULT;
    }

    public @org.jetbrains.annotations.NotNull NameplateVisibility withRank(boolean value) {
        return new NameplateVisibility(value, this.showTag, this.showTeam, this.showAll);
    }

    public @org.jetbrains.annotations.NotNull NameplateVisibility withTag(boolean value) {
        return new NameplateVisibility(this.showRank, value, this.showTeam, this.showAll);
    }

    public @org.jetbrains.annotations.NotNull NameplateVisibility withTeam(boolean value) {
        return new NameplateVisibility(this.showRank, this.showTag, value, this.showAll);
    }

    public @org.jetbrains.annotations.NotNull NameplateVisibility withAll(boolean value) {
        return new NameplateVisibility(this.showRank, this.showTag, this.showTeam, value);
    }

    public @org.jetbrains.annotations.NotNull NameplateVisibility toggleRank() {
        return this.withRank(!this.showRank);
    }

    public @org.jetbrains.annotations.NotNull NameplateVisibility toggleTag() {
        return this.withTag(!this.showTag);
    }

    public @org.jetbrains.annotations.NotNull NameplateVisibility toggleTeam() {
        return this.withTeam(!this.showTeam);
    }

    public @org.jetbrains.annotations.NotNull NameplateVisibility toggleAll() {
        return this.withAll(!this.showAll);
    }

    public boolean wantsRank() {
        return this.showAll && this.showRank;
    }

    public boolean wantsTag() {
        return this.showAll && this.showTag;
    }

    public boolean wantsTeam() {
        return this.showAll && this.showTeam;
    }
}
