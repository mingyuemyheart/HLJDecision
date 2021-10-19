package com.hlj.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hlj.dto.FactDto;

import java.util.List;

import shawn.cxwl.com.hlj.R;

/**
 * 实况站点查询-分钟
 */
public class FactQueryMinuteAdapter extends BaseAdapter {

	private Context mContext;
	private LayoutInflater mInflater;
	private List<FactDto> mArrayList;

	private final class ViewHolder{
		TextView tvStationName,tvStationCode,tvTime,tvRain;
	}

	public FactQueryMinuteAdapter(Context context, List<FactDto> mArrayList) {
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
		ViewHolder mHolder;
		if (convertView == null) {
			mHolder = new ViewHolder();
			convertView = mInflater.inflate(R.layout.adapter_fact_query_minute, null);
			mHolder.tvStationName = (TextView) convertView.findViewById(R.id.tvStationName);
			mHolder.tvStationCode = (TextView) convertView.findViewById(R.id.tvStationCode);
			mHolder.tvTime = (TextView) convertView.findViewById(R.id.tvTime);
			mHolder.tvRain = (TextView) convertView.findViewById(R.id.tvRain);
			convertView.setTag(mHolder);
		}else {
			mHolder = (ViewHolder) convertView.getTag();
		}

		FactDto dto = mArrayList.get(position);
		
		if (dto.stationName != null) {
			mHolder.tvStationName.setText(dto.stationName);
		}
		if (dto.stationCode != null) {
			mHolder.tvStationCode.setText(dto.stationCode);
		}
		if (dto.time != null) {
			mHolder.tvTime.setText(dto.time);
		}
		if (dto.rain != null) {
			mHolder.tvRain.setText(dto.rain);
		}

		return convertView;
	}

}
