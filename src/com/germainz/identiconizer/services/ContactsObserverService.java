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

package com.germainz.identiconizer.services;

import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.os.IBinder;
import android.provider.ContactsContract;

import com.germainz.identiconizer.Config;

public class ContactsObserverService extends Service {

    private final ContactsContentObserver mContactsContentObserver = new ContactsContentObserver();
    private boolean mContentObserverRegistered = false;
    private int mMaxContactID = -1;
    private Config mConfig;

    @Override
    public void onCreate() {
        super.onCreate();
        if (mMaxContactID == -1) {
            mConfig = Config.getInstance(getBaseContext());

            Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                    new String[]{ContactsContract.Contacts._ID},
                    ContactsContract.Contacts.IN_VISIBLE_GROUP,
                    null, "_id DESC LIMIT 1");
            cursor.moveToFirst();
            try {
                mMaxContactID = cursor.getInt(cursor.getColumnIndex("_id"));
            } catch (CursorIndexOutOfBoundsException e) {
                mMaxContactID = 0;
            }
            cursor.close();
            int maxContactID = mConfig.getMaxContactID();
            if (mMaxContactID != maxContactID)
                mConfig.setMaxContactID(mMaxContactID);
            if (mMaxContactID > maxContactID)
                startService(new Intent(getApplicationContext(),
                        IdenticonCreationService.class).putExtra("updateExisting", false));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mContentObserverRegistered) {
            this.getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_VCARD_URI,
                    true, mContactsContentObserver);
            mContentObserverRegistered = true;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.getContentResolver().unregisterContentObserver(mContactsContentObserver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class ContactsContentObserver extends ContentObserver {
        public ContactsContentObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            // ContentObserver doesn't tell us anything about the nature of the change (addition,
            // deletion, editing, etc.)
            // To see if a new contact has been added, we compare the current highest contact ID
            // with the previously stored one.
            Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                    new String[]{ContactsContract.Contacts._ID},
                    ContactsContract.Contacts.IN_VISIBLE_GROUP,
                    null, "_id DESC LIMIT 1");
            cursor.moveToFirst();
            int maxContactID = cursor.getInt(cursor.getColumnIndex("_id"));
            cursor.close();
            if (maxContactID == mMaxContactID)
                return;
            if (maxContactID > mMaxContactID)
                startService(new Intent(getApplicationContext(),
                        IdenticonCreationService.class).putExtra("updateExisting", false));
            mMaxContactID = maxContactID;
            mConfig.setMaxContactID(mMaxContactID);
        }
    }

}
