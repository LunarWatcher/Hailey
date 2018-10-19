package io.github.lunarwatcher.java.haileybot.data;

import java.util.regex.Pattern;

public class RegexConstants {
    public static final Pattern INVITE_SPAM = Pattern.compile("(?i)(?:discord\\.gg/[a-z0-9]+|discordapp\\.com/invite/[a-z0-9]+|(?:pl(?:[sz]|ease)\\W*)?add\\W*(?:me\\W*)?(?:pl(?:[sz]|ease)\\W*)?.*\\(?(?:tag|#)\\)?\\W*\\d+(?:#\\d+)?)");
    public static final Pattern GENERAL_SPAM = Pattern.compile("(?i)(?:free\\W*games\\W*at\\W*)?(?:twitch.tv/.*(?:#\\d+)?)");
}
