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

package io.github.lunarwatcher.java.haileybot.commands.roles.auto;

import io.github.lunarwatcher.java.haileybot.HaileyBot;
import io.github.lunarwatcher.java.haileybot.commands.Command;
import io.github.lunarwatcher.java.haileybot.utils.ExtensionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.Permissions;

import java.util.ArrayList;
import java.util.List;

public class AutoAssignCommand implements Command {
    private static final List<String> aliases = new ArrayList<>();

    static {
        aliases.add("add-auto-assignable");
        aliases.add("autoassign");
    }

    @Override
    public String getName() {
        return "addAutoAssignable";
    }

    @Override
    public @Nullable List<String> getAliases() {
        return aliases;
    }

    @Override
    public @Nullable String getHelp() {
        return "Sets a role to auto-assign. Supports multiple roles. Using this command adds a role to auto-assign new users with. ";
    }

    @Override
    public @Nullable String getDescription() {
        return getHelp();
    }

    @Override
    public void onMessage(HaileyBot bot, @NotNull IMessage message, String rawMessage, String commandName) {
        if (message.getChannel() instanceof IPrivateChannel) {
            message.getChannel().sendMessage("This is a DM channel. No mod tools available.");
            return;
        }
        if (!ExtensionsKt.canUserRunAdminCommand(message, bot)) {
            message.getChannel().sendMessage("You can't do that.");
            return;
        }

        if (rawMessage.isEmpty()) {
            message.getChannel().sendMessage("Which role should be auto-assignable?");
            return;
        }

        if (message.getClient().getOurUser().getPermissionsForGuild(message.getGuild()).stream().noneMatch((it) -> it == Permissions.MANAGE_ROLES || it == Permissions.ADMINISTRATOR)) {
            message.getChannel().sendMessage("WARNING: I don't have the \"manage roles\" or the \"administrator\" permission (I need one of them to assign roles).");

        }

        List<IRole> roles = message.getGuild().getRoles();
        for (IRole role : roles) {
            if (role.getName().equals(rawMessage)) {
                boolean result = bot.getAssigner().addAutoRole(message.getGuild().getLongID(), role);
                if (result)
                    message.getChannel().sendMessage("Successfully registered the role `" + rawMessage + "` as auto-assignable.");
                else
                    message.getChannel().sendMessage("Failed to register the role `" + rawMessage + "` as auto-assignable.");
                return;
            }
        }
        message.getChannel().sendMessage("I couldn't find that role. Note that roles are case-sensitive.");

    }
}
