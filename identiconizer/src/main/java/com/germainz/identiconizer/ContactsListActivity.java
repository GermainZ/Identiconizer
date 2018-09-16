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

package com.germainz.identiconizer;

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
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;

import com.germainz.identiconizer.services.IdenticonCreationService;
import com.germainz.identiconizer.services.IdenticonRemovalService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class ContactsListActivity extends AppCompatActivity {
    RecyclerView mRecyclerView;
    ArrayList<Integer> checkedItems = new ArrayList<>();
    ContactsCursorAdapter mAdapter;
    Cursor mCursor;
    final static int SERVICE_ADD = 0;
    final static int SERVICE_REMOVE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_list);
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        mCursor = getContacts();
        mAdapter = new ContactsCursorAdapter(this, mCursor);
        mRecyclerView.setAdapter(mAdapter);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.identicons_contacts_list_title);
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
        if (Config.getInstance(this).shouldIgnoreContactVisibility())
            selection = null;
        String sortOrder = "display_name COLLATE LOCALIZED ASC";

        return getContentResolver().query(uri, projection, selection, null, sortOrder);
    }

    public class ContactsCursorAdapter extends CursorRecyclerViewAdapter<ContactsCursorAdapter.ViewHolder> {
        public ContactsCursorAdapter(Context context, Cursor cursor) {
            super(context, cursor);
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public CheckedTextView mCheckedTextView;
            public ImageView mImageView;

            public ViewHolder(View view) {
                super(view);
                mCheckedTextView = view.findViewById(R.id.check);
                mImageView = view.findViewById(R.id.image);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean isChecked = !mCheckedTextView.isChecked();
                        if (isChecked)
                            checkedItems.add(getAdapterPosition());
                        else
                            checkedItems.remove((Integer) getAdapterPosition());
                        mCheckedTextView.setChecked(isChecked);
                    }
                });
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {
            MyListItem myListItem = MyListItem.fromCursor(cursor);

            CheckedTextView contactName = viewHolder.mCheckedTextView;
            contactName.setText(myListItem.getName());
            contactName.setChecked(checkedItems.contains(myListItem.getPosition()));
            final ImageView contactImage = viewHolder.mImageView;
            int photoThumbnailURIIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI);
            String photoThumbnailString = cursor.getString(photoThumbnailURIIndex);
            contactImage.setImageResource(R.drawable.ic_identicons_style_retro);
            if (photoThumbnailString == null)
                return;

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

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.image_list_item, parent, false);
            ViewHolder vh = new ViewHolder(itemView);
            return vh;
        }

        public int getCount() {
            return getCursor().getCount();
        }
    }
}

class MyListItem {
    private String name;
    private String image;
    private Integer position;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImage() {
        return image;
    }

    public static MyListItem fromCursor(Cursor cursor) {
        MyListItem listItem = new MyListItem();
        listItem.setName(cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)));
        listItem.setImage(cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)));
        listItem.setPosition(cursor.getPosition());
        return listItem;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
