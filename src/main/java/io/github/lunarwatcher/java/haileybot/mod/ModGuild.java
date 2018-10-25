package io.github.lunarwatcher.java.haileybot.mod;

import io.github.lunarwatcher.java.haileybot.CrashHandler;
import io.github.lunarwatcher.java.haileybot.HaileyBot;
import io.github.lunarwatcher.java.haileybot.data.DecayableList;
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
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static io.github.lunarwatcher.java.haileybot.commands.Moderator.*;
import static io.github.lunarwatcher.java.haileybot.utils.NumberUtils.getNumberWithNth;
import static io.github.lunarwatcher.java.haileybot.utils.TypeUtils.assertType;

public class ModGuild {
    private static final Logger logger = LoggerFactory.getLogger(ModGuild.class);
    private static final String DEFAULT_LEAVE_MESSAGE = "What a shame to see you go, {0}!";
    private static final String DEFAULT_JOIN_MESSAGE = "Welcome to {1}, {0}!";
    private static final long FLUSH_TIMEOUT = 30000;

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

    private DecayableList<AutoBannedUser> recentBans = new DecayableList<>(30000);
    private SizeLimitedList<IMessage> recentMessages = new SizeLimitedList<>(30);

    private StringBuilder messageBuffer = new StringBuilder();
    private long lastMessage = 0;

    public ModGuild(HaileyBot bot, long guild) {
        this.bot = bot;
        this.guild = guild;
    }

    public void banAndLog(IUser user, AutoBanReasons reason) {
        this.recentBans.add(new AutoBannedUser(user, reason));

        boolean logging = true;
        if (auditChannel == -1) {
            logger.warn("WARNING: Audit channel is null. Logging disabled.");
            logging = false;
        }

        try {
            RequestBuffer.request(() -> {
                user.getClient().getGuildByID(guild)
                        .banUser(user, 7);
            });

            if (logging) {
                RequestBuffer.request(() -> {
                    user.getClient().getGuildByID(guild)
                            .getChannelByID(auditChannel)
                            .sendMessage(new EmbedBuilder().withColor(Color.ORANGE)
                                    .withTitle("User banned")
                                    .withDesc("UID: " + user.getLongID() + ". Banned by auto-mod: " + reason.getReason())
                                    .build());
                });
            }
        } catch (Exception e) {
            logger.warn("Failed to ban user " + user.getLongID());
            if (logging) {
                RequestBuffer.request(() -> {
                    user.getClient().getGuildByID(guild)
                            .getChannelByID(auditChannel)
                            .sendMessage("***WARNING***: Banning user " + user.getLongID() + " failed. Check my perms");
                });
            }
        }

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
                banAndLog(event.getUser(), AutoBanReasons.INVITE_USERNAME);
                nukeMessages();
                return;
            } else if (RegexConstants.GENERAL_SPAM.matcher(event.getUser().getName()).find()) {
                banAndLog(event.getUser(), AutoBanReasons.SPAM_USERNAME);
                nukeMessages();
                return;
            } else if (RegexConstants.UNCAUGHT_SPAM.matcher(event.getUser().getName()).find()) {
                banAndLog(event.getUser(), AutoBanReasons.UNHANDLED_SPAM);
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
        if (recentBans.stream().anyMatch(
                it -> it.getBannedUser() == message.getUser()
                        || it.getBannedUser().equals(message.getUser())
        )) {
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
            recentMessages.add(event.getMessage());
        }
        nukeMessages();
        processCache();
    }

    private synchronized void nukeMessages() {
        if (recentBans.hasAny() && recentMessages.hasAny()) {
            logger.debug("{}, {}", recentBans, recentMessages);
            Map<IMessage, String> deletedMessages = new HashMap<>();
            for (AutoBannedUser autoBannedUser : recentBans) {
                IUser user = autoBannedUser.getBannedUser();
                for (IMessage message : recentMessages) {
                    if (message.isDeleted())
                        continue;


                    if (Pattern.compile("(?i)" + user.getName().toLowerCase()).matcher(message.getContent()).find()
                            || Pattern.compile("(?i)<@!?" + user.getStringID() + ">").matcher(message.getContent()).find()) {
                        try {

                            if (!deletedMessages.containsKey(message) && auditChannel > 0)
                                deletedMessages.put(message, autoBannedUser.getStringReason());
                            message.delete();
                        } catch (Exception e) {
                            logger.warn("Failed to delete message.");
                            CrashHandler.error(e);
                            audit("Failed to delete message " + message.getStringID());
                        }
                    }
                }

            }
            if (deletedMessages.size() != 0) {

                for (Map.Entry<IMessage, String> entry : deletedMessages.entrySet()) {
                    IMessage m = entry.getKey();
                    String reason = entry.getValue();

                    messageBuffer.append("Deleted message ")
                            .append(m.getStringID())
                            .append(" from ")
                            .append(m.getAuthor().getName()).append("#").append(m.getAuthor().getDiscriminator())
                            .append(" (Poster is a ")
                            .append(m.getAuthor().isBot() ? "bot" : "user")
                            .append(")\n")
                            .append("Occured while banning users for \"")
                            .append(reason).append("\"\n");

                }

                recentMessages.clear();
            }

            lastMessage = System.currentTimeMillis();

        }
    }

    public synchronized void processCache() {
        if (auditChannel < 0)
            return;
        if (messageBuffer.length() == 0 || System.currentTimeMillis() - lastMessage < FLUSH_TIMEOUT)
            return;
        audit(new EmbedBuilder()
                .withTitle("Auto-mod message deleter")
                .withColor(Color.RED)
                .withDesc(messageBuffer.toString()).build());
        messageBuffer = new StringBuilder();
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
                if (data == null) {
                    inviteSpamProtection = false;
                    return;
                }
                assertType(data, Boolean.class);
                inviteSpamProtection = (boolean) data;
                break;
            case BAN_MONITORING_FEATURE:
                if (data == null) {
                    banMonitoring = false;
                    return;
                }
                assertType(data, Boolean.class);
                banMonitoring = (boolean) data;
                break;
            case AUDIT_FEATURE:
                if (data == null) {
                    auditChannel = -1L;
                    return;
                }
                assertType(data, Long.class);
                auditChannel = (long) data;
                break;
            case WELCOME_LOGGING:
                if (data == null) {
                    welcomeChannel = -1L;
                    return;
                }
                assertType(data, Long.class);
                welcomeChannel = (long) data;
                joinMessage = DEFAULT_JOIN_MESSAGE;
                break;
            case LEAVE_LOGGING:
                if (data == null) {
                    userLeaveChannel = -1L;
                    return;
                }
                assertType(data, Long.class);
                userLeaveChannel = (long) data;
                leaveMessage = DEFAULT_LEAVE_MESSAGE;
                break;
            case JOIN_MESSAGE:
                if (data == null) {
                    joinMessage = null;
                    return;
                }
                assertType(data, String.class);
                joinMessage = ((String) data).replaceAll("(?i)<user>", "{0}")
                        .replaceAll("(?i)<server>", "{1}")
                        .replaceAll("(?i)<members>", "{2}")
                        .replaceAll("(?i)<nthmember>", "{3}");
                break;
            case LEAVE_MESSAGE:
                if (data == null) {
                    leaveMessage = null;
                    return;
                }
                assertType(data, String.class);
                leaveMessage = ((String) data).replaceAll("(?i)<user>", "{0}");
                break;
            case JOIN_DM:
                if (data == null) {
                    joinDM = null;
                    return;
                }
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

    public HaileyBot getBot() {
        return bot;
    }

}
