package su.nightexpress.sunlight.moduleImpl.socials;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.provider.SocialsProvider;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.exception.ModuleLoadException;
import su.nightexpress.sunlight.hook.HookId;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.socials.config.SocialsConfig;
import su.nightexpress.sunlight.moduleImpl.socials.config.SocialsLang;
import su.nightexpress.sunlight.moduleImpl.socials.config.SocialsPerms;
import su.nightexpress.sunlight.moduleImpl.socials.hook.DiscordHook;
import su.nightexpress.sunlight.moduleImpl.socials.listener.SocialsListener;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;

public class SocialsModule extends Module implements SocialsProvider {

    private DiscordHook discordHook;

    public SocialsModule(ModuleDefinition<SocialsModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
    }

    @Override
    protected void loadModule(FileConfig config) throws ModuleLoadException {
        config.initializeOptions(SocialsConfig.class);

        this.plugin.injectLang(SocialsLang.class);

        if (HookId.hasDiscordSRV()) {
            try {
                this.discordHook = new DiscordHook(SocialsConfig.CHANNEL_ID.get());
            } catch (NoClassDefFoundError | Exception exception) {
                this.discordHook = null;
                this.warn("Failed to initialize DiscordSRV hook: " + exception.getMessage());
            }
        } else {
            this.discordHook = null;
            this.warn("DiscordSRV not found: Discord relay is disabled.");
        }

        this.addListener(new SocialsListener(this.plugin, this));
    }

    @Override
    protected void unloadModule() {
        this.discordHook = null;
    }

    @Override
    protected void registerPermissions(PermissionTree root) {
        root.merge(SocialsPerms.MODULE);
    }

    @Override
    protected void registerCommands() {
        // Every player-facing link command moved to the Links module, which owns that data now.
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        // Kept so scoreboards and menus using the old 'socials_<id>' placeholders keep resolving after
        // the link data moved to the Links module.
        this.plugin.moduleManager().getByType(LinksModule.class)
                .ifPresent(links -> links.getLinks()
                        .forEach((id, link) -> registry.register("socials_" + id, (player, payload) -> link.getUrl())));
    }

    public boolean isDiscordAvailable() {
        DiscordHook hook = this.discordHook;
        if (hook == null) return false;
        try {
            return hook.available();
        } catch (NoClassDefFoundError | Exception exception) {
            return false;
        }
    }

    public void sendDiscordMessage(@Nullable String channelId, @NotNull String text) {
        DiscordHook hook = this.discordHook;
        if (hook == null) return;
        try {
            hook.sendToChannel(channelId == null ? "" : channelId, text);
        } catch (NoClassDefFoundError | Exception exception) {
            this.discordHook = null;
            this.warn("DiscordSRV hook failed, relay disabled: " + exception.getMessage());
        }
    }

    @Override
    public void broadcastToDiscord(@NotNull String text) {
        this.sendDiscordMessage(SocialsConfig.CHANNEL_ID.get(), text);
    }

    public void relayJoin(@NotNull Player player) {
        if (!SocialsConfig.RELAY_JOIN.get() || !this.isDiscordAvailable()) return;
        this.relayAsync(SocialsConfig.FORMAT_JOIN.get().replace("%player%", player.getName()));
    }

    public void relayQuit(@NotNull Player player) {
        if (!SocialsConfig.RELAY_QUIT.get() || !this.isDiscordAvailable()) return;
        this.relayAsync(SocialsConfig.FORMAT_QUIT.get().replace("%player%", player.getName()));
    }

    public void relayDeath(@NotNull Player player) {
        if (!SocialsConfig.RELAY_DEATH.get() || !this.isDiscordAvailable()) return;
        this.relayAsync(SocialsConfig.FORMAT_DEATH.get().replace("%player%", player.getName()));
    }

    public void relayReport(@NotNull String reportId, @NotNull String reporterName, @NotNull String targetName,
            @NotNull String category, @NotNull String details, @NotNull String date) {
        if (!SocialsConfig.ANNOUNCE_REPORTS.get() || !this.isDiscordAvailable()) return;

        String text = SocialsConfig.FORMAT_REPORT.get()
                .replace("%report_id%", reportId)
                .replace("%reporter%", reporterName)
                .replace("%reporter_name%", reporterName)
                .replace("%target%", targetName)
                .replace("%target_name%", targetName)
                .replace("%category%", category)
                .replace("%details%", details)
                .replace("%date%", date);
        this.relayAsync(text);
    }

    public void relayAdvancement(@NotNull Player player, @NotNull String advancement) {
        if (!SocialsConfig.RELAY_ADVANCEMENT.get() || !this.isDiscordAvailable()) return;
        this.relayAsync(SocialsConfig.FORMAT_ADVANCEMENT.get()
                .replace("%player%", player.getName())
                .replace("%advancement%", advancement));
    }

    public void relayChat(@NotNull Player player, @NotNull String message) {
        if (!SocialsConfig.RELAY_CHAT.get() || !this.isDiscordAvailable()) return;
        this.sendDiscordMessage(SocialsConfig.CHANNEL_ID.get(), DiscordHook.strip(SocialsConfig.FORMAT_CHAT.get()
                .replace("%player%", player.getName())
                .replace("%message%", message)));
    }

    private void relayAsync(@NotNull String text) {
        String plain = DiscordHook.strip(text);
        String channelId = SocialsConfig.CHANNEL_ID.get();
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin,
            () -> this.sendDiscordMessage(channelId, plain));
    }
}
