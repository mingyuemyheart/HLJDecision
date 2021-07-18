package com.hlj.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolygonOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.hlj.common.CONST;
import com.umeng.socialize.ShareAction;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.media.UMImage;
import com.umeng.socialize.media.UMWeb;
import com.umeng.socialize.shareboard.SnsPlatform;
import com.umeng.socialize.utils.ShareBoardlistener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import shawn.cxwl.com.hlj.R;

public class CommonUtil {

	/** 
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素) 
     */  
    public static float dip2px(Context context, float dpValue) {  
        final float scale = context.getResources().getDisplayMetrics().density;  
        return dpValue * scale;
    }  
  
    /** 
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp 
     */  
    public static float px2dip(Context context, float pxValue) {  
        final float scale = context.getResources().getDisplayMetrics().density;  
        return pxValue / scale;
    }

	public static int widthPixels(Context context) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		return dm.widthPixels;
	}

	public static int heightPixels(Context context) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		return dm.heightPixels;
	}

	/**
	 * 获取版本号
	 * @return 当前应用的版本号
	 */
	public static String getVersion(Context context) {
		try {
			PackageManager manager = context.getPackageManager();
			PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
			return info.versionName;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
    
    /**
	 * 解决ScrollView与ListView共存的问题
	 * 
	 * @param listView
	 */
	public static void setListViewHeightBasedOnChildren(ListView listView) {
		ListAdapter listAdapter = listView.getAdapter();
		if (listAdapter == null) {
			return;
		}

		int totalHeight = 0;
		for (int i = 0; i < listAdapter.getCount(); i++) {
			View listItem = listAdapter.getView(i, null, listView);
			listItem.measure(0, 0); 
			totalHeight += listItem.getMeasuredHeight();
		}

		ViewGroup.LayoutParams params = listView.getLayoutParams();
		params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
		((MarginLayoutParams) params).setMargins(0, 0, 0, 0);
		listView.setLayoutParams(params);
	}
	
	/**
	 * 解决ScrollView与GridView共存的问题
	 */
	public static void setGridViewHeightBasedOnChildren(GridView gridView) {
		ListAdapter listAdapter = gridView.getAdapter();
		if (listAdapter == null) {
			return;
		}
		
		Class<GridView> tempGridView = GridView.class; // 获得gridview这个类的class
		int column = -1;
        try {
 
            Field field = tempGridView.getDeclaredField("mRequestedNumColumns"); // 获得申明的字段
            field.setAccessible(true); // 设置访问权限
            column = Integer.valueOf(field.get(gridView).toString()); // 获取字段的值
        } catch (Exception e1) {
        }

		int totalHeight = 0;
		for (int i = 0; i < listAdapter.getCount(); i+=column) {
			View listItem = listAdapter.getView(i, null, gridView);
			listItem.measure(0, 0); 
			totalHeight += listItem.getMeasuredHeight();
		}

		ViewGroup.LayoutParams params = gridView.getLayoutParams();
		params.height = totalHeight + (gridView.getVerticalSpacing() * (listAdapter.getCount()/column - 1) + 30);
		((MarginLayoutParams) params).setMargins(15, 15, 15, 0);
		gridView.setLayoutParams(params);
	}
	
	/**
	 * 判断白天或晚上对应的天气现象
	 * @param fa 白天天气现象编号
	 * @param fb 晚上天气现象编号
	 * @return
	 */
	public static int getPheCode(int fa, int fb) {
		int pheCode = 0;
		SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm");
		
		try {
			long currentTime = new Date().getTime();//当前时间
			long eight = sdf1.parse("08:00").getTime();//早上08:00
			long twenty = sdf1.parse("20:00").getTime();//晚上20:00
			
			if (currentTime >= eight && currentTime <= twenty) {
				pheCode = fa;
			}else {
				pheCode = fb;
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return pheCode;
	}
	
	/**
	 * 从Assets中读取图片
	 */
	public static Bitmap getImageFromAssetsFile(Context context, String fileName) {
		Bitmap image = null;
		AssetManager am = context.getResources().getAssets();
		try {
			InputStream is = am.open(fileName);
			image = BitmapFactory.decodeStream(is);
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return image;
	}
	
	/**
	 * 获取圆角图片
	 * @param bitmap
	 * @param corner
	 * @return
	 */
	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int corner) {
		try {
			Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
			Canvas canvas = new Canvas(output);
			final Paint paint = new Paint();
			final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
			final RectF rectF = new RectF(new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()));
			paint.setAntiAlias(true);
			canvas.drawARGB(0, 0, 0, 0);
			paint.setColor(Color.BLACK);
			canvas.drawRoundRect(rectF, corner, corner, paint);
			paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));

			final Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
			canvas.drawBitmap(bitmap, src, rect, paint);
			bitmap.recycle();
			return output;
		} catch (Exception e) {
			return bitmap;
		}
	}
	
	/**
	 * 隐藏虚拟键盘
	 * @param editText 输入框
	 * @param context 上下文
	 */
	public static void hideInputSoft(EditText editText, Context context) {
		InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
	}
	
	/**
	 * 转换图片成圆形
	 * 
	 * @param bitmap
	 *            传入Bitmap对象
	 * @return
	 */
	public static Bitmap toRoundBitmap(Bitmap bitmap) {
		if (bitmap == null) {
			return null;
		}
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		float roundPx;
		float left, top, right, bottom, dst_left, dst_top, dst_right, dst_bottom;
		if (width <= height) {
			roundPx = width / 2;
			top = 0;
			bottom = width;
			left = 0;
			right = width;
			height = width;
			dst_left = 0;
			dst_top = 0;
			dst_right = width;
			dst_bottom = width;
		} else {
			roundPx = height / 2;
			float clip = (width - height) / 2;
			left = clip;
			right = width - clip;
			top = 0;
			bottom = height;
			width = height;
			dst_left = 0;
			dst_top = 0;
			dst_right = height;
			dst_bottom = height;
		}
		Bitmap output = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect src = new Rect((int) left, (int) top, (int) right, (int) bottom);
		final Rect dst = new Rect((int) dst_left, (int) dst_top, (int) dst_right, (int) dst_bottom);
		final RectF rectF = new RectF(dst);
		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, src, dst, paint);
		return output;
	}
	
	/**
     * 获取网落图片资源 
     * @param url
     * @return
     */
	public static Bitmap getHttpBitmap(String url) {
		URL myFileURL;
		Bitmap bitmap = null;
		try {
			myFileURL = new URL(url);
			// 获得连接
			HttpURLConnection conn = (HttpURLConnection) myFileURL.openConnection();
			// 设置超时时间为6000毫秒，conn.setConnectionTiem(0);表示没有时间限制
			conn.setConnectTimeout(6000);
			// 连接设置获得数据流
			conn.setDoInput(true);
			// 不使用缓存
			conn.setUseCaches(false);
			// 这句可有可无，没有影响
			conn.connect();
			// 得到数据流
			InputStream is = conn.getInputStream();
			// 解析得到图片
			bitmap = BitmapFactory.decodeStream(is);
			// 关闭数据流
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		}
		return bitmap;
	}
	
	/**
	 * 转换图片成六边形
	 * @return
	 */
	public static Bitmap getHexagonShape(Bitmap bitmap) {
		int targetWidth = bitmap.getWidth();
		int targetHeight = bitmap.getHeight();
		Bitmap targetBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);

		float radius = targetHeight / 2;
		float triangleHeight = (float) (Math.sqrt(3) * radius / 2);
		float centerX = targetWidth / 2;
		float centerY = targetHeight / 2;
		
		Canvas canvas = new Canvas(targetBitmap);
		canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG|Paint.ANTI_ALIAS_FLAG));
		Path path = new Path();
		path.moveTo(centerX, centerY + radius);
		path.lineTo(centerX - triangleHeight, centerY + radius / 2);
		path.lineTo(centerX - triangleHeight, centerY - radius / 2);
		path.lineTo(centerX, centerY - radius);
		path.lineTo(centerX + triangleHeight, centerY - radius / 2);
		path.lineTo(centerX + triangleHeight, centerY + radius / 2);
		path.moveTo(centerX, centerY + radius);
		canvas.clipPath(path);
		canvas.drawBitmap(bitmap, new Rect(0, 0, targetWidth, targetHeight), new Rect(0, 0, targetWidth, targetHeight), null);
		return targetBitmap;
	}

	/**
	 * 把本地的drawable转换成六边形图片
	 * @param drawable
	 * @return
	 */
	public static Bitmap drawableToBitmap(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		}

		Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		return bitmap;
	}
	
	/**
	 * 根据颜色值判断颜色
	 * @param colorType
	 * @param val
	 * @return
	 */
	public static int colorForValue(String colorType, float val) {
		int color = 0;
		if (TextUtils.equals(colorType, "jiangshui")) {
			if (val >= 0 && val < 1) {
				return 0xff2EAD06;
			} else if (val >= 1 && val < 10) {
				return 0xff000000;
			} else if (val >= 10 && val < 25) {
				return 0xff0901EC;
			} else if (val >= 25 && val < 50) {
				return 0xffC804C8;
			} else if (val >= 50) {
				return 0xffC50724;
			}
		} else if (TextUtils.equals(colorType, "wendu")) {
			if (val < -30) {
				return 0xff201885;
			} else if (val >= -30 && val < -20) {
				return 0xff114AD9;
			} else if (val >= -20 && val < -10) {
				return 0xff4DB4F7;
			} else if (val >= -10 && val < 0) {
				return 0xffD1F8F3;
			} else if (val >= 0 && val < 10) {
				return 0xffF9F2BB;
			} else if (val >= 10 && val < 20) {
				return 0xffF9DE45;
			} else if (val >= 20 && val < 30) {
				return 0xffFFA800;
			} else if (val >= 30 && val < 40) {
				return 0xffFF6D00;
			} else if (val >= 40 && val < 50) {
				return 0xffE60000;
			} else if (val >= 50) {
				return 0xff9E0001;
			}
		} else if (TextUtils.equals(colorType, "bianwen")) {
			if (val > 0) {
				return 0xffFF0000;
			} else if (val == 0) {
				return 0xff000000;
			} else {
				return 0xff0000FF;
			}
		} else if (TextUtils.equals(colorType, "radar")) {
			return 0xffff00ff;
		} else if (TextUtils.equals(colorType, "shidu")) {
			if (val >= 0 && val < 10) {
				return 0xffFF6000;
			} else if (val >= 10 && val < 30) {
				return 0xffFEA51A;
			} else if (val >= 30 && val < 50) {
				return 0xffFFFC9F;
			} else if (val >= 50) {
				return 0xffD6E6DA;
			}
		}

		return color;

	}
	
	/**
	 * 根据风速获取风向标
	 * @param context
	 * @param speed
	 * @return
	 */
	public static Bitmap getWindMarker(Context context, float speed) {
		Bitmap bitmap = null;
		if(speed >= 1 && speed <=2){
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.iv_wind12);
	    }else if(speed >= 3 && speed <=4){
	    	bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.iv_wind34);
	    }else if(speed >= 5 && speed <=6){
	    	bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.iv_wind56);
	    }else if(speed >= 7 && speed <=8){
	    	bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.iv_wind78);
	    }else if(speed >8){
	    	bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.iv_wind8s);
	    }
		return bitmap;
	}

	/**
	 * 根据风速获取风向标
	 * @param context
	 * @param speed
	 * @return
	 */
	public static Bitmap getWindMarker(Context context, double speed) {
		Bitmap bitmap = null;
		if (speed <= 0.2) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.iv_wind12);
		}else if (speed > 0.2 && speed <= 1.5) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.iv_wind12);
		}else if (speed > 1.5 && speed <= 3.3) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.iv_wind12);
		}else if (speed > 3.3 && speed <= 5.4) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.iv_wind34);
		}else if (speed > 5.4 && speed <= 7.9) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.iv_wind34);
		}else if (speed > 7.9 && speed <= 10.7) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.iv_wind56);
		}else if (speed > 10.7 && speed <= 13.8) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.iv_wind56);
		}else if (speed > 13.8 && speed <= 17.1) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.iv_wind78);
		}else if (speed > 17.1 && speed <= 20.7) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.iv_wind78);
		}else if (speed > 20.7 && speed <= 24.4) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.iv_wind8s);
		}else if (speed > 24.4 && speed <= 28.4) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.iv_wind8s);
		}else if (speed > 28.4 && speed <= 32.6) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.iv_wind8s);
		}else if (speed > 32.6 && speed < 99999.0) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.iv_wind8s);
		}
		return bitmap;
	}
	
	/**
	 * 读取assets下文件
	 * @param fileName
	 * @return
	 */
	public static String getFromAssets(Context context, String fileName) {
		String Result = "";
		try {
			InputStreamReader inputReader = new InputStreamReader(context.getResources().getAssets().open(fileName));
			BufferedReader bufReader = new BufferedReader(inputReader);
			String line = "";
			while ((line = bufReader.readLine()) != null)
				Result += line;
			return Result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Result;
	}
	
	/**
	 * 回执区域
	 * @param context
	 * @param aMap
	 */
	public static void drawDistrict(Context context, AMap aMap) {
		if (aMap == null) {
			return;
		}
		String result = CommonUtil.getFromAssets(context, "heilongjiang.json");
		if (!TextUtils.isEmpty(result)) {
			try {
				JSONObject obj = new JSONObject(result);
				JSONArray array = obj.getJSONArray("features");
				int transparency = 255;
				for (int i = 0; i < array.length(); i++) {
					JSONObject itemObj = array.getJSONObject(i);
					
					JSONObject properties = itemObj.getJSONObject("properties");
					String name = properties.getString("name");
					JSONArray cp = properties.getJSONArray("cp");
					for (int m = 0; m < cp.length(); m++) {
						double lat = cp.getDouble(1);
						double lng = cp.getDouble(0);
						
						LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
						View view = inflater.inflate(R.layout.rainfall_fact_marker_view2, null);
						TextView tvName = (TextView) view.findViewById(R.id.tvName);
						if (!TextUtils.isEmpty(name)) {
							tvName.setText(name);
						}
						MarkerOptions options = new MarkerOptions();
						options.anchor(0.5f, 0.5f);
						options.position(new LatLng(lat, lng));
						options.icon(BitmapDescriptorFactory.fromView(view));
						aMap.addMarker(options);
					}
					
					JSONObject geometry = itemObj.getJSONObject("geometry");
					JSONArray coordinates = geometry.getJSONArray("coordinates");
					JSONArray array2 = coordinates.getJSONArray(0);
					PolygonOptions polylineOption = new PolygonOptions();
					transparency = 255-(i+1)*15;
					polylineOption.fillColor(Color.argb(transparency, 0, 97, 194));
					polylineOption.strokeColor(Color.TRANSPARENT);
					for (int j = 0; j < array2.length(); j++) {
						JSONArray itemArray = array2.getJSONArray(j);
						double lng = itemArray.getDouble(0);
						double lat = itemArray.getDouble(1);
						polylineOption.add(new LatLng(lat, lng));
					}
					aMap.addPolygon(polylineOption);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 绘制黑龙江
	 */
	public static void drawHLJJson(Context context, AMap aMap) {
		if (aMap == null) {
			return;
		}
		String result = CommonUtil.getFromAssets(context, "heilongjiang.json");
		if (!TextUtils.isEmpty(result)) {
			try {
                LatLngBounds.Builder builder = LatLngBounds.builder();
				JSONObject obj = new JSONObject(result);
				JSONArray array = obj.getJSONArray("features");
				for (int i = 0; i < array.length(); i++) {
					JSONObject itemObj = array.getJSONObject(i);

					JSONObject properties = itemObj.getJSONObject("properties");
					String name = properties.getString("name");
					JSONObject geometry = itemObj.getJSONObject("geometry");
					JSONArray coordinates = geometry.getJSONArray("coordinates");
					JSONArray array2 = coordinates.getJSONArray(0);
					PolylineOptions polylineOption = new PolylineOptions();
					if (name.contains("加格达奇")) {
						polylineOption.setDottedLine(true);
					}
					polylineOption.width(5).color(0xff406bbf);
					for (int j = 0; j < array2.length(); j++) {
						JSONArray itemArray = array2.getJSONArray(j);
						double lng = itemArray.getDouble(0);
						double lat = itemArray.getDouble(1);
						polylineOption.add(new LatLng(lat, lng));
                        builder.include(new LatLng(lat, lng));
					}
					aMap.addPolyline(polylineOption);
				}
				if (array.length() > 0) {
				    aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50));
                }
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 根据当前时间获取日期
	 * @param i (+1为后一天，-1为前一天，0表示当天)
	 * @return
	 */
	public static String getDate(String time, int i) {
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMddHHmm", Locale.CHINA);
		Calendar c = Calendar.getInstance();
		try {
			Date date = sdf2.parse(time);
			c.setTime(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		c.add(Calendar.DAY_OF_MONTH, i);
		return sdf1.format(c.getTime());
	}

	/**
	 * 根据当前时间获取星期几
	 * @param i (+1为后一天，-1为前一天，0表示当天)
	 * @return
	 */
	public static String getWeek(String time, int i) {
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMddHHmm", Locale.CHINA);
		Calendar c = Calendar.getInstance();
		try {
			Date date = sdf2.parse(time);
			c.setTime(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		c.add(Calendar.DAY_OF_WEEK, i);

		String week = "";
		switch (c.get(Calendar.DAY_OF_WEEK)) {
			case Calendar.SUNDAY:
				week = "周日";
				break;
			case Calendar.MONDAY:
				week = "周一";
				break;
			case Calendar.TUESDAY:
				week = "周二";
				break;
			case Calendar.WEDNESDAY:
				week = "周三";
				break;
			case Calendar.THURSDAY:
				week = "周四";
				break;
			case Calendar.FRIDAY:
				week = "周五";
				break;
			case Calendar.SATURDAY:
				week = "周六";
				break;
		}
		return week;
	}

	/**
	 * 获取http://decision.tianqi.cn域名的请求头
	 * @return
	 */
	public static String getRequestHeader() {
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMdd00");
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd06");
		SimpleDateFormat sdf3 = new SimpleDateFormat("yyyyMMdd12");
		SimpleDateFormat sdf4 = new SimpleDateFormat("yyyyMMdd18");
		SimpleDateFormat sdf5 = new SimpleDateFormat("yyyyMMddHH");
		long time1 = 0, time2 = 0, time3 = 0, time4 = 0;
		long currentTime = 0;
		try {
			time1 = sdf5.parse(sdf1.format(new Date())).getTime();
			time2 = sdf5.parse(sdf2.format(new Date())).getTime();
			time3 = sdf5.parse(sdf3.format(new Date())).getTime();
			time4 = sdf5.parse(sdf4.format(new Date())).getTime();
			currentTime = new Date().getTime();
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		String date = null;
		if (currentTime >= time1 && currentTime < time2) {
			date = sdf1.format(new Date());
		}else if (currentTime >= time2 && currentTime < time3) {
			date = sdf2.format(new Date());
		}else if (currentTime >= time3 && currentTime < time4) {
			date = sdf3.format(new Date());
		}else if (currentTime >= time4) {
			date = sdf4.format(new Date());
		}
		String publicKey = "http://decision.tianqi.cn/?date="+date;//公钥
		String privateKye = "url_private_key_789";//私钥
		String result = "";
		try{
			byte[] rawHmac = null;
			byte[] keyBytes = privateKye.getBytes("UTF-8");
			SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(signingKey);
			rawHmac = mac.doFinal(publicKey.getBytes("UTF-8"));
			result = Base64.encodeToString(rawHmac, Base64.DEFAULT);
//			result = URLEncoder.encode(result, "UTF-8");
			result = "http://decision.tianqi.cn/"+result;
		}catch(Exception e){
			Log.e("SceneException", e.getMessage(), e);
		}
		return result;
	}

	/**
	 * 判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的
	 * @param context
	 * @return true 表示开启
	 */
	public static final boolean isLocationOpen(final Context context) {
		LocationManager locationManager  = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		// 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
		boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		// 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
		boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		if (gps || network) {
			return true;
		}
		return false;
	}

	/**
	 * 日期转星期
	 *
	 * @param datetime
	 * @return
	 */
	public static String dateToWeek(String datetime) {
		SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
		String[] weekDays = { "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六" };
		Calendar cal = Calendar.getInstance(); // 获得一个日历
		try {
			Date date = f.parse(datetime);
			cal.setTime(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		int w = cal.get(Calendar.DAY_OF_WEEK) - 1; // 指示一个星期中的某天。
		if (w < 0)
			w = 0;
		return weekDays[w];
	}

	/**
	 * 获取listview高度
	 * @param listView
	 */
	public static int getListViewHeightBasedOnChildren(ListView listView) {
		int height = 0;
		ListAdapter listAdapter = listView.getAdapter();
		if (listAdapter == null) {
			return height;
		}

		int totalHeight = 0;
		for (int i = 0; i < listAdapter.getCount(); i++) {
			View listItem = listAdapter.getView(i, null, listView);
			listItem.measure(0, 0);
			totalHeight += listItem.getMeasuredHeight();
		}
		height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
		return height;
	}

	/**
	 * 跳转至wps-office
	 * @param filePath
	 */
	public static void intentWPSOffice(Context context, String filePath) {
		if (context == null || TextUtils.isEmpty(filePath)) {
			return;
		}
		Uri uri;
		File file = new File(filePath);
		if (file.exists()) {
			final String authority = context.getPackageName()+".fileprovider";
			boolean build = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
			uri = build ? FileProvider.getUriForFile(context, authority, file) : Uri.fromFile(file);
		}else {
			uri = Uri.parse(filePath);
		}
		Intent intent = context.getPackageManager().getLaunchIntentForPackage("cn.wps.moffice_eng");//WPS个人版的包名
		if (intent == null) {
			context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=cn.wps.moffice_eng")));
			Toast.makeText(context, "可下载WPS Office来打开文件", Toast.LENGTH_SHORT).show();
			return;
		}
		Bundle bundle = new Bundle();
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//添加这一句表示对目标应用临时授权该Uri所代表的文件
		intent.setData(uri);//这里采用传入文档的在线地址进行打开，免除下载的步骤，也不需要判断安卓版本号
		intent.putExtras(bundle);
		context.startActivity(intent);
	}

	/**
	 * 根据风速获取风向标
	 * @param context
	 * @param speed
	 * @return
	 */
	public static Bitmap getStrongWindMarker(Context context, double speed) {
		Bitmap bitmap = null;
		if (speed > 17.1 && speed <= 20.7) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.fzj_wind_78);
		}else if (speed > 20.7 && speed <= 24.4) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.fzj_wind_89);
		}else if (speed > 24.4 && speed <= 28.4) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.fzj_wind_910);
		}else if (speed > 28.4 && speed <= 32.6) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.fzj_wind_1011);
		}else if (speed > 32.6 && speed < 99999.0) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.fzj_wind_1112);
		}
		return bitmap;
	}

	/**
	 * 提交点击次数
	 */
	public static void submitClickCount(final String columnId, final String name) {
		if (TextUtils.isEmpty(columnId) || TextUtils.isEmpty(name)) {
			return;
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH", Locale.CHINA);
		String addtime = sdf.format(new Date());
		final String clickUrl = String.format("http://decision-admin.tianqi.cn/Home/Count/clickCount?addtime=%s&appid=%s&eventid=menuClick_%s&eventname=%s&userid=%s&username=%s",
				addtime, CONST.APPID, columnId, name, CONST.UID, CONST.USERNAME);
		new Thread(new Runnable() {
			@Override
			public void run() {
				OkHttpUtil.enqueue(new Request.Builder().url(clickUrl).build(), new Callback() {
					@Override
					public void onFailure(Call call, IOException e) {
					}
					@Override
					public void onResponse(Call call, Response response) throws IOException {
					}
				});
			}
		}).start();
	}

	/**
	 * 图片二值化处理
	 * @param graymap
	 * @return
	 */
	public static Bitmap gray2Binary(Bitmap graymap, int color) {
		//得到图形的宽度和长度
		int width = graymap.getWidth();
		int height = graymap.getHeight();
		//创建二值化图像
		Bitmap binarymap = null;
		binarymap = graymap.copy(Config.ARGB_8888, true);
		//依次循环，对图像的像素进行处理
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				//得到当前像素的值
				int col = binarymap.getPixel(i, j);
				//得到alpha通道的值
				int alpha = col & 0xFF000000;
				//得到图像的像素RGB的值
				int red = (col & 0x00FF0000) >> 16;
				int green = (col & 0x0000FF00) >> 8;
				int blue = (col & 0x000000FF);
				// 用公式X = 0.3×R+0.59×G+0.11×B计算出X代替原来的RGB
//				int gray = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
				int gray = color;
				//对图像进行二值化处理
				if (gray <= 95) {
					gray = 0;
				} else {
					gray = 255;
				}
				// 新的ARGB
				int newColor = alpha | (gray << 16) | (gray << 8) | gray;
				//设置新图像的当前像素值
				binarymap.setPixel(i, j, newColor);
			}
		}
		return binarymap;
	}

	public static Bitmap grayScaleImage(Bitmap bm) {
//		int width = bitmap.getWidth();
//		int height = bitmap.getHeight();
//		Bitmap grayImg = null;
//		try {
//			float a = 1;
//			float b = 1;
//			float c = 2;
//			float lum = 0;
//			ColorMatrix colorMatrix = new ColorMatrix();
//			colorMatrix.setSaturation(0);
////			colorMatrix.set(new float[]
////							{a, b, c, 0, lum,
////							a, b, c, 0, lum,
////							a, b, c, 0, lum,
////							0, 0, 0, 1, 0});
//
//			grayImg = Bitmap.createBitmap(width, height, Config.ARGB_8888);
//			Canvas canvas = new Canvas(grayImg);
//			Paint paint = new Paint();
//			ColorMatrixColorFilter colorMatrixFilter = new ColorMatrixColorFilter(colorMatrix);
//			paint.setColorFilter(colorMatrixFilter);
//			canvas.drawBitmap(bitmap, 0, 0, paint);
//		}catch (Exception e) {
//			e.printStackTrace();
//		}
//		return grayImg;


		Bitmap bitmap = null;
		// 获取图片的宽和高
		int width = bm.getWidth();
		int height = bm.getHeight();
		// 创建二值化图像
		bitmap = bm.copy(Config.ARGB_8888, true);
		// 遍历原始图像像素,并进行二值化处理
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				// 得到当前的像素值
				int pixel = bitmap.getPixel(i, j);
				// 得到Alpha通道的值
				int alpha = pixel & 0xFF000000;
				// 得到Red的值
				int red = (pixel & 0x00FF0000) >> 16;
				// 得到Green的值
				int green = (pixel & 0x0000FF00) >> 8;
				// 得到Blue的值
				int blue = pixel & 0x000000FF;
				// 通过加权平均算法,计算出最佳像素值
				int gray = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
				// 对图像设置黑白图
				if (gray <= 95) {
					gray = 0;
				} else {
					gray = 255;
				}
				// 得到新的像素值
				int newPiexl = alpha | (gray << 16) | (gray << 8) | gray;
				// 赋予新图像的像素
				bitmap.setPixel(i, j, newPiexl);
			}
		}
		return bitmap;
	}

	/**
	 * 获取当前网络连接的类型信息
	 * 没有网络0：WIFI网络1：3G网络2：2G网络3
	 * @param context
	 * @return
	 */
	public static int getConnectedType(Context context) {
		if (context != null) {
			ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
			if (mNetworkInfo != null && mNetworkInfo.isAvailable()) {
				return mNetworkInfo.getType();
			}
		}
		return -1;
	}

	/**
	 * 根据aqi值获取aqi的描述（优、良等）
	 * @param value
	 * @return
	 */
	public static String getAqiDes(Context context, int value) {
		String aqi;
		if (value <= 50) {
			aqi = context.getString(R.string.aqi_level1);
		}else if (value <= 100) {
			aqi = context.getString(R.string.aqi_level2);
		}else if (value <= 150) {
			aqi = context.getString(R.string.aqi_level3);
		}else if (value <= 200) {
			aqi = context.getString(R.string.aqi_level4);
		}else if (value <= 300) {
			aqi = context.getString(R.string.aqi_level5);
		}else {
			aqi = context.getString(R.string.aqi_level6);
		}
		return aqi;
	}

	/**
	 * 根据aqi数据获取相对应的背景图标
	 * @param value
	 * @return
	 */
	public static int getCornerBackground(int value) {
		int drawable;
		if (value <= 50) {
			drawable = R.drawable.corner_aqi_one;
		}else if (value <= 100) {
			drawable = R.drawable.corner_aqi_two;
		}else if (value <= 150) {
			drawable = R.drawable.corner_aqi_three;
		}else if (value <= 200) {
			drawable = R.drawable.corner_aqi_four;
		}else if (value <= 300) {
			drawable = R.drawable.corner_aqi_five;
		}else {
			drawable = R.drawable.corner_aqi_six;
		}
		return drawable;
	}

	/**
	 * 获取随机颜色
	 * @return
	 */
	public static String randomColor() {
		Random rnd = new Random();
		String r = Integer.toHexString(rnd.nextInt(256));
		if (r.length() <= 1) {
			r = "0"+r;
		}
		String g = Integer.toHexString(rnd.nextInt(256));
		if (g.length() <= 1) {
			g = "0"+g;
		}
		String b = Integer.toHexString(rnd.nextInt(256));
		if (b.length() <= 1) {
			b = "0"+b;
		}
		Log.e("colorcolor", "#"+r+g+b);
		return "#"+r+g+b;
	}

	public static void topToBottom(View view) {
		startAnimation(false, view);
		view.setVisibility(View.VISIBLE);
	}

	public static void bottomToTop(View view) {
		startAnimation(true, view);
		view.setVisibility(View.GONE);
	}

	/**
	 * @param flag false为显示map，true为显示list
	 */
	private static void startAnimation(boolean flag, final View view) {
		AnimationSet animationSet = new AnimationSet(true);
		TranslateAnimation animation;
		if (!flag) {
			animation = new TranslateAnimation(
					Animation.RELATIVE_TO_SELF,0f,
					Animation.RELATIVE_TO_SELF,0f,
					Animation.RELATIVE_TO_SELF,-1.0f,
					Animation.RELATIVE_TO_SELF,0f);
		}else {
			animation = new TranslateAnimation(
					Animation.RELATIVE_TO_SELF,0f,
					Animation.RELATIVE_TO_SELF,0f,
					Animation.RELATIVE_TO_SELF,0f,
					Animation.RELATIVE_TO_SELF,-1.0f);
		}
		animation.setDuration(400);
		animationSet.addAnimation(animation);
		animationSet.setFillAfter(true);
		view.startAnimation(animationSet);
		animationSet.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation arg0) {
			}
			@Override
			public void onAnimationRepeat(Animation arg0) {
			}
			@Override
			public void onAnimationEnd(Animation arg0) {
				view.clearAnimation();
			}
		});
	}

	/**
	 * 截取webView快照(webView加载的整个内容的大小)
	 * @param webView
	 * @return
	 */
	public static Bitmap captureWebView(WebView webView){
		Picture snapShot = webView.capturePicture();
		Bitmap bitmap = Bitmap.createBitmap(snapShot.getWidth(),snapShot.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		snapShot.draw(canvas);
		clearCanvas(canvas);
		return bitmap;
	}

	/**
	 * 截取scrollView
	 * @param scrollView
	 * @return
	 */
	public static Bitmap captureScrollView(ScrollView scrollView) {
		int height = 0;
		// 获取scrollview实际高度
		for (int i = 0; i < scrollView.getChildCount(); i++) {
			height += scrollView.getChildAt(i).getHeight();
			scrollView.getChildAt(i).setBackgroundColor(0xffffffff);
		}
		// 创建对应大小的bitmap
		Bitmap bitmap = Bitmap.createBitmap(scrollView.getWidth(), height, Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		scrollView.draw(canvas);
		clearCanvas(canvas);
		return bitmap;
	}

	/**
	 * 截取listview
	 * @param listView
	 * @return
	 */
	public static Bitmap captureListView(ListView listView){
		ListAdapter listAdapter = listView.getAdapter();
		int count = listAdapter.getCount();
		if (count > 30) {
			count = 30;
		}
		List<View> childViews = new ArrayList<>(count);
		int totalHeight = 0;
		for(int i = 0; i < count; i++){
			View itemView = listAdapter.getView(i, null, listView);
			itemView.measure(0, 0);
			childViews.add(itemView);
			totalHeight += itemView.getMeasuredHeight();
		}
		Bitmap bitmap = Bitmap.createBitmap(listView.getWidth(), totalHeight, Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);

		int yPos = 0;
		//把每个ItemView生成图片，并画到背景画布上
		for(int j = 0; j < childViews.size(); j++){
			View itemView = childViews.get(j);
			int childHeight = itemView.getMeasuredHeight();
			itemView.layout(0, 0, listView.getWidth(), childHeight);
			itemView.buildDrawingCache();
			Bitmap itemBitmap = itemView.getDrawingCache();
			if(itemBitmap!=null){
				canvas.drawBitmap(itemBitmap, 0, yPos, null);
			}
			yPos = childHeight +yPos;
		}
		canvas.save();
		canvas.restore();
		clearCanvas(canvas);
		return bitmap;
	}

	/**
	 * 截屏自定义view
	 * @param view
	 * @return
	 */
	public static Bitmap captureMyView(View view) {
		if (view == null) {
			return null;
		}
		int width = view.getWidth();
		int height = view.getHeight();
		if (width <= 0 || height <= 0) {
			return null;
		}
		Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		view.draw(canvas);
		view.setDrawingCacheEnabled(true);
		view.buildDrawingCache();
		bitmap = view.getDrawingCache();
		clearCanvas(canvas);
		return bitmap;
	}

	/**
	 * 截屏,可是区域
	 * @return
	 */
	public static Bitmap captureView(View view) {
		if (view == null) {
			return null;
		}
		int width = view.getWidth();
		int height = view.getHeight();
		if (width <= 0 || height <= 0) {
			return null;
		}
		Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		view.draw(canvas);
		clearCanvas(canvas);
		return bitmap;
	}

	/**
	 * 合成图片
	 * @param bitmap1
	 * @param bitmap2
	 * @param isCover 判断是否为覆盖合成
	 * @return
	 */
	public static Bitmap mergeBitmap(Context context, Bitmap bitmap1, Bitmap bitmap2, boolean isCover) {
		if (bitmap1 == null || bitmap2 == null) {
			return null;
		}

		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		int width = wm.getDefaultDisplay().getWidth();
		bitmap1 = Bitmap.createScaledBitmap(bitmap1, width, width*bitmap1.getHeight()/bitmap1.getWidth(), true);
		bitmap2 = Bitmap.createScaledBitmap(bitmap2, width, width*bitmap2.getHeight()/bitmap2.getWidth(), true);

		Bitmap bitmap;
		Canvas canvas;
		if (isCover) {
			int height;
			if (bitmap1.getHeight() > bitmap2.getHeight()) {
				height = bitmap1.getHeight();
			}else {
				height = bitmap2.getHeight();
			}
			bitmap = Bitmap.createBitmap(bitmap1.getWidth(), height, Config.ARGB_8888);
			canvas = new Canvas(bitmap);
			canvas.drawBitmap(bitmap1, 0, 0 , null);
			canvas.drawBitmap(bitmap2, 0, 0, null);
		}else {
			bitmap = Bitmap.createBitmap(bitmap1.getWidth(), bitmap1.getHeight()+bitmap2.getHeight(), Config.ARGB_8888);
			canvas = new Canvas(bitmap);
			canvas.drawBitmap(bitmap1, 0, 0 , null);
			canvas.drawBitmap(bitmap2, 0, bitmap1.getHeight(), null);
		}
		clearCanvas(canvas);
		return bitmap;
	}

	public static void clearBitmap(Bitmap bitmap) {
		if (bitmap != null) {
//			if (!bitmap.isRecycled()) {
//				bitmap.recycle();
//			}
			bitmap = null;
			System.gc();
		}
	}

	public static void clearCanvas(Canvas canvas) {
		if (canvas != null) {
			canvas = null;
		}
	}

	/**
	 * 分享功能
	 * @param activity
	 */
	public static void share(final Activity activity, final Bitmap bitmap) {
		ShareAction panelAction = new ShareAction(activity);
		panelAction.setDisplayList(SHARE_MEDIA.WEIXIN,SHARE_MEDIA.WEIXIN_CIRCLE,SHARE_MEDIA.EMAIL,SHARE_MEDIA.SMS);
		panelAction.setShareboardclickCallback(new ShareBoardlistener() {
			@Override
			public void onclick(SnsPlatform arg0, SHARE_MEDIA arg1) {
				ShareAction shareAction = new ShareAction(activity);
				shareAction.setPlatform(arg1);
				if (bitmap != null) {
					shareAction.withMedia(new UMImage(activity, bitmap));
				}
				shareAction.share();
			}
		});
		panelAction.open();
	}

	/**
	 * 分享功能
	 * @param activity
	 */
	public static void share(final Activity activity, final String title, final String content, final String imgUrl, final String url) {
		ShareAction panelAction = new ShareAction(activity);
		panelAction.setDisplayList(SHARE_MEDIA.WEIXIN,SHARE_MEDIA.WEIXIN_CIRCLE,SHARE_MEDIA.EMAIL,SHARE_MEDIA.SMS);
		panelAction.setShareboardclickCallback(new ShareBoardlistener() {
			@Override
			public void onclick(SnsPlatform arg0, SHARE_MEDIA arg1) {
				ShareAction sAction = new ShareAction(activity);
				sAction.setPlatform(arg1);
				UMWeb web = new UMWeb(url);
				web.setTitle(title);//标题
				if (!TextUtils.isEmpty(imgUrl)) {
					web.setThumb(new UMImage(activity, imgUrl));  //缩略图
				}else {
					web.setThumb(new UMImage(activity, R.drawable.iv_icon));
				}
				web.setDescription(content);
				sAction.withMedia(web);
				sAction.share();
			}
		});
		panelAction.open();
	}

}
