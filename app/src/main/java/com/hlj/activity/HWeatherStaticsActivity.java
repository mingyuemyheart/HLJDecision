package com.hlj.activity;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.OnMapClickListener;
import com.amap.api.maps.AMap.OnMarkerClickListener;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.animation.ScaleAnimation;
import com.hlj.common.CONST;
import com.hlj.dto.WeatherStaticsDto;
import com.hlj.manager.RainManager;
import com.hlj.utils.CommonUtil;
import com.hlj.utils.OkHttpUtil;
import com.hlj.view.CircularProgressBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import cn.com.weather.api.WeatherAPI;
import cn.com.weather.listener.AsyncResponseHandler;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import shawn.cxwl.com.hlj.R;

/**
 * 天气统计
 */
public class HWeatherStaticsActivity extends BaseActivity implements OnClickListener, OnMarkerClickListener,
        OnMapClickListener, AMap.OnCameraChangeListener, AMapLocationListener {
	
	private Context mContext = null;
	private LinearLayout llBack = null;
	private TextView tvTitle = null;
	private MapView mMapView = null;
	private AMap aMap = null;
	private List<WeatherStaticsDto> provinceList = new ArrayList<>();//省级
	private List<WeatherStaticsDto> cityList = new ArrayList<>();//省级
	private List<WeatherStaticsDto> districtList = new ArrayList<>();//省级
	private CircularProgressBar mCircularProgressBar1 = null;
	private CircularProgressBar mCircularProgressBar2 = null;
	private CircularProgressBar mCircularProgressBar3 = null;
	private CircularProgressBar mCircularProgressBar4 = null;
	private CircularProgressBar mCircularProgressBar5 = null;
	private TextView tvName = null;
	private TextView tvBar1 = null;
	private TextView tvBar2 = null;
	private TextView tvBar3 = null;
	private TextView tvBar4 = null;
	private TextView tvBar5 = null;
	private TextView tvDetail = null;
	private ConstraintLayout clDetail = null;
	private ProgressBar progressBar = null;
	public final static String SANX_DATA_99 = "sanx_data_99";//加密秘钥名称
	public final static String APPID = "f63d329270a44900";//机密需要用到的AppId
	private List<Marker> proMarkers = new ArrayList<>();
	private List<Marker> cityMarkers = new ArrayList<>();
	private List<Marker> disMarkers = new ArrayList<>();
	private LatLng leftlatlng = null;
	private LatLng rightLatlng = null;
	private float zoom = 5.5f;
	private AMapLocationClientOption mLocationOption = null;//声明mLocationOption对象
	private AMapLocationClient mLocationClient = null;//声明AMapLocationClient类对象
	private String locationCityId = "101050101";//默认为哈尔滨

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hactivity_weather_statics);
		mContext = this;
		showDialog();
		initMap(savedInstanceState);
		initWidget();
	}
	
	/**
	 * 初始化控件
	 */
	private void initWidget() {
		llBack = (LinearLayout) findViewById(R.id.llBack);
		llBack.setOnClickListener(this);
		tvTitle = (TextView) findViewById(R.id.tvTitle);
		tvName = (TextView) findViewById(R.id.tvName);
		tvBar1 = (TextView) findViewById(R.id.tvBar1);
		tvBar2 = (TextView) findViewById(R.id.tvBar2);
		tvBar3 = (TextView) findViewById(R.id.tvBar3);
		tvBar4 = (TextView) findViewById(R.id.tvBar4);
		tvBar5 = (TextView) findViewById(R.id.tvBar5);
		tvDetail = (TextView) findViewById(R.id.tvDetail);
		mCircularProgressBar1 = (CircularProgressBar) findViewById(R.id.bar1);
		mCircularProgressBar2 = (CircularProgressBar) findViewById(R.id.bar2);
		mCircularProgressBar3 = (CircularProgressBar) findViewById(R.id.bar3);
		mCircularProgressBar4 = (CircularProgressBar) findViewById(R.id.bar4);
		mCircularProgressBar5 = (CircularProgressBar) findViewById(R.id.bar5);
		clDetail = findViewById(R.id.clDetail);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);

		String title = getIntent().getStringExtra(CONST.ACTIVITY_NAME);
		if (title != null) {
			tvTitle.setText(title);
		}

		startLocation();

		String columnId = getIntent().getStringExtra(CONST.COLUMN_ID);
		CommonUtil.submitClickCount(columnId, title);
	}

	/**
	 * 开始定位
	 */
	private void startLocation() {
		mLocationOption = new AMapLocationClientOption();//初始化定位参数
		mLocationClient = new AMapLocationClient(mContext);//初始化定位
		mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
		mLocationOption.setNeedAddress(true);//设置是否返回地址信息（默认返回地址信息）
		mLocationOption.setOnceLocation(true);//设置是否只定位一次,默认为false
		mLocationOption.setWifiActiveScan(true);//设置是否强制刷新WIFI，默认为强制刷新
		mLocationOption.setMockEnable(false);//设置是否允许模拟位置,默认为false，不允许模拟位置
		mLocationOption.setInterval(2000);//设置定位间隔,单位毫秒,默认为2000ms
		mLocationClient.setLocationOption(mLocationOption);//给定位客户端对象设置定位参数
		mLocationClient.setLocationListener(this);
		mLocationClient.startLocation();//启动定位
	}

	@Override
	public void onLocationChanged(AMapLocation amapLocation) {
		if (amapLocation != null && amapLocation.getErrorCode() == 0) {
			getCityId(amapLocation.getLongitude(), amapLocation.getLatitude());
		}
	}

	private void getCityId(double lng, double lat) {
		WeatherAPI.getGeo(mContext, lng+"", lat+"", new AsyncResponseHandler() {
			@Override
			public void onComplete(JSONObject content) {
				super.onComplete(content);
				if (!content.isNull("geo")) {
					try {
						JSONObject geo = content.getJSONObject("geo");
						if (!geo.isNull("id")) {
							locationCityId = geo.getString("id");
							if (!locationCityId.startsWith("10105")) {
								locationCityId = "101050101";
							}
							OkHttpStatistic();
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}
	
	/**
	 * 初始化地图
	 */
	private void initMap(Bundle bundle) {
		mMapView = (MapView) findViewById(R.id.mapView);
		mMapView.onCreate(bundle);
		if (aMap == null) {
			aMap = mMapView.getMap();
		}
		LatLng guizhouLatLng = new LatLng(46.102915,128.121040);
		aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(guizhouLatLng, zoom));
		aMap.getUiSettings().setZoomControlsEnabled(false);
		aMap.getUiSettings().setRotateGesturesEnabled(false);
		aMap.setOnMarkerClickListener(this);
		aMap.setOnMapClickListener(this);
		aMap.setOnCameraChangeListener(this);

		TextView tvMapNumber = findViewById(R.id.tvMapNumber);
		tvMapNumber.setText(aMap.getMapContentApprovalNumber());

		CommonUtil.drawHLJJson(mContext, aMap);
	}

	@Override
	public void onCameraChange(CameraPosition arg0) {
	}

	@Override
	public void onCameraChangeFinish(CameraPosition arg0) {
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		Point leftPoint = new Point(0, dm.heightPixels);
		Point rightPoint = new Point(dm.widthPixels, 0);
		leftlatlng = aMap.getProjection().fromScreenLocation(leftPoint);
		rightLatlng = aMap.getProjection().fromScreenLocation(rightPoint);

		if (zoom == arg0.zoom) {//如果是地图缩放级别不变，并且点击就不做处理
			return;
		}

		zoom = arg0.zoom;
		removeMarkers();
		if (arg0.zoom <= 7.0f) {
			addMarker(provinceList, proMarkers);
			addMarker(cityList, cityMarkers);
		}if (arg0.zoom > 7.0f) {
			addMarker(provinceList, proMarkers);
			addMarker(cityList, cityMarkers);
			addMarker(districtList, disMarkers);
		}
	}

	/**
	 * 加密请求字符串
	 * @return
	 */
	private String getSecretUrl() {
		String URL = "http://scapi.weather.com.cn/weather/stationinfo";//天气统计地址
		String sysdate = RainManager.getDate(Calendar.getInstance(), "yyyyMMddHHmm");//系统时间
		StringBuffer buffer = new StringBuffer();
		buffer.append(URL);
		buffer.append("?");
		buffer.append("date=").append(sysdate);
		buffer.append("&");
		buffer.append("appid=").append(APPID);
		
		String key = RainManager.getKey(SANX_DATA_99, buffer.toString());
		buffer.delete(buffer.lastIndexOf("&"), buffer.length());
		
		buffer.append("&");
		buffer.append("appid=").append(APPID.substring(0, 6));
		buffer.append("&");
		buffer.append("key=").append(key.substring(0, key.length() - 3));
		String result = buffer.toString();
		return result;
	}
	
	/**
	 * 获取天气统计数据
	 */
	private void OkHttpStatistic() {
		OkHttpUtil.enqueue(new Request.Builder().url(getSecretUrl()).build(), new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {

			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				if (!response.isSuccessful()) {
					return;
				}
				String result = response.body().string();
				if (result != null) {
					parseStationInfo(result, "level1", provinceList);
					parseStationInfo(result, "level2", cityList);
					parseStationInfo(result, "level3", districtList);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							cancelDialog();
							addMarker(provinceList, proMarkers);
							addMarker(cityList, cityMarkers);

							clickMarker(locationCityId);
						}
					});
				}
			}
		});
	}
	
	/**
	 * 解析数据
	 */
	private void parseStationInfo(String result, String level, List<WeatherStaticsDto> list) {
		list.clear();
		try {
			JSONObject obj = new JSONObject(result.toString());
			if (!obj.isNull(level)) {
				JSONArray array = new JSONArray(obj.getString(level));
				for (int i = 0; i < array.length(); i++) {
					WeatherStaticsDto dto = new WeatherStaticsDto();
					JSONObject itemObj = array.getJSONObject(i);
					if (!itemObj.isNull("name")) {
						dto.name = itemObj.getString("name");
					}
					if (!itemObj.isNull("stationid")) {
						dto.stationId = itemObj.getString("stationid");
					}
					if (!itemObj.isNull("level")) {
						dto.level = itemObj.getString("level");
					}
					if (!itemObj.isNull("areaid")) {
						dto.areaId = itemObj.getString("areaid");
					}
					if (!itemObj.isNull("lat")) {
						dto.latitude = itemObj.getString("lat");
					}
					if (!itemObj.isNull("lon")) {
						dto.longitude = itemObj.getString("lon");
					}
					if (!TextUtils.isEmpty(dto.areaId) && dto.areaId.startsWith("10105")) {
						list.add(dto);
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 给marker添加文字
	 * @param name 城市名称
	 * @return
	 */
	private View getTextBitmap(String name) {      
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.layout_marker_statistic, null);
		if (view == null) {
			return null;
		}
		TextView tvName = view.findViewById(R.id.tvName);
		tvName.setText(name);
		return view;
	}
	
	private void markerExpandAnimation(Marker marker) {
		ScaleAnimation animation = new ScaleAnimation(0,1,0,1);
		animation.setInterpolator(new LinearInterpolator());
		animation.setDuration(300);
		marker.setAnimation(animation);
		marker.startAnimation();
	}
	
	private void markerColloseAnimation(Marker marker) {
		ScaleAnimation animation = new ScaleAnimation(1,0,1,0);
		animation.setInterpolator(new LinearInterpolator());
		animation.setDuration(300);
		marker.setAnimation(animation);
		marker.startAnimation();
	}
	
	private void removeMarkers() {
		for (int i = 0; i < proMarkers.size(); i++) {
			Marker marker = proMarkers.get(i);
			markerColloseAnimation(marker);
			marker.remove();
		}
		proMarkers.clear();

		for (int i = 0; i < cityMarkers.size(); i++) {
			Marker marker = cityMarkers.get(i);
			markerColloseAnimation(marker);
			marker.remove();
		}
		cityMarkers.clear();

		for (int i = 0; i < disMarkers.size(); i++) {
			Marker marker = disMarkers.get(i);
			markerColloseAnimation(marker);
			marker.remove();
		}
		disMarkers.clear();
	}
	
	/**
	 * 添加marker
	 */
	private void addMarker(List<WeatherStaticsDto> list, List<Marker> markers) {
		if (list.isEmpty()) {
			return;
		}
		
		for (int i = 0; i < list.size(); i++) {
			WeatherStaticsDto dto = list.get(i);
			double lat = Double.valueOf(dto.latitude);
			double lng = Double.valueOf(dto.longitude);
			if (leftlatlng == null || rightLatlng == null) {
				MarkerOptions options = new MarkerOptions();
				options.title(list.get(i).areaId);
				options.anchor(0.5f, 0.5f);
				options.position(new LatLng(lat, lng));
				options.icon(BitmapDescriptorFactory.fromView(getTextBitmap(list.get(i).name)));
				Marker marker = aMap.addMarker(options);
				markers.add(marker);
				markerExpandAnimation(marker);
			}else {
				if (lat > leftlatlng.latitude && lat < rightLatlng.latitude && lng > leftlatlng.longitude && lng < rightLatlng.longitude) {
					MarkerOptions options = new MarkerOptions();
					options.title(list.get(i).areaId);
					options.anchor(0.5f, 0.5f);
					options.position(new LatLng(lat, lng));
					options.icon(BitmapDescriptorFactory.fromView(getTextBitmap(list.get(i).name)));
					Marker marker = aMap.addMarker(options);
					markers.add(marker);
					markerExpandAnimation(marker);
				}
			}
		}
	}
	
	@Override
	public void onMapClick(LatLng arg0) {
		if (clDetail.getVisibility() == View.VISIBLE) {
			hideAnimation(clDetail);
		}
	}
	
	/**
	 * 向上弹出动画
	 * @param layout
	 */
	private void showAnimation(final View layout) {
		TranslateAnimation animation = new TranslateAnimation(
				TranslateAnimation.RELATIVE_TO_SELF, 0, 
				TranslateAnimation.RELATIVE_TO_SELF, 0, 
				TranslateAnimation.RELATIVE_TO_SELF, 1f, 
				TranslateAnimation.RELATIVE_TO_SELF, 0);
		animation.setDuration(300);
		layout.startAnimation(animation);
		layout.setVisibility(View.VISIBLE);
	}
	
	/**
	 * 向下隐藏动画
	 * @param layout
	 */
	private void hideAnimation(final View layout) {
		TranslateAnimation animation = new TranslateAnimation(
				TranslateAnimation.RELATIVE_TO_SELF, 0, 
				TranslateAnimation.RELATIVE_TO_SELF, 0, 
				TranslateAnimation.RELATIVE_TO_SELF, 0, 
				TranslateAnimation.RELATIVE_TO_SELF, 1f);
		animation.setDuration(300);
		layout.startAnimation(animation);
		layout.setVisibility(View.GONE);
	}
	
	@Override
	public boolean onMarkerClick(Marker marker) {
		clickMarker(marker.getTitle());
		return true;
	}

	private void clickMarker(String cityId) {
		showAnimation(clDetail);
		String name = null;
		String areaId = null;
		String stationId = null;

		for (int i = 0; i < provinceList.size(); i++) {
			if (TextUtils.equals(cityId, provinceList.get(i).areaId)) {
				areaId = provinceList.get(i).areaId;
				stationId = provinceList.get(i).stationId;
				name = provinceList.get(i).name;
				break;
			}
		}
		for (int i = 0; i < cityList.size(); i++) {
			if (TextUtils.equals(cityId, cityList.get(i).areaId)) {
				areaId = cityList.get(i).areaId;
				stationId = cityList.get(i).stationId;
				name = cityList.get(i).name;
				break;
			}
		}
		for (int i = 0; i < districtList.size(); i++) {
			if (TextUtils.equals(cityId, districtList.get(i).areaId)) {
				areaId = districtList.get(i).areaId;
				stationId = districtList.get(i).stationId;
				name = districtList.get(i).name;
				break;
			}
		}

		tvName.setText(name + " " + stationId);
		tvDetail.setText("");
		progressBar.setVisibility(View.VISIBLE);

		OkHttpStatisticDetail(getSecretUrl2(stationId, areaId));
	}

	/**
	 * 加密请求字符串
	 * @return
	 */
	private String getSecretUrl2(String stationid, String areaid) {
		String URL = "http://scapi.weather.com.cn/weather/historycount";
		String sysdate = RainManager.getDate(Calendar.getInstance(), "yyyyMMddHHmm");//系统时间
		StringBuffer buffer = new StringBuffer();
		buffer.append(URL);
		buffer.append("?");
		buffer.append("stationid=").append(stationid);
		buffer.append("&");
		buffer.append("areaid=").append(areaid);
		buffer.append("&");
		buffer.append("date=").append(sysdate);
		buffer.append("&");
		buffer.append("appid=").append(APPID);

		String key = RainManager.getKey(SANX_DATA_99, buffer.toString());
		buffer.delete(buffer.lastIndexOf("&"), buffer.length());

		buffer.append("&");
		buffer.append("appid=").append(APPID.substring(0, 6));
		buffer.append("&");
		buffer.append("key=").append(key.substring(0, key.length() - 3));
		String result = buffer.toString();
		return result;
	}

	private void OkHttpStatisticDetail(String url) {
		OkHttpUtil.enqueue(new Request.Builder().url(url).build(), new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {

			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {

				if (!response.isSuccessful()) {
					return;
				}
				final String result = response.body().string();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						progressBar.setVisibility(View.INVISIBLE);
						if (result != null) {
							try {
								JSONObject obj = new JSONObject(result.toString());
								SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
								SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
								try {
									String startTime = sdf2.format(sdf.parse(obj.getString("starttime")));
									String endTime = sdf2.format(sdf.parse(obj.getString("endtime")));
									String no_rain_lx = obj.getInt("no_rain_lx")+"";//连续没雨天数
									if (TextUtils.equals(no_rain_lx, "-1")) {
										no_rain_lx = getString(R.string.no_statics);
									}else {
										no_rain_lx = no_rain_lx+"天";
									}
									String mai_lx = obj.getInt("mai_lx")+"";//连续霾天数
									if (TextUtils.equals(mai_lx, "-1")) {
										mai_lx = getString(R.string.no_statics);
									}else {
										mai_lx = mai_lx+"天";
									}
									String highTemp = null;//高温
									String lowTemp = null;//低温
									String highWind = null;//最大风速
									String highRain = null;//最大降水量

									if (!obj.isNull("count")) {
										JSONArray array = new JSONArray(obj.getString("count"));
										JSONObject itemObj0 = array.getJSONObject(0);//温度
										JSONObject itemObj1 = array.getJSONObject(1);//降水
										JSONObject itemObj5 = array.getJSONObject(5);//风速

										if (!itemObj0.isNull("max") && !itemObj0.isNull("min")) {
											highTemp = itemObj0.getString("max");
											if (TextUtils.equals(highTemp, "-1.0")) {
												highTemp = getString(R.string.no_statics);
											}else {
												highTemp = highTemp+"℃";
											}
											lowTemp = itemObj0.getString("min");
											if (TextUtils.equals(lowTemp, "-1.0")) {
												lowTemp = getString(R.string.no_statics);
											}else {
												lowTemp = lowTemp+"℃";
											}
										}
										if (!itemObj1.isNull("max")) {
											highRain = itemObj1.getString("max");
											if (TextUtils.equals(highRain, "-1.0")) {
												highRain = getString(R.string.no_statics);
											}else {
												highRain = highRain+"mm";
											}
										}
										if (!itemObj5.isNull("max")) {
											highWind = itemObj5.getString("max");
											if (TextUtils.equals(highWind, "-1.0")) {
												highWind = getString(R.string.no_statics);
											}else {
												highWind = highWind+"m/s";
											}
										}
									}

									if (startTime != null && endTime != null && highTemp != null && lowTemp != null && highWind != null && highRain != null) {
										StringBuffer buffer = new StringBuffer();
										buffer.append(getString(R.string.from)).append(startTime);
										buffer.append(getString(R.string.to)).append(endTime);
										buffer.append("：\n");
										buffer.append(getString(R.string.highest_temp)).append(highTemp).append("，");
										buffer.append(getString(R.string.lowest_temp)).append(lowTemp).append("，");
										buffer.append(getString(R.string.max_speed)).append(highWind).append("，");
										buffer.append(getString(R.string.max_fall)).append(highRain).append("，");
										buffer.append(getString(R.string.lx_no_fall)).append(no_rain_lx).append("，");
										buffer.append(getString(R.string.lx_no_mai)).append(mai_lx).append("。");

										SpannableStringBuilder builder = new SpannableStringBuilder(buffer.toString());
										ForegroundColorSpan builderSpan1 = new ForegroundColorSpan(getResources().getColor(R.color.builder));
										ForegroundColorSpan builderSpan2 = new ForegroundColorSpan(getResources().getColor(R.color.builder));
										ForegroundColorSpan builderSpan3 = new ForegroundColorSpan(getResources().getColor(R.color.builder));
										ForegroundColorSpan builderSpan4 = new ForegroundColorSpan(getResources().getColor(R.color.builder));
										ForegroundColorSpan builderSpan5 = new ForegroundColorSpan(getResources().getColor(R.color.builder));
										ForegroundColorSpan builderSpan6 = new ForegroundColorSpan(getResources().getColor(R.color.builder));

										builder.setSpan(builderSpan1, 29, 29+highTemp.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
										builder.setSpan(builderSpan2, 29+highTemp.length()+6, 29+highTemp.length()+6+lowTemp.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
										builder.setSpan(builderSpan3, 29+highTemp.length()+6+lowTemp.length()+6, 29+highTemp.length()+6+lowTemp.length()+6+highWind.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
										builder.setSpan(builderSpan4, 29+highTemp.length()+6+lowTemp.length()+6+highWind.length()+7, 29+highTemp.length()+6+lowTemp.length()+6+highWind.length()+7+highRain.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
										builder.setSpan(builderSpan5, 29+highTemp.length()+6+lowTemp.length()+6+highWind.length()+7+highRain.length()+8, 29+highTemp.length()+6+lowTemp.length()+6+highWind.length()+7+highRain.length()+8+no_rain_lx.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
										builder.setSpan(builderSpan6, 29+highTemp.length()+6+lowTemp.length()+6+highWind.length()+7+highRain.length()+8+no_rain_lx.length()+6, 29+highTemp.length()+6+lowTemp.length()+6+highWind.length()+7+highRain.length()+8+no_rain_lx.length()+6+mai_lx.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
										tvDetail.setText(builder);

										long start = sdf2.parse(startTime).getTime();
										long end = sdf2.parse(endTime).getTime();
										float dayCount = (float) ((end - start) / (1000*60*60*24)) + 1;
										if (!obj.isNull("tqxxcount")) {
											JSONArray array = new JSONArray(obj.getString("tqxxcount"));
											for (int i = 0; i < array.length(); i++) {
												JSONObject itemObj = array.getJSONObject(i);
												String name = itemObj.getString("name");
												int value = itemObj.getInt("value");

												if (i == 0) {
													if (value == -1) {
														tvBar1.setText(name + "\n" + "--");
														animate(mCircularProgressBar1, null, 0, 1000);
														mCircularProgressBar1.setProgress(0);
													}else {
														tvBar1.setText(name + "\n" + value + "天");
														animate(mCircularProgressBar1, null, -value/dayCount, 1000);
														mCircularProgressBar1.setProgress(-value/dayCount);
													}
												}else if (i == 1) {
													if (value == -1) {
														tvBar2.setText(name + "\n" + "--");
														animate(mCircularProgressBar2, null, 0, 1000);
														mCircularProgressBar2.setProgress(0);
													}else {
														tvBar2.setText(name + "\n" + value + "天");
														animate(mCircularProgressBar2, null, -value/dayCount, 1000);
														mCircularProgressBar2.setProgress(-value/dayCount);
													}
												}else if (i == 2) {
													if (value == -1) {
														tvBar3.setText(name + "\n" + "--");
														animate(mCircularProgressBar3, null, 0, 1000);
														mCircularProgressBar3.setProgress(0);
													}else {
														tvBar3.setText(name + "\n" + value + "天");
														animate(mCircularProgressBar3, null, -value/dayCount, 1000);
														mCircularProgressBar3.setProgress(-value/dayCount);
													}
												}else if (i == 3) {
													if (value == -1) {
														tvBar4.setText(name + "\n" + "--");
														animate(mCircularProgressBar4, null, 0, 1000);
														mCircularProgressBar4.setProgress(0);
													}else {
														tvBar4.setText(name + "\n" + value + "天");
														animate(mCircularProgressBar4, null, -value/dayCount, 1000);
														mCircularProgressBar4.setProgress(-value/dayCount);
													}
												}else if (i == 4) {
													if (value == -1) {
														tvBar5.setText(name + "\n" + "--");
														animate(mCircularProgressBar5, null, 0, 1000);
														mCircularProgressBar5.setProgress(0);
													}else {
														tvBar5.setText(name + "\n" + value + "天");
														animate(mCircularProgressBar5, null, -value/dayCount, 1000);
														mCircularProgressBar5.setProgress(-value/dayCount);
													}
												}
											}
										}

									}
								} catch (ParseException e) {
									e.printStackTrace();
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}
				});
			}
		});
	}
	
	/**
	 * 进度条动画
	 * @param progressBar
	 * @param listener
	 * @param progress
	 * @param duration
	 */
	private void animate(final CircularProgressBar progressBar, final AnimatorListener listener,final float progress, final int duration) {
		ObjectAnimator mProgressBarAnimator = ObjectAnimator.ofFloat(progressBar, "progress", progress);
		mProgressBarAnimator.setDuration(duration);
		mProgressBarAnimator.addListener(new AnimatorListener() {
			@Override
			public void onAnimationCancel(final Animator animation) {
			}

			@Override
			public void onAnimationEnd(final Animator animation) {
				progressBar.setProgress(progress);
			}

			@Override
			public void onAnimationRepeat(final Animator animation) {
			}

			@Override
			public void onAnimationStart(final Animator animation) {
			}
		});
		if (listener != null) {
			mProgressBarAnimator.addListener(listener);
		}
		mProgressBarAnimator.reverse();
		mProgressBarAnimator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(final ValueAnimator animation) {
				progressBar.setProgress((Float) animation.getAnimatedValue());
			}
		});
//		progressBar.setMarkerProgress(0f);
		mProgressBarAnimator.start();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (clDetail.getVisibility() == View.VISIBLE) {
				hideAnimation(clDetail);
				return false;
			} else {
				finish();
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.llBack:
			if (clDetail.getVisibility() == View.VISIBLE) {
				hideAnimation(clDetail);
			} else {
				finish();
			}
			break;

		default:
			break;
		}
	}
	
	/**
	 * 方法必须重写
	 */
	@Override
	protected void onResume() {
		super.onResume();
		if (mMapView != null) {
			mMapView.onResume();
		}
	}

	/**
	 * 方法必须重写
	 */
	@Override
	protected void onPause() {
		super.onPause();
		if (mMapView != null) {
			mMapView.onPause();
		}
	}

	/**
	 * 方法必须重写
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mMapView != null) {
			mMapView.onSaveInstanceState(outState);
		}
	}

	/**
	 * 方法必须重写
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mMapView != null) {
			mMapView.onDestroy();
		}
	}

}
