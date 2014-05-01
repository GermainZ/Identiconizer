/*
 * Copyright (C) 2013 GermainZ@xda-developers.com
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

import android.content.Context;
import android.content.SharedPreferences;

import de.robv.android.xposed.XSharedPreferences;

public class Config {
    private static Config mInstance;
    private XSharedPreferences mXPreferences = null;
    private SharedPreferences mPreferences = null;

    public static final String PACKAGE_NAME = "com.germainz.identiconizer";
    public static final String PREFS = PACKAGE_NAME + "_preferences";
    public static final String PREF_ENABLED = "identicons_enabled";
    public static final String PREF_STYLE = "identicons_style";
    public static final String PREF_SIZE = "identicons_size";
    public static final String PREF_CREATE = "identicons_create";
    public static final String PREF_REMOVE = "identicons_remove";
    public static final String PREF_CONTACTS_LIST = "identicons_contacts_list";
    public static final String PREF_ABOUT = "about";
    public static final String PREF_MAX_CONTACT_ID = "max_contact_id";

    public Config() {
        mXPreferences = new XSharedPreferences(PACKAGE_NAME, PREFS);
        mXPreferences.makeWorldReadable();
    }

    private Config(Context context) {
        mPreferences = context.getSharedPreferences(PREFS, Context.MODE_WORLD_READABLE);
    }

    public static Config getInstance(Context context) {
        if (mInstance == null)
            mInstance = new Config(context);
        return mInstance;
    }

    public void reload() {
        if (mXPreferences != null)
            mXPreferences.reload();
    }

    public boolean isEnabled() {
        return getBoolean(PREF_ENABLED, false);
    }

    public boolean isXposedModActive() {
        return false;
    }

    public int getIdenticonStyle() {
        return Integer.parseInt(getString(PREF_STYLE, "0"));
    }

    public int getIdenticonSize() {
        return Integer.parseInt(getString(PREF_SIZE, "96"));
    }

    public int getMaxContactID() {
        return getInt(PREF_MAX_CONTACT_ID, 0);
    }

    public void setEnabled(boolean enabled) {
        mPreferences.edit().putBoolean(PREF_ENABLED, enabled).commit();
    }

    public void setIdenticonStyle(int style) {
        mPreferences.edit().putString(PREF_STYLE, Integer.toString(style)).commit();
    }

    public void setIdenticonSize(int size) {
        mPreferences.edit().putString(PREF_SIZE, Integer.toString(size)).commit();
    }

    public void setMaxContactID(int id) {
        mPreferences.edit().putInt(PREF_MAX_CONTACT_ID, id).commit();
    }

    public String getString(String key, String defaultValue) {
        String returnResult = defaultValue;
        if (mPreferences != null)
            returnResult = mPreferences.getString(key, defaultValue);
        else if (mXPreferences != null)
            returnResult = mXPreferences.getString(key, defaultValue);
        return returnResult;
    }

    public int getInt(String key, int defaultValue) {
        int returnResult = defaultValue;
        if (mPreferences != null)
            returnResult = mPreferences.getInt(key, defaultValue);
        else if (mXPreferences != null)
            returnResult = mXPreferences.getInt(key, defaultValue);
        return returnResult;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        boolean returnResult = defaultValue;
        if (mPreferences != null)
            returnResult = mPreferences.getBoolean(key, defaultValue);
        else if (mXPreferences != null)
            returnResult = mXPreferences.getBoolean(key, defaultValue);
        return returnResult;
    }
}
