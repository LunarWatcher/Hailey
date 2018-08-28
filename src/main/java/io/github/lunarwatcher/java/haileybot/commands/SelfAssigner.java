package io.github.lunarwatcher.java.haileybot.commands;

import io.github.lunarwatcher.java.haileybot.HaileyBot;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SelfAssigner {
    private static final Logger logger = LoggerFactory.getLogger(SelfAssigner.class);

    private static final String KEY = "self-assign";

    private HaileyBot bot;
    private Map<Long, List<IRole>> assignableRoles;

    public SelfAssigner(HaileyBot bot) {
        this.bot = bot;
        assignableRoles = new HashMap<>();

        load();
    }

    private void load(){
        Map<String, Object> rawData = bot.getDatabase().getMap(KEY);

        if(rawData != null){
            outer:for(Map.Entry<String, Object> entry : rawData.entrySet()){
                try {
                    long guild = Long.parseLong(entry.getKey());
                    List<Long> roles = (List<Long>) entry.getValue();

                    assignableRoles.computeIfAbsent(guild, k -> new ArrayList<>());
                    IDiscordClient client = bot.getClient();

                    for(Long roleId : roles){
                        IGuild iGuild = client.getGuildByID(guild);
                        if(iGuild == null){
                            continue outer;
                        }
                        List<IRole> rolesWithId = iGuild.getRoles().stream().filter(role -> role.getLongID() == roleId).collect(Collectors.toList());
                        if(rolesWithId.size() == 0){
                            logger.warn("Failed to find role with ID " + roleId + " at guild " + guild);
                            continue;
                        }else {
                            assignableRoles.get(guild).addAll(rolesWithId);
                        }
                    }

                }catch(ClassCastException | NumberFormatException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public void save(){
        Map<Long, List<Long>> formattedRoles = new HashMap<>();
        assignableRoles.forEach((k, v) -> {
            formattedRoles.computeIfAbsent(k, s -> new ArrayList<>());
            formattedRoles.get(k).addAll(v.stream().map(IIDLinkedObject::getLongID).collect(Collectors.toList()));
        });
        bot.getDatabase().put(KEY, formattedRoles);
    }

    public boolean addRole(long guild, IRole role){
        if(assignableRoles.get(guild) != null && assignableRoles.get(guild).contains(role))
            return false;
        assignableRoles.computeIfAbsent(guild, k -> new ArrayList<>());
        assignableRoles.get(guild).add(role);
        return true;
    }

    public boolean removeRole(long guild, IRole role){
        if(assignableRoles.get(guild) == null || !assignableRoles.get(guild).contains(role))
            return false;
        assignableRoles.get(guild).remove(role);
        return true;
    }

    public void assign(IMessage message, String role){
        if(assignableRoles.get(message.getGuild().getLongID()) == null){
            message.getChannel().sendMessage("This guild doesn't have any self-assignable roles.");
            return;
        }

        if(message.getClient().getOurUser().getPermissionsForGuild(message.getGuild()).stream().noneMatch((it) -> it == Permissions.MANAGE_ROLES || it == Permissions.ADMINISTRATOR)){
            message.getChannel().sendMessage("I don't have the \"manage roles\" or the \"administrator\" permission (I need one of them to assign roles)");
            return;
        }

        List<IRole> rolesForGuild = getRolesForGuild(message.getGuild().getLongID());
        if(rolesForGuild == null){
            message.reply("Something went wrong with `getRolesForGuild`");
            return;
        }

        if(rolesForGuild.stream().noneMatch(r -> r.getName().equals(role))){
            message.reply("The role `" + role + "` isn't self-assignable");
            return;
        }

        try {
            message.getAuthor().addRole(message.getGuild().getRolesByName(role).get(0));
            message.getChannel().sendMessage("Added the `" + role + "` role to " + message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator());
        }catch(IndexOutOfBoundsException e){
            message.reply("the role seems to not exist.");
        }

    }

    public void unassign(IMessage message, String role){
        if(assignableRoles.get(message.getGuild().getLongID()) == null){
            message.getChannel().sendMessage("This guild doesn't have any self-assignable roles.");
            return;
        }

        if(message.getClient().getOurUser().getPermissionsForGuild(message.getGuild()).stream().noneMatch((it) -> it == Permissions.MANAGE_ROLES || it == Permissions.ADMINISTRATOR)){
            message.getChannel().sendMessage("I don't have the \"manage roles\" or the \"administrator\" permission (I need one of them to unassign roles)");
            return;
        }

        List<IRole> rolesForGuild = getRolesForGuild(message.getGuild().getLongID());
        if(rolesForGuild == null){
            message.reply("Something went wrong with `getRolesForGuild`");
            return;
        }

        if(rolesForGuild.stream().noneMatch(r -> r.getName().equals(role))){
            message.reply("The role `" + role + "` isn't self-(un)assignable");
            return;
        }

        if(message.getAuthor().getRolesForGuild(message.getGuild()).stream().anyMatch(r -> r.getName().equalsIgnoreCase(role)))

        try {
            message.getAuthor().removeRole(message.getGuild().getRolesByName(role).get(0));
            message.getChannel().sendMessage("Removed the `" + role + "` role from " + message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator());
        }catch(IndexOutOfBoundsException e){
            message.reply("the role seems to not exist.");
        }

    }

    @Nullable
    public List<IRole> getRolesForGuild(long guild){
        return assignableRoles.get(guild);
    }
}
