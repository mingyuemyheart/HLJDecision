package com.hlj.adapter;

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.hlj.dto.WeatherStaticsDto
import shawn.cxwl.com.hlj.R

/**
 * 负氧离子
 */
class FylzAdapter constructor(context: Context?, private var mArrayList: MutableList<WeatherStaticsDto>) : BaseAdapter() {

    private val mInflater = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private class ViewHolder {
        var tvName: TextView? = null
        var tvGasa: TextView? = null
        var tvTime: TextView? = null
    }

    override fun getCount(): Int {
        return mArrayList.size
    }

    override fun getItem(position: Int): Any? {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup?): View? {
        val mHolder: ViewHolder
        var convertView = view
        if (convertView == null) {
            convertView = mInflater!!.inflate(R.layout.adapter_fylz, null)
            mHolder = ViewHolder()
            mHolder!!.tvName = convertView.findViewById<View>(R.id.tvName) as TextView
            mHolder!!.tvGasa = convertView.findViewById<View>(R.id.tvGasa) as TextView
            mHolder!!.tvTime = convertView.findViewById<View>(R.id.tvTime) as TextView
            convertView.tag = mHolder
        } else {
            mHolder = convertView.tag as ViewHolder
        }

        val dto = mArrayList[position]

        if (dto.name != null) {
            mHolder!!.tvName!!.text = dto.name
        }
        if (dto.name != null) {
            mHolder!!.tvGasa!!.text = dto.gasa
        }
        if (dto.name != null) {
            mHolder!!.tvTime!!.text = dto.tm
        }

        return convertView
    }

}
