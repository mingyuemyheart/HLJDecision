package com.hlj.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View
import com.hlj.view.MyDialog

open class BaseFragment: Fragment() {

    private var mDialog : MyDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

}