package com.hlj.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import com.hlj.adapter.AddrBookAdapter
import com.hlj.adapter.AddrTypeAdapter
import com.hlj.common.CONST
import com.hlj.dto.ContactDto
import com.hlj.utils.AuthorityUtil
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.activity_addrbook.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException

/**
 * 通讯录
 */
class AddrBookActivity : BaseActivity(), View.OnClickListener {

    private var typeAdapter: AddrTypeAdapter? = null
    private val typeList: ArrayList<ContactDto> = ArrayList()
    private var mAdapter: AddrBookAdapter? = null
    private val dataList: ArrayList<ContactDto> = ArrayList()
    private var dialNumber = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addrbook)
        initWidget()
        initGridView()
        initListView()
    }

    private fun initWidget() {
        llBack.setOnClickListener(this)

        val title = intent.getStringExtra(CONST.ACTIVITY_NAME)
        if (!TextUtils.isEmpty(title)) {
            tvTitle.text = title
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                for (i in 0 until dataList.size) {
                    val dto = dataList[i]
                    if (dto.name.startsWith(s.toString())) {
                        listView.setSelection(i)
                        break
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        okHttpList()
    }

    private fun initGridView() {
        typeAdapter = AddrTypeAdapter(this, typeList)
        gridView.adapter = typeAdapter
        gridView.setOnItemClickListener { parent, view, position, id ->
            val data = typeList[position]
            for (i in 0 until dataList.size) {
                val dto = dataList[i]
                if (dto.company.startsWith(data.company)) {
                    listView.setSelection(i)
                    break
                }
            }
        }
    }

    private fun initListView() {
        mAdapter = AddrBookAdapter(this, dataList)
        listView!!.adapter = mAdapter
        listView!!.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 ->
            val data = dataList[arg2]
            dialNumber = data.number
            checkPhoneAuthority()
        }

        slideBar.setOnTouchLetterChangeListenner { isTouch, letter ->
            for (i in 0 until dataList.size) {
                val dto = dataList[i]
                if (TextUtils.equals(dto.prefix, letter)) {
                    listView.setSelection(i)
                    break
                }
            }
        }
    }

    /**
     * 获取资讯信息
     */
    private fun okHttpList() {
        Thread {
            val url = "https://decision-admin.tianqi.cn/Home/workwsj/getPhoneBook"
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
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
                                val obj = JSONObject(result)

                                if (!obj.isNull("type")) {
                                    typeList.clear()
                                    val array = obj.getJSONArray("type")
                                    for (i in 0 until array.length()) {
                                        val itemObj = array.getJSONObject(i)
                                        val dto = ContactDto()
                                        if (!itemObj.isNull("company")) {
                                            dto.company = itemObj.getString("company")
                                        }
                                        typeList.add(dto)
                                    }
                                    if (typeAdapter != null) {
                                        typeAdapter!!.notifyDataSetChanged()
                                    }
                                }

                                if (!obj.isNull("data")) {
                                    dataList.clear()
                                    val array = obj.getJSONArray("data")
                                    for (i in 0 until array.length()) {
                                        val itemObj = array.getJSONObject(i)
                                        val dto = ContactDto()
                                        if (!itemObj.isNull("worktelephone")) {
                                            dto.number = itemObj.getString("worktelephone")
                                        }
                                        if (!itemObj.isNull("name")) {
                                            dto.name = itemObj.getString("name")
                                        }
                                        if (!itemObj.isNull("company")) {
                                            dto.company = itemObj.getString("company")
                                        }
                                        if (!itemObj.isNull("letter")) {
                                            dto.prefix = itemObj.getString("letter")
                                        }
                                        dataList.add(dto)
                                    }
                                    if (mAdapter != null) {
                                        mAdapter!!.notifyDataSetChanged()
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
