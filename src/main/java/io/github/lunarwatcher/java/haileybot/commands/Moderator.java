package io.github.lunarwatcher.java.haileybot.commands;

import io.github.lunarwatcher.java.haileybot.HaileyBot;
import io.github.lunarwatcher.java.haileybot.mod.ModGuild;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.audit.ActionType;
import sx.blah.discord.handle.audit.entry.AuditLogEntry;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageEditEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserBanEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserLeaveEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;

import java.util.HashMap;
import java.util.Map;

public class Moderator {
    private static final Logger logger = LoggerFactory.getLogger(Moderator.class);

    public static final String INVITE_FEATURE = "invite_spam_blocking";
    public static final String AUDIT_FEATURE = "audit_channel";
    public static final String WELCOME_LOGGING = "welcome_logging";
    public static final String LEAVE_LOGGING = "leave_logging";

    public static final String JOIN_MESSAGE = "join_message";
    public static final String LEAVE_MESSAGE = "leave_message";
    public static final String JOIN_DM = "join_dm";
    public static final String DELETION_WATCHER = "deletion_watcher";

    public static final String BAN_MONITORING_FEATURE = "ban_monitoring";

    private static final String features = combine(INVITE_FEATURE, WELCOME_LOGGING, AUDIT_FEATURE, LEAVE_LOGGING,
            JOIN_MESSAGE, LEAVE_MESSAGE, JOIN_DM, DELETION_WATCHER, BAN_MONITORING_FEATURE);

    private HaileyBot bot;
    private Map<Long, ModGuild> enabledGuilds;

    public Moderator(HaileyBot bot) {
        this.bot = bot;
        enabledGuilds = new HashMap<>();
        load();
    }

    public void save(){
        Map<String, Map<String, Object>> data = new HashMap<>();
        for(Map.Entry<Long, ModGuild> modGuild : enabledGuilds.entrySet()){
            data.put(Long.toString(modGuild.getKey()), modGuild.getValue().getDataAsMap());
        }

        bot.getDatabase().put("moderator", data);
    }

    public void load(){
        Map<String, Object> data = bot.getDatabase().getMap("moderator");
        if (data == null)
            return;
        for(Map.Entry<String, Object> entry : data.entrySet()){
            long id = Long.parseLong(entry.getKey());
            try {
                Map<String, Object> dat = (Map<String, Object>) entry.getValue();
                ModGuild guild = new ModGuild(bot, id);
                guild.loadFromMap(dat);
                enabledGuilds.put(id, guild);
            }catch(ClassCastException e){
                e.printStackTrace();
            }

        }
    }

    // Events

    public void userJoined(UserJoinEvent event){
        ModGuild guild = enabledGuilds.get(event.getGuild().getLongID());
        if(guild == null)
            return;
        guild.userJoined(event);

    }

    public boolean messageReceived(MessageReceivedEvent event){
        if(event.getChannel() instanceof IPrivateChannel)
            return false;
        ModGuild guild = enabledGuilds.get(event.getGuild().getLongID());
        if(guild == null)
            return false;
        logger.info("Message received in {} (UID {}), channel {} . Author: {} (UID {}): \"{}\"",
                event.getGuild().getName(), event.getGuild().getLongID(), event.getChannel().getName(),
                event.getAuthor().getName(), event.getAuthor().getLongID(), event.getMessage().getContent());
        guild.messageReceived(event);
        return false;
    }

    public void messageEdited(MessageEditEvent event){
        if(event.getChannel() instanceof IPrivateChannel)
            return;
    }

    public void messageFilter(MessageReceivedEvent event){
        if(event.getChannel() instanceof IPrivateChannel)
            return;
    }


    public void userLeft(UserLeaveEvent event){
        ModGuild guild = enabledGuilds.get(event.getGuild().getLongID());
        if(guild == null)
            return;

        guild.userLeft(event);
    }

    public void messageDeleted(MessageDeleteEvent event){
        if(event.getChannel() instanceof IPrivateChannel)
            return;
    }

    // Meta

    public boolean registerGuild(IGuild guild){
        if(enabledGuilds.containsKey(guild.getLongID()))
            return false;

        enabledGuilds.put(guild.getLongID(), new ModGuild(bot, guild.getLongID()));
        return true;
    }

    public boolean removeGuild(IGuild guild){
        if(!enabledGuilds.containsKey(guild.getLongID()))
            return false;
        enabledGuilds.remove(guild.getLongID());
        return true;
    }

    public boolean isGuildEnabled(IGuild guild){
        return enabledGuilds.containsKey(guild.getLongID());
    }

    @Nullable
    public ModGuild getGuild(long id){
        return enabledGuilds.get(id);
    }

    public static String getFeatures(){
        return features;
    }

    private static String combine(String... args){
        StringBuilder res = new StringBuilder();
        for(int i = 0; i < args.length; i++){
            res.append(args[i]);
            if(args.length != args.length - 1){
                res.append(", ");
            }
        }
        return res.toString();
    }

    public void userBanned(UserBanEvent event) {

        ModGuild guild = enabledGuilds.get(event.getGuild().getLongID());
        if(guild == null)
            return;

        if(guild.getBanMonitoring()) {

            String usernameAndDiscriminator = event.getUser().getName() + "#" + event.getUser().getDiscriminator();
            long uid = event.getUser().getLongID();
            if (!event.getClient().getOurUser().getPermissionsForGuild(event.getGuild()).contains(Permissions.VIEW_AUDIT_LOG)
                    || !event.getClient().getOurUser().getPermissionsForGuild(event.getGuild()).contains(Permissions.ADMINISTRATOR)) {
                guild.audit("A user has been banned, but I'm missing the permissions necessary to see the details. ");
                guild.audit(banEmbed(usernameAndDiscriminator, uid, "Unknown", "Unknown"));
            }else {
                AuditLogEntry entry = event.getGuild()
                        .getAuditLog(ActionType.MEMBER_BAN_ADD)
                        .getEntriesByTarget(event.getUser().getLongID())
                        .get(0);
                IUser responsible = entry.getResponsibleUser();

                String banner = getUsername(responsible);
                String reason = entry.getReason().orElse("No reason specified");

                guild.audit(banEmbed(usernameAndDiscriminator, uid, banner, reason));
            }
        }

    }

    public static String getUsername(IUser user){
        return user.getName() + "#" + user.getDiscriminator();
    }

    public static EmbedObject banEmbed(String usernameAndDiscriminator, Long uid, String banner, String reason){
        return new EmbedBuilder()
                .withTitle("User banned")
                .withDesc(concenateDetails("Name", usernameAndDiscriminator))
                .appendDesc(concenateDetails("UID", String.valueOf(uid)))
                .appendDesc(concenateDetails("Banned by", banner))
                .appendDesc(concenateDetails("Reason", reason))
                .build();
    }

    public static String concenateDetails(String title, String content){
        return "**" + title + "**: " + content + "\n";
    }
}
