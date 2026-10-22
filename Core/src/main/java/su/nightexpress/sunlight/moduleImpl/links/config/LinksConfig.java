package su.nightexpress.sunlight.moduleImpl.links.config;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinkDefaults;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class LinksConfig {

    public static final String PATH_LINKS = "Links";

    private LinksConfig() {
    }

    /**
     * Writes any missing default link into the config, then reads back every configured link.
     * <p>
     * Unlike a per-object file layout, a single malformed hand edit can take out the whole section, so
     * every link is read independently and a broken one is dropped with a warning instead of aborting
     * the whole load.
     *
     * @param config The module config.
     * @param warn   Warning sink, normally the module's {@code warn(String)}.
     * @return The links, keyed by lower-case ID, in config order.
     */
    public static Map<String, Link> readLinks(FileConfig config, Consumer<String> warn) {
        Map<String, Link> defaults = LinkDefaults.createDefaults();
        defaults.forEach((id, link) -> {
            String path = PATH_LINKS + "." + id;
            if (!config.contains(path)) {
                link.write(config, path);
            }
        });

        Map<String, Link> map = new LinkedHashMap<>();
        for (String id : config.getSection(PATH_LINKS)) {
            String key = id.toLowerCase(Locale.ROOT);
            if (!LinkDefaults.isValidId(key)) {
                if (LinkDefaults.isReservedId(key)) {
                    warn.accept("Link ID '%s' is reserved by the Links commands and was skipped. Rename it."
                            .formatted(id));
                } else {
                    warn.accept("Link ID '%s' is invalid (allowed: a-z, 0-9 and underscore, max 32 chars) and was skipped."
                            .formatted(id));
                }
                continue;
            }

            if (!id.equals(key)) {
                warn.accept("Link ID '%s' contains upper-case characters and was loaded as '%s'. Rename it to lower-case to silence this warning."
                        .formatted(id, key));
            }

            if (map.containsKey(key)) {
                warn.accept("Duplicate link ID '%s' (case-insensitive) and was skipped.".formatted(id));
                continue;
            }

            Link link = new Link(key, key, "");
            String linkPath = PATH_LINKS + "." + id;
            link.load(config, linkPath);

            if (!link.isActionable()) {
                warn.accept("Link '%s' has neither a URL nor a command. It is hidden from players until one is set."
                        .formatted(key));
            }

            warnFeedbackIssues(config, linkPath, link, warn);

            map.put(key, link);
        }

        if (map.isEmpty()) {
            warn.accept("No valid links configured. Falling back to the default links.");
            return new LinkedHashMap<>(defaults);
        }

        return map;
    }

    /**
     * Warns about feedback values that will silently do nothing at runtime. The link itself is kept:
     * a typo'd sound or particle must not take the whole link down.
     */
    private static void warnFeedbackIssues(FileConfig config, String linkPath, Link link, Consumer<String> warn) {
        if (link.hasSound()) {
            String name = LinksModule.soundName(link.getSound());
            if (!LinksModule.isVanillaSound(name)) {
                warn.accept("Link '%s' uses sound '%s', which is not a vanilla sound. If it is not a custom resource-pack sound, it will be silent."
                        .formatted(link.getId(), name));
            }
        }

        String configuredParticle = config.getString(linkPath + ".Particle.Name", "");
        if (configuredParticle.isBlank()) {
            configuredParticle = config.getString(linkPath + ".Particle-Type", "");
        }
        if (!configuredParticle.isBlank() && !link.hasParticle()) {
            warn.accept("Link '%s' has invalid particle '%s' and it was disabled.".formatted(link.getId(),
                    configuredParticle.trim()));
        }
    }
}
