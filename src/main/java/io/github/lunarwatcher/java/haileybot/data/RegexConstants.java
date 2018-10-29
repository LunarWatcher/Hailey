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
            Pattern.compile("(?i)(?:discord\\.me.*" +
                    "|selly\\.gg.*" +
                    "|discord\\.gg/[a-z0-9]+" +
                    "|invite\\W*gg/.*" +
                    "|discordapp\\.com/invite/[a-z0-9]+" +
                    "|^.*\\.tumblr.com" +
                    "|" +
                    "(?:pl(?:[sz]|ease)\\W*)?add\\W*(?:me\\W*)?(?:pl(?:[sz]|ease)\\W*)?.*" +
                    "\\(?(?:tag|#|at)\\)?\\W*\\d+(?:#\\d+)?)");
    /**
     * Nukes twitch and rabb.it, and twitter.
     * <p>
     * All of these are legitimate sites, but they've frequently been abused by spammers to get views, follows,
     * etc. There's also no legitimate reason to have links in usernames anyways, so this is more or less
     * designed to fight back against the spam.
     */
    public static final Pattern GENERAL_SPAM =
            Pattern.compile("(?i)(?:free\\W*games|follow\\W*me)?" + // Optional spam sentences
                    "(?:\\W*at)?" + // Appendix to the spam sentences
                    "(?:\\W*(?:twitch\\W*tv|rabb\\W*it|twitter\\W*com|paypal\\W*me).*)"

            );// optional tag

    public static final Pattern UNCAUGHT_SPAM =
            Pattern.compile(
                    "(?i)"// Nothing can hide! <insert evil laugh>
                            //@formatter:off
                    + "(" // This will contain a ton of different stuff; this is the main capture group.
                            // Begin basic spam
                            + "(?:sell(?:ing)|tweet|follow).*senseibin" // Usually spams selling products, mainly bots. Also links to twitter, but that's caught.
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
