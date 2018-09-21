package io.github.lunarwatcher.java.haileybot.mod;

import io.github.lunarwatcher.java.haileybot.CrashHandler;
import io.github.lunarwatcher.java.haileybot.HaileyBot;
import io.github.lunarwatcher.java.haileybot.data.RegexConstants;
import io.github.lunarwatcher.java.haileybot.data.SizeLimitedList;
import io.github.lunarwatcher.java.haileybot.utils.ExtensionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserLeaveEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static io.github.lunarwatcher.java.haileybot.commands.Moderator.*;
import static io.github.lunarwatcher.java.haileybot.utils.NumberUtils.getNumberWithNth;
import static io.github.lunarwatcher.java.haileybot.utils.TypeUtils.assertType;

public class ModGuild {
    private static final Logger logger = LoggerFactory.getLogger(ModGuild.class);
    private static final String DEFAULT_LEAVE_MESSAGE = "What a shame to see you go, {0}!";
    private static final String DEFAULT_JOIN_MESSAGE = "Welcome to {1}, {0}!";

    // General
    private long guild;

    // Enabled/disabled features
    private boolean inviteSpamProtection;
    private boolean banMonitoring;

    // Metadata
    private long auditChannel = -1;
    private long welcomeChannel = -1;
    private long userLeaveChannel = -1;
    private int warnings;

    // Joining and leaving
    private String joinMessage;
    private String leaveMessage;
    private String joinDM;

    // Internal meta
    private HaileyBot bot;

    private SizeLimitedList<IUser> recentlyBanned = new SizeLimitedList<>(5);
    private SizeLimitedList<IMessage> messages = new SizeLimitedList<>(30);

    public ModGuild(HaileyBot bot, long guild) {
        this.bot = bot;
        this.guild = guild;
    }

    public void setWarnings(int warnings) {
        this.warnings = warnings;
    }

    public void banAndLog(IUser user, String reason) {
        this.recentlyBanned.add(user);


        boolean logging = true;
        if (auditChannel == -1) {
            logger.warn("WARNING: Audit channel is null. Logging disabled.");
            logging = false;
        }

        try {
            user.getClient().getGuildByID(guild)
                    .banUser(user, 7);
            if (logging) {
                user.getClient().getGuildByID(guild)
                        .getChannelByID(auditChannel)
                        .sendMessage("Banned user " + user.getLongID() + ": " + reason);
            }
        } catch (Exception e) {
            logger.warn("Failed to ban user " + user.getLongID());
            if (logging) {
                user.getClient().getGuildByID(guild)
                        .getChannelByID(auditChannel)
                        .sendMessage("***WARNING***: Banning user " + user.getLongID() + " failed. Check my perms");
            }
            e.printStackTrace();
        }

    }

    public void toggleInviteSpamProtection(boolean inviteSpamProtection) {
        this.inviteSpamProtection = inviteSpamProtection;
    }

    public boolean getInviteSpamProtection() {
        return inviteSpamProtection;
    }

    public long getChannel() {
        return auditChannel;
    }

    public long getGuild() {
        return guild;
    }

    public void userJoined(UserJoinEvent event) {
        if (inviteSpamProtection) {
            if (RegexConstants.INVITE_SPAM.matcher(event.getUser().getName()).find()) {
                banAndLog(event.getUser(), "Invite in username");
                recentlyBanned.add(event.getUser());
                nukeMessages();
                return;
            }
        }
        try {
            if (event.getUser().isBot())
                return;

            if (joinMessage == null && welcomeChannel > 0) {
                event.getGuild().getChannelByID(welcomeChannel)
                        .sendMessage(ExtensionsKt.messageFormat(DEFAULT_JOIN_MESSAGE, event.getUser().getName() + "#" + event.getUser().getDiscriminator(), event.getGuild().getName()));
            } else if (joinMessage != null && welcomeChannel > 0) {
                event.getGuild().getChannelByID(welcomeChannel)
                        .sendMessage(ExtensionsKt.messageFormat(joinMessage,
                                event.getUser().getName() + "#" + event.getUser().getDiscriminator(),
                                event.getGuild().getName(),
                                Integer.toString(event.getGuild().getUsers().size()),
                                getNumberWithNth(event.getGuild().getUsers().size())));
            }
        } catch (Exception e) {
            e.printStackTrace();
            CrashHandler.error(e);
            audit("An error happened while attempting to send a join message. Are the channels properly configured?");
        }

        if (joinDM != null) {
            event.getUser().getOrCreatePMChannel()
                    .sendMessage("Welcome to " + event.getGuild().getName() + "! One of the server owners/admins has set a welcome message for joining users.\n\n\n" +
                            ExtensionsKt.messageFormat(joinDM,
                                    event.getUser().getName() + "#" + event.getUser().getDiscriminator(),
                                    event.getGuild().getName(),
                                    Integer.toString(event.getGuild().getUsers().size())));
        }
    }

    public void userLeft(UserLeaveEvent message) {
        if (recentlyBanned.contains(message)) {
            return;
        }

        try {
            if (message.getUser().isBot())
                return;
            if (leaveMessage == null && userLeaveChannel > 0) {
                message.getGuild().getChannelByID(userLeaveChannel)
                        .sendMessage(ExtensionsKt.messageFormat(DEFAULT_LEAVE_MESSAGE, message.getUser().getName() + "#" + message.getUser().getDiscriminator()));
            } else if (leaveMessage != null && userLeaveChannel > 0) {
                message.getGuild().getChannelByID(userLeaveChannel)
                        .sendMessage(ExtensionsKt.messageFormat(leaveMessage, message.getUser().getName() + "#" + message.getUser().getDiscriminator()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            CrashHandler.error(e);
            audit("An error happened while attempting to send a leave message. Are the channels properly configured?");
        }
    }

    public void messageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            messages.add(event.getMessage());
        }
        nukeMessages();
    }

    private void nukeMessages() {
        if (recentlyBanned.hasAny() && messages.hasAny()) {
            logger.debug("{}, {}", recentlyBanned, messages);
            List<String> ids = new ArrayList<>();
            for (IUser user : recentlyBanned) {
                for (IMessage message : messages) {
                    if (message.isDeleted())
                        continue;

                    if (Pattern.compile("(?i)" + user.getName().toLowerCase()).matcher(message.getContent()).find()
                            || Pattern.compile("(?i)<@!?" + user.getStringID() + ">").matcher(message.getContent()).find()) {
                        try {
                            ids.add(message.getStringID());
                            message.delete();
                        } catch (Exception e) {
                            logger.warn("Failed to delete message.");
                            CrashHandler.error(e);
                            audit("Failed to delete message " + message.getStringID());
                        }
                    }
                }

            }
            if (ids.size() != 0) {
                audit("Deleted messages. IDs: " + ids);
                recentlyBanned.clear();
                messages.clear();
            }

        }
    }

    public void audit(String data) {
        if (auditChannel > 0) {
            try {
                bot.getClient()
                        .getGuildByID(guild)
                        .getChannelByID(auditChannel)
                        .sendMessage(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void audit(EmbedObject embed) {
        if (auditChannel > 0) {
            try {
                bot.getClient()
                        .getGuildByID(guild)
                        .getChannelByID(auditChannel)
                        .sendMessage(embed);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Map<String, Object> getDataAsMap() {
        Map<String, Object> data = new HashMap<>();
        data.put(INVITE_FEATURE, inviteSpamProtection);
        data.put(BAN_MONITORING_FEATURE, banMonitoring);
        if (auditChannel > 0)
            data.put(AUDIT_FEATURE, auditChannel);

        if (welcomeChannel > 0)
            data.put(WELCOME_LOGGING, welcomeChannel);

        if (userLeaveChannel > 0)
            data.put(LEAVE_LOGGING, userLeaveChannel);

        if (joinMessage != null)
            data.put(JOIN_MESSAGE, joinMessage);
        if (leaveMessage != null)
            data.put(LEAVE_MESSAGE, leaveMessage);
        if (joinDM != null)
            data.put(JOIN_DM, joinDM);

        return data;
    }

    public Map<String, Object> getDataAsReadableMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("Invite spam protection", formatBoolean(inviteSpamProtection));
        data.put("Ban monitoring", formatBoolean(banMonitoring));

        data.put("Audit channel", formatChannel(auditChannel));
        data.put("Welcome channel", formatChannel(welcomeChannel));
        data.put("Leave channel", formatChannel(userLeaveChannel));

        if (welcomeChannel > 0)
            data.put("Join greeting", formatString(joinMessage, true));
        if (userLeaveChannel > 0)
            data.put("Leave message", formatString(leaveMessage, true));
        data.put("Join DM", formatString(joinDM));

        return data;
    }

    public void loadFromMap(Map<String, Object> data) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getKey().equals(INVITE_FEATURE))
                inviteSpamProtection = (boolean) entry.getValue();
            else if (entry.getKey().equalsIgnoreCase(BAN_MONITORING_FEATURE))
                banMonitoring = (boolean) entry.getValue();
            else if (entry.getKey().equals(AUDIT_FEATURE))
                auditChannel = Long.valueOf(entry.getValue().toString());
            else if (entry.getKey().equals(WELCOME_LOGGING))
                welcomeChannel = Long.valueOf(entry.getValue().toString());
            else if (entry.getKey().equals(LEAVE_LOGGING))
                userLeaveChannel = Long.valueOf(entry.getValue().toString());
            else if (entry.getKey().equals(JOIN_MESSAGE))
                joinMessage = entry.getValue().toString().replaceAll("(?i)<user>", "{0}")
                        .replaceAll("(?i)<server>", "{1}")
                        .replaceAll("(?i)<members>", "{2}")
                        .replaceAll("(?i)<nthmember>", "{3}");
            else if (entry.getKey().equalsIgnoreCase(LEAVE_MESSAGE))
                leaveMessage = entry.getValue().toString().replaceAll("(?i)<user>", "{0}");
            else if (entry.getKey().equalsIgnoreCase(JOIN_DM))
                joinDM = entry.getValue().toString().replaceAll("(?i)<user>", "{0}")
                        .replaceAll("(?i)<server>", "{1}")
                        .replaceAll("(?i)<members>", "{2}")
                        .replaceAll("(?i)<nthmember>", "{3}");
            else
                logger.warn("Unknown key: " + entry.getKey() + ". Value: " + entry.getValue());

        }
    }

    public void set(@NotNull String featureName, Object data) {
        switch (featureName.toLowerCase()) {
            case INVITE_FEATURE:
                assertType(data, Boolean.class);
                inviteSpamProtection = (boolean) data;
                break;
            case BAN_MONITORING_FEATURE:
                assertType(data, Boolean.class);
                banMonitoring = (boolean) data;
                break;
            case AUDIT_FEATURE:
                assertType(data, Long.class);
                auditChannel = (long) data;
                break;
            case WELCOME_LOGGING:
                assertType(data, Long.class);
                welcomeChannel = (long) data;
                joinMessage = DEFAULT_JOIN_MESSAGE;
                break;
            case LEAVE_LOGGING:
                assertType(data, Long.class);
                userLeaveChannel = (long) data;
                leaveMessage = DEFAULT_LEAVE_MESSAGE;
                break;
            case JOIN_MESSAGE:
                assertType(data, String.class);
                joinMessage = ((String) data).replaceAll("(?i)<user>", "{0}")
                        .replaceAll("(?i)<server>", "{1}")
                        .replaceAll("(?i)<members>", "{2}")
                        .replaceAll("(?i)<nthmember>", "{3}");
                break;
            case LEAVE_MESSAGE:
                assertType(data, String.class);
                leaveMessage = ((String) data).replaceAll("(?i)<user>", "{0}");
                break;
            case JOIN_DM:
                assertType(data, String.class);
                joinDM = ((String) data).replaceAll("(?i)<user>", "{0}")
                        .replaceAll("(?i)<server>", "{1}")
                        .replaceAll("(?i)<members>", "{2}")
                        .replaceAll("(?i)<nthmember>", "{3}");
                break;
            default:
                throw new RuntimeException("");
        }
    }

    private String formatChannel(long channel) {
        return channel <= 0 ? "None" : "<#" + channel + ">";
    }

    private String formatBoolean(boolean value) {
        return value ? "Enabled" : "Disabled";
    }

    private String formatString(@Nullable String value) {
        return formatString(value, false);
    }

    private String formatString(@Nullable String value, boolean hasDefaults) {
        return value == null ? "None " + (hasDefaults ? "(using defaults)" : "") : value;
    }

    public boolean getBanMonitoring() {
        return banMonitoring;
    }
}
