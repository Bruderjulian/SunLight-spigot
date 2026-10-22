package su.nightexpress.sunlight.moduleImpl.texts;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import su.nightexpress.nightcore.commands.command.NightCommand;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.FileUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.sunlight.api.provider.TextsProvider;
import su.nightexpress.sunlight.api.provider.dto.CustomTextHandle;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.texts.command.TextCommandProvider;
import su.nightexpress.sunlight.moduleImpl.texts.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TextsModule extends Module implements TextsProvider {

    private final Map<String, Text> textByIdMap;
    private final Set<NightCommand> textCommands;

    public TextsModule(ModuleDefinition<TextsModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.textByIdMap = new HashMap<>();
        this.textCommands = new HashSet<>();
    }

    @Override
    protected void loadModule(FileConfig config) {
        this.plugin.injectLang(TextsLang.class);

        this.loadTexts();
        this.commandRegistry.addProvider(new TextCommandProvider(this));

        this.textByIdMap.values().forEach(text -> {
            NightCommand command = NightCommand.literal(this.plugin, text.getId(), builder -> builder
                    .description(text.getDescription())
                    .permission(text.getPermission())
                    .executes((context, arguments) -> {
                        this.showText(context.getSender(), text);
                        return true;
                    }));
            if (command.register()) {
                this.textCommands.add(command);
            }
        });
    }

    @Override
    protected void unloadModule() {
        this.textCommands.forEach(NightCommand::unregister);
        this.textCommands.clear();

        this.textByIdMap.clear();
    }

    @Override
    public String getPermissionNamespace() {
        return "customtext";
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {

    }

    private void loadTexts() {
        Path textDir = Path.of(this.getSystemPath() + TextsFiles.DIR_TEXTS);

        if (!Files.exists(textDir)) {
            TextsDefaults.defaultTexts().forEach((id, function) -> {
                Path textFile = Path.of(textDir.toString(), FileConfig.withExtension(id));
                FileConfig config = FileConfig.load(textFile);
                Text text = function.apply(textFile, id);
                config.edit(text::write);
            });
        }

        FileUtil.findYamlFiles(textDir.toString()).forEach(file -> {
            Text text = Text.fromFile(file);
            this.textByIdMap.put(text.getId(), text);
        });

        this.info("Loaded " + this.textByIdMap.size() + " custom texts.");
    }

    public Text getTextById(String id) {
        return this.textByIdMap.get(id.toLowerCase());
    }

    public Set<Text> getCustomTexts() {
        return Set.copyOf(this.textByIdMap.values());
    }

    public void showText(CommandSender sender, Text text) {
        List<String> texts = sender instanceof Player player ? text.getText(player) : text.getText();
        texts.forEach(line -> Players.sendMessage(sender, line));
    }

    @Override
    public List<String> getTextIds() {
        return List.copyOf(this.textByIdMap.keySet());
    }

    @Override
    public CustomTextHandle getText(@NotNull String id) {
        return handle(this.getTextById(id));
    }

    @Override
    public List<CustomTextHandle> getTexts() {
        return this.textByIdMap.values().stream().map(TextsModule::handle).toList();
    }

    @Override
    public boolean hasPermission(@NotNull CommandSender sender, @NotNull String id) {
        Text text = this.getTextById(id);
        return text != null && text.hasPermission(sender);
    }

    @Override
    public List<String> resolveText(@NotNull String id, @Nullable Player player) {
        Text text = this.getTextById(id);
        if (text == null)
            return List.of();

        return player == null ? List.copyOf(text.getText()) : List.copyOf(text.getText(player));
    }

    @Override
    public boolean showText(@NotNull CommandSender sender, @NotNull String id) {
        Text text = this.getTextById(id);
        if (text == null || !text.hasPermission(sender))
            return false;

        this.showText(sender, text);
        return true;
    }

    private static CustomTextHandle handle(@Nullable Text text) {
        return text == null ? null : new CustomTextHandle(text.getId(), text.getDescription(), text.getText());
    }
}
