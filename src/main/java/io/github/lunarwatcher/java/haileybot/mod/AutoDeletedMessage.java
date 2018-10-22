package io.github.lunarwatcher.java.haileybot.mod;

import sx.blah.discord.handle.obj.IMessage;

public class AutoDeletedMessage {
    private final IMessage message;
    private final AutoBanReasons reason;

    public AutoDeletedMessage(IMessage message, AutoBanReasons reason) {
        this.message = message;
        this.reason = reason;
    }

    public IMessage getMessage() {
        return message;
    }

    public AutoBanReasons getReason() {
        return reason;
    }
}
