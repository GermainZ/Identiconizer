/*
 * Original work Copyright (C) 2013 The ChameleonOS Open Source Project
 * Modified work Copyright (C) 2013 GermainZ@xda-developers.com
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

import android.graphics.Bitmap;
import android.graphics.Color;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

public abstract class Identicon {

    public static final String IDENTICON_MARKER = "identicon_marker";

    public static final String DEFAULT_IDENTICON_SALT =
            "zG~v(+&>fLX|!#9D*BTj*#K>amB&TUB}T/jBOQih|Sg8}@N-^Rk|?VEXI,9EQPH]";

    public static int SIZE = 96;
    public static int BG_COLOR = 0xFFDDDDDD;

    /**
     * Generates an identicon bitmap using the provided hash
     *
     * @param hash A 16 byte hash used to generate the identicon
     * @return The bitmap of the identicon created
     */
    public abstract Bitmap generateIdenticonBitmap(byte[] hash);

    /**
     * Generates an identicon bitmap, as a byte array, using the provided hash
     *
     * @param hash A 16 byte hash used to generate the identicon
     * @return The bitmap byte array of the identicon created
     */
    public abstract byte[] generateIdenticonByteArray(byte[] hash);

    /**
     * Generates an identicon bitmap using the provided key to generate a hash
     *
     * @param key A non empty string used to generate a hash when creating the identicon
     * @return The bitmap of the identicon created
     */
    public abstract Bitmap generateIdenticonBitmap(String key);

    /**
     * Generates an identicon bitmap, as a byte array, using the provided key to generate a hash
     *
     * @param key A non empty string used to generate a hash when creating the identicon
     * @return The bitmap byte array of the identicon created
     */
    public abstract byte[] generateIdenticonByteArray(String key);

    /**
     * Provides an MD5 sum for the given input string.
     *
     * @param input
     * @return
     */
    public byte[] generateHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(input.getBytes());
        } catch (Exception e) {
            return null;
        }
    }

    public String saltedKey(String key) {
        return DEFAULT_IDENTICON_SALT + key + DEFAULT_IDENTICON_SALT;
    }

    /**
     * Adds a chunk to the end of a byte array containing a png image
     *
     * @param original The png image to add the comment to
     * @return The same image provided with the added chunk
     */
    protected static byte[] makeTaggedIdenticon(byte[] original) {
        byte[] taggedBlock = makeTextBlock(IDENTICON_MARKER);
        byte[] taggedImage = new byte[original.length + taggedBlock.length];
        ByteBuffer buffer = ByteBuffer.wrap(taggedImage);
        buffer.put(original);
        buffer.put(taggedBlock);
        return taggedImage;
    }

    private static byte[] makeTextBlock(String text) {
        byte[] block = new byte[text.length() + 1];
        ByteBuffer blockBuffer = ByteBuffer.wrap(block);
        // http://www.libpng.org/pub/png/spec/1.2/PNG-Chunks.html
        // put the text as the chunk's keyword
        blockBuffer.put(text.getBytes());
        // followed by a null separator
        blockBuffer.put((byte) 0);
        // we leave the chunk's text empty

        return block;
    }

    protected static byte[] bitmapToByteArray(Bitmap bmp) {
        if (bmp == null) return null;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] bytes = stream.toByteArray();
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (bytes != null) {
            return makeTaggedIdenticon(bytes);
        }

        return bytes;
    }

    /**
     * Returns distance between two colors.
     *
     * @param c1
     * @param c2
     * @return
     */
    protected float getColorDistance(int c1, int c2) {
        float dr = Color.red(c1) - Color.red(c2);
        float dg = Color.green(c1) - Color.green(c2);
        float db = Color.blue(c1) - Color.blue(c2);
        return (float) Math.sqrt(dr * dr + dg * dg + db * db);
    }

    /**
     * Returns complementary color.
     *
     * @param color
     * @return
     */
    protected int getComplementaryColor(int color) {
        return color ^ 0x00FFFFFF;
    }
}

