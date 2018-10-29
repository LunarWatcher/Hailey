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

import java.util.Properties;

public class Config {
    public static final String CREATOR = "Olivia#0740";
    private Properties properties;

    /**
     * Whoever runs the bot
     */
    private String owner;

    /**
     * Link to the GitHub repository, or a forked version if applicable.
     * Points to <a href="https://github.com/LunarWatcher/Hailey">/LunarWatcher/Hailey</a> by default.
     */
    private String github;
    private String token;

    public Config(Properties properties) {
        this.properties = properties;

        refresh();
    }

    public void refresh() {
        this.github = properties.getOrDefault("github", "https://github.com/LunarWatcher/Hailey").toString();
        this.owner = properties.getOrDefault("owner", CREATOR).toString();
        this.token = (String) properties.get("token");
        if (token == null || token.equalsIgnoreCase("your token"))
            throw new NullPointerException("You need to add a token!");
    }

    @NotNull
    public String getToken() {
        return token;
    }

    public String getOwner() {
        return owner;
    }

    public String getGithub() {
        return github;
    }

    /**
     * Returns the raw representation of the properties.
     */
    public Properties getProperties() {
        return properties;
    }
}

