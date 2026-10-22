package su.nightexpress.sunlight.moduleImpl.voting.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.voting.VotingModule;
import su.nightexpress.sunlight.moduleImpl.voting.config.VotingLang;
import su.nightexpress.sunlight.moduleImpl.voting.config.VotingPerms;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteTemplate;

/**
 * Player-facing template commands.
 * <p>
 * Templates are admin-defined safe shapes; players may only instantiate templates
 * flagged player-allowed (or hold the staff create permission).
 */
public class VotingTemplateCommandProvider extends CommandProvider<VotingModule> {

    private static final String COMMAND_TEMPLATE = "template";
    private static final String ARGUMENT_TEMPLATE = "template";
    private static final String ARGUMENT_NAME = "name";

    public VotingTemplateCommandProvider(final VotingModule module) {
        super(module, "voting-template");
    }

    @Override
    public void setup() {
        this.register(COMMAND_TEMPLATE, builder -> builder
                .withFullDescription(VotingLang.COMMAND_TEMPLATE_DESC.text())
                .withPermission(VotingPerms.COMMAND_TEMPLATE)
                .executes(this::list)
                .withSubcommand(new CommandAPICommand("list")
                        .withPermission(VotingPerms.COMMAND_TEMPLATE)
                        .executes(this::list))
                .withSubcommand(new CommandAPICommand("use")
                        .withPermission(VotingPerms.COMMAND_TEMPLATE)
                        .withArguments(this.templateArgument())
                        .withOptionalArguments(CommandArgumentConstants.string(ARGUMENT_NAME, info -> List.of()))
                        .executes(this::useWithName)))
                .under(VotingCommands.ROOT_NAME);
    }

    private Argument<String> templateArgument() {
        return CommandArgumentConstants.string(ARGUMENT_TEMPLATE,
                info -> this.module.getTemplates().keySet().stream().sorted().toList());
    }

    private int list(final CommandSender sender, final CommandArguments arguments) {
        final var templates = this.module.getTemplates();
        if (templates.isEmpty()) {
            this.module.sendPrefixed(VotingLang.ERROR_NO_VOTES, sender);
            return 0;
        }
        templates.values().stream()
                .sorted((first, second) -> first.getId().compareTo(second.getId()))
                .forEach(template -> sender.sendMessage("<dark_gray>• <white>" + template.getDisplay()
                        + " <dark_gray>(" + template.getId() + ")"));
        return 1;
    }

    private int useWithName(final CommandSender sender, final CommandArguments arguments) {
        final Object raw = arguments.getOrDefaultUnchecked(ARGUMENT_TEMPLATE, null);
        final Object nameRaw = arguments.getOrDefaultUnchecked(ARGUMENT_NAME, null);
        if (!(raw instanceof final String templateId) || templateId.isBlank()) {
            this.module.sendPrefixed(CoreLang.COMMAND_SYNTAX_GENERIC_ERROR, sender);
            return 0;
        }
        final VoteTemplate template = this.module.getTemplate(templateId);
        if (template == null) {
            this.module.sendPrefixed(VotingLang.ERROR_UNKNOWN_TEMPLATE, sender, b -> b
                    .with(SLPlaceholders.GENERIC_TARGET, () -> templateId)
                    .with(SLPlaceholders.GENERIC_LIST,
                            () -> String.join(", ", this.module.getTemplates().keySet().stream().sorted().toList())));
            return 0;
        }
        if (!template.isPlayerAllowed() && !sender.hasPermission(VotingPerms.COMMAND_CREATE)
                && !sender.hasPermission(this.templatePermission(template))) {
            this.module.sendPrefixed(VotingLang.ERROR_TEMPLATE_LOCKED, sender,
                    b -> b.with(SLPlaceholders.GENERIC_TARGET, template::getId));
            return 0;
        }
        final String key = nameRaw instanceof final String name && !name.isBlank() ? name : "";
        return this.module.createFromTemplate(sender, template, key) != null ? 1 : 0;
    }

    private String templatePermission(final VoteTemplate template) {
        return VotingPerms.MODULE + ".template." + template.getId();
    }
}
