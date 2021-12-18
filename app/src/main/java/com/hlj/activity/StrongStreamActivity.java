package com.hlj.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.GroundOverlay;
import com.amap.api.maps.model.GroundOverlayOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.model.animation.Animation;
import com.amap.api.maps.model.animation.ScaleAnimation;
import com.hlj.common.CONST;
import com.hlj.dto.StrongStreamDto;
import com.hlj.manager.CaiyunManager;
import com.hlj.manager.StrongStreamManager;
import com.hlj.utils.CommonUtil;
import com.hlj.utils.OkHttpUtil;
import com.hlj.view.MySeekbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import shawn.cxwl.com.hlj.R;

/**
 * 分钟降水与强对流
 */
public class StrongStreamActivity extends BaseActivity implements OnClickListener {
	
	private Context mContext;
	private TextView tvTitle;
	private MapView mMapView;
	private AMap aMap;
	private List<StrongStreamDto> radarList = new ArrayList<>();
	private GroundOverlay mOverlay;
	private StrongStreamManager mRadarManager;
	private RadarThread mRadarThread;
	private static final int HANDLER_SHOW_RADAR = 1;
	private static final int HANDLER_PROGRESS = 2;
	private static final int HANDLER_LOAD_FINISHED = 3;
	private static final int HANDLER_PAUSE = 4;
	private HashMap<String, JSONObject> hashMap = new HashMap<>();//强对流数据
	private List<Polyline> polylines = new ArrayList<>();
	private TextView tvLighting, tvRadar;
	private ImageView ivRadar,ivLighting,ivLegend;
	private HashMap<String, JSONObject> lightingMap = new HashMap<>();//闪电数据
	private List<Marker> lightingMarkers = new ArrayList<>();//闪电markers
	private boolean isShowLightingMarkers = false;
	private LinearLayout llLegend,llContainer;
	private MySeekbar mySeekbar;
	private MyBroadCastReceiver mReceiver;
	private String BROAD_CLICKMENU = "broad_clickMenu";//点击播放或暂停

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_strong_stream);
		mContext = this;
		initBroadCast();
		showDialog();
		initMap(savedInstanceState);
		initWidget();
	}

	private void initBroadCast() {
		mReceiver = new MyBroadCastReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BROAD_CLICKMENU);
		registerReceiver(mReceiver, intentFilter);
	}

	private class MyBroadCastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (TextUtils.equals(intent.getAction(), BROAD_CLICKMENU)) {
				int currentIndex = intent.getExtras().getInt("currentIndex", 0);
				if (mRadarThread == null) {
					mRadarThread = new RadarThread(radarList);
					mRadarThread.index = currentIndex;
					mRadarThread.start();
				}else {
					if (mRadarThread.getCurrentState() == RadarThread.STATE_PLAYING) {
						mRadarThread.pause();
					}else if (mRadarThread.getCurrentState() == RadarThread.STATE_PAUSE) {
						mRadarThread.play();
					}
				}
			}
		}
	}

	private void unregistBroadCast() {
		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
		}
	}

	private void initMap(Bundle bundle) {
		mMapView = findViewById(R.id.mapView);
		mMapView.onCreate(bundle);
		if (aMap == null) {
			aMap = mMapView.getMap();
		}
		aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CONST.guizhouLatLng, 3.7f));
		aMap.getUiSettings().setZoomControlsEnabled(false);
		aMap.getUiSettings().setRotateGesturesEnabled(false);
		aMap.setOnMapLoadedListener(new AMap.OnMapLoadedListener() {
			@Override
			public void onMapLoaded() {
				TextView tvMapNumber = findViewById(R.id.tvMapNumber);
				tvMapNumber.setText(aMap.getMapContentApprovalNumber());
			}
		});
	}

	private void initWidget() {
		LinearLayout llBack = findViewById(R.id.llBack);
		llBack.setOnClickListener(this);
		tvTitle = findViewById(R.id.tvTitle);
		ivRadar = findViewById(R.id.ivRadar);
		ivRadar.setOnClickListener(this);
		ivLighting = findViewById(R.id.ivLighting);
		ivLighting.setOnClickListener(this);
		tvLighting = findViewById(R.id.tvLighting);
		tvLighting.setOnClickListener(this);
		tvRadar = findViewById(R.id.tvRadar);
		tvRadar.setOnClickListener(this);
		llLegend = findViewById(R.id.llLegend);
		ImageView ivRank = findViewById(R.id.ivRank);
		ivRank.setOnClickListener(this);
		ivLegend = findViewById(R.id.ivLegend);
		llContainer = findViewById(R.id.llContainer);

		String title = getIntent().getStringExtra(CONST.ACTIVITY_NAME);
		if (!TextUtils.isEmpty(title)) {
			tvTitle.setText(title);
		}
		
		mRadarManager = new StrongStreamManager(mContext);
		OkHttpData();
	}
	
	/**
	 * 获取雷达图数据
	 */
	private void OkHttpData() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				CommonUtil.drawHLJJson(mContext, aMap);
				final String url = "http://cn-scw.tianqi.cn/api/merge-all";
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
								JSONObject object = new JSONObject(result);

								//强对流图层数据
								if (!object.isNull("cn_scw")) {
									JSONObject obj = object.getJSONObject("cn_scw");
									hashMap.clear();
									if (!obj.isNull("obs")) {
										JSONArray array = obj.getJSONArray("obs");
										for (int i = 0; i < array.length(); i++) {
											JSONObject itemObj = array.getJSONObject(i);
											if (!itemObj.isNull("startTime")) {
												String startTime = itemObj.getString("startTime");
												hashMap.put(startTime, itemObj);
											}
										}
									}

//							if (!obj.isNull("forecast")) {
//								JSONObject itemObj = obj.getJSONObject("forecast");
//								if (!itemObj.isNull("startTime")) {
//									String startTime = itemObj.getString("startTime");
//									hashMap.put(startTime, itemObj);
//								}
//							}
								}

								//闪电数据
								if (!object.isNull("lightning")) {
									JSONObject obj = object.getJSONObject("lightning");
									lightingMap.clear();
									if (!obj.isNull("observe")) {
										JSONArray array = obj.getJSONArray("observe");
										for (int i = 0; i < array.length(); i++) {
											JSONObject itemObj = array.getJSONObject(i);
											if (!itemObj.isNull("startTime")) {
												String startTime = itemObj.getString("startTime");
												lightingMap.put(startTime, itemObj);
											}
										}
									}

									if (!obj.isNull("forecast")) {
										JSONArray array = obj.getJSONArray("forecast");
										for (int i = 0; i < array.length(); i++) {
											JSONObject itemObj = array.getJSONObject(i);
											if (!itemObj.isNull("startTime")) {
												String startTime = itemObj.getString("startTime");
												lightingMap.put(startTime, itemObj);
											}
										}
									}
								}

								//雷达图层数据
								if (!object.isNull("radar")) {
									JSONObject obj = object.getJSONObject("radar");
									radarList.clear();
									if (!obj.isNull("files_before")) {
										JSONArray array = new JSONArray(obj.getString("files_before"));
										for (int i = 0; i < array.length(); i++) {
											StrongStreamDto dto = new StrongStreamDto();
											String itemUrl = array.getString(i);
											if (!TextUtils.isEmpty(itemUrl)) {
												dto.imgUrl = "http://radar-qpfref.tianqi.cn/"+itemUrl;
												dto.time = itemUrl.substring(itemUrl.length()-16, itemUrl.length()-4);
												if (i == array.length()-1) {
													dto.tag = "currentTime";
												}
												radarList.add(dto);
											}
										}
									}

									if (!obj.isNull("files_after")) {
										JSONArray array = new JSONArray(obj.getString("files_after"));
										for (int i = 0; i < array.length(); i++) {
											StrongStreamDto dto = new StrongStreamDto();
											String itemUrl = array.getString(i);
											if (!TextUtils.isEmpty(itemUrl)) {
												dto.imgUrl = "http://radar-qpfref.tianqi.cn/"+itemUrl;
												dto.time = itemUrl.substring(itemUrl.length()-16, itemUrl.length()-4);
												radarList.add(dto);
											}
										}
									}

									if (radarList.size() > 0) {
										startDownLoadImgs(radarList);
									}
								}

							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}
				});
			}
		}).start();
	}
	
	private void startDownLoadImgs(List<StrongStreamDto> list) {
		if (mRadarThread != null) {
			mRadarThread.cancel();
			mRadarThread = null;
		}
 		mRadarManager.loadImagesAsyn(list, new StrongStreamManager.StrongStreamListener() {
			@Override
			public void onResult(int result, List<StrongStreamDto> images) {
				mHandler.sendEmptyMessage(HANDLER_LOAD_FINISHED);
				if (result == CaiyunManager.RadarListener.RESULT_SUCCESSED) {
					for (int i = 0; i < images.size(); i++) {
						StrongStreamDto dto = images.get(i);
						if (TextUtils.equals(dto.tag, "currentTime")) {
							Message message = mHandler.obtainMessage();
							message.what = HANDLER_SHOW_RADAR;
							message.obj = dto;
							message.arg1 = images.size()-1;
							message.arg2 = 0;
							mHandler.sendMessage(message);
							break;
						}
					}
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
	
	private void showRadar(Bitmap bitmap) {
		BitmapDescriptor fromView = BitmapDescriptorFactory.fromBitmap(bitmap);
		LatLngBounds bounds = new LatLngBounds.Builder()
		.include(new LatLng(53.56, 73.44))
		.include(new LatLng(10.15, 135.09))
		.build();
		
		if (mOverlay == null) {
			mOverlay = aMap.addGroundOverlay(new GroundOverlayOptions()
				.anchor(0.5f, 0.5f)
				.positionFromBounds(bounds)
				.image(fromView)
				.transparency(0.0f));
		} else {
			mOverlay.setImage(null);
			mOverlay.setPositionFromBounds(bounds);
			mOverlay.setImage(fromView);
		}
		aMap.runOnDrawFrame();
	}
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			int what = msg.what;
			switch (what) {
			case HANDLER_SHOW_RADAR: 
				if (msg.obj != null) {
					StrongStreamDto dto = (StrongStreamDto) msg.obj;
					if (!TextUtils.isEmpty(dto.path)) {
						Bitmap bitmap = BitmapFactory.decodeFile(dto.path);
						if (bitmap != null) {
							showRadar(bitmap);
						}
					}

					removeLightingMarkers();
					for (int i = 0; i < radarList.size(); i++) {
						String startTime = radarList.get(i).time;
						if (TextUtils.equals(dto.time, startTime)) {
							//绘制强对流数据图层
							if (hashMap.containsKey(startTime)) {
								removePolygons();
								drawStrongStreamLayer(startTime);
							}

							//绘制闪电数据
							if (lightingMap.containsKey(startTime)) {
								addLightingMarkers(startTime);
							}
							break;
						}
					}

					if (mRadarThread != null && mySeekbar != null) {
						mySeekbar.playingIndex = msg.arg2;
						mySeekbar.playingTime = dto.time;
						mySeekbar.postInvalidate();
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
				cancelDialog();
				tvRadar.setVisibility(View.VISIBLE);
				ivRadar.setVisibility(View.VISIBLE);
				tvLighting.setVisibility(View.VISIBLE);
				ivLighting.setVisibility(View.VISIBLE);
				llLegend.setVisibility(View.VISIBLE);
				llContainer.removeAllViews();
				mySeekbar = new MySeekbar(mContext);
				mySeekbar.setData(radarList, hashMap);
				llContainer.addView(mySeekbar, CommonUtil.widthPixels(mContext), (int) CommonUtil.dip2px(mContext, 60));
				break;
			case HANDLER_PAUSE:
				break;
			default:
				break;
			}
			
		};
	};
	
	private class RadarThread extends Thread {
		public static final int STATE_NONE = 0;
		public static final int STATE_PLAYING = 1;
		public static final int STATE_PAUSE = 2;
		public static final int STATE_CANCEL = 3;
		private List<StrongStreamDto> radarList;
		public int state;
		private int index;
		private int count;
		private boolean isTracking;

		public RadarThread(List<StrongStreamDto> radarList) {
			this.radarList = radarList;
			this.count = radarList.size();
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
					Thread.sleep(1000);
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
				StrongStreamDto dto = radarList.get(index);
				Message message = mHandler.obtainMessage();
				message.what = HANDLER_SHOW_RADAR;
				message.obj = dto;
				message.arg1 = count - 1;
				message.arg2 = index++;
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
	
	/**
	 * 清除强对流图层
	 */
	private void removePolygons() {
		for (int i = 0; i < polylines.size(); i++) {
			polylines.get(i).remove();
		}
		polylines.clear();
	}

	/**
	 * 绘制强对流图层
	 * @param startTime
	 */
	private void drawStrongStreamLayer(String startTime) {
		if (TextUtils.isEmpty(startTime)) {
			return;
		}
		if (!hashMap.containsKey(startTime)) {
			return;
		}
		Log.e("startTime", startTime);

		JSONObject obj = hashMap.get(startTime);
		try {
			if (!obj.isNull("wind")  && !TextUtils.isEmpty(obj.getString("wind"))) {
				JSONArray array = obj.getJSONArray("wind");
				for (int i = 0; i < array.length(); i++) {
					JSONObject itemObj = array.getJSONObject(i);
					if (!itemObj.isNull("xy")) {
						JSONArray itemArray = itemObj.getJSONArray("xy");
						PolylineOptions polylineOptions = new PolylineOptions();
						polylineOptions.color(0xff6727b0);
						polylineOptions.width(8);
						for (int j = 0; j < itemArray.length(); j+=2) {
							double lat = itemArray.getDouble(j);
							double lng = itemArray.getDouble(j+1);
							polylineOptions.add(new LatLng(lat, lng));
						}
						Polyline polyline = aMap.addPolyline(polylineOptions);
						polylines.add(polyline);
					}
				}
			}

			if (!obj.isNull("rain")  && !TextUtils.isEmpty(obj.getString("rain"))) {
				JSONArray array = obj.getJSONArray("rain");
				for (int i = 0; i < array.length(); i++) {
					JSONObject itemObj = array.getJSONObject(i);
					if (!itemObj.isNull("xy")) {
						JSONArray itemArray = itemObj.getJSONArray("xy");
						PolylineOptions polylineOptions = new PolylineOptions();
						polylineOptions.color(0xff0070c6);
						polylineOptions.width(8);
						for (int j = 0; j < itemArray.length(); j+=2) {
							double lat = itemArray.getDouble(j);
							double lng = itemArray.getDouble(j+1);
							polylineOptions.add(new LatLng(lat, lng));
						}
						Polyline polyline = aMap.addPolyline(polylineOptions);
						polylines.add(polyline);
					}
				}
			}

			if (!obj.isNull("hail") && !TextUtils.isEmpty(obj.getString("hail"))) {
				JSONArray array = obj.getJSONArray("hail");
				for (int i = 0; i < array.length(); i++) {
					JSONObject itemObj = array.getJSONObject(i);
					if (!itemObj.isNull("xy")) {
						JSONArray itemArray = itemObj.getJSONArray("xy");
						PolylineOptions polylineOptions = new PolylineOptions();
						polylineOptions.color(0xffc20052);
						polylineOptions.width(8);
						for (int j = 0; j < itemArray.length(); j+=2) {
							double lat = itemArray.getDouble(j);
							double lng = itemArray.getDouble(j+1);
							polylineOptions.add(new LatLng(lat, lng));
						}
						Polyline polyline = aMap.addPolyline(polylineOptions);
						polylines.add(polyline);
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 清除闪电marker
	 */
	private void removeLightingMarkers() {
		for (int i = 0; i < lightingMarkers.size(); i++) {
			Marker marker = lightingMarkers.get(i);
			colloseMarker(marker);
			marker.remove();
		}
		lightingMarkers.clear();
	}

	/**
	 * 隐藏闪电marker
	 */
	private void hideLightingMarkers() {
		for (int i = 0; i < lightingMarkers.size(); i++) {
			Marker marker = lightingMarkers.get(i);
			marker.setVisible(false);
			colloseMarker(marker);
		}
	}

	/**
	 * 显示闪电marker
	 */
	private void showLightingMarkers() {
		for (int i = 0; i < lightingMarkers.size(); i++) {
			Marker marker = lightingMarkers.get(i);
			marker.setVisible(true);
			expandMarker(marker);
		}
	}

	private void addLightingMarkers(String startTime) {
		if (TextUtils.isEmpty(startTime)) {
			return;
		}
		if (!lightingMap.containsKey(startTime)) {
			return;
		}

		try {
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			JSONObject obj = lightingMap.get(startTime);
			if (!obj.isNull("data")) {
				JSONArray array = obj.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject itemObj = array.getJSONObject(i);
					double lng = itemObj.getDouble("lon");
					double lat = itemObj.getDouble("lat");
					MarkerOptions options = new MarkerOptions();
					options.anchor(1.0f, 0.0f);
					options.position(new LatLng(lat, lng));
					View view = inflater.inflate(R.layout.shawn_layout_strong_stream_marker_icon, null);
					ImageView ivMarker = view.findViewById(R.id.ivMarker);
					ivMarker.setImageResource(R.drawable.fzj_pic_sd);
					options.icon(BitmapDescriptorFactory.fromView(view));
					Marker marker = aMap.addMarker(options);
					marker.setVisible(isShowLightingMarkers);
					lightingMarkers.add(marker);
					expandMarker(marker);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 长大动画
	 * @param marker
	 */
	private void expandMarker(Marker marker) {
		Animation animation = new ScaleAnimation(0,1,0,1);
		animation.setInterpolator(new LinearInterpolator());
		animation.setDuration(300);
		marker.setAnimation(animation);
		marker.startAnimation();
	}

	/**
	 * 变小动画
	 * @param marker
	 */
	private void colloseMarker(Marker marker) {
		Animation animation = new ScaleAnimation(1,0,1,0);
		animation.setInterpolator(new LinearInterpolator());
		animation.setDuration(300);
		marker.setAnimation(animation);
		marker.startAnimation();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.llBack:
			finish();
			break;
		case R.id.tvRadar:
		case R.id.ivRadar:
			if (mOverlay != null) {
				if (mOverlay.isVisible()) {
					mOverlay.setVisible(false);
					ivRadar.setImageResource(R.drawable.shawn_icon_radar);
					tvRadar.setTextColor(getResources().getColor(R.color.text_color4));
					tvRadar.setBackgroundResource(R.drawable.bg_map_btn);
				}else {
					mOverlay.setVisible(true);
					ivRadar.setImageResource(R.drawable.shawn_icon_radaron);
					tvRadar.setTextColor(Color.WHITE);
					tvRadar.setBackgroundResource(R.drawable.bg_map_btn_press);
				}
			}
			break;
		case R.id.tvLighting:
		case R.id.ivLighting:
			if (isShowLightingMarkers) {
				hideLightingMarkers();
				isShowLightingMarkers = false;
				ivLighting.setImageResource(R.drawable.icon_lighting_off);
				tvLighting.setTextColor(getResources().getColor(R.color.text_color4));
				tvLighting.setBackgroundResource(R.drawable.bg_map_btn);
			}else {
				showLightingMarkers();
				isShowLightingMarkers = true;
				ivLighting.setImageResource(R.drawable.icon_lighting_on);
				tvLighting.setTextColor(Color.WHITE);
				tvLighting.setBackgroundResource(R.drawable.bg_map_btn_press);
			}
			break;
			case R.id.ivRank:
				if (ivLegend.getVisibility() == View.VISIBLE) {
					ivLegend.setVisibility(View.GONE);
				}else {
					ivLegend.setVisibility(View.VISIBLE);
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
		if (mRadarManager != null) {
			mRadarManager.onDestory();
		}
		if (mRadarThread != null) {
			mRadarThread.cancel();
			mRadarThread = null;
		}
		unregistBroadCast();
	}

}
