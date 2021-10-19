package com.hlj.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.hlj.dto.WeatherDto;
import com.hlj.utils.CommonUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 分钟级降水图
 */
public class MinuteFallView extends View{

	private Context mContext;
	private List<WeatherDto> tempList = new ArrayList<>();
	private float maxValue = 0;
	private float minValue = 0;
	private Paint lineP = null;//画线画笔
	private Paint textP = null;//写字画笔
	private float level1 = 0.05f, level2 = 0.15f, level3 = 0.35f;//0.05-0.15是小雨，0.15-0.35是中雨, 0.35以上是大雨
	private String rain_level1 = "小雨";
	private String rain_level2 = "中雨";
	private String rain_level3 = "大雨";

	public MinuteFallView(Context context) {
		super(context);
		mContext = context;
		init();
	}

	public MinuteFallView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init();
	}

	public MinuteFallView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mContext = context;
		init();
	}

	private void init() {
		lineP = new Paint();
		lineP.setStyle(Paint.Style.STROKE);
		lineP.setAntiAlias(true);

		textP = new Paint();
		textP.setAntiAlias(true);
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
		float w = canvas.getWidth()-CommonUtil.dip2px(mContext, 30);
		float h = canvas.getHeight();
		float chartW = w-CommonUtil.dip2px(mContext, 20);
		float chartH = h-CommonUtil.dip2px(mContext, 30);
		float leftMargin = CommonUtil.dip2px(mContext, 10);
		float rightMargin = CommonUtil.dip2px(mContext, 10);
		float topMargin = CommonUtil.dip2px(mContext, 10);
		float bottomMargin = CommonUtil.dip2px(mContext, 20);
		float chartMaxH = chartH * maxValue / (Math.abs(maxValue)+Math.abs(minValue));//同时存在正负值时，正值高度

		int size = tempList.size();
		//获取曲线上每个温度点的坐标
		for (int i = 0; i < size; i++) {
			WeatherDto dto = tempList.get(i);
			dto.x = (chartW/(size-1))*i + leftMargin;

			float value = tempList.get(i).minuteFall;
			if (value >= 0) {
				dto.y = chartMaxH - chartH*Math.abs(value)/(Math.abs(maxValue)+Math.abs(minValue)) + topMargin;
				if (minValue >= 0) {
					dto.y = chartH - chartH*Math.abs(value)/(Math.abs(maxValue)+Math.abs(minValue)) + topMargin;
				}
			}else {
				dto.y = chartMaxH + chartH*Math.abs(value)/(Math.abs(maxValue)+Math.abs(minValue)) + topMargin;
				if (maxValue < 0) {
					dto.y = chartH*Math.abs(value)/(Math.abs(maxValue)+Math.abs(minValue)) + topMargin;
				}
			}
			tempList.set(i, dto);
		}

		//绘制小雨与中雨的分割线
		float dividerY = 0;
		float value = level2;
		if (value >= 0) {
			dividerY = chartMaxH - chartH*Math.abs(value)/(Math.abs(maxValue)+Math.abs(minValue)) + topMargin;
			if (minValue >= 0) {
				dividerY = chartH - chartH*Math.abs(value)/(Math.abs(maxValue)+Math.abs(minValue)) + topMargin;
			}
		}else {
			dividerY = chartMaxH + chartH*Math.abs(value)/(Math.abs(maxValue)+Math.abs(minValue)) + topMargin;
			if (maxValue < 0) {
				dividerY = chartH*Math.abs(value)/(Math.abs(maxValue)+Math.abs(minValue)) + topMargin;
			}
		}
		lineP.setStrokeWidth(CommonUtil.dip2px(mContext, 0.5f));
		lineP.setColor(0x60ffffff);
		canvas.drawLine(leftMargin, dividerY, w-rightMargin, dividerY, lineP);
		textP.setColor(Color.WHITE);
		textP.setTextSize(CommonUtil.dip2px(mContext, 12));
		canvas.drawText(rain_level1, w, dividerY+CommonUtil.dip2px(mContext, 17), textP);

		//绘制中雨与大雨的分割线
		dividerY = 0;
		value = level3;
		if (value >= 0) {
			dividerY = chartMaxH - chartH*Math.abs(value)/(Math.abs(maxValue)+Math.abs(minValue)) + topMargin;
			if (minValue >= 0) {
				dividerY = chartH - chartH*Math.abs(value)/(Math.abs(maxValue)+Math.abs(minValue)) + topMargin;
			}
		}else {
			dividerY = chartMaxH + chartH*Math.abs(value)/(Math.abs(maxValue)+Math.abs(minValue)) + topMargin;
			if (maxValue < 0) {
				dividerY = chartH*Math.abs(value)/(Math.abs(maxValue)+Math.abs(minValue)) + topMargin;
			}
		}
		lineP.setStrokeWidth(CommonUtil.dip2px(mContext, 0.5f));
		lineP.setColor(0x60ffffff);
		canvas.drawLine(leftMargin, dividerY, w-rightMargin, dividerY, lineP);
		canvas.drawText(rain_level2, w, dividerY+CommonUtil.dip2px(mContext, 22f), textP);
		canvas.drawText(rain_level3, w, dividerY-CommonUtil.dip2px(mContext, 10f), textP);

		//绘制分钟刻度线
		dividerY = 0;
		value = 0;
		if (value >= 0) {
			dividerY = chartMaxH - chartH*Math.abs(value)/(Math.abs(maxValue)+Math.abs(minValue)) + topMargin;
			if (minValue >= 0) {
				dividerY = chartH - chartH*Math.abs(value)/(Math.abs(maxValue)+Math.abs(minValue)) + topMargin;
			}
		}else {
			dividerY = chartMaxH + chartH*Math.abs(value)/(Math.abs(maxValue)+Math.abs(minValue)) + topMargin;
			if (maxValue < 0) {
				dividerY = chartH*Math.abs(value)/(Math.abs(maxValue)+Math.abs(minValue)) + topMargin;
			}
		}
		lineP.setStrokeWidth(CommonUtil.dip2px(mContext, 1));
		lineP.setColor(0x60ffffff);
		canvas.drawLine(leftMargin, dividerY, w-rightMargin, dividerY, lineP);

		textP.setColor(Color.WHITE);
		textP.setTextSize(CommonUtil.dip2px(mContext, 10));
		for (int i = 0; i < size; i++) {
			WeatherDto dto = tempList.get(i);

			if (i % 10 == 0 || i == size-1) {
				lineP.setStrokeWidth(CommonUtil.dip2px(mContext, 10));
				lineP.setColor(0xffE6B765);
				canvas.drawLine(dto.x, dividerY, dto.x, dto.y, lineP);
			}

			if (i % 20 == 0) {
				float tempWidth = textP.measureText(i+"");
				canvas.drawText(i+"", dto.x-tempWidth/2, dividerY+CommonUtil.dip2px(mContext, 15), textP);
			}
			if (i == size-1) {
				int num = i == size-1 ? size : i;
				float tempWidth = textP.measureText(num+"");
				canvas.drawText(num+" / 分钟", dto.x-tempWidth/2, dividerY+CommonUtil.dip2px(mContext, 15), textP);
			}
		}

	}

}
