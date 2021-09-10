package com.hlj.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hlj.adapter.MyPagerAdapter;
import com.hlj.common.CONST;
import com.hlj.common.ColumnData;
import com.hlj.common.MyApplication;
import com.hlj.fragment.CommonListFragment;
import com.hlj.fragment.ContactUsFragment;
import com.hlj.fragment.ForecastFragment;
import com.hlj.fragment.WarningFragment;
import com.hlj.fragment.WeatherFactFragment;
import com.hlj.fragment.WebviewFragment;
import com.hlj.manager.SystemStatusManager;
import com.hlj.utils.AuthorityUtil;
import com.hlj.utils.AutoUpdateUtil;
import com.hlj.utils.CommonUtil;
import com.hlj.view.MainViewPager;

import java.util.ArrayList;
import java.util.List;

import shawn.cxwl.com.hlj.R;

public class MainActivity extends BaseFragmentActivity implements OnClickListener {
	
	private Context mContext = null;
	private MainViewPager viewPager = null;
	private LinearLayout llContainer,llContainer1;
	private HorizontalScrollView hScrollView1;
	private List<Fragment> fragments = new ArrayList<>();
	private long mExitTime;//记录点击完返回按钮后的long型时间
	private int columnWidth = 0;
	private String BROADCAST_ACTION_NAME = "";//所有fragment广播名字

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		MyApplication.addDestoryActivity(this, "MainActivity");
        mContext = this;
		setTranslucentStatus();
		checkMultiAuthority();
    }

	/**
	 * 设置状态栏背景状态
	 */
	private void setTranslucentStatus() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			Window win = getWindow();
			WindowManager.LayoutParams winParams = win.getAttributes();
			final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
			winParams.flags |= bits;
			win.setAttributes(winParams);
		}
		SystemStatusManager tintManager = new SystemStatusManager(this);
		tintManager.setStatusBarTintEnabled(true);
		tintManager.setStatusBarTintResource(0);// 状态栏无背景
	}

    private void init() {
		initWidget();
		initViewPager();
	}

	/**
	 * 初始化控件
	 */
	private void initWidget() {
		AutoUpdateUtil.checkUpdate(MainActivity.this, mContext, "41", getString(R.string.app_name), true);//黑龙江气象
//		AutoUpdateUtil.checkUpdate(MainActivity.this, mContext, "53", getString(R.string.app_name), true);//决策气象服务

		ConstraintLayout clMain = findViewById(R.id.clMain);
		ImageView ivSetting = findViewById(R.id.ivSetting);
		ivSetting.setOnClickListener(this);
		llContainer = findViewById(R.id.llContainer);
		llContainer1 = findViewById(R.id.llContainer1);
		hScrollView1 = findViewById(R.id.hScrollView1);

		if (TextUtils.equals(MyApplication.getAppTheme(), "1")) {
			clMain.setBackgroundColor(Color.BLACK);
			ivSetting.setImageBitmap(CommonUtil.grayScaleImage(BitmapFactory.decodeResource(getResources(), R.drawable.icon_setting)));
		}

		//是否显示登录对话框
		SharedPreferences sp = getSharedPreferences("LOGINDIALOG", Context.MODE_PRIVATE);
		boolean isFirst = sp.getBoolean("isFirst", true);
		if (isFirst) {
			firstLoginDialog(sp);
		}
	}

	/**
	 * 第一次登陆
	 */
	private void firstLoginDialog(final SharedPreferences sp) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.dialog_first_login, null);
		TextView tvSure = view.findViewById(R.id.tvSure);

		final Dialog dialog = new Dialog(mContext, R.style.CustomProgressDialog);
		dialog.setContentView(view);
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();

		tvSure.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				dialog.dismiss();
				SharedPreferences.Editor editor = sp.edit();
				editor.putBoolean("isFirst", false);
				editor.apply();
			}
		});
	}

	/**
	 * 初始化viewpager
	 */
	private void initViewPager() {
		int columnSize = MyApplication.columnDataList.size();
		if (columnSize <= 1) {
			llContainer.setVisibility(View.GONE);
			llContainer1.setVisibility(View.GONE);
		}

		llContainer.removeAllViews();
		llContainer1.removeAllViews();
		for (int i = 0; i < columnSize; i++) {
			ColumnData channel = MyApplication.columnDataList.get(i);

			TextView tvName = new TextView(mContext);
			tvName.setGravity(Gravity.CENTER);
			tvName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
			tvName.setPadding(0, (int)(CommonUtil.dip2px(mContext, 6)), 0, (int)(CommonUtil.dip2px(mContext, 6)));
			tvName.setOnClickListener(new MyOnClickListener(i));
			tvName.setTextColor(getResources().getColor(R.color.white));
			if (!TextUtils.isEmpty(channel.name)) {
				tvName.setText(channel.name);
			}
			tvName.measure(0, 0);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			params.width = tvName.getMeasuredWidth();
			columnWidth += params.width;
			params.setMargins((int)(CommonUtil.dip2px(mContext, 10)), 0, (int)(CommonUtil.dip2px(mContext, 10)), 0);
			tvName.setLayoutParams(params);
			llContainer.addView(tvName, i);

			TextView tvBar = new TextView(mContext);
			tvBar.setGravity(Gravity.CENTER);
			tvBar.setOnClickListener(new MyOnClickListener(i));
			if (i == 0) {
				tvBar.setBackgroundColor(getResources().getColor(R.color.white));
			}else {
				tvBar.setBackgroundColor(getResources().getColor(R.color.transparent));
			}
			LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			params1.width = tvName.getMeasuredWidth();
			params1.height = (int)(CommonUtil.dip2px(mContext, 3));
			params1.setMargins((int)(CommonUtil.dip2px(mContext, 10)), 0, (int)(CommonUtil.dip2px(mContext, 10)), 0);
			tvBar.setLayoutParams(params1);
			llContainer1.addView(tvBar, i);

			Fragment fragment;
			String showType = channel.showType;
			if (TextUtils.equals(showType, CONST.LOCAL)) {
				String id = channel.id;
				if (TextUtils.equals(id, "1")) {
					fragment = new ForecastFragment();//首页
				} else if (TextUtils.equals(id, "5")) {
					fragment = new WarningFragment();//天气预警
				} else if (TextUtils.equals(id, "2") || TextUtils.equals(id, "3") || TextUtils.equals(id, "4")
						|| TextUtils.equals(id, "10") || TextUtils.equals(id, "7")
						|| TextUtils.equals(id, "8") || TextUtils.equals(id, "13")
						|| TextUtils.equals(id, "11") || TextUtils.equals(id, "14") || TextUtils.equals(id, "15")) {
					fragment = new WeatherFactFragment();//天气实况、天气预报、科普宣传、电力气象服务、铁路气象服务、人工影响天气、森林防火、农业气象
				} else if (TextUtils.equals(id, "106")) {
					fragment = new WebviewFragment();//旅游气象
				} else if (TextUtils.equals(id, "12")) {
					fragment = new ContactUsFragment();//联系我们
				} else {
					fragment = new CommonListFragment();
				}
			} else if (TextUtils.equals(showType, CONST.NEWS)) {
				fragment = new CommonListFragment();
			} else {
				fragment = new CommonListFragment();
			}

			if (fragment != null) {
				Bundle bundle = new Bundle();
				bundle.putString(CONST.BROADCAST_ACTION, fragment.getClass().getName()+channel.name);
				bundle.putString(CONST.COLUMN_ID, channel.columnId);
				bundle.putString(CONST.ACTIVITY_NAME, channel.name);
				bundle.putString(CONST.WEB_URL, channel.dataUrl);
				bundle.putString(CONST.LOCAL_ID, channel.id);
				bundle.putParcelable("data", channel);
				fragment.setArguments(bundle);
				fragments.add(fragment);
			}
		}

		viewPager = findViewById(R.id.viewPager);
		viewPager.setSlipping(false);//设置ViewPager是否可以滑动
		viewPager.setOffscreenPageLimit(fragments.size());
		viewPager.setOnPageChangeListener(new MyOnPageChangeListener());
		viewPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager(), fragments));
	}
	
	public class MyOnPageChangeListener implements OnPageChangeListener {
		@Override
		public void onPageSelected(int arg0) {
			if (llContainer != null) {
				hScrollView1.smoothScrollTo(columnWidth/llContainer.getChildCount()*arg0, 0);
				for (int i = 0; i < llContainer.getChildCount(); i++) {
					TextView tvName = (TextView) llContainer.getChildAt(i);
					if (i == arg0) {
						String actionName = fragments.get(arg0).getClass().getName()+tvName.getText().toString();
						if (!BROADCAST_ACTION_NAME.contains(actionName)) {
							Intent intent = new Intent();
							intent.setAction(actionName);
							sendBroadcast(intent);
							BROADCAST_ACTION_NAME += actionName;
						}
					}
				}
			}
			if (llContainer1 != null) {
				for (int i = 0; i < llContainer1.getChildCount(); i++) {
					TextView tvBar = (TextView) llContainer1.getChildAt(i);
					if (i == arg0) {
						tvBar.setBackgroundColor(getResources().getColor(R.color.white));
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
    
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if ((System.currentTimeMillis() - mExitTime) > 2000) {
				Toast.makeText(mContext, getString(R.string.confirm_exit)+getString(R.string.app_name), Toast.LENGTH_SHORT).show();
				mExitTime = System.currentTimeMillis();
			} else {
				finish();
			}
		}
		return false;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ivSetting:
			startActivity(new Intent(mContext, SettingActivity.class));
			break;
		default:
			break;
		}
	}
	
	//需要申请的所有权限
	private String[] allPermissions = new String[] {
			Manifest.permission.ACCESS_FINE_LOCATION,
			Manifest.permission.ACCESS_COARSE_LOCATION,
			Manifest.permission.READ_PHONE_STATE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.CALL_PHONE
	};

	//拒绝的权限集合
	private static List<String> deniedList = new ArrayList<>();
	/**
	 * 申请定位权限
	 */
	private void checkMultiAuthority() {
		if (Build.VERSION.SDK_INT < 23) {
			init();
		}else {
			deniedList.clear();
			for (String permission : allPermissions) {
				if (ContextCompat.checkSelfPermission(mContext, permission) != PackageManager.PERMISSION_GRANTED) {
					deniedList.add(permission);
				}
			}
			if (deniedList.isEmpty()) {//所有权限都授予
				init();
			}else {
				String[] permissions = deniedList.toArray(new String[deniedList.size()]);//将list转成数组
				ActivityCompat.requestPermissions(this, permissions, AuthorityUtil.AUTHOR_MULTI);
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
			case AuthorityUtil.AUTHOR_MULTI:
				if (grantResults.length > 0) {
					boolean isAllGranted = true;//是否全部授权
					for (int gResult : grantResults) {
						if (gResult != PackageManager.PERMISSION_GRANTED) {
							isAllGranted = false;
							break;
						}
					}
					if (isAllGranted) {//所有权限都授予
						init();
					}else {//只要有一个没有授权，就提示进入设置界面设置
						AuthorityUtil.intentAuthorSetting(mContext, "\""+getString(R.string.app_name)+"\""+"需要使用您的位置权限、设备信息权限、存储权限、电话权限，是否前往设置？");
					}
				}else {
					for (String permission : permissions) {
						if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
							AuthorityUtil.intentAuthorSetting(mContext, "\""+getString(R.string.app_name)+"\""+"需要使用您的位置权限、设备信息权限、存储权限、电话权限，是否前往设置？");
							break;
						}
					}
				}
				break;
		}
	}

}
