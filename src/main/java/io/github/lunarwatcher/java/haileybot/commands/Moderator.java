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

package io.github.lunarwatcher.java.haileybot.commands;

import io.github.lunarwatcher.java.haileybot.HaileyBot;
import io.github.lunarwatcher.java.haileybot.data.Database;
import io.github.lunarwatcher.java.haileybot.mod.ModGuild;
import io.github.lunarwatcher.java.haileybot.utils.ExtensionsKt;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.audit.ActionType;
import net.dv8tion.jda.core.audit.AuditLogEntry;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.GuildBanEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
public class Moderator {
    public static final String KEY = "moderator";
    public static final String INVITE_FEATURE = "invite_spam_blocking";
    public static final String AUDIT_FEATURE = "audit_channel";
    public static final String WELCOME_LOGGING = "welcome_logging";
    public static final String LEAVE_LOGGING = "leave_logging";
    public static final String JOIN_MESSAGE = "join_message";
    public static final String LEAVE_MESSAGE = "leave_message";
    public static final String JOIN_DM = "join_dm";
    public static final String DELETION_WATCHER = "deletion_watcher";
    public static final String BAN_MONITORING_FEATURE = "ban_monitoring";
    private static final Logger logger = LoggerFactory.getLogger(Moderator.class);
    private static final String features = combine(INVITE_FEATURE, WELCOME_LOGGING, AUDIT_FEATURE, LEAVE_LOGGING,
            JOIN_MESSAGE, LEAVE_MESSAGE, JOIN_DM, DELETION_WATCHER, BAN_MONITORING_FEATURE);

    private HaileyBot bot;
    private Map<Long, ModGuild> enabledGuilds;

    public Moderator(HaileyBot bot) {
        this.bot = bot;
        enabledGuilds = new HashMap<>();
        load();

        logger.info("Successfully loaded the moderator.");
    }

    public static String getFeatures() {
        return features;
    }

    private static String combine(String... args) {
        StringBuilder res = new StringBuilder();
        for (String arg : args) {
            res.append(arg);
            if (args.length != args.length - 1) {
                res.append(", ");
            }
        }
        return res.toString();
    }

    // Events

    public static String getUsername(User user) {
        if (user == null)
            return "Unknown";
        return user.getName() + "#" + user.getDiscriminator();
    }

    public static MessageEmbed banEmbed(String usernameAndDiscriminator, Long uid, String banner, long bannerUid, String reason) {
        return new EmbedBuilder()
                .setTitle("User banned")
                .setDescription(concenateDetails("Name", usernameAndDiscriminator))
                .appendDescription(concenateDetails("UID", String.valueOf(uid)))
                .appendDescription(concenateDetails("Banned by", banner + " (UID " + (bannerUid == 0 ? "Unknown" : bannerUid) + ")"))
                .appendDescription(concenateDetails("Reason", reason))
                .build();
    }

    public static String concenateDetails(String title, String content) {
        return "**" + title + "**: " + content + "\n";
    }

    public void save() {
        Map<String, Map<String, Object>> data = new HashMap<>();
        for (Map.Entry<Long, ModGuild> modGuild : enabledGuilds.entrySet()) {
            data.put(Long.toString(modGuild.getKey()), modGuild.getValue().getDataAsMap());
        }

        bot.getDatabase().put(KEY, data);
    }

    public void load() {
        Map<String, Object> data = bot.getDatabase().getMap(KEY);
        if (data == null)
            return;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            long id = Long.parseLong(entry.getKey());
            try {
                Map<String, Object> dat = (Map<String, Object>) entry.getValue();
                ModGuild guild = new ModGuild(bot, id);
                guild.loadFromMap(dat);
                enabledGuilds.put(id, guild);
            } catch (ClassCastException e) {
                e.printStackTrace();
            }

        }
    }

    // Meta

    public void userJoined(GuildMemberJoinEvent event) {
        try {
            bot.getAssigner()
                    .onUserJoined(event);
        } catch (NullPointerException e) {
            //Ignore
        }
        ModGuild guild = enabledGuilds.get(event.getGuild().getIdLong());
        if (guild == null)
            return;
        guild.userJoined(event);

    }

    public boolean messageReceived(MessageReceivedEvent event) {
        if (event.getChannel() instanceof PrivateChannel)
            return false;
        ModGuild guild = enabledGuilds.get(event.getGuild().getIdLong());
        if (guild == null)
            return false;
        logger.info("Message received in {} (UID {}), channel {} . Author: {} (UID {}): \"{}\"",
                event.getGuild().getName(), event.getGuild().getIdLong(), event.getChannel().getName(),
                event.getAuthor().getName(), event.getAuthor().getIdLong(), event.getMessage().getContentRaw());
        guild.messageReceived(event);
        return false;
    }

    public void messageEdited(MessageUpdateEvent event) {
        if (event.getChannel() instanceof PrivateChannel)
            return;

        // TODO create edit watcher
    }

    public void userLeft(GuildMemberLeaveEvent event) {
        ModGuild guild = enabledGuilds.get(event.getGuild().getIdLong());
        if (guild == null)
            return;

        guild.userLeft(event);
    }

    public void messageDeleted(MessageDeleteEvent event) {
        if (event.getChannel() instanceof PrivateChannel)
            return;

        // TODO create message deletion watcher
    }

    public boolean registerGuild(Guild guild) {
        if (enabledGuilds.containsKey(guild.getIdLong()))
            return false;

        enabledGuilds.put(guild.getIdLong(), new ModGuild(bot, guild.getIdLong()));
        return true;
    }

    public boolean removeGuild(Guild guild) {
        if (!enabledGuilds.containsKey(guild.getIdLong()))
            return false;
        enabledGuilds.remove(guild.getIdLong());
        return true;
    }

    public boolean isGuildEnabled(Guild guild) {
        return enabledGuilds.containsKey(guild.getIdLong());
    }

    @Nullable
    public ModGuild getGuild(long id) {
        return enabledGuilds.get(id);
    }

    @Nullable
    public ModGuild getGuild(Guild guild) {
        return getGuild(guild.getIdLong());
    }

    public void userBanned(GuildBanEvent event) {

        ModGuild guild = enabledGuilds.get(event.getGuild().getIdLong());
        if (guild == null)
            return;

        if (guild.getBanMonitoring()) {
            String usernameAndDiscriminator = event.getUser().getName() + "#" + event.getUser().getDiscriminator();
            final long uid = event.getUser().getIdLong();
            if (!ExtensionsKt.hasPermissions(event.getJDA().getSelfUser(), event.getGuild(), true, Permission.VIEW_AUDIT_LOGS, Permission.ADMINISTRATOR)) {
                fail(usernameAndDiscriminator, uid, guild);
            } else {
                event.getGuild()
                        .getAuditLogs()
                        .type(ActionType.BAN)
                        .queueAfter(10, TimeUnit.SECONDS, (entries) -> {
                            for (AuditLogEntry entry : entries) {
                                if (entry.getTargetIdLong() == uid) {
                                    User user = entry.getUser();

                                    long bannerUid = user == null ? -1 : user.getIdLong();
                                    String banner = getUsername(entry.getUser());
                                    String reason = entry.getReason();
                                    if (reason == null)
                                        reason = "No reason given";
                                    guild.audit(banEmbed(usernameAndDiscriminator, uid, banner, bannerUid, reason));
                                    return;
                                }
                            }
                            fail(usernameAndDiscriminator, uid, guild);
                        });
            }
        }

    }

    private void fail(String usernameAndDiscriminator, long uid, ModGuild guild) {
        guild.audit("A user has been banned, but I either don't have permission to see the details, or can't get an entry after 25 minutes.");
        guild.audit(banEmbed(usernameAndDiscriminator, uid, "Unknown", 0, "Unknown"));
    }

    public void refreshGuildLogs() {
        for (ModGuild i : enabledGuilds.values()) {
            i.processCache();
        }
    }

    public int size() {
        return enabledGuilds.size();
    }

    public Collection<ModGuild> getModGuilds() {
        return enabledGuilds.values();
    }
}
