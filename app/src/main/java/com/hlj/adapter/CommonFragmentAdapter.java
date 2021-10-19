package com.hlj.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hlj.dto.AgriDto;
import com.hlj.utils.CommonUtil;
import com.squareup.picasso.Picasso;

import java.util.List;

import shawn.cxwl.com.hlj.R;

/**
 * 天气预报、天气实况、电力气象服务、铁路气象服务
 */
public class CommonFragmentAdapter extends BaseAdapter{
	
	private Context mContext;
	private LayoutInflater mInflater;
	private List<AgriDto> mArrayList;

	private final class ViewHolder {
		ImageView imageView;
		TextView tvName;
	}

	public CommonFragmentAdapter(Context context, List<AgriDto> mArrayList) {
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
			convertView = mInflater.inflate(R.layout.adapter_common_fragment, null);
			mHolder = new ViewHolder();
			mHolder.imageView = convertView.findViewById(R.id.imageView);
			mHolder.tvName = convertView.findViewById(R.id.tvName);
			convertView.setTag(mHolder);
		}else {
			mHolder = (ViewHolder) convertView.getTag();
		}
		
		AgriDto dto = mArrayList.get(position);
		if (!TextUtils.isEmpty(dto.name)) {
			mHolder.tvName.setText(dto.name);
		}

		if (TextUtils.isEmpty(dto.icon)) {
			mHolder.imageView.setImageResource(R.drawable.iv_hlj2);
		}else {
			Picasso.get().load(dto.icon).into(mHolder.imageView);
		}

		AbsListView.LayoutParams params = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.MATCH_PARENT);
		params.width = CommonUtil.widthPixels(mContext)/3;
		params.height = CommonUtil.widthPixels(mContext)/3-30;
		convertView.setLayoutParams(params);
		
		return convertView;
	}

}
