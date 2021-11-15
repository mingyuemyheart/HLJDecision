package com.hlj.fragment

import android.animation.IntEvaluator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Parcelable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.AdapterView.OnItemClickListener
import android.widget.ImageView
import com.amap.api.maps.AMap
import com.amap.api.maps.AMap.*
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.hlj.activity.WarningDetailActivity
import com.hlj.activity.WarningHistoryActivity
import com.hlj.activity.WarningListActivity
import com.hlj.activity.WarningStatisticActivity
import com.hlj.adapter.ShawnWarningStatisticAdapter
import com.hlj.adapter.WarningAdapter
import com.hlj.common.CONST
import com.hlj.common.ColumnData
import com.hlj.dto.WarningDto
import com.hlj.utils.CommonUtil
import com.hlj.utils.OkHttpUtil
import com.hlj.view.ArcMenu
import kotlinx.android.synthetic.main.fragment_warning.*
import kotlinx.android.synthetic.main.shawn_warning_marker_icon_info.view.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * 天气预警
 */
class WarningFragment : BaseFragment(), OnClickListener, OnMapClickListener, OnMarkerClickListener, InfoWindowAdapter {

    private var mReceiver: MyBroadCastReceiver? = null
    private var aMap: AMap? = null
    private var blue = true
    private var yellow = true
    private var orange = true
    private var red = true
    private val warningList: MutableList<WarningDto?> = ArrayList()
    private val markers: MutableList<Marker> = ArrayList()
    private var selectMarker: Marker? = null
    private var isShowPrompt = true
    private val sdf3 = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    //预警统计列表
    private var statisticAdapter: ShawnWarningStatisticAdapter? = null
    private val statisticList: MutableList<WarningDto> = ArrayList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_warning, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBroadCast()
        initAmap(savedInstanceState)
    }

    private fun initBroadCast() {
        mReceiver = MyBroadCastReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(arguments!!.getString(CONST.BROADCAST_ACTION))
        activity!!.registerReceiver(mReceiver, intentFilter)
    }

    private inner class MyBroadCastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            init()
        }
    }

    private fun init() {
        initWidget()
        initListView()
    }

    /**
     * 初始化高德地图
     */
    private fun initAmap(bundle: Bundle?) {
        mapView.onCreate(bundle)
        if (aMap == null) {
            aMap = mapView.map
        }
        aMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(CONST.guizhouLatLng, 6.0f))
        aMap!!.uiSettings.isMyLocationButtonEnabled = false // 设置默认定位按钮是否显示
        aMap!!.uiSettings.isZoomControlsEnabled = false
        aMap!!.uiSettings.isRotateGesturesEnabled = false
        aMap!!.setOnMapClickListener(this)
        aMap!!.setOnMarkerClickListener(this)
        aMap!!.setInfoWindowAdapter(this)
        tvMapNumber.text = aMap!!.mapContentApprovalNumber
        aMap!!.setOnMapLoadedListener {
            CommonUtil.drawHLJJson(activity, aMap)
        }
    }

    /**
     * 初始化控件
     */
    private fun initWidget() {
        arcMenu.onMenuItemClickListener = arcMenuListener
        ivRefresh.setOnClickListener(this)
        ivList.setOnClickListener(this)
        ivHistory.setOnClickListener(this)
        clWarning.setOnClickListener(this)
        ivArrow.setOnClickListener(this)

        val data: ColumnData = arguments!!.getParcelable("data")
        for (i in data.child.indices) {
            val dto = data.child[i]
            if (TextUtils.equals(dto.columnId, "652")) {//预警统计
                ivStatistic.visibility = View.VISIBLE
                ivStatistic.setOnClickListener(this)
            }
        }

        refresh()
    }

    private fun refresh() {
        Thread { okHttpWarning() }.start()
    }

    /**
     * 获取预警信息
     */
    private fun okHttpWarning() {
        val url = "https://decision-admin.tianqi.cn/Home/work2019/getwarns?areaid=23"
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
                            warningList.clear()
                            val `object` = JSONObject(result)
                            if (!`object`.isNull("data")) {
                                val jsonArray = `object`.getJSONArray("data")
                                for (i in 0 until jsonArray.length()) {
                                    val tempArray = jsonArray.getJSONArray(i)
                                    val dto = WarningDto()
                                    dto.html = tempArray.getString(1)
                                    val array = dto.html.split("-").toTypedArray()
                                    val item0 = array[0]
                                    val item1 = array[1]
                                    val item2 = array[2]
                                    dto.item0 = item0
                                    dto.provinceId = item0.substring(0, 2)
                                    dto.type = item2.substring(0, 5)
                                    dto.color = item2.substring(5, 7)
                                    dto.time = item1
                                    dto.lng = tempArray.getDouble(2)
                                    dto.lat = tempArray.getDouble(3)
                                    dto.name = tempArray.getString(0)
                                    if (!dto.name.contains("解除")) {
                                        warningList.add(dto)
                                    }
                                }
                                addWarningMarkers()
                            }
                            try {
                                val count = warningList.size.toString() + ""
                                if (TextUtils.equals(count, "0")) {
                                    var time = ""
                                    if (!`object`.isNull("time")) {
                                        val t = `object`.getLong("time")
                                        time = sdf3.format(Date(t * 1000))
                                    }
                                    tvWarningStatistic!!.text = time + ", " + "当前生效预警" + count + "条"
                                    clWarning!!.visibility = View.VISIBLE
                                }
                                var time = ""
                                if (!`object`.isNull("time")) {
                                    val t = `object`.getLong("time")
                                    time = sdf3.format(Date(t * 1000))
                                }
                                val str1 = "$time, 当前生效预警"
                                val str2 = "条"
                                val warningInfo = str1 + count + str2
                                val builder = SpannableStringBuilder(warningInfo)
                                val builderSpan1 = ForegroundColorSpan(resources.getColor(R.color.text_color3))
                                val builderSpan2 = ForegroundColorSpan(resources.getColor(R.color.red))
                                val builderSpan3 = ForegroundColorSpan(resources.getColor(R.color.text_color3))
                                builder.setSpan(builderSpan1, 0, str1.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                builder.setSpan(builderSpan2, str1.length, str1.length + count.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                builder.setSpan(builderSpan3, str1.length + count.length, str1.length + count.length + str2.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                tvWarningStatistic!!.text = builder
                                clWarning!!.visibility = View.VISIBLE

                                //计算统计列表信息
                                var rnation = 0
                                var rpro = 0
                                var rcity = 0
                                var rdis = 0
                                var onation = 0
                                var opro = 0
                                var ocity = 0
                                var odis = 0
                                var ynation = 0
                                var ypro = 0
                                var ycity = 0
                                var ydis = 0
                                var bnation = 0
                                var bpro = 0
                                var bcity = 0
                                var bdis = 0
                                var wnation = 0
                                var wpro = 0
                                var wcity = 0
                                var wdis = 0
                                for (i in warningList.indices) {
                                    val dto = warningList[i]
                                    if (TextUtils.equals(dto!!.color, "04")) {
                                        if (TextUtils.equals(dto.item0, "000000")) {
                                            rnation += 1
                                        } else if (TextUtils.equals(dto.item0.substring(dto.item0.length - 4, dto.item0.length), "0000")) {
                                            rpro += 1
                                        } else if (TextUtils.equals(dto.item0.substring(dto.item0.length - 2, dto.item0.length), "00")) {
                                            rcity += 1
                                        } else {
                                            rdis += 1
                                        }
                                    } else if (TextUtils.equals(dto.color, "03")) {
                                        if (TextUtils.equals(dto.item0, "000000")) {
                                            onation += 1
                                        } else if (TextUtils.equals(dto.item0.substring(dto.item0.length - 4, dto.item0.length), "0000")) {
                                            opro += 1
                                        } else if (TextUtils.equals(dto.item0.substring(dto.item0.length - 2, dto.item0.length), "00")) {
                                            ocity += 1
                                        } else {
                                            odis += 1
                                        }
                                    } else if (TextUtils.equals(dto.color, "02")) {
                                        if (TextUtils.equals(dto.item0, "000000")) {
                                            ynation += 1
                                        } else if (TextUtils.equals(dto.item0.substring(dto.item0.length - 4, dto.item0.length), "0000")) {
                                            ypro += 1
                                        } else if (TextUtils.equals(dto.item0.substring(dto.item0.length - 2, dto.item0.length), "00")) {
                                            ycity += 1
                                        } else {
                                            ydis += 1
                                        }
                                    } else if (TextUtils.equals(dto.color, "01")) {
                                        if (TextUtils.equals(dto.item0, "000000")) {
                                            bnation += 1
                                        } else if (TextUtils.equals(dto.item0.substring(dto.item0.length - 4, dto.item0.length), "0000")) {
                                            bpro += 1
                                        } else if (TextUtils.equals(dto.item0.substring(dto.item0.length - 2, dto.item0.length), "00")) {
                                            bcity += 1
                                        } else {
                                            bdis += 1
                                        }
                                    } else if (TextUtils.equals(dto.color, "05")) {
                                        if (TextUtils.equals(dto.item0, "000000")) {
                                            wnation += 1
                                        } else if (TextUtils.equals(dto.item0.substring(dto.item0.length - 4, dto.item0.length), "0000")) {
                                            wpro += 1
                                        } else if (TextUtils.equals(dto.item0.substring(dto.item0.length - 2, dto.item0.length), "00")) {
                                            wcity += 1
                                        } else {
                                            wdis += 1
                                        }
                                    }
                                }
                                statisticList.clear()
                                var wDto = WarningDto()
                                wDto.colorName = "预警" + warningList.size
                                wDto.nationCount = "国家级" + (rnation + onation + ynation + bnation + wnation)
                                wDto.proCount = "省级" + (rpro + opro + ypro + bpro + wpro)
                                wDto.cityCount = "市级" + (rcity + ocity + ycity + bcity + wcity)
                                wDto.disCount = "县级" + (rdis + odis + ydis + bdis + wdis)
                                statisticList.add(wDto)
                                wDto = WarningDto()
                                wDto.colorName = "红" + (rnation + rpro + rcity + rdis)
                                wDto.nationCount = rnation.toString() + ""
                                wDto.proCount = rpro.toString() + ""
                                wDto.cityCount = rcity.toString() + ""
                                wDto.disCount = rdis.toString() + ""
                                statisticList.add(wDto)
                                wDto = WarningDto()
                                wDto.colorName = "橙" + (onation + opro + ocity + odis)
                                wDto.nationCount = onation.toString() + ""
                                wDto.proCount = opro.toString() + ""
                                wDto.cityCount = ocity.toString() + ""
                                wDto.disCount = odis.toString() + ""
                                statisticList.add(wDto)
                                wDto = WarningDto()
                                wDto.colorName = "黄" + (ynation + ypro + ycity + ydis)
                                wDto.nationCount = ynation.toString() + ""
                                wDto.proCount = ypro.toString() + ""
                                wDto.cityCount = ycity.toString() + ""
                                wDto.disCount = ydis.toString() + ""
                                statisticList.add(wDto)
                                wDto = WarningDto()
                                wDto.colorName = "蓝" + (bnation + bpro + bcity + bdis)
                                wDto.nationCount = bnation.toString() + ""
                                wDto.proCount = bpro.toString() + ""
                                wDto.cityCount = bcity.toString() + ""
                                wDto.disCount = bdis.toString() + ""
                                statisticList.add(wDto)
                                wDto = WarningDto()
                                wDto.colorName = "未知" + (wnation + wpro + wcity + wdis)
                                wDto.nationCount = wnation.toString() + ""
                                wDto.proCount = wpro.toString() + ""
                                wDto.cityCount = wcity.toString() + ""
                                wDto.disCount = wdis.toString() + ""
                                statisticList.add(wDto)
                                if (statisticAdapter != null) {
                                    statisticAdapter!!.notifyDataSetChanged()
                                }
                            } catch (e: Exception) {
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
     * 在地图上添加marker
     */
    private fun addWarningMarkers() {
        val inflater = activity!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        for (dto in warningList) {
            val optionsTemp = MarkerOptions()
            optionsTemp.title(dto!!.lat.toString() + "," + dto.lng + "," + dto.item0 + "," + dto.color)
            optionsTemp.snippet(dto.color)
            optionsTemp.anchor(0.5f, 0.5f)
            optionsTemp.position(LatLng(dto.lat, dto.lng))
            val mView = inflater.inflate(R.layout.shawn_warning_marker_icon, null)
            val ivMarker = mView.findViewById<ImageView>(R.id.ivMarker)
            var bitmap: Bitmap? = null
            if (dto.color == CONST.blue[0]) {
                bitmap = CommonUtil.getImageFromAssetsFile(activity, "warning/" + dto.type + CONST.blue[1] + CONST.imageSuffix)
            } else if (dto.color == CONST.yellow[0]) {
                bitmap = CommonUtil.getImageFromAssetsFile(activity, "warning/" + dto.type + CONST.yellow[1] + CONST.imageSuffix)
            } else if (dto.color == CONST.orange[0]) {
                bitmap = CommonUtil.getImageFromAssetsFile(activity, "warning/" + dto.type + CONST.orange[1] + CONST.imageSuffix)
            } else if (dto.color == CONST.red[0]) {
                bitmap = CommonUtil.getImageFromAssetsFile(activity, "warning/" + dto.type + CONST.red[1] + CONST.imageSuffix)
            }
            if (bitmap == null) {
                bitmap = CommonUtil.getImageFromAssetsFile(activity, "warning/" + "default" + CONST.imageSuffix)
            }
            ivMarker.setImageBitmap(bitmap)
            optionsTemp.icon(BitmapDescriptorFactory.fromView(mView))
            val marker = if (TextUtils.equals(dto.color, "01") && blue) {
                aMap!!.addMarker(optionsTemp)
            } else if (TextUtils.equals(dto.color, "02") && yellow) {
                aMap!!.addMarker(optionsTemp)
            } else if (TextUtils.equals(dto.color, "03") && orange) {
                aMap!!.addMarker(optionsTemp)
            } else if (TextUtils.equals(dto.color, "04") && red) {
                aMap!!.addMarker(optionsTemp)
            } else {
                aMap!!.addMarker(optionsTemp)
            }
            markers.add(marker)
        }
    }

    private fun switchMarkers() {
        for (marker in markers) {
            if (TextUtils.equals(marker.snippet, "01")) {
                marker.isVisible = blue
            } else if (TextUtils.equals(marker.snippet, "02")) {
                marker.isVisible = yellow
            } else if (TextUtils.equals(marker.snippet, "03")) {
                marker.isVisible = orange
            } else if (TextUtils.equals(marker.snippet, "04")) {
                marker.isVisible = red
            }
        }
    }

    override fun onMapClick(arg0: LatLng?) {
        if (selectMarker != null) {
            selectMarker!!.hideInfoWindow()
        }
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        if (marker != null) {
            selectMarker = marker
            if (selectMarker!!.isInfoWindowShown) {
                selectMarker!!.hideInfoWindow()
            } else {
                selectMarker!!.showInfoWindow()
            }
        }
        return true
    }

    override fun getInfoContents(marker: Marker): View? {
        val inflater = activity!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val mView = inflater.inflate(R.layout.shawn_warning_marker_icon_info, null)
        val infoList = addInfoList(marker)
        val mAdapter = WarningAdapter(activity, infoList, true)
        mView.listView.adapter = mAdapter
        val params = mView.listView.layoutParams
        if (infoList.size == 1) {
            params.height = CommonUtil.dip2px(activity, 50f).toInt()
        } else if (infoList.size == 2) {
            params.height = CommonUtil.dip2px(activity, 100f).toInt()
        } else if (infoList.size > 2) {
            params.height = CommonUtil.dip2px(activity, 150f).toInt()
        }
        mView.listView.layoutParams = params
        mView.listView.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 -> intentDetail(infoList[arg2]) }
        return mView
    }

    private fun addInfoList(marker: Marker): List<WarningDto?> {
        val infoList: MutableList<WarningDto?> = ArrayList()
        for (dto in warningList) {
            val latLng = marker.title.split(",").toTypedArray()
            if (TextUtils.equals(latLng[0], dto!!.lat.toString() + "") && TextUtils.equals(latLng[1], dto.lng.toString() + "")) {
                infoList.add(dto)
            }
        }
        return infoList
    }

    private fun intentDetail(data: WarningDto?) {
        val intentDetail = Intent(activity, WarningDetailActivity::class.java)
        val bundle = Bundle()
        bundle.putParcelable("data", data)
        intentDetail.putExtras(bundle)
        startActivity(intentDetail)
    }

    override fun getInfoWindow(arg0: Marker?): View? {
        return null
    }

    private val arcMenuListener = ArcMenu.OnMenuItemClickListener { view, pos ->
        if (pos == 0) {
            blue = !blue
            if (!blue) {
                (view as ImageView).setImageResource(R.drawable.shawn_icon_arc_blue_press)
            } else {
                (view as ImageView).setImageResource(R.drawable.shawn_icon_arc_blue)
            }
            switchMarkers()
        } else if (pos == 1) {
            yellow = !yellow
            if (!yellow) {
                (view as ImageView).setImageResource(R.drawable.shawn_icon_arc_yellow_press)
            } else {
                (view as ImageView).setImageResource(R.drawable.shawn_icon_arc_yellow)
            }
            switchMarkers()
        } else if (pos == 2) {
            orange = !orange
            if (!orange) {
                (view as ImageView).setImageResource(R.drawable.shawn_icon_arc_orange_press)
            } else {
                (view as ImageView).setImageResource(R.drawable.shawn_icon_arc_orange)
            }
            switchMarkers()
        } else if (pos == 3) {
            red = !red
            if (!red) {
                (view as ImageView).setImageResource(R.drawable.shawn_icon_arc_red_press)
            } else {
                (view as ImageView).setImageResource(R.drawable.shawn_icon_arc_red)
            }
            switchMarkers()
        }
    }

    /**
     * 初始化预警统计列表
     */
    private fun initListView() {
        statisticAdapter = ShawnWarningStatisticAdapter(activity, statisticList)
        listView.adapter = statisticAdapter
        listView.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 -> clickPromptWarning() }
    }

    /**
     * 隐藏或显示ListView的动画
     */
    private fun hideOrShowListViewAnimator(view: View?, startValue: Int, endValue: Int) {
        //1.设置属性的初始值和结束值
        val mAnimator = ValueAnimator.ofInt(0, 100)
        //2.为目标对象的属性变化设置监听器
        mAnimator.addUpdateListener { animation ->
            val animatorValue = animation.animatedValue as Int
            val fraction = animatorValue / 100f
            val mEvaluator = IntEvaluator()
            //3.使用IntEvaluator计算属性值并赋值给ListView的高
            view!!.layoutParams.height = mEvaluator.evaluate(fraction, startValue, endValue)
            view.requestLayout()
        }
        //4.为ValueAnimator设置LinearInterpolator
        mAnimator.interpolator = LinearInterpolator()
        //5.设置动画的持续时间
        mAnimator.duration = 200
        //6.为ValueAnimator设置目标对象并开始执行动画
        mAnimator.setTarget(view)
        mAnimator.start()
    }

    private fun clickPromptWarning() {
        val height = CommonUtil.getListViewHeightBasedOnChildren(listView)
        isShowPrompt = !isShowPrompt
        if (!isShowPrompt) {
            ivArrow.setImageResource(R.drawable.shawn_icon_arrow_up_black)
            hideOrShowListViewAnimator(listView, 0, height)
        } else {
            ivArrow.setImageResource(R.drawable.shawn_icon_arrow_down_black)
            hideOrShowListViewAnimator(listView, height, 0)
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.clWarning, R.id.ivArrow -> clickPromptWarning()
            R.id.ivRefresh -> refresh()
            R.id.ivList -> {
                val intent = Intent(activity, WarningListActivity::class.java)
                intent.putExtra("isVisible", true)
                val bundle = Bundle()
                bundle.putParcelableArrayList("warningList", warningList as ArrayList<out Parcelable?>)
                intent.putExtras(bundle)
                startActivity(intent)
            }
            R.id.ivHistory -> startActivity(Intent(activity, WarningHistoryActivity::class.java))
            R.id.ivStatistic -> startActivity(Intent(activity, WarningStatisticActivity::class.java))
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
    override fun onDestroy() {
        super.onDestroy()
        if (mapView != null) {
            mapView!!.onDestroy()
        }
        if (mReceiver != null) {
            activity!!.unregisterReceiver(mReceiver)
        }
    }

}
