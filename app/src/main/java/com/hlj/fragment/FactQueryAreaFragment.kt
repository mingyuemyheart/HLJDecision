package com.hlj.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.Toast
import com.hlj.activity.FactQueryAreaActivity
import com.hlj.adapter.FactQueryHourAdapter
import com.hlj.adapter.FactQueryMinuteAdapter
import com.hlj.common.CONST
import com.hlj.dto.FactDto
import com.hlj.utils.OkHttpUtil
import com.hlj.view.wheelview.NumericWheelAdapter
import com.hlj.view.wheelview.OnWheelScrollListener
import com.hlj.view.wheelview.WheelView
import kotlinx.android.synthetic.main.fragment_fact_query_area.*
import kotlinx.android.synthetic.main.layout_date.*
import net.sourceforge.pinyin4j.PinyinHelper
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import shawn.cxwl.com.hlj.R
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * 实况站点查询-区域查询
 */
class FactQueryAreaFragment : BaseFragment(), OnClickListener {

    private var isMinute = true
    private var city = ""
    private var area = ""
    private var startTime = ""
    private var minute1 = false
    private var minute2 = false
    private var minute3 = false //false为将序，true为升序
    private var hour1 = false
    private var hour2 = false
    private var hour3 = false
    private var hour4 = false
    private var hour5 = false
    private var hour6 = false
    private var hour7 = false
    private var hour8 = false
    private var hour9 = false
    private var minuteAdapter: FactQueryMinuteAdapter? = null
    private var hourAdapter: FactQueryHourAdapter? = null
    private val dataList: MutableList<FactDto> = ArrayList()
    private val sdf1 = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    private val sdf2 = SimpleDateFormat("yyyyMMddHHmm00", Locale.CHINA)
    private val sdf3 = SimpleDateFormat("yyyyMMddHH0000", Locale.CHINA)
    private val sdf4 = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_fact_query_area, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initWidget()
        initWheelView()
        initListViewMinute()
        initListViewHour()
    }

    private fun initWidget() {
        tvMinute.setOnClickListener(this)
        tvHour.setOnClickListener(this)
        tvStartTime.setOnClickListener(this)
        tvSearch.setOnClickListener(this)
        tvCheck.setOnClickListener(this)
        tvStationNameMinute.setOnClickListener(this)
        tvTimeMinute.setOnClickListener(this)
        tvRainMinute.setOnClickListener(this)
        tvArea.setOnClickListener(this)
        tvStationName.setOnClickListener(this)
        tvTime.setOnClickListener(this)
        tvRain.setOnClickListener(this)
        tvTemp.setOnClickListener(this)
        tvHumidity.setOnClickListener(this)
        tvWinds.setOnClickListener(this)
        tvWindd.setOnClickListener(this)
        tvVis.setOnClickListener(this)
    }

    private fun initListViewMinute() {
        minuteAdapter = FactQueryMinuteAdapter(activity, dataList)
        listViewMinute!!.adapter = minuteAdapter
    }

    private fun initListViewHour() {
        hourAdapter = FactQueryHourAdapter(activity, dataList)
        listViewHour!!.adapter = hourAdapter
    }

    private fun okHttpList() {
        if (TextUtils.isEmpty(city) || TextUtils.isEmpty(area)) {
            Toast.makeText(activity, "请选择区域", Toast.LENGTH_SHORT).show()
            return
        }
        if (TextUtils.isEmpty(startTime)) {
            Toast.makeText(activity, "请选择时间", Toast.LENGTH_SHORT).show()
            return
        }
        showDialog()
        Thread {
            val url = if (isMinute) {
                "http://decision-admin.tianqi.cn/Home/workwsj/hlj_area_10minrain?time=${sdf2.format(sdf4.parse(startTime))}&city=$city&area=$area"
            } else {
                "http://decision-admin.tianqi.cn/Home/workwsj/hlj_area_hourrain?time=${sdf3.format(sdf4.parse(startTime))}&city=$city&area=$area"
            }
            Log.e("FactQueryFragment", url)
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    activity!!.runOnUiThread {
                        cancelDialog()
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                dataList.clear()
                                val array = JSONArray(result)
                                for (i in 0 until array.length()) {
                                    val itemObj = array.getJSONObject(i)
                                    val dto = FactDto()
                                    if (!itemObj.isNull("stationCode")) {
                                        dto.stationCode = itemObj.getString("stationCode")
                                    }
                                    if (!itemObj.isNull("stationName")) {
                                        dto.stationName = itemObj.getString("stationName")
                                    }
                                    if (!itemObj.isNull("area")) {
                                        dto.area = itemObj.getString("area")
                                    }
                                    if (!itemObj.isNull("time")) {
                                        dto.time = itemObj.getString("time")
                                    }
                                    if (!itemObj.isNull("rain")) {
                                        dto.rain = itemObj.getString("rain")
                                        if (TextUtils.equals(dto.rain, "999999")) {
                                            dto.rain = CONST.NO_VALUE
                                        }
                                    }
                                    if (!itemObj.isNull("tem")) {
                                        dto.temp = itemObj.getString("tem")
                                        if (TextUtils.equals(dto.temp, "999999")) {
                                            dto.temp = CONST.NO_VALUE
                                        }
                                    }
                                    if (!itemObj.isNull("winds")) {
                                        dto.windSpeed = itemObj.getString("winds")
                                        if (TextUtils.equals(dto.windSpeed, "999999")) {
                                            dto.windSpeed = CONST.NO_VALUE
                                        }
                                    }
                                    if (!itemObj.isNull("windd")) {
                                        dto.windDir = itemObj.getString("windd")
                                        if (TextUtils.equals(dto.windDir, "999999")) {
                                            dto.windDir = CONST.NO_VALUE
                                        }
                                    }
                                    if (!itemObj.isNull("xdsd")) {
                                        dto.humidity = itemObj.getString("xdsd")
                                        if (TextUtils.equals(dto.humidity, "999999")) {
                                            dto.humidity = CONST.NO_VALUE
                                        }
                                    }
                                    if (!itemObj.isNull("vis")) {
                                        dto.visibility = itemObj.getString("vis")
                                        if (TextUtils.equals(dto.visibility, "999999")) {
                                            dto.visibility = CONST.NO_VALUE
                                        }
                                    }
                                    dataList.add(dto)
                                }
                                if (dataList.size <= 0) {
                                    tvPrompt.visibility = View.VISIBLE
                                } else {
                                    tvPrompt.visibility = View.GONE
                                }

                                if (isMinute) {
                                    if (minuteAdapter != null) {
                                        minuteAdapter!!.notifyDataSetChanged()
                                    }
                                } else {
                                    if (hourAdapter != null) {
                                        hourAdapter!!.notifyDataSetChanged()
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

    // 返回中文的首字母
    fun getPinYinHeadChar(str: String): String {
        var convert = ""
        var size = str.length
        if (size >= 2) {
            size = 2
        }
        for (j in 0 until size) {
            val word = str[j]
            val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(word)
            convert += if (pinyinArray != null) {
                pinyinArray[0][0]
            } else {
                word
            }
        }
        return convert
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.tvMinute -> {
                isMinute = true
                tvMinute.setTextColor(ContextCompat.getColor(activity!!, R.color.white))
                tvHour.setTextColor(ContextCompat.getColor(activity!!, R.color.text_color4))
                tvMinute.setBackgroundResource(R.drawable.corner_left_blue)
                tvHour.setBackgroundResource(R.drawable.corner_right_white)
                minute.visibility = View.VISIBLE
                clMinute.visibility = View.VISIBLE
                listViewMinute.visibility = View.VISIBLE
                hScrollView.visibility = View.GONE
                if (!TextUtils.isEmpty(city) && !TextUtils.isEmpty(area)) {
                    okHttpList()
                }
            }
            R.id.tvHour -> {
                isMinute = false
                tvMinute.setTextColor(ContextCompat.getColor(activity!!, R.color.text_color4))
                tvHour.setTextColor(ContextCompat.getColor(activity!!, R.color.white))
                tvMinute.setBackgroundResource(R.drawable.corner_left_white)
                tvHour.setBackgroundResource(R.drawable.corner_right_blue)
                minute.visibility = View.GONE
                clMinute.visibility = View.GONE
                listViewMinute.visibility = View.GONE
                hScrollView.visibility = View.VISIBLE
                if (!TextUtils.isEmpty(city) && !TextUtils.isEmpty(area)) {
                    okHttpList()
                }
            }
            R.id.tvStartTime, R.id.tvNegtive -> bootTimeLayoutAnimation(layoutDate)
            R.id.tvPositive -> {
                setTextViewValue()
                bootTimeLayoutAnimation(layoutDate)
                if (!TextUtils.isEmpty(city) && !TextUtils.isEmpty(area)) {
                    okHttpList()
                }
            }
            R.id.tvSearch -> {
                startActivityForResult(Intent(activity, FactQueryAreaActivity::class.java), 1001)
            }
            R.id.tvCheck -> okHttpList()
            R.id.tvStationNameMinute -> {
                tvTimeMinute.text = "时间"
                tvRainMinute.text = "雨量\n(mm)"
                minute1 = !minute1
                if (minute1) { //升序
                    tvStationNameMinute.text = "站名↑"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.stationName) || TextUtils.isEmpty(arg1.stationName)) {
                            0
                        } else {
                            getPinYinHeadChar(arg0.stationName).compareTo(getPinYinHeadChar(arg1.stationName))
                        }
                    })
                } else { //将序
                    tvStationNameMinute.text = "站名↓"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.stationName) || TextUtils.isEmpty(arg1.stationName)) {
                            -1
                        } else {
                            getPinYinHeadChar(arg1.stationName).compareTo(getPinYinHeadChar(arg0.stationName))
                        }
                    })
                }
                if (minuteAdapter != null) {
                    minuteAdapter!!.notifyDataSetChanged()
                }
            }
            R.id.tvTimeMinute -> {
                tvStationNameMinute.text = "站名"
                tvRainMinute.text = "雨量\n(mm)"
                minute2 = !minute2
                if (minute2) { //升序
                    tvTimeMinute.text = "时间↑"
                    dataList.sortWith(Comparator { arg0, arg1 -> sdf1.parse(arg0.time).compareTo(sdf1.parse(arg1.time)) })
                } else { //将序
                    tvTimeMinute.text = "时间↓"
                    dataList.sortWith(Comparator { arg0, arg1 -> sdf1.parse(arg1.time).compareTo(sdf1.parse(arg0.time)) })
                }
                if (minuteAdapter != null) {
                    minuteAdapter!!.notifyDataSetChanged()
                }
            }
            R.id.tvRainMinute -> {
                tvStationNameMinute.text = "站名"
                tvTimeMinute.text = "时间"
                minute3 = !minute3
                if (minute3) { //升序
                    tvRainMinute.text = "雨量↑\n(mm)"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.rain) || TextUtils.isEmpty(arg1.rain) || TextUtils.equals(arg0.rain, CONST.NO_VALUE) || TextUtils.equals(arg1.rain, CONST.NO_VALUE)) {
                            0
                        } else {
                            java.lang.Double.valueOf(arg0.rain).compareTo(java.lang.Double.valueOf(arg1.rain))
                        }
                    })
                } else { //将序
                    tvRainMinute.text = "雨量↓\n(mm)"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.rain) || TextUtils.isEmpty(arg1.rain) || TextUtils.equals(arg0.rain, CONST.NO_VALUE) || TextUtils.equals(arg1.rain, CONST.NO_VALUE)) {
                            -1
                        } else {
                            java.lang.Double.valueOf(arg1.rain).compareTo(java.lang.Double.valueOf(arg0.rain))
                        }
                    })
                }
                if (minuteAdapter != null) {
                    minuteAdapter!!.notifyDataSetChanged()
                }
            }

            R.id.tvArea -> {
                tvStationName.text = "站名"
                tvTime.text = "时间"
                tvRain.text = "雨量\n(mm)"
                tvTemp.text = "温度\n(℃)"
                tvHumidity.text = "湿度\n(%)"
                tvWinds.text = "风速\n(m/s)"
                tvWindd.text = "风向\n(度)"
                tvVis.text = "能见度\n(km)"
                hour1 = !hour1
                if (hour1) { //升序
                    tvArea.text = "所属区域↑"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.area) || TextUtils.isEmpty(arg1.area)) {
                            0
                        } else {
                            getPinYinHeadChar(arg0.area).compareTo(getPinYinHeadChar(arg1.area))
                        }
                    })
                } else { //将序
                    tvArea.text = "所属区域↓"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.area) || TextUtils.isEmpty(arg1.area)) {
                            -1
                        } else {
                            getPinYinHeadChar(arg1.area).compareTo(getPinYinHeadChar(arg0.area))
                        }
                    })
                }
                if (hourAdapter != null) {
                    hourAdapter!!.notifyDataSetChanged()
                }
            }
            R.id.tvStationName -> {
                tvArea.text = "所属区域"
                tvTime.text = "时间"
                tvRain.text = "雨量\n(mm)"
                tvTemp.text = "温度\n(℃)"
                tvHumidity.text = "湿度\n(%)"
                tvWinds.text = "风速\n(m/s)"
                tvWindd.text = "风向\n(度)"
                tvVis.text = "能见度\n(km)"
                hour2 = !hour2
                if (hour2) { //升序
                    tvStationName.text = "站名↑"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.stationName) || TextUtils.isEmpty(arg1.stationName)) {
                            0
                        } else {
                            getPinYinHeadChar(arg0.stationName).compareTo(getPinYinHeadChar(arg1.stationName))
                        }
                    })
                } else { //将序
                    tvStationName.text = "站名↓"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.stationName) || TextUtils.isEmpty(arg1.stationName)) {
                            -1
                        } else {
                            getPinYinHeadChar(arg1.stationName).compareTo(getPinYinHeadChar(arg0.stationName))
                        }
                    })
                }
                if (hourAdapter != null) {
                    hourAdapter!!.notifyDataSetChanged()
                }
            }
            R.id.tvTime -> {
                tvArea.text = "所属区域"
                tvStationName.text = "站名"
                tvRain.text = "雨量\n(mm)"
                tvTemp.text = "温度\n(℃)"
                tvHumidity.text = "湿度\n(%)"
                tvWinds.text = "风速\n(m/s)"
                tvWindd.text = "风向\n(度)"
                tvVis.text = "能见度\n(km)"
                hour3 = !hour3
                if (hour3) { //升序
                    tvTime.text = "时间↑"
                    dataList.sortWith(Comparator { arg0, arg1 -> sdf1.parse(arg0.time).compareTo(sdf1.parse(arg1.time)) })
                } else { //将序
                    tvTime.text = "时间↓"
                    dataList.sortWith(Comparator { arg0, arg1 -> sdf1.parse(arg1.time).compareTo(sdf1.parse(arg0.time)) })
                }
                if (hourAdapter != null) {
                    hourAdapter!!.notifyDataSetChanged()
                }
            }
            R.id.tvRain -> {
                tvArea.text = "所属区域"
                tvStationName.text = "站名"
                tvTime.text = "时间"
                tvTemp.text = "温度\n(℃)"
                tvHumidity.text = "湿度\n(%)"
                tvWinds.text = "风速\n(m/s)"
                tvWindd.text = "风向\n(度)"
                tvVis.text = "能见度\n(km)"
                hour4 = !hour4
                if (hour4) { //升序
                    tvRain.text = "雨量↑\n(mm)"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.rain) || TextUtils.isEmpty(arg1.rain) || TextUtils.equals(arg0.rain, CONST.NO_VALUE) || TextUtils.equals(arg1.rain, CONST.NO_VALUE)) {
                            0
                        } else {
                            java.lang.Double.valueOf(arg0.rain).compareTo(java.lang.Double.valueOf(arg1.rain))
                        }
                    })
                } else { //将序
                    tvRain.text = "雨量↓\n(mm)"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.rain) || TextUtils.isEmpty(arg1.rain) || TextUtils.equals(arg0.rain, CONST.NO_VALUE) || TextUtils.equals(arg1.rain, CONST.NO_VALUE)) {
                            -1
                        } else {
                            java.lang.Double.valueOf(arg1.rain).compareTo(java.lang.Double.valueOf(arg0.rain))
                        }
                    })
                }
                if (hourAdapter != null) {
                    hourAdapter!!.notifyDataSetChanged()
                }
            }
            R.id.tvTemp -> {
                tvArea.text = "所属区域"
                tvStationName.text = "站名"
                tvTime.text = "时间"
                tvRain.text = "雨量\n(mm)"
                tvHumidity.text = "湿度\n(%)"
                tvWinds.text = "风速\n(m/s)"
                tvWindd.text = "风向\n(度)"
                tvVis.text = "能见度\n(km)"
                hour5 = !hour5
                if (hour5) { //升序
                    tvTemp.text = "温度↑\n(℃)"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.temp) || TextUtils.isEmpty(arg1.temp) || TextUtils.equals(arg0.temp, CONST.NO_VALUE) || TextUtils.equals(arg1.temp, CONST.NO_VALUE)) {
                            0
                        } else {
                            java.lang.Double.valueOf(arg0.temp).compareTo(java.lang.Double.valueOf(arg1.temp))
                        }
                    })
                } else { //将序
                    tvTemp.text = "温度↓\n(℃)"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.temp) || TextUtils.isEmpty(arg1.temp) || TextUtils.equals(arg0.temp, CONST.NO_VALUE) || TextUtils.equals(arg1.temp, CONST.NO_VALUE)) {
                            -1
                        } else {
                            java.lang.Double.valueOf(arg1.temp).compareTo(java.lang.Double.valueOf(arg0.temp))
                        }
                    })
                }
                if (hourAdapter != null) {
                    hourAdapter!!.notifyDataSetChanged()
                }
            }
            R.id.tvHumidity -> {
                tvArea.text = "所属区域"
                tvStationName.text = "站名"
                tvTime.text = "时间"
                tvRain.text = "雨量\n(mm)"
                tvTemp.text = "温度\n(℃)"
                tvWinds.text = "风速\n(m/s)"
                tvWindd.text = "风向\n(度)"
                tvVis.text = "能见度\n(km)"
                hour6 = !hour6
                if (hour6) { //升序
                    tvHumidity.text = "湿度↑\n(%)"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.humidity) || TextUtils.isEmpty(arg1.humidity) || TextUtils.equals(arg0.humidity, CONST.NO_VALUE) || TextUtils.equals(arg1.humidity, CONST.NO_VALUE)) {
                            0
                        } else {
                            java.lang.Double.valueOf(arg0.humidity).compareTo(java.lang.Double.valueOf(arg1.humidity))
                        }
                    })
                } else { //将序
                    tvHumidity.text = "湿度↓\n(%)"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.humidity) || TextUtils.isEmpty(arg1.humidity) || TextUtils.equals(arg0.humidity, CONST.NO_VALUE) || TextUtils.equals(arg1.humidity, CONST.NO_VALUE)) {
                            -1
                        } else {
                            java.lang.Double.valueOf(arg1.humidity).compareTo(java.lang.Double.valueOf(arg0.humidity))
                        }
                    })
                }
                if (hourAdapter != null) {
                    hourAdapter!!.notifyDataSetChanged()
                }
            }
            R.id.tvWinds -> {
                tvArea.text = "所属区域"
                tvStationName.text = "站名"
                tvTime.text = "时间"
                tvRain.text = "雨量\n(mm)"
                tvTemp.text = "温度\n(℃)"
                tvHumidity.text = "湿度\n(%)"
                tvWindd.text = "风向\n(度)"
                tvVis.text = "能见度\n(km)"
                hour7 = !hour7
                if (hour7) { //升序
                    tvWinds.text = "风速↑\n(m/s)"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.windSpeed) || TextUtils.isEmpty(arg1.windSpeed) || TextUtils.equals(arg0.windSpeed, CONST.NO_VALUE) || TextUtils.equals(arg1.windSpeed, CONST.NO_VALUE)) {
                            0
                        } else {
                            java.lang.Double.valueOf(arg0.windSpeed).compareTo(java.lang.Double.valueOf(arg1.windSpeed))
                        }
                    })
                } else { //将序
                    tvWinds.text = "风速↓\n(m/s)"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.windSpeed) || TextUtils.isEmpty(arg1.windSpeed) || TextUtils.equals(arg0.windSpeed, CONST.NO_VALUE) || TextUtils.equals(arg1.windSpeed, CONST.NO_VALUE)) {
                            -1
                        } else {
                            java.lang.Double.valueOf(arg1.windSpeed).compareTo(java.lang.Double.valueOf(arg0.windSpeed))
                        }
                    })
                }
                if (hourAdapter != null) {
                    hourAdapter!!.notifyDataSetChanged()
                }
            }
            R.id.tvWindd -> {
                tvArea.text = "所属区域"
                tvStationName.text = "站名"
                tvTime.text = "时间"
                tvRain.text = "雨量\n(mm)"
                tvTemp.text = "温度\n(℃)"
                tvHumidity.text = "湿度\n(%)"
                tvWinds.text = "风速\n(m/s)"
                tvVis.text = "能见度\n(km)"
                hour8 = !hour8
                if (hour8) { //升序
                    tvWindd.text = "风向↑\n(度)"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.windDir) || TextUtils.isEmpty(arg1.windDir) || TextUtils.equals(arg0.windDir, CONST.NO_VALUE) || TextUtils.equals(arg1.windDir, CONST.NO_VALUE)) {
                            0
                        } else {
                            java.lang.Double.valueOf(arg0.windDir).compareTo(java.lang.Double.valueOf(arg1.windDir))
                        }
                    })
                } else { //将序
                    tvWindd.text = "风向↓\n(度)"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.windDir) || TextUtils.isEmpty(arg1.windDir) || TextUtils.equals(arg0.windDir, CONST.NO_VALUE) || TextUtils.equals(arg1.windDir, CONST.NO_VALUE)) {
                            -1
                        } else {
                            java.lang.Double.valueOf(arg1.windDir).compareTo(java.lang.Double.valueOf(arg0.windDir))
                        }
                    })
                }
                if (hourAdapter != null) {
                    hourAdapter!!.notifyDataSetChanged()
                }
            }
            R.id.tvVis -> {
                tvArea.text = "所属区域"
                tvStationName.text = "站名"
                tvTime.text = "时间"
                tvRain.text = "雨量\n(mm)"
                tvTemp.text = "温度\n(℃)"
                tvHumidity.text = "湿度\n(%)"
                tvWinds.text = "风速\n(m/s)"
                tvWindd.text = "风向\n(度)"
                hour9 = !hour9
                if (hour9) { //升序
                    tvVis.text = "能见度↑\n(km)"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.visibility) || TextUtils.isEmpty(arg1.visibility) || TextUtils.equals(arg0.visibility, CONST.NO_VALUE) || TextUtils.equals(arg1.visibility, CONST.NO_VALUE)) {
                            0
                        } else {
                            java.lang.Double.valueOf(arg0.visibility).compareTo(java.lang.Double.valueOf(arg1.visibility))
                        }
                    })
                } else { //将序
                    tvVis.text = "能见度↓\n(km)"
                    dataList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0.visibility) || TextUtils.isEmpty(arg1.visibility) || TextUtils.equals(arg0.visibility, CONST.NO_VALUE) || TextUtils.equals(arg1.visibility, CONST.NO_VALUE)) {
                            -1
                        } else {
                            java.lang.Double.valueOf(arg1.visibility).compareTo(java.lang.Double.valueOf(arg0.visibility))
                        }
                    })
                }
                if (hourAdapter != null) {
                    hourAdapter!!.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when(requestCode) {
                1001 -> {
                    if (data != null) {
                        val bundle = data.extras
                        if (bundle != null) {
                            city = bundle.getString("city")
                            area = bundle.getString("area")
                            if (!TextUtils.isEmpty(city) && !TextUtils.isEmpty(area)) {
                                tvSearch.text = "$area($city)"
                                okHttpList()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initWheelView() {
        tvStartTime.setOnClickListener(this)
        tvNegtive.setOnClickListener(this)
        tvPositive.setOnClickListener(this)

        val c = Calendar.getInstance()
        val curYear = c[Calendar.YEAR]
        val curMonth = c[Calendar.MONTH] + 1 //通过Calendar算出的月数要+1
        val curDate = c[Calendar.DATE]
        val curHour = c[Calendar.HOUR_OF_DAY]
        val curMinute = c[Calendar.MINUTE]
        val curSecond = c[Calendar.SECOND]

        val numericWheelAdapter1 = NumericWheelAdapter(activity, 1950, curYear)
        numericWheelAdapter1.setLabel("年")
        year.viewAdapter = numericWheelAdapter1
        year.isCyclic = false //是否可循环滑动
        year.addScrollingListener(scrollListener)
        year.visibleItems = 7
        year.visibility = View.VISIBLE

        val numericWheelAdapter2 = NumericWheelAdapter(activity, 1, 12, "%02d")
        numericWheelAdapter2.setLabel("月")
        month.viewAdapter = numericWheelAdapter2
        month.isCyclic = false
        month.addScrollingListener(scrollListener)
        month.visibleItems = 7
        month.visibility = View.VISIBLE

        initDay(curYear, curMonth)
        day.isCyclic = false
        day.visibleItems = 7
        day.visibility = View.VISIBLE

        val numericWheelAdapter3 = NumericWheelAdapter(activity, 0, 23, "%02d")
        numericWheelAdapter3.setLabel("时")
        hour.viewAdapter = numericWheelAdapter3
        hour.isCyclic = false
        hour.addScrollingListener(scrollListener)
        hour.visibleItems = 7
        hour.visibility = View.VISIBLE

        val numericWheelAdapter4 = NumericWheelAdapter(activity, 0, 59, "%02d")
        numericWheelAdapter4.setLabel("分")
        minute.viewAdapter = numericWheelAdapter4
        minute.isCyclic = false
        minute.addScrollingListener(scrollListener)
        minute.visibleItems = 7
        minute.visibility = View.VISIBLE

        val numericWheelAdapter5 = NumericWheelAdapter(activity, 0, 59, "%02d")
        numericWheelAdapter5.setLabel("秒")
        second.viewAdapter = numericWheelAdapter5
        second.isCyclic = false
        second.addScrollingListener(scrollListener)
        second.visibleItems = 7
        second.visibility = View.GONE

        year.currentItem = curYear - 1950
        month.currentItem = curMonth - 1
        day.currentItem = curDate - 1
        hour.currentItem = curHour
        minute.currentItem = curMinute
        second.currentItem = curSecond
    }

    private val scrollListener: OnWheelScrollListener = object : OnWheelScrollListener {
        override fun onScrollingStarted(wheel: WheelView) {}
        override fun onScrollingFinished(wheel: WheelView) {
            val nYear = year!!.currentItem + 1950 //年
            val nMonth: Int = month.currentItem + 1 //月
            initDay(nYear, nMonth)
        }
    }

    /**
     */
    private fun initDay(arg1: Int, arg2: Int) {
        val numericWheelAdapter = NumericWheelAdapter(activity, 1, getDay(arg1, arg2), "%02d")
        numericWheelAdapter.setLabel("日")
        day.viewAdapter = numericWheelAdapter
    }

    /**
     * @param year
     * @param month
     * @return
     */
    private fun getDay(year: Int, month: Int): Int {
        var day = 30
        var flag = false
        flag = when (year % 4) {
            0 -> true
            else -> false
        }
        day = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            2 -> if (flag) 29 else 28
            else -> 30
        }
        return day
    }

    /**
     */
    private fun setTextViewValue() {
        val yearStr = (year!!.currentItem + 1950).toString()
        val monthStr = if (month.currentItem + 1 < 10) "0" + (month.currentItem + 1) else (month.currentItem + 1).toString()
        val dayStr = if (day.currentItem + 1 < 10) "0" + (day.currentItem + 1) else (day.currentItem + 1).toString()
        val hourStr = if (hour.currentItem < 10) "0" + (hour.currentItem) else (hour.currentItem).toString()
        val minuteStr = if (minute.currentItem < 10) "0" + (minute.currentItem) else (minute.currentItem).toString()
        val secondStr = if (second.currentItem < 10) "0" + (second.currentItem) else (second.currentItem).toString()
        if (isMinute) {
            tvStartTime.text = "$yearStr-$monthStr-$dayStr ${hourStr}:${minuteStr}"
        } else {
            tvStartTime.text = "$yearStr-$monthStr-$dayStr ${hourStr}"
        }
        startTime = "$yearStr$monthStr$dayStr${hourStr}${minuteStr}${secondStr}"
    }

    private fun bootTimeLayoutAnimation(view: View) {
        if (view!!.visibility == View.GONE) {
            timeLayoutAnimation(true, view)
            view!!.visibility = View.VISIBLE
        } else {
            timeLayoutAnimation(false, view)
            view!!.visibility = View.GONE
        }
    }

    /**
     * 时间图层动画
     * @param flag
     * @param view
     */
    private fun timeLayoutAnimation(flag: Boolean, view: View?) {
        //列表动画
        val animationSet = AnimationSet(true)
        val animation: TranslateAnimation = if (!flag) {
            TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0f,
                    Animation.RELATIVE_TO_SELF, 0f,
                    Animation.RELATIVE_TO_SELF, 0f,
                    Animation.RELATIVE_TO_SELF, 1f)
        } else {
            TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0f,
                    Animation.RELATIVE_TO_SELF, 0f,
                    Animation.RELATIVE_TO_SELF, 1f,
                    Animation.RELATIVE_TO_SELF, 0f)
        }
        animation.duration = 200
        animationSet.addAnimation(animation)
        animationSet.fillAfter = true
        view!!.startAnimation(animationSet)
        animationSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(arg0: Animation) {}
            override fun onAnimationRepeat(arg0: Animation) {}
            override fun onAnimationEnd(arg0: Animation) {
                view.clearAnimation()
            }
        })
    }
	
}
