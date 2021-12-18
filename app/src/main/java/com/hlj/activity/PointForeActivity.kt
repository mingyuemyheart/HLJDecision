package com.hlj.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.media.ThumbnailUtils
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
import com.hlj.common.CONST
import com.hlj.dto.PointForeDto
import com.hlj.utils.CommonUtil
import com.hlj.utils.OkHttpUtil
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_point_fore.*
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
import kotlin.collections.ArrayList

/**
 * 格点预报
 */
class PointForeActivity : BaseActivity(), OnClickListener, AMapLocationListener, AMap.OnCameraChangeListener, AMap.OnMapClickListener {

    private var aMap: AMap? = null
    private var zoom = 5.5f
    private val sdf1 = SimpleDateFormat("dd日HH时", Locale.CHINA)
    private val sdf2 = SimpleDateFormat("yyyyMMddHH", Locale.CHINA)
    private var dataType = 1 //1温度、2湿度、3风速、4能见度、5云量、6降水
    private var locationLat = CONST.defaultLat
    private var locationLng = CONST.defaultLng
    private var mapType = AMap.MAP_TYPE_NORMAL
    private var start: LatLng? = null
    private var end: LatLng? = null
    private var savedInstanceState: Bundle? = null
    private var mRadarThread: RadarThread? = null
    private var locationMarker: Marker? = null
    private var currentIndex = 0
    private val tems: ArrayList<PointForeDto> = ArrayList()
    private val humiditys: ArrayList<PointForeDto> = ArrayList()
    private val winds: ArrayList<PointForeDto> = ArrayList()
    private val clouds: ArrayList<PointForeDto> = ArrayList()
    private val rains: ArrayList<PointForeDto> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_point_fore)
        this.savedInstanceState = savedInstanceState
        showDialog()
        initMap()
        initWidget()
    }

    /**
     * 初始化控件
     */
    private fun initWidget() {
        llBack.setOnClickListener(this)
        ivTemp.setOnClickListener(this)
        ivHumidity.setOnClickListener(this)
        ivWind.setOnClickListener(this)
        ivCloud.setOnClickListener(this)
        ivRain.setOnClickListener(this)
        tvTemp.setOnClickListener(this)
        tvHumidity.setOnClickListener(this)
        tvWind.setOnClickListener(this)
        tvCloud.setOnClickListener(this)
        tvRain.setOnClickListener(this)
        ivLocation.setOnClickListener(this)
        ivSwitch.setOnClickListener(this)
        tvDataSource.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        tvDataSource.setOnClickListener(this)
        ivDataSource.setOnClickListener(this)
        ivPlay.setOnClickListener(this)
        seekBar.setOnSeekBarChangeListener(seekbarListener)
        ivLegendPrompt.setOnClickListener(this)

        val title = intent.getStringExtra(CONST.ACTIVITY_NAME)
        if (title != null) {
            tvTitle.text = title
        }

        checkLocationAuthority(object : LocationCallback {
            override fun grantedLocation(isGranted: Boolean) {
                if (isGranted) {
                    startLocation()
                } else {
                    locationComplete()
                }
            }

        })
        okHttpList()
    }

    /**
     * 开始定位
     */
    private fun startLocation() {
        val mLocationOption = AMapLocationClientOption() //初始化定位参数
        val mLocationClient = AMapLocationClient(this) //初始化定位
        mLocationOption.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.isNeedAddress = true //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.isOnceLocation = true //设置是否只定位一次,默认为false
        mLocationOption.isMockEnable = false //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.interval = 2000 //设置定位间隔,单位毫秒,默认为2000ms
        mLocationClient.setLocationOption(mLocationOption) //给定位客户端对象设置定位参数
        mLocationClient.setLocationListener(this)
        mLocationClient.startLocation() //启动定位
    }

    override fun onLocationChanged(amapLocation: AMapLocation?) {
        if (amapLocation != null && amapLocation.errorCode == AMapLocation.LOCATION_SUCCESS) {
            locationLat = amapLocation.latitude
            locationLng = amapLocation.longitude
            locationComplete()
        }
    }

    private fun locationComplete() {
        if (locationMarker != null) {
            locationMarker!!.remove()
        }
        val latLng = LatLng(locationLat, locationLng)
        val options = MarkerOptions()
        options.anchor(0.5f, 0.5f)
        val bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(resources, R.drawable.icon_map_location),
                CommonUtil.dip2px(this, 16f).toInt(), CommonUtil.dip2px(this, 24f).toInt())
        if (bitmap != null) {
            options.icon(BitmapDescriptorFactory.fromBitmap(bitmap))
        } else {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_map_location))
        }
        options.position(latLng)
        locationMarker = aMap!!.addMarker(options)
        locationMarker!!.isClickable = false
    }

    override fun onCameraChange(arg0: CameraPosition?) {}

    override fun onCameraChangeFinish(arg0: CameraPosition) {
        Log.e("zoom", arg0.zoom.toString() + "")
        zoom = arg0.zoom
    }

    override fun onMapClick(latLng: LatLng) {
        locationLat = latLng.latitude
        locationLng = latLng.longitude
        locationComplete()
        val intent = Intent(this, ShawnPointForeDetailActivity::class.java)
        intent.putExtra("lat", latLng.latitude)
        intent.putExtra("lng", latLng.longitude)
        startActivity(intent)
    }

    private val seekbarListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
        override fun onStopTrackingTouch(arg0: SeekBar) {
            if (mRadarThread != null) {
                mRadarThread!!.setCurrent(seekBar!!.progress)
                mRadarThread!!.stopTracking()
            }
        }

        override fun onStartTrackingTouch(arg0: SeekBar) {
            if (mRadarThread != null) {
                mRadarThread!!.startTracking()
            }
        }

        override fun onProgressChanged(arg0: SeekBar, arg1: Int, arg2: Boolean) {}
    }

    /**
     * 初始化地图
     */
    private fun initMap() {
        mapView.onCreate(savedInstanceState)
        if (aMap == null) {
            aMap = mapView.map
        }
        aMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(46.102915, 128.121040), zoom))
        aMap!!.uiSettings.isZoomControlsEnabled = false
        aMap!!.uiSettings.isRotateGesturesEnabled = false
        aMap!!.setOnCameraChangeListener(this)
        aMap!!.setOnMapClickListener(this)
        tvMapNumber.text = aMap!!.mapContentApprovalNumber
        CommonUtil.drawHLJJson(this, aMap)
    }

    private fun okHttpList() {
        val url = "http://decision-admin.tianqi.cn/Home/extra/decision_gdsk_yb_images"
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
                        cancelDialog()
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                val obj = JSONObject(result)
                                start = LatLng(obj.getDouble("minlat"), obj.getDouble("minlon"))
                                end = LatLng(obj.getDouble("maxlat"), obj.getDouble("maxlon"))
                                if (!obj.isNull("tem")) {
                                    val tem = obj.getJSONObject("tem")
                                    tems.clear()
                                    val tuliurl = tem.getString("tuliurl")
                                    val array = tem.getJSONArray("list")
                                    val cHour = sdf2.format(Date())
                                    for (i in 0 until array.length()) {
                                        val dto = PointForeDto()
                                        val itemObj = array.getJSONObject(i)
                                        dto.legendUrl = tuliurl
                                        dto.imgUrl = itemObj.getString("imgurl")
                                        dto.time = itemObj.getString("time")
                                        tems.add(dto)
                                        if (TextUtils.equals(cHour, dto.time)) {
                                            currentIndex = i
                                        }
                                    }
                                }
                                if (!obj.isNull("humidity")) {
                                    val humidity = obj.getJSONObject("humidity")
                                    humiditys.clear()
                                    val tuliurl = humidity.getString("tuliurl")
                                    val array = humidity.getJSONArray("list")
                                    for (i in 0 until array.length()) {
                                        val dto = PointForeDto()
                                        val itemObj = array.getJSONObject(i)
                                        dto.legendUrl = tuliurl
                                        dto.imgUrl = itemObj.getString("imgurl")
                                        dto.time = itemObj.getString("time")
                                        humiditys.add(dto)
                                    }
                                }
                                if (!obj.isNull("wind")) {
                                    val wind = obj.getJSONObject("wind")
                                    winds.clear()
                                    val tuliurl = wind.getString("tuliurl")
                                    val array = wind.getJSONArray("list")
                                    for (i in 0 until array.length()) {
                                        val dto = PointForeDto()
                                        val itemObj = array.getJSONObject(i)
                                        dto.legendUrl = tuliurl
                                        dto.imgUrl = itemObj.getString("imgurl")
                                        dto.time = itemObj.getString("time")
                                        winds.add(dto)
                                    }
                                }
                                if (!obj.isNull("cloud")) {
                                    val cloud = obj.getJSONObject("cloud")
                                    clouds.clear()
                                    val tuliurl = cloud.getString("tuliurl")
                                    val array = cloud.getJSONArray("list")
                                    for (i in 0 until array.length()) {
                                        val dto = PointForeDto()
                                        val itemObj = array.getJSONObject(i)
                                        dto.legendUrl = tuliurl
                                        dto.imgUrl = itemObj.getString("imgurl")
                                        dto.time = itemObj.getString("time")
                                        clouds.add(dto)
                                    }
                                }
                                if (!obj.isNull("rain")) {
                                    val rain = obj.getJSONObject("rain")
                                    rains.clear()
                                    val tuliurl = rain.getString("tuliurl")
                                    val array = rain.getJSONArray("list")
                                    for (i in 0 until array.length()) {
                                        val dto = PointForeDto()
                                        val itemObj = array.getJSONObject(i)
                                        dto.legendUrl = tuliurl
                                        dto.imgUrl = itemObj.getString("imgurl")
                                        dto.time = itemObj.getString("time")
                                        rains.add(dto)
                                    }
                                }
                                switchElement()
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            })
        }.start()
    }

    /**
     * 切换要素
     */
    private fun switchElement() {
        if (mRadarThread != null) {
            mRadarThread!!.cancel()
            mRadarThread = null
        }
        mRadarThread = RadarThread()
        var data: PointForeDto? = null
        try {
            when (dataType) {
                1 -> {
                    mRadarThread!!.setDataList(tems)
                    data = tems[currentIndex]
                    changeProgress(data.time, currentIndex, tems.size - 1)
                }
                2 -> {
                    mRadarThread!!.setDataList(humiditys)
                    data = humiditys[currentIndex]
                    changeProgress(data.time, currentIndex, humiditys.size - 1)
                }
                3 -> {
                    mRadarThread!!.setDataList(winds)
                    data = winds[currentIndex]
                    changeProgress(data.time, currentIndex, winds.size - 1)
                }
                5 -> {
                    mRadarThread!!.setDataList(clouds)
                    data = clouds[currentIndex]
                    changeProgress(data.time, currentIndex, clouds.size - 1)
                }
                6 -> {
                    mRadarThread!!.setDataList(rains)
                    data = rains[currentIndex]
                    changeProgress(data.time, currentIndex, rains.size - 1)
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
        if (data != null) {
            okHttpBitmap(data.imgUrl)
            Picasso.get().load(data.legendUrl).into(ivLegend)
            mRadarThread!!.start()
        }
    }

    private fun okHttpBitmap(url: String) {
        Thread {
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val bytes = response.body!!.bytes()
                    runOnUiThread {
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        bitmap?.let { drawFactBitmap(it) }
                    }
                }
            })
        }.start()
    }

    private var factOverlay: GroundOverlay? = null

    /**
     * 绘制实况图
     */
    private fun drawFactBitmap(bitmap: Bitmap?) {
        if (bitmap == null) {
            return
        }
        val fromView = BitmapDescriptorFactory.fromBitmap(bitmap)
        val bounds = LatLngBounds.Builder()
                .include(start)
                .include(end)
                .build()

//        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        if (factOverlay == null) {
            factOverlay = aMap!!.addGroundOverlay(GroundOverlayOptions()
                    .anchor(0.5f, 0.5f)
                    .positionFromBounds(bounds)
                    .image(fromView)
                    .transparency(0.2f))
        } else {
            factOverlay!!.setImage(null)
            factOverlay!!.setPositionFromBounds(bounds)
            factOverlay!!.setImage(fromView)
        }
    }

    /**
     * 清除实况图
     */
    private fun removeFactLayer() {
        if (factOverlay != null) {
            factOverlay!!.remove()
            factOverlay = null
        }
    }

    private inner class RadarThread : Thread() {
        private var itemList: ArrayList<PointForeDto>? = null
        var currentState: Int
        private var index: Int
        private var count = 0
        private var isTracking: Boolean

        val STATE_NONE = 0
        val STATE_PLAYING = 1
        val STATE_PAUSE = 2
        val STATE_CANCEL = 3

        init {
            index = currentIndex
            currentState = STATE_PAUSE
            isTracking = false
        }

        fun setDataList(itemList: ArrayList<PointForeDto>) {
            this.itemList = itemList
            count = itemList.size
        }

        override fun run() {
            super.run()
            while (true) {
                if (currentState == STATE_CANCEL) {
                    break
                }
                if (currentState == STATE_PAUSE) {
                    continue
                }
                if (isTracking) {
                    continue
                }
                sendRadar()
                try {
                    sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }

        private fun sendRadar() {
            if (index >= count || index < 0) {
                index = 0
            } else {
                runOnUiThread(Runnable {
                    val dto = itemList!![index]
                    okHttpBitmap(dto.imgUrl)
                    changeProgress(dto.time, index++, count - 1)
                    currentIndex = index
                })
            }
        }

        fun cancel() {
            currentState = STATE_CANCEL
        }

        fun pause() {
            currentState = STATE_PAUSE
        }

        fun play() {
            currentState = STATE_PLAYING
        }

        fun setCurrent(index: Int) {
            this.index = index
        }

        fun startTracking() {
            isTracking = true
        }

        fun stopTracking() {
            isTracking = false
            if (currentState == STATE_PAUSE) {
                sendRadar()
            }
        }

    }

    @SuppressLint("SimpleDateFormat")
    private fun changeProgress(time: String, progress: Int, max: Int) {
        if (seekBar != null) {
            seekBar!!.max = max
            seekBar!!.progress = progress
        }
        if (!TextUtils.isEmpty(time)) {
            try {
                tvTime.text = sdf1.format(sdf2.parse(time))
                val ttTime: String = tvTime.text.toString()
                when (dataType) {
                    1 -> {
                        tvName!!.text = ttTime + "格点温度预报[单位:" + getString(R.string.unit_degree) + "]"
                    }
                    2 -> {
                        tvName!!.text = ttTime + "格点相对湿度预报[单位:" + getString(R.string.unit_percent) + "]"
                    }
                    3 -> {
                        tvName!!.text = ttTime + "格点风速预报[单位:" + getString(R.string.unit_speed) + "]"
                    }
                    5 -> {
                        tvName!!.text = ttTime + "格点云量预报[单位:" + getString(R.string.unit_percent) + "]"
                    }
                    6 -> {
                        tvName!!.text = ttTime + "格点降水预报[单位:" + getString(R.string.unit_mm) + "]"
                    }
                }
            } catch (e: ParseException) {
                e.printStackTrace()
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.llBack -> finish()
            R.id.tvTemp, R.id.ivTemp -> {
                ivTemp!!.setImageResource(R.drawable.com_temp_press)
                ivHumidity.setImageResource(R.drawable.com_humidity)
                ivWind.setImageResource(R.drawable.fzj_butn_windoff)
                ivCloud.setImageResource(R.drawable.com_cloud)
                ivRain.setImageResource(R.drawable.fzj_butn_rainoff)
                tvTemp!!.setBackgroundResource(R.drawable.bg_map_btn_press)
                tvHumidity.setBackgroundResource(R.drawable.bg_map_btn)
                tvWind.setBackgroundResource(R.drawable.bg_map_btn)
                tvCloud.setBackgroundResource(R.drawable.bg_map_btn)
                tvRain.setBackgroundResource(R.drawable.bg_map_btn)
                tvTemp!!.setTextColor(Color.WHITE)
                tvHumidity.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                tvWind.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                tvCloud.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                tvRain.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                dataType = 1
                switchElement()
            }
            R.id.tvHumidity, R.id.ivHumidity -> {
                ivTemp!!.setImageResource(R.drawable.com_temp)
                ivHumidity.setImageResource(R.drawable.com_humidity_press)
                ivWind.setImageResource(R.drawable.fzj_butn_windoff)
                ivCloud.setImageResource(R.drawable.com_cloud)
                ivRain.setImageResource(R.drawable.fzj_butn_rainoff)
                tvTemp!!.setBackgroundResource(R.drawable.bg_map_btn)
                tvHumidity.setBackgroundResource(R.drawable.bg_map_btn_press)
                tvWind.setBackgroundResource(R.drawable.bg_map_btn)
                tvCloud.setBackgroundResource(R.drawable.bg_map_btn)
                tvRain.setBackgroundResource(R.drawable.bg_map_btn)
                tvTemp!!.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                tvHumidity.setTextColor(Color.WHITE)
                tvWind.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                tvCloud.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                tvRain.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                dataType = 2
                switchElement()
            }
            R.id.tvWind, R.id.ivWind -> {
                ivTemp!!.setImageResource(R.drawable.com_temp)
                ivHumidity.setImageResource(R.drawable.com_humidity)
                ivWind.setImageResource(R.drawable.fzj_butn_wind)
                ivCloud.setImageResource(R.drawable.com_cloud)
                ivRain.setImageResource(R.drawable.fzj_butn_rainoff)
                tvTemp!!.setBackgroundResource(R.drawable.bg_map_btn)
                tvHumidity.setBackgroundResource(R.drawable.bg_map_btn)
                tvWind.setBackgroundResource(R.drawable.bg_map_btn_press)
                tvCloud.setBackgroundResource(R.drawable.bg_map_btn)
                tvRain.setBackgroundResource(R.drawable.bg_map_btn)
                tvTemp!!.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                tvHumidity.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                tvWind.setTextColor(Color.WHITE)
                tvCloud.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                tvRain.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                dataType = 3
                switchElement()
            }
            R.id.tvCloud, R.id.ivCloud -> {
                ivTemp!!.setImageResource(R.drawable.com_temp)
                ivHumidity.setImageResource(R.drawable.com_humidity)
                ivWind.setImageResource(R.drawable.fzj_butn_windoff)
                ivCloud.setImageResource(R.drawable.com_cloud_press)
                ivRain.setImageResource(R.drawable.fzj_butn_rainoff)
                tvTemp!!.setBackgroundResource(R.drawable.bg_map_btn)
                tvHumidity.setBackgroundResource(R.drawable.bg_map_btn)
                tvWind.setBackgroundResource(R.drawable.bg_map_btn)
                tvCloud.setBackgroundResource(R.drawable.bg_map_btn_press)
                tvRain.setBackgroundResource(R.drawable.bg_map_btn)
                tvTemp!!.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                tvHumidity.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                tvWind.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                tvCloud.setTextColor(Color.WHITE)
                tvRain.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                dataType = 5
                switchElement()
            }
            R.id.tvRain, R.id.ivRain -> {
                ivTemp!!.setImageResource(R.drawable.com_temp)
                ivHumidity.setImageResource(R.drawable.com_humidity)
                ivWind.setImageResource(R.drawable.fzj_butn_windoff)
                ivCloud.setImageResource(R.drawable.com_cloud)
                ivRain.setImageResource(R.drawable.fzj_butn_rain)
                tvTemp!!.setBackgroundResource(R.drawable.bg_map_btn)
                tvHumidity.setBackgroundResource(R.drawable.bg_map_btn)
                tvWind.setBackgroundResource(R.drawable.bg_map_btn)
                tvCloud.setBackgroundResource(R.drawable.bg_map_btn)
                tvRain.setBackgroundResource(R.drawable.bg_map_btn_press)
                tvTemp!!.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                tvHumidity.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                tvWind.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                tvCloud.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                tvRain.setTextColor(Color.WHITE)
                dataType = 6
                switchElement()
            }
            R.id.ivSwitch -> {
                if (mapType == AMap.MAP_TYPE_NORMAL) {
                    mapType = AMap.MAP_TYPE_SATELLITE
                    ivSwitch.setImageResource(R.drawable.com_switch_map_press)
                } else if (mapType == AMap.MAP_TYPE_SATELLITE) {
                    mapType = AMap.MAP_TYPE_NORMAL
                    ivSwitch.setImageResource(R.drawable.com_switch_map)
                }
                if (aMap != null) {
                    aMap!!.mapType = mapType
                }
            }
            R.id.ivDataSource -> if (tvDataSource.visibility == View.VISIBLE) {
                    tvDataSource.visibility = View.GONE
                ivDataSource.setImageResource(R.drawable.com_data_source)
            } else {
                    tvDataSource.visibility = View.VISIBLE
                ivDataSource.setImageResource(R.drawable.com_data_source_press)
            }
            R.id.tvDataSource -> {
                val intent = Intent(this, WebviewActivity::class.java)
                intent.putExtra(CONST.ACTIVITY_NAME, "中央气象台智能网格预报产品")
                intent.putExtra(CONST.WEB_URL, "http://www.cma.gov.cn/2011xzt/2017zt/2017qmt/20170728/")
                startActivity(intent)
            }
            R.id.ivLocation -> if (zoom >= 12f) {
                aMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(locationLat, locationLng), 5.5f))
            } else {
                aMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(locationLat, locationLng), 12.0f))
            }
            R.id.ivLegendPrompt -> if (ivLegend.visibility == View.VISIBLE) {
                    ivLegend.visibility = View.INVISIBLE
            } else {
                    ivLegend.visibility = View.VISIBLE
            }
            R.id.ivPlay -> if (mRadarThread != null && mRadarThread!!.currentState == mRadarThread!!.STATE_PLAYING) {
                mRadarThread!!.pause()
                ivPlay.setImageResource(R.drawable.icon_play)
            } else if (mRadarThread != null && mRadarThread!!.currentState == mRadarThread!!.STATE_PAUSE) {
                mRadarThread!!.play()
                ivPlay.setImageResource(R.drawable.icon_pause)
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
