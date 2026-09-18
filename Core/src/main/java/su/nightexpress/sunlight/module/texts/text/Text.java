package su.nightexpress.sunlight.module.texts.text;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.FileUtil;
import su.nightexpress.nightcore.util.LowerCase;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.sunlight.module.texts.TextsPerms;

import java.nio.file.Path;
import java.util.List;

public class Text {

    private final Path file;
    private final String id;
    private final String description;
    private final List<String> text;

    public Text(Path file, String id, String description, List<String> text) {
        this.file = file;
        this.id = id;
        this.description = description;
        this.text = text;
    }

    public static Text fromFile(Path filePath) {
        FileConfig config = FileConfig.load(filePath);
        String id = LowerCase.INTERNAL.apply(FileUtil.getNameWithoutExtension(filePath));

        String description = config.getString("Description", "");
        List<String> text = config.getStringList("Text");

        return new Text(filePath, id, description, text);
    }

    public void write(FileConfig config) {
        config.set("Description", this.description);
        config.set("Text", this.text);
    }

    public boolean hasPermission(CommandSender sender) {
        return TextsPerms.TEXT.hasChildAccess(sender, this.id);
    }

    public Path getFile() {
        return this.file;
    }

    public String getId() {
        return this.id;
    }

    public String getPermission() {
        return TextsPerms.TEXT.childrenNode(this.id);
    }

    public String getDescription() {
        return this.description;
    }

    public List<String> getText() {
        return this.text;
    }

    public List<String> getText(Player player) {
        PlaceholderContext context = PlaceholderContext.builder()
                .with(CommonPlaceholders.PLAYER.resolver(player))
                .andThen(CommonPlaceholders.forPlaceholderAPI(player))
                .build();

        return context.apply(this.text);
    }
}
