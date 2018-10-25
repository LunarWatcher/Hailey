package io.github.lunarwatcher.java.haileybot.mod;

import org.jetbrains.annotations.NotNull;
import sx.blah.discord.handle.obj.IUser;

public class AutoBannedUser {
    private final @NotNull IUser bannedUser;
    private final @NotNull AutoBanReasons internalReason;

    public AutoBannedUser(@NotNull IUser bannedUser, @NotNull AutoBanReasons internalReason) {
        this.bannedUser = bannedUser;
        this.internalReason = internalReason;
    }

    public IUser getBannedUser() {
        return bannedUser;
    }

    public AutoBanReasons getBanReason() {
        return internalReason;
    }

    public String getStringReason() {
        return internalReason.getReason();
    }

}
