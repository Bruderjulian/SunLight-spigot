package su.nightexpress.sunlight.command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.bukkit.command.CommandSender;
import org.jspecify.annotations.Nullable;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.utils.Utils;

import static su.nightexpress.nightcore.util.Placeholders.GENERIC_COMMAND;
import static su.nightexpress.nightcore.util.Placeholders.GENERIC_DESCRIPTION;
import static su.nightexpress.nightcore.util.Placeholders.GENERIC_ENTRY;
import static su.nightexpress.nightcore.util.Placeholders.GENERIC_NAME;

/**
 * The plugin's own command, {@code /sunlight}, and its administrative
 * sub-commands.
 * <p>
 * Unlike a {@link CommandProvider} this one is not backed by a module, so it
 * owns its
 * root command rather than attaching itself to somebody else's.
 */
public class SunlightCommandProvider extends AbstractCommandProvider {

    private static final String EXTENSION = ".zip";

    /**
     * The root command as it was built by the registry. Captured while the node is
     * built so that {@code help} can list whatever ended up attached to it.
     */
    private CommandAPICommand root;

    public SunlightCommandProvider(final SunLightPlugin plugin) {
        super(plugin, "sunlight");
    }

    @Override
    public void setup() {
        this.registerRoot("sunlight", command -> {
            this.root = command;
            command
                    .withAliases("sl")
                    .withFullDescription(Lang.COMMAND_ROOT_DESC.text())
                    .withPermission("sunlight.admin")
                    .executes((sender, arguments) -> {
                        this.sendHelp(sender);
                        return 1;
                    });
        });

        this.register("help", command -> command
                .withFullDescription(CoreLang.COMMAND_HELP_DESC.text())
                .executes((sender, arguments) -> {
                    this.sendHelp(sender);
                    return 1;
                }))
                .under("sunlight");

        this.register("reload", command -> command
                .withFullDescription(CoreLang.COMMAND_RELOAD_DESC.text())
                .executes((sender, arguments) -> {
                    this.plugin.doReload(sender);
                    return 1;
                }))
                .under("sunlight");

        this.register("reloadmodule", command -> command
                .withFullDescription(Lang.COMMAND_RELOAD_MODULE_DESC.text())
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.NAME, info -> this.moduleIds()))
                .executes((sender, arguments) -> {
                    final String moduleId = argument(arguments);
                    if (moduleId == null) {
                        sender.sendMessage("Usage: /sunlight reloadmodule <id>");
                        return 0;
                    }

                    final boolean reloaded = this.plugin.moduleManager().reloadSingle(Utils.lowercase(moduleId));
                    sender.sendMessage("Reload module '%s': %s".formatted(moduleId, reloaded ? "OK" : "NOT FOUND"));
                    return reloaded ? 1 : 0;
                }))
                .under("sunlight");

        this.register("dump", command -> command
                .withFullDescription(Lang.COMMAND_DUMP_DESC.text())
                .executes((sender, arguments) -> {
                    final StringBuilder builder = new StringBuilder();
                    builder.append("SunLight ").append(this.plugin.getDescription().getVersion());
                    builder.append(" | Server ").append(this.plugin.getServer().getVersion());
                    builder.append(" | Java ").append(System.getProperty("java.version"));
                    builder.append(" | Online ").append(this.plugin.getServer().getOnlinePlayers().size());
                    builder.append(" | NMS ").append(this.plugin.internals().map(nms -> nms.getClass().getSimpleName())
                            .orElse("none"));
                    builder.append(" | Modules ");
                    this.moduleIds().forEach(moduleId -> builder.append(moduleId).append(','));
                    sender.sendMessage(builder.toString());
                    this.plugin.info("[SunLight dump] " + builder);
                    return 1;
                }))
                .under("sunlight");

        this.register("export", command -> command
                .withFullDescription(Lang.COMMAND_EXPORT_DESC.text())
                .withOptionalArguments(
                        CommandArgumentConstants.string(CommandArgumentConstants.NAME, info -> List.of()))
                .executes((sender, arguments) -> {
                    final String name = argument(arguments);
                    this.plugin.runTaskAsync(() -> this.exportData(sender, name));
                    return 1;
                }))
                .under("sunlight");

        this.register("import", command -> command
                .withFullDescription(Lang.COMMAND_IMPORT_DESC.text())
                .withOptionalArguments(CommandArgumentConstants.string(CommandArgumentConstants.NAME,
                        info -> this.exportNames()))
                .executes((sender, arguments) -> {
                    String name = argument(arguments);
                    if (name == null) {
                        name = this.latestExportName();
                    }
                    if (name == null) {
                        sender.sendMessage("No exports found. Usage: /sunlight import <name>.");
                        return 0;
                    }
                    final String fileName = name;
                    this.plugin.runTaskAsync(() -> this.importData(sender, fileName));
                    return 1;
                }))
                .under("sunlight");
    }

    /**
     * Prints the root command help. Handled explicitly (instead of relying on a hub
     * fallback) so that both '/sunlight' and '/sunlight help' always list the
     * available sub-commands.
     */
    private void sendHelp(final CommandSender sender) {
        final CommandAPICommand root = this.root;
        if (root == null) {
            return;
        }

        Lang.PLUGIN_HELP.message().send(sender, replacer -> replacer
                .replace(GENERIC_NAME, this.plugin.getNameLocalized())
                .replace(GENERIC_ENTRY, list -> {
                    root.getSubcommands().stream()
                            .sorted(Comparator.comparing(CommandAPICommand::getName))
                            .forEach(sub -> {
                                if (!canSee(sender, sub)) {
                                    return;
                                }
                                list.add(Lang.PLUGIN_HELP_ENTRY.text()
                                        .replace(GENERIC_COMMAND, root.getName() + ' ' + usage(sub))
                                        .replace(GENERIC_DESCRIPTION, description(sub)));
                            });
                }));
    }

    private static boolean canSee(final CommandSender sender, final CommandAPICommand command) {
        final CommandPermission permission = command.getPermission();
        if (permission == null) {
            return true;
        }
        return permission.getPermission().map(sender::hasPermission).orElseGet(sender::isOp);
    }

    private static String description(final CommandAPICommand command) {
        final String description = command.getShortDescription() != null
                ? command.getShortDescription()
                : command.getFullDescription();
        return description == null ? "" : description;
    }

    private static String usage(final CommandAPICommand command) {
        final StringBuilder builder = new StringBuilder(command.getName());
        for (final Argument<?> argument : command.getArguments()) {
            builder.append(argument.isOptional() ? " [<" : " <").append(argument.getNodeName()).append('>');
        }
        return builder.toString();
    }

    private List<String> moduleIds() {
        return this.plugin.moduleManager().getModules().stream()
                .map(Module::getId)
                .sorted()
                .toList();
    }

    private void exportData(final CommandSender sender, @Nullable final String name) {
        // The name becomes a file inside 'exports', so it must not be able to point
        // anywhere else.
        final String safeName = name == null || isUnsafeFileName(name) ? "backup-" + System.currentTimeMillis() : name;
        try {
            final Path dataDir = this.plugin.getDataFolder().toPath();
            final Path outDir = dataDir.resolve("exports");
            Files.createDirectories(outDir);

            final String fileName = safeName + EXTENSION;
            try (final ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(outDir.resolve(fileName)))) {
                try (final Stream<Path> stream = Files.walk(dataDir)) {
                    for (final Path path : (Iterable<Path>) stream::iterator) {
                        if (Files.isDirectory(path) || path.startsWith(outDir)) {
                            continue;
                        }
                        zip.putNextEntry(new ZipEntry(dataDir.relativize(path).toString().replace('\\', '/')));
                        Files.copy(path, zip);
                        zip.closeEntry();
                    }
                }
            }
            sender.sendMessage("Exported to exports/" + fileName);
        } catch (final Exception exception) {
            exception.printStackTrace();
            sender.sendMessage("Export failed: " + exception.getMessage());
        }
    }

    private void importData(final CommandSender sender, final String name) {
        if (isUnsafeFileName(name)) {
            sender.sendMessage("Import file not found: exports/" + name);
            return;
        }

        try {
            final Path dataDir = this.plugin.getDataFolder().toPath();
            final Path file = dataDir.resolve("exports").resolve(withExtension(name));
            if (!Files.exists(file)) {
                sender.sendMessage("Import file not found: exports/" + name);
                return;
            }

            try (final ZipInputStream zip = new ZipInputStream(Files.newInputStream(file))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    final Path out = dataDir.resolve(entry.getName()).normalize();
                    if (!out.startsWith(dataDir)) {
                        continue;
                    }
                    Files.createDirectories(out.getParent());
                    Files.copy(zip, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            this.plugin.runTask(task -> this.plugin.doReload(sender));
            sender.sendMessage("Import complete, reloaded.");
        } catch (final Exception exception) {
            exception.printStackTrace();
            sender.sendMessage("Import failed: " + exception.getMessage());
        }
    }

    /**
     * @return The name of the most recently written export, without its extension.
     */
    @Nullable
    private String latestExportName() {
        try {
            final Path outDir = this.plugin.getDataFolder().toPath().resolve("exports");
            if (!Files.isDirectory(outDir)) {
                return null;
            }
            try (final Stream<Path> stream = Files.list(outDir)) {
                return stream.filter(path -> path.getFileName().toString().endsWith(EXTENSION))
                        .max(Comparator.comparingLong(SunlightCommandProvider::lastModified))
                        .map(path -> stripExtension(path.getFileName().toString()))
                        .orElse(null);
            }
        } catch (final Exception exception) {
            return null;
        }
    }

    private List<String> exportNames() {
        try {
            final Path outDir = this.plugin.getDataFolder().toPath().resolve("exports");
            if (!Files.isDirectory(outDir)) {
                return List.of();
            }
            try (final Stream<Path> stream = Files.list(outDir)) {
                return stream.map(path -> path.getFileName().toString())
                        .filter(fileName -> fileName.endsWith(EXTENSION))
                        .map(SunlightCommandProvider::stripExtension)
                        .sorted()
                        .toList();
            }
        } catch (final Exception exception) {
            return List.of();
        }
    }

    private static long lastModified(final Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (final Exception exception) {
            return 0L;
        }
    }

    private static String withExtension(final String fileName) {
        return fileName.endsWith(EXTENSION) ? fileName : fileName + EXTENSION;
    }

    private static boolean isUnsafeFileName(final String fileName) {
        return fileName.isBlank() || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..");
    }

    private static String stripExtension(final String fileName) {
        return fileName.substring(0, fileName.length() - EXTENSION.length());
    }

    @Nullable
    private static String argument(final CommandArguments arguments) {
        final String value = arguments.<String>getOptionalUnchecked(CommandArgumentConstants.NAME).orElse(null);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}