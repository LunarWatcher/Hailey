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

package io.github.lunarwatcher.java.haileybot.botmeta

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.data.Database
import io.github.lunarwatcher.java.haileybot.utils.canUserRunBotAdminCommand
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import org.jetbrains.annotations.NotNull
import java.util.*


class BlacklistStorage(var guilds: ArrayList<Long>) {
    @Suppress("UNCHECKED_CAST")
    constructor(database: Database) : this(database.get(KEY) as? ArrayList<Long> ?: arrayListOf<Long>())

    fun isBlacklisted(guild: Guild) = isBlacklisted(guild.idLong)
    fun isBlacklisted(guild: Long) = guilds.contains(guild)

    fun blacklist(guild: Guild) = blacklist(guild.idLong)
    fun blacklist(guild: Long) {
        if (!guilds.contains(guild))
            guilds.add(guild)
    }

    fun unblacklist(guild: Guild) = unblacklist(guild.idLong)
    fun unblacklist(guild: Long) {
        if (guilds.contains(guild))
            guilds.remove(guild);
    }

    fun save(database: Database) {
        database.put(KEY, guilds);
    }

    companion object {
        const val KEY = "blacklisted-guilds";
    }

}

class ListGuildsCommand : Command {
    override fun getName(): String = "listGuilds"
    override fun getHelp(): String? = "Sends a DM containing joined guilds. Bot admins only!"
    override fun getDescription(): String? = help
    override fun onMessage(bot: HaileyBot, message: @NotNull Message, rawMessage: String, commandName: String) {
        if (!message.canUserRunBotAdminCommand(bot)) {

            message.channel.sendMessage("You can't ue this command.").queue()

            return;
        }

        val author = message.author
        author.openPrivateChannel().queue { dms ->

            var res = ""
            val guilds = message.jda.guilds

            for (i in 0 until guilds.size) {
                val guild = guilds[i]

                val appendix = "${guild.name} (UID ${guild.idLong})" + if (i == guilds.size - 1) "" else ", "
                res += if (res.length + appendix.length >= 2000) {
                    res = ""
                    dms.sendMessage(res).queue()
                    appendix;
                } else appendix
            }

            if (res != "")
                dms.sendMessage(res).queue();
            message.channel.sendMessage("You have mail!").queue()
        }
    }

    override fun getAliases(): MutableList<String>? = null

}

class BlacklistGuildCommand : Command {
    override fun getName(): String = "blacklistGuild"
    override fun getAliases(): MutableList<String>? = null

    override fun getHelp(): String? = "Blacklists a guild by ID."
    override fun getDescription(): String? = help
    override fun onMessage(bot: HaileyBot, message: @NotNull Message, rawMessage: String, commandName: String) {
        if (!message.canUserRunBotAdminCommand(bot)) {
            message.channel.sendMessage("You can't do that.").queue()

            return;
        }
        val id = rawMessage.toLongOrNull()
        if (id == null) {
            message.channel.sendMessage("Which guild?").queue()

            return;
        }

        val guild = message.jda.getGuildById(id);
        if (guild == null) {
            message.channel.sendMessage("Failed to find guild with the ID $id").queue()

            return;
        }

        guild.leave()

        if (message.guild.idLong != id) {
            message.channel.sendMessage("Successfully blacklisted the guild.").queue()

        }

        bot.blacklistStorage.blacklist(guild)
    }

}

class UnblacklistGuildCommand : Command {
    override fun getName(): String = "unblacklistGuild"
    override fun getAliases(): MutableList<String>? = null

    override fun getHelp(): String? = "Blacklists a guild by ID."
    override fun getDescription(): String? = help
    override fun onMessage(bot: HaileyBot, message: @NotNull Message, rawMessage: String, commandName: String) {
        if (!message.canUserRunBotAdminCommand(bot)) {
            message.channel.sendMessage("You can't do that.").queue()

            return;
        }
        val id = rawMessage.toLongOrNull()
        if (id == null) {
            message.channel.sendMessage("Which guild?").queue()
            return;
        }

        val guild = message.jda.getGuildById(id);
        if (guild == null) {
            message.channel.sendMessage("Failed to find guild with the ID $id").queue()
            return;
        }

        bot.blacklistStorage.unblacklist(guild)

        message.channel.sendMessage("Removed the guild $id from the blacklist").queue()

    }

}