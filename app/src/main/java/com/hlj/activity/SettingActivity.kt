package com.hlj.activity

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.hlj.common.CONST
import com.hlj.common.MyApplication
import com.hlj.manager.DataCleanManager
import com.hlj.utils.AutoUpdateUtil
import com.hlj.utils.CommonUtil
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.dialog_delete.view.*
import kotlinx.android.synthetic.main.dialog_delete.view.tvContent
import kotlinx.android.synthetic.main.dialog_delete.view.tvMessage
import kotlinx.android.synthetic.main.dialog_delete.view.tvNegtive
import kotlinx.android.synthetic.main.dialog_delete.view.tvPositive
import kotlinx.android.synthetic.main.dialog_prompt.view.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException
import java.util.*

/**
 * 设置
 */
class SettingActivity : BaseActivity(), OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        initWidget()
    }

    /**
     * 初始化控件
     */
    private fun initWidget() {
        llBack!!.setOnClickListener(this)
        llFeedBack!!.setOnClickListener(this)
        llVersion!!.setOnClickListener(this)
        llClearCache!!.setOnClickListener(this)
        llClearData!!.setOnClickListener(this)
        llBuild!!.setOnClickListener(this)
        llCity!!.setOnClickListener(this)
        llProtocal!!.setOnClickListener(this)
        llPolicy!!.setOnClickListener(this)
        tvTitle!!.text = getString(R.string.setting)
        tvLogout!!.setOnClickListener(this)
        tvVersion!!.text = CommonUtil.getVersion(this)
        ivPortrait!!.setOnClickListener(this)
        tvUserName!!.setOnClickListener(this)
        ivPushNews!!.setOnClickListener(this)
        llManage!!.setOnClickListener(this)
        llLogout!!.setOnClickListener(this)

        if (TextUtils.equals(MyApplication.USERNAME, CONST.publicUser) || TextUtils.isEmpty(MyApplication.USERNAME)) { //公众用户或为空
            tvUserName!!.text = "点击登录\n非注册用户"
            tvLogout!!.visibility = View.GONE
        } else {
            tvUserName!!.text = "${MyApplication.USERNAME}\n分组：${MyApplication.UGROUPNAME}"
            tvLogout!!.visibility = View.VISIBLE
        }
        tvCache!!.text = DataCleanManager.getCacheSize(this)
        val push = getSharedPreferences("PUSH_STATE", Context.MODE_PRIVATE)
        val pushState = push.getBoolean("state", true)
        if (pushState) {
            ivPushNews!!.setImageResource(R.drawable.setting_checkbox_on)
        } else {
            ivPushNews!!.setImageResource(R.drawable.setting_checkbox_off)
        }
    }

    /**
     * 删除对话框
     * @param message 标题
     * @param content 内容
     * @param flag 0删除本地存储，1删除缓存
     */
    private fun deleteDialog(flag: Boolean, message: String, content: String, textView: TextView?) {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_delete, null)
        val dialog = Dialog(this, R.style.CustomProgressDialog)
        dialog.setContentView(view)
        dialog.show()
        view.tvMessage.text = message
        view.tvContent.text = content
        view.tvContent.visibility = View.VISIBLE
        view.llNegative.setOnClickListener { dialog.dismiss() }
        view.llPositive.setOnClickListener {
            dialog.dismiss()
            if (flag) {
                DataCleanManager.clearCache(this)
                textView!!.text = DataCleanManager.getCacheSize(this)
            } else {
                DataCleanManager.clearLocalSave(this)
                textView!!.text = DataCleanManager.getLocalSaveSize(this)
            }
        }
    }

    /**
     * 删除对话框
     * @param message 标题
     * @param content 内容
     */
    private fun logout(message: String, content: String) {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_delete, null)
        val dialog = Dialog(this, R.style.CustomProgressDialog)
        dialog.setContentView(view)
        dialog.show()
        view.tvMessage.text = message
        view.tvContent.text = content
        view.tvContent.visibility = View.VISIBLE
        view.llNegative.setOnClickListener { dialog.dismiss() }
        view.llPositive.setOnClickListener {
            dialog.dismiss()
            exit()
        }
    }

    /**
     * 温馨提示对话框
     */
    private fun promptDialog() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_prompt, null)
        val dialog = Dialog(this, R.style.CustomProgressDialog)
        dialog.setContentView(view)
        dialog.show()
        view.tvProtocal.setOnClickListener {
            val intent = Intent(this, WebviewActivity::class.java)
            intent.putExtra(CONST.ACTIVITY_NAME, "用户协议")
            intent.putExtra(CONST.WEB_URL, CONST.yhxy)
            startActivity(intent)
        }
        view.tvPolicy.setOnClickListener {
            val intent = Intent(this, WebviewActivity::class.java)
            intent.putExtra(CONST.ACTIVITY_NAME, "隐私政策")
            intent.putExtra(CONST.WEB_URL, CONST.yszc)
            startActivity(intent)
        }
        view.tvNegtive.setOnClickListener { dialog.dismiss() }
        view.tvPositive.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.llBack -> finish()
            R.id.ivPortrait, R.id.tvUserName -> {
                if (TextUtils.equals(CONST.publicUser, MyApplication.USERNAME)) {
                    promptDialog()
                } else {
                    startActivity(Intent(this, PersonInfoActivity::class.java))
                }
            }
            R.id.llFeedBack -> {
                if (TextUtils.equals(CONST.publicUser, MyApplication.USERNAME)) {
                    Toast.makeText(this, "登录后才可以使用该功能", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(this, FeedbackListActivity::class.java)
                    intent.putExtra(CONST.ACTIVITY_NAME, getString(R.string.setting_feedback))
                    startActivity(intent)
                }
            }
            R.id.llVersion -> {
//                AutoUpdateUtil.checkUpdate(this@SettingActivity, this, "41", getString(R.string.app_name), false) //黑龙江气象
                AutoUpdateUtil.checkUpdate(this@SettingActivity, this, "53", getString(R.string.app_name), false) //决策气象服务
            }
            R.id.llClearCache -> deleteDialog(true, getString(R.string.delete_cache), getString(R.string.sure_delete_cache), tvCache)
            R.id.llClearData -> deleteDialog(false, getString(R.string.delete_data), getString(R.string.sure_delete_data), tvData)
            R.id.llBuild -> {
                val intentBuild = Intent(this, WebviewActivity::class.java)
                intentBuild.putExtra(CONST.ACTIVITY_NAME, getString(R.string.setting_build))
                intentBuild.putExtra(CONST.WEB_URL, CONST.BUILD_URL)
                startActivity(intentBuild)
            }
            R.id.llCity -> startActivity(Intent(this, ReserveCityActivity::class.java))
            R.id.ivPushNews -> {
                val push = getSharedPreferences("PUSH_STATE", Context.MODE_PRIVATE)
                val pushState = push.getBoolean("state", true)
                val editor = push.edit()
                if (pushState) {
                    editor.putBoolean("state", false)
                    editor.apply()
                    ivPushNews!!.setImageResource(R.drawable.setting_checkbox_off)
                    MyApplication.disablePush()
                } else {
                    editor.putBoolean("state", true)
                    editor.commit()
                    ivPushNews!!.setImageResource(R.drawable.setting_checkbox_on)
                    MyApplication.enablePush()
                }
            }
            R.id.llProtocal -> {
                val intent = Intent(this, WebviewActivity::class.java)
                intent.putExtra(CONST.ACTIVITY_NAME, "用户协议")
                intent.putExtra(CONST.WEB_URL, "http://decision-admin.tianqi.cn/Public/share/hlj_htmls/yhxy.html")
                startActivity(intent)
            }
            R.id.llPolicy -> {
                val intent = Intent(this, WebviewActivity::class.java)
                intent.putExtra(CONST.ACTIVITY_NAME, "隐私政策")
                intent.putExtra(CONST.WEB_URL, "http://decision-admin.tianqi.cn/Public/share/hlj_htmls/yscl.html")
                startActivity(intent)
            }
            R.id.llManage -> {
                if (TextUtils.equals(CONST.publicUser, MyApplication.USERNAME)) {
                    Toast.makeText(this, "登录后才可以使用该功能", Toast.LENGTH_SHORT).show()
                } else {
                    startActivityForResult(Intent(this, ManageActivity::class.java), 1001)
                }
            }
            R.id.llLogout -> dialogRemove("确定注销该账号？")
            R.id.tvLogout -> logout(getString(R.string.logout), getString(R.string.sure_logout))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                1001 -> {
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }

    private fun dialogRemove(message: String) {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_delete, null)
        val tvMessage = view.findViewById<View>(R.id.tvMessage) as TextView
        val llNegative = view.findViewById<View>(R.id.llNegative) as LinearLayout
        val llPositive = view.findViewById<View>(R.id.llPositive) as LinearLayout
        val dialog = Dialog(this, R.style.CustomProgressDialog)
        dialog.setContentView(view)
        dialog.show()
        tvMessage.text = message
        llNegative.setOnClickListener { dialog.dismiss() }
        llPositive.setOnClickListener {
            dialog.dismiss()
            okhttpRemoveUser()
        }
    }

    /**
     * 注销账号
     */
    private fun okhttpRemoveUser() {
        showDialog()
        val url = "http://decision-admin.tianqi.cn/Home/work2019/xz_user_remove"
        val builder = FormBody.Builder()
        builder.add("id", MyApplication.UID)
        builder.add("appid", CONST.APPID)
        val body: RequestBody = builder.build()
        Thread {
            OkHttpUtil.enqueue(Request.Builder().post(body).url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        cancelDialog()
                        Toast.makeText(this@SettingActivity, "注销账号失败", Toast.LENGTH_SHORT).show()
                    }
                }

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
                                if (!`object`.isNull("status")) {
                                    val status = `object`.getInt("status")
                                    if (status == 1) {
                                        Toast.makeText(this@SettingActivity, "账号注销成功", Toast.LENGTH_SHORT).show()
                                        exit()
                                    } else {
                                        val msg = `object`.getString("msg")
                                        if (msg != null) {
                                            Toast.makeText(this@SettingActivity, "账号注销成功", Toast.LENGTH_SHORT).show()
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

    private fun exit() {
        MyApplication.clearUserInfo(this)
        MyApplication.destoryActivity()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

}
