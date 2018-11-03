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

package io.github.lunarwatcher.java.haileybot.commands.watching;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.PrivateChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public boolean doesLocationMatch(Message message) {
        if (message.getChannel() instanceof PrivateChannel || message.getGuild() == null) {
            logger.debug("Ignoring matching. Channel is a: " + message.getChannel().getClass().getName());
            return false;
        }
        long id;
        if (guild) id = message.getGuild().getIdLong();
        else id = message.getChannel().getIdLong();

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

    public List<String> getRegex() {
        return regex;
    }

    public long getLocationId() {
        return locationId;
    }

    public boolean getGuild() {
        return guild;
    }
}