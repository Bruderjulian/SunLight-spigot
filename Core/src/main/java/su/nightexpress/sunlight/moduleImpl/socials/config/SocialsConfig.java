package su.nightexpress.sunlight.moduleImpl.socials.config;

import su.nightexpress.nightcore.config.ConfigValue;

public class SocialsConfig {

    public static final ConfigValue<Boolean> RELAY_JOIN = ConfigValue.create("Socials.Discord.Relay.Join",
        true,
        "Relay player joins to Discord."
    );

    public static final ConfigValue<Boolean> RELAY_QUIT = ConfigValue.create("Socials.Discord.Relay.Quit",
        true,
        "Relay player quits to Discord."
    );

    public static final ConfigValue<Boolean> RELAY_DEATH = ConfigValue.create("Socials.Discord.Relay.Death",
        true,
        "Relay player deaths to Discord."
    );

    public static final ConfigValue<Boolean> RELAY_ADVANCEMENT = ConfigValue.create("Socials.Discord.Relay.Advancement",
        false,
        "Relay advancement grants to Discord.",
        "Recipe unlocks are always skipped."
    );

    public static final ConfigValue<Boolean> RELAY_CHAT = ConfigValue.create("Socials.Discord.Relay.Chat",
        false,
        "Relay in-game chat to Discord.",
        "Keep disabled if DiscordSRV already relays chat to avoid duplicates."
    );

    public static final ConfigValue<String> CHANNEL_ID = ConfigValue.create("Socials.Discord.Relay.ChannelId",
        "",
        "Discord channel ID for relay messages.",
        "When empty, the DiscordSRV main channel is used."
    );

    public static final ConfigValue<String> FORMAT_JOIN = ConfigValue.create("Socials.Discord.Format.Join",
        "%player% joined the server",
        "Relay message formats. Placeholders: %player%, %message%, %advancement%."
    );

    public static final ConfigValue<String> FORMAT_QUIT = ConfigValue.create("Socials.Discord.Format.Quit",
        "%player% left the server"
    );

    public static final ConfigValue<String> FORMAT_DEATH = ConfigValue.create("Socials.Discord.Format.Death",
        "%player% died"
    );

    public static final ConfigValue<String> FORMAT_ADVANCEMENT = ConfigValue.create("Socials.Discord.Format.Advancement",
        "%player% earned %advancement%"
    );

    public static final ConfigValue<String> FORMAT_CHAT = ConfigValue.create("Socials.Discord.Format.Chat",
        "<%player%> %message%"
    );

    public static final ConfigValue<Boolean> ANNOUNCE_REPORTS = ConfigValue.create("Socials.Discord.AnnounceReports",
        true,
        "When enabled, other modules may forward player reports to Discord via SocialsProvider."
    );

    public static final ConfigValue<Boolean> ANNOUNCE_PUNISHMENTS = ConfigValue.create("Socials.Discord.AnnouncePunishments",
        true,
        "When enabled, other modules may forward punishments to Discord via SocialsProvider."
    );

    public static final ConfigValue<String> FORMAT_REPORT = ConfigValue.create("Socials.Discord.Format.Report",
        "<yellow>New report<reset>: <white>%reporter_name%<gray> reported <white>%target_name%<gray> for <white>%category%<reset> (%report_id%)",
        "Relay message format for player reports forwarded by the Reports module.",
        "Placeholders: %reporter%, %reporter_name%, %target%, %target_name%, %category%,",
        "%details%, %report_id%, %date%."
    );
}
