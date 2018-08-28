package io.github.lunarwatcher.java.haileybot.utils;

@FunctionalInterface
public interface Function3<A1, A2, A3> {
    void invoke(A1 a1, A2 a2, A3 a3);
}