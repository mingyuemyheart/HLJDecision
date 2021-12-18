package com.hlj.utils

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.TextView
import shawn.cxwl.com.hlj.R

/**
 * 动态获取权限
 */
object AuthorityUtil {

    var AUTHOR_MULTI = 1000 //多个权限一起申请
    val AUTHOR_LOCATION = 1001 //定位权限
    val AUTHOR_STORAGE = 1002 //存储权限
    val AUTHOR_PHONE = 1003 //电话权限
    val AUTHOR_CAMERA = 1004 //相机权限
    val AUTHOR_CALL = 1005 //打电话权限

//    public static final int AUTHOR_CONTACTS = 1005;//通讯录权限
//    public static final int AUTHOR_MICROPHONE = 1006;//麦克风权限
//    public static final int AUTHOR_SMS = 1007;//短信权限
//    public static final int AUTHOR_CALENDAR = 1008;//日历权限
//    public static final int AUTHOR_SENSORS = 1009;//传感器权限

    /**
     * 前往权限设置界面
     * @param content
     */
    fun intentAuthorSetting(context: Context, content: String?) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.shawn_dialog_cache, null)
        val tvContent = view.findViewById<TextView>(R.id.tvContent)
        val tvNegtive = view.findViewById<TextView>(R.id.tvNegtive)
        val tvPositive = view.findViewById<TextView>(R.id.tvPositive)
        val dialog = Dialog(context, R.style.CustomProgressDialog)
        dialog.setContentView(view)
        dialog.show()
        tvContent.text = content
        tvNegtive.setOnClickListener { dialog.dismiss() }
        tvPositive.setOnClickListener {
            dialog.dismiss()
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            context.startActivity(intent)
        }
    }

}
