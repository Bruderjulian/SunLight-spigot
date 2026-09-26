package su.nightexpress.sunlight.moduleImpl.links;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.Enums;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.placeholder.PlaceholderResolvable;
import su.nightexpress.nightcore.util.placeholder.PlaceholderResolver;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksPerms;

public class Link implements PlaceholderResolvable {

    private final String id;

    private String display;
    private String url;
    private String command;
    private LinkExecutor executor;
    private String permission;
    private NightItem icon;
    private int priority;
    private boolean enabled;
    private long clicks;

    private boolean dirty;

    public Link(@NotNull String id, @NotNull String display, @NotNull String url) {
        this.id = id;
        this.display = display;
        this.url = url;
        this.command = "";
        this.executor = LinkExecutor.CONSOLE;
        this.permission = "";
        this.icon = NightItem.fromType(Material.PAPER);
        this.priority = 0;
        this.enabled = true;
        this.clicks = 0L;
    }

    public void load(@NotNull FileConfig config, @NotNull String path) {
        this.setDisplay(config.getString(path + ".Display", this.id));
        this.setUrl(config.getString(path + ".URL", ""));
        this.setCommand(config.getString(path + ".Command", ""));

        this.setExecutor(Enums.parse(config.getString(path + ".Command-Executor", ""), LinkExecutor.class)
                .orElse(LinkExecutor.CONSOLE));
        this.setPermission(config.getString(path + ".Permission", ""));
        this.setIcon(this.readIcon(config, path + ".Icon"));
        this.setPriority(config.getInt(path + ".Priority"));
        this.setEnabled(config.getBoolean(path + ".Enabled", true));
        this.setClicks(config.getLong(path + ".Clicks"));
    }

    public void write(@NotNull FileConfig config, @NotNull String path) {
        config.set(path + ".Enabled", this.enabled);
        config.set(path + ".Display", this.display);
        config.set(path + ".URL", this.url);
        config.set(path + ".Command", this.command);
        config.set(path + ".Command-Executor", this.executor.name());
        config.set(path + ".Permission", this.permission);
        config.set(path + ".Icon", this.icon);
        config.set(path + ".Priority", this.priority);
        config.set(path + ".Clicks", this.clicks);
    }

    /**
     * The icon is the most likely value to be hand-edited wrongly, and a broken icon must not take the
     * whole link down with it, so anything unusable falls back to paper.
     */
    private static NightItem readIcon(@NotNull FileConfig config, @NotNull String path) {
        if (!config.contains(path))
            return NightItem.fromType(Material.PAPER);

        NightItem icon = config.getCosmeticItem(path);
        return icon == null || icon.getMaterial().isAir() ? NightItem.fromType(Material.PAPER) : icon;
    }

    @Override
    public @NotNull PlaceholderResolver placeholders() {
        return LinksPlaceholders.LINK.resolver(this);
    }

    public boolean hasUrl() {
        return this.url != null && !this.url.isBlank();
    }

    public boolean hasCommand() {
        return this.command != null && !this.command.isBlank();
    }

    public boolean hasPermission() {
        return this.permission != null && !this.permission.isBlank();
    }

    /**
     * A link with neither a URL nor a command can do nothing when activated, so it is treated as
     * misconfigured instead of presenting the player with an empty click target.
     */
    public boolean isActionable() {
        return this.hasUrl() || this.hasCommand();
    }

    public boolean hasAccess(@NotNull CommandSender sender) {
        if (!this.enabled)
            return false;

        if (this.hasPermission() && !sender.hasPermission(this.permission))
            return false;

        return sender.hasPermission(LinksPerms.COMMAND_LINK) || sender.hasPermission(LinksPerms.COMMAND
                .childrenNode("link." + this.id));
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void markClean() {
        this.dirty = false;
    }

    public @NotNull String getId() {
        return this.id;
    }

    public @NotNull String getDisplay() {
        return this.display;
    }

    public void setDisplay(@Nullable String display) {
        this.display = display == null || display.isBlank() ? this.id : display;
    }

    public @NotNull String getUrl() {
        return this.url;
    }

    public void setUrl(@Nullable String url) {
        this.url = url == null ? "" : url.trim();
    }

    public @NotNull String getCommand() {
        return this.command;
    }

    public void setCommand(@Nullable String command) {
        String result = command == null ? "" : command.trim();
        while (result.startsWith("/")) {
            result = result.substring(1);
        }

        this.command = result.trim();
    }

    public @NotNull LinkExecutor getExecutor() {
        return this.executor;
    }

    public void setExecutor(@NotNull LinkExecutor executor) {
        this.executor = executor;
    }

    public @NotNull String getPermission() {
        return this.permission;
    }

    public void setPermission(@Nullable String permission) {
        this.permission = permission == null ? "" : permission.trim();
    }

    public @NotNull NightItem getIcon() {
        return this.icon.copy();
    }

    public void setIcon(@NotNull NightItem icon) {
        this.icon = icon.copy();
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getClicks() {
        return this.clicks;
    }

    public void setClicks(long clicks) {
        this.clicks = Math.max(0L, clicks);
    }

    public void addClick() {
        this.clicks++;
        this.markDirty();
    }

    public void resetClicks() {
        this.clicks = 0L;
        this.markDirty();
    }
}
