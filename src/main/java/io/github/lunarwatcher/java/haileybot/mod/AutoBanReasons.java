package io.github.lunarwatcher.java.haileybot.mod;

/**
 * Handles the internal reasons for banning. This is an emum to save memory where possible.
 */
public enum AutoBanReasons {
    INVITE_USERNAME("Invite in username"),
    SPAM_USERNAME("Spam in username"),
    UNHANDLED_SPAM("Spam in username not caught by the other categories");

    private final String reason;

    AutoBanReasons(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
