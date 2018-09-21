package io.github.lunarwatcher.java.haileybot.commands;

import io.github.lunarwatcher.java.haileybot.HaileyBot;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.obj.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoleAssignmentManager {
    private static final Logger logger = LoggerFactory.getLogger(RoleAssignmentManager.class);

    private static final String KEY_SELF = "self-assign";
    private static final String KEY_AUTO = "auto-assign";

    private HaileyBot bot;
    private Map<Long, List<IRole>> assignableRoles;
    private Map<Long, List<IRole>> autoRoles;

    public RoleAssignmentManager(HaileyBot bot) {
        this.bot = bot;
        assignableRoles = new HashMap<>();

        load();
    }

    private void load() {
        assignableRoles = parseRoles(KEY_SELF);
        autoRoles = parseRoles(KEY_AUTO);
        logger.info("Loaded the role assignment manager.");

    }

    private Map<Long, List<IRole>> parseRoles(String key) {
        logger.info("Now loading {}", key);
        Map<String, Object> rawData = bot.getDatabase().getMap(key);
        IDiscordClient client = bot.getClient();
        if (rawData != null) {
            Map<Long, List<IRole>> mappedRoles = new HashMap<>();
            outer:
            for (Map.Entry<String, Object> entry : rawData.entrySet()) {
                try {
                    long guild = Long.parseLong(entry.getKey());
                    List<Long> roles = (List<Long>) entry.getValue();

                    mappedRoles.computeIfAbsent(guild, k -> new ArrayList<>());


                    for (Long roleId : roles) {
                        IGuild iGuild = client.getGuildByID(guild);
                        if (iGuild == null) {
                            continue outer;
                        }
                        List<IRole> rolesWithId = iGuild.getRoles().stream()
                                .filter(role -> role.getLongID() == roleId)
                                .collect(Collectors.toList());
                        if (rolesWithId.size() == 0) {
                            logger.warn("Failed to find role with ID " + roleId + " at guild " + guild);
                            continue;
                        } else {
                            mappedRoles.get(guild)
                                    .addAll(rolesWithId);
                        }
                    }

                } catch (ClassCastException | NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            return mappedRoles;
        } else return new HashMap<>();
    }

    public void save() {
        saveSet(KEY_AUTO, autoRoles);
        saveSet(KEY_SELF, assignableRoles);
    }

    private void saveSet(String key, Map<Long, List<IRole>> raw) {
        Map<Long, List<Long>> formattedRoles = new HashMap<>();
        raw.forEach((k, v) -> {
            formattedRoles.computeIfAbsent(k, s -> new ArrayList<>());
            formattedRoles.get(k).addAll(v.stream().map(IIDLinkedObject::getLongID).collect(Collectors.toList()));
        });
        bot.getDatabase().put(key, formattedRoles);
    }

    public boolean addRole(long guild, IRole role) {
        if (assignableRoles.get(guild) != null && assignableRoles.get(guild).contains(role))
            return false;
        assignableRoles.computeIfAbsent(guild, k -> new ArrayList<>());
        assignableRoles.get(guild).add(role);
        return true;
    }

    public boolean removeRole(long guild, IRole role) {
        if (assignableRoles.get(guild) == null || !assignableRoles.get(guild).contains(role))
            return false;
        assignableRoles.get(guild).remove(role);
        return true;
    }


    public boolean addAutoRole(long guild, IRole role) {
        if (autoRoles.get(guild) != null && autoRoles.get(guild).contains(role))
            return false;
        autoRoles.computeIfAbsent(guild, k -> new ArrayList<>());
        autoRoles.get(guild).add(role);
        return true;
    }

    public boolean removeAutoRole(long guild, IRole role) {
        if (autoRoles.get(guild) == null || !autoRoles.get(guild).contains(role))
            return false;
        autoRoles.get(guild).remove(role);
        return true;
    }

    public void assign(IMessage message, String role) {
        if (assignableRoles.get(message.getGuild().getLongID()) == null) {
            message.getChannel().sendMessage("This guild doesn't have any self-assignable roles.");
            return;
        }

        if (message.getClient().getOurUser().getPermissionsForGuild(message.getGuild()).stream().noneMatch((it) -> it == Permissions.MANAGE_ROLES || it == Permissions.ADMINISTRATOR)) {
            message.getChannel().sendMessage("I don't have the \"manage roles\" or the \"administrator\" permission (I need one of them to assign roles)");
            return;
        }

        List<IRole> rolesForGuild = getRolesForGuild(message.getGuild().getLongID());
        if (rolesForGuild == null) {
            message.reply("Something went wrong with `getRolesForGuild`");
            return;
        }

        if (rolesForGuild.stream().noneMatch(r -> r.getName().equals(role))) {
            message.reply("The role `" + role + "` isn't self-assignable");
            return;
        }

        try {
            message.getAuthor().addRole(message.getGuild().getRolesByName(role).get(0));
            message.getChannel().sendMessage("Added the `" + role + "` role to " + message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator());
        } catch (IndexOutOfBoundsException e) {
            message.reply("the role seems to not exist.");
        }

    }

    public void unassign(IMessage message, String role) {
        if (assignableRoles.get(message.getGuild().getLongID()) == null) {
            message.getChannel().sendMessage("This guild doesn't have any self-assignable roles.");
            return;
        }

        if (message.getClient().getOurUser().getPermissionsForGuild(message.getGuild()).stream().noneMatch((it) -> it == Permissions.MANAGE_ROLES || it == Permissions.ADMINISTRATOR)) {
            message.getChannel().sendMessage("I don't have the \"manage roles\" or the \"administrator\" permission (I need one of them to unassign roles)");
            return;
        }

        List<IRole> rolesForGuild = getRolesForGuild(message.getGuild().getLongID());
        if (rolesForGuild == null) {
            message.reply("Something went wrong with `getRolesForGuild`");
            return;
        }

        if (rolesForGuild.stream().noneMatch(r -> r.getName().equals(role))) {
            message.reply("The role `" + role + "` isn't self-(un)assignable");
            return;
        }

        if (message.getAuthor().getRolesForGuild(message.getGuild()).stream().anyMatch(r -> r.getName().equalsIgnoreCase(role)))

            try {
                message.getAuthor().removeRole(message.getGuild().getRolesByName(role).get(0));
                message.getChannel().sendMessage("Removed the `" + role + "` role from " + message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator());
            } catch (IndexOutOfBoundsException e) {
                message.reply("the role seems to not exist.");
            }

    }

    public void onUserJoined(UserJoinEvent event) {
        long guild = event.getGuild().getLongID();
        logger.info("{}", autoRoles);
        if (autoRoles.containsKey(guild)) {
            List<IRole> roles = autoRoles.get(guild);

            if (roles != null
                    && !roles.isEmpty()) {
                for (IRole role : roles) {
                    event.getUser().addRole(role);
                }

            }
        }
    }

    @Nullable
    public List<IRole> getRolesForGuild(long guild) {
        return assignableRoles.get(guild);
    }

    @Nullable
    public List<IRole> getRolesForGuild(IGuild guild) {
        return getRolesForGuild(guild.getLongID());
    }

    @Nullable
    public List<IRole> getAutoRolesForGuild(long guild) {
        return autoRoles.get(guild);
    }

    @Nullable
    public List<IRole> getAutoRolesForGuild(IGuild guild) {
        return getAutoRolesForGuild(guild.getLongID());
    }

}
