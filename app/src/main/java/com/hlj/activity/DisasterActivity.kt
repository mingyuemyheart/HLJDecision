package com.hlj.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.View.OnClickListener
import android.widget.AdapterView.OnItemClickListener
import com.hlj.adapter.DisasterAdapter
import com.hlj.common.CONST
import com.hlj.common.MyApplication
import com.hlj.dto.AgriDto
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.activity_disaster.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException
import java.util.*

/**
 * 灾情反馈
 */
class DisasterActivity : BaseActivity(), OnClickListener {

    private var mAdapter: DisasterAdapter? = null
    private val dataList: MutableList<AgriDto> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disaster)
        initRefreshLayout()
        initWidget()
        initListView()
    }

    /**
     * 初始化下拉刷新布局
     */
    private fun initRefreshLayout() {
        refreshLayout.setColorSchemeResources(CONST.color1, CONST.color2, CONST.color3, CONST.color4)
        refreshLayout.setProgressViewEndTarget(true, 400)
        refreshLayout.isRefreshing = true
        refreshLayout.setOnRefreshListener { okHttpList() }
    }

    private fun initWidget() {
        llBack.setOnClickListener(this)
        tvControl.text = "反馈"
        tvControl.visibility = View.VISIBLE
        tvControl.setOnClickListener(this)

        val title = intent.getStringExtra(CONST.ACTIVITY_NAME)
        if (!TextUtils.isEmpty(title)) {
            tvTitle.text = title
        }
        okHttpList()
    }

    private fun initListView() {
        mAdapter = DisasterAdapter(this, dataList)
        listView.adapter = mAdapter
        listView.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            val data = dataList[position]
            val intent = Intent(this@DisasterActivity, DisasterDetailActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable("data", data)
            intent.putExtras(bundle)
            startActivity(intent)
        }
    }

    private fun okHttpList() {
        refreshLayout.isRefreshing = true
        Thread {
            val url = "http://decision-admin.tianqi.cn/home/work2019/hlj_zqfkSelect"
            val builder = FormBody.Builder()
            builder.add("appid", CONST.APPID)
            builder.add("uid", MyApplication.UID)
            val body: RequestBody = builder.build()
            OkHttpUtil.enqueue(Request.Builder().post(body).url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    runOnUiThread {
                        refreshLayout!!.isRefreshing = false
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                val obj = JSONObject(result)
                                if (!obj.isNull("data")) {
                                    dataList.clear()
                                    val array = obj.getJSONArray("data")
                                    for (i in 0 until array.length()) {
                                        val itemObj = array.getJSONObject(i)
                                        val dto = AgriDto()
                                        if (!itemObj.isNull("title")) {
                                            dto.title = itemObj.getString("title")
                                        }
                                        if (!itemObj.isNull("content")) {
                                            dto.content = itemObj.getString("content")
                                        }
                                        if (!itemObj.isNull("type")) {
                                            dto.disasterType = itemObj.getString("type")
                                        }
                                        if (!itemObj.isNull("location")) {
                                            dto.addr = itemObj.getString("location")
                                        }
                                        if (!itemObj.isNull("addtime")) {
                                            dto.time = itemObj.getString("addtime")
                                        }
                                        if (!itemObj.isNull("createtime")) {
                                            dto.createtime = itemObj.getString("createtime")
                                        }
                                        if (!itemObj.isNull("status_cn")) {
                                            dto.status_cn = itemObj.getString("status_cn")
                                        }
                                        if (!itemObj.isNull("pic")) {
                                            val imgArray = itemObj.getJSONArray("pic")
                                            for (j in 0 until imgArray.length()) {
                                                dto.imgList.add(imgArray.getString(j))
                                            }
                                        }
                                        dataList.add(dto)
                                    }
                                    if (dataList.size <= 0) {
                                        tvPrompt.visibility = View.VISIBLE
                                    } else {
                                        tvPrompt.visibility = View.GONE
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
            R.id.tvControl -> startActivityForResult(Intent(this, DisasterUploadActivity::class.java), 1001)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when(requestCode) {
                1001 -> okHttpList()
            }
        }
    }

}
