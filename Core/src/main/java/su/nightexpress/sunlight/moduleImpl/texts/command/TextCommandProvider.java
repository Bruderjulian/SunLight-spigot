package su.nightexpress.sunlight.moduleImpl.texts.command;


import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.texts.TextsLang;
import su.nightexpress.sunlight.moduleImpl.texts.TextsModule;
import su.nightexpress.sunlight.moduleImpl.texts.TextsPerms;
import su.nightexpress.sunlight.moduleImpl.texts.text.Text;

public class TextCommandProvider extends CommandProvider<TextsModule> {

    public TextCommandProvider(final TextsModule module) {
        super(module, "texts-text");
    }

    @Override
    public void setup() {
        this.register("customtext", command -> command
                .withFullDescription(TextsLang.COMMAND_TEXT_DESC.text())
                .withPermission(TextsPerms.COMMAND_TEXT)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.NAME,
                        info -> this.module.getCustomTexts().stream()
                                .filter(text -> text.hasPermission(info.sender()))
                                .map(Text::getId)
                                .toList()))
                .executes(this::showText));
    }

    private int showText(final CommandSender sender, final CommandArguments arguments) {
        final String name = arguments.getUnchecked(CommandArgumentConstants.NAME);
        final Text text = name == null ? null : this.module.getTextById(name);
        if (text == null) {
            this.module.sendPrefixed(TextsLang.COMMAND_SYNTAX_INVALID_TEXT, sender);
            return 0;
        }
        if (!text.hasPermission(sender)) {
            this.module.sendPrefixed(CoreLang.ERROR_NO_PERMISSION, sender);
            return 0;
        }

        this.module.showText(sender, text);
        return 1;
    }
}
