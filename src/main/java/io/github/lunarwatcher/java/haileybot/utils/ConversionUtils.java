package io.github.lunarwatcher.java.haileybot.utils;

import sx.blah.discord.handle.obj.StatusType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConversionUtils {
    private static final String[] yes = {"yes", "true", "on", "enabled"};
    private static final String[] no = {"no", "false", "off", "disabled"};
    private static final Pattern CHANNEL_PATTERN = Pattern.compile("<#!?(\\d+)>");
    private static final Pattern USER_PATTERN = Pattern.compile("<@!?(\\d+)>");

    public static boolean convertToBoolean(String input){
        String lower = input.toLowerCase();
        for(String t : yes){
            if(t.equals(lower)){
                return true;
            }
        }
        for(String f : no){
            if(f.equals(lower))
                return false;
        }
        throw new ClassCastException();
    }

    public static long convertToLong(String input){
        try {
            return Long.parseLong(input);
        }catch(NumberFormatException e){
            return Long.valueOf(input);
        }
    }

    public static long parseChannel(String input){
        Matcher matcher = CHANNEL_PATTERN.matcher(input);
        if(!matcher.find()){
            try{
                return convertToLong(input);
            }catch(NumberFormatException e){
                return -2;
            }
        }

        return convertToLong(matcher.group(1));
    }

    public static long parseUser(String input){
        Matcher matcher = USER_PATTERN.matcher(input);
        if(!matcher.find()) {
            try{
                return convertToLong(input);
            }catch(NumberFormatException e){
                return -2;
            }
        }

        return convertToLong(matcher.group(1));
    }

    public static String convertStatusToString(StatusType status){

        switch (status){
            case ONLINE:
                return "online";
            case OFFLINE:
                return "offline";
            case DND:
                return "do not disturb";
            case IDLE:
                return "idle";
            case INVISIBLE:
                return "invisible";
            default:
                return "unknown";
        }
    }
}
