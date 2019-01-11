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

package io.github.lunarwatcher.java.haileybot.mod;

import org.jetbrains.annotations.NotNull;

/**
 * Handles the internal reasons for banning. This is an emum to save memory where possible.
 */
public enum AutoBanReasons {
    INVITE_USERNAME("Invite in username"),
    SPAM_USERNAME("Spam in username"),
    DATING_SPAM_NEW_ACCOUNT("An account that joined the server under a day ago posted dating spam -- insta-nuking the account"),
    UNSPECIFIED("A severe enough issue to warrant auto-ban, but that hasn't been properly described in enums yet."),
    BLACKLISTED_URL("A URL never seen outside spam."),
    UNHANDLED_SPAM("Spam in username not caught by the other categories");

    private final String reason;

    AutoBanReasons(@NotNull String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
