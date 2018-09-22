package io.github.lunarwatcher.java.haileybot.data;

import java.util.ArrayList;

public class SizeLimitedList<T> extends ArrayList<T> {
    private int maxCap;

    public SizeLimitedList(int maxCap) {
        this.maxCap = maxCap;
    }

    @Override
    public boolean add(T item) {
        if(this.size() > maxCap) {

            for(int i = 0; i < size() - maxCap; i++){
                super.remove(0);
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

    public boolean isNotEmpty() {
        return !isEmpty();
    }
}
