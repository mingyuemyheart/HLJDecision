package com.hlj.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import com.hlj.common.CONST
import com.hlj.dto.ContactDto
import com.hlj.utils.CommonUtil
import kotlinx.android.synthetic.main.activity_addrbook_detail.*
import kotlinx.android.synthetic.main.layout_title.*
import shawn.cxwl.com.hlj.R

/**
 * 通讯录-详情
 */
class AddrBookDetailActivity : BaseActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addrbook_detail)
        initWidget()
    }

    private fun initWidget() {
        llBack.setOnClickListener(this)

        val title = intent.getStringExtra(CONST.ACTIVITY_NAME)
        if (!TextUtils.isEmpty(title)) {
            tvTitle.text = title
        }

        val level = intent.getStringExtra("level")
        if (!TextUtils.isEmpty(level)) {
            tvLevel.text = level
        }

        if (intent.hasExtra("data")) {
            val data: ContactDto = intent.getParcelableExtra("data")
            if (data.name != null) {
                tvName.text = data.name
            }
            if (data.level != null) {
                tvUnit.text = data.level
            }
            if (data.duties != null) {
                tvJob.text = data.duties
            }
            if (!TextUtils.isEmpty(data.worktelephone)) {
                tvPhone.text = data.worktelephone
                tvPhoneDial.setOnClickListener {
                    checkCallAuthority(object : CallCallback {
                        override fun grantedCall(isGranted: Boolean) {
                            if (isGranted) {
                                try {
                                    startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${data.worktelephone}")))
                                } catch (e: SecurityException) {
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.makeText(this@AddrBookDetailActivity, "需要开启电话权限", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                }
                tvPhoneCopy.setOnClickListener {
                    CommonUtil.copy(this, data.worktelephone)
                    Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
                }
            }
            if (!TextUtils.isEmpty(data.mobile)) {
                tvMobile.text = data.mobile
                tvMobileDial.setOnClickListener {
                    checkCallAuthority(object : CallCallback {
                        override fun grantedCall(isGranted: Boolean) {
                            if (isGranted) {
                                try {
                                    startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${data.mobile}")))
                                } catch (e: SecurityException) {
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.makeText(this@AddrBookDetailActivity, "需要开启电话权限", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                }
                tvMobileCopy.setOnClickListener {
                    CommonUtil.copy(this, data.mobile)
                    Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.llBack -> finish()
        }
    }

}
