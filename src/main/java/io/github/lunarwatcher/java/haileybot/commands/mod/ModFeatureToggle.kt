package io.github.lunarwatcher.java.haileybot.commands.mod

import io.github.lunarwatcher.java.haileybot.CrashHandler
import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.commands.Moderator
import io.github.lunarwatcher.java.haileybot.commands.Moderator.*
import io.github.lunarwatcher.java.haileybot.data.Constants
import io.github.lunarwatcher.java.haileybot.mod.ModGuild
import io.github.lunarwatcher.java.haileybot.utils.ConversionUtils
import io.github.lunarwatcher.java.haileybot.utils.canUserRunAdminCommand
import org.slf4j.LoggerFactory
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IPrivateChannel
import sx.blah.discord.handle.obj.Permissions

/**
 * The primary feature toggle command. It's accessed by the
 * [io.github.lunarwatcher.java.haileybot.commands.mod.humaninterface.HumanlyUnderstandableFeatureToggleCommand]s, in
 * addition to being accessed directly
 */
class ModFeatureToggle(private val bot: HaileyBot) : Command {

    override fun getName(): String {
        return "set"
    }

    override fun getAliases(): List<String>? {
        return null
    }

    override fun getHelp(): String? {
        return "Currently available togglable features: " + Moderator.getFeatures()
    }

    override fun getDescription(): String? {
        return "Toggles moderation features"
    }

    override fun onMessage(message: IMessage, rawMessage: String, commandName: String) {
        if(message.channel is IPrivateChannel){
            message.channel.sendMessage("This is a DM channel. No mod tools available.");
            return;
        }
        if (!message.canUserRunAdminCommand(bot)) {
            message.reply("You need to be a bot admin or have the administrator permission to do that.")
            return
        }
        if (rawMessage.isEmpty()) {
            message.channel.sendMessage("You have to tell me which feature you want to toggle.")
            return
        }

        if (!bot.moderator.isGuildEnabled(message.guild)) {
            message.channel.sendMessage("Guild moderation not enabled. Please run `" + Constants.TRIGGER + "enableMod` to enable it.")
            return
        }

        val splitBySpace = rawMessage.split(" ".toRegex(), 2).toTypedArray()
        if (splitBySpace.size != 2) {
            message.channel.sendMessage("You need to arguments to run this command")
            return
        }
        val feature = splitBySpace[0].trim()
        val mode = splitBySpace[1].trim()

        logger.info("**Enabling features**")

        val guild = bot.moderator.getGuild(message.guild.longID)!!

        when (feature.toLowerCase()) {
            INVITE_FEATURE -> try {
                val toggle = boolean(guild, INVITE_FEATURE, mode)
                message.channel.sendMessage((if (toggle) "Enabled" else "Disabled") + " invite spam protection")
            } catch (e: ClassCastException) {
                message.channel.sendMessage("Failed to convert the mode to type `boolean`. Please only use `true` (enabled) or `false` (disabled)")
            } catch (e: NullPointerException) {
                message.channel.sendMessage("Caught an NPE.")
            }
            BAN_MONITORING_FEATURE -> try{
                val toggle = boolean(guild, BAN_MONITORING_FEATURE, mode);
                message.channel.sendMessage((if(toggle) "Enabled" else "Disabled") + " ban monitoring.");
            } catch(e: ClassCastException){
                message.channel.sendMessage("Failed to convert the mode to type `boolean`. Please only use `true` (enabled) or `false` (disabled).");
            }
            AUDIT_FEATURE -> try {
                val channel = if(mode.toLongOrNull() == null){
                    ConversionUtils.parseChannel(mode);
                }else mode.toLong()
                if(channel == -2L){
                    message.channel.sendMessage("Failed to parse channel.");
                    return;
                }
                message.guild.getChannelByID(channel)
                        .sendMessage("Audit channel set.");
                guild.set(AUDIT_FEATURE, channel)
                message.channel.sendMessage("Successfully set the audit channel.");

            } catch (e: ClassCastException) {
                message.channel.sendMessage("Failed to convert channel to a long ID.")
            }
            WELCOME_LOGGING -> try {
                val channel = if(mode.toLongOrNull() == null){
                    ConversionUtils.parseChannel(mode);
                }else mode.toLong()
                if(channel == -2L){
                    message.channel.sendMessage("Failed to parse channel.");
                    return;
                }
                message.guild.getChannelByID(channel)
                        .sendMessage("Welcome channel set to <#$channel>.");
                guild.set(WELCOME_LOGGING, channel)
                message.channel.sendMessage("Successfully set the welcome channel.  Please run the `set` command using join_message to specify a custom message. <user> is a placeholder if you want to add the user's username to the message, and <server> is for the server name");

            } catch (e: ClassCastException) {
                message.channel.sendMessage("Failed to convert channel to a long ID.")
            }
            LEAVE_LOGGING -> try {
                val channel = if(mode.toLongOrNull() == null){
                    ConversionUtils.parseChannel(mode);
                }else mode.toLong()
                if(channel == -2L){
                    message.channel.sendMessage("Failed to parse channel.");
                    return;
                }

                message.guild.getChannelByID(channel)
                        .sendMessage("Leave channel set to <#$channel>.");
                guild.set(LEAVE_LOGGING, channel)
                message.channel.sendMessage("Successfully set the leave message channel. Please run the `set` command using leave_message to specify a custom message. <user> is a placeholder if you want to add the user's username to the message");

            } catch (e: ClassCastException) {
                message.channel.sendMessage("Failed to convert channel to a long ID.")
            }
            JOIN_MESSAGE -> try{

                guild.set(JOIN_MESSAGE, mode)
                message.channel.sendMessage("Set join message to: \"$mode\"")
            }catch(e: Exception){
                CrashHandler.error(e);
                message.channel.sendMessage("Something went wrong. Check the logs.");
            }

            LEAVE_MESSAGE -> try{

                guild.set(LEAVE_MESSAGE, mode)
                message.channel.sendMessage("Set leave message to: \"$mode\"")
            }catch(e: Exception){
                CrashHandler.error(e);
                message.channel.sendMessage("Something went wrong. Check the logs.");
            }

            JOIN_DM -> try{
                guild.set(JOIN_DM, mode)
                message.channel.sendMessage("Set join DM to: \"$mode\"")
            }catch(e: Exception){
                CrashHandler.error(e);
                message.channel.sendMessage("Something went wrong. Check the logs.");
            }

            else -> message.channel.sendMessage("Could not find the feature $feature (note: checks are case-insensitive)")
        }
    }

    fun boolean(guild: ModGuild, feature: String, raw: String): Boolean{
	    val parsed: Boolean = ConversionUtils.convertToBoolean(raw);
	    guild.set(feature, parsed);
        return parsed;
    }
    companion object {
        private val logger = LoggerFactory.getLogger(ModFeatureToggle::class.java)
    }


}
