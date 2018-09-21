package io.github.lunarwatcher.java.haileybot.commands.meta

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.EmbedBuilder
import java.awt.Color

class HelpCommand(val bot: HaileyBot) : Command {
    override fun getName(): String = "help"
    override fun getHelp(): String = "Run `help` for a list of all commands. Doing `help commandName` (where commandName is a specific command) displays the help and description of that command."
    override fun getAliases(): List<String>? = Companion.aliases
    override fun getDescription(): String = "Lists all the bots commands, or shows the help for a specific one"

    override fun onMessage(message: IMessage, rawMessage: String, commandName: String) {
        if (rawMessage.isNotEmpty()) {
            val command: Command? = bot.commands.commandSets.map {
                it.firstOrNull {
                    it.matchesCommand(rawMessage)
                }
            }.firstOrNull { it != null }
            if (command == null) {
                message.channel.sendMessage("Command not found: $rawMessage");
                return;
            }
            val embed = EmbedBuilder()
            embed.apply {
                withTitle(command.name)
                withDesc("Aliases: ${command.aliases}\n\nDescription: ${command.description
                        ?: "None"}\n\nHelp: ${command.help ?: "No help"}")
                withColor(Color(1f, 0f, .2f))
                withFooterText("Made with Java, Kotlin, and ♥")
            }
            message.channel.sendMessage(embed.build())
            return;
        }
        message.channel.sendMessage(generateHelpMessage())

    }

    private fun generateHelpMessage() = StringBuilder().append("```ini\n")
            .append(getFormattedFor("Moderation", bot.commands.moderationCommands))
            .append(getFormattedFor("Meta", bot.commands.metaCommands))
            .append(getFormattedFor("Bot", bot.commands.botCommands))
            .append(getFormattedFor("Fun", bot.commands.funCommands))
            .append(getFormattedFor("Self assign", bot.commands.roleCommands))
            .append("```").toString()


    companion object {
        val aliases = listOf("halp", "sos", "commands")

        private fun getFormattedFor(sectionName: String, commands: List<Command>): String {
            val builder = StringBuilder()
            builder.append("[").append(sectionName).append("]").append("\n")
            commands.forEach { entry ->
                builder.append(entry.name).append(", ")
            }
            builder.replace(builder.length - 2, builder.length, "\n")
            return builder.toString()
        }


    }
}