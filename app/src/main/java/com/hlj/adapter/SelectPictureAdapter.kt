package com.hlj.adapter

import android.content.Context
import android.graphics.Color
import android.support.constraint.ConstraintLayout
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.hlj.interfaces.SelectListener
import com.hlj.dto.AgriDto
import com.hlj.utils.CommonUtil
import com.squareup.picasso.Picasso
import shawn.cxwl.com.hlj.R
import java.io.File

/**
 * 选择图片
 */
class SelectPictureAdapter(private val context: Context?, private val mArrayList: ArrayList<AgriDto>?, private val maxCount: Int) : BaseAdapter() {

    private var mInflater = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var lastCount = 0 //上一次已经选了几张

    private var selectListener: SelectListener? = null

    fun setSelectListener(selectListener: SelectListener?) {
        this.selectListener = selectListener
    }

    fun setLastCount(lastCount: Int) {
        this.lastCount = lastCount
    }

    private inner class ViewHolder {
        var imageView: ImageView? = null
        var imageView1: ImageView? = null
        var clBg: ConstraintLayout? = null
        var tvSize: TextView? = null
    }

    override fun getCount(): Int {
        return mArrayList!!.size
    }

    override fun getItem(position: Int): Any? {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup?): View? {
        var convertView = view
        val mHolder: ViewHolder
        if (convertView == null) {
            convertView = mInflater!!.inflate(R.layout.adapter_select_picture, null)
            mHolder = ViewHolder()
            mHolder.imageView = convertView.findViewById(R.id.imageView)
            mHolder.imageView1 = convertView.findViewById(R.id.imageView1)
            mHolder.clBg = convertView.findViewById(R.id.clBg)
            mHolder.tvSize = convertView.findViewById(R.id.tvSize)
            convertView.tag = mHolder
        } else {
            mHolder = convertView.tag as ViewHolder
        }

        val dto = mArrayList!![position]
        if (TextUtils.isEmpty(dto.imgUrl)) {
            mHolder.imageView1!!.visibility = View.INVISIBLE
            mHolder.imageView!!.setImageResource(R.drawable.icon_camera)
            mHolder.imageView!!.setPadding(CommonUtil.dip2px(context, 30f).toInt(), CommonUtil.dip2px(context, 30f).toInt(), CommonUtil.dip2px(context, 30f).toInt(), CommonUtil.dip2px(context, 30f).toInt())
            mHolder.tvSize!!.text = ""
            mHolder.tvSize!!.setBackgroundColor(Color.TRANSPARENT)
        } else {
            mHolder.imageView1!!.visibility = View.VISIBLE
            mHolder.imageView!!.setPadding(0, 0, 0, 0)
            val params = ConstraintLayout.LayoutParams(CommonUtil.widthPixels(context) / 4, CommonUtil.widthPixels(context) / 4)
            if (!TextUtils.isEmpty(dto.imgUrl)) {
                val file = File(dto.imgUrl)
                if (file.exists()) {
                    Picasso.get().load(file).centerCrop().resize(200, 200).into(mHolder.imageView)
                    mHolder.imageView!!.layoutParams = params
                }
            }
            if (dto.isSelected) {
                mHolder.imageView1!!.setImageResource(R.drawable.bg_checkbox_selected)
                mHolder.clBg!!.setBackgroundColor(0x60000000)
            } else {
                mHolder.imageView1!!.setImageResource(R.drawable.bg_checkbox)
                mHolder.clBg!!.setBackgroundColor(Color.TRANSPARENT)
            }
            mHolder.clBg!!.layoutParams = params

            //选择图片
            mHolder.imageView1!!.setOnClickListener(View.OnClickListener {
                val fiveMB = 1024*1024*5
                if (dto.fileSize > fiveMB) {
                    Toast.makeText(context, "文件超过5MB，不能上传！！！", Toast.LENGTH_SHORT).show()
                    return@OnClickListener
                }
                dto.isSelected = !dto.isSelected
                var selectCount = 0
                for (i in mArrayList!!.indices) {
                    val data = mArrayList!![i]
                    if (data.isSelected) {
                        selectCount++
                    }
                }
                if (selectCount + lastCount > maxCount) {
                    dto.isSelected = false
                    Toast.makeText(context, "最多只能选择${maxCount-lastCount}张图片", Toast.LENGTH_SHORT).show()
                    return@OnClickListener
                }
                if (selectListener != null) {
                    selectListener!!.setCount(selectCount)
                }
                notifyDataSetChanged()
            })

            mHolder.tvSize!!.text = CommonUtil.getFormatSize(dto.fileSize)+""
            mHolder.tvSize!!.setBackgroundColor(0x40000000)
        }
        return convertView
    }

}
