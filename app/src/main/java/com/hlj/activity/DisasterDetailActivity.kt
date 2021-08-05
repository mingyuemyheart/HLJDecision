package com.hlj.activity

import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.text.TextUtils
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.hlj.dto.DisasterDto
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_disaster_detail.*
import kotlinx.android.synthetic.main.layout_title.*
import shawn.cxwl.com.hlj.R

/**
 * 灾情反馈-详情
 */
class DisasterDetailActivity : BaseActivity(), OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disaster_detail)
        initWidget()
    }

    private fun initWidget() {
        llBack.setOnClickListener(this)
        tvTitle.text = "灾情详情"

        val data: DisasterDto = intent.getParcelableExtra("data")
        if (data != null) {
            if (data.imgList.size > 0) {
                val size = data.imgList.size
                for (i in 0 until size) {
                    val imgUrl = data.imgList[i]
                    val imageView = ImageView(this)
                    imageView.tag = imgUrl
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    imageView.adjustViewBounds = true
                    val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
                    imageView.layoutParams = params
                    Picasso.get().load(imgUrl).into(imageView)
                }
                initViewPager(0, data.imgList)
            }
            if (!TextUtils.isEmpty(data.title)) {
                tvSubtitle.text = data.title
            }
            if (!TextUtils.isEmpty(data.time)) {
                tvTime.text = data.time + " 发布"
            }
            if (!TextUtils.isEmpty(data.addr)) {
                tvAddr.text = data.addr
            }
            if (!TextUtils.isEmpty(data.disasterType)) {
                tvType.text = data.disasterType
            }
            if (!TextUtils.isEmpty(data.createtime)) {
                tvDisTime.text = data.createtime
            }
            if (!TextUtils.isEmpty(data.content)) {
                tvContent.text = data.content
            }
        }
    }

    /**
     * 初始化viewPager
     */
    private fun initViewPager(current: Int, list: ArrayList<String>) {
        val imageArray: ArrayList<ImageView> = ArrayList()
        for (i in list.indices) {
            val imgUrl = list[i]
            if (!TextUtils.isEmpty(imgUrl)) {
                val imageView = ImageView(this)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                Picasso.get().load(imgUrl).into(imageView)
                imageArray.add(imageView)
            }
        }
        val myViewPagerAdapter = MyViewPagerAdapter(imageArray)
        viewPager.adapter = myViewPagerAdapter
        viewPager.currentItem = current
        viewPager.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(arg0: Int) {}
            override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {}
            override fun onPageScrollStateChanged(arg0: Int) {}
        })
    }

    private class MyViewPagerAdapter(private val mImageViews: ArrayList<ImageView>) : PagerAdapter() {
        override fun getCount(): Int {
            return mImageViews.size
        }

        override fun isViewFromObject(arg0: View, arg1: Any): Boolean {
            return arg0 === arg1
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(mImageViews[position])
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            container.addView(mImageViews[position])
            return mImageViews[position]
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.llBack -> finish()
        }
    }

}
