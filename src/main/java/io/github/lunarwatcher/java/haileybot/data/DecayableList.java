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

package io.github.lunarwatcher.java.haileybot.data;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Threadless decayable list
 *
 * @param <T> The type to store.
 */
public class DecayableList<T> implements Iterable<T> {
    private final long decayTime;
    private final List<Decayable> storage = new ArrayList<>();

    /**
     * Initializes the list
     *
     * @param decayTime The time before an item dies.
     */
    public DecayableList(long decayTime) {
        if (decayTime <= 0)
            throw new IllegalArgumentException("The decay time can not be <= 0");

        this.decayTime = decayTime;
    }

    public void add(T item) {
        decay();
        storage.add(new Decayable(item));
    }

    /**
     * Handles the actual item decay.
     * It's called in multiple places to help with the GC, but it's only called when methods of the list are.
     * This is to avoid threading, which could quickly turn into a mess.
     */
    private void decay() {
        if (storage.size() == 0)
            return;
        long now = System.currentTimeMillis();
        storage.removeIf(it -> now - it.expirationDate > decayTime);
    }

    public boolean hasAny() {
        decay();
        return !storage.isEmpty();
    }

    public boolean hasAnyLike(Predicate<T> predicate) {
        return storage.stream().anyMatch(it -> predicate.test(it.item));
    }

    public boolean contains(T item) {
        if (storage.size() == 0)
            return false;
        decay();
        return storage.stream().anyMatch(it -> it == item || it.equals(item));
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        decay();
        return storage.stream().map(it -> it.item)
                .iterator();
    }

    public Stream<T> stream() {
        return storage.stream().map(it -> it.item);
    }


    /**
     * Immutable, decayable, item.
     */
    private class Decayable {
        /**
         * the item for this Decayable
         */
        final T item;

        /**
         * The expiration date. This is automatically initialized in the constructor, using the decay time
         * defined for the list instance.
         */
        final long expirationDate;

        public Decayable(T item) {
            this.item = item;
            expirationDate = System.currentTimeMillis() + decayTime;
        }
    }

}
