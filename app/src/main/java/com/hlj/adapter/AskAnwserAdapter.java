package com.hlj.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hlj.dto.AgriDto;
import com.squareup.picasso.Picasso;

import java.util.List;

import shawn.cxwl.com.hlj.R;

/**
 * 提问解答
 */
public class AskAnwserAdapter extends BaseAdapter {

	private LayoutInflater mInflater;
	private List<AgriDto> mArrayList;

	private final class ViewHolder{
		ImageView imageView;
		TextView tvTitle,tvReply,tvTime;
	}

	public AskAnwserAdapter(Context context, List<AgriDto> mArrayList) {
		this.mArrayList = mArrayList;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
			convertView = mInflater.inflate(R.layout.adapter_ask_anwser, null);
			mHolder = new ViewHolder();
			mHolder.imageView = convertView.findViewById(R.id.imageView);
			mHolder.tvTitle = convertView.findViewById(R.id.tvTitle);
			mHolder.tvReply = convertView.findViewById(R.id.tvReply);
			mHolder.tvTime = convertView.findViewById(R.id.tvTime);
			convertView.setTag(mHolder);
		}else {
			mHolder = (ViewHolder) convertView.getTag();
		}

		AgriDto dto = mArrayList.get(position);

		if (!TextUtils.isEmpty(dto.imgUrl)) {
			Picasso.get().load(dto.imgUrl).error(R.drawable.icon_no_bitmap).into(mHolder.imageView);
			mHolder.imageView.setVisibility(View.VISIBLE);
		} else {
			mHolder.imageView.setVisibility(View.GONE);
		}

		if (!TextUtils.isEmpty(dto.time)) {
			mHolder.tvTime.setText(dto.time);
		}
		if (!TextUtils.isEmpty(dto.title)) {
			mHolder.tvTitle.setText(dto.title);
		}
		if (!TextUtils.isEmpty(dto.reply_content)) {
			mHolder.tvReply.setText(dto.reply_content);
		}

		return convertView;
	}

}
