package io.github.lunarwatcher.java.haileybot.commands.`fun`

import com.vdurmont.emoji.Emoji
import com.vdurmont.emoji.EmojiLoader
import com.vdurmont.emoji.EmojiParser
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.utils.messageFormat
import io.github.lunarwatcher.java.haileybot.utils.randomItem
import sx.blah.discord.handle.impl.obj.EmojiImpl
import sx.blah.discord.handle.impl.obj.ReactionEmoji
import sx.blah.discord.handle.obj.IEmoji
import sx.blah.discord.handle.obj.IMessage

class AliveCommand : Command{
    override fun getName(): String = "alive"

    override fun getAliases(): MutableList<String>? = null

    override fun getHelp(): String = "Checks if the bot is alive"

    override fun getDescription(): String = help

    override fun onMessage(message: IMessage, rawMessage: String?, commandName: String?) {
        message.channel.sendMessage(replies.randomItem())
    }

    companion object {
        val replies = listOf(
                "No, I'm dead",
                "Lurking around the guilds",
                "Depends on how you define it",
                "Maybe",
                "Just went to grab some coffee",
                "Did I miss something?"
                )
    }
}

abstract class ActionCommand(val replies: List<String>, val emojis: List<String>, val onEmptyMessage: (IMessage) -> Unit) : Command{
    override fun onMessage(message: IMessage, rawMessage: String, commandName: String) {
        if(rawMessage.isEmpty()){
            onEmptyMessage.invoke(message);
            return;
        }
        val theOtherOne = if(rawMessage.contains(genericPingRegex)){
            val pings = genericPingRegex.findAll(rawMessage);

            val stringIds = pings.map { it.groupValues[1] }.toList()

            if(stringIds.isEmpty()){
                rawMessage
            }else if(stringIds.size == 1) {
                val id = stringIds[0].toLongOrNull()
                if (id != null){
                    if (id == message.author.longID || id == message.client.ourUser.longID) {
                        onMessage(message, "", commandName)
                        return;
                    } else
                        message.client.getUserByID(id)?.getDisplayName(message.guild) ?: rawMessage
                }else
                    rawMessage
            }else {
                val res = StringBuilder()
                var index = -1;
                var skipped = 0;
                for (ping in stringIds) {
                    index++;

                    val id = ping.toLongOrNull()
                    if (id != null) {
                        if (id == message.author.longID || id == message.client.ourUser.longID) {
                            skipped++;
                            if(skipped == stringIds.size){
                                onMessage(message, "", commandName)
                                return;
                            }
                            continue;
                        } else
                            res.append(message.client.getUserByID(id)?.getDisplayName(message.guild) + "").append(
                                    if(index == stringIds.size - 2) ", and " else if(index == stringIds.size - 1) "" else ", "
                            )
                    }

                }
                res.toString()
            }

        } else
            rawMessage

        message.channel.sendMessage("**${message.author.getDisplayName(message.guild)}** " + replies.randomItem().messageFormat(theOtherOne))
    }

    companion object {
        val genericPingRegex = "<@!?(\\d+)>".toRegex()
    }

}
class ShootCommand : ActionCommand(replies, listOf(), { message ->
    message.channel.sendMessage(self.messageFormat(message.author.getDisplayName(message.guild)))
    .addReaction(ReactionEmoji.of("\uD83C\uDDF7"));
}){
    override fun getName(): String = "shoot"
    override fun getAliases(): MutableList<String>? = null;
    override fun getHelp(): String? = "Shoots someone!!"
    override fun getDescription(): String? = help
    override fun onMessage(message: IMessage, rawMessage: String, commandName: String) {

        super.onMessage(message,rawMessage, commandName)
    }


    companion object {

        const val self = "**{0}** shot themselves! Press **R** to show respects."
        val replies = listOf(
                "shoots **{0}**. Any last words?",
                "emptied a mag in **{0}**'s head.",
                "shoots **{0}** down at noon.",
                "killed **{0}** by sending automated gun drones after them.",
                "bust a cap in **{0}**.",
                "sniped **{0}**. ***HEADSHOT!***"

        )
    }
}