package io.github.lunarwatcher.java.haileybot.commands.mod.general;

import io.github.lunarwatcher.java.haileybot.commands.Command;
import io.github.lunarwatcher.java.haileybot.commands.mod.utils.ModUtils;
import io.github.lunarwatcher.java.haileybot.utils.Factory2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;

import java.util.List;

public class ModerationCommand implements Command {
    @NotNull
    private String name;
    @Nullable
    private List<String> aliases;
    @Nullable
    private String help;
    @Nullable
    private String description;

    @NotNull
    private Permissions permission;

    @NotNull
    private Factory2<Boolean, ModUtils.InternalDataForwarder, IMessage> handler;

    public ModerationCommand(@NotNull String name, @Nullable List<String> aliases, @Nullable String help,
                             @Nullable String description, @NotNull Permissions permission, @NotNull Factory2<Boolean, ModUtils.InternalDataForwarder, IMessage> handler) {
        this.name = name;
        this.aliases = aliases;
        this.help = help;
        this.description = description;
        this.permission = permission;
        this.handler = handler;
    }

    @NotNull
    @Override
    public String getName(){
        return name;
    }

    @Nullable
    @Override
    public List<String> getAliases(){
        return aliases;
    }


    @Nullable
    @Override
    public String getDescription(){
        return description;
    }

    @Nullable
    @Override
    public String getHelp(){
        return help;
    }

    @Override
    public void onMessage(@NotNull IMessage message, String rawMessage, String commandName) {
        ModUtils.onMessageRun(message, rawMessage, permission, handler);
    }

}
