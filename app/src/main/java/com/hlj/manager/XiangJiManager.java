package com.hlj.manager;

import android.util.Base64;
import android.util.Log;

import com.hlj.utils.SecretUrlUtil;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class XiangJiManager {

	private final static String APPID = "182e7b37a63445558b05fbcce2b3d6e7";//机密需要用到的AppId
	private final static String CHINAWEATHER_DATA = "9d0232248739420fa4ff19593c731c11";//加密秘钥名称
	private final static String URL = "https://scapi-py.tianqi.cn/api/aqi/fc/coor/";
	
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
	public static final String getXJSecretUrl(double lng, double lat, String start, String end, long timestamp) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(URL);
		buffer.append(lng).append("/").append(lat);
		buffer.append("/h/");
		buffer.append(start).append("/").append(end);
		buffer.append(".json?");
		buffer.append("appid=").append(APPID);
		buffer.append("&");
		buffer.append("timestamp=").append(timestamp);
		buffer.append("&");
		
		String key = SecretUrlUtil.getKey(CHINAWEATHER_DATA, timestamp+APPID);
		buffer.delete(buffer.lastIndexOf("&"), buffer.length());
		
		buffer.append("&");
		buffer.append("key=").append(key.substring(0, key.length() - 3));
		String result = buffer.toString();
		Log.e("getXJSecretUrl", result);
		return result;
	}
	
	/**
	 * 加密请求字符串
	 * @param lng 经度
	 * @param lat 维度
	 * @return
	 */
	public static final String getXJSecretUrl2(double lng, double lat, String start, String end, long timestamp) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(URL);
		buffer.append(lng).append("/").append(lat);
		buffer.append("/d/");
		buffer.append(start).append("/").append(end);
		buffer.append(".json?");
		buffer.append("appid=").append(APPID);
		buffer.append("&");
		buffer.append("timestamp=").append(timestamp);
		buffer.append("&");
		
		String key = SecretUrlUtil.getKey(CHINAWEATHER_DATA, timestamp+APPID);
		buffer.delete(buffer.lastIndexOf("&"), buffer.length());
		
		buffer.append("&");
		buffer.append("key=").append(key.substring(0, key.length() - 3));
		String result = buffer.toString();
		Log.e("getXJSecretUrl2", result);
		return result;
	}

}
