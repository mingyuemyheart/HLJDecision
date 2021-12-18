package com.hlj.activity

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.View.OnClickListener
import android.widget.Toast
import com.hlj.common.CONST
import com.hlj.dto.WarningDto
import com.hlj.manager.DBManager
import com.hlj.utils.CommonUtil
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.fragment_warning_detail.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException
import java.util.*

/**
 * 预警详情
 */
class WarningDetailActivity : BaseActivity(), OnClickListener{

	private var isBigText = false
	private var data: WarningDto? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_warning_detail)
		showDialog()
		initWidget()
	}

	/**
	 * 初始化控件
	 */
	private fun initWidget() {
		tvTitle.text = "预警详情"
		llBack.setOnClickListener(this)
		ivControl.setImageResource(R.drawable.icon_share)
		ivControl.setOnClickListener(this)
		ivControl2.setImageResource(R.drawable.icon_text)
		ivControl2.visibility = View.VISIBLE
		ivControl2.setOnClickListener(this)

		if (intent.hasExtra("data")) {
			data = intent.getParcelableExtra("data")
			okHttpWarningDetail()
		}
	}

	private fun okHttpWarningDetail() {
		if (data == null || TextUtils.isEmpty(data!!.html)) {
			return
		}
		Thread {
			val url = "http://decision.tianqi.cn/alarm12379/content2/${data!!.html}"
			OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
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
								val obj = JSONObject(result)
								if (!obj.isNull("sendTime")) {
									tvTime!!.text = obj.getString("sendTime")
								}
								if (!obj.isNull("description")) {
									tvIntro!!.text = obj.getString("description")
								}
								if (!obj.isNull("headline")) {
									tvName!!.text = obj.getString("headline")
								}
								var bitmap: Bitmap? = null
								if (obj.getString("severityCode") == CONST.blue[0]) {
									bitmap = CommonUtil.getImageFromAssetsFile(this@WarningDetailActivity, "warning/" + obj.getString("eventType") + CONST.blue[1] + CONST.imageSuffix)
									if (bitmap == null) {
										bitmap = CommonUtil.getImageFromAssetsFile(this@WarningDetailActivity, "warning/" + "default" + CONST.blue[1] + CONST.imageSuffix)
									}
								} else if (obj.getString("severityCode") == CONST.yellow[0]) {
									bitmap = CommonUtil.getImageFromAssetsFile(this@WarningDetailActivity, "warning/" + obj.getString("eventType") + CONST.yellow[1] + CONST.imageSuffix)
									if (bitmap == null) {
										bitmap = CommonUtil.getImageFromAssetsFile(this@WarningDetailActivity, "warning/" + "default" + CONST.yellow[1] + CONST.imageSuffix)
									}
								} else if (obj.getString("severityCode") == CONST.orange[0]) {
									bitmap = CommonUtil.getImageFromAssetsFile(this@WarningDetailActivity, "warning/" + obj.getString("eventType") + CONST.orange[1] + CONST.imageSuffix)
									if (bitmap == null) {
										bitmap = CommonUtil.getImageFromAssetsFile(this@WarningDetailActivity, "warning/" + "default" + CONST.orange[1] + CONST.imageSuffix)
									}
								} else if (obj.getString("severityCode") == CONST.red[0]) {
									bitmap = CommonUtil.getImageFromAssetsFile(this@WarningDetailActivity, "warning/" + obj.getString("eventType") + CONST.red[1] + CONST.imageSuffix)
									if (bitmap == null) {
										bitmap = CommonUtil.getImageFromAssetsFile(this@WarningDetailActivity, "warning/" + "default" + CONST.red[1] + CONST.imageSuffix)
									}
								}
								imageView!!.setImageBitmap(bitmap)
								initDBManager()
								ivControl.visibility = View.VISIBLE
							} catch (e: JSONException) {
								e.printStackTrace()
							}
						}
					}
				}
			})
		}.start()
	}

	/**
	 * 初始化数据库
	 */
	private fun initDBManager() {
		val dbManager = DBManager(this)
		dbManager.openDateBase()
		dbManager.closeDatabase()
		val database = SQLiteDatabase.openOrCreateDatabase(DBManager.DB_PATH + "/" + DBManager.DB_NAME, null)
		var cursor: Cursor? = null
		cursor = database.rawQuery("select * from " + DBManager.TABLE_NAME2 + " where WarningId = " + "\"" + data!!.type + data!!.color + "\"", null)
		var content: String? = null
		for (i in 0 until cursor.count) {
			cursor.moveToPosition(i)
			content = cursor.getString(cursor.getColumnIndex("WarningGuide"))
		}
		if (!TextUtils.isEmpty(content)) {
			tvGuide!!.text = getString(R.string.warning_guide).toString() + content
			tvGuide!!.visibility = View.VISIBLE
		} else {
			tvGuide!!.visibility = View.GONE
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.llBack -> finish()
			R.id.ivControl2 -> {
				isBigText = !isBigText
				if (isBigText) {
					tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
					tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
					tvIntro.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
					tvGuide.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
				} else {
					tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
					tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
					tvIntro.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
					tvGuide.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
				}
			}
			R.id.ivControl -> {
				share()
			}
		}
	}

	private fun share() {
		val bitmap1 = CommonUtil.captureScrollView(scrollView)
		val bitmap2 = BitmapFactory.decodeResource(resources, R.drawable.legend_share_portrait)
		val bitmap = CommonUtil.mergeBitmap(this, bitmap1, bitmap2, false)
		CommonUtil.clearBitmap(bitmap1)
		CommonUtil.clearBitmap(bitmap2)
		CommonUtil.share(this@WarningDetailActivity, bitmap)
	}

}
