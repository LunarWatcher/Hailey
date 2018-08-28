package io.github.lunarwatcher.java.haileybot.commands.roles;

import io.github.lunarwatcher.java.haileybot.HaileyBot;
import io.github.lunarwatcher.java.haileybot.commands.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.Permissions;

import java.util.Arrays;
import java.util.List;

public class UnassignCommand implements Command {
    private HaileyBot bot;

    public UnassignCommand(HaileyBot bot) {
        this.bot = bot;
    }

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
        return "Assigns a self-assignable role. Note that the roles are case sensitive";
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
        List<IRole> roles = bot.getAssigner().getRolesForGuild(message.getGuild().getLongID());
        if(roles == null || roles.size() == 0){
            message.reply("There are no self-(un)assignable roles.");
            return;
        }

        if(message.getClient().getOurUser().getPermissionsForGuild(message.getGuild()).stream().noneMatch((it) -> it == Permissions.MANAGE_ROLES || it == Permissions.ADMINISTRATOR)){
            message.getChannel().sendMessage("I don't have the \"manage roles\" or the \"administrator\" permission (I need one of them to unassign roles).");
            return;
        }

        if(rawMessage.isEmpty()){
            message.getChannel().sendMessage("Which role do you want to remove? Note: roles are case-sensitive.");
            return;
        }

        bot.getAssigner().unassign(message, rawMessage);
    }
}
