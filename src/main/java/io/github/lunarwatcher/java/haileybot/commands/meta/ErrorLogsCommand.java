package io.github.lunarwatcher.java.haileybot.commands.meta;

import io.github.lunarwatcher.java.haileybot.CrashHandler;
import io.github.lunarwatcher.java.haileybot.HaileyBot;
import io.github.lunarwatcher.java.haileybot.commands.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sx.blah.discord.handle.obj.IMessage;

import java.util.List;

public class ErrorLogsCommand implements Command {
    private HaileyBot bot;

    public ErrorLogsCommand(HaileyBot bot) {
        this.bot = bot;
    }

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
    public void onMessage(@NotNull IMessage message, String rawMessage, String commandName) {
        if (!bot.getBotAdmins().contains(message.getAuthor().getLongID())) {
            message.reply("no");
            return;
        }

        List<String> errorHandler = CrashHandler.splitErrors();
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
