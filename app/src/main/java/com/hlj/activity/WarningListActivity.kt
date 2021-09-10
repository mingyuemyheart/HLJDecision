package com.hlj.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.View.OnClickListener
import android.widget.AdapterView.OnItemClickListener
import com.hlj.adapter.WarningAdapter
import com.hlj.adapter.WarningListScreenAdapter
import com.hlj.dto.WarningDto
import com.hlj.utils.CommonUtil
import kotlinx.android.synthetic.main.activity_warning_list.*
import kotlinx.android.synthetic.main.layout_title.*
import shawn.cxwl.com.hlj.R
import java.util.*

/**
 * 预警列表
 */
class WarningListActivity : BaseActivity(), OnClickListener {

    private var cityAdapter: WarningAdapter? = null
    private val warningList: MutableList<WarningDto> = ArrayList() //上个界面传过来的所有预警数据
    private val showList: MutableList<WarningDto> = ArrayList() //用于存放listview上展示的数据
    private val searchList: MutableList<WarningDto> = ArrayList() //用于存放搜索框搜索的数据
    private val selecteList: MutableList<WarningDto> = ArrayList() //用于存放三个sppiner删选的数据
    private val gridList: MutableList<WarningDto> = ArrayList()
    private var section = 1
    private val sectionMap = HashMap<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_warning_list)
        initWidget()
        initListView()
        initGridView()
    }

    /**
     * 初始化控件
     */
    private fun initWidget() {
        llBack.setOnClickListener(this)
        tvTitle.text = "预警列表"
        etSearch.addTextChangedListener(watcher)

        if (intent.hasExtra("isVisible")) {
            val isVisible = intent.getBooleanExtra("isVisible", false)
            if (isVisible) {
                tvControl.text = "选择地区"
                tvControl.visibility = View.VISIBLE
                tvControl.setOnClickListener(this)
            }
        }
        warningList.addAll(intent.extras.getParcelableArrayList("warningList"))
        showList.addAll(warningList)

        if (warningList.size > 0) {
            tvPrompt.visibility = View.GONE
        } else {
            tvPrompt.visibility = View.VISIBLE
        }
    }

    private val watcher: TextWatcher = object : TextWatcher {
        override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
        override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
        override fun afterTextChanged(arg0: Editable) {
            searchList.clear()
            if (!TextUtils.isEmpty(arg0.toString().trim { it <= ' ' })) {
                animation()
                for (i in warningList.indices) {
                    val data = warningList[i]
                    if (data.name.contains(arg0.toString().trim { it <= ' ' })) {
                        searchList.add(data)
                    }
                }
                showList.clear()
                showList.addAll(searchList)
                cityAdapter!!.notifyDataSetChanged()
            } else {
                showList.clear()
                showList.addAll(warningList)
                cityAdapter!!.notifyDataSetChanged()
            }
        }
    }

    /**
     * 初始化listview
     */
    private fun initListView() {
        cityAdapter = WarningAdapter(this, showList, false)
        cityListView.adapter = cityAdapter
        cityListView.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 ->
            val data = showList[arg2]
            val intentDetail = Intent(this, WarningDetailActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable("data", data)
            intentDetail.putExtras(bundle)
            startActivity(intentDetail)
        }
    }

    /**
     * 初始化listview
     */
    private fun initGridView() {
        gridList.clear()
        val array3 = resources.getStringArray(R.array.warningDis)
        for (i in array3.indices) {
            val map = HashMap<String, Int>()
            val value = array3[i].split(",").toTypedArray()
            var count = 0
            for (j in warningList.indices) {
                val dto2 = warningList[j]
                val array = dto2.html.split("-").toTypedArray()
                val warningId = array[0]
                if (TextUtils.equals(warningId, value[3])) {
                    map[warningId] = count++
                }
            }
            val dto = WarningDto()
            dto.sectionName = value[0]
            dto.areaName = value[1]
            dto.warningId = value[3]
            dto.count = count
            if (i == 0 || count != 0) {
                gridList.add(dto)
            }
        }
        for (i in gridList.indices) {
            val sectionDto = gridList[i]
            if (!sectionMap.containsKey(sectionDto.sectionName)) {
                sectionDto.section = section
                sectionMap[sectionDto.sectionName] = section
                section++
            } else {
                sectionDto.section = sectionMap[sectionDto.sectionName]!!
            }
            gridList[i] = sectionDto
        }
        val adapter3 = WarningListScreenAdapter(this, gridList)
        gridView3.adapter = adapter3
        gridView3.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 ->
            val dto = gridList[arg2]
            animation()
            selecteList.clear()
            for (i in warningList.indices) {
                if (warningList[i].html.startsWith(dto.warningId)) {
                    selecteList.add(warningList[i])
                }
            }
            showList.clear()
            showList.addAll(selecteList)
            cityAdapter!!.notifyDataSetChanged()
        }
    }


    private fun animation() {
        if (gridView3!!.visibility == View.GONE) {
            CommonUtil.topToBottom(gridView3)
        } else {
            CommonUtil.bottomToTop(gridView3)
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.llBack -> finish()
            R.id.tvControl -> animation()
        }
    }

}
