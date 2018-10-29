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

package io.github.lunarwatcher.java.haileybot.commands.meta;

import io.github.lunarwatcher.java.haileybot.HaileyBot;
import io.github.lunarwatcher.java.haileybot.commands.Command;
import io.github.lunarwatcher.java.haileybot.data.Config;
import io.github.lunarwatcher.java.haileybot.data.Constants;
import org.jetbrains.annotations.NotNull;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.EmbedBuilder;

import java.awt.*;
import java.util.List;

public class AboutCommand implements Command {

    @Override
    public String getName() {
        return "about";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getHelp() {
        return "Tells you about me";
    }

    @Override
    public String getDescription() {
        return getHelp();
    }

    @Override
    public void onMessage(HaileyBot bot, @NotNull IMessage message, String rawMessage, String commandName) {
        Config props = bot.getConfig();
        message.getChannel()
                .sendMessage(
                        new EmbedBuilder()
                                .withTitle("About me")
                                .withColor(new Color(0, .3f, 1f))
                                .appendField("General", "**Owner:** " + props.getOwner() + "\n" +
                                                "**Creator:** " + Config.CREATOR + "\n" +
                                                "**Source code:** [GitHub](" + props.getGithub() + ")\n" +
                                                "**Version:** " + Constants.VERSION + "\n" +
                                                "**Prefix:** " + Constants.TRIGGER + "\n",
                                        false)
                                .appendField("Technical details", "**Shards:** " + bot.getClient().getShardCount() + "\n" +
                                                "**Current shard index:** " + message.getShard().getInfo()[0] + "\n" +
                                                "**Joined this guild:** " + Constants.dateFormatter.format(message.getGuild().getJoinTimeForUser(message.getClient().getOurUser())) + "\n" +
                                                "**Users affected** " + message.getClient().getGuilds().stream().mapToInt((it) -> it.getUsers().size()).sum() + "\n",
                                        true)
                                .build()
                );

    }
}
