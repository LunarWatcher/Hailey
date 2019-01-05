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

package io.github.lunarwatcher.java.haileybot;

import io.github.lunarwatcher.java.haileybot.utils.ExtensionsKt;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CrashHandler {
    private static final Logger logger = LoggerFactory.getLogger(CrashHandler.class);
    private static final List<String> errors = new ArrayList<>();

    private static HaileyBot bot;

    private static long lastBugged = 0;

    public static void injectBotClass(HaileyBot bot) {
        CrashHandler.bot = bot;
    }

    public static void error(Throwable e) {
        error(e, true);
    }

    /**
     * Logs the exception, and sends a DM to bot admins if `notify` is true, and the timeout since the last message has
     * passed.
     *
     * @param e      The throwable to log.
     * @param notify Whether to DM bot admins or not.
     */
    public static void error(Throwable e, boolean notify) {
        logger.warn("Crash!!");
        String base = e.toString();
        StringBuilder error = new StringBuilder(base + "\n");
        for (StackTraceElement element : e.getStackTrace()) {
            error.append(StringUtils.repeat(" ", 4)).append("at ").append(element.toString()).append("\n");
        }

        String err = error.toString();
        logger.error(err);

        errors.add(err);

        if (System.currentTimeMillis() - lastBugged > 2 * 60 * 60 * 1000 && notify) {
            lastBugged = System.currentTimeMillis();
            // TODO better system?
            for (long uid : bot.getBotAdmins()) {
                bot.getClient().retrieveUserById(uid).queue(user -> {
                    if (user != null) {
                        try {
                            user.openPrivateChannel().queue((channel) -> channel.sendMessage("Something bad happened :c").queue(null, localErr -> {
                                CrashHandler.error(localErr, false);
                            }));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        logger.warn("Failed to send an error DM to {}: I could not find them.", uid);
                    }
                }, retrieveError -> logger.warn("Failed to find that user: {}", retrieveError.getMessage()));

            }
        }

    }

    public static List<String> splitErrors() {
        StringBuilder builder = new StringBuilder("```");

        for (String e : CrashHandler.errors) {
            builder.append(e);
        }
        builder.append("```");

        String str = builder.toString();
        if (str.toLowerCase().equals("``````")) {
            return new ArrayList<>();
        }


        return ExtensionsKt.fitDiscordLengthRequirements(str, 2000);
    }

    public static void clear() {
        errors.clear();
    }
}
