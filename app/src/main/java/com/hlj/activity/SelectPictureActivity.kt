package com.hlj.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.Toast
import com.hlj.adapter.SelectPictureAdapter
import com.hlj.common.CONST
import com.hlj.dto.AgriDto
import com.hlj.interfaces.SelectListener
import com.hlj.utils.AuthorityUtil
import com.hlj.utils.CommonUtil
import kotlinx.android.synthetic.main.activity_select_picture.*
import kotlinx.android.synthetic.main.layout_title.*
import shawn.cxwl.com.hlj.R
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * 获取本地相册图片
 */
class SelectPictureActivity : BaseActivity(), View.OnClickListener, SelectListener {

    private var mAdapter: SelectPictureAdapter? = null
    private val dataList: ArrayList<AgriDto> = ArrayList()
    private var maxCount = 0 // 允许上传最大值
    private var lastCount = 0 //上一次已经选了几张
    private var selectCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_picture)
        checkStorageAuthority()
    }

    private fun init() {
        initWidget()
        initGridView()
    }

    private fun initWidget() {
        llBack.setOnClickListener(this)
        tvTitle.text = "选择图片"
        tvControl.setOnClickListener(this)
        tvControl.visibility = View.VISIBLE

        lastCount = intent.getIntExtra("count", 0)
        maxCount = intent.getIntExtra("maxCount", 0)
        tvControl!!.text = "完成($selectCount/${maxCount-lastCount})"
        loadImages()
    }

    /**
     * 获取相册信息
     */
    private fun loadImages() {
        dataList.clear()
        dataList.addAll(CommonUtil.getAllLocalImages(this))
        val dto = AgriDto()
        dto.imageName = "拍照"
        dto.imgUrl = ""
        dataList.add(0, dto)
        if (mAdapter != null) {
            mAdapter!!.notifyDataSetChanged()
        }
    }

    private fun initGridView() {
        mAdapter = SelectPictureAdapter(this, dataList, maxCount)
        gridView.adapter = mAdapter
        mAdapter!!.setLastCount(lastCount)
        mAdapter!!.setSelectListener(this)
        gridView.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            val dto = dataList[position]
            if (TextUtils.isEmpty(dto.imgUrl)) {//拍照
                checkCameraAuthority()
            } else {
                val intent = Intent(this, DisplayPictureActivity::class.java)
                val bundle = Bundle()
                bundle.putParcelable("data", dto)
                intent.putExtras(bundle)
                startActivity(intent)
            }
        }
    }

    override fun setCount(count: Int) {
        selectCount = count
        tvControl!!.text = "完成($selectCount/${maxCount-lastCount})"
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.tvControl -> {
                if (selectCount <= 0) {
                    Toast.makeText(this, "请选择需要上传的图片！", Toast.LENGTH_SHORT).show()
                } else {
                    val list: ArrayList<AgriDto?> = ArrayList()
                    var i = 0
                    while (i < dataList.size) {
                        val dto = dataList[i]
                        if (dto.isSelected) {
                            dto.isSelected = false//这块是为了不影响传过去界面的属性值
                            list.add(dto)
                        }
                        i++
                    }
                    val intent = Intent()
                    val bundle = Bundle()
                    bundle.putParcelableArrayList("dataList", list)
                    intent.putExtras(bundle)
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                1001 -> {
                    var fis: FileInputStream? = null
                    try {
                        fis = FileInputStream(cameraFile)
                        val bitmap = BitmapFactory.decodeStream(fis)

                        val dto = AgriDto()
                        dto.imageName = cameraFile!!.name
                        dto.imgUrl = cameraFile!!.absolutePath
                        dataList.add(1, dto)
                        if (mAdapter != null) {
                            mAdapter!!.notifyDataSetChanged()
                        }

                        CommonUtil.notifyAlbum(this, cameraFile)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            fis?.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private var cameraFile: File? = null
    private fun intentCamera() {
        val files = File("${getExternalFilesDir(null)}/image") //获取sdcard根目录
        if (!files.exists()) {
            files.mkdirs()
        }
        cameraFile = File("${files.absolutePath}/${System.currentTimeMillis()}${CONST.JPG}")
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val uri: Uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(this, "$packageName.FileProvider", cameraFile!!)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            uri = Uri.fromFile(cameraFile)
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        startActivityForResult(intent, 1001)
    }

    /**
     * 申请相机权限
     */
    private fun checkCameraAuthority() {
        if (Build.VERSION.SDK_INT < 23) {
            intentCamera()
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !== PackageManager.PERMISSION_GRANTED) {
                val permissions = arrayOf(Manifest.permission.CAMERA)
                ActivityCompat.requestPermissions(this, permissions, AuthorityUtil.AUTHOR_CAMERA)
            } else {
                intentCamera()
            }
        }
    }

    /**
     * 申请存储权限
     */
    private fun checkStorageAuthority() {
        if (Build.VERSION.SDK_INT < 23) {
            init()
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !== PackageManager.PERMISSION_GRANTED) {
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                ActivityCompat.requestPermissions(this, permissions, AuthorityUtil.AUTHOR_STORAGE)
            } else {
                init()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            AuthorityUtil.AUTHOR_CAMERA -> if (grantResults.isNotEmpty()) {
                var isAllGranted = true //是否全部授权
                for (gResult in grantResults) {
                    if (gResult != PackageManager.PERMISSION_GRANTED) {
                        isAllGranted = false
                        break
                    }
                }
                if (isAllGranted) { //所有权限都授予
                    intentCamera()
                } else { //只要有一个没有授权，就提示进入设置界面设置
                    checkCameraAuthority()
                }
            } else {
                for (permission in permissions) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission!!)) {
                        checkCameraAuthority()
                        break
                    }
                }
            }
            AuthorityUtil.AUTHOR_STORAGE -> if (grantResults.isNotEmpty()) {
                var isAllGranted = true //是否全部授权
                for (gResult in grantResults) {
                    if (gResult != PackageManager.PERMISSION_GRANTED) {
                        isAllGranted = false
                        break
                    }
                }
                if (isAllGranted) { //所有权限都授予
                    init()
                } else { //只要有一个没有授权，就提示进入设置界面设置
                    checkStorageAuthority()
                }
            } else {
                for (permission in permissions) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission!!)) {
                        checkStorageAuthority()
                        break
                    }
                }
            }
        }
    }

}
