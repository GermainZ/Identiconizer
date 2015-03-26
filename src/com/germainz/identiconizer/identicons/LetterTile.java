/*
 * Copyright (C) 2013-2014 GermainZ@xda-developers.com
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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;

public class LetterTile extends Identicon {

    // Colors, font and values taken from the Google Messenger APK
    private final Rect mBounds = new Rect();
    private final Canvas mCanvas = new Canvas();
    private final TextPaint mPaint = new TextPaint();
    private static final int[] COLORS = new int[]{
            Color.parseColor("#C90000"), Color.parseColor("#CE3B00"),
            Color.parseColor("#F06B00"), Color.parseColor("#FF8E00"),
            Color.parseColor("#82E00D"), Color.parseColor("#C0C448"),
            Color.parseColor("#C2A722"), Color.parseColor("#CA7B00"),
            Color.parseColor("#74C13D"), Color.parseColor("#28B724"),
            Color.parseColor("#52A710"), Color.parseColor("#00934D"),
            Color.parseColor("#00B967"), Color.parseColor("#54BC93"),
            Color.parseColor("#0095A8"), Color.parseColor("#6AB3B9"),
            Color.parseColor("#44AAE7"), Color.parseColor("#2D8DCD"),
            Color.parseColor("#4574EC"), Color.parseColor("#3B71D1"),
            Color.parseColor("#9121CB"), Color.parseColor("#8400B3"),
            Color.parseColor("#782ECC"), Color.parseColor("#4413B9"),
            Color.parseColor("#B621CB"), Color.parseColor("#AA00CC"),
            Color.parseColor("#AB00B6"), Color.parseColor("#FF2033"),
            Color.parseColor("#A29195"), Color.parseColor("#A5777F"),
            Color.parseColor("#C94979"), Color.parseColor("#EF005A"),
            Color.parseColor("#444444"), Color.parseColor("#363636")};
    private static final int TILE_FONT_COLOR = Color.WHITE;

    public LetterTile() {
        Typeface serif = Typeface.create("serif", 0);
        Typeface sans = Typeface.create("sans-serif-light", 0);
        if(SERIF) {
            mPaint.setTypeface(serif);
        } else {
            mPaint.setTypeface(sans);
        }
        float divider = 1;
        if (LENGTH != 1) divider = ((float)LENGTH ) / (float)2;
        float tileLetterFontSize = ( 69 * SIZE / 100 ) / divider;
        mPaint.setTextSize(tileLetterFontSize);
        mPaint.setColor(TILE_FONT_COLOR);
        mPaint.setTextAlign(android.graphics.Paint.Align.CENTER);
        mPaint.setAntiAlias(true);
    }

    @Override
    public Bitmap generateIdenticonBitmap(byte[] hash) {
        return null;
    }

    @Override
    public byte[] generateIdenticonByteArray(byte[] hash) {
        return null;
    }

    @Override
    public Bitmap generateIdenticonBitmap(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        Bitmap bitmap = getBitmap();
        Canvas canvas = mCanvas;
        canvas.setBitmap(bitmap);
        canvas.drawColor(pickColor(name));
        mPaint.getTextBounds(name, 0, LENGTH, mBounds);
        canvas.drawText(name, 0, LENGTH, SIZE / 2, SIZE / 2 + (mBounds.bottom - mBounds.top) / 2, mPaint);
        return bitmap;
    }

    @Override
    public byte[] generateIdenticonByteArray(String name) {
        return bitmapToByteArray(generateIdenticonBitmap(name));
    }

    private int pickColor(String s) {
        int i = Math.abs(s.hashCode() - 16 ) % 34;
        if (i < COLORS.length)
            return COLORS[i];
        else
            return DEFAULT_COLOR;
    }

    private Bitmap getBitmap() {
        return Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
    }

}
