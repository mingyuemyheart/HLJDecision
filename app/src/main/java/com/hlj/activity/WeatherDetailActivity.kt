package com.hlj.activity

/**
 * 天气详情
 */

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
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.View.OnClickListener
import android.widget.TextView
import android.widget.Toast
import cn.com.weather.api.WeatherAPI
import cn.com.weather.beans.Weather
import cn.com.weather.constants.Constants
import cn.com.weather.listener.AsyncResponseHandler
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
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
import com.hlj.view.CubicView2
import com.hlj.view.MinuteFallView
import com.hlj.view.WeeklyView
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechError
import com.iflytek.cloud.SpeechSynthesizer
import com.iflytek.cloud.SynthesizerListener
import kotlinx.android.synthetic.main.activity_weather_detail.*
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

class WeatherDetailActivity : BaseActivity(), OnClickListener, CaiyunManager.RadarListener {

    private var mAdapter: WeeklyForecastAdapter? = null
    private val weeklyList: MutableList<WeatherDto> = ArrayList()
    private val sdf1 = SimpleDateFormat("HH", Locale.CHINA)
    private val sdf2 = SimpleDateFormat("yyyy年MM月dd日 HH时mm分", Locale.CHINA)
    private val sdf3 = SimpleDateFormat("yyyyMMdd", Locale.CHINA)
    private val sdf4 = SimpleDateFormat("yyyyMMddHHmm", Locale.CHINA)
    private val sdf5 = SimpleDateFormat("yyyyMMddHH", Locale.CHINA)
    private val sdf6 = SimpleDateFormat("HH:mm", Locale.CHINA)
    private var hour = 0
    private val disWarnings: MutableList<WarningDto?> = ArrayList()
    private val cityWarnings: MutableList<WarningDto?> = ArrayList()
    private val proWarnings: MutableList<WarningDto?> = ArrayList()
    private val aqiList: ArrayList<WeatherDto> = ArrayList()

    //语音播报
    private var mTts : SpeechSynthesizer? = null// 语音合成对象
    private var voicer = "xiaoyan"// 默认发音人
    private var mPercentForBuffering = 0// 缓冲进度
    private var mPercentForPlaying = 0// 播放进度
    private var mEngineType = SpeechConstant.TYPE_CLOUD// 引擎类型
    private var mToast : Toast? = null
    private var weatherText = ""
    private var cityName : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_detail)
        initRefreshLayout()
        initMap(savedInstanceState)
        initWidget()
        initListView()
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
        llBack.setOnClickListener(this)
        //解决scrollView嵌套listview，动态计算listview高度后，自动滚动到屏幕底部
        tvPosition!!.isFocusable = true
        tvPosition!!.isFocusableInTouchMode = true
        tvPosition!!.requestFocus()
        tvPosition!!.setOnClickListener(this)
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
        clHour.setOnClickListener(this)
        tvInfo.setOnClickListener(this)
        ivClimate.setOnClickListener(this)
        clAudio.setOnClickListener(this)
        ivPlay2!!.setOnClickListener(this)
        hour = sdf1.format(Date()).toInt()
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

        refresh()
    }

    private fun refresh() {
        if (intent.hasExtra("data")) {
            val data: CityDto = intent.extras.getParcelable("data")
            tvTitle.text = data.areaName
            cityName = data.areaName
            tvPosition.text = data.areaName
            addMarkerToMap(LatLng(data.lat, data.lng))
//            if (data.provinceName.contains(data.cityName)) {
//                okHttpInfo(data.cityName, data.areaName)
//            } else {
//                okHttpInfo(data.provinceName, data.cityName)
//            }
            OkHttpHourRain(data.lng, data.lat)
            getWeatherInfo(data.cityId)
        }

        initSpeech()
    }

    private fun addMarkerToMap(latLng: LatLng) {
        val options = MarkerOptions()
        options.position(latLng)
        options.anchor(0.5f, 0.5f)
        val bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(resources, R.drawable.icon_map_location),
                CommonUtil.dip2px(this, 16f).toInt(), CommonUtil.dip2px(this, 24f).toInt())
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
        mTts = SpeechSynthesizer.createSynthesizer(this) { code ->

        }
        mToast = Toast.makeText(this,"", Toast.LENGTH_SHORT)

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
            mRadarManager = CaiyunManager(this)
            okHttpMinuteImage()
        }
    }


    private val dataList: ArrayList<MinuteFallDto> = ArrayList()
    private val images: ArrayList<MinuteFallDto> = ArrayList()
    private var mOverlay: GroundOverlay? = null
    private var mRadarManager: CaiyunManager? = null
    private var mRadarThread: RadarThread? = null
    private val HANDLER_SHOW_RADAR = 1
    private fun okHttpMinuteImage() {
        Thread(Runnable {
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
        }).start()
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
            llContainerTime.removeAllViews()
            for (i in 0 until images.size) {
                val tv = TextView(this)
                tv.setPadding(10, 10, 10, 10)
                tv.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
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
                tv.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
            } else {
                tv.setBackgroundColor(0xff9DC7FA.toInt())
            }
        }
    }

    /**
     * 获取疫情
     */
    private fun okHttpInfo(pro: String, city: String) {
        val url = String.format("http://warn-wx.tianqi.cn/Test/getwhqydata?pro=%s&city=%s&appid=%s", pro, city, CONST.APPID)
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
                            var proCount = ""
                            if (!obj.isNull("total_pro")) {
                                val proObj = obj.getJSONObject("total_pro")
                                if (!proObj.isNull("confirm")) {
                                    proCount = proObj.getString("confirm")
                                }
                            }
                            var cityCount = ""
                            if (!obj.isNull("total")) {
                                val cityObj = obj.getJSONObject("total")
                                if (!cityObj.isNull("confirm")) {
                                    cityCount = cityObj.getString("confirm")
                                }
                            }
                            tvInfo!!.text = String.format("今日疫情\n%s累计确诊%s例\n%s累计确诊%s例", city, cityCount, pro, proCount)
                            tvInfo!!.visibility = View.VISIBLE
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        })
    }

    /**
     * 获取天气数据
     */
    private fun getWeatherInfo(lng: Double, lat: Double) {
        OkHttpXiangJiAqi(lat, lng)
    }

    /**
     * 请求象辑aqi
     */
    private fun OkHttpXiangJiAqi(lat: Double, lng: Double) {
        Thread(Runnable {
            val timestamp = Date().time
            val start1 = sdf5.format(timestamp)
            val end1 = sdf5.format(timestamp + 1000 * 60 * 60 * 24)
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

                            if (!obj.isNull("series")) {
                                aqiList.clear()
                                val array = obj.getJSONArray("series")
                                for (i in 0 until array.length()) {
                                    val data = WeatherDto()
                                    data.aqi = array[i].toString()
                                    aqiList.add(data)
                                }
                            }
                        } catch (e1: JSONException) {
                            e1.printStackTrace()
                        }
                    }
                }
            })

            WeatherAPI.getGeo(this, lng.toString(), lat.toString(), object : AsyncResponseHandler() {
                override fun onComplete(content: JSONObject) {
                    super.onComplete(content)
                    if (!content.isNull("geo")) {
                        try {
                            val geoObj = content.getJSONObject("geo")
                            if (!geoObj.isNull("id")) {
                                val cityId = geoObj.getString("id")
                                if (!TextUtils.isEmpty(cityId)) {
                                    getWeatherInfo(cityId)
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }

            })
        }).start()
    }

    private fun getWeatherInfo(cityId: String) {
        Thread(Runnable {
            val url = String.format("http://api.weatherdt.com/common/?area=%s&type=forecast|observe|alarm|air|rise&key=eca9a6c9ee6fafe74ac6bc81f577a680", cityId)
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
                                                if (!TextUtils.isEmpty(weatherCode) && !TextUtils.equals(weatherCode, "?") && !TextUtils.equals(weatherCode, "null")) {
                                                    try {
                                                        tvPhe!!.text = getString(WeatherUtil.getWeatherId(Integer.valueOf(weatherCode)))
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }
                                            if (!o.isNull("002")) {
                                                val factTemp = o.getString("002")
                                                tvTemp!!.text = "$factTemp°"
                                                tvFact.tag = "$factTemp°"
                                            }
                                            if (!o.isNull("002")) {
                                                val bodyTemp = o.getString("002")
                                                tvBody.tag = "$bodyTemp°"
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
                                                    val diviTime = sdf6.parse(setTime).time-sdf6.parse(riseTime).time
                                                    val hour = diviTime/(1000*60*60)
                                                    val minute = (diviTime-hour*1000*60*60)/(1000*60)
                                                    tvRiseTime.text = "日出时间：$riseTime\n日落时间：$setTime\n日照时间：$hour:$minute"
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
                                                        tvAqiCount!!.setBackgroundResource(WeatherUtil.getAqiIcon(Integer.valueOf(aqi)))
                                                        tvAqi.text = "空气质量 "+WeatherUtil.getAqi(this@WeatherDetailActivity, Integer.valueOf(aqi))
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (!obj.isNull("forecast")) {
                                    val forecast = obj.getJSONObject("forecast")

                                    //逐小时预报信息
                                    if (!forecast.isNull("1h")) {
                                        val `object` = forecast.getJSONObject("1h")
                                        if (!`object`.isNull(cityId)) {
                                            val object1 = `object`.getJSONObject(cityId)
                                            if (!object1.isNull("1001001")) {
                                                val array = object1.getJSONArray("1001001")
                                                val hourlyList: MutableList<WeatherDto> = ArrayList()
                                                var length = array.length()
                                                if (length >= 24) {
                                                    length = 24
                                                }
                                                for (i in 0 until length) {
                                                    val itemObj = array.getJSONObject(i)
                                                    val dto = WeatherDto()
                                                    dto.hourlyTime = itemObj.getString("000")
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
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                    hourlyList.add(dto)
                                                }

                                                //逐小时预报信息
                                                val cubicView = CubicView2(this@WeatherDetailActivity)
                                                cubicView.setData(hourlyList)
                                                llContainerHour!!.removeAllViews()
                                                llContainerHour!!.addView(cubicView, CommonUtil.widthPixels(this@WeatherDetailActivity) * 2, CommonUtil.dip2px(this@WeatherDetailActivity, 300f).toInt())
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
                                            var foreDate: Long = 0
                                            var currentDate: Long = 0
                                            try {
                                                val fTime = sdf3.format(sdf4.parse(f0))
                                                foreDate = sdf3.parse(fTime).time
                                                currentDate = sdf3.parse(sdf3.format(Date())).time
                                            } catch (e: ParseException) {
                                                e.printStackTrace()
                                            }
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
                                                    weeklyList.add(dto)
                                                    if (i == 0) {
                                                        weatherText = ""
                                                        var pheText : String? = null
                                                        var temperatureText : String? = null
                                                        var windDirText : String? = null
                                                        var windForceText : String? = null
                                                        pheText = if (dto.lowPheCode == dto.highPheCode) {
                                                            getString(WeatherUtil.getWeatherId(dto.lowPheCode))
                                                        }else {
                                                            getString(WeatherUtil.getWeatherId(dto.highPheCode))+"转"+getString(WeatherUtil.getWeatherId(dto.lowPheCode))
                                                        }
                                                        temperatureText = "最高气温"+dto.highTemp+"摄氏度，"+"最低气温"+dto.lowTemp+"摄氏度"
                                                        windDirText = getString(WeatherUtil.getWindDirection(dto.windDir))
                                                        windForceText = WeatherUtil.getDayWindForce(dto.windForce)
                                                        weatherText = "，"+getString(R.string.app_name)+"现在为您播报"+cityName+"天气预报。今天白天到今天夜间，"+pheText+"，"+temperatureText+"，"+windDirText+windForceText


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
                                                        if (!TextUtils.isEmpty(tvAqiCount.text.toString()) && !TextUtils.equals(tvAqiCount.text.toString(), "?") && !TextUtils.equals(tvAqiCount.text.toString(), "null")) {
                                                            val value: Int = tvAqiCount.text.toString().toInt()
                                                            tvAqi1.text = CommonUtil.getAqiDes(this@WeatherDetailActivity, value)
                                                            tvAqi1.setBackgroundResource(CommonUtil.getCornerBackground(value))
                                                        }
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
                                                        if (aqiList.size > 1) {
                                                            if (!TextUtils.isEmpty(aqiList[1].aqi) && !TextUtils.equals(aqiList[1].aqi, "?") && !TextUtils.equals(aqiList[1].aqi, "null")) {
                                                                val value: Int = aqiList[1].aqi.toInt()
                                                                tvAqi2.text = CommonUtil.getAqiDes(this@WeatherDetailActivity, value)
                                                                tvAqi2.setBackgroundResource(CommonUtil.getCornerBackground(value))
                                                            }
                                                        }
                                                    }
                                                }

                                                //一周预报列表
                                                if (mAdapter != null) {
                                                    mAdapter!!.foreDate = foreDate
                                                    mAdapter!!.currentDate = currentDate
                                                    mAdapter!!.notifyDataSetChanged()
                                                }

                                                //一周预报曲线
                                                val weeklyView = WeeklyView(this@WeatherDetailActivity)
                                                weeklyView.setData(weeklyList, foreDate, currentDate)
                                                llContainerFifteen!!.removeAllViews()
                                                llContainerFifteen!!.addView(weeklyView, CommonUtil.widthPixels(this@WeatherDetailActivity) * 2, CommonUtil.dip2px(this@WeatherDetailActivity, 320f).toInt())
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
                        runOnUiThread { //获取预警信息
                            val warningId = queryWarningIdByCityId(cityId)
                            if (!TextUtils.isEmpty(warningId)) {
                                if (warningId!!.startsWith("23")) {
                                    val city = warningId.substring(0, 4)+"00"
                                    val ids = "${warningId},230000,${city}"
                                    setPushTags(ids)
                                }
                                OkHttpWarning("http://decision-admin.tianqi.cn/Home/extra/getwarns?order=0&areaid=" + warningId!!.substring(0, 2), warningId)
                            }
                        }
                    }
                }
            })
        }).start()
    }

    /**
     * 设置umeng推送的tags
     * @param warningId
     */
    private fun setPushTags(warningId: String?) {
        var tags = warningId
        val sharedPreferences = this.getSharedPreferences("RESERVE_CITY", Context.MODE_PRIVATE)
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
        val dbManager = DBManager(this@WeatherDetailActivity)
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
     * 获取预警信息
     */
    private fun OkHttpWarning(url: String, warningId: String?) {
        Thread(Runnable {
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
                                                        TextUtils.equals(warningId.substring(0, 2) + "0000", item0) -> {
                                                            proWarnings.add(dto)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (disWarnings.size > 0) {
                                            tvDisWarning.text = "本地预警${disWarnings.size}条"
                                            tvDisWarning.visibility = View.VISIBLE
                                        }
                                        if (cityWarnings.size > 0) {
                                            tvCityWarning.text = "市级预警${cityWarnings.size}条"
                                            tvCityWarning.visibility = View.VISIBLE
                                        }
                                        if (proWarnings.size > 0) {
                                            tvProWarning.text = "省级预警${proWarnings.size}条"
                                            tvProWarning.visibility = View.VISIBLE
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
        }).start()
    }

    /**
     * 初始化listview
     */
    private fun initListView() {
        mAdapter = WeeklyForecastAdapter(this, weeklyList)
        listView.adapter = mAdapter
    }

    /**
     * 异步加载一小时内降雨、或降雪信息
     * @param lng
     * @param lat
     */
    private fun OkHttpHourRain(lng: Double, lat: Double) {
        val url = String.format("http://api.caiyunapp.com/v2/HyTVV5YAkoxlQ3Zd/%s,%s/forecast", lng, lat)
        Thread(Runnable {
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
                                            val minuteFallView = MinuteFallView(this@WeatherDetailActivity)
                                            minuteFallView.setData(minuteList, tvRain!!.text.toString())
                                            llContainerRain!!.removeAllViews()
                                            llContainerRain!!.addView(minuteFallView, CommonUtil.widthPixels(this@WeatherDetailActivity), CommonUtil.dip2px(this@WeatherDetailActivity, 150f).toInt())
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
        }).start()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.llBack -> finish()
            R.id.tvPosition -> startActivityForResult(Intent(this, CityActivity::class.java), 1001)
            R.id.tvFact -> {
                tvTemp.text = tvFact.tag.toString() + ""
                tvFact.setTextColor(Color.WHITE)
                tvFact.setBackgroundResource(R.drawable.bg_fact_temp_press)
                tvBody.setTextColor(0x60ffffff)
                tvBody.setBackgroundResource(R.drawable.bg_body_temp)
            }
            R.id.tvBody -> {
                tvTemp.text = tvBody.tag.toString() + ""
                tvFact.setTextColor(0x60ffffff)
                tvFact.setBackgroundResource(R.drawable.bg_fact_temp)
                tvBody.setTextColor(Color.WHITE)
                tvBody.setBackgroundResource(R.drawable.bg_body_temp_press)
            }
            R.id.tvAqiCount, R.id.tvAqi -> {
                val intent = Intent(this, AirActivity::class.java)
                intent.putExtra(CONST.ACTIVITY_NAME, "空气质量")
                startActivity(intent)
            }
            R.id.ivClimate -> {
                val intent = Intent(this, WebviewActivity::class.java)
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
                val intent = Intent(this, HeadWarningActivity::class.java)
                val bundle = Bundle()
                bundle.putParcelableArrayList("warningList", disWarnings as ArrayList<out Parcelable?>)
                intent.putExtras(bundle)
                startActivity(intent)
            }
            R.id.tvCityWarning -> {
                val intent = Intent(this, HeadWarningActivity::class.java)
                val bundle = Bundle()
                bundle.putParcelableArrayList("warningList", cityWarnings as ArrayList<out Parcelable?>)
                intent.putExtras(bundle)
                startActivity(intent)
            }
            R.id.tvProWarning -> {
                val intent = Intent(this, HeadWarningActivity::class.java)
                val bundle = Bundle()
                bundle.putParcelableArrayList("warningList", proWarnings as ArrayList<out Parcelable?>)
                intent.putExtras(bundle)
                startActivity(intent)
            }
            R.id.tvInfo -> {
                val intent = Intent(this, WebviewActivity::class.java)
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
                    val audioText = "亲爱的用户您好，现在是北京时间$currentTime$weatherText，感谢您的收听！"
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
                            } else {
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
                1001 -> if (data != null) {
                    val dto: CityDto = data.getParcelableExtra("data")
                    tvPosition!!.text = dto.areaName
                    if (dto.lng == 0.0 || dto.lat == 0.0) {
                        getLatlngByCityid(dto.cityId)
                    } else {
                        OkHttpHourRain(dto.lng, dto.lat)
                        getWeatherInfo(dto.lng, dto.lat)
                    }
                }
            }
        }
    }

    private fun getLatlngByCityid(cityId: String) {
        Thread(Runnable {
            WeatherAPI.getWeather2(this, cityId, Constants.Language.ZH_CN, object : AsyncResponseHandler() {
                override fun onComplete(content: Weather) {
                    super.onComplete(content)
                    runOnUiThread {
                        val result = content.toString()
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                val obj = JSONObject(result)
                                if (!obj.isNull("c")) {
                                    val c = obj.getJSONObject("c")
                                    val lng = c.getDouble("c13")
                                    val lat = c.getDouble("c14")
                                    OkHttpHourRain(lng, lat)
                                    getWeatherInfo(lng, lat)
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

            })
        }).start()
    }

}