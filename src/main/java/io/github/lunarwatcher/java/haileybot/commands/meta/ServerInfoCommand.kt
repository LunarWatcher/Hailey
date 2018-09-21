package io.github.lunarwatcher.java.haileybot.commands.meta

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.commands.Moderator
import io.github.lunarwatcher.java.haileybot.data.Constants
import io.github.lunarwatcher.java.haileybot.data.Constants.dateFormatter
import io.github.lunarwatcher.java.haileybot.utils.ConversionUtils
import io.github.lunarwatcher.java.haileybot.utils.canUserRunBotAdminCommand
import org.apache.commons.lang3.StringUtils
import sx.blah.discord.handle.impl.obj.Embed
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IPrivateChannel
import sx.blah.discord.util.EmbedBuilder
import java.awt.Color
import java.util.*

class ServerInfoCommand(private val bot: HaileyBot) : Command {

    override fun getName(): String {
        return "serverInfo"
    }

    override fun getAliases(): List<String>? {
        return null
    }

    override fun getHelp(): String? {
        return "Displays server info."
    }

    override fun getDescription(): String? {
        return help
    }

    override fun onMessage(message: IMessage, rawMessage: String, commandName: String) {
        if (message.channel is IPrivateChannel) {
            message.channel.sendMessage("This is a DM channel. No server info is available.");
            return;
        }
        val guild = message.guild
        val modGuild = bot.moderator.getGuild(guild.longID)

        val modInfo = StringBuilder()
        if (modGuild != null) {
            modInfo.append("|### Guild moderation module enabled. ###|").append(StringUtils.repeat(" ", 4)).append("\n\nEnabled features:\n")
            val data = modGuild.dataAsReadableMap
            for ((key, value) in data) {
                modInfo.append("**$key**").append(": ")

                if (key.equals(Moderator.AUDIT_FEATURE, ignoreCase = true) ||
                        key.equals(Moderator.WELCOME_LOGGING, ignoreCase = true) ||
                        key.equals(Moderator.LEAVE_LOGGING, ignoreCase = true)) {
                    modInfo.append("<#$value>")
                } else {
                    modInfo.append(value.toString())
                }

                modInfo.append("\n")
            }
        } else {
            modInfo.append("|### Guild moderation module disabled. ###|").append(StringUtils.repeat(" ", 4))
        }

        val users = message.guild.users;
        val bots = users.filter { it.isBot }.size
        val members = users.size - bots;

        val content = "**Owner:** ${guild.owner.name} (${guild.owner.longID})\n" +
                "**Server created at:** ${dateFormatter.format(guild.creationDate)}\n" +
                "**Members:** ${guild.totalMemberCount} ($members members, $bots bots)\n" + "\n" +
                "**Meta:** ID ${guild.longID} at shard " + (guild.shard.info[0] + 1) +
                "/${guild.shard.info[1]}\n\n" /*+
                "**Roles (${guildRoles?.size ?: 0})**: ${guildRoles?.map { it.name }?.joinToString(", ") ?: "No roles"}"*/


        val roleInfo = bot.assigner
                .getRolesForGuild(message.guild.longID)
                ?.joinToString(", ") { it.name } ?: "No self-assignable roles"

        val autoInfo = bot.assigner
                .getAutoRolesForGuild(message.guild)
                ?.joinToString(", ") { it.name } ?: "No auto-assign roles"

        val selfAssignable = bot.assigner.getRolesForGuild(message.guild)?.size ?: 0
        val autoAssignable = bot.assigner.getAutoRolesForGuild(message.guild)?.size ?: 0

        val serverInfo = EmbedBuilder()
                .withTitle("Server info for **${guild.name}**")
                .withColor(getRandomColor())
                .withAuthorIcon(guild.iconURL)
                .withAuthorName("Hailey")
                .appendField(Embed.EmbedField("Self- and auto-assignable roles",
                        "There are $selfAssignable self-assignable roles, and $autoAssignable roles that get automatically assigned. " +
                                (if (roleInfo.length > 1200)
                                    "Too many roles to display. Use `${Constants.TRIGGER}roles` to see self-assignable roles."
                                else roleInfo) + "\nAuto-assignable: " +
                                (if (autoInfo.length > 1200) "Too many roles to display."
                                else autoInfo),
                        true))
                .appendField(Embed.EmbedField("Verification level", ConversionUtils.parseVerificationLevel(message.guild.verificationLevel), true))
                .appendField(Embed.EmbedField("Location", message.guild.region.name, true))
                .appendField(Embed.EmbedField("Channels", "${message.guild.channels.size} channels in ${message.guild.categories.size} categories. ${message.guild.voiceChannels.size} voice channels.", true))
                .appendField(Embed.EmbedField("Info", content, false))

                .build()
        message.channel.sendMessage(serverInfo)

        if (message.canUserRunBotAdminCommand(bot)) {
            val modInfoEmbed = EmbedBuilder().apply {
                withTitle("Server mod info")
                        .withColor(message.author.getColorForGuild(message.guild))
                        .withDescription(modInfo.toString());
            }.build()

            message.channel.sendMessage(modInfoEmbed)
        }

    }
}

val random = Random(System.currentTimeMillis());
fun getRandomColor(): Color {
    val r = random.nextFloat();
    val g = random.nextFloat();
    val b = random.nextFloat();

    return Color(r, g, b, 1f);
}
