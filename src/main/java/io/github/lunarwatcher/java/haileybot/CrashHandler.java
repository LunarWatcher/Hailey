package io.github.lunarwatcher.java.haileybot;

import io.github.lunarwatcher.java.haileybot.utils.ExtensionsKt;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.obj.IUser;

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
                IUser user = bot.getClient().getUserByID(uid);
                if (user != null) {
                    try {
                        user.getOrCreatePMChannel().sendMessage("Something bad happened :c");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
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
