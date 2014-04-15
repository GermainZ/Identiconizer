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

package com.germainz.identiconizer;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListAdapter;

public class ImageListPreference extends ListPreference {
    private int[] mResourceIds = null;
    private int mRadioDrawableId;

    /**
     * Constructor of the ImageListPreference. Initializes the custom images.
     * @param context application context.
     * @param attrs custom xml attributes.
     */
    public ImageListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.ImageListPreference);

        String[] imageNames = context.getResources().getStringArray(
                typedArray.getResourceId(typedArray.getIndexCount()-1, -1));

        mResourceIds = new int[imageNames.length];

        for (int i=0;i<imageNames.length;i++) {
            String imageName = imageNames[i].substring(
                    imageNames[i].lastIndexOf('/') + 1,
                    imageNames[i].lastIndexOf('.'));

            mResourceIds[i] = context.getResources().getIdentifier(imageName,
                    "drawable", context.getPackageName());
        }

        typedArray.recycle();

        // in order to use the holo themed radio button we must get the id
        // from the system resources and then get the drawable for that id
        // Because it uses reflection twice, we'll store this value for future use.
        mRadioDrawableId = Resources.getSystem().getIdentifier("btn_radio_holo_dark", "drawable", "android");
    }

    /**
     * {@inheritDoc}
     */
    protected void onPrepareDialogBuilder(Builder builder) {
        int index = findIndexOfValue(getSharedPreferences().getString(
                getKey(), "1"));

        ListAdapter listAdapter = new ImageArrayAdapter(getContext(),
                R.layout.image_list_item, getEntries(), mResourceIds, index);

        // Order matters.
        builder.setAdapter(listAdapter, this);
        super.onPrepareDialogBuilder(builder);
    }

    public class ImageArrayAdapter extends ArrayAdapter<CharSequence> {
        private int index = 0;
        private int[] resourceIds = null;

        /**
         * ImageArrayAdapter constructor.
         * @param context the context.
         * @param textViewResourceId resource id of the text view.
         * @param objects to be displayed.
         * @param ids resource id of the images to be displayed.
         * @param i index of the previous selected item.
         */
        public ImageArrayAdapter(Context context, int textViewResourceId,
                                 CharSequence[] objects, int[] ids, int i) {
            super(context, textViewResourceId, objects);

            index = i;
            resourceIds = ids;
        }
        /**
         * {@inheritDoc}
         */
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
            View row = inflater.inflate(R.layout.image_list_item, parent, false);

            ImageView imageView = (ImageView)row.findViewById(R.id.image);
            imageView.setImageResource(resourceIds[position]);

            CheckedTextView checkedTextView = (CheckedTextView)row.findViewById(
                    R.id.check);

            checkedTextView.setText(getItem(position));
            checkedTextView.setCheckMarkDrawable(Resources.getSystem().getDrawable(mRadioDrawableId));

            if (position == index) {
                checkedTextView.setChecked(true);
            }

            return row;
        }
    }
}
