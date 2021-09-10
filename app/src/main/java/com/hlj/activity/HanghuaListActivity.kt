package com.hlj.activity

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.Toast
import com.hlj.adapter.HanghuaListAdapter
import com.hlj.common.CONST
import com.hlj.common.MyApplication
import com.hlj.dto.StationMonitorDto
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.activity_hanghua_list.*
import kotlinx.android.synthetic.main.dialog_add_land.view.*
import kotlinx.android.synthetic.main.dialog_add_land.view.tvNegtive
import kotlinx.android.synthetic.main.dialog_add_land.view.tvPositive
import kotlinx.android.synthetic.main.dialog_delete.view.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException

/**
 * 航化作业列表
 */
class HanghuaListActivity : BaseActivity(), View.OnClickListener {

    private var mAdapter: HanghuaListAdapter? = null
    private val dataList: ArrayList<StationMonitorDto> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hanghua_list)
        initRefreshLayout()
        initWidget()
        initListView()
    }

    private fun initWidget() {
        llBack.setOnClickListener(this)
        tvTitle.setOnClickListener(this)
        ivControl.setImageResource(R.drawable.iv_add)
        ivControl.setOnClickListener(this)
        ivControl.visibility = View.VISIBLE

        val title: String = intent.getStringExtra(CONST.ACTIVITY_NAME)
        if (!TextUtils.isEmpty(title)) {
            tvTitle.text = title
        }

        refresh()
    }

    private fun refresh() {
        okHttpList()
    }

    /**
     * 初始化下拉刷新布局
     */
    private fun initRefreshLayout() {
        refreshLayout.setColorSchemeResources(CONST.color1, CONST.color2, CONST.color3, CONST.color4)
        refreshLayout.setProgressViewEndTarget(true, 400)
        refreshLayout.isRefreshing = true
        refreshLayout.setOnRefreshListener { refresh() }
    }

    private fun initListView() {
        mAdapter = HanghuaListAdapter(this, dataList)
        listView!!.adapter = mAdapter
        listView!!.onItemClickListener = AdapterView.OnItemClickListener { arg0, arg1, arg2, arg3 ->
            val dto = dataList[arg2]
            val intent = Intent(this, HanghuaDetailActivity::class.java)
            intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
            intent.putExtra(CONST.LAT, dto.lat)
            intent.putExtra(CONST.LNG, dto.lng)
            startActivity(intent)
        }
        listView.onItemLongClickListener = OnItemLongClickListener { parent, view, position, id ->
            val data = dataList[position]
            deleteDialog(data.stationId)
            true
        }
    }

    private fun okHttpList() {
        Thread(Runnable {
            val url = "http://decision-admin.tianqi.cn/Home/work2019/hlj_getmyhanghuoData?uid=${MyApplication.UID}"
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                }
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    runOnUiThread {
                        refreshLayout.isRefreshing = false
                        if (!TextUtils.isEmpty(result)) {
                            val obj = JSONObject(result)
                            if (!obj.isNull("data")) {
                                dataList.clear()
                                val array = obj.getJSONArray("data")
                                for (i in 0 until array.length()) {
                                    val dto = StationMonitorDto()
                                    val itemObj = array.getJSONObject(i)
                                    if (!itemObj.isNull("id")) {
                                        dto.stationId = itemObj.getString("id")
                                    }
                                    if (!itemObj.isNull("name")) {
                                        dto.name = itemObj.getString("name")
                                    }
                                    if (!itemObj.isNull("lat")) {
                                        val lat = itemObj.getString("lat")
                                        if (!TextUtils.isEmpty(lat)) {
                                            dto.lat = lat.toDouble()
                                        }
                                    }
                                    if (!itemObj.isNull("lon")) {
                                        val lng = itemObj.getString("lon")
                                        if (!TextUtils.isEmpty(lng)) {
                                            dto.lng = lng.toDouble()
                                        }
                                    }
                                    if (!itemObj.isNull("addtime")) {
                                        dto.time = itemObj.getString("addtime")
                                    }
                                    dataList.add(dto)
                                }
                                if (mAdapter != null) {
                                    mAdapter!!.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }
            })
        }).start()
    }

    private fun addDialog() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_add_land, null)
        val dialog = Dialog(this, R.style.CustomProgressDialog)
        dialog.setContentView(view)
        dialog.show()
        view.etLng.hint = "请输入经度(126.55或126°34'54\")"
        view.etLat.hint = "请输入纬度(40.55或40°34'54\")"
        view.tvNegtive.setOnClickListener { dialog.dismiss() }
        view.tvPositive.setOnClickListener {
            if (TextUtils.isEmpty(view.etName.text)) {
                Toast.makeText(this, "请输入地块名", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(view.etLng.text)) {
                Toast.makeText(this, "请输入经度(126.55或126°34'54\")", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(view.etLat.text)) {
                Toast.makeText(this, "请输入纬度(40.55或40°34'54\")", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            okHttpAdd(view.etName.text.toString(), view.etLat.text.toString(), view.etLng.text.toString())
        }
    }

    private fun okHttpAdd(name: String, lat: String, lng: String) {
        Thread(Runnable {
            val url = "http://decision-admin.tianqi.cn/Home/work2019/hlj_addmyhanghuoData?uid=${MyApplication.UID}&name=$name&lat=$lat&lon=$lng"
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                }
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    runOnUiThread {
                        if (!TextUtils.isEmpty(result)) {
                            val obj = JSONObject(result)
                            if (!obj.isNull("status")) {
                                val status = obj.getString("status")
                                if (TextUtils.equals(status, "1")) {
                                    refresh()
                                }
                            }
                            if (!obj.isNull("msg")) {
                                val msg = obj.getString("msg")
                                if (!TextUtils.isEmpty(msg)) {
                                    Toast.makeText(this@HanghuaListActivity, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            })
        }).start()
    }

    /**
     * 删除对话框
     */
    private fun deleteDialog(id: String) {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_delete, null)
        val dialog = Dialog(this, R.style.CustomProgressDialog)
        dialog.setContentView(view)
        dialog.show()
        view.tvMessage.text = "确认删除已选中地块信息？"
        view.llNegative.setOnClickListener { dialog.dismiss() }
        view.llPositive.setOnClickListener {
            dialog.dismiss()
            okHttpDelete(id)
        }
    }

    private fun okHttpDelete(id: String) {
        Thread(Runnable {
            val url = "http://decision-admin.tianqi.cn/Home/work2019/hlj_removemyhanghuoData?id=$id&uid=${MyApplication.UID}"
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                }
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    runOnUiThread {
                        if (!TextUtils.isEmpty(result)) {
                            val obj = JSONObject(result)
                            if (!obj.isNull("status")) {
                                val status = obj.getString("status")
                                if (TextUtils.equals(status, "1")) {
                                    refresh()
                                }
                            }
                            if (!obj.isNull("msg")) {
                                val msg = obj.getString("msg")
                                if (!TextUtils.isEmpty(msg)) {
                                    Toast.makeText(this@HanghuaListActivity, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            })
        }).start()
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.llBack -> {
                finish()
            }
            R.id.ivControl -> {
                addDialog()
            }
        }
    }

}
