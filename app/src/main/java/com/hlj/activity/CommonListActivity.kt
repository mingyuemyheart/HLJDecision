package com.hlj.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import com.hlj.adapter.CommonPdfListAdapter
import com.hlj.common.CONST
import com.hlj.dto.AgriDto
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.fragment_common_list.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList

/**
 * 通用列表界面，包括pdf列表和数据列表
 */
class CommonListActivity : BaseActivity(), View.OnClickListener {

    private var mAdapter: CommonPdfListAdapter? = null
    private val dataList: ArrayList<AgriDto> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_common_list)
        initRefreshLayout()
        initWidget()
        initListView()
    }

    private fun initWidget() {
        llBack.setOnClickListener(this)
        tvTitle.setOnClickListener(this)

        val title: String = intent.getStringExtra(CONST.ACTIVITY_NAME)
        if (!TextUtils.isEmpty(title)) {
            tvTitle.text = title
        }

        refresh()
    }

    private fun refresh() {
        if (intent.hasExtra("data")) {
            val dto: AgriDto = intent.extras.getParcelable("data")

            tvTitle.text = dto.name

            if (TextUtils.isEmpty(dto.showType) || TextUtils.isEmpty(dto.dataUrl)) {
                if (!TextUtils.isEmpty(dto.child[0].dataUrl)) {
                    okHttpList(dto.child[0].dataUrl)
                }
            } else {
                if (!TextUtils.isEmpty(dto.dataUrl)) {
                    okHttpList(dto.dataUrl)
                }
            }
        } else if (intent.hasExtra(CONST.WEB_URL)) {
            okHttpList(intent.getStringExtra(CONST.WEB_URL))
        }
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
        mAdapter = CommonPdfListAdapter(this, dataList)
        listView!!.adapter = mAdapter
        listView!!.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 ->
            val dto = dataList[arg2]
            val intent = when {
                TextUtils.equals(dto.type, CONST.PDF) -> {
                    Intent(this, PDFActivity::class.java)
                }
                TextUtils.equals(dto.type, "tw") || TextUtils.equals(dto.type, "jcbw") -> {
                    Intent(this, WebviewCssActivity::class.java)
                }
                else -> {
                    Intent(this, WebviewActivity::class.java)
                }
            }
            intent.putExtra(CONST.ACTIVITY_NAME, dto.title)
            intent.putExtra(CONST.WEB_URL, dto.dataUrl)
            startActivity(intent)
        }
    }

    private fun okHttpList(url: String?) {
        Thread {
            OkHttpUtil.enqueue(Request.Builder().url(url!!).build(), object : Callback {
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
                                if (!obj.isNull("info")) {
                                    dataList.clear()
                                    val array = obj.getJSONArray("info")
                                    for (i in 0 until array.length()) {
                                        val dto = AgriDto()
                                        val itemResult = array.getString(i)
                                        if (itemResult.contains("\"url\":")) {
                                            val itemObj = array.getJSONObject(i)
                                            if (!itemObj.isNull("url")) {
                                                dto.dataUrl = itemObj.getString("url")
                                                if (!itemObj.isNull("time")) {
                                                    dto.time = itemObj.getString("time")
                                                }
                                                if (!itemObj.isNull("image")) {
                                                    dto.icon = itemObj.getString("image")
                                                }
                                                if (!itemObj.isNull("title")) {
                                                    dto.title = itemObj.getString("title")
                                                }
                                                if (dto.dataUrl.endsWith(".pdf") || dto.dataUrl.endsWith(".PDF")) {
                                                    dto.type = CONST.PDF
                                                }
                                                dataList.add(dto)
                                            }
                                        } else {
                                            val title = "-因无业务需求,暂停发布本产品"
                                            dto.dataUrl = itemResult
                                            if (!TextUtils.isEmpty(dto.dataUrl) && !TextUtils.equals(dto.dataUrl, "null")) {
                                                if (dto.dataUrl.contains(title)) {
                                                    dto.title = tvTitle.text.toString() + title
                                                } else {
                                                    dto.title = tvTitle.text.toString()
                                                }
                                                dto.time = time(dto.dataUrl)
                                                if (dto.dataUrl.endsWith(".pdf") || dto.dataUrl.endsWith(".PDF")) {
                                                    dto.type = CONST.PDF
                                                }
                                                dataList.add(dto)
                                            }
                                        }
                                    }
                                } else {
                                    var type: String? = null
                                    if (!obj.isNull("type")) {
                                        type = obj.getString("type")
                                    }
                                    if (!obj.isNull("l")) {
                                        dataList.clear()
                                        val array = obj.getJSONArray("l")
                                        for (i in 0 until array.length()) {
                                            val itemObj = array.getJSONObject(i)
                                            val dto = AgriDto()
                                            if (!itemObj.isNull("l1")) {
                                                dto.title = itemObj.getString("l1")
                                            }
                                            if (!itemObj.isNull("l2")) {
                                                dto.dataUrl = itemObj.getString("l2")
                                            }
                                            if (!itemObj.isNull("l3")) {
                                                dto.time = itemObj.getString("l3")
                                            }
                                            if (!itemObj.isNull("l4")) {
                                                dto.icon = itemObj.getString("l4")
                                            }
                                            dto.type = type
                                            dataList.add(dto)
                                        }
                                    }
                                }
                                if (mAdapter != null) {
                                    mAdapter!!.notifyDataSetChanged()
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

    private val sdf11 = SimpleDateFormat("yyyyMMdd", Locale.CHINA)
    private val sdf12 = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
    private val sdf21 = SimpleDateFormat("yyyyMMddHH", Locale.CHINA)
    private val sdf22 = SimpleDateFormat("yyyy年MM月dd日HH时", Locale.CHINA)
    private val sdf31 = SimpleDateFormat("yyyyMMddHHmm", Locale.CHINA)
    private val sdf32 = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINA)
    private val sdf41 = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA)
    private val sdf42 = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.CHINA)

    private fun isMatches(pattern: String, content: String): Boolean {
        return Pattern.matches(pattern, content)
    }

    private fun time(dataUrl: String): String? {
        var time: String? = ""
        try {
            when {
                isMatches("^[0-9]\\d{13}", dataUrl.substring(dataUrl.length - 18, dataUrl.length - 4)) -> {
                    time = sdf42.format(sdf41.parse(dataUrl.substring(dataUrl.length - 18, dataUrl.length - 4)))
                }
                isMatches("^[0-9]\\d{11}", dataUrl.substring(dataUrl.length - 16, dataUrl.length - 4)) -> {
                    time = sdf32.format(sdf31.parse(dataUrl.substring(dataUrl.length - 16, dataUrl.length - 4)))
                }
                isMatches("^[0-9]\\d{9}", dataUrl.substring(dataUrl.length - 14, dataUrl.length - 4)) -> {
                    time = sdf22.format(sdf21.parse(dataUrl.substring(dataUrl.length - 14, dataUrl.length - 4)))
                }
                isMatches("^[0-9]\\d{7}", dataUrl.substring(dataUrl.length - 12, dataUrl.length - 4)) -> {
                    time = sdf12.format(sdf11.parse(dataUrl.substring(dataUrl.length - 12, dataUrl.length - 4)))
                }
                isMatches("^[0-9]\\d{7}-[0-9]", dataUrl.substring(dataUrl.length - 14, dataUrl.length - 4)) -> {
                    time = sdf12.format(sdf11.parse(dataUrl.substring(dataUrl.length - 14, dataUrl.length - 6)))
                }
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return time
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.llBack -> {
                finish()
            }
        }
    }

}
