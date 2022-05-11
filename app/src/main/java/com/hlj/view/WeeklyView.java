package com.hlj.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
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
import java.util.List;
import java.util.Locale;

import shawn.cxwl.com.hlj.R;

/**
 * 一周预报曲线图
 */
public class WeeklyView extends View {

	private Context mContext;
	private List<WeatherDto> tempList = new ArrayList<>();
	private int maxTemp = 0;//最高温度
	private int minTemp = 0;//最低温度
	private Paint lineP = null;//画线画笔
	private Paint textP = null;//写字画笔
	private Paint roundP = null;//aqi背景颜色画笔
	private Bitmap windBitmap = null;
	private SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
	private SimpleDateFormat sdf2 = new SimpleDateFormat("MM/dd", Locale.CHINA);
	private int totalDivider = 0;
	private long foreDate = 0, currentDate = 0;

	public WeeklyView(Context context) {
		super(context);
		mContext = context;
		init();
	}

	public WeeklyView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init();
	}

	public WeeklyView(Context context, AttributeSet attrs, int defStyleAttr) {
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

		roundP = new Paint();
		roundP.setStyle(Paint.Style.FILL);
		roundP.setStrokeCap(Paint.Cap.ROUND);
		roundP.setAntiAlias(true);

		Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon_wind);
		windBitmap = ThumbnailUtils.extractThumbnail(bitmap, (int)(CommonUtil.dip2px(mContext, 18)), (int)(CommonUtil.dip2px(mContext, 18)));
	}

	public void setData(List<WeatherDto> dataList, long foreDate, long currentDate) {
		this.foreDate = foreDate;
		this.currentDate = currentDate;
		if (!dataList.isEmpty()) {
			tempList.clear();
			tempList.addAll(dataList);

			maxTemp = tempList.get(0).highTemp;
			minTemp = tempList.get(0).lowTemp;
			for (int i = 0; i < tempList.size(); i++) {
				if (maxTemp <= tempList.get(i).highTemp) {
					maxTemp = tempList.get(i).highTemp;
				}
				if (minTemp >= tempList.get(i).lowTemp) {
					minTemp = tempList.get(i).lowTemp;
				}
			}
			totalDivider = maxTemp-minTemp;
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		float w = canvas.getWidth();
		float h = canvas.getHeight();
		float chartW = w-CommonUtil.dip2px(mContext, 40);
		float chartH = h-CommonUtil.dip2px(mContext, 280);
		float leftMargin = CommonUtil.dip2px(mContext, 20);
		float rightMargin = CommonUtil.dip2px(mContext, 20);
		float topMargin = CommonUtil.dip2px(mContext, 130);

		int size = tempList.size();
		for (int i = 0; i < size; i++) {
			WeatherDto dto = tempList.get(i);

			//获取最高温度对应的坐标点信息
			float x = (chartW/(size-1))*i + leftMargin;
			dto.highX = x;
			dto.highY = chartH*Math.abs(maxTemp-dto.highTemp)/totalDivider+topMargin;

			//获取最低温度的对应的坐标点信息
			dto.lowX = x;
			dto.lowY = chartH*Math.abs(maxTemp-dto.lowTemp)/totalDivider+topMargin;

			//绘制星期
			textP.setColor(getResources().getColor(R.color.white));
			textP.setTextSize(getResources().getDimension(R.dimen.level_5));
			String week = "";
			if (currentDate > foreDate) {
				if (i == 0) {
					week = "昨天";
				}else if (i == 1) {
					week = "今天";
				}else if (i == 2) {
					week = "明天";
				}else {
					week = dto.week;
				}
			} else {
				if (i == 0) {
					week = "今天";
				}else if (i == 1) {
					week = "明天";
				}else {
					week = dto.week;
				}
			}
			float weekWidth = textP.measureText(week);
			canvas.drawText(week, dto.highX-weekWidth/2, CommonUtil.dip2px(mContext, 20), textP);

			//绘制日期
			textP.setColor(getResources().getColor(R.color.white));
			textP.setTextSize(getResources().getDimension(R.dimen.level_5));
			try {
				String date = sdf2.format(sdf1.parse(dto.date));
				float dateText = textP.measureText(date);
				canvas.drawText(date, dto.highX-dateText/2, CommonUtil.dip2px(mContext, 40), textP);
			} catch (ParseException e) {
				e.printStackTrace();
			}

			//绘制白天天气现象文字
			textP.setColor(getResources().getColor(R.color.white));
			textP.setTextSize(getResources().getDimension(R.dimen.level_5));
			float highPheWidth = textP.measureText(dto.highPhe);
			canvas.drawText(dto.highPhe, dto.highX-highPheWidth/2, CommonUtil.dip2px(mContext, 60), textP);

			//绘制白天天气现象图标
			Bitmap dayB = WeatherUtil.getBitmap(mContext, dto.highPheCode);
			if (dayB != null) {
				Bitmap dayBitmap = ThumbnailUtils.extractThumbnail(dayB, (int)(CommonUtil.dip2px(mContext, 20)), (int)(CommonUtil.dip2px(mContext, 20)));
				if (dayBitmap != null) {
					if (TextUtils.equals(MyApplication.getAppTheme(), "1")) {
						canvas.drawBitmap(CommonUtil.grayScaleImage(dayBitmap), dto.highX-dayBitmap.getWidth()/2, CommonUtil.dip2px(mContext, 68), textP);
					} else {
						canvas.drawBitmap(dayBitmap, dto.highX-dayBitmap.getWidth()/2, CommonUtil.dip2px(mContext, 68), textP);
					}
				}
			}

			//绘制高温
			textP.setColor(getResources().getColor(R.color.white));
			textP.setTextSize(CommonUtil.dip2px(mContext, 13));
			float highText = textP.measureText(String.valueOf(tempList.get(i).highTemp));//高温字符串占像素宽度
			canvas.drawText(tempList.get(i).highTemp+"℃", dto.highX-highText/2, CommonUtil.dip2px(mContext, 103), textP);

			//绘制低温
			textP.setColor(getResources().getColor(R.color.white));
			textP.setTextSize(CommonUtil.dip2px(mContext, 13));
			float lowText = textP.measureText(String.valueOf(tempList.get(i).lowTemp));//低温字符串所占的像素宽度
			canvas.drawText(tempList.get(i).lowTemp+"℃", dto.lowX-lowText/2, h-CommonUtil.dip2px(mContext, 110), textP);

			//绘制晚上天气现象图标
			Bitmap nightB = WeatherUtil.getNightBitmap(mContext, dto.lowPheCode);
			if (nightB != null) {
				Bitmap nightBitmap = ThumbnailUtils.extractThumbnail(nightB, (int)(CommonUtil.dip2px(mContext, 20)), (int)(CommonUtil.dip2px(mContext, 20)));
				if (nightBitmap != null) {
					if (TextUtils.equals(MyApplication.getAppTheme(), "1")) {
						canvas.drawBitmap(CommonUtil.grayScaleImage(nightBitmap), dto.lowX-nightBitmap.getWidth()/2, h-CommonUtil.dip2px(mContext, 105), textP);
					} else {
						canvas.drawBitmap(nightBitmap, dto.lowX-nightBitmap.getWidth()/2, h-CommonUtil.dip2px(mContext, 105), textP);
					}
				}
			}

			//绘制晚上天气现象文字
			textP.setColor(getResources().getColor(R.color.white));
			textP.setTextSize(getResources().getDimension(R.dimen.level_5));
			float lowPheWidth = textP.measureText(dto.lowPhe);//天气现象字符串占像素宽度
			canvas.drawText(dto.lowPhe, dto.lowX-lowPheWidth/2, h-CommonUtil.dip2px(mContext, 70), textP);

			//绘制风向
//			textP.setColor(getResources().getColor(R.color.white));
//			textP.setTextSize(CommonUtil.dip2px(mContext, 12));
//			String windDir = mContext.getString(WeatherUtil.getWindDirection(Integer.valueOf(dto.windDir)));
//			float windDirWidth = textP.measureText(windDir);//低温字符串所占的像素宽度
//			canvas.drawText(windDir, dto.highX-windDirWidth/2, h-CommonUtil.dip2px(mContext, 45), textP);

			//绘制风向标
			float rotation = 0;
			if (dto.windDir == 1) {
				rotation = 225;
			}else if (dto.windDir == 2) {
				rotation = 270;
			}else if (dto.windDir == 3) {
				rotation = 315;
			}else if (dto.windDir == 4) {
				rotation = 0;
			}else if (dto.windDir == 5) {
				rotation = 45;
			}else if (dto.windDir == 6) {
				rotation = 90;
			}else if (dto.windDir == 7) {
				rotation = 135;
			}else if (dto.windDir == 8) {
				rotation = 180;
			}else {
				rotation = 0;
			}
			Matrix matrix = new Matrix();
			matrix.postScale(1, 1);
			matrix.postRotate(rotation);
			if (windBitmap != null) {
				Bitmap wBitmap = Bitmap.createBitmap(windBitmap, 0, 0, windBitmap.getWidth(), windBitmap.getHeight(), matrix, true);
				if (wBitmap != null) {
					canvas.drawBitmap(wBitmap, dto.highX-wBitmap.getWidth()/2, h-CommonUtil.dip2px(mContext, 55)-wBitmap.getHeight()/2, textP);
				}
			}

			//绘制风力
			textP.setColor(getResources().getColor(R.color.white));
			textP.setTextSize(CommonUtil.dip2px(mContext, 12));
			float windForceWidth = textP.measureText(dto.windForceString);//低温字符串所占的像素宽度
			canvas.drawText(dto.windForceString, dto.highX-windForceWidth/2, h-CommonUtil.dip2px(mContext, 30), textP);

			//绘制aqi数值
			textP.setTextSize(CommonUtil.dip2px(mContext, 12));
			if (!TextUtils.isEmpty(dto.aqi)) {
				int aqi = Integer.valueOf(dto.aqi);
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
				RectF rectF = new RectF(dto.lowX-CommonUtil.dip2px(mContext, 12.5f), h-CommonUtil.dip2px(mContext, 23),
						dto.lowX+CommonUtil.dip2px(mContext, 12.5f), h-CommonUtil.dip2px(mContext, 6));
				canvas.drawRoundRect(rectF, CommonUtil.dip2px(mContext, 5), CommonUtil.dip2px(mContext, 5), roundP);
				float aqiWidth = textP.measureText(dto.aqi);
				canvas.drawText(dto.aqi, dto.lowX-aqiWidth/2, h-CommonUtil.dip2px(mContext, 10f), textP);
			}

			//绘制曲线上每个时间点上的圆点marker
			if (TextUtils.equals(MyApplication.getAppTheme(), "1")) {
				lineP.setColor(Color.WHITE);
			} else {
				lineP.setColor(getResources().getColor(R.color.low_color));
			}
			lineP.setStrokeWidth(CommonUtil.dip2px(mContext, 8));
			canvas.drawPoint(dto.lowX, dto.lowY, lineP);

			if (TextUtils.equals(MyApplication.getAppTheme(), "1")) {
				lineP.setColor(Color.WHITE);
			} else {
				lineP.setColor(getResources().getColor(R.color.high_color));
			}
			lineP.setStrokeWidth(CommonUtil.dip2px(mContext, 8));
			canvas.drawPoint(dto.highX, dto.highY, lineP);
		}

		//绘制最低温度、最高温度曲线
		for (int i = 0; i < size-1; i++) {
			//低温
			float x1 = tempList.get(i).lowX;
			float y1 = tempList.get(i).lowY;
			float x2 = tempList.get(i+1).lowX;
			float y2 = tempList.get(i+1).lowY;
			float wt = (x1 + x2) / 2;
			float x3 = wt;
			float y3 = y1;
			float x4 = wt;
			float y4 = y2;
			Path pathLow = new Path();
			pathLow.moveTo(x1, y1);
			pathLow.cubicTo(x3, y3, x4, y4, x2, y2);
			if (TextUtils.equals(MyApplication.getAppTheme(), "1")) {
				lineP.setColor(Color.WHITE);
			} else {
				lineP.setColor(getResources().getColor(R.color.low_color));
			}
			lineP.setStrokeWidth(5.0f);
			canvas.drawPath(pathLow, lineP);

			//高温
			x1 = tempList.get(i).highX;
			y1 = tempList.get(i).highY;
			x2 = tempList.get(i+1).highX;
			y2 = tempList.get(i+1).highY;
			wt = (x1 + x2) / 2;
			x3 = wt;
			y3 = y1;
			x4 = wt;
			y4 = y2;
			Path pathHigh = new Path();
			pathHigh.moveTo(x1, y1);
			pathHigh.cubicTo(x3, y3, x4, y4, x2, y2);
			if (TextUtils.equals(MyApplication.getAppTheme(), "1")) {
				lineP.setColor(Color.WHITE);
			} else {
				lineP.setColor(getResources().getColor(R.color.high_color));
			}
			lineP.setStrokeWidth(5.0f);
			canvas.drawPath(pathHigh, lineP);
		}

	}

}
