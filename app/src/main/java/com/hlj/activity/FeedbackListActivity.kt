package com.hlj.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import com.hlj.adapter.FeedbackListAdapter
import com.hlj.common.CONST
import com.hlj.common.MyApplication
import com.hlj.dto.AgriDto
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.activity_feedback_list.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException

/**
 * 意见反馈列表
 */
class FeedbackListActivity : BaseActivity(), View.OnClickListener {

    private var mAdapter: FeedbackListAdapter? = null
    private val dataList: ArrayList<AgriDto> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback_list)
        initRefreshLayout()
        initWidget()
        initListView()
    }

    private fun initWidget() {
        llBack.setOnClickListener(this)
        tvTitle.setOnClickListener(this)
        tvControl!!.text = "意见反馈"
        tvControl!!.visibility = View.VISIBLE
        tvControl!!.setOnClickListener(this)

        val title: String = intent.getStringExtra(CONST.ACTIVITY_NAME)
        if (!TextUtils.isEmpty(title)) {
            tvTitle.text = title
        }

        okHttpList()
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

    private fun initListView() {
        mAdapter = FeedbackListAdapter(this, dataList)
        listView!!.adapter = mAdapter
        listView!!.onItemClickListener = OnItemClickListener { arg0, arg1, position, arg3 ->
            val data = dataList[position]
            val intent = Intent(this, FeedbackDetailActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable("data", data)
            intent.putExtras(bundle)
            startActivity(intent)
        }
    }

    private fun okHttpList() {
        refreshLayout!!.isRefreshing = true
        Thread {
            val url = "http://decision-admin.tianqi.cn/Home/workwsj/getFeedbacklist"
            val builder = FormBody.Builder()
            builder.add("uid", MyApplication.UID)
            builder.add("appid", CONST.APPID)
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
                                        if (!itemObj.isNull("add_time")) {
                                            dto.time = itemObj.getString("add_time")
                                        }
                                        if (!itemObj.isNull("content")) {
                                            dto.title = itemObj.getString("content")
                                        }
                                        if (!itemObj.isNull("reply_content")) {
                                            dto.reply_content = itemObj.getString("reply_content")
                                        }
                                        dataList.add(dto)
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
            R.id.llBack -> {
                finish()
            }
            R.id.tvControl -> {
                val intent = Intent(this, FeedbackActivity::class.java)
                intent.putExtra(CONST.ACTIVITY_NAME, tvTitle.text.toString())
                startActivityForResult(intent, 1001)
            }
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
