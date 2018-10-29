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
import org.jetbrains.annotations.NotNull
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IPrivateChannel
import sx.blah.discord.handle.obj.Permissions
import sx.blah.discord.util.RequestBuffer

class EnableModCommand : Command {
    override fun getName(): String = "enableMod"
    override fun getAliases(): MutableList<String>? = null;
    override fun getHelp(): String = "Just run the command. You need to be an administrator, the owner of the server, or a bot admin to use it"
    override fun getDescription(): String = "Enables moderation features for this guild."

    override fun onMessage(bot: HaileyBot, message: @NotNull IMessage, rawMessage: String, commandName: String) {
        if (message.channel is IPrivateChannel) {
            RequestBuffer.request {
                message.channel.sendMessage("This is a DM channel. No mod tools available.")
            };
            return;
        }
        if (!message.author.getPermissionsForGuild(message.guild).contains(Permissions.ADMINISTRATOR) &&
                !bot.botAdmins.contains(message.author.longID) &&
                message.author.longID != message.guild.ownerLongID) {
            message.reply("You need to be a bot admin or have the administrator permission to do that.")
            return
        }
        if (bot.moderator.registerGuild(message.guild)) {
            message.reply("Added guild to the list of moderation guilds")
        } else {
            message.reply("Already enabled.")
        }
    }

}

class DisableModCommand(val bot: HaileyBot) : Command {
    override fun getName(): String = "disableMod"
    override fun getAliases(): MutableList<String>? = null;
    override fun getHelp(): String = "Just run the command. You need to be an administrator, the owner of the server, or a bot admin to use it"
    override fun getDescription(): String = "Disables moderation features for this guild."

    override fun onMessage(bot: HaileyBot, message: @NotNull IMessage, rawMessage: String, commandName: String) {
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
        if (bot.moderator.removeGuild(message.guild)) {
            message.reply("Removed guild to the list of moderation guilds")
        } else {
            message.reply("Already disabled.")
        }
    }

}