package su.nightexpress.sunlight.moduleImpl.links.command;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinkExecutor;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksLang;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksPerms;
import su.nightexpress.sunlight.utils.Utils;

public class LinksAdminCommandProvider extends CommandProvider<LinksModule> {

    private static final String ARG_LINK = "link";
    private static final String ARG_URL = "url";
    private static final String ARG_COMMAND = "command";
    private static final String ARG_NAME = "name";
    private static final String ARG_EXECUTOR = "executor";
    private static final String ARG_PERMISSION = "permission";
    private static final String ARG_MATERIAL = "material";
    private static final String ARG_PRIORITY = "priority";
    private static final String ARG_SECONDS = "seconds";
    private static final String ARG_COST = "cost";
    private static final String ARG_SOUND = "sound";
    private static final String ARG_TEXT = "text";
    private static final String ARG_PARTICLE = "particle";
    private static final String ARG_COUNT = "count";

    private static final String COMMAND_CREATE = "create";
    private static final String COMMAND_DELETE = "delete";
    private static final String COMMAND_EDIT = "edit";
    private static final String COMMAND_SET_URL = "seturl";
    private static final String COMMAND_SET_NAME = "setname";
    private static final String COMMAND_SET_COMMAND = "setcommand";
    private static final String COMMAND_SET_EXECUTOR = "setexecutor";
    private static final String COMMAND_SET_PERM = "setpermission";
    private static final String COMMAND_SET_USE_PERM = "setusepermission";
    private static final String COMMAND_SET_ICON = "seticon";
    private static final String COMMAND_SET_PRIORITY = "setpriority";
    private static final String COMMAND_SET_COOLDOWN = "setcooldown";
    private static final String COMMAND_SET_COST = "setcost";
    private static final String COMMAND_SET_REWARD = "setreward";
    private static final String COMMAND_SET_SOUND = "setsound";
    private static final String COMMAND_SET_ACTIONBAR = "setactionbar";
    private static final String COMMAND_SET_PARTICLE = "setparticle";
    private static final String COMMAND_TOGGLE = "toggle";
    private static final String COMMAND_RESET_CLICKS = "resetclicks";

    private static final String VALUE_NONE = "none";

    public LinksAdminCommandProvider(final LinksModule module) {
        super(module, "links-admin");
    }

    @Override
    public void setup() {
        this.register(COMMAND_CREATE, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_CREATE_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_CREATE)
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(ARG_LINK, info -> List.of()))
                .executes(this::createLink))
            .under("linksadmin");

        this.register(COMMAND_DELETE, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_DELETE_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_DELETE)
                .withArguments(this.linkArgument())
                .executes(this::deleteLink))
            .under("linksadmin");

        this.register(COMMAND_EDIT, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_EDIT_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_EDIT)
                .withRequirement(sender -> sender instanceof Player)
                .executes(this::openEditor))
            .under("linksadmin");

        this.register(COMMAND_SET_URL, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_SET_URL_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_SET_URL)
                .withArguments(this.linkArgument())
                .withOptionalArguments(CommandArgumentConstants.greedy(ARG_URL, info -> List.of()))
                .executes(this::setUrl))
            .under("linksadmin");

        this.register(COMMAND_SET_NAME, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_SET_NAME_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_SET_NAME)
                .withArguments(this.linkArgument(), new GreedyStringArgument(ARG_NAME))
                .executes(this::setName))
            .under("linksadmin");

        this.register(COMMAND_SET_COMMAND, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_SET_COMMAND_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_SET_COMMAND)
                .withArguments(this.linkArgument())
                .withOptionalArguments(CommandArgumentConstants.greedy(ARG_COMMAND, info -> List.of()))
                .executes(this::setCommand))
            .under("linksadmin");

        this.register(COMMAND_SET_EXECUTOR, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_SET_EXECUTOR_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_SET_EXECUTOR)
                .withArguments(this.linkArgument(), CommandArgumentConstants.string(ARG_EXECUTOR,
                        info -> Utils.getEnumNames(LinkExecutor.class)))
                .executes(this::setExecutor))
            .under("linksadmin");

        this.register(COMMAND_SET_PERM, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_SET_PERMISSION_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_SET_PERMISSION)
                .withArguments(this.linkArgument())
                .withOptionalArguments(CommandArgumentConstants.string(ARG_PERMISSION, info -> List.of()))
                .executes(this::setPermission))
            .under("linksadmin");

        this.register(COMMAND_SET_USE_PERM, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_SET_USE_PERMISSION_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_SET_USE_PERMISSION)
                .withArguments(this.linkArgument())
                .withOptionalArguments(CommandArgumentConstants.string(ARG_PERMISSION, info -> List.of()))
                .executes(this::setUsePermission))
            .under("linksadmin");

        this.register(COMMAND_SET_ICON, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_SET_ICON_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_SET_ICON)
                .withArguments(this.linkArgument(), CommandArgumentConstants.string(ARG_MATERIAL,
                        info -> materialSuggestions()))
                .executes(this::setIcon))
            .under("linksadmin");

        this.register(COMMAND_SET_PRIORITY, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_SET_PRIORITY_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_SET_PRIORITY)
                .withArguments(this.linkArgument(), new IntegerArgument(ARG_PRIORITY))
                .executes(this::setPriority))
            .under("linksadmin");

        this.register(COMMAND_SET_COOLDOWN, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_SET_COOLDOWN_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_SET_COOLDOWN)
                .withArguments(this.linkArgument(), new IntegerArgument(ARG_SECONDS, 0))
                .executes(this::setCooldown))
            .under("linksadmin");

        this.register(COMMAND_SET_COST, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_SET_COST_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_SET_COST)
                .withArguments(this.linkArgument(), new DoubleArgument(ARG_COST, 0D))
                .executes(this::setCost))
            .under("linksadmin");

        this.register(COMMAND_SET_REWARD, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_SET_REWARD_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_SET_REWARD)
                .withArguments(this.linkArgument())
                .withOptionalArguments(CommandArgumentConstants.greedy(ARG_COMMAND, info -> List.of()))
                .executes(this::setReward))
            .under("linksadmin");

        this.register(COMMAND_SET_SOUND, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_SET_SOUND_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_SET_SOUND)
                .withArguments(this.linkArgument())
                .withOptionalArguments(CommandArgumentConstants.greedy(ARG_SOUND, info -> List.of()))
                .executes(this::setSound))
            .under("linksadmin");

        this.register(COMMAND_SET_ACTIONBAR, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_SET_ACTIONBAR_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_SET_ACTIONBAR)
                .withArguments(this.linkArgument())
                .withOptionalArguments(CommandArgumentConstants.greedy(ARG_TEXT, info -> List.of()))
                .executes(this::setActionbar))
            .under("linksadmin");

        this.register(COMMAND_SET_PARTICLE, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_SET_PARTICLE_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_SET_PARTICLE)
                .withArguments(this.linkArgument())
                .withOptionalArguments(
                        CommandArgumentConstants.string(ARG_PARTICLE, info -> particleSuggestions()),
                        new IntegerArgument(ARG_COUNT, 0, 100))
                .executes(this::setParticle))
            .under("linksadmin");

        this.register(COMMAND_TOGGLE, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_TOGGLE_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_TOGGLE)
                .withArguments(this.linkArgument())
                .executes(this::toggleLink))
            .under("linksadmin");

        this.register(COMMAND_RESET_CLICKS, builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_RESET_CLICKS_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_RESET_CLICKS)
                .withArguments(this.linkArgument())
                .executes(this::resetClicks))
            .under("linksadmin");

        this.registerRoot("linksadmin", builder -> builder
                .withFullDescription(LinksLang.COMMAND_ADMIN_ROOT_DESC.text())
                .withPermission(LinksPerms.COMMAND_ADMIN_ROOT)).aliases("links-admin");
    }

    private static List<String> materialSuggestions() {
        return Arrays.stream(Material.values())
                .filter(Material::isItem)
                .map(material -> material.getKey().getKey())
                .sorted()
                .toList();
    }

    private static List<String> particleSuggestions() {
        return Arrays.stream(Particle.values()).map(Enum::name).sorted().toList();
    }

    private Argument<String> linkArgument() {
        return CommandArgumentConstants.string(ARG_LINK, info -> this.module.getLinkIds().stream().toList());
    }

    private Link getLink(final CommandSender sender, final CommandArguments arguments) {
        final Object linkObj = arguments.get(ARG_LINK);
        if (!(linkObj instanceof final String id)) {
            return null;
        }

        final Link link = this.module.getLink(id);
        if (link == null) {
            this.module.sendPrefixed(LinksLang.COMMAND_SYNTAX_INVALID_LINK, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_INPUT, () -> id));
        }
        return link;
    }

    // Handlers

    private int createLink(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }

        final Object nameObj = arguments.get(ARG_LINK);
        if (!(nameObj instanceof final String rawId)) {
            return 0;
        }

        return this.module.createLinkAndReport(player, rawId) ? 1 : 0;
    }

    private int deleteLink(final CommandSender sender, final CommandArguments arguments) {
        final Link link = this.getLink(sender, arguments);
        if (link == null) {
            return 0;
        }

        if (!this.module.deleteLink(link)) {
            return 0;
        }

        this.module.sendPrefixed(LinksLang.ADMIN_DELETE_FEEDBACK, sender,
                replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, link::getId));
        // The deleted link's Bukkit command survives until reload and now only prints usage.
        this.module.sendPrefixed(LinksLang.ADMIN_RELOAD_REQUIRED, sender);
        return 1;
    }

    private int openEditor(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }
        this.module.openEditor(player);
        return 1;
    }

    private int setUrl(final CommandSender sender, final CommandArguments arguments) {
        final Link link = this.getLink(sender, arguments);
        if (link == null) {
            return 0;
        }

        final Object urlObj = arguments.get(ARG_URL);
        final String url = urlObj instanceof final String value ? value : "";

        if (url.isBlank() && !link.hasCommand()) {
            this.module.sendPrefixed(LinksLang.ERROR_NOT_ACTIONABLE, sender);
            return 0;
        }

        link.setUrl(url);
        this.module.saveLink(link);

        this.feedback(sender, LinksLang.ADMIN_SET_URL_FEEDBACK, link, url);
        return 1;
    }

    private int setName(final CommandSender sender, final CommandArguments arguments) {
        final Link link = this.getLink(sender, arguments);
        if (link == null) {
            return 0;
        }

        final Object nameObj = arguments.get(ARG_NAME);
        final String name = nameObj instanceof final String value ? value : "";

        link.setDisplay(name);
        this.module.saveLink(link);

        this.feedback(sender, LinksLang.ADMIN_SET_NAME_FEEDBACK, link, name);
        return 1;
    }

    private int setCommand(final CommandSender sender, final CommandArguments arguments) {
        final Link link = this.getLink(sender, arguments);
        if (link == null) {
            return 0;
        }

        final Object commandObj = arguments.get(ARG_COMMAND);
        final String command = commandObj instanceof final String value ? value : "";

        if (command.isBlank() && !link.hasUrl()) {
            this.module.sendPrefixed(LinksLang.ERROR_NOT_ACTIONABLE, sender);
            return 0;
        }

        link.setCommand(command);
        this.module.saveLink(link);

        this.feedback(sender, LinksLang.ADMIN_SET_COMMAND_FEEDBACK, link, command.isEmpty() ? VALUE_NONE : command);
        return 1;
    }

    private int setExecutor(final CommandSender sender, final CommandArguments arguments) {
        final Link link = this.getLink(sender, arguments);
        if (link == null) {
            return 0;
        }

        final Object executorObj = arguments.get(ARG_EXECUTOR);
        final LinkExecutor executor = Utils.enumOptionalValueOf(
                executorObj instanceof final String value ? value : "", LinkExecutor.class).orElse(null);
        if (executor == null) {
            this.module.sendPrefixed(LinksLang.COMMAND_SYNTAX_INVALID_LINK, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_INPUT, () -> String.valueOf(executorObj)));
            return 0;
        }

        link.setExecutor(executor);
        this.module.saveLink(link);

        this.feedback(sender, LinksLang.ADMIN_SET_EXECUTOR_FEEDBACK, link, executor.name());
        return 1;
    }

    private int setPermission(final CommandSender sender, final CommandArguments arguments) {
        final Link link = this.getLink(sender, arguments);
        if (link == null) {
            return 0;
        }

        String permission = this.readString(arguments, ARG_PERMISSION);
        if (VALUE_NONE.equalsIgnoreCase(permission)) {
            permission = "";
        }

        link.setPermission(permission);
        this.module.saveLink(link);

        this.feedback(sender, LinksLang.ADMIN_SET_PERMISSION_FEEDBACK, link,
                permission.isEmpty() ? VALUE_NONE : permission);
        return 1;
    }

    private int setUsePermission(final CommandSender sender, final CommandArguments arguments) {
        final Link link = this.getLink(sender, arguments);
        if (link == null) {
            return 0;
        }

        String permission = this.readString(arguments, ARG_PERMISSION);
        if (VALUE_NONE.equalsIgnoreCase(permission)) {
            permission = "";
        }

        link.setUsePermission(permission);
        this.module.saveLink(link);

        this.feedback(sender, LinksLang.ADMIN_SET_USE_PERMISSION_FEEDBACK, link,
                permission.isEmpty() ? VALUE_NONE : permission);
        return 1;
    }

    private int setIcon(final CommandSender sender, final CommandArguments arguments) {
        final Link link = this.getLink(sender, arguments);
        if (link == null) {
            return 0;
        }

        final Object materialObj = arguments.get(ARG_MATERIAL);
        final Material material = materialObj instanceof final String value ? Material.matchMaterial(value) : null;

        // Air would render as an empty menu slot; fall back to paper like the config loader does.
        final Material effective = (material == null || material.isAir()) ? Material.PAPER : material;
        link.setIcon(NightItem.fromType(effective));
        this.module.saveLink(link);

        this.feedback(sender, LinksLang.ADMIN_SET_ICON_FEEDBACK, link, effective.name());
        return 1;
    }

    private int setPriority(final CommandSender sender, final CommandArguments arguments) {
        final Link link = this.getLink(sender, arguments);
        if (link == null) {
            return 0;
        }

        final Object priorityObj = arguments.get(ARG_PRIORITY);
        if (!(priorityObj instanceof final Integer priority)) {
            return 0;
        }

        link.setPriority(priority);
        this.module.saveLink(link);

        this.feedback(sender, LinksLang.ADMIN_SET_PRIORITY_FEEDBACK, link, String.valueOf(priority));
        return 1;
    }

    private int toggleLink(final CommandSender sender, final CommandArguments arguments) {
        final Link link = this.getLink(sender, arguments);
        if (link == null) {
            return 0;
        }

        link.setEnabled(!link.isEnabled());
        this.module.saveLink(link);

        this.feedback(sender, LinksLang.ADMIN_TOGGLE_FEEDBACK, link, String.valueOf(link.isEnabled()));
        return 1;
    }

    private int setCooldown(final CommandSender sender, final CommandArguments arguments) {
        final Link link = this.getLink(sender, arguments);
        if (link == null) {
            return 0;
        }

        // Non-negative is enforced by the argument type; the setter clamps defensively.
        final Object secondsObj = arguments.get(ARG_SECONDS);
        if (!(secondsObj instanceof final Integer seconds)) {
            return 0;
        }

        link.setCooldown(seconds);
        this.module.saveLink(link);

        this.feedback(sender, LinksLang.ADMIN_SET_COOLDOWN_FEEDBACK, link, String.valueOf(seconds));
        return 1;
    }

    private int setCost(final CommandSender sender, final CommandArguments arguments) {
        final Link link = this.getLink(sender, arguments);
        if (link == null) {
            return 0;
        }

        // Non-negative is enforced by the argument type; the setter clamps defensively.
        final Object costObj = arguments.get(ARG_COST);
        if (!(costObj instanceof final Double cost)) {
            return 0;
        }

        link.setCost(cost);
        this.module.saveLink(link);

        this.feedback(sender, LinksLang.ADMIN_SET_COST_FEEDBACK, link, String.valueOf(cost));
        return 1;
    }

    private int setReward(final CommandSender sender, final CommandArguments arguments) {
        final Link link = this.getLink(sender, arguments);
        if (link == null) {
            return 0;
        }

        final String command = this.readString(arguments, ARG_COMMAND);

        link.setFirstRewardCommand(command);
        this.module.saveLink(link);

        this.feedback(sender, LinksLang.ADMIN_SET_REWARD_FEEDBACK, link, command.isEmpty() ? VALUE_NONE : command);
        return 1;
    }

    private int setSound(final CommandSender sender, final CommandArguments arguments) {
        final Link link = this.getLink(sender, arguments);
        if (link == null) {
            return 0;
        }

        final String raw = this.readString(arguments, ARG_SOUND);
        final String sound = VALUE_NONE.equalsIgnoreCase(raw) ? "" : raw;

        if (!sound.isBlank() && !LinksModule.isValidSound(sound)) {
            this.module.sendPrefixed(LinksLang.ADMIN_SET_INVALID_SOUND, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> sound));
            return 0;
        }

        link.setSound(sound);
        this.module.saveLink(link);

        this.feedback(sender, LinksLang.ADMIN_SET_SOUND_FEEDBACK, link, sound.isEmpty() ? VALUE_NONE : sound);
        return 1;
    }

    private int setActionbar(final CommandSender sender, final CommandArguments arguments) {
        final Link link = this.getLink(sender, arguments);
        if (link == null) {
            return 0;
        }

        final String text = this.readString(arguments, ARG_TEXT);

        link.setActionbar(text);
        this.module.saveLink(link);

        this.feedback(sender, LinksLang.ADMIN_SET_ACTIONBAR_FEEDBACK, link, text.isEmpty() ? VALUE_NONE : text);
        return 1;
    }

    private int setParticle(final CommandSender sender, final CommandArguments arguments) {
        final Link link = this.getLink(sender, arguments);
        if (link == null) {
            return 0;
        }

        final String raw = this.readString(arguments, ARG_PARTICLE);
        final String particle = VALUE_NONE.equalsIgnoreCase(raw) ? "" : raw;

        if (!LinksModule.isValidParticle(particle)) {
            this.module.sendPrefixed(LinksLang.ADMIN_SET_INVALID_PARTICLE, sender,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, () -> particle));
            return 0;
        }

        link.setParticle(particle);
        // Count is optional and keeps its current value when omitted.
        final Object countObj = arguments.get(ARG_COUNT);
        link.setParticleCount(countObj instanceof final Integer count ? count : link.getParticleCount());
        this.module.saveLink(link);

        final String display = link.getParticleName().isEmpty() ? VALUE_NONE
                : link.getParticleName() + " x" + link.getParticleCount();
        this.feedback(sender, LinksLang.ADMIN_SET_PARTICLE_FEEDBACK, link, display);
        return 1;
    }

    private int resetClicks(final CommandSender sender, final CommandArguments arguments) {
        final Link link = this.getLink(sender, arguments);
        if (link == null) {
            return 0;
        }

        link.resetClicks();
        this.module.saveLink(link);

        this.module.sendPrefixed(LinksLang.ADMIN_RESET_CLICKS_FEEDBACK, sender,
                replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE, link::getId));
        return 1;
    }

    private String readString(final CommandArguments arguments, final String key) {
        final Object value = arguments.get(key);
        return value instanceof final String string ? string : "";
    }

    private void feedback(final CommandSender sender, final MessageLocale message, final Link link,
            final String newValue) {
        this.module.sendPrefixed(message, sender, replacer -> replacer
                .with(SLPlaceholders.GENERIC_VALUE, link::getId)
                .with(SLPlaceholders.GENERIC_NEW_VALUE, () -> newValue));
    }
}
