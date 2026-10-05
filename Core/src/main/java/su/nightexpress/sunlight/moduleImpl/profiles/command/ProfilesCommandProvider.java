package su.nightexpress.sunlight.moduleImpl.profiles.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.profiles.PlayerProfile;
import su.nightexpress.sunlight.moduleImpl.profiles.ProfileManager;
import su.nightexpress.sunlight.moduleImpl.profiles.ProfilesModule;
import su.nightexpress.sunlight.moduleImpl.profiles.config.ProfilesLang;
import su.nightexpress.sunlight.moduleImpl.profiles.config.ProfilesPerms;
import su.nightexpress.sunlight.user.SunUser;

public class ProfilesCommandProvider extends CommandProvider<ProfilesModule> {

    private static final String ARG_PROFILE = "profile";
    private static final String ARG_NAME = "name";

    public ProfilesCommandProvider(ProfilesModule module) {
        super(module, "profiles");
    }

    @Override
    public void setup() {
        this.register("switch", command -> command
                .withFullDescription(ProfilesLang.COMMAND_SWITCH_DESC.text())
                .withPermission(ProfilesPerms.COMMAND_SWITCH)
                .withArguments(this.profileArgument())
                .executes(this::switchProfile))
                .under("profile");

        this.register("list", command -> command
                .withFullDescription(ProfilesLang.COMMAND_LIST_DESC.text())
                .withPermission(ProfilesPerms.COMMAND_LIST)
                .executes(this::listProfiles))
                .under("profile");

        this.register("create", command -> command
                .withFullDescription(ProfilesLang.COMMAND_CREATE_DESC.text())
                .withPermission(ProfilesPerms.COMMAND_CREATE)
                .withArguments(CommandArgumentConstants.string(ARG_NAME, info -> List.of()))
                .executes(this::createProfile))
                .under("profile");

        this.register("rename", command -> command
                .withFullDescription(ProfilesLang.COMMAND_RENAME_DESC.text())
                .withPermission(ProfilesPerms.COMMAND_RENAME)
                .withArguments(this.profileArgument(), CommandArgumentConstants.string(ARG_NAME, info -> List.of()))
                .executes(this::renameProfile))
                .under("profile");

        this.register("delete", command -> command
                .withFullDescription(ProfilesLang.COMMAND_DELETE_DESC.text())
                .withPermission(ProfilesPerms.COMMAND_DELETE)
                .withArguments(this.profileArgument())
                .executes(this::deleteProfile))
                .under("profile");

        this.register("info", command -> command
                .withFullDescription(ProfilesLang.COMMAND_INFO_DESC.text())
                .withPermission(ProfilesPerms.COMMAND_INFO)
                .withOptionalArguments(this.profileArgument())
                .executes(this::infoProfile))
                .under("profile");

        this.registerRoot("profile", command -> command
                .withFullDescription(ProfilesLang.COMMAND_PROFILE_DESC.text())
                .withPermission(ProfilesPerms.COMMAND_PROFILE)
                .executes(this::listProfiles))
                .aliases("profiles");
    }

    private Argument<String> profileArgument() {
        return CommandArgumentConstants.string(ARG_PROFILE, info -> {
            if (!(info.sender() instanceof Player player)) return List.of();
            try {
                return this.module.getManager().profileNames(player.getUniqueId());
            } catch (Exception exception) {
                return List.of();
            }
        });
    }

    private int listProfiles(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        ProfileManager manager = this.module.getManager();
        SunUser user = this.module.userManager().getOrFetch(player);
        manager.ensureLoaded(player, user);

        List<PlayerProfile> profiles = manager.getProfileList(player.getUniqueId());
        if (profiles.isEmpty()) {
            this.module.sendPrefixed(ProfilesLang.LIST_EMPTY, sender);
            return 1;
        }
        String activeId = manager.getActiveId(user);
        this.module.sendPrefixed(ProfilesLang.LIST_HEADER, sender, builder -> builder
                .with(SLPlaceholders.GENERIC_CURRENT, () -> String.valueOf(profiles.size()))
                .with(SLPlaceholders.GENERIC_MAX, () -> String.valueOf(manager.maxSlots(player))));
        for (PlayerProfile profile : profiles) {
            boolean active = profile.getId().equals(activeId);
            this.module.sendPrefixed(ProfilesLang.LIST_ENTRY, sender, builder -> builder
                    .with(SLPlaceholders.GENERIC_NAME, () -> profile.getName())
                    .with(SLPlaceholders.GENERIC_VALUE, () -> profile.getId())
                    .with(SLPlaceholders.GENERIC_STATUS, () -> active ? " §a(active)" : ""));
        }
        return 1;
    }

    private int switchProfile(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        Object raw = arguments.get(ARG_PROFILE);
        if (!(raw instanceof String query) || query.isBlank()) {
            return this.listProfiles(sender, arguments);
        }
        ProfileManager manager = this.module.getManager();
        SunUser user = this.module.userManager().getOrFetch(player);

        boolean bypass = player.hasPermission(ProfilesPerms.BYPASS_SAFETY);
        ProfileManager.SwitchResult result = manager.switchTo(player, user, query, bypass);

        switch (result.block()) {
            case NONE -> {
                PlayerProfile target = manager.findByIdOrName(player.getUniqueId(), query);
                String name = target == null ? query : target.getName();
                this.module.sendPrefixed(ProfilesLang.SWITCH_NOTIFY, sender, builder -> builder
                        .with(SLPlaceholders.GENERIC_NAME, () -> name));
                return 1;
            }
            case NOT_FOUND -> this.module.sendPrefixed(ProfilesLang.NOT_FOUND, sender, builder -> builder
                    .with(SLPlaceholders.GENERIC_NAME, () -> query));
            case ALREADY_ACTIVE -> {
                PlayerProfile target = manager.findByIdOrName(player.getUniqueId(), query);
                String name = target == null ? query : target.getName();
                this.module.sendPrefixed(ProfilesLang.ALREADY_ACTIVE, sender, builder -> builder
                        .with(SLPlaceholders.GENERIC_NAME, () -> name));
            }
            case COOLDOWN -> this.module.sendPrefixed(ProfilesLang.SWITCH_COOLDOWN, sender, builder -> builder
                    .with(SLPlaceholders.GENERIC_TIME, () -> formatSeconds(result.cooldownLeftMs())));
            case DEAD -> this.module.sendPrefixed(ProfilesLang.SWITCH_BLOCKED_DEAD, sender);
            case VANISHED -> this.module.sendPrefixed(ProfilesLang.SWITCH_BLOCKED_VANISHED, sender);
            case FROZEN -> this.module.sendPrefixed(ProfilesLang.SWITCH_BLOCKED_FROZEN, sender);
            case FLYING -> this.module.sendPrefixed(ProfilesLang.SWITCH_BLOCKED_FLYING, sender);
            case CANCELLED, FAILED -> this.module.sendPrefixed(ProfilesLang.SWITCH_FAILED, sender);
        }
        return 0;
    }

    private int createProfile(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        Object raw = arguments.get(ARG_NAME);
        String name = raw instanceof String value ? value.trim() : "";
        ProfileManager manager = this.module.getManager();
        SunUser user = this.module.userManager().getOrFetch(player);

        switch (manager.create(player, user, name)) {
            case OK -> {
                this.module.sendPrefixed(ProfilesLang.CREATED, sender, builder -> builder
                        .with(SLPlaceholders.GENERIC_NAME, () -> name)
                        .with(SLPlaceholders.GENERIC_VALUE, () -> name.toLowerCase(java.util.Locale.ROOT)));
                return 1;
            }
            case LIMIT -> this.module.sendPrefixed(ProfilesLang.CREATE_LIMIT, sender, builder -> builder
                    .with(SLPlaceholders.GENERIC_MAX, () -> String.valueOf(manager.maxSlots(player))));
            case EXISTS -> this.module.sendPrefixed(ProfilesLang.CREATE_EXISTS, sender, builder -> builder
                    .with(SLPlaceholders.GENERIC_NAME, () -> name));
            case INVALID_NAME -> this.module.sendPrefixed(ProfilesLang.CREATE_INVALID_NAME, sender);
        }
        return 0;
    }

    private int renameProfile(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        if (!this.module.getSettings().isRenameAllowed()) {
            this.module.sendPrefixed(ProfilesLang.RENAME_DISABLED, sender);
            return 0;
        }
        Object rawProfile = arguments.get(ARG_PROFILE);
        Object rawName = arguments.get(ARG_NAME);
        String query = rawProfile instanceof String value ? value : "";
        String newName = rawName instanceof String value ? value.trim() : "";

        // Renaming the active profile id would orphan the active pointer, so
        // re-point it when needed.
        ProfileManager manager = this.module.getManager();
        SunUser user = this.module.userManager().getOrFetch(player);
        PlayerProfile before = manager.findByIdOrName(player.getUniqueId(), query);
        String activeBefore = before == null ? null : manager.getActiveId(user).equals(before.getId()) ? before.getId() : null;

        switch (manager.rename(player.getUniqueId(), query, newName)) {
            case OK -> {
                if (activeBefore != null) {
                    manager.setActiveId(user, newName.toLowerCase(java.util.Locale.ROOT));
                }
                this.module.sendPrefixed(ProfilesLang.RENAMED, sender, builder -> builder
                        .with(SLPlaceholders.GENERIC_NAME, () -> newName));
                return 1;
            }
            case NOT_FOUND -> this.module.sendPrefixed(ProfilesLang.NOT_FOUND, sender, builder -> builder
                    .with(SLPlaceholders.GENERIC_NAME, () -> query));
            case EXISTS -> this.module.sendPrefixed(ProfilesLang.CREATE_EXISTS, sender, builder -> builder
                    .with(SLPlaceholders.GENERIC_NAME, () -> newName));
            case INVALID_NAME -> this.module.sendPrefixed(ProfilesLang.CREATE_INVALID_NAME, sender);
        }
        return 0;
    }

    private int deleteProfile(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        if (!this.module.getSettings().isDeleteAllowed()) {
            this.module.sendPrefixed(ProfilesLang.DELETE_DISABLED, sender);
            return 0;
        }
        Object raw = arguments.get(ARG_PROFILE);
        String query = raw instanceof String value ? value : "";
        ProfileManager manager = this.module.getManager();
        SunUser user = this.module.userManager().getOrFetch(player);

        switch (manager.delete(player, user, query)) {
            case OK -> {
                this.module.sendPrefixed(ProfilesLang.DELETED, sender, builder -> builder
                        .with(SLPlaceholders.GENERIC_NAME, () -> query));
                return 1;
            }
            case NOT_FOUND -> this.module.sendPrefixed(ProfilesLang.NOT_FOUND, sender, builder -> builder
                    .with(SLPlaceholders.GENERIC_NAME, () -> query));
            case ACTIVE -> this.module.sendPrefixed(ProfilesLang.DELETE_ACTIVE, sender);
            case LAST -> this.module.sendPrefixed(ProfilesLang.DELETE_LAST, sender);
        }
        return 0;
    }

    private int infoProfile(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        ProfileManager manager = this.module.getManager();
        SunUser user = this.module.userManager().getOrFetch(player);
        Object raw = arguments.get(ARG_PROFILE);
        String query = raw instanceof String value && !value.isBlank() ? value
            : manager.getActiveId(user);

        PlayerProfile profile = manager.findByIdOrName(player.getUniqueId(), query);
        if (profile == null) {
            this.module.sendPrefixed(ProfilesLang.NOT_FOUND, sender, builder -> builder
                    .with(SLPlaceholders.GENERIC_NAME, () -> query));
            return 0;
        }
        boolean active = manager.getActiveId(user).equals(profile.getId());
        this.module.sendPrefixed(ProfilesLang.INFO_HEADER, sender, builder -> builder
                .with(SLPlaceholders.GENERIC_NAME, () -> profile.getName())
                .with(SLPlaceholders.GENERIC_VALUE, () -> profile.getId()));
        this.module.sendPrefixed(ProfilesLang.INFO_ACTIVE, sender, builder -> builder
                .with(SLPlaceholders.GENERIC_STATE, () -> active ? "yes" : "no"));
        this.module.sendPrefixed(ProfilesLang.INFO_LAST_PLAYED, sender, builder -> builder
                .with(SLPlaceholders.GENERIC_TIME, () -> manager.formatLastPlayed(profile.getLastPlayed())));
        return 1;
    }

    private static String formatSeconds(long millis) {
        long seconds = Math.max(0L, millis + 999L) / 1000L;
        if (seconds < 60L) return seconds + "s";
        long minutes = seconds / 60L;
        long rest = seconds % 60L;
        return rest == 0L ? minutes + "m" : minutes + "m " + rest + "s";
    }
}
