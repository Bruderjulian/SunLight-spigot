package su.nightexpress.sunlight.moduleImpl.profiles;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * File persistence for profiles: one YAML file per player under the module
 * folder ({@code data/<uuid>.yml}).
 */
public class ProfileStore {

    private final File directory;

    public ProfileStore(File moduleFolder) {
        this.directory = new File(moduleFolder, "data");
        if (!this.directory.exists()) {
            this.directory.mkdirs();
        }
    }

    public synchronized Map<String, PlayerProfile> load(UUID ownerId) {
        Map<String, PlayerProfile> profiles = new LinkedHashMap<>();
        File file = this.file(ownerId);
        if (!file.exists()) return profiles;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("profiles");
        if (root == null) return profiles;

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            try {
                PlayerProfile profile = PlayerProfile.deserialize(ownerId, id, section.getValues(false));
                if (profile != null) {
                    profiles.put(id, profile);
                }
            } catch (Exception ignored) {
            }
        }
        return profiles;
    }

    public synchronized void save(UUID ownerId, Map<String, PlayerProfile> profiles) {
        File file = this.file(ownerId);
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, PlayerProfile> entry : profiles.entrySet()) {
            config.set("profiles." + entry.getKey(), entry.getValue().serialize());
        }
        try {
            config.save(file);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public synchronized void delete(UUID ownerId) {
        File file = this.file(ownerId);
        if (file.exists()) {
            file.delete();
        }
    }

    private File file(UUID ownerId) {
        return new File(this.directory, ownerId + ".yml");
    }
}
