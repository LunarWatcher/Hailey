package io.github.lunarwatcher.java.haileybot.botmeta

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.data.Database
import io.github.lunarwatcher.java.haileybot.utils.canUserRunBotAdminCommand
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IMessage
import java.util.*


class BlacklistStorage(var guilds: ArrayList<Long>) {
    @Suppress("UNCHECKED_CAST")
    constructor(database: Database) : this(database.get(KEY) as? ArrayList<Long> ?: arrayListOf<Long>())

    fun isBlacklisted(guild: IGuild) = isBlacklisted(guild.longID)
    fun isBlacklisted(guild: Long) = guilds.contains(guild)

    fun blacklist(guild: IGuild) = blacklist(guild.longID)
    fun blacklist(guild: Long) {
        if (!guilds.contains(guild))
            guilds.add(guild)
    }

    fun unblacklist(guild: IGuild) = unblacklist(guild.longID)
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

class ListGuildsCommand(val bot: HaileyBot) : Command {
    override fun getName(): String = "listGuilds"
    override fun getHelp(): String? = "Sends a DM containing joined guilds. Bot admins only!"
    override fun getDescription(): String? = help
    override fun onMessage(message: IMessage, rawMessage: String?, commandName: String?) {
        if (!message.canUserRunBotAdminCommand(bot)) {
            message.channel.sendMessage("You can't ue this command.");
            return;
        }

        val author = message.author
        val dms = author.orCreatePMChannel
        var res = ""
        val guilds = message.client.guilds

        for (i in 0 until guilds.size) {
            val guild = guilds[i]

            val appendix = "${guild.name} (UID ${guild.longID})" + if (i == guilds.size - 1) "" else ", "
            res += if (res.length + appendix.length >= 2000) {
                res = ""
                dms.sendMessage(res)
                appendix;
            } else appendix
        }

        if (res != "")
            dms.sendMessage(res);
        message.channel.sendMessage("You have mail!");
    }

    override fun getAliases(): MutableList<String>? = null

}

class BlacklistGuildCommand(val bot: HaileyBot) : Command {
    override fun getName(): String = "blacklistGuild"
    override fun getAliases(): MutableList<String>? = null

    override fun getHelp(): String? = "Blacklists a guild by ID."
    override fun getDescription(): String? = help
    override fun onMessage(message: IMessage, rawMessage: String, commandName: String) {
        if (!message.canUserRunBotAdminCommand(bot)) {
            message.channel.sendMessage("You can't do that.");
            return;
        }
        val id = rawMessage.toLongOrNull()
        if (id == null) {
            message.channel.sendMessage("Which guild?");
            return;
        }

        val guild = message.client.getGuildByID(id);
        if (guild == null) {
            message.channel.sendMessage("Failed to find guild with the ID $id");
            return;
        }

        guild.leave()

        if (message.guild.longID != id) {
            message.channel.sendMessage("Successfully blacklisted the guild.");
        }

        bot.blacklistStorage.blacklist(guild)
    }

}

class UnblacklistGuildCommand(val bot: HaileyBot) : Command {
    override fun getName(): String = "unblacklistGuild"
    override fun getAliases(): MutableList<String>? = null

    override fun getHelp(): String? = "Blacklists a guild by ID."
    override fun getDescription(): String? = help
    override fun onMessage(message: IMessage, rawMessage: String, commandName: String) {
        if (!message.canUserRunBotAdminCommand(bot)) {
            message.channel.sendMessage("You can't do that.");
            return;
        }
        val id = rawMessage.toLongOrNull()
        if (id == null) {
            message.channel.sendMessage("Which guild?");
            return;
        }

        val guild = message.client.getGuildByID(id);
        if (guild == null) {
            message.channel.sendMessage("Failed to find guild with the ID $id");
            return;
        }

        bot.blacklistStorage.unblacklist(guild)

        message.channel.sendMessage("Removed the guild $id from the blacklist")
    }

}