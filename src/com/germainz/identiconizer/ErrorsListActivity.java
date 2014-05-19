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

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ErrorsListActivity extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView header = (TextView) getLayoutInflater().inflate(android.R.layout.simple_list_item_1, getListView(), false);
        header.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
        header.setText(R.string.sql_error_explanation);
        getListView().addHeaderView(header, null, false);

        ArrayList<ContactInfo> insertErrors = getIntent().getParcelableArrayListExtra("insertErrors");
        ArrayList<ContactInfo> updateErrors = getIntent().getParcelableArrayListExtra("updateErrors");

        ArrayList<String> listItems = new ArrayList<>();
        for (ContactInfo insertError : insertErrors)
            listItems.add(getString(R.string.sql_error, getString(R.string.sql_insert),
                    insertError.contactName, insertError.contactPhotoSize / 1024, insertError.nameRawContactId));
        for (ContactInfo updateError : updateErrors)
            listItems.add(getString(R.string.sql_error, getString(R.string.sql_update),
                    updateError.contactName, updateError.contactPhotoSize / 1024, updateError.nameRawContactId));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);
        setListAdapter(adapter);
    }
}
