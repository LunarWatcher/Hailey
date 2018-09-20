package io.github.lunarwatcher.java.haileybot.commands.mod

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.utils.canUserRunAdminCommand
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IPrivateChannel
import sx.blah.discord.handle.obj.Permissions

class EnableModCommand(val bot: HaileyBot) : Command{
    override fun getName(): String = "enableMod"
    override fun getAliases(): MutableList<String>? = null;
    override fun getHelp(): String = "Just run the command. You need to be an administrator, the owner of the server, or a bot admin to use it"
    override fun getDescription(): String = "Enables moderation features for this guild."

    override fun onMessage(message: IMessage, rawMessage: String?, commandName: String?) {
        if(message.channel is IPrivateChannel){
            message.channel.sendMessage("This is a DM channel. No mod tools available.");
            return;
        }
        if (!message.author.getPermissionsForGuild(message.guild).contains(Permissions.ADMINISTRATOR) &&
                !bot.botAdmins.contains(message.author.longID) &&
                message.author.longID != message.guild.ownerLongID) {
            message.reply("You need to be a bot admin or have the administrator permission to do that.")
            return
        }
        if (bot.moderator.registerGuild(message.guild)) {
            message.reply("Added guild to the list of moderation guilds")
        } else {
            message.reply("Already enabled.")
        }
    }

}

class DisableModCommand(val bot: HaileyBot) : Command{
    override fun getName(): String = "disableMod"
    override fun getAliases(): MutableList<String>? = null;
    override fun getHelp(): String = "Just run the command. You need to be an administrator, the owner of the server, or a bot admin to use it"
    override fun getDescription(): String = "Disables moderation features for this guild."

    override fun onMessage(message: IMessage, rawMessage: String?, commandName: String?) {
        if(message.channel is IPrivateChannel){
            message.channel.sendMessage("This is a DM channel. No mod tools available.");
            return;
        }

        val limit: Int = 90
        for(i in 0 until limit) {

        }
        if (!message.canUserRunAdminCommand(bot)) {
            message.reply("You need to be a bot admin or have the administrator permission to do that.")
            return
        }
        if (bot.moderator.removeGuild(message.guild)) {
            message.reply("Removed guild to the list of moderation guilds")
        } else {
            message.reply("Already disabled.")
        }
    }

}