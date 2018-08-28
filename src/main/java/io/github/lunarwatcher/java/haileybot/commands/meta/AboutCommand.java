package io.github.lunarwatcher.java.haileybot.commands.meta;

import io.github.lunarwatcher.java.haileybot.commands.Command;
import io.github.lunarwatcher.java.haileybot.data.Constants;
import org.jetbrains.annotations.NotNull;
import sx.blah.discord.handle.obj.IMessage;

import java.util.List;

public class AboutCommand implements Command {
    public AboutCommand() {

    }

    @Override
    public String getName() {
        return "about";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getHelp() {
        return "Tells you about me";
    }

    @Override
    public String getDescription() {
        return getHelp();
    }

    @Override
    public void onMessage(@NotNull IMessage message, String rawMessage, String commandName) {
        message.getChannel()
                .sendMessage("Hiya! I'm Hailey, another product of Olivia#0740's boredom. Currently running version '" + Constants.VERSION + "'.");

    }
}
