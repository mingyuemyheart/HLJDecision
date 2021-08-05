package com.hlj.dto;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * 灾情反馈
 */
public class DisasterDto implements Parcelable {

    public String disasterName,disasterType;//对应预警类型名称,预警类型，如11B09
    public String aoiName,addr,title,content,time,imgUrl,imageName,createtime,reply_content;
    public boolean isSelected;
    public boolean isLastItem;//为了区分添加按钮
    public ArrayList<String> imgList = new ArrayList<>();//图片集合

    public DisasterDto() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.disasterName);
        dest.writeString(this.disasterType);
        dest.writeString(this.aoiName);
        dest.writeString(this.addr);
        dest.writeString(this.title);
        dest.writeString(this.content);
        dest.writeString(this.time);
        dest.writeString(this.imgUrl);
        dest.writeString(this.imageName);
        dest.writeString(this.createtime);
        dest.writeString(this.reply_content);
        dest.writeByte(this.isSelected ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isLastItem ? (byte) 1 : (byte) 0);
        dest.writeStringList(this.imgList);
    }

    protected DisasterDto(Parcel in) {
        this.disasterName = in.readString();
        this.disasterType = in.readString();
        this.aoiName = in.readString();
        this.addr = in.readString();
        this.title = in.readString();
        this.content = in.readString();
        this.time = in.readString();
        this.imgUrl = in.readString();
        this.imageName = in.readString();
        this.createtime = in.readString();
        this.reply_content = in.readString();
        this.isSelected = in.readByte() != 0;
        this.isLastItem = in.readByte() != 0;
        this.imgList = in.createStringArrayList();
    }

    public static final Creator<DisasterDto> CREATOR = new Creator<DisasterDto>() {
        @Override
        public DisasterDto createFromParcel(Parcel source) {
            return new DisasterDto(source);
        }

        @Override
        public DisasterDto[] newArray(int size) {
            return new DisasterDto[size];
        }
    };
}
