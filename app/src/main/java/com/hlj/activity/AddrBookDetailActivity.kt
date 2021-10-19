package com.hlj.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import com.hlj.common.CONST
import com.hlj.dto.ContactDto
import com.hlj.utils.AuthorityUtil
import com.hlj.utils.CommonUtil
import kotlinx.android.synthetic.main.activity_addrbook_detail.*
import kotlinx.android.synthetic.main.layout_title.*
import shawn.cxwl.com.hlj.R

/**
 * 通讯录-详情
 */
class AddrBookDetailActivity : BaseActivity(), View.OnClickListener {

    private var dialNumber = ""

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
                    dialNumber = data.worktelephone
                    checkPhoneAuthority()
                }
                tvPhoneCopy.setOnClickListener {
                    CommonUtil.copy(this, data.worktelephone)
                    Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
                }
            }
            if (!TextUtils.isEmpty(data.mobile)) {
                tvMobile.text = data.mobile
                tvMobileDial.setOnClickListener {
                    dialNumber = data.mobile
                    checkPhoneAuthority()
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

    /**
     * 申请电话权限
     */
    private fun checkPhoneAuthority() {
        if (Build.VERSION.SDK_INT < 23) {
            try {
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$dialNumber")))
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) !== PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), AuthorityUtil.AUTHOR_PHONE)
            } else {
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$dialNumber")))
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            AuthorityUtil.AUTHOR_PHONE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$dialNumber")))
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE)) {
                    AuthorityUtil.intentAuthorSetting(this, "\"" + getString(R.string.app_name) + "\"" + "需要使用电话权限，是否前往设置？")
                }
            }
        }
    }
	
}
