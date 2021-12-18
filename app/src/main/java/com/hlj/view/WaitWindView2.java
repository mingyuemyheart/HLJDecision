package com.hlj.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import com.amap.api.maps.model.LatLng;
import com.hlj.activity.WaitWindActivity;
import com.hlj.dto.WindData;
import com.hlj.dto.WindDto;
import com.hlj.utils.CommonUtil;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class WaitWindView2 extends View {

	private Paint paint;
	private float width = 0f, height = 0f;//手机屏幕宽高
	private List<WindDto> particles = new ArrayList<>();//存放随机点的list
	private int time = 60;//ms,刷新画布时间
	private int maxLife = 100;//长度，粒子的最大生命周期
	private Bitmap bitmap;//每一帧图像承载对象
	private Canvas tempCanvas;
	private WindThread mThread;
	private WaitWindActivity activity;
	private WindData windData;
	private List<ImageView> images = new ArrayList<>();//存放位图的list
	private float zoom;

	//高配
	private int partileCount = 1200;//绘制粒子个数
	private int frameCount = 10;//帧数
	private float speedRate = 1.0f;//离子运动速度系数

	public WaitWindView2(Context context) {
		super(context);
	}

	public WaitWindView2(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public WaitWindView2(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void init(WaitWindActivity activity) {
		this.activity = activity;

		width = CommonUtil.widthPixels(activity);
		height = CommonUtil.heightPixels(activity);

		paint = new Paint();
		paint.setColor(Color.WHITE);
		paint.setAntiAlias(true);
		paint.setStrokeWidth(4f);

		float totalMemory = getTotalMemorySize()/1024/1024;//手机内存大小(G)
		if (totalMemory <= 3.0) {//内存小于等于2G
			partileCount = 600;//绘制粒子个数
			frameCount = 10;//帧数
			speedRate = 1.0f;//离子运动速度系数
		}else if (totalMemory > 3.0 && totalMemory <= 4.0) {//内存小于等于3G
			partileCount = 800;//绘制粒子个数
			frameCount = 10;//帧数
			speedRate = 1.0f;//离子运动速度系数
		}else if (totalMemory > 4.0) {//内存大于3G
			partileCount = 1000;//绘制粒子个数
			frameCount = 10;//帧数
			speedRate = 1.0f;//离子运动速度系数
		}

//		float curFreq = Float.parseFloat(getMaxCpuFreq());
//		if (curFreq <= 1000000) {
//			partileCount = 400;//绘制粒子个数
//			frameCount = 6;//帧数
//			speedRate = 1.8f;//离子运动速度系数
//		}else if (curFreq > 1000000 && curFreq <= 1200000) {
//			partileCount = 600;//绘制粒子个数
//			frameCount = 8;//帧数
//			speedRate = 1.5f;//离子运动速度系数
//		}else if (curFreq > 1200000 && curFreq <= 1500000) {
//			partileCount = 1000;//绘制粒子个数
//			frameCount = 8;//帧数
//			speedRate = 1.2f;//离子运动速度系数
//		}else if (curFreq > 1500000) {
//			partileCount = 1500;//绘制粒子个数
//			frameCount = 10;//帧数
//			speedRate = 1.0f;//离子运动速度系数
//		}
//		Log.e("curFreq", curFreq+"");

		getParticleInfo();
	}

	public void setData(WindData windData, float zoom) {
		this.zoom = zoom;
		this.windData = windData;
	}

	public void start() {
		startThread();
	}

	/**
	 * 获取随机里的坐标信息
	 */
	private void getParticleInfo() {
		particles.clear();
		for (int i = 0; i < partileCount; i++) {
			particles.add(new WindDto());
		}
	}

	/**
	 * 开始绘制粒子线程
	 */
	private void startThread() {
		activity.container2.removeAllViews();

		if (bitmap != null) {
			if (!bitmap.isRecycled()) {
				bitmap.recycle();
			}
			bitmap = null;
			System.gc();
		}

		if (mThread != null) {
			mThread.cancel();
			mThread = null;
		}
		mThread = new WindThread();
		mThread.start();
	}

	private class WindThread extends Thread {
		static final int STATE_START = 0;
		static final int STATE_CANCEL = 1;
		private int state;

		@Override
		public void run() {
			super.run();
			this.state = STATE_START;
			while (true) {
				if (state == STATE_CANCEL) {
					break;
				}
				try {
					calculatePoints();
					Thread.sleep(time);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				postInvalidate();
			}
		}

		public void cancel() {
			this.state = STATE_CANCEL;
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		try {
			bitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
			if (tempCanvas != null) {
				tempCanvas = null;
			}
			tempCanvas = new Canvas(bitmap);

			ImageView image = new ImageView(getContext());
			image.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			image.setImageBitmap(bitmap);
			activity.container2.addView(image);
			images.add(0, image);

			int imageSize = images.size();
			for (int i = imageSize-1; i >= 0; i--) {
				ImageView iv = images.get(i);
				BigDecimal bd = new BigDecimal(1 - 1.0f*i/imageSize);
				iv.setAlpha(bd.setScale(1, BigDecimal.ROUND_HALF_UP).floatValue());
			}
			if (imageSize >= frameCount) {
				ImageView imageView = images.get(imageSize-1);
				images.remove(imageView);
				activity.container2.removeView(imageView);
			}
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void calculatePoints(){
		for (WindDto dto : particles) {
			if (dto.life <= 0) {
				float x = (float)(Math.random()*width);
				float y = (float)(Math.random()*height);
				double longDetla = windData.latLngEnd.longitude - windData.latLngStart.longitude;
				if (Math.abs(windData.latLngEnd.longitude - windData.latLngStart.longitude) > 180) {
					longDetla = (windData.latLngEnd.longitude - (-180)) + (180 - windData.latLngStart.longitude);
				}
				double lng = (longDetla)*x/width+windData.latLngStart.longitude;
				if (lng > 180) {
					lng = lng - 360;
				}
				if (lng <= windData.x0) {
					lng = windData.x1 - windData.x0 + lng;
				}
				double lat = (windData.latLngEnd.latitude - windData.latLngStart.latitude)*y/height+windData.latLngStart.latitude;
				if (zoom <= 3.0f) {
					lat = lat+zoom*4.2;
				} else if (zoom <= 4.0f) {
					lat = lat+zoom*1.5;
				} else if (zoom <= 5.0f) {
					lat = lat+zoom*0.6;
				} else {
					lat = lat-zoom/(zoom+220);
				}
				LatLng latLng = new LatLng(lat, lng);
				double[] p = getVector(latLng);
				int life = (int)(Math.random()*maxLife);

				double m = Math.sqrt(p[0]*p[0] + p[1]*p[1]);
				if (m < 1) {
					dto.life = 0;
				}else {
					dto.oldX = -1;
					dto.oldY = -1;
					dto.x = x;
					dto.y = y;
					dto.vx = (float) p[0];
					dto.vy = (float) p[1];
					dto.latLng = latLng;
					dto.life = life;
				}
			}else {
				float x = dto.x + dto.vx;
				float y = dto.y - dto.vy;
				double longDetla = windData.latLngEnd.longitude - windData.latLngStart.longitude;
				if (Math.abs(windData.latLngEnd.longitude - windData.latLngStart.longitude) > 180) {
					longDetla = (windData.latLngEnd.longitude - (-180)) + (180 - windData.latLngStart.longitude);
				}
				double lng = (longDetla)*x/width+windData.latLngStart.longitude;
				if (lng > 180) {
					lng = lng - 360;
				}
				if (lng <= windData.x0) {
					lng = windData.x1 - windData.x0 + lng;
				}
				double lat = (windData.latLngEnd.latitude - windData.latLngStart.latitude)*y/height+windData.latLngStart.latitude;
				if (zoom <= 3.0f) {
					lat = lat+zoom*4.2;
				} else if (zoom <= 4.0f) {
					lat = lat+zoom*1.5;
				} else if (zoom <= 5.0f) {
					lat = lat+zoom*0.6;
				} else {
					lat = lat-zoom/(zoom+220);
				}
				LatLng latLng = new LatLng(lat, lng);
				double[] p = getVector(latLng);

				double m = Math.sqrt(p[0]*p[0] + p[1]*p[1]);
				if (m < 1) {
					dto.life = 0;
				}else {
					dto.oldX = dto.x;
					dto.oldY = dto.y;
					dto.x = x;
					dto.y = y;
					dto.life = dto.life-1;
					dto.vx = (float) p[0];
					dto.vy = (float) p[1];
					dto.latLng = latLng;
				}
			}
		}
		Message msg = new Message();
		msg.obj = particles;
		handler.sendMessage(msg);
	}

	/**
	 * 获取粒子的速度
	 * @param latLng
	 * @return
	 */
	private double[] getVector(LatLng latLng) {
		double a = (windData.width - 1 - 1e-6)*(latLng.longitude - windData.x0)/(windData.x1 - windData.x0);
		double b = (windData.height - 1 - 1e-6)*(latLng.latitude - windData.y0)/(windData.y1 - windData.y0);

		double na = Math.min(Math.floor(a), windData.width - 1);
		double nb = Math.min(Math.floor(b), windData.height - 1);
		double ma = Math.min(Math.ceil(a), windData.width - 1);
		double mb = Math.min(Math.ceil(b), windData.height - 1);

		double fa = a - na;
		double fb = b - nb;

		int index = windData.height;
		int count = windData.dataList.size();

		double[] array = new double[2];
		try {
			double vx = (windData.dataList.get((int)Math.min(na*index+nb, count-1)).initX * (1-fa)*(1-fb)+
					windData.dataList.get((int) Math.min(ma*index+nb, count-1)).initX * fa*(1-fb)+
					windData.dataList.get((int) Math.min(na*index+mb, count-1)).initX * (1-fa)*fb+
					windData.dataList.get((int) Math.min(ma*index+mb, count-1)).initX * fa*fb) * speedRate;

			double vy = (windData.dataList.get((int) Math.min(na*index+nb, count-1)).initY * (1-fa)*(1-fb)+
					windData.dataList.get((int) Math.min(ma*index+nb, count-1)).initY * fa*(1-fb)+
					windData.dataList.get((int) Math.min(na*index+mb, count-1)).initY * (1-fa)*fb+
					windData.dataList.get((int) Math.min(ma*index+mb, count-1)).initY * fa*fb) * speedRate;

			array[0] = vx;
			array[1] = vy;
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		return array;
	}

	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			List<WindDto> list = (ArrayList)msg.obj;
			for (WindDto dto : list) {
				if (dto != null) {
					if (dto.oldX != -1 || dto.oldY != -1) {
						if (tempCanvas != null) {
							tempCanvas.drawLine(dto.oldX, dto.oldY, dto.x, dto.y, paint);
						}
					}
				}
			}
		};
	};

	/**
	 * 判断是否为华为P9
	 * @return
	 */
	private boolean isHuaWeiP9() {
		String brand = android.os.Build.BRAND; //手机品牌
		String model = android.os.Build.MODEL; // 手机型号
		if (TextUtils.equals("HUAWEI", brand) && TextUtils.equals("VIE-AL10", model)) {
			return true;
		}
		return false;
	}

	/**
	 * 获取系统总内存
	 *
	 * @return 总内存大单位为kB。
	 */
	public static float getTotalMemorySize() {
		String dir = "/proc/meminfo";
		try {
			FileReader fr = new FileReader(dir);
			BufferedReader br = new BufferedReader(fr, 2048);
			String memoryLine = br.readLine();
			String subMemoryLine = memoryLine.substring(memoryLine.indexOf("MemTotal:"));
			br.close();
			return Float.parseFloat(subMemoryLine.replaceAll("\\D+", ""));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * 实时获取CPU当前频率（单位KHZ）
	 * @return
	 */
	public static float getCurCpuFreq() {
		float result = 0;
		try {
			FileReader fr = new FileReader("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
			BufferedReader br = new BufferedReader(fr);
			String text = br.readLine();
			if (!TextUtils.isEmpty(text)) {
				result = Float.valueOf(text.trim());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	// 获取CPU最大频率（单位KHZ）
	public static String getMaxCpuFreq() {
		String result = "";
		ProcessBuilder cmd;
		try {
			String[] args = { "/system/bin/cat", "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq" };
			cmd = new ProcessBuilder(args);
			Process process = cmd.start();
			InputStream in = process.getInputStream();
			byte[] re = new byte[24];
			while (in.read(re) != -1) {
				result = result + new String(re);
			}
			in.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			result = "N/A";
		}
		return result.trim();
	}

	// 获取CPU最小频率（单位KHZ）
	public static String getMinCpuFreq() {
		String result = "";
		ProcessBuilder cmd;
		try {
			String[] args = { "/system/bin/cat", "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq" };
			cmd = new ProcessBuilder(args);
			Process process = cmd.start();
			InputStream in = process.getInputStream();
			byte[] re = new byte[24];
			while (in.read(re) != -1) {
				result = result + new String(re);
			}
			in.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			result = "N/A";
		}
		return result.trim();
	}

}
