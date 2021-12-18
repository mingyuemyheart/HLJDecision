package com.hlj.activity

import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import com.hlj.adapter.HTemperatureForecastAdapter
import com.hlj.dto.AgriDto
import kotlinx.android.synthetic.main.activity_temp_fore.*
import kotlinx.android.synthetic.main.layout_title.*
import shawn.cxwl.com.hlj.R
import java.util.*

/**
 * 气温预报、雾霾预报、降温大风沙尘预报  同一种类型
 * @author shawn_sun
 */
class TempForeActivity : BaseActivity(), OnClickListener {

    private var mAdapter: HTemperatureForecastAdapter? = null
    private val dataList: MutableList<AgriDto> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temp_fore)
        initWidget()
        initListView()
    }

    private fun initWidget() {
        llBack!!.setOnClickListener(this)

        val data: AgriDto = intent.extras.getParcelable("data")
        if (data != null) {
            tvTitle!!.text = data.name
            dataList.clear()
            for (i in data.child.indices) {
                val dto = AgriDto()
                dto.name = data.child[i].name
                dto.dataUrl = data.child[i].dataUrl
                dataList.add(dto)
            }
            if (mAdapter != null) {
                mAdapter!!.notifyDataSetChanged()
            }
        }
    }

    private fun initListView() {
        mAdapter = HTemperatureForecastAdapter(this, dataList, this@TempForeActivity)
        listView!!.adapter = mAdapter
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.llBack -> finish()
        }
    }
	
}
