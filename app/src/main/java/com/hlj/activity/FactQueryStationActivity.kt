package com.hlj.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.View.OnClickListener
import com.hlj.adapter.FactQueryAreaAdapter
import com.hlj.adapter.FactQueryStationAdapter
import com.hlj.dto.FactDto
import com.hlj.interfaces.SelectStationListener
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.activity_fact_query_area.*
import kotlinx.android.synthetic.main.activity_fact_query_station.*
import kotlinx.android.synthetic.main.activity_fact_query_station.listView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException
import java.util.*

/**
 * 实况站点查询-搜索站点
 */
class FactQueryStationActivity : BaseActivity(), OnClickListener, SelectStationListener {

    private var mAdapter: FactQueryAreaAdapter? = null
    private val groupList: MutableList<FactDto> = ArrayList()
    private val childList: MutableList<MutableList<FactDto>> = ArrayList()

    //搜索
    private var searchAdapter: FactQueryStationAdapter? = null
    private val searchList: MutableList<FactDto> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fact_query_station)
        initWidget()
        initListView()
        initListViewSearch()
    }

    private fun initWidget() {
        llBack!!.setOnClickListener(this)
        tvSearch!!.setOnClickListener(this)
        etSearch.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?) {
                if (TextUtils.isEmpty(s.toString())) {
                    listView.visibility = View.VISIBLE
                    listViewSearch.visibility = View.GONE
                } else {
                    listView.visibility = View.GONE
                    listViewSearch.visibility = View.VISIBLE
                }
            }
        })

        okHttpList()
    }

    private fun initListView() {
        mAdapter = FactQueryAreaAdapter(this, groupList, childList, listView)
        listView.setAdapter(mAdapter)
        mAdapter!!.setSelectArea(false)
        mAdapter!!.setSelectListener(this)
        listView.setOnGroupClickListener { parent, v, groupPosition, id ->
            val dto = groupList[groupPosition]
            if (listView.isGroupExpanded(groupPosition)) {
                listView.collapseGroup(groupPosition)
            } else {
                listView.expandGroup(groupPosition)
            }
            true
        }
        listView.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
            val dto = childList[groupPosition][childPosition]
            true
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
                                                if (TextUtils.equals(d.area, "全市")) {
                                                    continue
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

    private fun initListViewSearch() {
        searchAdapter = FactQueryStationAdapter(this, searchList)
        listViewSearch!!.adapter = searchAdapter
        listViewSearch!!.setOnItemClickListener { parent, view, position, id ->
            val data = searchList[position]
            if (!TextUtils.isEmpty(data.stationCode)) {
                val intent = Intent()
                intent.putExtra("stationCode", data.stationCode)
                intent.putExtra("stationName", data.stationName)
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }

    private fun okHttpSearch() {
        showDialog()
        Thread {
            val url = "http://decision-admin.tianqi.cn/Home/workwsj/hlj_site_search?key=${etSearch.text}"
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
                                searchList.clear()
                                val array = JSONArray(result)
                                for (i in 0 until array.length()) {
                                    val itemObj = array.getJSONObject(i)
                                    val dto = FactDto()
                                    if (!itemObj.isNull("stationCode")) {
                                        dto.stationCode = itemObj.getString("stationCode")
                                    }
                                    if (!itemObj.isNull("name")) {
                                        dto.stationName = itemObj.getString("name")
                                    }
                                    searchList.add(dto)
                                }
                                if (searchAdapter != null) {
                                    searchAdapter!!.notifyDataSetChanged()
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
            R.id.tvSearch -> okHttpSearch()
        }
    }

    override fun success(stationName: String?, stationCode: String?) {
        val intent = Intent()
        intent.putExtra("stationCode", stationCode)
        intent.putExtra("stationName", stationName)
        setResult(RESULT_OK, intent)
        finish()
    }

}
