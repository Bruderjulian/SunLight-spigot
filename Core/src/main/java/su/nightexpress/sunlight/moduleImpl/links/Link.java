package su.nightexpress.sunlight.moduleImpl.links;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.Enums;
import su.nightexpress.nightcore.util.StringUtil;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.placeholder.PlaceholderResolvable;
import su.nightexpress.nightcore.util.placeholder.PlaceholderResolver;
import su.nightexpress.nightcore.util.wrapper.UniParticle;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksPerms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class Link implements PlaceholderResolvable {

    private final String id;

    private String display;
    private String url;
    private String command;
    private LinkExecutor executor;
    private String permission;
    private String usePermission;
    private NightItem icon;
    private int priority;
    private boolean enabled;
    private long clicks;

    private int cooldown;
    private double cost;
    private String sound;
    private String actionbar;
    private UniParticle particle;
    private int particleCount;
    private List<String> firstRewardCommands;

    /**
     * UUID strings of players that activated this link. Drives the unique-clicks stat only; first-click
     * reward eligibility is tracked per-user instead, so stat resets never re-grant rewards.
     * <p>
     * Synchronized: the async click saver serializes this off the main thread while activations
     * mutate it on it. Every access synchronizes on the set itself.
     */
    private final Set<String> seenPlayers;

    private volatile boolean dirty;

    public Link(@NotNull String id, @NotNull String display, @NotNull String url) {
        this.id = id;
        this.display = display;
        this.url = url;
        this.command = "";
        this.executor = LinkExecutor.CONSOLE;
        this.permission = "";
        this.usePermission = "";
        this.icon = NightItem.fromType(Material.PAPER);
        this.priority = 0;
        this.enabled = true;
        this.clicks = 0L;
        this.cooldown = 0;
        this.cost = 0D;
        this.sound = "";
        this.actionbar = "";
        this.particle = UniParticle.of(null);
        this.particleCount = 20;
        this.firstRewardCommands = new ArrayList<>();
        this.seenPlayers = Collections.synchronizedSet(new LinkedHashSet<>());
    }

    public void load(@NotNull FileConfig config, @NotNull String path) {
        this.setDisplay(config.getString(path + ".Display", this.id));
        this.setUrl(config.getString(path + ".URL", ""));
        this.setCommand(config.getString(path + ".Command", ""));

        this.setExecutor(Enums.parse(config.getString(path + ".Command-Executor", ""), LinkExecutor.class)
                .orElse(LinkExecutor.CONSOLE));
        this.setPermission(config.getString(path + ".Permission", ""));
        this.setUsePermission(config.getString(path + ".Use-Permission", ""));
        this.setIcon(this.readIcon(config, path + ".Icon"));
        this.setPriority(config.getInt(path + ".Priority"));
        this.setEnabled(config.getBoolean(path + ".Enabled", true));
        this.setClicks(config.getLong(path + ".Clicks"));
        this.setCooldown(config.getInt(path + ".Cooldown"));
        this.setCost(config.getDouble(path + ".Cost"));
        this.setSound(config.getString(path + ".Sound", ""));
        this.setActionbar(config.getString(path + ".Actionbar", ""));
        this.setParticle(UniParticle.read(config, path + ".Particle"));
        if (this.particle.isEmpty() && config.contains(path + ".Particle-Type")) {
            // Legacy schema from before particles carried data: a bare particle name.
            this.setParticle(config.getString(path + ".Particle-Type", ""));
        }
        this.setParticleCount(config.getInt(path + ".Particle-Count", 20));
        this.setFirstRewardCommands(config.getStringList(path + ".First-Reward-Commands"));
        synchronized (this.seenPlayers) {
            this.seenPlayers.clear();
            this.seenPlayers.addAll(config.getStringList(path + ".Seen-Players"));
        }
    }

    public void write(@NotNull FileConfig config, @NotNull String path) {
        config.set(path + ".Enabled", this.enabled);
        config.set(path + ".Display", this.display);
        config.set(path + ".URL", this.url);
        config.set(path + ".Command", this.command);
        config.set(path + ".Command-Executor", this.executor.name());
        config.set(path + ".Permission", this.permission);
        config.set(path + ".Use-Permission", this.usePermission);
        config.set(path + ".Icon", this.icon);
        config.set(path + ".Priority", this.priority);
        config.set(path + ".Clicks", this.clicks);
        config.set(path + ".Cooldown", this.cooldown);
        config.set(path + ".Cost", this.cost);
        config.set(path + ".Sound", this.sound);
        config.set(path + ".Actionbar", this.actionbar);
        this.particle.write(config, path + ".Particle");
        if (config.contains(path + ".Particle-Type")) {
            config.remove(path + ".Particle-Type");
        }
        config.set(path + ".Particle-Count", this.particleCount);
        config.set(path + ".First-Reward-Commands", this.firstRewardCommands);
        synchronized (this.seenPlayers) {
            // Sorted: LinkedHashSet alone would still reshuffle the file whenever the first
            // entry differs, so every save sorts for a stable diff.
            config.set(path + ".Seen-Players", this.seenPlayers.stream().sorted().toList());
        }
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

    public boolean hasUsePermission() {
        return this.usePermission != null && !this.usePermission.isBlank();
    }

    public boolean hasSound() {
        return this.sound != null && !this.sound.isBlank() && !"none".equalsIgnoreCase(this.sound.trim());
    }

    public boolean hasActionbar() {
        return this.actionbar != null && !this.actionbar.isBlank();
    }

    public boolean hasParticle() {
        return !this.particle.isEmpty() && this.particle.getParticle() != null;
    }

    public boolean hasFirstReward() {
        return this.firstRewardCommands != null && this.firstRewardCommands.stream().anyMatch(s -> s != null && !s.isBlank());
    }

    /**
     * A link with neither a URL nor a command can do nothing when activated, so it is treated as
     * misconfigured instead of presenting the player with an empty click target.
     */
    public boolean isActionable() {
        return this.hasUrl() || this.hasCommand();
    }

    public boolean hasAccess(@NotNull CommandSender sender) {
        return this.canSee(sender);
    }

    /**
     * Whether the sender can see the link in menus and lists.
     */
    public boolean canSee(@NotNull CommandSender sender) {
        if (!this.enabled)
            return false;

        if (this.hasPermission() && !sender.hasPermission(this.permission))
            return false;

        return sender.hasPermission(LinksPerms.COMMAND_LINK) || sender.hasPermission(LinksPerms.COMMAND
                .childrenNode("link." + this.id));
    }

    /**
     * Whether the sender can activate the link. Implies {@link #canSee(CommandSender)}.
     */
    public boolean canUse(@NotNull CommandSender sender) {
        if (!this.canSee(sender))
            return false;

        return !this.hasUsePermission() || sender.hasPermission(this.usePermission);
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

    public @NotNull String getUsePermission() {
        return this.usePermission;
    }

    public void setUsePermission(@Nullable String usePermission) {
        if (usePermission != null && "none".equalsIgnoreCase(usePermission.trim())) {
            usePermission = "";
        }
        this.usePermission = usePermission == null ? "" : usePermission.trim();
    }

    public int getCooldown() {
        return this.cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = Math.max(0, cooldown);
    }

    public double getCost() {
        return this.cost;
    }

    public void setCost(double cost) {
        this.cost = Math.max(0D, cost);
    }

    public @NotNull String getSound() {
        return this.sound;
    }

    public void setSound(@Nullable String sound) {
        if (sound != null && "none".equalsIgnoreCase(sound.trim())) {
            sound = "";
        }
        this.sound = sound == null ? "" : sound.trim();
    }

    public @NotNull String getActionbar() {
        return this.actionbar;
    }

    public void setActionbar(@Nullable String actionbar) {
        this.actionbar = actionbar == null ? "" : actionbar;
    }

    public @NotNull UniParticle getParticle() {
        return this.particle;
    }

    public void setParticle(@Nullable UniParticle particle) {
        this.particle = particle == null ? UniParticle.of(null) : particle;
        this.particle.validateData();
    }

    public void setParticle(@Nullable String name) {
        if (name == null || name.isBlank() || "none".equalsIgnoreCase(name.trim())) {
            this.setParticle(UniParticle.of(null));
            return;
        }

        this.setParticle(UniParticle.of(StringUtil.getEnum(name.trim().toUpperCase(Locale.ROOT),
                org.bukkit.Particle.class).orElse(null)));
    }

    public @NotNull String getParticleName() {
        org.bukkit.Particle particle = this.particle.getParticle();
        return particle == null ? "" : particle.name();
    }

    public int getParticleCount() {
        return this.particleCount;
    }

    public void setParticleCount(int particleCount) {
        this.particleCount = Math.max(0, Math.min(100, particleCount));
    }

    public @NotNull List<String> getFirstRewardCommands() {
        return new ArrayList<>(this.firstRewardCommands);
    }

    public void setFirstRewardCommands(@Nullable List<String> commands) {
        this.firstRewardCommands = new ArrayList<>();
        if (commands == null) return;
        commands.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> {
                    String result = s.trim();
                    while (result.startsWith("/")) result = result.substring(1);
                    return result.trim();
                })
                .filter(s -> !s.isBlank())
                .forEach(this.firstRewardCommands::add);
    }

    public void setFirstRewardCommand(@Nullable String command) {
        this.setFirstRewardCommands(command == null || command.isBlank() ? List.of() : List.of(command));
    }

    public boolean hasSeen(@NotNull UUID uuid) {
        synchronized (this.seenPlayers) {
            return this.seenPlayers.contains(uuid.toString());
        }
    }

    public boolean markSeen(@NotNull UUID uuid) {
        synchronized (this.seenPlayers) {
            boolean added = this.seenPlayers.add(uuid.toString());
            if (added) this.markDirty();
            return added;
        }
    }

    public int getUniqueClicks() {
        synchronized (this.seenPlayers) {
            return this.seenPlayers.size();
        }
    }

    public void clearSeen() {
        synchronized (this.seenPlayers) {
            if (!this.seenPlayers.isEmpty()) {
                this.seenPlayers.clear();
                this.markDirty();
            }
        }
    }

    public @NotNull NightItem getIcon() {
        return this.icon.copy();
    }

    public void setIcon(@NotNull NightItem icon) {
        // An air icon would render as an empty menu slot, so fall back to paper like the config loader.
        if (icon.getMaterial().isAir()) {
            this.icon = NightItem.fromType(Material.PAPER);
            return;
        }
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
        this.clearSeen();
        this.markDirty();
    }
}
