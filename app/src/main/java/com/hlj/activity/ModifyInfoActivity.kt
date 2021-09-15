package com.hlj.activity

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.View.OnClickListener
import android.widget.Toast
import com.hlj.common.MyApplication
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.activity_modify_info.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException

/**
 * 修改用户信息
 */
class ModifyInfoActivity : BaseActivity(), OnClickListener {

    private var title: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify_info)
        initWidget()
    }

    /**
     * 初始化控件
     */
    private fun initWidget() {
        llBack.setOnClickListener(this)
        tvControl.setOnClickListener(this)
        tvControl.visibility = View.VISIBLE
        tvControl.text = "完成"
        if (intent.hasExtra("title")) {
            title = intent.getStringExtra("title")
            if (title != null) {
                tvTitle.text = title
            }
        }
        if (intent.hasExtra("content")) {
            val content = intent.getStringExtra("content")
            if (content != null) {
                etContent.setText(content)
                etContent.setSelection(content.length)
            }
        }
    }

    /**
     * 修改用户信息
     */
    private fun okHttpModify() {
        val url = "http://decision-admin.tianqi.cn/home/work2019/user_update"
        val builder = FormBody.Builder()
        builder.add("id", MyApplication.UID)
        if (TextUtils.equals(title, "姓名")) {
            builder.add("name", etContent!!.text.toString().trim { it <= ' ' })
        }
        val body: RequestBody = builder.build()
        Thread {
            OkHttpUtil.enqueue(Request.Builder().post(body).url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    runOnUiThread {
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                val `object` = JSONObject(result)
                                if (!`object`.isNull("status")) {
                                    val status = `object`.getInt("status")
                                    if (status == 1) { //成功
                                        if (!`object`.isNull("user")) {
                                            val obj = `object`.getJSONObject("user")
                                            if (!obj.isNull("usergroup")) {
                                                MyApplication.GROUPID = obj.getString("usergroup")
                                            }
                                            if (!obj.isNull("id")) {
                                                MyApplication.UID = obj.getString("id")
                                            }
                                            if (!obj.isNull("mobile")) {
                                                MyApplication.USERNAME = obj.getString("mobile")
                                            }
                                            if (!obj.isNull("name")) {
                                                MyApplication.NAME = obj.getString("name")
                                            }
                                            if (!obj.isNull("department")) {
                                                MyApplication.DEPARTMENT = obj.getString("department")
                                            }
                                            MyApplication.saveUserInfo(this@ModifyInfoActivity)
                                            setResult(RESULT_OK)
                                            finish()
                                        }
                                    } else { //失败
                                        if (!`object`.isNull("msg")) {
                                            Toast.makeText(this@ModifyInfoActivity, `object`.getString("msg"), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            })
        }.start()
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.llBack -> finish()
            R.id.tvControl -> if (!TextUtils.isEmpty(etContent!!.text.toString().trim { it <= ' ' })) {
                okHttpModify()
            } else {
                if (title != null) {
                    Toast.makeText(this, "请输入$title", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
	
}
