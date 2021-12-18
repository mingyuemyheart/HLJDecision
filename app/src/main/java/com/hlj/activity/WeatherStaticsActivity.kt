package com.hlj.activity

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ObjectAnimator
import android.graphics.Point
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.widget.TextView
import cn.com.weather.api.WeatherAPI
import cn.com.weather.listener.AsyncResponseHandler
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.AMap
import com.amap.api.maps.AMap.OnMapClickListener
import com.amap.api.maps.AMap.OnMarkerClickListener
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
import com.amap.api.maps.model.animation.ScaleAnimation
import com.hlj.common.CONST
import com.hlj.dto.WeatherStaticsDto
import com.hlj.utils.CommonUtil
import com.hlj.utils.OkHttpUtil
import com.hlj.utils.SecretUrlUtil
import com.hlj.view.CircularProgressBar
import kotlinx.android.synthetic.main.activity_weather_statics.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * 天气统计
 */
class WeatherStaticsActivity : BaseActivity(), OnClickListener, OnMarkerClickListener, OnMapClickListener, AMap.OnCameraChangeListener, AMapLocationListener {

    private var aMap: AMap? = null
    private val provinceList: MutableList<WeatherStaticsDto> = ArrayList() //省级
    private val cityList: MutableList<WeatherStaticsDto> = ArrayList() //省级
    private val districtList: MutableList<WeatherStaticsDto> = ArrayList() //省级
    private val proMarkers: MutableList<Marker> = ArrayList()
    private val cityMarkers: MutableList<Marker> = ArrayList()
    private val disMarkers: MutableList<Marker> = ArrayList()
    private var leftlatlng: LatLng? = null
    private var rightLatlng: LatLng? = null
    private var zoom = 5.5f
    private var mLocationOption: AMapLocationClientOption? = null //声明mLocationOption对象
    private var mLocationClient: AMapLocationClient? = null //声明AMapLocationClient类对象
    private var defaultLat = CONST.defaultLat
    private var defaultLng = CONST.defaultLng
    private var locationCityId = CONST.defaultCityId //默认为哈尔滨

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_statics)
        showDialog()
        initMap(savedInstanceState)
        initWidget()
    }

    /**
     * 初始化控件
     */
    private fun initWidget() {
        llBack!!.setOnClickListener(this)

        val title = intent.getStringExtra(CONST.ACTIVITY_NAME)
        if (title != null) {
            tvTitle!!.text = title
        }

        checkLocationAuthority(object : LocationCallback {
            override fun grantedLocation(isGranted: Boolean) {
                if (isGranted) {
                    startLocation()
                } else {
                    getCityId()
                }
            }
        })
    }

    /**
     * 开始定位
     */
    private fun startLocation() {
        mLocationOption = AMapLocationClientOption() //初始化定位参数
        mLocationClient = AMapLocationClient(this) //初始化定位
        mLocationOption!!.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption!!.isNeedAddress = true //设置是否返回地址信息（默认返回地址信息）
        mLocationOption!!.isOnceLocation = true //设置是否只定位一次,默认为false
        mLocationOption!!.isWifiActiveScan = true //设置是否强制刷新WIFI，默认为强制刷新
        mLocationOption!!.isMockEnable = false //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption!!.interval = 2000 //设置定位间隔,单位毫秒,默认为2000ms
        mLocationClient!!.setLocationOption(mLocationOption) //给定位客户端对象设置定位参数
        mLocationClient!!.setLocationListener(this)
        mLocationClient!!.startLocation() //启动定位
    }

    override fun onLocationChanged(amapLocation: AMapLocation?) {
        if (amapLocation != null && amapLocation.errorCode == AMapLocation.LOCATION_SUCCESS) {
            defaultLat = amapLocation.latitude
            defaultLng = amapLocation.longitude
            getCityId()
        }
    }

    private fun getCityId() {
        WeatherAPI.getGeo(this, defaultLng.toString(), defaultLat.toString(), object : AsyncResponseHandler() {
            override fun onComplete(content: JSONObject) {
                super.onComplete(content)
                if (!content.isNull("geo")) {
                    try {
                        val geo = content.getJSONObject("geo")
                        if (!geo.isNull("id")) {
                            locationCityId = geo.getString("id")
                            if (!locationCityId.startsWith("10105")) {
                                locationCityId = CONST.defaultCityId
                            }
                            okHttpStatistic()
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    /**
     * 初始化地图
     */
    private fun initMap(bundle: Bundle?) {
        mapView!!.onCreate(bundle)
        if (aMap == null) {
            aMap = mapView!!.map
        }
        aMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(CONST.guizhouLatLng, zoom))
        aMap!!.uiSettings.isZoomControlsEnabled = false
        aMap!!.uiSettings.isRotateGesturesEnabled = false
        aMap!!.setOnMarkerClickListener(this)
        aMap!!.setOnMapClickListener(this)
        aMap!!.setOnCameraChangeListener(this)
        aMap!!.setOnMapLoadedListener {
            tvMapNumber.text = aMap!!.mapContentApprovalNumber
            CommonUtil.drawHLJJson(this, aMap)
        }
    }

    override fun onCameraChange(arg0: CameraPosition?) {}

    override fun onCameraChangeFinish(arg0: CameraPosition) {
        val leftPoint = Point(0, CommonUtil.heightPixels(this))
        val rightPoint = Point(CommonUtil.widthPixels(this), 0)
        leftlatlng = aMap!!.projection.fromScreenLocation(leftPoint)
        rightLatlng = aMap!!.projection.fromScreenLocation(rightPoint)
        if (zoom == arg0.zoom) { //如果是地图缩放级别不变，并且点击就不做处理
            return
        }
        zoom = arg0.zoom
        removeMarkers()
        if (arg0.zoom <= 7.0f) {
            addMarker(provinceList, proMarkers)
            addMarker(cityList, cityMarkers)
        }
        if (arg0.zoom > 7.0f) {
            addMarker(provinceList, proMarkers)
            addMarker(cityList, cityMarkers)
            addMarker(districtList, disMarkers)
        }
    }

    /**
     * 获取天气统计数据
     */
    private fun okHttpStatistic() {
        OkHttpUtil.enqueue(Request.Builder().url(SecretUrlUtil.statistic()).build(), object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    return
                }
                val result = response.body!!.string()
                if (result != null) {
                    parseStationInfo(result, "level1", provinceList)
                    parseStationInfo(result, "level2", cityList)
                    parseStationInfo(result, "level3", districtList)
                    runOnUiThread {
                        cancelDialog()
                        addMarker(provinceList, proMarkers)
                        addMarker(cityList, cityMarkers)
                        clickMarker(locationCityId)
                    }
                }
            }
        })
    }

    /**
     * 解析数据
     */
    private fun parseStationInfo(result: String, level: String, list: MutableList<WeatherStaticsDto>) {
        list.clear()
        try {
            val obj = JSONObject(result)
            if (!obj.isNull(level)) {
                val array = JSONArray(obj.getString(level))
                for (i in 0 until array.length()) {
                    val dto = WeatherStaticsDto()
                    val itemObj = array.getJSONObject(i)
                    if (!itemObj.isNull("name")) {
                        dto.name = itemObj.getString("name")
                    }
                    if (!itemObj.isNull("stationid")) {
                        dto.stationId = itemObj.getString("stationid")
                    }
                    if (!itemObj.isNull("level")) {
                        dto.level = itemObj.getString("level")
                    }
                    if (!itemObj.isNull("areaid")) {
                        dto.areaId = itemObj.getString("areaid")
                    }
                    if (!itemObj.isNull("lat")) {
                        dto.latitude = itemObj.getString("lat")
                    }
                    if (!itemObj.isNull("lon")) {
                        dto.longitude = itemObj.getString("lon")
                    }
                    if (!TextUtils.isEmpty(dto.areaId) && dto.areaId.startsWith("10105")) {
                        list.add(dto)
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    /**
     * 给marker添加文字
     * @param name 城市名称
     * @return
     */
    private fun getTextBitmap(name: String): View? {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.layout_marker_statistic, null) ?: return null
        val tvName = view.findViewById<TextView>(R.id.tvName)
        tvName.text = name
        return view
    }

    private fun markerExpandAnimation(marker: Marker) {
        val animation = ScaleAnimation(0f, 1f, 0f, 1f)
        animation.setInterpolator(LinearInterpolator())
        animation.setDuration(300)
        marker.setAnimation(animation)
        marker.startAnimation()
    }

    private fun markerColloseAnimation(marker: Marker) {
        val animation = ScaleAnimation(1f, 0f, 1f, 0f)
        animation.setInterpolator(LinearInterpolator())
        animation.setDuration(300)
        marker.setAnimation(animation)
        marker.startAnimation()
    }

    private fun removeMarkers() {
        for (i in proMarkers.indices) {
            val marker = proMarkers[i]
            markerColloseAnimation(marker)
            marker.remove()
        }
        proMarkers.clear()
        for (i in cityMarkers.indices) {
            val marker = cityMarkers[i]
            markerColloseAnimation(marker)
            marker.remove()
        }
        cityMarkers.clear()
        for (i in disMarkers.indices) {
            val marker = disMarkers[i]
            markerColloseAnimation(marker)
            marker.remove()
        }
        disMarkers.clear()
    }

    /**
     * 添加marker
     */
    private fun addMarker(list: List<WeatherStaticsDto>, markers: MutableList<Marker>) {
        if (list.isEmpty()) {
            return
        }
        for (i in list.indices) {
            val dto = list[i]
            val lat = dto.latitude.toDouble()
            val lng = dto.longitude.toDouble()
            if (leftlatlng == null || rightLatlng == null) {
                val options = MarkerOptions()
                options.title(dto.areaId)
                options.anchor(0.5f, 0.5f)
                options.position(LatLng(lat, lng))
                options.icon(BitmapDescriptorFactory.fromView(getTextBitmap(dto.name)))
                val marker = aMap!!.addMarker(options)
                markers.add(marker)
                markerExpandAnimation(marker)
            } else {
                if (lat > leftlatlng!!.latitude && lat < rightLatlng!!.latitude && lng > leftlatlng!!.longitude && lng < rightLatlng!!.longitude) {
                    val options = MarkerOptions()
                    options.title(dto.areaId)
                    options.anchor(0.5f, 0.5f)
                    options.position(LatLng(lat, lng))
                    options.icon(BitmapDescriptorFactory.fromView(getTextBitmap(dto.name)))
                    val marker = aMap!!.addMarker(options)
                    markers.add(marker)
                    markerExpandAnimation(marker)
                }
            }
        }
    }

    override fun onMapClick(arg0: LatLng?) {
        if (clDetail!!.visibility == View.VISIBLE) {
            hideAnimation(clDetail)
        }
    }

    /**
     * 向上弹出动画
     * @param layout
     */
    private fun showAnimation(layout: View?) {
        val animation = TranslateAnimation(
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 1f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f)
        animation.duration = 300
        layout!!.startAnimation(animation)
        layout.visibility = View.VISIBLE
    }

    /**
     * 向下隐藏动画
     * @param layout
     */
    private fun hideAnimation(layout: View?) {
        val animation = TranslateAnimation(
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 1f)
        animation.duration = 300
        layout!!.startAnimation(animation)
        layout.visibility = View.GONE
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        clickMarker(marker.title)
        return true
    }

    private fun clickMarker(cityId: String) {
        showAnimation(clDetail)
        var name: String? = null
        var areaId: String? = null
        var stationId: String? = null
        for (i in provinceList.indices) {
            if (TextUtils.equals(cityId, provinceList[i].areaId)) {
                areaId = provinceList[i].areaId
                stationId = provinceList[i].stationId
                name = provinceList[i].name
                break
            }
        }
        for (i in cityList.indices) {
            if (TextUtils.equals(cityId, cityList[i].areaId)) {
                areaId = cityList[i].areaId
                stationId = cityList[i].stationId
                name = cityList[i].name
                break
            }
        }
        for (i in districtList.indices) {
            if (TextUtils.equals(cityId, districtList[i].areaId)) {
                areaId = districtList[i].areaId
                stationId = districtList[i].stationId
                name = districtList[i].name
                break
            }
        }
        tvName!!.text = "$name $stationId"
        tvDetail!!.text = ""
        progressBar!!.visibility = View.VISIBLE
        okHttpStatisticDetail(stationId!!)
    }

    private fun okHttpStatisticDetail(stationId: String) {
        OkHttpUtil.enqueue(Request.Builder().url(SecretUrlUtil.statisticDetail(stationId)).build(), object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    return
                }
                val result = response.body!!.string()
                runOnUiThread {
                    progressBar!!.visibility = View.INVISIBLE
                    if (result != null) {
                        try {
                            val obj = JSONObject(result)
                            val sdf = SimpleDateFormat("yyyyMMdd")
                            val sdf2 = SimpleDateFormat("yyyy-MM-dd")
                            try {
                                val startTime = sdf2.format(sdf.parse(obj.getString("starttime")))
                                val endTime = sdf2.format(sdf.parse(obj.getString("endtime")))
                                var no_rain_lx = obj.getInt("no_rain_lx").toString() + "" //连续没雨天数
                                no_rain_lx = if (TextUtils.equals(no_rain_lx, "-1")) {
                                    getString(R.string.no_statics)
                                } else {
                                    no_rain_lx + "天"
                                }
                                var mai_lx = obj.getInt("mai_lx").toString() + "" //连续霾天数
                                mai_lx = if (TextUtils.equals(mai_lx, "-1")) {
                                    getString(R.string.no_statics)
                                } else {
                                    mai_lx + "天"
                                }
                                var highTemp: String? = null //高温
                                var lowTemp: String? = null //低温
                                var highWind: String? = null //最大风速
                                var highRain: String? = null //最大降水量
                                if (!obj.isNull("count")) {
                                    val array = JSONArray(obj.getString("count"))
                                    val itemObj0 = array.getJSONObject(0) //温度
                                    val itemObj1 = array.getJSONObject(1) //降水
                                    val itemObj5 = array.getJSONObject(5) //风速
                                    if (!itemObj0.isNull("max") && !itemObj0.isNull("min")) {
                                        highTemp = itemObj0.getString("max")
                                        highTemp = if (TextUtils.equals(highTemp, "-1.0")) {
                                            getString(R.string.no_statics)
                                        } else {
                                            "$highTemp℃"
                                        }
                                        lowTemp = itemObj0.getString("min")
                                        lowTemp = if (TextUtils.equals(lowTemp, "-1.0")) {
                                            getString(R.string.no_statics)
                                        } else {
                                            "$lowTemp℃"
                                        }
                                    }
                                    if (!itemObj1.isNull("max")) {
                                        highRain = itemObj1.getString("max")
                                        highRain = if (TextUtils.equals(highRain, "-1.0")) {
                                            getString(R.string.no_statics)
                                        } else {
                                            highRain + "mm"
                                        }
                                    }
                                    if (!itemObj5.isNull("max")) {
                                        highWind = itemObj5.getString("max")
                                        highWind = if (TextUtils.equals(highWind, "-1.0")) {
                                            getString(R.string.no_statics)
                                        } else {
                                            highWind + "m/s"
                                        }
                                    }
                                }
                                if (startTime != null && endTime != null && highTemp != null && lowTemp != null && highWind != null && highRain != null) {
                                    val buffer = StringBuffer()
                                    buffer.append(getString(R.string.from)).append(startTime)
                                    buffer.append(getString(R.string.to)).append(endTime)
                                    buffer.append("：\n")
                                    buffer.append(getString(R.string.highest_temp)).append(highTemp).append("，")
                                    buffer.append(getString(R.string.lowest_temp)).append(lowTemp).append("，")
                                    buffer.append(getString(R.string.max_speed)).append(highWind).append("，")
                                    buffer.append(getString(R.string.max_fall)).append(highRain).append("，")
                                    buffer.append(getString(R.string.lx_no_fall)).append(no_rain_lx).append("，")
                                    buffer.append(getString(R.string.lx_no_mai)).append(mai_lx).append("。")
                                    val builder = SpannableStringBuilder(buffer.toString())
                                    val builderSpan1 = ForegroundColorSpan(resources.getColor(R.color.builder))
                                    val builderSpan2 = ForegroundColorSpan(resources.getColor(R.color.builder))
                                    val builderSpan3 = ForegroundColorSpan(resources.getColor(R.color.builder))
                                    val builderSpan4 = ForegroundColorSpan(resources.getColor(R.color.builder))
                                    val builderSpan5 = ForegroundColorSpan(resources.getColor(R.color.builder))
                                    val builderSpan6 = ForegroundColorSpan(resources.getColor(R.color.builder))
                                    builder.setSpan(builderSpan1, 29, 29 + highTemp.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    builder.setSpan(builderSpan2, 29 + highTemp.length + 6, 29 + highTemp.length + 6 + lowTemp.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                                    builder.setSpan(builderSpan3, 29 + highTemp.length + 6 + lowTemp.length + 6, 29 + highTemp.length + 6 + lowTemp.length + 6 + highWind.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    builder.setSpan(builderSpan4, 29 + highTemp.length + 6 + lowTemp.length + 6 + highWind.length + 7, 29 + highTemp.length + 6 + lowTemp.length + 6 + highWind.length + 7 + highRain.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    builder.setSpan(builderSpan5, 29 + highTemp.length + 6 + lowTemp.length + 6 + highWind.length + 7 + highRain.length + 8, 29 + highTemp.length + 6 + lowTemp.length + 6 + highWind.length + 7 + highRain.length + 8 + no_rain_lx.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    builder.setSpan(builderSpan6, 29 + highTemp.length + 6 + lowTemp.length + 6 + highWind.length + 7 + highRain.length + 8 + no_rain_lx.length + 6, 29 + highTemp.length + 6 + lowTemp.length + 6 + highWind.length + 7 + highRain.length + 8 + no_rain_lx.length + 6 + mai_lx.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    tvDetail!!.text = builder
                                    val start = sdf2.parse(startTime).time
                                    val end = sdf2.parse(endTime).time
                                    val dayCount = ((end - start) / (1000 * 60 * 60 * 24)).toFloat() + 1
                                    if (!obj.isNull("tqxxcount")) {
                                        val array = JSONArray(obj.getString("tqxxcount"))
                                        for (i in 0 until array.length()) {
                                            val itemObj = array.getJSONObject(i)
                                            val name = itemObj.getString("name")
                                            val value = itemObj.getInt("value")
                                            if (i == 0) {
                                                if (value == -1) {
                                                    tvBar1!!.text = "$name\n--"
                                                    animate(bar1, null, 0f, 1000)
                                                    bar1!!.progress = 0f
                                                } else {
                                                    tvBar1!!.text = "$name\n${value}天"
                                                    animate(bar1, null, -value / dayCount, 1000)
                                                    bar1!!.progress = -value / dayCount
                                                }
                                            } else if (i == 1) {
                                                if (value == -1) {
                                                    tvBar2!!.text = "$name\n--"
                                                    animate(bar2, null, 0f, 1000)
                                                    bar2!!.progress = 0f
                                                } else {
                                                    tvBar2!!.text = "$name\n${value}天"
                                                    animate(bar2, null, -value / dayCount, 1000)
                                                    bar2!!.progress = -value / dayCount
                                                }
                                            } else if (i == 2) {
                                                if (value == -1) {
                                                    tvBar3!!.text = "$name\n--"
                                                    animate(bar3, null, 0f, 1000)
                                                    bar3!!.progress = 0f
                                                } else {
                                                    tvBar3!!.text = "$name\n${value}天"
                                                    animate(bar3, null, -value / dayCount, 1000)
                                                    bar3!!.progress = -value / dayCount
                                                }
                                            } else if (i == 3) {
                                                if (value == -1) {
                                                    tvBar4!!.text = "$name\n--"
                                                    animate(bar4, null, 0f, 1000)
                                                    bar4!!.progress = 0f
                                                } else {
                                                    tvBar4!!.text = "$name\n${value}天"
                                                    animate(bar4, null, -value / dayCount, 1000)
                                                    bar4!!.progress = -value / dayCount
                                                }
                                            } else if (i == 4) {
                                                if (value == -1) {
                                                    tvBar5!!.text = "$name\n--"
                                                    animate(bar5, null, 0f, 1000)
                                                    bar5!!.progress = 0f
                                                } else {
                                                    tvBar5!!.text = "$name\n${value}天"
                                                    animate(bar5, null, -value / dayCount, 1000)
                                                    bar5!!.progress = -value / dayCount
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (e: ParseException) {
                                e.printStackTrace()
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        })
    }

    /**
     * 进度条动画
     * @param progressBar
     * @param listener
     * @param progress
     * @param duration
     */
    private fun animate(progressBar: CircularProgressBar?, listener: AnimatorListener?, progress: Float, duration: Int) {
        val mProgressBarAnimator = ObjectAnimator.ofFloat(progressBar, "progress", progress)
        mProgressBarAnimator.duration = duration.toLong()
        mProgressBarAnimator.addListener(object : AnimatorListener {
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                progressBar!!.progress = progress
            }

            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationStart(animation: Animator) {}
        })
        if (listener != null) {
            mProgressBarAnimator.addListener(listener)
        }
        mProgressBarAnimator.reverse()
        mProgressBarAnimator.addUpdateListener { animation -> progressBar!!.progress = (animation.animatedValue as Float) }
        //		progressBar.setMarkerProgress(0f);
        mProgressBarAnimator.start()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (clDetail!!.visibility == View.VISIBLE) {
                hideAnimation(clDetail)
                return false
            } else {
                finish()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.llBack -> if (clDetail!!.visibility == View.VISIBLE) {
                hideAnimation(clDetail)
            } else {
                finish()
            }
        }
    }

    /**
     * 方法必须重写
     */
    override fun onResume() {
        super.onResume()
        if (mapView != null) {
            mapView!!.onResume()
        }
    }

    /**
     * 方法必须重写
     */
    override fun onPause() {
        super.onPause()
        if (mapView != null) {
            mapView!!.onPause()
        }
    }

    /**
     * 方法必须重写
     */
    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (mapView != null) {
            mapView!!.onSaveInstanceState(outState)
        }
    }

    /**
     * 方法必须重写
     */
    override fun onDestroy() {
        super.onDestroy()
        if (mapView != null) {
            mapView!!.onDestroy()
        }
    }

}
