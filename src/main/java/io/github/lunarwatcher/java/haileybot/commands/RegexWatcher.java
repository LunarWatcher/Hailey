package io.github.lunarwatcher.java.haileybot.commands;

import io.github.lunarwatcher.java.haileybot.HaileyBot;
import io.github.lunarwatcher.java.haileybot.commands.watching.RegexMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.obj.IMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class RegexWatcher {
    private static final String KEY = "regex-watches";
    private static final Logger logger = LoggerFactory.getLogger(RegexWatcher.class);

    private HaileyBot bot;
    private Map<Long, List<RegexMatch>> stored;

    public RegexWatcher(HaileyBot bot) {
        this.bot = bot;
        stored = new HashMap<>();
        load();
        logger.info("Loaded the regex watcher. {} entries in the map.", stored.size());
    }

    public boolean watch(long user, long guild, String regex) {
        try {
            Pattern.compile(regex);
        } catch (Exception e) {
            return false;
        }
        AtomicBoolean existing = new AtomicBoolean(false);
        stored.computeIfAbsent(user, k -> new ArrayList<>());
        stored.get(user).forEach((it) -> {
            if (it.getGuild() == guild) {
                it.getRegex().add(regex);
                it.patternify();
                existing.set(true);
            }
        });

        if (!existing.get()) {
            stored.get(user).add(new RegexMatch(regex, guild));
        }
        return true;

    }

    public boolean unwatch(long user, long guild, String regex) {
        RegexMatch affected = null;
        if (stored.get(user) == null)
            return false;

        List<RegexMatch> watches = stored.get(user);
        if (watches.size() == 0)
            return false;

        for (RegexMatch match : watches) {
            if (match.getGuild() == guild) {
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

    public void checkMessageForMatch(IMessage message) {
        if (stored.size() == 0)
            return;

        for (Map.Entry<Long, List<RegexMatch>> entry : stored.entrySet()) {
            long user = entry.getKey();
            if(user == message.getAuthor().getLongID())
                continue;
            for (RegexMatch match : entry.getValue()) {
                if(match.getGuild() != message.getGuild().getLongID())
                    continue;
                if (match.matches(message.getContent())) {
                    message.getChannel().sendMessage("Regex match. /cc <@" + entry.getKey() + "> ");
                }
            }
        }
    }

    private void load() {
        Map<String, Object> users = bot.getDatabase().getMap(KEY);

        if (users != null) {
            for (Map.Entry<String, Object> entry : users.entrySet()) {
                long user = Long.parseLong(entry.getKey());
                stored.computeIfAbsent(user, i -> new ArrayList<>());

                //noinspection unchecked
                Map<String, List<String>> value = (Map<String, List<String>>) entry.getValue();
                for (Map.Entry<String, List<String>> nested : value.entrySet()) {

                    stored.get(user).add(new RegexMatch(nested.getValue(), Long.valueOf(nested.getKey())));
                }

            }
        }
    }

    public void save() {
        Map<String, Map<Long, List<String>>> data = new HashMap<>();


        for (Map.Entry<Long, List<RegexMatch>> matches : stored.entrySet()) {
            String user = String.valueOf(matches.getKey());
            for (RegexMatch match : matches.getValue()) {
                if (data.containsKey(user)) {
                    data.get(user).put(match.getGuild(), match.getRegex());
                } else {
                    Map<Long, List<String>> toAdd = new HashMap<>();
                    toAdd.put(match.getGuild(), match.getRegex());
                    data.put(user, toAdd);
                }
            }

        }

        bot.getDatabase().put(KEY, data);

    }

    public List<RegexMatch> getWatchesForUser(long uid) {
        if (stored.get(uid) == null) return new ArrayList<>();
        return stored.get(uid);
    }


}
