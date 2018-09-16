package io.github.lunarwatcher.java.haileybot.commands.mod

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.utils.canUserRunAdminCommand
import org.slf4j.LoggerFactory
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IPrivateChannel

class WatchCommand(val bot: HaileyBot) : Command{
    override fun getName(): String = "watch"
    override fun getAliases(): MutableList<String>? = null;
    override fun getHelp(): String = "Usage: `watch [channel/here] [regex/all]`. Used without brackets"
    override fun getDescription(): String = "Pings you when a pattern is detected in chat."

    override fun onMessage(message: IMessage, rawMessage: String, commandName: String?) {
        if(message.channel is IPrivateChannel){
            message.channel.sendMessage("This is a DM channel. No mod tools available.");
            return;
        }
        if (!message.canUserRunAdminCommand(bot)) {
            message.reply("You need to be a bot admin or have the administrator permission to do that.")
            return
        }
        if (rawMessage == ".*")
            return

        val pieces = rawMessage.split(" ".toRegex(), 3).toTypedArray()
        if (pieces.size != 2) {
            message.reply("You need 2 arguments: channel and regex. Write \"here\" (without quotes) to use this channel")
        } else {
            val channel = pieces[0]
            val regex = pieces[1]

            val user = message.author.longID
            val lChannel: Long

            if (channel.matches("<#\\d+>".toRegex())) {
                try {
                    lChannel = java.lang.Long.parseLong(channel.substring(2, channel.length - 1))

                } catch (e: NumberFormatException) {
                    message.reply("Failed to parse channel ID")
                    return
                }

            } else if (channel.matches("\\d+".toRegex())) {
                try {
                    lChannel = java.lang.Long.parseLong(channel)

                } catch (e: NumberFormatException) {
                    message.reply("Failed to parse channel ID")
                    return
                }

            } else if (channel.equals("here", ignoreCase = true)) {
                lChannel = message.channel.longID
            } else {
                message.reply("Invalid channel")
                return
            }


            if (lChannel != -1L) {
                val result = bot.matcher.watch(user, lChannel, regex)

                if (!result)
                    message.reply("Compiling failed. Check your regex")
                else
                    message.reply("Watched!")
            } else {
                message.reply("Channel not found")
            }

        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(WatchCommand::class.java)
    }
}

class UnwatchCommand(val bot: HaileyBot) : Command {
    override fun getName(): String = "unwatch"
    override fun getAliases(): MutableList<String>? = null;
    override fun getHelp(): String = "Usage: `unwatch [channel/here] [regex/all]`. Used without brackets"
    override fun getDescription(): String = "Unwatches any regex registered regex notifications"

    override fun onMessage(message: IMessage, rawMessage: String, commandName: String?) {
        if(message.channel is IPrivateChannel){
            message.channel.sendMessage("This is a DM channel. No mod tools available.");
            return;
        }
        val pieces = rawMessage.split(" ".toRegex(), 3).toTypedArray()

        if (pieces.size != 2) {
            message.reply("You need 2 arguments: channel and regex. Write \"here\" (without quotes) to use this channel")
        } else {
            val channel = pieces[0]
            val regex = pieces[1]

            val user = message.author.longID
            var lChannel: Long

            if (channel.matches("<#\\d+>".toRegex())) {
                try {
                    lChannel = java.lang.Long.parseLong(channel.substring(2, channel.length - 1))

                } catch (e: NumberFormatException) {
                    message.reply("Failed to parse channel ID")
                    return
                }

            } else if (channel.matches("\\d+".toRegex())) {
                try {
                    lChannel = java.lang.Long.parseLong(channel)

                } catch (e: NumberFormatException) {
                    message.reply("Failed to parse channel ID")
                    return
                }

            } else if (channel.equals("here", ignoreCase = true)) {
                lChannel = message.channel.longID
            } else {
                message.reply("Invalid channel")
                return
            }


            if (lChannel != -1L) {
                val result = bot.getMatcher().unwatch(user, lChannel, regex)

                if (!result)
                    message.reply("You weren't watching that.")
                else
                    message.reply("Unwatched!")
            } else {
                message.reply("Channel not found")
            }

        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UnwatchCommand::class.java)
    }

}
