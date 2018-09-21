package io.github.lunarwatcher.java.haileybot.commands.watching;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RegexMatch {
    private static final long MIN_TIMEOUT = 120000;
    private List<String> regex;
    private Pattern pattern;
    private long guild;
    private long lastMatch = 0;

    public RegexMatch(String regex, long guild) {
        this.regex = new ArrayList<>();
        this.regex.add(regex);

        this.guild = guild;

        patternify();
    }

    public RegexMatch(List<String> regex, long guild) {
        this.regex = regex;
        this.guild = guild;

        patternify();
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

    public long getGuild() {
        return guild;
    }
}