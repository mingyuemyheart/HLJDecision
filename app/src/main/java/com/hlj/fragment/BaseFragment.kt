package com.hlj.fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.View
import com.hlj.utils.AuthorityUtil
import com.hlj.view.MyDialog

open class BaseFragment: Fragment() {

    private var mDialog : MyDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getCheckLocationAuthority()
        getCheckStorageAuthority()
        getCheckPhoneAuthority()
        getCheckCameraAuthority()
        getCheckCallAuthority()
    }

    /**
     * 初始化dialog
     */
    protected fun showDialog() {
        if (mDialog == null) {
            mDialog = MyDialog(activity)
        }
        if (!mDialog!!.isShowing) {
            mDialog!!.show()
        }
    }

    protected fun cancelDialog() {
        if (mDialog != null) {
            mDialog!!.dismiss()
        }
    }

//	--------------------------------------------------------------------
    private var isCheckLocationAuthority = false

    private fun getCheckLocationAuthority() {
        val sp = activity!!.getSharedPreferences(Manifest.permission.ACCESS_FINE_LOCATION, Context.MODE_PRIVATE)
        isCheckLocationAuthority = sp.getBoolean("granted", isCheckLocationAuthority)
    }

    private fun setCheckLocationAuthority() {
        val sp = activity!!.getSharedPreferences(Manifest.permission.ACCESS_FINE_LOCATION, Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putBoolean("granted", isCheckLocationAuthority)
        editor.apply()
    }

    /**
     * 申请定位权限
     */
    protected fun checkLocationAuthority(locationCallback: LocationCallback?) {
        this.locationCallback = locationCallback
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (isCheckLocationAuthority) {
                if (locationCallback != null) {
                    locationCallback!!.grantedLocation(false)
                }
            } else {
                ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), AuthorityUtil.AUTHOR_LOCATION)
            }
        } else {
            if (locationCallback != null) {
                locationCallback!!.grantedLocation(true)
            }
        }
    }
//	--------------------------------------------------------------------

//	--------------------------------------------------------------------
    private var isCheckStorageAuthority = false

    private fun getCheckStorageAuthority() {
        val sp = activity!!.getSharedPreferences(Manifest.permission.READ_EXTERNAL_STORAGE, Context.MODE_PRIVATE)
        isCheckStorageAuthority = sp.getBoolean("granted", isCheckStorageAuthority)
    }

    private fun setCheckStorageAuthority() {
        val sp = activity!!.getSharedPreferences(Manifest.permission.READ_EXTERNAL_STORAGE, Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putBoolean("granted", isCheckStorageAuthority)
        editor.apply()
    }

    /**
     * 申请存储权限
     */
    protected fun checkStorageAuthority(storageCallback: StorageCallback?) {
        this.storageCallback = storageCallback
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (isCheckStorageAuthority) {
                if (storageCallback != null) {
                    storageCallback!!.grantedStorage(false)
                }
            } else {
                ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), AuthorityUtil.AUTHOR_STORAGE)
            }
        } else {
            if (storageCallback != null) {
                storageCallback!!.grantedStorage(true)
            }
        }
    }
//	--------------------------------------------------------------------

//	--------------------------------------------------------------------
    private var isCheckCameraAuthority = false

    private fun getCheckCameraAuthority() {
        val sp = activity!!.getSharedPreferences(Manifest.permission.CAMERA, Context.MODE_PRIVATE)
        isCheckCameraAuthority = sp.getBoolean("granted", isCheckCameraAuthority)
    }

    private fun setCheckCameraAuthority() {
        val sp = activity!!.getSharedPreferences(Manifest.permission.CAMERA, Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putBoolean("granted", isCheckCameraAuthority)
        editor.apply()
    }

    /**
     * 申请相机权限
     */
    protected fun checkCameraAuthority(cameraCallback: CameraCallback?) {
        this.cameraCallback = cameraCallback
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (isCheckCameraAuthority) {
                if (cameraCallback != null) {
                    cameraCallback!!.grantedCamera(false)
                }
            } else {
                ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.CAMERA), AuthorityUtil.AUTHOR_CAMERA)
            }
        } else {
            if (cameraCallback != null) {
                cameraCallback!!.grantedCamera(true)
            }
        }
    }
//	--------------------------------------------------------------------

//	--------------------------------------------------------------------
    private var isCheckPhoneAuthority = false

    private fun getCheckPhoneAuthority() {
        val sp = activity!!.getSharedPreferences(Manifest.permission.READ_PHONE_STATE, Context.MODE_PRIVATE)
        isCheckPhoneAuthority = sp.getBoolean("granted", isCheckPhoneAuthority)
    }

    private fun setCheckPhoneAuthority() {
        val sp = activity!!.getSharedPreferences(Manifest.permission.READ_PHONE_STATE, Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putBoolean("granted", isCheckPhoneAuthority)
        editor.apply()
    }

    /**
     * 申请设备权限
     */
    protected fun checkPhoneAuthority(phoneCallback: PhoneCallback?) {
        this.phoneCallback = phoneCallback
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (isCheckPhoneAuthority) {
                if (phoneCallback != null) {
                    phoneCallback!!.grantedPhone(false)
                }
            } else {
                ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.READ_PHONE_STATE), AuthorityUtil.AUTHOR_PHONE)
            }
        } else {
            if (phoneCallback != null) {
                phoneCallback!!.grantedPhone(true)
            }
        }
    }
//	--------------------------------------------------------------------

//	--------------------------------------------------------------------
    private var isCheckCallAuthority = false

    private fun getCheckCallAuthority() {
        val sp = activity!!.getSharedPreferences(Manifest.permission.CALL_PHONE, Context.MODE_PRIVATE)
        isCheckCallAuthority = sp.getBoolean("granted", isCheckCallAuthority)
    }

    private fun setCheckCallAuthority() {
        val sp = activity!!.getSharedPreferences(Manifest.permission.CALL_PHONE, Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putBoolean("granted", isCheckCallAuthority)
        editor.apply()
    }

    /**
     * 申请电话权限
     */
    protected fun checkCallAuthority(callCallback: CallCallback?) {
        this.callCallback = callCallback
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            if (isCheckCallAuthority) {
                if (callCallback != null) {
                    callCallback!!.grantedCall(false)
                }
            } else {
                ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.CALL_PHONE), AuthorityUtil.AUTHOR_CALL)
            }
        } else {
            if (callCallback != null) {
                callCallback!!.grantedCall(true)
            }
        }
    }
//	--------------------------------------------------------------------

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {//申请过权限，无论授予、拒绝都不再提醒权限授予
            AuthorityUtil.AUTHOR_LOCATION -> {
                isCheckLocationAuthority = true
                setCheckLocationAuthority()
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (locationCallback != null) {
                        locationCallback!!.grantedLocation(true)
                    }
                } else {
                    if (locationCallback != null) {
                        locationCallback!!.grantedLocation(false)
                    }
                }
            }
            AuthorityUtil.AUTHOR_STORAGE -> {
                isCheckStorageAuthority = true
                setCheckStorageAuthority()
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (storageCallback != null) {
                        storageCallback!!.grantedStorage(true)
                    }
                } else {
                    if (storageCallback != null) {
                        storageCallback!!.grantedStorage(false)
                    }
                }
            }
            AuthorityUtil.AUTHOR_CAMERA -> {
                isCheckCameraAuthority = true
                setCheckCameraAuthority()
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (cameraCallback != null) {
                        cameraCallback!!.grantedCamera(true)
                    }
                } else {
                    if (cameraCallback != null) {
                        cameraCallback!!.grantedCamera(false)
                    }
                }
            }
            AuthorityUtil.AUTHOR_PHONE -> {
                isCheckPhoneAuthority = true
                setCheckPhoneAuthority()
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (phoneCallback != null) {
                        phoneCallback!!.grantedPhone(true)
                    }
                } else {
                    if (phoneCallback != null) {
                        phoneCallback!!.grantedPhone(false)
                    }
                }
            }
            AuthorityUtil.AUTHOR_CALL -> {
                isCheckCallAuthority = true
                setCheckCallAuthority()
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (callCallback != null) {
                        callCallback!!.grantedCall(true)
                    }
                } else {
                    if (callCallback != null) {
                        callCallback!!.grantedCall(false)
                    }
                }
            }
        }
    }

    private var locationCallback: LocationCallback? = null
    interface LocationCallback {
        fun grantedLocation(isGranted: Boolean)
    }

    private var storageCallback: StorageCallback? = null
    interface StorageCallback {
        fun grantedStorage(isGranted: Boolean)
    }

    private var phoneCallback: PhoneCallback? = null
    interface PhoneCallback {
        fun grantedPhone(isGranted: Boolean)
    }

    private var cameraCallback: CameraCallback? = null
    interface CameraCallback {
        fun grantedCamera(isGranted: Boolean)
    }

    private var callCallback: CallCallback? = null
    interface CallCallback {
        fun grantedCall(isGranted: Boolean)
    }

}