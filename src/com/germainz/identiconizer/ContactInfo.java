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

import android.os.Parcel;
import android.os.Parcelable;

public class ContactInfo implements Parcelable {
    public int nameRawContactId;
    public String contactName;
    public int contactPhotoSize;

    public ContactInfo(int id, String name) {
        nameRawContactId = id;
        contactName = name;
    }

    public ContactInfo(int id, String name, int photoSize) {
        nameRawContactId = id;
        contactName = name;
        contactPhotoSize = photoSize;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(nameRawContactId);
        out.writeString(contactName);
        out.writeInt(contactPhotoSize);
    }

    public ContactInfo(Parcel in) {
        nameRawContactId = in.readInt();
        contactName = in.readString();
        contactPhotoSize = in.readInt();
    }

    public static final Parcelable.Creator<ContactInfo> CREATOR = new Parcelable.Creator<ContactInfo>() {
        public ContactInfo createFromParcel(Parcel in) {
            return new ContactInfo(in);
        }

        public ContactInfo[] newArray(int size) {
            return new ContactInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

}
