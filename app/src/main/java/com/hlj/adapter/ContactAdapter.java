package com.hlj.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hlj.common.ColumnData;

import java.util.List;

import shawn.cxwl.com.hlj.R;

/**
 * 预警列表
 */
public class ContactAdapter extends BaseAdapter{

	private Context mContext;
	private LayoutInflater mInflater;
	private List<ColumnData> mArrayList;
	private String flag = "1";

	public String getFlag() {
		return flag;
	}

	public void setFlag(String flag) {
		this.flag = flag;
	}

	private final class ViewHolder {
		TextView tvName,tvLogo;
	}

	public ContactAdapter(Context context, List<ColumnData> mArrayList) {
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
			convertView = mInflater.inflate(R.layout.adapter_contact, null);
			mHolder = new ViewHolder();
			mHolder.tvName = convertView.findViewById(R.id.tvName);
			mHolder.tvLogo = convertView.findViewById(R.id.tvLogo);
			convertView.setTag(mHolder);
		}else {
			mHolder = (ViewHolder) convertView.getTag();
		}

		ColumnData dto = mArrayList.get(position);

		if (!TextUtils.isEmpty(dto.name)) {
			mHolder.tvName.setText(dto.name);
		}
		if (TextUtils.equals(flag, "1")) {
			mHolder.tvLogo.setText("");
		} else if (TextUtils.equals(flag, "2")) {
			mHolder.tvLogo.setText("公众");
			mHolder.tvLogo.setTextColor(Color.WHITE);
			mHolder.tvLogo.setBackgroundResource(R.drawable.bg_public);
		} else if (TextUtils.equals(flag, "3")) {
			mHolder.tvLogo.setText("专业");
			mHolder.tvLogo.setTextColor(Color.BLACK);
			mHolder.tvLogo.setBackgroundResource(R.drawable.bg_special);
		}

		return convertView;
	}

}
