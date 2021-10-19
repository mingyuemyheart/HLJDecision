package com.hlj.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.View.OnClickListener
import com.hlj.adapter.FactQueryAreaAdapter
import com.hlj.dto.FactDto
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.activity_fact_query_area.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException
import java.util.*

/**
 * 实况站点查询-选择区域
 */
class FactQueryAreaActivity : BaseActivity(), OnClickListener {

    private var mAdapter: FactQueryAreaAdapter? = null
    private val groupList: MutableList<FactDto> = ArrayList()
    private val childList: MutableList<MutableList<FactDto>> = ArrayList()
    private var city = ""
    private var area = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fact_query_area)
        initWidget()
        initListView()
    }

    private fun initWidget() {
        llBack!!.setOnClickListener(this)
        tvTitle.text = "选择区域"

        okHttpList()
    }

    private fun initListView() {
        mAdapter = FactQueryAreaAdapter(this, groupList, childList, listView)
        listView.setAdapter(mAdapter)
        listView.setOnGroupClickListener { parent, v, groupPosition, id ->
            val dto = groupList[groupPosition]
            city = dto.area
            if (listView.isGroupExpanded(groupPosition)) {
                listView.collapseGroup(groupPosition)
            } else {
                listView.expandGroup(groupPosition)
            }
            true
        }
        listView.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
            val dto = childList[groupPosition][childPosition]
            area = dto.area
            val intent = Intent()
            intent.putExtra("city", city)
            intent.putExtra("area", area)
            setResult(RESULT_OK, intent)
            finish()
            false
        }
    }

    private fun okHttpList() {
        showDialog()
        Thread {
            val url = "http://decision-admin.tianqi.cn/Home/workwsj/hlj_area"
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    runOnUiThread {
                        cancelDialog()
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                val obj = JSONObject(result)
                                if (!obj.isNull("area")) {
                                    groupList.clear()
                                    childList.clear()
                                    val array = obj.getJSONArray("area")
                                    for (i in 0 until array.length()) {
                                        val itemObj = array.getJSONObject(i)
                                        val dto = FactDto()
                                        if (!itemObj.isNull("city")) {
                                            dto.area = itemObj.getString("city")
                                        }
                                        if (!itemObj.isNull("list")) {
                                            val list: MutableList<FactDto> = ArrayList()
                                            val listArray = itemObj.getJSONArray("list")
                                            for (j in 0 until listArray.length()) {
                                                val listObj = listArray.getJSONObject(j)
                                                val d = FactDto()
                                                if (!listObj.isNull("name")) {
                                                    d.area = listObj.getString("name")
                                                }
                                                if (!listObj.isNull("list")) {
                                                    val itemList: MutableList<FactDto> = ArrayList()
                                                    val listArray2 = listObj.getJSONArray("list")
                                                    for (m in 0 until listArray2.length()) {
                                                        val listObj2 = listArray2.getJSONObject(m)
                                                        val d2 = FactDto()
                                                        if (!listObj2.isNull("name")) {
                                                            d2.stationName = listObj2.getString("name")
                                                        }
                                                        if (!listObj2.isNull("stationCode")) {
                                                            d2.stationCode = listObj2.getString("stationCode")
                                                        }
                                                        itemList.add(d2)
                                                    }
                                                    d.itemList.addAll(itemList)
                                                }
                                                list.add(d)
                                            }
                                            childList.add(list)
                                        }
                                        groupList.add(dto)
                                    }
                                    if (mAdapter != null) {
                                        mAdapter!!.notifyDataSetChanged()
                                    }
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            })
        }.start()
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.llBack -> finish()
        }
    }
	
}
