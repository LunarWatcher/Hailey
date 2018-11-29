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

package io.github.lunarwatcher.java.haileybot.commands.roles

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.PrivateChannel
import org.jetbrains.annotations.NotNull

class ListRolesCommand : Command {

    override fun getName(): String = "roles"
    override fun getAliases(): List<String>? = null

    override fun getHelp(): String = "Lists all the self-assignable roles on the server"
    override fun getDescription(): String = help

    override fun onMessage(bot: HaileyBot, message: @NotNull Message, rawMessage: String, commandName: String) {
        if (message.channel is PrivateChannel) {
            message.channel.sendMessage("This is a DM channel. No roles are available.").queue();
            return;
        }
        val selfAssignable = bot.assigner.getRolesForGuild(message.guild.idLong)?.let { bot.assigner.getRolesFromId(it).map { it2 -> it2.name } }
        if (selfAssignable == null || selfAssignable.isEmpty()) {
            message.channel.sendMessage(
                    EmbedBuilder()
                            .setTitle("Assignable roles for " + message.guild.name)
                            .apply {
                                fields.add(MessageEmbed.Field("Self-assignable roles", "No self-assignable roles", true))
                                //addField(MessageEmbed.Field("Server roles (not necessarily assignable)", serverRolesString, true))
                            }
                            .setColor(message.member.color)
                            .build()
            ).queue()
            return;
        }

        message.author.openPrivateChannel().queue({
            var current = ""
            var currentEmbed = EmbedBuilder()
                    .setColor(message.member.color)

            for (i in 0 until selfAssignable.size) {
                val role = selfAssignable[i];

                val appendix = role + if (i != selfAssignable.size - 1) {
                    ", "
                } else "."
                if (appendix.length + current.length >= 1000) {
                    val field = MessageEmbed.Field("Roles", current, false)

                    val currentChars = currentEmbed.length()
                    if (currentChars + current.length >= 6000) {
                        it.sendMessage(currentEmbed.build()).queue()

                        currentEmbed = EmbedBuilder()
                                .setColor(message.member.color)
                    }
                    currentEmbed.addField(field)
                    current = "";
                }
                current += appendix
            }


            if (current != "") {
                if (currentEmbed.length() != 0) {
                    val field = MessageEmbed.Field("Roles", current, false)

                    val currentChars = currentEmbed.length()
                    if (currentChars + current.length >= 6000) {
                        it.sendMessage(currentEmbed.build()).queue()

                    } else {
                        currentEmbed.addField(field)
                        it.sendMessage(currentEmbed.build()).queue();
                        return@queue;
                    }
                }

                sendEmbed(message, current, it);
            }

        }, {
            message.channel.sendMessage("An error occured when attempting to get the DM channel: ${it.message} I might not be able to DM you.").queue()

        })
    }

    private fun sendEmbed(message: Message, content: String, channel: PrivateChannel) {
        if (content.isEmpty() || content.isBlank())
            return;

        channel.sendMessage(
                EmbedBuilder()
                        .setTitle("Assignable roles for " + message.guild.name)
                        .apply {
                            setDescription(content)
                            //addField(MessageEmbed.Field("Server roles (not necessarily assignable)", serverRolesString, true))
                        }
                        .setColor(message.member.color)
                        .build()
        ).queue()
    }
}