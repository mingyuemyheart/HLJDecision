package com.hlj.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.OnCameraChangeListener;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.GroundOverlay;
import com.amap.api.maps.model.GroundOverlayOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.hlj.common.CONST;
import com.hlj.dto.MinuteFallDto;
import com.hlj.dto.StreamFactDto;
import com.hlj.manager.CaiyunManager;
import com.hlj.utils.CommonUtil;
import com.hlj.utils.OkHttpUtil;
import com.hlj.utils.WeatherUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import shawn.cxwl.com.hlj.R;

/**
 * 强对流天气实况（新）
 * @author shawn_sun
 */
public class StreamFactActivity extends BaseActivity implements OnClickListener, AMapLocationListener, OnCameraChangeListener,
		AMap.OnMarkerClickListener, AMap.OnMapClickListener, AMap.InfoWindowAdapter {
	
	private Context mContext;
	private TextView tvTitle;
	private TextView tvLighting, tvRain, tvWind, tvHail;
	private ImageView ivLighting,ivRain,ivWind,ivHail,ivRadar,ivLocation,ivLegend;
	private MapView mMapView;
	private AMap aMap;
	private float zoom = 3.7f;
	private SimpleDateFormat sdf1 = new SimpleDateFormat("HH", Locale.CHINA);
	private SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMddHH", Locale.CHINA);
	private SimpleDateFormat sdf3 = new SimpleDateFormat("HH:mm", Locale.CHINA);
	private double locationLat = CONST.defaultLat, locationLng = CONST.defaultLng;
	private List<StreamFactDto> lightingList = new ArrayList<>();//闪电
	private List<StreamFactDto> rainList = new ArrayList<>();//强降水
	private List<StreamFactDto> windList = new ArrayList<>();//大风
	private List<StreamFactDto> hailList = new ArrayList<>();//冰雹
	private List<Marker> lightingMarkers = new ArrayList<>();
	private List<Marker> rainMarkers = new ArrayList<>();
	private List<Marker> windMarkers = new ArrayList<>();
	private List<Marker> hailMarkers = new ArrayList<>();
	private boolean isShowLighting = true, isShowRain = false, isShowWind = false, isShowHail = false;
	private Marker locationMarker,selectMarker;

	//时间轴
	private LinearLayout llContainer,llSeekBar;
	private ImageView ivPlay;
	private SeekBar seekBar;
	private SeekbarThread seekbarThread;
	private int lightingType = 1;//1、2、3、4、5、6分别对应每10分钟，6为最新

	//彩云
	private ArrayList<MinuteFallDto> caiyunList = new ArrayList<>();
	private GroundOverlay radarOverlay;
	private CaiyunManager caiyunManager;
	private CaiyunThread caiyunThread;
	private static final int HANDLER_SHOW_RADAR = 1;
	private static final int HANDLER_PROGRESS = 2;
	private static final int HANDLER_LOAD_FINISHED = 3;
	private static final int HANDLER_PAUSE = 4;
	private boolean isShowOverlay = false;//是否显示彩云overlay

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stream_fact);
		mContext = this;
		showDialog();
		initMap(savedInstanceState);
		initWidget();
	}
	
	/**
	 * 初始化控件
	 */
	private void initWidget() {
		LinearLayout llBack = findViewById(R.id.llBack);
		llBack.setOnClickListener(this);
		tvTitle = findViewById(R.id.tvTitle);
		TextView tvName = findViewById(R.id.tvName);
		ivLighting = findViewById(R.id.ivLighting);
		ivLighting.setOnClickListener(this);
		ivRain = findViewById(R.id.ivRain);
		ivRain.setOnClickListener(this);
		ivWind = findViewById(R.id.ivWind);
		ivWind.setOnClickListener(this);
		ivHail = findViewById(R.id.ivHail);
		ivHail.setOnClickListener(this);
		tvLighting = findViewById(R.id.tvLighting);
		tvLighting.setOnClickListener(this);
		tvRain = findViewById(R.id.tvRain);
		tvRain.setOnClickListener(this);
		tvWind = findViewById(R.id.tvWind);
		tvWind.setOnClickListener(this);
		tvHail = findViewById(R.id.tvHail);
		tvHail.setOnClickListener(this);
		ImageView ivData = findViewById(R.id.ivData);
		ivData.setOnClickListener(this);
		ivRadar = findViewById(R.id.ivRadar);
		ivRadar.setOnClickListener(this);
		ImageView ivLegendPrompt = findViewById(R.id.ivLegendPrompt);
		ivLegendPrompt.setOnClickListener(this);
		ivLegend = findViewById(R.id.ivLegend);
		ivLocation = findViewById(R.id.ivLocation);
		ivLocation.setOnClickListener(this);
		llContainer = findViewById(R.id.llContainer);
		llSeekBar = findViewById(R.id.llSeekBar);
		ivPlay = findViewById(R.id.ivPlay);
		ivPlay.setOnClickListener(this);
		seekBar = findViewById(R.id.seekBar);

		String title = getIntent().getStringExtra(CONST.ACTIVITY_NAME);
		if (title != null) {
			tvTitle.setText(title);
		}
		String end = sdf1.format(new Date())+"时";
		String start = sdf1.format(new Date().getTime()-1000*60*60)+"时";
		tvName.setText("1小时强对流天气实况"+"("+start+"-"+end+")");
		caiyunManager = new CaiyunManager(mContext);

		checkLocationAuthority(new LocationCallback() {
			@Override
			public void grantedLocation(boolean isGranted) {
				if (isGranted) {
					startLocation();
				} else {
					addLocationMarker();
				}
			}
		});
		new Thread(new Runnable() {
			@Override
			public void run() {
				OkHttpList();
				OkHttpCaiyun();
			}
		}).start();
	}

	/**
	 * 开始定位
	 */
	private void startLocation() {
		AMapLocationClientOption mLocationOption = new AMapLocationClientOption();//初始化定位参数
		AMapLocationClient mLocationClient = new AMapLocationClient(mContext);//初始化定位
		mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
		mLocationOption.setNeedAddress(true);//设置是否返回地址信息（默认返回地址信息）
		mLocationOption.setOnceLocation(true);//设置是否只定位一次,默认为false
		mLocationOption.setMockEnable(false);//设置是否允许模拟位置,默认为false，不允许模拟位置
		mLocationOption.setInterval(2000);//设置定位间隔,单位毫秒,默认为2000ms
		mLocationClient.setLocationOption(mLocationOption);//给定位客户端对象设置定位参数
		mLocationClient.setLocationListener(this);
		mLocationClient.startLocation();//启动定位
	}

	@Override
	public void onLocationChanged(AMapLocation amapLocation) {
		if (amapLocation != null && amapLocation.getErrorCode() == AMapLocation.LOCATION_SUCCESS) {
			locationLat = amapLocation.getLatitude();
			locationLng = amapLocation.getLongitude();
			ivLocation.setVisibility(View.VISIBLE);
			addLocationMarker();
		}
	}

	private void addLocationMarker() {
		if (locationMarker != null) {
			locationMarker.remove();
		}
		LatLng latLng = new LatLng(locationLat, locationLng);
		MarkerOptions options = new MarkerOptions();
		options.anchor(0.5f, 0.5f);
		Bitmap bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(getResources(), R.drawable.icon_map_location),
				(int) (CommonUtil.dip2px(mContext, 16)), (int) (CommonUtil.dip2px(mContext, 24)));
		if (bitmap != null) {
			options.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
		} else {
			options.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_map_location));
		}
		options.position(latLng);
		locationMarker = aMap.addMarker(options);
		locationMarker.setClickable(false);
	}

	/**
	 * 初始化地图
	 */
	private void initMap(Bundle bundle) {
		mMapView = findViewById(R.id.mapView);
		mMapView.onCreate(bundle);
		if (aMap == null) {
			aMap = mMapView.getMap();
		}
		aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(35.926628, 105.178100), zoom));
		aMap.getUiSettings().setZoomControlsEnabled(false);
		aMap.getUiSettings().setRotateGesturesEnabled(false);
		aMap.setOnCameraChangeListener(this);
		aMap.setOnMarkerClickListener(this);
		aMap.setOnMapClickListener(this);
		aMap.setInfoWindowAdapter(this);
		aMap.setOnMapLoadedListener(new AMap.OnMapLoadedListener() {
			@Override
			public void onMapLoaded() {
				TextView tvMapNumber = findViewById(R.id.tvMapNumber);
				tvMapNumber.setText(aMap.getMapContentApprovalNumber());
				CommonUtil.drawHLJJson(mContext, aMap);
			}
		});
	}

	@Override
	public void onCameraChange(CameraPosition arg0) {
	}

	@Override
	public void onCameraChangeFinish(CameraPosition arg0) {
		zoom = arg0.zoom;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.llBack:
				finish();
				break;
			case R.id.tvLighting:
			case R.id.ivLighting:
				if (!isShowLighting) {
					tvLighting.setBackgroundResource(R.drawable.bg_map_btn_press);
					tvLighting.setTextColor(getResources().getColor(R.color.white));
					ivLighting.setImageResource(R.drawable.icon_lighting_on);
					addLightingMarkers();
					llSeekBar.setVisibility(View.VISIBLE);
				}else {
					tvLighting.setBackgroundResource(R.drawable.bg_map_btn);
					tvLighting.setTextColor(getResources().getColor(R.color.text_color4));
					ivLighting.setImageResource(R.drawable.icon_lighting_off);
					removeLightingMarkers();
					llSeekBar.setVisibility(View.INVISIBLE);
				}
				isShowLighting = !isShowLighting;
				break;
			case R.id.tvRain:
			case R.id.ivRain:
				if (!isShowRain) {
					tvRain.setBackgroundResource(R.drawable.bg_map_btn_press);
					tvRain.setTextColor(getResources().getColor(R.color.white));
					ivRain.setImageResource(R.drawable.fzj_butn_rain);
					addRainMarkers();
				}else {
					tvRain.setBackgroundResource(R.drawable.bg_map_btn);
					tvRain.setTextColor(getResources().getColor(R.color.text_color4));
					ivRain.setImageResource(R.drawable.fzj_butn_rainoff);
					removeRainMarkers();
				}
				isShowRain = !isShowRain;
				break;
			case R.id.tvWind:
			case R.id.ivWind:
				if (!isShowWind) {
					tvWind.setBackgroundResource(R.drawable.bg_map_btn_press);
					tvWind.setTextColor(getResources().getColor(R.color.white));
					ivWind.setImageResource(R.drawable.fzj_butn_wind);
					addWindMarkers();
				}else {
					tvWind.setBackgroundResource(R.drawable.bg_map_btn);
					tvWind.setTextColor(getResources().getColor(R.color.text_color4));
					ivWind.setImageResource(R.drawable.fzj_butn_windoff);
					removeWindMarkers();
				}
				isShowWind = !isShowWind;
				break;
			case R.id.tvHail:
			case R.id.ivHail:
				if (!isShowHail) {
					tvHail.setBackgroundResource(R.drawable.bg_map_btn_press);
					tvHail.setTextColor(getResources().getColor(R.color.white));
					ivHail.setImageResource(R.drawable.fzj_butn_hail);
					addHailMarkers();
				}else {
					tvHail.setBackgroundResource(R.drawable.bg_map_btn);
					tvHail.setTextColor(getResources().getColor(R.color.text_color4));
					ivHail.setImageResource(R.drawable.fzj_butn_hailoff);
					removeHailMarkers();
				}
				isShowHail = !isShowHail;
				break;
			case R.id.ivRadar:
				if (!isShowOverlay) {
					ivRadar.setImageResource(R.drawable.shawn_icon_radar_on);
					if (caiyunThread != null && caiyunThread.getCurrentState() == CaiyunThread.STATE_PLAYING) {
						caiyunThread.pause();
					} else if (caiyunThread != null && caiyunThread.getCurrentState() == CaiyunThread.STATE_PAUSE) {
						caiyunThread.play();
					} else if (caiyunThread == null) {
						if (caiyunThread != null) {
							caiyunThread.cancel();
							caiyunThread = null;
						}
						caiyunThread = new CaiyunThread(caiyunList);
						caiyunThread.start();
					}
				}else {
					ivRadar.setImageResource(R.drawable.shawn_icon_radar_off);
					if (radarOverlay != null) {
						radarOverlay.remove();
						radarOverlay = null;
					}

					if (caiyunThread != null) {
						caiyunThread.cancel();
						caiyunThread = null;
					}
				}
				isShowOverlay = !isShowOverlay;
				break;
			case R.id.ivData:
//				Intent intent = new Intent(mContext, ShawnStreamFactListActivity.class);
//				Bundle bundle = new Bundle();
//				bundle.putParcelableArrayList("lightingList", (ArrayList<? extends Parcelable>)lightingList);
//				bundle.putParcelableArrayList("rainList", (ArrayList<? extends Parcelable>)rainList);
//				bundle.putParcelableArrayList("windList", (ArrayList<? extends Parcelable>)windList);
//				bundle.putParcelableArrayList("hailList", (ArrayList<? extends Parcelable>)hailList);
//				intent.putExtras(bundle);
				startActivity(new Intent(mContext, ShawnStreamFactListActivity.class));
				break;
			case R.id.ivLocation:
				if (zoom >= 12.f) {
					aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(locationLat, locationLng), 3.5f));
				}else {
					aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(locationLat, locationLng), 12.0f));
				}
				break;
			case R.id.ivLegendPrompt:
				if (ivLegend.getVisibility() == View.VISIBLE) {
					ivLegend.setVisibility(View.INVISIBLE);
				}else {
					ivLegend.setVisibility(View.VISIBLE);
				}
				break;
			case R.id.ivPlay:
				if (seekbarThread != null && seekbarThread.getCurrentState() == SeekbarThread.STATE_PLAYING) {
					seekbarThread.pause();
					ivPlay.setImageResource(R.drawable.icon_play);
				} else if (seekbarThread != null && seekbarThread.getCurrentState() == SeekbarThread.STATE_PAUSE) {
					seekbarThread.play();
					ivPlay.setImageResource(R.drawable.icon_pause);
				} else {
					if (seekbarThread != null) {
						seekbarThread.cancel();
						seekbarThread = null;
					}
					if (llContainer.getChildCount() > 0) {
						seekbarThread = new SeekbarThread();
						seekbarThread.start();
						ivPlay.setImageResource(R.drawable.icon_pause);
					}
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
		if (caiyunManager != null) {
			caiyunManager.onDestory();
		}
		if (caiyunThread != null) {
			caiyunThread.cancel();
			caiyunThread = null;
		}
		if (seekbarThread != null) {
			seekbarThread.cancel();
			seekbarThread = null;
		}
	}

	/**
	 * 获取数据
	 */
	private void OkHttpList() {
		final String url = String.format("http://scapi.weather.com.cn/weather/getServerWeather?time=%s&test=ncg", sdf2.format(new Date()));
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
						if (!TextUtils.isEmpty(result)) {
							try {
								JSONObject obj = new JSONObject(result);
								if (!obj.isNull("Lit")) {
									JSONObject object = obj.getJSONObject("Lit");
									lightingList.clear();
									if (!object.isNull("data_1")) {
										JSONArray array = object.getJSONArray("data_1");
										Log.e("length", array.length()+"");
										for (int i = 0; i < array.length(); i++) {
											StreamFactDto dto = new StreamFactDto();
											JSONObject itemObj = array.getJSONObject(i);
											if (!itemObj.isNull("Lat")) {
												dto.lat = itemObj.getDouble("Lat");
											}
											if (!itemObj.isNull("Lon")) {
												dto.lng = itemObj.getDouble("Lon");
											}
											if (!itemObj.isNull("Station_ID_C")) {
												dto.stationId = itemObj.getString("Station_ID_C");
											}
											if (!itemObj.isNull("Station_Name")) {
												dto.stationName = itemObj.getString("Station_Name");
											}
											if (!itemObj.isNull("Lit_Prov")) {
												dto.province = itemObj.getString("Lit_Prov");
											}
											if (!itemObj.isNull("Lit_City")) {
												dto.city = itemObj.getString("Lit_City");
											}
											if (!itemObj.isNull("Lit_Cnty")) {
												dto.dis = itemObj.getString("Lit_Cnty");
											}
											if (!itemObj.isNull("Lit_Current")) {
												dto.lighting = itemObj.getString("Lit_Current");
											}
											dto.lightingType = 1;
											if (!dto.lighting.contains("99999") && !TextUtils.isEmpty(dto.province) && dto.province.startsWith("黑龙江")) {
												lightingList.add(dto);
											}
										}
									}
									if (!object.isNull("data_2")) {
										JSONArray array = object.getJSONArray("data_2");
										Log.e("length", array.length()+"");
										for (int i = 0; i < array.length(); i++) {
											StreamFactDto dto = new StreamFactDto();
											JSONObject itemObj = array.getJSONObject(i);
											if (!itemObj.isNull("Lat")) {
												dto.lat = itemObj.getDouble("Lat");
											}
											if (!itemObj.isNull("Lon")) {
												dto.lng = itemObj.getDouble("Lon");
											}
											if (!itemObj.isNull("Station_ID_C")) {
												dto.stationId = itemObj.getString("Station_ID_C");
											}
											if (!itemObj.isNull("Station_Name")) {
												dto.stationName = itemObj.getString("Station_Name");
											}
											if (!itemObj.isNull("Lit_Prov")) {
												dto.province = itemObj.getString("Lit_Prov");
											}
											if (!itemObj.isNull("Lit_City")) {
												dto.city = itemObj.getString("Lit_City");
											}
											if (!itemObj.isNull("Lit_Cnty")) {
												dto.dis = itemObj.getString("Lit_Cnty");
											}
											if (!itemObj.isNull("Lit_Current")) {
												dto.lighting = itemObj.getString("Lit_Current");
											}
											dto.lightingType = 2;
											if (!dto.lighting.contains("99999") && !TextUtils.isEmpty(dto.province) && dto.province.startsWith("黑龙江")) {
												lightingList.add(dto);
											}
										}
									}
									if (!object.isNull("data_3")) {
										JSONArray array = object.getJSONArray("data_3");
										Log.e("length", array.length()+"");
										for (int i = 0; i < array.length(); i++) {
											StreamFactDto dto = new StreamFactDto();
											JSONObject itemObj = array.getJSONObject(i);
											if (!itemObj.isNull("Lat")) {
												dto.lat = itemObj.getDouble("Lat");
											}
											if (!itemObj.isNull("Lon")) {
												dto.lng = itemObj.getDouble("Lon");
											}
											if (!itemObj.isNull("Station_ID_C")) {
												dto.stationId = itemObj.getString("Station_ID_C");
											}
											if (!itemObj.isNull("Station_Name")) {
												dto.stationName = itemObj.getString("Station_Name");
											}
											if (!itemObj.isNull("Lit_Prov")) {
												dto.province = itemObj.getString("Lit_Prov");
											}
											if (!itemObj.isNull("Lit_City")) {
												dto.city = itemObj.getString("Lit_City");
											}
											if (!itemObj.isNull("Lit_Cnty")) {
												dto.dis = itemObj.getString("Lit_Cnty");
											}
											if (!itemObj.isNull("Lit_Current")) {
												dto.lighting = itemObj.getString("Lit_Current");
											}
											dto.lightingType = 3;
											if (!dto.lighting.contains("99999") && !TextUtils.isEmpty(dto.province) && dto.province.startsWith("黑龙江")) {
												lightingList.add(dto);
											}
										}
									}
									if (!object.isNull("data_4")) {
										JSONArray array = object.getJSONArray("data_4");
										Log.e("length", array.length()+"");
										for (int i = 0; i < array.length(); i++) {
											StreamFactDto dto = new StreamFactDto();
											JSONObject itemObj = array.getJSONObject(i);
											if (!itemObj.isNull("Lat")) {
												dto.lat = itemObj.getDouble("Lat");
											}
											if (!itemObj.isNull("Lon")) {
												dto.lng = itemObj.getDouble("Lon");
											}
											if (!itemObj.isNull("Station_ID_C")) {
												dto.stationId = itemObj.getString("Station_ID_C");
											}
											if (!itemObj.isNull("Station_Name")) {
												dto.stationName = itemObj.getString("Station_Name");
											}
											if (!itemObj.isNull("Lit_Prov")) {
												dto.province = itemObj.getString("Lit_Prov");
											}
											if (!itemObj.isNull("Lit_City")) {
												dto.city = itemObj.getString("Lit_City");
											}
											if (!itemObj.isNull("Lit_Cnty")) {
												dto.dis = itemObj.getString("Lit_Cnty");
											}
											if (!itemObj.isNull("Lit_Current")) {
												dto.lighting = itemObj.getString("Lit_Current");
											}
											dto.lightingType = 4;
											if (!dto.lighting.contains("99999") && !TextUtils.isEmpty(dto.province) && dto.province.startsWith("黑龙江")) {
												lightingList.add(dto);
											}
										}
									}
									if (!object.isNull("data_5")) {
										JSONArray array = object.getJSONArray("data_5");
										Log.e("length", array.length()+"");
										for (int i = 0; i < array.length(); i++) {
											StreamFactDto dto = new StreamFactDto();
											JSONObject itemObj = array.getJSONObject(i);
											if (!itemObj.isNull("Lat")) {
												dto.lat = itemObj.getDouble("Lat");
											}
											if (!itemObj.isNull("Lon")) {
												dto.lng = itemObj.getDouble("Lon");
											}
											if (!itemObj.isNull("Station_ID_C")) {
												dto.stationId = itemObj.getString("Station_ID_C");
											}
											if (!itemObj.isNull("Station_Name")) {
												dto.stationName = itemObj.getString("Station_Name");
											}
											if (!itemObj.isNull("Lit_Prov")) {
												dto.province = itemObj.getString("Lit_Prov");
											}
											if (!itemObj.isNull("Lit_City")) {
												dto.city = itemObj.getString("Lit_City");
											}
											if (!itemObj.isNull("Lit_Cnty")) {
												dto.dis = itemObj.getString("Lit_Cnty");
											}
											if (!itemObj.isNull("Lit_Current")) {
												dto.lighting = itemObj.getString("Lit_Current");
											}
											dto.lightingType = 5;
											if (!dto.lighting.contains("99999") && !TextUtils.isEmpty(dto.province) && dto.province.startsWith("黑龙江")) {
												lightingList.add(dto);
											}
										}
									}
									if (!object.isNull("data_6")) {
										JSONArray array = object.getJSONArray("data_6");
										Log.e("length", array.length()+"");
										for (int i = 0; i < array.length(); i++) {
											StreamFactDto dto = new StreamFactDto();
											JSONObject itemObj = array.getJSONObject(i);
											if (!itemObj.isNull("Lat")) {
												dto.lat = itemObj.getDouble("Lat");
											}
											if (!itemObj.isNull("Lon")) {
												dto.lng = itemObj.getDouble("Lon");
											}
											if (!itemObj.isNull("Station_ID_C")) {
												dto.stationId = itemObj.getString("Station_ID_C");
											}
											if (!itemObj.isNull("Station_Name")) {
												dto.stationName = itemObj.getString("Station_Name");
											}
											if (!itemObj.isNull("Province")) {
												dto.province = itemObj.getString("Province");
											}
											if (!itemObj.isNull("Lit_City")) {
												dto.city = itemObj.getString("Lit_City");
											}
											if (!itemObj.isNull("Lit_Cnty")) {
												dto.dis = itemObj.getString("Lit_Cnty");
											}
											if (!itemObj.isNull("Lit_Current")) {
												dto.lighting = itemObj.getString("Lit_Current");
											}
											dto.lightingType = 6;
											if (!dto.lighting.contains("99999") && !TextUtils.isEmpty(dto.province) && dto.province.startsWith("黑龙江")) {
												lightingList.add(dto);
											}
										}
									}
									addLightingMarkers();
									settingSeekbar();
									llSeekBar.setVisibility(View.VISIBLE);
								}

								if (!obj.isNull("PRE")) {
									JSONObject object = obj.getJSONObject("PRE");
									if (!object.isNull("data")) {
										rainList.clear();
										JSONArray array = object.getJSONArray("data");
										for (int i = 0; i < array.length(); i++) {
											StreamFactDto dto = new StreamFactDto();
											JSONObject itemObj = array.getJSONObject(i);
											if (!itemObj.isNull("Lat")) {
												dto.lat = itemObj.getDouble("Lat");
											}
											if (!itemObj.isNull("Lon")) {
												dto.lng = itemObj.getDouble("Lon");
											}
											if (!itemObj.isNull("Station_ID_C")) {
												dto.stationId = itemObj.getString("Station_ID_C");
											}
											if (!itemObj.isNull("Station_Name")) {
												dto.stationName = itemObj.getString("Station_Name");
											}
											if (!itemObj.isNull("Province")) {
												dto.province = itemObj.getString("Province");
											}
											if (!itemObj.isNull("City")) {
												dto.city = itemObj.getString("City");
											}
											if (!itemObj.isNull("Cnty")) {
												dto.dis = itemObj.getString("Cnty");
											}
											if (!itemObj.isNull("PRE_1h")) {
												dto.pre1h = itemObj.getString("PRE_1h");
											}
											if (!dto.pre1h.contains("99999") && !TextUtils.isEmpty(dto.province) && dto.province.startsWith("黑龙江")) {
												double pre1h = Double.parseDouble(dto.pre1h);
												if (pre1h <= 300) {//过滤掉300mm以上
													rainList.add(dto);
												}
											}
										}
									}
								}

								if (!obj.isNull("WIN")) {
									JSONObject object = obj.getJSONObject("WIN");
									if (!object.isNull("data")) {
										windList.clear();
										JSONArray array = object.getJSONArray("data");
										for (int i = 0; i < array.length(); i++) {
											StreamFactDto dto = new StreamFactDto();
											JSONObject itemObj = array.getJSONObject(i);
											if (!itemObj.isNull("Lat")) {
												dto.lat = itemObj.getDouble("Lat");
											}
											if (!itemObj.isNull("Lon")) {
												dto.lng = itemObj.getDouble("Lon");
											}
											if (!itemObj.isNull("Station_ID_C")) {
												dto.stationId = itemObj.getString("Station_ID_C");
											}
											if (!itemObj.isNull("Station_Name")) {
												dto.stationName = itemObj.getString("Station_Name");
											}
											if (!itemObj.isNull("Province")) {
												dto.province = itemObj.getString("Province");
											}
											if (!itemObj.isNull("City")) {
												dto.city = itemObj.getString("City");
											}
											if (!itemObj.isNull("Cnty")) {
												dto.dis = itemObj.getString("Cnty");
											}
											if (!itemObj.isNull("WIN_S_Max")) {
												dto.windS = itemObj.getString("WIN_S_Max");
											}
											if (!itemObj.isNull("WIN_D_S_Max")) {
												dto.windD = itemObj.getString("WIN_D_S_Max");
											}
											if (!dto.windS.contains("99999") && !TextUtils.isEmpty(dto.province) && dto.province.startsWith("黑龙江")) {
												double windS = Double.parseDouble(dto.windS);
												if (windS > 17 && windS < 60) {//过滤掉17m/s以下、60m/s以上
													windList.add(dto);
												}
											}
										}
									}
								}

								if (!obj.isNull("HAIL")) {
									JSONObject object = obj.getJSONObject("HAIL");
									if (!object.isNull("data")) {
										hailList.clear();
										JSONArray array = object.getJSONArray("data");
										for (int i = 0; i < array.length(); i++) {
											StreamFactDto dto = new StreamFactDto();
											JSONObject itemObj = array.getJSONObject(i);
											if (!itemObj.isNull("Lat")) {
												dto.lat = itemObj.getDouble("Lat");
											}
											if (!itemObj.isNull("Lon")) {
												dto.lng = itemObj.getDouble("Lon");
											}
											if (!itemObj.isNull("Station_ID_C")) {
												dto.stationId = itemObj.getString("Station_ID_C");
											}
											if (!itemObj.isNull("Station_Name")) {
												dto.stationName = itemObj.getString("Station_Name");
											}
											if (!itemObj.isNull("Province")) {
												dto.province = itemObj.getString("Province");
											}
											if (!itemObj.isNull("City")) {
												dto.city = itemObj.getString("City");
											}
											if (!itemObj.isNull("Cnty")) {
												dto.dis = itemObj.getString("Cnty");
											}
											if (!itemObj.isNull("HAIL_Diam_Max")) {
												dto.hail = itemObj.getString("HAIL_Diam_Max");
											}
											if (!dto.hail.contains("99999") && !TextUtils.isEmpty(dto.province) && dto.province.startsWith("黑龙江")) {
												hailList.add(dto);
											}
										}
									}
								}

								ivLighting.setVisibility(View.VISIBLE);
								ivRain.setVisibility(View.VISIBLE);
								ivWind.setVisibility(View.VISIBLE);
								ivHail.setVisibility(View.VISIBLE);
								tvLighting.setVisibility(View.VISIBLE);
								tvRain.setVisibility(View.VISIBLE);
								tvWind.setVisibility(View.VISIBLE);
								tvHail.setVisibility(View.VISIBLE);

							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
						cancelDialog();
					}
				});
			}
		});
	}

	private void settingSeekbar() {
		llContainer.removeAllViews();
		for (int i = 0; i < 6; i++) {
			TextView tvName = new TextView(mContext);
			tvName.setGravity(Gravity.CENTER);
			tvName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
			tvName.setPadding(0, 0, 0, 0);
			if (i == 0) {
				tvName.setTextColor(getResources().getColor(R.color.colorPrimary));
			}else {
				tvName.setTextColor(getResources().getColor(R.color.text_color2));
			}
			try {
				String time = sdf1.format(new Date().getTime()-1000*60*60);
				time = sdf3.format(sdf1.parse(time).getTime()+1000*60*10*(i+1));
				tvName.setText(time);
				tvName.setTag(i+1);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			tvName.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					for (int j = 0; j < llContainer.getChildCount(); j++) {
						TextView tvName = (TextView) llContainer.getChildAt(j);
						if (tvName.getTag() == arg0.getTag()) {
							lightingType = (Integer)arg0.getTag();
							tvName.setTextColor(getResources().getColor(R.color.colorPrimary));
							addLightingMarkers();
							changeSeekbarProgress(lightingType-1, llContainer.getChildCount()-1);
						}else {
							tvName.setTextColor(getResources().getColor(R.color.text_color2));
						}
					}
				}
			});

			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			params.weight = 1.0f;
			tvName.setLayoutParams(params);
			llContainer.addView(tvName, i);
		}

//		if (seekbarThread != null) {
//			seekbarThread.cancel();
//			seekbarThread = null;
//		}
//		if (llContainer.getChildCount() > 0) {
//			seekbarThread = new SeekbarThread();
//			seekbarThread.start();
//			ivPlay.setImageResource(R.drawable.iv_pause);
//		}

	}

	/**
	 * 时间轴
	 */
	private class SeekbarThread extends Thread {
		static final int STATE_NONE = 0;
		static final int STATE_PLAYING = 1;
		static final int STATE_PAUSE = 2;
		static final int STATE_CANCEL = 3;
		private int state;
		private int count = 6;
		private boolean isTracking;

		public SeekbarThread() {
			this.state = STATE_NONE;
			this.isTracking = false;
		}

		public int getCurrentState() {
			return state;
		}

		@Override
		public void run() {
			super.run();
			this.state = STATE_PLAYING;
			while (true) {
				if (state == STATE_CANCEL) {
					break;
				}
				if (state == STATE_PAUSE) {
					continue;
				}
				if (isTracking) {
					continue;
				}
				switchLightingData();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		private void switchLightingData() {
			if (lightingType > count) {
				lightingType = 1;

				if (seekbarThread != null) {
					seekbarThread.pause();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							ivPlay.setImageResource(R.drawable.icon_play);
						}
					});
				}
			}else {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						addLightingMarkers();
						changeSeekbarProgress(lightingType-1, count-1);
						lightingType++;
					}
				});
			}

		}

		public void cancel() {
			this.state = STATE_CANCEL;
		}
		public void pause() {
			this.state = STATE_PAUSE;
		}
		public void play() {
			this.state = STATE_PLAYING;
		}
		public void startTracking() {
			isTracking = true;
		}

		public void stopTracking() {
			isTracking = false;
			if (this.state == STATE_PAUSE) {
				switchLightingData();
			}
		}
	}

	private void changeSeekbarProgress(int progress, int max) {
		if (seekBar != null) {
			seekBar.setMax(max);
			seekBar.setProgress(progress);
		}

		for (int j = 0; j < llContainer.getChildCount(); j++) {
			TextView tvName = (TextView) llContainer.getChildAt(j);
			if (lightingType == (Integer)tvName.getTag()) {
				tvName.setTextColor(getResources().getColor(R.color.colorPrimary));
			}else {
				tvName.setTextColor(getResources().getColor(R.color.text_color2));
			}
		}
	}

	private void removeLightingMarkers() {
		for (Marker marker : lightingMarkers) {
			marker.remove();
		}
		lightingMarkers.clear();
	}

	private void addLightingMarkers() {
		removeLightingMarkers();
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		for (StreamFactDto dto : lightingList) {
			if (dto.lightingType == lightingType) {
				MarkerOptions options = new MarkerOptions();
				options.title("位置："+dto.lat+","+dto.lng+"\n"+"行政区划："+dto.province+dto.city+dto.dis+"\n"+"强度："+dto.lighting+"(10KA)");
				options.position(new LatLng(dto.lat, dto.lng));
				View view = inflater.inflate(R.layout.shawn_layout_marker_stream_fact, null);
				ImageView ivMarker = view.findViewById(R.id.ivMarker);
				double value = Double.parseDouble(dto.lighting);
				if (value > 0) {
					ivMarker.setImageResource(R.drawable.sd_01);
				}else {
					ivMarker.setImageResource(R.drawable.sd_02);
				}
				options.icon(BitmapDescriptorFactory.fromView(view));
				Marker marker = aMap.addMarker(options);
				lightingMarkers.add(marker);
			}
		}
	}

	private void removeRainMarkers() {
		for (Marker marker : rainMarkers) {
			marker.remove();
		}
		rainMarkers.clear();
	}

	private void addRainMarkers() {
		removeRainMarkers();
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		for (StreamFactDto dto : rainList) {
			MarkerOptions options = new MarkerOptions();
			options.title("站名："+dto.stationName+"\n"+"行政区划："+dto.province+dto.city+dto.dis+"\n"+"站号："+dto.stationId+"\n"+"降水量："+dto.pre1h+getString(R.string.unit_mm));
			options.position(new LatLng(dto.lat, dto.lng));
			View view = inflater.inflate(R.layout.shawn_layout_marker_stream_fact, null);
			ImageView ivMarker = view.findViewById(R.id.ivMarker);
			double value = Double.parseDouble(dto.pre1h);
			if (value < 30) {
				ivMarker.setImageResource(R.drawable.qjs20);
			}else if (value >= 30 && value < 50){
				ivMarker.setImageResource(R.drawable.qjs30);
			}else if (value >= 50 && value < 80){
				ivMarker.setImageResource(R.drawable.qjs50);
			}else {
				ivMarker.setImageResource(R.drawable.qjs80);
			}
			options.icon(BitmapDescriptorFactory.fromView(view));
			Marker marker = aMap.addMarker(options);
			rainMarkers.add(marker);
		}
	}

	private void removeWindMarkers() {
		for (Marker marker : windMarkers) {
			marker.remove();
		}
		windMarkers.clear();
	}

	private void addWindMarkers() {
		removeWindMarkers();
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		for (StreamFactDto dto : windList) {
			MarkerOptions options = new MarkerOptions();
			float windS = Float.parseFloat(dto.windS);
			options.title("站名："+dto.stationName+"\n"+"行政区划："+dto.province+dto.city+dto.dis+"\n"+"站号："+dto.stationId+"\n"+"风速："+dto.windS+getString(R.string.unit_speed)+"("+ WeatherUtil.getHourWindForce(windS)+")");
			options.position(new LatLng(dto.lat, dto.lng));
			View view = inflater.inflate(R.layout.shawn_layout_marker_stream_fact, null);
			ImageView ivMarker = view.findViewById(R.id.ivMarker);
			Bitmap b = CommonUtil.getStrongWindMarker(mContext, Float.parseFloat(dto.windS));
			if (b != null) {
				Matrix matrix = new Matrix();
				matrix.postScale(1, 1);
				matrix.postRotate(Float.parseFloat(dto.windD));
				Bitmap bitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
				if (bitmap != null) {
					ivMarker.setImageBitmap(bitmap);
				}
			}

			options.icon(BitmapDescriptorFactory.fromView(view));
			Marker marker = aMap.addMarker(options);
			windMarkers.add(marker);
		}
	}

	private void removeHailMarkers() {
		for (Marker marker : hailMarkers) {
			marker.remove();
		}
        hailMarkers.clear();
	}

	private void addHailMarkers() {
		removeHailMarkers();
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		for (StreamFactDto dto : hailList) {
			MarkerOptions options = new MarkerOptions();
			options.title("站名："+dto.stationName+"\n"+"行政区划："+dto.province+dto.city+dto.dis+"\n"+"站号："+dto.stationId+"\n"+"直径："+dto.hail+getString(R.string.unit_mm));
			options.position(new LatLng(dto.lat, dto.lng));
			View view = inflater.inflate(R.layout.shawn_layout_marker_stream_fact, null);
			ImageView ivMarker = view.findViewById(R.id.ivMarker);
			double value = Double.parseDouble(dto.hail);
			if (value < 5) {
				ivMarker.setImageResource(R.drawable.bingb0);
			}else if (value >= 5 && value < 10) {
				ivMarker.setImageResource(R.drawable.bingb5);
			}else if (value >= 10 && value < 20) {
				ivMarker.setImageResource(R.drawable.bingb10);
			}else {
				ivMarker.setImageResource(R.drawable.bingb20);
			}
			options.icon(BitmapDescriptorFactory.fromView(view));
			Marker marker = aMap.addMarker(options);
            hailMarkers.add(marker);
		}
	}

	@Override
	public void onMapClick(LatLng latLng) {
		if (selectMarker != null && selectMarker.isInfoWindowShown()) {
			selectMarker.hideInfoWindow();
		}
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		selectMarker = marker;
		marker.showInfoWindow();
		return true;
	}

	@Override
	public View getInfoContents(final Marker marker) {
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.shawn_layout_marker_stream_fact_info, null);
		TextView tvValue = view.findViewById(R.id.tvValue);
		if (!TextUtils.isEmpty(marker.getTitle())) {
			tvValue.setText(marker.getTitle());
		}
		return view;
	}

	@Override
	public View getInfoWindow(Marker arg0) {
		return null;
	}

	/**
	 * 获取彩云数据
	 */
	private void OkHttpCaiyun() {
		final String url = "http://api.tianqi.cn:8070/v1/img.py";
		OkHttpUtil.enqueue(new Request.Builder().url(url).build(), new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
			}
			@Override
			public void onResponse(Call call, Response response) throws IOException {
				if (!response.isSuccessful()) {
					return;
				}
				String result = response.body().string();
				if (!TextUtils.isEmpty(result)) {
					try {
						JSONObject obj = new JSONObject(result);
						if (!obj.isNull("status")) {
							if (obj.getString("status").equals("ok")) {
								if (!obj.isNull("radar_img")) {
									JSONArray array = new JSONArray(obj.getString("radar_img"));
									caiyunList.clear();
									for (int i = 0; i < array.length(); i++) {
										JSONArray array0 = array.getJSONArray(i);
										MinuteFallDto dto = new MinuteFallDto();
										dto.setImgUrl(array0.optString(0));
										dto.setTime(array0.optLong(1));
										JSONArray itemArray = array0.getJSONArray(2);
										dto.setP1(itemArray.optDouble(0));
										dto.setP2(itemArray.optDouble(1));
										dto.setP3(itemArray.optDouble(2));
										dto.setP4(itemArray.optDouble(3));
										caiyunList.add(dto);
									}
									if (caiyunList.size() > 0) {
										startDownloadCaiyunImgs(caiyunList);
									}
								}
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	/**
	 * 下载图片
	 * @param list
	 */
	private void startDownloadCaiyunImgs(ArrayList<MinuteFallDto> list) {
		if (caiyunThread != null) {
			caiyunThread.cancel();
			caiyunThread = null;
		}
		caiyunManager.loadImagesAsyn(list, new CaiyunManager.RadarListener() {
			@Override
			public void onResult(int result, ArrayList<MinuteFallDto> images) {
				mHandler.sendEmptyMessage(HANDLER_LOAD_FINISHED);
				if (result == CaiyunManager.RadarListener.RESULT_SUCCESSED) {
//			if (mRadarThread != null) {
//				mRadarThread.cancel();
//				mRadarThread = null;
//			}
//			mRadarThread = new RadarThread(images);
//			mRadarThread.start();

					//把最新的一张降雨图片覆盖在地图上
//			MinuteFallDto radar = images.get(images.size()-1);
//			Message message = mHandler.obtainMessage();
//			message.what = HANDLER_SHOW_RADAR;
//			message.obj = radar;
//			message.arg1 = 100;
//			message.arg2 = 100;
//			mHandler.sendMessage(message);
				}
			}

			@Override
			public void onProgress(String url, int progress) {
				Message msg = new Message();
				msg.obj = progress;
				msg.what = HANDLER_PROGRESS;
				mHandler.sendMessage(msg);
			}
		});
	}

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			int what = msg.what;
			switch (what) {
				case HANDLER_SHOW_RADAR:
					if (msg.obj != null) {
						MinuteFallDto dto = (MinuteFallDto) msg.obj;
						if (dto.getPath() != null) {
							Bitmap bitmap = BitmapFactory.decodeFile(dto.getPath());
							if (bitmap != null) {
								showRadar(bitmap, dto.getP1(), dto.getP2(), dto.getP3(), dto.getP4());
							}
						}
					}
					break;
				case HANDLER_PROGRESS:
//				if (mDialog != null) {
//					if (msg.obj != null) {
//						int progress = (Integer) msg.obj;
//						mDialog.setPercent(progress);
//					}
//				}
					break;
				case HANDLER_LOAD_FINISHED:
					ivRadar.setVisibility(View.VISIBLE);
					break;
				case HANDLER_PAUSE:

					break;

				default:
					break;
			}

		};
	};

	private void showRadar(Bitmap bitmap, double p1, double p2, double p3, double p4) {
		BitmapDescriptor fromView = BitmapDescriptorFactory.fromBitmap(bitmap);
		LatLngBounds bounds = new LatLngBounds.Builder()
				.include(new LatLng(p3, p2))
				.include(new LatLng(p1, p4))
				.build();

		if (radarOverlay == null) {
			radarOverlay = aMap.addGroundOverlay(new GroundOverlayOptions()
					.anchor(0.5f, 0.5f)
					.positionFromBounds(bounds)
					.image(fromView)
					.transparency(0.0f));
		} else {
			radarOverlay.setImage(null);
			radarOverlay.setPositionFromBounds(bounds);
			radarOverlay.setImage(fromView);
		}
		aMap.runOnDrawFrame();
	}

	private class CaiyunThread extends Thread {
		static final int STATE_NONE = 0;
		static final int STATE_PLAYING = 1;
		static final int STATE_PAUSE = 2;
		static final int STATE_CANCEL = 3;
		private List<MinuteFallDto> images;
		private int state;
		private int index;
		private int count;
		private boolean isTracking;

		public CaiyunThread(List<MinuteFallDto> images) {
			this.images = images;
			this.count = images.size();
			this.index = 0;
			this.state = STATE_NONE;
			this.isTracking = false;
		}

		public int getCurrentState() {
			return state;
		}

		@Override
		public void run() {
			super.run();
			this.state = STATE_PLAYING;
			while (true) {
				if (state == STATE_CANCEL) {
					break;
				}
				if (state == STATE_PAUSE) {
					continue;
				}
				if (isTracking) {
					continue;
				}
				sendRadar();
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		private void sendRadar() {
			if (index >= count || index < 0) {
				index = 0;

//				if (mRadarThread != null) {
//					mRadarThread.pause();
//
//					Message message = mHandler.obtainMessage();
//					message.what = HANDLER_PAUSE;
//					mHandler.sendMessage(message);
//					if (seekBar != null) {
//						seekBar.setProgress(100);
//					}
//				}
			}else {
				MinuteFallDto radar = images.get(index);
				Message message = mHandler.obtainMessage();
				message.what = HANDLER_SHOW_RADAR;
				message.obj = radar;
				message.arg1 = count - 1;
				message.arg2 = index ++;
				mHandler.sendMessage(message);
			}
		}

		public void cancel() {
			this.state = STATE_CANCEL;
		}
		public void pause() {
			this.state = STATE_PAUSE;
		}
		public void play() {
			this.state = STATE_PLAYING;
		}

		public void setCurrent(int index) {
			this.index = index;
		}

		public void startTracking() {
			isTracking = true;
		}

		public void stopTracking() {
			isTracking = false;
			if (this.state == STATE_PAUSE) {
				sendRadar();
			}
		}
	}

}
