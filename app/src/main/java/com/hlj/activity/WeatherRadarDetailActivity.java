package com.hlj.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.hlj.adapter.WeatherRadarDetailAdpater;
import com.hlj.common.CONST;
import com.hlj.dto.AgriDto;
import com.hlj.dto.RadarDto;
import com.hlj.manager.RadarManager;
import com.hlj.manager.RadarManager.RadarListener;
import com.hlj.utils.OkHttpUtil;
import com.hlj.utils.SecretUrlUtil;
import com.hlj.view.MyDialog;

import net.tsz.afinal.FinalBitmap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import shawn.cxwl.com.hlj.R;

/**
 * 天气雷达详情
 */
public class WeatherRadarDetailActivity extends BaseActivity implements OnClickListener, RadarListener{
	
	private Context mContext = null;
	private List<RadarDto> radarList = new ArrayList<>();
	private ImageView imageView = null;
	private RadarManager mRadarManager = null;
	private RadarThread mRadarThread = null;
	private static final int HANDLER_SHOW_RADAR = 1;
	private static final int HANDLER_PROGRESS = 2;
	private static final int HANDLER_LOAD_FINISHED = 3;
	private static final int HANDLER_PAUSE = 4;
	private LinearLayout llSeekBar = null;
	private ImageView ivPlay = null;
	private SeekBar seekBar = null;
	private TextView tvTime = null;
	private WeatherRadarDetailAdpater mAdapter = null;
	private String selected = "1";//选中
	private String unselected = "0";//未选中
	private final static String APPID = "fec60dca880595d7";//机密需要用到的AppId
	private final static String LEID_DATA = "leid_data";//加密秘钥名称
	private SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm");
	private SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private SimpleDateFormat sdf3 = new SimpleDateFormat("yyyyMMddHHmm");
	private MyDialog dialog = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_weather_radar_detail);
		mContext = this;
		initWidget();
		initGridView();
	}

	private void initDialog() {
		if (dialog == null) {
			dialog = new MyDialog(mContext);
		}
		dialog.setPercent(0);
		dialog.show();
	}

	private void dismissDialog() {
		if (dialog != null) {
			dialog.dismiss();
		}
	}
	
	/**
	 * 初始化控件
	 */
	private void initWidget() {
		LinearLayout llBack = (LinearLayout) findViewById(R.id.llBack);
		llBack.setOnClickListener(this);
		TextView tvTitle = (TextView) findViewById(R.id.tvTitle);
		imageView = (ImageView) findViewById(R.id.imageView);
		imageView.setOnClickListener(this);
		ivPlay = (ImageView) findViewById(R.id.ivPlay);
		ivPlay.setOnClickListener(this);
		seekBar = (SeekBar) findViewById(R.id.seekBar);
		seekBar.setOnSeekBarChangeListener(seekbarListener);
		tvTime = (TextView) findViewById(R.id.tvTime);
		llSeekBar = (LinearLayout) findViewById(R.id.llSeekBar);
		
		mRadarManager = new RadarManager(mContext);
		
		AgriDto data = getIntent().getExtras().getParcelable("data");
		if (data != null) {
			if (data.name != null) {
				tvTitle.setText(data.name);
			}
			
			if (!TextUtils.isEmpty(data.radarId)) {
				getRadarData(data.radarId, sdf3.format(new Date()));
			}
		}
	}
	
	private OnSeekBarChangeListener seekbarListener = new OnSeekBarChangeListener() {
		@Override
		public void onStopTrackingTouch(SeekBar arg0) {
			if (mRadarThread != null) {
				mRadarThread.setCurrent(seekBar.getProgress());
				mRadarThread.stopTracking();
			}
		}
		@Override
		public void onStartTrackingTouch(SeekBar arg0) {
			if (mRadarThread != null) {
				mRadarThread.startTracking();
			}
		}
		@Override
		public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
		}
	};
	
	private String getRadarUrl(String url, String areaid, String type, String date) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(url);
		buffer.append("?");
		buffer.append("areaid=").append(areaid);
		buffer.append("&");
		buffer.append("type=").append(type);
		buffer.append("&");
		buffer.append("date=").append(date);
		buffer.append("&");
		buffer.append("appid=").append(APPID);
		
		String key = SecretUrlUtil.getKey(LEID_DATA, buffer.toString());
		buffer.delete(buffer.lastIndexOf("&"), buffer.length());
		
		buffer.append("&");
		buffer.append("appid=").append(APPID.subSequence(0, 6));
		buffer.append("&");
		buffer.append("key=").append(key.substring(0, key.length()-3));
		String result = buffer.toString();
		return result;
	}
	
	/**
	 * 获取雷达图片集信息
	 */
	private void getRadarData(String radarCode, String currentTime) {
		initDialog();
		final String url = getRadarUrl("http://hfapi.tianqi.cn/data/", radarCode, "product", currentTime);
		new Thread(new Runnable() {
			@Override
			public void run() {
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
								dismissDialog();
								if (!TextUtils.isEmpty(result)) {
									try {
										JSONObject obj = new JSONObject(result);
										String r2 = obj.getString("r2");
										String r3 = obj.getString("r3");
										String r5 = obj.getString("r5");
										JSONArray array = new JSONArray(obj.getString("r6"));
										for (int i = array.length()-1; i >= 0 ; i--) {
											JSONArray itemArray = array.getJSONArray(i);
											String r6_0 = itemArray.getString(0);
											String r6_1 = itemArray.getString(1);

											String url = r2 + r5 + r6_0 + "." + r3;

											if (i == 0) {
												if (!TextUtils.isEmpty(url)) {
													FinalBitmap finalBitmap = FinalBitmap.create(mContext);
													finalBitmap.display(imageView, url, null, 0);
												}
												try {
													tvTime.setText(sdf1.format(sdf2.parse(r6_1)));
												} catch (ParseException e) {
													e.printStackTrace();
												}
												llSeekBar.setVisibility(View.VISIBLE);
											}

											RadarDto dto = new RadarDto();
											dto.url = url;
											try {
												dto.time = sdf1.format(sdf2.parse(r6_1));
											} catch (ParseException e) {
												e.printStackTrace();
											}

											if (i == 0) {
												dto.isSelect = "1";
											}else {
												dto.isSelect = "0";
											}

											radarList.add(dto);
										}

										if (seekBar != null) {
											seekBar.setMax(radarList.size());
											seekBar.setProgress(radarList.size());
										}

										if (mAdapter != null) {
											mAdapter.notifyDataSetChanged();
										}

										if (radarList.size() <= 0) {
											cancelDialog();
											imageView.setImageResource(R.drawable.icon_no_bitmap);
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
		}).start();
	}
	
	/**
	 * 初始化gridview
	 */
	private void initGridView() {
		GridView mGridView = (GridView) findViewById(R.id.gridView);
		mAdapter = new WeatherRadarDetailAdpater(mContext, radarList);
		mGridView.setAdapter(mAdapter);
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				for (int i = 0; i < radarList.size(); i++) {
					if (i == arg2) {
						radarList.get(arg2).isSelect = selected;
					}else {
						radarList.get(i).isSelect = unselected;
					}
				}
				mAdapter.notifyDataSetChanged();
				
				if (seekBar != null && radarList.size() > 0) {
					seekBar.setProgress(arg2+1);
				}
				
				RadarDto dto = radarList.get(arg2);
				if (!TextUtils.isEmpty(dto.url)) {
					FinalBitmap finalBitmap = FinalBitmap.create(mContext);
					finalBitmap.display(imageView, dto.url, null, 0);
				}
				try {
					tvTime.setText(sdf1.format(sdf2.parse(dto.time)));
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	private void startDownLoadImgs(List<RadarDto> list) {
		if (mRadarThread != null) {
			mRadarThread.cancel();
			mRadarThread = null;
		}
		if (list.size() > 0) {
			mRadarManager.loadImagesAsyn(list, this);
		}
	}
	
	@Override
	public void onResult(int result, List<RadarDto> images) {
		mHandler.sendEmptyMessage(HANDLER_LOAD_FINISHED);
		if (result == RadarListener.RESULT_SUCCESSED) {
			if (mRadarThread != null) {
				mRadarThread.cancel();
				mRadarThread = null;
			}
			if (images.size() > 0) {
				mRadarThread = new RadarThread(images);
				mRadarThread.start();
			}
		}
	}
	
	private class RadarThread extends Thread {
		static final int STATE_NONE = 0;
		static final int STATE_PLAYING = 1;
		static final int STATE_PAUSE = 2;
		static final int STATE_CANCEL = 3;
		private List<RadarDto> images;
		private int state;
		private int index;
		private int count;
		private boolean isTracking = false;
		
		public RadarThread(List<RadarDto> images) {
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
				
				if (mRadarThread != null) {
					mRadarThread.pause();
					
					Message message = mHandler.obtainMessage();
					message.what = HANDLER_PAUSE;
					mHandler.sendMessage(message);
					if (seekBar != null) {
						seekBar.setProgress(radarList.size());
					}
				}
			}else {
				RadarDto radar = images.get(index);
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

	@Override
	public void onProgress(String url, int progress) {
		Message msg = new Message();
		msg.obj = progress;
		msg.what = HANDLER_PROGRESS;
		mHandler.sendMessage(msg);
	}
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			int what = msg.what;
			switch (what) {
			case HANDLER_SHOW_RADAR: 
				if (msg.obj != null) {
					RadarDto radar = (RadarDto) msg.obj;
					if (radar != null) {
						Bitmap bitmap = BitmapFactory.decodeFile(radar.url);
						if (bitmap != null) {
							imageView.setImageBitmap(bitmap);
						}
					}
					changeProgress(radar.time, msg.arg2, msg.arg1);
					
					for (int i = 0; i < radarList.size(); i++) {
						if (i == msg.arg2) {
							radarList.get(msg.arg2).isSelect = selected;
						}else {
							radarList.get(i).isSelect = unselected;
						}
					}
					mAdapter.notifyDataSetChanged();
				}
				break;
			case HANDLER_PROGRESS: 
				if (dialog != null) {
					if (msg.obj != null) {
						int progress = (Integer) msg.obj;
						dialog.setPercent(progress);
					}
				}
				break;
			case HANDLER_LOAD_FINISHED: 
				dismissDialog();
				if (ivPlay != null) {
					ivPlay.setImageResource(R.drawable.iv_pause);
				}
				break;
			case HANDLER_PAUSE:
				if (ivPlay != null) {
					ivPlay.setImageResource(R.drawable.iv_play);
				}
				break;
			default:
				break;
			}
			
		};
	};
	
	private void changeProgress(String time, int progress, int max) {
		if (seekBar != null) {
			seekBar.setMax(max);
			seekBar.setProgress(progress);
		}
		tvTime.setText(time);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mRadarManager != null) {
			mRadarManager.onDestory();
		}
		if (mRadarThread != null) {
			mRadarThread.cancel();
			mRadarThread = null;
		}
	}
	
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.llBack) {
			finish();
		}else if (v.getId() == R.id.ivPlay) {
			if (mRadarThread != null && mRadarThread.getCurrentState() == RadarThread.STATE_PLAYING) {
				mRadarThread.pause();
				ivPlay.setImageResource(R.drawable.iv_play);
			} else if (mRadarThread != null && mRadarThread.getCurrentState() == RadarThread.STATE_PAUSE) {
				mRadarThread.play();
				ivPlay.setImageResource(R.drawable.iv_pause);
			} else if (mRadarThread == null) {
				initDialog();
				startDownLoadImgs(radarList);//开始下载
			}
		}else if (v.getId() == R.id.imageView) {
			for (int i = 0; i < radarList.size(); i++) {
				if (radarList.get(i).isSelect.equals("1")) {
					if (mRadarThread != null && mRadarThread.getCurrentState() == RadarThread.STATE_PLAYING) {
						mRadarThread.pause();
						ivPlay.setImageResource(R.drawable.iv_play);
					}
					
					Intent intent = new Intent(mContext, DisplayPictureActivity.class);
					intent.putExtra(CONST.WEB_URL, radarList.get(i).url);
					startActivity(intent);
					break;
				}
			}
		}
	}

}
