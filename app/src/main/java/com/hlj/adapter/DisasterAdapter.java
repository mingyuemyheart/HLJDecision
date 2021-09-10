package com.hlj.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hlj.dto.DisasterDto;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import shawn.cxwl.com.hlj.R;

/**
 * 灾情列表
 */
public class DisasterAdapter extends BaseAdapter {

	private Context context;
	private LayoutInflater mInflater;
	private List<DisasterDto> mArrayList;
	private List<DisasterDto> typeList = new ArrayList<>();

	private final class ViewHolder{
		ImageView imageView;
		TextView tvTitle,tvType,tvAddr,tvTime,tvStatus;
	}

	public DisasterAdapter(Context context, List<DisasterDto> mArrayList) {
		this.context = context;
		this.mArrayList = mArrayList;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		typeList.clear();
//		String[] array = context.getResources().getStringArray(R.array.disaster_type);
//		for (int i = 0; i < array.length; i++) {
//			String[] value = array[i].split(",");
//			DisasterDto dto = new DisasterDto();
//			dto.disasterType = value[0];
//			dto.disasterName = value[1];
//			typeList.add(dto);
//		}
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
			convertView = mInflater.inflate(R.layout.adapter_disaster, null);
			mHolder = new ViewHolder();
			mHolder.imageView = convertView.findViewById(R.id.imageView);
			mHolder.tvTitle = convertView.findViewById(R.id.tvTitle);
			mHolder.tvType = convertView.findViewById(R.id.tvType);
			mHolder.tvAddr = convertView.findViewById(R.id.tvAddr);
			mHolder.tvTime = convertView.findViewById(R.id.tvTime);
			mHolder.tvStatus = convertView.findViewById(R.id.tvStatus);
			convertView.setTag(mHolder);
		}else {
			mHolder = (ViewHolder) convertView.getTag();
		}

		DisasterDto dto = mArrayList.get(position);

		if (dto.imgList.size() > 0) {
			String imgUrl = dto.imgList.get(0);
			if (!TextUtils.isEmpty(imgUrl)) {
				Picasso.get().load(imgUrl).error(R.drawable.icon_no_bitmap).into(mHolder.imageView);
			} else {
				mHolder.imageView.setImageResource(R.drawable.icon_no_bitmap);
			}
		} else {
			mHolder.imageView.setImageResource(R.drawable.icon_no_bitmap);
		}

		if (!TextUtils.isEmpty(dto.content)) {
			mHolder.tvTitle.setText(dto.content);
		}

		if (!TextUtils.isEmpty(dto.addr)) {
			mHolder.tvAddr.setText(dto.addr);
		}

		if (dto.status_cn != null) {
			mHolder.tvStatus.setText(dto.status_cn);
		}
		if (TextUtils.equals(dto.status_cn, "审核中")) {
			mHolder.tvStatus.setTextColor(context.getResources().getColor(R.color.text_color4));
		} else if (TextUtils.equals(dto.status_cn, "审核不通过")) {
			mHolder.tvStatus.setTextColor(context.getResources().getColor(R.color.red));
		} else if (TextUtils.equals(dto.status_cn, "审核通过")) {
			mHolder.tvStatus.setTextColor(context.getResources().getColor(R.color.colorPrimary));
		}

		if (!TextUtils.isEmpty(dto.time)) {
			mHolder.tvTime.setText(dto.time);
		}

		if (!TextUtils.isEmpty(dto.disasterType)) {
			mHolder.tvType.setText(dto.disasterType);
		}
//		if (!TextUtils.isEmpty(dto.disasterType)) {
//			for (DisasterDto data : typeList) {
//				if (TextUtils.equals(dto.disasterType, data.disasterType)) {
//					dto.disasterName = data.disasterName;
//					break;
//				}
//			}
//			if (!TextUtils.isEmpty(dto.disasterName)) {
//				mHolder.tvType.setText(dto.disasterName);
//			}
//		}

		return convertView;
	}

}
