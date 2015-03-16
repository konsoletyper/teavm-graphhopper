/*
 *  Copyright 2015 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.graphhopper;

import java.util.Arrays;

/**
 *
 * @author Alexey Andreev
 */
public class Base64 {
    private static char[] map;
    private static int[] unmap;

    static {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        map = new char[chars.length()];
        unmap = new int[127];
        Arrays.fill(unmap, -1);
        for (int i = 0; i < chars.length(); ++i) {
            map[i] = chars.charAt(i);
            unmap[map[i]] = i;
        }
    }

    public static String encode(byte[] bytes) {
        int buffer = 0;
        char[] chars = new char[(bytes.length + 2) / 3 * 4];
        int j = 0;
        int sz = (bytes.length / 3) * 3;
        for (int i = 0; i < sz; i += 3, j += 4) {
            buffer = ((bytes[i + 0] & 0xFF) << 16) |
                     ((bytes[i + 1] & 0xFF) <<  8) |
                     ((bytes[i + 2] & 0xFF) <<  0);
            printBuffer(chars, buffer, j);
        }
        switch (bytes.length % 3) {
            case 2:
                buffer = (bytes[sz + 0] << 16) |
                         (bytes[sz + 1] <<  8);
                chars[j++] = map[(buffer >>> 18) & 63];
                chars[j++] = map[(buffer >>> 12) & 63];
                chars[j++] = map[(buffer >>>  6) & 63];
                chars[j++] = '=';
                break;
            case 1:
                buffer = (bytes[sz + 0] << 16);
                chars[j++] = map[(buffer >>> 18) & 63];
                chars[j++] = map[(buffer >>> 12) & 63];
                chars[j++] = '=';
                chars[j++] = '=';
                break;
        }
        return new String(chars);
    }

    private static void printBuffer(char[] chars, int buffer, int target) {
        chars[target++] = map[(buffer >>> 18) & 63];
        chars[target++] = map[(buffer >>> 12) & 63];
        chars[target++] = map[(buffer >>>  6) & 63];
        chars[target++] = map[(buffer >>>  0) & 63];
    }

    public static byte[] decode(String message) {
        byte[] data = new byte[calculateLength(message)];
        int sz = (data.length / 3) * 4;
        int buffer = 0;
        int j = 0;
        for (int i = 0; i < sz; i += 4) {
            buffer = readBuffer(message, i);
            data[j++] = (byte)(buffer >> 16);
            data[j++] = (byte)(buffer >>  8);
            data[j++] = (byte)(buffer >>  0);
        }
        switch (data.length - j) {
            case 3:
                buffer = readBuffer(message, sz);
                data[j++] = (byte)(buffer >> 16);
                data[j++] = (byte)(buffer >>  8);
                data[j++] = (byte)(buffer >>  0);
                break;
            case 2:
                buffer = (unmap[message.charAt(sz + 0)] << 18) |
                         (unmap[message.charAt(sz + 1)] << 12) |
                         (unmap[message.charAt(sz + 2)] <<  6);
                data[j++] = (byte)(buffer >> 16);
                data[j++] = (byte)(buffer >>  8);
                break;
            case 1:
                buffer = (unmap[message.charAt(sz + 0)] << 18) |
                         (unmap[message.charAt(sz + 1)] << 12);
                data[j++] = (byte)(buffer >> 16);
                break;
        }
        return data;
    }

    private static int readBuffer(String chars, int index) {
        return (unmap[chars.charAt(index + 0)] << 18) |
               (unmap[chars.charAt(index + 1)] << 12) |
               (unmap[chars.charAt(index + 2)] <<  6) |
               (unmap[chars.charAt(index + 3)] <<  0);
    }

    private static int calculateLength(String message) {
        int diff = 0;
        int sz = message.length();
        if (sz == 0) {
            return 0;
        }
        if (message.charAt(sz - 1) == '=') {
            ++diff;
        }
        if (message.charAt(sz - 2) == '=') {
            ++diff;
        }
        return sz / 4 * 3 - diff;
    }
}
