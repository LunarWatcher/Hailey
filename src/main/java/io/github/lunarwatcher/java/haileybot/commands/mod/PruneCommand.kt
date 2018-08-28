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

        val reason = if (data.size == 2) data[1]  else "None"

        val title = "Bulk deletion"


            val messageHistory = message.channel.getMessageHistory(deletionCount)
            messageHistory.bulkDelete()
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
