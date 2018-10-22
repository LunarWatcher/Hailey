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
            Pattern.compile("(?i)(?:discord\\.me.*|discord\\.gg/[a-z0-9]+|discordapp\\.com/invite/[a-z0-9]+|(?:pl(?:[sz]|ease)\\W*)?" +
                    "add\\W*(?:me\\W*)?(?:pl(?:[sz]|ease)\\W*)?.*\\(?(?:tag|#|at)\\)?\\W*\\d+(?:#\\d+)?)");
    /**
     * Nukes twitch and rabb.it, and twitter.
     *
     * All of these are legitimate sites, but they've frequently been abused by spammers to get views, follows,
     * etc. There's also no legitimate reason to have links in usernames anyways, so this is more or less
     * designed to fight back against the spam.
     */
    public static final Pattern GENERAL_SPAM =
            Pattern.compile("(?i)(?:free\\W*games|follow\\W*me)?" + // Optional spam sentences
                    "(?:\\W*at\\W*)?" + // Appendix to the spam sentences
                    "(?:(?:twitch\\W*tv|rabb\\W*it|twitter\\W*com|paypal\\W*me)\\W*" + // Problematic URLs
                    ".*(?:\\W*(?:at\\W*|tag\\W*)?\\d+)?)");// optional tag

    public static final Pattern UNCAUGHT_SPAM =
            Pattern.compile(
                    "(?i)"// Nothing can hide! <insert evil laugh>
                    //@formatter:off
                    + "(" // This will contain a ton of different stuff; this is the main capture group.
                            // Begin basic spam
                            + "(?:sell(?:ing)|tweet).*senseibin" // Usually spams selling products, mainly bots. Also links to twitter, but that's caught.
                            + "|"
                            + "follow\\W*me\\W*(?:at|@)?.*" // catches regular "follow me" stuff. There's no legitimate reason to have this in a username anyways.
                            + "|"
                            + "pl[easz]* donate" // Donation spam; again, no legit reason to have in a normal username.
                            + "|"
                            + "bit\\.ly|ad\\.fly1goo\\.gl" // URL shorteners
                    + ")"
                    // @formatter:on
            );
}
