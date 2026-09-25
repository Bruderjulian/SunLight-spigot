package su.nightexpress.sunlight.moduleImpl.socials;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.sunlight.api.provider.SocialsProvider;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.exception.ModuleLoadException;
import su.nightexpress.sunlight.hook.HookId;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.socials.command.SocialsCommandProvider;
import su.nightexpress.sunlight.moduleImpl.socials.config.SocialLink;
import su.nightexpress.sunlight.moduleImpl.socials.config.SocialsConfig;
import su.nightexpress.sunlight.moduleImpl.socials.config.SocialsLang;
import su.nightexpress.sunlight.moduleImpl.socials.config.SocialsPerms;
import su.nightexpress.sunlight.moduleImpl.socials.hook.DiscordHook;
import su.nightexpress.sunlight.moduleImpl.socials.listener.SocialsListener;
import su.nightexpress.sunlight.moduleImpl.socials.menu.SocialsMenu;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_NAME;

public class SocialsModule extends Module implements SocialsProvider {

    private final Map<String, SocialLink> links = new LinkedHashMap<>();

    private DiscordHook discordHook;
    private SocialsMenu menu;

    public SocialsModule(ModuleDefinition<SocialsModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
    }

    @Override
    protected void loadModule(FileConfig config) throws ModuleLoadException {
        config.initializeOptions(SocialsConfig.class);

        this.links.clear();
        this.links.putAll(SocialsConfig.readLinks(config));

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
            this.warn("DiscordSRV not found: Discord relay is disabled, links still work.");
        }

        this.menu = new SocialsMenu(this.plugin, this);
        this.menu.load(this.plugin, FileConfig.load(this.getLocalUIPath(), "socials.yml"));

        this.addListener(new SocialsListener(this.plugin, this));
    }

    @Override
    protected void unloadModule() {
        this.links.clear();
        this.discordHook = null;
        this.menu = null;
    }

    @Override
    protected void registerPermissions(PermissionTree root) {
        root.merge(SocialsPerms.MODULE);
    }

    @Override
    protected void registerCommands() {
        this.commandRegistry.addProvider("socials", new SocialsCommandProvider(this.plugin, this), this);
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        this.links.forEach((id, link) -> registry.register("socials_" + id + "_url", (player, payload) -> link.url()));
        SocialLink discord = this.links.get("discord");
        if (discord != null) {
            registry.register("socials_discord", (player, payload) -> discord.url());
        }
    }

    public Map<String, SocialLink> getLinkMap() {
        return Collections.unmodifiableMap(this.links);
    }

    public @Nullable SocialLink getLink(@NotNull String id) {
        return this.links.get(id.toLowerCase(java.util.Locale.ROOT));
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

    @Override
    public Map<String, String> getLinks() {
        Map<String, String> map = new LinkedHashMap<>();
        this.links.forEach((id, link) -> map.put(id, link.url()));
        return map;
    }

    public void openLinks(@NotNull Player player) {
        if (this.links.isEmpty()) {
            this.sendPrefixed(SocialsLang.LINKS_EMPTY, player);
            return;
        }
        if (this.menu != null) {
            this.menu.open(player);
        } else {
            this.sendLinksList(player);
        }
    }

    public void sendLinksList(@NotNull CommandSender sender) {
        if (this.links.isEmpty()) {
            this.sendPrefixed(SocialsLang.LINKS_EMPTY, sender);
            return;
        }
        this.sendPrefixed(SocialsLang.LINKS_HEADER, sender);
        this.links.forEach((id, link) -> {
            if (sender instanceof Player player && !SocialsPerms.hasLinkAccess(player, link.permission(), id)) return;
            this.sendLink(sender, link);
        });
    }

    public void sendLink(@NotNull CommandSender sender, @NotNull SocialLink link) {
        String chunk = "<click:open_url:'" + link.url() + "'><hover:show_text:'" + link.url() + "'>"
            + link.display() + "</hover></click>";
        this.sendPrefixed(SocialsLang.LINK_LINE, sender, replacer -> replacer.with(GENERIC_NAME, () -> chunk));
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
