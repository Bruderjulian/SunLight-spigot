package su.nightexpress.sunlight.moduleImpl.kits.command;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.kits.KitsModule;
import su.nightexpress.sunlight.moduleImpl.kits.config.KitsLang;
import su.nightexpress.sunlight.moduleImpl.kits.config.KitsPerms;
import su.nightexpress.sunlight.moduleImpl.kits.model.Kit;
import su.nightexpress.sunlight.utils.TimeUtil;
import su.nightexpress.sunlight.utils.Utils;

public class KitsCommandProvider extends CommandProvider<KitsModule> {

    private static final String ARG_KIT = "kit";

    private static final String COMMAND_EDITOR = "editor";
    private static final String COMMAND_GET = "get";
    private static final String COMMAND_GIVE = "give";
    private static final String COMMAND_LIST = "list";
    private static final String COMMAND_RESET_COOLDOWN = "reset_cooldown";
    private static final String COMMAND_SET_COOLDOWN = "set_cooldown";
    private static final String COMMAND_PREVIEW = "preview";

    public KitsCommandProvider(final KitsModule module) {
        super(module, "kits-commons");
    }

    @Override
    public void setup() {
        this.register(COMMAND_EDITOR, builder -> builder
                .withFullDescription(KitsLang.COMMAND_KITS_EDITOR_DESC.text())
                .withPermission(KitsPerms.COMMAND_EDIT_KIT)
                .withRequirement(sender -> sender instanceof Player)
                .executes(this::openEditor))
            .under("kits");

        this.register(COMMAND_GET, builder -> builder
                .withFullDescription(KitsLang.COMMAND_KITS_GET_DESC.text())
                .withPermission(KitsPerms.COMMAND_KIT_GET)
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(this.kitArgument())
                .executes(this::getKit))
            .under("kits");

        this.register(COMMAND_GIVE, builder -> builder
                .withFullDescription(KitsLang.COMMAND_KITS_GIVE_DESC.text())
                .withPermission(KitsPerms.COMMAND_KIT_GIVE)
                .withArguments(this.kitArgument(), CommandArgumentConstants.targetArgument())
                .executes(this::giveKit))
            .under("kits");

        this.register(COMMAND_LIST, builder -> builder
                .withFullDescription(KitsLang.COMMAND_KITS_LIST_DESC.text())
                .withPermission(KitsPerms.COMMAND_KIT_LIST)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::listKits))
            .under("kits");

        this.register(COMMAND_PREVIEW, builder -> builder
                .withFullDescription(KitsLang.COMMAND_KITS_PREVIEW_DESC.text())
                .withPermission(KitsPerms.COMMAND_PREVIEW_KIT)
                .withArguments(this.kitArgument())
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::previewKit))
            .under("kits");

        this.register(COMMAND_RESET_COOLDOWN, builder -> builder
                .withFullDescription(KitsLang.COMMAND_KITS_RESET_COOLDOWN_DESC.text())
                .withPermission(KitsPerms.COMMAND_RESET_KIT_COOLDOWN)
                .withArguments(this.kitArgument(), CommandArgumentConstants.targetArgument())
                .executes(this::resetCooldown))
            .under("kits");

        this.register(COMMAND_SET_COOLDOWN, builder -> builder
                .withFullDescription(KitsLang.COMMAND_KITS_SET_COOLDOWN_DESC.text())
                .withPermission(KitsPerms.COMMAND_SET_KIT_COOLDOWN)
                .withArguments(
                        this.kitArgument(),
                        new IntegerArgument(CommandArgumentConstants.TIME, 1)
                                .replaceSuggestions(ArgumentSuggestions
                                        .stringCollection(info -> List.of("300", "3600", "86400"))),
                        CommandArgumentConstants.targetArgument())
                .executes(this::setCooldown))
            .under("kits");

        this.registerRoot("kits", builder -> builder
                .withFullDescription(KitsLang.COMMAND_KITS_ROOT_DESC.text())
                .withPermission(KitsPerms.COMMAND_KITS_ROOT)).aliases("kit");
    }

    private Argument<String> kitArgument() {
        return CommandArgumentConstants.string(ARG_KIT,
                info -> info.sender() instanceof final Player player ? this.module.getKitIds(player)
                        : this.module.getKitIds());
    }

    private Kit resolveKit(final CommandSender sender, final CommandArguments arguments) {
        final Object kitObj = arguments.get(ARG_KIT);
        if (!(kitObj instanceof final String kitId)) {
            return null;
        }

        final Kit kit = this.module.getKitById(kitId);
        if (kit == null) {
            this.module.sendPrefixed(KitsLang.COMMAND_SYNTAX_INVALID_KIT, sender);
        }
        return kit;
    }

    private int openEditor(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }
        this.module.openEditor(player);
        return 1;
    }

    private int getKit(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            return 0;
        }

        final Kit kit = this.resolveKit(sender, arguments);
        if (kit == null) {
            return 0;
        }

        return this.module.giveKit(kit, player, false, false) ? 1 : 0;
    }

    private int giveKit(final CommandSender sender, final CommandArguments arguments) {
        final Kit kit = this.resolveKit(sender, arguments);
        if (kit == null) {
            return 0;
        }

        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null || !target.hasTarget()) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return this.loadPlayerAndRunInMainThread(sender, target.playerName(), recipient -> {
            final boolean force = sender != recipient;

            this.module.giveKit(kit, recipient, force, target.silent());

            this.module.sendPrefixed(KitsLang.KIT_GIVE_FEEDBACK, sender, builder -> builder
                    .with(kit.placeholders())
                    .with(CommonPlaceholders.PLAYER.resolver(recipient)));
        }) ? 1 : 0;
    }

    private int listKits(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, KitsPerms.COMMAND_KIT_LIST_OTHERS,
                (user, targetPlayer) -> {
                    this.module.openKitsMenu(targetPlayer);

                    if (sender != targetPlayer) {
                        this.module.sendPrefixed(KitsLang.KIT_BROWSER_OPEN_FEEDBACK, sender, builder -> builder
                                .with(CommonPlaceholders.PLAYER.resolver(targetPlayer)));
                    }
                });
    }

    private int previewKit(final CommandSender sender, final CommandArguments arguments) {
        final Kit kit = this.resolveKit(sender, arguments);
        if (kit == null) {
            return 0;
        }

        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, KitsPerms.COMMAND_PREVIEW_KIT_OTHERS,
                (user, targetPlayer) -> {
                    this.module.previewKit(targetPlayer, kit);

                    if (sender != targetPlayer) {
                        this.module.sendPrefixed(KitsLang.KIT_PREVIEW_FEEDBACK, sender, builder -> builder
                                .with(kit.placeholders())
                                .with(CommonPlaceholders.PLAYER.resolver(targetPlayer)));
                    }
                });
    }

    private int resetCooldown(final CommandSender sender, final CommandArguments arguments) {
        final Kit kit = this.resolveKit(sender, arguments);
        if (kit == null) {
            return 0;
        }

        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null || !target.hasTarget()) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        this.module.userManager().loadTargetProfile(target.playerName()).thenCompose(profile -> {
            if (profile == null) {
                this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                return CompletableFuture.completedFuture(null);
            }

            return this.module.getKitDataOrCreate(profile.id(), kit.getId()).thenAccept(kitData -> {
                kitData.setCooldownDate(0L);
                kitData.markDirty();

                this.module.sendPrefixed(KitsLang.KIT_RESET_COOLDOWN_FEEDBACK, sender, replacer -> replacer
                        .with(kit.placeholders())
                        .with(CommonPlaceholders.PLAYER_NAME, profile::name));

                Player targetPlayer = Utils.getPlayer(profile.id());
                if (targetPlayer != null && !target.silent()) {
                    this.module.sendPrefixed(KitsLang.KIT_RESET_COOLDOWN_NOTIFY, targetPlayer, replacer -> replacer
                            .with(kit.placeholders()));
                }
            }).whenComplete(Utils::printStacktrace);
        });

        return 1;
    }

    private int setCooldown(final CommandSender sender, final CommandArguments arguments) {
        final Kit kit = this.resolveKit(sender, arguments);
        if (kit == null) {
            return 0;
        }

        final Object amountObj = arguments.get(CommandArgumentConstants.TIME);
        final Integer amount = amountObj instanceof final Integer value ? value : null;
        if (amount == null) {
            return 0;
        }

        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null || !target.hasTarget()) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        this.module.userManager().loadTargetProfile(target.playerName()).thenCompose(profile -> {
            if (profile == null) {
                this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                return CompletableFuture.completedFuture(null);
            }

            return this.module.getKitDataOrCreate(profile.id(), kit.getId()).thenAccept(kitData -> {
                kitData.setCooldownDate(TimeUtil.createFutureTimestamp(amount));
                kitData.markDirty();

                this.module.sendPrefixed(KitsLang.KIT_SET_COOLDOWN_FEEDBACK, sender, replacer -> replacer
                        .with(kit.placeholders())
                        .with(CommonPlaceholders.PLAYER_NAME, profile::name)
                        .with(SLPlaceholders.GENERIC_AMOUNT, () -> TimeFormats
                                .formatAmount(TimeUnit.SECONDS.toMillis(amount), TimeFormatType.LITERAL)));

                Player targetPlayer = Utils.getPlayer(profile.id());
                if (targetPlayer != null && !target.silent()) {
                    this.module.sendPrefixed(KitsLang.KIT_SET_COOLDOWN_NOTIFY, targetPlayer, replacer -> replacer
                            .with(kit.placeholders())
                            .with(SLPlaceholders.GENERIC_AMOUNT, () -> TimeFormats
                                    .formatAmount(TimeUnit.SECONDS.toMillis(amount), TimeFormatType.LITERAL)));
                }
            });
        }).whenComplete(Utils::printStacktrace);

        return 1;
    }
}
