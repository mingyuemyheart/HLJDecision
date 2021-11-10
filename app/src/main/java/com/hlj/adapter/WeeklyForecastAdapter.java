package com.hlj.adapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import shawn.cxwl.com.hlj.R;

import com.hlj.common.MyApplication;
import com.hlj.dto.WeatherDto;
import com.hlj.utils.CommonUtil;

public class WeeklyForecastAdapter extends BaseAdapter{
	
	private Context mContext;
	private LayoutInflater mInflater;
	private List<WeatherDto> mArrayList = new ArrayList<>();
	private SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMdd");
	private SimpleDateFormat sdf2 = new SimpleDateFormat("MM/dd");
	public long foreDate = 0, currentDate = 0;
	
	private final class ViewHolder{
		TextView tvWeek;
		TextView tvDate;
		TextView tvHighPhe;
		ImageView ivHighPhe;
		TextView tvHighTemp;
		TextView tvLowPhe;
		ImageView ivLowPhe;
		TextView tvLowTemp;
	}
	
	private ViewHolder mHolder = null;
	
	public WeeklyForecastAdapter(Context context, List<WeatherDto> mArrayList) {
		mContext = context;
		this.mArrayList = mArrayList;
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return mArrayList.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.adapter_weekly_forecast, null);
			mHolder = new ViewHolder();
			mHolder.tvWeek = (TextView) convertView.findViewById(R.id.tvWeek);
			mHolder.tvDate = (TextView) convertView.findViewById(R.id.tvDate);
			mHolder.tvHighPhe = (TextView) convertView.findViewById(R.id.tvHighPhe);
			mHolder.ivHighPhe = (ImageView) convertView.findViewById(R.id.ivHighPhe);
			mHolder.tvHighTemp = (TextView) convertView.findViewById(R.id.tvHighTemp);
			mHolder.tvLowPhe = (TextView) convertView.findViewById(R.id.tvLowPhe);
			mHolder.ivLowPhe = (ImageView) convertView.findViewById(R.id.ivLowPhe);
			mHolder.tvLowTemp = (TextView) convertView.findViewById(R.id.tvLowTemp);
			convertView.setTag(mHolder);
		}else {
			mHolder = (ViewHolder) convertView.getTag();
		}
		
		WeatherDto dto = mArrayList.get(position);
		if (position == 0) {
			mHolder.tvWeek.setText(mContext.getString(R.string.today));
		}else {
			String week = dto.week;
			mHolder.tvWeek.setText(mContext.getString(R.string.week)+week.substring(week.length()-1, week.length()));
		}
		if (currentDate > foreDate) {
			if (position == 0) {
				mHolder.tvWeek.setText("昨天");
			}else if (position == 1) {
				mHolder.tvWeek.setText("今天");
			}else if (position == 2) {
				mHolder.tvWeek.setText("明天");
			}else {
				mHolder.tvWeek.setText(dto.week);
			}
		}else {
			if (position == 0) {
				mHolder.tvWeek.setText("今天");
			}else if (position == 1) {
				mHolder.tvWeek.setText("明天");
			}else {
				mHolder.tvWeek.setText(dto.week);
			}
		}
		try {
			mHolder.tvDate.setText(sdf2.format(sdf1.parse(dto.date)));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		
		mHolder.tvLowPhe.setText(dto.lowPhe);
		mHolder.tvLowTemp.setText(dto.lowTemp+"℃");
		Drawable ld = mContext.getResources().getDrawable(R.drawable.phenomenon_drawable_night);
		ld.setLevel(dto.lowPheCode);
		
		mHolder.tvHighPhe.setText(dto.highPhe);
		mHolder.tvHighTemp.setText(dto.highTemp+"℃");
		Drawable hd = mContext.getResources().getDrawable(R.drawable.phenomenon_drawable);
		hd.setLevel(dto.highPheCode);

		if (TextUtils.equals(MyApplication.getAppTheme(), "1")) {
			mHolder.ivHighPhe.setImageBitmap(CommonUtil.grayScaleImage(CommonUtil.drawableToBitmap(hd)));
			mHolder.ivLowPhe.setImageBitmap(CommonUtil.grayScaleImage(CommonUtil.drawableToBitmap(ld)));
		} else {
			mHolder.ivHighPhe.setBackground(hd);
			mHolder.ivLowPhe.setBackground(ld);
		}
		
		return convertView;
	}

}
