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