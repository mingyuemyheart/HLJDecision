package com.hlj.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.hlj.dto.DisasterDto;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import shawn.cxwl.com.hlj.R;

/**
 * 灾情反馈
 */
public class DisasterUploadAdapter extends BaseAdapter {

	private Context mContext;
	private LayoutInflater mInflater;
	private ArrayList<DisasterDto> mArrayList;
	private LinearLayout.LayoutParams params;
	private int itemWidth;

	private final class ViewHolder{
		ImageView imageView;
	}

	public DisasterUploadAdapter(Context context, ArrayList<DisasterDto> mArrayList, int itemWidth) {
		mContext = context;
		this.itemWidth = itemWidth;
		this.mArrayList = mArrayList;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		params = new LinearLayout.LayoutParams(itemWidth, itemWidth);
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
			convertView = mInflater.inflate(R.layout.adapter_disaster_upload, null);
			mHolder = new ViewHolder();
			mHolder.imageView = convertView.findViewById(R.id.imageView);
			convertView.setTag(mHolder);
		}else {
			mHolder = (ViewHolder) convertView.getTag();
		}
		
		DisasterDto dto = mArrayList.get(position);

		if (!dto.isLastItem) {
			if (!TextUtils.isEmpty(dto.imgUrl)) {
				File file = new File(dto.imgUrl);
				if (file.exists()) {
					Picasso.get().load(file).centerCrop().resize(200, 200).into(mHolder.imageView);
					mHolder.imageView.setPadding(0,0,0,0);
					mHolder.imageView.setLayoutParams(params);
				}
			}
		}else {
			mHolder.imageView.setBackgroundColor(mContext.getResources().getColor(R.color.light_gray));
			mHolder.imageView.setImageResource(R.drawable.icon_add);
			mHolder.imageView.setPadding(itemWidth*3/5,itemWidth*3/5,itemWidth*3/5,itemWidth*3/5);
			mHolder.imageView.setLayoutParams(params);
		}

		return convertView;
	}

}
