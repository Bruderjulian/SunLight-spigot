package su.nightexpress.sunlight.moduleImpl.socials.command;

import org.bukkit.command.CommandSender;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.socials.SocialsModule;
import su.nightexpress.sunlight.moduleImpl.socials.config.SocialLink;
import su.nightexpress.sunlight.moduleImpl.socials.config.SocialsLang;
import su.nightexpress.sunlight.moduleImpl.socials.config.SocialsPerms;

public class SocialsCommandProvider extends CommandProvider {

    private static final String COMMAND_SOCIALS = "socials";

    private static final String[] LINK_IDS = {"discord", "site", "store", "vote", "map"};

    private final SocialsModule module;

    public SocialsCommandProvider(SunLightPlugin plugin, SocialsModule module) {
        super(plugin);
        this.module = module;
    }

    @Override
    public void registerDefaults() {
        this.registerLiteral(COMMAND_SOCIALS, true, new String[]{"socials"}, builder -> builder
            .description(SocialsLang.COMMAND_SOCIALS_DESC)
            .permission(SocialsPerms.COMMAND_SOCIALS)
            .executes(this::showLinks));

        for (String id : LINK_IDS) {
            this.registerLiteral(id, true, new String[]{id}, builder -> builder
                .description(SocialsLang.COMMAND_LINK_DESC)
                .permission(SocialsPerms.COMMAND_LINK)
                .executes((context, arguments) -> this.sendLink(context, arguments, id)));
        }
    }

    private boolean showLinks(CommandContext context, ParsedArguments arguments) {
        CommandSender sender = context.getSender();
        if (context.isPlayer()) {
            this.module.openLinks(context.getPlayerOrThrow());
        } else {
            this.module.sendLinksList(sender);
        }
        return true;
    }

    private boolean sendLink(CommandContext context, ParsedArguments arguments, String id) {
        CommandSender sender = context.getSender();
        SocialLink link = this.module.getLinkMap().get(id);
        if (link == null) {
            this.module.sendPrefixed(SocialsLang.LINKS_EMPTY, sender);
            return false;
        }
        if (!SocialsPerms.hasLinkAccess(sender, link.permission(), id)) {
            this.module.sendPrefixed(SocialsLang.ERROR_NO_PERMISSION, sender);
            return false;
        }
        this.module.sendLink(sender, link);
        return true;
    }
}
