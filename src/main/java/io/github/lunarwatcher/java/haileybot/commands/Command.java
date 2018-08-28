package io.github.lunarwatcher.java.haileybot.commands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sx.blah.discord.handle.obj.IMessage;

import java.util.List;

public interface Command {
    String getName();
    @Nullable
    List<String> getAliases();
    @Nullable
    String getHelp();
    @Nullable
    String getDescription();

    void onMessage(@NotNull IMessage message, String rawMessage, String commandName);

    default boolean matchesCommand(String commandName){
        return getName().equalsIgnoreCase(commandName) || (getAliases() != null && getAliases().stream().anyMatch(commandName::equalsIgnoreCase));
    }

}
