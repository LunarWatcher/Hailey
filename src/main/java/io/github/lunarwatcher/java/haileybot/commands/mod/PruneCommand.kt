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
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.PrivateChannel
import net.dv8tion.jda.core.entities.TextChannel
import org.jetbrains.annotations.NotNull
import org.slf4j.LoggerFactory

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

    override fun onMessage(bot: HaileyBot, message: @NotNull Message, rawMessage: String, commandName: String) {
        if (message.channel is PrivateChannel) {
            message.channel.sendMessage("This is a DM channel. No mod tools available.").queue()
            return;
        } else if (message.channel !is TextChannel) {
            return;
        }
        if (!message.canUserRunAdminCommand(bot, Permission.MESSAGE_MANAGE)) {
            message.channel.sendMessage("You can't run that. You need to have the \"manage messages\" or \"administrator\" permission to do that.").queue()

            return;
        }
        val guild = bot.moderator.getGuild(message.guild.idLong)
        if (guild == null) {
            message.channel.sendMessage("Please run `${Constants.TRIGGER}enableMod` before using this command.").queue()

            return;
        }
        val data = rawMessage.split(" ", limit = 2)
        val count = if (data.isEmpty()) null else data[0].toIntOrNull()
        if (count == null) {
            message.channel.sendMessage("That's not a valid number.").queue()

            return;
        } else if (count <= 1) {
            message.channel.sendMessage("You have to delete more than one message.").queue()
            return;
        }

        val reason = if (data.size == 2) data[1] else "None"

        val title = "Bulk deletion"

        message.channel.getHistoryBefore(message, count).queue({ messageHistory ->
            try {

                (message.channel as TextChannel).deleteMessages(messageHistory.retrievedHistory).queue({ _ ->
                    message.channel.sendMessage("<@${message.author.idLong}>, deleted ${messageHistory.size()} messages. \uD83D\uDC3A").queue { msg ->
                        msg.scheduleDeletion(10000)
                    }

                    val deletedCount = messageHistory.size()

                    val description = """**Message count:** $deletedCount
                                **Channel:** <#${message.channel.id}>
                                **Deleter:** ${message.author.name}#${message.author.discriminator} (UID ${message.author.idLong})
                                **Reason:** $reason""".trimIndent()
                    message.delete().queue();
                    val embed = EmbedBuilder()
                            .setTitle(title)
                            .setColor(message.member.color)
                            .setDescription(description)
                            .build()
                    guild.audit(embed)
                }, {
                    message.channel.sendMessage("<@${message.author.idLong}>, I could not delete the messages; an internal error occured: ${it.message}").queue()
                })
            } catch (e: Exception) {
                message.channel.sendMessage("Something bad happened when attempting to bulk delete. The exception is: " + e.message).queue()

                return@queue;
            }

        }, { err ->
            message.channel.sendMessage("Failed to bulk delete messages: " + err.message).queue();
        });
    }


    companion object {
        private val logger = LoggerFactory.getLogger(PruneCommand::class.java)

        private val aliases = listOf("purge", "delete", "deleteMessages")
    }
}
