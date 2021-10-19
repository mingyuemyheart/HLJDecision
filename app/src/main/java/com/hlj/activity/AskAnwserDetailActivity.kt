package com.hlj.activity

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.View.OnClickListener
import com.hlj.dto.AgriDto
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_ask_anwser_detail.*
import kotlinx.android.synthetic.main.layout_title.*
import shawn.cxwl.com.hlj.R

/**
 * 提问解答-详情
 */
class AskAnwserDetailActivity : BaseActivity(), OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ask_anwser_detail)
        initWidget()
    }

    private fun initWidget() {
        llBack.setOnClickListener(this)
        tvTitle.text = "提问解答详情"

        val data: AgriDto = intent.getParcelableExtra("data")
        if (data != null) {
            if (!TextUtils.isEmpty(data.time)) {
                tvTime.text = data.time
            }
            if (!TextUtils.isEmpty(data.title)) {
                tvSubtitle.text = data.title
            }
            if (!TextUtils.isEmpty(data.content)) {
                tvContent.text = data.content
            }
            if (!TextUtils.isEmpty(data.imgUrl)) {
                Picasso.get().load(data.imgUrl).error(R.drawable.icon_no_bitmap).into(imageView)
            } else {
                imageView.setImageResource(R.drawable.icon_no_bitmap)
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
