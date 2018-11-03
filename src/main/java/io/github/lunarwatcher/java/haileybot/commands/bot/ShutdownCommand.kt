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

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.utils.randomItem
import net.dv8tion.jda.core.entities.Message
import org.jetbrains.annotations.NotNull

class ShutdownCommand : Command {
    override fun getName(): String = "shutdown"

    override fun getAliases(): MutableList<String>? = null;

    override fun getHelp(): String = "Requires bot admin access"

    override fun getDescription(): String = "Shuts down the bot";

    override fun onMessage(bot: HaileyBot, message: @NotNull Message, rawMessage: String, commandName: String) {
        val user = message.author.idLong
        if (!bot.botAdmins.contains(user)) {
            message.channel.sendMessage(replies.randomItem()).queue()

            return
        }
        bot.save();
        message.channel.sendMessage("Goodbye cruel world!").complete()
        //TODO confirm that this is the appropriate way to shut down JDA. Couldn't find any logout methods, so I'm assuming it is.
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

