
/*
 * MIT License
 *
 * Copyright (c) 2018 Olivia Zoe
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

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
import io.github.lunarwatcher.java.haileybot.status.PresenceManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.SelfUser;
import net.dv8tion.jda.core.events.DisconnectEvent;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildBanEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;
import net.dv8tion.jda.core.events.role.RoleDeleteEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
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
public class HaileyBot implements EventListener {
    private static final Logger logger = LoggerFactory.getLogger(HaileyBot.class);
    private static final long PRESENCE_UPDATE_TIME = 60 * 30 * 1000; // 30 minutes
    private static List<Long> botAdmins;


    static {
        botAdmins = new ArrayList<>();

        botAdmins.add(363018555081359360L);

    }

    ScheduledFuture<?> autoSaver;
    private JDA client;
    private Database database;
    private Commands commands;
    private Moderator moderator;
    private RegexWatcher matcher;
    private RoleAssignmentManager assigner;
    private BlacklistStorage blacklistStorage;
    private Config config;
    private PresenceManager presenceManager;

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
        try {
            client = new JDABuilder(token)
                    .addEventListener(this)
                    .build();
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }


    }

    public void onReady(ReadyEvent event) {
        logger.info("Bot running!");
        // Initialize stuff that needs the API here
        matcher = new RegexWatcher(this);
        commands = new Commands(this);
        moderator = new Moderator(this);
        assigner = new RoleAssignmentManager(this);

        presenceManager = new PresenceManager(this);
        presenceManager.onReady();

        logger.info("Bot ID: {}", client.getSelfUser().getIdLong());

        logger.info("Initializing systems...");

        autoSaver = executor.scheduleAtFixedRate(this::save, 1, 1, TimeUnit.HOURS);

        registerShutdownHook();
        registerTasks();

        for(Guild guild : client.getGuilds()){
            handleGuild(guild);
        }

        moderator.withJoinedGuilds(client.getGuilds());
    }


    public void onGuildCreateEvent(GuildJoinEvent event) {
        handleGuild(event.getGuild());

        presenceManager.refresh();
    }

    public void onGuildLeaveEvent(GuildLeaveEvent event) {
        logger.info("I've left a guild. Nuking config...");
        matcher.clearWatchesForGuild(event.getGuild().getIdLong());
        moderator.removeGuild(event.getGuild());
        presenceManager.refresh();
    }

    public void onRoleDeleteEvent(RoleDeleteEvent event) {
        try {
            List<Long> selfAssignable = assigner.getRolesForGuild(event.getGuild().getIdLong());
            if (selfAssignable != null && selfAssignable.stream().anyMatch(r -> r == event.getRole().getIdLong())) {
                assigner.removeRole(event.getGuild().getIdLong(), event.getRole());
                if (moderator.isGuildEnabled(event.getGuild())) {
                    //noinspection ConstantConditions
                    moderator.getGuild(event.getGuild().getIdLong()).audit("A self-assignable role was deleted. Removed from self-assign: " + event.getRole().getName());
                }

            }

            List<Long> autoAssignable = assigner.getRolesForGuild(event.getGuild().getIdLong());
            if (autoAssignable != null && autoAssignable.stream().anyMatch(r -> r == event.getRole().getIdLong())) {
                assigner.removeAutoRole(event.getGuild().getIdLong(), event.getRole());
                if (moderator.isGuildEnabled(event.getGuild())) {
                    //noinspection ConstantConditions
                    moderator.getGuild(event.getGuild().getIdLong()).audit("An auto-assigned role was deleted. Removed from auto-assign: " + event.getRole().getName());
                }

            }
        } catch (Throwable e) {
            CrashHandler.error(e);
            e.printStackTrace();
        }
    }

    public void onUserJoinEvent(GuildMemberJoinEvent event) {
        try {
            moderator.userJoined(event);
        } catch (Throwable e) {
            CrashHandler.error(e);
            e.printStackTrace();
        }
    }

    public void onUserBanEvent(GuildBanEvent event) {
        try {
            moderator.userBanned(event);
        } catch (Throwable e) {
            CrashHandler.error(e);
            e.printStackTrace();
        }
    }

    public void onUserLeaveEvent(GuildMemberLeaveEvent event) {

        matcher.clearWatchesForUser(event.getUser().getIdLong(), event.getGuild().getIdLong());
        try {
            moderator.userLeft(event);
        } catch (Throwable e) {
            CrashHandler.error(e);
            e.printStackTrace();
        }
    }

    public void onMessageEditedEvent(MessageUpdateEvent event) {
        if (event.getAuthor().getIdLong() == client.getSelfUser().getIdLong())
            return;

        if (matcher == null || commands == null) {
            logger.debug("Matcher or commands not initialized yet. {}, {}", matcher, commands);
            return;
        }

        try {
            onMessageReceivedEvent(new MessageReceivedEvent(event.getJDA(), event.getResponseNumber(), event.getMessage()));
            moderator.messageEdited(event);

        } catch (Throwable e) {
            CrashHandler.error(e);
            e.printStackTrace();

            if (event.getMessage().getContentRaw().startsWith(Constants.TRIGGER)) {
                event.getChannel().sendMessage(new EmbedBuilder().setColor(Color.RED).setTitle(":warning: Error :warning:")
                        .setDescription("Something bad happened when processing that :c My devs are probably on it already").build())
                        .queue();
            }
        }
    }

    public void onMessageReceivedEvent(MessageReceivedEvent event) {
        if (event.getAuthor().getIdLong() == client.getSelfUser().getIdLong())
            return;

        if (matcher == null || commands == null) {
            logger.debug("Matcher or commands not initialized yet. {}, {}", matcher, commands);
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

            if (event.getMessage().getContentRaw().startsWith(Constants.TRIGGER)) {
                event.getChannel().sendMessage(new EmbedBuilder().setColor(Color.RED).setTitle(":warning: Error :warning:")
                        .setDescription("Something bad happened when processing that :c My devs are probably on it already").build())
                        .queue();
            }
        }
    }

    public void onMessageDeletedEvent(MessageDeleteEvent event) {
        moderator.messageDeleted(event);

    }


    public void onDisconnectedEvent(DisconnectEvent event) {
        save();
    }

    public void save() {
        try {
            if (moderator != null)
                moderator.save();
            if (matcher != null)
                matcher.save();
            if (assigner != null)
                assigner.save();


            database.commit();
        }catch(Throwable e){
            CrashHandler.error(e);
        }
    }


    public SelfUser getBotUser() {
        return client.getSelfUser();
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

    public JDA getClient() {
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

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new ControlHook());
    }

    private void registerTasks() {
        executor.scheduleAtFixedRate(this::refreshLogsInGuilds, 30000, 30000, TimeUnit.MILLISECONDS);

    }

    public void refreshLogsInGuilds() {
        moderator.refreshGuildLogs();
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof ReadyEvent) {
            this.onReady((ReadyEvent) event);
        } else if (event instanceof GuildBanEvent) {
            this.onUserBanEvent((GuildBanEvent) event);
        } else if (event instanceof GuildMemberJoinEvent) {
            this.onUserJoinEvent((GuildMemberJoinEvent) event);
        } else if (event instanceof GuildMemberLeaveEvent) {
            this.onUserLeaveEvent((GuildMemberLeaveEvent) event);
        } else if (event instanceof MessageUpdateEvent) {
            this.onMessageEditedEvent((MessageUpdateEvent) event);
        } else if (event instanceof GuildJoinEvent) {
            this.onGuildCreateEvent((GuildJoinEvent) event);
        } else if(event instanceof MessageReceivedEvent){
            this.onMessageReceivedEvent((MessageReceivedEvent) event);
        } else if(event instanceof MessageDeleteEvent){
            this.onMessageDeletedEvent((MessageDeleteEvent) event);
        } else if(event instanceof GuildLeaveEvent){
            this.onGuildLeaveEvent((GuildLeaveEvent) event);
        }
    }

    public void handleGuild(Guild guild){
        logger.info("Joined guild: {}. Owner: {}",
                guild.getName(),
                guild.getOwner().getUser().getName() + "#" + guild.getOwner().getUser().getDiscriminator());

        if (blacklistStorage.isBlacklisted(guild)) {
            logger.warn("Joined blacklisted guild! Leaving...");
            logger.warn("Dumping info. Guild: {} (UID {}), owned by {} (UID {}).",
                    guild.getName(),
                    guild.getId(),
                    guild.getOwner().getUser().getName(),
                    guild.getOwner().getUser().getId());
            guild.leave()
                    .queue(null, CrashHandler::error);

        }
    }

    public class ControlHook extends Thread {
        public ControlHook() {
            super("ShutdownHook:HaileyBot.java)");
        }

        public void run() {
            save();
            try {
                executor.shutdownNow();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

}
