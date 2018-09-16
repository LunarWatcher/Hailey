package io.github.lunarwatcher.java.haileybot.utils;

@FunctionalInterface
public interface Factory2<S, T, U> {
    S accept(T t, U u);
}
