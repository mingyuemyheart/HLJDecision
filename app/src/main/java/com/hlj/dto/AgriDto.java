package com.hlj.dto;

import android.os.Parcel;
import android.os.Parcelable;

import com.hlj.common.ColumnData;

import java.util.ArrayList;
import java.util.List;

public class AgriDto implements Parcelable{

	public String columnId;
	public String id;//localviewid
	public String name;//名称
	public String showType;
	public String icon;
	public String icon2;
	public String title;
	public String dataUrl;
	public String time;
	public String type;//区分pdf、html等
	public List<ColumnData> child = new ArrayList<>();
	public double lat;
	public double lng;
	public String radarId;
	
	//预警信号
	public String warningType;
	public String blue;
	public String blueCode;
	public String yellow;
	public String yellowCode;
	public String orange;
	public String orangeCode;
	public String red;
	public String redCode;

	public AgriDto() {
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.columnId);
		dest.writeString(this.id);
		dest.writeString(this.name);
		dest.writeString(this.showType);
		dest.writeString(this.icon);
		dest.writeString(this.icon2);
		dest.writeString(this.title);
		dest.writeString(this.dataUrl);
		dest.writeString(this.time);
		dest.writeString(this.type);
		dest.writeTypedList(this.child);
		dest.writeDouble(this.lat);
		dest.writeDouble(this.lng);
		dest.writeString(this.radarId);
		dest.writeString(this.warningType);
		dest.writeString(this.blue);
		dest.writeString(this.blueCode);
		dest.writeString(this.yellow);
		dest.writeString(this.yellowCode);
		dest.writeString(this.orange);
		dest.writeString(this.orangeCode);
		dest.writeString(this.red);
		dest.writeString(this.redCode);
	}

	protected AgriDto(Parcel in) {
		this.columnId = in.readString();
		this.id = in.readString();
		this.name = in.readString();
		this.showType = in.readString();
		this.icon = in.readString();
		this.icon2 = in.readString();
		this.title = in.readString();
		this.dataUrl = in.readString();
		this.time = in.readString();
		this.type = in.readString();
		this.child = in.createTypedArrayList(ColumnData.CREATOR);
		this.lat = in.readDouble();
		this.lng = in.readDouble();
		this.radarId = in.readString();
		this.warningType = in.readString();
		this.blue = in.readString();
		this.blueCode = in.readString();
		this.yellow = in.readString();
		this.yellowCode = in.readString();
		this.orange = in.readString();
		this.orangeCode = in.readString();
		this.red = in.readString();
		this.redCode = in.readString();
	}

	public static final Creator<AgriDto> CREATOR = new Creator<AgriDto>() {
		@Override
		public AgriDto createFromParcel(Parcel source) {
			return new AgriDto(source);
		}

		@Override
		public AgriDto[] newArray(int size) {
			return new AgriDto[size];
		}
	};
}
