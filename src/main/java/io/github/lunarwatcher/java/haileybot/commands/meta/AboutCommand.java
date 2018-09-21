package io.github.lunarwatcher.java.haileybot.commands.meta;

import io.github.lunarwatcher.java.haileybot.HaileyBot;
import io.github.lunarwatcher.java.haileybot.commands.Command;
import io.github.lunarwatcher.java.haileybot.data.Config;
import io.github.lunarwatcher.java.haileybot.data.Constants;
import io.github.lunarwatcher.java.haileybot.utils.ConversionUtils;
import org.jetbrains.annotations.NotNull;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.obj.Embed;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.EmbedBuilder;

import java.awt.*;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class AboutCommand implements Command {
    private HaileyBot bot;

    public AboutCommand(HaileyBot bot) {
        this.bot = bot;

    }

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
    public void onMessage(@NotNull IMessage message, String rawMessage, String commandName) {
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
