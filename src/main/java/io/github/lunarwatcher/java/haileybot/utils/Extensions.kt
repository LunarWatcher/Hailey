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

package io.github.lunarwatcher.java.haileybot.utils

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.mod.ModGuild
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import java.text.MessageFormat
import java.util.*

private val random = Random(System.currentTimeMillis())

fun <T> List<T>.randomItem(): T? = if (this.isEmpty()) null else this[random.nextInt(this.size)];
fun <T> List<T>.randomItem(fallback: T): T = if (this.isEmpty()) fallback else this[random.nextInt(this.size)];

fun String.messageFormat(vararg objects: Any): String = MessageFormat.format(this.replace("'", "''"), *objects)

fun Message.canUserRunAdminCommand(bot: HaileyBot): Boolean =
        member.hasPermission(Permission.ADMINISTRATOR) ||
                this.canUserRunBotAdminCommand(bot) ||
                author.idLong == guild.ownerIdLong

fun Message.canUserRunAdminCommand(bot: HaileyBot, permission: Permission): Boolean = canUserRunAdminCommand(bot) || member.hasPermission(permission)

fun Message.canUserRunAdminCommand(bot: HaileyBot, permissions: Array<Permission>): Boolean = permissions.map { this.canUserRunAdminCommand(bot, it) }.any { it }

fun Message.canUserRunBotAdminCommand(bot: HaileyBot): Boolean = bot.botAdmins.contains(author.idLong)
fun Message.getModGuild(bot: HaileyBot): ModGuild? = bot.moderator.getGuild(this.guild.idLong)

fun Message.scheduleDeletion(time: Long) {
    Thread() {
        try {
            Thread.sleep(time);
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        this.delete().queue()
    }.start()
}

fun StringBuilder.newLine() = this.nl()
fun StringBuilder.nl(): StringBuilder = this.append("\n");

fun StringBuilder.newLines(count: Int) = this.nl(count);
fun StringBuilder.nl(count: Int): StringBuilder = this.append("\n".repeat(if (count <= 0) throw IllegalArgumentException() else count))

fun StringBuilder.appendLine(data: String) = this.appendln(data)
fun StringBuilder.appendln(data: String): StringBuilder = this.append(data).nl()

fun String.fitDiscordLengthRequirements(allowedLen: Int): List<String> {
    if (this.isEmpty() || this.isBlank() || this.equals("``````"))
        return listOf();

    val code = this.startsWith("```")
    val targetLen = if (code) allowedLen - 6 else allowedLen

    val result = mutableListOf<String>()

    var i = 0;
    while (true) {
        val range = if (i + targetLen > length) {
            length - i;
        } else {
            if (i == 0 && code) targetLen + 3
            else targetLen
        }
        var subset = subSequence(i, i + range).toString();
        if (code && !subset.startsWith("```"))
            subset = "```$subset";
        if (code && !subset.endsWith("```"))
            subset = "$subset```"
        result.add(subset);
        i += range;

        if (range < targetLen) break;
    }

    return result;
}

fun User.hasPermissions(guild: Guild, any: Boolean, vararg permissions: Permission): Boolean {
    val member = guild.getMember(this) ?: return false
    if (any) {
        for (permission in permissions)
            if (member.hasPermission(permission))
                return true;
        return false;
    } else {
        return member.hasPermission(*permissions)
    }
}
