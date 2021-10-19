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
		TextView tvName,tvUnit;
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
			mHolder.tvUnit = convertView.findViewById(R.id.tvUnit);
			convertView.setTag(mHolder);
		}else {
			mHolder = (ViewHolder) convertView.getTag();
		}

		ContactDto dto = mArrayList.get(position);

		if (TextUtils.equals(dto.type, "0")) {
			if (dto.name != null) {
				mHolder.tvName.setText(dto.name);
			} else if (dto.company != null) {
				mHolder.tvName.setText(dto.company);
			}

			if (dto.level != null) {
				mHolder.tvUnit.setText(dto.level);
			}
			mHolder.tvUnit.setVisibility(View.VISIBLE);
		} else {
			if (dto.company != null) {
				mHolder.tvName.setText(dto.company);
			} else if (dto.name != null) {
				mHolder.tvName.setText(dto.name);
			}
			mHolder.tvUnit.setText("");
			mHolder.tvUnit.setVisibility(View.GONE);
		}

		return convertView;
	}

}
