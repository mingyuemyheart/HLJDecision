package com.hlj.manager;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;
import android.util.Log;

import com.hlj.utils.SecretUrlUtil;

public class StationSecretManager {

	private static String SANX_DATA_99 = "sanx_data_99";//加密秘钥名称
	private static String APPID = "f63d329270a44900";//机密需要用到的AppId
	
	public static String getDate(Calendar calendar, String format) {
		String date = null;
		SimpleDateFormat dateFormat = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
		dateFormat.applyPattern(format);
		date = dateFormat.format(calendar.getTime());
		return date;
	}
	
	/**
	 * 加密请求字符串
	 * @param url 基本串
	 * @return
	 */
	public static String getStationUrl(String url, String stationIds) {
		String sysdate = getDate(Calendar.getInstance(), "yyyyMMddHHmmss");//系统时间
		StringBuffer buffer = new StringBuffer();
		buffer.append(url);
		buffer.append("?");
		buffer.append("stationids=").append(stationIds);
		buffer.append("&");
		buffer.append("date=").append(sysdate);
		buffer.append("&");
		buffer.append("appid=").append(APPID);
		
		String key = SecretUrlUtil.getKey(SANX_DATA_99, buffer.toString());
		buffer.delete(buffer.lastIndexOf("&"), buffer.length());
		
		buffer.append("&");
		buffer.append("appid=").append(APPID.substring(0, 6));
		buffer.append("&");
		buffer.append("key=").append(key.substring(0, key.length() - 3));
		String result = buffer.toString();
		return result;
	}

}
