package su.nightexpress.sunlight.moduleImpl.links.command;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.Commands;
import su.nightexpress.nightcore.commands.builder.ArgumentNodeBuilder;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.nightcore.commands.exceptions.CommandSyntaxException;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandArguments;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinkExecutor;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksLang;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksPerms;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class LinksAdminCommandProvider extends CommandProvider {

    private static final String ARG_LINK = "link";

    private static final String COMMAND_CREATE       = "create";
    private static final String COMMAND_DELETE       = "delete";
    private static final String COMMAND_EDIT         = "edit";
    private static final String COMMAND_SET_URL      = "seturl";
    private static final String COMMAND_SET_NAME     = "setname";
    private static final String COMMAND_SET_COMMAND  = "setcommand";
    private static final String COMMAND_SET_EXECUTOR = "setexecutor";
    private static final String COMMAND_SET_PERM     = "setpermission";
    private static final String COMMAND_SET_USE_PERM = "setusepermission";
    private static final String COMMAND_SET_ICON     = "seticon";
    private static final String COMMAND_SET_PRIORITY = "setpriority";
    private static final String COMMAND_SET_COOLDOWN = "setcooldown";
    private static final String COMMAND_SET_COST     = "setcost";
    private static final String COMMAND_SET_REWARD   = "setreward";
    private static final String COMMAND_SET_SOUND    = "setsound";
    private static final String COMMAND_SET_ACTIONBAR = "setactionbar";
    private static final String COMMAND_SET_PARTICLE = "setparticle";
    private static final String COMMAND_TOGGLE       = "toggle";
    private static final String COMMAND_RESET_CLICKS = "resetclicks";

    private static final String VALUE_NONE = "none";

    private final LinksModule module;

    public LinksAdminCommandProvider(SunLightPlugin plugin, LinksModule module) {
        super(plugin);
        this.module = module;
    }

    @Override
    public void registerDefaults() {
        this.registerLiteral(COMMAND_CREATE, true, new String[] { "createlink" }, builder -> builder
                .playerOnly()
                .description(LinksLang.COMMAND_ADMIN_CREATE_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_CREATE)
                .withArguments(Arguments.string(LinksLang.COMMAND_ARGUMENT_LINK.text())
                        .localized(LinksLang.COMMAND_ARGUMENT_LINK))
                .executes(this::createLink));

        this.registerLiteral(COMMAND_DELETE, true, new String[] { "deletelink" }, builder -> builder
                .description(LinksLang.COMMAND_ADMIN_DELETE_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_DELETE)
                .withArguments(this.linkArgument())
                .executes(this::deleteLink));

        this.registerLiteral(COMMAND_EDIT, true, new String[] { "editlinks" }, builder -> builder
                .playerOnly()
                .description(LinksLang.COMMAND_ADMIN_EDIT_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_EDIT)
                .executes(this::openEditor));

        this.registerLiteral(COMMAND_SET_URL, true, new String[] { "setlinkurl" }, builder -> builder
                .description(LinksLang.COMMAND_ADMIN_SET_URL_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_SET_URL)
                .withArguments(this.linkArgument(), Arguments.greedyString("url").optional())
                .executes(this::setUrl));

        this.registerLiteral(COMMAND_SET_NAME, true, new String[] { "setlinkname" }, builder -> builder
                .description(LinksLang.COMMAND_ADMIN_SET_NAME_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_SET_NAME)
                .withArguments(this.linkArgument(), Arguments.greedyString("name"))
                .executes(this::setName));

        this.registerLiteral(COMMAND_SET_COMMAND, true, new String[] { "setlinkcommand" }, builder -> builder
                .description(LinksLang.COMMAND_ADMIN_SET_COMMAND_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_SET_COMMAND)
                .withArguments(this.linkArgument(), Arguments.greedyString("command").optional())
                .executes(this::setCommand));

        this.registerLiteral(COMMAND_SET_EXECUTOR, true, new String[] { "setlinkexecutor" }, builder -> builder
                .description(LinksLang.COMMAND_ADMIN_SET_EXECUTOR_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_SET_EXECUTOR)
                .withArguments(this.linkArgument(), CommandArguments.enumed("executor", LinkExecutor.class))
                .executes(this::setExecutor));

        this.registerLiteral(COMMAND_SET_PERM, true, new String[] { "setlinkpermission" }, builder -> builder
                .description(LinksLang.COMMAND_ADMIN_SET_PERMISSION_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_SET_PERMISSION)
                .withArguments(this.linkArgument(), Arguments.string("permission").optional())
                .executes(this::setPermission));

        this.registerLiteral(COMMAND_SET_USE_PERM, true, new String[] { "setlinkusepermission" }, builder -> builder
                .description(LinksLang.COMMAND_ADMIN_SET_USE_PERMISSION_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_SET_USE_PERMISSION)
                .withArguments(this.linkArgument(), Arguments.string("permission").optional())
                .executes(this::setUsePermission));

        this.registerLiteral(COMMAND_SET_ICON, true, new String[] { "setlinkicon" }, builder -> builder
                .description(LinksLang.COMMAND_ADMIN_SET_ICON_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_SET_ICON)
                .withArguments(this.linkArgument(), Arguments.itemType("material"))
                .executes(this::setIcon));

        this.registerLiteral(COMMAND_SET_PRIORITY, true, new String[] { "setlinkpriority" }, builder -> builder
                .description(LinksLang.COMMAND_ADMIN_SET_PRIORITY_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_SET_PRIORITY)
                .withArguments(this.linkArgument(), Arguments.integer("priority"))
                .executes(this::setPriority));

        this.registerLiteral(COMMAND_SET_COOLDOWN, true, new String[] { "setlinkcooldown" }, builder -> builder
                .description(LinksLang.COMMAND_ADMIN_SET_COOLDOWN_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_SET_COOLDOWN)
                .withArguments(this.linkArgument(), Arguments.integer("seconds"))
                .executes(this::setCooldown));

        this.registerLiteral(COMMAND_SET_COST, true, new String[] { "setlinkcost" }, builder -> builder
                .description(LinksLang.COMMAND_ADMIN_SET_COST_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_SET_COST)
                .withArguments(this.linkArgument(), Arguments.decimal("cost", 0))
                .executes(this::setCost));

        this.registerLiteral(COMMAND_SET_REWARD, true, new String[] { "setlinkreward" }, builder -> builder
                .description(LinksLang.COMMAND_ADMIN_SET_REWARD_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_SET_REWARD)
                .withArguments(this.linkArgument(), Arguments.greedyString("command").optional())
                .executes(this::setReward));

        this.registerLiteral(COMMAND_SET_SOUND, true, new String[] { "setlinksound" }, builder -> builder
                .description(LinksLang.COMMAND_ADMIN_SET_SOUND_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_SET_SOUND)
                .withArguments(this.linkArgument(), Arguments.greedyString("sound").optional())
                .executes(this::setSound));

        this.registerLiteral(COMMAND_SET_ACTIONBAR, true, new String[] { "setlinkactionbar" }, builder -> builder
                .description(LinksLang.COMMAND_ADMIN_SET_ACTIONBAR_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_SET_ACTIONBAR)
                .withArguments(this.linkArgument(), Arguments.greedyString("text").optional())
                .executes(this::setActionbar));

        this.registerLiteral(COMMAND_SET_PARTICLE, true, new String[] { "setlinkparticle" }, builder -> builder
                .description(LinksLang.COMMAND_ADMIN_SET_PARTICLE_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_SET_PARTICLE)
                .withArguments(this.linkArgument(), Arguments.greedyString("particle").optional())
                .executes(this::setParticle));

        this.registerLiteral(COMMAND_TOGGLE, true, new String[] { "togglelink" }, builder -> builder
                .description(LinksLang.COMMAND_ADMIN_TOGGLE_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_TOGGLE)
                .withArguments(this.linkArgument())
                .executes(this::toggleLink));

        this.registerLiteral(COMMAND_RESET_CLICKS, true, new String[] { "resetlinkclicks" }, builder -> builder
                .description(LinksLang.COMMAND_ADMIN_RESET_CLICKS_DESC)
                .permission(LinksPerms.COMMAND_ADMIN_RESET_CLICKS)
                .withArguments(this.linkArgument())
                .executes(this::resetClicks));

        this.registerRoot("linksadmin", true, new String[] { "links-admin" }, this.adminChildrens(),
                builder -> builder.description(LinksLang.COMMAND_ADMIN_ROOT_DESC)
                        .permission(LinksPerms.COMMAND_ADMIN_ROOT));
    }

    private static Map<String, String> adminChildrens() {
        // Map.of() tops out at ten pairs, and there are more admin nodes than that.
        Map<String, String> map = new LinkedHashMap<>();
        map.put(COMMAND_CREATE, "create");
        map.put(COMMAND_DELETE, "delete");
        map.put(COMMAND_EDIT, "edit");
        map.put(COMMAND_SET_URL, "seturl");
        map.put(COMMAND_SET_NAME, "setname");
        map.put(COMMAND_SET_COMMAND, "setcommand");
        map.put(COMMAND_SET_EXECUTOR, "setexecutor");
        map.put(COMMAND_SET_PERM, "setpermission");
        map.put(COMMAND_SET_USE_PERM, "setusepermission");
        map.put(COMMAND_SET_ICON, "seticon");
        map.put(COMMAND_SET_PRIORITY, "setpriority");
        map.put(COMMAND_SET_COOLDOWN, "setcooldown");
        map.put(COMMAND_SET_COST, "setcost");
        map.put(COMMAND_SET_REWARD, "setreward");
        map.put(COMMAND_SET_SOUND, "setsound");
        map.put(COMMAND_SET_ACTIONBAR, "setactionbar");
        map.put(COMMAND_SET_PARTICLE, "setparticle");
        map.put(COMMAND_TOGGLE, "toggle");
        map.put(COMMAND_RESET_CLICKS, "resetclicks");
        return map;
    }

    private ArgumentNodeBuilder<Link> linkArgument() {
        return Commands.argument(ARG_LINK, (context, string) -> Optional.ofNullable(this.module.getLink(string))
                        .orElseThrow(() -> CommandSyntaxException
                                .dynamic(LinksLang.COMMAND_SYNTAX_INVALID_LINK, string)))
                .localized(LinksLang.COMMAND_ARGUMENT_LINK)
                .suggestions((reader, context) -> this.module.getLinkIds().stream().toList());
    }

    // Handlers

    private boolean createLink(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        return this.module.createLinkAndReport(player, arguments.getString(ARG_LINK));
    }

    private boolean deleteLink(CommandContext context, ParsedArguments arguments) {
        Link link = arguments.get(ARG_LINK, Link.class);
        CommandSender sender = context.getSender();

        if (!this.module.deleteLink(link)) {
            return false;
        }

        this.module.sendPrefixed(LinksLang.ADMIN_DELETE_FEEDBACK, sender,
                replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, link::getId));
        // The deleted link's Bukkit command survives until reload and now only prints usage.
        this.module.sendPrefixed(LinksLang.ADMIN_RELOAD_REQUIRED, sender);
        return true;
    }

    private boolean openEditor(CommandContext context, ParsedArguments arguments) {
        this.module.openEditor(context.getPlayerOrThrow());
        return true;
    }

    private boolean setUrl(CommandContext context, ParsedArguments arguments) {
        Link link = arguments.get(ARG_LINK, Link.class);
        String url = arguments.getString("url", "");

        if (url.isBlank() && !link.hasCommand()) {
            this.module.sendPrefixed(LinksLang.ERROR_NOT_ACTIONABLE, context.getSender());
            return false;
        }

        link.setUrl(url);
        this.module.saveLink(link);

        this.feedback(context, LinksLang.ADMIN_SET_URL_FEEDBACK, link, url);
        return true;
    }

    private boolean setName(CommandContext context, ParsedArguments arguments) {
        Link link = arguments.get(ARG_LINK, Link.class);
        String name = arguments.getString("name");

        link.setDisplay(name);
        this.module.saveLink(link);

        this.feedback(context, LinksLang.ADMIN_SET_NAME_FEEDBACK, link, name);
        return true;
    }

    private boolean setCommand(CommandContext context, ParsedArguments arguments) {
        Link link = arguments.get(ARG_LINK, Link.class);
        String command = arguments.getString("command", "");

        if (command.isBlank() && !link.hasUrl()) {
            this.module.sendPrefixed(LinksLang.ERROR_NOT_ACTIONABLE, context.getSender());
            return false;
        }

        link.setCommand(command);
        this.module.saveLink(link);

        this.feedback(context, LinksLang.ADMIN_SET_COMMAND_FEEDBACK, link, command.isEmpty() ? VALUE_NONE : command);
        return true;
    }

    private boolean setExecutor(CommandContext context, ParsedArguments arguments) {
        Link link = arguments.get(ARG_LINK, Link.class);
        LinkExecutor executor = arguments.get("executor", LinkExecutor.class);

        link.setExecutor(executor);
        this.module.saveLink(link);

        this.feedback(context, LinksLang.ADMIN_SET_EXECUTOR_FEEDBACK, link, executor.name());
        return true;
    }

    private boolean setPermission(CommandContext context, ParsedArguments arguments) {
        Link link = arguments.get(ARG_LINK, Link.class);
        String permission = arguments.getString("permission", "");
        if (VALUE_NONE.equalsIgnoreCase(permission)) {
            permission = "";
        }

        link.setPermission(permission);
        this.module.saveLink(link);

        this.feedback(context, LinksLang.ADMIN_SET_PERMISSION_FEEDBACK, link,
                permission.isEmpty() ? VALUE_NONE : permission);
        return true;
    }

    private boolean setUsePermission(CommandContext context, ParsedArguments arguments) {
        Link link = arguments.get(ARG_LINK, Link.class);
        String permission = arguments.getString("permission", "");
        if (VALUE_NONE.equalsIgnoreCase(permission)) {
            permission = "";
        }

        link.setUsePermission(permission);
        this.module.saveLink(link);

        this.feedback(context, LinksLang.ADMIN_SET_USE_PERMISSION_FEEDBACK, link,
                permission.isEmpty() ? VALUE_NONE : permission);
        return true;
    }

    private boolean setIcon(CommandContext context, ParsedArguments arguments) {
        Link link = arguments.get(ARG_LINK, Link.class);
        Material material = arguments.get("material", Material.class);

        // Air would render as an empty menu slot; fall back to paper like the config loader does.
        Material effective = (material == null || material.isAir()) ? Material.PAPER : material;
        link.setIcon(NightItem.fromType(effective));
        this.module.saveLink(link);

        this.feedback(context, LinksLang.ADMIN_SET_ICON_FEEDBACK, link, effective.name());
        return true;
    }

    private boolean setPriority(CommandContext context, ParsedArguments arguments) {
        Link link = arguments.get(ARG_LINK, Link.class);
        int priority = arguments.getInt("priority");

        link.setPriority(priority);
        this.module.saveLink(link);

        this.feedback(context, LinksLang.ADMIN_SET_PRIORITY_FEEDBACK, link, String.valueOf(priority));
        return true;
    }

    private boolean toggleLink(CommandContext context, ParsedArguments arguments) {
        Link link = arguments.get(ARG_LINK, Link.class);

        link.setEnabled(!link.isEnabled());
        this.module.saveLink(link);

        this.feedback(context, LinksLang.ADMIN_TOGGLE_FEEDBACK, link, String.valueOf(link.isEnabled()));
        return true;
    }

    private boolean setCooldown(CommandContext context, ParsedArguments arguments) {
        Link link = arguments.get(ARG_LINK, Link.class);
        int seconds = arguments.getInt("seconds");

        if (seconds < 0) {
            this.module.sendPrefixed(LinksLang.ADMIN_SET_INVALID_NUMBER, context.getSender(),
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> String.valueOf(seconds)));
            return false;
        }

        link.setCooldown(seconds);
        this.module.saveLink(link);

        this.feedback(context, LinksLang.ADMIN_SET_COOLDOWN_FEEDBACK, link, String.valueOf(seconds));
        return true;
    }

    private boolean setCost(CommandContext context, ParsedArguments arguments) {
        Link link = arguments.get(ARG_LINK, Link.class);
        double cost = arguments.getDouble("cost");

        if (cost < 0D) {
            this.module.sendPrefixed(LinksLang.ADMIN_SET_INVALID_NUMBER, context.getSender(),
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> String.valueOf(cost)));
            return false;
        }

        link.setCost(cost);
        this.module.saveLink(link);

        this.feedback(context, LinksLang.ADMIN_SET_COST_FEEDBACK, link, String.valueOf(cost));
        return true;
    }

    private boolean setReward(CommandContext context, ParsedArguments arguments) {
        Link link = arguments.get(ARG_LINK, Link.class);
        String command = arguments.getString("command", "");

        link.setFirstRewardCommand(command);
        this.module.saveLink(link);

        this.feedback(context, LinksLang.ADMIN_SET_REWARD_FEEDBACK, link, command.isEmpty() ? VALUE_NONE : command);
        return true;
    }

    private boolean setSound(CommandContext context, ParsedArguments arguments) {
        Link link = arguments.get(ARG_LINK, Link.class);
        String raw = arguments.getString("sound", "");
        final String sound = VALUE_NONE.equalsIgnoreCase(raw) ? "" : raw;

        if (!sound.isBlank() && !LinksModule.isValidSound(sound)) {
            this.module.sendPrefixed(LinksLang.ADMIN_SET_INVALID_SOUND, context.getSender(),
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> sound));
            return false;
        }

        link.setSound(sound);
        this.module.saveLink(link);

        this.feedback(context, LinksLang.ADMIN_SET_SOUND_FEEDBACK, link, sound.isEmpty() ? VALUE_NONE : sound);
        return true;
    }

    private boolean setActionbar(CommandContext context, ParsedArguments arguments) {
        Link link = arguments.get(ARG_LINK, Link.class);
        String text = arguments.getString("text", "");

        link.setActionbar(text);
        this.module.saveLink(link);

        this.feedback(context, LinksLang.ADMIN_SET_ACTIONBAR_FEEDBACK, link, text.isEmpty() ? VALUE_NONE : text);
        return true;
    }

    private boolean setParticle(CommandContext context, ParsedArguments arguments) {
        Link link = arguments.get(ARG_LINK, Link.class);
        String raw = arguments.getString("particle", "");
        final String particle = VALUE_NONE.equalsIgnoreCase(raw) ? "" : raw;

        if (!LinksModule.isValidParticle(particle)) {
            this.module.sendPrefixed(LinksLang.ADMIN_SET_INVALID_PARTICLE, context.getSender(),
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> particle));
            return false;
        }

        link.setParticle(particle);
        this.module.saveLink(link);

        this.feedback(context, LinksLang.ADMIN_SET_PARTICLE_FEEDBACK, link, particle.isEmpty() ? VALUE_NONE : particle);
        return true;
    }

    private boolean resetClicks(CommandContext context, ParsedArguments arguments) {
        Link link = arguments.get(ARG_LINK, Link.class);

        link.resetClicks();
        this.module.saveLink(link);

        this.module.sendPrefixed(LinksLang.ADMIN_RESET_CLICKS_FEEDBACK, context.getSender(),
                replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, link::getId));
        return true;
    }

    private void feedback(CommandContext context, MessageLocale message, Link link, String newValue) {
        this.module.sendPrefixed(message, context.getSender(), replacer -> replacer
                .with(SLPlaceholders.GENERIC_VALUE, link::getId)
                .with(SLPlaceholders.GENERIC_NEW_VALUE, () -> newValue));
    }
}
