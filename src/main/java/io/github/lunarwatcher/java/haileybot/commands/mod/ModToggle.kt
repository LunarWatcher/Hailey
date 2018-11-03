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

package io.github.lunarwatcher.java.haileybot.commands.mod

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.utils.canUserRunAdminCommand
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.PrivateChannel
import org.jetbrains.annotations.NotNull


class EnableModCommand : Command {
    override fun getName(): String = "enableMod"
    override fun getAliases(): MutableList<String>? = null;
    override fun getHelp(): String = "Just run the command. You need to be an administrator, the owner of the server, or a bot admin to use it"
    override fun getDescription(): String = "Enables moderation features for this guild."

    override fun onMessage(bot: HaileyBot, message: @NotNull Message, rawMessage: String, commandName: String) {
        if (message.channel is PrivateChannel) {
            message.channel.sendMessage("This is a DM channel. No mod tools available.").queue();
            return;
        }
        if (!message.member.permissions.contains(Permission.ADMINISTRATOR) &&
                !bot.botAdmins.contains(message.author.idLong) &&
                message.author.idLong != message.guild.ownerIdLong) {
            message.channel.sendMessage("You need to be a bot admin or have the administrator permission to do that.").queue();
            return
        }
        if (bot.moderator.registerGuild(message.guild)) {
            message.channel.sendMessage("Added guild to the list of moderation guilds").queue();
        } else {
            message.channel.sendMessage("Already enabled.").queue();
        }
    }

}

class DisableModCommand : Command {
    override fun getName(): String = "disableMod"
    override fun getAliases(): MutableList<String>? = null;
    override fun getHelp(): String = "Just run the command. You need to be an administrator, the owner of the server, or a bot admin to use it"
    override fun getDescription(): String = "Disables moderation features for this guild."

    override fun onMessage(bot: HaileyBot, message: @NotNull Message, rawMessage: String, commandName: String) {
        if (message.channel is PrivateChannel) {
            message.channel.sendMessage("This is a DM channel. No mod tools available.").queue();
            return;
        }

        if (!message.canUserRunAdminCommand(bot)) {
            message.channel.sendMessage("You need to be a bot admin or have the administrator permission to do that.").queue();
            return
        }
        if (bot.moderator.removeGuild(message.guild)) {
            message.channel.sendMessage("Removed guild to the list of moderation guilds").queue();
        } else {
            message.channel.sendMessage("Already disabled.").queue();
        }
    }

}