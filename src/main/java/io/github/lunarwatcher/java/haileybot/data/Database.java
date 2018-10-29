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

import io.github.lunarwatcher.java.haileybot.utils.JacksonParser;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is the memory core of the bot, it saves all the data it gets into a .json database.
 * .json is used because implementing SQL is overkill and using regular .txt files is a mess.
 */
@SuppressWarnings("unchecked")
public class Database {
    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    public final Path file;
    private Map<String, Object> cache = new HashMap<>();
    private boolean changed = false;

    public Database(@NotNull Path file) throws IOException {
        this.file = file;

        if (Files.exists(file)) {
            try {
                load();
            } catch (Exception e) {
                e.printStackTrace();
                logger.warn("Failed to load database.");
            }
        } else {

            Files.createFile(file);
        }
    }

    /**
     * Loads existing data from the file.
     *
     * @throws IOException if there's a problem reading the file
     */
    private void load() throws IOException {
        cache = JacksonParser.getJsonParser()
                .parse(Files.newInputStream(file));
    }

    /**
     * Get a value
     *
     * @param key The key to retrieve
     * @return a value, or null if key not found
     */
    public Object get(String key) {
        return cache.get(key);
    }

    /**
     * Put something into the data. Does not update until {@link #commit()} is called
     *
     * @param key The key of the value to add
     * @param value The associated value
     */
    public void put(String key, Object value) {
        cache.put(key, value);
        changed = true;

        commit();
    }

    /**
     * Commit the changes and write the data to the file
     */
    public void commit() {
        if (!changed) {
            return;
        }

        StandardOpenOption[] options;
        if (Files.exists(file)) {
            options = new StandardOpenOption[]{StandardOpenOption.TRUNCATE_EXISTING};
        } else {
            options = new StandardOpenOption[]{};
        }

        try {
            JacksonParser.getJsonParser()
                    .saveData(cache, Files.newBufferedWriter(file, options), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        changed = false;
    }


    public Map<String, Object> getMap(String key) {
        return (Map<String, Object>) get(key);
    }

    public List<Object> getList(String key) {
        return (List<Object>) get(key);
    }

    public boolean isEmpty() {
        return cache.isEmpty();
    }

    public int size() {
        return cache.size();
    }

    public void remove(String baseKey) {
        if (!cache.containsKey(baseKey))
            return;
        cache.remove(baseKey);
    }
}