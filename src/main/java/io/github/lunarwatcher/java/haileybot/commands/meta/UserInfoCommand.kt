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
import org.jetbrains.annotations.NotNull
import sx.blah.discord.handle.impl.obj.Embed
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.EmbedBuilder
import sx.blah.discord.util.RequestBuffer

class UserInfoCommand(val bot: HaileyBot) : Command {

    override fun getName(): String {
        return "userInfo"
    }

    override fun getAliases(): List<String>? {
        return null
    }

    override fun getHelp(): String? = "Prints into about a user, or yourself if none defined"


    override fun getDescription(): String? = null

    override fun onMessage(bot: HaileyBot, message: @NotNull IMessage, rawMessage: String, commandName: String) {
        val uid = ConversionUtils.parseUser(rawMessage);

        val user = if (uid == -2L) message.author else message.client.getUserByID(uid)
        if (user == null) {
            RequestBuffer.request {
                message.channel.sendMessage("Failed to find a user with the UID $uid")
            }
            return;
        }
        val watches = bot.matcher.getWatchesForUser(user.longID);
        val username = user.name + "#" + user.discriminator
        val nick = user.getNicknameForGuild(message.guild) ?: "None"
        val formattedCreationDate = dateFormatter.format(user.creationDate)
        val bot: Boolean = user.isBot
        val presence = ConversionUtils.convertStatusToString(user.presence.status);
        val roles = user.getRolesForGuild(message.guild).filter { !it.isEveryoneRole }.sortedBy { it.position }.map { it.name }
        val stringRoles = roles.joinToString(", ");

        val activity = ConversionUtils.getGame(user)
        val permissions = user.getPermissionsForGuild(message.guild)
                .map { it.name.replace("_", " ").toLowerCase() }


        val uidEmbed = Embed.EmbedField("User ID", user.stringID, true)
        val nicknameEmbed = Embed.EmbedField("Server nickname", nick, true)
        val botStatusEmbed = Embed.EmbedField("Bot", if (bot) "Yes" else "No", true)
        val accountCreationEmbed = Embed.EmbedField("Creation date", formattedCreationDate, false)
        val presenceEmbed = Embed.EmbedField("Presence", presence, false)
        val activityEmbed = Embed.EmbedField("Activity", activity, false)
        val roleEmbed = Embed.EmbedField("Roles (${roles.size})", if (stringRoles.length > 1200) "(Too many to display :c)" else stringRoles, false)
        val permissionEmbed = Embed.EmbedField("Permissions", permissions.joinToString(", "), false)
        val watchesEmbed = Embed.EmbedField("Watches", watches.filter { it.regex.isNotEmpty() }.flatMap { it.regex }.size.toString(), false)

        val embed = EmbedBuilder()
                .withTitle("User info")
                .withAuthorName(username)
                .withAuthorIcon(user.avatarURL)
                .withColor(user.getColorForGuild(message.guild))
                .appendField(uidEmbed)
                .appendField(nicknameEmbed)
                .appendField(botStatusEmbed)
                .appendField(accountCreationEmbed)
                .appendField(presenceEmbed)
                .appendField(activityEmbed)
                .appendField(roleEmbed)
                .appendField(permissionEmbed)
                .appendField(watchesEmbed)
                .build()
        RequestBuffer.request {
            message.channel.sendMessage(embed)
        }

    }
}
