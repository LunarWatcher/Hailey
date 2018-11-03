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

package io.github.lunarwatcher.java.haileybot.commands.mod.humaninterface

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.commands.Moderator.*
import io.github.lunarwatcher.java.haileybot.commands.mod.ModFeatureToggle
import io.github.lunarwatcher.java.haileybot.data.Constants
import io.github.lunarwatcher.java.haileybot.utils.canUserRunAdminCommand
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.PrivateChannel
import org.jetbrains.annotations.NotNull

const val messageRemoval = "Messages can be removed by writing `null`. This will also remove channels" +
        " where applicable.";

abstract class HumanlyUnderstandableFeatureToggleCommand(private val cmdName: String, private val cmdDescription: String,
                                                         private val cmdHelp: String, private val cmdAliases: List<String>,
                                                         val toggleCommand: ModFeatureToggle,
                                                         vararg val features: String) : Command {
    override fun getAliases(): List<String> = cmdAliases
    override fun getName(): String = cmdName
    override fun getDescription(): String? = cmdDescription
    override fun getHelp(): String? = cmdHelp

    override fun onMessage(bot: HaileyBot, message: @NotNull Message, rawMessage: String, commandName: String) {
        if (message.channel is PrivateChannel) {
            message.channel.sendMessage("This is a DM channel. No mod tools available.").queue();
            return;
        }
        if (!message.canUserRunAdminCommand(bot)) {
            message.getChannel().sendMessage("You need to be a bot admin or have the administrator permission to do that.").queue();
            return
        }

        if (!bot.moderator.isGuildEnabled(message.guild)) {
            message.channel.sendMessage("Guild moderation not enabled. Please run `${Constants.TRIGGER}enableMod` to enable it.").queue()
            return
        }

        if (rawMessage.equals("null", true)) {
            for (i in 0 until features.size) {
                val featureName = features[i];
                toggleCommand.onMessage(bot, message, "$featureName null", commandName);

            }
            return;
        }
        val splitBySpace = rawMessage.split(" ", limit = features.size);
        if (splitBySpace.size != features.size) {
            message.channel.sendMessage("You seem to be missing one or more arguments. See the help command for more information on required arguments").queue()
            return;
        }
        for (i in 0 until features.size) {
            val featureName = features[i];
            val arguments = splitBySpace[i]
            toggleCommand.onMessage(bot, message, "$featureName $arguments", commandName);

        }
    }
}

class SetJoinMessageCommand(toggleCommand: ModFeatureToggle)
    : HumanlyUnderstandableFeatureToggleCommand("setJoinMessage", "Sets the join message, which is sent in a public channel when a new user joins. This method " +
        "supports some arguments: <members> (raw number containing the amount of members. I.e. the 75th member to join will get the number `75` added where this is). <nthmember> adds `75th` instead, " +
        "which is a tiny difference between teh two. There's also <user> (which adds the username with the discriminator), and <server>, which adds the server name.",
        "The command takes two arguments: `[#channel] [join message]` (without brackets). $messageRemoval", listOf(),
        toggleCommand, WELCOME_LOGGING, JOIN_MESSAGE);

class SetLeaveMessageCommand(toggleCommand: ModFeatureToggle)
    : HumanlyUnderstandableFeatureToggleCommand("setLeaveMessage", "Sets the leave message, which is sent in a public channel when a user leaves",
        "The command takes two arguments: `[#channel] [leave message]` (without brackets).  $messageRemoval", listOf(),
        toggleCommand, LEAVE_LOGGING, LEAVE_MESSAGE);

class SetJoinDMCommand(toggleCommand: ModFeatureToggle)
    : HumanlyUnderstandableFeatureToggleCommand("setJoinDm", "Sets the join DM, which is sent to new users when they join",
        "The command takes one argument: `[join message]` (without brackets). The message supports three varying arguments: " +
                "<user> (for the username), <server> (for the server name), and <members> for how many members are in the server. Note that <members> is the raw number; " +
                "if there are 74 members in the server, this adds `75` in the message to the 75th member to join. However, <nthmember> adds `75th` to the message for the 75th member. $messageRemoval",
        listOf(),
        toggleCommand, JOIN_DM);

class SetInviteSpamProtection(toggleCommand: ModFeatureToggle)
    : HumanlyUnderstandableFeatureToggleCommand("setInviteUsernameSpamProtection", "Toggles invite spam protection",
        "The command takes one argument: `[enabled/disabled]` (without brackets, and only one of the items).", listOf(),
        toggleCommand, INVITE_FEATURE);

class SetAuditChannelFeature(toggleCommand: ModFeatureToggle)
    : HumanlyUnderstandableFeatureToggleCommand("setAuditChannel", "Sets the audit channel, which is where any moderation logs get posted.",
        "The command takes one argument: `[#channel]` (without brackets). Pass `null` as the argument to this command to remove the channel. ", listOf(),
        toggleCommand, AUDIT_FEATURE);

class SetBanMonitoringFeature(toggleCommand: ModFeatureToggle)
    : HumanlyUnderstandableFeatureToggleCommand("setBanMonitoring", "Enables/disables ban monitoring. Requires a boolean input",
        "Takes one argument: `[boolean]` (without brackets. `enabled`/`disabled` is also accepted)",
        listOf(), toggleCommand, BAN_MONITORING_FEATURE)
