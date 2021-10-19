package com.hlj.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.animation.*
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.hlj.adapter.DisasterUploadAdapter
import com.hlj.common.CONST
import com.hlj.common.MyApplication
import com.hlj.dto.AgriDto
import com.hlj.utils.AuthorityUtil
import com.hlj.utils.CommonUtil
import com.hlj.utils.OkHttpUtil
import com.hlj.view.PhotoView
import kotlinx.android.synthetic.main.activity_ask_anwser_upload.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

/**
 * 提问解答
 */
class AskAnwserUploadActivity : BaseActivity(), OnClickListener {

    private var mAdapter: DisasterUploadAdapter? = null
    private val dataList = ArrayList<AgriDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ask_anwser_upload)
        initWidget()
    }

    private fun initWidget() {
        llBack.setOnClickListener(this)
        tvTitle.text = "提问"
        tvControl.text = "提交"
        tvControl.setOnClickListener(this)
        tvControl.visibility = View.VISIBLE
        etContent.addTextChangedListener(contentWatcher)
        val w = (CommonUtil.widthPixels(this) - CommonUtil.dip2px(this, 24f).toInt()) / 3
        initGridView(w)
    }

    /**
     * 输入内容监听器
     */
    private val contentWatcher: TextWatcher = object : TextWatcher {
        override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
        override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
        override fun afterTextChanged(arg0: Editable) {
            if (etContent.text.isEmpty()) {
                tvTextCount!!.text = "(200字以内)"
            } else {
                val count: Int = 200 - etContent.text.length
                tvTextCount!!.text = "(还可输入" + count + "字)"
            }
        }
    }

    private fun addLastElement() {
        val dto = AgriDto()
        dto.isLastItem = true
        dataList.add(dto)
    }

    private fun initGridView(itemWidth: Int) {
        addLastElement()
        mAdapter = DisasterUploadAdapter(this, dataList, itemWidth)
        gridView.adapter = mAdapter
        gridView.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            val data = dataList[position]
            if (data.isLastItem) { //点击添加按钮
                checkStorageAuthority()
            } else {
                val imgList: ArrayList<String> = ArrayList()
                for (i in dataList.indices) {
                    val d = dataList[i]
                    if (!d.isLastItem) {
                        imgList.add(d.imgUrl)
                    }
                }
                initViewPager(position, imgList)
                if (clViewPager!!.visibility == View.GONE) {
                    scaleExpandAnimation(clViewPager)
                    clViewPager!!.visibility = View.VISIBLE
                    tvCount.text = (position + 1).toString() + "/" + imgList.size
                }
            }
        }
    }

    private fun okHttpPost() {
        showDialog()
        val url = "http://decision-admin.tianqi.cn/Home/workwsj/decisionAddProblem"
        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)
        builder.addFormDataPart("uid", MyApplication.UID)
        builder.addFormDataPart("appid", CONST.APPID)
        if (!TextUtils.isEmpty(etTitle!!.text.toString())) {
            builder.addFormDataPart("title", etTitle!!.text.toString())
        }
        if (!TextUtils.isEmpty(etContent.text.toString())) {
            builder.addFormDataPart("content", etContent.text.toString())
        }
        val size = dataList.size
        if (size > 0) {
            for (i in dataList.indices) {
                val dto = dataList[i]
                if (!TextUtils.isEmpty(dto.imgUrl)) {
                    val imgFile = File(dto.imgUrl)
                    if (imgFile.exists()) {
                        builder.addFormDataPart("img", imgFile.name, imgFile.asRequestBody("image/*".toMediaTypeOrNull()))
                    }
                }
            }
        }
        val body: RequestBody = builder.build()
        Thread {
            OkHttpUtil.enqueue(Request.Builder().post(body).url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("", "")
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    runOnUiThread {
                        try {
                            val obj = JSONObject(result)
                            if (!obj.isNull("status")) {
                                val status = obj.getString("status")
                                if (TextUtils.equals(status, "1")) {
                                    Toast.makeText(this@AskAnwserUploadActivity, "提交成功！", Toast.LENGTH_SHORT).show()
                                    cancelDialog()
                                    setResult(RESULT_OK)
                                    finish()
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }
            })
        }.start()
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.llBack -> finish()
            R.id.tvControl -> {
                if (TextUtils.isEmpty(etTitle!!.text.toString())) {
                    Toast.makeText(this, "请输入问题标题！", Toast.LENGTH_SHORT).show()
                    return
                }
                if (TextUtils.isEmpty(etContent.text.toString())) {
                    Toast.makeText(this, "请输入详情说明！", Toast.LENGTH_SHORT).show()
                    return
                }
                okHttpPost()
            }
        }
    }

    private val maxCount = 1
    private fun intentAlbum() {
        val intent = Intent(this, SelectPictureActivity::class.java)
        intent.putExtra("maxCount", maxCount)
        intent.putExtra("lastCount", dataList.size - 1)
        startActivityForResult(intent, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                1001 -> if (data != null) {
                    val bundle = data.extras
                    if (bundle != null) {
                        if (dataList.size <= 1) {
                            dataList.removeAt(0)
                        } else {
                            dataList.removeAt(dataList.size - 1)
                        }
                        val list: ArrayList<AgriDto> = bundle.getParcelableArrayList("dataList")
                        dataList.addAll(list)
                        addLastElement()
                        if (dataList.size >= (maxCount+1)) {
                            dataList.removeAt(dataList.size - 1)
                        }
                        if (mAdapter != null) {
                            mAdapter!!.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (clViewPager!!.visibility == View.VISIBLE) {
            scaleColloseAnimation(clViewPager)
            clViewPager!!.visibility = View.GONE
            return false
        } else {
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * 初始化viewPager
     */
    private fun initViewPager(current: Int, list: ArrayList<String>) {
        val imageArray = arrayOfNulls<ImageView>(list.size)
        for (i in list.indices) {
            val imgUrl = list[i]
            if (!TextUtils.isEmpty(imgUrl)) {
                val imageView = ImageView(this)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                val bitmap = BitmapFactory.decodeFile(imgUrl)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    imageArray[i] = imageView
                }
            }
        }
        val mViewPager: ViewPager = findViewById(R.id.viewPager)
        val myViewPagerAdapter = MyViewPagerAdapter(imageArray)
        mViewPager.adapter = myViewPagerAdapter
        mViewPager.currentItem = current
        mViewPager.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(arg0: Int) {
                tvCount.text = (arg0 + 1).toString() + "/" + list.size
            }

            override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {}
            override fun onPageScrollStateChanged(arg0: Int) {}
        })
    }

    private inner class MyViewPagerAdapter(private val mImageViews: Array<ImageView?>) : PagerAdapter() {
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
            val photoView = PhotoView(container.context)
            val drawable = mImageViews[position]!!.drawable
            photoView.setImageDrawable(drawable)
            container.addView(photoView, 0)
            photoView.onPhotoTapListener = OnPhotoTapListener { view, v, v1 ->
                scaleColloseAnimation(clViewPager)
                clViewPager.visibility = View.GONE
            }
            return photoView
        }
    }

    /**
     * 放大动画
     * @param view
     */
    private fun scaleExpandAnimation(view: View?) {
        val animationSet = AnimationSet(true)
        val scaleAnimation = ScaleAnimation(0f, 1.0f, 0f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        scaleAnimation.interpolator = LinearInterpolator()
        scaleAnimation.duration = 300
        animationSet.addAnimation(scaleAnimation)
        val alphaAnimation = AlphaAnimation(0f, 1.0f)
        alphaAnimation.duration = 300
        animationSet.addAnimation(alphaAnimation)
        view!!.startAnimation(animationSet)
    }

    /**
     * 缩小动画
     * @param view
     */
    private fun scaleColloseAnimation(view: View?) {
        val animationSet = AnimationSet(true)
        val scaleAnimation = ScaleAnimation(1.0f, 0.0f, 1.0f, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        scaleAnimation.interpolator = LinearInterpolator()
        scaleAnimation.duration = 300
        animationSet.addAnimation(scaleAnimation)
        val alphaAnimation = AlphaAnimation(1.0f, 0f)
        alphaAnimation.duration = 300
        animationSet.addAnimation(alphaAnimation)
        view!!.startAnimation(animationSet)
    }

    /**
     * 申请存储权限
     */
    private fun checkStorageAuthority() {
        if (Build.VERSION.SDK_INT < 23) {
            intentAlbum()
        } else {
            if (ContextCompat.checkSelfPermission(this!!, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                ActivityCompat.requestPermissions(this, permissions, AuthorityUtil.AUTHOR_STORAGE)
            } else {
                intentAlbum()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            AuthorityUtil.AUTHOR_STORAGE -> if (grantResults.isNotEmpty()) {
                var isAllGranted = true //是否全部授权
                for (gResult in grantResults) {
                    if (gResult != PackageManager.PERMISSION_GRANTED) {
                        isAllGranted = false
                        break
                    }
                }
                if (isAllGranted) { //所有权限都授予
                    intentAlbum()
                } else { //只要有一个没有授权，就提示进入设置界面设置
                    AuthorityUtil.intentAuthorSetting(this, "\"" + getString(R.string.app_name) + "\"" + "需要使用您的存储权限，是否前往设置？")
                }
            } else {
                for (permission in permissions) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission!!)) {
                        AuthorityUtil.intentAuthorSetting(this, "\"" + getString(R.string.app_name) + "\"" + "需要使用您的存储权限，是否前往设置？")
                        break
                    }
                }
            }
        }
    }

}
