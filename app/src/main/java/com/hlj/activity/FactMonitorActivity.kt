package com.hlj.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
import com.hlj.adapter.FactMonitorAdapter
import com.hlj.adapter.FactTimeAdapter
import com.hlj.common.CONST
import com.hlj.dto.AgriDto
import com.hlj.dto.FactDto
import com.hlj.fragment.FactCheckFragment
import com.hlj.utils.CommonUtil
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.activity_fact_monitor.*
import kotlinx.android.synthetic.main.dialog_fact_history.view.*
import kotlinx.android.synthetic.main.layout_fact_value.view.*
import kotlinx.android.synthetic.main.layout_title.*
import net.tsz.afinal.FinalBitmap
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException
import java.util.*

/**
 * 自动站实况监测
 */
class FactMonitorActivity : BaseFragmentActivity(), View.OnClickListener, AMap.OnCameraChangeListener {

    private var aMap: AMap? = null //高德地图
    private var zoom = 5.5f
    private var factAdapter: FactMonitorAdapter? = null
    private val factList: MutableList<FactDto> = ArrayList()
    private val polygons: MutableList<Polygon> = ArrayList() //图层数据
    private val texts: MutableList<Text> = ArrayList() //等值线数值
    private val polylines: MutableList<Polyline> = ArrayList() //边界线
    private val cityTexts: MutableList<Marker> = ArrayList() //市县名称
    private val autoTexts: MutableList<Marker> = ArrayList() //自动站
    private val autoTextsH: MutableList<Marker> = ArrayList() //自动站
    private val cityInfos: MutableList<FactDto> = ArrayList() //城市信息
    private val timeList: MutableList<FactDto> = ArrayList() //时间列表
    private val realDatas: MutableList<FactDto?> = ArrayList() //全省站点列表

    private var stationName = ""
    private var area = ""
    private var `val` = ""
    private var timeString = ""
    private var childId = ""
    private val fragments: MutableList<Fragment> = ArrayList()
    private val layerMap: MutableMap<String, String?> = HashMap()
    private var factOverlay: GroundOverlay? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fact_monitor)
        initAmap(savedInstanceState)
        initWidget()
        initListView()
    }

    /**
     * 初始化高德地图
     */
    private fun initAmap(bundle: Bundle?) {
        mapView.visibility = View.VISIBLE
        mapView.onCreate(bundle)
        if (aMap == null) {
            aMap = mapView.map
        }
        aMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(CONST.guizhouLatLng, zoom))
        aMap!!.uiSettings.isMyLocationButtonEnabled = false // 设置默认定位按钮是否显示
        aMap!!.uiSettings.isZoomControlsEnabled = false
        aMap!!.uiSettings.isRotateGesturesEnabled = false
        aMap!!.showMapText(false)
        aMap!!.setOnCameraChangeListener(this)
        tvMapNumber.text = aMap!!.mapContentApprovalNumber
        aMap!!.setOnMapTouchListener { arg0 ->
            if (scrollView != null) {
                if (arg0.action == MotionEvent.ACTION_UP) {
                    scrollView!!.requestDisallowInterceptTouchEvent(false)
                } else {
                    scrollView!!.requestDisallowInterceptTouchEvent(true)
                }
            }
        }
    }

    private fun initWidget() {
        llBack.setOnClickListener(this)
        tvDetail.setOnClickListener(this)
        tvHistory.setOnClickListener(this)

        val title = intent.getStringExtra(CONST.ACTIVITY_NAME)
        if (!TextUtils.isEmpty(title)) {
            tvTitle.text = title
        }
        okHttpLayer()
        val columnId = intent.getStringExtra(CONST.COLUMN_ID)
        CommonUtil.submitClickCount(columnId, title)
    }

    private fun initListView() {
        factAdapter = FactMonitorAdapter(this, factList)
        listView.adapter = factAdapter
    }

    /**
     * 获取图层信息
     */
    private fun okHttpLayer() {
        val url = intent.getStringExtra(CONST.WEB_URL)
        if (TextUtils.isEmpty(url)) {
            return
        }
        Log.e("okHttpLayer", url)
        //        final String url = "https://decision-admin.tianqi.cn/Home/work2019/getHljSKImages";
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
                        layerMap.clear()
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                val obj = JSONObject(result)
                                val iterator = obj.keys()
                                while (iterator.hasNext()) {
                                    val key = iterator.next()
                                    val value = obj.getString(key)
                                    layerMap[key] = value
                                }
                                if (layerMap.isNotEmpty()) {
                                    addColumn()
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

    /**
     * 添加子栏目
     */
    private fun addColumn() {
        if (intent.hasExtra("data")) {
            val data: AgriDto = intent.getParcelableExtra("data")
            if (data != null) {
                llContainer!!.removeAllViews()
                llContainer1.removeAllViews()
                llContainer2.removeAllViews()
                val size = data.child.size
                for (i in 0 until size) {
                    val dto = data.child[i]
                    val tvName = TextView(this)
                    tvName.gravity = Gravity.CENTER
                    tvName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
                    tvName.setPadding(0, CommonUtil.dip2px(this, 10f).toInt(), 0, CommonUtil.dip2px(this, 10f).toInt())
                    tvName.maxLines = 1
                    tvName.text = dto.name
                    val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    params.width = CommonUtil.widthPixels(this)/size
                    tvName.layoutParams = params
                    llContainer!!.addView(tvName)

                    val tvBar = TextView(this)
                    tvBar.gravity = Gravity.CENTER
                    tvBar.setPadding(CommonUtil.dip2px(this, 10f).toInt(), 0, CommonUtil.dip2px(this, 10f).toInt(), 0)
                    val params1 = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    params1.setMargins(CommonUtil.dip2px(this, 10f).toInt(), 0, CommonUtil.dip2px(this, 10f).toInt(), 0)
                    params1.weight = 1f
                    params1.height = CommonUtil.dip2px(this, 2f).toInt()
                    params1.gravity = Gravity.CENTER
                    tvBar.layoutParams = params1
                    llContainer1.addView(tvBar)

                    if (i == 0) {
                        tvName.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
                        tvBar.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    } else {
                        tvName.setTextColor(ContextCompat.getColor(this, R.color.text_color3))
                        tvBar.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent))
                    }

                    val llItem = LinearLayout(this)
                    llItem.tag = tvName.text.toString()
                    llItem.orientation = LinearLayout.VERTICAL
                    llItem.setPadding(CommonUtil.dip2px(this, 1f).toInt(), CommonUtil.dip2px(this, 1f).toInt(), CommonUtil.dip2px(this, 1f).toInt(), CommonUtil.dip2px(this, 1f).toInt())
                    llItem.gravity = Gravity.CENTER
                    llItem.setBackgroundColor(ContextCompat.getColor(this, R.color.title_bg))
                    val params2 = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    params2.width = CommonUtil.widthPixels(this)/size
                    llItem.visibility = View.GONE
                    llItem.layoutParams = params2

                    for (j in 0 until dto.child.size) {
                        val item = dto.child[j]
                        val tvItem = TextView(this)
                        tvItem.gravity = Gravity.CENTER
                        tvItem.setPadding(0, 15, 0, 15)
                        tvItem.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
                        tvItem.maxLines = 1
                        tvItem.text = item.name
                        tvItem.tag = item.id
                        if (j == 0) {
                            tvItem.setTextColor(Color.WHITE)
                            tvItem.setBackgroundColor(Color.TRANSPARENT)
                        } else {
                            tvItem.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                            tvItem.setBackgroundColor(Color.WHITE)
                        }
                        llItem.addView(tvItem)

                        if (i == 0 && j == 0) {
                            childId = item.id
                            okHttpFact("")
                        }

                        tvItem.setOnClickListener { arg0 ->
                            val itemTag = arg0.tag.toString()
                            for (m in 0 until llItem.childCount) {
                                val name = llItem.getChildAt(m) as TextView
                                if (TextUtils.equals(name.tag.toString(), itemTag)) {
                                    name.setTextColor(Color.WHITE)
                                    name.setBackgroundColor(Color.TRANSPARENT)
                                    childId = itemTag
                                    if (name.text.startsWith("任意时段")) {
                                        viewPager!!.visibility = View.VISIBLE
                                        scrollView!!.visibility = View.GONE
                                        if (llContainer2 != null) {
                                            for (n in 0 until llContainer2.childCount) {
                                                val ll = llContainer2.getChildAt(n) as LinearLayout
                                                ll.visibility = View.INVISIBLE
                                            }
                                        }
                                        initViewPager()
                                    } else {
                                        if (viewPager!!.visibility == View.VISIBLE) {
                                            viewPager!!.visibility = View.GONE
                                            scrollView!!.visibility = View.VISIBLE
                                        }
                                        okHttpFact("")
                                    }
                                } else {
                                    name.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                                    name.setBackgroundColor(Color.WHITE)
                                }
                            }
                            llItem.visibility = View.GONE
                        }
                    }
                    llContainer2.addView(llItem)

                    tvName.setOnClickListener { arg0 ->
                        if (llContainer != null) {
                            for (j in 0 until llContainer!!.childCount) {
                                val name = llContainer!!.getChildAt(j) as TextView
                                val bar = llContainer1.getChildAt(j) as TextView
                                if (TextUtils.equals(tvName.text.toString(), name.text.toString())) {
                                    name.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
                                    bar.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                                    for (n in 0 until llContainer2.childCount) {
                                        val ll = llContainer2.getChildAt(n) as LinearLayout
                                        if (TextUtils.equals(ll.tag.toString(), tvName.text.toString())) {
                                            ll.visibility = View.VISIBLE
                                        } else {
                                            ll.visibility = View.INVISIBLE
                                        }
                                    }
                                } else {
                                    name.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                                    bar.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取实况信息
     */
    private fun okHttpFact(timeParams: String) {
        if (TextUtils.isEmpty(childId)) {
            return
        }
        progressBar!!.visibility = View.VISIBLE
        Thread {
            try {
                val obj = JSONObject(layerMap[childId])
                val url = obj.getString("dataurl") + timeParams
                Log.e("urlurl", url)
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
                                    if (!obj.isNull("zh")) {
                                        val itemObj = obj.getJSONObject("zh")
                                        if (!itemObj.isNull("stationName")) {
                                            tv3.text = itemObj.getString("stationName")
                                        }
                                        if (!itemObj.isNull("area")) {
                                            tv2.text = itemObj.getString("area")
                                        }
                                        if (!itemObj.isNull("val")) {
                                            tv1!!.text = itemObj.getString("val")
                                        }
                                    }
                                    if (!obj.isNull("title")) {
                                        tvLayerName!!.text = obj.getString("title")
                                        tvLayerName!!.visibility = View.VISIBLE
                                    }
                                    if (!obj.isNull("cutlineUrl")) {
                                        val finalBitmap = FinalBitmap.create(this@FactMonitorActivity)
                                        finalBitmap.display(ivChart, obj.getString("cutlineUrl"), null, 0)
                                    }
                                    if (!obj.isNull("zx")) {
                                        tvIntro.text = obj.getString("zx")
                                    }
                                    if (!obj.isNull("t")) {
                                        cityInfos.clear()
                                        val array = obj.getJSONArray("t")
                                        for (i in 0 until array.length()) {
                                            val f = FactDto()
                                            val o = array.getJSONObject(i)
                                            if (!o.isNull("name")) {
                                                f.name = o.getString("name")
                                                f.stationName = o.getString("name")
                                            }
                                            if (!o.isNull("lon")) {
                                                f.lng = o.getString("lon").toDouble()
                                            }
                                            if (!o.isNull("lat")) {
                                                f.lat = o.getString("lat").toDouble()
                                            }
                                            if (!o.isNull("value")) {
                                                f.`val` = o.getString("value").toDouble()
                                            }
                                            if (!o.isNull("val1")) {
                                                f.val1 = o.getString("val1").toDouble()
                                            }
                                            cityInfos.add(f)
                                        }
                                    }
                                    okHttpFactBitmap()

                                    //详情开始
                                    if (!obj.isNull("th")) {
                                        val itemObj = obj.getJSONObject("th")
                                        if (!itemObj.isNull("stationName")) {
                                            stationName = itemObj.getString("stationName")
                                        }
                                        if (!itemObj.isNull("area")) {
                                            area = itemObj.getString("area")
                                        }
                                        if (!itemObj.isNull("val")) {
                                            `val` = itemObj.getString("val")
                                        }
                                    }
                                    if (!obj.isNull("times")) {
                                        timeList.clear()
                                        val timeArray = obj.getJSONArray("times")
                                        for (i in 0 until timeArray.length()) {
                                            val f = FactDto()
                                            val fo = timeArray.getJSONObject(i)
                                            if (!fo.isNull("timeString")) {
                                                f.timeString = fo.getString("timeString")
                                            }
                                            if (!fo.isNull("timestart")) {
                                                f.timeStart = fo.getString("timestart")
                                            }
                                            if (!fo.isNull("timeParams")) {
                                                f.timeParams = fo.getString("timeParams")
                                            }
                                            timeList.add(f)
                                            if (i == 0) {
                                                timeString = f.timeString
                                            }
                                        }
                                    }
                                    realDatas.clear()
                                    if (!obj.isNull("realDatas")) {
                                        val array = JSONArray(obj.getString("realDatas"))
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
                                            if (!itemObj.isNull("area1")) {
                                                dto.area1 = itemObj.getString("area1")
                                            }
                                            if (!itemObj.isNull("val")) {
                                                dto.`val` = itemObj.getDouble("val")
                                            }
                                            if (!itemObj.isNull("val1")) {
                                                dto.val1 = itemObj.getDouble("val1")
                                            }
                                            if (!itemObj.isNull("Lon")) {
                                                dto.lng = itemObj.getString("Lon").toDouble()
                                            }
                                            if (!itemObj.isNull("Lat")) {
                                                dto.lat = itemObj.getString("Lat").toDouble()
                                            }
                                            if (!TextUtils.isEmpty(dto.stationName) && !TextUtils.isEmpty(dto.area)) {
                                                realDatas.add(dto)
                                            }
                                        }
                                    }
                                    //详情结束
                                    if (!obj.isNull("jb")) {
                                        factList.clear()
                                        val array = obj.getJSONArray("jb")
                                        for (i in 0 until array.length()) {
                                            val itemObj = array.getJSONObject(i)
                                            val data = FactDto()
                                            if (!itemObj.isNull("lv")) {
                                                data.rainLevel = itemObj.getString("lv")
                                            }
                                            if (!itemObj.isNull("count")) {
                                                data.count = itemObj.getInt("count").toString() + ""
                                            }
                                            if (!itemObj.isNull("xs")) {
                                                val xsArray = itemObj.getJSONArray("xs")
                                                val list: MutableList<FactDto> = ArrayList()
                                                list.clear()
                                                for (j in 0 until xsArray.length()) {
                                                    val d = FactDto()
                                                    d.area = xsArray.getString(j)
                                                    list.add(d)
                                                }
                                                data.areaList.addAll(list)
                                            }
                                            factList.add(data)
                                        }
                                        if (factList.size > 0 && factAdapter != null) {
                                            CommonUtil.setListViewHeightBasedOnChildren(listView)
                                            factAdapter!!.timeString = timeString
                                            factAdapter!!.stationName = stationName
                                            factAdapter!!.area = area
                                            factAdapter!!.`val` = `val`
                                            factAdapter!!.realDatas.clear()
                                            factAdapter!!.realDatas.addAll(realDatas)
                                            factAdapter!!.notifyDataSetChanged()
                                            tvIntro.visibility = View.VISIBLE
                                            tvIntro.setBackgroundResource(R.drawable.bg_corner_black)
                                            listTitle!!.visibility = View.VISIBLE
                                            listView!!.visibility = View.VISIBLE
                                            tvDetail!!.visibility = View.VISIBLE
                                            tvHistory.visibility = View.VISIBLE
                                        }
                                    } else {
                                        tvIntro.visibility = View.GONE
                                        listTitle!!.visibility = View.GONE
                                        listView!!.visibility = View.GONE
                                        tvDetail!!.visibility = View.GONE
                                        tvHistory.visibility = View.GONE
                                    }
                                    tvLayerName!!.isFocusable = true
                                    tvLayerName!!.isFocusableInTouchMode = true
                                    tvLayerName!!.requestFocus()
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                }
                            } else {
                                removePolygons()
                                progressBar!!.visibility = View.GONE
                                tvToast.visibility = View.VISIBLE
                                Handler().postDelayed({ tvToast.visibility = View.GONE }, 1000)
                            }
                        }
                    }
                })
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }.start()
    }

    /**
     * 获取并绘制图层
     */
    private fun okHttpFactBitmap() {
        Thread {
            if (layerMap.containsKey(childId)) {
                val value = layerMap[childId]
                if (!TextUtils.isEmpty(value)) {
                    try {
                        val obj = JSONObject(value)
                        val maxlat = obj.getDouble("maxlat")
                        val maxlon = obj.getDouble("maxlon")
                        val minlat = obj.getDouble("minlat")
                        val minlon = obj.getDouble("minlon")
                        val imgurl = obj.getString("imgurl")
                        OkHttpUtil.enqueue(Request.Builder().url(imgurl).build(), object : Callback {
                            override fun onFailure(call: Call, e: IOException) {}

                            @Throws(IOException::class)
                            override fun onResponse(call: Call, response: Response) {
                                if (!response.isSuccessful) {
                                    return
                                }
                                val bytes = response.body!!.bytes()
                                runOnUiThread {
                                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    if (bitmap != null) {
                                        drawFactBitmap(bitmap, LatLng(maxlat, maxlon), LatLng(minlat, minlon))
                                    }
                                }
                            }
                        })
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }.start()
    }

    /**
     * 绘制实况图
     */
    private fun drawFactBitmap(bitmap: Bitmap?, max: LatLng, min: LatLng) {
        if (bitmap == null) {
            return
        }
        val fromView = BitmapDescriptorFactory.fromBitmap(bitmap)
        val bounds = LatLngBounds.Builder()
                .include(max)
                .include(min)
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
        drawDataToMap()
    }

    private fun removeTexts() {
        for (i in texts.indices) {
            texts[i].remove()
        }
        texts.clear()
    }

    /**
     * 清除图层
     */
    private fun removePolygons() {
        for (i in polygons.indices) {
            polygons[i].remove()
        }
        polygons.clear()
    }

    /**
     * 清除边界线
     */
    private fun removePolylines() {
        for (i in polylines.indices) {
            polylines[i].remove()
        }
        polylines.clear()
    }

    /**
     * 清除市县名称
     */
    private fun removeCityTexts() {
        for (i in cityTexts.indices) {
            cityTexts[i].remove()
        }
        cityTexts.clear()
    }

    /**
     * 清除自动站
     */
    private fun removeAutoTexts() {
        for (i in autoTexts.indices) {
            autoTexts[i].remove()
        }
        autoTexts.clear()
    }

    /**
     * 清除自动站
     */
    private fun removeAutoTextsH() {
        for (i in autoTextsH.indices) {
            autoTextsH[i].remove()
        }
        autoTextsH.clear()
    }

    /**
     * 绘制图层
     */
    private fun drawDataToMap() {
        if (aMap == null) {
            return
        }
        removeTexts()
        removePolygons()
        removePolylines()
        removeCityTexts()
        removeAutoTexts()
        removeAutoTextsH()
        drawAllDistrict()
        progressBar!!.visibility = View.GONE
    }

    /**
     * 绘制广西市县边界
     */
    private fun drawAllDistrict() {
        if (aMap == null) {
            return
        }
        val result = CommonUtil.getFromAssets(this, "heilongjiang.json")
        if (!TextUtils.isEmpty(result)) {
            try {
                val obj = JSONObject(result)
                val array = obj.getJSONArray("features")
                for (i in 0 until array.length()) {
                    val itemObj = array.getJSONObject(i)
                    val geometry = itemObj.getJSONObject("geometry")
                    val coordinates = geometry.getJSONArray("coordinates")
                    for (m in 0 until coordinates.length()) {
                        val array2 = coordinates.getJSONArray(m)
                        val polylineOption = PolylineOptions()
                        polylineOption.width(3f).color(-0x262627)
                        for (j in 0 until array2.length()) {
                            val itemArray = array2.getJSONArray(j)
                            val lng = itemArray.getDouble(0)
                            val lat = itemArray.getDouble(1)
                            polylineOption.add(LatLng(lat, lng))
                        }
                        val polyLine = aMap!!.addPolyline(polylineOption)
                        polylines.add(polyLine)
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        handler.removeMessages(1001)
        val msg = handler.obtainMessage()
        msg.what = 1001
        msg.obj = zoom
        handler.sendMessageDelayed(msg, 1000)
    }

    override fun onCameraChange(arg0: CameraPosition?) {}

    override fun onCameraChangeFinish(arg0: CameraPosition) {
        zoom = arg0.zoom
        handler.removeMessages(1001)
        val msg = handler.obtainMessage()
        msg.what = 1001
        handler.sendMessageDelayed(msg, 1000)
    }

    @SuppressLint("HandlerLeak")
    private val handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                1001 -> {
                    removeCityTexts()
                    removeAutoTexts()
                    removeAutoTextsH()
                    if (zoom <= 7.5f) {
                        addCityMarkers()
                    } else if (zoom <= 8.2f) { //乡镇站点
                        addCityMarkers()
                        addAutoMarkers()
                    } else {
                        addCityMarkers()
                        addAutoMarkers()
                        addAutoHMarkers()
                    }
                }
            }
        }
    }

    private fun addCityMarkers() {
        //绘制人工站
        for (i in cityInfos.indices) {
            val dto = cityInfos[i]
            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.layout_fact_value, null)
            if (dto.`val` >= 99999) {
                view.tvValue.text = ""
            } else {
                view.tvValue.text = dto.`val`.toString() + ""
                if (dto.val1 != -1.0) {
                    val b = CommonUtil.getWindMarker(this, dto.`val`)
                    if (b != null) {
                        val matrix = Matrix()
                        matrix.postScale(1f, 1f)
                        matrix.postRotate(dto.val1.toFloat())
                        val bitmap = Bitmap.createBitmap(b, 0, 0, b.width, b.height, matrix, true)
                        if (bitmap != null) {
                            view.ivWind.setImageBitmap(bitmap)
                            view.ivWind.visibility = View.VISIBLE
                        }
                    }
                }
                if (!TextUtils.isEmpty(dto.stationName)) {
                    view.tvName.text = dto.stationName
                }
                val options = MarkerOptions()
                options.title(dto.stationName)
                options.anchor(0.5f, 0.5f)
                options.position(LatLng(dto.lat, dto.lng))
                options.icon(BitmapDescriptorFactory.fromView(view))
                val marker = aMap!!.addMarker(options)
                marker.isVisible = true
                cityTexts.add(marker)
            }
        }
    }

    private fun addAutoMarkers() {
        for (i in realDatas.indices) {
            val dto = realDatas[i]
            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.layout_fact_value, null)
            if (!dto!!.stationName.startsWith("H")) {
                if (dto.`val` >= 99999) {
                    view.tvValue.text = ""
                } else {
                    view.tvValue.text = dto.`val`.toString()
                    if (dto.val1 != -1.0) {
                        val b = CommonUtil.getWindMarker(this, dto.`val`)
                        if (b != null) {
                            val matrix = Matrix()
                            matrix.postScale(1f, 1f)
                            matrix.postRotate(dto.val1.toFloat())
                            val bitmap = Bitmap.createBitmap(b, 0, 0, b.width, b.height, matrix, true)
                            if (bitmap != null) {
                                view.ivWind.setImageBitmap(bitmap)
                                view.ivWind.visibility = View.VISIBLE
                            }
                        }
                    }
                    if (!TextUtils.isEmpty(dto.stationName)) {
                        view.tvName.text = dto.stationName
                    }
                    val options = MarkerOptions()
                    options.title(dto.stationName)
                    options.anchor(0.5f, 0.5f)
                    options.position(LatLng(dto.lat, dto.lng))
                    options.icon(BitmapDescriptorFactory.fromView(view))
                    val marker = aMap!!.addMarker(options)
                    marker.isVisible = true
                    autoTexts.add(marker)
                }
            }
        }
    }

    private fun addAutoHMarkers() {
        for (i in realDatas.indices) {
            val dto = realDatas[i]
            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.layout_fact_value, null)
            if (dto!!.stationName.startsWith("H")) {
                if (dto.`val` >= 99999) {
                    view.tvValue.text = ""
                } else {
                    view.tvValue.text = dto.`val`.toString()
                    if (dto.val1 != -1.0) {
                        val b = CommonUtil.getWindMarker(this, dto.`val`)
                        if (b != null) {
                            val matrix = Matrix()
                            matrix.postScale(1f, 1f)
                            matrix.postRotate(dto.val1.toFloat())
                            val bitmap = Bitmap.createBitmap(b, 0, 0, b.width, b.height, matrix, true)
                            if (bitmap != null) {
                                view.ivWind.setImageBitmap(bitmap)
                                view.ivWind.visibility = View.VISIBLE
                            }
                        }
                    }
                    if (!TextUtils.isEmpty(dto.stationName)) {
                        view.tvName.text = dto.stationName
                    }
                    val options = MarkerOptions()
                    options.title(dto.stationName)
                    options.anchor(0.5f, 0.5f)
                    options.position(LatLng(dto.lat, dto.lng))
                    options.icon(BitmapDescriptorFactory.fromView(view))
                    val marker = aMap!!.addMarker(options)
                    marker.isVisible = true
                    autoTexts.add(marker)
                }
            }
        }
    }

    /**
     * 初始化viewPager
     */
    private fun initViewPager() {
        if (viewPager != null) {
            viewPager!!.removeAllViewsInLayout()
            fragments.clear()
        }
        val fragment1: Fragment = FactCheckFragment()
        fragments.add(fragment1)
        viewPager!!.setSlipping(true) //设置ViewPager是否可以滑动
        viewPager!!.offscreenPageLimit = fragments.size
        viewPager!!.adapter = MyPagerAdapter(supportFragmentManager)
    }

    private inner class MyPagerAdapter(fm: FragmentManager?) : FragmentStatePagerAdapter(fm) {
        override fun getCount(): Int {
            return fragments.size
        }

        override fun getItem(arg0: Int): Fragment {
            return fragments[arg0]
        }

        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE
        }

        init {
            notifyDataSetChanged()
        }
    }

    private fun dialogHistory() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_fact_history, null)
        val mAdapter = FactTimeAdapter(this, timeList)
        view.listView.adapter = mAdapter
        val dialog = Dialog(this, R.style.CustomProgressDialog)
        dialog.setContentView(view)
        dialog.show()
        view.listView.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 ->
            dialog.dismiss()
            val dto = timeList[arg2]
            okHttpFact(dto.timeParams)
        }
        view.tvNegative.setOnClickListener { dialog.dismiss() }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.llBack -> finish()
            R.id.tvDetail -> {
                val intent = Intent(this, FactDetailActivity::class.java)
                intent.putExtra("title", "详情数据")
                intent.putExtra("timeString", timeString)
                intent.putExtra("stationName", stationName)
                intent.putExtra("area", area)
                intent.putExtra("val", `val`)
                val bundle = Bundle()
                bundle.putParcelableArrayList("realDatas", realDatas as ArrayList<out Parcelable?>)
                intent.putExtras(bundle)
                startActivity(intent)
            }
            R.id.tvHistory -> dialogHistory()
        }
    }
    
}
