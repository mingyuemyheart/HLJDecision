package com.hlj.activity

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.View.OnClickListener
import com.hlj.dto.AgriDto
import kotlinx.android.synthetic.main.activity_feedback_detail.*
import kotlinx.android.synthetic.main.layout_title.*
import shawn.cxwl.com.hlj.R

/**
 * 意见反馈-详情
 */
class FeedbackDetailActivity : BaseActivity(), OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback_detail)
        initWidget()
    }

    private fun initWidget() {
        llBack.setOnClickListener(this)
        tvTitle.text = "意见反馈详情"

        val data: AgriDto = intent.getParcelableExtra("data")
        if (data != null) {
            if (!TextUtils.isEmpty(data.time)) {
                tvTime.text = data.time
            }
            if (!TextUtils.isEmpty(data.title)) {
                tvContent.text = data.title
            }
            if (!TextUtils.isEmpty(data.reply_content)) {
                tvReply.text = data.reply_content
            }
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.llBack -> finish()
        }
    }

}
