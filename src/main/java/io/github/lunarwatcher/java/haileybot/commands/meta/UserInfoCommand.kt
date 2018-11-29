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

package io.github.lunarwatcher.java.haileybot.commands.meta

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.data.Constants.dateFormatter
import io.github.lunarwatcher.java.haileybot.utils.ConversionUtils
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageEmbed
import org.jetbrains.annotations.NotNull

class UserInfoCommand : Command {

    override fun getName(): String {
        return "userInfo"
    }

    override fun getAliases(): List<String>? {
        return null
    }

    override fun getHelp(): String? = "Prints into about a user, or yourself if none defined"


    override fun getDescription(): String? = null

    override fun onMessage(bot: HaileyBot, message: @NotNull Message, rawMessage: String, commandName: String) {
        val uid = ConversionUtils.parseUser(rawMessage);

        val member = if (uid == -2L) message.member else message.guild.getMember(message.jda.getUserById(uid))
        if (member == null) {
            message.channel.sendMessage("Failed to find a user with the UID $uid").queue()
            return;
        }
        val watches = bot.matcher.getWatchesForUser(member.user.idLong);
        val username = member.user.name + "#" + member.user.discriminator
        val nick = member.nickname ?: "None"
        val formattedCreationDate = dateFormatter.format(member.joinDate)
        val isUserBot: Boolean = member.user.isBot
        val presence = ConversionUtils.convertStatusToString(member.onlineStatus);
        val roles = member.roles.filter { !it.isPublicRole }.sortedByDescending { it.position }.map { it.name }
        val stringRoles = roles.joinToString(", ");

        val activity = ConversionUtils.getGame(member)
        val permissions = member.permissions
                .map {
                    it.name.replace("_", " ").toLowerCase()
                }


        val uidEmbed = MessageEmbed.Field("User ID", member.user.id, true)
        val nicknameEmbed = MessageEmbed.Field("Server nickname", nick, true)
        val botStatusEmbed = MessageEmbed.Field("Bot", if (isUserBot) "Yes" else "No", true)
        val accountCreationEmbed = MessageEmbed.Field("Creation date", "$formattedCreationDate (dd-mm-yyyy)", false)
        val presenceEmbed = MessageEmbed.Field("Presence", presence, false)
        val activityEmbed = MessageEmbed.Field("Activity", activity, false)
        val roleEmbed = MessageEmbed.Field("Roles (${roles.size})", if (stringRoles.length > 1200) "(Too many to display :c)" else stringRoles, false)
        val permissionEmbed = MessageEmbed.Field("Permissions", permissions.joinToString(", "), false)
        val watchesEmbed = MessageEmbed.Field("Watches", watches.filter { it.regex.isNotEmpty() }.flatMap { it.regex }.size.toString(), false)

        val embed = EmbedBuilder()
                .setTitle("User info")
                .setAuthor(username, null, member.user.avatarUrl)
                .setColor(member.color)
                .addField(uidEmbed)
                .addField(nicknameEmbed)
                .addField(botStatusEmbed)
                .addField(accountCreationEmbed)
                .addField(presenceEmbed)
                .addField(activityEmbed)
                .addField(roleEmbed)
                .addField(permissionEmbed)
                .addField(watchesEmbed)
                .build()
        message.channel.sendMessage(embed).queue()

    }
}
