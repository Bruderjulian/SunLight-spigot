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
    private static final String ARG_MATERIAL = "material";
    private static final String ARG_TEXT = "text";
    private static final String ARG_ACTION = "action";
    private static final String ARG_PLAYER = "player";

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

        this.register("clone", command -> command
                .withFullDescription(ProfilesLang.COMMAND_CLONE_DESC.text())
                .withPermission(ProfilesPerms.COMMAND_CLONE)
                .withArguments(this.profileArgument(), CommandArgumentConstants.string(ARG_NAME, info -> List.of()))
                .executes(this::cloneProfile))
                .under("profile");

        this.register("copy", command -> command
                .withFullDescription(ProfilesLang.COMMAND_CLONE_DESC.text())
                .withPermission(ProfilesPerms.COMMAND_CLONE)
                .withArguments(this.profileArgument(), CommandArgumentConstants.string(ARG_NAME, info -> List.of()))
                .executes(this::cloneProfile))
                .under("profile");

        this.register("icon", command -> command
                .withFullDescription(ProfilesLang.COMMAND_ICON_DESC.text())
                .withPermission(ProfilesPerms.COMMAND_ICON)
                .withArguments(this.profileArgument(), CommandArgumentConstants.string(ARG_MATERIAL,
                    info -> java.util.Arrays.stream(org.bukkit.Material.values())
                        .map(org.bukkit.Material::name).sorted().toList()))
                .executes(this::iconProfile))
                .under("profile");

        this.register("describe", command -> command
                .withFullDescription(ProfilesLang.COMMAND_DESCRIBE_DESC.text())
                .withPermission(ProfilesPerms.COMMAND_DESCRIBE)
                .withArguments(this.profileArgument())
                .withOptionalArguments(CommandArgumentConstants.greedy(ARG_TEXT, info -> List.of()))
                .executes(this::describeProfile))
                .under("profile");

        this.register("admin", command -> command
                .withFullDescription(ProfilesLang.COMMAND_ADMIN_DESC.text())
                .withPermission(ProfilesPerms.COMMAND_ADMIN)
                .withArguments(CommandArgumentConstants.string(ARG_ACTION,
                        info -> List.of("list", "switch", "delete", "reset")),
                    CommandArgumentConstants.string(ARG_PLAYER, info -> CommandArgumentConstants.onlinePlayerNames()))
                .withOptionalArguments(this.profileArgument())
                .executes(this::adminProfiles))
                .under("profile");

        this.registerRoot("profile", command -> command
                .withFullDescription(ProfilesLang.COMMAND_PROFILE_DESC.text())
                .withPermission(ProfilesPerms.COMMAND_PROFILE)
                .executes(this::openMenu))
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

    private int openMenu(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        return this.module.openProfilesMenu(player) ? 1 : 0;
    }

    private int switchProfile(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        Object raw = arguments.get(ARG_PROFILE);
        if (!(raw instanceof String query) || query.isBlank()) {
            return this.openMenu(sender, arguments);
        }
        return this.module.switchToProfile(player, query) ? 1 : 0;
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

    private int cloneProfile(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        Object rawSource = arguments.get(ARG_PROFILE);
        Object rawName = arguments.get(ARG_NAME);
        String source = rawSource instanceof String value ? value : "";
        String newName = rawName instanceof String value ? value.trim() : "";
        ProfileManager manager = this.module.getManager();

        switch (manager.cloneProfile(player.getUniqueId(), manager.maxSlots(player), source, newName)) {
            case OK -> {
                this.module.sendPrefixed(ProfilesLang.CLONED, sender, builder -> builder
                        .with(SLPlaceholders.GENERIC_NAME, () -> source)
                        .with(SLPlaceholders.GENERIC_VALUE, () -> newName));
                return 1;
            }
            case NOT_FOUND -> this.module.sendPrefixed(ProfilesLang.NOT_FOUND, sender, builder -> builder
                    .with(SLPlaceholders.GENERIC_NAME, () -> source));
            case LIMIT -> this.module.sendPrefixed(ProfilesLang.CREATE_LIMIT, sender, builder -> builder
                    .with(SLPlaceholders.GENERIC_MAX, () -> String.valueOf(manager.maxSlots(player))));
            case EXISTS -> this.module.sendPrefixed(ProfilesLang.CREATE_EXISTS, sender, builder -> builder
                    .with(SLPlaceholders.GENERIC_NAME, () -> newName));
            case INVALID_NAME -> this.module.sendPrefixed(ProfilesLang.CREATE_INVALID_NAME, sender);
        }
        return 0;
    }

    private int iconProfile(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        Object rawProfile = arguments.get(ARG_PROFILE);
        Object rawMaterial = arguments.get(ARG_MATERIAL);
        String query = rawProfile instanceof String value ? value : "";
        String material = rawMaterial instanceof String value ? value.trim().toUpperCase(java.util.Locale.ROOT) : "";
        ProfileManager manager = this.module.getManager();

        if (org.bukkit.Material.matchMaterial(material) == null) {
            this.module.sendPrefixed(ProfilesLang.ICON_INVALID, sender, builder -> builder
                    .with(SLPlaceholders.GENERIC_VALUE, () -> material));
            return 0;
        }
        if (!manager.setIcon(player.getUniqueId(), query, material)) {
            this.module.sendPrefixed(ProfilesLang.NOT_FOUND, sender, builder -> builder
                    .with(SLPlaceholders.GENERIC_NAME, () -> query));
            return 0;
        }
        this.module.sendPrefixed(ProfilesLang.ICON_SET, sender, builder -> builder
                .with(SLPlaceholders.GENERIC_NAME, () -> query));
        return 1;
    }

    private int describeProfile(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        Object rawProfile = arguments.get(ARG_PROFILE);
        Object rawText = arguments.get(ARG_TEXT);
        String query = rawProfile instanceof String value ? value : "";
        String text = rawText instanceof String value ? value.trim() : "";
        ProfileManager manager = this.module.getManager();

        if (!manager.setDescription(player.getUniqueId(), query, text)) {
            this.module.sendPrefixed(ProfilesLang.NOT_FOUND, sender, builder -> builder
                    .with(SLPlaceholders.GENERIC_NAME, () -> query));
            return 0;
        }
        this.module.sendPrefixed(ProfilesLang.DESCRIBED, sender, builder -> builder
                .with(SLPlaceholders.GENERIC_NAME, () -> query));
        return 1;
    }

    private int adminProfiles(CommandSender sender, CommandArguments arguments) {
        Object rawAction = arguments.get(ARG_ACTION);
        Object rawPlayer = arguments.get(ARG_PLAYER);
        Object rawProfile = arguments.get(ARG_PROFILE);
        String action = rawAction instanceof String value ? value.trim().toLowerCase(java.util.Locale.ROOT) : "";
        String playerName = rawPlayer instanceof String value ? value.trim() : "";
        String profileQuery = rawProfile instanceof String value ? value.trim() : "";
        ProfileManager manager = this.module.getManager();

        String needed = switch (action) {
            case "list" -> ProfilesPerms.COMMAND_ADMIN_LIST;
            case "switch" -> ProfilesPerms.COMMAND_ADMIN_SWITCH;
            case "delete" -> ProfilesPerms.COMMAND_ADMIN_DELETE;
            case "reset" -> ProfilesPerms.COMMAND_ADMIN_RESET;
            default -> ProfilesPerms.COMMAND_ADMIN;
        };
        if (!sender.hasPermission(needed) && !sender.hasPermission(ProfilesPerms.ADMIN)) {
            this.module.sendPrefixed(CoreLang.ERROR_NO_PERMISSION, sender);
            return 0;
        }

        Player online = su.nightexpress.sunlight.utils.Utils.getPlayer(playerName);
        if (online == null) {
            org.bukkit.OfflinePlayer offline = this.module.plugin().getServer().getOfflinePlayer(playerName);
            if (!offline.hasPlayedBefore() && offline.getName() == null) {
                this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                return 0;
            }
            return this.adminOffline(sender, offline.getUniqueId(), offline.getName(), action, profileQuery);
        }

        SunUser targetUser = this.module.userManager().getOrFetch(online);
        return switch (action) {
            case "list" -> {
                this.sendAdminList(sender, online.getName(), manager.getProfileList(online.getUniqueId()),
                    manager.getActiveId(targetUser));
                yield 1;
            }
            case "switch" -> this.module.adminSwitch(sender, online, profileQuery) ? 1 : 0;
            case "delete" -> {
                switch (manager.delete(online, targetUser, profileQuery)) {
                    case OK -> {
                        this.module.sendPrefixed(ProfilesLang.DELETED, sender, builder -> builder
                                .with(SLPlaceholders.GENERIC_NAME, () -> profileQuery));
                        yield 1;
                    }
                    case NOT_FOUND -> {
                        this.module.sendPrefixed(ProfilesLang.NOT_FOUND, sender, builder -> builder
                                .with(SLPlaceholders.GENERIC_NAME, () -> profileQuery));
                        yield 0;
                    }
                    case ACTIVE -> {
                        this.module.sendPrefixed(ProfilesLang.DELETE_ACTIVE, sender);
                        yield 0;
                    }
                    default -> {
                        this.module.sendPrefixed(ProfilesLang.DELETE_LAST, sender);
                        yield 0;
                    }
                }
            }
            case "reset" -> {
                manager.resetAll(online.getUniqueId());
                manager.ensureLoaded(online, targetUser);
                this.module.sendPrefixed(ProfilesLang.ADMIN_RESET, sender, builder -> builder
                        .with(SLPlaceholders.GENERIC_NAME, online::getName));
                yield 1;
            }
            default -> {
                this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                yield 0;
            }
        };
    }

    private int adminOffline(CommandSender sender, java.util.UUID uuid, String name, String action, String profileQuery) {
        ProfileManager manager = this.module.getManager();
        switch (action) {
            case "list" -> {
                List<PlayerProfile> profiles = manager.getProfileList(uuid);
                if (profiles.isEmpty()) {
                    this.module.sendPrefixed(ProfilesLang.ADMIN_NO_PROFILE_DATA, sender, builder -> builder
                            .with(SLPlaceholders.GENERIC_NAME, () -> name));
                    return 1;
                }
                this.sendAdminList(sender, name, profiles, null);
                return 1;
            }
            case "switch" -> {
                this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                return 0;
            }
            case "reset" -> {
                manager.resetAll(uuid);
                this.module.sendPrefixed(ProfilesLang.ADMIN_RESET, sender, builder -> builder
                        .with(SLPlaceholders.GENERIC_NAME, () -> name));
                return 1;
            }
            case "delete" -> {
                this.module.userManager().loadByNameAsync(name).thenAccept(optional -> {
                    SunUser loaded = optional.orElse(null);
                    String activeId = loaded == null ? null : manager.getActiveId(loaded);
                    ProfileManager.DeleteResult result = manager.deleteById(uuid, activeId, profileQuery);
                    this.module.plugin().runTask(() -> {
                        switch (result) {
                            case OK -> this.module.sendPrefixed(ProfilesLang.DELETED, sender, builder -> builder
                                    .with(SLPlaceholders.GENERIC_NAME, () -> profileQuery));
                            case NOT_FOUND -> this.module.sendPrefixed(ProfilesLang.NOT_FOUND, sender, builder -> builder
                                    .with(SLPlaceholders.GENERIC_NAME, () -> profileQuery));
                            case ACTIVE -> this.module.sendPrefixed(ProfilesLang.DELETE_ACTIVE, sender);
                            default -> this.module.sendPrefixed(ProfilesLang.DELETE_LAST, sender);
                        }
                    });
                }).whenComplete(su.nightexpress.sunlight.utils.Utils::printStacktrace);
                return 1;
            }
            default -> {
                this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                return 0;
            }
        }
    }

    private void sendAdminList(CommandSender sender, String ownerName, List<PlayerProfile> profiles, String activeId) {
        ProfileManager manager = this.module.getManager();
        this.module.sendPrefixed(ProfilesLang.LIST_HEADER, sender, builder -> builder
                .with(SLPlaceholders.GENERIC_CURRENT, () -> String.valueOf(profiles.size()))
                .with(SLPlaceholders.GENERIC_MAX, () -> ownerName));
        for (PlayerProfile profile : profiles) {
            boolean active = profile.getId().equals(activeId);
            this.module.sendPrefixed(ProfilesLang.LIST_ENTRY, sender, builder -> builder
                    .with(SLPlaceholders.GENERIC_NAME, () -> profile.getName())
                    .with(SLPlaceholders.GENERIC_VALUE, () -> profile.getId())
                    .with(SLPlaceholders.GENERIC_STATUS, () -> active ? " §a(active)" : ""));
        }
    }
}
