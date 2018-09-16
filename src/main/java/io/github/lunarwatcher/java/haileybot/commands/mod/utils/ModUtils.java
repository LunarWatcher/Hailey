package io.github.lunarwatcher.java.haileybot.commands.mod.utils;

import io.github.lunarwatcher.java.haileybot.utils.Factory2;
import io.github.lunarwatcher.java.haileybot.utils.ConversionUtils;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;

import java.util.List;

public class ModUtils {
    public static void onMessageRun(IMessage message, String rawMessage, Permissions permission,
                                        Factory2<Boolean, InternalDataForwarder, IMessage> handleUser){
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
        if(message == null || handleUser == null || rawMessage == null)
            throw new NullPointerException();
        List<IUser> mentions = message.getMentions();
        if(mentions.size() == 0){
            try{
                Long uid = ConversionUtils.parseUser(rawMessage);
                IUser user = message.getClient()
                        .fetchUser(uid);
                if(user == null){
                    message.reply("specify who to ban with either a mention, or their UID");
                    return;
                }

                boolean result = safeAccept(handleUser, new InternalDataForwarder(user, uid), message);
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
            boolean result = safeAccept(handleUser, new InternalDataForwarder(user, user.getLongID()), message);
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
        return true;
    }

    public static boolean kickHandler(InternalDataForwarder user, IMessage message) {
        if(user.useLong()) {
            message.reply("I failed to find the user.");
            return false;
        }
        message.getGuild().kickUser(user.user);
        return true;
    }

    public static boolean unbanHandler(InternalDataForwarder user, IMessage message){
        if(!user.hasUID()) {
            message.reply("You need a valid UID to do that.");
            return false;
        }
        message.getGuild().pardonUser(user.id);
        return true;
    }

    /**
     * This <b>should not be used outside this class.</b> It's public to access the functions defined earlier (see {@link ModUtils#banHandler(InternalDataForwarder, IMessage)},
     * {@link ModUtils#kickHandler(InternalDataForwarder, IMessage)}, and {@link ModUtils#unbanHandler(InternalDataForwarder, IMessage)}).
     */
    public static final class InternalDataForwarder{
        private IUser user;
        private Long id;

        public InternalDataForwarder(IUser user, Long id){
            this.user = user;
            this.id = id;
        }

        public boolean useLong(){
            return user == null;
        }

        public boolean hasUID(){
            return id != 0;
        }
    }

}
