package com.hlj.activity

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.widget.*
import cn.com.weather.api.WeatherAPI
import cn.com.weather.beans.Weather
import cn.com.weather.constants.Constants
import cn.com.weather.listener.AsyncResponseHandler
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.AMap
import com.amap.api.maps.AMap.*
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
import com.amap.api.maps.model.animation.ScaleAnimation
import com.hlj.common.CONST
import com.hlj.dto.AirPolutionDto
import com.hlj.dto.AqiDto
import com.hlj.manager.XiangJiManager
import com.hlj.utils.CommonUtil
import com.hlj.utils.OkHttpUtil
import com.hlj.utils.SecretUrlUtil
import com.hlj.view.AqiQualityView
import kotlinx.android.synthetic.main.activity_air.*
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

/**
 * 空气质量
 * @author shawn_sun
 */
class AirActivity : BaseActivity(), OnClickListener, OnMarkerClickListener, OnMapClickListener, OnCameraChangeListener, AMapLocationListener {

    private var defaultLat = CONST.defaultLat
    private var defaultLng = CONST.defaultLng
    private var locationCityId = CONST.defaultCityId //默认为哈尔滨

    private var aMap: AMap? = null
    private val provinceList: MutableList<AirPolutionDto> = ArrayList() //省级
    private val cityList: MutableList<AirPolutionDto> = ArrayList() //市级
    private val districtList: MutableList<AirPolutionDto> = ArrayList() //县级

    private var zoom = 5.5f
    private var isClick = false //判断是否点击

    private val sdf1 = SimpleDateFormat("yyyyMMddHHmm", Locale.CHINA)
    private val sdf2 = SimpleDateFormat("HH:mm", Locale.CHINA)
    private val sdf3 = SimpleDateFormat("yyyyMMddHH", Locale.CHINA)
    private val aqiList: MutableList<AqiDto> = ArrayList()
    private val factAqiList: MutableList<AqiDto> = ArrayList() //实况aqi数据
    private val foreAqiList: MutableList<AqiDto> = ArrayList() //预报aqi数据

    private var maxAqi = 0
    private var minAqi = 0
    private var aqiDate: String? = null
    private var configuration: Configuration? = null
    private val markerList: MutableList<Marker> = ArrayList()
    private var leftlatlng: LatLng? = null
    private var rightLatlng: LatLng? = null
    private var mLocationOption: AMapLocationClientOption? = null //声明mLocationOption对象
    private var mLocationClient: AMapLocationClient? = null //声明AMapLocationClient类对象

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_air)
        showDialog()
        initMap(savedInstanceState)
        initWidget()
    }

    /**
     * 初始化控件
     */
    private fun initWidget() {
        llBack!!.setOnClickListener(this)
        ivExpand!!.setOnClickListener(this)
        val title = intent.getStringExtra(CONST.ACTIVITY_NAME)
        if (title != null) {
            tvTitle!!.text = title
        }
        configuration = resources.configuration

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
                            okHttpAirRank()
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
        if (zoom == arg0.zoom && isClick) { //如果是地图缩放级别不变，并且点击就不做处理
            isClick = false
            return
        }
        zoom = arg0.zoom
        removeMarkers()
        if (arg0.zoom <= 7.0f) {
            addMarker(provinceList)
            addMarker(cityList)
        }
        if (arg0.zoom > 7.0f) {
            addMarker(provinceList)
            addMarker(cityList)
            addMarker(districtList)
        }
    }

    /**
     * 获取空气质量排行
     */
    private fun okHttpAirRank() {
        Thread {
            OkHttpUtil.enqueue(Request.Builder().url(SecretUrlUtil.airpollution()).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    if (!TextUtils.isEmpty(result)) {
                        parseStationInfo(result, "level1", provinceList)
                        parseStationInfo(result, "level2", cityList)
                        parseStationInfo(result, "level3", districtList)
                        runOnUiThread {
                            cancelDialog()
                            addMarker(provinceList)
                            addMarker(cityList)
                            clickMarker(locationCityId, defaultLat, defaultLng)
                        }
                    }
                }
            })
        }.start()
    }

    /**
     * 解析数据
     */
    private fun parseStationInfo(result: String, level: String, list: MutableList<AirPolutionDto>) {
        list.clear()
        try {
            val obj = JSONObject(result)
            if (!obj.isNull("data")) {
                val time = obj.getString("time")
                val dataObj = obj.getJSONObject("data")
                if (!dataObj.isNull(level)) {
                    val array = dataObj.getJSONArray(level)
                    for (i in 0 until array.length()) {
                        val dto = AirPolutionDto()
                        val itemObj = array.getJSONObject(i)
                        if (!itemObj.isNull("name")) {
                            dto.name = itemObj.getString("name")
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
                        if (!itemObj.isNull("aqi")) {
                            dto.aqi = itemObj.getString("aqi")
                        }
                        if (!itemObj.isNull("pm10")) {
                            dto.pm10 = itemObj.getString("pm10")
                        }
                        if (!itemObj.isNull("pm2_5")) {
                            dto.pm2_5 = itemObj.getString("pm2_5")
                        }
                        if (!itemObj.isNull("rank")) {
                            dto.rank = itemObj.getInt("rank")
                        }
                        dto.time = time
                        if (dto.areaId.startsWith("10105")) {
                            list.add(dto)
                        }
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
    private fun getTextBitmap(cityName: String, aqi: String): View? {
        var name = cityName
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.airpolution_item, null) ?: return null
        val tvName = view.findViewById<View>(R.id.tvName) as TextView
        val icon = view.findViewById<View>(R.id.icon) as ImageView
        if (!TextUtils.isEmpty(name) && name.length > 2) {
            name = """
                    ${name.substring(0, 2)}
                    ${name.substring(2, name.length)}
                    """.trimIndent()
        }
        tvName.text = name
        val value = Integer.valueOf(aqi)
        icon.setImageResource(getMarker(value))
        return view
    }

    /**
     * 根据aqi数据获取相对应的marker图标
     * @param value
     * @return
     */
    private fun getMarker(value: Int): Int {
        var drawable = -1
        if (value >= 0 && value <= 50) {
            drawable = R.drawable.iv_air1
        } else if (value >= 51 && value < 100) {
            drawable = R.drawable.iv_air2
        } else if (value >= 101 && value < 150) {
            drawable = R.drawable.iv_air3
        } else if (value >= 151 && value < 200) {
            drawable = R.drawable.iv_air4
        } else if (value >= 201 && value < 300) {
            drawable = R.drawable.iv_air5
        } else if (value >= 301) {
            drawable = R.drawable.iv_air6
        }
        return drawable
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
        for (i in markerList.indices) {
            val marker = markerList[i]
            markerColloseAnimation(marker)
            marker.remove()
        }
        markerList.clear()
    }

    /**
     * 添加marker
     */
    private fun addMarker(list: List<AirPolutionDto>) {
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
                options.icon(BitmapDescriptorFactory.fromView(getTextBitmap(dto.name, dto.aqi)))
                val marker = aMap!!.addMarker(options)
                markerList.add(marker)
                markerExpandAnimation(marker)
            } else {
                if (lat > leftlatlng!!.latitude && lat < rightLatlng!!.latitude && lng > leftlatlng!!.longitude && lng < rightLatlng!!.longitude) {
                    val options = MarkerOptions()
                    options.title(dto.areaId)
                    options.anchor(0.5f, 0.5f)
                    options.position(LatLng(lat, lng))
                    options.icon(BitmapDescriptorFactory.fromView(getTextBitmap(dto.name, dto.aqi)))
                    val marker = aMap!!.addMarker(options)
                    markerList.add(marker)
                    markerExpandAnimation(marker)
                }
            }
        }
    }

    override fun onMapClick(arg0: LatLng?) {
        hideAnimation(llContent)
    }

    /**
     * 根据aqi值获取aqi的提示信息
     * @param value
     * @return
     */
    private fun getPrompt(value: Int): String? {
        var aqi: String? = null
        if (value >= 0 && value <= 50) {
            aqi = getString(R.string.aqi1_text)
        } else if (value >= 51 && value < 100) {
            aqi = getString(R.string.aqi2_text)
        } else if (value >= 101 && value < 150) {
            aqi = getString(R.string.aqi3_text)
        } else if (value >= 151 && value < 200) {
            aqi = getString(R.string.aqi4_text)
        } else if (value >= 201 && value < 300) {
            aqi = getString(R.string.aqi5_text)
        } else if (value >= 301) {
            aqi = getString(R.string.aqi6_text)
        }
        return aqi
    }

    /**
     * 根据aqi数据获取相对应的背景图标
     * @param value
     * @return
     */
    private fun getCicleBackground(value: Int): Int {
        var drawable = -1
        if (value >= 0 && value <= 50) {
            drawable = R.drawable.circle_aqi_one
        } else if (value >= 51 && value < 100) {
            drawable = R.drawable.circle_aqi_two
        } else if (value >= 101 && value < 150) {
            drawable = R.drawable.circle_aqi_three
        } else if (value >= 151 && value < 200) {
            drawable = R.drawable.circle_aqi_four
        } else if (value >= 201 && value < 300) {
            drawable = R.drawable.circle_aqi_five
        } else if (value >= 301) {
            drawable = R.drawable.circle_aqi_six
        }
        return drawable
    }

    private fun setValue(areaId: String, list: List<AirPolutionDto>) {
        for (i in list.indices) {
            val dto = list[i]
            if (TextUtils.equals(areaId, dto.areaId)) {
                tvName!!.text = dto.name
                tvCity!!.text = dto.name + "空气质量指数（AQI）\n实况（过去24小时）和预报（未来24小时）"
                tvAqiCount!!.text = dto.aqi
                val value = dto.aqi.toInt()
                tvAqi!!.text = CommonUtil.getAqiDes(this, value)
                tvAqi!!.setBackgroundResource(CommonUtil.getCornerBackground(value))
                if (value > 150) {
                    tvAqi!!.setTextColor(Color.WHITE)
                } else {
                    tvAqi!!.setTextColor(Color.BLACK)
                }
                tvPrompt!!.text = "温馨提示：" + getPrompt(value)
                tvRank!!.setBackgroundResource(getCicleBackground(value))
                tvRank!!.text = dto.rank.toString()
                clPm25!!.setBackgroundResource(getCicleBackground(value))
                tvPm2_5!!.text = dto.pm2_5
                clPm10!!.setBackgroundResource(getCicleBackground(value))
                tvPm10!!.text = dto.pm10
                if (!TextUtils.isEmpty(dto.time)) {
                    try {
                        tvTime!!.text = sdf2.format(sdf1.parse(dto.time)) + getString(R.string.update)
                    } catch (e: ParseException) {
                        e.printStackTrace()
                    }
                }
                break
            }
        }
    }

    /**
     * 向上弹出动画
     * @param layout
     */
    private fun showAnimation(layout: View?) {
        if (layout!!.visibility == View.VISIBLE) {
            return
        }
        val animation = TranslateAnimation(
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 1f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f)
        animation.duration = 300
        layout.startAnimation(animation)
        layout.visibility = View.VISIBLE
    }

    /**
     * 向下隐藏动画
     * @param layout
     */
    private fun hideAnimation(layout: View?) {
        if (layout!!.visibility == View.GONE) {
            return
        }
        val animation = TranslateAnimation(
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 1f)
        animation.duration = 300
        layout.startAnimation(animation)
        layout.visibility = View.GONE
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        clickMarker(marker.title, marker.position.latitude, marker.position.longitude)
        return true
    }

    private fun clickMarker(cityId: String, lat: Double, lng: Double) {
        showAnimation(llContent)
        isClick = true
        setValue(cityId, provinceList)
        setValue(cityId, cityList)
        setValue(cityId, districtList)
        llContainer!!.removeAllViews()
        okHttpXiangJiAqi(lat, lng)
    }

    /**
     * 请求象辑aqi
     */
    private fun okHttpXiangJiAqi(lat: Double, lng: Double) {
        Thread {
            val timestamp = Date().time
            val start1 = sdf3.format(timestamp)
            val end1 = sdf3.format(timestamp + 1000 * 60 * 60 * 24)
            val url = XiangJiManager.getXJSecretUrl(lng, lat, start1, end1, timestamp)
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    if (!TextUtils.isEmpty(result)) {
                        try {
                            val obj = JSONObject(result)
                            if (!obj.isNull("reqTime")) {
                                aqiDate = obj.getString("reqTime")
                            }
                            if (!obj.isNull("series")) {
                                aqiList.clear()
                                val array = obj.getJSONArray("series")
                                foreAqiList.clear()
                                for (i in 0 until array.length()) {
                                    val data = AqiDto()
                                    data.aqi = array[i].toString()
                                    foreAqiList.add(data)
                                }
                                aqiList.addAll(factAqiList)
                                aqiList.addAll(foreAqiList)
                            }
                            if (aqiList.size > 0) {
                                try {
                                    if (!TextUtils.isEmpty(aqiList[0].aqi)) {
                                        maxAqi = Integer.valueOf(aqiList[0].aqi)
                                        minAqi = Integer.valueOf(aqiList[0].aqi)
                                        for (i in aqiList.indices) {
                                            if (!TextUtils.isEmpty(aqiList[i].aqi)) {
                                                if (maxAqi <= Integer.valueOf(aqiList[i].aqi)) {
                                                    maxAqi = Integer.valueOf(aqiList[i].aqi)
                                                }
                                                if (minAqi >= Integer.valueOf(aqiList[i].aqi)) {
                                                    minAqi = Integer.valueOf(aqiList[i].aqi)
                                                }
                                            }
                                        }
                                        maxAqi += (50 - maxAqi % 50)
                                        runOnUiThread {
                                            if (configuration!!.orientation == Configuration.ORIENTATION_PORTRAIT) {
                                                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                                showPortrait()
                                                ivExpand!!.setImageResource(R.drawable.iv_expand_black)
                                            } else {
                                                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                                showLandscape()
                                                ivExpand!!.setImageResource(R.drawable.iv_collose_black)
                                            }
                                        }
                                    }
                                } catch (e: ArrayIndexOutOfBoundsException) {
                                    e.printStackTrace()
                                }
                            }
                        } catch (e1: JSONException) {
                            e1.printStackTrace()
                        }
                    }
                }
            })
        }.start()
    }

    private fun showPortrait() {
        hScrollView!!.visibility = View.VISIBLE
        ivExpand!!.visibility = View.VISIBLE
        clAqi.visibility = View.VISIBLE
        tvCity.visibility = View.GONE
        val aqiView = AqiQualityView(this)
        aqiView.setData(aqiList, aqiDate)
        val viewHeight = CommonUtil.dip2px(this, 180f).toInt()
        //		if (maxAqi <= 100) {
//			viewHeight = (int)(CommonUtil.dip2px(mContext, 150));
//		}else if (maxAqi > 100 && maxAqi <= 150) {
//			viewHeight = (int)(CommonUtil.dip2px(mContext, 200));
//		}else if (maxAqi > 150) {
//			viewHeight = (int)(CommonUtil.dip2px(mContext, 250));
//		}
        llContainer!!.removeAllViews()
        llContainer!!.addView(aqiView, CommonUtil.widthPixels(this)*4-CommonUtil.dip2px(this, 30f).toInt(), viewHeight)
        Handler().post { hScrollView!!.scrollTo(CommonUtil.widthPixels(this)*2, hScrollView!!.height) }
    }

    private fun showLandscape() {
        hScrollView!!.visibility = View.VISIBLE
        ivExpand!!.visibility = View.VISIBLE
        clAqi.visibility = View.GONE
        tvCity.visibility = View.VISIBLE
        val aqiView = AqiQualityView(this)
        aqiView.setData(aqiList, aqiDate)
        llContainer!!.removeAllViews()
        llContainer!!.addView(aqiView, CommonUtil.widthPixels(this)*2-CommonUtil.dip2px(this, 30f).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
        Handler().post { hScrollView!!.scrollTo(CommonUtil.widthPixels(this), hScrollView!!.height) }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (configuration == null) {
                return false
            }
            if (configuration!!.orientation == Configuration.ORIENTATION_PORTRAIT) {
                finish()
            } else {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                return false
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.llBack -> {
                if (configuration == null) {
                    return
                }
                if (configuration!!.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    finish()
                } else {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
            R.id.ivExpand -> {
                if (configuration == null) {
                    return
                }
                requestedOrientation = if (configuration!!.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        }
    }

    /**
     * 横竖屏切换监听
     */
    override fun onConfigurationChanged(config: Configuration) {
        super.onConfigurationChanged(config)
        this.configuration = config
        when (config.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                showLandscape()
                ivExpand!!.setImageResource(R.drawable.iv_collose_black)
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                showPortrait()
                ivExpand!!.setImageResource(R.drawable.iv_expand_black)
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
