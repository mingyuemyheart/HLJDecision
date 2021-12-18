package com.hlj.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.AdapterView.OnItemClickListener
import android.widget.ImageView
import com.amap.api.maps.AMap
import com.amap.api.maps.AMap.OnMarkerClickListener
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.hlj.adapter.WeatherRadarAdapter
import com.hlj.common.CONST
import com.hlj.dto.AgriDto
import com.hlj.utils.CommonUtil
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.activity_weather_radar.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import shawn.cxwl.com.hlj.R
import java.io.IOException
import java.util.*

/**
 * 天气雷达
 */
class WeatherRadarActivity : BaseActivity(), OnClickListener, OnMarkerClickListener {

    private var aMap: AMap? = null //高德地图
    private var mAdapter: WeatherRadarAdapter? = null
    private val dataList: MutableList<AgriDto> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_radar)
        initAmap(savedInstanceState)
        initWidget()
        initGridView()
    }

    private fun initWidget() {
        llBack!!.setOnClickListener(this)
        ivExpand!!.setOnClickListener(this)

        val data: AgriDto = intent.extras.getParcelable("data")
        if (data.name != null) {
            tvTitle!!.text = data.name
        }
    }

    /**
     * 初始化高德地图
     */
    private fun initAmap(bundle: Bundle?) {
        mapView!!.onCreate(bundle)
        if (aMap == null) {
            aMap = mapView!!.map
        }
        aMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(CONST.guizhouLatLng, 5.5f))
        aMap!!.uiSettings.isMyLocationButtonEnabled = false // 设置默认定位按钮是否显示
        aMap!!.uiSettings.isZoomControlsEnabled = false
        aMap!!.setOnMarkerClickListener(this)
        aMap!!.setOnMapLoadedListener {
            tvMapNumber.text = aMap!!.mapContentApprovalNumber
            okHttpList()
            CommonUtil.drawHLJJson(this, aMap)
        }
    }

    /**
     * 获取雷达图片集信息
     */
    private fun okHttpList() {
        val url = "http://decision-admin.tianqi.cn/Home/extra/getHljRadarData"
        dataList.clear()
        val d = AgriDto()
        d.name = "东北"
        d.lat = 0.0
        d.lng = 0.0
        d.radarId = "JC_RADAR_DB_JB"
        dataList.add(d)
        Thread {
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    runOnUiThread {
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                val array = JSONArray(result)
                                for (i in 0 until array.length()) {
                                    val itemObj = array.getJSONObject(i)
                                    val dto = AgriDto()
                                    if (!itemObj.isNull("name")) {
                                        dto.name = itemObj.getString("name")
                                    }
                                    if (!itemObj.isNull("lat")) {
                                        dto.lat = java.lang.Double.valueOf(itemObj.getString("lat"))
                                    }
                                    if (!itemObj.isNull("lon")) {
                                        dto.lng = java.lang.Double.valueOf(itemObj.getString("lon"))
                                    }
                                    if (!itemObj.isNull("id")) {
                                        dto.radarId = itemObj.getString("id")
                                    }
                                    dataList.add(dto)
                                }
                                if (mAdapter != null) {
                                    mAdapter!!.notifyDataSetChanged()
                                }
                                addMarkerToMap()
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            })
        }.start()
    }

    private fun addMarkerToMap() {
        for (i in 1 until dataList.size) {
            val dto = dataList[i]
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.wind_dir_speed_marker_view, null)
            val ivWind = view.findViewById<View>(R.id.ivWind) as ImageView
            ivWind.setImageResource(R.drawable.iv_radar)
            val options = MarkerOptions()
            options.title(dto.name)
            options.snippet(dto.radarId)
            options.anchor(0.5f, 0.5f)
            options.position(LatLng(dto.lat, dto.lng))
            options.icon(BitmapDescriptorFactory.fromView(view))
            aMap!!.addMarker(options)
        }
    }

    private fun initGridView() {
        mAdapter = WeatherRadarAdapter(this, dataList)
        gridView!!.adapter = mAdapter
        gridView!!.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 ->
            val dto = dataList[arg2]
            val intent = Intent(this@WeatherRadarActivity, WeatherRadarDetailActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable("data", dto)
            intent.putExtras(bundle)
            startActivity(intent)
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val dto = AgriDto()
        dto.name = marker.title
        dto.radarId = marker.snippet
        val intent = Intent(this@WeatherRadarActivity, WeatherRadarDetailActivity::class.java)
        val bundle = Bundle()
        bundle.putParcelable("data", dto)
        intent.putExtras(bundle)
        startActivity(intent)
        return true
    }

    private fun translateAnimation(flag: Boolean, llGridView: View) {
        val animup = AnimationSet(true)
        val mytranslateanimup0 = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 1.0f)
        mytranslateanimup0.duration = 200
        val mytranslateanimup1 = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 1f,
                Animation.RELATIVE_TO_SELF, 0f)
        mytranslateanimup1.duration = 200
        mytranslateanimup1.startOffset = 200
        if (flag) {
            animup.addAnimation(mytranslateanimup0)
        } else {
            animup.addAnimation(mytranslateanimup1)
        }
        animup.fillAfter = true
        llGridView.startAnimation(animup)
        animup.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {}
            override fun onAnimationRepeat(arg0: Animation) {}
            override fun onAnimationEnd(arg0: Animation) {
                llGridView.clearAnimation()
                if (flag) {
                    ivExpand!!.setImageResource(R.drawable.iv_collose1)
                } else {
                    ivExpand!!.setImageResource(R.drawable.iv_expand1)
                    llGridView.visibility = View.VISIBLE
                }
            }
        })
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.llBack -> finish()
            R.id.ivExpand -> {
                if (gridView!!.visibility == View.VISIBLE) {
                    translateAnimation(true, gridView)
                    gridView!!.visibility = View.GONE
                    aMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(CONST.guizhouLatLng, 6.0f))
                } else {
                    translateAnimation(false, gridView)
                    aMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(CONST.guizhouLatLng, 5.5f))
                }
            }
        }
    }

}
