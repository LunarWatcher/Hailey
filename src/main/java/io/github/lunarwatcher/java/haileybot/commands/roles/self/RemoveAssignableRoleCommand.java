package io.github.lunarwatcher.java.haileybot.commands.roles.self;

import io.github.lunarwatcher.java.haileybot.HaileyBot;
import io.github.lunarwatcher.java.haileybot.commands.Command;
import io.github.lunarwatcher.java.haileybot.utils.ExtensionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.Permissions;

import java.util.List;
import java.util.stream.Collectors;

public class RemoveAssignableRoleCommand implements Command {
    private HaileyBot bot;

    public RemoveAssignableRoleCommand(HaileyBot bot) {
        this.bot = bot;
    }

    @Override
    public String getName() {
        return "removeAssignable";
    }

    @Override
    public @Nullable List<String> getAliases() {
        return null;
    }

    @Override
    public @Nullable String getHelp() {
        return "Removes an assignable role.";
    }

    @Override
    public @Nullable String getDescription() {
        return getHelp();
    }

    @Override
    public void onMessage(@NotNull IMessage message, String rawMessage, String commandName) {
        if(message.getChannel() instanceof IPrivateChannel){
            message.getChannel().sendMessage("This is a DM channel. No mod tools available.");
            return;
        }
        if(!ExtensionsKt.canUserRunAdminCommand(message, bot)){
            message.getChannel().sendMessage("You can't do that.");
            return;
        }

        if(rawMessage.isEmpty()){
            message.getChannel().sendMessage("Which role do you want to remove from being self-assignable?");
            return;
        }

        if(message.getClient().getOurUser().getPermissionsForGuild(message.getGuild()).stream().noneMatch((it) -> it == Permissions.MANAGE_ROLES || it == Permissions.ADMINISTRATOR)){
            message.getChannel().sendMessage("WARNING: I don't have the \"manage roles\" or the \"administrator\" permission (I need one of them to assign roles).");
        }

        List<IRole> roles = bot.getAssigner().getRolesForGuild(message.getGuild().getLongID());
        if(roles == null){
            message.getChannel().sendMessage("No self-assignable roles are registered.");
            return;
        }
        for(IRole role : roles) {
            if (role.getName().equals(rawMessage)) {
                boolean result = bot.getAssigner().removeRole(message.getGuild().getLongID(), role);
                if (result)
                    message.getChannel().sendMessage("Successfully removed the role `" + rawMessage + "` as self-assignable.");
                else
                    message.getChannel().sendMessage("Failed to remove the role `" + rawMessage + "` as self-assignable.");
                return;
            }
        }
        message.getChannel().sendMessage("I couldn't find that role. Note that roles are case-sensitive.");

    }
}
