package com.hlj.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.ThumbnailUtils;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.hlj.common.MyApplication;
import com.hlj.dto.WeatherDto;
import com.hlj.utils.CommonUtil;
import com.hlj.utils.WeatherUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import shawn.cxwl.com.hlj.R;

/**
 * 逐小时预报
 */
public class HourlyView extends View {

	private Context mContext;
	private SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMddHHmm", Locale.CHINA);
	private SimpleDateFormat sdf2 = new SimpleDateFormat("HH:00", Locale.CHINA);
	private SimpleDateFormat sdf3 = new SimpleDateFormat("HH", Locale.CHINA);
	private List<WeatherDto> tempList = new ArrayList<>();
	private int maxTemp = 0;//最高温度
	private int minTemp = 0;//最低温度
	private Paint lineP = null;//画线画笔
	private Paint textP = null;//写字画笔
	private Paint rectP = null;
	private Paint roundP = null;//aqi背景颜色画笔
	private Paint shaderPaint;
	private int totalDivider = 0;
	private int itemDivider = 1;
	private Bitmap wBitmap;

	public HourlyView(Context context) {
		super(context);
		mContext = context;
		init();
	}

	public HourlyView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init();
	}

	public HourlyView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mContext = context;
		init();
	}

	private void init() {
		lineP = new Paint();
		lineP.setStyle(Paint.Style.STROKE);
		lineP.setStrokeCap(Paint.Cap.ROUND);
		lineP.setAntiAlias(true);

		textP = new Paint();
		textP.setAntiAlias(true);

		rectP = new Paint();
		rectP.setStyle(Paint.Style.FILL);
		rectP.setAntiAlias(true);

		roundP = new Paint();
		roundP.setStyle(Paint.Style.FILL);
		roundP.setStrokeCap(Paint.Cap.ROUND);
		roundP.setAntiAlias(true);

		shaderPaint = new Paint();
		shaderPaint.setStrokeCap(Paint.Cap.ROUND);
		shaderPaint.setAntiAlias(true);

		wBitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(getResources(), R.drawable.icon_wind),
				(int)(CommonUtil.dip2px(mContext, 12)), (int)(CommonUtil.dip2px(mContext, 12)));
	}

	public void setData(List<WeatherDto> dataList) {
		if (!dataList.isEmpty()) {
			tempList.clear();
			tempList.addAll(dataList);

			maxTemp = tempList.get(0).hourlyTemp;
			minTemp = tempList.get(0).hourlyTemp;
			for (int i = 0; i < tempList.size(); i++) {
				if (maxTemp <= tempList.get(i).hourlyTemp) {
					maxTemp = tempList.get(i).hourlyTemp;
				}
				if (minTemp >= tempList.get(i).hourlyTemp) {
					minTemp = tempList.get(i).hourlyTemp;
				}
			}

			totalDivider = maxTemp-minTemp;
			if (totalDivider <= 5) {
				itemDivider = 1;
			}else if (totalDivider <= 10) {
				itemDivider = 3;
			}else if (totalDivider <= 15) {
				itemDivider = 5;
			}else if (totalDivider <= 20) {
				itemDivider = 10;
			}else {
				itemDivider = 15;
			}
			maxTemp = maxTemp+itemDivider;
			minTemp = minTemp-itemDivider;
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		float w = canvas.getWidth();
		float h = canvas.getHeight();
		float chartW = w-CommonUtil.dip2px(mContext, 60);
		float chartH = h-CommonUtil.dip2px(mContext, 130);
		float leftMargin = CommonUtil.dip2px(mContext, 50);
		float rightMargin = CommonUtil.dip2px(mContext, 10);
		float bottomMargin = CommonUtil.dip2px(mContext, 80);

		int size = tempList.size();
		//获取曲线上每个温度点的坐标
		for (int i = 0; i < size; i++) {
			WeatherDto dto = tempList.get(i);
			dto.x = (chartW/(size-1))*i + leftMargin;
			dto.y = chartH*Math.abs(maxTemp-dto.hourlyTemp)/totalDivider;
			tempList.set(i, dto);
		}

		//绘制区域
		//新建一个线性渐变，前两个参数是渐变开始的点坐标，第三四个参数是渐变结束的点的坐标。连接这2个点就拉出一条渐变线了，玩过PS的都懂。然后那个数组是渐变的颜色。下一个参数是渐变颜色的分布，如果为空，每个颜色就是均匀分布的。最后是模式，这里设置的是循环渐变
		Shader mShader = new LinearGradient(w/2,h,w/2,bottomMargin,new int[] {0x01E6B765,0x30E6B765},null,Shader.TileMode.CLAMP);
		shaderPaint.setShader(mShader);
		shaderPaint.setStrokeWidth(CommonUtil.dip2px(mContext, 1));
		shaderPaint.setStyle(Style.FILL);
		for (int i = 0; i < size-1; i++) {
			float x1 = tempList.get(i).x;
			float y1 = tempList.get(i).y;
			float x2 = tempList.get(i+1).x;
			float y2 = tempList.get(i+1).y;
			float wt = (x1 + x2) / 2;
			float x3 = wt;
			float y3 = y1;
			float x4 = wt;
			float y4 = y2;
			Path rectPath = new Path();
			rectPath.moveTo(x1, y1);
			rectPath.cubicTo(x3, y3, x4, y4, x2, y2);
			rectPath.lineTo(x2, h-bottomMargin);
			rectPath.lineTo(x1, h-bottomMargin);
			rectPath.close();
			canvas.drawPath(rectPath, shaderPaint);

			Path pathLow = new Path();
			pathLow.moveTo(x1, y1);
			pathLow.cubicTo(x3, y3, x4, y4, x2, y2);
			if (TextUtils.equals(MyApplication.getAppTheme(), "1")) {
				lineP.setColor(Color.WHITE);
			} else {
				lineP.setColor(0xffE6B765);
			}
			lineP.setStrokeWidth(5.0f);
			canvas.drawPath(pathLow, lineP);
		}

		float halfX = (chartW+rightMargin)/size/2;
		String hourlyWindForceString = "";
		for (int i = 0; i < size; i++) {
			WeatherDto dto = tempList.get(i);

			float itemWidth = (chartW/(size-1))*i + leftMargin;

			//绘制曲线上每个时间点marker
			textP.setColor(getResources().getColor(R.color.white));
			textP.setTextSize(CommonUtil.dip2px(mContext, 10));
			float tempWidth = textP.measureText(dto.hourlyTemp+"");
			canvas.drawText(dto.hourlyTemp+"", dto.x-tempWidth/2, dto.y-CommonUtil.dip2px(mContext, 5f), textP);

			//绘制天气现象图标
			int hour = 0;
			try {
				hour = Integer.valueOf(sdf3.format(sdf1.parse(dto.hourlyTime)));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			Bitmap bitmap;
			if (hour > 5 && hour <= 17) {
				bitmap = WeatherUtil.getBitmap(mContext, dto.hourlyCode);
			} else {
				bitmap = WeatherUtil.getNightBitmap(mContext, dto.hourlyCode);
			}
			if (bitmap != null) {
				Bitmap dayBitmap = ThumbnailUtils.extractThumbnail(bitmap, (int)(CommonUtil.dip2px(mContext, 20)), (int)(CommonUtil.dip2px(mContext, 20)));
				if (dayBitmap != null) {
					canvas.drawBitmap(dayBitmap, dto.x-dayBitmap.getWidth()/2, dto.y+CommonUtil.dip2px(mContext, 5f), textP);
				}
			}

			//绘制aqi数值
			if (!TextUtils.isEmpty(dto.hourlyAqi)) {
				int aqi = Integer.valueOf(dto.hourlyAqi);
				if (aqi <= 50) {
					textP.setColor(getResources().getColor(R.color.black));
					roundP.setColor(0xff50b74a);
				} else if (aqi <= 100) {
					textP.setColor(getResources().getColor(R.color.black));
					roundP.setColor(0xfff4f01b);
				} else if (aqi <= 150) {
					textP.setColor(getResources().getColor(R.color.black));
					roundP.setColor(0xfff38025);
				} else if (aqi <= 200) {
					textP.setColor(getResources().getColor(R.color.white));
					roundP.setColor(0xffec2222);
				} else if (aqi <= 300) {
					textP.setColor(getResources().getColor(R.color.white));
					roundP.setColor(0xff7b297d);
				} else {
					textP.setColor(getResources().getColor(R.color.white));
					roundP.setColor(0xff771512);
				}
				RectF rectF = new RectF(dto.x-halfX, h-CommonUtil.dip2px(mContext, 55),
						dto.x+halfX, h-CommonUtil.dip2px(mContext, 40));
				canvas.drawRoundRect(rectF, CommonUtil.dip2px(mContext, 5), CommonUtil.dip2px(mContext, 5), roundP);

				textP.setTextSize(CommonUtil.dip2px(mContext, 12));
				float aqiWidth = textP.measureText(dto.hourlyAqi);
				canvas.drawText(dto.hourlyAqi, dto.x-aqiWidth/2, h-CommonUtil.dip2px(mContext, 43f), textP);
			}

			//绘制风速风向背景
			rectP.setStrokeWidth(5.0f);
			rectP.setColor(0x40000000);
			rectP.setStyle(Style.FILL);
			rectP.setStrokeWidth(CommonUtil.dip2px(mContext, 1));
			canvas.drawRect(dto.x-halfX, h-CommonUtil.dip2px(mContext, 36), dto.x+halfX, h-CommonUtil.dip2px(mContext, 20), rectP);

			//绘制风速风向
//			if (!TextUtils.equals(hourlyWindForceString, dto.hourlyWindForceString)) {
				hourlyWindForceString = dto.hourlyWindForceString;
				Matrix matrix = new Matrix();
				matrix.postScale(1, 1);
				float rotation = 0;
				String dir = mContext.getString(WeatherUtil.getWindDirection(dto.hourlyWindDirCode));
				if (TextUtils.equals(dir, "北风")) {
					rotation = 180f;
				}else if (TextUtils.equals(dir, "东北风")) {
					rotation = 225f;
				}else if (TextUtils.equals(dir, "东风")) {
					rotation = 270f;
				}else if (TextUtils.equals(dir, "东南风")) {
					rotation = 315f;
				}else if (TextUtils.equals(dir, "南风")) {
					rotation = 0f;
				}else if (TextUtils.equals(dir, "西南风")) {
					rotation = 45f;
				}else if (TextUtils.equals(dir, "西风")) {
					rotation = 90f;
				}else if (TextUtils.equals(dir, "西北风")) {
					rotation = 135f;
				}else {
					rotation = 0f;
				}
				matrix.postRotate(rotation);
				if (wBitmap != null) {
					Bitmap b = Bitmap.createBitmap(wBitmap, 0, 0, wBitmap.getWidth(), wBitmap.getHeight(), matrix, true);
					if (b != null) {
						canvas.drawBitmap(b, dto.x-b.getWidth()-CommonUtil.dip2px(mContext, 3), h-CommonUtil.dip2px(mContext, 27)-b.getHeight()/2, textP);
					}
				}
				textP.setColor(getResources().getColor(R.color.white));
				textP.setTextSize(CommonUtil.dip2px(mContext, 12));
				float windWidth = textP.measureText(hourlyWindForceString);
				canvas.drawText(hourlyWindForceString, dto.x-windWidth/4, h-CommonUtil.dip2px(mContext, 23), textP);
//			}

			//绘制时间
			textP.setColor(getResources().getColor(R.color.white));
			textP.setTextSize(CommonUtil.dip2px(mContext, 12));
			if (i % 2 == 0) {
				try {
					String hourlyTime = i == 0 ? "现在" : sdf2.format(sdf1.parse(dto.hourlyTime));
					canvas.drawText(hourlyTime, dto.x-CommonUtil.dip2px(mContext, 12.5f), h-CommonUtil.dip2px(mContext, 5f), textP);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}

		//绘制刻度线
		textP.setColor(mContext.getResources().getColor(R.color.white));
		textP.setTextSize(CommonUtil.dip2px(mContext, 12));
		for (int i = minTemp; i <= maxTemp; i+=itemDivider) {
			if (i == minTemp || i == maxTemp) {
				continue;
			}
			float dividerY = chartH*Math.abs(maxTemp-i)/totalDivider;
			canvas.drawText(i+"°", CommonUtil.dip2px(mContext, 5), dividerY, textP);
		}

		//绘制aqi标签文字
		textP.setColor(mContext.getResources().getColor(R.color.white));
		textP.setTextSize(CommonUtil.dip2px(mContext, 12));
		canvas.drawText("空气", CommonUtil.dip2px(mContext, 5), h-CommonUtil.dip2px(mContext, 43), textP);

		//绘制风力标签文字
		textP.setColor(mContext.getResources().getColor(R.color.white));
		textP.setTextSize(CommonUtil.dip2px(mContext, 12));
		canvas.drawText("风力", CommonUtil.dip2px(mContext, 5), h-CommonUtil.dip2px(mContext, 23), textP);

	}

}
