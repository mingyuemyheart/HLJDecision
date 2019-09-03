package com.hlj.common;

import com.amap.api.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class CONST {

	public static String noValue = "--";
	public static final String imageSuffix = ".png";//图标后缀名
	public static final String publicUser = "publicuser";//公众账号
	public static final String publicPwd = "123456";//公众密码
	public static String APPID = "18";//贵州客户端对应服务器的appid
	public static final LatLng guizhouLatLng = new LatLng(48.602915,128.121040);//贵州中点
	public static boolean isDelete = false;//判断是否可以删除
	public static String BUILD_URL = "http://decision-admin.tianqi.cn/infomes/data/heilongjiang/about_heilongjiang.html";//企业信息地址
	public static final String ZERO = "0";//不显示频道
	public static final String ONE = "1";//显示频道
	public static final String POSITION = "position";//频道对应的下标

	//showType类型，区分本地类或者图文
	public static final String LOCAL = "local";
	public static final String NEWS = "news";
	public static final String PRODUCT = "product";
	public static final String URL = "url";
	public static final String PDF = "pdf";

	//intent传值的标示
	public static final String INTENT_APPID = "intent_appid";
	public static final String COLUMN_ID = "column_id";//栏目id
	public static final String WEB_URL = "web_Url";//网页地址的标示
	public static final String ACTIVITY_NAME = "activity_name";//界面名称
	public static final String INTENT_IMGURL = "intent_imgurl";//分享时分享的图片

	//下拉刷新progresBar四种颜色
	public static final int color1 = android.R.color.holo_blue_bright;
	public static final int color2 = android.R.color.holo_blue_light;
	public static final int color3 = android.R.color.holo_blue_bright;
	public static final int color4 = android.R.color.holo_blue_light;

	//本地保存用户信息参数
	public static String USERINFO = "userInfo";//userInfo sharedPreferance名称
	public static class UserInfo {
		public static final String uId = "uId";
		public static final String userName = "uName";
		public static final String passWord = "pwd";
	}

	public static String UID = null;//用户id
	public static String USERNAME = null;//用户名
	public static String PASSWORD = null;//用户密码

	public static final List<ColumnData> dataList = new ArrayList<>();//存放接口数据对象

	//预警颜色对应规则
	public static String[] blue = {"01", "_blue"};
	public static String[] yellow = {"02", "_yellow"};
	public static String[] orange = {"03", "_orange"};
	public static String[] red = {"04", "_red"};

	//贵州接口
	public static String GUIZHOU_BASE = "http://decision-admin.tianqi.cn/Home";

}
