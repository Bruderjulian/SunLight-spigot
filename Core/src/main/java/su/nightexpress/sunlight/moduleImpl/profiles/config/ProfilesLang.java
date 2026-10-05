package su.nightexpress.sunlight.moduleImpl.profiles.config;

import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;

import static su.nightexpress.nightcore.util.text.tag.Tags.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;

public class ProfilesLang implements LangContainer {

    public static final TextLocale COMMAND_PROFILE_DESC = LangEntry.builder("Profiles.Command.Profile.Desc")
        .text("Manage your game profiles.");
    public static final TextLocale COMMAND_SWITCH_DESC = LangEntry.builder("Profiles.Command.Switch.Desc")
        .text("Switch to another profile.");
    public static final TextLocale COMMAND_LIST_DESC = LangEntry.builder("Profiles.Command.List.Desc")
        .text("List your profiles.");
    public static final TextLocale COMMAND_CREATE_DESC = LangEntry.builder("Profiles.Command.Create.Desc")
        .text("Create a new profile.");
    public static final TextLocale COMMAND_RENAME_DESC = LangEntry.builder("Profiles.Command.Rename.Desc")
        .text("Rename a profile.");
    public static final TextLocale COMMAND_DELETE_DESC = LangEntry.builder("Profiles.Command.Delete.Desc")
        .text("Delete a profile.");
    public static final TextLocale COMMAND_INFO_DESC = LangEntry.builder("Profiles.Command.Info.Desc")
        .text("Show profile details.");

    public static final MessageLocale LIST_HEADER = LangEntry.builder("Profiles.List.Header").chatMessage(
        GRAY.wrap("Your profiles (" + WHITE.wrap(GENERIC_CURRENT) + "/" + WHITE.wrap(GENERIC_MAX) + "):"));

    public static final MessageLocale LIST_ENTRY = LangEntry.builder("Profiles.List.Entry").chatMessage(
        GRAY.wrap("- " + ORANGE.wrap(GENERIC_NAME) + GRAY.wrap(" (id: " + WHITE.wrap(GENERIC_VALUE) + ")") + GENERIC_STATUS));

    public static final MessageLocale LIST_EMPTY = LangEntry.builder("Profiles.List.Empty").chatMessage(
        GRAY.wrap("You have no profiles yet. Use " + ORANGE.wrap("/profile create <name>") + GRAY.wrap(" to make one.")));

    public static final MessageLocale CREATED = LangEntry.builder("Profiles.Created").chatMessage(
        GRAY.wrap("Profile " + ORANGE.wrap(GENERIC_NAME) + GRAY.wrap(" created. Switch to it with ")
            + ORANGE.wrap("/profile switch " + GENERIC_VALUE) + GRAY.wrap(".")));

    public static final MessageLocale CREATE_LIMIT = LangEntry.builder("Profiles.Error.Limit").chatMessage(
        GRAY.wrap("You already own the maximum of " + ORANGE.wrap(GENERIC_MAX) + GRAY.wrap(" profiles.")));

    public static final MessageLocale CREATE_EXISTS = LangEntry.builder("Profiles.Error.Exists").chatMessage(
        GRAY.wrap("A profile named " + ORANGE.wrap(GENERIC_NAME) + GRAY.wrap(" already exists.")));

    public static final MessageLocale CREATE_INVALID_NAME = LangEntry.builder("Profiles.Error.InvalidName").chatMessage(
        GRAY.wrap("Profile names must be 3-16 characters: letters, numbers and underscores."));

    public static final MessageLocale RENAMED = LangEntry.builder("Profiles.Renamed").chatMessage(
        GRAY.wrap("Profile renamed to " + ORANGE.wrap(GENERIC_NAME) + GRAY.wrap(".")));

    public static final MessageLocale DELETED = LangEntry.builder("Profiles.Deleted").chatMessage(
        GRAY.wrap("Profile " + ORANGE.wrap(GENERIC_NAME) + GRAY.wrap(" deleted.")));

    public static final MessageLocale DELETE_ACTIVE = LangEntry.builder("Profiles.Error.DeleteActive").chatMessage(
        GRAY.wrap("You can not delete the profile you are currently playing on. Switch first."));

    public static final MessageLocale DELETE_LAST = LangEntry.builder("Profiles.Error.DeleteLast").chatMessage(
        GRAY.wrap("You can not delete your last profile."));

    public static final MessageLocale NOT_FOUND = LangEntry.builder("Profiles.Error.NotFound").chatMessage(
        GRAY.wrap("Profile " + ORANGE.wrap(GENERIC_NAME) + GRAY.wrap(" not found. Use ")
            + ORANGE.wrap("/profile list") + GRAY.wrap(".")));

    public static final MessageLocale ALREADY_ACTIVE = LangEntry.builder("Profiles.Error.AlreadyActive").chatMessage(
        GRAY.wrap("You are already playing on " + ORANGE.wrap(GENERIC_NAME) + GRAY.wrap(".")));

    public static final MessageLocale SWITCH_NOTIFY = LangEntry.builder("Profiles.Switch.Notify").chatMessage(
        GRAY.wrap("Switched to profile " + ORANGE.wrap(GENERIC_NAME) + GRAY.wrap(".")));

    public static final MessageLocale SWITCH_COOLDOWN = LangEntry.builder("Profiles.Error.Cooldown").chatMessage(
        GRAY.wrap("You can switch profiles again in " + ORANGE.wrap(GENERIC_TIME) + GRAY.wrap(".")));

    public static final MessageLocale SWITCH_BLOCKED_VANISHED = LangEntry.builder("Profiles.Error.BlockedVanished").chatMessage(
        GRAY.wrap("You can not switch profiles while vanished. Unvanish first."));

    public static final MessageLocale SWITCH_BLOCKED_FROZEN = LangEntry.builder("Profiles.Error.BlockedFrozen").chatMessage(
        GRAY.wrap("You can not switch profiles while frozen."));

    public static final MessageLocale SWITCH_BLOCKED_FLYING = LangEntry.builder("Profiles.Error.BlockedFlying").chatMessage(
        GRAY.wrap("You can not switch profiles while flying mid-air. Land first."));

    public static final MessageLocale SWITCH_BLOCKED_DEAD = LangEntry.builder("Profiles.Error.BlockedDead").chatMessage(
        GRAY.wrap("You can not switch profiles while dead."));

    public static final MessageLocale SWITCH_FAILED = LangEntry.builder("Profiles.Error.Failed").chatMessage(
        GRAY.wrap("Profile switch failed. Your previous state was restored. Contact staff if this repeats."));

    public static final MessageLocale INFO_HEADER = LangEntry.builder("Profiles.Info.Header").chatMessage(
        GRAY.wrap("Profile " + ORANGE.wrap(GENERIC_NAME) + GRAY.wrap(" (id: ") + WHITE.wrap(GENERIC_VALUE) + GRAY.wrap("):")));

    public static final MessageLocale INFO_ACTIVE = LangEntry.builder("Profiles.Info.Active").chatMessage(
        GRAY.wrap("Active: " + WHITE.wrap(GENERIC_STATE)));

    public static final MessageLocale INFO_LAST_PLAYED = LangEntry.builder("Profiles.Info.LastPlayed").chatMessage(
        GRAY.wrap("Last played: " + WHITE.wrap(GENERIC_TIME)));

    public static final MessageLocale RENAME_DISABLED = LangEntry.builder("Profiles.Error.RenameDisabled").chatMessage(
        GRAY.wrap("Renaming profiles is disabled by the server."));

    public static final MessageLocale DELETE_DISABLED = LangEntry.builder("Profiles.Error.DeleteDisabled").chatMessage(
        GRAY.wrap("Deleting profiles is disabled by the server."));
}
