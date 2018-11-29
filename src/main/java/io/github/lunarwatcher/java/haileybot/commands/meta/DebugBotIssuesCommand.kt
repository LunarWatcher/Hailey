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
import io.github.lunarwatcher.java.haileybot.commands.Moderator.*
import io.github.lunarwatcher.java.haileybot.data.Constants
import io.github.lunarwatcher.java.haileybot.utils.canUserRunBotAdminCommand
import io.github.lunarwatcher.java.haileybot.utils.fitDiscordLengthRequirements
import io.github.lunarwatcher.java.haileybot.utils.nl
import io.github.lunarwatcher.java.haileybot.utils.scheduleDeletion
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageEmbed
import org.jetbrains.annotations.NotNull
import java.awt.Color

class DebugBotIssuesCommand : Command {

    override fun getName(): String {
        return "debug"
    }

    override fun getAliases(): List<String>? {
        return null
    }

    override fun getHelp(): String? {
        return "Usage is straight-forward: run the command. Append the `--with-invalid-channels` flag to also dump inaccessible channels."
    }

    override fun getDescription(): String? {
        return "Dumps debug data for the bot. Admins only."
    }

    override fun onMessage(bot: HaileyBot, message: @NotNull Message, rawMessage: String, commandName: String) {
        if (!message.canUserRunBotAdminCommand(bot)) {
            message.channel.sendMessage("only bot admins can do that.").queue();
            return
        }
        val dumpChannels = rawMessage.contains("--with-invalid-channels")

        val moderator = bot.moderator
        val watcher = bot.matcher
        val database = bot.database

        val botUser = bot.botUser
        val botMember = message.guild.getMember(botUser);

        val modGuild = bot.moderator.getGuild(message.guild)
        val builder = EmbedBuilder()
                .setColor(Color.MAGENTA)
        builder.setTitle("Bot debug")
        builder.setDescription("Currently running version " + Constants.VERSION + "\n")
                .appendDescription("Running under " + botUser.name + "#" + botUser.discriminator + "\n")

        builder.addField(MessageEmbed.Field("Permissions for this guild",
                message.guild.getMember(botUser).permissions.joinToString(", ") { it.toString().toLowerCase().replace("_", " ") },
                true))
        builder.addField(MessageEmbed.Field("Moderator", """
            There are currently ${moderator.size()} mod guilds.
            This is ${if (moderator.getGuild(message.guild) == null) "not one of them" else "one of them."}
            ${if (moderator.getGuild(message.guild) != null) "Enabled features can be seen using the `serverInfo` command" else ""}
        """.trimIndent(), true))

        val hasReadWrite = botMember.hasPermission(Permission.MESSAGE_READ) && botMember.hasPermission(Permission.MESSAGE_WRITE)

        val usableChannels =  message.guild.channels.filter {
            val override = it.getPermissionOverride(botMember)
            override?.allowed?.containsAll(readWrite) ?: hasReadWrite
        }

        var stringBuilder = StringBuilder();
        stringBuilder.append("Dumping info for categories:").nl()
        stringBuilder.append("I know of ${message.guild.channels.size} text channels, and ${message.guild.voiceChannels.size} voice channels.").nl()
        stringBuilder.append("Of these, I can read and write in" +
                " ${usableChannels.size}" +
                " of the text channels.").nl()
                .append("Ignoring permissions in voice channels; no voice features are currently used.").nl()
                .append("For this guild, there's a combined number of ${watcher.getWatchesInGuild(message.guild).size} watches for this guild.").nl()
                .append("In addition, the database currently has ${database.size()} items in it. ").nl().nl()
        modGuild?.auditChannel?.let {channelId ->
            if (channelId >= 0) {
                stringBuilder.append("Checking the audit channel (<#$channelId>) for validity.... ")
                val channel: Channel? = bot.client.getTextChannelById(channelId)

                val valid = channel != null && (channel.getPermissionOverride(message.guild.getMember(botUser))?.let { perms ->
                    perms.allowed.contains(Permission.MESSAGE_READ) && perms.allowed.contains(Permission.MESSAGE_WRITE)
                } ?: hasReadWrite)

                stringBuilder.append("It is ${if (valid) "valid" else "not valid. I will remove this channel"}")
                        .nl()

                if (!valid) {
                    modGuild.set(AUDIT_FEATURE, -1L);
                }
            }
        }
        modGuild?.welcomeChannel?.let {channelId->
            if (channelId >= 0) {
                stringBuilder.append("Checking the greeting channel (<#$channelId>) for validity.... ");
                val channel: Channel? = bot.client.getTextChannelById(channelId)
                val valid = channel != null && (channel.getPermissionOverride(message.guild.getMember(botUser))?.let { perms ->
                    perms.allowed.contains(Permission.MESSAGE_READ) && perms.allowed.contains(Permission.MESSAGE_WRITE)
                } ?: hasReadWrite)

                stringBuilder.append("It is ${if (valid) "valid" else "not valid. I will remove this channel"}")
                        .nl()
                if (!valid) {
                    modGuild.set(WELCOME_LOGGING, -1L);
                }
            }
        }
        modGuild?.userLeaveChannel?.let {channelId ->
            if (channelId >= 0) {
                stringBuilder.append("Checking the user leave channel (<#$channelId>) for validity.... ");
                val channel: Channel? = bot.client.getTextChannelById(channelId)
                val valid = channel != null && (channel.getPermissionOverride(message.guild.getMember(botUser))?.let { perms ->
                    perms.allowed.contains(Permission.MESSAGE_READ) && perms.allowed.contains(Permission.MESSAGE_WRITE)
                } ?: hasReadWrite)
                stringBuilder.append("It is ${if (valid) "valid" else "not valid. I will remove this channel"}")
                        .nl()
                if (!valid) {
                    modGuild.set(LEAVE_LOGGING, -1L);
                }
            }
        }

        builder.addField(MessageEmbed.Field("Internal info", stringBuilder.toString(), true))
        message.channel.sendMessage(builder.build()).queue {
            it.scheduleDeletion(TIMEOUT)
        }
        if (dumpChannels) {

            val unusableChannels = message.guild.channels.filter { it !in usableChannels }
            if(unusableChannels.isEmpty()){
                message.channel.sendMessage("There are no unusable channels :D").queue {
                    it.scheduleDeletion(TIMEOUT)
                }
                return;
            }
            stringBuilder = StringBuilder()
            stringBuilder.append("```\nUnusable channels:\n")
            for (channel in unusableChannels) {
                stringBuilder.append("${channel.name} + (<#${channel.idLong}>)").nl()
            }
            stringBuilder.append("```");
            val messages = stringBuilder.toString().fitDiscordLengthRequirements(2000)
            for (channelMessage in messages) {
                message.channel.sendMessage(channelMessage).queue {
                    it.scheduleDeletion(TIMEOUT)
                }
            }
        }
    }

    companion object {
        const val TIMEOUT: Long = 60L * 20L * 1000L;
        val readWrite = listOf(
                Permission.MESSAGE_READ,
                Permission.MESSAGE_WRITE
        )
    }
}
