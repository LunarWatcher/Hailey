package io.github.lunarwatcher.java.haileybot.commands.`fun`

import com.vdurmont.emoji.Emoji
import com.vdurmont.emoji.EmojiLoader
import com.vdurmont.emoji.EmojiParser
import io.github.lunarwatcher.java.haileybot.HaileyBot
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

        val result = message.mentions
                .filter { it.longID != message.client.ourUser.longID }
                .map{
                    it.name
                }.toHashSet()
                .joinToString(", ")
        message.channel.sendMessage("**${message.author.getDisplayName(message.guild)}** " + replies.randomItem()?.messageFormat(result) + " ${emojis.randomItem() ?: ""}")
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

class HugCommand : ActionCommand(replies, listOf(), { message ->
    message.channel.sendMessage(self.messageFormat(message.author.getDisplayName(message.guild)))
            .addReaction(ReactionEmoji.of("\uD83C\uDDF7"));
}){
    override fun getName(): String = "hug"
    override fun getAliases(): MutableList<String>? = null;
    override fun getHelp(): String? = "Hugs someone <3"
    override fun getDescription(): String? = help

    companion object {

        const val self = "**{0}** hugs themselves? Nope! *hugs **{0}***"
        val replies = listOf(
                "hugs **{0}**",
                "covers **{0}** in fluff <3",
                "warms **{0}** with hugs",
                "cuddles **{0}**"
        )
    }
}


