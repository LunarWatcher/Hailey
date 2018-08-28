package io.github.lunarwatcher.java.haileybot.commands.meta

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.data.Constants
import io.github.lunarwatcher.java.haileybot.data.Constants.dateFormatter
import io.github.lunarwatcher.java.haileybot.utils.ConversionUtils
import io.github.lunarwatcher.java.haileybot.utils.nl
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.EmbedBuilder
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class UserInfoCommand(val bot: HaileyBot) : Command {

    override fun getName(): String {
        return "userInfo"
    }

    override fun getAliases(): List<String>? {
        return null
    }

    override fun getHelp(): String? = "Prints into about a user, or yourself if none defined"


    override fun getDescription(): String? = null

    override fun onMessage(message: IMessage, rawMessage: String, commandName: String) {
        val uid = ConversionUtils.parseUser(rawMessage);

        val user = if(uid == -2L) message.author else message.client.getUserByID(uid)
        if(user == null){
            message.channel.sendMessage("Failed to find a user with the UID $uid")
            return;
        }
        val watches = bot.matcher.getWatchesForUser(user.longID);
        val username = user.name + "#" + user.discriminator
        val nick = user.getNicknameForGuild(message.guild)
        val formattedCreationDate = dateFormatter.format(user.creationDate)
        val bot: Boolean = user.isBot
        val presence = ConversionUtils.convertStatusToString(user.presence.status);
        val roles = user.getRolesForGuild(message.guild).filter { !it.isEveryoneRole }.sortedBy { it.position }.map { it.name }
        val stringRoles = roles.joinToString(", ");

        val builder = StringBuilder()
        builder.appendln("***Meta***:");
        builder.appendln("**Username:** $username")
                .appendln("**Server nickname:** $nick")
                .appendln("**UID:** ${user.longID}")
                .appendln("**Created on:** $formattedCreationDate")
                .appendln("**Bot:** ${if(bot) "Yes" else "No"}")
                .appendln("**Presence:** $presence")
                .appendln("**Roles (${roles.size}):** ${if(stringRoles.length > 1500) "(Too many to display :c)" else stringRoles}")
                .nl().nl()

        builder.appendln("***Bot data:***");
        builder.appendln("**Regex watches**: ${watches.size}");


        val embed = EmbedBuilder()
                .withTitle("User info")
                .withAuthorName(username)
                .withAuthorIcon(user.avatarURL)
                .withColor(user.getColorForGuild(message.guild))
                .withDesc(builder.toString())
                .build()
        message.channel.sendMessage(embed)

    }
}
