package io.github.lunarwatcher.java.haileybot.commands.mod;

import io.github.lunarwatcher.java.haileybot.commands.Command;
import io.github.lunarwatcher.java.haileybot.commands.Moderator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sx.blah.discord.handle.obj.IMessage;

import java.util.List;

public class ModFeaturesCommand implements Command {
    public ModFeaturesCommand() {

    }

    @Override
    public String getName() {
        return "getModFeatures";
    }

    @Override
    public @Nullable List<String> getAliases() {
        return null;
    }

    @Override
    public @Nullable String getHelp() {
        return "Lists all available mod features";
    }

    @Override
    public @Nullable String getDescription() {
        return getHelp();
    }

    @Override
    public void onMessage(@NotNull IMessage message, String rawMessage, String commandName) {

        message.getChannel().sendMessage(Moderator.getFeatures());
    }
}
