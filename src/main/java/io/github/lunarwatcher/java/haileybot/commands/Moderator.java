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
import io.github.lunarwatcher.java.haileybot.mod.ModGuild;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.audit.ActionType;
import sx.blah.discord.handle.audit.entry.AuditLogEntry;
import sx.blah.discord.handle.audit.entry.TargetedEntry;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageEditEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserBanEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserLeaveEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static String getUsername(IUser user) {
        return user.getName() + "#" + user.getDiscriminator();
    }

    public static EmbedObject banEmbed(String usernameAndDiscriminator, Long uid, String banner, long bannerUid, String reason) {
        return new EmbedBuilder()
                .withTitle("User banned")
                .withDesc(concenateDetails("Name", usernameAndDiscriminator))
                .appendDesc(concenateDetails("UID", String.valueOf(uid)))
                .appendDesc(concenateDetails("Banned by", banner + " (UID " + (bannerUid == 0 ? "Unknown" : bannerUid) + ")"))
                .appendDesc(concenateDetails("Reason", reason))
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

    public void userJoined(UserJoinEvent event) {
        try {
            bot.getAssigner()
                    .onUserJoined(event);
        } catch (NullPointerException e) {
            //Ignore
        }
        ModGuild guild = enabledGuilds.get(event.getGuild().getLongID());
        if (guild == null)
            return;
        guild.userJoined(event);

    }

    public boolean messageReceived(MessageReceivedEvent event) {
        if (event.getChannel() instanceof IPrivateChannel)
            return false;
        ModGuild guild = enabledGuilds.get(event.getGuild().getLongID());
        if (guild == null)
            return false;
        logger.info("Message received in {} (UID {}), channel {} . Author: {} (UID {}): \"{}\"",
                event.getGuild().getName(), event.getGuild().getLongID(), event.getChannel().getName(),
                event.getAuthor().getName(), event.getAuthor().getLongID(), event.getMessage().getContent());
        guild.messageReceived(event);
        return false;
    }

    public void messageEdited(MessageEditEvent event) {
        if (event.getChannel() instanceof IPrivateChannel)
            return;

        // TODO create edit watcher
    }

    public void userLeft(UserLeaveEvent event) {
        ModGuild guild = enabledGuilds.get(event.getGuild().getLongID());
        if (guild == null)
            return;

        guild.userLeft(event);
    }

    public void messageDeleted(MessageDeleteEvent event) {
        if (event.getChannel() instanceof IPrivateChannel)
            return;

        // TODO create message deletion watcher
    }

    public boolean registerGuild(IGuild guild) {
        if (enabledGuilds.containsKey(guild.getLongID()))
            return false;

        enabledGuilds.put(guild.getLongID(), new ModGuild(bot, guild.getLongID()));
        return true;
    }

    public boolean removeGuild(IGuild guild) {
        if (!enabledGuilds.containsKey(guild.getLongID()))
            return false;
        enabledGuilds.remove(guild.getLongID());
        return true;
    }

    public boolean isGuildEnabled(IGuild guild) {
        return enabledGuilds.containsKey(guild.getLongID());
    }

    @Nullable
    public ModGuild getGuild(long id) {
        return enabledGuilds.get(id);
    }

    @Nullable
    public ModGuild getGuild(IGuild guild) {
        return getGuild(guild.getLongID());
    }

    public void userBanned(UserBanEvent event) {

        ModGuild guild = enabledGuilds.get(event.getGuild().getLongID());
        if (guild == null)
            return;

        if (guild.getBanMonitoring()) {
            String usernameAndDiscriminator = event.getUser().getName() + "#" + event.getUser().getDiscriminator();
            long uid = event.getUser().getLongID();
            if (!event.getClient().getOurUser().getPermissionsForGuild(event.getGuild()).contains(Permissions.VIEW_AUDIT_LOG)
                    || !event.getClient().getOurUser().getPermissionsForGuild(event.getGuild()).contains(Permissions.ADMINISTRATOR)) {
                fail(usernameAndDiscriminator, uid, guild);
            } else {
                launchAsyncAuditBanLogTask(usernameAndDiscriminator, event, guild);
            }
        }

    }

    private void fail(String usernameAndDiscriminator, long uid, ModGuild guild) {
        guild.audit("A user has been banned, but I either don't have permission to see the details, or can't get an entry after 25 minutes.");
        guild.audit(banEmbed(usernameAndDiscriminator, uid, "Unknown", 0, "Unknown"));
    }

    private void launchAsyncAuditBanLogTask(final String usernameAndDiscriminator, UserBanEvent event, final ModGuild guild) {
        final long uid = event.getUser().getLongID();

        new Thread(() -> {
            int attempt = 0;

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    for (int i = 0; i < 5; i++) {
                        if (Thread.currentThread().isInterrupted())
                            break;
                        Thread.sleep(60000);
                    }
                } catch (InterruptedException e) {
                    // Ignore
                }
                attempt++;
                List<TargetedEntry> entries = event.getGuild()
                        .getAuditLog(ActionType.MEMBER_BAN_ADD)
                        .getEntriesByTarget(event.getUser().getLongID());


                if (entries.size() == 0) {
                    if (attempt >= 5) {
                        fail(usernameAndDiscriminator, uid, guild);
                        break;
                    }
                    continue;
                }
                AuditLogEntry entry = entries.get(0);

                IUser responsible = entry.getResponsibleUser();
                if (responsible.getLongID() == bot.getClient().getOurUser().getLongID()) {
                    // If the banner is the bot, it's guranteed the bot has sent a message about the ban.
                    // if not, that's a bug.
                    break;
                }
                long bannerUid = responsible.getLongID();
                String banner = getUsername(responsible);
                String reason = entry.getReason().orElse("No reason specified");

                guild.audit(banEmbed(usernameAndDiscriminator, uid, banner, bannerUid, reason));
                break;
            }
        }).start();
    }

    public void refreshGuildLogs() {
        for(ModGuild i : enabledGuilds.values()){
            i.processCache();
        }
    }

    public int size(){
        return enabledGuilds.size();
    }
}
