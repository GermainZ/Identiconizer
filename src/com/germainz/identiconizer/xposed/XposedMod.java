/*
 * Copyright (C) 2013-2014 GermainZ@xda-developers.com
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

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.germainz.identiconizer.Config;
import com.germainz.identiconizer.R;
import com.germainz.identiconizer.identicons.Identicon;
import com.germainz.identiconizer.identicons.IdenticonFactory;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;

public class XposedMod implements IXposedHookLoadPackage, IXposedHookInitPackageResources, IXposedHookZygoteInit {
    private static final Config CONFIG = new Config();
    private static String MODULE_PATH;
    private static int NOTIF_ICON_RES_ID;

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if ("com.android.providers.contacts".equals(lpparam.packageName)) {
            try {
                findAndHookMethod("com.android.providers.contacts.DataRowHandlerForStructuredName",
                        lpparam.classLoader, "insert", SQLiteDatabase.class,
                        "com.android.providers.contacts.TransactionContext",
                        long.class, ContentValues.class, new XC_MethodHook() {

                            @Override
                            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                                CONFIG.reload();
                                if (CONFIG.isEnabled()) {
                                    ContentValues values = (ContentValues) param.args[3];
                                    String name = values.getAsString(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME);

                                    if (!TextUtils.isEmpty(name)) {
                                        long rawContactId = ((Number) param.args[2]).longValue();
                                        SQLiteDatabase db = (SQLiteDatabase) param.args[0];
                                        Identicon identicon = IdenticonFactory.makeIdenticon(CONFIG.getIdenticonStyle(),
                                                CONFIG.getIdenticonSize(), CONFIG.getIdenticonBgColor(), CONFIG.getIdenticonLength());

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
                        }
                );
            } catch (Throwable e) {
                Context systemContext = (Context) getStaticObjectField(findClass("android.app.ActivityThread", null), "mSystemContext");
                if (systemContext == null) {
                    Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
                    systemContext = (Context) callMethod(activityThread, "getSystemContext");
                }

                Context contactsProviderContext = systemContext.createPackageContext("com.android.providers.contacts", Context.CONTEXT_IGNORE_SECURITY);
                Context identiconizerContext = systemContext.createPackageContext(Config.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);

                String contentText = identiconizerContext.getString(R.string.xposed_error_text);
                Notification notice = new NotificationCompat.Builder(contactsProviderContext)
                        .setSmallIcon(NOTIF_ICON_RES_ID)
                        .setContentTitle(identiconizerContext.getString(R.string.xposed_error_title))
                        .setContentText(contentText)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                        .build();
                NotificationManager nm = (NotificationManager) contactsProviderContext.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(1, notice);
            }
        }

        if (Config.PACKAGE_NAME.equals(lpparam.packageName)) {
            findAndHookMethod(Config.PACKAGE_NAME + ".Config", lpparam.classLoader,
                    "isXposedModActive", XC_MethodReplacement.returnConstant(true));
        }
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals("com.android.providers.contacts"))
            return;
        Resources res = XModuleResources.createInstance(MODULE_PATH, resparam.res);
        NOTIF_ICON_RES_ID = resparam.res.addResource(res, R.drawable.ic_settings_identicons);
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
    }
}
