package io.github.lunarwatcher.java.haileybot.commands.mod.humaninterface

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.commands.Moderator.*
import io.github.lunarwatcher.java.haileybot.commands.mod.ModFeatureToggle
import io.github.lunarwatcher.java.haileybot.data.Constants
import io.github.lunarwatcher.java.haileybot.utils.canUserRunAdminCommand
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IPrivateChannel

abstract class HumanlyUnderstandableFeatureToggleCommand(private val cmdName: String, private val cmdDescription: String,
                                                         private val cmdHelp: String, private val cmdAliases: List<String>,
                                                         val toggleCommand: ModFeatureToggle, val bot: HaileyBot,
                                                         vararg val features: String) : Command {
    override fun getAliases(): List<String> = cmdAliases
    override fun getName(): String = cmdName
    override fun getDescription(): String? = cmdDescription
    override fun getHelp(): String? = cmdHelp

    override fun onMessage(message: IMessage, rawMessage: String, commandName: String) {
        if(message.channel is IPrivateChannel){
            message.channel.sendMessage("This is a DM channel. No mod tools available.");
            return;
        }
        if (!message.canUserRunAdminCommand(bot)) {
            message.reply("You need to be a bot admin or have the administrator permission to do that.")
            return
        }

        if (!bot.moderator.isGuildEnabled(message.guild)) {
            message.channel.sendMessage("Guild moderation not enabled. Please run `${Constants.TRIGGER}enableMod` to enable it.")
            return
        }
        val splitBySpace = rawMessage.split(" ", limit = features.size);
        if(splitBySpace.size != features.size){
            message.channel.sendMessage("You seem to be missing one or more arguments. See the help command for more information on required arguments")
            return;
        }
        for(i in 0 until features.size) {
            val featureName = features[i];
            val arguments = splitBySpace[i]
            toggleCommand.onMessage(message, "$featureName $arguments", commandName);

        }
    }
}

class SetJoinMessageCommand(bot: HaileyBot, toggleCommand: ModFeatureToggle)
    : HumanlyUnderstandableFeatureToggleCommand("setJoinMessage", "Sets the join message, which is sent in a public channel when a new user joins",
        "The command takes two arguments: `[#channel] [join message]` (without brackets).", listOf(),
        toggleCommand, bot, WELCOME_LOGGING, JOIN_MESSAGE);

class SetLeaveMessageCommand(bot: HaileyBot, toggleCommand: ModFeatureToggle)
    : HumanlyUnderstandableFeatureToggleCommand("setLeaveMessage", "Sets the leave message, which is sent in a public channel when a user leaves",
        "The command takes two arguments: `[#channel] [leave message]` (without brackets).", listOf(),
        toggleCommand, bot, LEAVE_LOGGING, LEAVE_MESSAGE);

class SetJoinDMCommand(bot: HaileyBot, toggleCommand: ModFeatureToggle)
    : HumanlyUnderstandableFeatureToggleCommand("setJoinDm", "Sets the join DM, which is sent to new users when they join",
        "The command takes one argument: `[join message]` (without brackets). The message supports three varying arguments: " +
                "<user> (for the username), <server> (for the server name), and <members> for how many members are in the server",
        listOf(),
        toggleCommand, bot, JOIN_DM);

class SetInviteSpamProtection(bot: HaileyBot, toggleCommand: ModFeatureToggle)
    : HumanlyUnderstandableFeatureToggleCommand("setInviteUsernameSpamProtection", "Toggles invite spam protection",
        "The command takes one argument: `[enabled/disabled]` (without brackets, and only one of the items).", listOf(),
        toggleCommand, bot, INVITE_FEATURE);

class SetAuditChannelFeature(bot: HaileyBot, toggleCommand: ModFeatureToggle)
    : HumanlyUnderstandableFeatureToggleCommand("setAuditChannel", "Sets the audit channel, which is where any moderation logs get posted.",
        "The command takes one argument: `[#channel]` (without brackets).", listOf(),
        toggleCommand, bot, AUDIT_FEATURE);
