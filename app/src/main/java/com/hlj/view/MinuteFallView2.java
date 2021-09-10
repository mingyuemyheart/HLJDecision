package com.hlj.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.media.ThumbnailUtils;
import android.util.AttributeSet;
import android.view.View;

import com.hlj.dto.WeatherDto;
import com.hlj.utils.CommonUtil;

import java.util.ArrayList;
import java.util.List;

import shawn.cxwl.com.hlj.R;

/**
 * 分钟级降水图
 */
public class MinuteFallView2 extends View {

	private Context mContext ;
	private List<WeatherDto> tempList = new ArrayList<>();
	private float maxValue,minValue;
	private Paint lineP,textP,shaderPaint;//画线画笔
	private float level1 = 0.05f, level2 = 0.15f, level3 = 0.35f;//0.05-0.15是小雨，0.15-0.35是中雨, 0.35以上是大雨
	private String rain_level1 = "小雨",rain_level2 = "中雨",rain_level3 = "大雨";
	private Bitmap bitmap1, bitmap2, bitmap3;

	public MinuteFallView2(Context context) {
		super(context);
		mContext = context;
		init();
	}

	public MinuteFallView2(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init();
	}

	public MinuteFallView2(Context context, AttributeSet attrs, int defStyleAttr) {
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

		shaderPaint = new Paint();
		shaderPaint.setStyle(Paint.Style.STROKE);
		shaderPaint.setStrokeCap(Paint.Cap.ROUND);
		shaderPaint.setAntiAlias(true);


		bitmap1 = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(getResources(), R.drawable.icon_minute_xy),
				(int)(CommonUtil.dip2px(mContext, 17)), (int)(CommonUtil.dip2px(mContext, 13)));
		bitmap2 = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(getResources(), R.drawable.icon_minute_zy),
				(int)(CommonUtil.dip2px(mContext, 17)), (int)(CommonUtil.dip2px(mContext, 13)));
		bitmap3 = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(getResources(), R.drawable.icon_minute_dy),
				(int)(CommonUtil.dip2px(mContext, 17)), (int)(CommonUtil.dip2px(mContext, 16)));
	}

	/**
	 * 对cubicView进行赋值
	 */
	public void setData(List<WeatherDto> dataList, String type) {
		if (type.contains("雪")) {
			rain_level1 = "小雪";
			rain_level2 = "中雪";
			rain_level3 = "大雪";
		}else {
			rain_level1 = "小雨";
			rain_level2 = "中雨";
			rain_level3 = "大雨";
		}

		if (!dataList.isEmpty()) {
			tempList.clear();
			tempList.addAll(dataList);

			maxValue = tempList.get(0).minuteFall;
			minValue = tempList.get(0).minuteFall;
			for (int i = 0; i < tempList.size(); i++) {
				if (maxValue <= tempList.get(i).minuteFall) {
					maxValue = tempList.get(i).minuteFall;
				}
				if (minValue >= tempList.get(i).minuteFall) {
					minValue = tempList.get(i).minuteFall;
				}
			}

			maxValue = 0.5f;
			minValue = 0;

		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (tempList.isEmpty()) {
			return;
		}

		canvas.drawColor(Color.TRANSPARENT);
		float w = canvas.getWidth();
		float h = canvas.getHeight();
		float chartW = w-CommonUtil.dip2px(mContext, 60);
		float chartH = h-CommonUtil.dip2px(mContext, 30);
		float leftMargin = CommonUtil.dip2px(mContext, 10);
		float rightMargin = CommonUtil.dip2px(mContext, 10);
		float topMargin = CommonUtil.dip2px(mContext, 10);
		float bottomMargin = CommonUtil.dip2px(mContext, 20);

		int size = tempList.size();
		//获取曲线上每个温度点的坐标
		for (int i = 0; i < size; i++) {
			WeatherDto dto = tempList.get(i);
			dto.x = (chartW/(size-1))*i + leftMargin;

			float value = tempList.get(i).minuteFall;
			dto.y = chartH - chartH* Math.abs(value)/(Math.abs(maxValue)+ Math.abs(minValue)) + topMargin;
			tempList.set(i, dto);
		}

		//绘制区域
		//新建一个线性渐变，前两个参数是渐变开始的点坐标，第三四个参数是渐变结束的点的坐标。连接这2个点就拉出一条渐变线了，玩过PS的都懂。然后那个数组是渐变的颜色。下一个参数是渐变颜色的分布，如果为空，每个颜色就是均匀分布的。最后是模式，这里设置的是循环渐变
		Shader mShader = new LinearGradient(w/2,h,w/2,bottomMargin,new int[] {0xfff6fafd,0xff6fc9ed},null,Shader.TileMode.REPEAT);
		shaderPaint.setShader(mShader);
		shaderPaint.setStrokeWidth(CommonUtil.dip2px(mContext, 1));
		shaderPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		for (int i = 0; i < size-1; i++) {
			float x1 = tempList.get(i).x;
			float y1 = tempList.get(i).y;
			float x2 = tempList.get(i+1).x;
			float y2 = tempList.get(i+1).y;

			if (i != size-1) {
				Path rectPath = new Path();
				rectPath.moveTo(x1, y1);
				rectPath.lineTo(x2, y2);
				rectPath.lineTo(x2, h-bottomMargin);
				rectPath.lineTo(x1, h-bottomMargin);
				rectPath.close();
				canvas.drawPath(rectPath, shaderPaint);
			}
		}

		//绘制小雨与中雨的分割线
		float dividerY;
		float value = level2;
		dividerY = chartH - chartH* Math.abs(value)/(Math.abs(maxValue)+ Math.abs(minValue)) + topMargin;
		lineP.setStrokeWidth(CommonUtil.dip2px(mContext, 0.5f));
		lineP.setColor(0x809aadd9);
		canvas.drawLine(leftMargin, dividerY, w-rightMargin, dividerY, lineP);
		textP.setColor(0xff999999);
		textP.setTextSize(CommonUtil.dip2px(mContext, 10));
//		canvas.drawText(String.valueOf(value)+"mm", CommonUtil.dip2px(mContext, 5), dividerY-CommonUtil.dip2px(mContext, 2.5f), textP);
		textP.setTextSize(CommonUtil.dip2px(mContext, 10));
		canvas.drawText(rain_level1, chartW, dividerY+CommonUtil.dip2px(mContext, 17), textP);
		canvas.drawBitmap(bitmap1, leftMargin, dividerY+CommonUtil.dip2px(mContext, 7), textP);

		//绘制中雨与大雨的分割线
		value = level3;
		dividerY = chartH - chartH* Math.abs(value)/(Math.abs(maxValue)+ Math.abs(minValue)) + topMargin;
		lineP.setStrokeWidth(CommonUtil.dip2px(mContext, 0.5f));
		lineP.setColor(0x809aadd9);
		canvas.drawLine(leftMargin, dividerY, w-rightMargin, dividerY, lineP);
		textP.setColor(0xff999999);
		textP.setTextSize(CommonUtil.dip2px(mContext, 10));
//		canvas.drawText(String.valueOf(value)+"mm", CommonUtil.dip2px(mContext, 5), dividerY-CommonUtil.dip2px(mContext, 2.5f), textP);
		textP.setTextSize(CommonUtil.dip2px(mContext, 10));
		canvas.drawText(rain_level2, chartW, dividerY+CommonUtil.dip2px(mContext, 22f), textP);
		canvas.drawBitmap(bitmap2, leftMargin, dividerY+bitmap2.getHeight()/2, textP);
		textP.setTextSize(CommonUtil.dip2px(mContext, 10));
		canvas.drawText(rain_level3, chartW, dividerY-CommonUtil.dip2px(mContext, 10f), textP);
		canvas.drawBitmap(bitmap3, leftMargin, dividerY-CommonUtil.dip2px(mContext, 25f), textP);

		//绘制分钟刻度线
		value = 0;
		dividerY = chartH - chartH* Math.abs(value)/(Math.abs(maxValue)+ Math.abs(minValue)) + topMargin;
		lineP.setStrokeWidth(CommonUtil.dip2px(mContext, 0.5f));
		lineP.setColor(0xff999999);
		canvas.drawLine(leftMargin, dividerY, w-rightMargin, dividerY, lineP);

		for (int i = 0; i < size; i++) {
			WeatherDto dto = tempList.get(i);
			//绘制分钟刻度线上的刻度
			if (i == 0 || i == 20 || i == 40 || i == 60 || i == 80 || i == 100 || i == 119) {
				lineP.setColor(0xff999999);
				lineP.setStrokeWidth(CommonUtil.dip2px(mContext, 1));
				canvas.drawLine(dto.x, dividerY, dto.x, dividerY+CommonUtil.dip2px(mContext, 4), lineP);
			}
			//绘制10、30、50分钟值
			if (i == 20 || i == 60 || i == 100) {
				textP.setColor(0xff999999);
				textP.setTextSize(CommonUtil.dip2px(mContext, 10));
				float tempWidth = textP.measureText(i+"分钟");
				canvas.drawText(i+"分钟", dto.x-tempWidth/2, dividerY+CommonUtil.dip2px(mContext, 15), textP);
			}
		}

	}

}
