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

package io.github.lunarwatcher.java.haileybot.commands.roles.self;

import io.github.lunarwatcher.java.haileybot.HaileyBot;
import io.github.lunarwatcher.java.haileybot.commands.Command;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.PrivateChannel;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class UnassignCommand implements Command {


    @Override
    public String getName() {
        return "unassign";
    }

    @Override
    public @Nullable List<String> getAliases() {
        return Arrays.asList("iamnot");
    }

    @Override
    public @Nullable String getHelp() {
        return "Unassigns a self-assignable role. Note that the roles are case sensitive";
    }

    @Override
    public @Nullable String getDescription() {
        return getHelp();
    }

    @Override
    public void onMessage(HaileyBot bot, Message message, String rawMessage, String commandName) {
        if (message.getChannel() instanceof PrivateChannel) {
            message.getChannel().sendMessage("This is a DM channel. No mod tools available.").queue();
            return;
        }
        List<Long> roleIds = bot.getAssigner().getRolesForGuild(message.getGuild().getIdLong());
        if (roleIds == null || roleIds.size() == 0) {
            message.getChannel().sendMessage("There are no self-(un)assignable roles.").queue();
            return;
        }

        if (message.getGuild().getMember(message.getJDA().getSelfUser()).getPermissions().stream().noneMatch((it) -> it == Permission.MANAGE_ROLES || it == Permission.ADMINISTRATOR)) {
            message.getChannel().sendMessage("I don't have the \"manage roles\" or the \"administrator\" permission (I need one of them to unassign roles).").queue();
            return;
        }

        if (rawMessage.isEmpty()) {
            message.getChannel().sendMessage("Which role do you want to remove? Note: roles are case-sensitive.").queue();
            return;
        }

        bot.getAssigner().unassign(message, rawMessage);
    }
}
