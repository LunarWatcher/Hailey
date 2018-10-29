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

package io.github.lunarwatcher.java.haileybot.commands.meta;

import io.github.lunarwatcher.java.haileybot.CrashHandler;
import io.github.lunarwatcher.java.haileybot.HaileyBot;
import io.github.lunarwatcher.java.haileybot.commands.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sx.blah.discord.handle.obj.IMessage;

import java.util.List;

public class ErrorLogsCommand implements Command {
    @Override
    public String getName() {
        return "logs";
    }

    @Override
    public @Nullable List<String> getAliases() {
        return null;
    }

    @Override
    public @Nullable String getHelp() {
        return "Prints logs";
    }

    @Override
    public @Nullable String getDescription() {
        return getHelp();
    }

    @Override
    public void onMessage(HaileyBot bot, @NotNull IMessage message, String rawMessage, String commandName) {
        if (!bot.getBotAdmins().contains(message.getAuthor().getLongID())) {
            message.reply("no. Bot admins only.");
            return;
        }

        List<String> errorHandler = CrashHandler.splitErrors();
        CrashHandler.clear();
        if (errorHandler.size() == 0) {
            message.getChannel().sendMessage("No logs.");
            return;
        }
        for (String error : errorHandler) {
            if (error.length() == 0)
                continue;
            message.getChannel().sendMessage(error);
        }

    }
}
