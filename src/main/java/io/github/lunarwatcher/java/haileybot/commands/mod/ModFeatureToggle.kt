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

import io.github.lunarwatcher.java.haileybot.CrashHandler
import io.github.lunarwatcher.java.haileybot.HaileyBot
import io.github.lunarwatcher.java.haileybot.commands.Command
import io.github.lunarwatcher.java.haileybot.commands.Moderator
import io.github.lunarwatcher.java.haileybot.commands.Moderator.*
import io.github.lunarwatcher.java.haileybot.data.Constants
import io.github.lunarwatcher.java.haileybot.mod.ModGuild
import io.github.lunarwatcher.java.haileybot.utils.ConversionUtils
import io.github.lunarwatcher.java.haileybot.utils.canUserRunAdminCommand
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.PrivateChannel
import org.jetbrains.annotations.NotNull
import org.slf4j.LoggerFactory

/**
 * The primary feature toggle command. It's accessed by the
 * [io.github.lunarwatcher.java.haileybot.commands.mod.humaninterface.HumanlyUnderstandableFeatureToggleCommand]s, in
 * addition to being accessed directly
 */
class ModFeatureToggle : Command {

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

    override fun onMessage(bot: HaileyBot, message: @NotNull Message, rawMessage: String, commandName: String) {
        if (message.channel is PrivateChannel) {
            message.channel.sendMessage("This is a DM channel. No mod tools available.").queue();
            return;
        }
        if (!message.canUserRunAdminCommand(bot)) {
            message.getChannel().sendMessage("You need to be a bot admin or have the administrator permission to do that.").queue();
            return
        }
        if (rawMessage.isEmpty()) {
            message.channel.sendMessage("You have to tell me which feature you want to toggle.").queue()
            return
        }

        if (!bot.moderator.isGuildEnabled(message.guild)) {
            message.channel.sendMessage("Guild moderation not enabled. Please run `" + Constants.TRIGGER + "enableMod` to enable it.").queue()
            return
        }

        val splitBySpace = rawMessage.split(" ".toRegex(), 2).toTypedArray()
        if (splitBySpace.size != 2) {
            message.channel.sendMessage("You need to arguments to run this command").queue()
            return
        }
        val feature = splitBySpace[0].trim()

        val mode = splitBySpace[1].trim()

        logger.info("**Enabling features**")

        val guild = bot.moderator.getGuild(message.guild.idLong)!!

        when (feature.toLowerCase()) {
            INVITE_FEATURE -> try {
                val toggle = boolean(guild, INVITE_FEATURE, mode)
                message.channel.sendMessage((if (toggle) "Enabled" else "Disabled") + " invite spam protection").queue()
            } catch (e: ClassCastException) {
                message.channel.sendMessage("Failed to convert the mode to type `boolean`. Please only use `true` (enabled) or `false` (disabled)").queue()
            } catch (e: NullPointerException) {
                message.channel.sendMessage("Caught an NPE.").queue()
            }
            BAN_MONITORING_FEATURE -> try {
                val toggle = boolean(guild, BAN_MONITORING_FEATURE, mode);
                message.channel.sendMessage((if (toggle) "Enabled" else "Disabled") + " ban monitoring.").queue();
            } catch (e: ClassCastException) {
                message.channel.sendMessage("Failed to convert the mode to type `boolean`. Please only use `true` (enabled) or `false` (disabled).").queue();
            }
            AUDIT_FEATURE -> try {

                val channel = if (mode == "null") -1 else if (mode.toLongOrNull() == null) {
                    ConversionUtils.parseChannel(mode);
                } else mode.toLong()
                if (channel == -2L) {
                    // -2 is triggered by failed parsing; -1 is used for no channel. That's why only -2 is handled here.
                    message.channel.sendMessage("Failed to parse channel.").queue();
                    return;
                }
                if (channel != -1L)
                    message.guild.getTextChannelById(channel)
                            .sendMessage("Audit channel set.").queue();

                guild.set(AUDIT_FEATURE, channel)
                if (channel != -1L)
                    message.channel.sendMessage("Successfully set the audit channel.").queue();
                else
                    message.channel.sendMessage("Removed the audit channel.").queue();

            } catch (e: ClassCastException) {
                message.channel.sendMessage("Failed to convert channel to a long ID.").queue()
            }
            WELCOME_LOGGING -> try {
                val channel = if (mode == "null") -1 else if (mode.toLongOrNull() == null) {
                    ConversionUtils.parseChannel(mode);
                } else mode.toLong()
                if (channel == -2L) {
                    message.channel.sendMessage("Failed to parse channel.").queue();
                    return;
                }
                if (channel != -1L)
                    message.guild.getTextChannelById(channel)
                            .sendMessage("Welcome channel set to <#$channel>.").queue();
                guild.set(WELCOME_LOGGING, channel)
                if (channel != -1L)
                    message.channel.sendMessage("Successfully set the welcome channel.").queue();
                message.channel.sendMessage("Removed the welcome channel.").queue();
            } catch (e: ClassCastException) {
                message.channel.sendMessage("Failed to convert channel to a long ID.").queue()
            }
            LEAVE_LOGGING -> try {
                val channel = if (mode == "null") -1 else if (mode.toLongOrNull() == null) {
                    ConversionUtils.parseChannel(mode);
                } else mode.toLong()
                if (channel == -2L) {
                    message.channel.sendMessage("Failed to parse channel.").queue();
                    return;
                }
                if (channel != -1L)
                    message.guild.getTextChannelById(channel)
                            .sendMessage("Leave channel set to <#$channel>.").queue();
                guild.set(LEAVE_LOGGING, channel)
                if (channel != -1L)
                    message.channel.sendMessage("Successfully set the leave message channel. Please run the `set` command using leave_message to specify a custom message. <user> is a placeholder if you want to add the user's username to the message").queue();
                message.channel.sendMessage("Removed the leave logging channel.").queue();
            } catch (e: ClassCastException) {
                message.channel.sendMessage("Failed to convert channel to a long ID.").queue()
            }
            JOIN_MESSAGE -> try {

                guild.set(JOIN_MESSAGE, mode)
                if (!mode.equals("null", true))
                    message.channel.sendMessage("Set join message to: \"$mode\"").queue()
                else
                    message.channel.sendMessage("Removed the join message.").queue();

            } catch (e: Exception) {
                CrashHandler.error(e);
                message.channel.sendMessage("Something went wrong. Check the logs.").queue();
            }

            LEAVE_MESSAGE -> try {

                guild.set(LEAVE_MESSAGE, mode)
                if (!mode.equals("null", true))
                    message.channel.sendMessage("Set leave message to: \"$mode\"").queue()
                else
                    message.channel.sendMessage("Removed the leave message.").queue();
            } catch (e: Exception) {
                CrashHandler.error(e);
                message.channel.sendMessage("Something went wrong. Check the logs.").queue();
            }

            JOIN_DM -> try {
                guild.set(JOIN_DM, mode)
                if (!mode.equals("null", true))
                    message.channel.sendMessage("Set join dm to: \"$mode\"").queue()
                else
                    message.channel.sendMessage("Removed the join dm.").queue();
            } catch (e: Exception) {
                CrashHandler.error(e);
                message.channel.sendMessage("Something went wrong. Check the logs.").queue();
            }
            else ->
                message.channel.sendMessage("Could not find the feature `$feature` (note: checks are case-insensitive)").queue()
        }
    }

    fun boolean(guild: ModGuild, feature: String, raw: String): Boolean {
        val parsed: Boolean = ConversionUtils.convertToBoolean(raw);
        guild.set(feature, parsed);
        return parsed;
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ModFeatureToggle::class.java)
    }


}
