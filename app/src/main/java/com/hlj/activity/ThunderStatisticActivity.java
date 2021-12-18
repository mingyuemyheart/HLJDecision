package com.hlj.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.TextureMapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.GroundOverlay;
import com.amap.api.maps.model.GroundOverlayOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.hlj.common.CONST;
import com.hlj.dto.StrongStreamDto;
import com.hlj.utils.CommonUtil;
import com.hlj.utils.OkHttpUtil;
import com.hlj.view.ThunderView;
import com.hlj.view.wheelview.NumericWheelAdapter;
import com.hlj.view.wheelview.OnWheelScrollListener;
import com.hlj.view.wheelview.WheelView;
import com.hlj.view.wheelview.XunNumericWheelAdapter;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import shawn.cxwl.com.hlj.R;

/**
 * 雷电统计
 */
public class ThunderStatisticActivity extends BaseActivity implements View.OnClickListener, AMap.OnMapClickListener, GeocodeSearch.OnGeocodeSearchListener {

    private Context mContext;
    private TextureMapView mapView;
    private AMap aMap;//高德地图
    private AMapLocationClientOption mLocationOption;//声明mLocationOption对象
    private AMapLocationClient mLocationClient;//声明AMapLocationClient类对象
    private Marker locationMarker;
    private LatLng locationLatLng;
    private TextView tvPosition,tvStreet,tvThunder;
    private LinearLayout llContainer,llContainer1,llContainer2,llContainer3,llContainer4;
    private SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
    private SimpleDateFormat sdf2 = new SimpleDateFormat("dd", Locale.CHINA);
    private String TYPE = "hour";//数据类型，区分小时、天等
    private GeocodeSearch geocoderSearch;
    private int width = 0;
    private float density = 0;
    private boolean isChart = false;
    private ConstraintLayout clList,clChart;
    private ImageView ivControl,ivLegend;

    private TextView tvTime;
    private String hourTime = "20170101000000",dayTime = "20120101000000",tendaysTime = "20120101000000",monthAverTime = "20170101000000",monthTime = "20120101000000",yearTime = "20120101000000";
    private SimpleDateFormat sdf3 = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);
    private SimpleDateFormat sdf4 = new SimpleDateFormat("HH时", Locale.CHINA);
    private SimpleDateFormat sdf5 = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);
    private SimpleDateFormat sdf6 = new SimpleDateFormat("yyyy年MM月", Locale.CHINA);
    private SimpleDateFormat sdf7 = new SimpleDateFormat("MM月", Locale.CHINA);
    private SimpleDateFormat sdf8 = new SimpleDateFormat("yyyy年", Locale.CHINA);
    private String name, tag;

    private List<String> hourDatas = new ArrayList<>();
    private int hourIndex = 0;

    private List<String> xunList = new ArrayList<>();
    private int xunIndex = 0;

    private List<String> monthDatas = new ArrayList<>();
    private int monthIndex = 0;

    private List<String> yearDatas = new ArrayList<>();
    private int yearIndex = 0;

    private LinearLayout layoutDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thunder_statistic);
        mContext = this;
        showDialog();
        initAmap(savedInstanceState);
        initWidget();
    }

    private void initWidget() {
        ivControl = findViewById(R.id.ivControl);
        ivControl.setOnClickListener(this);
        ivControl.setVisibility(View.VISIBLE);
        ivControl.setImageResource(R.drawable.iv_warning_map);
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(getIntent().getStringExtra(CONST.ACTIVITY_NAME));
        LinearLayout llBack = findViewById(R.id.llBack);
        llBack.setOnClickListener(this);
        ImageView ivLocation = findViewById(R.id.ivLocation);
        ivLocation.setOnClickListener(this);
        tvPosition = findViewById(R.id.tvPosition);
        tvStreet = findViewById(R.id.tvStreet);
        tvThunder = findViewById(R.id.tvThunder);
        llContainer = findViewById(R.id.llContainer);
        llContainer1 = findViewById(R.id.llContainer1);
        llContainer2 = findViewById(R.id.llContainer2);
        llContainer3 = findViewById(R.id.llContainer3);
        llContainer4 = findViewById(R.id.llContainer4);
        clList = findViewById(R.id.clList);
        clChart = findViewById(R.id.clChart);
        tvTime = findViewById(R.id.tvTime);
        tvTime.setOnClickListener(this);
        ImageView ivPre = findViewById(R.id.ivPre);
        ivPre.setOnClickListener(this);
        ImageView ivNext = findViewById(R.id.ivNext);
        ivNext.setOnClickListener(this);
        layoutDate = findViewById(R.id.layoutDate);
        TextView tvNegtive = findViewById(R.id.tvNegtive);
        tvNegtive.setOnClickListener(this);
        TextView tvPositive = findViewById(R.id.tvPositive);
        tvPositive.setOnClickListener(this);
        ivLegend = findViewById(R.id.ivLegend);

        geocoderSearch = new GeocodeSearch(mContext);
        geocoderSearch.setOnGeocodeSearchListener(this);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        width = dm.widthPixels;
        density = dm.density;

        initData();
        initList();
        initChart();
    }

    private void initData() {
        //时平均列表信息
        hourDatas.clear();
        hourIndex = new Date().getHours();
        for (int i = 0; i < 24; i++) {
            String index = i+"";
            if (i < 10) {
                index = "0"+i;
            }
            String value = "20170101"+index+"0000";
            if (i == hourIndex) {
                hourTime = value;
            }
            hourDatas.add(value);
        }

        //逐日
        dayTime = "2012"+new SimpleDateFormat("MMdd", Locale.CHINA).format(new Date())+"000000";

        //获取旬列表信息
        xunList.clear();
        String dayStr;
        int day = new Date().getDay();
        if (day <= 10) {
            dayStr = "01";
        } else if (day <= 20) {
            dayStr = "10";
        } else {
            dayStr = "20";
        }
        int month = new Date().getMonth()+1;
        String monthStr = month+"";
        if (month < 10) {
            monthStr = "0"+month;
        }
        tendaysTime = "2012"+monthStr+dayStr+"000000";
        for (int i = 20120101; i <= 20171220; i++) {
            String count = String.valueOf(i);
            if (Integer.valueOf(count.substring(count.length()-4, count.length()-2)) <= 12 && Integer.valueOf(count.substring(count.length()-4, count.length()-2)) > 0) {
                String xun = count.substring(count.length()-2);
                if (TextUtils.equals(xun, "01") || TextUtils.equals(xun, "10") || TextUtils.equals(xun, "20")) {
                    xunList.add(count+"000000");
                }
            }
        }
        for (int i = 0; i < xunList.size(); i++) {
            if (TextUtils.equals(tendaysTime, xunList.get(i))) {
                xunIndex = i;
                break;
            }
        }

        //月平均列表信息
        monthDatas.clear();
        monthIndex = new Date().getMonth()+1;
        for (int i = 1; i <= 12; i++) {
            String index = i+"";
            if (i < 10) {
                index = "0"+i;
            }

            String value = "2017"+index+"01000000";
            if (i == monthIndex) {
                monthAverTime = value;
            }
            monthDatas.add(value);
        }

        //逐月
        monthTime = "2012"+monthStr+"01000000";

        //年列表信息
        yearDatas.clear();
        yearIndex = 0;
        for (int i = 2012; i <= 2017; i++) {
            yearDatas.add(i+"0101000000");
        }

        initHourWheelView();
        initDayWheelView();
        initXunWheelView();
        initMonthWheelView();
        initMonthMonthWheelView();
        initYearWheelView();
    }

    private void initList() {
        List<String> dataList = new ArrayList<>();
        dataList.add("小时,hour");
        dataList.add("日,day");
        dataList.add("旬,tendays");
        dataList.add("月,month");
        dataList.add("年,annual");

        llContainer.removeAllViews();
        llContainer1.removeAllViews();
        for (int i = 0; i < dataList.size(); i++) {
            String[] values = dataList.get(i).split(",");
            TextView tvName = new TextView(mContext);
            tvName.setGravity(Gravity.CENTER);
            tvName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            tvName.setPadding(0, (int)(density*3), 0, (int)(density*3));
            if (i == 0) {
                tvName.setTextColor(getResources().getColor(R.color.colorPrimary));
            }else {
                tvName.setTextColor(getResources().getColor(R.color.text_color3));
            }
            tvName.setText(values[0]);
            tvName.setTag(values[1]);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.weight = 1.0f;
            params.width = width/5;
            tvName.setLayoutParams(params);
            llContainer.addView(tvName, i);

            TextView tvBar = new TextView(mContext);
            tvBar.setGravity(Gravity.CENTER);
            if (i == 0) {
                tvBar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            }else {
                tvBar.setBackgroundColor(getResources().getColor(R.color.transparent));
            }
            tvBar.setTag(values[1]);
            LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params1.weight = 1.0f;
            params1.width = width/5-(int)(density*40);
            params1.height = (int) (density*2);
            params1.setMargins((int)(density*20), 0, (int)(density*20), 0);
            tvBar.setLayoutParams(params1);
            llContainer1.addView(tvBar, i);

            tvName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (llContainer != null) {
                        for (int i = 0; i < llContainer.getChildCount(); i++) {
                            TextView tvName = (TextView) llContainer.getChildAt(i);
                            if (TextUtils.equals(tvName.getTag()+"", v.getTag()+"")) {
                                tvName.setTextColor(getResources().getColor(R.color.colorPrimary));
                                OkHttpThunderStatistic(locationLatLng.longitude, locationLatLng.latitude, v.getTag()+"");
                            }else {
                                tvName.setTextColor(getResources().getColor(R.color.text_color3));
                            }
                        }
                    }

                    if (llContainer1 != null) {
                        for (int i = 0; i < llContainer1.getChildCount(); i++) {
                            TextView tvBar = (TextView) llContainer1.getChildAt(i);
                            if (TextUtils.equals(tvBar.getTag()+"", v.getTag()+"")) {
                                tvBar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                            }else {
                                tvBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                            }
                        }
                    }
                }
            });
        }

        checkLocationAuthority(new LocationCallback() {
            @Override
            public void grantedLocation(boolean isGranted) {
                if (isGranted) {
                    if (CommonUtil.isLocationOpen(mContext)) {
                        startLocation();
                    }else {
                        tvPosition.setText("北京市 | 东城区");
                        tvStreet.setText("正义路2号");
                        locationLatLng = new LatLng(39.904030, 116.407526);
                        addLocationMarker();
                        OkHttpThunderStatistic(locationLatLng.longitude, locationLatLng.latitude, "hour");
                    }
                } else {
                    tvPosition.setText("北京市 | 东城区");
                    tvStreet.setText("正义路2号");
                    locationLatLng = new LatLng(39.904030, 116.407526);
                    addLocationMarker();
                    OkHttpThunderStatistic(locationLatLng.longitude, locationLatLng.latitude, "hour");
                }
            }
        });

    }

    private void initChart() {
        List<String> dataList = new ArrayList<>();
        dataList.add("时平均,hour");
        dataList.add("逐日,day");
        dataList.add("逐旬,tendays");
        dataList.add("月平均,monthAver");
        dataList.add("逐月,month");
        dataList.add("逐年,year");

        llContainer3.removeAllViews();
        llContainer4.removeAllViews();
        for (int i = 0; i < dataList.size(); i++) {
            String[] values = dataList.get(i).split(",");
            TextView tvName = new TextView(mContext);
            tvName.setGravity(Gravity.CENTER);
            tvName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            tvName.setPadding(0, (int)(density*3), 0, (int)(density*3));
            if (i == 0) {
                name = values[0];
                tag = values[1];
                drawChartLayer();
                tvName.setTextColor(getResources().getColor(R.color.colorPrimary));
            }else {
                tvName.setTextColor(getResources().getColor(R.color.text_color3));
            }
            tvName.setText(values[0]);
            tvName.setTag(values[1]);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.weight = 1.0f;
            params.width = width/5;
            tvName.setLayoutParams(params);
            llContainer3.addView(tvName, i);

            TextView tvBar = new TextView(mContext);
            tvBar.setGravity(Gravity.CENTER);
            if (i == 0) {
                tvBar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            }else {
                tvBar.setBackgroundColor(getResources().getColor(R.color.transparent));
            }
            tvBar.setTag(values[1]);
            LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params1.weight = 1.0f;
            params1.width = width/5-(int)(density*40);
            params1.height = (int) (density*2);
            params1.setMargins((int)(density*20), 0, (int)(density*20), 0);
            tvBar.setLayoutParams(params1);
            llContainer4.addView(tvBar, i);

            tvName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    layoutDate.setVisibility(View.GONE);
                    if (llContainer3 != null) {
                        for (int i = 0; i < llContainer3.getChildCount(); i++) {
                            TextView tvName = (TextView) llContainer3.getChildAt(i);
                            if (TextUtils.equals(tvName.getTag()+"", v.getTag()+"")) {
                                name = tvName.getText().toString();
                                tag = v.getTag()+"";
                                tvName.setTextColor(getResources().getColor(R.color.colorPrimary));
                                drawChartLayer();
                            }else {
                                tvName.setTextColor(getResources().getColor(R.color.text_color3));
                            }
                        }
                    }

                    if (llContainer4 != null) {
                        for (int i = 0; i < llContainer4.getChildCount(); i++) {
                            TextView tvBar = (TextView) llContainer4.getChildAt(i);
                            if (TextUtils.equals(tvBar.getTag()+"", v.getTag()+"")) {
                                tvBar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                            }else {
                                tvBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                            }
                        }
                    }
                }
            });
        }
    }


    private GroundOverlay layerOverlay;
    private void drawChartLayer() {
        String imgUrl = null;
        if (TextUtils.equals(tag, "hour")) {
            imgUrl = String.format("http://decision-admin.tianqi.cn/infomes/data/lightning/nc_tj_data/multiyear_mean_lightDensity_hour/%s.png", hourTime);
            try {
                tvTime.setText(name+"闪电次数统计图层"+"\n"+sdf4.format(sdf3.parse(hourTime)));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Picasso.get().load("http://decision-admin.tianqi.cn/Public/images/lighting/multiyear_mean_lightDensity_hour.jpg").into(ivLegend);
        } else if (TextUtils.equals(tag, "day")) {
            imgUrl = String.format("http://decision-admin.tianqi.cn/infomes/data/lightning/nc_tj_data/LightDensity_day_2012_2017/%s.png", dayTime);
            try {
                tvTime.setText(name+"闪电次数统计图层"+"\n"+sdf5.format(sdf3.parse(dayTime)));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Picasso.get().load("http://decision-admin.tianqi.cn/Public/images/lighting/LightDensity_day_2012_2017.jpg").into(ivLegend);
        } else if (TextUtils.equals(tag, "tendays")) {
            imgUrl = String.format("http://decision-admin.tianqi.cn/infomes/data/lightning/nc_tj_data/LightDensity_Tendays_2012_2017/%s.png", tendaysTime);
            try {
                String xun = "";
                String day = tendaysTime.substring(6, 8);
                if (TextUtils.equals(day, "01")) {
                    xun = "上旬";
                } else if (TextUtils.equals(day, "10")) {
                    xun = "中旬";
                } else if (TextUtils.equals(day, "20")) {
                    xun = "下旬";
                }
                tvTime.setText(name+"闪电次数统计图层"+"\n"+sdf6.format(sdf3.parse(tendaysTime))+xun);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Picasso.get().load("http://decision-admin.tianqi.cn/Public/images/lighting/LightDensity_Tendays_2012_2017.jpg").into(ivLegend);
        } else if (TextUtils.equals(tag, "monthAver")) {
            imgUrl = String.format("http://decision-admin.tianqi.cn/infomes/data/lightning/nc_tj_data/multiyear_mean_lightDensity_month/%s.png", monthAverTime);
            try {
                tvTime.setText(name+"闪电次数统计图层"+"\n"+sdf7.format(sdf3.parse(monthAverTime)));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Picasso.get().load("http://decision-admin.tianqi.cn/Public/images/lighting/multiyear_mean_lightDensity_month.jpg").into(ivLegend);
        } else if (TextUtils.equals(tag, "month")) {
            imgUrl = String.format("http://decision-admin.tianqi.cn/infomes/data/lightning/nc_tj_data/LightDensity_month_2012_2017/%s.png", monthTime);
            try {
                tvTime.setText(name+"闪电次数统计图层"+"\n"+sdf6.format(sdf3.parse(monthTime)));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Picasso.get().load("http://decision-admin.tianqi.cn/Public/images/lighting/LightDensity_month_2012_2017.jpg").into(ivLegend);
        } else if (TextUtils.equals(tag, "year")) {
            imgUrl = String.format("http://decision-admin.tianqi.cn/infomes/data/lightning/nc_tj_data/LightDensity_Annual_2012_2017/%s.png", yearTime);
            try {
                tvTime.setText(name+"闪电次数统计图层"+"\n"+sdf8.format(sdf3.parse(yearTime)));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Picasso.get().load("http://decision-admin.tianqi.cn/Public/images/lighting/LightDensity_Annual_2012_2017.jpg").into(ivLegend);
        }
        if (TextUtils.isEmpty(imgUrl)) {
            return;
        }
        if (!isChart) {
            return;
        }
        Picasso.get().load(imgUrl).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                BitmapDescriptor fromView = BitmapDescriptorFactory.fromBitmap(bitmap);
                LatLngBounds bounds = new LatLngBounds.Builder()
                        .include(new LatLng(15.1, 70.0))
                        .include(new LatLng(55.0, 139.9))
                        .build();
                if (layerOverlay == null) {
                    layerOverlay = aMap.addGroundOverlay(new GroundOverlayOptions()
                            .anchor(0.5f, 0.5f)
                            .positionFromBounds(bounds)
                            .image(fromView)
                            .zIndex(1001)
                            .transparency(0.25f));
                } else {
                    layerOverlay.setImage(null);
                    layerOverlay.setPositionFromBounds(bounds);
                    layerOverlay.setImage(fromView);
                }
                aMap.runOnDrawFrame();
            }
            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
            }
            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        });
    }

    private void removeOverlay() {
        if (layerOverlay != null) {
            layerOverlay.remove();
            layerOverlay = null;
        }
    }

    private void showOverlay() {
        if (layerOverlay != null) {
            layerOverlay.setVisible(true);
        } else {
            drawChartLayer();
        }
    }

    private void hideOverlay() {
        if (layerOverlay != null) {
            layerOverlay.setVisible(false);
        }
    }

    /**
     * 初始化高德地图
     */
    private void initAmap(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mapView.getMap();
        }
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(35.926628, 105.178100), 4.0f));
        aMap.getUiSettings().setMyLocationButtonEnabled(false);// 设置默认定位按钮是否显示
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.getUiSettings().setRotateGesturesEnabled(false);
        aMap.setOnMapClickListener(this);
//        aMap.setOnMapLoadedListener(new AMap.OnMapLoadedListener() {
//            @Override
//            public void onMapLoaded() {
//                if (CommonUtil.isLocationOpen(mContext)) {
//                    startLocation();
//                }else {
//                    tvPosition.setText("北京市 | 东城区");
//                    tvStreet.setText("正义路2号");
//                    locationLatLng = new LatLng(39.904030, 116.407526);
//                    addLocationMarker();
//                    OkHttpThunderStatistic(locationLatLng.longitude, locationLatLng.latitude, "hour");
//                }
//            }
//        });
    }

    /**
     * 开始定位
     */
    private void startLocation() {
        if (mLocationOption == null) {
            mLocationOption = new AMapLocationClientOption();//初始化定位参数
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
            mLocationOption.setNeedAddress(true);//设置是否返回地址信息（默认返回地址信息）
            mLocationOption.setOnceLocation(true);//设置是否只定位一次,默认为false
            mLocationOption.setMockEnable(false);//设置是否允许模拟位置,默认为false，不允许模拟位置
            mLocationOption.setInterval(2000);//设置定位间隔,单位毫秒,默认为2000ms
        }
        if (mLocationClient == null) {
            mLocationClient = new AMapLocationClient(mContext);//初始化定位
            mLocationClient.setLocationOption(mLocationOption);//给定位客户端对象设置定位参数
        }
        mLocationClient.startLocation();//启动定位
        mLocationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                if (aMapLocation != null && aMapLocation.getErrorCode() == 0) {
                    tvPosition.setText(aMapLocation.getCity()+" | "+aMapLocation.getDistrict());
                    tvStreet.setText(aMapLocation.getStreet()+aMapLocation.getStreetNum());
                    locationLatLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
                    addLocationMarker();
                    OkHttpThunderStatistic(locationLatLng.longitude, locationLatLng.latitude, "hour");
                }
            }
        });
    }

    private void addLocationMarker() {
        if (locationLatLng == null) {
            return;
        }
        MarkerOptions options = new MarkerOptions();
        options.position(locationLatLng);
        options.anchor(0.5f, 1.0f);
        Bitmap bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(getResources(), R.drawable.shawn_iv_map_click_map),
                (int)(CommonUtil.dip2px(mContext, 21)), (int)(CommonUtil.dip2px(mContext, 32)));
        if (bitmap != null) {
            options.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
        }else {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.shawn_iv_map_click_map));
        }
        if (locationMarker != null) {
            locationMarker.remove();
        }
        locationMarker = aMap.addMarker(options);
        locationMarker.setClickable(false);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        searchAddrByLatLng(latLng.latitude, latLng.longitude);
        locationLatLng = latLng;
        addLocationMarker();
        OkHttpThunderStatistic(locationLatLng.longitude, locationLatLng.latitude, TYPE);
    }

    /**
     * 通过经纬度获取地理位置信息
     * @param lat
     * @param lng
     */
    private void searchAddrByLatLng(final double lat, final double lng) {
        //latLonPoint参数表示一个Latlng，第二参数表示范围多少米，GeocodeSearch.AMAP表示是国测局坐标系还是GPS原生坐标系
        showDialog();
        new Thread(new Runnable() {
            @Override
            public void run() {
                RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(lat, lng), 200, GeocodeSearch.AMAP);
                geocoderSearch.getFromLocationAsyn(query);
            }
        }).start();

    }

    @Override
    public void onGeocodeSearched(GeocodeResult arg0, int arg1) {
    }
    @Override
    public void onRegeocodeSearched(final RegeocodeResult result, int rCode) {
        if (result != null && result.getRegeocodeAddress() != null && result.getRegeocodeAddress().getFormatAddress() != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cancelDialog();
                    tvPosition.setText(result.getRegeocodeAddress().getCity()+" | "+result.getRegeocodeAddress().getDistrict());
                    tvStreet.setText("");
                }
            });
        }
    }

    /**
     * 雷电统计
     * @param lng
     * @param lat
     * @param type annual 年 month 月 tendays 旬  日day (暂无等待对方提供文件) 时 hour （暂无等待对方提供文件）
     */
    private void OkHttpThunderStatistic(double lng, double lat, final String type) {
        showDialog();
        this.TYPE = type;
        final String url = String.format("http://lightning.app.tianqi.cn/lightning/lhdata/ldtj?lonlat=%s,%s&type=%s", lng, lat, type);
        Log.e("url", url);
        new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpUtil.enqueue(new Request.Builder().url(url).build(), new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                    }
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            return;
                        }
                        final String result = response.body().string();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!TextUtils.isEmpty(result)) {
                                    try {
                                        JSONObject obj = new JSONObject(result);
                                        if (!obj.isNull("reminder")) {
                                            tvThunder.setText(obj.getString("reminder"));
                                        }else {
                                            tvThunder.setText("暂无数据");
                                        }

                                        String time = null;
                                        if (!obj.isNull("time")) {
                                            time = obj.getString("time");
                                        }

                                        if (!obj.isNull("data")) {
                                            List<StrongStreamDto> list = new ArrayList<>();
                                            JSONArray array = obj.getJSONArray("data");
                                            for (int i = 0; i < array.length(); i++) {
                                                StrongStreamDto dto = new StrongStreamDto();
                                                dto.thunderCount = array.getInt(i);
                                                if (TextUtils.equals(type, "hour")) {
                                                    dto.thunderTime = (i+1)+"";
                                                }else if (TextUtils.equals(type, "day")) {
                                                    if (!TextUtils.isEmpty(time) && time.contains("-")) {
                                                        String[] value = time.split("-");
                                                        try {
                                                            long t = sdf1.parse(value[0]).getTime()+i*1000*60*60*24;
                                                            dto.thunderTime = sdf2.format(new Date(t).getTime());
                                                        } catch (ParseException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                }else if (TextUtils.equals(type, "month")) {
                                                    dto.thunderTime = (i+1)+"";
                                                }else if (TextUtils.equals(type, "tendays")) {
                                                    dto.thunderTime = (i+1)+"";
                                                }else if (TextUtils.equals(type, "annual")) {
                                                    if (!TextUtils.isEmpty(time) && time.contains("-")) {
                                                        String[] value = time.split("-");
                                                        int t = Integer.parseInt(value[0])+i;
                                                        dto.thunderTime = t+"";
                                                    }
                                                }
                                                list.add(dto);
                                            }

                                            llContainer2.removeAllViews();
                                            ThunderView thunderView = new ThunderView(mContext);
                                            thunderView.setData(list, type);
                                            llContainer2.addView(thunderView, width-(int)(CommonUtil.dip2px(mContext, 30)), (int)(CommonUtil.dip2px(mContext, 120)));
                                        }

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                cancelDialog();
                            }
                        });
                    }
                });
            }
        }).start();
    }

    /**
     * 根据当前时间获取日期
     * @param i (+1为后一天，-1为前一天，0表示当天)
     * @return
     */
    private String getMonth(String time, int i) {
        Calendar c = Calendar.getInstance();
        try {
            Date date = sdf3.parse(time);
            c.setTime(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        c.add(Calendar.MONTH, i);
        return sdf3.format(c.getTime());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.llBack:
                finish();
                break;
            case R.id.ivControl:
                isChart = !isChart;
                if (isChart) {
                    ivControl.setImageResource(R.drawable.iv_warning_list);
                    clList.setVisibility(View.GONE);
                    clChart.setVisibility(View.VISIBLE);
                    ivLegend.setVisibility(View.VISIBLE);
                    showOverlay();
                } else {
                    ivControl.setImageResource(R.drawable.iv_warning_map);
                    clList.setVisibility(View.VISIBLE);
                    clChart.setVisibility(View.GONE);
                    ivLegend.setVisibility(View.GONE);
                    hideOverlay();
                }
                break;
            case R.id.ivLocation:
                if (locationLatLng != null) {
                    addLocationMarker();
                    aMap.animateCamera(CameraUpdateFactory.newLatLng(locationLatLng));
                }
                break;
            case R.id.ivPre:
                try {
                    if (TextUtils.equals(tag, "hour")) {
                        if (sdf3.parse(hourTime).getTime() <= sdf3.parse(hourDatas.get(0)).getTime()) {
                            hourIndex = hourDatas.size()-1;
                        } else {
                            hourIndex--;
                        }
                        hourTime = hourDatas.get(hourIndex);
                    } else if (TextUtils.equals(tag, "day")) {
                        if (sdf3.parse(dayTime).getTime() <= sdf3.parse("20120101000000").getTime()) {
                            dayTime = "20171231000000";
                        } else {
                            dayTime = sdf3.format(sdf3.parse(dayTime).getTime()-1000*60*60*24);
                        }
                    } else if (TextUtils.equals(tag, "tendays")) {
                        if (sdf3.parse(tendaysTime).getTime() <= sdf3.parse(xunList.get(0)).getTime()) {
                            xunIndex = xunList.size()-1;
                        } else {
                            xunIndex--;
                        }
                        tendaysTime = xunList.get(xunIndex);
                    } else if (TextUtils.equals(tag, "monthAver")) {
                        if (sdf3.parse(monthAverTime).getTime() <= sdf3.parse(monthDatas.get(0)).getTime()) {
                            monthIndex = monthDatas.size()-1;
                        } else {
                            monthIndex--;
                        }
                        monthAverTime = monthDatas.get(monthIndex);
                    } else if (TextUtils.equals(tag, "month")) {
                        if (sdf3.parse(monthTime).getTime() <= sdf3.parse("20120101000000").getTime()) {
                            monthTime = "20171201000000";
                        } else {
                            monthTime = getMonth(monthTime, -1);
                        }
                    } else if (TextUtils.equals(tag, "year")) {
                        if (sdf3.parse(yearTime).getTime() <= sdf3.parse(yearDatas.get(0)).getTime()) {
                            yearIndex = yearDatas.size()-1;
                        } else {
                            yearIndex--;
                        }
                        yearTime = yearDatas.get(yearIndex);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                drawChartLayer();
                break;
            case R.id.ivNext:
                try {
                    if (TextUtils.equals(tag, "hour")) {
                        if (sdf3.parse(hourTime).getTime() >= sdf3.parse(hourDatas.get(hourDatas.size()-1)).getTime()) {
                            hourIndex = 0;
                        } else {
                            hourIndex++;
                        }
                        hourTime = hourDatas.get(hourIndex);
                    } else if (TextUtils.equals(tag, "day")) {
                        if (sdf3.parse(dayTime).getTime() >= sdf3.parse("20171231000000").getTime()) {
                            dayTime = "20120101000000";
                        } else {
                            dayTime = sdf3.format(sdf3.parse(dayTime).getTime()+1000*60*60*24);
                        }
                    } else if (TextUtils.equals(tag, "tendays")) {
                        if (sdf3.parse(tendaysTime).getTime() >= sdf3.parse(xunList.get(xunList.size()-1)).getTime()) {
                            xunIndex = 0;
                        } else {
                            xunIndex++;
                        }
                        tendaysTime = xunList.get(xunIndex);
                    } else if (TextUtils.equals(tag, "monthAver")) {
                        if (sdf3.parse(monthAverTime).getTime() >= sdf3.parse(monthDatas.get(monthDatas.size()-1)).getTime()) {
                            monthIndex = 0;
                        } else {
                            monthIndex++;
                        }
                        monthAverTime = monthDatas.get(monthIndex);
                    } else if (TextUtils.equals(tag, "month")) {
                        if (sdf3.parse(monthTime).getTime() >= sdf3.parse("20171201000000").getTime()) {
                            monthTime = "20120101000000";
                        } else {
                            monthTime = getMonth(monthTime, 1);
                        }
                    } else if (TextUtils.equals(tag, "year")) {
                        if (sdf3.parse(yearTime).getTime() >= sdf3.parse(yearDatas.get(yearDatas.size()-1)).getTime()) {
                            yearIndex = 0;
                        } else {
                            yearIndex++;
                        }
                        yearTime = yearDatas.get(yearIndex);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                drawChartLayer();
                break;
            case R.id.tvTime:
                layoutDate.setVisibility(View.VISIBLE);
                if (TextUtils.equals(tag, "hour")) {
                    hourWheelView.setVisibility(View.VISIBLE);
                    year.setVisibility(View.GONE);
                    month.setVisibility(View.GONE);
                    day.setVisibility(View.GONE);
                    xunyear.setVisibility(View.GONE);
                    xunmonth.setVisibility(View.GONE);
                    xun.setVisibility(View.GONE);
                    monthWheelView.setVisibility(View.GONE);
                    monthYear.setVisibility(View.GONE);
                    monthMonth.setVisibility(View.GONE);
                    yearWheelView.setVisibility(View.GONE);
                } else if (TextUtils.equals(tag, "day")) {
                    hourWheelView.setVisibility(View.GONE);
                    year.setVisibility(View.VISIBLE);
                    month.setVisibility(View.VISIBLE);
                    day.setVisibility(View.VISIBLE);
                    xunyear.setVisibility(View.GONE);
                    xunmonth.setVisibility(View.GONE);
                    xun.setVisibility(View.GONE);
                    monthWheelView.setVisibility(View.GONE);
                    monthYear.setVisibility(View.GONE);
                    monthMonth.setVisibility(View.GONE);
                    yearWheelView.setVisibility(View.GONE);
                } else if (TextUtils.equals(tag, "tendays")) {
                    hourWheelView.setVisibility(View.GONE);
                    year.setVisibility(View.GONE);
                    month.setVisibility(View.GONE);
                    day.setVisibility(View.GONE);
                    xunyear.setVisibility(View.VISIBLE);
                    xunmonth.setVisibility(View.VISIBLE);
                    xun.setVisibility(View.VISIBLE);
                    monthWheelView.setVisibility(View.GONE);
                    monthYear.setVisibility(View.GONE);
                    monthMonth.setVisibility(View.GONE);
                    yearWheelView.setVisibility(View.GONE);
                } else if (TextUtils.equals(tag, "monthAver")) {
                    hourWheelView.setVisibility(View.GONE);
                    year.setVisibility(View.GONE);
                    month.setVisibility(View.GONE);
                    day.setVisibility(View.GONE);
                    xunyear.setVisibility(View.GONE);
                    xunmonth.setVisibility(View.GONE);
                    xun.setVisibility(View.GONE);
                    monthWheelView.setVisibility(View.VISIBLE);
                    monthYear.setVisibility(View.GONE);
                    monthMonth.setVisibility(View.GONE);
                    yearWheelView.setVisibility(View.GONE);
                } else if (TextUtils.equals(tag, "month")) {
                    hourWheelView.setVisibility(View.GONE);
                    year.setVisibility(View.GONE);
                    month.setVisibility(View.GONE);
                    day.setVisibility(View.GONE);
                    xunyear.setVisibility(View.GONE);
                    xunmonth.setVisibility(View.GONE);
                    xun.setVisibility(View.GONE);
                    monthWheelView.setVisibility(View.GONE);
                    monthYear.setVisibility(View.VISIBLE);
                    monthMonth.setVisibility(View.VISIBLE);
                    yearWheelView.setVisibility(View.GONE);
                } else if (TextUtils.equals(tag, "year")) {
                    hourWheelView.setVisibility(View.GONE);
                    year.setVisibility(View.GONE);
                    month.setVisibility(View.GONE);
                    day.setVisibility(View.GONE);
                    xunyear.setVisibility(View.GONE);
                    xunmonth.setVisibility(View.GONE);
                    xun.setVisibility(View.GONE);
                    monthWheelView.setVisibility(View.GONE);
                    monthYear.setVisibility(View.GONE);
                    monthMonth.setVisibility(View.GONE);
                    yearWheelView.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.tvNegtive:
                layoutDate.setVisibility(View.GONE);
                break;
            case R.id.tvPositive:
                if (TextUtils.equals(tag, "hour")) {
                    hourIndex = hourWheelView.getCurrentItem();
                    hourTime = "20170101"+(((hourWheelView.getCurrentItem()+1) < 10) ? "0" + (hourWheelView.getCurrentItem()) : (hourWheelView.getCurrentItem()))+"0000";
                    try {
                        tvTime.setText(name+"闪电次数统计图层"+"\n"+sdf4.format(sdf3.parse(hourTime)));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else if (TextUtils.equals(tag, "day")) {
                    String yearStr = String.valueOf(year.getCurrentItem()+2012);
                    String monthStr = String.valueOf((month.getCurrentItem()+1) < 10 ? "0" + (month.getCurrentItem()+1) : (month.getCurrentItem()+1));
                    String dayStr = String.valueOf(((day.getCurrentItem()+1) < 10) ? "0" + (day.getCurrentItem()+1) : (day.getCurrentItem()+1));
                    dayTime = yearStr+monthStr+dayStr+"000000";
                    try {
                        tvTime.setText(name+"闪电次数统计图层"+"\n"+sdf5.format(sdf3.parse(dayTime)));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else if (TextUtils.equals(tag, "tendays")) {
                    String yearStr = String.valueOf(xunyear.getCurrentItem()+2012);
                    String monthStr = String.valueOf((xunmonth.getCurrentItem()+1) < 10 ? "0" + (xunmonth.getCurrentItem()+1) : (xunmonth.getCurrentItem()+1));
                    String xunStr = String.valueOf((xun.getCurrentItem()+1) < 10 ? "0" + (xun.getCurrentItem()+1) : (xun.getCurrentItem()+1));
                    if (TextUtils.equals(xunStr, "01")) {
                        xunStr = "01";
                    } else if (TextUtils.equals(xunStr, "02")) {
                        xunStr = "10";
                    } else if (TextUtils.equals(xunStr, "03")) {
                        xunStr = "20";
                    }
                    tendaysTime = yearStr+monthStr+xunStr+"000000";
                    try {
                        String xun = "";
                        String day = tendaysTime.substring(6, 8);
                        if (TextUtils.equals(day, "01")) {
                            xun = "上旬";
                        } else if (TextUtils.equals(day, "10")) {
                            xun = "中旬";
                        } else if (TextUtils.equals(day, "20")) {
                            xun = "下旬";
                        }
                        tvTime.setText(name+"闪电次数统计图层"+"\n"+sdf6.format(sdf3.parse(tendaysTime))+xun);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    for (int i = 0; i < xunList.size(); i++) {
                        if (TextUtils.equals(tendaysTime, xunList.get(i))) {
                            xunIndex = i;
                            break;
                        }
                    }
                } else if (TextUtils.equals(tag, "monthAver")) {
                    monthIndex = monthWheelView.getCurrentItem();
                    monthAverTime = "2017"+(((monthWheelView.getCurrentItem()+1) < 10) ? "0" + (monthWheelView.getCurrentItem()+1) : (monthWheelView.getCurrentItem()+1))+"01000000";
                    try {
                        tvTime.setText(name+"闪电次数统计图层"+"\n"+sdf7.format(sdf3.parse(monthAverTime)));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else if (TextUtils.equals(tag, "month")) {
                    String yearStr = String.valueOf(monthYear.getCurrentItem()+2012);
                    String monthStr = String.valueOf((monthMonth.getCurrentItem()+1) < 10 ? "0" + (monthMonth.getCurrentItem()+1) : (monthMonth.getCurrentItem()+1));
                    monthTime = yearStr+monthStr+"01000000";
                    try {
                        tvTime.setText(name+"闪电次数统计图层"+"\n"+sdf6.format(sdf3.parse(monthTime)));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else if (TextUtils.equals(tag, "year")) {
                    yearIndex = yearWheelView.getCurrentItem();
                    String yearStr = String.valueOf(yearWheelView.getCurrentItem()+2012);
                    yearTime = yearStr+"0101000000";
                    try {
                        tvTime.setText(name+"闪电次数统计图层"+"\n"+sdf8.format(sdf3.parse(yearTime)));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                drawChartLayer();
                layoutDate.setVisibility(View.GONE);
                break;
        }
    }

    private WheelView hourWheelView;
    private void initHourWheelView() {
        hourWheelView = findViewById(R.id.hourWheelView);
        NumericWheelAdapter numericWheelAdapter3=new NumericWheelAdapter(mContext,0, 23, "%02d");
        numericWheelAdapter3.setLabel("");
        hourWheelView.setViewAdapter(numericWheelAdapter3);
        hourWheelView.setCyclic(false);
        hourWheelView.setCurrentItem(0);
        hourWheelView.setVisibleItems(7);
        hourWheelView.addScrollingListener(new OnWheelScrollListener() {
            @Override
            public void onScrollingStarted(WheelView wheel) {
            }
            @Override
            public void onScrollingFinished(WheelView wheel) {

            }
        });
    }

    private WheelView year, month, day;
    private void initDayWheelView() {
        int curYear = 2012;
        int curMonth = 1;
        int curDate = 1;

        year = findViewById(R.id.year);
        NumericWheelAdapter numericWheelAdapter1=new NumericWheelAdapter(mContext,2012, 2017);
        numericWheelAdapter1.setLabel("年");
        year.setViewAdapter(numericWheelAdapter1);
        year.setCyclic(false);//是否可循环滑动
        year.addScrollingListener(dayscrollListener);

        month = findViewById(R.id.month);
        NumericWheelAdapter numericWheelAdapter2=new NumericWheelAdapter(mContext,1, 12, "%02d");
        numericWheelAdapter2.setLabel("月");
        month.setViewAdapter(numericWheelAdapter2);
        month.setCyclic(false);
        month.addScrollingListener(dayscrollListener);

        day = findViewById(R.id.day);
        initDay(curYear,curMonth);
        day.setCyclic(false);

        year.setVisibleItems(7);
        month.setVisibleItems(7);
        day.setVisibleItems(7);

        year.setCurrentItem(curYear-2012);
        month.setCurrentItem(curMonth-1);
        day.setCurrentItem(curDate-1);
    }

    private OnWheelScrollListener dayscrollListener = new OnWheelScrollListener() {
        @Override
        public void onScrollingStarted(WheelView wheel) {
        }
        @Override
        public void onScrollingFinished(WheelView wheel) {
            int n_year = year.getCurrentItem()+2012;//年
            int n_month = month.getCurrentItem()+1;//月
            initDay(n_year,n_month);
        }
    };

    /**
     */
    private void initDay(int arg1, int arg2) {
        NumericWheelAdapter numericWheelAdapter=new NumericWheelAdapter(mContext,1, getDay(arg1, arg2), "%02d");
        numericWheelAdapter.setLabel("日");
        day.setViewAdapter(numericWheelAdapter);
    }

    /**
     *
     * @param year
     * @param month
     * @return
     */
    private int getDay(int year, int month) {
        int day;
        boolean flag;
        switch (year % 4) {
            case 0:
                flag = true;
                break;
            default:
                flag = false;
                break;
        }
        switch (month) {
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                day = 31;
                break;
            case 2:
                day = flag ? 29 : 28;
                break;
            default:
                day = 30;
                break;
        }
        return day;
    }

    private WheelView xunyear, xunmonth, xun;
    private void initXunWheelView() {
        int curYear = 2012;
        int curMonth = 1;
        int curDate = 1;

        xunyear = findViewById(R.id.xunyear);
        NumericWheelAdapter numericWheelAdapter1=new NumericWheelAdapter(mContext,2012, 2017);
        numericWheelAdapter1.setLabel("年");
        xunyear.setViewAdapter(numericWheelAdapter1);
        xunyear.setCyclic(false);//是否可循环滑动

        xunmonth = findViewById(R.id.xunmonth);
        NumericWheelAdapter numericWheelAdapter2=new NumericWheelAdapter(mContext,1, 12, "%02d");
        numericWheelAdapter2.setLabel("月");
        xunmonth.setViewAdapter(numericWheelAdapter2);
        xunmonth.setCyclic(false);

        xun = findViewById(R.id.xun);
        XunNumericWheelAdapter numericWheelAdapter3=new XunNumericWheelAdapter(mContext,1, 3, "%02d");
        numericWheelAdapter3.setLabel("旬");
        xun.setViewAdapter(numericWheelAdapter3);
        xun.setCyclic(false);

        xunyear.setVisibleItems(7);
        xunmonth.setVisibleItems(7);
        xun.setVisibleItems(7);

        xunyear.setCurrentItem(curYear-2012);
        xunmonth.setCurrentItem(curMonth-1);
        xun.setCurrentItem(curDate-1);
    }

    private WheelView monthWheelView;
    private void initMonthWheelView() {
        monthWheelView = findViewById(R.id.monthWheelView);
        NumericWheelAdapter numericWheelAdapter3=new NumericWheelAdapter(mContext,1, 12, "%02d");
        numericWheelAdapter3.setLabel("");
        monthWheelView.setViewAdapter(numericWheelAdapter3);
        monthWheelView.setCyclic(false);
        monthWheelView.setCurrentItem(0);
        monthWheelView.setVisibleItems(7);
        monthWheelView.addScrollingListener(new OnWheelScrollListener() {
            @Override
            public void onScrollingStarted(WheelView wheel) {
            }
            @Override
            public void onScrollingFinished(WheelView wheel) {

            }
        });
    }

    private WheelView monthYear, monthMonth;
    private void initMonthMonthWheelView() {
        int curYear = 2012;
        int curMonth = 1;

        monthYear = findViewById(R.id.monthYear);
        NumericWheelAdapter numericWheelAdapter1=new NumericWheelAdapter(mContext,2012, 2017);
        numericWheelAdapter1.setLabel("年");
        monthYear.setViewAdapter(numericWheelAdapter1);
        monthYear.setCyclic(false);//是否可循环滑动

        monthMonth = findViewById(R.id.monthMonth);
        NumericWheelAdapter numericWheelAdapter2=new NumericWheelAdapter(mContext,1, 12, "%02d");
        numericWheelAdapter2.setLabel("月");
        monthMonth.setViewAdapter(numericWheelAdapter2);
        monthMonth.setCyclic(false);

        xunyear.setVisibleItems(7);
        xunmonth.setVisibleItems(7);

        xunyear.setCurrentItem(curYear-2012);
        xunmonth.setCurrentItem(curMonth-1);
    }

    private WheelView yearWheelView;
    private void initYearWheelView() {
        int curYear = 2012;
        yearWheelView = findViewById(R.id.yearWheelView);
        NumericWheelAdapter numericWheelAdapter3=new NumericWheelAdapter(mContext,2012, 2017, "%02d");
        numericWheelAdapter3.setLabel("年");
        yearWheelView.setViewAdapter(numericWheelAdapter3);
        yearWheelView.setCyclic(false);
        yearWheelView.setCurrentItem(curYear-2012);
        yearWheelView.setVisibleItems(7);
        yearWheelView.addScrollingListener(new OnWheelScrollListener() {
            @Override
            public void onScrollingStarted(WheelView wheel) {
            }
            @Override
            public void onScrollingFinished(WheelView wheel) {

            }
        });
    }

    /**
     * 方法必须重写
     */
    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
    }

}
