package com.hlj.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.view.View
import android.view.View.OnClickListener
import android.widget.Toast
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.hlj.common.CONST
import com.hlj.common.MyApplication
import com.hlj.utils.AuthorityUtil
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.activity_pdf.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import shawn.cxwl.com.hlj.R
import java.io.*
import java.net.URLEncoder
import java.util.*
import kotlin.math.floor

/**
 * PDF文档
 */
class PDFActivity : BaseActivity(), OnClickListener {

	private var dataUrl = ""
	private var filePath = ""

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_pdf)
		checkAuthority()
	}

	private fun init() {
		initWidget()
		initPDFView()
	}

	private fun initWidget() {
		llBack.setOnClickListener(this)
		tvTitle.text = "详情"
		ivControl.setImageResource(R.drawable.icon_download)
		ivControl.setOnClickListener(this)

//		if (intent.hasExtra(CONST.ACTIVITY_NAME)) {
//			title = intent.getStringExtra(CONST.ACTIVITY_NAME)
//			if (!TextUtils.isEmpty(title)) {
//				tvTitle.text = title
//			}
//		}
	}

	// 根据Unicode编码完美的判断中文汉字和符号
	private fun isChinese(c: Char): Boolean {
		val ub = Character.UnicodeBlock.of(c)
		return ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub === Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS || ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B || ub === Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub === Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS || ub === Character.UnicodeBlock.GENERAL_PUNCTUATION
	}

	// 完整的判断中文汉字和符号
	private fun isChinese(name: String): String {
		var strName = name
		val ch = strName.toCharArray()
		for (c in ch) {
			if (isChinese(c)) {
				try {
					strName = strName.replace(c.toString() + "", URLEncoder.encode(c.toString() + "", "UTF-8"))
				} catch (e: UnsupportedEncodingException) {
					e.printStackTrace()
				}
			}
		}
		return strName
	}

	private fun initPDFView() {
		if (!intent.hasExtra(CONST.WEB_URL)) {
			return
		}
		dataUrl = intent.getStringExtra(CONST.WEB_URL)
		dataUrl = if (TextUtils.isEmpty(dataUrl)) {
			return
		} else {
			isChinese(dataUrl)
		}
		okHttpFile(dataUrl)
	}

	private fun okHttpFile(url: String) {
		if (TextUtils.isEmpty(url)) {
			return
		}
		Thread {
			OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
				override fun onFailure(call: Call, e: IOException) {}

				@Throws(IOException::class)
				override fun onResponse(call: Call, response: Response) {
					if (!response.isSuccessful) {
						return
					}
					var `is`: InputStream? = null
					var fos: FileOutputStream? = null
					try {
						`is` = response.body!!.byteStream() //获取输入流
						val total = response.body!!.contentLength().toFloat() //获取文件大小
						if (`is` != null) {
							val files = File("${getExternalFilesDir(null)}/HljDecision")
							if (!files.exists()) {
								files.mkdirs()
							}
							filePath = "${files.absolutePath}/1.pdf"
							if (intent.hasExtra(CONST.ACTIVITY_NAME)) {
								val title = intent.getStringExtra(CONST.ACTIVITY_NAME)
								filePath = "${files.absolutePath}/$title.pdf"
							}
							fos = FileOutputStream(filePath)
							val buf = ByteArray(1024)
							var ch = -1
							var process = 0
							while (`is`.read(buf).also { ch = it } != -1) {
								fos.write(buf, 0, ch)
								process += ch
								val percent = floor((process / total * 100).toDouble()).toInt()
								val msg = handler.obtainMessage(1001)
								msg.what = 1001
								msg.obj = filePath
								msg.arg1 = percent
								handler.sendMessage(msg)
							}
						}
						fos!!.flush()
						fos.close() // 下载完成
					} catch (e: Exception) {
						e.printStackTrace()
					} finally {
						`is`?.close()
						fos?.close()
					}
				}
			})
		}.start()
	}

	@SuppressLint("HandlerLeak")
	private val handler: Handler = object : Handler() {
		override fun handleMessage(msg: Message) {
			if (msg.what == 1001) {
				if (tvPercent == null || pdfView == null) {
					return
				}
				val percent = msg.arg1
				tvPercent.text = "$percent${getString(R.string.unit_percent)}"
				if (percent >= 100) {
					if (TextUtils.equals(MyApplication.JC_DOWNLOAD, "1")) {
						ivControl.visibility = View.VISIBLE
					}
					tvPercent!!.visibility = View.GONE
					val filePath = msg.obj.toString() + ""
					if (!TextUtils.isEmpty(filePath)) {
						val file = File(msg.obj.toString() + "")
						if (file.exists()) {
							pdfView!!.fromFile(file)
									.defaultPage(0)
									.scrollHandle(DefaultScrollHandle(this@PDFActivity))
									.onPageChange { page, pageCount ->

									}
									.load()
						}
					}
				}
			}
		}
	}

	override fun onClick(v: View?) {
		when (v!!.id) {
			R.id.llBack -> finish()
			R.id.ivControl -> Toast.makeText(this, "已保存至$filePath", Toast.LENGTH_LONG).show()
		}
	}

	//需要申请的所有权限
	private val allPermissions = arrayOf(
			Manifest.permission.WRITE_EXTERNAL_STORAGE
	)

	//拒绝的权限集合
	private val deniedList: MutableList<String> = ArrayList()

	/**
	 * 申请定位权限
	 */
	private fun checkAuthority() {
		if (Build.VERSION.SDK_INT < 23) {
			init()
		} else {
			deniedList.clear()
			for (permission in allPermissions) {
				if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
					deniedList.add(permission)
				}
			}
			if (deniedList.isEmpty()) { //所有权限都授予
				init()
			} else {
				val permissions = deniedList.toTypedArray() //将list转成数组
				ActivityCompat.requestPermissions(this, permissions, AuthorityUtil.AUTHOR_STORAGE)
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
					init()
				} else { //只要有一个没有授权，就提示进入设置界面设置
					AuthorityUtil.intentAuthorSetting(this, "\"" + getString(R.string.app_name) + "\"" + "需要使用存储权限，是否前往设置？")
				}
			} else {
				for (permission in permissions) {
					if (!ActivityCompat.shouldShowRequestPermissionRationale(this@PDFActivity, permission!!)) {
						AuthorityUtil.intentAuthorSetting(this, "\"" + getString(R.string.app_name) + "\"" + "需要使用存储权限，是否前往设置？")
						break
					}
				}
			}
		}
	}

}
