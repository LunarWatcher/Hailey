package io.github.lunarwatcher.java.haileybot.utils;

public class NumberUtils {
    private static final String ONE = "st";
    private static final String TWO = "nd";
    private static final String THREE = "rd";
    private static final String NTH = "th";

    public static String getNumberWithNth(long value){
        boolean isNegative = value < 0;

        long tens = value % 10;
        long hundreds = value % 100;

        if(equalsPlusMinus(tens, 1) && hundreds != 11)
            return stringify(value, ONE);
        else if(equalsPlusMinus(tens, 2) && hundreds != 12)
            return stringify(value, TWO);
        else if(equalsPlusMinus(tens, 3) && hundreds != 13)
            return stringify(value, THREE);

        return stringify(value, NTH);
    }

    public static boolean equalsPlusMinus(long val, long equals){
        return val == equals || val == -equals;
    }

    private static String stringify(long num, String postfix){
        return String.valueOf(num) + postfix;
    }
}
