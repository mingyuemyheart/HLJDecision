package com.hlj.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import com.hlj.adapter.AddrBookAdapter
import com.hlj.common.CONST
import com.hlj.common.MyApplication
import com.hlj.dto.ContactDto
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

    private var key = ""
    private var mAdapter: AddrBookAdapter? = null
    private val dataList: ArrayList<ContactDto> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addrbook)
        initWidget()
        initListView()
    }

    private fun initWidget() {
        llBack.setOnClickListener(this)
        tvSearch.setOnClickListener(this)

        val title = intent.getStringExtra(CONST.ACTIVITY_NAME)
        if (!TextUtils.isEmpty(title)) {
            tvTitle.text = title
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                key = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        okHttpList(false)
    }

    private fun initListView() {
        mAdapter = AddrBookAdapter(this, dataList)
        listView!!.adapter = mAdapter
        listView!!.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 ->
            val data = dataList[arg2]
            if (TextUtils.equals(data.type, "0")) {
                val intent = Intent(this, AddrBookDetailActivity::class.java)
                val bundle = Bundle()
                intent.putExtra(CONST.ACTIVITY_NAME, data.name)
                bundle.putParcelable("data", data)
                intent.putExtras(bundle)
                startActivity(intent)
            } else {
                val intent = Intent(this, AddrBookActivity::class.java)
                intent.putExtra(CONST.ACTIVITY_NAME, data.company)
                intent.putExtra("id", data.id)
                startActivity(intent)
            }
        }

        slideBar.setOnTouchLetterChangeListenner { isTouch, letter ->
            for (i in 0 until dataList.size) {
                val dto = dataList[i]
                if (TextUtils.equals(dto.letter, letter)) {
                    listView.setSelection(i)
                    break
                }
            }
        }
    }

    /**
     * 获取资讯信息
     */
    private fun okHttpList(isSearch: Boolean) {
        val id = if (!intent.hasExtra("id")) {
            ""
        } else {
            intent.getStringExtra("id")
        }
        Thread {
            var url = "https://decision-admin.tianqi.cn/Home/workwsj/getPhoneBook?pid=$id&key=$key&uid=${MyApplication.UID}"
            if (isSearch) {
                url = "https://decision-admin.tianqi.cn/Home/workwsj/getPhoneBook?all=1&key=$key&uid=${MyApplication.UID}"
            }
            Log.e("addrBook", url)
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

                                var level = ""
                                if (!obj.isNull("level")) {
                                    val array = obj.getJSONArray("level")
                                    for (i in 0 until array.length()) {
                                        val itemObj = array.getJSONObject(i)
                                        if (!itemObj.isNull("company")) {
                                            val company = itemObj.getString("company")
                                            if (!TextUtils.isEmpty(company)) {
                                                level += if (i == array.length()-1) {
                                                    company
                                                } else {
                                                    "$company > "
                                                }
                                            }
                                        }
                                    }
                                }

                                if (!obj.isNull("data")) {
                                    dataList.clear()
                                    val array = obj.getJSONArray("data")
                                    for (i in 0 until array.length()) {
                                        val itemObj = array.getJSONObject(i)
                                        val dto = ContactDto()
                                        if (!itemObj.isNull("id")) {
                                            dto.id = itemObj.getString("id")
                                        }
                                        if (!itemObj.isNull("name")) {
                                            dto.name = itemObj.getString("name")
                                        }
                                        if (!itemObj.isNull("company")) {
                                            dto.company = itemObj.getString("company")
                                        }
                                        if (!itemObj.isNull("worktelephone")) {
                                            dto.worktelephone = itemObj.getString("worktelephone")
                                        }
                                        if (!itemObj.isNull("mobile")) {
                                            dto.mobile = itemObj.getString("mobile")
                                        }
                                        if (!itemObj.isNull("updatetime")) {
                                            dto.updatetime = itemObj.getString("updatetime")
                                        }
                                        if (!itemObj.isNull("letter")) {
                                            dto.letter = itemObj.getString("letter")
                                        }
                                        if (!itemObj.isNull("type")) {
                                            dto.type = itemObj.getString("type")
                                        }
                                        if (!itemObj.isNull("duties")) {
                                            dto.duties = itemObj.getString("duties")
                                        }
                                        if (!itemObj.isNull("level")) {
                                            dto.level = itemObj.getString("level")
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
            R.id.tvSearch -> {
                showDialog()
                okHttpList(true)
            }
        }
    }
	
}
