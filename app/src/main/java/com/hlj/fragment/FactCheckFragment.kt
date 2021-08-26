package com.hlj.fragment

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.Toast
import com.hlj.activity.FactDetailChartActivity
import com.hlj.adapter.FactCheckCitysAdapter
import com.hlj.adapter.FactDetailAdapter
import com.hlj.dto.FactDto
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.dialog_fact_check_time.view.*
import kotlinx.android.synthetic.main.fragment_fact_check.*
import net.sourceforge.pinyin4j.PinyinHelper
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

/**
 * 任意时段查询,小时
 * @author shawn_sun
 */
class FactCheckFragment : Fragment(), OnClickListener {

    private var checkArea = ""
    private var areaAdapter: FactCheckCitysAdapter? = null
    private val areaList: MutableList<FactDto> = ArrayList()
    private var b1 = false
    private var b2 = false
    private var b3 = false //false为将序，true为升序
    private var checkAdapter: FactDetailAdapter? = null
    private val checkList: MutableList<FactDto> = ArrayList()
    private val sdf3 = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA)
    private var startTimeCheck: String? = null
    private var endTimeCheck: String? = null
    private var maxTime: String? = null
    private var minTime: String? = null
    private var startOrEnd = true //true为start
    private val hanNan = "黑龙江全省"
    private val childId = "1154"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_fact_check, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initWidget()
        initAreaList()
        initCheckListView()
    }

    private fun initWidget() {
        llArea!!.setOnClickListener(this)
        tvArea!!.setOnClickListener(this)
        tvStartDay!!.setOnClickListener(this)
        tvStartHour.setOnClickListener(this)
        tvEndDay!!.setOnClickListener(this)
        tvEndHour.setOnClickListener(this)
        tvCheck!!.setOnClickListener(this)
        llStart!!.setOnClickListener(this)
        llEnd.setOnClickListener(this)
        llStartMinute!!.visibility = View.GONE
        llEndMinute.visibility = View.GONE
        ll1!!.setOnClickListener(this)
        ll2.setOnClickListener(this)
        ll3.setOnClickListener(this)
        okHttpCheck("http://decision-171.tianqi.cn/api/heilj/dates/getcitid?city=&start=&end=&cid=$childId")
    }

    private fun initAreaList() {
        areaAdapter = FactCheckCitysAdapter(activity, areaList)
        areaListView!!.adapter = areaAdapter
        areaListView!!.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 ->
            val dto = areaList[arg2]
            tvArea!!.text = dto.area
            checkArea = dto.area
            areaListView!!.visibility = View.GONE
        }
    }

    private fun initCheckListView() {
        checkAdapter = FactDetailAdapter(activity, checkList)
        listViewCheck!!.adapter = checkAdapter
        listViewCheck!!.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 ->
            val dto = checkList[arg2]
            val intent = Intent(activity, FactDetailChartActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable("data", dto)
            intent.putExtras(bundle)
            startActivity(intent)
        }
    }

    private fun okHttpCheck(url: String) {
        progressBar!!.visibility = View.VISIBLE
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
                                if (TextUtils.isEmpty(startTimeCheck) && TextUtils.isEmpty(endTimeCheck)) {
                                    if (!obj.isNull("maxtime")) {
                                        try {
                                            minTime = obj.getString("mintime")
                                            maxTime = obj.getString("maxtime")
                                            endTimeCheck = obj.getString("maxtime")
                                            startTimeCheck = sdf3.format(sdf3.parse(endTimeCheck).time - 1000 * 60 * 60)
                                            tvArea!!.text = hanNan
                                            checkArea = ""
                                            val y = startTimeCheck!!.substring(0, 4)
                                            val m = startTimeCheck!!.substring(4, 6)
                                            val d = startTimeCheck!!.substring(6, 8)
                                            val h = startTimeCheck!!.substring(8, 10)
                                            tvStartDay!!.text = "$y-$m-$d"
                                            tvStartHour.text = h + "时"
                                            val y2 = endTimeCheck!!.substring(0, 4)
                                            val m2 = endTimeCheck!!.substring(4, 6)
                                            val d2 = endTimeCheck!!.substring(6, 8)
                                            val h2 = endTimeCheck!!.substring(8, 10)
                                            tvEndDay!!.text = "$y2-$m2-$d2"
                                            tvEndHour.text = h2 + "时"
                                        } catch (e: ParseException) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                                if (!obj.isNull("ciytlist")) {
                                    areaList.clear()
                                    val array = obj.getJSONArray("ciytlist")
                                    for (i in 0 until array.length()) {
                                        val dto = FactDto()
                                        if (!TextUtils.isEmpty(array.getString(i))) {
                                            dto.area = array.getString(i)
                                            areaList.add(dto)
                                        }
                                    }
                                    val dto = FactDto()
                                    dto.area = hanNan
                                    areaList.add(0, dto)
                                    if (areaList.size > 0 && areaAdapter != null) {
                                        areaAdapter!!.notifyDataSetChanged()
                                    }
                                }
                                if (!obj.isNull("th")) {
                                    val th = obj.getJSONObject("th")
                                    if (!th.isNull("stationName")) {
                                        tv1!!.text = th.getString("stationName")
                                    }
                                    if (!th.isNull("area")) {
                                        tv2.text = th.getString("area")
                                    }
                                    if (!th.isNull("val")) {
                                        tv3.text = th.getString("val")
                                    }
                                }
                                if (!obj.isNull("list")) {
                                    checkList.clear()
                                    val array = obj.getJSONArray("list")
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
                                        if (!itemObj.isNull("val")) {
                                            dto.`val` = itemObj.getDouble("val")
                                        }
                                        if (!TextUtils.isEmpty(dto.area)) {
                                            checkList.add(dto)
                                        }
                                    }
                                    if (checkList.size > 0 && checkAdapter != null) {
                                        checkAdapter!!.notifyDataSetChanged()
                                    }
                                }
                                if (!b3) { //将序
                                    iv3.setImageResource(R.drawable.arrow_down)
                                } else { //将序
                                    iv3.setImageResource(R.drawable.arrow_up)
                                }
                                iv3.visibility = View.VISIBLE
                                progressBar!!.visibility = View.GONE
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

    private fun selectDateTimeDialog(message: String) {
        val inflater = activity!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_fact_check_time, null)
        view.timePicker.setIs24HourView(true)
        view.timePicker.setOnTimeChangedListener { arg0, arg1, arg2 -> view.timePicker.currentMinute = 0 }
        try {
            view.datePickr.minDate = sdf3.parse(minTime).time
            view.datePickr.maxDate = sdf3.parse(maxTime).time
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        val y: String
        val m: String
        val d: String
        val h: String
        if (startOrEnd) {
            if (!TextUtils.isEmpty(startTimeCheck)) {
                y = startTimeCheck!!.substring(0, 4)
                m = startTimeCheck!!.substring(4, 6)
                d = startTimeCheck!!.substring(6, 8)
                h = startTimeCheck!!.substring(8, 10)
                view.datePickr.init(Integer.valueOf(y), Integer.valueOf(m) - 1, Integer.valueOf(d), null)
                view.timePicker.currentHour = Integer.valueOf(h)
                view.timePicker.currentMinute = 0
            }
        } else {
            if (!TextUtils.isEmpty(endTimeCheck)) {
                y = endTimeCheck!!.substring(0, 4)
                m = endTimeCheck!!.substring(4, 6)
                d = endTimeCheck!!.substring(6, 8)
                h = endTimeCheck!!.substring(8, 10)
                view.datePickr.init(Integer.valueOf(y), Integer.valueOf(m) - 1, Integer.valueOf(d), null)
                view.timePicker.currentHour = Integer.valueOf(h)
                view.timePicker.currentMinute = 0
            }
        }
        val dialog = Dialog(activity, R.style.CustomProgressDialog)
        dialog.setContentView(view)
        dialog.show()
        view.tvMessage.text = message
        view.llPositive.setOnClickListener {
            dialog.dismiss()
            val year = view.datePickr.year
            val month = view.datePickr.month + 1
            val day = view.datePickr.dayOfMonth
            val hour = view.timePicker.currentHour
            val yearStr = year.toString() + ""
            var monthStr = month.toString() + ""
            if (month < 10) {
                monthStr = "0$monthStr"
            }
            var dayStr = day.toString() + ""
            if (day < 10) {
                dayStr = "0$dayStr"
            }
            var hourStr = hour.toString() + ""
            if (hour < 10) {
                hourStr = "0$hourStr"
            }
            if (startOrEnd) {
                startTimeCheck = yearStr + monthStr + dayStr + hourStr + "0000"
                tvStartDay!!.text = "$yearStr-$monthStr-$dayStr"
                tvStartHour.text = hourStr + "时"
            } else {
                endTimeCheck = yearStr + monthStr + dayStr + hourStr + "0000"
                tvEndDay!!.text = "$yearStr-$monthStr-$dayStr"
                tvEndHour.text = hourStr + "时"
            }
        }
        view.llNegative.setOnClickListener { dialog.dismiss() }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.tvArea, R.id.llArea -> {
                if (areaListView!!.visibility == View.GONE) {
                    areaListView!!.visibility = View.VISIBLE
                } else {
                    areaListView!!.visibility = View.GONE
                }
            }
            R.id.tvStartDay, R.id.tvStartHour, R.id.tvStartMinute, R.id.llStart -> {
                startOrEnd = true
                selectDateTimeDialog("选择开始时间")
            }
            R.id.tvEndDay, R.id.tvEndHour, R.id.tvEndMinute, R.id.llEnd -> {
                startOrEnd = false
                selectDateTimeDialog("选择结束时间")
            }
            R.id.tvCheck -> {
                if (TextUtils.isEmpty(startTimeCheck)) {
                    Toast.makeText(activity, "请选择开始时间", Toast.LENGTH_SHORT).show()
                    return
                }
                if (TextUtils.isEmpty(endTimeCheck)) {
                    Toast.makeText(activity, "请选择结束时间", Toast.LENGTH_SHORT).show()
                    return
                }
                if (java.lang.Long.valueOf(startTimeCheck) >= java.lang.Long.valueOf(endTimeCheck)) {
                    Toast.makeText(activity, "开始时间不能大于或等于结束时间", Toast.LENGTH_SHORT).show()
                    return
                }
                if (TextUtils.equals(tvArea!!.text.toString(), hanNan)) {
                    checkArea = ""
                }
                okHttpCheck("http://decision-171.tianqi.cn/api/heilj/dates/getcitid?city=$checkArea&start=$startTimeCheck&end=$endTimeCheck&cid=$childId")
            }
            R.id.ll1 -> {
                if (b1) { //升序
                    b1 = false
                    iv1!!.setImageResource(R.drawable.arrow_up)
                    iv1!!.visibility = View.VISIBLE
                    iv2.visibility = View.INVISIBLE
                    iv3.visibility = View.INVISIBLE
                    checkList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0!!.stationName) || TextUtils.isEmpty(arg1!!.stationName)) {
                            0
                        } else {
                            getPinYinHeadChar(arg0!!.stationName).compareTo(getPinYinHeadChar(arg1!!.stationName))
                        }
                    })
                } else { //将序
                    b1 = true
                    iv1!!.setImageResource(R.drawable.arrow_down)
                    iv1!!.visibility = View.VISIBLE
                    iv2.visibility = View.INVISIBLE
                    iv3.visibility = View.INVISIBLE
                    checkList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0!!.stationName) || TextUtils.isEmpty(arg1!!.stationName)) {
                            -1
                        } else {
                            getPinYinHeadChar(arg1!!.stationName).compareTo(getPinYinHeadChar(arg0!!.stationName))
                        }
                    })
                }
                if (checkAdapter != null) {
                    checkAdapter!!.notifyDataSetChanged()
                }
            }
            R.id.ll2 -> {
                if (b2) { //升序
                    b2 = false
                    iv2.setImageResource(R.drawable.arrow_up)
                    iv1!!.visibility = View.INVISIBLE
                    iv2.visibility = View.VISIBLE
                    iv3.visibility = View.INVISIBLE
                    checkList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0!!.area) || TextUtils.isEmpty(arg1!!.area)) {
                            0
                        } else {
                            getPinYinHeadChar(arg0!!.area).compareTo(getPinYinHeadChar(arg1!!.area))
                        }
                    })
                } else { //将序
                    b2 = true
                    iv2.setImageResource(R.drawable.arrow_down)
                    iv1!!.visibility = View.INVISIBLE
                    iv2.visibility = View.VISIBLE
                    iv3.visibility = View.INVISIBLE
                    checkList.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0!!.area) || TextUtils.isEmpty(arg1!!.area)) {
                            -1
                        } else {
                            getPinYinHeadChar(arg1!!.area).compareTo(getPinYinHeadChar(arg0!!.area))
                        }
                    })
                }
                if (checkAdapter != null) {
                    checkAdapter!!.notifyDataSetChanged()
                }
            }
            R.id.ll3 -> {
                if (b3) { //升序
                    b3 = false
                    iv3.setImageResource(R.drawable.arrow_up)
                    iv1!!.visibility = View.INVISIBLE
                    iv2.visibility = View.INVISIBLE
                    iv3.visibility = View.VISIBLE
                    checkList.sortWith(Comparator { arg0, arg1 -> java.lang.Double.valueOf(arg0!!.`val`).compareTo(java.lang.Double.valueOf(arg1!!.`val`)) })
                } else { //将序
                    b3 = true
                    iv3.setImageResource(R.drawable.arrow_down)
                    iv1!!.visibility = View.INVISIBLE
                    iv2.visibility = View.INVISIBLE
                    iv3.visibility = View.VISIBLE
                    checkList.sortWith(Comparator { arg0, arg1 -> java.lang.Double.valueOf(arg1!!.`val`).compareTo(java.lang.Double.valueOf(arg0!!.`val`)) })
                }
                if (checkAdapter != null) {
                    checkAdapter!!.notifyDataSetChanged()
                }
            }
        }
    }
	
}
