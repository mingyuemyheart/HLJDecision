package com.hlj.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;


import com.hlj.activity.FactDetailActivity;
import com.hlj.dto.FactDto;
import com.hlj.utils.CommonUtil;

import java.util.ArrayList;
import java.util.List;

import shawn.cxwl.com.hlj.R;

/**
 * 实况资料
 */
public class FactMonitorAdapter extends BaseAdapter {
	
	private Context mContext;
	private LayoutInflater mInflater;
	private List<FactDto> mArrayList;
	public String stationName = "", area = "", val = "", timeString = "";
	public List<FactDto> realDatas = new ArrayList<>();
	
	private final class ViewHolder {
		TextView tvRailLevel;
		TextView tvCount;
		LinearLayout llContainer;
	}
	
	public FactMonitorAdapter(Context context, List<FactDto> mArrayList) {
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
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder mHolder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.adapter_fact_monitor, null);
			mHolder = new ViewHolder();
			mHolder.tvRailLevel = (TextView) convertView.findViewById(R.id.tvRailLevel);
			mHolder.tvCount = (TextView) convertView.findViewById(R.id.tvCount);
			mHolder.llContainer = (LinearLayout) convertView.findViewById(R.id.llContainer);
			convertView.setTag(mHolder);
		}else {
			mHolder = (ViewHolder) convertView.getTag();
		}
		
		FactDto dto = mArrayList.get(position);
		
		if (!TextUtils.isEmpty(dto.rainLevel)) {
			mHolder.tvRailLevel.setText(dto.rainLevel);
		}
		
		if (!TextUtils.isEmpty(dto.count)) {
			mHolder.tvCount.setText(dto.count);
		}
		
		mHolder.llContainer.removeAllViews();
		if (dto.areaList.size() > 0) {
			if (dto.areaList.size() <= 4) {
				mHolder.llContainer.setOrientation(LinearLayout.HORIZONTAL);
				for (int i = 0; i < dto.areaList.size(); i++) {
					final FactDto data = dto.areaList.get(i);
					if (!TextUtils.isEmpty(data.area)) {
						TextView tvArea = new TextView(mContext);
						tvArea.setGravity(Gravity.CENTER);
						tvArea.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
						tvArea.setTextColor(mContext.getResources().getColor(R.color.colorPrimary));
						tvArea.setPadding(10, 0, 10, 0);
						tvArea.setText(data.area);
						tvArea.setTag(data.area);
						mHolder.llContainer.addView(tvArea);
						tvArea.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View arg0) {
								Intent intent = new Intent(mContext, FactDetailActivity.class);
								intent.putExtra("title", data.area);
								intent.putExtra("timeString", timeString);
								intent.putExtra("stationName", stationName);
								intent.putExtra("area", area);
								intent.putExtra("val", val);
								Bundle bundle = new Bundle();
								List<FactDto> list = new ArrayList<>();
								for (int j = 0; j < realDatas.size(); j++) {
									if (realDatas.get(j).area.contains(data.area)) {
										list.add(realDatas.get(j));
									}
								}
								bundle.putParcelableArrayList("realDatas", (ArrayList<? extends Parcelable>) list);
								intent.putExtras(bundle);
								mContext.startActivity(intent);
							}
						});
					}
				}
			} else {
				mHolder.llContainer.setOrientation(LinearLayout.VERTICAL);
				mHolder.llContainer.setGravity(Gravity.CENTER|Gravity.CENTER_VERTICAL);
				int containerCount = 0;
				if (dto.areaList.size() % 4 == 0) {
					containerCount = dto.areaList.size() / 4;
				}else {
					containerCount = dto.areaList.size() / 4 + 1;
				}
				for (int j = 0; j < containerCount; j++) {
					LinearLayout llItem = new LinearLayout(mContext);
					llItem.setOrientation(LinearLayout.HORIZONTAL);
					llItem.setGravity(Gravity.CENTER|Gravity.CENTER_VERTICAL);
					
					int i = 0;
					int size = j*4+4;
					if (size >= dto.areaList.size()) {
						size = dto.areaList.size();
					}
					for (i = j*4; i < size; i++) {
						final FactDto data = dto.areaList.get(i);
						if (!TextUtils.isEmpty(data.area)) {
							TextView tvArea = new TextView(mContext);
							tvArea.setGravity(Gravity.CENTER);
							tvArea.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
							tvArea.setTextColor(mContext.getResources().getColor(R.color.colorPrimary));
							tvArea.setPadding(10, 0, 10, 0);
							tvArea.setText(data.area);
							tvArea.setTag(data.area);
							llItem.addView(tvArea);
							LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
							params.topMargin = (int) CommonUtil.dip2px(mContext, 5);
							params.bottomMargin = (int)CommonUtil.dip2px(mContext, 5);
							llItem.setLayoutParams(params);
							tvArea.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View arg0) {
									Intent intent = new Intent(mContext, FactDetailActivity.class);
									intent.putExtra("title", data.area);
									intent.putExtra("timeString", timeString);
									intent.putExtra("stationName", stationName);
									intent.putExtra("area", area);
									intent.putExtra("val", val);
									Bundle bundle = new Bundle();
									List<FactDto> list = new ArrayList<>();
									for (int j = 0; j < realDatas.size(); j++) {
										if (realDatas.get(j).area.contains(data.area)) {
											list.add(realDatas.get(j));
										}
									}
									bundle.putParcelableArrayList("realDatas", (ArrayList<? extends Parcelable>) list);
									intent.putExtras(bundle);
									mContext.startActivity(intent);
								}
							});
						}
					}
					mHolder.llContainer.addView(llItem);
				}
			}
		} else {
			TextView tvArea = new TextView(mContext);
			tvArea.setGravity(Gravity.CENTER);
			tvArea.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
			tvArea.setTextColor(mContext.getResources().getColor(R.color.text_color3));
			tvArea.setPadding(10, 0, 10, 0);
			tvArea.setText("无");
			mHolder.llContainer.addView(tvArea);
		}
		
		if (position % 2 == 0) {
			convertView.setBackgroundColor(0xfff5f5f5);
		}else {
			convertView.setBackgroundColor(0xffeaeaea);
		}
		
		return convertView;
	}

}
