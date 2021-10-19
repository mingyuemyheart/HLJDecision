package com.hlj.dto;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class FactDto implements Parcelable{

	public String name;
	public String temp;
	public String tMax;
	public String tMin;
	public String windSpeed;
	public String windDir;
	public String rain;
	public List<FactDto> nationList = new ArrayList<>();//90个国家站
	public List<FactDto> topList = new ArrayList<>();//前100自动站
	public List<FactDto> bottomList = new ArrayList<>();//后100自动站
	public String imgUrl;
	public String time;
	public List<FactDto> imgs = new ArrayList<>();
	public String itemName;//如温度里的当前最高温
	public String isSelect;//是否选中(0未选中，1选中)
	public String flag;//区分类型

	public String id;
	public String dataUrl;
	public String timeString;
	public String timeStart;
	public String timeParams;
	public String stationCode;
	public String stationName;
	public String area;
	public String area1;
	public double val;
	public double val1 = -1;
	public String cityName;
	public double lng;
	public double lat;
	public String title;
	public String icon1, icon2;
	public List<FactDto> itemList = new ArrayList<>();

	public String rainLevel;
	public String count;
	public List<FactDto> areaList = new ArrayList<>();//地图下方列表

	public float factRain;//实况降水
	public float factTemp;//实况温度
	public float factWind;//实况风速
	public String factTime;
	public float x = 0;
	public float y = 0;
	public String humidity;
	public String visibility;

	public FactDto() {
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.name);
		dest.writeString(this.temp);
		dest.writeString(this.tMax);
		dest.writeString(this.tMin);
		dest.writeString(this.windSpeed);
		dest.writeString(this.windDir);
		dest.writeString(this.rain);
		dest.writeTypedList(this.nationList);
		dest.writeTypedList(this.topList);
		dest.writeTypedList(this.bottomList);
		dest.writeString(this.imgUrl);
		dest.writeString(this.time);
		dest.writeTypedList(this.imgs);
		dest.writeString(this.itemName);
		dest.writeString(this.isSelect);
		dest.writeString(this.flag);
		dest.writeString(this.id);
		dest.writeString(this.dataUrl);
		dest.writeString(this.timeString);
		dest.writeString(this.timeStart);
		dest.writeString(this.timeParams);
		dest.writeString(this.stationCode);
		dest.writeString(this.stationName);
		dest.writeString(this.area);
		dest.writeString(this.area1);
		dest.writeDouble(this.val);
		dest.writeDouble(this.val1);
		dest.writeString(this.cityName);
		dest.writeDouble(this.lng);
		dest.writeDouble(this.lat);
		dest.writeString(this.title);
		dest.writeString(this.icon1);
		dest.writeString(this.icon2);
		dest.writeTypedList(this.itemList);
		dest.writeString(this.rainLevel);
		dest.writeString(this.count);
		dest.writeTypedList(this.areaList);
		dest.writeFloat(this.factRain);
		dest.writeFloat(this.factTemp);
		dest.writeFloat(this.factWind);
		dest.writeString(this.factTime);
		dest.writeFloat(this.x);
		dest.writeFloat(this.y);
		dest.writeString(this.humidity);
		dest.writeString(this.visibility);
	}

	protected FactDto(Parcel in) {
		this.name = in.readString();
		this.temp = in.readString();
		this.tMax = in.readString();
		this.tMin = in.readString();
		this.windSpeed = in.readString();
		this.windDir = in.readString();
		this.rain = in.readString();
		this.nationList = in.createTypedArrayList(FactDto.CREATOR);
		this.topList = in.createTypedArrayList(FactDto.CREATOR);
		this.bottomList = in.createTypedArrayList(FactDto.CREATOR);
		this.imgUrl = in.readString();
		this.time = in.readString();
		this.imgs = in.createTypedArrayList(FactDto.CREATOR);
		this.itemName = in.readString();
		this.isSelect = in.readString();
		this.flag = in.readString();
		this.id = in.readString();
		this.dataUrl = in.readString();
		this.timeString = in.readString();
		this.timeStart = in.readString();
		this.timeParams = in.readString();
		this.stationCode = in.readString();
		this.stationName = in.readString();
		this.area = in.readString();
		this.area1 = in.readString();
		this.val = in.readDouble();
		this.val1 = in.readDouble();
		this.cityName = in.readString();
		this.lng = in.readDouble();
		this.lat = in.readDouble();
		this.title = in.readString();
		this.icon1 = in.readString();
		this.icon2 = in.readString();
		this.itemList = in.createTypedArrayList(FactDto.CREATOR);
		this.rainLevel = in.readString();
		this.count = in.readString();
		this.areaList = in.createTypedArrayList(FactDto.CREATOR);
		this.factRain = in.readFloat();
		this.factTemp = in.readFloat();
		this.factWind = in.readFloat();
		this.factTime = in.readString();
		this.x = in.readFloat();
		this.y = in.readFloat();
		this.humidity = in.readString();
		this.visibility = in.readString();
	}

	public static final Creator<FactDto> CREATOR = new Creator<FactDto>() {
		@Override
		public FactDto createFromParcel(Parcel source) {
			return new FactDto(source);
		}

		@Override
		public FactDto[] newArray(int size) {
			return new FactDto[size];
		}
	};
}
