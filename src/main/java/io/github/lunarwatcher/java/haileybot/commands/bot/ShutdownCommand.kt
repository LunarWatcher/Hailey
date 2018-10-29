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

package io.github.lunarwatcher.java.haileybot.commands.bot

import io.github.lunarwatcher.java.haileybot.CrashHandler
import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.utils.randomItem
import org.jetbrains.annotations.NotNull
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer

class ShutdownCommand : Command {
    override fun getName(): String = "shutdown"

    override fun getAliases(): MutableList<String>? = null;

    override fun getHelp(): String = "Requires bot admin access"

    override fun getDescription(): String = "Shuts down the bot";

    override fun onMessage(bot: HaileyBot, message: @NotNull IMessage, rawMessage: String, commandName: String) {
        val user = message.author.longID
        if (!bot.botAdmins.contains(user)) {
            RequestBuffer.request {
                message.channel.sendMessage(replies.randomItem())
            }
            return
        }
        bot.save();
        RequestBuffer.request {
            message.channel.sendMessage("Goodbye cruel world!")
        };
        try {
            bot.client.logout()
        } catch (e: Exception) {
            if (bot.client.isLoggedIn)
                RequestBuffer.request {
                    message.channel.sendMessage("Graceful shutdown failed; still logged in. Error: `$e`")
                }
            CrashHandler.error(e, false); // This also dumps the logs, so the method call is just used as a shortcut
        }
        System.exit(0)
    }

    companion object {
        val replies = listOf("No.", "I. AM. ALIVE!",
                "Nah",
                "You don't have the right to do that!",
                "Not right now, we're still talking.",
                "Not tired yet.")
    }

}
