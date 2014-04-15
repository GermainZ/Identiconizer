/*
 * Copyright (C) 2013 GermainZ@xda-developers.com
 * Portions Copyright (C) 2013 The ChameleonOS Open Source Project
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

package com.germainz.identiconizer.xposed;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract;
import android.text.TextUtils;

import com.germainz.identiconizer.Config;
import com.germainz.identiconizer.identicons.IdenticonFactory;
import com.germainz.identiconizer.identicons.Identicon;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class XposedMod implements IXposedHookLoadPackage {
    private static final Config CONFIG = new Config();

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if ("com.android.providers.contacts".equals(lpparam.packageName)) {
            findAndHookMethod("com.android.providers.contacts.DataRowHandlerForStructuredName",
                    null, "insert", SQLiteDatabase.class,
                    "com.android.providers.contacts.TransactionContext",
                    long.class, ContentValues.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    CONFIG.reload();
                    if (CONFIG.isEnabled()) {
                        ContentValues values = (ContentValues) param.args[3];
                        String name = values.getAsString(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME);

                        if (!TextUtils.isEmpty(name)) {
                            long rawContactId = ((Number) param.args[2]).longValue();
                            SQLiteDatabase db = (SQLiteDatabase) param.args[0];
                            Identicon identicon = IdenticonFactory.makeIdenticon(CONFIG.getIdenticonStyle(),
                                    CONFIG.getIdenticonSize());

                            ContentValues identiconValues = new ContentValues();
                            identiconValues.put("mimetype_id", 10);
                            identiconValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
                            identiconValues.put(ContactsContract.CommonDataKinds.Photo.PHOTO,
                                    identicon.generateIdenticonByteArray(name));
                            identiconValues.put(ContactsContract.Data.IS_PRIMARY, 1);
                            identiconValues.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);

                            db.insert(ContactsContract.Contacts.Data.CONTENT_DIRECTORY, null, identiconValues);
                        }
                    }
                }
            });
        }

        if (Config.PACKAGE_NAME.equals(lpparam.packageName)) {
            findAndHookMethod(Config.PACKAGE_NAME + ".Config", lpparam.classLoader,
                    "isXposedModActive", XC_MethodReplacement.returnConstant(true));
        }
    }

}
