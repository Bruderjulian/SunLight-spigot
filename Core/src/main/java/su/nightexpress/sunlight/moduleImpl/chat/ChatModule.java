package su.nightexpress.sunlight.moduleImpl.chat;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import su.nightexpress.nightcore.bridge.chat.UniversalChatEvent;
import su.nightexpress.nightcore.bridge.chat.UniversalChatEventHandler;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.nightcore.util.FileUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.text.night.NightMessage;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.hook.HookId;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleContext;
import su.nightexpress.sunlight.moduleImpl.chat.cache.UserChatCache;
import su.nightexpress.sunlight.moduleImpl.chat.channel.ChannelRepository;
import su.nightexpress.sunlight.moduleImpl.chat.channel.ChannelSchema;
import su.nightexpress.sunlight.moduleImpl.chat.channel.ChatChannel;
import su.nightexpress.sunlight.moduleImpl.chat.command.*;
import su.nightexpress.sunlight.moduleImpl.chat.context.ChatContext;
import su.nightexpress.sunlight.moduleImpl.chat.context.CommandContext;
import su.nightexpress.sunlight.moduleImpl.chat.context.ConversationContext;
import su.nightexpress.sunlight.moduleImpl.chat.context.MessageContext;
import su.nightexpress.sunlight.moduleImpl.chat.core.ChatLang;
import su.nightexpress.sunlight.moduleImpl.chat.core.ChatPerms;
import su.nightexpress.sunlight.moduleImpl.chat.core.ChatSettings;
import su.nightexpress.sunlight.moduleImpl.chat.discord.DiscordHandler;
import su.nightexpress.sunlight.moduleImpl.chat.event.PlayerPrivateMessageEvent;
import su.nightexpress.sunlight.moduleImpl.chat.format.FormatDefinition;
import su.nightexpress.sunlight.moduleImpl.chat.listener.ChatListener;
import su.nightexpress.sunlight.moduleImpl.chat.mail.MailCommandProvider;
import su.nightexpress.sunlight.moduleImpl.chat.mail.MailData;
import su.nightexpress.sunlight.moduleImpl.chat.mail.MailDataManager;
import su.nightexpress.sunlight.moduleImpl.chat.processor.ChatProcessor;
import su.nightexpress.sunlight.moduleImpl.chat.processor.chat.ChannelProcessor;
import su.nightexpress.sunlight.moduleImpl.chat.processor.chat.DiscordProcessor;
import su.nightexpress.sunlight.moduleImpl.chat.processor.chat.FormatProcessor;
import su.nightexpress.sunlight.moduleImpl.chat.processor.chat.MentionProcessor;
import su.nightexpress.sunlight.moduleImpl.chat.processor.command.CommandCooldownProcessor;
import su.nightexpress.sunlight.moduleImpl.chat.processor.conversation.ConversationProcessor;
import su.nightexpress.sunlight.moduleImpl.chat.processor.global.*;
import su.nightexpress.sunlight.moduleImpl.chat.report.ReportHandler;
import su.nightexpress.sunlight.moduleImpl.chat.report.ReportPacketsHandler;
import su.nightexpress.sunlight.moduleImpl.chat.report.ReportProtocolHandler;
import su.nightexpress.sunlight.moduleImpl.chat.rule.WordFilter;
import su.nightexpress.sunlight.moduleImpl.chat.spy.SpyLogger;
import su.nightexpress.sunlight.moduleImpl.chat.spy.SpyType;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserProperty;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;
import su.nightexpress.sunlight.utils.Utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChatModule extends Module {

    private final ChatSettings settings;
    private final ChannelRepository channelRepository;
    private final UniversalChatEventHandler chatEventHandler;

    private Pattern mentionsPattern;
    private WordFilter wordFilter;
    private SpyLogger spyLogger;
    private ReportHandler reportHandler;
    private DiscordHandler discordHandler;
    private MailDataManager mailDataManager;

    public ChatModule(ModuleContext context) {
        super(context);
        this.settings = new ChatSettings();
        this.channelRepository = new ChannelRepository();
        this.chatEventHandler = this::handleChatMessage;
    }

    @Override
    protected void loadModule(FileConfig config) {
        this.plugin.injectLang(ChatLang.class);
        this.settings.load(config);

        this.loadMentions();
        this.loadConversations();
        this.loadMail();
        this.loadChannels();
        this.loadWordFilter();
        this.loadSpy();
        this.loadReportHandler();
        this.loadDiscordHook();

        this.plugin.addChatHandler(this.settings.getChatEventPriority(), this.chatEventHandler);

        this.plugin.getServer().getOnlinePlayers().forEach(this::autoJoinChannels);

        this.addListener(new ChatListener(this.plugin, this));
    }

    @Override
    protected void unloadModule() {
        this.plugin.removeChatHandler(this.chatEventHandler);

        this.channelRepository.clear();
        this.wordFilter = null;

        if (this.spyLogger != null) {
            this.spyLogger.write();
            this.spyLogger.shutdown();
            this.spyLogger = null;
        }

        if (this.discordHandler != null) {
            this.discordHandler.shutdown();
            this.discordHandler = null;
        }

        if (this.reportHandler != null) {
            this.reportHandler.unload();
            this.reportHandler = null;
        }

        this.mailDataManager = null;
    }

    @Override
    protected void registerPermissions(PermissionTree root) {
        // Attach channel-specific permissions.
        this.channelRepository.getChannels().forEach(channel -> {
            ChatPerms.CHANNEL_LISTEN.permission(channel.getId());
            ChatPerms.CHANNEL_SPEAK.permission(channel.getId());
        });

        root.merge(ChatPerms.ROOT);
    }

    @Override
    protected void registerCommands() {
        this.commandRegistry.addProvider("chat-clearchat", new ClearChatCommandProvider(this.plugin, this), this);

        if (this.settings.isChannelsEnabled()) {
            this.commandRegistry.addProvider("chat-channel", new ChannelCommandsProvider(this.plugin, this), this);
        }

        if (this.settings.isConversationsEnabled()) {
            this.commandRegistry.addProvider("chat-conversations",
                    new ConversationCommandProvider(this.plugin, this, this.userManager), this);
        }

        if (this.settings.isMailEnabled()) {
            this.commandRegistry.addProvider("chat-mail",
                    new MailCommandProvider(this.plugin, this, this.userManager), this);
        }

        if (this.settings.isMentionsEnabled()) {
            this.commandRegistry.addProvider("chat-mentions",
                    new MentionsCommandProvider(this.plugin, this, this.userManager), this);
        }

        if (this.settings.isRoleplayCommandEnabled()) {
            this.commandRegistry.addProvider("chat-roleplay", new RoleplayCommands(this.plugin, this), this);
        }

        if (this.settings.isSpyEnabled()) {
            this.commandRegistry.addProvider("chat-spy", new SpyCommandProvider(this.plugin, this, this.userManager),
                    this);
        }
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        registry.register("chat_conversations_state", (player, payload) -> {
            return CoreLang.STATE_ENABLED_DISALBED.get(this.userManager.getOrFetch(player).getPropertyOrDefault(
                    ChatProperties.CONVERSATIONS));
        });

        registry.register("chat_conversations_bool", (player, payload) -> {
            return String.valueOf(this.userManager.getOrFetch(player).getPropertyOrDefault(
                    ChatProperties.CONVERSATIONS));
        });
    }

    private void loadMentions() {
        if (!this.settings.isMentionsEnabled())
            return;

        UserPropertyRegistry.register(ChatProperties.MENTIONS);

        this.mentionsPattern = Pattern.compile(this.settings.getMentionsPattern());
        this.settings.getCustomMentions().forEach((id, groupMention) -> {
            ChatPerms.MENTION.permission(id);
        });
    }

    private void loadConversations() {
        if (!this.settings.isConversationsEnabled())
            return;

        UserPropertyRegistry.register(ChatProperties.CONVERSATIONS);
    }

    private void loadMail() {
        if (!this.settings.isMailEnabled())
            return;

        this.mailDataManager = new MailDataManager(this.dataHandler);
        this.mailDataManager.init(this.settings.getMailTablePrefix());
        this.mailDataManager.purgeOldEntries(TimeUnit.DAYS.toMillis(this.settings.getMailExpiryDays()));

        this.plugin.getServer().getOnlinePlayers().forEach(this::deliverMails);
    }

    private void loadChannels() {
        Path channelsDir = Path.of(this.getSystemPath() + ChatFiles.DIR_CHANNELS);

        if (!this.settings.isChannelsEnabled()) {
            this.loadDefaultChannel(channelsDir);
            return;
        }

        if (!Files.exists(channelsDir)) {
            ChannelSchema.getDefaultChannels().forEach(channel -> this.writeChannel(channelsDir, channel));
        }

        FileUtil.findYamlFiles(channelsDir.toString()).forEach(this::loadChannel);

        String defaultId = this.settings.getChannelDefaultId();
        ChatChannel defChannel = this.channelRepository.getById(defaultId);

        if (defChannel == null) {
            this.error(
                    "Channel '%s', that is set as default one, does not exist. The '%s' one will be used to keep the chat working."
                            .formatted(defaultId, ChatDefaults.DEFAULT_CHANNEL_ID));
            this.loadDefaultChannel(channelsDir);
            return;
        }

        this.channelRepository.setDefaultChannel(defChannel);
    }

    private void writeChannel(Path channelsDir, ChatChannel channel) {
        Path file = Path.of(channelsDir.toString(), FileConfig.withExtension(channel.getId()));
        FileConfig config = FileConfig.load(file);
        config.edit(channel::write);
    }

    private ChatChannel loadChannel(Path channelFile) {
        ChatChannel channel = ChatChannel.fromFile(channelFile);
        this.channelRepository.add(channel);
        return channel;
    }

    private void loadDefaultChannel(Path channelsDir) {
        Path defFile = Path.of(channelsDir.toString(), FileConfig.withExtension(ChatDefaults.DEFAULT_CHANNEL_ID));
        if (!Files.exists(defFile)) {
            this.writeChannel(channelsDir, ChannelSchema.createDefaultChannel());
        }

        ChatChannel channel = this.loadChannel(defFile);

        this.channelRepository.setDefaultChannel(channel);
    }

    private void loadWordFilter() {
        if (!this.settings.getProfanityFilterEnabled())
            return;

        Path rulesPath = Path.of(this.getSystemPath() + ChatFiles.DIR_RULES);
        if (!Files.exists(rulesPath)) {
            try {
                Files.createDirectories(rulesPath);

                Collection<String> defaultRules = ChatDefaults.getDefaultWordFilterRules(Locale.getDefault());
                if (!defaultRules.isEmpty()) {
                    this.writeRules(defaultRules, Path.of(rulesPath.toString(), ChatDefaults.DEFAULT_RULE_FILE_NAME));
                }
            } catch (IOException exception) {
                exception.printStackTrace();
                return;
            }
        }

        Set<String> ruleNames = this.settings.getProfanityFilterRules();
        Set<String> allRules = new HashSet<>();

        FileUtil.findFiles(rulesPath.toString(), file -> ruleNames.contains(file.getFileName().toString())).forEach(
                file -> {
                    allRules.addAll(this.readRules(file));
                });

        this.wordFilter = new WordFilter(allRules);
    }

    private void writeRules(Collection<String> rules, Path file) {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (String rule : rules) {
                writer.append(rule);
                writer.newLine();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private Collection<String> readRules(Path file) {
        Set<String> rules = new HashSet<>();

        try (Stream<String> stream = Files.lines(file)) {
            stream.filter(Predicate.not(String::isBlank)).forEach(rules::add);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return rules;
    }

    private void loadSpy() {
        if (!this.settings.isSpyEnabled())
            return;

        for (SpyType spyType : SpyType.values()) {
            UserPropertyRegistry.register(ChatProperties.getSpyInfoProperty(spyType));
            UserPropertyRegistry.register(ChatProperties.getSpyLogProperty(spyType));
        }

        try {
            this.spyLogger = new SpyLogger(this.plugin, Path.of(this.getSystemPath(), ChatFiles.FILE_SPY_LOG));
            this.addAsyncTask(this.spyLogger::write, 60);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void loadReportHandler() {
        if (this.settings.getReportsDisable()) {
            if (Utils.isInstalled(HookId.PACKET_EVENTS)) {
                this.reportHandler = new ReportPacketsHandler();
            } else if (Utils.isInstalled(HookId.PROTOCOL_LIB)) {
                this.reportHandler = new ReportProtocolHandler(this.plugin);
            }

            if (this.reportHandler != null) {
                this.reportHandler.load();
            }
        }
    }

    private void loadDiscordHook() {
        if (this.settings.isDiscordHookEnabled() && HookId.hasDiscordSRV()) {
            this.discordHandler = new DiscordHandler(this.plugin, this);
            this.discordHandler.setup();
        }
    }

    public ChatSettings getSettings() {
        return this.settings;
    }

    public UserChatCache getChatCache(Player player) {
        return this.userManager.getOrFetch(player).getCacheOrCreate(UserChatCache.class, UserChatCache::new);
    }

    public ChannelRepository getChannelRepository() {
        return this.channelRepository;
    }

    public DiscordHandler getDiscordHandler() {
        return this.discordHandler;
    }

    public Set<ChatChannel> getChannelsAllowedToListen(Player player) {
        return this.channelRepository.getChannels().stream().filter(channel -> channel.canListenHere(player)).collect(
                Collectors.toSet());
    }

    public String getEffectiveChatFormat(Player player) {
        return this.settings.getFormatDefinitions()
                .values()
                .stream()
                .filter(container -> container.isApplicable(player))
                .max(Comparator.comparingInt(FormatDefinition::getPriority))
                .map(FormatDefinition::getFormat)
                .orElse(this.settings.getFormatFallback());
    }

    public ChatChannel getEffectiveChannel(Player player, Character prefix) {
        if (prefix != null) {
            ChatChannel byPrefix = this.channelRepository.getByPrefix(prefix);
            if (byPrefix != null && byPrefix.canSpeakHere(player)) {
                return byPrefix;
            }
        }

        return this.channelRepository.getDefaultChannel();
    }

    public boolean joinChannel(Player player, ChatChannel channel) {
        return this.joinChannel(player, channel, false);
    }

    public boolean joinChannel(Player player, ChatChannel channel, boolean isSilent) {
        if (!channel.canListenOrSpeakHere(player)) {
            if (!isSilent) {
                this.sendPrefixed(ChatLang.CHANNEL_JOIN_ERROR_NO_PERMISSION, player, builder -> builder.with(channel
                        .placeholders()));
            }
            return false;
        }

        if (channel.addPlayer(player)) {
            if (!isSilent) {
                this.sendPrefixed(ChatLang.CHANNEL_JOIN_SUCCESS, player, builder -> builder.with(channel
                        .placeholders()));
            }
            return true;
        }

        if (!isSilent) {
            this.sendPrefixed(ChatLang.CHANNEL_JOIN_ERROR_ALREADY_IN, player, builder -> builder.with(channel
                    .placeholders()));
        }

        return false;
    }

    public boolean leaveChannel(Player player, ChatChannel channel) {
        if (channel.removePlayer(player)) {
            this.sendPrefixed(ChatLang.CHANNEL_LEAVE_SUCCESS, player, builder -> builder.with(channel.placeholders()));
            return true;
        }

        this.sendPrefixed(ChatLang.CHANNEL_LEAVE_ERROR_NOT_IN, player, builder -> builder.with(channel.placeholders()));
        return false;
    }

    public void autoJoinChannels(Player player) {
        this.getChannelsAllowedToListen(player).stream().filter(channel -> channel.getAccessibility().autoJoin())
                .forEach(channel -> {
                    this.joinChannel(player, channel, true);
                });
    }

    public void removeFromAllChannels(Player player) {
        this.channelRepository.getChannels().forEach(channel -> channel.removePlayer(player));
    }

    public Set<Player> getSpies(SpyType type) {
        UserProperty<Boolean> property = ChatProperties.getSpyInfoProperty(type);

        return this.plugin.getServer().getOnlinePlayers().stream()
                .filter(player -> this.userManager.getOrFetch(player).getPropertyOrDefault(property))
                .collect(Collectors.toSet());
    }

    public void sendSpyInfo(Player player, String message, String format,
            SpyType spyType) {
        PlaceholderContext context = PlaceholderContext.builder()
                .with(CommonPlaceholders.PLAYER.resolver(player))
                .with(SLPlaceholders.GENERIC_MESSAGE, () -> message)
                .build();

        String formatted = context.apply(format);

        this.getSpies(spyType).forEach(spy -> Players.sendMessage(spy, formatted));

        SunUser user = this.userManager.getOrFetch(player);
        if (this.spyLogger != null && user.getPropertyOrDefault(ChatProperties.getSpyLogProperty(spyType))) {
            this.spyLogger.addEntry(formatted);
        }
    }

    public void handleChatMessage(UniversalChatEvent event) {
        if (event.isCancelled())
            return;

        Player player = event.getPlayer();
        String originalMessage = event.message();

        UserChatCache data = this.getChatCache(player);
        String format = this.getEffectiveChatFormat(player);
        ChatChannel channel = this.getEffectiveChannel(player, originalMessage.charAt(0));

        MessageContext context = new MessageContext(player, data, originalMessage, format, channel, event.viewers());
        List<ChatProcessor<? super MessageContext>> processors = new ArrayList<>();

        processors.add(new ColorProcessor());

        processors.add(new ChannelProcessor(this.plugin)); // Check channel cooldown, remove channel prefix from
                                                           // message.

        if (this.settings.isAntiFloodEnabled() && !player.hasPermission(ChatPerms.BYPASS_ANTI_FLOOD)) {
            processors.add(new AntiFloodProcessor()); // Check message similarity only after all modifications are done.
        }

        if (this.settings.isAntiCapsEnabled() && !player.hasPermission(ChatPerms.BYPASS_ANTI_CAPS)) {
            processors.add(new AntiCapsProcessor()); // Check CAPS usage and adjust to lower case if needed.
        }

        if (this.settings.getProfanityFilterEnabled() && !player.hasPermission(ChatPerms.BYPASS_PROFANITY_FILTER)) {
            if (this.wordFilter != null) {
                processors.add(new FilterProcessor(this.wordFilter)); // Check custom regex rules and adjust/cancel if
                                                                      // needed.
            }
        }

        processors.add(new FormatProcessor()); // Replace format placeholders in postProcess

        if (this.settings.isItemShowEnabled()) {
            processors.add(new ItemDisplayProcessor()); // Inject item display in postProcess in prepared format.
        }

        if (this.settings.isMentionsEnabled()) {
            processors.add(new MentionProcessor(this.mentionsPattern, this.userManager)); // Inject mentions in
                                                                                          // postProcess in prepared
                                                                                          // format.
        }

        if (this.settings.isSpyEnabled() && !player.hasPermission(ChatPerms.BYPASS_SPY_MONITOR)) {
            processors.add(new SpyProcessor());
        }

        if (this.discordHandler != null) {
            processors.add(new DiscordProcessor(this.discordHandler));
        }

        if (!this.process(processors, context)) {
            event.setCancelled(true);
            return;
        }

        event.editViewers(viewers -> {
            viewers.clear();
            viewers.addAll(context.getViewers());
        });
        event.message(NightMessage.parse(context.getMessage()));
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            return NightMessage.parse(context.getFormat());
        });
    }

    public void handleCommandEvent(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UserChatCache cache = this.getChatCache(player);
        String originalMessage = event.getMessage();

        CommandContext context = new CommandContext(player, cache, originalMessage);
        List<ChatProcessor<? super CommandContext>> processors = new ArrayList<>();

        String commandName = context.getCommandName();

        if (this.settings.isAntiFloodEnabled() && !this.settings.isAntiFloodWhitelistedCommand(commandName) && !player
                .hasPermission(ChatPerms.BYPASS_ANTI_FLOOD)) {
            processors.add(new CommandCooldownProcessor()); // Check general commands cooldown.
            processors.add(new AntiFloodProcessor()); // Check message similarity only after all modifications are done.
        }

        if (this.settings.isAntiCapsEnabled() && this.settings.isAntiCapsBlacklistedCommand(commandName) && !player
                .hasPermission(ChatPerms.BYPASS_ANTI_CAPS)) {
            processors.add(new AntiCapsProcessor()); // Check CAPS usage and adjust to lower case if needed.
        }

        if (this.settings.getProfanityFilterEnabled() && this.settings.isProfanityFilterAffectedCommand(
                commandName) && !player.hasPermission(ChatPerms.BYPASS_PROFANITY_FILTER)) {
            if (this.wordFilter != null) {
                processors.add(new FilterProcessor(this.wordFilter)); // Check custom regex rules and adjust/cancel if
                                                                      // needed.
            }
        }

        if (this.settings.isSpyEnabled() && !player.hasPermission(ChatPerms.BYPASS_SPY_MONITOR)) {
            processors.add(new SpyProcessor());
        }

        if (!this.process(processors, context)) {
            event.setCancelled(true);
            return;
        }

        event.setMessage(context.getMessage());
    }

    public MailDataManager getMailDataManager() {
        return this.mailDataManager;
    }

    public void sendMail(Player sender, UserInfo recipient, String message) {
        if (this.mailDataManager == null)
            return;

        Player online = Utils.getPlayer(recipient.id());
        if (online != null) {
            this.sendPrivateMessage(sender, online, message);
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            List<MailData> inbox = this.mailDataManager.getMails(recipient.id());
            int max = this.settings.getMailMaxInbox();
            if (max > 0 && inbox.size() >= max)
                return false;

            this.mailDataManager.insertMail(new MailData(UUID.randomUUID(), sender.getUniqueId(), sender.getName(),
                    recipient.id(), message, System.currentTimeMillis()));
            return true;
        }).thenAcceptAsync(stored -> {
            if (stored) {
                this.sendPrefixed(ChatLang.MAIL_SEND_SUCCESS, sender,
                        builder -> builder.with(SLPlaceholders.GENERIC_NAME, recipient::name));
            } else {
                this.sendPrefixed(ChatLang.MAIL_SEND_ERROR_FULL, sender,
                        builder -> builder.with(SLPlaceholders.GENERIC_NAME, recipient::name));
            }
        }, this.plugin::runTask).whenComplete(Utils::printStacktrace);
    }

    public void readMails(Player player) {
        if (this.mailDataManager == null)
            return;

        CompletableFuture.supplyAsync(() -> this.mailDataManager.getMails(player.getUniqueId()))
                .thenAcceptAsync(mails -> {
                    if (mails.isEmpty()) {
                        this.sendPrefixed(ChatLang.MAIL_READ_EMPTY, player);
                        return;
                    }
                    this.printMails(player, mails);
                    this.mailDataManager.deleteMails(player.getUniqueId());
                }, this.plugin::runTask).whenComplete(Utils::printStacktrace);
    }

    public void clearMails(Player player) {
        if (this.mailDataManager == null)
            return;

        CompletableFuture.runAsync(() -> this.mailDataManager.deleteMails(player.getUniqueId()))
                .thenRunAsync(() -> this.sendPrefixed(ChatLang.MAIL_CLEAR_DONE, player), this.plugin::runTask)
                .whenComplete(Utils::printStacktrace);
    }

    public void deliverMails(Player player) {
        if (this.mailDataManager == null)
            return;

        CompletableFuture.supplyAsync(() -> this.mailDataManager.getMails(player.getUniqueId()))
                .thenAcceptAsync(mails -> {
                    if (mails.isEmpty())
                        return;

                    this.printMails(player, mails);
                    this.sendPrefixed(ChatLang.MAIL_NOTIFY, player,
                            builder -> builder.with(SLPlaceholders.GENERIC_AMOUNT, () -> String.valueOf(mails.size())));
                    this.mailDataManager.deleteMails(player.getUniqueId());
                }, this.plugin::runTask).whenComplete(Utils::printStacktrace);
    }

    private void printMails(Player player, List<MailData> mails) {
        String format = this.settings.getMailFormat();
        mails.forEach(mail -> {
            String text = PlaceholderContext.builder()
                    .with(CommonPlaceholders.PLAYER_NAME, mail::getSenderName)
                    .with(CommonPlaceholders.PLAYER_DISPLAY_NAME, mail::getSenderName)
                    .with(SLPlaceholders.GENERIC_MESSAGE, mail::getMessage)
                    .build().apply(format);
            Players.sendMessage(player, text);
        });
    }

    public void sendChannelCooldownNotice(Player player, ChatChannel channel, String remaining, int totalSeconds) {
        String text = PlaceholderContext.builder()
                .with(SLPlaceholders.GENERIC_TIME, () -> remaining)
                .with(SLPlaceholders.GENERIC_COOLDOWN, () -> String.valueOf(totalSeconds))
                .build().apply(channel.getAccessibility().cooldownMessage());

        Players.sendMessage(player, this.definition.prefix() + text);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1F, 1F);
    }

    public boolean sendPrivateMessage(Player player, Player target, String message) {
        if (player == target) {
            this.sendPrefixed(ChatLang.CONVERSATIONS_SEND_YOURSELF, player);
            return false;
        }

        SunUser targetUser = this.userManager.getOrFetch(target);
        if (!targetUser.getPropertyOrDefault(ChatProperties.CONVERSATIONS) && !player.hasPermission(
                ChatPerms.BYPASS_CONVERSATIONS_DISABLED)) {
            this.sendPrefixed(ChatLang.CONVERSATIONS_SEND_DENIED, player, replacer -> replacer.with(
                    CommonPlaceholders.PLAYER.resolver(target)));
            return false;
        }

        PlayerPrivateMessageEvent event = new PlayerPrivateMessageEvent(player, target, message);
        this.plugin.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return false;

        if (!this.handlePrivateMessage(event)) {
            return false;
        }

        if (this.plugin.afkProvider().map(afkProvider -> afkProvider.isAfk(target)).orElse(false)) {
            this.sendPrefixed(ChatLang.CONVERSATIONS_TARGET_AFK, player, builder -> builder
                    .with(CommonPlaceholders.PLAYER.resolver(target)));
        }

        return true;
    }

    public boolean handlePrivateMessage(PlayerPrivateMessageEvent event) {
        Player player = event.getSender();
        Player target = event.getTarget();
        String originalMessage = event.getMessage();
        String proxyFormat = this.settings.getConversationProxyFormat();

        UserChatCache cache = this.getChatCache(player);
        ConversationContext context = new ConversationContext(player, cache, originalMessage, proxyFormat, target);

        List<ChatProcessor<? super ConversationContext>> processors = new ArrayList<>();

        processors.add(new ColorProcessor());

        if (this.settings.isAntiFloodEnabled() && !player.hasPermission(ChatPerms.BYPASS_ANTI_FLOOD)) {
            processors.add(new AntiFloodProcessor()); // Check message similarity only after all modifications are done.
        }

        if (this.settings.isAntiCapsEnabled() && !player.hasPermission(ChatPerms.BYPASS_ANTI_CAPS)) {
            processors.add(new AntiCapsProcessor()); // Check CAPS usage and adjust to lower case if needed.
        }

        if (this.settings.getProfanityFilterEnabled() && !player.hasPermission(ChatPerms.BYPASS_PROFANITY_FILTER)) {
            if (this.wordFilter != null) {
                processors.add(new FilterProcessor(this.wordFilter)); // Check custom regex rules and adjust/cancel if
                                                                      // needed.
            }
        }

        if (this.settings.isItemShowEnabled()) {
            processors.add(new ItemDisplayProcessor()); // Inject item display in postProcess in prepared format.
        }

        if (this.settings.isSpyEnabled() && !player.hasPermission(ChatPerms.BYPASS_SPY_MONITOR)) {
            processors.add(new SpyProcessor());
        }

        processors.add(new ConversationProcessor()); // Format and send messages.

        return this.process(processors, context);
    }

    private <T extends ChatContext> boolean process(List<ChatProcessor<? super T>> processors,
            T context) {
        for (var processor : processors) {
            processor.preProcess(this, context);

            if (context.isCancelled()) {
                return false;
            }
        }

        processors.forEach(messageProcessor -> messageProcessor.postProcess(this, context));
        return true;
    }
}
