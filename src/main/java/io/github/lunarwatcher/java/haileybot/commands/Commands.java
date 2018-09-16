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
import io.github.lunarwatcher.java.haileybot.commands.roles.*;
import io.github.lunarwatcher.java.haileybot.commands.roles.auto.AutoAssignCommand;
import io.github.lunarwatcher.java.haileybot.commands.roles.auto.RemoveAutoAssignCommand;
import io.github.lunarwatcher.java.haileybot.commands.roles.self.AddAssignableRoleCommand;
import io.github.lunarwatcher.java.haileybot.commands.roles.self.AssignCommand;
import io.github.lunarwatcher.java.haileybot.commands.roles.self.RemoveAssignableRoleCommand;
import io.github.lunarwatcher.java.haileybot.commands.roles.self.UnassignCommand;
import io.github.lunarwatcher.java.haileybot.data.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.Permissions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Commands {
    private static final Logger logger = LoggerFactory.getLogger(Commands.class);

    private HaileyBot bot;
    private Pattern ping;

    public List<Command> moderationCommands;
    public List<Command> funCommands;
    public List<Command> metaCommands;
    public List<Command> botCommands;
    public List<Command> roleCommands;

    public List<List<Command>> commandSets;


    public Commands(HaileyBot bot){
        this.bot = bot;
        ping = Pattern.compile("<@!?" + bot.getClient().getOurUser().getLongID() + ">");

        moderationCommands = new ArrayList<>();
        funCommands = new ArrayList<>();
        metaCommands = new ArrayList<>();
        botCommands = new ArrayList<>();
        roleCommands = new ArrayList<>();

        //////////////////////////////////

        ModFeatureToggle modFeatureToggle = new ModFeatureToggle(bot);

        moderationCommands.add(new WatchCommand(bot));
        moderationCommands.add(new UnwatchCommand(bot));
        moderationCommands.add(new DisableModCommand(bot));
        moderationCommands.add(new EnableModCommand(bot));

        moderationCommands.add(modFeatureToggle);
        moderationCommands.add(new SetAuditChannelFeature(bot, modFeatureToggle));
        moderationCommands.add(new SetJoinDMCommand(bot, modFeatureToggle));
        moderationCommands.add(new SetJoinMessageCommand(bot, modFeatureToggle));

        moderationCommands.add(new SetLeaveMessageCommand(bot, modFeatureToggle));
        moderationCommands.add(new SetInviteSpamProtection(bot, modFeatureToggle));
        moderationCommands.add(new SetBanMonitoringFeature(bot, modFeatureToggle));
        moderationCommands.add(new PruneCommand(bot));

        moderationCommands.add(new ModerationCommand("ban", null, "Bans a user (@ mention or UID)", null, Permissions.BAN, ModUtils::banHandler));
        moderationCommands.add(new ModerationCommand("unban", null, "Unbans a user (requires a UID)", null, Permissions.BAN, ModUtils::unbanHandler));
        moderationCommands.add(new ModerationCommand("kick", null, "Kicks a user", null, Permissions.KICK, ModUtils::kickHandler));

        // Fun commands

        funCommands.add(new AliveCommand());
        funCommands.add(new ShootCommand());
        funCommands.add(new HugCommand());
        funCommands.add(new KissCommand());

        funCommands.add(new LickCommand());
        funCommands.add(new BoopCommand());

        // Meta commands

        metaCommands.add(new HelpCommand(bot));
        metaCommands.add(new AboutCommand(bot));
        metaCommands.add(new ErrorLogsCommand(bot));
        metaCommands.add(new ServerInfoCommand(bot));

        metaCommands.add(new ModFeaturesCommand());
        metaCommands.add(new UserInfoCommand(bot));

        // Bot commands

        botCommands.add(new ShutdownCommand(bot));
        botCommands.add(new JoinCommand());
        botCommands.add(new BlacklistGuildCommand(bot));
        botCommands.add(new UnblacklistGuildCommand(bot));

        botCommands.add(new ListGuildsCommand(bot));

        // Self-assign commands

        roleCommands.add(new AddAssignableRoleCommand(bot));
        roleCommands.add(new AssignCommand(bot));
        roleCommands.add(new UnassignCommand(bot));
        roleCommands.add(new ListRolesCommand(bot));

        roleCommands.add(new RemoveAssignableRoleCommand(bot));
        roleCommands.add(new AutoAssignCommand(bot));
        roleCommands.add(new RemoveAutoAssignCommand(bot));

        // Commandset concat

        commandSets = new ArrayList<>();
        commandSets.add(moderationCommands);
        commandSets.add(funCommands);
        commandSets.add(metaCommands);
        commandSets.add(botCommands);
        commandSets.add(roleCommands);

    }

    public void onCommand(IMessage message){
        if(!triggered(message))
            return;

        if(message.getAuthor().isBot())
            return;
        String rawMessage = stripTrigger(message.getContent());
        if(message.getChannel() instanceof IPrivateChannel)
            logMessage(message.getChannel().getName(), message.getChannel().getLongID(), message.getAuthor().getName(), message.getAuthor().getLongID(), message.getContent());
        else
            logMessage(message.getGuild().getName(), message.getGuild().getLongID(), message.getAuthor().getName(), message.getAuthor().getLongID(), message.getContent());

        String commandName = rawMessage.split (" ")[0].trim();
        commandSets
                .forEach((list) ->{
                    list.forEach((command) -> {
                        if(command.matchesCommand(commandName)){
                            logger.info("Running onMessage for " + command.getClass().getSimpleName());
                            command.onMessage(message, stripTriggerAndName(message.getContent()), commandName);
                        }
                    });
                });

    }

    private void logMessage(String name, long id, String authorName, long authorId, String message){
        try {
            logger.info("Command run at " + name + " (UID " + id + ") by " + authorName + " (UID " + authorId + "): " + message);
        }catch(NullPointerException e){
            if(name == null) name = "null";
            if(authorName == null) authorName = "null";
            if(message == null) message = "null";
            logger.warn("WARNING: Null received. Attempted logging failed. Data: [name: {}, id: {}, authorName: {}, authorId: {}, message: {}]",
                    name, id, authorName, authorId, message);
        }
    }

    private String stripTriggerAndName(String content){
        if(content.toLowerCase().startsWith(Constants.TRIGGER)){
            String[] p = content.split (" ", 2);
            if(p.length != 2)
                return "";
            return p[1];
        }
        Matcher matcher = ping.matcher(content);
        String partial = matcher.replaceFirst("").trim();
        String[] p = partial.split(" ", 2);
        if(p.length != 2)
            return "";
        return p[1];
    }

    private String stripTrigger(String content){
        if(content.toLowerCase().startsWith(Constants.TRIGGER)){
            return content.substring(Constants.TRIGGER.length());
        }
        Matcher matcher = ping.matcher(content);
        return matcher.replaceFirst("").trim();
    }

    private boolean triggered (IMessage message){
        String content = message.getContent().split(" ", 2)[0];
        return content.toLowerCase().startsWith(Constants.TRIGGER) || (Constants.ALLOW_MENTION_TRIGGER && ping.matcher(content).find());
    }


}
