package su.nightexpress.sunlight.moduleImpl.reports.model;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;

import java.util.Locale;

public record ReportCategory(String display,
                              String icon,
                              String permission,
                              String defaultDetail
) implements Writeable {

    public static ReportCategory read(FileConfig config, String path) {
        return new ReportCategory(
                config.getString(path + ".Display", "Reason"),
                config.getString(path + ".Icon", "PAPER"),
                config.getString(path + ".Permission", ""),
                config.getString(path + ".Default-Detail", "")
        );
    }

    @Override
    public void write(FileConfig config, String path) {
        config.set(path + ".Display", this.display);
        config.set(path + ".Icon", this.icon);
        config.set(path + ".Permission", this.permission);
        config.set(path + ".Default-Detail", this.defaultDetail);
    }

    public @NotNull String getDisplay() {
        return this.display == null || this.display.isBlank() ? "Reason" : this.display;
    }

    public @NotNull String getIcon() {
        return this.icon == null || this.icon.isBlank() ? "PAPER" : this.icon.toUpperCase(Locale.ROOT);
    }

    public boolean requiresPermission() {
        return this.permission != null && !this.permission.isBlank();
    }

    public boolean hasAccess(CommandSender sender) {
        return !this.requiresPermission() || sender.hasPermission(this.permission);
    }

    /** Falls back to a caller-supplied value so a report is never left without any text. */
    public @NotNull String getDefaultDetailOr(@NotNull String fallback) {
        return this.defaultDetail == null || this.defaultDetail.isBlank() ? fallback : this.defaultDetail;
    }
}
