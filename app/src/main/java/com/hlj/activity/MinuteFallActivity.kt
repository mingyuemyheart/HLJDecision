package com.hlj.activity

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.view.View
import android.widget.SeekBar
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.hlj.common.CONST
import com.hlj.dto.MinuteFallDto
import com.hlj.dto.WeatherDto
import com.hlj.manager.CaiyunManager
import com.hlj.utils.CommonUtil
import com.hlj.utils.OkHttpUtil
import com.hlj.view.MinuteFallView2
import kotlinx.android.synthetic.main.activity_minute_fall.*
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * 分钟级降水
 */
class MinuteFallActivity : BaseActivity(), View.OnClickListener, CaiyunManager.RadarListener, AMap.OnMapClickListener, GeocodeSearch.OnGeocodeSearchListener, AMapLocationListener, AMap.OnMapScreenShotListener {

    private var locationLat = CONST.defaultLat
    private var locationLng = CONST.defaultLng
    private var aMap: AMap? = null
    private val dataList: ArrayList<MinuteFallDto> = ArrayList()
    private val images: ArrayList<MinuteFallDto> = ArrayList()
    private var mOverlay: GroundOverlay? = null
    private var mRadarManager: CaiyunManager? = null
    private var mRadarThread: RadarThread? = null
    private val HANDLER_SHOW_RADAR = 1
    private var clickMarker: Marker? = null
    private var geocoderSearch: GeocodeSearch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_minute_fall)
        showDialog()
        initMap(savedInstanceState)
        initWidget()
    }

    private fun initWidget() {
        llBack!!.setOnClickListener(this)
        ivControl.setImageResource(R.drawable.icon_share)
        ivControl.setOnClickListener(this)
        ivControl.visibility = View.VISIBLE
        ivPlay!!.setOnClickListener(this)
        ivRank!!.setOnClickListener(this)
        seekBar!!.setOnSeekBarChangeListener(seekbarListener)

        val title = intent.getStringExtra(CONST.ACTIVITY_NAME)
        if (!TextUtils.isEmpty(title)) {
            tvTitle!!.text = title
        }
        geocoderSearch = GeocodeSearch(this)
        geocoderSearch!!.setOnGeocodeSearchListener(this)
        mRadarManager = CaiyunManager(this)

        checkLocationAuthority(object : LocationCallback {
            override fun grantedLocation(isGranted: Boolean) {
                if (isGranted) {
                    startLocation()
                } else {
                    aMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(locationLat, locationLng), 8.0f))
                    addMarkerToMap()
                    okHttpMinuteInfo()
                    okHttpMinuteImage()
                }
            }
        })
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
            aMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(locationLat, locationLng), 8.0f))
            addMarkerToMap()
            okHttpMinuteInfo()
            okHttpMinuteImage()
        }
    }

    /**
     * 获取彩云一小时内降雨、或降雪信息
     * @param lng
     * @param lat
     */
    private fun okHttpMinuteInfo() {
        val url = "http://api.caiyunapp.com/v2/HyTVV5YAkoxlQ3Zd/$locationLng,$locationLat/forecast"
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
                                val `object` = JSONObject(result)
                                if (!`object`.isNull("result")) {
                                    val obj = `object`.getJSONObject("result")
                                    if (!obj.isNull("minutely")) {
                                        val objMin = obj.getJSONObject("minutely")
                                        if (!objMin.isNull("description")) {
                                            val rain = objMin.getString("description")
                                            if (!TextUtils.isEmpty(rain)) {
                                                tvRain!!.text = rain.replace("小彩云", "")
                                                tvRain!!.visibility = View.VISIBLE
                                            } else {
                                                tvRain!!.visibility = View.GONE
                                            }
                                        }
                                        if (!objMin.isNull("precipitation_2h")) {
                                            val array = objMin.getJSONArray("precipitation_2h")
                                            val size = array.length()
                                            val minuteList: ArrayList<WeatherDto> = ArrayList()
                                            for (i in 0 until size) {
                                                val dto = WeatherDto()
                                                dto.minuteFall = array.getDouble(i).toFloat()
                                                minuteList.add(dto)
                                            }
                                            val minuteFallView = MinuteFallView2(this@MinuteFallActivity)
                                            minuteFallView.setData(minuteList, tvRain!!.text.toString())
                                            llContainer3!!.removeAllViews()
                                            llContainer3!!.addView(minuteFallView, CommonUtil.widthPixels(this@MinuteFallActivity), CommonUtil.dip2px(this@MinuteFallActivity, 120f).toInt())
                                        }
                                    }
                                }
                            } catch (e1: JSONException) {
                                e1.printStackTrace()
                            }
                        }
                    }
                }
            })
        }.start()
    }

    private val seekbarListener: SeekBar.OnSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
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

    private fun initMap(bundle: Bundle?) {
        mapView!!.onCreate(bundle)
        if (aMap == null) {
            aMap = mapView!!.map
        }
        aMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(CONST.guizhouLatLng, 8.0f))
        aMap!!.uiSettings.isZoomControlsEnabled = false
        aMap!!.uiSettings.isRotateGesturesEnabled = false
        aMap!!.setOnMapClickListener(this)
        aMap!!.setOnMapLoadedListener {
            tvMapNumber.text = aMap!!.mapContentApprovalNumber
            CommonUtil.drawHLJJson(this, aMap)
        }
    }

    private fun addMarkerToMap() {
        if (clickMarker != null) {
            clickMarker!!.remove()
        }
        val options = MarkerOptions()
        options.position(LatLng(locationLat, locationLng))
        options.anchor(0.5f, 0.5f)
        val bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(resources, R.drawable.icon_map_location),
                CommonUtil.dip2px(this, 16f).toInt(), CommonUtil.dip2px(this, 24f).toInt())
        if (bitmap != null) {
            options.icon(BitmapDescriptorFactory.fromBitmap(bitmap))
        } else {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_map_location))
        }
        clickMarker = aMap!!.addMarker(options)
        clickMarker!!.isClickable = false
        searchAddrByLatLng()
    }

    override fun onMapClick(arg0: LatLng) {
        tvAddr!!.text = ""
        tvRain!!.text = ""
        locationLat = arg0.latitude
        locationLng = arg0.longitude
        addMarkerToMap()
        okHttpMinuteInfo()
    }

    /**
     * 通过经纬度获取地理位置信息
     * @param lat
     * @param lng
     */
    private fun searchAddrByLatLng() {
        //latLonPoint参数表示一个Latlng，第二参数表示范围多少米，GeocodeSearch.AMAP表示是国测局坐标系还是GPS原生坐标系
        val query = RegeocodeQuery(LatLonPoint(locationLat, locationLng), 200f, GeocodeSearch.AMAP)
        geocoderSearch!!.getFromLocationAsyn(query)
    }

    override fun onGeocodeSearched(arg0: GeocodeResult?, arg1: Int) {}
    override fun onRegeocodeSearched(result: RegeocodeResult?, rCode: Int) {
        if (rCode == 1000) {
            if (result != null && result.regeocodeAddress != null && result.regeocodeAddress.formatAddress != null) {
                val addr = result.regeocodeAddress.formatAddress
                if (!TextUtils.isEmpty(addr)) {
                    tvAddr!!.text = addr
                }
            }
        }
    }

    private fun okHttpMinuteImage() {
        Thread {
            val url = "http://api.tianqi.cn:8070/v1/img.py"
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
                                val obj = JSONObject(result)
                                if (!obj.isNull("status")) {
                                    if (obj.getString("status") == "ok") {
                                        if (!obj.isNull("radar_img")) {
                                            dataList.clear()
                                            val array = JSONArray(obj.getString("radar_img"))
                                            for (i in 0 until array.length()) {
                                                val array0 = array.getJSONArray(i)
                                                val dto = MinuteFallDto()
                                                dto.imgUrl = array0.optString(0)
                                                dto.time = array0.optLong(1)
                                                val itemArray = array0.getJSONArray(2)
                                                dto.p1 = itemArray.optDouble(0)
                                                dto.p2 = itemArray.optDouble(1)
                                                dto.p3 = itemArray.optDouble(2)
                                                dto.p4 = itemArray.optDouble(3)
                                                dataList.add(dto)
                                            }
                                            if (dataList.size > 0) {
                                                startDownLoadImgs(dataList)
                                            }
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

    private fun startDownLoadImgs(list: ArrayList<MinuteFallDto>) {
        if (mRadarThread != null) {
            mRadarThread!!.cancel()
            mRadarThread = null
        }
        mRadarManager!!.loadImagesAsyn(list, this)
    }

    override fun onResult(result: Int, images: ArrayList<MinuteFallDto>) {
        runOnUiThread {
            cancelDialog()
            llSeekBar!!.visibility = View.VISIBLE
        }
        if (result == CaiyunManager.RadarListener.RESULT_SUCCESSED) {
            this.images.clear()
            this.images.addAll(images)

            //把最新的一张降雨图片覆盖在地图上
            val radar = images[images.size - 1]
            val message = mHandler.obtainMessage()
            message.what = HANDLER_SHOW_RADAR
            message.obj = radar
            message.arg1 = 100
            message.arg2 = 100
            mHandler.sendMessage(message)
        }
    }

    override fun onProgress(url: String?, progress: Int) {}

    private fun showRadar(bitmap: Bitmap, p1: Double, p2: Double, p3: Double, p4: Double) {
        val fromView = BitmapDescriptorFactory.fromBitmap(bitmap)
        val bounds = LatLngBounds.Builder()
                .include(LatLng(p3, p2))
                .include(LatLng(p1, p4))
                .build()
        if (mOverlay == null) {
            mOverlay = aMap!!.addGroundOverlay(GroundOverlayOptions()
                    .anchor(0.5f, 0.5f)
                    .positionFromBounds(bounds)
                    .image(fromView)
                    .transparency(0f))
        } else {
            mOverlay!!.setImage(null)
            mOverlay!!.setPositionFromBounds(bounds)
            mOverlay!!.setImage(fromView)
        }
        aMap!!.runOnDrawFrame()
    }

    @SuppressLint("HandlerLeak")
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                HANDLER_SHOW_RADAR -> if (msg.obj != null) {
                    val dto = msg.obj as MinuteFallDto
                    if (dto.getPath() != null) {
                        val bitmap = BitmapFactory.decodeFile(dto.getPath())
                        if (bitmap != null) {
                            showRadar(bitmap, dto.p1, dto.p2, dto.p3, dto.p4)
                        }
                    }
                    changeProgress(dto.time, msg.arg2, msg.arg1)
                }
            }
        }
    }

    private val STATE_NONE = 0
    private val STATE_PLAYING = 1
    private val STATE_PAUSE = 2
    private val STATE_CANCEL = 3
    private inner class RadarThread(private val images: ArrayList<MinuteFallDto>) : Thread() {
        var currentState: Int
        private var index: Int
        private val count: Int
        private var isTracking = false

        init {
            count = images.size
            index = 0
            currentState = STATE_NONE
            isTracking = false
        }

        override fun run() {
            super.run()
            currentState = STATE_PLAYING
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
                    sleep(200)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }

        private fun sendRadar() {
            if (index >= count || index < 0) {
                index = 0
            } else {
                val radar = images[index]
                val message: Message = mHandler.obtainMessage()
                message.what = HANDLER_SHOW_RADAR
                message.obj = radar
                message.arg1 = count - 1
                message.arg2 = index++
                mHandler.sendMessage(message)
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
    private fun changeProgress(time: Long, progress: Int, max: Int) {
        if (seekBar != null) {
            seekBar!!.max = max
            seekBar!!.progress = progress
        }
        val sdf = SimpleDateFormat("HH:mm")
        val value = time.toString() + "000"
        val date = Date(java.lang.Long.valueOf(value))
        tvTime!!.text = sdf.format(date)
    }

    override fun onMapScreenShot(bitmap1: Bitmap?) { //bitmap1为地图截屏
        val bitmap2 = CommonUtil.captureView(clShare)
        val bitmap3 = CommonUtil.mergeBitmap(this, bitmap1, bitmap2, true)
        CommonUtil.clearBitmap(bitmap1)
        CommonUtil.clearBitmap(bitmap2)
        val bitmap4 = CommonUtil.captureView(clRain)
        val bitmap5 = CommonUtil.captureMyView(llContainer3)
        val bitmap6 = CommonUtil.mergeBitmap(this, bitmap4, bitmap5, false)
        CommonUtil.clearBitmap(bitmap4)
        CommonUtil.clearBitmap(bitmap5)
        val bitmap9 = CommonUtil.captureView(clRainBg)
        val bitmap10 = CommonUtil.mergeBitmap(this, bitmap6, bitmap9, true)
        CommonUtil.clearBitmap(bitmap6)
        CommonUtil.clearBitmap(bitmap9)
        val bitmap7 = CommonUtil.mergeBitmap(this, bitmap3, bitmap10, true)
        CommonUtil.clearBitmap(bitmap3)
        CommonUtil.clearBitmap(bitmap10)
        val bitmap8 = BitmapFactory.decodeResource(resources, R.drawable.legend_share_portrait)
        val bitmap = CommonUtil.mergeBitmap(this, bitmap7, bitmap8, false)
        CommonUtil.clearBitmap(bitmap7)
        CommonUtil.clearBitmap(bitmap8)
        CommonUtil.share(this, bitmap)
    }

    override fun onMapScreenShot(arg0: Bitmap?, arg1: Int) {}

    override fun onClick(v: View) {
        when (v.id) {
            R.id.llBack -> {
                finish()
            }
            R.id.ivControl -> aMap!!.getMapScreenShot(this)
            R.id.ivPlay -> {
                if (mRadarThread != null && mRadarThread!!.currentState == STATE_PLAYING) {
                    mRadarThread!!.pause()
                    ivPlay!!.setImageResource(R.drawable.icon_play)
                } else if (mRadarThread != null && mRadarThread!!.currentState == STATE_PAUSE) {
                    mRadarThread!!.play()
                    ivPlay!!.setImageResource(R.drawable.icon_pause)
                } else if (mRadarThread == null) {
                    ivPlay!!.setImageResource(R.drawable.icon_pause)
                    if (mRadarThread != null) {
                        mRadarThread!!.cancel()
                        mRadarThread = null
                    }
                    if (images.isNotEmpty()) {
                        mRadarThread = RadarThread(images)
                        mRadarThread!!.start()
                    }
                }
            }
            R.id.ivRank -> {
                if (ivLegend!!.visibility == View.VISIBLE) {
                    ivLegend!!.visibility = View.INVISIBLE
                } else {
                    ivLegend!!.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * 方法必须重写
     */
    override fun onResume() {
        super.onResume()
        mapView!!.onResume()
    }

    /**
     * 方法必须重写
     */
    override fun onPause() {
        super.onPause()
        mapView!!.onPause()
    }

    /**
     * 方法必须重写
     */
    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        mapView!!.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mapView != null) {
            mapView!!.onDestroy()
        }
        if (mRadarManager != null) {
            mRadarManager!!.onDestory()
        }
        if (mRadarThread != null) {
            mRadarThread!!.cancel()
            mRadarThread = null
        }
    }

}
