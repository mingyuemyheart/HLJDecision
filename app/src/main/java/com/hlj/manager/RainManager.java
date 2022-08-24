package com.hlj.manager;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;
import android.util.Log;

import com.hlj.utils.SecretUrlUtil;

public class RainManager {

	public final static String APPID = "6f688d62594549a2";//机密需要用到的AppId
	public final static String CHINAWEATHER_DATA = "chinaweather_data";//加密秘钥名称
	private final static String URL = "http://scapi.weather.com.cn/weather/raintime";//请求距离下雨时间的URL

	public static String getDate(Calendar calendar, String format) {
		String date = null;
		SimpleDateFormat dateFormat = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
		dateFormat.applyPattern(format);
		date = dateFormat.format(calendar.getTime());
		return date;
	}

	/**
	 * 加密请求字符串
	 * @param lng 经度
	 * @param lat 维度
	 * @return
	 */
	public static final String getSecretUrl(double lng, double lat) {
		String sysdate = getDate(Calendar.getInstance(), "yyyyMMddHHmm");//系统时间

		StringBuffer buffer = new StringBuffer();
		buffer.append(URL);
		buffer.append("?");
		buffer.append("lon=").append(lng);
		buffer.append("&");
		buffer.append("lat=").append(lat);
		buffer.append("&");
		buffer.append("date=").append(sysdate);
		buffer.append("&");
		buffer.append("appid=").append(APPID);

		String key = SecretUrlUtil.getKey(CHINAWEATHER_DATA, buffer.toString());
		buffer.delete(buffer.lastIndexOf("&"), buffer.length());

		buffer.append("&");
		buffer.append("appid=").append(APPID.substring(0, 6));
		buffer.append("&");
		buffer.append("key=").append(key.substring(0, key.length() - 3));
		String result = buffer.toString();
		return result;
	}

}
