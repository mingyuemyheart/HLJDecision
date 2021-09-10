package com.hlj.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Toast
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.hlj.common.CONST
import com.hlj.common.ColumnData
import com.hlj.common.MyApplication
import com.hlj.utils.CommonUtil
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import shawn.cxwl.com.hlj.R
import java.io.IOException
import java.util.*

class LoginActivity : BaseActivity(), OnClickListener, AMapLocationListener {

	private var lat = 0.0
	private var lng = 0.0
	private var areaId = ""
	private var seconds:Int = 60
	private var timer: Timer? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_login)
		initWidget()
	}

	/**
	 * 开始定位
	 */
	private fun startLocation() {
		val mLocationOption = AMapLocationClientOption() //初始化定位参数
		val mLocationClient = AMapLocationClient(this) //初始化定位
		mLocationOption.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
		mLocationOption.isNeedAddress = true //设置是否返回地址信息（默认返回地址信息）
		mLocationOption.isOnceLocation = true //设置是否只定位一次,默认为false
		mLocationOption.isMockEnable = false //设置是否允许模拟位置,默认为false，不允许模拟位置
		mLocationOption.interval = 2000 //设置定位间隔,单位毫秒,默认为2000ms
		mLocationClient.setLocationOption(mLocationOption) //给定位客户端对象设置定位参数
		mLocationClient.setLocationListener(this)
		mLocationClient.startLocation() //启动定位
	}

	override fun onLocationChanged(amapLocation: AMapLocation?) {
		if (amapLocation != null && amapLocation.errorCode == AMapLocation.LOCATION_SUCCESS) {
			if (amapLocation.longitude != 0.0 && amapLocation.latitude != AMapLocation.LOCATION_SUCCESS.toDouble()) {
				lat = amapLocation.latitude
				lng = amapLocation.longitude
				areaId = amapLocation.adCode
			}
		}
	}

	/**
	 * 初始化控件
	 */
	private fun initWidget() {
		llBack.setOnClickListener(this)
		tvTitle.text = "用户登录"
		tvSend.setOnClickListener(this)
		tvLogin.setOnClickListener(this)

		startLocation()
	}

	/**
	 * 验证手机号权限
	 * 有权限直接登录，无需验证码
	 * 无权限需要获取验证码登录
	 */
	private fun okHttpNext() {
		if (TextUtils.isEmpty(etPhone!!.text.toString())) {
			Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show()
			return
		}
		showDialog()
		val url = "http://decision-admin.tianqi.cn/Home/work2019/hlgNewLogin"
		val builder = FormBody.Builder()
		builder.add("mobile", etPhone!!.text.toString())
		val body: RequestBody = builder.build()
		Thread {
			OkHttpUtil.enqueue(Request.Builder().post(body).url(url).build(), object : Callback {
				override fun onFailure(call: Call, e: IOException) {
					runOnUiThread {
						resetTimer()
					}
				}

				override fun onResponse(call: Call, response: Response) {
					if (!response.isSuccessful) {
						return
					}
					val result = response.body!!.string()
					runOnUiThread {
						cancelDialog()
						if (!TextUtils.isEmpty(result)) {
							val obj = JSONObject(result)
							if (!obj.isNull("column")) {//有权限
								parseData(result)
							} else {//无权限
								etCode.visibility = View.VISIBLE
								dividerCode.visibility = View.VISIBLE
								tvSend.visibility = View.VISIBLE
								divider4.visibility = View.VISIBLE
								tvLogin.text = "登录"

								if (timer == null) {
									timer = Timer()
									timer!!.schedule(object : TimerTask() {
										override fun run() {
											handler.sendEmptyMessage(101)
										}
									}, 0, 1000)
								}
							}
						}
					}
				}
			})
		}.start()
	}

	@SuppressLint("HandlerLeak")
	private val handler: Handler = object : Handler() {
		override fun handleMessage(msg: Message) {
			when (msg.what) {
				101 -> if (seconds <= 0) {
					resetTimer()
				} else {
					tvSend.text = seconds--.toString() + "s"
				}
			}
		}
	}

	/**
	 * 重置计时器
	 */
	private fun resetTimer() {
		if (timer != null) {
			timer!!.cancel()
			timer = null
		}
		seconds = 60
		tvSend.text = "获取验证码"
	}

	override fun onDestroy() {
		super.onDestroy()
		resetTimer()
	}

	private fun okHttpLogin() {
		if (TextUtils.isEmpty(etPhone!!.text.toString())) {
			Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show()
			return
		}
		if (TextUtils.isEmpty(etCode!!.text.toString())) {
			Toast.makeText(this, "请输入验证码", Toast.LENGTH_SHORT).show()
			return
		}
		showDialog()
		val url = "http://decision-admin.tianqi.cn/Home/work2019/hlgVcodeLogin"
		val builder = FormBody.Builder()
		builder.add("mobile", etPhone!!.text.toString())
		builder.add("vcode", etCode!!.text.toString())
		builder.add("appid", CONST.APPID)
		builder.add("device_id", "")
		builder.add("platform", "android")
		builder.add("os_version", Build.VERSION.RELEASE)
		builder.add("software_version", CommonUtil.getVersion(this))
		builder.add("mobile_type", Build.MODEL)
		builder.add("address", "")
		builder.add("lat", lat.toString() + "")
		builder.add("lon", lng.toString() + "")
		val body: RequestBody = builder.build()
		Thread {
			OkHttpUtil.enqueue(Request.Builder().post(body).url(url).build(), object : Callback {
				override fun onFailure(call: Call, e: IOException) {
					runOnUiThread { resetTimer() }
				}

				@Throws(IOException::class)
				override fun onResponse(call: Call, response: Response) {
					if (!response.isSuccessful) {
						return
					}
					val result = response.body!!.string()
					parseData(result)
				}
			})
		}.start()
	}

	private fun parseData(result: String) {
		runOnUiThread {
			cancelDialog()
			if (!TextUtils.isEmpty(result)) {
				try {
					val obje = JSONObject(result)
					if (!obje.isNull("status")) {
						val status = obje.getInt("status")
						if (status == 1) { //成功
							val array = JSONArray(obje.getString("column"))
							MyApplication.columnDataList.clear()
							for (i in 0 until array.length()) {
								val obj = array.getJSONObject(i)
								val data = ColumnData()
								if (!obj.isNull("id")) {
									data.columnId = obj.getString("id")
								}
								if (!obj.isNull("localviewid")) {
									data.id = obj.getString("localviewid")
								}
								if (!obj.isNull("name")) {
									data.name = obj.getString("name")
								}
								if (!obj.isNull("default")) {
									data.level = obj.getString("default")
								}
								if (!obj.isNull("icon")) {
									data.icon = obj.getString("icon")
								}
								if (!obj.isNull("icon2")) {
									data.icon2 = obj.getString("icon2")
								}
								if (!obj.isNull("desc")) {
									data.desc = obj.getString("desc")
								}
								if (!obj.isNull("dataurl")) {
									data.dataUrl = obj.getString("dataurl")
								}
								if (!obj.isNull("showtype")) {
									data.showType = obj.getString("showtype")
								}
								if (!obj.isNull("child")) {
									val childArray = JSONArray(obj.getString("child"))
									for (j in 0 until childArray.length()) {
										val childObj = childArray.getJSONObject(j)
										val dto = ColumnData()
										if (!childObj.isNull("id")) {
											dto.columnId = childObj.getString("id")
										}
										if (!childObj.isNull("localviewid")) {
											dto.id = childObj.getString("localviewid")
										}
										if (!childObj.isNull("name")) {
											dto.name = childObj.getString("name")
										}
										if (!childObj.isNull("desc")) {
											dto.desc = childObj.getString("desc")
										}
										if (!childObj.isNull("icon")) {
											dto.icon = childObj.getString("icon")
										}
										if (!childObj.isNull("icon2")) {
											dto.icon2 = childObj.getString("icon2")
										}
										if (!childObj.isNull("showtype")) {
											dto.showType = childObj.getString("showtype")
										}
										if (!childObj.isNull("dataurl")) {
											dto.dataUrl = childObj.getString("dataurl")
										}
										if (!childObj.isNull("child")) {
											val child2Array = JSONArray(childObj.getString("child"))
											for (k in 0 until child2Array.length()) {
												val child2Obj = child2Array.getJSONObject(k)
												val child2 = ColumnData()
												if (!child2Obj.isNull("id")) {
													child2.columnId = child2Obj.getString("id")
												}
												if (!child2Obj.isNull("localviewid")) {
													child2.id = child2Obj.getString("localviewid")
												}
												if (!child2Obj.isNull("name")) {
													child2.name = child2Obj.getString("name")
												}
												if (!child2Obj.isNull("desc")) {
													child2.desc = child2Obj.getString("desc")
												}
												if (!child2Obj.isNull("icon")) {
													child2.icon = child2Obj.getString("icon")
												}
												if (!child2Obj.isNull("icon2")) {
													child2.icon2 = child2Obj.getString("icon2")
												}
												if (!child2Obj.isNull("dataurl")) {
													child2.dataUrl = child2Obj.getString("dataurl")
												}
												if (!child2Obj.isNull("showtype")) {
													child2.showType = child2Obj.getString("showtype")
												}
												if (!child2Obj.isNull("child")) {
													val child3Array = JSONArray(child2Obj.getString("child"))
													for (m in 0 until child3Array.length()) {
														val child3Obj = child3Array.getJSONObject(m)
														val child3 = ColumnData()
														if (!child3Obj.isNull("id")) {
															child3.columnId = child3Obj.getString("id")
														}
														if (!child3Obj.isNull("localviewid")) {
															child3.id = child3Obj.getString("localviewid")
														}
														if (!child3Obj.isNull("name")) {
															child3.name = child3Obj.getString("name")
														}
														if (!child3Obj.isNull("desc")) {
															child3.desc = child3Obj.getString("desc")
														}
														if (!child3Obj.isNull("icon")) {
															child3.icon = child3Obj.getString("icon")
														}
														if (!child3Obj.isNull("icon2")) {
															child3.icon2 = child3Obj.getString("icon2")
														}
														if (!child3Obj.isNull("dataurl")) {
															child3.dataUrl = child3Obj.getString("dataurl")
														}
														if (!child3Obj.isNull("showtype")) {
															child3.showType = child3Obj.getString("showtype")
														}
														child2.child.add(child3)
													}
												}
												dto.child.add(child2)
											}
										}
										data.child.add(dto)
									}
								}
								MyApplication.columnDataList.add(data)
							}
							if (!obje.isNull("info")) {
								val obj = JSONObject(obje.getString("info"))
								if (!obj.isNull("id")) {
									val uid = obj.getString("id")
									if (uid != null) {
										//把用户信息保存在sharedPreferance里
										MyApplication.UID = uid
										MyApplication.USERNAME = obj.getString("username")
										MyApplication.TOKEN = obj.getString("token")
										MyApplication.GROUPID = obj.getString("usergroup")
										MyApplication.UGROUPNAME = obj.getString("usergroup_name")
										MyApplication.saveUserInfo(this)

										resetTimer()
										okHttpPushToken()

										MyApplication.destoryActivity()
										startActivity(Intent(this@LoginActivity, MainActivity::class.java))
										finish()
									}
								}
							}
						} else {
							//失败
							if (!obje.isNull("msg")) {
								val msg = obje.getString("msg")
								if (msg != null) {
									resetTimer()
									Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
								}
							}
						}
					}
				} catch (e: JSONException) {
					e.printStackTrace()
				}
			}
		}
	}

	private fun okHttpPushToken() {
		val url = "https://decision-admin.tianqi.cn/Home/extra/savePushToken"
		val builder = FormBody.Builder()
		val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
		val serial = Build.SERIAL
		builder.add("uuid", androidId+serial)
		builder.add("uid", MyApplication.UID)
		builder.add("groupid", MyApplication.GROUPID)
		builder.add("pushtoken", MyApplication.DEVICETOKEN)
		builder.add("platform", "android")
		builder.add("um_key", MyApplication.appKey)
		builder.add("areaid", areaId)
		val body = builder.build()
		Thread {
			OkHttpUtil.enqueue(Request.Builder().url(url).post(body).build(), object : Callback {
				override fun onFailure(call: Call, e: IOException) {
				}

				override fun onResponse(call: Call, response: Response) {
					if (!response.isSuccessful) {
						return
					}
					val result = response.body!!.string()
					Log.e("result", result)
				}
			})
		}.start()
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.llBack -> finish()
			R.id.tvSend -> {
				if (TextUtils.equals(tvSend.text.toString(), "获取验证码")) {
					okHttpNext()
				}
			}
			R.id.tvLogin -> {
				if (TextUtils.equals(tvLogin.text.toString(), "获取验证码登录")) {
					okHttpNext()
				} else {
					okHttpLogin()
				}
			}
		}
	}

}
