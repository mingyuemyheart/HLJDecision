package com.hlj.adapter

import android.content.Context
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.LinearLayout
import android.widget.TextView
import com.hlj.interfaces.SelectListener
import com.hlj.dto.FactDto
import com.hlj.interfaces.SelectStationListener
import com.hlj.utils.CommonUtil
import shawn.cxwl.com.hlj.R

/**
 * 实况站点查询-选择区域
 */
class FactQueryAreaAdapter constructor(private var context: Context?, private var groupList: List<FactDto>?, private var childList: List<List<FactDto>>?, private var listView: ExpandableListView?) : BaseExpandableListAdapter() {

    private val mInflater = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private var isSelectArea = true

    fun setSelectArea(isSelectArea: Boolean) {
        this.isSelectArea = isSelectArea
    }

    internal class GroupHolder {
        var tvCityName: TextView? = null
    }

    internal class ChildHolder {
        var tvAreaName: TextView? = null
        var llContainer: LinearLayout? = null
    }

    override fun getGroupCount(): Int {
        return groupList!!.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return childList!![groupPosition].size
    }

    override fun getGroup(groupPosition: Int): Any? {
        return groupList!![groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any? {
        return childList!![groupPosition][childPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, view: View?, parent: ViewGroup?): View? {
        var convertView = view
        val groupHolder: GroupHolder
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.adapter_fact_query_area_group, null)
            groupHolder = GroupHolder()
            groupHolder.tvCityName = convertView.findViewById(R.id.tvCityName)
            convertView.tag = groupHolder
        } else {
            groupHolder = convertView.tag as GroupHolder
        }

        val dto = groupList!![groupPosition]
        if (!TextUtils.isEmpty(dto.area)) {
            groupHolder.tvCityName!!.text = dto.area
        }

        return convertView
    }

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, view: View?, parent: ViewGroup?): View? {
        var convertView = view
        val childHolder: ChildHolder
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.adapter_fact_query_area_child, null)
            childHolder = ChildHolder()
            childHolder.tvAreaName = convertView.findViewById(R.id.tvAreaName)
            childHolder.llContainer = convertView.findViewById(R.id.llContainer)
            convertView.tag = childHolder
        } else {
            childHolder = convertView.tag as ChildHolder
        }

        val dto = childList!![groupPosition][childPosition]
        if (!TextUtils.isEmpty(dto.area)) {
            childHolder.tvAreaName!!.text = dto.area
        }

        if (!isSelectArea) {
            childHolder.llContainer!!.removeAllViews()
            for (i in 0 until dto.itemList.size) {
                val item = dto.itemList[i]
                val tv = TextView(context)
                tv.text = "${item.stationName}(${item.stationCode})"
                tv.setTextColor(ContextCompat.getColor(context!!, R.color.text_color4))
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                tv.setPadding(CommonUtil.dip2px(context, 70f).toInt(), CommonUtil.dip2px(context, 10f).toInt(), 0, CommonUtil.dip2px(context, 10f).toInt())
                childHolder.llContainer!!.addView(tv)

                tv.setOnClickListener {
                    selectStationListener!!.success(item.stationName, item.stationCode)
                }
            }
        }

        return convertView
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    private var selectStationListener: SelectStationListener? = null

    fun setSelectListener(selectStationListener: SelectStationListener?) {
        this.selectStationListener = selectStationListener
    }

}
