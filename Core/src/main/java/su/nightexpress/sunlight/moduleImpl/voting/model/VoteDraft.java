package su.nightexpress.sunlight.moduleImpl.voting.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An unpersisted poll being built in the creation wizard.
 * <p>
 * Never touches the database until the player confirms: the wizard is a different door
 * into the same {@code VotingModule#create} validation path, not a second implementation.
 */
public class VoteDraft {

    private String key = "";
    private String question = "";
    private String description = "";
    private int durationSeconds = 3600;
    private boolean silent;
    private final Map<String, String> options = new LinkedHashMap<>();

    public VoteDraft() {
    }

    public VoteDraft(final boolean silentDefault) {
        this.silent = silentDefault;
    }

    public boolean isValid() {
        return !this.key.isBlank() && !this.question.isBlank()
                && this.durationSeconds > 0 && this.options.size() >= 2;
    }

    public String validationError() {
        if (this.key.isBlank()) {
            return "Set a key first.";
        }
        if (this.question.isBlank()) {
            return "Set a question first.";
        }
        if (this.durationSeconds <= 0) {
            return "Duration must be positive.";
        }
        if (this.options.size() < 2) {
            return "Add at least two choices.";
        }
        return "";
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(final String key) {
        this.key = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }

    public String getQuestion() {
        return this.question;
    }

    public void setQuestion(final String question) {
        this.question = question == null ? "" : question.trim();
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(final String description) {
        this.description = description == null ? "" : description.trim();
    }

    public int getDurationSeconds() {
        return this.durationSeconds;
    }

    public void setDurationSeconds(final int durationSeconds) {
        this.durationSeconds = Math.max(1, durationSeconds);
    }

    public boolean isSilent() {
        return this.silent;
    }

    public void setSilent(final boolean silent) {
        this.silent = silent;
    }

    public Map<String, String> getOptions() {
        return Map.copyOf(this.options);
    }

    public List<String> getOptionKeys() {
        return new ArrayList<>(this.options.keySet());
    }

    public boolean addOption(final String keyRaw, final String displayRaw) {
        if (keyRaw == null || keyRaw.isBlank()) {
            return false;
        }
        final String key = keyRaw.trim().toLowerCase(Locale.ROOT);
        if (this.options.containsKey(key)) {
            return false;
        }
        final String display = displayRaw == null || displayRaw.isBlank() ? key : displayRaw.trim();
        this.options.put(key, display);
        return true;
    }

    public boolean removeOption(final String keyRaw) {
        if (keyRaw == null) {
            return false;
        }
        return this.options.remove(keyRaw.trim().toLowerCase(Locale.ROOT)) != null;
    }

    public void clear() {
        this.key = "";
        this.question = "";
        this.description = "";
        this.durationSeconds = 3600;
        this.options.clear();
    }
}
