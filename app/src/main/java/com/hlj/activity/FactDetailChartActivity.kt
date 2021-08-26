package com.hlj.activity


import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.View.OnClickListener
import com.hlj.dto.FactDto
import com.hlj.utils.CommonUtil
import com.hlj.utils.OkHttpUtil
import com.hlj.view.FactRainView
import com.hlj.view.FactTempView
import com.hlj.view.FactWindView
import kotlinx.android.synthetic.main.activity_fact_detail_chart.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException
import java.util.*

/**
 * 实况详情图表
 */
class FactDetailChartActivity : BaseActivity(), OnClickListener {

    private val rainList: MutableList<FactDto> = ArrayList()
    private val tempList: MutableList<FactDto> = ArrayList()
    private val windList: MutableList<FactDto> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fact_detail_chart)
        showDialog()
        initWidget()
    }

    private fun initWidget() {
        llBack.setOnClickListener(this)
        val data: FactDto = intent.getParcelableExtra("data")
        if (data != null) {
            if (!TextUtils.isEmpty(data.stationName)) {
                tvTitle.text = data.stationName
            }
            if (!TextUtils.isEmpty(data.stationCode)) {
                okHttpStationInfo(data.stationCode)
            }
        }
    }

    /**
     * 获取站点数据
     */
    private fun okHttpStationInfo(stationCode: String) {
        Thread {
            val url = "http://decision-171.tianqi.cn/api/heilj/dates/getone48?id=$stationCode"
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
                                if (!obj.isNull("list")) {
                                    val array = obj.getJSONArray("list")
                                    rainList.clear()
                                    tempList.clear()
                                    windList.clear()
                                    for (i in 0 until array.length()) {
                                        val dto = FactDto()
                                        val itemObj = array.getJSONObject(i)
                                        if (!itemObj.isNull("Datetime")) {
                                            dto.factTime = itemObj.getString("Datetime")
                                        }
                                        var value = ""
                                        if (!itemObj.isNull("JS")) {
                                            value = itemObj.getString("JS")
                                            dto.factRain = value.toFloat()
                                        }
                                        if (!value.contains("99999")) {
                                            rainList.add(dto)
                                        }
                                        value = ""
                                        if (!itemObj.isNull("WD")) {
                                            value = itemObj.getString("WD")
                                            dto.factTemp = value.toFloat()
                                        }
                                        if (!value.contains("99999")) {
                                            tempList.add(dto)
                                        }
                                        value = ""
                                        if (!itemObj.isNull("FS")) {
                                            value = itemObj.getString("FS")
                                            dto.factWind = value.toFloat()
                                        }
                                        if (!value.contains("99999")) {
                                            windList.add(dto)
                                        }
                                    }
                                    val rainView = FactRainView(this@FactDetailChartActivity)
                                    rainView.setData(rainList)
                                    llContainer1!!.removeAllViews()
                                    val viewWidth1 = if (rainList.size <= 25) CommonUtil.widthPixels(this@FactDetailChartActivity) * 2 else CommonUtil.widthPixels(this@FactDetailChartActivity) * 4
                                    llContainer1!!.addView(rainView, viewWidth1, CommonUtil.heightPixels(this@FactDetailChartActivity) / 3)
                                    val tempView = FactTempView(this@FactDetailChartActivity)
                                    tempView.setData(tempList)
                                    llContainer2.removeAllViews()
                                    val viewWidth2 = if (rainList.size <= 25) CommonUtil.widthPixels(this@FactDetailChartActivity) * 2 else CommonUtil.widthPixels(this@FactDetailChartActivity) * 4
                                    llContainer2.addView(tempView, viewWidth2, CommonUtil.heightPixels(this@FactDetailChartActivity) / 3)
                                    val windView = FactWindView(this@FactDetailChartActivity)
                                    windView.setData(windList)
                                    llContainer3.removeAllViews()
                                    val viewWidth3 = if (windList.size <= 25) CommonUtil.widthPixels(this@FactDetailChartActivity) * 2 else CommonUtil.widthPixels(this@FactDetailChartActivity) * 4
                                    llContainer3.addView(windView, viewWidth3, CommonUtil.heightPixels(this@FactDetailChartActivity) / 3)
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
        }
    }
	
}
