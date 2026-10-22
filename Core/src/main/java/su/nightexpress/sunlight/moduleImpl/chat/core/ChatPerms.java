package su.nightexpress.sunlight.moduleImpl.chat.core;

import org.bukkit.command.CommandSender;

import su.nightexpress.sunlight.utils.PermissionUtils;

public class ChatPerms {

    public static final String MODULE        = "sunlight.chat";
    public static final String COMMAND       = MODULE + ".command";
    public static final String BYPASS        = MODULE + ".bypass";
    public static final String MENTION       = MODULE + ".mention";
    public static final String CHANNEL_LISTEN = MODULE + ".channel.hear";
    public static final String CHANNEL_SPEAK  = MODULE + ".channel.speak";

    public static final String COMMAND_CHANNEL_ROOT  = COMMAND + ".channel.root";
    public static final String COMMAND_CHANNEL_JOIN  = COMMAND + ".channel.join";
    public static final String COMMAND_CHANNEL_LEAVE = COMMAND + ".channel.leave";

    public static final String COMMAND_CLEARCHAT = COMMAND + ".clearchat";
    public static final String COMMAND_ME        = COMMAND + ".me";

    public static final String COMMAND_MENTIONS_TOGGLE        = COMMAND + ".mentions.toggle";
    public static final String COMMAND_MENTIONS_TOGGLE_OTHERS = COMMAND + ".mentions.toggle.others";
    public static final String COMMAND_MENTIONS_ROOT          = COMMAND + ".mentions.root";

    public static final String COMMAND_SPY_ROOT         = COMMAND + ".spy.root";
    public static final String COMMAND_SPY_LOGGER       = COMMAND + ".spy.logger";
    public static final String COMMAND_SPY_CHAT           = COMMAND + ".spy.chat";
    public static final String COMMAND_SPY_CHAT_OTHERS    = COMMAND + ".spy.chat.others";
    public static final String COMMAND_SPY_COMMAND        = COMMAND + ".spy.command";
    public static final String COMMAND_SPY_COMMAND_OTHERS = COMMAND + ".spy.command.others";
    public static final String COMMAND_SPY_SOCIAL         = COMMAND + ".spy.social";
    public static final String COMMAND_SPY_SOCIAL_OTHERS  = COMMAND + ".spy.social.others";

    public static final String COMMAND_CONVERSATIONS_ROOT          = COMMAND + ".conversations.root";
    public static final String COMMAND_CONVERSATIONS_TOGGLE        = COMMAND + ".conversations.toggle";
    public static final String COMMAND_CONVERSATIONS_TOGGLE_OTHERS = COMMAND + ".conversations.toggle.others";
    public static final String COMMAND_REPLY                       = COMMAND + ".conversations.reply";
    public static final String COMMAND_TELL                        = COMMAND + ".conversations.send";

    public static final String COMMAND_MAIL_ROOT  = COMMAND + ".mail.root";
    public static final String COMMAND_MAIL_SEND  = COMMAND + ".mail.send";
    public static final String COMMAND_MAIL_READ  = COMMAND + ".mail.read";
    public static final String COMMAND_MAIL_CLEAR = COMMAND + ".mail.clear";

    public static final String BYPASS_CONVERSATIONS_DISABLED = BYPASS + ".conversations.disabled";
    public static final String BYPASS_MENTION_COOLDOWN       = BYPASS + ".mention.cooldown";
    public static final String BYPASS_MENTION_AMOUNT         = BYPASS + ".mention.amount";
    public static final String BYPASS_CHANNEL_COOLDOWN       = BYPASS + ".channel.cooldown";
    public static final String BYPASS_ANTI_CAPS              = BYPASS + ".moderation.anticaps";
    public static final String BYPASS_ANTI_FLOOD             = BYPASS + ".moderation.antiflood";
    public static final String BYPASS_PROFANITY_FILTER       = BYPASS + ".moderation.profanity.filter";
    public static final String BYPASS_SPY_MONITOR            = BYPASS + ".spy.monitor";

    public static boolean canHearChannel(CommandSender sender, String channelId) {
        return PermissionUtils.hasChildAccess(sender, CHANNEL_LISTEN, channelId);
    }

    public static boolean canSpeakInChannel(CommandSender sender, String channelId) {
        return PermissionUtils.hasChildAccess(sender, CHANNEL_SPEAK, channelId);
    }

    private ChatPerms() {
    }
}
