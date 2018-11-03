/*
 * MIT License
 *
 * Copyright (c) 2018 Olivia Zoe
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.github.lunarwatcher.java.haileybot.commands;

import io.github.lunarwatcher.java.haileybot.HaileyBot;
import io.github.lunarwatcher.java.haileybot.utils.ExtensionsKt;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private Map<Long, List<Role>> assignableRoles;
    private Map<Long, List<Role>> autoRoles;

    public RoleAssignmentManager(HaileyBot bot) {
        this.bot = bot;
        assignableRoles = new HashMap<>();

        load();

        logger.info("Loaded the role assignment manager. Loaded assignable roles for {} guilds, and auto-assign roles for {} guilds",
                assignableRoles.size(), autoRoles.size());
    }

    private void load() {
        assignableRoles = parseRoles(KEY_SELF);
        autoRoles = parseRoles(KEY_AUTO);
        logger.info("Loaded the role assignment manager.");

    }

    private Map<Long, List<Role>> parseRoles(String key) {
        logger.info("Now loading {}", key);
        Map<String, Object> rawData = bot.getDatabase().getMap(key);
        JDA client = bot.getClient();
        if (rawData != null) {
            Map<Long, List<Role>> mappedRoles = new HashMap<>();
            outer:
            for (Map.Entry<String, Object> entry : rawData.entrySet()) {
                try {
                    long guild = Long.parseLong(entry.getKey());
                    @SuppressWarnings("unchecked")
                    List<Long> roles = (List<Long>) entry.getValue();

                    mappedRoles.computeIfAbsent(guild, k -> new ArrayList<>());


                    for (Long roleId : roles) {
                        Guild iGuild = client.getGuildById(guild);
                        if (iGuild == null) {
                            continue outer;
                        }
                        List<Role> rolesWithId = iGuild.getRoles().stream()
                                .filter(role -> role.getIdLong() == roleId)
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

    private void saveSet(String key, Map<Long, List<Role>> raw) {
        Map<Long, List<Long>> formattedRoles = new HashMap<>();
        raw.forEach((k, v) -> {
            formattedRoles.computeIfAbsent(k, s -> new ArrayList<>());
            formattedRoles.get(k).addAll(v.stream().map(ISnowflake::getIdLong).collect(Collectors.toList()));
        });
        bot.getDatabase().put(key, formattedRoles);
    }

    public boolean addRole(long guild, Role role) {
        if (assignableRoles.get(guild) != null && assignableRoles.get(guild).contains(role))
            return false;
        assignableRoles.computeIfAbsent(guild, k -> new ArrayList<>());
        assignableRoles.get(guild).add(role);
        return true;
    }

    public boolean removeRole(long guild, Role role) {
        if (assignableRoles.get(guild) == null || !assignableRoles.get(guild).contains(role))
            return false;
        assignableRoles.get(guild).remove(role);
        return true;
    }


    public boolean addAutoRole(long guild, Role role) {
        if (autoRoles.get(guild) != null && autoRoles.get(guild).contains(role))
            return false;
        autoRoles.computeIfAbsent(guild, k -> new ArrayList<>());
        autoRoles.get(guild).add(role);
        return true;
    }

    public boolean removeAutoRole(long guild, Role role) {
        if (autoRoles.get(guild) == null || !autoRoles.get(guild).contains(role))
            return false;
        autoRoles.get(guild).remove(role);
        return true;
    }

    public void assign(Message message, String role) {
        if (assignableRoles.get(message.getGuild().getIdLong()) == null) {
            message.getChannel().sendMessage("This guild doesn't have any self-assignable roles.").queue();
            return;
        }

        if (message.getGuild().getMember(message.getJDA().getSelfUser()).getPermissions().stream().noneMatch((it) -> it == Permission.MANAGE_ROLES || it == Permission.ADMINISTRATOR)) {
            message.getChannel().sendMessage("I don't have the \"manage roles\" or the \"administrator\" permission (I need one of them to assign roles)").queue();
            return;
        }

        List<Role> rolesForGuild = getRolesForGuild(message.getGuild().getIdLong());
        if (rolesForGuild == null) {
            message.getChannel().sendMessage("Something went wrong with `getRolesForGuild`").queue();
            return;
        }

        if (rolesForGuild.stream().noneMatch(r -> r.getName().equals(role))) {
            message.getChannel().sendMessage("The role `" + role + "` isn't self-assignable").queue();
            return;
        }

        try {
            Role r = message.getGuild().getRolesByName(role, false).get(0);
            if (r == null) {
                message.getChannel().sendMessage("I couldn't find that role. It might've been removed from the guild").queue();

                return;
            }
            message.getGuild().getController()
                    .addRolesToMember(message.getMember(), r)
                    .queue();
            message.getChannel().sendMessage("Added the `" + role + "` role to " + message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator()).queue();
        } catch (IndexOutOfBoundsException e) {
            message.getChannel().sendMessage("the role seems to not exist.").queue();

        }

    }

    public void unassign(Message message, String role) {
        if (assignableRoles.get(message.getGuild().getIdLong()) == null) {
            message.getChannel().sendMessage("This guild doesn't have any self-assignable roles.").queue();
            return;
        }

        if (!ExtensionsKt.hasPermissions(message.getJDA().getSelfUser(), message.getGuild(), true, Permission.MANAGE_ROLES, Permission.ADMINISTRATOR)) {
            message.getChannel().sendMessage("I don't have the \"manage roles\" or the \"administrator\" permission (I need one of them to unassign roles)").queue();
            return;
        }

        List<Role> rolesForGuild = getRolesForGuild(message.getGuild().getIdLong());
        if (rolesForGuild == null) {
            message.getChannel().sendMessage("Something went wrong with `getRolesForGuild`").queue();
            return;
        }

        if (rolesForGuild.stream().noneMatch(r -> r.getName().equals(role))) {
            message.getChannel().sendMessage("The role `" + role + "` isn't self-(un)assignable").queue();
            return;
        }

        if (message.getMember().getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase(role)))

            try {
                message.getGuild().getController().removeRolesFromMember(message.getMember(),
                        message.getGuild().getRolesByName(role, false).get(0))
                        .queue();
                message.getChannel().sendMessage("Removed the role \"`" + role + "`\" from " + message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator()).queue();
            } catch (IndexOutOfBoundsException e) {
                message.getChannel().sendMessage("the role seems to not exist.").queue();
            }

    }

    public void onUserJoined(GuildMemberJoinEvent event) {
        long guild = event.getGuild().getIdLong();
        logger.info("{}", autoRoles);
        if (autoRoles.containsKey(guild)) {
            List<Role> roles = autoRoles.get(guild);

            if (roles != null
                    && !roles.isEmpty()) {
                event.getGuild().getController()
                        .addRolesToMember(event.getMember(), roles)
                        .queue();
            }
        }
    }

    @Nullable
    public List<Role> getRolesForGuild(long guild) {
        return assignableRoles.get(guild);
    }

    @Nullable
    public List<Role> getRolesForGuild(Guild guild) {
        return getRolesForGuild(guild.getIdLong());
    }

    @Nullable
    public List<Role> getAutoRolesForGuild(long guild) {
        return autoRoles.get(guild);
    }

    @Nullable
    public List<Role> getAutoRolesForGuild(Guild guild) {
        return getAutoRolesForGuild(guild.getIdLong());
    }

}
