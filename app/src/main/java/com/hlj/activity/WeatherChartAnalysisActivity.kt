package com.hlj.activity

import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.view.View
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
import com.hlj.common.CONST
import com.hlj.dto.AgriDto
import com.hlj.utils.CommonUtil
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.activity_weather_chart_analysis.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * 天气图分析
 */
class WeatherChartAnalysisActivity : BaseActivity(), View.OnClickListener {

    private var aMap: AMap? = null
    private val zoom = 3.7f
    private val polyline1: MutableList<Polyline> = ArrayList()
    private val textList1: MutableList<Text> = ArrayList()
    private val polyline2: MutableList<Polyline> = ArrayList()
    private val textList2: MutableList<Text> = ArrayList()
    private var swithWidth = 0
    private val typeh000 = "h000"
    private val typeh850 = "h850"
    private val typeh500 = "h500"
    private var type = typeh000//h000、h850、h500
    private var result1: String? = null
    private var result2: String? = null
    private var result3: String? = null
    private val sdf1 = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_chart_analysis)
        initMap(savedInstanceState)
        initWidget()
    }

    /**
     * 初始化地图
     */
    private fun initMap(bundle: Bundle?) {
        mapView!!.onCreate(bundle)
        if (aMap == null) {
            aMap = mapView!!.map
        }
        aMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(35.926628, 105.178100), zoom))
        aMap!!.uiSettings.isZoomControlsEnabled = false
        aMap!!.uiSettings.isRotateGesturesEnabled = false
        aMap!!.setOnMapLoadedListener {
            tvMapNumber.text = aMap!!.mapContentApprovalNumber
        }
        refresh()
    }

    private fun refresh() {
        okHttpList()
    }

    private fun initWidget() {
        llBack!!.setOnClickListener(this)
        ivChart!!.setOnClickListener(this)
        ivSwitch!!.setOnClickListener(this)
        tv1!!.setOnClickListener(this)
        tv2.setOnClickListener(this)
        tv3.setOnClickListener(this)
        val w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        llSwitch!!.measure(w, h)
        swithWidth = llSwitch!!.measuredWidth

        val data: AgriDto = intent.extras.getParcelable("data")
        if (data.name != null) {
            tvTitle!!.text = data.name
        }
    }

    private fun okHttpList() {
        showDialog()
        Thread {
            val url = "https://scapi.tianqi.cn/weather/xstu?test=ncg&type=1&hm=$type"
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    when(type) {
                        typeh000 -> result1 = result
                        typeh850 -> result2 = result
                        typeh500 -> result3 = result
                    }
                    parseData(result)
                }
            })
        }.start()
    }

    private fun parseData(res: String?) {
        aMap!!.clear()
        if (!TextUtils.isEmpty(res)) {
            try {
                val resObj = JSONObject(res)
                if (!resObj.isNull("list")) {
                    val array = resObj.getJSONArray("list")
                    val dataUrl = array.getString(0)
                    OkHttpUtil.enqueue(Request.Builder().url(dataUrl).build(), object : Callback {
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
                                        if (!obj.isNull("mtime")) {
                                            val mTime = obj.getLong("mtime")
                                            tvTime!!.text = sdf1.format(Date(mTime)) + "更新"
                                            tvTime!!.visibility = View.VISIBLE
                                        }
                                        if (!obj.isNull("lines")) {
                                            val lines = obj.getJSONArray("lines")
                                            for (i in 0 until lines.length()) {
                                                val itemObj = lines.getJSONObject(i)
                                                if (!itemObj.isNull("point")) {
                                                    val points = itemObj.getJSONArray("point")
                                                    val polylineOption = PolylineOptions()
                                                    polylineOption.width(6f).color(-0xbf9441)
                                                    for (j in 0 until points.length()) {
                                                        val point = points.getJSONObject(j)
                                                        val lat = point.getDouble("y")
                                                        val lng = point.getDouble("x")
                                                        polylineOption.add(LatLng(lat, lng))
                                                    }
                                                    val p = aMap!!.addPolyline(polylineOption)
                                                    polyline1.add(p)
                                                }
                                                if (!itemObj.isNull("flags")) {
                                                    val flags = itemObj.getJSONObject("flags")
                                                    var text: String? = ""
                                                    if (!flags.isNull("text")) {
                                                        text = flags.getString("text")
                                                    }
                                                    if (!flags.isNull("items")) {
                                                        val items = flags.getJSONArray("items")
                                                        val item = items.getJSONObject(0)
                                                        val lat = item.getDouble("y")
                                                        val lng = item.getDouble("x")
                                                        val to = TextOptions()
                                                        to.position(LatLng(lat, lng))
                                                        to.text(text)
                                                        to.fontColor(Color.BLACK)
                                                        to.fontSize(30)
                                                        to.backgroundColor(Color.TRANSPARENT)
                                                        val t = aMap!!.addText(to)
                                                        textList1.add(t)
                                                    }
                                                }
                                            }
                                        }
                                        if (!obj.isNull("line_symbols")) {
                                            val line_symbols = obj.getJSONArray("line_symbols")
                                            for (i in 0 until line_symbols.length()) {
                                                val itemObj = line_symbols.getJSONObject(i)
                                                if (!itemObj.isNull("items")) {
                                                    val items = itemObj.getJSONArray("items")
                                                    val polylineOption = PolylineOptions()
                                                    polylineOption.width(6f).color(-0xbf9441)
                                                    for (j in 0 until items.length()) {
                                                        val item = items.getJSONObject(j)
                                                        val lat = item.getDouble("y")
                                                        val lng = item.getDouble("x")
                                                        polylineOption.add(LatLng(lat, lng))
                                                    }
                                                    val p = aMap!!.addPolyline(polylineOption)
                                                    polyline2.add(p)
                                                }
                                            }
                                        }
                                        if (!obj.isNull("symbols")) {
                                            val symbols = obj.getJSONArray("symbols")
                                            for (i in 0 until symbols.length()) {
                                                val itemObj = symbols.getJSONObject(i)
                                                var text = ""
                                                var color = Color.BLACK
                                                if (!itemObj.isNull("type")) {
                                                    val type = itemObj.getString("type")
                                                    if (TextUtils.equals(type, "60")) {
                                                        text = "H"
                                                        color = Color.RED
                                                    } else if (TextUtils.equals(type, "61")) {
                                                        text = "L"
                                                        color = Color.BLUE
                                                    } else if (TextUtils.equals(type, "37")) {
                                                        text = "台"
                                                        color = Color.GREEN
                                                    }
                                                }
                                                val lat = itemObj.getDouble("y")
                                                val lng = itemObj.getDouble("x")
                                                val to = TextOptions()
                                                to.position(LatLng(lat, lng))
                                                to.text(text)
                                                to.fontColor(color)
                                                to.fontSize(60)
                                                to.backgroundColor(Color.TRANSPARENT)
                                                val t = aMap!!.addText(to)
                                                textList2.add(t)
                                            }
                                        }
                                    } catch (e: JSONException) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    })
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.llBack -> finish()
            R.id.ivSwitch -> {
                if (llSwitch.visibility == View.VISIBLE) {
                    llSwitch.visibility = View.INVISIBLE
                } else {
                    llSwitch.visibility = View.VISIBLE
                }
            }
            R.id.tv1 -> {
                type = typeh000
                tv1!!.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
                tv2.setTextColor(ContextCompat.getColor(this, R.color.text_color3))
                tv3.setTextColor(ContextCompat.getColor(this, R.color.text_color3))
                if (!TextUtils.isEmpty(result1)) {
                    parseData(result1)
                } else {
                    refresh()
                }
            }
            R.id.tv2 -> {
                type = typeh850
                tv1!!.setTextColor(ContextCompat.getColor(this, R.color.text_color3))
                tv2.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
                tv3.setTextColor(ContextCompat.getColor(this, R.color.text_color3))
                if (!TextUtils.isEmpty(result2)) {
                    parseData(result2)
                } else {
                    refresh()
                }
            }
            R.id.tv3 -> {
                type = typeh500
                tv1!!.setTextColor(ContextCompat.getColor(this, R.color.text_color3))
                tv2.setTextColor(ContextCompat.getColor(this, R.color.text_color3))
                tv3.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
                if (!TextUtils.isEmpty(result3)) {
                    parseData(result3)
                } else {
                    refresh()
                }
            }
            R.id.ivChart -> {
                if (ivLegend.visibility == View.VISIBLE) {
                    ivLegend.visibility = View.INVISIBLE
                } else {
                    ivLegend.visibility = View.VISIBLE
                }
            }
        }
    }

}
