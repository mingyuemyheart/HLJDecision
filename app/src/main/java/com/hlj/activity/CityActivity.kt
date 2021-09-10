package com.hlj.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.View.OnClickListener
import android.widget.AdapterView.OnItemClickListener
import com.hlj.adapter.CitySearchAdapter
import com.hlj.adapter.CityProAdapter
import com.hlj.adapter.CityNationAdapter
import com.hlj.dto.CityDto
import com.hlj.manager.DBManager
import kotlinx.android.synthetic.main.activity_city.*
import kotlinx.android.synthetic.main.layout_title.*
import shawn.cxwl.com.hlj.R
import java.util.*

/**
 * 城市选择
 */
class CityActivity : BaseActivity(), OnClickListener {

    //搜索城市后的结果列表
    private var cityAdapter: CitySearchAdapter? = null
    private val cityList: MutableList<CityDto> = ArrayList()

    //省内热门
    private var pAdapter: CityProAdapter? = null
    private val pList: MutableList<CityDto> = ArrayList()
    private var section = 1
    private val sectionMap = HashMap<String, Int>()

    //全国热门
    private var nAdapter: CityNationAdapter? = null
    private val nList: MutableList<CityDto> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_city)
        initWidget()
        initListView()
        initPGridView()
        initNGridView()
    }

    /**
     * 初始化控件
     */
    private fun initWidget() {
        llBack!!.setOnClickListener(this)
        tvTitle!!.text = "城市选择"
        etSearch!!.addTextChangedListener(watcher)
        tvProvince!!.setOnClickListener(this)
        tvNational!!.setOnClickListener(this)
    }

    private val watcher: TextWatcher = object : TextWatcher {
        override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
        override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
        override fun afterTextChanged(arg0: Editable) {
            cityList.clear()
            if (TextUtils.isEmpty(arg0.toString())) {
                listView!!.visibility = View.GONE
                tvProvince!!.visibility = View.VISIBLE
                tvNational!!.visibility = View.VISIBLE
                pGridView!!.visibility = View.VISIBLE
                nGridView!!.visibility = View.VISIBLE
            } else {
                listView!!.visibility = View.VISIBLE
                tvProvince!!.visibility = View.GONE
                tvNational!!.visibility = View.GONE
                pGridView!!.visibility = View.GONE
                nGridView!!.visibility = View.GONE
                getCityInfo(arg0.toString())
            }
        }
    }

    /**
     * 迁移到天气详情界面
     */
    private fun intentWeatherDetail(data: CityDto) {
        val bundle = Bundle()
        bundle.putParcelable("data", data)
        val intent = Intent()
        intent.putExtras(bundle)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    /**
     * 初始化listview
     */
    private fun initListView() {
        cityAdapter = CitySearchAdapter(this, cityList)
        listView!!.adapter = cityAdapter
        listView!!.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 -> intentWeatherDetail(cityList[arg2]) }
    }

    /**
     * 初始化省内热门gridview
     */
    private fun initPGridView() {
        val stations = resources.getStringArray(R.array.pro_hotCity)
        for (i in stations.indices) {
            val value = stations[i].split(",").toTypedArray()
            val dto = CityDto()
            dto.cityId = value[2]
            dto.areaName = value[3]
            dto.lat = java.lang.Double.valueOf(value[1])
            dto.lng = java.lang.Double.valueOf(value[0])
            dto.level = value[4]
            dto.sectionName = value[5]
            pList.add(dto)
        }
        for (i in pList.indices) {
            val sectionDto = pList[i]
            if (!sectionMap.containsKey(sectionDto.sectionName)) {
                sectionDto.section = section
                sectionMap[sectionDto.sectionName] = section
                section++
            } else {
                sectionDto.section = sectionMap[sectionDto.sectionName]!!
            }
            pList[i] = sectionDto
        }
        pAdapter = CityProAdapter(this, pList)
        pGridView!!.adapter = pAdapter
        pGridView!!.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 -> intentWeatherDetail(pList[arg2]) }
    }

    /**
     * 获取全国热门城市
     * @param context
     * @return
     */
    private fun getNationHotCity(context: Context?): MutableList<CityDto>? {
        val nList: MutableList<CityDto> = ArrayList()
        val array = context!!.resources.getStringArray(R.array.nation_hotCity)
        for (i in array.indices) {
            val data = array[i].split(",").toTypedArray()
            val dto = CityDto()
            dto.lng = java.lang.Double.valueOf(data[3])
            dto.lat = java.lang.Double.valueOf(data[2])
            dto.cityId = data[0]
            dto.areaName = data[1]
            nList.add(dto)
        }
        return nList
    }

    /**
     * 初始化全国热门
     */
    private fun initNGridView() {
        nList.clear()
        nList.addAll(getNationHotCity(this)!!)
        nAdapter = CityNationAdapter(this, nList)
        nGridView!!.adapter = nAdapter
        nGridView!!.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 -> intentWeatherDetail(nList[arg2]) }
    }

    /**
     * 获取城市信息
     */
    private fun getCityInfo(keyword: String) {
        val dbManager = DBManager(this)
        dbManager.openDateBase()
        dbManager.closeDatabase()
        val database = SQLiteDatabase.openOrCreateDatabase(DBManager.DB_PATH + "/" + DBManager.DB_NAME, null)
        var cursor: Cursor? = null
        cursor = database.rawQuery("select * from " + DBManager.TABLE_NAME3 + " where pro like " + "\"%" + keyword + "%\"" + " or city like " + "\"%" + keyword + "%\"" + " or dis like " + "\"%" + keyword + "%\"", null)
        for (i in 0 until cursor.count) {
            cursor.moveToPosition(i)
            val dto = CityDto()
            dto.provinceName = cursor.getString(cursor.getColumnIndex("pro"))
            dto.cityName = cursor.getString(cursor.getColumnIndex("city"))
            dto.areaName = cursor.getString(cursor.getColumnIndex("dis"))
            dto.cityId = cursor.getString(cursor.getColumnIndex("cid"))
            dto.warningId = cursor.getString(cursor.getColumnIndex("wid"))
            cityList.add(dto)
        }
        if (cityList.size > 0 && cityAdapter != null) {
            cityAdapter!!.notifyDataSetChanged()
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.llBack -> finish()
            R.id.tvProvince -> {
                tvProvince!!.setTextColor(Color.WHITE)
                tvNational!!.setTextColor(ContextCompat.getColor(this, R.color.title_bg))
                tvProvince!!.setBackgroundResource(R.drawable.corner_left_blue)
                tvNational!!.setBackgroundResource(R.drawable.corner_right_white)
                pGridView!!.visibility = View.VISIBLE
                nGridView!!.visibility = View.GONE
            }
            R.id.tvNational -> {
                tvProvince!!.setTextColor(ContextCompat.getColor(this, R.color.title_bg))
                tvNational!!.setTextColor(Color.WHITE)
                tvProvince!!.setBackgroundResource(R.drawable.corner_left_white)
                tvNational!!.setBackgroundResource(R.drawable.corner_right_blue)
                pGridView!!.visibility = View.GONE
                nGridView!!.visibility = View.VISIBLE
            }
        }
    }

}
