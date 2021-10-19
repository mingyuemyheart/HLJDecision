package com.hlj.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import com.hlj.activity.DisasterDetailActivity
import com.hlj.activity.PDFActivity
import com.hlj.activity.WebviewActivity
import com.hlj.adapter.CommonPdfListAdapter
import com.hlj.common.CONST
import com.hlj.dto.AgriDto
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.fragment_common_list.*
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
 * 通用列表界面
 */
class CommonListFragment : Fragment() {

    private var mReceiver: MyBroadCastReceiver? = null
    private var mAdapter: CommonPdfListAdapter? = null
    private val dataList: ArrayList<AgriDto> = ArrayList()
    private var columnTitle: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_common_list, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBroadCast()
    }

    private fun initBroadCast() {
        mReceiver = MyBroadCastReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(arguments!!.getString(CONST.BROADCAST_ACTION))
        activity!!.registerReceiver(mReceiver, intentFilter)
    }

    private inner class MyBroadCastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refresh()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mReceiver != null) {
            activity!!.unregisterReceiver(mReceiver)
        }
    }

    private fun refresh() {
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
        columnTitle = arguments!!.getString(CONST.ACTIVITY_NAME)
    }

    private fun initListView() {
        okHttpList()

        mAdapter = CommonPdfListAdapter(activity, dataList)
        listView!!.adapter = mAdapter
        listView!!.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 ->
            val dto = dataList[arg2]
            val intent = when {
                TextUtils.equals(dto.type, CONST.PDF) -> {
                    Intent(activity, PDFActivity::class.java)
                }
                TextUtils.equals(dto.type, CONST.MP4) -> {
                    Intent(activity, WebviewActivity::class.java)
                }
                else -> {
                    if (TextUtils.isEmpty(dto.uid)) {
                        Intent(activity, WebviewActivity::class.java)
                    } else {
                        Intent(activity, DisasterDetailActivity::class.java)
                    }
                }
            }
            intent.putExtra(CONST.ACTIVITY_NAME, dto.title)
            intent.putExtra(CONST.WEB_URL, dto.dataUrl)
            val bundle = Bundle()
            bundle.putParcelable("data", dto)
            intent.putExtras(bundle)
            startActivity(intent)
        }
    }

    private fun okHttpList() {
        val url = arguments!!.getString(CONST.WEB_URL)
        if (TextUtils.isEmpty(url) || !url.startsWith("http")) {
            refreshLayout.isRefreshing = false
            return
        }
        Thread {
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    activity!!.runOnUiThread {
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
                                                    dto.title = columnTitle + title
                                                } else {
                                                    dto.title = columnTitle
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

                                            //灾情反馈
                                            if (!itemObj.isNull("uid")) {
                                                dto.uid = itemObj.getString("uid")
                                            }
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
	
}
