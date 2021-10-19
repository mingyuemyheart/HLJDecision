package com.hlj.activity;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hlj.adapter.BaseViewPagerAdapter;
import com.hlj.common.CONST;
import com.hlj.dto.WeatherMeetingDto;
import com.hlj.fragment.WeatherMeetingFragment;
import com.hlj.utils.CommonUtil;
import com.hlj.view.MainViewPager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import shawn.cxwl.com.hlj.R;

/**
 * 天气会商
 * @author shawn_sun
 */
public class ShawnWeatherMeetingActivity extends BaseFragmentActivity implements OnClickListener{
	
	private Context mContext = this;
	private LinearLayout llContainer = null;
	private LinearLayout llContainer1 = null;
	private MainViewPager viewPager = null;
	private ArrayList<Fragment> fragments = new ArrayList<>();
	private List<WeatherMeetingDto> dataList = new ArrayList<>();

	private String videoUrl1 = "http://10.0.86.110/rest/QxjRestService/getVideoList";
	private String videoUrl2 = "http://106.120.82.240/rest/QxjRestService/getVideoList";
	//	private String videoUrl3 = "http://111.205.114.31/rest/QxjRestService/getVideoList";
	private String publicIp = "106.120.82.240";//公网ip
	private List<WeatherMeetingDto> videoList = new ArrayList<>();//点播视频列表

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.shawn_activity_weather_meeting);
		mContext = this;
		showDialog();
		initWidget();
	}

	private void initWidget() {
		LinearLayout llBack = findViewById(R.id.llBack);
		llBack.setOnClickListener(this);
		TextView tvTitle = findViewById(R.id.tvTitle);
		llContainer = findViewById(R.id.llContainer);
		llContainer1 = findViewById(R.id.llContainer1);
		
		String title = getIntent().getStringExtra(CONST.ACTIVITY_NAME);
		if (title != null) {
			tvTitle.setText(title);
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					OkHttpVideoList(videoUrl1);
				} catch (Exception e) {
					e.printStackTrace();
					try {
						OkHttpVideoList(videoUrl2);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
		}).start();

		String columnId = getIntent().getStringExtra(CONST.COLUMN_ID);
		CommonUtil.submitClickCount(columnId, title);

	}

	/**
	 * 初始化viewPager
	 */
	private void initViewPager(List<WeatherMeetingDto> list) {
		if (list.size() <= 1) {
			llContainer.setVisibility(View.GONE);
			llContainer1.setVisibility(View.GONE);
		}
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		llContainer.removeAllViews();
		llContainer1.removeAllViews();
		for (int i = 0; i < list.size(); i++) {
			WeatherMeetingDto dto = list.get(i);

			TextView tvName = new TextView(mContext);
			tvName.setGravity(Gravity.CENTER);
			tvName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
			tvName.setPadding(0, (int)(dm.density*10), 0, (int)(dm.density*10));
			tvName.setOnClickListener(new MyOnClickListener(i));
			tvName.setTextColor(Color.WHITE);
			if (!TextUtils.isEmpty(dto.columnName)) {
				tvName.setText(dto.columnName);
			}
			if (i == 0) {
				tvName.setTextColor(getResources().getColor(R.color.colorPrimary));
			}else {
				tvName.setTextColor(getResources().getColor(R.color.text_color4));
			}
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			params.weight = 1.0f;
			tvName.setLayoutParams(params);
			llContainer.addView(tvName, i);

			TextView tvBar = new TextView(mContext);
			tvBar.setGravity(Gravity.CENTER);
			tvBar.setOnClickListener(new MyOnClickListener(i));
			if (i == 0) {
				tvBar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
			}else {
				tvBar.setBackgroundColor(getResources().getColor(R.color.transparent));
			}
			LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			params1.weight = 1.0f;
			params1.height = (int) (dm.density*2);
			params1.leftMargin = (int) (dm.density*50);
			params1.rightMargin = (int) (dm.density*50);
			tvBar.setLayoutParams(params1);
			llContainer1.addView(tvBar, i);

			WeatherMeetingFragment fragment = new WeatherMeetingFragment();
			Bundle bundle = new Bundle();
			bundle.putInt("index", i);
			bundle.putParcelable("data", dto);
			bundle.putParcelableArrayList("videoList", (ArrayList<? extends Parcelable>) videoList);
			fragment.setArguments(bundle);
			fragments.add(fragment);
		}

		viewPager = findViewById(R.id.viewPager);
		viewPager.setSlipping(true);//设置ViewPager是否可以滑动
		viewPager.setOffscreenPageLimit(fragments.size());
		viewPager.setOnPageChangeListener(new MyOnPageChangeListener());
		viewPager.setAdapter(new BaseViewPagerAdapter(getSupportFragmentManager(), fragments));
	}

	public class MyOnPageChangeListener implements ViewPager.OnPageChangeListener {
		@Override
		public void onPageSelected(int arg0) {
			if (llContainer != null) {
				for (int i = 0; i < llContainer.getChildCount(); i++) {
					TextView tvName = (TextView) llContainer.getChildAt(i);
					if (i == arg0) {
						tvName.setTextColor(getResources().getColor(R.color.colorPrimary));
					}else {
						tvName.setTextColor(getResources().getColor(R.color.transparent));
					}
				}
			}

			if (llContainer1 != null) {
				for (int i = 0; i < llContainer1.getChildCount(); i++) {
					TextView tvBar = (TextView) llContainer1.getChildAt(i);
					if (i == arg0) {
						tvBar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
					}else {
						tvBar.setBackgroundColor(getResources().getColor(R.color.transparent));
					}
				}
			}
		}
		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}
		@Override
		public void onPageScrollStateChanged(int arg0) {
		}
	}

	/**
	 * 头标点击监听
	 * @author shawn_sun
	 */
	private class MyOnClickListener implements OnClickListener {

		private int index;

		private MyOnClickListener(int i) {
			index = i;
		}

		@Override
		public void onClick(View v) {
			if (viewPager != null) {
				viewPager.setCurrentItem(index, true);
			}
		}
	}

	private final OkHttpClient.Builder builder = new OkHttpClient.Builder().connectTimeout(1, TimeUnit.SECONDS);
	private final OkHttpClient okHttpClient = builder.build();

	//获取点播视频列表
	private void OkHttpVideoList(String url) throws IOException{
		final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

		JSONObject obj = new JSONObject();
		try {
			obj.put("coId", "10001");
			obj.put("index", "1");
			obj.put("pageOff", "50");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		String content = obj.toString();

		RequestBody body = RequestBody.create(JSON, content);
		Request request = new Request.Builder().post(body).url(url).build();
		Response response = okHttpClient.newCall(request).execute();
		if (response.isSuccessful()) {
			String result = response.body().string();
			parseData(result, url);
		} else {
			throw new IOException("Unexpected code " + response);
		}
	}

	private void parseData(final String result, final String url) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (result != null) {
					try {
						JSONObject obj = new JSONObject(result);
						if (!obj.isNull("data")) {
							videoList.clear();
							JSONArray array = obj.getJSONArray("data");

							int length = array.length();
							if (length > 30) {
								length = 30;
							}
							for (int i = 0; i < length; i++) {
								JSONObject itemObj = array.getJSONObject(i);
								WeatherMeetingDto dto = new WeatherMeetingDto();
								if (!itemObj.isNull("videoImgs")) {
									dto.videoImgs = itemObj.getString("videoImgs");
								}
								if (!itemObj.isNull("videoTime")) {
									dto.videoTime = itemObj.getString("videoTime");
								}
								if (!itemObj.isNull("resAddr")) {
									if (url.contains(publicIp)) {
										String addr = itemObj.getString("resAddr");
										if (addr.contains("http://")) {
											addr = addr.substring("http://".length(), addr.length());
											addr = addr.replace(addr.substring(0, addr.indexOf("/")), publicIp);
											dto.hlsAddress = "http://"+addr;
										}
									}else {
										dto.hlsAddress = itemObj.getString("resAddr");
									}
								}
								videoList.add(dto);
							}
						}

						cancelDialog();

						WeatherMeetingDto dto = new WeatherMeetingDto();
						dto.columnName = "本周安排";
						dto.columnUrl = "http://decision-admin.tianqi.cn/Home/extra/getDecisionHsap";
						dataList.add(dto);
						dto = new WeatherMeetingDto();
						dto.columnName = "下周安排";
						dto.columnUrl = "http://decision-admin.tianqi.cn/Home/extra/getDecisionHsap";
						dataList.add(dto);

						initViewPager(dataList);

					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.llBack:
			finish();
			break;

		default:
			break;
		}
	}

}
