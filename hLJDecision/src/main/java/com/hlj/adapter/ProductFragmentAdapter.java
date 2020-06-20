package com.hlj.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hlj.dto.NewsDto;
import com.hlj.utils.CommonUtil;

import net.tsz.afinal.FinalBitmap;

import java.util.ArrayList;
import java.util.List;

import shawn.cxwl.com.hlj.R;

public class ProductFragmentAdapter extends BaseAdapter{
	
	private Context mContext = null;
	private LayoutInflater mInflater = null;
	private List<NewsDto> mArrayList = new ArrayList<NewsDto>();

	private final class ViewHolder{
		ImageView imageView;
		TextView tvTitle;
	}
	
	private ViewHolder mHolder = null;
	
	public ProductFragmentAdapter(Context context, List<NewsDto> mArrayList) {
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
			convertView = mInflater.inflate(R.layout.product_item, null);
			mHolder = new ViewHolder();
			mHolder.imageView = (ImageView) convertView.findViewById(R.id.imageView);
			mHolder.tvTitle = (TextView) convertView.findViewById(R.id.tvTitle);
			convertView.setTag(mHolder);
		}else {
			mHolder = (ViewHolder) convertView.getTag();
		}
		
		NewsDto dto = mArrayList.get(position);
		mHolder.tvTitle.setText(dto.title);
		
		if (TextUtils.isEmpty(dto.imgUrl)) {
			mHolder.imageView.setImageResource(R.drawable.iv_no_pic);
		}else {
			FinalBitmap finalBitmap = FinalBitmap.create(mContext);
			finalBitmap.display(mHolder.imageView, dto.imgUrl, null, (int)(CommonUtil.dip2px(mContext, 5)));
			mHolder.imageView.setBackgroundResource(R.drawable.corner_image_border);
			int dp = (int) CommonUtil.dip2px(mContext, 1);
			mHolder.imageView.setPadding(dp, dp, dp, dp);
		}
		
		return convertView;
	}

}
