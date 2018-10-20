package io.github.lunarwatcher.java.haileybot.data;

import java.util.ArrayList;

public class SizeLimitedList<T> extends ArrayList<T> {
    private int maxCap;

    public SizeLimitedList(int maxCap) {
        if(maxCap <= 0)
            throw new IllegalArgumentException("The size cannot be negative.");
        this.maxCap = maxCap;
    }

    @Override
    public boolean add(T item) {
        if(this.size() > maxCap) {

            if (size() - maxCap > 0) {
                super.subList(0, size() - maxCap).clear();
            }
        }
        if (this.size() == maxCap) {
            super.remove(0);
        }
        return super.add(item);
    }

    public boolean hasAny() {
        return size() != 0;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

}
