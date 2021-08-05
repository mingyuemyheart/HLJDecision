package com.hlj.dto;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 联系人
 */
public class ContactDto implements Parcelable {

    public String name, company, number, prefix;
    public int drawable;

    public ContactDto() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.company);
        dest.writeString(this.number);
        dest.writeString(this.prefix);
        dest.writeInt(this.drawable);
    }

    protected ContactDto(Parcel in) {
        this.name = in.readString();
        this.company = in.readString();
        this.number = in.readString();
        this.prefix = in.readString();
        this.drawable = in.readInt();
    }

    public static final Creator<ContactDto> CREATOR = new Creator<ContactDto>() {
        @Override
        public ContactDto createFromParcel(Parcel source) {
            return new ContactDto(source);
        }

        @Override
        public ContactDto[] newArray(int size) {
            return new ContactDto[size];
        }
    };
}
