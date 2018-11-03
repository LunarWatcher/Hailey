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

package io.github.lunarwatcher.java.haileybot.commands.mod.utils;

import io.github.lunarwatcher.java.haileybot.CrashHandler;
import io.github.lunarwatcher.java.haileybot.HaileyBot;
import io.github.lunarwatcher.java.haileybot.data.Constants;
import io.github.lunarwatcher.java.haileybot.mod.ModGuild;
import io.github.lunarwatcher.java.haileybot.utils.ConversionUtils;
import io.github.lunarwatcher.java.haileybot.utils.ExtensionsKt;
import io.github.lunarwatcher.java.haileybot.utils.Factory2;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     *
     * @param message    The message; used for replies and getting necessary API info, in addition to getting info
     *                   on the user who posted the message.
     * @param rawMessage The content of the message, as received by a command.
     * @param permission The permission that's necessary to run the command. This can be something like {@link Permission#BAN_MEMBERS}.
     *                   The command will not work if the permission is null.
     * @param handleUser A {@link Factory2} &lt;Boolean, InternalDataForwarder, Message&gt; of a handler function. This is
     *                   called after the shared code for all the functions are called, which primarily includes parsing data
     *                   from the message, finding the user, and finding the reason, in addition to checking if the guild
     *                   is a {@link ModGuild}
     */
    public static void onMessageRun(Message message, String rawMessage, Permission permission,
                                    Factory2<Boolean, InternalDataForwarder, Message> handleUser) {
        if (getInstance().bot.getModerator().getGuild(message.getGuild()) == null) {

            message.getChannel().sendMessage("this isn't a mod-enabled guild. Please run `" + Constants.TRIGGER + "enableMod` to access this feature.").queue();

            return;
        }
        if (permission != null) {
            if (!message.getGuild().getMember(message.getJDA().getSelfUser()).hasPermission(permission)
                    && !message.getGuild().getMember(message.getJDA().getSelfUser()).hasPermission(Permission.ADMINISTRATOR)) {
                message.getChannel().sendMessage("I do not have the appropriate permissions to do that.").queue();
                return;
            }
            if (!message.getMember().hasPermission(permission)
                    && !message.getMember().hasPermission(Permission.ADMINISTRATOR)) {

                message.getChannel().sendMessage("you don't have the necessary permissions to do that").queue();

                return;
            }
        } else {
            message.getChannel().sendMessage("the permission is null. As a security precausion, this command cannot be used. Please ping a bot admin with the problem").queue();

            return;
        }
        if (handleUser == null || rawMessage == null)
            throw new NullPointerException();
        List<Member> mentions = message.getMentionedMembers();
        String reason = rawMessage.replaceAll("<@!?\\d+>", "").trim();
        if (reason.length() == 0 || reason.replace(" ", "").length() == 0)
            reason = "No reason.";

        if (mentions.size() == 0) {
            try {
                String[] sections = rawMessage.split(" ", 2);
                if (sections.length == 0) {
                    unknownUsageMessage(message);
                    return;
                }
                long uid = ConversionUtils.parseUser(sections[0]);
                if (uid == -2) {
                    unknownUsageMessage(message);
                    return;
                }
                User user = message.getJDA().getUserById(uid);
                if (user == null) {
                    unknownUsageMessage(message);
                    return;
                }

                if (user.getIdLong() == message.getJDA().getSelfUser().getIdLong()) {
                    message.getChannel().sendMessage("I can't ban/kick myself. If you really want me to leave, please remove me manually.").queue();
                    return;
                } else if (user.getIdLong() == message.getAuthor().getIdLong()) {

                    message.getChannel().sendMessage("You can't ban/kick yourself.").queue();

                    return;
                }
                String[] cache = reason.split(" ", 2);
                if (cache.length != 2)
                    reason = "No reason.";
                else {
                    if (cache[1].replace(" ", "").isEmpty()) reason = "No reason.";
                    else reason = cache[1];

                }
                Member member = message.getGuild().getMember(user);
                boolean result = safeAccept(handleUser, new InternalDataForwarder(user, member, reason), message);
                handleResult(result, message);
            } catch (Exception e) {
                unknownUsageMessage(message);
            }
        } else {
            int count = mentions.size();
            if (count > 1) {
                message.getChannel().sendMessage("mass banning/kicking isn't supported for security reasons.").queue();

                return;
            }
            Member member = mentions.get(0);
            if (member.getUser().getIdLong() == message.getJDA().getSelfUser().getIdLong()) {
                message.getChannel().sendMessage("I can't ban/kick myself. If you really want me to leave, please remove me manually.").queue();
                return;
            } else if (member.getUser().getIdLong() == message.getAuthor().getIdLong()) {

                message.getChannel().sendMessage("You can't ban/kick yourself.").queue();

                return;
            }

            boolean result = safeAccept(handleUser, new InternalDataForwarder(member.getUser(), member, reason), message);
            handleResult(result, message);
        }
    }

    /**
     * Tiny one-line method to unify a repeated message.
     */
    private static void unknownUsageMessage(Message message) {
        message.getChannel().sendMessage("specify who to ban with either a mention, or their UID").queue();

    }

    /**
     * Handles the result of a ban, kick, or unban, sends a message that has a timer, and deletes the original message
     * that triggered the command. This is to keep chat clean.
     */
    private static void handleResult(boolean result, Message message) {
        if (result)
            message.getChannel().sendMessage("successfully completed the action.").queue(msg -> ExtensionsKt.scheduleDeletion(msg, 10000));

        else
            message.getChannel().sendMessage("failed to complete the action.").queue(msg -> ExtensionsKt.scheduleDeletion(msg, 10000));

        message.delete().queue();
    }

    /**
     * Utility method for accepting a Factory2&lt;Boolean, InternalDataForwarder, Message&gt;, and handling
     * any failure.
     */
    private static boolean safeAccept(Factory2<Boolean, InternalDataForwarder, Message> fun, InternalDataForwarder data, Message message) {

        try {
            return fun.accept(data, message);
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            /*
             * The Discord role hirearchy prevents lower users from banning those above them. That results in a problem
             * for the bot, if the bot role is under any member roles. Because the message may not make sense for
             * users of the command, this if-statement checks if the message contains "higher position" (in an attempt
             * to match the message "Attempt to interact with user of equal or higher position in role hierarchy"),
             * and then sends a more detailed message of what went wrong, and how to fix it.
             *
             * This will likely only be called for kick an banned; when they're banned they obviously don't have a role,
             * which means any position for the bot role in the role hierarchy would successfully unban the user.
             *
             */
            if (errorMessage.contains("higher position")) {
                message.getChannel().sendMessage("Seems like I couldn't ban them: `" + errorMessage + "`. My highest role needs to be above" +
                        " the role of the person you're trying to ban. So if it's i.e. a member, please move my role up" +
                        " before attempting to use this command again.").queue();
                return false;
            }
            message.getChannel().sendMessage("Failed: " + e.getMessage()).queue();

            CrashHandler.error(e);
            return false;
        }
    }

    /**
     * Bans a user with a specified reason
     */
    public static boolean banHandler(InternalDataForwarder data, Message message) {

        message.getGuild().getController().ban(data.user, 7, data.reason).queue(
                v -> audit(message.getGuild(), createEmbedLog("Ban", data, message.getAuthor())),
                err -> message.getChannel().sendMessage("Failed to ban the user: " + err.getMessage()).queue());

        return true;
    }

    /**
     * Kicks a user with a specified reason
     */
    public static boolean kickHandler(InternalDataForwarder data, Message message) {
        Member member = message.getGuild().getMember(data.user);
        if (member == null) {
            message.getChannel().sendMessage("The member is null; I can't kick them.").queue();
            return false;
        }
        if (data.member == null) {
            message.getChannel().sendMessage("I failed to find that member; are they in the server?").queue();
            return false;
        }
        message.getGuild().getController().kick(data.member, data.reason).queue();
        audit(message.getGuild(), createEmbedLog("Kick", data, message.getAuthor()));
        return true;
    }

    /**
     * Note that the API doesn't care about unbanning reasons; only kick and ban has a reason field in it. The reason
     * for unbanning is only posted in chat by the bot.
     */
    public static boolean unbanHandler(InternalDataForwarder data, Message message) {
        message.getGuild().getController().unban(data.user).queue();
        audit(message.getGuild(), createEmbedLog("Unban", data, message.getAuthor()));

        return true;
    }

    private static MessageEmbed createEmbedLog(String mode, InternalDataForwarder forwarder, User handler) {
        return new EmbedBuilder()
                .setTitle(mode)
                .setDescription("**User:** " + forwarder.getName() + " (UID: " + forwarder.getUser().getIdLong() + ")\n")
                .appendDescription("**Moderator:** " + handler.getName() + "#" + handler.getDiscriminator() + " (UID " + handler.getIdLong() + ")\n")
                .appendDescription("**Reason:** " + forwarder.reason)
                .build();
    }

    private static void audit(Guild guild, String message) {
        ModGuild modGuild = getInstance().bot.getModerator().getGuild(guild);
        if (modGuild == null)
            return;

        modGuild.audit(message);
    }

    private static void audit(Guild guild, MessageEmbed message) {
        ModGuild modGuild = getInstance().bot.getModerator().getGuild(guild);
        if (modGuild == null)
            return;

        modGuild.audit(message);
    }

    /**
     * This <b>should not be used outside this class.</b> It's public to access the functions defined earlier (see {@link ModUtils#banHandler(InternalDataForwarder, Message)},
     * {@link ModUtils#kickHandler(InternalDataForwarder, Message)}, and {@link ModUtils#unbanHandler(InternalDataForwarder, Message)}).
     */
    public static final class InternalDataForwarder {
        private @NotNull User user;
        private String reason;
        private @Nullable Member member;

        public InternalDataForwarder(@NotNull User user, @Nullable Member member, String reason) {
            this.user = user;
            this.member = member;

            this.reason = reason;
        }

        public String getName() {
            return user.getName() + "#" + user.getDiscriminator();
        }

        @NotNull
        public User getUser() {
            return user;
        }

        @Nullable
        public Member getMember() {
            return member;
        }

        public long getUid() {
            return user.getIdLong();
        }
    }

}
