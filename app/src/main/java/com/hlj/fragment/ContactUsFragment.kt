package com.hlj.fragment

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.hlj.activity.DataIntroActivity
import com.hlj.activity.LoginActivity
import com.hlj.adapter.ContactAdapter
import com.hlj.common.CONST
import com.hlj.common.ColumnData
import com.hlj.common.MyApplication
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.dialog_delete.view.*
import kotlinx.android.synthetic.main.fragment_contact_us.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import shawn.cxwl.com.hlj.R
import java.io.IOException

/**
 * 联系我们
 * @author shawn_sun
 */
class ContactUsFragment : BaseFragment(), View.OnClickListener {

    private var mReceiver: MyBroadCastReceiver? = null
    private var mAdapter1: ContactAdapter? = null
    private var mAdapter2: ContactAdapter? = null
    private var mAdapter3: ContactAdapter? = null
    private val dataList1: ArrayList<ColumnData> = ArrayList()
    private val dataList2: ArrayList<ColumnData> = ArrayList()
    private val dataList3: ArrayList<ColumnData> = ArrayList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_contact_us, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBroadCast()
    }

    private fun refresh() {
        tvLogin.setOnClickListener(this)
        tvMsg.setOnClickListener(this)
        clData.setOnClickListener(this)

        if (TextUtils.isEmpty(MyApplication.USERNAME) || TextUtils.equals(MyApplication.USERNAME, CONST.publicUser)) {
            clLogin.visibility = View.VISIBLE
        } else {
            clLogin.visibility = View.GONE
        }

        initGridView1()
        initGridView2()
        initGridView3()
        okHttpList()
    }

    private fun initBroadCast() {
        mReceiver = MyBroadCastReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(arguments!!.getString(CONST.BROADCAST_ACTION))
        activity!!.registerReceiver(mReceiver, intentFilter)
    }

    private inner class MyBroadCastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refresh()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mReceiver != null) {
            activity!!.unregisterReceiver(mReceiver)
        }
    }

    private fun dialPhone(message: String, content: String, positive: String) {
        val inflater = activity!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_delete, null)
        val dialog = Dialog(activity, R.style.CustomProgressDialog)
        dialog.setContentView(view)
        dialog.show()
        view.tvPositive.text = positive
        view.tvMessage.text = message
        view.tvContent.text = content
        view.tvContent.visibility = View.VISIBLE
        view.llNegative.setOnClickListener { dialog.dismiss() }
        view.llPositive.setOnClickListener {
            dialog.dismiss()
            checkCallAuthority(object : CallCallback {
                override fun grantedCall(isGranted: Boolean) {
                    if (isGranted) {
                        startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$content")))
                    } else {
                        Toast.makeText(activity, "需要开启拨打电话权限", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    private fun initGridView1() {
        mAdapter1 = ContactAdapter(activity, dataList1)
        mAdapter1!!.flag = "1"
        gridView1.adapter = mAdapter1
    }

    private fun initGridView2() {
        mAdapter2 = ContactAdapter(activity, dataList2)
        mAdapter2!!.flag = "2"
        gridView2.adapter = mAdapter2
    }

    private fun initGridView3() {
        mAdapter3 = ContactAdapter(activity, dataList3)
        mAdapter3!!.flag = "3"
        gridView3.adapter = mAdapter3
    }

    private fun okHttpList() {
        Thread(Runnable {
            val url = "https://decision-admin.tianqi.cn/Home/work2019/hlg_get_lxwm"
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback{
                override fun onFailure(call: Call, e: IOException) {
                }
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    activity!!.runOnUiThread {
                        dataList1.clear()
                        dataList2.clear()
                        dataList3.clear()
                        if (!TextUtils.isEmpty(result)) {
                            val array = JSONArray(result)
                            for (i in 0 until array.length()) {
                                val obj = array.getJSONObject(i)
                                if (!obj.isNull("list")) {
                                    val itemArray = obj.getJSONArray("list")
                                    for (j in 0 until itemArray.length()) {
                                        val columnData = ColumnData()
                                        columnData.name = itemArray.getString(j)
                                        when (i) {
                                            0 -> {
                                                dataList1.add(columnData)
                                            }
                                            1 -> {
                                                dataList2.add(columnData)
                                            }
                                            2 -> {
                                                dataList3.add(columnData)
                                            }
                                        }
                                    }
                                    if (mAdapter1 != null) {
                                        mAdapter1!!.notifyDataSetChanged()
                                    }
                                    if (mAdapter2 != null) {
                                        mAdapter2!!.notifyDataSetChanged()
                                    }
                                    if (mAdapter3 != null) {
                                        mAdapter3!!.notifyDataSetChanged()
                                    }
                                }
                            }
                        }
                    }
                }
            })
        }).start()
    }

    override fun onClick(v: View?) {
        when(v!!.id) {
            R.id.tvLogin -> startActivity(Intent(activity, LoginActivity::class.java))
            R.id.tvPhone, R.id.tvMsg -> {
                dialPhone("联系电话", "4006000121", "拨打")
            }
            R.id.clData -> {
                val intent = Intent(activity, DataIntroActivity::class.java)
                intent.putExtra(CONST.ACTIVITY_NAME, "客户端产品数据说明")
                startActivity(intent)
            }
        }
    }

}
