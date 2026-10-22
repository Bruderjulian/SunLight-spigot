package su.nightexpress.sunlight.moduleImpl.essential.command;


import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.GRAY;

public class SuicideCommandProvider extends CommandProvider<EssentialModule> {

    private static final String COMMAND_SUICIDE = "suicide";

    private static final String PERMISSION = EssentialPerms.COMMAND + ".suicide";

    private static final TextLocale DESCRIPTION = LangEntry.builder("Command.Suicide.Desc").text("Commit suicide.");

    private static final MessageLocale MESSAGE_SUICIDE_NOTIFY = LangEntry.builder("Command.Suicide.Notify").chatMessage(
            GRAY.wrap("You have commited suicide."));

    public SuicideCommandProvider(final EssentialModule module) {
        super(module, "suicide");
    }

    @Override
    public void setup() {
        this.register(COMMAND_SUICIDE, command -> command
                .withFullDescription(DESCRIPTION.text())
                .withPermission(PERMISSION)
                .withRequirement(sender -> sender instanceof Player)
                .executes((sender, arguments) -> {
                    return this.commitSuicide(sender, arguments);
                }));
    }

    private int commitSuicide(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player target)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }

        final DamageSource source = DamageSource.builder(DamageType.GENERIC_KILL).withDirectEntity(target)
                .withCausingEntity(target).build();
        target.damage(Integer.MAX_VALUE, source);
        target.setHealth(0);
        this.module.sendPrefixed(MESSAGE_SUICIDE_NOTIFY, target,
                replacer -> replacer.with(CommonPlaceholders.PLAYER.resolver(target)));
        return 1;
    }
}