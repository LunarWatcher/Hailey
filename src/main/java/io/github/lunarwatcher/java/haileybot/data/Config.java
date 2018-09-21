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
     * Link to the GitHub repository, or a fork. Points to <a href="https://github.com/LunarWatcher/Hailey">/LunarWatcher/Hailey</a>
     * by default.
     */
    private String github;
    private String token;

    public Config(Properties properties) {
        this.properties = properties;

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

    public Properties getProperties() {
        return properties;
    }
}

