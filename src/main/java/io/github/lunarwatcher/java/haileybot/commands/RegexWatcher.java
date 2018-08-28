package io.github.lunarwatcher.java.haileybot.commands;

import io.github.lunarwatcher.java.haileybot.HaileyBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.obj.IMessage;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RegexWatcher {
    private static final String KEY = "regex-watches";
    private static final Logger logger = LoggerFactory.getLogger(RegexWatcher.class);

    private HaileyBot bot;
    private Map<Long, List<RegexMatch>> stored;

    public RegexWatcher(HaileyBot bot) {
        this.bot = bot;
        stored = new HashMap<>();
        load();
    }

    public boolean watch(long user, long channel, String regex){
        try{
            Pattern.compile(regex);
        }catch(Exception e){
            return false;
        }
        AtomicBoolean existing = new AtomicBoolean(false);
        stored.computeIfAbsent(user, k -> new ArrayList<>());
        stored.get(user).forEach((it) -> {
            if(it.channel == channel){
                it.regex.add(regex);
                it.patternify();
                existing.set(true);
            }
        });

        if(!existing.get()){
            stored.get(user).add(new RegexMatch(channel, regex));
        }
        return true;

    }

    public boolean unwatch(long user, long channel, String regex){
        RegexMatch affected = null;
        if(stored.get(user) == null)
            return false;

        List<RegexMatch> watches = stored.get(user);
        if(watches.size() == 0)
            return false;

        for (RegexMatch match : watches) {
            if(match.channel == channel){
                if(match.regex.contains(regex)){
                    match.regex.remove(regex);
                    affected = match;
                    break;
                }else if(regex.equalsIgnoreCase("all")){
                    match.regex.clear();
                    affected = match;
                }
            }
        }
        if(affected != null){
            if(affected.regex == null)
                stored.get(user).remove(affected);
            return true;
        }

        return false;
    }

    public void checkMessageForMatch(IMessage message){
        if(stored.size() == 0)
            return;

        for(Map.Entry<Long, List<RegexMatch>> entry : stored.entrySet()) for(RegexMatch match : entry.getValue()) {
            if (match.matches(message.getContent())) {
                message.getChannel().sendMessage("Regex match. <@" + entry.getKey() + "> ");
            }
        }

    }

    private void load(){
        Map<String, Object> users = bot.getDatabase().getMap(KEY);

        if(users != null){
            for(Map.Entry<String, Object> entry : users.entrySet()){
                long user = Long.parseLong(entry.getKey());
                stored.computeIfAbsent(user, i -> new ArrayList<>());

                //noinspection unchecked
                Map<String, List<String>> value = (Map<String, List<String>>) entry.getValue();
                for (Map.Entry<String, List<String>> nested : value.entrySet()) {

                    stored.get(user).add(new RegexMatch(Long.valueOf(nested.getKey()),
                            nested.getValue()));
                }

            }
        }
    }

    public void save(){
        Map<String, Map<Long, List<String>>> data = new HashMap<>();


        for(Map.Entry<Long, List<RegexMatch>> matches : stored.entrySet()){
            String user = String.valueOf(matches.getKey());
            for(RegexMatch match : matches.getValue()) {
                if (data.containsKey(user)) {
                    data.get(user).put(match.channel, match.regex);
                } else {
                    Map<Long, List<String>> toAdd = new HashMap<>();
                    toAdd.put(match.channel, match.regex);
                    data.put(user, toAdd);
                }
            }

        }

        bot.getDatabase().put(KEY, data);

    }

    public List<RegexMatch> getWatchesForUser(long uid){
        if(stored.get(uid) == null) return new ArrayList<>();
        return stored.get(uid);
    }

    public static class RegexMatch{
        private static final long MIN_TIMEOUT = 60000;
        long channel;
        List<String> regex;
        private Pattern pattern;
        private long lastMatch = 0;

        public RegexMatch(long channel, String regex){
            this.channel = channel;
            this.regex = new ArrayList<>();
            this.regex.add(regex);

            patternify();
        }

        public RegexMatch(long channel, List<String> regex){
            this.channel = channel;
            this.regex = regex;

            patternify();
        }

        void patternify(){
            StringBuilder res = new StringBuilder("((?i)");
            for(int i = 0; i < regex.size(); i++){
                res.append(regex.get(i));
                if(i != regex.size() - 1) res.append("|");
            }
            res.append(")");
            pattern = Pattern.compile(res.toString());
        }

        public boolean matches(String content){
            if(System.currentTimeMillis() - lastMatch < MIN_TIMEOUT){
                return false;
            }
            boolean match = pattern.matcher(content).find();
            lastMatch = System.currentTimeMillis();
            return match;
        }
    }
}
