package com.hlj.activity

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.View.OnClickListener
import android.widget.Toast
import com.hlj.common.CONST
import com.hlj.common.MyApplication
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.activity_feedback.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException

/**
 * 意见反馈
 */
class FeedbackActivity : BaseActivity(), OnClickListener{

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)
        initWidget()
    }

    private fun initWidget() {
        llBack!!.setOnClickListener(this)
        tvControl!!.text = getString(R.string.submit)
        tvControl!!.visibility = View.VISIBLE
        tvControl!!.setOnClickListener(this)
        val title = intent.getStringExtra(CONST.ACTIVITY_NAME)
        if (title != null) {
            tvTitle!!.text = title
        }
    }

    private fun okHttpFeedBack() {
        val url = "http://decision-admin.tianqi.cn/Home/Work/request"
        val builder = FormBody.Builder()
        builder.add("uid", MyApplication.UID)
        builder.add("content", etContent!!.text.toString())
        builder.add("appid", CONST.APPID)
        val body: RequestBody = builder.build()
        Thread(Runnable {
            OkHttpUtil.enqueue(Request.Builder().post(body).url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    runOnUiThread {
                        cancelDialog()
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                val `object` = JSONObject(result)
                                if (`object` != null) {
                                    if (!`object`.isNull("status")) {
                                        val status = `object`.getInt("status")
                                        if (status == 1) { //成功
                                            Toast.makeText(this@FeedbackActivity, getString(R.string.submit_success), Toast.LENGTH_SHORT).show()
                                            finish()
                                        } else {
                                            //失败
                                            if (!`object`.isNull("msg")) {
                                                val msg = `object`.getString("msg")
                                                if (msg != null) {
                                                    Toast.makeText(this@FeedbackActivity, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
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
        }).start()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.llBack -> {
                finish()
            }
            R.id.tvControl -> {
                if (TextUtils.isEmpty(etContent!!.text.toString())) {
                    return
                }
                showDialog()
                okHttpFeedBack()
            }
        }
    }

}
