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

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.germainz.identiconizer.services.IdenticonCreationService;
import com.germainz.identiconizer.services.IdenticonRemovalService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class ContactsListActivity extends ListActivity {

    ArrayList<Integer> checkedItems = new ArrayList<>();
    ContactsCursorAdapter mAdapter;
    Cursor mCursor;
    final static int SERVICE_ADD = 0;
    final static int SERVICE_REMOVE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCursor = getContacts();
        String[] fromColumns = {ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.PHOTO_THUMBNAIL_URI};
        int[] toViews = {R.id.check, R.id.image};
        mAdapter = new ContactsCursorAdapter(this, R.layout.image_list_item, mCursor, fromColumns, toViews, 0);
        setListAdapter(mAdapter);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(R.string.identicons_contacts_list_title);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mAdapter.swapCursor(getContacts());
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter("CONTACTS_UPDATED"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        CheckedTextView checkedTextView = (CheckedTextView) v.findViewById(R.id.check);
        boolean isChecked = !checkedTextView.isChecked();
        if (isChecked)
            checkedItems.add(position);
        else
            checkedItems.remove((Integer) position);
        checkedTextView.setChecked(isChecked);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contacts_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                startIdenticonService(SERVICE_ADD);
                break;
            case R.id.action_clear:
                startIdenticonService(SERVICE_REMOVE);
                break;
            case R.id.action_select_all:
                checkedItems.clear();
                for (int i = 0, j = mAdapter.getCount(); i < j; i++)
                    checkedItems.add(i);
                mAdapter.notifyDataSetChanged();
                break;
            case R.id.action_deselect_all:
                checkedItems.clear();
                mAdapter.notifyDataSetChanged();
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return true;
    }

    private void startIdenticonService(int serviceType) {
        int displayName = mCursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME);
        ArrayList<ContactInfo> contactsList = new ArrayList<>();
        int contactId = mCursor.getColumnIndexOrThrow("name_raw_contact_id");
        mCursor.moveToPosition(-1);
        while (mCursor.moveToNext()) {
            if (checkedItems.contains(mCursor.getPosition()))
                contactsList.add(new ContactInfo(mCursor.getInt(contactId), mCursor.getString(displayName)));
        }
        Intent intent = null;
        switch (serviceType) {
            case SERVICE_ADD:
                intent = new Intent(this, IdenticonCreationService.class);
                break;
            case SERVICE_REMOVE:
                intent = new Intent(this, IdenticonRemovalService.class);
                break;
        }
        intent.putParcelableArrayListExtra("contactsList", contactsList);
        startService(intent);
    }

    private Cursor getContacts() {
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        String[] projection = new String[]{
                ContactsContract.Contacts._ID,
                "name_raw_contact_id",
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
        };
        String selection = "in_visible_group = '1'";
        String sortOrder = "display_name COLLATE LOCALIZED ASC";

        return getContentResolver().query(uri, projection, selection, null, sortOrder);
    }

    public class ContactsCursorAdapter extends SimpleCursorAdapter {
        private LayoutInflater layoutInflater;
        private int layout;

        public ContactsCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
            layoutInflater = LayoutInflater.from(context);
            this.layout = layout;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return layoutInflater.inflate(layout, parent, false);
        }

        @Override
        public void bindView(final View view, Context context, Cursor cursor) {
            CheckedTextView contactName = (CheckedTextView) view.findViewById(R.id.check);
            int displayName = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME);
            contactName.setText(cursor.getString(displayName));
            contactName.setChecked(checkedItems.contains(cursor.getPosition()));
            final ImageView contactImage = (ImageView) view.findViewById(R.id.image);
            int photoThumbnailURIIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI);
            String photoThumbnailString = cursor.getString(photoThumbnailURIIndex);
            if (photoThumbnailString == null) {
                contactImage.setImageResource(R.drawable.ic_identicons_style_retro);
                return;
            }

            final Uri photoThumbnailURI = Uri.parse(photoThumbnailString);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        // Original implementation from: http://stackoverflow.com/a/6228188
                        // Create a 48 dip thumbnail
                        InputStream input = getContentResolver().openInputStream(photoThumbnailURI);

                        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
                        onlyBoundsOptions.inJustDecodeBounds = true;
                        onlyBoundsOptions.inDither = true;
                        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
                        input.close();

                        int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;
                        Resources r = getResources();
                        float thumbnailSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, r.getDisplayMetrics());
                        double ratio = (originalSize > thumbnailSize) ? (originalSize / thumbnailSize) : 1.0;

                        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                        bitmapOptions.inSampleSize = Integer.highestOneBit((int) Math.floor(ratio));
                        bitmapOptions.inDither = true;
                        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        input = getContentResolver().openInputStream(photoThumbnailURI);
                        final Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
                        input.close();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                contactImage.setImageBitmap(bitmap);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }
    }

}
