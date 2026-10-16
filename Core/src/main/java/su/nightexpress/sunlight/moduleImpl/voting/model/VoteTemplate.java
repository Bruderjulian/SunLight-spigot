package su.nightexpress.sunlight.moduleImpl.voting.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import su.nightexpress.nightcore.config.FileConfig;

/**
 * An admin-defined safe poll shape players may instantiate without staff permissions.
 * <p>
 * Templates lock everything except the key: options, duration and silent flag come from
 * the template, rules come from the server defaults. Players can never set their own
 * outcome commands through a template.
 */
public class VoteTemplate {

    public record TemplateOption(String key, String display, String icon) {
    }

    private final String id;
    private String display;
    private String description;
    private int durationSeconds;
    private boolean silent;
    private boolean playerAllowed;
    private final List<TemplateOption> options;

    public VoteTemplate(final String id) {
        this.id = id.toLowerCase(Locale.ROOT);
        this.display = id;
        this.description = "";
        this.durationSeconds = 3600;
        this.silent = true;
        this.playerAllowed = true;
        this.options = new ArrayList<>();
    }

    public static VoteTemplate read(final FileConfig config, final String path, final String id) {
        final VoteTemplate template = new VoteTemplate(id);
        template.display = config.getString(path + ".Display", id);
        template.description = config.getString(path + ".Description", "");
        template.durationSeconds = Math.max(1, config.getInt(path + ".Duration-Seconds", 3600));
        template.silent = config.getBoolean(path + ".Silent", true);
        template.playerAllowed = config.getBoolean(path + ".Player-Allowed", true);
        template.options.clear();
        for (final String raw : config.getStringList(path + ".Options")) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            // Format: "key = display" with optional " | icon" suffix.
            final int eq = raw.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            final String key = raw.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty()) {
                continue;
            }
            String rest = raw.substring(eq + 1).trim();
            if (rest.isEmpty()) {
                continue;
            }
            String display = rest;
            String icon = "PAPER";
            final int pipe = rest.lastIndexOf('|');
            if (pipe >= 0) {
                display = rest.substring(0, pipe).trim();
                final String iconRaw = rest.substring(pipe + 1).trim();
                if (!iconRaw.isEmpty()) {
                    icon = iconRaw.toUpperCase(Locale.ROOT);
                }
                if (display.isEmpty()) {
                    display = key;
                }
            }
            template.options.add(new TemplateOption(key, display, icon));
        }
        return template;
    }

    public void write(final FileConfig config, final String path) {
        config.set(path + ".Display", this.display);
        config.set(path + ".Description", this.description);
        config.set(path + ".Duration-Seconds", this.durationSeconds);
        config.set(path + ".Silent", this.silent);
        config.set(path + ".Player-Allowed", this.playerAllowed);
        final List<String> raw = new ArrayList<>();
        for (final TemplateOption option : this.options) {
            raw.add(option.key() + " = " + option.display() + " | " + option.icon());
        }
        config.set(path + ".Options", raw);
    }

    public static Map<String, VoteTemplate> readAll(final FileConfig config, final String basePath) {
        final Map<String, VoteTemplate> map = new LinkedHashMap<>();
        for (final String id : config.getSection(basePath)) {
            final String key = id.toLowerCase(Locale.ROOT);
            if (map.containsKey(key)) {
                continue;
            }
            final VoteTemplate template = read(config, basePath + "." + id, key);
            if (template.options.size() < 2) {
                continue;
            }
            map.put(key, template);
        }
        return map;
    }

    public static void writeDefaults(final FileConfig config, final String basePath) {
        if (config.contains(basePath)) {
            return;
        }
        final VoteTemplate mapVote = new VoteTemplate("map-vote");
        mapVote.display = "Map Vote";
        mapVote.description = "Vote for the next map.";
        mapVote.durationSeconds = 300;
        mapVote.silent = false;
        mapVote.playerAllowed = true;
        mapVote.options.add(new TemplateOption("yes", "Yes", "LIME_DYE"));
        mapVote.options.add(new TemplateOption("no", "No", "RED_DYE"));
        mapVote.write(config, basePath + ".map-vote");

        final VoteTemplate eventVote = new VoteTemplate("event-vote");
        eventVote.display = "Event Vote";
        eventVote.description = "Vote for tonight's event.";
        eventVote.durationSeconds = 600;
        eventVote.silent = true;
        eventVote.playerAllowed = true;
        eventVote.options.add(new TemplateOption("pvp", "PvP Tournament", "DIAMOND_SWORD"));
        eventVote.options.add(new TemplateOption("build", "Build Contest", "BRICKS"));
        eventVote.options.add(new TemplateOption("drop", "Drop Party", "CHEST"));
        eventVote.write(config, basePath + ".event-vote");
    }

    public String getId() {
        return this.id;
    }

    public String getDisplay() {
        return this.display == null || this.display.isBlank() ? this.id : this.display;
    }

    public String getDescription() {
        return this.description == null ? "" : this.description;
    }

    public int getDurationSeconds() {
        return Math.max(1, this.durationSeconds);
    }

    public boolean isSilent() {
        return this.silent;
    }

    public boolean isPlayerAllowed() {
        return this.playerAllowed;
    }

    public List<TemplateOption> getOptions() {
        return List.copyOf(this.options);
    }
}
