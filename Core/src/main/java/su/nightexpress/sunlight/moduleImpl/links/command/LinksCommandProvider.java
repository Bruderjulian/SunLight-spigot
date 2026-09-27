package su.nightexpress.sunlight.moduleImpl.links.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import su.nightexpress.nightcore.commands.Commands;
import su.nightexpress.nightcore.commands.builder.ArgumentNodeBuilder;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.nightcore.commands.exceptions.CommandSyntaxException;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksLang;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksPerms;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class LinksCommandProvider extends CommandProvider {

    private static final String ARG_LINK        = "link";
    private static final String COMMAND_ALL     = "all";
    private static final String COMMAND_GET     = "get";
    private static final String COMMAND_STATS   = "stats";
    private static final String COMMAND_PREFIX  = "link_";

    private final LinksModule module;

    public LinksCommandProvider(SunLightPlugin plugin, LinksModule module) {
        super(plugin);
        this.module = module;
    }

    @Override
    public void registerDefaults() {
        this.registerLiteral(COMMAND_ALL, true, new String[] { "alllinks", "linklist" }, builder -> builder
                .description(LinksLang.COMMAND_LINKS_ALL_DESC)
                .permission(LinksPerms.COMMAND_LINKS_ALL)
                .executes(this::showAllLinks));

        // Reached by ID, so it also works for links created after startup, which have no command of
        // their own yet.
        this.registerLiteral(COMMAND_GET, true, new String[] { "getlink" }, builder -> builder
                .description(LinksLang.COMMAND_LINKS_GET_DESC)
                .permission(LinksPerms.COMMAND_LINK)
                .withArguments(this.linkArgument())
                .executes(this::getLink));

        this.registerLiteral(COMMAND_STATS, true, new String[] { "linkstats" }, builder -> builder
                .description(LinksLang.COMMAND_LINKS_STATS_DESC)
                .permission(LinksPerms.COMMAND_LINKS_STATS)
                .executes(this::showStats));

        // One node per link that existed when the module was loaded. Bukkit cannot register a command
        // after startup, so a link created later gets its own command on the next reload.
        // Always registered: the link's enabled state is enforced at execution time by hasAccess(),
        // so toggling a link never desyncs from the command config. No framework permission either:
        // access is 'generic OR per-link OR custom', which a single permission node cannot express,
        // so openLink() enforces it manually and stays the single gate for every path.
        this.module.getLinks().forEach((id, link) -> this.registerLiteral(COMMAND_PREFIX + id, true,
                new String[] { id }, builder -> builder
                        .description(LinksLang.COMMAND_LINKS_LINK_DESC)
                        .executes((context, arguments) -> this.openLink(context, link.getId()))));

        // 'all' and 'get' are always children, so the hub is never childless and '/links' always resolves.
        Map<String, String> childrens = new LinkedHashMap<>();
        childrens.put(COMMAND_ALL, "all");
        childrens.put(COMMAND_GET, "get");
        childrens.put(COMMAND_STATS, "stats");
        this.module.getLinkIds().forEach(id -> childrens.put(COMMAND_PREFIX + id, id));

        this.registerRoot("links", true, new String[] { "links", "serverlinks" }, childrens,
                builder -> builder
                        .description(LinksLang.COMMAND_LINKS_ROOT_DESC)
                        .permission(LinksPerms.COMMAND_LINKS_ROOT)
                        .executes(this::showAllLinks));
    }

    private ArgumentNodeBuilder<Link> linkArgument() {
        return Commands.argument(ARG_LINK, (context, string) -> Optional.ofNullable(this.module.getLink(string))
                        .filter(link -> link.canSee(context.getSender()))
                        .orElseThrow(() -> CommandSyntaxException
                                .dynamic(LinksLang.COMMAND_SYNTAX_INVALID_LINK, string)))
                .localized(LinksLang.COMMAND_ARGUMENT_LINK)
                .suggestions((reader, context) -> this.module.getVisibleLinks(context.getSender())
                        .stream()
                        .map(Link::getId)
                        .toList());
    }

    private boolean showAllLinks(CommandContext context, ParsedArguments arguments) {
        CommandSender sender = context.getSender();
        if (context.isPlayer()) {
            this.module.openLinks(context.getPlayerOrThrow());
        } else {
            this.module.sendLinkList(sender);
        }

        return true;
    }

    private boolean getLink(CommandContext context, ParsedArguments arguments) {
        return this.openLink(context, arguments.get(ARG_LINK, Link.class).getId());
    }

    private boolean showStats(CommandContext context, ParsedArguments arguments) {
        this.module.sendStats(context.getSender(), 10);
        return true;
    }

    private boolean openLink(CommandContext context, String id) {
        // Resolved by ID rather than using the link captured at registration time, so the command
        // cannot act on a stale object after a delete or a reload.
        Link link = this.module.getLink(id);
        if (link == null) {
            context.printUsage();
            return false;
        }

        // Duplicates the checks inside activateLink() on purpose: the console branch below never
        // reaches activateLink(), so removing these would let console bypass use permissions.
        if (!link.canSee(context.getSender())) {
            this.module.sendPrefixed(LinksLang.ERROR_NO_PERMISSION, context.getSender());
            return false;
        }

        if (!link.canUse(context.getSender())) {
            this.module.sendPrefixed(LinksLang.ERROR_NO_USE_PERMISSION, context.getSender());
            return false;
        }

        Player player = context.getPlayer();
        if (player == null) {
            // The console has no menu to click, so only the link itself is shown.
            this.module.sendLink(context.getSender(), link);
            return true;
        }

        this.module.activateLink(player, link);
        return true;
    }
}
