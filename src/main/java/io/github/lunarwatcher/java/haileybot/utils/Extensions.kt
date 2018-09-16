package io.github.lunarwatcher.java.haileybot.utils

import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.mod.ModGuild
import org.apache.commons.lang3.StringUtils
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.Permissions
import java.text.MessageFormat
import java.util.*

private val random = Random(System.currentTimeMillis())

fun <T> List<T>.randomItem() : T? = if(this.isEmpty()) null else this[random.nextInt(this.size)];

fun String.messageFormat(vararg objects: Any) : String = MessageFormat.format(this.replace("'", "''"), *objects)

fun IMessage.canUserRunAdminCommand(bot: HaileyBot) : Boolean =
        author.getPermissionsForGuild(guild).contains(Permissions.ADMINISTRATOR) ||
                this.canUserRunBotAdminCommand(bot) ||
                author.longID == guild.ownerLongID

fun IMessage.canUserRunAdminCommand(bot: HaileyBot, permission: Permissions) : Boolean
        = canUserRunAdminCommand(bot) || author.getPermissionsForGuild(guild).contains(permission)

fun IMessage.canUserRunAdminCommand(bot: HaileyBot, permissions: Array<Permissions>) : Boolean
        = permissions.map { this.canUserRunAdminCommand(bot, it) }.any { it }

fun IMessage.canUserRunBotAdminCommand(bot: HaileyBot) : Boolean = bot.botAdmins.contains(author.longID)
fun IMessage.getModGuild(bot: HaileyBot) : ModGuild? = bot.moderator.getGuild(this.guild.longID)

fun IMessage.scheduleDeletion(time: Long) {
    Thread(){
        try{
            Thread.sleep(time);
        }catch(e: InterruptedException){
            e.printStackTrace()
        }
        if(!this.isDeleted)
            this.delete()
    }.start()
}

fun StringBuilder.newLine() = this.nl()
fun StringBuilder.nl() = this.append("\n");

fun StringBuilder.newLines(count: Int) = this.nl(count);
fun StringBuilder.nl(count: Int) = this.append(StringUtils.repeat("\n", if(count <= 0) throw IllegalArgumentException() else count))

fun StringBuilder.appendLine(data: String) = this.appendln(data)
fun StringBuilder.appendln(data: String) = this.append(data).nl()