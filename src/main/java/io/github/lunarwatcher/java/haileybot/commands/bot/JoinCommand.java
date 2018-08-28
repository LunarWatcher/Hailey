package io.github.lunarwatcher.java.haileybot.commands.bot;

import io.github.lunarwatcher.java.haileybot.commands.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sx.blah.discord.handle.obj.IMessage;

import java.util.Arrays;
import java.util.List;

public class JoinCommand implements Command {
    public JoinCommand() {

    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public @Nullable List<String> getAliases() {
        return Arrays.asList("summon");
    }

    @Override
    public @Nullable String getHelp() {
        return "Sends a link that makes it possible to summon the bot to a server";
    }

    @Override
    public @Nullable String getDescription() {
        return getHelp();
    }

    @Override
    public void onMessage(@NotNull IMessage message, String rawMessage, String commandName) {
        message.getChannel().sendMessage("Click this link to authorize me: https://discordapp.com/oauth2/authorize?client_id=" + message.getClient().getApplicationClientID() + "&scope=bot&permissions=8" );
    }
}
