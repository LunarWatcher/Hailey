@file:Suppress("IMPLICIT_CAST_TO_ANY")

package io.github.lunarwatcher.java.haileybot.commands.mod

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.commands.mod.WatchCommand.Companion.logger
import io.github.lunarwatcher.java.haileybot.data.Constants
import io.github.lunarwatcher.java.haileybot.utils.ConversionUtils
import io.github.lunarwatcher.java.haileybot.utils.canUserRunAdminCommand
import io.github.lunarwatcher.java.haileybot.utils.scheduleDeletion
import org.apache.commons.lang3.ObjectUtils
import org.slf4j.LoggerFactory
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IPrivateChannel
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.util.EmbedBuilder

class PruneCommand(private val bot: HaileyBot) : Command {

    override fun getName(): String {
        return "prune"
    }

    override fun getAliases() = Companion.aliases

    override fun getHelp(): String? {
        return "Command usage: `${Constants.TRIGGER}prune [messages (int)] [optional user (long id)] [optional reason (string)]`. Run without brackets, with a matching type. The amount of messages should match the wanted deletion count: 1 is added to the count to compensate for the current message."
    }

    override fun getDescription() = "Deletes some recent messages (defined by command usage)"

    override fun onMessage(message: IMessage, rawMessage: String, commandName: String) {
        if(message.channel is IPrivateChannel){
            message.channel.sendMessage("This is a DM channel. No mod tools available.");
            return;
        }
        if (!message.canUserRunAdminCommand(bot)) {
            message.channel.sendMessage("You can't run that. You need to have the \"adminstrator\" permission to do that.");
            return;
        }
        val guild = bot.moderator.getGuild(message.guild.longID)
        if (guild == null) {
            message.channel.sendMessage("Please run `${Constants.TRIGGER}enableMod` before using this command.");
            return;
        }
        val data = rawMessage.split(" ", limit = 2)
        val count = if (data.size == 0) null else data[0].toIntOrNull()
        if (count == null) {
            message.channel.sendMessage("That's not a valid number.")
            return;
        }
        val deletionCount = count + 1

        val who = if (data.size == 2) {
            try {
                val uid = ConversionUtils.parseUser(data[1])
                if(uid == -2L) {
                    if (data[1].equals("bots", true)) {
                        "bots"
                    } else
                        null
                }else
                    message.guild.getUserByID(uid)
            } catch (e: Exception) {
                if (data[1].equals("bots", true)) {
                    "bots"
                } else
                    null
            }
        } else null

        val reason = if (who == null && data.size == 2) data[1] else if (data.size == 2 && who != null) {
            val reformattedData = rawMessage.split(" ", limit = 3);
            if (reformattedData.size == 3)
                reformattedData[2]
            else "None"
        } else "None"

        val title = if (who != null) {
            "Bulk deletion of user messages (WARNING: limited by the cache)"
        } else "Bulk deletion"

        val deletedCount = if (who != null && who is IUser) {
            val uid = who.longID
            val messageHistory = message.channel.getMessageHistory()
            var deleted = 0;
            for (msg in messageHistory) {
                if(deleted >= count)
                    break;
                if (msg.author.longID == uid && !msg.isDeleted) {
                    msg.delete()
                    deleted++;
                    logger.info("Message deletion: ${msg.longID} - ${msg.content} - ${msg.author}")
                }
            }
            message.reply("deleted $deleted messages. \uD83D\uDC3A")
                    ?.scheduleDeletion(10000);
            deleted;
        }else if(who != null && who is String){
            val messageHistory = message.channel.getMessageHistory()
            if(who == "bots") {
                var deleted = 0;
                for (msg in messageHistory) {
                    if(deleted >= count)
                        break;
                    if (msg.author.isBot && !msg.isDeleted){
                        msg.delete()
                        deleted++;
                        logger.info("Message deletion: ${msg.longID} - ${msg.content} - ${msg.author}")
                    }
                }

                message.reply("deleted $deleted messages. \uD83D\uDC3A")
                        ?.scheduleDeletion(10000);
                message.delete()
                deleted;
            }else{
                message.reply("you used an invalid mode: $who")
                return;
            }
        }else {
            val messageHistory = message.channel.getMessageHistory(deletionCount)
            messageHistory.bulkDelete()
            message.reply("deleted ${messageHistory.size - 1} messages. \uD83D\uDC3A")
                    ?.scheduleDeletion(10000);
            messageHistory.size - 1
        }
        val description = """**Message count:** $deletedCount
            **Deletion mode:** ${if (who != null && who is IUser) "User (" + who.name + "#" + who.discriminator + ")" else if (who != null && who == "bots") "Bot messages" else "Count"}
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
