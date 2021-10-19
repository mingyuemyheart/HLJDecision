package com.hlj.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hlj.common.ColumnData;
import com.hlj.stickygridheaders.StickyGridHeadersSimpleAdapter;

import java.util.List;

import shawn.cxwl.com.hlj.R;

/**
 * 模块管理
 */
public class ManageAdapter extends BaseAdapter implements StickyGridHeadersSimpleAdapter {

	private Context mContext;
	private LayoutInflater mInflater;
	private List<ColumnData> mArrayList;

	public ManageAdapter(Context context, List<ColumnData> mArrayList) {
		mContext = context;
		this.mArrayList = mArrayList;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	private class HeaderViewHolder {
		TextView tvHeader;
	}

	@Override
	public long getHeaderId(int position) {
		return mArrayList.get(position).section;
	}

	@Override
	public View getHeaderView(int position, View convertView, ViewGroup parent) {
		final HeaderViewHolder headerViewHolder;
		if (convertView == null) {
			headerViewHolder = new HeaderViewHolder();
			convertView = mInflater.inflate(R.layout.adapter_manage_header, null);
			headerViewHolder.tvHeader = convertView.findViewById(R.id.tvHeader);
			convertView.setTag(headerViewHolder);
		} else {
			headerViewHolder = (HeaderViewHolder) convertView.getTag();
		}

		final ColumnData dto = mArrayList.get(position);

//		if (dto.isSelected) {
//			headerViewHolder.tvHeader.setBackgroundResource(R.drawable.bg_corner_blue);
//			headerViewHolder.tvHeader.setTextColor(mContext.getResources().getColor(R.color.white));
//		} else {
//			headerViewHolder.tvHeader.setBackgroundResource(R.drawable.bg_corner_gray);
//			headerViewHolder.tvHeader.setTextColor(mContext.getResources().getColor(R.color.text_color3));
//		}

		headerViewHolder.tvHeader.setText(dto.headerName);
//		convertView.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				dto.isSelected = !dto.isSelected;
//				if (dto.isSelected) {
//					headerViewHolder.tvHeader.setBackgroundResource(R.drawable.bg_corner_blue);
//					headerViewHolder.tvHeader.setTextColor(mContext.getResources().getColor(R.color.white));
//				} else {
//					headerViewHolder.tvHeader.setBackgroundResource(R.drawable.bg_corner_gray);
//					headerViewHolder.tvHeader.setTextColor(mContext.getResources().getColor(R.color.text_color3));
//				}
//				for (ColumnData data : mArrayList) {
//					if (TextUtils.equals(data.headerName, dto.headerName)) {
//						data.isSelected = dto.isSelected;
//					}
//				}
//				notifyDataSetChanged();
//			}
//		});

		return convertView;
	}


	private class ChildViewHolder {
		TextView tvItemName;
	}

	@Override
	public int getCount() {
		return mArrayList.size();
	}

	@Override
	public Object getItem(int position) {
		return mArrayList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ChildViewHolder childViewHolder;
		if (convertView == null) {
			childViewHolder = new ChildViewHolder();
			convertView = mInflater.inflate(R.layout.adapter_manage_content, null);
			childViewHolder.tvItemName = convertView.findViewById(R.id.tvItemName);
			convertView.setTag(childViewHolder);
		} else {
			childViewHolder = (ChildViewHolder) convertView.getTag();
		}

		final ColumnData dto = mArrayList.get(position);

		if (dto.name != null) {
			childViewHolder.tvItemName.setText(dto.name);
			if (dto.name.length() > 10) {
				childViewHolder.tvItemName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
			}
		}

		if (dto.isSelected) {
			childViewHolder.tvItemName.setBackgroundResource(R.drawable.bg_corner_blue);
			childViewHolder.tvItemName.setTextColor(mContext.getResources().getColor(R.color.white));
		} else {
			childViewHolder.tvItemName.setBackgroundResource(R.drawable.bg_corner_gray);
			childViewHolder.tvItemName.setTextColor(mContext.getResources().getColor(R.color.text_color3));
		}

		convertView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dto.isSelected = !dto.isSelected;
				if (dto.isSelected) {
					childViewHolder.tvItemName.setBackgroundResource(R.drawable.bg_corner_blue);
					childViewHolder.tvItemName.setTextColor(mContext.getResources().getColor(R.color.white));
				} else {
					childViewHolder.tvItemName.setBackgroundResource(R.drawable.bg_corner_gray);
					childViewHolder.tvItemName.setTextColor(mContext.getResources().getColor(R.color.text_color3));
				}
				notifyDataSetChanged();
			}
		});

		return convertView;
	}

}
