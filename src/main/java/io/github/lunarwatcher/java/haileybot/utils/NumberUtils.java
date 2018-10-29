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

package io.github.lunarwatcher.java.haileybot.utils;

public class NumberUtils {
    private static final String ONE = "st";
    private static final String TWO = "nd";
    private static final String THREE = "rd";
    private static final String NTH = "th";

    public static String getNumberWithNth(long value) {
        long abs = value < 0 ? -value : value;

        long tens = abs % 10;
        long hundreds = abs % 100;

        if (equalsPlusMinus(tens, 1) && hundreds != 11)
            return stringify(value, ONE);
        else if (equalsPlusMinus(tens, 2) && hundreds != 12)
            return stringify(value, TWO);
        else if (equalsPlusMinus(tens, 3) && hundreds != 13)
            return stringify(value, THREE);

        return stringify(value, NTH);
    }

    public static boolean equalsPlusMinus(long val, long equals) {
        return val == equals || val == -equals;
    }

    private static String stringify(long num, String postfix) {
        return String.valueOf(num) + postfix;
    }
}
