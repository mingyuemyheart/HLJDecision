package com.hlj.common;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.hlj.activity.WarningDetailActivity;
import com.hlj.activity.WebviewActivity;
import com.hlj.dto.WarningDto;
import com.hlj.utils.CrashHandler;
import com.iflytek.cloud.SpeechUtility;
import com.umeng.commonsdk.UMConfigure;
import com.umeng.message.IUmengCallback;
import com.umeng.message.IUmengRegisterCallback;
import com.umeng.message.PushAgent;
import com.umeng.message.UmengNotificationClickHandler;
import com.umeng.message.common.inter.ITagManager.Result;
import com.umeng.message.entity.UMessage;
import com.umeng.message.tag.TagManager;
import com.umeng.message.tag.TagManager.TCallBack;
import com.umeng.socialize.PlatformConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MyApplication extends Application{

	public static String appKey = "5755277767e58e5ca4000e07", msgSecret = "3464bbdf388960ddcea9c5cebf46cd66";//旧
//	public static String appKey = "5efe98800cafb240580000e2", msgSecret = "a03a519f1e2867391368d006baefd69f";//新
    private static PushAgent mPushAgent = null;
    private static TagManager tagManager = null;
	public static String DEVICETOKEN = "";

	private static String appTheme = "0";
	public static String getAppTheme() {
		return appTheme;
	}
	public static void setTheme(String theme) {
		appTheme = theme;
	}

	public static ArrayList<ColumnData> columnDataList = new ArrayList<>();

	public static long refreshTime = 5;//单位分钟

	@Override
	public void onCreate() {
		super.onCreate();
		getUserInfo(this);

		CrashHandler crashHandler = CrashHandler.getInstance();
		crashHandler.init(getApplicationContext());

		//科大讯飞
		SpeechUtility.createUtility(this, "appid=" + "5983c375");

		initUmeng();
	}

	/**
	 * 初始化umeng
	 */
	private void initUmeng() {
		//umeng分享
		UMConfigure.init(this, appKey, "umeng", UMConfigure.DEVICE_TYPE_PHONE, msgSecret);
		PlatformConfig.setWeixin("wx029d05ce279a188f", "7f2a515f4c85c0b180fc9894d6c7c80a");
		UMConfigure.setLogEnabled(true);

		registerUmengPush();
	}

	/**
	 * 注册umeng推送
	 */
	private void registerUmengPush() {
		mPushAgent = PushAgent.getInstance(this);
		//manifest里面的package最好与build.gradle中的applicationId保持一 致，如不一致，需调用setResourcePackageName设置资源文件包名
		mPushAgent.setResourcePackageName("shawn.cxwl.com.hlj");

		//参数number可以设置为0~10之间任意整数。当参数为0时，表示不合并通知
		mPushAgent.setDisplayNotificationNumber(0);

//        //sdk开启通知声音
//        mPushAgent.setNotificationPlaySound(MsgConstant.NOTIFICATION_PLAY_SDK_ENABLE);
//        // sdk关闭通知声音
//		mPushAgent.setNotificationPlaySound(MsgConstant.NOTIFICATION_PLAY_SDK_DISABLE);
//        // 通知声音由服务端控制
//		mPushAgent.setNotificationPlaySound(MsgConstant.NOTIFICATION_PLAY_SERVER);
//		mPushAgent.setNotificationPlayLights(MsgConstant.NOTIFICATION_PLAY_SDK_DISABLE);
//		mPushAgent.setNotificationPlayVibrate(MsgConstant.NOTIFICATION_PLAY_SDK_DISABLE);
//
//        //此处是完全自定义处理设置
//        mPushAgent.setPushIntentServiceClass(MyPushIntentService.class);

		//注册推送服务 每次调用register都会回调该接口
		mPushAgent.register(new IUmengRegisterCallback() {
			@Override
			public void onSuccess(String deviceToken) {
				Log.e("deviceToken", deviceToken);
				DEVICETOKEN = deviceToken;
				Intent intent = new Intent();
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				sendBroadcast(intent);
			}

			@Override
			public void onFailure(String s, String s1) {
			}
		});

		/**
		 * 自定义行为的回调处理
		 * UmengNotificationClickHandler是在BroadcastReceiver中被调用，故
		 * 如果需启动Activity，需添加Intent.FLAG_ACTIVITY_NEW_TASK
		 * */
		mPushAgent.setNotificationClickHandler(new UmengNotificationClickHandler() {
			@Override
			public void dealWithCustomAction(Context context, UMessage msg) {
				super.dealWithCustomAction(context, msg);
				if (msg.extra != null) {
					JSONObject obj = new JSONObject(msg.extra);
					try {
						if (!obj.isNull("show_type")) {
							String showType = obj.getString("show_type");
							if (TextUtils.equals(showType, "web")) {
								if (!obj.isNull("url")) {
									String url = obj.getString("url");
									if (!TextUtils.isEmpty(url)) {
										Intent intentDetail = new Intent(getApplicationContext(), WebviewActivity.class);
										intentDetail.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
										intentDetail.putExtra(CONST.ACTIVITY_NAME, "详情");
										intentDetail.putExtra(CONST.WEB_URL, url);
										startActivity(intentDetail);
									}
								}
							} else {
								if (!obj.isNull("url")) {
									String url = obj.getString("url");
									if (!TextUtils.isEmpty(url)) {
										WarningDto data = new WarningDto();
										data.html = url;
										Intent intentDetail = new Intent(getApplicationContext(), WarningDetailActivity.class);
										intentDetail.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
										Bundle bundle = new Bundle();
										bundle.putParcelable("data", data);
										intentDetail.putExtras(bundle);
										startActivity(intentDetail);
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
	 * 添加tags，添加多个tag以","隔开
	 * @param tags
	 */
	public static void resetTags(final String tags) {
		if (tagManager != null) {
			tagManager.addTags(new TCallBack() {
				@Override
				public void onMessage(boolean arg0, Result arg1) {
					Log.d("", "");
				}
			}, tags);
		}
	}
	
	/**
	 * 打开推送
	 */
	public static void enablePush() {
		if (mPushAgent != null) {
			mPushAgent.enable(new IUmengCallback() {
				@Override
				public void onSuccess() {
				}
				@Override
				public void onFailure(String arg0, String arg1) {
				}
			});
		}
	}
	
	/**
	 * 关闭推送
	 */
	public static void disablePush() {
		if (mPushAgent != null) {
			mPushAgent.disable(new IUmengCallback() {
				@Override
				public void onSuccess() {
				}
				@Override
				public void onFailure(String arg0, String arg1) {
				}
			});
		}
	}
	
	/**
	 * 设置推送消息声音
	 * @param context
	 * @param id R.raw.id
	 */
	public static void setSound(Context context, int id) {
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification mNotification = new Notification();
		mNotification.sound = Uri.parse("android.resource://" + context.getPackageName() + "/" + id);
		// mNotification.defaults = Notification.DEFAULT_SOUND;
		manager.notify(mNotification.icon, mNotification);
	}

	private static Map<String,Activity> destoryMap = new HashMap<>();
	/**
	 * 添加到销毁队列
	 * @param activity 要销毁的activity
	 */
	public static void addDestoryActivity(Activity activity, String activityName) {
		destoryMap.put(activityName,activity);
	}

	/**
	 *销毁指定Activity
	 */
	public static void destoryActivity() {
		Set<String> keySet=destoryMap.keySet();
		for (String key:keySet){
			destoryMap.get(key).finish();
		}
	}

	//本地保存用户信息参数
	public static String USERINFO = "userInfo";//userInfo sharedPreferance名称
	public static class UserInfo {
		public static final String uId = "uId";
		public static final String userName = "uName";
		public static final String passWord = "pwd";
		public static final String token = "token";
		public static final String groupId = "groupId";
		public static final String uGroupName = "uGroupName";
		public static final String name = "name";
		public static final String department = "department";
		public static final String mobile = "mobile";
		public static final String jc_download = "jc_download";
	}

	public static String UID = "2606";//用户id
	public static String USERNAME = CONST.publicUser;//用户名
	public static String PASSWORD = CONST.publicPwd;//用户密码
	public static String TOKEN = "";//token
	public static String GROUPID = "50";
	public static String UGROUPNAME = "";//uGroupName
	public static String NAME = "";
	public static String DEPARTMENT = "";
	public static String MOBILE = "";
	public static String JC_DOWNLOAD = "";//1才有下载权限

	public static void getUserInfo(Context context) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(USERINFO, Context.MODE_PRIVATE);
		UID = sharedPreferences.getString(UserInfo.uId, UID);
		USERNAME = sharedPreferences.getString(UserInfo.userName, USERNAME);
		PASSWORD = sharedPreferences.getString(UserInfo.passWord, PASSWORD);
		TOKEN = sharedPreferences.getString(UserInfo.token, TOKEN);
		GROUPID = sharedPreferences.getString(UserInfo.groupId, GROUPID);
		UGROUPNAME = sharedPreferences.getString(UserInfo.uGroupName, UGROUPNAME);
		NAME = sharedPreferences.getString(UserInfo.name, NAME);
		DEPARTMENT = sharedPreferences.getString(UserInfo.department, DEPARTMENT);
		MOBILE = sharedPreferences.getString(UserInfo.mobile, MOBILE);
		JC_DOWNLOAD = sharedPreferences.getString(UserInfo.jc_download, JC_DOWNLOAD);
	}

	public static void saveUserInfo(Context context) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(USERINFO, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString(UserInfo.uId, UID);
		editor.putString(UserInfo.userName, USERNAME);
		editor.putString(UserInfo.passWord, PASSWORD);
		editor.putString(UserInfo.token, TOKEN);
		editor.putString(UserInfo.groupId, GROUPID);
		editor.putString(UserInfo.uGroupName, UGROUPNAME);
		editor.putString(UserInfo.name, NAME);
		editor.putString(UserInfo.department, DEPARTMENT);
		editor.putString(UserInfo.mobile, MOBILE);
		editor.putString(UserInfo.jc_download, JC_DOWNLOAD);
		editor.apply();
	}

	public static void clearUserInfo(Context context) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(USERINFO, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.clear();
		editor.apply();
		UID = "2606";
		USERNAME = CONST.publicUser;
		PASSWORD = CONST.publicPwd;
		TOKEN = "";
		GROUPID = "50";
		UGROUPNAME = "";
		NAME = "";
		DEPARTMENT = "";
		MOBILE = "";
		JC_DOWNLOAD = "";
	}

	/**
	 * 保存columnIds
	 */
	public static void saveColumnIds(Context context, String columnIds) {
		SharedPreferences sharedPreferences = context.getSharedPreferences("COLUMNIDS", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("columnIds", columnIds);
		editor.apply();
	}

	/**
	 * 获取columnIds
	 */
	public static String getColumnIds(Context context) {
		SharedPreferences sharedPreferences = context.getSharedPreferences("COLUMNIDS", Context.MODE_PRIVATE);
		return sharedPreferences.getString("columnIds", "");
	}

}
