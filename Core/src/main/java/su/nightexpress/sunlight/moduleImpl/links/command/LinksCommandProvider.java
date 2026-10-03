package su.nightexpress.sunlight.moduleImpl.links.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksLang;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksPerms;

public class LinksCommandProvider extends CommandProvider<LinksModule> {

    private static final String ARG_LINK = "link";
    private static final String COMMAND_ALL = "all";
    private static final String COMMAND_GET = "get";
    private static final String COMMAND_STATS = "stats";
    private static final String COMMAND_PREFIX = "link_";

    public LinksCommandProvider(final LinksModule module) {
        super(module, "links-common");
    }

    @Override
    public void setup() {
        this.register(COMMAND_ALL, List.of(), builder -> builder
                .withFullDescription(LinksLang.COMMAND_LINKS_ALL_DESC.text())
                .withPermission(LinksPerms.COMMAND_LINKS_ALL.getName())
                .executes(this::showAllLinks));

        // Reached by ID, so it also works for links created after startup, which have no command of
        // their own yet.
        this.register(COMMAND_GET, List.of(), builder -> builder
                .withFullDescription(LinksLang.COMMAND_LINKS_GET_DESC.text())
                .withPermission(LinksPerms.COMMAND_LINK.getName())
                .withArguments(this.linkArgument())
                .executes(this::getLink));

        this.register(COMMAND_STATS, List.of(), builder -> builder
                .withFullDescription(LinksLang.COMMAND_LINKS_STATS_DESC.text())
                .withPermission(LinksPerms.COMMAND_LINKS_STATS.getName())
                .executes(this::showStats));

        // One node per link that existed when the module was loaded. Bukkit cannot register a command
        // after startup, so a link created later gets its own command on the next reload.
        // Always registered: the link's enabled state is enforced at execution time by the access
        // checks, so toggling a link never desyncs from the command config. No framework permission
        // either: access is 'generic OR per-link OR custom', which a single permission node cannot
        // express, so openLink() enforces it manually and stays the single gate for every path.
        this.module.getLinks().keySet().forEach(id -> this.register(COMMAND_PREFIX + id, List.of(), builder -> builder
                .withFullDescription(LinksLang.COMMAND_LINKS_LINK_DESC.text())
                .executes((sender, arguments) -> {
                    return this.openLink(sender, id);
                })));

        this.registerRoot("links", builder -> builder
                .withFullDescription(LinksLang.COMMAND_LINKS_ROOT_DESC.text())
                .withPermission(LinksPerms.COMMAND_LINKS_ROOT.getName())
                .executes(this::showAllLinks));
    }

    private Argument<String> linkArgument() {
        return CommandArgumentConstants.string(ARG_LINK, info -> this.module
                .getVisibleLinks(info.sender())
                .stream()
                .map(Link::getId)
                .toList());
    }

    private int showAllLinks(final CommandSender sender, final CommandArguments arguments) {
        if (sender instanceof final Player player) {
            this.module.openLinks(player);
        } else {
            this.module.sendLinkList(sender);
        }

        return 1;
    }

    private int getLink(final CommandSender sender, final CommandArguments arguments) {
        final Object linkObj = arguments.get(ARG_LINK);
        if (!(linkObj instanceof final String id)) {
            this.module.sendPrefixed(LinksLang.COMMAND_SYNTAX_INVALID_LINK, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_INPUT, () -> ""));
            return 0;
        }

        return this.openLink(sender, id);
    }

    private int showStats(final CommandSender sender, final CommandArguments arguments) {
        this.module.sendStats(sender, 10);
        return 1;
    }

    private int openLink(final CommandSender sender, final String id) {
        // Resolved by ID rather than using the link captured at registration time, so the command
        // cannot act on a stale object after a delete or a reload.
        final Link link = this.module.getLink(id);
        if (link == null) {
            this.module.sendPrefixed(LinksLang.COMMAND_SYNTAX_INVALID_LINK, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_INPUT, () -> id));
            return 0;
        }

        // Duplicates the checks inside activateLink() on purpose: the console branch below never
        // reaches activateLink(), so removing these would let console bypass use permissions.
        if (!link.canSee(sender)) {
            this.module.sendPrefixed(LinksLang.ERROR_NO_PERMISSION, sender);
            return 0;
        }

        if (!link.canUse(sender)) {
            this.module.sendPrefixed(LinksLang.ERROR_NO_USE_PERMISSION, sender);
            return 0;
        }

        if (!(sender instanceof final Player player)) {
            // The console has no menu to click, so only the link itself is shown.
            this.module.sendLink(sender, link);
            return 1;
        }

        this.module.activateLink(player, link);
        return 1;
    }
}
