package com.hlj.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.View.OnClickListener
import android.widget.AdapterView.OnItemClickListener
import com.hlj.adapter.FactDetailAdapter
import com.hlj.dto.FactDto
import kotlinx.android.synthetic.main.activity_fact_detail.*
import kotlinx.android.synthetic.main.layout_title.*
import net.sourceforge.pinyin4j.PinyinHelper
import shawn.cxwl.com.hlj.R
import java.util.*

/**
 * 实况详情
 */
class FactDetailActivity : BaseActivity(), OnClickListener {

    private var b1 = false
    private var b2 = false
    private var b3 = false //false为将序，true为升序
    private var mAdapter: FactDetailAdapter? = null
    private val realDatas: MutableList<FactDto> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fact_detail)
        initWidget()
        initListView()
    }

    private fun initWidget() {
        llBack!!.setOnClickListener(this)
        ll1!!.setOnClickListener(this)
        ll2.setOnClickListener(this)
        ll3.setOnClickListener(this)
        if (intent.hasExtra("realDatas")) {
            val title = intent.getStringExtra("title")
            val timeString = intent.getStringExtra("timeString")
            val stationName = intent.getStringExtra("stationName")
            val area = intent.getStringExtra("area")
            val `val` = intent.getStringExtra("val")
            if (!TextUtils.isEmpty(title)) {
                tvTitle!!.text = title
            }
            if (!TextUtils.isEmpty(timeString)) {
                tvPrompt!!.text = timeString
            }
            if (!TextUtils.isEmpty(stationName)) {
                tv1!!.text = stationName
            }
            if (!TextUtils.isEmpty(area)) {
                tv2.text = area
            }
            if (!TextUtils.isEmpty(`val`)) {
                tv3.text = `val`
            }
            realDatas.clear()
            realDatas.addAll(intent.extras.getParcelableArrayList("realDatas"))
        }
        if (!b3) { //将序
            iv3.setImageResource(R.drawable.arrow_down)
        } else { //将序
            iv3.setImageResource(R.drawable.arrow_up)
        }
        iv3.visibility = View.VISIBLE
    }

    private fun initListView() {
        mAdapter = FactDetailAdapter(this, realDatas)
        listView!!.adapter = mAdapter
        listView!!.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 ->
            val dto = realDatas[arg2]
            val intent = Intent(this, FactDetailChartActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable("data", dto)
            intent.putExtras(bundle)
            startActivity(intent)
        }
    }

    /**
     * 返回中文的首字母
     * @param str
     * @return
     */
    fun getPinYinHeadChar(str: String): String {
        var convert = ""
        var size = str.length
        if (size >= 2) {
            size = 2
        }
        for (j in 0 until size) {
            val word = str[j]
            val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(word)
            convert += if (pinyinArray != null) {
                pinyinArray[0][0]
            } else {
                word
            }
        }
        return convert
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.llBack -> finish()
            R.id.ll1 -> {
                if (b1) { //升序
                    b1 = false
                    iv1!!.setImageResource(R.drawable.arrow_up)
                    iv1!!.visibility = View.VISIBLE
                    iv2.visibility = View.INVISIBLE
                    iv3.visibility = View.INVISIBLE
                    realDatas.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0!!.stationName) || TextUtils.isEmpty(arg1!!.stationName)) {
                            0
                        } else {
                            getPinYinHeadChar(arg0!!.stationName).compareTo(getPinYinHeadChar(arg1!!.stationName))
                        }
                    })
                } else { //将序
                    b1 = true
                    iv1!!.setImageResource(R.drawable.arrow_down)
                    iv1!!.visibility = View.VISIBLE
                    iv2.visibility = View.INVISIBLE
                    iv3.visibility = View.INVISIBLE
                    realDatas.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0!!.stationName) || TextUtils.isEmpty(arg1!!.stationName)) {
                            -1
                        } else {
                            getPinYinHeadChar(arg1!!.stationName).compareTo(getPinYinHeadChar(arg0!!.stationName))
                        }
                    })
                }
                if (mAdapter != null) {
                    mAdapter!!.notifyDataSetChanged()
                }
            }
            R.id.ll2 -> {
                if (b2) { //升序
                    b2 = false
                    iv2.setImageResource(R.drawable.arrow_up)
                    iv1!!.visibility = View.INVISIBLE
                    iv2.visibility = View.VISIBLE
                    iv3.visibility = View.INVISIBLE
                    realDatas.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0!!.area) || TextUtils.isEmpty(arg1!!.area)) {
                            0
                        } else {
                            getPinYinHeadChar(arg0!!.area).compareTo(getPinYinHeadChar(arg1!!.area))
                        }
                    })
                } else { //将序
                    b2 = true
                    iv2.setImageResource(R.drawable.arrow_down)
                    iv1!!.visibility = View.INVISIBLE
                    iv2.visibility = View.VISIBLE
                    iv3.visibility = View.INVISIBLE
                    realDatas.sortWith(Comparator { arg0, arg1 ->
                        if (TextUtils.isEmpty(arg0!!.area) || TextUtils.isEmpty(arg1!!.area)) {
                            -1
                        } else {
                            getPinYinHeadChar(arg1!!.area).compareTo(getPinYinHeadChar(arg0!!.area))
                        }
                    })
                }
                if (mAdapter != null) {
                    mAdapter!!.notifyDataSetChanged()
                }
            }
            R.id.ll3 -> {
                if (b3) { //升序
                    b3 = false
                    iv3.setImageResource(R.drawable.arrow_up)
                    iv1!!.visibility = View.INVISIBLE
                    iv2.visibility = View.INVISIBLE
                    iv3.visibility = View.VISIBLE
                    realDatas.sortWith(Comparator { arg0, arg1 -> java.lang.Double.valueOf(arg0!!.`val`).compareTo(java.lang.Double.valueOf(arg1!!.`val`)) })
                } else { //将序
                    b3 = true
                    iv3.setImageResource(R.drawable.arrow_down)
                    iv1!!.visibility = View.INVISIBLE
                    iv2.visibility = View.INVISIBLE
                    iv3.visibility = View.VISIBLE
                    realDatas.sortWith(Comparator { arg0, arg1 -> java.lang.Double.valueOf(arg1!!.`val`).compareTo(java.lang.Double.valueOf(arg0!!.`val`)) })
                }
                if (mAdapter != null) {
                    mAdapter!!.notifyDataSetChanged()
                }
            }
        }
    }
	
}
