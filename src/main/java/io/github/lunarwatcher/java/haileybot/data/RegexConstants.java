package io.github.lunarwatcher.java.haileybot.data;

import java.util.regex.Pattern;

/**
 * Contains regex constants, mainly used for the spam detector.
 */
public class RegexConstants {
    /**
     * Gets rid of pesky invite and add my account bots
     */
    public static final Pattern INVITE_SPAM =
            Pattern.compile("(?i)(?:discord\\.gg/[a-z0-9]+|discordapp\\.com/invite/[a-z0-9]+|(?:pl(?:[sz]|ease)\\W*)?" +
                    "add\\W*(?:me\\W*)?(?:pl(?:[sz]|ease)\\W*)?.*\\(?(?:tag|#)\\)?\\W*\\d+(?:#\\d+)?)");
    /**
     * Nukes twitch and rabb.it, and twitter.
     *
     * All of these are legitimate sites, but they've frequently been abused by spammers to get views, follows,
     * etc. There's also no legitimate reason to have links in usernames anyways, so this is more or less
     * designed to fight back against the spam.
     */
    public static final Pattern GENERAL_SPAM =
            Pattern.compile("(?i)(?:free\\W*games|follow\\W*me)?(?:\\W*at\\W*)?(?:(?:twitch\\W*tv|rabb.it|twitter.com)/.*(?:#\\d+)?)");
}
