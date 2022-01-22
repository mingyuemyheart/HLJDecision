package com.hlj.activity

import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.text.TextUtils
import android.util.TypedValue
import android.view.*
import android.view.View.OnClickListener
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.hlj.adapter.BaseViewPagerAdapter
import com.hlj.common.CONST
import com.hlj.common.ColumnData
import com.hlj.common.MyApplication
import com.hlj.fragment.*
import com.hlj.manager.SystemStatusManager
import com.hlj.utils.AutoUpdateUtil
import com.hlj.utils.CommonUtil
import kotlinx.android.synthetic.main.activity_main.*
import shawn.cxwl.com.hlj.R
import java.util.*

class MainActivity : BaseFragmentActivity(), OnClickListener {

    private val fragments = ArrayList<Fragment>()
    private var mExitTime: Long = 0 //记录点击完返回按钮后的long型时间
    private var BROADCAST_ACTION_NAME = ""//所有fragment广播名字

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MyApplication.addDestoryActivity(this, "MainActivity")
        setTranslucentStatus()
        initWidget()
        initViewPager()
    }

    /**
     * 设置状态栏背景状态
     */
    private fun setTranslucentStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val win = window
            val winParams = win.attributes
            val bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            winParams.flags = winParams.flags or bits
            win.attributes = winParams
        }
        val tintManager = SystemStatusManager(this)
        tintManager.isStatusBarTintEnabled = true
        tintManager.setStatusBarTintResource(0) // 状态栏无背景
    }

    /**
     * 初始化控件
     */
    private fun initWidget() {
		AutoUpdateUtil.checkUpdate(this, this, "41", getString(R.string.app_name), true)//黑龙江气象
//        AutoUpdateUtil.checkUpdate(this, this, "53", getString(R.string.app_name), true) //决策气象服务
        ivSetting.setOnClickListener(this)
        if (TextUtils.equals(MyApplication.getAppTheme(), "1")) {
            clMain.setBackgroundColor(Color.BLACK)
            ivSetting.setImageBitmap(CommonUtil.grayScaleImage(BitmapFactory.decodeResource(resources, R.drawable.icon_setting)))
        }

        //是否显示登录对话框
        val sp = getSharedPreferences("LOGINDIALOG", MODE_PRIVATE)
        val isFirst = sp.getBoolean("isFirst", true)
        if (isFirst) {
            firstLoginDialog(sp)
        }
    }

    /**
     * 第一次登陆
     */
    private fun firstLoginDialog(sp: SharedPreferences) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_first_login, null)
        val tvSure = view.findViewById<TextView>(R.id.tvSure)
        val dialog = Dialog(this, R.style.CustomProgressDialog)
        dialog.setContentView(view)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        tvSure.setOnClickListener {
            dialog.dismiss()
            val editor = sp.edit()
            editor.putBoolean("isFirst", false)
            editor.apply()
        }
    }

    /**
     * 初始化viewpager
     */
    private fun initViewPager() {
        val columnIds = MyApplication.getColumnIds(this)
        val dataList: MutableList<ColumnData> = ArrayList()
        if (TextUtils.isEmpty(columnIds)) {
            dataList.addAll(MyApplication.columnDataList)
        } else {
            for (i in MyApplication.columnDataList.indices) {
                val item1 = MyApplication.columnDataList[i]
                if (!columnIds.contains(item1.columnId + "--")) { //已经有保存的栏目
                    dataList.add(item1)
                }
            }
        }
        val columnSize = dataList.size
        if (columnSize <= 1) {
            llContainer!!.visibility = View.GONE
            llContainer1.visibility = View.GONE
        }
        fragments.clear()
        llContainer!!.removeAllViews()
        llContainer1.removeAllViews()
        for (i in 0 until columnSize) {
            val channel = dataList[i]
            val tvName = TextView(this)
            tvName.gravity = Gravity.CENTER
            tvName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            tvName.setPadding(0, CommonUtil.dip2px(this, 6f).toInt(), 0, CommonUtil.dip2px(this, 6f).toInt())
            tvName.setOnClickListener(MyOnClickListener(i))
            tvName.setTextColor(Color.WHITE)
            if (!TextUtils.isEmpty(channel.name)) {
                tvName.text = channel.name
            }
            tvName.measure(0, 0)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.width = tvName.measuredWidth
            params.setMargins(CommonUtil.dip2px(this, 10f).toInt(), 0, CommonUtil.dip2px(this, 10f).toInt(), 0)
            tvName.layoutParams = params
            llContainer!!.addView(tvName)
            val tvBar = TextView(this)
            tvBar.gravity = Gravity.CENTER
            tvBar.setOnClickListener(MyOnClickListener(i))
            if (i == 0) {
                tvBar.setBackgroundColor(Color.WHITE)
            } else {
                tvBar.setBackgroundColor(Color.TRANSPARENT)
            }
            val params1 = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params1.width = tvName.measuredWidth
            params1.height = CommonUtil.dip2px(this, 3f).toInt()
            params1.setMargins(CommonUtil.dip2px(this, 10f).toInt(), 0, CommonUtil.dip2px(this, 10f).toInt(), 0)
            tvBar.layoutParams = params1
            llContainer1.addView(tvBar)
            val showType = channel.showType
            val fragment = if (TextUtils.equals(showType, CONST.LOCAL)) {
                val id = channel.id
                if (TextUtils.equals(id, "1")) {
                    ForecastFragment() //首页
                } else if (TextUtils.equals(id, "5")) {
                    WarningFragment() //天气预警
                } else if (TextUtils.equals(id, "2") || TextUtils.equals(id, "3") || TextUtils.equals(id, "4")
                        || TextUtils.equals(id, "9") || TextUtils.equals(id, "10") || TextUtils.equals(id, "7")
                        || TextUtils.equals(id, "8") || TextUtils.equals(id, "13")
                        || TextUtils.equals(id, "11") || TextUtils.equals(id, "14") || TextUtils.equals(id, "15")) {
                    WeatherFactFragment() //天气实况、天气预报、科普宣传、气象服务产品、电力气象服务、铁路气象服务、人工影响天气、森林防火、农业气象
                } else if (TextUtils.equals(id, "106")) {
                    WebviewFragment() //旅游气象
                } else if (TextUtils.equals(id, "12")) {
                    ContactUsFragment() //联系我们
                } else {
                    CommonListFragment()
                }
            } else if (TextUtils.equals(showType, CONST.NEWS)) {
                CommonListFragment()
            } else {
                CommonListFragment()
            }
            val bundle = Bundle()
            bundle.putString(CONST.BROADCAST_ACTION, fragment.javaClass.name + channel.name)
            bundle.putString(CONST.COLUMN_ID, channel.columnId)
            bundle.putString(CONST.ACTIVITY_NAME, channel.name)
            bundle.putString(CONST.WEB_URL, channel.dataUrl)
            bundle.putString(CONST.LOCAL_ID, channel.id)
            bundle.putParcelable("data", channel)
            fragment.arguments = bundle
            fragments.add(fragment)
        }
        viewPager.setSlipping(false) //设置ViewPager是否可以滑动
        viewPager.offscreenPageLimit = fragments.size
        viewPager.setOnPageChangeListener(MyOnPageChangeListener())
        viewPager.adapter = BaseViewPagerAdapter(supportFragmentManager, fragments)
    }

    private inner class MyOnPageChangeListener : OnPageChangeListener {
        override fun onPageSelected(arg0: Int) {
            if (llContainer != null) {
                for (i in 0 until llContainer.childCount) {
                    val tvName = llContainer.getChildAt(i) as TextView
                    if (i == arg0) {
                        val actionName = fragments[arg0].javaClass.name + tvName.text.toString()
                        if (!BROADCAST_ACTION_NAME.contains(actionName)) {
                            val intent = Intent()
                            intent.action = actionName
                            sendBroadcast(intent)
                            BROADCAST_ACTION_NAME += actionName
                        }
                    }
                }
            }
            if (llContainer1 != null) {
                for (i in 0 until llContainer1.childCount) {
                    val tvBar = llContainer1.getChildAt(i) as TextView
                    if (i == arg0) {
                        tvBar.setBackgroundColor(Color.WHITE)
                    } else {
                        tvBar.setBackgroundColor(Color.TRANSPARENT)
                    }
                }
            }
        }
        override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {}
        override fun onPageScrollStateChanged(arg0: Int) {}
    }

    /**
     * 头标点击监听
     * @author shawn_sun
     */
    private inner class MyOnClickListener(private val index: Int) : OnClickListener {
        override fun onClick(v: View) {
            if (viewPager != null) {
                viewPager.setCurrentItem(index, true)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (System.currentTimeMillis() - mExitTime > 2000) {
                Toast.makeText(this, getString(R.string.confirm_exit) + getString(R.string.app_name), Toast.LENGTH_SHORT).show()
                mExitTime = System.currentTimeMillis()
            } else {
                finish()
            }
        }
        return false
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.ivSetting -> startActivityForResult(Intent(this, SettingActivity::class.java), 1001)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                1001 -> initViewPager()
            }
        }
    }

}
