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

@file:Suppress("IMPLICIT_CAST_TO_ANY")

package io.github.lunarwatcher.java.haileybot.commands.mod

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.data.Constants
import io.github.lunarwatcher.java.haileybot.utils.canUserRunAdminCommand
import io.github.lunarwatcher.java.haileybot.utils.scheduleDeletion
import org.jetbrains.annotations.NotNull
import org.slf4j.LoggerFactory
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IPrivateChannel
import sx.blah.discord.handle.obj.Permissions
import sx.blah.discord.util.EmbedBuilder
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer

class PruneCommand : Command {

    override fun getName(): String {
        return "prune"
    }

    @Suppress("RedundantCompanionReference")
    override fun getAliases() = Companion.aliases

    override fun getHelp(): String? {
        return "Command usage: `${Constants.TRIGGER}prune [messages (int)] [optional reason (string)]`. Run without brackets, with a matching type. The amount of messages should match the wanted deletion count: 1 is added to the count to compensate for the current message."
    }

    override fun getDescription() = "Deletes some recent messages (defined by command usage)"

    override fun onMessage(bot: HaileyBot, message: @NotNull IMessage, rawMessage: String, commandName: String) {
        if (message.channel is IPrivateChannel) {
            RequestBuffer.request {
                message.channel.sendMessage("This is a DM channel. No mod tools available.")
            };
            return;
        }
        if (!message.canUserRunAdminCommand(bot, Permissions.MANAGE_MESSAGES)) {
            RequestBuffer.request {
                message.channel.sendMessage("You can't run that. You need to have the \"manage messages\" or \"administrator\" permission to do that.")
            };
            return;
        }
        val guild = bot.moderator.getGuild(message.guild.longID)
        if (guild == null) {
            RequestBuffer.request {
                message.channel.sendMessage("Please run `${Constants.TRIGGER}enableMod` before using this command.")
            };
            return;
        }
        val data = rawMessage.split(" ", limit = 2)
        val count = if (data.isEmpty()) null else data[0].toIntOrNull()
        if (count == null) {
            RequestBuffer.request {
                message.channel.sendMessage("That's not a valid number.")
            }
            return;
        }
        val deletionCount = count + 1

        val reason = if (data.size == 2) data[1] else "None"

        val title = "Bulk deletion"

        val messageHistory = message.channel.getMessageHistory(deletionCount)
        try {

            messageHistory.bulkDelete()
        } catch (e: MissingPermissionsException) {
            RequestBuffer.request {
                message.channel.sendMessage("I do not have the appropriate permissions to delete messages. Unfortunately, due to a bug in the API I use, the \"manage messages\" permission needs to be explcitly declared.")
            }
            return;
        }
        message.reply("deleted ${messageHistory.size - 1} messages. \uD83D\uDC3A")
                ?.scheduleDeletion(10000);
        val deletedCount = messageHistory.size - 1

        val description = """**Message count:** $deletedCount
            **Channel:** ${message.channel}
            **Deleter:** ${message.author.name}#${message.author.discriminator} (UID ${message.author.longID})
            **Reason:** $reason
        """.trimIndent()
        val embed = EmbedBuilder()
                .withTitle(title)
                .withColor(message.author.getColorForGuild(message.guild))
                .withDescription(description)
                .build()
        guild.audit(embed)
    }


    companion object {
        private val logger = LoggerFactory.getLogger(PruneCommand::class.java)

        private val aliases = listOf("purge", "delete", "deleteMessages")
    }
}
