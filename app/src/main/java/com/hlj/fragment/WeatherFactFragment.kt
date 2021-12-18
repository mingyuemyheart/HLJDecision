package com.hlj.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import com.hlj.activity.*
import com.hlj.adapter.CommonFragmentAdapter
import com.hlj.common.CONST
import com.hlj.common.ColumnData
import com.hlj.common.MyApplication
import com.hlj.dto.AgriDto
import com.hlj.utils.CommonUtil
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.fragment_weather_fact.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException

/**
 * 天气预报、天气实况、电力气象服务、铁路气象服务
 */
class WeatherFactFragment : BaseFragment() {

    private var mReceiver: MyBroadCastReceiver? = null
    private var mAdapter: CommonFragmentAdapter? = null
    private val dataList: MutableList<AgriDto> = ArrayList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_weather_fact, null)
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
        initGridView()
        val columnId = arguments!!.getString(CONST.COLUMN_ID)
        val title = arguments!!.getString(CONST.ACTIVITY_NAME)
        CommonUtil.submitClickCount(columnId, title)
    }

    /**
     * 初始化listview
     */
    private fun initGridView() {
        dataList.clear()
        val data: ColumnData = arguments!!.getParcelable("data")

        val columnIds = MyApplication.getColumnIds(activity)
        val list: MutableList<ColumnData> = ArrayList()
        if (TextUtils.isEmpty(columnIds)) {
            list.addAll(data.child)
        } else {
            for (i in 0 until data.child.size) {
                val item1 = data.child[i]
                if (!columnIds.contains(item1.columnId+"--")) { //已经有保存的栏目
                    list.add(item1)
                }
            }
        }
        for (i in list.indices) {
            val item = list[i]
            val dto = AgriDto()
            dto.columnId = item.columnId
            dto.id = item.id
            dto.icon = item.icon
            dto.icon2 = item.icon2
            dto.showType = item.showType
            dto.name = item.name
            dto.dataUrl = item.dataUrl
            dto.child = item.child
            dataList.add(dto)
        }

        mAdapter = CommonFragmentAdapter(activity, dataList)
        gridView!!.adapter = mAdapter
        gridView!!.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 ->
            val dto = dataList[arg2]
            val intent: Intent
            if (TextUtils.equals(dto.showType, CONST.LOCAL)) { //气温预报、雾霾预报、降温大风沙尘预报
                if (TextUtils.equals(dto.id, "111")) { //天气雷达
                    intent = Intent(activity, WeatherRadarActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    val bundle = Bundle()
                    bundle.putParcelable("data", dto)
                    intent.putExtras(bundle)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "106")) { //天气图分析
                    intent = Intent(activity, WeatherChartAnalysisActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    val bundle = Bundle()
                    bundle.putParcelable("data", dto)
                    intent.putExtras(bundle)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "108")) { //空气质量
                    intent = Intent(activity, AirActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "112")) { //天气统计
                    intent = Intent(activity, WeatherStaticsActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "121")) { //负氧离子监测新
                    intent = Intent(activity, FylzActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "117")) { //台风路径
                    intent = Intent(activity, TyphoonRouteActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "120")) { //强对流天气实况（新）
                    intent = Intent(activity, StreamFactActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "201")) { //气温预报
                    intent = Intent(activity, TempForeActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    val bundle = Bundle()
                    bundle.putParcelable("data", dto)
                    intent.putExtras(bundle)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "203")) { //分钟级降水
                    intent = Intent(activity, MinuteFallActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "205")) { //等风来
                    intent = Intent(activity, WaitWindActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "207")) { //格点预报
                    intent = Intent(activity, PointForeActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "208")) { //分钟降水与强对流
                    intent = Intent(activity, StrongStreamActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "701")) { //全省预报
                    intent = Intent(activity, ProvinceForecastActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    intent.putExtra(CONST.WEB_URL, dto.dataUrl)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "1001")) {  //15天全省天气趋势、中期旬报、月气候趋势预测、电力气象预报、铁路气象服务子项目
                    intent = Intent(activity, CommonListActivity::class.java)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    intent.putExtra(CONST.WEB_URL, dto.dataUrl)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    val bundle = Bundle()
                    bundle.putParcelable("data", dto)
                    intent.putExtras(bundle)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "1002")) { //铁路气象服务（6小时降水量）
                    intent = Intent(activity, SixHourRainActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    val bundle = Bundle()
                    bundle.putParcelable("data", dto)
                    intent.putExtras(bundle)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "115") || TextUtils.equals(dto.id, "113") || TextUtils.equals(dto.id, "114") || TextUtils.equals(dto.id, "116")) { //115降水实况，113气温实况，114风向风速实况，116相对湿度分析
                    intent = Intent(activity, FactActivity2::class.java)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    intent.putExtra(CONST.WEB_URL, dto.dataUrl)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    val bundle = Bundle()
                    bundle.putParcelable("data", dto)
                    intent.putExtras(bundle)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "118")) { //天气会商
                    intent = Intent(activity, WeatherMeetingActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "119")) { //自动站实况监测
                    intent = Intent(activity, FactMonitorActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    intent.putExtra(CONST.WEB_URL, dto.dataUrl)
                    val bundle = Bundle()
                    bundle.putParcelable("data", dto)
                    intent.putExtras(bundle)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "209")) { //雷电预报
                    intent = Intent(activity, ThunderForeActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    intent.putExtra(CONST.WEB_URL, dto.dataUrl)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "210")) { //雷电统计
                    intent = Intent(activity, ThunderStatisticActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    intent.putExtra(CONST.WEB_URL, dto.dataUrl)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "211")) { //航化作业
                    intent = Intent(activity, HanghuaActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    intent.putExtra(CONST.WEB_URL, dto.dataUrl)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "131")) {//森林火险等级预报
                    okHttpDetail(dto.dataUrl)
                } else if (TextUtils.equals(dto.id, "132")) {//林场站气象要素
                    intent = Intent(activity, PDFSingleActivity::class.java)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    val bundle = Bundle()
                    bundle.putParcelable("data", dto)
                    intent.putExtras(bundle)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "1205")) { //农业气象服务
                    intent = Intent(activity, WebviewActivity::class.java)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    intent.putExtra(CONST.WEB_URL, dto.dataUrl)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "1206")) { //提问解答
                    intent = Intent(activity, AskAnwserActivity::class.java)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "122")) { //灾情反馈
                    intent = Intent(activity, DisasterActivity::class.java)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "123")) { //通讯录
                    intent = Intent(activity, AddrBookActivity::class.java)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "125")) { //实况站点查询
                    intent = Intent(activity, FactQueryActivity::class.java)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "1401")) { //气候背景
                    intent = Intent(activity, WebviewCssActivity::class.java)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    intent.putExtra(CONST.WEB_URL, dto.dataUrl)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    startActivity(intent)
                } else if (TextUtils.equals(dto.id, "13")) {
                    intent = Intent(activity, ProductActivity::class.java)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    intent.putExtra(CONST.WEB_URL, dto.dataUrl)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    val bundle = Bundle()
                    bundle.putParcelable("data", dto)
                    intent.putExtras(bundle)
                    startActivity(intent)
                } else {
                    intent = Intent(activity, CommonListActivity::class.java)
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                    intent.putExtra(CONST.WEB_URL, dto.dataUrl)
                    intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                    val bundle = Bundle()
                    bundle.putParcelable("data", dto)
                    intent.putExtras(bundle)
                    startActivity(intent)
                }
            } else if (TextUtils.equals(dto.showType, CONST.URL)) { //三天降水量预报
                intent = Intent(activity, WebviewActivity::class.java)
                intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                intent.putExtra(CONST.WEB_URL, dto.dataUrl)
                startActivity(intent)
            } else if (TextUtils.equals(dto.showType, CONST.PRODUCT)) {
                intent = Intent(activity, ProductActivity::class.java)
                intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                intent.putExtra(CONST.WEB_URL, dto.dataUrl)
                intent.putExtra(CONST.COLUMN_ID, dto.columnId)
                val bundle = Bundle()
                bundle.putParcelable("data", dto)
                intent.putExtras(bundle)
                startActivity(intent)
            } else if (TextUtils.equals(dto.showType, CONST.NEWS)) {
                intent = Intent(activity, CommonListActivity::class.java)
                intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                intent.putExtra(CONST.WEB_URL, dto.dataUrl)
                val bundle = Bundle()
                bundle.putParcelable("data", dto)
                intent.putExtras(bundle)
                startActivity(intent)
            } else {
                intent = Intent(activity, CommonListActivity::class.java)
                intent.putExtra(CONST.ACTIVITY_NAME, dto.name)
                intent.putExtra(CONST.WEB_URL, dto.dataUrl)
                val bundle = Bundle()
                bundle.putParcelable("data", dto)
                intent.putExtras(bundle)
                startActivity(intent)
            }
        }
    }

    /**
     * 获取详情
     */
    private fun okHttpDetail(url: String) {
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
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                val obj = JSONObject(result)
                                if (!obj.isNull("info")) {
                                    val array = obj.getJSONArray("info")
                                    if (array.length() > 0) {
                                        val dataUrl = array.getString(0)
                                        if (!TextUtils.isEmpty(dataUrl)) {
                                            val intent = Intent(activity, WebviewActivity::class.java)
                                            intent.putExtra(CONST.WEB_URL, dataUrl)
                                            startActivity(intent)
                                        }
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
	
}
