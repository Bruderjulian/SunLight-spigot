package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.placeholder.Replacer;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;
import su.nightexpress.sunlight.utils.Utils;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.BR;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.GRAY;
import static su.nightexpress.sunlight.SLPlaceholders.*;

public class StaffCommandProvider extends CommandProvider<EssentialModule> {

    private static final String COMMAND_STAFF = "staff";

    private static final String STAFF = EssentialPerms.COMMAND + ".staff";

    private static final TextLocale DESCRIPTION = LangEntry.builder("Command.Staff.Desc").text("Show online staff.");

    private static final MessageLocale MESSAGE_NO_STAFF_ONLINE = LangEntry.builder("Command.Staff.Empty").chatMessage(
            GRAY.wrap("There is no staff online."));

    public StaffCommandProvider(final EssentialModule module) {
        super(module, "staff");
    }

    @Override
    public void setup() {
        this.register(COMMAND_STAFF, command -> command
                .withFullDescription(DESCRIPTION.text())
                .withPermission(STAFF)
                .executes((sender, arguments) -> {
                    return this.showStaff(sender, arguments);
                }));
    }

    private String formatEntry(final Player player) {
        return forPlayerWithPAPI(player).apply(this.module.settings().staffEntryFormat.get());
    }

    private int showStaff(final CommandSender sender, final CommandArguments arguments) {
        final Player executor = sender instanceof final Player player ? player : null;
        final Set<Player> staffs = new HashSet<>();

        Utils.onlinePlayers().forEach(other -> {
            if (executor != null && !executor.canSee(other)) {
                return;
            }

            final Set<String> playerRanks = Players.getInheritanceGroups(other);
            if (playerRanks.stream().anyMatch(this.module.settings().staffRanks.get()::contains)) {
                staffs.add(other);
            }
        });

        if (staffs.isEmpty()) {
            this.module.sendPrefixed(MESSAGE_NO_STAFF_ONLINE, sender);
            return 0;
        }

        final String entries = staffs.stream()
                .sorted(Comparator.comparing(Player::getName))
                .map(this::formatEntry)
                .collect(Collectors.joining(BR));

        final String text = String.join("\n", Replacer.create()
                .replace(GENERIC_ENTRY, entries)
                .replace(GENERIC_AMOUNT, () -> String.valueOf(staffs.size()))
                .apply(this.module.settings().staffFormat.get()));

        Players.sendMessage(sender, text);

        return 1;
    }
}