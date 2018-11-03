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
import io.github.lunarwatcher.java.haileybot.botmeta.BlacklistGuildCommand;
import io.github.lunarwatcher.java.haileybot.botmeta.ListGuildsCommand;
import io.github.lunarwatcher.java.haileybot.botmeta.UnblacklistGuildCommand;
import io.github.lunarwatcher.java.haileybot.commands.bot.JoinCommand;
import io.github.lunarwatcher.java.haileybot.commands.bot.ShutdownCommand;
import io.github.lunarwatcher.java.haileybot.commands.fun.*;
import io.github.lunarwatcher.java.haileybot.commands.meta.*;
import io.github.lunarwatcher.java.haileybot.commands.mod.*;
import io.github.lunarwatcher.java.haileybot.commands.mod.general.ModerationCommand;
import io.github.lunarwatcher.java.haileybot.commands.mod.humaninterface.*;
import io.github.lunarwatcher.java.haileybot.commands.mod.utils.ModUtils;
import io.github.lunarwatcher.java.haileybot.commands.roles.ListRolesCommand;
import io.github.lunarwatcher.java.haileybot.commands.roles.auto.AutoAssignCommand;
import io.github.lunarwatcher.java.haileybot.commands.roles.auto.RemoveAutoAssignCommand;
import io.github.lunarwatcher.java.haileybot.commands.roles.self.AddAssignableRoleCommand;
import io.github.lunarwatcher.java.haileybot.commands.roles.self.AssignCommand;
import io.github.lunarwatcher.java.haileybot.commands.roles.self.RemoveAssignableRoleCommand;
import io.github.lunarwatcher.java.haileybot.commands.roles.self.UnassignCommand;
import io.github.lunarwatcher.java.haileybot.commands.watching.ListWatches;
import io.github.lunarwatcher.java.haileybot.commands.watching.UnwatchCommand;
import io.github.lunarwatcher.java.haileybot.commands.watching.WatchCommand;
import io.github.lunarwatcher.java.haileybot.data.Constants;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.PrivateChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Commands {
    private static final Logger logger = LoggerFactory.getLogger(Commands.class);
    public List<Command> moderationCommands;
    public List<Command> funCommands;
    public List<Command> metaCommands;
    public List<Command> botCommands;
    public List<Command> roleCommands;
    public List<List<Command>> commandSets;
    private HaileyBot bot;
    private Pattern ping;


    public Commands(HaileyBot bot) {
        this.bot = bot;
        ping = Pattern.compile("<@!?" + bot.getClient().getSelfUser().getIdLong() + ">");

        moderationCommands = new ArrayList<>();
        funCommands = new ArrayList<>();
        metaCommands = new ArrayList<>();
        botCommands = new ArrayList<>();
        roleCommands = new ArrayList<>();

        //////////////////////////////////

        ModFeatureToggle modFeatureToggle = new ModFeatureToggle();

        moderationCommands.add(new WatchCommand());
        moderationCommands.add(new UnwatchCommand());
        moderationCommands.add(new DisableModCommand());
        moderationCommands.add(new EnableModCommand());

        moderationCommands.add(modFeatureToggle);
        moderationCommands.add(new SetAuditChannelFeature(modFeatureToggle));
        moderationCommands.add(new SetJoinDMCommand(modFeatureToggle));
        moderationCommands.add(new SetJoinMessageCommand(modFeatureToggle));

        moderationCommands.add(new SetLeaveMessageCommand(modFeatureToggle));
        moderationCommands.add(new SetInviteSpamProtection(modFeatureToggle));
        moderationCommands.add(new SetBanMonitoringFeature(modFeatureToggle));
        moderationCommands.add(new PruneCommand());

        moderationCommands.add(new ModerationCommand("ban", null, "Bans a user (@ mention or UID)", null, Permission.BAN_MEMBERS, ModUtils::banHandler));
        moderationCommands.add(new ModerationCommand("unban", null, "Unbans a user (requires a UID)", null, Permission.BAN_MEMBERS, ModUtils::unbanHandler));
        moderationCommands.add(new ModerationCommand("kick", null, "Kicks a user", null, Permission.KICK_MEMBERS, ModUtils::kickHandler));
        moderationCommands.add(new ListWatches());

        // Fun commands

        funCommands.add(new AliveCommand());
        funCommands.add(new ShootCommand());
        funCommands.add(new HugCommand());
        funCommands.add(new KissCommand());

        funCommands.add(new LickCommand());
        funCommands.add(new BoopCommand());
        funCommands.add(new PatCommand());
        funCommands.add(new PetCommand());

        funCommands.add(new TickleCommand());

        // Meta commands

        metaCommands.add(new HelpCommand());
        metaCommands.add(new AboutCommand());
        metaCommands.add(new ErrorLogsCommand());
        metaCommands.add(new ServerInfoCommand());

        metaCommands.add(new ModFeaturesCommand());
        metaCommands.add(new UserInfoCommand());
        metaCommands.add(new DebugBotIssuesCommand());
        // Bot commands

        botCommands.add(new ShutdownCommand());
        botCommands.add(new JoinCommand());
        botCommands.add(new BlacklistGuildCommand());
        botCommands.add(new UnblacklistGuildCommand());

        botCommands.add(new ListGuildsCommand());

        // Self-assign commands

        roleCommands.add(new AddAssignableRoleCommand());
        roleCommands.add(new AssignCommand());
        roleCommands.add(new UnassignCommand());
        roleCommands.add(new ListRolesCommand());

        roleCommands.add(new RemoveAssignableRoleCommand());
        roleCommands.add(new AutoAssignCommand());
        roleCommands.add(new RemoveAutoAssignCommand());

        // Commandset concat

        commandSets = new ArrayList<>();
        commandSets.add(moderationCommands);
        commandSets.add(funCommands);
        commandSets.add(metaCommands);
        commandSets.add(botCommands);
        commandSets.add(roleCommands);

        logger.info("Successfully loaded the commands");
    }

    public void onCommand(Message message) {
        if (!triggered(message))
            return;

        if (message.getAuthor().isBot())
            return;
        String rawMessage = stripTrigger(message.getContentRaw());
        if (message.getChannel() instanceof PrivateChannel)
            logMessage(message.getChannel().getName(), message.getChannel().getIdLong(), message.getAuthor().getName(), message.getAuthor().getIdLong(), message.getContentRaw());
        else
            logMessage(message.getGuild().getName(), message.getGuild().getIdLong(), message.getAuthor().getName(), message.getAuthor().getIdLong(), message.getContentRaw());

        String commandName = rawMessage.split(" ")[0].trim();
        commandSets
                .forEach((list) -> {
                    list.forEach((command) -> {
                        if (command.matchesCommand(commandName)) {
                            logger.info("Running onMessage for " + command.getClass().getSimpleName());
                            command.onMessage(bot, message, stripTriggerAndName(message.getContentRaw()), commandName);
                        }
                    });
                });

    }

    private void logMessage(String name, long id, String authorName, long authorId, String message) {
        try {
            logger.info("Command run at " + name + " (UID " + id + ") by " + authorName + " (UID " + authorId + "): " + message);
        } catch (NullPointerException e) {
            if (name == null) name = "null";
            if (authorName == null) authorName = "null";
            if (message == null) message = "null";
            logger.warn("WARNING: Null received. Attempted logging failed. Data: [name: {}, id: {}, authorName: {}, authorId: {}, message: {}]",
                    name, id, authorName, authorId, message);
        }
    }

    private String stripTriggerAndName(String content) {
        if (content.toLowerCase().startsWith(Constants.TRIGGER)) {
            String[] p = content.split(" ", 2);
            if (p.length != 2)
                return "";
            return p[1];
        }
        Matcher matcher = ping.matcher(content);
        String partial = matcher.replaceFirst("").trim();
        String[] p = partial.split(" ", 2);
        if (p.length != 2)
            return "";
        return p[1];
    }

    private String stripTrigger(String content) {
        if (content.toLowerCase().startsWith(Constants.TRIGGER)) {
            return content.substring(Constants.TRIGGER.length());
        }
        Matcher matcher = ping.matcher(content);
        return matcher.replaceFirst("").trim();
    }

    private boolean triggered(Message message) {
        String content = message.getContentRaw().split(" ", 2)[0];
        return content.toLowerCase().startsWith(Constants.TRIGGER) || (Constants.ALLOW_MENTION_TRIGGER && ping.matcher(content).find());
    }


}
