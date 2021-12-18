package com.hlj.activity

import android.content.Intent
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.hlj.adapter.HanghuaDetailAdapter
import com.hlj.common.CONST
import com.hlj.common.MyApplication
import com.hlj.dto.StationMonitorDto
import com.hlj.utils.CommonUtil
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.activity_hanghua.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException

/**
 * 航化作业
 */
class HanghuaActivity : BaseActivity(), View.OnClickListener, AMap.OnMapClickListener, AMap.OnMarkerClickListener {

    private var aMap: AMap? = null
    private val dataList: ArrayList<StationMonitorDto> = ArrayList()
    private var mAdapter: HanghuaDetailAdapter? = null
    private val dataList2: ArrayList<StationMonitorDto> = ArrayList()
    private var clickMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hanghua)
        showDialog()
        initMap(savedInstanceState)
        initWidget()
        initListView()
    }

    private fun initWidget() {
        llBack!!.setOnClickListener(this)
        ivList.setOnClickListener(this)
        ivClear.setOnClickListener(this)
        val title = intent.getStringExtra(CONST.ACTIVITY_NAME)
        if (!TextUtils.isEmpty(title)) {
            tvTitle!!.text = title
        }
    }

    private fun initMap(bundle: Bundle?) {
        mapView!!.onCreate(bundle)
        if (aMap == null) {
            aMap = mapView!!.map
        }
        aMap!!.moveCamera(CameraUpdateFactory.zoomTo(8.0f))
        aMap!!.uiSettings.isZoomControlsEnabled = false
        aMap!!.uiSettings.isRotateGesturesEnabled = false
        aMap!!.setOnMarkerClickListener(this)
        aMap!!.setOnMapClickListener(this)
        aMap!!.setOnMapLoadedListener {
            tvMapNumber.text = aMap!!.mapContentApprovalNumber
            CommonUtil.drawHLJJson(this, aMap)
            okHttpList()
        }
    }

    private fun addMarkerToMap(latLng: LatLng) {
        if (clickMarker != null) {
            clickMarker!!.remove()
        }
        val options = MarkerOptions()
        options.position(latLng)
        options.anchor(0.5f, 1.0f)
        val bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(resources, R.drawable.iv_location),
                CommonUtil.dip2px(this, 25f).toInt(), CommonUtil.dip2px(this, 25f).toInt())
        if (bitmap != null) {
            options.icon(BitmapDescriptorFactory.fromBitmap(bitmap))
        }
        clickMarker = aMap!!.addMarker(options)
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        okHttpList2(p0!!.position.latitude, p0.position.longitude)
        return false
    }

    override fun onMapClick(arg0: LatLng) {
        addMarkerToMap(arg0)
        okHttpList2(arg0.latitude, arg0.longitude)
    }

    private fun okHttpList() {
        Thread {
            val url = "http://decision-admin.tianqi.cn/Home/work2019/hlj_getmyhanghuoData?uid=${MyApplication.UID}"
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    runOnUiThread {
                        cancelDialog()
                        if (!TextUtils.isEmpty(result)) {
                            val obj = JSONObject(result)
                            if (!obj.isNull("data")) {
                                dataList.clear()
                                val array = obj.getJSONArray("data")
                                for (i in 0 until array.length()) {
                                    val dto = StationMonitorDto()
                                    val itemObj = array.getJSONObject(i)
                                    if (!itemObj.isNull("name")) {
                                        dto.name = itemObj.getString("name")
                                    }
                                    if (!itemObj.isNull("lat")) {
                                        val lat = itemObj.getString("lat")
                                        if (!TextUtils.isEmpty(lat)) {
                                            dto.lat = lat.toDouble()
                                        }
                                    }
                                    if (!itemObj.isNull("lon")) {
                                        val lng = itemObj.getString("lon")
                                        if (!TextUtils.isEmpty(lng)) {
                                            dto.lng = lng.toDouble()
                                        }
                                    }
                                    if (!itemObj.isNull("addtime")) {
                                        dto.time = itemObj.getString("addtime")
                                    }
                                    dataList.add(dto)
                                    if (dto.lat != 0.0 && dto.lng != 0.0) {
                                        val options = MarkerOptions()
                                        options.position(LatLng(dto.lat, dto.lng))
                                        options.anchor(0.5f, 1.0f)
                                        val bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(resources, R.drawable.sz_icon_locat),
                                                CommonUtil.dip2px(this@HanghuaActivity, 25f).toInt(), CommonUtil.dip2px(this@HanghuaActivity, 25f).toInt())
                                        if (bitmap != null) {
                                            options.icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                                        }
                                        clickMarker = aMap!!.addMarker(options)
                                    }
                                }
                            }
                        }
                    }
                }
            })
        }.start()
    }

    private fun initListView() {
        mAdapter = HanghuaDetailAdapter(this, dataList2)
        listView!!.adapter = mAdapter
    }

    private fun okHttpList2(lat: Double, lng: Double) {
        Thread {
            val url = "http://decision-admin.tianqi.cn/Home/work2019/hlj_gethanghuoData?lat=$lat&lon=$lng"
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                }
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    runOnUiThread {
                        clContent.visibility = View.VISIBLE
                        if (!TextUtils.isEmpty(result)) {
                            val obj = JSONObject(result)
                            if (!obj.isNull("msg")) {
                                val msg = obj.getString("msg")
                                if (!TextUtils.isEmpty(msg)) {
                                    tvName.text = msg
                                }
                            }
                            if (!obj.isNull("sunrise")) {
                                val sunrise = obj.getString("sunrise")
                                if (!TextUtils.isEmpty(sunrise)) {
                                    tvName.text = "${tvName.text}\n日升日落:$sunrise"
                                }
                            }
                            if (!obj.isNull("sunset")) {
                                val sunset = obj.getString("sunset")
                                if (!TextUtils.isEmpty(sunset)) {
                                    tvName.text = "${tvName.text}|$sunset"
                                }
                            }
                            if (!obj.isNull("data")) {
                                dataList2.clear()
                                val array = obj.getJSONArray("data")
                                for (i in 0 until array.length()) {
                                    val dto = StationMonitorDto()
                                    val itemObj = array.getJSONObject(i)
                                    if (!itemObj.isNull("title")) {
                                        dto.name = itemObj.getString("title")
                                    }
                                    if (!itemObj.isNull("desc")) {
                                        dto.value = itemObj.getString("desc")
                                    }
                                    dataList2.add(dto)
                                }
                                if (mAdapter != null) {
                                    mAdapter!!.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }
            })
        }.start()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.llBack -> {
                finish()
            }
            R.id.ivList -> {
                val intent = Intent(this, HanghuaListActivity::class.java)
                intent.putExtra(CONST.ACTIVITY_NAME, "我的地块")
                startActivity(intent)
            }
            R.id.ivClear -> clContent.visibility = View.GONE
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
    }

}
