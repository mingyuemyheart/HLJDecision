package com.hlj.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hlj.dto.ContactDto;

import java.util.List;

import shawn.cxwl.com.hlj.R;

/**
 * 通讯录
 */
public class AddrBookAdapter extends BaseAdapter{

	private Context mContext;
	private LayoutInflater mInflater;
	private List<ContactDto> mArrayList;

	private final class ViewHolder {
		TextView tvName,tvNumber;
		ImageView ivArrow;
	}

	public AddrBookAdapter(Context context, List<ContactDto> mArrayList) {
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
			convertView = mInflater.inflate(R.layout.adapter_addr_book, null);
			mHolder = new ViewHolder();
			mHolder.tvName = convertView.findViewById(R.id.tvName);
			mHolder.tvNumber = convertView.findViewById(R.id.tvNumber);
			mHolder.ivArrow = convertView.findViewById(R.id.ivArrow);
			convertView.setTag(mHolder);
		}else {
			mHolder = (ViewHolder) convertView.getTag();
		}

		ContactDto dto = mArrayList.get(position);

		if (TextUtils.equals(dto.type, "0")) {
			if (!TextUtils.isEmpty(dto.name)) {
				mHolder.tvName.setText(dto.name);
			}
			if (!TextUtils.isEmpty(dto.worktelephone)) {
				mHolder.tvNumber.setText(dto.worktelephone);
			}
			mHolder.ivArrow.setVisibility(View.GONE);
		} else {
			if (!TextUtils.isEmpty(dto.company)) {
				mHolder.tvName.setText(dto.company);
			}
			mHolder.ivArrow.setVisibility(View.VISIBLE);
		}

		return convertView;
	}

}
