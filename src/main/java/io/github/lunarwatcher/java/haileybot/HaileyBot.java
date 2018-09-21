package io.github.lunarwatcher.java.haileybot;

import io.github.lunarwatcher.java.haileybot.botmeta.BlacklistStorage;
import io.github.lunarwatcher.java.haileybot.commands.Commands;
import io.github.lunarwatcher.java.haileybot.commands.Moderator;
import io.github.lunarwatcher.java.haileybot.commands.RegexWatcher;
import io.github.lunarwatcher.java.haileybot.commands.RoleAssignmentManager;
import io.github.lunarwatcher.java.haileybot.commands.mod.utils.ModUtils;
import io.github.lunarwatcher.java.haileybot.data.Config;
import io.github.lunarwatcher.java.haileybot.data.Database;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class HaileyBot {
    private static final Logger logger = LoggerFactory.getLogger(HaileyBot.class);
    private boolean running;
    private IDiscordClient client;
    private Database database;

    private Commands commands;
    private Moderator moderator;
    private RegexWatcher matcher;
    private RoleAssignmentManager assigner;
    private BlacklistStorage blacklistStorage;

    private static List<Long> botAdmins;

    private Config config;

    static {
        botAdmins = new ArrayList<>();

        botAdmins.add(363018555081359360L);
    }


    public HaileyBot() {
        ModUtils.initialize(this);
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


    }

    @EventSubscriber
    public void onGuildCreateEvent(GuildCreateEvent event){
        logger.info("Joined guild: {}. Owner: {}", event.getGuild().getName(), event.getGuild().getOwner().getName());

        if(blacklistStorage.isBlacklisted(event.getGuild())){
            logger.warn("Joined blacklisted guild! Leaving...");
            event.getGuild().leave();
        }

        changePresence();

    }

    @EventSubscriber
    public void onReady(ReadyEvent event){
        logger.info("Bot running!");
        // Initialize stuff that needs the API here
        matcher = new RegexWatcher(this);
        commands = new Commands(this);
        moderator = new Moderator(this);
        assigner = new RoleAssignmentManager(this);
    }


    @EventSubscriber
    public void onRoleDeleteEvent(RoleDeleteEvent event){
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
        }catch(Throwable e){
            CrashHandler.error(e);
            e.printStackTrace();
        }
    }

    @EventSubscriber
    public void onUserJoinEvent(UserJoinEvent event){
        try{
            moderator.userJoined(event);
        }catch(Throwable e){
            CrashHandler.error(e);
            e.printStackTrace();
        }
    }

    @EventSubscriber
    public void onUserBanEvent(UserBanEvent event){
        try{
            moderator.userBanned(event);
        }catch(Throwable e){
            CrashHandler.error(e);
            e.printStackTrace();
        }
    }

    @EventSubscriber
    public void onUserLeaveEvent(UserLeaveEvent event){
        if(event.getUser().getLongID() == client.getOurUser().getLongID())
            return;
        try {
            moderator.userLeft(event);
        }catch(Throwable e){
            CrashHandler.error(e);
            e.printStackTrace();
        }
    }


    @EventSubscriber
    public void onMessageEditedEvent(MessageEditEvent event){
        if(event.getAuthor().getLongID() == client.getOurUser().getLongID())
            return;

        try {
            onMessageReceivedEvent(new MessageReceivedEvent(event.getMessage()));
            moderator.messageEdited(event);

        }catch(Throwable e){
            CrashHandler.error(e);
            e.printStackTrace();
        }
    }

    @EventSubscriber
    public void onMessageReceivedEvent(MessageReceivedEvent event){
        if(event.getAuthor().getLongID() == client.getOurUser().getLongID())
            return;

        try {
            matcher.checkMessageForMatch(event.getMessage());

            boolean ctn = moderator.messageReceived(event);

            if (ctn)
                return;
            
            commands.onCommand(event.getMessage());
        }catch(Throwable e){
            CrashHandler.error(e);
            e.printStackTrace();
        }
    }

    @EventSubscriber
    public void onMessageDeletedEvent(MessageDeleteEvent event){
        moderator.messageDeleted(event);

    }


    @EventSubscriber
    public void onDisconnectedEvent(DisconnectedEvent event){
        save();
    }

    private void changePresence(){
        client.changePresence(StatusType.ONLINE, ActivityType.WATCHING, client.getGuilds().size() + " guilds");
    }

    public void save(){
        moderator.save();
        matcher.save();
        assigner.save();

        database.commit();
    }


    public IUser getBotUser(){
        return client.getOurUser();
    }

    public Database getDatabase(){
        return database;
    }

    public RegexWatcher getMatcher(){
        return matcher;
    }

    public Moderator getModerator(){
        return moderator;
    }

    public Commands getCommands(){
        return commands;
    }

    public List<Long> getBotAdmins(){
        return botAdmins;
    }

    public IDiscordClient getClient() {
        return client;
    }

    public RoleAssignmentManager getAssigner(){
        return assigner;
    }

    public BlacklistStorage getBlacklistStorage(){
        return blacklistStorage;
    }

    public Config getConfig(){
        return config;
    }
}
