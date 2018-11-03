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

package io.github.lunarwatcher.java.haileybot.commands.`fun`

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.utils.messageFormat
import io.github.lunarwatcher.java.haileybot.utils.randomItem
import net.dv8tion.jda.client.managers.EmoteManager
import net.dv8tion.jda.core.entities.Emote
import net.dv8tion.jda.core.entities.Message
import org.jetbrains.annotations.NotNull

class AliveCommand : Command {
    override fun getName(): String = "alive"

    override fun getAliases(): MutableList<String>? = null

    override fun getHelp(): String = "Checks if the bot is alive"

    override fun getDescription(): String = help

    override fun onMessage(bot: HaileyBot, message: @NotNull Message, rawMessage: String, commandName: String) {
        message.channel.sendMessage(replies.randomItem()).queue()

    }

    companion object {
        val replies = listOf(
                "No, I'm dead",
                "Lurking around the guilds",
                "Depends on how you define it",
                "Maybe",
                "Just went to grab some coffee",
                "Did I miss something?"
        )
    }
}

abstract class ActionCommand(val replies: List<String>, val emojis: List<String>, val onEmptyMessage: (Message) -> Unit) : Command {
    override fun onMessage(bot: HaileyBot, message: @NotNull Message, rawMessage: String, commandName: String) {
        if (rawMessage.isEmpty()) {
            onEmptyMessage.invoke(message);
            return;
        }

        val result = message.mentionedMembers
                .asSequence()
                .filter { it.user.idLong != bot.botUser.idLong && it.user.idLong != message.author.idLong }
                .map {
                    it.effectiveName
                }.toHashSet()
                .joinToString("** and **")
        if (result.isBlank() || result.isEmpty()) {
            onEmptyMessage.invoke(message)
            return;
        }
        message.channel.sendMessage("**${message.member.effectiveName}** " + replies.randomItem()?.messageFormat(result, message.member.effectiveName + " ${emojis.randomItem()
                ?: ""}")).queue()


    }

}

class ShootCommand : ActionCommand(replies, listOf(), { message ->
    message.channel.sendMessage(self.messageFormat(message.member.effectiveName)).queue {
        it.addReaction("\uD83C\uDDF7").queue();
    }
}) {
    override fun getName(): String = "shoot"
    override fun getAliases(): MutableList<String>? = null;
    override fun getHelp(): String? = "Shoots someone!!"
    override fun getDescription(): String? = help

    companion object {

        const val self = "**{0}** shot themselves! Press **R** to pay respects."
        val replies = listOf(
                "shoots **{0}**. Any last words?",
                "emptied a mag in **{0}**'s head.",
                "shoots **{0}** down at noon.",
                "killed **{0}** by sending automated gun drones after them.",
                "bust a cap in **{0}**.",
                "sniped **{0}**. ***HEADSHOT!***",
                "tried to shoot **{0}**, but realized they're out of bullets.",
                "tries to shoot **{0}**. PLOT TWIST!! **{0}** turns around and shoots **{1}**",
                "shoots at **{0}**, but misses!"

        )
    }
}

class HugCommand : ActionCommand(replies, listOf(), { message ->
    message.channel.sendMessage(self.messageFormat(message.member.effectiveName)).queue()

}) {
    override fun getName(): String = "hug"
    override fun getAliases(): MutableList<String>? = null;
    override fun getHelp(): String? = "Hugs someone <3"
    override fun getDescription(): String? = help

    companion object {

        const val self = "**{0}** hugs themselves? Nope! *hugs **{0}***"
        val replies = listOf(
                "hugs **{0}**",
                "covers **{0}** in fluff <3",
                "warms **{0}** with hugs",
                "cuddles **{0}**",
                "hugs **{0}** tightly and refuses to let go",
                "soaks **{0}** into their fluff owo"
        )
    }
}

class LickCommand : ActionCommand(replies, listOf(), { message -> message.channel.sendMessage(self.messageFormat(message.member.effectiveName)).queue() }) {
    override fun getName(): String = "lick"
    override fun getHelp(): String? = "Licks someone :tongue:"
    override fun getDescription(): String? = help;
    override fun getAliases(): MutableList<String>? = null

    companion object {
        const val self = "**{0}** licks themselves"
        val replies = listOf(
                "licks **{0}**",
                "gives **{0}** some soft licks :tongue:",
                "licks **{0}** gently"
        )
    }
}

class KissCommand : ActionCommand(replies, listOf(), { message -> message.channel.sendMessage(self.messageFormat(message.member.effectiveName)).queue() }) {
    override fun getName(): String = "kiss";
    override fun getAliases(): MutableList<String>? = null
    override fun getHelp(): String? = "Kisses someone :kissing_heart:"
    override fun getDescription(): String? = help

    companion object {
        const val self = "**{0}** kisses their own reflection"
        val replies = listOf(
                "takes a deep breath, and kisses **{0}**",
                "kisses **{0}**",
                "gives **{0}** a quick smooch :kissing_heart:"
        )
    }
}

class BoopCommand : ActionCommand(replies, listOf(), { message -> message.channel.sendMessage(self.messageFormat(message.member.effectiveName)).queue() }) {
    override fun getName(): String = "boop";
    override fun getAliases(): MutableList<String>? = null
    override fun getHelp(): String? = "BOOP!"
    override fun getDescription(): String? = help

    companion object {
        const val self = "**{0}** boops the mirror"
        val replies = listOf(
                "sneaks up on **{0}**, and boops them",
                "boops **{0}**",
                "buys **{0}** a meal, and boops them",
                "runs up to **{0}** and boops them <3"
        )
    }
}

class PatCommand : ActionCommand(replies, listOf(), { message -> message.channel.sendMessage(self.messageFormat(message.member.effectiveName)).queue() }) {
    override fun getName(): String = "pat"
    override fun getAliases(): List<String>? = null
    override fun getHelp(): String? = null;
    override fun getDescription(): String? = "Pats someone owo"

    companion object {
        const val self = "**{0}** pats themselves!"
        val replies = listOf(
                "pats **{0}** :heart:",
                "yells *\"SURPRISE!\"* and pats **{0}**",
                "puts **{0}** in their lap, and pats **{0}**"

        )
    }

}

class PetCommand : ActionCommand(replies, listOf(), { message -> message.channel.sendMessage(self.messageFormat(message.member.effectiveName)).queue() }) {
    override fun getName(): String = "pet"
    override fun getAliases(): List<String>? = null
    override fun getHelp(): String? = null;
    override fun getDescription(): String? = "Pet someone ~<3"

    companion object {
        const val self = "**{0}** pets themselves!"
        val replies = listOf(
                "gently pets **{0}**",
                "pets **{0}** on their head, and says \"Who's a good <pronoun>?\"",
                "cuddles and pets **{0}**"
        )
    }

}


class TickleCommand : ActionCommand(replies, listOf(), { message -> message.channel.sendMessage(self.messageFormat(message.member.effectiveName)).queue() }) {
    override fun getName(): String = "tickle"
    override fun getAliases(): List<String>? = null
    override fun getHelp(): String? = null;
    override fun getDescription(): String? = "Tickle someone!! *laughs incontrollably*"

    companion object {
        const val self = "**{0}** tickles themselves?!"
        val replies = listOf(
                "tickles **{0}**",
                "tickles **{0}** to insanity",
                "surprises **{0}** with tickling owo"
        )
    }

}
