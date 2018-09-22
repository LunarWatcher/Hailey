package io.github.lunarwatcher.java.haileybot.commands.mod.utils;

import io.github.lunarwatcher.java.haileybot.HaileyBot;
import io.github.lunarwatcher.java.haileybot.data.Constants;
import io.github.lunarwatcher.java.haileybot.mod.ModGuild;
import io.github.lunarwatcher.java.haileybot.utils.ConversionUtils;
import io.github.lunarwatcher.java.haileybot.utils.Factory2;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;

import java.util.List;

public class ModUtils {
    private static ModUtils instance;

    private HaileyBot bot;

    private ModUtils(HaileyBot bot) {
        this.bot = bot;
    }

    public static void initialize(HaileyBot bot) {
        instance = new ModUtils(bot);
    }

    public static synchronized ModUtils getInstance() {
        if (instance == null)
            throw new NullPointerException();
        return instance;
    }

    /**
     * The core of the main moderation tools.
     * @param message The message; used for replies and getting necessary API info, in addition to getting info
     *                on the user who posted the message.
     * @param rawMessage The content of the message, as received by a command.
     * @param permission The permission that's necessary to run the command. This can be something like {@link Permissions#BAN}.
     *                   The command will not work if the permission is null.
     * @param handleUser A {@link Factory2} &lt;Boolean, InternalDataForwarder, IMessage&gt; of a handler function. This is
     *                   called after the shared code for all the functions are called, which primarily includes parsing data
     *                   from the message, finding the user, and finding the reason, in addition to checking if the guild
     *                   is a {@link ModGuild}
     */
    public static void onMessageRun(IMessage message, String rawMessage, Permissions permission,
                                    Factory2<Boolean, InternalDataForwarder, IMessage> handleUser) {
        if (getInstance().bot.getModerator().getGuild(message.getGuild()) == null) {
            message.reply("this isn't a mod-enabled guild. Please run `" + Constants.TRIGGER + "enableMod` to access this feature.");
            return;
        }
        if (permission != null) {
            if (!message.getClient().getOurUser().getPermissionsForGuild(message.getGuild()).contains(permission)
                    && !message.getClient().getOurUser().getPermissionsForGuild(message.getGuild()).contains(Permissions.ADMINISTRATOR)) {
                message.reply("I do not have the appropriate permissions to do that.");
                return;
            }
            if (!message.getAuthor().getPermissionsForGuild(message.getGuild()).contains(permission)
                    && !message.getAuthor().getPermissionsForGuild(message.getGuild()).contains(Permissions.ADMINISTRATOR)) {
                message.reply("you don't have the necessary permissions to do that");
                return;
            }
        } else {
            message.reply("the permission is null. As a security precausion, this command cannot be used. Please ping a bot admin with the problem");
            return;
        }
        if (handleUser == null || rawMessage == null)
            throw new NullPointerException();
        List<IUser> mentions = message.getMentions();
        String reason = rawMessage.replaceAll("<@!?\\d+>", "").trim();
        if (reason.length() == 0 || reason.replace(" ", "").length() == 0)
            reason = "No reason.";

        if (mentions.size() == 0) {
            try {
                String[] sections = rawMessage.split(" ", 2);
                if(sections.length == 0){
                    unknownUsageMessage(message);
                    return;
                }
                Long uid = ConversionUtils.parseUser(sections[0]);
                if(uid == -2){
                    unknownUsageMessage(message);
                    return;
                }
                IUser user = message.getClient()
                        .fetchUser(uid);
                if (user == null) {
                    unknownUsageMessage(message);
                    return;
                }

                boolean result = safeAccept(handleUser, new InternalDataForwarder(user, uid, reason), message);
                handleResult(result, message);
            } catch (Exception e) {
                unknownUsageMessage(message);
            }
        } else {
            int count = mentions.size();
            if (count > 1) {
                message.reply("mass banning/kicking isn't supported due to security reasons.");
                return;
            }
            IUser user = mentions.get(0);
            if (user.getLongID() == message.getClient().getOurUser().getLongID()) {
                message.reply("I can't ban/kick myself. If you really want me to leave, please do so manually.");
                return;
            } else if (user.getLongID() == message.getAuthor().getLongID()) {
                message.reply("You can't ban/kick yourself.");
                return;
            }

            boolean result = safeAccept(handleUser, new InternalDataForwarder(user, user.getLongID(), reason), message);
            handleResult(result, message);
        }
    }

    /**
     * Tiny one-line method to unify a repeated message.
     */
    private static void unknownUsageMessage(IMessage message){
        message.reply("specify who to ban with either a mention, or their UID");
    }

    private static void handleResult(boolean result, IMessage message) {
        if (result)
            message.reply("successfully completed the action.");
        else
            message.reply("failed to complete the action.");
    }

    /**
     * Utility method for accepting a Factory2&lt;Boolean, InternalDataForwarder, IMessage&gt;, and handling
     * any failure.
     */
    private static boolean safeAccept(Factory2<Boolean, InternalDataForwarder, IMessage> fun, InternalDataForwarder data, IMessage message) {

        try {
            return fun.accept(data, message);
        } catch (Exception e) {
            e.printStackTrace();
            message.reply("Failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Bans a user with a specified reason
     */
    public static boolean banHandler(InternalDataForwarder data, IMessage message) {
        if (data.useLong()) {
            if (data.hasUID())
                message.getGuild().banUser(data.id, data.reason);
            else {
                message.reply("I failed to find that user.");
                return false;
            }
        } else
            message.getGuild().banUser(data.user, data.reason);
        audit(message.getGuild(), createEmbedLog("Ban", data, message.getAuthor()));
        return true;
    }

    /**
     * Kicks a user with a specified reason
     */
    public static boolean kickHandler(InternalDataForwarder data, IMessage message) {
        if (data.useLong()) {
            message.reply("I failed to find the user.");
            return false;
        }
        message.getGuild().kickUser(data.user, data.reason);
        audit(message.getGuild(), createEmbedLog("Kick", data, message.getAuthor()));
        return true;
    }

    /**
     * Note that the API doesn't care about unbanning reasons; only kick and ban has a reason field in it. The reason
     * for unbanning is only posted in chat by the bot.
     */
    public static boolean unbanHandler(InternalDataForwarder data, IMessage message) {
        if (!data.hasUID()) {
            message.reply("You need a valid UID to do that.");
            return false;
        }
        message.getGuild().pardonUser(data.id);
        audit(message.getGuild(), createEmbedLog("Unban", data, message.getAuthor()));

        return true;
    }

    private static EmbedObject createEmbedLog(String mode, InternalDataForwarder forwarder, IUser handler) {
        return new EmbedBuilder()
                .withTitle(mode)
                .withDesc("**User taken action against:** " + forwarder.getName() + " (UID: " + forwarder.getId() + ")\n")
                .appendDesc("**Moderator:** " + handler.getName() + "#" + handler.getDiscriminator() + " (UID " + handler.getLongID() + ")\n")
                .appendDesc("**Reason:** " + forwarder.reason)
                .build();
    }

    private static void audit(IGuild guild, String message) {
        ModGuild modGuild = getInstance().bot.getModerator().getGuild(guild);
        if (modGuild == null)
            return;

        modGuild.audit(message);
    }

    private static void audit(IGuild guild, EmbedObject message) {
        ModGuild modGuild = getInstance().bot.getModerator().getGuild(guild);
        if (modGuild == null)
            return;

        modGuild.audit(message);
    }

    /**
     * This <b>should not be used outside this class.</b> It's public to access the functions defined earlier (see {@link ModUtils#banHandler(InternalDataForwarder, IMessage)},
     * {@link ModUtils#kickHandler(InternalDataForwarder, IMessage)}, and {@link ModUtils#unbanHandler(InternalDataForwarder, IMessage)}).
     */
    public static final class InternalDataForwarder {
        private IUser user;
        private Long id;
        private String reason;

        public InternalDataForwarder(IUser user, Long id, String reason) {
            this.user = user;
            this.id = id;
            this.reason = reason;
        }

        public boolean useLong() {
            return user == null;
        }

        public boolean hasUID() {
            return id != 0;
        }

        public String getName() {
            if (user == null) return "Unknown username.";
            return user.getName() + "#" + user.getDiscriminator();
        }

        public long getId() {
            if (id == 0 && user == null)
                return 0;
            else if (id == 0)
                return user.getLongID();
            return id;
        }
    }

}
