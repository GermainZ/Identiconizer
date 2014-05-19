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

package com.germainz.identiconizer.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.germainz.identiconizer.ContactInfo;
import com.germainz.identiconizer.identicons.IdenticonFactory;
import com.germainz.identiconizer.identicons.IdenticonUtils;
import com.germainz.identiconizer.IdenticonsSettings;
import com.germainz.identiconizer.R;
import com.germainz.identiconizer.identicons.Identicon;

import java.util.ArrayList;

public class IdenticonCreationService extends IntentService {
    private static final String TAG = "IdenticonCreationService";
    private static final int SERVICE_NOTIFICATION_ID = 8675309;

    public IdenticonCreationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        startForeground(SERVICE_NOTIFICATION_ID, createNotification());
        // If a predefined contacts list is provided, use it directly.
        // contactsList is set when this service is started from ContactsListActivity.
        if (intent.hasExtra("contactsList")) {
            ArrayList<ContactInfo> contactsList = intent.getParcelableArrayListExtra("contactsList");
            processContacts(contactsList);
        } else {
            // If updateExisting is set to false, only contacts without a picture will get a new one.
            // Otherwise, even those that have an identicon set will get a new one. The latter is useful
            // when changing identicon styles, but is a waste of time when we're starting this service
            // after a new contact has been added. In that case, we just want the new contact to get his
            // identicon.
            boolean updateExisting = intent.getBooleanExtra("updateExisting", true);
            processContacts(updateExisting);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("CONTACTS_UPDATED"));
        getContentResolver().notifyChange(ContactsContract.Data.CONTENT_URI, null);
        stopForeground(true);
    }

    private void processContacts(boolean updateExisting) {
        Cursor cursor = getContacts();
        while (cursor.moveToNext()) {
            final long rawContactId = cursor.getLong(0);
            final String name = cursor.getString(1);
            final long photoId = cursor.getLong(2);
            if (!TextUtils.isEmpty(name)) {
                final byte[] photo = getContactPhotoBlob(photoId);
                if (photoId <= 0 || photo == null || (updateExisting && IdenticonUtils.isIdenticon(photo))) {
                    generateIdenticon(rawContactId, name);
                }
            }
        }
        cursor.close();
    }

    private void processContacts(ArrayList<ContactInfo> contactInfos) {
        for (ContactInfo contactInfo : contactInfos)
            generateIdenticon(contactInfo.nameRawContactId, contactInfo.contactName);
    }

    private Cursor getContacts() {
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        String[] projection = new String[]{
                "name_raw_contact_id",
                "display_name",
                "photo_id"
        };
        String selection = "in_visible_group = '1'";
        String sortOrder = "display_name COLLATE LOCALIZED ASC";

        return getContentResolver().query(uri, projection, selection, null, sortOrder);
    }

    private byte[] getContactPhotoBlob(long photoId) {
        String[] projection = new String[]{ContactsContract.Data.DATA15};
        String where = ContactsContract.Data._ID + " == "
                + String.valueOf(photoId) + " AND " + ContactsContract.Data.MIMETYPE + "=='"
                + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'";
        Cursor cursor = getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                where,
                null,
                null);
        byte[] blob = null;
        if (cursor.moveToFirst()) {
            blob = cursor.getBlob(0);
        }
        cursor.close();

        return blob;
    }

    private void generateIdenticon(long contactId, String name) {
        if (!TextUtils.isEmpty(name)) {
            updateNotification(getString(R.string.identicons_creation_service_running_title),
                    String.format(getString(R.string.identicons_creation_service_contact_summary),
                            name)
            );
            final Identicon identicon = IdenticonFactory.makeIdenticon(this);
            final byte[] identiconImage = identicon.generateIdenticonByteArray(name);
            setContactPhoto(getContentResolver(), identiconImage, contactId);
        }
    }

    private void setContactPhoto(ContentResolver resolver, byte[] bytes, long personId) {
        ContentValues values = new ContentValues();
        int photoRow = -1;
        String where = ContactsContract.Data.RAW_CONTACT_ID + " == "
                + String.valueOf(personId) + " AND " + ContactsContract.Data.MIMETYPE + "=='"
                + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'";
        Cursor cursor = resolver.query(
                ContactsContract.Data.CONTENT_URI,
                null,
                where,
                null,
                null);
        int idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data._ID);
        if (cursor.moveToFirst()) {
            photoRow = cursor.getInt(idIdx);
        }
        cursor.close();

        if (photoRow >= 0) {
            final String selection = ContactsContract.Data.RAW_CONTACT_ID
                    + " = ? AND "
                    + ContactsContract.Data.MIMETYPE
                    + " = ?";
            final String[] selectionArgs = new String[]{
                    String.valueOf(personId),
                    ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE};
            values.put(ContactsContract.Data.RAW_CONTACT_ID, personId);
            values.put(ContactsContract.Data.IS_PRIMARY, 1);
            values.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
            values.put(ContactsContract.CommonDataKinds.Photo.PHOTO, bytes);
            values.put("skip_processing", "skip_processing");
            // We're not using applyBatch because of the 1024K limit of the transaction buffer,
            // which isn't enough when we're using a large identicon size and certain styles (e.g.
            // the Spirograph style, which occupies roughly that much on its own when the size is
            // set to 720x720.)
            if (getContentResolver().update(ContactsContract.Data.CONTENT_URI, values, selection, selectionArgs) != 1) {
                Log.d(TAG, "Couldn't update image for raw_contact_id " + Long.toString(personId));
                Log.d(TAG, "Image size: " + bytes.length + " bytes");
            }
        } else {
            values.put(ContactsContract.Data.RAW_CONTACT_ID, personId);
            values.put(ContactsContract.Data.IS_PRIMARY, 1);
            values.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
            values.put(ContactsContract.CommonDataKinds.Photo.PHOTO, bytes);
            values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
            values.put("skip_processing", "skip_processing");
            if (getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values) == null) {
                Log.d(TAG, "Couldn't insert image for raw_contact_id " + Long.toString(personId));
                Log.d(TAG, "Image size: " + bytes.length + " bytes");
            }
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, IdenticonsSettings.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        @SuppressWarnings("deprecation")
        Notification notice = new Notification.Builder(this)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentTitle(getString(R.string.identicons_creation_service_running_title))
                .setContentText(getString(R.string.identicons_creation_service_running_summary))
                .setSmallIcon(R.drawable.ic_settings_identicons)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(contentIntent)
                .getNotification();
        return notice;
    }

    private void updateNotification(String title, String text) {
        Intent intent = new Intent(this, IdenticonsSettings.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationManager nm =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        @SuppressWarnings("deprecation")
        Notification notice = new Notification.Builder(this)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_settings_identicons)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(contentIntent)
                .getNotification();
        nm.notify(SERVICE_NOTIFICATION_ID, notice);
    }
}
