package com.hlj.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.os.*
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.View.OnClickListener
import android.widget.TextView
import android.widget.Toast
import cn.com.weather.api.WeatherAPI
import cn.com.weather.beans.Weather
import cn.com.weather.constants.Constants.Language
import cn.com.weather.listener.AsyncResponseHandler
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
import com.hlj.activity.*
import com.hlj.adapter.WeeklyForecastAdapter
import com.hlj.common.CONST
import com.hlj.common.MyApplication
import com.hlj.dto.CityDto
import com.hlj.dto.MinuteFallDto
import com.hlj.dto.WarningDto
import com.hlj.dto.WeatherDto
import com.hlj.manager.CaiyunManager
import com.hlj.manager.DBManager
import com.hlj.manager.XiangJiManager
import com.hlj.utils.CommonUtil
import com.hlj.utils.OkHttpUtil
import com.hlj.utils.WeatherUtil
import com.hlj.view.HourlyView
import com.hlj.view.MinuteFallView
import com.hlj.view.WeeklyView
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechError
import com.iflytek.cloud.SpeechSynthesizer
import com.iflytek.cloud.SynthesizerListener
import kotlinx.android.synthetic.main.fragment_forecast.*
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
import kotlin.collections.ArrayList

/**
 * 天气预报
 */
class ForecastFragment : BaseFragment(), OnClickListener, AMapLocationListener, CaiyunManager.RadarListener{

    private var isFusion = false
    private var lat = CONST.centerLat
    private var lng = CONST.centerLng
    private var cityId = ""
    private var timer: Timer? = null
    private var mAdapter: WeeklyForecastAdapter? = null
    private val weeklyList: MutableList<WeatherDto> = ArrayList()
    private val hourlyList: MutableList<WeatherDto> = ArrayList()
    private val sdf1 = SimpleDateFormat("HH", Locale.CHINA)
    private val sdf2 = SimpleDateFormat("yyyy年MM月dd日 HH时mm分", Locale.CHINA)
    private val sdf3 = SimpleDateFormat("yyyyMMdd", Locale.CHINA)
    private val sdf4 = SimpleDateFormat("yyyyMMddHHmm", Locale.CHINA)
    private val sdf5 = SimpleDateFormat("yyyyMMddHH", Locale.CHINA)
    private val sdf6 = SimpleDateFormat("HH:mm", Locale.CHINA)
    private val sdf7 = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA)
    private val disWarnings: MutableList<WarningDto?> = ArrayList()
    private val cityWarnings: MutableList<WarningDto?> = ArrayList()
    private val proWarnings: MutableList<WarningDto?> = ArrayList()
    private val hourAqiList: ArrayList<WeatherDto> = ArrayList()
    private val dayAqiList: ArrayList<WeatherDto> = ArrayList()

    //语音播报
    private var mTts : SpeechSynthesizer? = null// 语音合成对象
    private var voicer = "xiaoyan"// 默认发音人
    private var mPercentForBuffering = 0// 缓冲进度
    private var mPercentForPlaying = 0// 播放进度
    private var mEngineType = SpeechConstant.TYPE_CLOUD// 引擎类型
    private var mToast : Toast? = null
    private var weatherText = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_forecast, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRefreshLayout()
        initMap(savedInstanceState)
        initWidget()
        initListView()
        initSpeech()
    }

    private fun refresh() {
        if (mTts != null && mTts!!.isSpeaking) {
            mTts!!.stopSpeaking()
        }
        refreshLayout.isRefreshing = true
        completeLocation()
    }

    /**
     * 初始化下拉刷新布局
     */
    private fun initRefreshLayout() {
        refreshLayout.setColorSchemeResources(CONST.color1, CONST.color2, CONST.color3, CONST.color4)
        refreshLayout.setProgressViewEndTarget(true, 300)
        refreshLayout.isRefreshing = true
        refreshLayout.setOnRefreshListener { refresh() }
    }

    /**
     * 初始化控件
     */
    private fun initWidget() {
        //解决scrollView嵌套listview，动态计算listview高度后，自动滚动到屏幕底部
        ivMap.setOnClickListener(this)
        tvPosition.setOnClickListener(this)
        tvFact.setOnClickListener(this)
        tvBody.setOnClickListener(this)
        tvChart.setOnClickListener(this)
        tvList.setOnClickListener(this)
        tvAqiCount.setOnClickListener(this)
        tvAqi.setOnClickListener(this)
        clMinute.setOnClickListener(this)
        tvDisWarning!!.setOnClickListener(this)
        tvCityWarning!!.setOnClickListener(this)
        tvProWarning!!.setOnClickListener(this)
        tvRain.setOnClickListener(this)
        clHour.setOnClickListener(this)
        tvDivPolicy.setOnClickListener(this)
        tvInfo.setOnClickListener(this)
        ivClimate.setOnClickListener(this)
        clVideo.setOnClickListener(this)
        clAudio.setOnClickListener(this)
        ivPlay2!!.setOnClickListener(this)
        if (TextUtils.equals(MyApplication.getAppTheme(), "1")) {
            refreshLayout!!.setBackgroundColor(Color.BLACK)
            clDay1.setBackgroundColor(Color.BLACK)
            clDay2.setBackgroundColor(Color.BLACK)
            clMinute.setBackgroundColor(Color.BLACK)
            clHour.setBackgroundColor(Color.BLACK)
            clFifteen.setBackgroundColor(Color.BLACK)
            ivHourly.setImageBitmap(CommonUtil.grayScaleImage(BitmapFactory.decodeResource(resources, R.drawable.icon_hour_rain)))
            ivFifteen.setImageBitmap(CommonUtil.grayScaleImage(BitmapFactory.decodeResource(resources, R.drawable.icon_fifteen)))
        }

        if (mTts != null && mTts!!.isSpeaking) {
            mTts!!.stopSpeaking()
        }

        if (CommonUtil.isLocationOpen(activity)) {
            startLocation()
        } else {
            Toast.makeText(activity, "未开启定位，请选择城市", Toast.LENGTH_LONG).show()
            startActivityForResult(Intent(activity, CityActivity::class.java), 1001)
        }
    }

    /**
     * 重置计时器
     */
    private fun resetTimer() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        resetTimer()
    }

    /**
     * 开始定位
     */
    private fun startLocation() {
        val mLocationOption = AMapLocationClientOption() //初始化定位参数
        val mLocationClient = AMapLocationClient(activity) //初始化定位
        mLocationOption.locationMode = AMapLocationMode.Hight_Accuracy //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.isNeedAddress = true //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.isOnceLocation = true //设置是否只定位一次,默认为false
        mLocationOption.isWifiActiveScan = true //设置是否强制刷新WIFI，默认为强制刷新
        mLocationOption.isMockEnable = false //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.interval = 2000 //设置定位间隔,单位毫秒,默认为2000ms
        mLocationClient.setLocationOption(mLocationOption) //给定位客户端对象设置定位参数
        mLocationClient.setLocationListener(this)
        mLocationClient.startLocation() //启动定位
    }

    override fun onLocationChanged(amapLocation: AMapLocation?) {
        if (amapLocation != null && amapLocation.errorCode == AMapLocation.LOCATION_SUCCESS) {
            tvPosition!!.text = amapLocation.district+amapLocation.street + amapLocation.streetNum
            lat = amapLocation.latitude
            lng = amapLocation.longitude
            if (timer == null) {
                timer = Timer()
                timer!!.schedule(object : TimerTask() {
                    override fun run() {
                        activity!!.runOnUiThread {
                            refresh()
                        }
                    }
                }, 0, 1000*60*MyApplication.refreshTime)
            }
        }
    }

    private fun completeLocation() {
        addMarkerToMap(LatLng(lat, lng))
        getGeo(isFusion)
        okHttpHourRain()
    }

    private fun addMarkerToMap(latLng: LatLng) {
        val options = MarkerOptions()
        options.position(latLng)
        options.anchor(0.5f, 0.5f)
        val bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(resources, R.drawable.icon_map_location),
                CommonUtil.dip2px(activity, 16f).toInt(), CommonUtil.dip2px(activity, 24f).toInt())
        if (bitmap != null) {
            options.icon(BitmapDescriptorFactory.fromBitmap(bitmap))
        } else {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_map_location))
        }
        aMap!!.addMarker(options)
        aMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 6.0f))
    }

    private fun initSpeech() {
        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(activity) { code ->

        }
        mToast = Toast.makeText(activity,"",Toast.LENGTH_SHORT)

        // 设置参数
        mTts!!.setParameter(SpeechConstant.PARAMS, null)
        // 根据合成引擎设置相应参数
        if(mEngineType == SpeechConstant.TYPE_CLOUD) {
            mTts!!.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD)
            // 设置在线合成发音人
            mTts!!.setParameter(SpeechConstant.VOICE_NAME, voicer)
            //设置合成语速
            mTts!!.setParameter(SpeechConstant.SPEED, "50")
            //设置合成音调
            mTts!!.setParameter(SpeechConstant.PITCH, "50")
            //设置合成音量
            mTts!!.setParameter(SpeechConstant.VOLUME, "50")
        }else {
            mTts!!.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL)
            // 设置本地合成发音人 voicer为空，默认通过语记界面指定发音人。
            mTts!!.setParameter(SpeechConstant.VOICE_NAME, "")
        }
        //设置播放器音频流类型
        mTts!!.setParameter(SpeechConstant.STREAM_TYPE, "3")
        // 设置播放合成音频打断音乐播放，默认为true
        mTts!!.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true")

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts!!.setParameter(SpeechConstant.AUDIO_FORMAT, "wav")
        mTts!!.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory().toString()+"/msc/weather.wav")
    }

    override fun onDestroy() {
        super.onDestroy()
        mTts!!.stopSpeaking()
        mTts!!.destroy()

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

    private var aMap: AMap? = null
    private fun initMap(bundle: Bundle?) {
        mapView.onCreate(bundle)
        if (aMap == null) {
            aMap = mapView.map
        }
        aMap!!.moveCamera(CameraUpdateFactory.zoomTo(8.0f))
        aMap!!.uiSettings.isZoomControlsEnabled = false
        aMap!!.uiSettings.isRotateGesturesEnabled = false
        aMap!!.setOnMapLoadedListener {
            mRadarManager = CaiyunManager(activity)
            okHttpMinuteImage()
        }
        aMap!!.setOnMapTouchListener { arg0 ->
            if (scrollView != null) {
                if (arg0.action == MotionEvent.ACTION_UP) {
                    scrollView.requestDisallowInterceptTouchEvent(false)
                } else {
                    scrollView.requestDisallowInterceptTouchEvent(true)
                }
            }
        }
    }

    /**
     * 获取逐小时aqi
     */
    private fun okHttpHourAqi(f0: String) {
        Thread {
            val timestamp = Date().time
            val start1 = sdf5.format(sdf7.parse(f0))
            val end1 = sdf5.format(timestamp + 1000 * 60 * 60 * 24)
            val url = XiangJiManager.getXJSecretUrl(lng, lat, start1, end1, timestamp)
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                }
                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    if (!isAdded) {
                        return
                    }
                    val result = response.body!!.string()
                    activity!!.runOnUiThread {
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                val obj = JSONObject(result)

                                if (!obj.isNull("series")) {
                                    hourAqiList.clear()
                                    val array = obj.getJSONArray("series")
                                    for (i in 0 until array.length()) {
                                        val data = WeatherDto()
                                        data.aqi = array[i].toString()
                                        hourAqiList.add(data)
                                    }

                                    for (i in 0 until hourlyList.size) {
                                        val dto = hourlyList[i]
                                        if (hourAqiList.size > 0 && i < hourAqiList.size) {
                                            val aqiValue = hourAqiList[i].aqi
                                            if (!TextUtils.isEmpty(aqiValue)) {
                                                dto.hourlyAqi = aqiValue
                                            }
                                        }
                                    }
                                    //逐小时预报信息
                                    val cubicView = HourlyView(activity)
                                    cubicView.setData(hourlyList)
                                    llContainerHour!!.removeAllViews()
                                    llContainerHour!!.addView(cubicView, CommonUtil.widthPixels(activity) * 5 / 2, CommonUtil.dip2px(activity, 200f).toInt())
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

    /**
     * 获取15天aqi
     */
    private fun okHttpDayAqi(f0: String) {
        Thread {
            val timestamp = Date().time
            val start1 = sdf3.format(sdf4.parse(f0))
            val end1 = sdf3.format(sdf3.parse(start1).time + 1000 * 60 * 60 * 24 * 15)
            val url = XiangJiManager.getXJSecretUrl2(lng, lat, start1, end1, timestamp)
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                }
                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    if (!isAdded) {
                        return
                    }
                    val result = response.body!!.string()
                    activity!!.runOnUiThread {
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                val obj = JSONObject(result)

                                if (!obj.isNull("series")) {
                                    dayAqiList.clear()
                                    val array = obj.getJSONArray("series")
                                    for (i in 0 until array.length()) {
                                        val data = WeatherDto()
                                        data.aqi = array[i].toString()
                                        dayAqiList.add(data)
                                    }

                                    for (i in 0 until weeklyList.size) {
                                        val dto = weeklyList[i]
                                        if (dayAqiList.size > 0 && i < dayAqiList.size) {
                                            val aqiValue = dayAqiList[i].aqi
                                            if (!TextUtils.isEmpty(aqiValue)) {
                                                dto.aqi = aqiValue
                                            }
                                        }

                                        if (i == 0) {
                                            if (!TextUtils.isEmpty(dto.aqi) && !TextUtils.equals(dto.aqi, "?") && !TextUtils.equals(dto.aqi, "null")) {
                                                val value: Int = dto.aqi.toInt()
                                                if (value <= 150) {
                                                    tvAqi1!!.setTextColor(Color.BLACK)
                                                } else {
                                                    tvAqi1!!.setTextColor(Color.WHITE)
                                                }
                                                tvAqi1.text = CommonUtil.getAqiDes(activity, value)
                                                tvAqi1.setBackgroundResource(CommonUtil.getCornerBackground(value))
                                            }
                                        }
                                        if (i == 1) {
                                            if (dayAqiList.size > 1) {
                                                if (!TextUtils.isEmpty(dto.aqi) && !TextUtils.equals(dto.aqi, "?") && !TextUtils.equals(dto.aqi, "null")) {
                                                    val value: Int = dto.aqi.toInt()
                                                    if (value <= 150) {
                                                        tvAqi2!!.setTextColor(Color.BLACK)
                                                    } else {
                                                        tvAqi2!!.setTextColor(Color.WHITE)
                                                    }
                                                    tvAqi2.text = CommonUtil.getAqiDes(activity, value)
                                                    tvAqi2.setBackgroundResource(CommonUtil.getCornerBackground(value))
                                                }
                                            }
                                        }
                                    }

                                    var foreDate: Long = 0
                                    var currentDate: Long = 0
                                    try {
                                        val fTime = sdf3.format(sdf4.parse(f0))
                                        foreDate = sdf3.parse(fTime).time
                                        currentDate = sdf3.parse(sdf3.format(Date())).time
                                    } catch (e: ParseException) {
                                        e.printStackTrace()
                                    }
                                    //一周预报列表
                                    if (mAdapter != null) {
                                        mAdapter!!.foreDate = foreDate
                                        mAdapter!!.currentDate = currentDate
                                        mAdapter!!.notifyDataSetChanged()
                                    }

                                    //一周预报曲线
                                    val weeklyView = WeeklyView(activity)
                                    weeklyView.setData(weeklyList, foreDate, currentDate)
                                    llContainerFifteen!!.removeAllViews()
                                    llContainerFifteen!!.addView(weeklyView, CommonUtil.widthPixels(activity) * 2, CommonUtil.dip2px(activity, 360f).toInt())
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

    private fun getGeo(isFusion: Boolean) {
        if (isFusion) {
            getWeatherInfo(isFusion)
        } else {
            WeatherAPI.getGeo(activity, lng.toString(), lat.toString(), object : AsyncResponseHandler() {
                override fun onComplete(content: JSONObject) {
                    super.onComplete(content)
                    if (!content.isNull("geo")) {
                        try {
                            val geoObj = content.getJSONObject("geo")
                            if (!geoObj.isNull("id")) {
                                cityId = geoObj.getString("id")
                                Log.e("getGeogetGeo", content.toString())
                                if (!TextUtils.isEmpty(cityId)) {
                                    getWeatherInfo(isFusion)
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }
                override fun onError(error: Throwable, content: String) {
                    super.onError(error, content)
                }
            })
        }
    }

    private fun getWeatherInfo(isFusion: Boolean) {
        Thread {
            var url = "http://api.weatherdt.com/common/?area=$cityId&type=forecast|observe|alarm|air|rise&key=eca9a6c9ee6fafe74ac6bc81f577a680"
            if (isFusion) {
                url = "http://decision-admin.tianqi.cn/Home/Work2019/hlj_fusion_weather?lat=$lat&lon=$lng&type=forecast|observe&key=eca9a6c9ee6fafe74ac6bc81f577a680"
            }
            Log.e("getWeatherInfo", url)
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    activity!!.runOnUiThread {
                        refreshLayout.isRefreshing = false
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                val obj = JSONObject(result)

                                //实况信息
                                if (!obj.isNull("observe")) {
                                    val observe = obj.getJSONObject("observe")
                                    if (!observe.isNull(cityId)) {
                                        val `object` = observe.getJSONObject(cityId)
                                        if (!`object`.isNull("1001002")) {
                                            val o = `object`.getJSONObject("1001002")
                                            if (!o.isNull("000")) {
                                                val time = o.getString("000")
                                                if (time != null) {
                                                    tvTime!!.text = time + getString(R.string.update)
                                                }
                                            }
                                            if (!o.isNull("001")) {
                                                val weatherCode = o.getString("001")
                                                if (TextUtils.isEmpty(weatherCode) && !TextUtils.equals(weatherCode, "?") && !TextUtils.equals(weatherCode, "null")) {
                                                    try {
                                                        tvPhe!!.text = getString(WeatherUtil.getWeatherId(Integer.valueOf(weatherCode)))
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }
                                            if (!o.isNull("002")) {
                                                val factTemp = o.getString("002")
                                                tvTemp!!.text = "$factTemp"
                                                tvFact.tag = "$factTemp"
                                            }
                                            if (!o.isNull("004")) {
                                                val windDir = o.getString("004")
                                                if (!TextUtils.isEmpty(windDir) && !TextUtils.equals(windDir, "?") && !TextUtils.equals(windDir, "null")) {
                                                    val dir = getString(WeatherUtil.getWindDirection(Integer.valueOf(windDir)))
                                                    if (!o.isNull("003")) {
                                                        val windForce = o.getString("003")
                                                        if (!TextUtils.isEmpty(windForce) && !TextUtils.equals(windForce, "?") && !TextUtils.equals(windForce, "null")) {
                                                            val force = WeatherUtil.getFactWindForce(Integer.valueOf(windForce))
                                                            tvWind!!.text = "$dir $force"
                                                            when {
                                                                TextUtils.equals(dir, "北风") -> {
                                                                    ivWind!!.rotation = 0f
                                                                }
                                                                TextUtils.equals(dir, "东北风") -> {
                                                                    ivWind!!.rotation = 45f
                                                                }
                                                                TextUtils.equals(dir, "东风") -> {
                                                                    ivWind!!.rotation = 90f
                                                                }
                                                                TextUtils.equals(dir, "东南风") -> {
                                                                    ivWind!!.rotation = 135f
                                                                }
                                                                TextUtils.equals(dir, "南风") -> {
                                                                    ivWind!!.rotation = 180f
                                                                }
                                                                TextUtils.equals(dir, "西南风") -> {
                                                                    ivWind!!.rotation = 225f
                                                                }
                                                                TextUtils.equals(dir, "西风") -> {
                                                                    ivWind!!.rotation = 270f
                                                                }
                                                                TextUtils.equals(dir, "西北风") -> {
                                                                    ivWind!!.rotation = 315f
                                                                }
                                                            }
                                                            if (TextUtils.equals("1", MyApplication.getAppTheme())) {
                                                                ivWind!!.setImageBitmap(CommonUtil.grayScaleImage(BitmapFactory.decodeResource(resources, R.drawable.iv_winddir)))
                                                            } else {
                                                                ivWind!!.setImageResource(R.drawable.iv_winddir)
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            okHttpBody(cityId, o.getString("002"), o.getString("005"), o.getString("012"))
                                        }
                                    }
                                }

                                if (!obj.isNull("rise")) {
                                    val rise = obj.getJSONObject("rise")
                                    if (!rise.isNull(cityId)) {
                                        val obj1 = rise.getJSONObject(cityId)
                                        if (!obj1.isNull("1001008")) {
                                            val riseArray = obj1.getJSONArray("1001008")
                                            if (riseArray.length() > 0) {
                                                val itemObj: JSONObject = riseArray.getJSONObject(0)
                                                if (!itemObj.isNull("001") && !itemObj.isNull("002")) {
                                                    val riseTime = itemObj.getString("001")
                                                    val setTime = itemObj.getString("002")
                                                    val diviTime = sdf6.parse(setTime).time - sdf6.parse(riseTime).time
                                                    val hour = diviTime / (1000 * 60 * 60)
                                                    val hourStr = if (hour < 10) {
                                                        "0$hour"
                                                    } else {
                                                        "$hour"
                                                    }
                                                    val minute = (diviTime - hour * 1000 * 60 * 60) / (1000 * 60)
                                                    val minuteStr = if (minute < 10) {
                                                        "0$minute"
                                                    } else {
                                                        "$minute"
                                                    }
                                                    tvRiseTime.text = "日出时间：$riseTime\n日落时间：$setTime\n日照时间：$hourStr:$minuteStr"
                                                }
                                            }
                                        }
                                    }
                                }

                                //空气质量
                                if (!obj.isNull("air")) {
                                    val `object` = obj.getJSONObject("air")
                                    if (!`object`.isNull(cityId)) {
                                        val object1 = `object`.getJSONObject(cityId)
                                        if (!object1.isNull("2001006")) {
                                            val k = object1.getJSONObject("2001006")
                                            if (!k.isNull("002")) {
                                                val aqi = k.getString("002")
                                                if (!TextUtils.isEmpty(aqi) && !TextUtils.equals(aqi, "?") && !TextUtils.equals(aqi, "null")) {
                                                    tvAqiCount!!.text = aqi
                                                    try {
                                                        val aqiCount = Integer.valueOf(aqi)
                                                        if (aqiCount <= 150) {
                                                            tvAqiCount!!.setTextColor(Color.BLACK)
                                                        } else {
                                                            tvAqiCount!!.setTextColor(Color.WHITE)
                                                        }
                                                        tvAqiCount!!.setBackgroundResource(WeatherUtil.getAqiIcon(aqiCount))
                                                        tvAqi.text = "空气质量 " + WeatherUtil.getAqi(activity, aqiCount)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                //预报
                                if (!obj.isNull("forecast")) {
                                    val forecast = obj.getJSONObject("forecast")

                                    //逐小时预报信息
                                    if (!forecast.isNull("1h")) {
                                        hourlyList.clear()
                                        val `object` = forecast.getJSONObject("1h")
                                        if (!`object`.isNull(cityId)) {
                                            val object1 = `object`.getJSONObject(cityId)
                                            if (!object1.isNull("1001001")) {
                                                val array = object1.getJSONArray("1001001")
                                                var length = array.length()
                                                if (length >= 24) {
                                                    length = 24
                                                }

                                                var f0 = sdf5.format(Date())
                                                for (i in 0 until length) {
                                                    val itemObj = array.getJSONObject(i)
                                                    val dto = WeatherDto()
                                                    dto.hourlyTime = itemObj.getString("000")
                                                    if (i == 0) {
                                                        f0 = dto.hourlyTime
                                                    }
                                                    try {
                                                        val one = itemObj.getString("001")
                                                        if (!TextUtils.isEmpty(one) && !TextUtils.equals(one, "?") && !TextUtils.equals(one, "null")) {
                                                            dto.hourlyCode = Integer.valueOf(one)
                                                        }
                                                        val two = itemObj.getString("002")
                                                        if (!TextUtils.isEmpty(two) && !TextUtils.equals(two, "?") && !TextUtils.equals(two, "null")) {
                                                            dto.hourlyTemp = Integer.valueOf(two)
                                                        }
                                                        val four = itemObj.getString("004")
                                                        if (!TextUtils.isEmpty(four) && !TextUtils.equals(four, "?") && !TextUtils.equals(four, "null")) {
                                                            dto.hourlyWindDirCode = Integer.valueOf(four)
                                                        }
                                                        val three = itemObj.getString("003")
                                                        if (!TextUtils.isEmpty(three) && !TextUtils.equals(three, "?") && !TextUtils.equals(three, "null")) {
                                                            dto.hourlyWindForceCode = Integer.valueOf(three)
                                                            dto.hourlyWindForceString = WeatherUtil.getHourWindForce(dto.hourlyWindForceCode.toFloat())
                                                        }
//                                                        val five = itemObj.getString("005")
//                                                        if (!TextUtils.isEmpty(five) && !TextUtils.equals(five, "?") && !TextUtils.equals(five, "null")) {
//                                                            dto.hourlyAqi = five
//                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                    hourlyList.add(dto)
                                                }

                                                okHttpHourAqi(f0)
                                            }
                                        }
                                    }

                                    //15天预报信息
                                    if (!forecast.isNull("24h")) {
                                        weeklyList.clear()
                                        val `object` = forecast.getJSONObject("24h")
                                        if (!`object`.isNull(cityId)) {
                                            val object1 = `object`.getJSONObject(cityId)
                                            val f0 = object1.getString("000")
                                            if (!object1.isNull("1001001")) {
                                                val f1 = object1.getJSONArray("1001001")
                                                var length = f1.length()
                                                if (length >= 15) {
                                                    length = 15
                                                }
                                                for (i in 0 until length) {
                                                    val dto = WeatherDto()

                                                    //预报时间
                                                    dto.date = CommonUtil.getDate(f0, i) //日期
                                                    dto.week = CommonUtil.getWeek(f0, i) //星期几

                                                    //预报内容
                                                    val weeklyObj = f1.getJSONObject(i)

                                                    //晚上
                                                    val two = weeklyObj.getString("002")
                                                    if (!TextUtils.isEmpty(two) && !TextUtils.equals(two, "?") && !TextUtils.equals(two, "null")) {
                                                        dto.lowPheCode = Integer.valueOf(two)
                                                        dto.lowPhe = getString(WeatherUtil.getWeatherId(dto.lowPheCode))
                                                    }
                                                    val four = weeklyObj.getString("004")
                                                    if (!TextUtils.isEmpty(two) && !TextUtils.equals(two, "?") && !TextUtils.equals(two, "null")) {
                                                        dto.lowTemp = Integer.valueOf(four)
                                                    }

                                                    //白天
                                                    val one = weeklyObj.getString("001")
                                                    if (!TextUtils.isEmpty(one) && !TextUtils.equals(one, "?") && !TextUtils.equals(one, "null")) {
                                                        dto.highPheCode = Integer.valueOf(one)
                                                        dto.highPhe = getString(WeatherUtil.getWeatherId(dto.highPheCode))
                                                    }
                                                    val three = weeklyObj.getString("003")
                                                    if (!TextUtils.isEmpty(three) && !TextUtils.equals(three, "?") && !TextUtils.equals(three, "null")) {
                                                        dto.highTemp = Integer.valueOf(three)
                                                    }
                                                    val hour = sdf1.format(Date()).toInt()
                                                    if (hour in 5..17) {
                                                        val seven = weeklyObj.getString("007")
                                                        if (!TextUtils.isEmpty(seven) && !TextUtils.equals(seven, "?") && !TextUtils.equals(seven, "null")) {
                                                            dto.windDir = Integer.valueOf(seven)
                                                        }
                                                        val five = weeklyObj.getString("005")
                                                        if (!TextUtils.isEmpty(five) && !TextUtils.equals(five, "?") && !TextUtils.equals(five, "null")) {
                                                            dto.windForce = Integer.valueOf(five)
                                                            dto.windForceString = WeatherUtil.getDayWindForce(dto.windForce)
                                                        }
                                                    } else {
                                                        val eight = weeklyObj.getString("008")
                                                        if (!TextUtils.isEmpty(eight) && !TextUtils.equals(eight, "?") && !TextUtils.equals(eight, "null")) {
                                                            dto.windDir = Integer.valueOf(eight)
                                                        }
                                                        val six = weeklyObj.getString("006")
                                                        if (!TextUtils.isEmpty(six) && !TextUtils.equals(six, "?") && !TextUtils.equals(six, "null")) {
                                                            dto.windForce = Integer.valueOf(six)
                                                            dto.windForceString = WeatherUtil.getDayWindForce(dto.windForce)
                                                        }
                                                    }
                                                    if (dayAqiList.size > 0 && i < dayAqiList.size) {
                                                        val aqiValue = dayAqiList[i].aqi
                                                        if (!TextUtils.isEmpty(aqiValue)) {
                                                            dto.aqi = aqiValue
                                                        }
                                                    }
                                                    weeklyList.add(dto)
                                                    if (i == 0) {
                                                        weatherText = ""
                                                        var pheText: String? = null
                                                        var temperatureText: String? = null
                                                        var windDirText: String? = null
                                                        var windForceText: String? = null
                                                        pheText = if (dto.lowPheCode == dto.highPheCode) {
                                                            getString(WeatherUtil.getWeatherId(dto.lowPheCode))
                                                        } else {
                                                            getString(WeatherUtil.getWeatherId(dto.highPheCode)) + "转" + getString(WeatherUtil.getWeatherId(dto.lowPheCode))
                                                        }
                                                        temperatureText = "最高气温" + dto.highTemp + "摄氏度，" + "最低气温" + dto.lowTemp + "摄氏度"
                                                        windDirText = getString(WeatherUtil.getWindDirection(dto.windDir))
                                                        windForceText = WeatherUtil.getDayWindForce(dto.windForce)
                                                        weatherText = "，今天白天到今天夜间，" + pheText + "，" + temperatureText + "，" + windDirText + windForceText


                                                        tvDay1!!.text = "今天"
                                                        var drawable: Drawable
                                                        if (hour in 5..17) {
                                                            drawable = resources.getDrawable(R.drawable.phenomenon_drawable)
                                                            drawable.level = dto.highPheCode
                                                            tvPhe1!!.text = getString(WeatherUtil.getWeatherId(dto.highPheCode))
                                                        } else {
                                                            drawable = resources.getDrawable(R.drawable.phenomenon_drawable_night)
                                                            drawable.level = dto.lowPheCode
                                                            tvPhe1!!.text = getString(WeatherUtil.getWeatherId(dto.lowPheCode))
                                                        }
                                                        tvTemp1!!.text = dto.lowTemp.toString() + "/" + dto.highTemp + "℃"
                                                    }
                                                    if (i == 1) {
                                                        tvDay2!!.text = "明天"
                                                        var drawable: Drawable
                                                        if (hour in 5..17) {
                                                            drawable = resources.getDrawable(R.drawable.phenomenon_drawable)
                                                            drawable.level = dto.highPheCode
                                                            tvPhe2!!.text = getString(WeatherUtil.getWeatherId(dto.highPheCode))
                                                        } else {
                                                            drawable = resources.getDrawable(R.drawable.phenomenon_drawable_night)
                                                            drawable.level = dto.lowPheCode
                                                            tvPhe2!!.text = getString(WeatherUtil.getWeatherId(dto.lowPheCode))
                                                        }
                                                        tvTemp2!!.text = dto.lowTemp.toString() + "/" + dto.highTemp + "℃"
                                                    }
                                                }

                                                okHttpDayAqi(f0)
                                            }
                                        }
                                    }
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                            clMain!!.visibility = View.VISIBLE
                            refreshLayout!!.isRefreshing = false
                        }
                        activity!!.runOnUiThread { //获取预警信息
                            val warningId = queryWarningIdByCityId(cityId)
                            if (!TextUtils.isEmpty(warningId)) {
                                val pro = warningId!!.substring(0, 2) + "0000"
                                val city = warningId!!.substring(0, 4) + "00"
                                val ids = "${warningId},${pro},${city}"
                                setPushTags(ids)
                                okHttpWarning("http://decision-admin.tianqi.cn/Home/extra/getwarns?order=0&areaid=" + warningId!!.substring(0, 2), warningId)
                            }
                        }
                    }
                }
            })
        }.start()
    }

    /**
     * 设置umeng推送的tags
     * @param warningId
     */
    private fun setPushTags(warningId: String?) {
        if (activity == null) {
            return
        }
        var tags = warningId
        val sharedPreferences = activity!!.getSharedPreferences("RESERVE_CITY", Context.MODE_PRIVATE)
        val cityInfo = sharedPreferences.getString("cityInfo", "")
        if (!TextUtils.isEmpty(cityInfo)) {
            tags = "$tags,"
            if (cityInfo.contains(";")) {
                val array = cityInfo.split(";").toTypedArray()
                for (i in array.indices) {
                    val item = array[i]
                    if (!TextUtils.isEmpty(item) && item.contains(",")) {
                        val itemArray = item.split(",").toTypedArray()
                        tags = if (i == array.size - 1) {
                            tags + itemArray[2]
                        } else {
                            tags + itemArray[2] + ","
                        }
                    }
                }
            }
        }
        if (!TextUtils.isEmpty(tags)) {
            MyApplication.resetTags(tags)
        }
    }

    /**
     * 获取预警id
     */
    private fun queryWarningIdByCityId(cityId: String): String? {
        val dbManager = DBManager(activity)
        dbManager.openDateBase()
        dbManager.closeDatabase()
        val database = SQLiteDatabase.openOrCreateDatabase(DBManager.DB_PATH + "/" + DBManager.DB_NAME, null)
        val cursor = database.rawQuery("select * from " + DBManager.TABLE_NAME3 + " where cid = " + "\"" + cityId + "\"", null)
        var warningId: String? = null
        for (i in 0 until cursor.count) {
            cursor.moveToPosition(i)
            warningId = cursor.getString(cursor.getColumnIndex("wid"))
        }
        return warningId
    }

    /**
     * 初始化listview
     */
    private fun initListView() {
        mAdapter = WeeklyForecastAdapter(activity, weeklyList)
        listView.adapter = mAdapter
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.ivMap -> {
                val intent = Intent(activity, SelectPositionActivity::class.java)
                intent.putExtra("lat", lat)
                intent.putExtra("lng", lng)
                startActivityForResult(intent, 1004)
            }
            R.id.tvPosition -> startActivityForResult(Intent(activity, CityActivity::class.java), 1001)
            R.id.tvFact -> {
                tvTemp.text = tvFact.tag.toString()
                tvFact.setTextColor(Color.WHITE)
                tvFact.setBackgroundResource(R.drawable.bg_fact_temp_press)
                tvBody.setTextColor(0x60ffffff)
                tvBody.setBackgroundResource(R.drawable.bg_body_temp)
            }
            R.id.tvBody -> {
                tvTemp.text = tvBody.tag.toString()
                tvFact.setTextColor(0x60ffffff)
                tvFact.setBackgroundResource(R.drawable.bg_fact_temp)
                tvBody.setTextColor(Color.WHITE)
                tvBody.setBackgroundResource(R.drawable.bg_body_temp_press)
            }
            R.id.tvAqiCount, R.id.tvAqi -> {
//                val intent = Intent(activity, HAirPolutionActivity::class.java)
//                intent.putExtra(CONST.ACTIVITY_NAME, "空气质量")
//                startActivity(intent)
            }
            R.id.ivClimate -> {
                val intent = Intent(activity, WebviewActivity::class.java)
                intent.putExtra(CONST.ACTIVITY_NAME, "24节气")
                intent.putExtra(CONST.WEB_URL, "http://wx.tianqi.cn/Solar/jieqidetail")
                startActivity(intent)
            }
            R.id.tvChart, R.id.tvList -> if (listView!!.visibility == View.VISIBLE) {
                tvChart!!.setBackgroundResource(R.drawable.bg_chart_press)
                tvList.setBackgroundResource(R.drawable.bg_list)
                listView!!.visibility = View.GONE
                hScrollView2!!.visibility = View.VISIBLE
            } else {
                tvChart!!.setBackgroundResource(R.drawable.bg_chart)
                tvList.setBackgroundResource(R.drawable.bg_list_press)
                listView!!.visibility = View.VISIBLE
                hScrollView2!!.visibility = View.GONE
            }
            R.id.tvRain -> {
                val intent = Intent(activity, MinuteFallActivity::class.java)
                intent.putExtra(CONST.ACTIVITY_NAME, "分钟级降水估测")
                startActivity(intent)
            }
            R.id.clMinute -> if (llContainerRain!!.visibility == View.VISIBLE) {
                ivClose!!.setImageResource(R.drawable.iv_open)
                llContainerRain!!.visibility = View.GONE
                mapView!!.visibility = View.GONE
                ivPlay2!!.visibility = View.GONE
                hsTime!!.visibility = View.GONE
            } else {
                ivClose!!.setImageResource(R.drawable.iv_close)
                llContainerRain!!.visibility = View.VISIBLE
                mapView!!.visibility = View.VISIBLE
                ivPlay2!!.visibility = View.VISIBLE
                hsTime!!.visibility = View.VISIBLE
            }
            R.id.clHour -> if (hScrollView!!.visibility == View.VISIBLE) {
                hScrollView!!.visibility = View.GONE
                ivClose2.setImageResource(R.drawable.iv_open)
            } else {
                hScrollView!!.visibility = View.VISIBLE
                ivClose2.setImageResource(R.drawable.iv_close)
            }
            R.id.tvDisWarning -> {
                val intent = Intent(activity, HeadWarningActivity::class.java)
                val bundle = Bundle()
                bundle.putParcelableArrayList("warningList", disWarnings as ArrayList<out Parcelable?>)
                intent.putExtras(bundle)
                startActivity(intent)
            }
            R.id.tvCityWarning -> {
                val intent = Intent(activity, HeadWarningActivity::class.java)
                val bundle = Bundle()
                bundle.putParcelableArrayList("warningList", cityWarnings as ArrayList<out Parcelable?>)
                intent.putExtras(bundle)
                startActivity(intent)
            }
            R.id.tvProWarning -> {
                val intent = Intent(activity, HeadWarningActivity::class.java)
                val bundle = Bundle()
                bundle.putParcelableArrayList("warningList", proWarnings as ArrayList<out Parcelable?>)
                intent.putExtras(bundle)
                startActivity(intent)
            }
            R.id.clVideo -> {
                okHttpVideoList()
            }
            R.id.tvDivPolicy -> {
                val intent = Intent(activity, WebviewActivity::class.java)
                intent.putExtra(CONST.ACTIVITY_NAME, "各地隔离政策查询")
                intent.putExtra(CONST.WEB_URL, "http://m.heb.bendibao.com/news/gelizhengce/all.php?leavecity=&src=12379")
                startActivity(intent)
            }
            R.id.tvInfo -> {
                val intent = Intent(activity, WebviewActivity::class.java)
                intent.putExtra(CONST.ACTIVITY_NAME, "实时更新：新型冠状病毒肺炎疫情实时大数据报告")
                intent.putExtra(CONST.WEB_URL, "https://voice.baidu.com/act/newpneumonia/newpneumonia?fraz=partner&paaz=gjyj")
                startActivity(intent)
            }
            R.id.clAudio -> {
                if (!mTts!!.isSpeaking) {
                    ivAudio.setImageResource(R.drawable.audio_animation)
                    val audioAnimation = ivAudio.drawable as AnimationDrawable
                    audioAnimation.start()
                    val currentTime = sdf2.format(Date())
                    val audioText = "现在是北京时间$currentTime$weatherText"
                    mTts!!.startSpeaking(audioText, object : SynthesizerListener {
                        override fun onBufferProgress(percent: Int, p1: Int, p2: Int, p3: String?) {
                            // 合成进度
                            mPercentForBuffering = percent
                        }
                        override fun onSpeakBegin() {
                        }
                        override fun onSpeakProgress(percent: Int, p1: Int, p2: Int) {
                            // 播放进度
                            mPercentForPlaying = percent
                        }

                        override fun onEvent(eventType: Int, p1: Int, p2: Int, obj: Bundle?) {
                            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
                            // 若使用本地能力，会话id为null
//							if (SpeechEvent.EVENT_SESSION_ID == eventType) {
//								val sid = obj!!.getString(SpeechEvent.KEY_EVENT_SESSION_ID)
//							}
                        }
                        override fun onSpeakPaused() {
                        }
                        override fun onSpeakResumed() {
                        }
                        override fun onCompleted(error: SpeechError?) {
                            if (error == null) {
                                ivAudio.setImageResource(R.drawable.icon_audio)
                            }
                        }
                    })
                }else {
                    ivAudio.setImageResource(R.drawable.icon_audio)
                    mTts!!.stopSpeaking()
                }
            }
            R.id.ivPlay2 -> {
                if (mRadarThread != null && mRadarThread!!.currentState == STATE_PLAYING) {
                    mRadarThread!!.pause()
                    ivPlay2!!.setImageResource(R.drawable.iv_play2)
                } else if (mRadarThread != null && mRadarThread!!.currentState == STATE_PAUSE) {
                    mRadarThread!!.play()
                    ivPlay2!!.setImageResource(R.drawable.iv_pause2)
                } else if (mRadarThread == null) {
                    ivPlay2!!.setImageResource(R.drawable.iv_pause2)
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
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                1001 -> {
                    if (data != null) {
                        val dto: CityDto = data.getParcelableExtra("data")
                        tvPosition!!.text = dto.areaName
                        lat = dto.lat
                        lng = dto.lng
                        cityId = dto.cityId
                        if (lng == 0.0 || lat == 0.0) {
                            getLatlngByCityid(cityId)
                        } else {
                            isFusion = false
                            refresh()
                        }
                    }
                }
                1004 -> {
                    if (data != null) {
                        val bundle = data.extras
                        if (bundle != null) {
                            lat = bundle.getDouble("lat", lat)
                            lng = bundle.getDouble("lng", lat)
                            val position = bundle.getString("position")
                            Log.e("position", position)
                            if (!TextUtils.isEmpty(position)) {
                                tvPosition.text = position
                            }
                            isFusion = true
                            refresh()
                        }
                    }
                }
            }
        }
    }

    private fun okHttpBody(cityId: String, l1: String, l2: String, l11: String) {
        Thread {
            val url = "http://decision-admin.tianqi.cn/home/work2019/getBodyTem?cityId=$cityId&l1=$l1&l2=$l2&l11=$l11"
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    if (!isAdded) {
                        return
                    }
                    val result = response.body!!.string()
                    activity!!.runOnUiThread {
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                val obj = JSONObject(result)
                                //实况信息
                                if (!obj.isNull("l12")) {
                                    val bodyTemp = obj.getString("l12")
                                    tvBody.tag = "$bodyTemp"
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

    private fun okHttpHourRain() {
        Thread {
            val url = "http://api.caiyunapp.com/v2/HyTVV5YAkoxlQ3Zd/$lng,$lat/forecast"
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
                                val `object` = JSONObject(result)
                                if (!`object`.isNull("result")) {
                                    val obj = `object`.getJSONObject("result")
                                    if (!obj.isNull("minutely")) {
                                        val objMin = obj.getJSONObject("minutely")
                                        if (!objMin.isNull("description")) {
                                            val rain = objMin.getString("description")
                                            if (!TextUtils.isEmpty(rain)) {
                                                tvRain!!.text = rain.replace(getString(R.string.little_caiyun), "")
                                            }
                                        }
                                        if (!objMin.isNull("precipitation_2h")) {
                                            val array = objMin.getJSONArray("precipitation_2h")
                                            val size = array.length()
                                            val minuteList: MutableList<WeatherDto> = ArrayList()
                                            for (i in 0 until size) {
                                                val dto = WeatherDto()
                                                dto.minuteFall = array.getDouble(i).toFloat()
                                                minuteList.add(dto)
                                            }
                                            val minuteFallView = MinuteFallView(activity)
                                            minuteFallView.setData(minuteList, tvRain!!.text.toString())
                                            llContainerRain!!.removeAllViews()
                                            llContainerRain!!.addView(minuteFallView, CommonUtil.widthPixels(activity), CommonUtil.dip2px(activity, 150f).toInt())
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

    /**
     * 获取预警信息
     */
    private fun okHttpWarning(url: String, warningId: String?) {
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
                                val `object` = JSONObject(result)
                                if (`object` != null) {
                                    if (!`object`.isNull("data")) {
                                        disWarnings.clear()
                                        cityWarnings.clear()
                                        proWarnings.clear()
                                        val jsonArray = `object`.getJSONArray("data")
                                        for (i in 0 until jsonArray.length()) {
                                            val tempArray = jsonArray.getJSONArray(i)
                                            val dto = WarningDto()
                                            dto.html = tempArray.optString(1)
                                            val array = dto.html.split("-").toTypedArray()
                                            val item0 = array[0]
                                            val item1 = array[1]
                                            val item2 = array[2]
                                            dto.provinceId = item0.substring(0, 2)
                                            dto.type = item2.substring(0, 5)
                                            dto.color = item2.substring(5, 7)
                                            dto.time = item1
                                            dto.lng = tempArray.getDouble(2)
                                            dto.lat = tempArray.getDouble(3)
                                            dto.name = tempArray.optString(0)
                                            if (!dto.name.contains("解除")) {
                                                if (!TextUtils.isEmpty(warningId)) {
                                                    when {
                                                        TextUtils.equals(warningId, item0) -> {
                                                            disWarnings.add(dto)
                                                        }
                                                        TextUtils.equals(warningId!!.substring(0, 4) + "00", item0) -> {
                                                            cityWarnings.add(dto)
                                                        }
                                                        TextUtils.equals(warningId!!.substring(0, 2) + "0000", item0) -> {
                                                            proWarnings.add(dto)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (disWarnings.size > 0) {
                                            tvDisWarning.text = "本地预警${disWarnings.size}条"
                                            tvDisWarning.visibility = View.VISIBLE
                                        } else {
                                            tvDisWarning.visibility = View.GONE
                                        }
                                        if (cityWarnings.size > 0) {
                                            tvCityWarning.text = "市级预警${cityWarnings.size}条"
                                            tvCityWarning.visibility = View.VISIBLE
                                        } else {
                                            tvCityWarning.visibility = View.GONE
                                        }
                                        if (proWarnings.size > 0) {
                                            tvProWarning.text = "省级预警${proWarnings.size}条"
                                            tvProWarning.visibility = View.VISIBLE
                                        } else {
                                            tvProWarning.visibility = View.GONE
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

    private fun okHttpVideoList() {
        Thread {
            val url = "https://decision-admin.tianqi.cn/Home/work2019/hlg_getVideos"
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
                                var type: String? = null
                                if (!obj.isNull("type")) {
                                    type = obj.getString("type")
                                }
                                if (!obj.isNull("l")) {
                                    val array = obj.getJSONArray("l")
                                    if (array.length() > 0) {
                                        val itemObj = array.getJSONObject(0)
                                        val title = itemObj.getString("l1")
                                        val dataUrl = itemObj.getString("l2")

                                        val intent = Intent(activity, WebviewActivity::class.java)
                                        intent.putExtra(CONST.ACTIVITY_NAME, title)
                                        intent.putExtra(CONST.WEB_URL, dataUrl)
                                        startActivity(intent)
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

    private fun getLatlngByCityid(cityId: String) {
        Thread {
            WeatherAPI.getWeather2(activity, cityId, Language.ZH_CN, object : AsyncResponseHandler() {
                override fun onComplete(content: Weather) {
                    super.onComplete(content)
                    activity!!.runOnUiThread {
                        val result = content.toString()
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                val obj = JSONObject(result)
                                if (!obj.isNull("c")) {
                                    val c = obj.getJSONObject("c")
                                    lng = c.getDouble("c13")
                                    lat = c.getDouble("c14")
                                    isFusion = false
                                    refresh()
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                override fun onError(error: Throwable, content: String) {
                    super.onError(error, content)
                }
            })
        }.start()
    }

    private val dataList: ArrayList<MinuteFallDto> = ArrayList()
    private val images: ArrayList<MinuteFallDto> = ArrayList()
    private var mOverlay: GroundOverlay? = null
    private var mRadarManager: CaiyunManager? = null
    private var mRadarThread: RadarThread? = null
    private val HANDLER_SHOW_RADAR = 1
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
                    activity!!.runOnUiThread {
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
        activity!!.runOnUiThread {
            llContainerTime.removeAllViews()
            for (i in 0 until images.size) {
                val tv = TextView(activity)
                tv.setPadding(10, 10, 10, 10)
                tv.setBackgroundColor(ContextCompat.getColor(activity!!, R.color.colorPrimary))
                tv.gravity = Gravity.CENTER
                tv.setTextColor(Color.WHITE)
                tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
                val dto = images[i]
                val value = dto.time.toString() + "000"
                val date = Date(value.toLong())
                tv.text = sdf6.format(date)
                llContainerTime.addView(tv)
            }
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

    private fun changeProgress(time: Long, progress: Int, max: Int) {
        val value = time.toString() + "000"
        val date = Date(value.toLong())
        val text = sdf6.format(date)

        for (i in 0 until llContainerTime.childCount) {
            val tv = llContainerTime.getChildAt(i) as TextView
            if (TextUtils.equals(text, tv.text)) {
                tv.setBackgroundColor(ContextCompat.getColor(activity!!, R.color.colorPrimary))
            } else {
                tv.setBackgroundColor(0xff9DC7FA.toInt())
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

}