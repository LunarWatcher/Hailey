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

import io.github.lunarwatcher.java.haileybot.CrashHandler;
import io.github.lunarwatcher.java.haileybot.HaileyBot;
import io.github.lunarwatcher.java.haileybot.data.Constants;
import io.github.lunarwatcher.java.haileybot.utils.ExtensionsKt;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Game.GameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PresenceManager {
    private final Logger logger = LoggerFactory.getLogger(PresenceContent.class);
    private static final long DELAY = 30;
    private static final long DELAY_MILLIS = TimeUnit.SECONDS.toMillis(DELAY);
    private final HaileyBot bot;
    private final JDA client;

    private final List<PresenceContent> presences = new ArrayList<>();

    private PresenceContent activePresence;

    // Time management
    private long lastSet;
    private long lastUpdated;

    public PresenceManager(HaileyBot bot) {
        this.bot = bot;
        this.client = bot.getClient();

        configurePresence();
    }

    private void configurePresence() {
        presences.add(new PresenceContent(GameType.WATCHING, "$guilds snowy guilds <3 | " + Constants.TRIGGER + "help", null));
        presences.add(new PresenceContent(GameType.DEFAULT, "with the pack", null));
        presences.add(new PresenceContent(GameType.DEFAULT, "with the source code", null));
        presences.add(new PresenceContent(GameType.LISTENING, "the ocean", null));
        presences.add(new PresenceContent(GameType.WATCHING, "the never-ending feed at $guilds guilds | " + Constants.TRIGGER + "help", null));
        presences.add(new PresenceContent(GameType.WATCHING, "for spammers | Protecting $modguilds guilds.", null));
        presences.add(new PresenceContent(GameType.DEFAULT, "with my tail in $guilds guilds \uD83D\uDC9B", null));
        presences.add(new PresenceContent(GameType.DEFAULT, "in the snow", null));

        logger.info("Loaded PresenceManager: {} messages ready.");
    }

    public void onReady() {
        if (presences.size() == 0) {
            logger.warn("No presences!!");
            return;
        }
        bot.getExecutor()
                .scheduleAtFixedRate(this::update, 0, DELAY, TimeUnit.MINUTES);
    }

    private void update() {
        try {
            logger.info("Now updating presence");
            PresenceContent presence = ExtensionsKt.randomItem(presences);

            if (presence == null) {
                logger.warn("Failed to get a presence to display!");
                return;
            }
            lastSet = System.currentTimeMillis();
            this.activePresence = presence;
            setPresence();
        } catch (Throwable e) {
            CrashHandler.error(e);
        }
    }

    public void refresh() {

        if (activePresence == null)
            return;
        long now = System.currentTimeMillis();
        //because of the way this method is called, there needs to be limits.

        // First of all, if there's 2 minutes or less 'til we update anyways, don't.
        // This is just a waste of resources, since it is really close to updating.
        // The limit is randomly set; this is mainly to avoid too frequent updates.
        if (now - lastSet > DELAY_MILLIS - 120000)
            return;

        // Finally, we check the time since the last update. If it's been more than 20 seconds, we should be fine to
        // update again. This is to avoid issues with mass-sending of the guild creation event, which can happen
        // as a result of downtime.
        if (now - lastUpdated > 20000)
            return;

        logger.info("Refreshing presence");
        lastUpdated = System.currentTimeMillis();
        setPresence();
    }

    private void setPresence() {
        String message = activePresence.getMessage(bot, client);
        GameType type = activePresence.getType();
        String url = activePresence.getUrl();
        if (client.getPresence().getGame() != null) {
            if (message.toLowerCase().equalsIgnoreCase(client.getPresence().getGame().getName())) {
                logger.warn("Ignoring update. Found duplicated content: \"{}\" currently set vs \"{}\" requested.", client.getPresence().getGame().getName(), message);
                return;
            }
        }

        logger.debug("Updating presence to: {}, {}, {}", type, message, url);
        client.getPresence().setGame(Game.of(type, message, url));
    }


}
