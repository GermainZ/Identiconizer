/*
 * Original work Copyright (C) 2013 The ChameleonOS Open Source Project
 * Modified work Copyright (C) 2013-2014 GermainZ@xda-developers.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.germainz.identiconizer.identicons;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class IdenticonUtils {

    private static final byte[] JPG_HEADER = new byte[]{(byte) 0xFF, (byte) 0xD8};
    private static final byte[] PNG_HEADER = new byte[]{(byte) 137, (byte) 80, (byte) 78,
            (byte) 71, (byte) 13, (byte) 10, (byte) 26, (byte) 10};
    private static final int JPG_FORMAT = 0;
    private static final int PNG_FORMAT = 1;
    private static final int OTHER_FORMAT = 2;

    public static boolean isIdenticon(byte[] data) {
        int format = getDataFormat(data);
        if (data == null || format == OTHER_FORMAT)
            return false;

        int start, end;
        String charSet;
        if (format == JPG_FORMAT) {
            start = data.length - 18;
            end = data.length -2;
            charSet = "US-ASCII";
        } else {
            start = data.length - (Identicon.IDENTICON_MARKER.length() + 1);
            end = data.length - 1;
            charSet = "ISO-8859-1";
        }
        byte[] tag = Arrays.copyOfRange(data, start, end);

        String tagString;
        try {
            tagString = new String(tag, charSet);
        } catch (UnsupportedEncodingException e) {
            return false;
        }

        return Identicon.IDENTICON_MARKER.equals(tagString);
    }

    private static int getDataFormat(byte[] data) {
        boolean isPng = true;
        if (data.length < PNG_HEADER.length) {
            isPng = false;
        } else {
            for (int i = 0; i < PNG_HEADER.length; i++) {
                if (data[i] != PNG_HEADER[i])
                    isPng = false;
            }
        }
        if (isPng)
            return PNG_FORMAT;

        boolean isJpg = true;
        if (data.length < JPG_HEADER.length) {
            isJpg = false;
        } else {
            for (int i = 0; i < JPG_HEADER.length; i++) {
                if (data[i] != JPG_HEADER[i])
                    isJpg = false;
            }
        }
        if (isJpg)
            return JPG_FORMAT;

        return OTHER_FORMAT;
    }
}
