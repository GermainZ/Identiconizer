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

package com.germainz.identiconizer.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.germainz.identiconizer.ContactInfo;
import com.germainz.identiconizer.IdenticonsSettings;
import com.germainz.identiconizer.R;
import com.germainz.identiconizer.identicons.IdenticonUtils;

import java.util.ArrayList;

public class IdenticonRemovalService extends IntentService {
    private static final String TAG = "IdenticonRepairService";
    private static final int SERVICE_NOTIFICATION_ID = 8675311;

    ArrayList<ContentProviderOperation> mOps = new ArrayList<ContentProviderOperation>();

    public IdenticonRemovalService() {
        super("IdenticonRepairService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        startForeground(SERVICE_NOTIFICATION_ID, createNotification());
        // If a predefined contacts list is provided, use it directly.
        // contactsList is set when this service is started from ContactsListActivity.
        if (intent.hasExtra("contactsList")) {
            ArrayList<ContactInfo> contactsList = intent.getParcelableArrayListExtra("contactsList");
            processPhotos(contactsList);
        } else {
            processPhotos();
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("CONTACTS_UPDATED"));
        stopForeground(true);
    }

    private void processPhotos() {
        Cursor cursor = getIdenticonPhotos();
        final int totalPhotos = cursor.getCount();
        int currentPhoto = 1;
        while (cursor.moveToNext()) {
            final long contactId = cursor.getLong(0);
            updateNotification(getString(R.string.identicons_remove_service_running_title),
                    String.format(getString(R.string.identicons_remove_service_contact_summary),
                            currentPhoto++, totalPhotos)
            );
            byte[] data = cursor.getBlob(1);
            if (IdenticonUtils.isIdenticon(data))
                removeIdenticon(contactId);
        }
        cursor.close();

        if (!mOps.isEmpty()) {
            updateNotification(getString(R.string.identicons_remove_service_running_title),
                    getString(R.string.identicons_remove_service_contact_summary_finishing));
            try {
                // Perform operations in batches of 100, to avoid TransactionTooLargeExceptions
                for (int i = 0, j = mOps.size(); i < j; i += 100)
                    getContentResolver().applyBatch(ContactsContract.AUTHORITY,
                            new ArrayList<ContentProviderOperation>(mOps.subList(i, i + Math.min(100, j - i))));
            } catch (RemoteException | OperationApplicationException e) {
                Log.e(TAG, "Unable to apply batch", e);
            }
        }
    }

    private void processPhotos(ArrayList<ContactInfo> contactInfos) {
        for (int i = 0, j = contactInfos.size(); i < j; i++) {
            updateNotification(getString(R.string.identicons_remove_service_running_title),
                    String.format(getString(R.string.identicons_remove_service_contact_summary),
                            i, j)
            );
            // ContactsListActivity gives us name_raw_contact_id, which is not unique.
            // For example, if the user has 3 accounts (e.g. Google, WhatsApp and Viber) then three
            // photo rows will exist, one for each.
            // We want to get those that have a photo set (any photo, not identicons.)
            // We can't assume name_raw_contact_id == raw_contact_id because it might only match
            // one photo row (which might be empty) when multiple accounts are present.
            Cursor cursor = getIdenticonPhotos(contactInfos.get(i).nameRawContactId);
            while (cursor.moveToNext()) {
                int contactId = cursor.getInt(0);
                byte[] data = cursor.getBlob(1);
                if (data != null)
                    removeIdenticon(contactId);
            }
        }

        if (!mOps.isEmpty()) {
            updateNotification(getString(R.string.identicons_remove_service_running_title),
                    getString(R.string.identicons_remove_service_contact_summary_finishing));
            try {
                // Perform operations in batches of 100, to avoid TransactionTooLargeExceptions
                for (int i = 0, j = mOps.size(); i < j; i += 100)
                    getContentResolver().applyBatch(ContactsContract.AUTHORITY,
                            new ArrayList<ContentProviderOperation>(mOps.subList(i, i + Math.min(100, j - i))));
            } catch (RemoteException | OperationApplicationException e) {
                Log.e(TAG, "Unable to apply batch", e);
            }
        }
    }

    private Cursor getIdenticonPhotos() {
        Uri uri = ContactsContract.Data.CONTENT_URI;
        String[] projection = new String[]{ContactsContract.Data._ID,
                ContactsContract.Data.DATA15};
        final String selection = ContactsContract.Data.DATA15
                + " IS NOT NULL AND "
                + ContactsContract.Data.MIMETYPE
                + " = ?";
        final String[] selectionArgs = new String[]{
                ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE};
        return getContentResolver().query(uri, projection, selection, selectionArgs, null);
    }

    private Cursor getIdenticonPhotos(int nameRawContactId) {
        Uri uri = ContactsContract.Data.CONTENT_URI;
        String[] projection = new String[]{ContactsContract.Data._ID,
                ContactsContract.Data.DATA15};
        final String selection = "name_raw_contact_id = ? AND "
                + ContactsContract.Data.DATA15
                + " IS NOT NULL AND "
                + ContactsContract.Data.MIMETYPE
                + " = ?";
        final String[] selectionArgs = new String[]{String.valueOf(nameRawContactId),
                ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE};
        return getContentResolver().query(uri, projection, selection, selectionArgs, null);
    }

    private void removeIdenticon(long id) {
        ContentValues values = new ContentValues();
        values.put(ContactsContract.Data.DATA15, (byte[]) null);
        final String selection = ContactsContract.Data._ID
                + " = ? AND "
                + ContactsContract.Data.MIMETYPE
                + " = ?";
        final String[] selectionArgs = new String[]{
                String.valueOf(id),
                ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE};
        mOps.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withValue(ContactsContract.Data.DATA15, null)
                .withSelection(selection, selectionArgs)
                .build());
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, IdenticonsSettings.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        @SuppressWarnings("deprecation")
        Notification notice = new Notification.Builder(this)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentTitle(getString(R.string.identicons_remove_service_running_title))
                .setContentText(getString(R.string.identicons_remove_service_running_summary))
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
