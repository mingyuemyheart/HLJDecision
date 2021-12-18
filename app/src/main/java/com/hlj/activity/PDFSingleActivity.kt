package com.hlj.activity

import android.annotation.SuppressLint
import android.os.*
import android.text.TextUtils
import android.view.View
import android.view.View.OnClickListener
import android.widget.Toast
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.hlj.common.CONST
import com.hlj.common.MyApplication
import com.hlj.dto.AgriDto
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.activity_pdf.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.*
import java.net.URLEncoder
import java.util.*
import kotlin.math.floor

/**
 * PDF文档，森林防火，单个pdf
 */
class PDFSingleActivity : BaseActivity(), OnClickListener {

    private var filePath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf)
        checkStorageAuthority(object : StorageCallback {
            override fun grantedStorage(isGranted: Boolean) {
                if (isGranted) {
                    initWidget()
                } else {
                    tvPercent.visibility = View.GONE
                    Toast.makeText(this@PDFSingleActivity, "需要开启存储权限", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun initWidget() {
        llBack.setOnClickListener(this)
        tvTitle!!.text = "详情"
        ivControl.setImageResource(R.drawable.icon_download)
        ivControl.setOnClickListener(this)

//        if (intent.hasExtra(CONST.ACTIVITY_NAME)) {
//            title = intent.getStringExtra(CONST.ACTIVITY_NAME)
//            if (!TextUtils.isEmpty(title)) {
//                tvTitle.text = title
//            }
//        }

        val dto: AgriDto = intent.extras.getParcelable("data")
        if (dto != null) {
            if (!TextUtils.isEmpty(dto.dataUrl)) {
                okHttpDetail(dto.dataUrl)
            }
        }
    }

    /**
     * 获取详情
     */
    private fun okHttpDetail(url: String) {
        Thread(Runnable {
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    runOnUiThread(Runnable {
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                val obj = JSONObject(result)
                                if (!obj.isNull("info")) {
                                    val array = obj.getJSONArray("info")
                                    if (array.length() > 0) {
                                        var pdfUrl = array.getString(0)
                                        pdfUrl = if (TextUtils.isEmpty(pdfUrl)) {
                                            return@Runnable
                                        } else {
                                            isChinese(pdfUrl)
                                        }
                                        pdfUrl = isChinese(pdfUrl)
                                        okHttpFile(pdfUrl)
                                    }
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                        cancelDialog()
                    })
                }
            })
        }).start()
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
                                    .scrollHandle(DefaultScrollHandle(this@PDFSingleActivity))
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
        }
    }

}
