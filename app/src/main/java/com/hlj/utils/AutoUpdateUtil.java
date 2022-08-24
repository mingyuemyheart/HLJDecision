package com.hlj.utils;
/**
 * 自动更新提示、下载
 * @author shawn_sun
 *
 */

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import shawn.cxwl.com.hlj.R;

public class AutoUpdateUtil {

	private static Context mContext = null;
	private static String appName = null;
	private static boolean flag = true;

	/**
	 * 获取版本号
	 * @return 当前应用的版本号
	 */
	public static int getVersionCode(Context context) {
		try {
			PackageManager manager = context.getPackageManager();
			PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
			return info.versionCode;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * 检查更新
	 * @param context
	 * @param app_id
	 * @param is_flag true为主界面自己请求，false为个人点击获取
	 */
	public static void checkUpdate(final Activity activity, Context context, String app_id, String app_name, boolean is_flag) {
		mContext = context;
		appName = app_name;
		flag = is_flag;
		if (TextUtils.isEmpty(app_id)) {
			Toast.makeText(mContext, "The app_id is empty", Toast.LENGTH_SHORT).show();
			return;
		}
		String url = "https://app.tianqi.cn/update/check";
		FormBody.Builder builder = new FormBody.Builder();
		builder.add("app_id", app_id);
		RequestBody body = builder.build();
		OkHttpUtil.enqueue(new okhttp3.Request.Builder().post(body).url(url).build(), new Callback() {
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
						UpdateDto dto = new UpdateDto();
						if (!obj.isNull("version")) {
							dto.version = obj.getString("version");
						}
						if (!obj.isNull("update_info")) {
							dto.update_info = obj.getString("update_info");
						}
						if (!obj.isNull("dl_url")) {
							dto.dl_url = obj.getString("dl_url");
						}
						if (!obj.isNull("versionCode")) {
							dto.versionCode = obj.getInt("versionCode");
						}

						//检查版本不一样时候才更新
						if (dto.versionCode > getVersionCode(mContext)) {
							Message msg = new Message();
							msg.what = 1000;
							msg.obj = dto;
							handler.sendMessage(msg);
						}else {
							if (flag == false) {
								activity.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										Toast.makeText(mContext, "已经是最新版本", Toast.LENGTH_SHORT).show();
									}
								});
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	private static class UpdateDto {
		public String version = "";
		public String update_info = "";
		public String dl_url = "";
		public int versionCode = 0;
	}

	private static Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case 1000:
					UpdateDto dto = (UpdateDto) msg.obj;
					updateDialog(dto);
					break;

				default:
					break;
			}
		};
	};

	private static void updateDialog(final UpdateDto dto) {
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.auto_update_dialog, null);
		TextView tvTitle = (TextView) view.findViewById(R.id.tvTitle);
		TextView tvVersion = (TextView) view.findViewById(R.id.tvVersion);
		TextView tvContent = (TextView) view.findViewById(R.id.tvContent);
		LinearLayout llNegative = (LinearLayout) view.findViewById(R.id.llNegative);
		LinearLayout llPositive = (LinearLayout) view.findViewById(R.id.llPositive);

		final Dialog dialog = new Dialog(mContext, R.style.CustomProgressDialog);
		dialog.setContentView(view);
		dialog.show();

		tvTitle.setText("更新提醒");
		if (!TextUtils.isEmpty(dto.version)) {
			tvVersion.setText("更新版本："+dto.version);
		}
		if (!TextUtils.isEmpty(dto.update_info)) {
			tvContent.setText(dto.update_info);
		}
		llNegative.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				dialog.dismiss();
			}
		});

		llPositive.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				dialog.dismiss();
				if (!TextUtils.isEmpty(dto.dl_url)) {
					new Thread() {
						public void run() {
							intoDownloadManager(dto.dl_url);
						}
					}.start();
				}
			}
		});
	}

	private static void intoDownloadManager(String dl_url) {
		DownloadManager dManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
		Uri uri = Uri.parse(dl_url);
		Request request = new Request(uri);
		// 设置下载路径和文件名
		String filename = dl_url.substring(dl_url.lastIndexOf("/") + 1);//获取文件名称
		String filePath = Environment.getExternalStorageDirectory()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+filename;
		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
		request.setDescription(appName);
		request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		request.setMimeType("application/vnd.android.package-archive");
		// 设置为可被媒体扫描器找到
		request.allowScanningByMediaScanner();
		// 设置为可见和可管理
		request.setVisibleInDownloadsUi(true);
		long referneceId = dManager.enqueue(request);
//		// 把当前下载的ID保存起来
		SharedPreferences sPreferences = mContext.getSharedPreferences("downloadplato", 0);
		sPreferences.edit().putLong("plato", referneceId).apply();
		initBroadCast(mContext, referneceId, filePath);
	}

	/**
	 * 注册广播监听系统的下载完成事件
	 */
	private static void initBroadCast(Context context, final long referneceId, final String filePath) {
		BroadcastReceiver mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
				if (id == referneceId) {
					intent = new Intent(Intent.ACTION_VIEW);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
						intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName()+".FileProvider", new File(filePath));
						intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
					} else {
						intent.setDataAndType(Uri.fromFile(new File(filePath)), "application/vnd.android.package-archive");
					}
					context.startActivity(intent);
				}

			}
		};
		IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		context.registerReceiver(mReceiver, intentFilter);
	}

}

