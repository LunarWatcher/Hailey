package io.github.lunarwatcher.java.haileybot.data;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class Constants {
    private Constants(){}
    public static final String VERSION = "0.013.001 \"Debugging nightmare\"";
    public static final String TRIGGER = "h!";
    public static final boolean ALLOW_MENTION_TRIGGER = true;

    public static final String DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";

    public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy @ ss:mm:hh")
            .withZone(ZoneId.systemDefault());
}
