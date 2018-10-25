package io.github.lunarwatcher.java.haileybot;

import io.github.lunarwatcher.java.haileybot.botmeta.BlacklistStorage;
import io.github.lunarwatcher.java.haileybot.commands.Commands;
import io.github.lunarwatcher.java.haileybot.commands.Moderator;
import io.github.lunarwatcher.java.haileybot.commands.RegexWatcher;
import io.github.lunarwatcher.java.haileybot.commands.RoleAssignmentManager;
import io.github.lunarwatcher.java.haileybot.commands.mod.utils.ModUtils;
import io.github.lunarwatcher.java.haileybot.data.Config;
import io.github.lunarwatcher.java.haileybot.data.Constants;
import io.github.lunarwatcher.java.haileybot.data.Database;
import io.github.lunarwatcher.java.haileybot.mod.ModGuild;
import io.github.lunarwatcher.java.haileybot.utils.Method0;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageEditEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserBanEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserLeaveEvent;
import sx.blah.discord.handle.impl.events.guild.role.RoleDeleteEvent;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.EmbedBuilder;

import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class HaileyBot {
    private static final Logger logger = LoggerFactory.getLogger(HaileyBot.class);
    private static List<Long> botAdmins;

    static {
        botAdmins = new ArrayList<>();

        botAdmins.add(363018555081359360L);
    }

    ScheduledFuture<?> autoSaver;
    private boolean running;
    private IDiscordClient client;
    private Database database;
    private Commands commands;
    private Moderator moderator;
    private RegexWatcher matcher;
    private RoleAssignmentManager assigner;
    private BlacklistStorage blacklistStorage;
    private Config config;
    // TODO replace with multiple threads if the amount of tasks grow
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();


    public HaileyBot() {
        ModUtils.initialize(this);
        CrashHandler.injectBotClass(this);
        try {
            database = new Database(Paths.get("database.json"));
            blacklistStorage = new BlacklistStorage(database);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Properties config = new Properties();
            config.load(new FileInputStream("config.properties"));
            this.config = new Config(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final String token = config.getToken();
        client = new ClientBuilder()
                .withToken(token)
                .withRecommendedShardCount()
                .registerListener(this)
                .login();
        changePresence();
        logger.info("App ID: {}", client.getApplicationClientID());

        logger.info("Initializing systems...");

        autoSaver = executor.scheduleAtFixedRate(this::save, 1, 1, TimeUnit.HOURS);

        registerShutdownHook();
        registerTasks();
    }

    @EventSubscriber
    public void onGuildCreateEvent(GuildCreateEvent event) {
        logger.info("Joined guild: {}. Owner: {}",
                event.getGuild().getName(),
                event.getGuild().getOwner().getName());

        if (blacklistStorage.isBlacklisted(event.getGuild())) {
            logger.warn("Joined blacklisted guild! Leaving...");
            logger.warn("Dumping info. Guild: {} (UID {}), owned by {} (UID {}).",
                    event.getGuild().getName(),
                    event.getGuild().getStringID(),
                    event.getGuild().getOwner().getName(),
                    event.getGuild().getOwner().getStringID());
            event.getGuild().leave();
            return;
        }
        if (matcher != null) {
            /*
             * If the matcher != null, that means we've initialized. It could be any of the other fields initialized
             * in on-ready. It was picked at random, there's no special meaning to it.
             *
             * The presence is updated in onReady to avoid rate limiting, so once it's initialized, any new guilds
             * will update it, instead of spam-updating on boot.
             */
            changePresence();
        }

    }

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        logger.info("Bot running!");
        // Initialize stuff that needs the API here
        matcher = new RegexWatcher(this);
        commands = new Commands(this);
        moderator = new Moderator(this);
        assigner = new RoleAssignmentManager(this);

        changePresence();
    }


    @EventSubscriber
    public void onRoleDeleteEvent(RoleDeleteEvent event) {
        try {
            List<IRole> selfAssignable = assigner.getRolesForGuild(event.getGuild().getLongID());
            if (selfAssignable != null && selfAssignable.stream().anyMatch(r -> r.getLongID() == event.getRole().getLongID())) {
                assigner.removeRole(event.getGuild().getLongID(), event.getRole());
                if (moderator.isGuildEnabled(event.getGuild())) {
                    //noinspection ConstantConditions
                    moderator.getGuild(event.getGuild().getLongID()).audit("A self-assignable role was deleted. Removed from self-assign: " + event.getRole().getName());
                }

            }

            List<IRole> autoAssignable = assigner.getRolesForGuild(event.getGuild().getLongID());
            if (autoAssignable != null && autoAssignable.stream().anyMatch(r -> r.getLongID() == event.getRole().getLongID())) {
                assigner.removeAutoRole(event.getGuild().getLongID(), event.getRole());
                if (moderator.isGuildEnabled(event.getGuild())) {
                    //noinspection ConstantConditions
                    moderator.getGuild(event.getGuild().getLongID()).audit("An auto-assignable role was deleted. Removed from auto-assign: " + event.getRole().getName());
                }

            }
        } catch (Throwable e) {
            CrashHandler.error(e);
            e.printStackTrace();
        }
    }

    @EventSubscriber
    public void onUserJoinEvent(UserJoinEvent event) {
        try {
            moderator.userJoined(event);
        } catch (Throwable e) {
            CrashHandler.error(e);
            e.printStackTrace();
        }
    }

    @EventSubscriber
    public void onUserBanEvent(UserBanEvent event) {
        try {
            moderator.userBanned(event);
        } catch (Throwable e) {
            CrashHandler.error(e);
            e.printStackTrace();
        }
    }

    @EventSubscriber
    public void onUserLeaveEvent(UserLeaveEvent event) {
        if (event.getUser().getLongID() == client.getOurUser().getLongID()) {
            matcher.clearWatchesForGuild(event.getGuild().getLongID());
            return;
        }

        matcher.clearWatchesForUser(event.getUser().getLongID(), event.getGuild().getLongID());
        try {
            moderator.userLeft(event);
        } catch (Throwable e) {
            CrashHandler.error(e);
            e.printStackTrace();
        }
    }


    @EventSubscriber
    public void onMessageEditedEvent(MessageEditEvent event) {
        if (event.getAuthor().getLongID() == client.getOurUser().getLongID())
            return;

        if (matcher == null || commands == null) {
            logger.debug("Mather or commands not initialized yet. {}, {}", matcher, commands);
            return;
        }

        try {
            onMessageReceivedEvent(new MessageReceivedEvent(event.getMessage()));
            moderator.messageEdited(event);

        } catch (Throwable e) {
            CrashHandler.error(e);
            e.printStackTrace();

            if (event.getMessage().getContent().startsWith(Constants.TRIGGER)) {
                event.getChannel().sendMessage(new EmbedBuilder().withColor(Color.RED).withTitle(":warning: Error :warning:")
                        .withDesc("Something bad happened when processing that :c My devs are probably on it already").build());
            }
        }
    }

    @EventSubscriber
    public void onMessageReceivedEvent(MessageReceivedEvent event) {
        if (event.getAuthor().getLongID() == client.getOurUser().getLongID())
            return;

        if (matcher == null || commands == null) {
            logger.debug("Mather or commands not initialized yet. {}, {}", matcher, commands);
            return;
        }

        try {
            matcher.checkMessageForMatch(event.getMessage());

            boolean ctn = moderator.messageReceived(event);

            if (ctn)
                return;

            commands.onCommand(event.getMessage());
        } catch (Throwable e) {
            CrashHandler.error(e);
            e.printStackTrace();

            if (event.getMessage().getContent().startsWith(Constants.TRIGGER)) {
                event.getChannel().sendMessage(new EmbedBuilder().withColor(Color.RED).withTitle(":warning: Error :warning:")
                        .withDesc("Something bad happened when processing that :c My devs are probably on it already").build());
            }
        }
    }

    @EventSubscriber
    public void onMessageDeletedEvent(MessageDeleteEvent event) {
        moderator.messageDeleted(event);

    }


    @EventSubscriber
    public void onDisconnectedEvent(DisconnectedEvent event) {
        save();
    }

    private void changePresence() {
        client.changePresence(StatusType.ONLINE, ActivityType.WATCHING, client.getGuilds().size() + " guilds");
    }

    public void save() {
        if (moderator != null)
            moderator.save();
        if (matcher != null)
            matcher.save();
        if (assigner != null)
            assigner.save();


        database.commit();
    }


    public IUser getBotUser() {
        return client.getOurUser();
    }

    public Database getDatabase() {
        return database;
    }

    public RegexWatcher getMatcher() {
        return matcher;
    }

    public Moderator getModerator() {
        return moderator;
    }

    public Commands getCommands() {
        return commands;
    }

    public List<Long> getBotAdmins() {
        return botAdmins;
    }

    public IDiscordClient getClient() {
        return client;
    }

    public RoleAssignmentManager getAssigner() {
        return assigner;
    }

    public BlacklistStorage getBlacklistStorage() {
        return blacklistStorage;
    }

    public Config getConfig() {
        return config;
    }

    private void registerShutdownHook(){
        Runtime.getRuntime().addShutdownHook(new ControlHook());
    }

    private void registerTasks(){
        executor.scheduleAtFixedRate(this::refreshLogsInGuilds, 30000, 30000, TimeUnit.MILLISECONDS);
    }

    public void refreshLogsInGuilds(){
        moderator.refreshGuildLogs();
    }

    public class ControlHook extends Thread {
        public ControlHook(){
            super("ShutdownHook:HaileyBot.java)");
        }

        public void run(){
            // Necessary call to log out the client. This is a second attempt in case something fails, but to make sure
            // it shuts down properly. This also controls some threads
            if(client.isLoggedIn()){
                client.logout();
            }
        }
    }
}
