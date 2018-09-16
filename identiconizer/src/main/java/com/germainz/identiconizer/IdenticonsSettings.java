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

package com.germainz.identiconizer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.germainz.identiconizer.identicons.IdenticonFactory;
import com.germainz.identiconizer.services.ContactsObserverService;
import com.germainz.identiconizer.services.IdenticonCreationService;
import com.germainz.identiconizer.services.IdenticonRemovalService;
import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.OpacityBar;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

import java.io.File;

public class IdenticonsSettings extends AppCompatPreferenceActivity implements OnPreferenceChangeListener {
    private static final int PERMISSIONS_REQUEST_CODE = 123;
    private static final String ACTION_SETTINGS_ABOUT = "com.germainz.identiconizer.SETTINGS_ABOUT";
    private SwitchPreference mEnabledPref;
    private ImageListPreference mStylePref;
    private SwitchPreference mSerifPref;
    private Preference mLengthPref;
    private Preference mBgColorPref;
    private Config mConfig;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar bar = getSupportActionBar();
        String action = getIntent().getAction();
        if (action != null && action.equals(ACTION_SETTINGS_ABOUT)) {
            bar.setHomeButtonEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowTitleEnabled(true);
            bar.setTitle(R.string.about_title);
            addPreferencesFromResource(R.xml.settings_about);
            return;
        }

        bar.setTitle(R.string.app_name);
        getPreferenceManager().setSharedPreferencesMode(MODE_PRIVATE);
        addPreferencesFromResource(R.xml.identicons_prefs);
        File prefsDir = new File(this.getApplicationInfo().dataDir, "shared_prefs");
        File prefsFile = new File(prefsDir, getPreferenceManager().getSharedPreferencesName() + ".xml");
        if (prefsFile.exists()) prefsFile.setReadable(true, false);
        mConfig = Config.getInstance(this);

        mEnabledPref = (SwitchPreference) findPreference(Config.PREF_ENABLED);
        mEnabledPref.setChecked(mConfig.isEnabled());
        if (mEnabledPref.isChecked() && !mConfig.isXposedModActive())
            startService(new Intent(this, ContactsObserverService.class));
        mEnabledPref.setOnPreferenceChangeListener(this);

        PreferenceScreen prefSet = getPreferenceScreen();
        mStylePref = (ImageListPreference) prefSet.findPreference(Config.PREF_STYLE);
        mStylePref.setOnPreferenceChangeListener(this);
        int style = mConfig.getIdenticonStyle();
        mStylePref.setValue(String.valueOf(style));
        updateStyleSummary(style);

        Preference startServicePref = findPreference(Config.PREF_CREATE);
        startServicePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startService(new Intent(IdenticonsSettings.this, IdenticonCreationService.class));
                return true;
            }
        });

        startServicePref = findPreference(Config.PREF_REMOVE);
        startServicePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startService(new Intent(IdenticonsSettings.this, IdenticonRemovalService.class));
                return true;
            }
        });

        Preference contactsListPref = findPreference(Config.PREF_CONTACTS_LIST);
        contactsListPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(IdenticonsSettings.this, ContactsListActivity.class));
                return true;
            }
        });

        final Preference sizePref = findPreference(Config.PREF_SIZE);
        final int identiconSize = mConfig.getIdenticonSize();
        sizePref.setSummary(identiconSize + " × " + identiconSize);
        sizePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final NumberPicker npView = new NumberPicker(IdenticonsSettings.this);

                final int minValue = 96;
                final int maxValue = getMaxContactPhotoSize();
                final int step = 16;
                String[] valueSet = new String[(maxValue - minValue) / step + 1];
                for (int i = minValue; i <= maxValue; i += step) {
                    valueSet[(i - minValue) / step] = i + " × " + i;
                }
                setDividerColor(npView, Color.LTGRAY);
                npView.setMinValue(0);
                npView.setMaxValue(valueSet.length - 1);
                npView.setDisplayedValues(valueSet);
                npView.setValue((mConfig.getIdenticonSize() - minValue) / step);

                new AlertDialog.Builder(IdenticonsSettings.this)
                        .setView(npView)
                        .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                int value = npView.getValue() * step + minValue;
                                mConfig.setIdenticonSize(value);
                                sizePref.setSummary(value + " × " + value);
                            }
                        })
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .create().show();
                return true;
            }
        });

        mSerifPref = (SwitchPreference) findPreference(Config.PREF_SERIF);
        mSerifPref.setChecked(mConfig.isIdenticonSerif());
        if (mConfig.getIdenticonStyle() != IdenticonFactory.IDENTICON_STYLE_GMAIL)
            mSerifPref.setEnabled(false);
        mSerifPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean serif = !mConfig.isIdenticonSerif();
                mConfig.setIdenticonSerif(serif);
                return true;
            }
        });

        mLengthPref = findPreference(Config.PREF_LENGTH);
        final int length = mConfig.getIdenticonLength();
        final String length_summary = " (Text may overflow when too long)";
        mLengthPref.setSummary(length + length_summary);
        mLengthPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final NumberPicker npView = new NumberPicker(IdenticonsSettings.this);

                final int minValue = 1;
                final int maxValue = 5;
                setDividerColor(npView, Color.LTGRAY);
                npView.setMinValue(minValue);
                npView.setMaxValue(maxValue);
                npView.setValue((mConfig.getIdenticonLength()));

                new AlertDialog.Builder(IdenticonsSettings.this)
                        .setView(npView)
                        .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                int length = npView.getValue();
                                mConfig.setIdenticonLength(length);
                                mLengthPref.setSummary(length + length_summary);
                            }
                        })
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .create().show();
                return true;
            }
        });
        if (mConfig.getIdenticonStyle() != IdenticonFactory.IDENTICON_STYLE_GMAIL)
            mLengthPref.setEnabled(false);

        mBgColorPref = findPreference(Config.PREF_BG_COLOR);
        if (mConfig.getIdenticonStyle() == IdenticonFactory.IDENTICON_STYLE_GMAIL)
            mBgColorPref.setEnabled(false);
        mBgColorPref.setSummary(colorIntToRGB(mConfig.getIdenticonBgColor()));
        mBgColorPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LayoutInflater inflater = IdenticonsSettings.this.getLayoutInflater();
                View colorPickerView = inflater.inflate(R.layout.color_picker, (ViewGroup) findViewById(R.id.color_picker_root));

                final ColorPicker colorPicker = (ColorPicker) colorPickerView.findViewById(R.id.picker);
                SaturationBar saturationBar = (SaturationBar) colorPickerView.findViewById(R.id.saturationbar);
                ValueBar valueBar = (ValueBar) colorPickerView.findViewById(R.id.valuebar);
                final OpacityBar opacityBar = (OpacityBar) colorPickerView.findViewById(R.id.opacitybar);
                final TextView valueTextView = (TextView) colorPickerView.findViewById(R.id.value);
                Button applyButton = (Button) colorPickerView.findViewById(R.id.apply_value);

                colorPicker.addSaturationBar(saturationBar);
                colorPicker.addValueBar(valueBar);
                colorPicker.addOpacityBar(opacityBar);

                int savedColor = mConfig.getIdenticonBgColor();
                colorPicker.setColor(savedColor);
                valueTextView.setText(colorIntToRGB(mConfig.getIdenticonBgColor()));

                colorPicker.setOnColorChangedListener(new ColorPicker.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int color) {
                        valueTextView.setText(colorIntToRGB(color));
                    }
                });

                applyButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String value = "#" + valueTextView.getText().toString();
                        try {
                            int color = Color.parseColor(value);
                            colorPicker.setColor(color);
                        } catch (IllegalArgumentException e) {
                            Toast.makeText(IdenticonsSettings.this, getString(R.string.invalid_color), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                AlertDialog alertDialog = new AlertDialog.Builder(IdenticonsSettings.this)
                        .setView(colorPickerView)
                        .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        })
                        .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                int color = colorPicker.getColor();
                                mConfig.setIdenticonBgColor(color);
                                mBgColorPref.setSummary(colorIntToRGB(color));
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
                alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                return true;
            }
        });

        checkPermissions();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_CONTACTS)) {
                new AlertDialog.Builder(this)
                        .setTitle("Permission required")
                        .setMessage("This app requires access to your contacts to function")
                        .setPositiveButton("Request permission", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(IdenticonsSettings.this,
                                        new String[]{Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS},
                                        PERMISSIONS_REQUEST_CODE);
                            }
                        })
                        .show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS},
                        PERMISSIONS_REQUEST_CODE);
            }
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        File prefsDir = new File(this.getApplicationInfo().dataDir, "shared_prefs");
        File prefsFile = new File(prefsDir, getPreferenceManager().getSharedPreferencesName() + ".xml");
        if (prefsFile.exists()) prefsFile.setReadable(true, false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "This app cannot function without the required permissions", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mEnabledPref) {
            mConfig.setEnabled((Boolean) newValue);
            if ((Boolean) newValue && !mConfig.isXposedModActive())
                startService(new Intent(this, ContactsObserverService.class));
            else if (!mConfig.isXposedModActive())
                stopService(new Intent(this, ContactsObserverService.class));
            return true;
        } else if (preference == mStylePref) {
            int style = Integer.valueOf((String) newValue);
            updateStyleSummary(style);
            mBgColorPref.setEnabled(style != IdenticonFactory.IDENTICON_STYLE_GMAIL);
            mSerifPref.setEnabled(style == IdenticonFactory.IDENTICON_STYLE_GMAIL);
            mLengthPref.setEnabled(style == IdenticonFactory.IDENTICON_STYLE_GMAIL);
            return true;
        }
        return false;
    }

    private void updateStyleSummary(int value) {
        mStylePref.setSummary(mStylePref.getEntries()[mStylePref.findIndexOfValue("" + value)]);
        mConfig.setIdenticonStyle(value);
    }

    public int getMaxContactPhotoSize() {
        final Uri uri = ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI;
        final String[] projection = new String[]{ContactsContract.DisplayPhoto.DISPLAY_MAX_DIM};
        final Cursor c = this.getContentResolver().query(uri, projection, null, null, null);
        try {
            c.moveToFirst();
            return c.getInt(0);
        } finally {
            c.close();
        }
    }

    public String colorIntToRGB(int color) {
        return String.format("%08X", color);
    }

    private void setDividerColor(NumberPicker picker, int color) {
        java.lang.reflect.Field[] pickerFields = NumberPicker.class.getDeclaredFields();
        for (java.lang.reflect.Field pf : pickerFields) {
            if (pf.getName().equals("mSelectionDivider")) {
                pf.setAccessible(true);
                try {
                    ColorDrawable colorDrawable = new ColorDrawable(color);
                    pf.set(picker, colorDrawable);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (Resources.NotFoundException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
}
