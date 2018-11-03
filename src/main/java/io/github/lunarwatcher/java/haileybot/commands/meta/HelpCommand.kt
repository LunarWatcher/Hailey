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
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Message
import org.jetbrains.annotations.NotNull
import java.awt.Color

@Suppress("RedundantCompanionReference")
class HelpCommand : Command {
    override fun getName(): String = "help"
    override fun getHelp(): String = "Run `help` for a list of all commands. Doing `help commandName` (where commandName is a specific command) displays the help and description of that command."
    override fun getAliases(): List<String>? = Companion.aliases
    override fun getDescription(): String = "Lists all the bots commands, or shows the help for a specific one"

    override fun onMessage(bot: HaileyBot, message: @NotNull Message, rawMessage: String, commandName: String) {
        if (rawMessage.isNotEmpty()) {
            val command: Command? = bot.commands.commandSets.asSequence().map {
                it.firstOrNull { second ->
                    second.matchesCommand(rawMessage)
                }
            }.firstOrNull { it != null }
            if (command == null) {
                message.channel.sendMessage("Command not found: $rawMessage").queue();
                return;
            }
            val embed = EmbedBuilder()
            embed.apply {
                setTitle(command.name)
                setDescription("Aliases: ${command.aliases ?: "None"}\n\nDescription: ${command.description
                        ?: "None"}\n\nHelp: ${command.help ?: "No help"}")
                setColor(Color(1f, 0f, .2f))
                setFooter("Made with Java, Kotlin, and ♥", null)
            }
            message.channel.sendMessage(embed.build()).queue()
            return;
        }
        message.channel.sendMessage(generateHelpMessage(bot)).queue()

    }

    private fun generateHelpMessage(bot: HaileyBot) = StringBuilder().append("```ini\n")
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