package com.hlj.activity

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.View.OnClickListener
import android.widget.LinearLayout
import android.widget.TextView
import com.hlj.adapter.BaseViewPagerAdapter
import com.hlj.common.CONST
import com.hlj.fragment.BaseFragment
import com.hlj.fragment.FactQueryAreaFragment
import com.hlj.fragment.FactQueryStationFragment
import com.hlj.utils.CommonUtil
import kotlinx.android.synthetic.main.activity_fact_query.*
import kotlinx.android.synthetic.main.layout_title.*
import shawn.cxwl.com.hlj.R

/**
 * 实况站点查询
 */
class FactQueryActivity : BaseFragmentActivity(), OnClickListener {

    private val fragments: ArrayList<Fragment> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fact_query)
        initWidget()
        initViewPager()
    }

    private fun initWidget() {
        llBack!!.setOnClickListener(this)

        val title = intent.getStringExtra(CONST.ACTIVITY_NAME)
        if (!TextUtils.isEmpty(title)) {
            tvTitle.text = title
        }
    }

    /**
     * 初始化viewPager
     */
    private fun initViewPager() {
        llContainer!!.removeAllViews()
        llContainer1.removeAllViews()
        val names: ArrayList<String> = ArrayList()
        names.add("单站查询")
        names.add("区域查询")
        val size = names.size
        for (i in names.indices) {
            val name = names[i]

            val tvName = TextView(this)
            tvName.gravity = Gravity.CENTER
            tvName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            tvName.setPadding(0, CommonUtil.dip2px(this, 10f).toInt(), 0, CommonUtil.dip2px(this, 10f).toInt())
            tvName.maxLines = 1
            tvName.text = name
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.width = CommonUtil.widthPixels(this)/size
            tvName.layoutParams = params
            tvName.setOnClickListener(MyOnClickListener(i))
            llContainer!!.addView(tvName)

            val tvBar = TextView(this)
            tvBar.gravity = Gravity.CENTER
            val params1 = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params1.setMargins(CommonUtil.dip2px(this, 70f).toInt(), 0, CommonUtil.dip2px(this, 70f).toInt(), 0)
            params1.weight = 1f
            params1.height = CommonUtil.dip2px(this, 3f).toInt()
            params1.gravity = Gravity.CENTER
            tvBar.layoutParams = params1
            llContainer1.addView(tvBar)

            var fragment: BaseFragment? = null
            if (i == 0) {
                fragment = FactQueryStationFragment()
                tvName.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
                tvBar.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
            } else {
                fragment = FactQueryAreaFragment()
                tvName.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                tvBar.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent))
            }

            val bundle = Bundle()
            bundle.putString(CONST.ACTIVITY_NAME, name)
            fragment.arguments = bundle
            fragments.add(fragment)
        }
        viewPager!!.setSlipping(false) //设置ViewPager是否可以滑动
        viewPager!!.offscreenPageLimit = fragments.size
        viewPager!!.setOnPageChangeListener(MyOnPageChangeListener())
        viewPager!!.adapter = BaseViewPagerAdapter(supportFragmentManager, fragments)
    }

    private inner class MyOnPageChangeListener : OnPageChangeListener {
        override fun onPageSelected(arg0: Int) {
            for (j in 0 until llContainer.childCount) {
                val tvName = llContainer.getChildAt(j) as TextView
                val tvBar = llContainer1.getChildAt(j) as TextView
                if (arg0 == j) {
                    tvName.setTextColor(ContextCompat.getColor(this@FactQueryActivity, R.color.colorPrimary))
                    tvBar.setBackgroundColor(ContextCompat.getColor(this@FactQueryActivity, R.color.colorPrimary))
                } else {
                    tvName.setTextColor(ContextCompat.getColor(this@FactQueryActivity, R.color.text_color4))
                    tvBar.setBackgroundColor(ContextCompat.getColor(this@FactQueryActivity, R.color.transparent))
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
    private inner class MyOnClickListener constructor(private val index: Int) : OnClickListener {
        override fun onClick(v: View) {
            if (viewPager != null) {
                viewPager.setCurrentItem(index, true)
            }
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.llBack -> finish()
        }
    }

}
