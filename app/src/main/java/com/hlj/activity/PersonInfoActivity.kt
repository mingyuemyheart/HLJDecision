package com.hlj.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.View.OnClickListener
import com.hlj.common.MyApplication
import kotlinx.android.synthetic.main.activity_person_info.*
import kotlinx.android.synthetic.main.layout_title.*
import shawn.cxwl.com.hlj.R

/**
 * 用户信息
 */
class PersonInfoActivity : BaseActivity(), OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_person_info)
        initWidget()
    }

    /**
     * 初始化控件
     */
    private fun initWidget() {
        llBack.setOnClickListener(this)
        tvTitle.text = "用户信息"
        llNickName.setOnClickListener(this)
        refreshUserinfo()
    }

    private fun refreshUserinfo() {
        if (!TextUtils.isEmpty(MyApplication.NAME)) {
            tvNickName!!.text = MyApplication.NAME
        }
        if (!TextUtils.isEmpty(MyApplication.MOBILE)) {
            tvMobile.text = MyApplication.MOBILE
        }
        if (!TextUtils.isEmpty(MyApplication.DEPARTMENT)) {
            tvUnit.text = MyApplication.DEPARTMENT
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.llBack -> finish()
            R.id.llNickName -> {
                val intent = Intent(this, ModifyInfoActivity::class.java)
                intent.putExtra("title", "姓名")
                intent.putExtra("content", MyApplication.NAME)
                startActivityForResult(intent, 1001)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                1001 -> if (!TextUtils.isEmpty(MyApplication.NAME)) {
                    tvNickName!!.text = MyApplication.NAME
                }
            }
        }
    }
	
}
