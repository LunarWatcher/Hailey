package io.github.lunarwatcher.java.haileybot.commands.watching

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.commands.meta.getRandomColor
import io.github.lunarwatcher.java.haileybot.utils.canUserRunAdminCommand
import io.github.lunarwatcher.java.haileybot.utils.fitDiscordLengthRequirements
import org.slf4j.LoggerFactory
import sx.blah.discord.handle.impl.obj.Embed
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IPrivateChannel
import sx.blah.discord.util.EmbedBuilder

class WatchCommand(val bot: HaileyBot) : Command {
    override fun getName(): String = "watch"
    override fun getAliases(): MutableList<String>? = null;
    override fun getHelp(): String = "Usage: `watch [regex]`. Used without brackets"
    override fun getDescription(): String = "Pings you when a pattern is detected in chat."

    override fun onMessage(message: IMessage, rawMessage: String, commandName: String?) {
        if (message.channel is IPrivateChannel) {
            message.channel.sendMessage("This is a DM channel. No mod tools available.");
            return;
        }
        if (!message.canUserRunAdminCommand(bot)) {
            message.reply("You need to be a bot admin or have the administrator permission to do that.")
            return
        }
        if (rawMessage == ".*")
            return

        if (rawMessage.isEmpty() || rawMessage.isBlank()) {
            message.reply("You need 1 argument: regex")
        } else {
            val user = message.author.longID
            val guild = message.guild.longID
            val result = bot.matcher.watch(user, guild, rawMessage)

            if (!result)
                message.reply("Compiling failed. Check your regex")
            else
                message.reply("Watched!")


        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WatchCommand::class.java)
    }
}

class UnwatchCommand(val bot: HaileyBot) : Command {
    override fun getName(): String = "unwatch"
    override fun getAliases(): MutableList<String>? = null;
    override fun getHelp(): String = "Usage: `unwatch [regex/all]`. Used without brackets"
    override fun getDescription(): String = "Unwatches any regex registered regex notifications"

    override fun onMessage(message: IMessage, rawMessage: String, commandName: String?) {
        if (message.channel is IPrivateChannel) {
            message.channel.sendMessage("This is a DM channel. No mod tools available.");
            return;
        }
        if (rawMessage.isEmpty() || rawMessage.isBlank()) {
            message.reply("You need 1 argument: regex")
        } else {

            val user = message.author.longID
            val guild = message.guild.longID

            val result = bot.matcher.unwatch(user, guild, rawMessage)

            if (!result)
                message.reply("You weren't watching that.")
            else
                message.reply("Unwatched!")


        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UnwatchCommand::class.java)
    }

}

class ListWatches(val bot: HaileyBot) : Command {
    override fun getName(): String = "listWatches"
    override fun getAliases(): MutableList<String>? = null

    override fun getHelp(): String? = null
    override fun getDescription(): String? = "Lists your regex watches globally"
    override fun onMessage(message: IMessage, rawMessage: String?, commandName: String?) {
        val watcher = bot.matcher
        val watches = watcher.getWatchesForUser(message.author.longID)
        if (watches.isEmpty()) {
            message.channel.sendMessage("You don't have any regex watches.");
            return;
        }

        val targetChannel = message.author.orCreatePMChannel

        val watchesByGuild = mutableMapOf<String, MutableList<RegexMatch>>()

        for (watch in watches) {
            if(watch.regex.size == 0)
                continue;
            val guildId = watch.guild
            val guild = getGuild(message, guildId)

            watchesByGuild.computeIfAbsent(guild.name) { mutableListOf() }

            watchesByGuild[guild.name]?.add(watch);
        }

        if(watchesByGuild.isEmpty()){
            message.channel.sendMessage("You don't have any regex watches.");
            return;
        }
        val embed = EmbedBuilder()
                .withColor(getRandomColor())
        val builder = StringBuilder()

        for ((k, v) in watchesByGuild) {
            val string = "```${v.flatMap { it.regex }.joinToString("\n")}```"
            val len = string.length
            if(len > EmbedBuilder.FIELD_CONTENT_LIMIT){
                val pieces = string.fitDiscordLengthRequirements(EmbedBuilder.FIELD_CONTENT_LIMIT)
                for(piece in pieces){
                    if(embed.fieldCount == EmbedBuilder.FIELD_COUNT_LIMIT){
                        targetChannel.sendMessage(embed.build());
                        embed.clearFields();
                    }
                    embed.appendField(Embed.EmbedField("Guild: $k", piece, false))
                }
            }else {
                if(embed.fieldCount == EmbedBuilder.FIELD_COUNT_LIMIT){
                    targetChannel.sendMessage(embed.build());
                    embed.clearFields();
                }
                embed.appendField("Guild: $k", string, false);
            }

            if(embed.totalVisibleCharacters > 0){
                targetChannel.sendMessage(embed.build());
            }
        }
    }

    private fun getGuild(message: IMessage, channel: Long): IGuild {
        if (cache[channel] != null)
            return cache[channel]!!;

        val guild = message.client.getChannelByID(channel)
                .guild
        cache[channel] = guild;
        return guild;
    }

    fun nukeCache() {
        cache.clear();
    }

    companion object {
        private val cache = mutableMapOf<Long, IGuild>()
    }

}