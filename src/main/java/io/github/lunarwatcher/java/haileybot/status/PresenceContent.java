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

package io.github.lunarwatcher.java.haileybot.status;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Game;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Contains a single message displayed as presence.
 * This contains the type of activity, the message to display, and an optional URL.
 * The message can also be dymanic; using <code>$guild</code> or <code>$members</code>, it's possible to replace
 * parts of the message with the guild count or total amount of members across servers. Note that the amount of
 * members are the amount of <i>unique</i> members. This means, if there are 100 members over 2 servers,
 * the method will display 100 members, even if all the members are in both the servers.
 * This, internally, leads to more instances of the Member class, which is counted when replacing the members.
 *
 */
public class PresenceContent {
    private final @NotNull Game.GameType type;
    private final @NotNull String message;
    private final @Nullable String url;

    public PresenceContent(@NotNull Game.GameType type, @NotNull String message, @Nullable String url) {
        this.type = type;
        this.message = message;
        this.url = url;
    }

    public Game.GameType getType() {
        return type;
    }

    public String getMessage(JDA client) {
        return message.replace("$guilds", String.valueOf(client.getGuilds().size()))
                .replace("$members", String.valueOf(client.getGuilds().stream().mapToInt(it -> it.getMembers().size()).sum()));
    }

    public String getUrl() {
        return url;
    }

    public boolean hasUrl(){
        return url != null;
    }
}
