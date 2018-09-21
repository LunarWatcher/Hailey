package io.github.lunarwatcher.java.haileybot.commands.roles

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import sx.blah.discord.handle.impl.obj.Embed
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IPrivateChannel
import sx.blah.discord.util.EmbedBuilder

class ListRolesCommand(private val bot: HaileyBot) : Command {

    override fun getName(): String = "roles"
    override fun getAliases(): List<String>? = null

    override fun getHelp(): String = "Lists all the self-assignable roles on the server"
    override fun getDescription(): String = help

    override fun onMessage(message: IMessage, rawMessage: String, commandName: String) {
        if (message.channel is IPrivateChannel) {
            message.channel.sendMessage("This is a DM channel. No roles are available.");
            return;
        }
        val selfAssignable = bot.assigner.getRolesForGuild(message.guild.longID)?.map { it.name }
        if (selfAssignable == null || selfAssignable.isEmpty()) {
            message.channel.sendMessage(
                    EmbedBuilder()
                            .withTitle("Assignable roles for " + message.guild.name)
                            .apply {
                                appendField(Embed.EmbedField("Self-assignable roles", "No self-assignable roles", true))
                                //appendField(Embed.EmbedField("Server roles (not necessarily assignable)", serverRolesString, true))
                            }
                            .withColor(message.author.getColorForGuild(message.guild))
                            .build()
            )
            return;
        }


        var current = ""
        var currentEmbed = EmbedBuilder()
                .withColor(message.author.getColorForGuild(message.guild))

        currentEmbed.totalVisibleCharacters
        for (i in 0 until selfAssignable.size) {
            val role = selfAssignable[i];

            val appendix = role + if (i != selfAssignable.size - 1) {
                ", "
            } else "."
            if (appendix.length + current.length >= 1000) {
                val field = Embed.EmbedField("Roles", current, false)

                val currentChars = currentEmbed.totalVisibleCharacters
                if (currentChars + current.length >= 6000) {
                    message.author.orCreatePMChannel.sendMessage(currentEmbed.build())
                    currentEmbed = EmbedBuilder()
                            .withColor(message.author.getColorForGuild(message.guild))
                }
                currentEmbed.appendField(field)
                current = "";
            }
            current += appendix
        }


        if (current != "") {
            if (currentEmbed.totalVisibleCharacters != 0) {
                val field = Embed.EmbedField("Roles", current, false)

                val currentChars = currentEmbed.totalVisibleCharacters
                if (currentChars + current.length >= 6000) {
                    message.author.orCreatePMChannel.sendMessage(currentEmbed.build())

                } else {
                    currentEmbed.appendField(field)
                    message.author.orCreatePMChannel.sendMessage(currentEmbed.build());
                    return;
                }
            }

            sendEmbed(message, current);
        }


    }

    private fun sendEmbed(message: IMessage, content: String) {
        if (content.isEmpty() || content.isBlank())
            return;

        message.author.orCreatePMChannel.sendMessage(
                EmbedBuilder()
                        .withTitle("Assignable roles for " + message.guild.name)
                        .apply {
                            withDesc(content)
                            //appendField(Embed.EmbedField("Server roles (not necessarily assignable)", serverRolesString, true))
                        }
                        .withColor(message.author.getColorForGuild(message.guild))
                        .build()
        )
    }
}