package io.github.lunarwatcher.java.haileybot.utils;

public class TypeUtils {
    public static void assertType(Object t, Class<?> clazz){
        if(!clazz.isInstance(t))
            throw new AssertionError();
    }
}
