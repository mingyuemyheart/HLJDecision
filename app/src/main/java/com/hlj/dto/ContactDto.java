package com.hlj.dto;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 联系人
 */
public class ContactDto implements Parcelable {

    public String id, name, company, worktelephone, letter, type;
    public int drawable;

    public ContactDto() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.name);
        dest.writeString(this.company);
        dest.writeString(this.worktelephone);
        dest.writeString(this.letter);
        dest.writeString(this.type);
        dest.writeInt(this.drawable);
    }

    protected ContactDto(Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.company = in.readString();
        this.worktelephone = in.readString();
        this.letter = in.readString();
        this.type = in.readString();
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
