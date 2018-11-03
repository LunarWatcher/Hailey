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
import io.github.lunarwatcher.java.haileybot.commands.watching.RegexMatch;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RegexWatcher {
    private static final String BASE_KEY = "regex-watches";
    private static final String CHANNEL_KEY = BASE_KEY + "-channel";
    private static final String GUILD_KEY = BASE_KEY + "-guild";

    private static final Logger logger = LoggerFactory.getLogger(RegexWatcher.class);

    private HaileyBot bot;
    private Map<Long, List<RegexMatch>> stored;

    public RegexWatcher(HaileyBot bot) {
        this.bot = bot;
        stored = new HashMap<>();
        load();
        bot.getDatabase().remove(BASE_KEY);
        logger.info("Loaded the regex watcher. {} entries in the map.", stored.size());
    }

    public boolean watch(long user, long guild, long channel, String regex) {
        try {
            Pattern.compile(regex);
        } catch (Exception e) {
            return false;
        }
        AtomicBoolean existing = new AtomicBoolean(false);
        stored.computeIfAbsent(user, k -> new ArrayList<>());
        stored.get(user).forEach((it) -> {
            long id;

            if (it.getGuild() && channel == -2) id = guild;
            else {
                if (channel == -2)
                    return;
                id = channel;
            }
            if (it.getLocationId() == id) {
                it.getRegex().add(regex);
                it.patternify();
                existing.set(true);
            }
        });

        if (!existing.get()) {
            if (channel != -2)
                stored.get(user).add(new RegexMatch(regex, channel, false));
            else stored.get(user).add(new RegexMatch(regex, guild, true));
        }
        return true;

    }

    public boolean unwatch(long user, long guild, long channel, String regex) {
        RegexMatch affected = null;
        if (stored.get(user) == null)
            return false;

        List<RegexMatch> watches = stored.get(user);
        if (watches.size() == 0)
            return false;

        for (RegexMatch match : watches) {
            long id;
            if (match.getGuild() && channel == -2) id = guild;
            else {
                if (channel == -2) {
                    continue;
                }
                id = channel;
            }

            if (match.getLocationId() == id) {
                if (match.getRegex().contains(regex)) {
                    match.getRegex().remove(regex);
                    affected = match;
                    break;
                } else if (regex.equalsIgnoreCase("all")) {
                    match.getRegex().clear();
                    affected = match;
                    break;
                }
            }
        }
        if (affected != null) {
            if (affected.getRegex() == null || affected.getRegex().isEmpty())
                stored.get(user).remove(affected);
            return true;
        }

        return false;
    }

    public void checkMessageForMatch(Message message) {
        if (stored.size() == 0)
            return;

        for (Map.Entry<Long, List<RegexMatch>> entry : stored.entrySet()) {

            long user = entry.getKey();
            if (user == message.getAuthor().getIdLong())
                continue;
            for (RegexMatch match : entry.getValue()) {
                if (!match.doesLocationMatch(message))
                    continue;
                if (match.matches(message.getContentRaw())) {
                    message.getChannel().sendMessage("Regex match. /cc <@" + entry.getKey() + "> ").queue();
                }
            }
        }
    }

    private void load() {
        loadMap(CHANNEL_KEY, false);
        loadMap(GUILD_KEY, true);
    }

    private void loadMap(String key, boolean guild) {
        Map<String, Object> users = bot.getDatabase().getMap(key);
        if (users != null) {
            for (Map.Entry<String, Object> entry : users.entrySet()) {
                long user = Long.parseLong(entry.getKey());
                stored.computeIfAbsent(user, i -> new ArrayList<>());

                //noinspection unchecked
                Map<String, List<String>> value = (Map<String, List<String>>) entry.getValue();
                for (Map.Entry<String, List<String>> nested : value.entrySet()) {

                    stored.get(user).add(new RegexMatch(nested.getValue(), Long.valueOf(nested.getKey()), guild));
                }

            }
        }
    }

    public void save() {
        Map<String, Map<Long, List<String>>> channels = new HashMap<>();
        Map<String, Map<Long, List<String>>> guilds = new HashMap<>();

        for (Map.Entry<Long, List<RegexMatch>> matches : stored.entrySet()) {
            String user = String.valueOf(matches.getKey());
            for (RegexMatch match : matches.getValue()) {
                if (match.getGuild()) {
                    addWatch(match, user, guilds);
                } else addWatch(match, user, channels);
            }

        }

        bot.getDatabase().put(CHANNEL_KEY, channels);
        bot.getDatabase().put(GUILD_KEY, guilds);
    }

    private void addWatch(RegexMatch match, String user, Map<String, Map<Long, List<String>>> data) {

        if (data.containsKey(user)) {
            data.get(user).put(match.getLocationId(), match.getRegex());
        } else {
            Map<Long, List<String>> toAdd = new HashMap<>();
            toAdd.put(match.getLocationId(), match.getRegex());
            data.put(user, toAdd);
        }
    }

    public List<RegexMatch> getWatchesForUser(long uid) {
        if (stored.get(uid) == null) return new ArrayList<>();
        return stored.get(uid);
    }


    public void clearWatchesForGuild(long guildId) {
        JDA client = bot.getClient();

        for (Map.Entry<Long, List<RegexMatch>> entry : stored.entrySet()) {
            List<RegexMatch> matches = entry.getValue();
            for (RegexMatch match : matches) {
                if (match.getGuild()) {
                    if (match.getLocationId() == guildId) {
                        match.getRegex().clear();
                    }
                } else {
                    Channel channel = client.getTextChannelById(match.getLocationId());
                    if (channel.getGuild().getIdLong() == guildId) {
                        match.getRegex().clear();
                    }
                }
            }
        }

        cleanStored();
    }

    public void clearWatchesForUser(long userId, long guildId) {
        if (stored.get(userId) == null) return;

        for (RegexMatch match : stored.get(userId)) {
            if (match.getGuild()) {
                if (match.getLocationId() == guildId) {
                    match.getRegex().clear();
                }
            } else {
                Channel channel = bot.getClient().getTextChannelById(match.getLocationId());
                if (channel.getGuild().getIdLong() == guildId) {
                    match.getRegex().clear();
                }
            }

        }

        cleanStored();

    }


    private void cleanStored() {
        stored.forEach((key, value) -> {
            if (value.stream().map(RegexMatch::getRegex).flatMap(List::stream).collect(Collectors.toList()).size() == 0) {
                value.clear();
            } else {
                value.removeIf((it) -> it.getRegex().size() == 0);
            }
        });

        Set<Long> entries = stored.keySet();

        for (long key : entries) {
            if (stored.get(key).size() == 0)
                stored.remove(key);
        }
    }

    @NotNull
    public Map<Long, List<RegexMatch>> getWatchesInGuild(@NotNull Guild guild) {
        Map<Long, List<RegexMatch>> watches = new HashMap<>();

        stored.forEach((key, val) -> {
            List<RegexMatch> matches = val.stream().filter(it -> {
                if (it.getGuild()) {
                    return guild.getIdLong() == it.getLocationId();
                } else {
                    Channel channel = bot.getClient().getTextChannelById(it.getLocationId());
                    return channel != null && channel.getGuild().getIdLong() == it.getLocationId();
                }
            }).collect(Collectors.toList());
            if (matches != null && !matches.isEmpty()) {
                watches.put(key, matches);
            }
        });
        return watches;
    }
}
