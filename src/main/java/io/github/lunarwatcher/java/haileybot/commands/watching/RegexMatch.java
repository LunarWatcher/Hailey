package io.github.lunarwatcher.java.haileybot.commands.watching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.impl.obj.PrivateChannel;
import sx.blah.discord.handle.obj.IMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RegexMatch {
    private static final Logger logger = LoggerFactory.getLogger(RegexMatch.class);
    private static final long MIN_TIMEOUT = 120000;
    private List<String> regex;
    private Pattern pattern;
    private long locationId;
    private long lastMatch = 0;

    private boolean guild;

    public RegexMatch(String regex, long locationId, boolean guild) {
        this.regex = new ArrayList<>();
        this.regex.add(regex);

        this.locationId = locationId;
        this.guild = guild;

        patternify();
    }

    public RegexMatch(List<String> regex, long locationId, boolean guild) {
        this.regex = regex;
        this.locationId = locationId;
        this.guild = guild;

        patternify();
    }

    public boolean doesLocationMatch(IMessage message){
        if(message.getChannel() instanceof PrivateChannel || message.getGuild() == null){
            logger.warn("Ignoring matching. Channel is a: " + message.getChannel().getClass().getName());
            return false;
        }
        long id;
        if(guild) id = message.getGuild().getLongID();
        else id = message.getChannel().getLongID();

        return locationId == id;
    }

    public void patternify() {
        StringBuilder res = new StringBuilder("((?i)");
        for (int i = 0; i < regex.size(); i++) {
            res.append(regex.get(i));
            if (i != regex.size() - 1) res.append("|");
        }
        res.append(")");
        pattern = Pattern.compile(res.toString());
    }

    public boolean matches(String content) {
        if (System.currentTimeMillis() - lastMatch < MIN_TIMEOUT) {
            return false;
        }
        boolean match = pattern.matcher(content).find();
        lastMatch = System.currentTimeMillis();
        return match;
    }

    public List<String> getRegex(){
        return regex;
    }

    public long getLocationId() {
        return locationId;
    }

    public boolean getGuild(){
        return guild;
    }
}