package io.github.lunarwatcher.java.haileybot.commands.mod.utils;

import io.github.lunarwatcher.java.haileybot.HaileyBot;
import io.github.lunarwatcher.java.haileybot.data.Constants;
import io.github.lunarwatcher.java.haileybot.mod.ModGuild;
import io.github.lunarwatcher.java.haileybot.utils.Factory2;
import io.github.lunarwatcher.java.haileybot.utils.ConversionUtils;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;

import java.util.List;

public class ModUtils {
    private static ModUtils instance;

    private HaileyBot bot;

    private ModUtils (HaileyBot bot){
        this.bot = bot;
    }

    public static void initialize(HaileyBot bot){
        instance = new ModUtils(bot);
    }

    public static synchronized ModUtils getInstance(){
        if(instance == null)
            throw new NullPointerException();
        return instance;
    }
    public static void onMessageRun(IMessage message, String rawMessage, Permissions permission,
                                        Factory2<Boolean, InternalDataForwarder, IMessage> handleUser){
        if(getInstance().bot.getModerator().getGuild(message.getGuild()) == null){
            message.reply("this isn't a mod-enabled guild. Please run `" + Constants.TRIGGER + "enableMod` to access this feature.");
            return;
        }
        if(!message.getClient().getOurUser().getPermissionsForGuild(message.getGuild()).contains(permission)
                && !message.getClient().getOurUser().getPermissionsForGuild(message.getGuild()).contains(Permissions.ADMINISTRATOR)){
            message.reply("I do not have the appropriate permissions to do that.");
            return;
        }
        if(permission != null){
            if(!message.getAuthor().getPermissionsForGuild(message.getGuild()).contains(permission)
                    && !message.getAuthor().getPermissionsForGuild(message.getGuild()).contains(Permissions.ADMINISTRATOR)){
                message.reply("you don't have the necessary permissions to do that");
                return;
            }
        }else{
            message.reply("the permission is null. As a security precausion, this command cannot be used");
            return;
        }
        if(handleUser == null || rawMessage == null)
            throw new NullPointerException();
        List<IUser> mentions = message.getMentions();
        String reason = rawMessage.replaceAll("<@!?\\d+>", "").trim();
        if(mentions.size() == 0){
            try{
                Long uid = ConversionUtils.parseUser(rawMessage);
                IUser user = message.getClient()
                        .fetchUser(uid);
                if(user == null){
                    message.reply("specify who to ban with either a mention, or their UID");
                    return;
                }

                boolean result = safeAccept(handleUser, new InternalDataForwarder(user, uid, reason), message);
                handleResult(result, message);
            }catch(Exception e){
                message.reply("specify who to ban with either a mention, or their UID");
                return;
            }
        }else{
            int count = mentions.size();
            if(count > 1){
                message.reply("mass banning/kicking isn't supported due to security reasons.");
                return;
            }
            IUser user = mentions.get(0);
            if(user.getLongID() == message.getClient().getOurUser().getLongID()){
                message.reply("I can't ban/kick myself. If you really want me to leave, please do so manually.");
                return;
            }else if(user.getLongID() == message.getAuthor().getLongID()){
                message.reply("You can't ban/kick yourself.");
                return;
            }


            if(reason.length() == 0 || reason.replace(" ", "").length() == 0)
                reason = "No reason.";
            boolean result = safeAccept(handleUser, new InternalDataForwarder(user, user.getLongID(), reason), message);
            handleResult(result, message);
        }
    }

    private static void handleResult(boolean result, IMessage message){
        if(result)
            message.reply("successfully completed the action.");
        else
            message.reply("failed to complete the action.");
    }

    private static boolean safeAccept(Factory2<Boolean, InternalDataForwarder, IMessage> fun, InternalDataForwarder data, IMessage message){

        try{
            return fun.accept(data, message);
        }catch(Exception e){
            e.printStackTrace();
            message.reply("Failed: " + e.getMessage());
            return false;
        }
    }

    public static boolean banHandler(InternalDataForwarder user, IMessage message) {
        if (user.useLong()){
            if(user.hasUID())
                message.getGuild().banUser(user.id);
            else{
                message.reply("I failed to find that user.");
                return false;
            }
        }else
            message.getGuild().banUser(user.user);
        audit(message.getGuild(), createEmbedLog("Ban", user, message.getAuthor()));
        return true;
    }

    public static boolean kickHandler(InternalDataForwarder user, IMessage message) {
        if(user.useLong()) {
            message.reply("I failed to find the user.");
            return false;
        }
        message.getGuild().kickUser(user.user);
        audit(message.getGuild(), createEmbedLog("Kick", user, message.getAuthor()));
        return true;
    }

    public static boolean unbanHandler(InternalDataForwarder user, IMessage message){
        if(!user.hasUID()) {
            message.reply("You need a valid UID to do that.");
            return false;
        }
        message.getGuild().pardonUser(user.id);
        audit(message.getGuild(), createEmbedLog("Unban", user, message.getAuthor()));

        return true;
    }

    private static EmbedObject createEmbedLog(String mode, InternalDataForwarder forwarder, IUser handler){
        return new EmbedBuilder()
                .withTitle(mode)
                .withDesc("**User taken action against:** " + forwarder.getName() + " (UID: " + forwarder.getId() + ")\n")
                .appendDesc("**Moderator:** " + handler.getName() + "#" + handler.getDiscriminator() + " (UID " + handler.getLongID() + ")\n")
                .appendDesc("**Reason:** " + forwarder.reason)
                .build();
    }

    private static void audit(IGuild guild, String message){
        ModGuild modGuild = getInstance().bot.getModerator().getGuild(guild);
        if(modGuild == null)
            return;

        modGuild.audit(message);
    }

    private static void audit(IGuild guild, EmbedObject message){
        ModGuild modGuild = getInstance().bot.getModerator().getGuild(guild);
        if(modGuild == null)
            return;

        modGuild.audit(message);
    }

    /**
     * This <b>should not be used outside this class.</b> It's public to access the functions defined earlier (see {@link ModUtils#banHandler(InternalDataForwarder, IMessage)},
     * {@link ModUtils#kickHandler(InternalDataForwarder, IMessage)}, and {@link ModUtils#unbanHandler(InternalDataForwarder, IMessage)}).
     */
    public static final class InternalDataForwarder{
        private IUser user;
        private Long id;
        private String reason;

        public InternalDataForwarder(IUser user, Long id, String reason){
            this.user = user;
            this.id = id;
            this.reason = reason;
        }

        public boolean useLong(){
            return user == null;
        }

        public boolean hasUID(){
            return id != 0;
        }

        public String getName(){
            if(user == null) return "Unknown username.";
            return user.getName() + "#" + user.getDiscriminator();
        }

        public long getId(){
            if(id == 0 && user == null)
                return 0;
            else if(id == 0)
                return user.getLongID();
            return id;
        }
    }

}
