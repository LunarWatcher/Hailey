package io.github.lunarwatcher.java.haileybot.commands.watching

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.commands.meta.getRandomColor
import io.github.lunarwatcher.java.haileybot.utils.ConversionUtils
import io.github.lunarwatcher.java.haileybot.utils.canUserRunAdminCommand
import io.github.lunarwatcher.java.haileybot.utils.canUserRunBotAdminCommand
import io.github.lunarwatcher.java.haileybot.utils.fitDiscordLengthRequirements
import sx.blah.discord.handle.impl.obj.Embed
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IPrivateChannel
import sx.blah.discord.util.EmbedBuilder
import sx.blah.discord.util.RequestBuffer

class WatchCommand(val bot: HaileyBot) : Command {
    override fun getName(): String = "watch"
    override fun getAliases(): MutableList<String>? = null;
    override fun getHelp(): String = "Usage: `watch [regex]`. Used without brackets"
    override fun getDescription(): String = "Pings you when a pattern is detected in chat."

    override fun onMessage(message: IMessage, rawMessage: String, commandName: String?) {
        if (message.channel is IPrivateChannel) {
            RequestBuffer.request {
                message.channel.sendMessage("This is a DM channel. No mod tools available.")
            };
            return;
        }
        if (!message.canUserRunAdminCommand(bot)) {
            message.reply("You need to be a bot admin or have the administrator permission to do that.")
            return
        }
        if (rawMessage == ".*")
            return

        if (rawMessage.length > 800 && !message.canUserRunBotAdminCommand(bot)) {
            RequestBuffer.request {
                message.channel.sendMessage("Woah, slow down! You need to be a bot admin to have regex entries over 800 chars. *(What were you planning to do with that anyways?)*")
            }
            return;
        }

        if (rawMessage.isEmpty() || rawMessage.isBlank()) {
            message.reply("You need at least 1 argument: regex. An optional #channel can be placed in front of the regex.")
        } else {
            val channel = ConversionUtils.parseChannel(rawMessage.split(" ").getOrNull(0), false)

            val reifiedMessage = if (channel != -2L) {
                if (!assertChannelServerMatches(channel, message)) {
                    RequestBuffer.request {
                        message.channel.sendMessage("You can't watch stuff on other guilds -_-")
                    }
                    return;
                }
                rawMessage.split(" ", limit = 2).getOrNull(1)?.trim() ?: ""
            } else rawMessage

            if (reifiedMessage.isBlank() || reifiedMessage.isEmpty()) {
                RequestBuffer.request {
                    message.channel.sendMessage("What do you want me to unwatch?")
                }
                return;
            }

            val user = message.author.longID
            val guild = message.guild.longID
            val result = bot.matcher.watch(user, guild, channel, reifiedMessage)

            if (!result)
                message.reply("Compiling failed. Check your regex")
            else
                message.reply("Watched!")


        }
    }

}

class UnwatchCommand(val bot: HaileyBot) : Command {
    override fun getName(): String = "unwatch"
    override fun getAliases(): MutableList<String>? = null;
    override fun getHelp(): String = "Usage: `unwatch [regex/all]`. Used without brackets"
    override fun getDescription(): String = "Unwatches any regex registered regex notifications"

    override fun onMessage(message: IMessage, rawMessage: String, commandName: String?) {
        if (message.channel is IPrivateChannel) {
            RequestBuffer.request {
                message.channel.sendMessage("This is a DM channel. No mod tools available.")
            };
            return;
        }
        if (rawMessage.isEmpty() || rawMessage.isBlank()) {
            message.reply("You need at least 1 argument: regex. An optional #channel can be placed in front of the regex.")
        } else {
            val channel = ConversionUtils.parseChannel(rawMessage.split(" ").getOrNull(0), false)

            val reifiedMessage = if (channel != -2L) {
                if (!assertChannelServerMatches(channel, message)) {
                    RequestBuffer.request {
                        message.channel.sendMessage("You can't watch stuff on other guilds -_-")
                    }
                    return;
                }
                rawMessage.split(" ", limit = 2).getOrNull(1)?.trim() ?: ""
            } else rawMessage

            if (reifiedMessage.isBlank() || reifiedMessage.isEmpty()) {
                RequestBuffer.request {
                    message.channel.sendMessage("What do you want me to unwatch?")
                }
                return;
            }

            val user = message.author.longID
            val guild = message.guild.longID

            val result = bot.matcher.unwatch(user, guild, channel, reifiedMessage)

            if (!result)
                message.reply("You weren't watching that."
                        + if (channel != -2L) " Note that you tried a channel unwatch. If it's watched for the guild, don't pass a channel as an argument"
                else "")
            else
                message.reply("Unwatched!")


        }
    }

}

class ListWatches(val bot: HaileyBot) : Command {
    override fun getName(): String = "listWatches"
    override fun getAliases() = Companion.aliases

    override fun getHelp(): String? = null
    override fun getDescription(): String? = "Lists your regex watches globally"
    override fun onMessage(message: IMessage, rawMessage: String?, commandName: String?) {
        val watcher = bot.matcher
        val watches = watcher.getWatchesForUser(message.author.longID)
        if (watches.isEmpty() || watches.flatMap { it.regex }.isEmpty()) {
            RequestBuffer.request {
                message.channel.sendMessage("You don't have any regex watches.")
            };
            return;
        }

        val targetChannel = message.author.orCreatePMChannel

        val watchesByGuild = mutableMapOf<String, MutableList<RegexMatch>>()
        val watchesByChannel = mutableMapOf<String, MutableList<RegexMatch>>()

        for (watch in watches) {
            if (watch.regex.size == 0)
                continue;
            val locationId = watch.locationId
            val isGuild = watch.guild

            if (isGuild) {
                val guild = getGuild(message, locationId)?.name ?: "Unknown guild"
                watchesByGuild.computeIfAbsent(guild) { mutableListOf() }
                watchesByGuild[guild]?.add(watch);
            } else {
                val c = getChannel(message, locationId)
                val channel = c?.let {
                    it.name + if (it.guild != null) " (${it.guild.name})" else "";
                } ?: "Unknown channel"

                watchesByChannel.computeIfAbsent(channel) { mutableListOf() }
                watchesByChannel[channel]?.add(watch);
            }
        }

        if (watchesByGuild.isEmpty() && watchesByChannel.isEmpty()) {
            RequestBuffer.request {
                message.channel.sendMessage("You don't have any regex watches.")
            };
            return;
        }

        val embed = EmbedBuilder()
                .withColor(getRandomColor())
        processWatches(message, watchesByChannel, embed, targetChannel, "Channel")
        processWatches(message, watchesByGuild, embed, targetChannel, "Guild");

        RequestBuffer.request {
            message.channel.sendMessage("Check your DM's!")
        }
    }

    private fun processWatches(message: IMessage, watches: Map<String, MutableList<RegexMatch>>, embed: EmbedBuilder, targetChannel: IPrivateChannel, label: String) {
        try {
            for ((k, v) in watches) {
                val cache = v.flatMap { it.regex }
                if (cache.isEmpty()) {
                    continue;
                }
                val string = "```${v.flatMap { it.regex }.joinToString("\n")}```"
                val len = string.length
                if (len > EmbedBuilder.FIELD_CONTENT_LIMIT) {
                    val pieces = string.fitDiscordLengthRequirements(EmbedBuilder.FIELD_CONTENT_LIMIT)
                    for (piece in pieces) {
                        if (embed.fieldCount == EmbedBuilder.FIELD_COUNT_LIMIT) {
                            targetChannel.sendMessage(embed.build());
                            embed.clearFields();
                        }
                        embed.appendField(Embed.EmbedField("$label: $k", piece, false))
                    }
                } else {
                    if (embed.fieldCount == EmbedBuilder.FIELD_COUNT_LIMIT) {
                        targetChannel.sendMessage(embed.build());
                        embed.clearFields();
                    }
                    embed.appendField("$label: $k", string, false);
                }

                if (embed.totalVisibleCharacters > 6) {
                    targetChannel.sendMessage(embed.build());
                }
            }
        } catch (e: Exception) {
            RequestBuffer.request {
                message.channel.sendMessage("Something went wrong when sending the message. You might not be able to send you DM's :c")
            }
        }
    }

    private fun getGuild(message: IMessage, guild: Long): IGuild? = message.client.getGuildByID(guild)
    private fun getChannel(message: IMessage, channel: Long): IChannel? = message.client.getChannelByID(channel)

    companion object {
        val aliases = listOf("watches");
    }

}

// Functions

internal fun assertChannelServerMatches(channel: Long, message: IMessage): Boolean {
    if (message.guild == null) {
        return false;
    }

    return message.client.getChannelByID(channel)?.guild?.longID == message.guild.longID;
}
