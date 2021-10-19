package com.hlj.common;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class ColumnData implements Parcelable{

	public String columnId,groupColumnId;//栏目id
	public String id;//频道id,区分频道标示
	public String name;//频道名称
	public String level;//1为显示，0为不显示
	public String showType;//分为local、news
	public String icon;//未选中图片地址
	public String icon2;//选中图片地址
	public String desc;//描述信息
	public String dataUrl;//如果存在，则是网页数据
	public String newsType;//阅读量
	public String newsCount;//文章数
	public List<ColumnData> child = new ArrayList<ColumnData>();//儿子辈
	
	public List<ColumnData> banners = new ArrayList<ColumnData>();//首页viewpager子栏目集合
	public String localTag;
	public String bannerUrl;
	public String imgUrl;

	//模块管理用
	public int section;
	public String headerName;
	public boolean isSelected = true;
	public String deleteable;

	public ColumnData() {
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.columnId);
		dest.writeString(this.groupColumnId);
		dest.writeString(this.id);
		dest.writeString(this.name);
		dest.writeString(this.level);
		dest.writeString(this.showType);
		dest.writeString(this.icon);
		dest.writeString(this.icon2);
		dest.writeString(this.desc);
		dest.writeString(this.dataUrl);
		dest.writeString(this.newsType);
		dest.writeString(this.newsCount);
		dest.writeTypedList(this.child);
		dest.writeTypedList(this.banners);
		dest.writeString(this.localTag);
		dest.writeString(this.bannerUrl);
		dest.writeString(this.imgUrl);
		dest.writeInt(this.section);
		dest.writeString(this.headerName);
		dest.writeByte(this.isSelected ? (byte) 1 : (byte) 0);
		dest.writeString(this.deleteable);
	}

	protected ColumnData(Parcel in) {
		this.columnId = in.readString();
		this.groupColumnId = in.readString();
		this.id = in.readString();
		this.name = in.readString();
		this.level = in.readString();
		this.showType = in.readString();
		this.icon = in.readString();
		this.icon2 = in.readString();
		this.desc = in.readString();
		this.dataUrl = in.readString();
		this.newsType = in.readString();
		this.newsCount = in.readString();
		this.child = in.createTypedArrayList(ColumnData.CREATOR);
		this.banners = in.createTypedArrayList(ColumnData.CREATOR);
		this.localTag = in.readString();
		this.bannerUrl = in.readString();
		this.imgUrl = in.readString();
		this.section = in.readInt();
		this.headerName = in.readString();
		this.isSelected = in.readByte() != 0;
		this.deleteable = in.readString();
	}

	public static final Creator<ColumnData> CREATOR = new Creator<ColumnData>() {
		@Override
		public ColumnData createFromParcel(Parcel source) {
			return new ColumnData(source);
		}

		@Override
		public ColumnData[] newArray(int size) {
			return new ColumnData[size];
		}
	};
}
