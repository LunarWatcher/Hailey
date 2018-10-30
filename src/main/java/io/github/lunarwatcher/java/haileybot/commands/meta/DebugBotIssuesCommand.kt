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
import org.jetbrains.annotations.NotNull
import sx.blah.discord.handle.impl.obj.Embed
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.Permissions
import sx.blah.discord.util.EmbedBuilder
import sx.blah.discord.util.RequestBuffer
import java.awt.Color

class DebugBotIssuesCommand : Command {

    override fun getName(): String {
        return "debug"
    }

    override fun getAliases(): List<String>? {
        return null
    }

    override fun getHelp(): String? {
        return null
    }

    override fun getDescription(): String? {
        return "Dumps debug data for the bot. Admins only."
    }

    override fun onMessage(bot: HaileyBot, message: @NotNull IMessage, rawMessage: String, commandName: String) {
        if (!message.canUserRunBotAdminCommand(bot)) {
            RequestBuffer.request { message.reply("only bot admins can do that.") }
            return
        }
        val dumpChannels = rawMessage.contains("--with-invalid-channels")

        val moderator = bot.moderator
        val watcher = bot.matcher
        val database = bot.database
        val botUser = bot.botUser
        val modGuild = bot.moderator.getGuild(message.guild)
        val builder = EmbedBuilder()
                .withColor(Color.MAGENTA)
        builder.withTitle("Bot debug")
        builder.withDesc("Currently running version " + Constants.VERSION + "\n")
                .appendDesc("Running under " + botUser.name + "#" + botUser.discriminator + "\n")

        builder.appendField(Embed.EmbedField("Permissions for this guild",
                botUser.getPermissionsForGuild(message.guild).joinToString(", "){ it.toString().toLowerCase().replace("_", " ")},
                true))
        builder.appendField(Embed.EmbedField("Moderator", """
            There are currently ${moderator.size()} mod guilds.
            This is ${ if (moderator.getGuild(message.guild) == null) "not one of them" else "one of them."}
            ${ if (moderator.getGuild(message.guild) != null) "Enabled features can be seen using the `serverInfo` command" else ""}
        """.trimIndent(), true))

        val usableChannels = message.guild.channels.filter { !it.isDeleted && it.getModifiedPermissions(bot.botUser).containsAll (readWritePerms)}

        var stringBuilder = StringBuilder();
        stringBuilder.append("Dumping info for categories:").nl()
        stringBuilder.append("I know of ${ message.guild.channels.size } text channels, and ${ message.guild.voiceChannels.size } voice channels.").nl()
        stringBuilder.append("Of these, I can read and write in" +
                " ${ usableChannels.size}" +
                " of the text channels.").nl()
                .append("Ignoring permissions in voice channels; no voice features are currently used.").nl()
                .append("For this guild, there's a combined number of ${ watcher.getWatchesInGuild(message.guild).size } watches for this guild.").nl()
                .append("In addition, the database currently has ${ database.size() } items in it. ")
        modGuild?.auditChannel?.let {
            if(it >= 0) {
                stringBuilder.append("Checking the audit channel (<#$it>) for validity. ")
                val channel: IChannel? = bot.client.getChannelByID(it)
                val valid = channel?.getModifiedPermissions(botUser)
                        ?.containsAll(readWritePerms) ?: run {
                    stringBuilder.append("This channel has been removed, or I cannot find it. ")
                    false
                } && channel?.isDeleted?.let { bool ->
                    if(bool){
                        stringBuilder.append("This channel has been removed.");
                    }
                    bool
                } == false
                stringBuilder.append("It is ${if (valid) "valid" else "not valid. I will remove this channel"}")
                        .nl()

                if (!valid) {
                    modGuild.set(AUDIT_FEATURE, -1);
                }
            }
        }
        modGuild?.welcomeChannel?.let {
            if(it >= 0) {
                stringBuilder.append("Checking the greeting channel (<#$it>) for validity. ");
                val channel: IChannel? = bot.client.getChannelByID(it)
                val valid = channel?.getModifiedPermissions(botUser)
                        ?.containsAll(readWritePerms) ?: run {
                    stringBuilder.append("This channel has been removed, or I cannot find it. ")
                    false
                } && channel?.isDeleted?.let { bool ->
                    if(bool){
                        stringBuilder.append("This channel has been removed.");
                    }
                    bool
                } == false
                stringBuilder.append("It is ${if (valid) "valid" else "not valid. I will remove this channel"}")
                        .nl()
                if (!valid) {
                    modGuild.set(WELCOME_LOGGING, -1);
                }
            }
        }
        modGuild?.userLeaveChannel?.let {
            if(it >= 0) {
                stringBuilder.append("Checking the user leave channel (<#$it>) for validity. ");
                val channel: IChannel? = bot.client.getChannelByID(it)
                val valid = channel?.getModifiedPermissions(botUser)
                        ?.containsAll(readWritePerms) ?: run {
                    stringBuilder.append("This channel has been removed, or I cannot find it. ")
                    false
                } && channel?.isDeleted?.let { bool ->
                    if(bool){
                        stringBuilder.append("This channel has been removed.");
                    }
                    bool
                } == false
                stringBuilder.append("It is ${if (valid) "valid" else "not valid. I will remove this channel"}")
                        .nl()
                if (!valid) {
                    modGuild.set(LEAVE_LOGGING, -1);
                }
            }
        }



        builder.appendField(Embed.EmbedField("Internal info", stringBuilder.toString(), true))
        RequestBuffer.request {
            message.channel.sendMessage(builder.build())
                    .scheduleDeletion(TIMEOUT) //Auto-deletes the message after 20 minutes, to avoid clutter in chat.
        }
        if(dumpChannels){
            val unusableChannels = message.guild.channels.filter { it !in usableChannels }
            stringBuilder = StringBuilder()
            stringBuilder.append("Unusable channels:\n```")
            for(channel in unusableChannels){
                stringBuilder.append("${channel.name} + (<#${channel.longID}>)").nl()
            }
            stringBuilder.append("```");
            val messages = stringBuilder.toString().fitDiscordLengthRequirements(2000)
            for(channelMessage in messages){
                RequestBuffer.request {
                    message.channel.sendMessage(channelMessage)
                            .scheduleDeletion(TIMEOUT)
                }
            }
        }
    }

    companion object {
        const val TIMEOUT: Long = 60L * 20L * 1000L;
        val readWritePerms = listOf(Permissions.READ_MESSAGES, Permissions.SEND_MESSAGES)
    }
}
