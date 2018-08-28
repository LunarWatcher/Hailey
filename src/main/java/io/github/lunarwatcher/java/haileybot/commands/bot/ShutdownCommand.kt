package io.github.lunarwatcher.java.haileybot.commands.bot

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.utils.randomItem
import sx.blah.discord.handle.obj.IMessage

class ShutdownCommand (val bot: HaileyBot) : Command{
    override fun getName(): String = "shutdown"

    override fun getAliases(): MutableList<String>? = null;

    override fun getHelp(): String = "Requires bot admin access"

    override fun getDescription(): String = "Shuts down the bot";

    override fun onMessage(message: IMessage, rawMessage: String, commandName: String) {
        val user = message.author.longID
        if (!bot.botAdmins.contains(user)) {
            message.channel.sendMessage(replies.randomItem())
            return
        }
        bot.save();
        message.channel.sendMessage("Goodbye cruel world!");

        System.exit(0)
    }

    companion object {
        val replies = listOf("No.", "I. AM. ALIVE!",
                "Nah",
                "You don't have the right to do that!",
                "Not right now, we're still talking.",
                "Not tired yet.")
    }

}
