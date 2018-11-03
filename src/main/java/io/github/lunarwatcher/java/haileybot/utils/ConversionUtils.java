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

package io.github.lunarwatcher.java.haileybot.utils;

import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConversionUtils {
    private static final String[] yes = {"yes", "true", "on", "enabled"};
    private static final String[] no = {"no", "false", "off", "disabled"};
    private static final Pattern CHANNEL_PATTERN = Pattern.compile("<#!?(\\d+)>");
    private static final Pattern USER_PATTERN = Pattern.compile("<@!?(\\d+)>");

    public static boolean convertToBoolean(String input) {
        String lower = input.toLowerCase();
        for (String t : yes) {
            if (t.equals(lower)) {
                return true;
            }
        }
        for (String f : no) {
            if (f.equals(lower))
                return false;
        }
        throw new ClassCastException();
    }

    public static long convertToLong(String input) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            return Long.valueOf(input);
        }
    }

    public static long parseChannel(String input) {
        return parseChannel(input, true);
    }

    public static long parseChannel(String input, boolean convertRawToLong) {
        if (input == null) return -2;
        Matcher matcher = CHANNEL_PATTERN.matcher(input);
        if (!matcher.find()) {
            if (!convertRawToLong)
                return -2;
            try {
                return convertToLong(input);
            } catch (NumberFormatException e) {
                return -2;
            }
        }

        return convertToLong(matcher.group(1));
    }

    public static long parseUser(String input) {
        Matcher matcher = USER_PATTERN.matcher(input);
        if (!matcher.find()) {
            try {
                return convertToLong(input);
            } catch (NumberFormatException e) {
                return -2;
            }
        }

        return convertToLong(matcher.group(1));
    }

    public static @NotNull String convertStatusToString(OnlineStatus status) {

        switch (status) {
            case ONLINE:
                return "Online";
            case OFFLINE:
                return "Offline";
            case DO_NOT_DISTURB:
                return "Do not disturb";
            case IDLE:
                return "Idle";
            case INVISIBLE:
                return "Invisible";
            default:
                return "Unknown";
        }
    }

    public static @NotNull String getGame(Member user) {
        Game game = user.getGame();
        if(game == null)
            return "None.";
        Game.GameType activity = game.getType();

        if (activity != null) {
            String type = activity.name().toLowerCase();
            String what = game.getName();
            String url = game.getUrl();

            String result = type;
            if (what != null)
                result += ": " + what;
            if (url != null)
                result += " (<" + url + ">)";

            return result;

        } else return "None.";
    }

    public static @NotNull String parseVerificationLevel(Guild.VerificationLevel level) {
        switch (level) {
            case NONE:
                return "None";
            case LOW:
                return "Low (requires a valid email)";
            case MEDIUM:
                return "Medium (needs to be registered for > 5 minutes)";
            case HIGH:
                return "High (must also have been a member for > 10 minutes)";
            case VERY_HIGH:
                return "Extreme (must have a verified phone)";
            default:
                return "Unknown";
        }
    }

    public static boolean isInt(String toVerify) {
        if (toVerify.isEmpty() || toVerify.trim().isEmpty())
            return false;
        try {
            Integer.parseInt(toVerify);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
