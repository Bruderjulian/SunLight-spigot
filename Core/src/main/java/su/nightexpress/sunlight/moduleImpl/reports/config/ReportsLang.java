package su.nightexpress.sunlight.moduleImpl.reports.config;

import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.EnumLocale;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportFilter;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportStatus;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;
import static su.nightexpress.sunlight.moduleImpl.reports.ReportsPlaceholders.*;

public class ReportsLang implements LangContainer {

    public static final EnumLocale<ReportStatus> STATUS = LangEntry.builder("Reports.Status")
            .enumeration(ReportStatus.class);

    public static final EnumLocale<ReportFilter> FILTER = LangEntry.builder("Reports.Filter")
            .enumeration(ReportFilter.class);

    public static final TextLocale STATE_REWARDED = LangEntry.builder("Reports.State.Rewarded")
            .text(YELLOW.wrap("Reward Paid"));

    public static final TextLocale STATE_NOT_REWARDED = LangEntry.builder("Reports.State.NotRewarded")
            .text(GRAY.wrap("No Reward"));

    public static final TextLocale COMMAND_REPORT_DESC = LangEntry.builder("Command.Report.Desc")
            .text("Report a player for breaking the rules.");

    public static final TextLocale COMMAND_REPORT_STATUS_DESC = LangEntry.builder("Command.ReportStatus.Desc")
            .text("View the status of your own reports.");

    public static final TextLocale COMMAND_ARGUMENT_NAME_PLAYER = LangEntry.builder("Command.Report.Arg.Player")
            .text("player");

    public static final TextLocale COMMAND_ARGUMENT_NAME_CATEGORY = LangEntry.builder("Command.Report.Arg.Category")
            .text("category");

    public static final TextLocale COMMAND_ARGUMENT_NAME_DETAILS = LangEntry.builder("Command.Report.Arg.Details")
            .text("details");

    public static final TextLocale COMMAND_ARGUMENT_NAME_REPORT_ID = LangEntry.builder("Command.Reports.Arg.ReportId")
            .text("report");

    public static final TextLocale COMMAND_ARGUMENT_NAME_NOTE = LangEntry.builder("Command.Reports.Arg.Note")
            .text("note");

    public static final TextLocale COMMAND_ARGUMENT_NAME_FILTER = LangEntry.builder("Command.Reports.Arg.Filter")
            .text("filter");

    public static final TextLocale COMMAND_REPORTS_ROOT_DESC = LangEntry.builder("Command.Reports.Root.Desc")
            .text("Review and manage player reports.");

    public static final TextLocale COMMAND_LIST_DESC = LangEntry.builder("Command.Reports.List.Desc")
            .text("Open the reports menu.");

    public static final TextLocale COMMAND_VIEW_DESC = LangEntry.builder("Command.Reports.View.Desc")
            .text("Open a single report.");

    public static final TextLocale COMMAND_CLAIM_DESC = LangEntry.builder("Command.Reports.Claim.Desc")
            .text("Take responsibility for a report.");

    public static final TextLocale COMMAND_RELEASE_DESC = LangEntry.builder("Command.Reports.Release.Desc")
            .text("Put a claimed report back in the queue.");

    public static final TextLocale COMMAND_RESOLVE_DESC = LangEntry.builder("Command.Reports.Resolve.Desc")
            .text("Conclude a report as justified.");

    public static final TextLocale COMMAND_DENY_DESC = LangEntry.builder("Command.Reports.Deny.Desc")
            .text("Conclude a report as unjustified.");

    public static final TextLocale COMMAND_NOTE_DESC = LangEntry.builder("Command.Reports.Note.Desc")
            .text("Add a note to a report.");

    public static final TextLocale COMMAND_TELEPORT_DESC = LangEntry.builder("Command.Reports.Teleport.Desc")
            .text("Teleport to the reported player.");

    public static final TextLocale COMMAND_DELETE_DESC = LangEntry.builder("Command.Reports.Delete.Desc")
            .text("Delete a report and its notes.");

    public static final MessageLocale ERROR_SELF_REPORT = LangEntry.builder("Reports.Error.SelfReport")
            .chatMessage(SOFT_RED.wrap("You cannot report yourself."));

    public static final MessageLocale ERROR_INVALID_NAME = LangEntry.builder("Reports.Error.InvalidName")
            .chatMessage(SOFT_RED.wrap("That is not a valid player name."));

    public static final MessageLocale ERROR_UNKNOWN_CATEGORY = LangEntry.builder("Reports.Error.UnknownCategory")
            .chatMessage(SOFT_RED.wrap("Unknown category. Try: ") + WHITE.wrap(GENERIC_LIST));

    public static final MessageLocale ERROR_CATEGORY_LOCKED = LangEntry.builder("Reports.Error.CategoryLocked")
            .chatMessage(SOFT_RED.wrap("You may not use that report category."));

    public static final MessageLocale ERROR_DETAILS_REQUIRED = LangEntry.builder("Reports.Error.DetailsRequired")
            .chatMessage(SOFT_RED.wrap("You must describe what happened."));

    public static final MessageLocale ERROR_DETAILS_TOO_SHORT = LangEntry.builder("Reports.Error.DetailsTooShort")
            .chatMessage(SOFT_RED.wrap("Your description is too short. Minimum: ") + WHITE.wrap(GENERIC_MIN)
                    + SOFT_RED.wrap(" characters."));

    public static final MessageLocale ERROR_DETAILS_TOO_LONG = LangEntry.builder("Reports.Error.DetailsTooLong")
            .chatMessage(SOFT_RED.wrap("Your description is too long. Maximum: ") + WHITE.wrap(GENERIC_MAX)
                    + SOFT_RED.wrap(" characters."));

    public static final MessageLocale ERROR_COOLDOWN = LangEntry.builder("Reports.Error.Cooldown")
            .chatMessage(SOFT_RED.wrap("You must wait ") + WHITE.wrap(GENERIC_TIME)
                    + SOFT_RED.wrap(" before reporting again."));

    public static final MessageLocale ERROR_TOO_MANY_OPEN = LangEntry.builder("Reports.Error.TooManyOpen")
            .chatMessage(SOFT_RED.wrap("You already have ") + WHITE.wrap(GENERIC_CURRENT) + SOFT_RED.wrap(" of ")
                    + WHITE.wrap(GENERIC_MAX) + SOFT_RED.wrap(" open reports."));

    public static final MessageLocale ERROR_ALREADY_REPORTED = LangEntry.builder("Reports.Error.AlreadyReported")
            .chatMessage(SOFT_RED.wrap("You already reported ") + WHITE.wrap(GENERIC_TARGET) + SOFT_RED.wrap(" ")
                    + WHITE.wrap(GENERIC_TIME) + SOFT_RED.wrap(" ago."));

    public static final MessageLocale ERROR_EXEMPT = LangEntry.builder("Reports.Error.Exempt")
            .chatMessage(SOFT_RED.wrap("You are exempt from the reports system."));

    public static final MessageLocale ERROR_BLACKLIST_WORLD = LangEntry.builder("Reports.Error.BlacklistWorld")
            .chatMessage(SOFT_RED.wrap("Reporting is disabled in this world."));

    public static final MessageLocale ERROR_DATA_NOT_LOADED = LangEntry.builder("Reports.Error.DataNotLoaded")
            .chatMessage(SOFT_RED.wrap("Reports are still loading, try again in a moment."));

    public static final MessageLocale ERROR_INVALID_REPORT_ID = LangEntry.builder("Reports.Error.InvalidReportId")
            .chatMessage(SOFT_RED.wrap("No such report: ") + WHITE.wrap(GENERIC_VALUE));

    public static final MessageLocale ERROR_ALREADY_CONCLUDED = LangEntry.builder("Reports.Error.AlreadyConcluded")
            .chatMessage(SOFT_RED.wrap("That report is already ") + WHITE.wrap(GENERIC_STATUS) + SOFT_RED.wrap("."));

    public static final MessageLocale ERROR_NOT_CLAIMED = LangEntry.builder("Reports.Error.NotClaimed")
            .chatMessage(SOFT_RED.wrap("Nobody has claimed that report."));

    public static final MessageLocale ERROR_CLAIMED_BY_OTHER = LangEntry.builder("Reports.Error.ClaimedByOther")
            .chatMessage(SOFT_RED.wrap(GENERIC_TARGET) + SOFT_RED.wrap(" is already working that report."));

    public static final MessageLocale ERROR_TARGET_OFFLINE = LangEntry.builder("Reports.Error.TargetOffline")
            .chatMessage(SOFT_RED.wrap(GENERIC_TARGET) + SOFT_RED.wrap(" is not online."));

    public static final MessageLocale ERROR_NOTE_EMPTY = LangEntry.builder("Reports.Error.NoteEmpty")
            .chatMessage(SOFT_RED.wrap("A note cannot be empty."));

    public static final MessageLocale ERROR_NOTE_TOO_LONG = LangEntry.builder("Reports.Error.NoteTooLong")
            .chatMessage(SOFT_RED.wrap("That note is too long. Maximum: ") + WHITE.wrap(GENERIC_MAX)
                    + SOFT_RED.wrap(" characters."));

    public static final MessageLocale ERROR_NO_PERMISSION = LangEntry.builder("Reports.Error.NoPermission")
            .chatMessage(SOFT_RED.wrap("You do not have permission to do that."));

    public static final MessageLocale REPORT_CREATED = LangEntry.builder("Reports.Created")
            .chatMessage(GREEN.wrap("Report submitted. Reference: ") + WHITE.wrap(REPORT_ID));

    public static final MessageLocale REPORT_REJECTED = LangEntry.builder("Reports.Rejected")
            .chatMessage(SOFT_RED.wrap("Your report was rejected by a plugin."));

    public static final MessageLocale REPORT_CLAIMED = LangEntry.builder("Reports.Claimed")
            .chatMessage(GREEN.wrap("You claimed report ") + WHITE.wrap(REPORT_ID));

    public static final MessageLocale REPORT_CLAIM_TAKEN = LangEntry.builder("Reports.ClaimTaken")
            .chatMessage(SOFT_RED.wrap("Too late — ") + WHITE.wrap(GENERIC_TARGET) + SOFT_RED.wrap(" claimed that report first."));

    public static final MessageLocale REPORT_RELEASED = LangEntry.builder("Reports.Released")
            .chatMessage(GREEN.wrap("Report ") + WHITE.wrap(REPORT_ID) + GREEN.wrap(" is back in the queue."));

    public static final MessageLocale REPORT_NOTE_ADDED = LangEntry.builder("Reports.NoteAdded")
            .chatMessage(GREEN.wrap("Note added to report ") + WHITE.wrap(REPORT_ID));

    public static final MessageLocale REPORT_DELETED = LangEntry.builder("Reports.Deleted")
            .chatMessage(GREEN.wrap("Report ") + WHITE.wrap(REPORT_ID) + GREEN.wrap(" deleted."));

    public static final MessageLocale REPORT_CONCLUDED = LangEntry.builder("Reports.Concluded")
            .chatMessage(GREEN.wrap("Report ") + WHITE.wrap(REPORT_ID) + GREEN.wrap(" concluded as ")
                    + WHITE.wrap(GENERIC_STATUS) + GREEN.wrap("."));

    public static final MessageLocale NOTIFY_STAFF = LangEntry.builder("Reports.Notify.Staff")
            .chatMessage(DARK_GRAY.wrap("[") + GOLD.wrap("Reports") + DARK_GRAY.wrap("] ") + WHITE.wrap(GENERIC_TARGET)
                    + DARK_GRAY.wrap(" reported by ") + WHITE.wrap(GENERIC_SOURCE) + DARK_GRAY.wrap(" (")
                    + WHITE.wrap(REPORT_ID) + DARK_GRAY.wrap(") → /reports"));

    public static final MessageLocale NOTIFY_TARGET = LangEntry.builder("Reports.Notify.Target")
            .chatMessage(SOFT_RED.wrap("You have been reported. Staff will review it."));

    public static final MessageLocale NOTIFY_REWARDED = LangEntry.builder("Reports.Notify.Rewarded")
            .chatMessage(GREEN.wrap("Your report was justified. You received a reward!"));

    public static final TextLocale SYSTEM_CLAIMED = LangEntry.builder("Reports.System.Claimed")
            .text("Claimed by %staff%");

    public static final TextLocale SYSTEM_RELEASED = LangEntry.builder("Reports.System.Released")
            .text("Claim released by %staff%");

    public static final TextLocale SYSTEM_RESOLVED = LangEntry.builder("Reports.System.Resolved")
            .text("Concluded as RESOLVED by %staff%");

    public static final TextLocale SYSTEM_DENIED = LangEntry.builder("Reports.System.Denied")
            .text("Concluded as DENIED by %staff%");

    public static final TextLocale SYSTEM_PUNISHED = LangEntry.builder("Reports.System.Punished")
            .text("Closed automatically: the reported player was punished (%type%) by %staff%");

    public static final TextLocale SYSTEM_TELEPORT = LangEntry.builder("Reports.System.Teleport")
            .text("Teleported to the reported player by %staff%");

    public static final TextLocale SYSTEM_DELETED = LangEntry.builder("Reports.System.Deleted")
            .text("Deleted by %staff%");

    public static final TextLocale DIALOG_NOTE_TITLE = LangEntry.builder("Reports.Dialog.Note.Title")
            .text("Report » Note");

    public static final TextLocale DIALOG_NOTE_INPUT = LangEntry.builder("Reports.Dialog.Note.Input")
            .text("note");

    public static final TextLocale DIALOG_OUTCOME_TITLE = LangEntry.builder("Reports.Dialog.Outcome.Title")
            .text("Report » Outcome");

    public static final TextLocale DIALOG_OUTCOME_INPUT = LangEntry.builder("Reports.Dialog.Outcome.Input")
            .text("outcome");

    public static final TextLocale DIALOG_OUTCOME_NOTE_INPUT = LangEntry.builder("Reports.Dialog.Outcome.NoteInput")
            .text("note (optional)");

    public static final TextLocale MENU_LIST_TITLE = LangEntry.builder("Reports.Menu.List.Title")
            .text("[" + GENERIC_MODE + "] Reports");

    public static final TextLocale MENU_VIEW_TITLE = LangEntry.builder("Reports.Menu.View.Title")
            .text("Report: " + REPORT_TARGET);

    public static final TextLocale MENU_EMPTY = LangEntry.builder("Reports.Menu.Empty")
            .text(DARK_GRAY.wrap("Nothing to show here."));

    public static final TextLocale BUTTON_FILTER = LangEntry.builder("Reports.Menu.Button.Filter")
            .text(GOLD.wrap("Filter"));

    public static final TextLocale BUTTON_BACK = LangEntry.builder("Reports.Menu.Button.Back")
            .text(GREEN.wrap("Back"));

    public static final TextLocale BUTTON_CLAIM = LangEntry.builder("Reports.Menu.Button.Claim")
            .text(YELLOW.wrap("Claim"));

    public static final TextLocale BUTTON_CONCLUDE = LangEntry.builder("Reports.Menu.Button.Conclude")
            .text(ORANGE.wrap("Conclude"));

    public static final TextLocale BUTTON_TELEPORT = LangEntry.builder("Reports.Menu.Button.Teleport")
            .text(AQUA.wrap("Teleport to Target"));

    public static final TextLocale BUTTON_NOTE = LangEntry.builder("Reports.Menu.Button.Note")
            .text(LIGHT_PURPLE.wrap("Add Note"));

    public static final TextLocale BUTTON_DELETE = LangEntry.builder("Reports.Menu.Button.Delete")
            .text(RED.wrap("Delete"));

    public static final TextLocale NOTE_LINE = LangEntry.builder("Reports.Menu.NoteLine")
            .text(DARK_GRAY.wrap("• ") + WHITE.wrap(GENERIC_NAME) + DARK_GRAY.wrap(" ") + GRAY.wrap(GENERIC_TIME));

    public static final TextLocale OUTCOME_RESOLVED = LangEntry.builder("Reports.Outcome.Resolved")
            .text(GREEN.wrap("Resolved — justified, reporter may be rewarded"));

    public static final TextLocale OUTCOME_DENIED = LangEntry.builder("Reports.Outcome.Denied")
            .text(RED.wrap("Denied — unjustified, no reward"));
}
