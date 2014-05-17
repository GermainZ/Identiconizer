/*
 * Copyright (C) 2013 The ChameleonOS Open Source Project
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

    private static final byte[] PNG_HEADER = new byte[] { (byte) 137, (byte) 80, (byte) 78,
            (byte) 71, (byte) 13, (byte) 10, (byte) 26, (byte) 10 };

    public static boolean isIdenticon(byte[] data) {
        if (data == null || !isPngFormat(data))
            return false;

        byte[] tag = Arrays.copyOfRange(data, data.length - (Identicon.IDENTICON_MARKER.length() + 1), data.length - 1);
        String tagString;
        try {
            tagString = new String(tag, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            return false;
        }

        return Identicon.IDENTICON_MARKER.equals(tagString);
    }

    private static boolean isPngFormat(byte[] data) {
        if (data.length < PNG_HEADER.length)
            return false;

        for (int i = 0; i < PNG_HEADER.length; i++) {
            if (data[i] != PNG_HEADER[i])
                return false;
        }

        return true;
    }
}
