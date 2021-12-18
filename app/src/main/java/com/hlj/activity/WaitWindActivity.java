package com.hlj.activity;

import android.animation.IntEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.OnCameraChangeListener;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.hlj.common.CONST;
import com.hlj.dto.TyphoonDto;
import com.hlj.dto.WindData;
import com.hlj.dto.WindDto;
import com.hlj.utils.CommonUtil;
import com.hlj.utils.OkHttpUtil;
import com.hlj.utils.SecretUrlUtil;
import com.hlj.view.WaitWindView2;
import com.hlj.view.WindForeView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import shawn.cxwl.com.hlj.R;

/**
 * 等风来
 */
public class WaitWindActivity extends BaseActivity implements OnClickListener, OnCameraChangeListener,
        AMap.OnMapClickListener, AMapLocationListener {

    private Context mContext;
    private LinearLayout llHeight,llContainer1;
    private TextView tvTitle,tvFileTime,tvHeight200,tvHeight500,tvHeight1000,tvLocation;
    private ImageView ivArrow, ivHeight,ivSwitch,ivLocation;
    private MapView mapView;
    private AMap aMap;
    private float zoom = 5.5f;
    private SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy", Locale.CHINA);
    private SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMddHH", Locale.CHINA);
    private SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy年MM月dd日HH时", Locale.CHINA);
    private RelativeLayout container;
    public RelativeLayout container2;
    private int width = 0, height = 0, swithWidth;
    private WaitWindView2 waitWindView;
    private boolean isGfs = true;//默认为风场新数据
    private WindData windDataGFS,windDataT639;
    private GeocodeSearch geocoderSearch;
    private Marker locationMarker;
    private double locationLat = CONST.defaultLat, locationLng = CONST.defaultLng;
    private String dataHeight = "1000";
    private boolean isShowDetail = false;
    private boolean isShowHeight = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wait_wind);
        mContext = this;
        showDialog();
        initAmap(savedInstanceState);
        initWidget();
    }

    private void initAmap(Bundle bundle) {
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(bundle);
        if (aMap == null) {
            aMap = mapView.getMap();
        }
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CONST.guizhouLatLng, zoom));
        aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.getUiSettings().setRotateGesturesEnabled(false);
        aMap.setOnCameraChangeListener(this);
        aMap.setOnMapClickListener(this);
        aMap.setOnMapLoadedListener(new AMap.OnMapLoadedListener() {
            @Override
            public void onMapLoaded() {
                TextView tvMapNumber = findViewById(R.id.tvMapNumber);
                tvMapNumber.setText(aMap.getMapContentApprovalNumber());
                CommonUtil.drawHLJJson(mContext, aMap);
            }
        });
    }

    private void initWidget() {
        LinearLayout llBack = findViewById(R.id.llBack);
        llBack.setOnClickListener(this);
        tvTitle = findViewById(R.id.tvTitle);
        tvFileTime = findViewById(R.id.tvFileTime);
        container = findViewById(R.id.container);
        container2 = findViewById(R.id.container2);
        ivSwitch = findViewById(R.id.ivSwitch);
        ivSwitch.setOnClickListener(this);
        ivLocation = findViewById(R.id.ivLocation);
        ivLocation.setOnClickListener(this);
        ivHeight = findViewById(R.id.ivHeight);
        ivHeight.setOnClickListener(this);
        llHeight = findViewById(R.id.llHeight);
        tvHeight200 = findViewById(R.id.tvHeight200);
        tvHeight200.setOnClickListener(this);
        tvHeight500 = findViewById(R.id.tvHeight500);
        tvHeight500.setOnClickListener(this);
        tvHeight1000 = findViewById(R.id.tvHeight1000);
        tvHeight1000.setOnClickListener(this);
        tvLocation = findViewById(R.id.tvLocation);
        llContainer1 = findViewById(R.id.llContainer1);
        ivArrow = findViewById(R.id.ivArrow);
        ivArrow.setOnClickListener(this);
        LinearLayout llDetail = findViewById(R.id.llDetail);
        llDetail.setOnClickListener(this);

        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        llHeight.measure(w, h);
        swithWidth = llHeight.getMeasuredWidth();

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        width = dm.widthPixels;
        height = dm.heightPixels;

        String title = getIntent().getStringExtra(CONST.ACTIVITY_NAME);
        if (title != null) {
            tvTitle.setText(title);
        }

        checkLocationAuthority(new LocationCallback() {
            @Override
            public void grantedLocation(boolean isGranted) {
                if (isGranted) {
                    startLocation();
                } else {
                    ivLocation.setVisibility(View.VISIBLE);
                    addLocationMarker();
                }
            }
        });

        OkHttpGFS();

        int currentYear = Integer.valueOf(sdf1.format(new Date()));
        OkHttpTyphoonList(currentYear);
    }

    /**
     * 开始定位
     */
    private void startLocation() {
        AMapLocationClientOption mLocationOption = new AMapLocationClientOption();//初始化定位参数
        AMapLocationClient mLocationClient = new AMapLocationClient(mContext);//初始化定位
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setNeedAddress(true);//设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setOnceLocation(true);//设置是否只定位一次,默认为false
        mLocationOption.setWifiActiveScan(true);//设置是否强制刷新WIFI，默认为强制刷新
        mLocationOption.setMockEnable(false);//设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setInterval(2000);//设置定位间隔,单位毫秒,默认为2000ms
        mLocationClient.setLocationOption(mLocationOption);//给定位客户端对象设置定位参数
        mLocationClient.setLocationListener(this);
        mLocationClient.startLocation();//启动定位
    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation != null && amapLocation.getErrorCode() == AMapLocation.LOCATION_SUCCESS) {
            locationLat = amapLocation.getLatitude();
            locationLng = amapLocation.getLongitude();
            ivLocation.setVisibility(View.VISIBLE);
            addLocationMarker();
        }
    }

    private void addLocationMarker() {
        MarkerOptions options = new MarkerOptions();
        options.position(new LatLng(locationLat, locationLng));
        options.anchor(0.5f, 1.0f);
        Bitmap bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(getResources(), R.drawable.icon_map_location),
                (int)(CommonUtil.dip2px(mContext, 16)), (int)(CommonUtil.dip2px(mContext, 24)));
        if (bitmap != null) {
            options.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
        }else {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_map_location));
        }
        if (locationMarker != null) {
            locationMarker.remove();
        }
        locationMarker = aMap.addMarker(options);
        locationMarker.setClickable(false);

        //latLonPoint参数表示一个Latlng，第二参数表示范围多少米，GeocodeSearch.AMAP表示是国测局坐标系还是GPS原生坐标系
        RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(locationLat, locationLng), 200, GeocodeSearch.AMAP);
        if (geocoderSearch == null) {
            geocoderSearch = new GeocodeSearch(mContext);
        }
        geocoderSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
            @Override
            public void onGeocodeSearched(GeocodeResult arg0, int arg1) {
            }
            @Override
            public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
                if (result != null && result.getRegeocodeAddress().getFormatAddress() != null) {
                    tvLocation.setText(result.getRegeocodeAddress().getFormatAddress());
                }
            }
        });
        geocoderSearch.getFromLocationAsyn(query);

        OkHttpWindDetail(SecretUrlUtil.windDetail(locationLng, locationLat));
    }

    @Override
    public void onMapClick(LatLng latLng) {
        locationLat = latLng.latitude;
        locationLng = latLng.longitude;
        addLocationMarker();
    }

    /**
     * 获取某点的风速信息
     */
    private void OkHttpWindDetail(final String url) {
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
                                        if (!obj.isNull("forecast")) {
                                            List<WindDto> windList = new ArrayList<>();
                                            JSONArray array = obj.getJSONArray("forecast");
                                            for (int i = 0; i < array.length(); i+=3) {
                                                JSONObject itemObj = array.getJSONObject(i);
                                                WindDto dto = new WindDto();
                                                if (!itemObj.isNull("speed")) {
                                                    dto.speed = itemObj.getString("speed");
                                                }
                                                if (!itemObj.isNull("date")) {
                                                    dto.date = itemObj.getString("date");
                                                }
                                                windList.add(dto);
                                            }

                                            llContainer1.removeAllViews();
                                            WindForeView cubicView = new WindForeView(mContext);
                                            cubicView.setData(windList);
                                            llContainer1.addView(cubicView, width, height/4);
                                        }
                                    } catch (JSONException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            }
                        });
                    }
                });
            }
        }).start();
    }

    private void OkHttpGFS() {
        if (windDataGFS != null) {
            return;
        }
        showDialog();
        new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpUtil.enqueue(new Request.Builder().url(SecretUrlUtil.windGFS(dataHeight)).build(), new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                    }
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            return;
                        }
                        String result = response.body().string();
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                JSONObject obj = new JSONObject(result);
                                if (windDataGFS == null) {
                                    windDataGFS = new WindData();
                                }
                                if (!obj.isNull("gridHeight")) {
                                    windDataGFS.height = obj.getInt("gridHeight");
                                }
                                if (!obj.isNull("gridWidth")) {
                                    windDataGFS.width = obj.getInt("gridWidth");
                                }
                                if (!obj.isNull("x0")) {
                                    windDataGFS.x0 = obj.getDouble("x0");
                                }
                                if (!obj.isNull("y0")) {
                                    windDataGFS.y0 = obj.getDouble("y0");
                                }
                                if (!obj.isNull("x1")) {
                                    windDataGFS.x1 = obj.getDouble("x1");
                                }
                                if (!obj.isNull("y1")) {
                                    windDataGFS.y1 = obj.getDouble("y1");
                                }
                                if (!obj.isNull("filetime")) {
                                    windDataGFS.filetime = obj.getString("filetime");
                                }

                                if (!obj.isNull("field")) {
                                    windDataGFS.dataList.clear();
                                    JSONArray array = new JSONArray(obj.getString("field"));
                                    for (int i = 0; i < array.length(); i += 2) {
                                        WindDto dto2 = new WindDto();
                                        dto2.initX = (float) (array.optDouble(i));
                                        dto2.initY = (float) (array.optDouble(i + 1));
                                        windDataGFS.dataList.add(dto2);
                                    }
                                }

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        cancelDialog();
                                        reloadWind(true);
                                    }
                                });

                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                });
            }
        }).start();
    }

    private void OkHttpT639() {
        if (windDataT639 != null) {
            return;
        }
        showDialog();
        new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpUtil.enqueue(new Request.Builder().url(SecretUrlUtil.windT639(dataHeight, "0")).build(), new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                    }
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            return;
                        }
                        String result = response.body().string();
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                JSONObject obj = new JSONObject(result);
                                if (windDataT639 == null) {
                                    windDataT639 = new WindData();
                                }
                                if (!obj.isNull("gridHeight")) {
                                    windDataT639.height = obj.getInt("gridHeight");
                                }
                                if (!obj.isNull("gridWidth")) {
                                    windDataT639.width = obj.getInt("gridWidth");
                                }
                                if (!obj.isNull("x0")) {
                                    windDataT639.x0 = obj.getDouble("x0");
                                }
                                if (!obj.isNull("y0")) {
                                    windDataT639.y0 = obj.getDouble("y0");
                                }
                                if (!obj.isNull("x1")) {
                                    windDataT639.x1 = obj.getDouble("x1");
                                }
                                if (!obj.isNull("y1")) {
                                    windDataT639.y1 = obj.getDouble("y1");
                                }
                                if (!obj.isNull("filetime")) {
                                    windDataT639.filetime = obj.getString("filetime");
                                }

                                if (!obj.isNull("field")) {
                                    windDataT639.dataList.clear();
                                    JSONArray array = new JSONArray(obj.getString("field"));
                                    for (int i = 0; i < array.length(); i += 2) {
                                        WindDto dto2 = new WindDto();
                                        dto2.initX = (float) (array.optDouble(i));
                                        dto2.initY = (float) (array.optDouble(i + 1));
                                        windDataT639.dataList.add(dto2);
                                    }
                                }

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        cancelDialog();
                                        reloadWind(false);
                                    }
                                });

                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    public void onCameraChange(CameraPosition arg0) {
        container.removeAllViews();
        container2.removeAllViews();
        tvFileTime.setVisibility(View.GONE);
    }

    @Override
    public void onCameraChangeFinish(CameraPosition arg0) {
        zoom = arg0.zoom;
        if (isGfs) {
            reloadWind(true);
        }else {
            reloadWind(false);
        }
    }

    long t = new Date().getTime();

    /**
     * 重新加载风场
     */
    private void reloadWind(boolean isGfs) {
        t = new Date().getTime() - t;
        if (t < 1000) {
            return;
        }

        LatLng latLngStart = aMap.getProjection().fromScreenLocation(new Point(0, 0));
        LatLng latLngEnd = aMap.getProjection().fromScreenLocation(new Point(width, height));
        Log.e("latLng", latLngStart.latitude+","+latLngStart.longitude+"\n"+latLngEnd.latitude+","+latLngEnd.longitude);
        if (isGfs) {
            windDataGFS.latLngStart = latLngStart;
            windDataGFS.latLngEnd = latLngEnd;
        }else {
            windDataT639.latLngStart = latLngStart;
            windDataT639.latLngEnd = latLngEnd;
        }
        if (waitWindView == null) {
            waitWindView = new WaitWindView2(mContext);
            waitWindView.init(WaitWindActivity.this);
            if (isGfs) {
                waitWindView.setData(windDataGFS, zoom);
            }else {
                waitWindView.setData(windDataT639, zoom);
            }
            waitWindView.start();
            waitWindView.invalidate();
        }else {
            if (isGfs) {
                waitWindView.setData(windDataGFS, zoom);
            }else {
                waitWindView.setData(windDataT639, zoom);
            }
        }

        container2.removeAllViews();
        container.removeAllViews();
        container.addView(waitWindView);
        tvFileTime.setVisibility(View.VISIBLE);
        String time;
        if (isGfs) {
            time = windDataGFS.filetime;
            if (!TextUtils.isEmpty(time)) {
                try {
                    tvFileTime.setText("GFS "+sdf3.format(sdf2.parse(time)) + "风场预报");
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }else {
            time = windDataT639.filetime;
            if (!TextUtils.isEmpty(time)) {
                try {
                    tvFileTime.setText("T639 "+sdf3.format(sdf2.parse(time)) + "风场预报");
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 隐藏或显示ListView的动画
     */
    public void hideOrShowListViewAnimator(final View view, final int startValue,final int endValue){
        //1.设置属性的初始值和结束值
        ValueAnimator mAnimator = ValueAnimator.ofInt(0,100);
        //2.为目标对象的属性变化设置监听器
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animatorValue = (Integer) animation.getAnimatedValue();
                float fraction = animatorValue/100f;
                IntEvaluator mEvaluator = new IntEvaluator();
                //3.使用IntEvaluator计算属性值并赋值给ListView的高
                view.getLayoutParams().height = mEvaluator.evaluate(fraction, startValue, endValue);
                view.requestLayout();
            }
        });
        //4.为ValueAnimator设置LinearInterpolator
        mAnimator.setInterpolator(new LinearInterpolator());
        //5.设置动画的持续时间
        mAnimator.setDuration(200);
        //6.为ValueAnimator设置目标对象并开始执行动画
        mAnimator.setTarget(view);
        mAnimator.start();
    }

    private void clickWindDetail() {
        int height = (int)CommonUtil.dip2px(mContext, 150);
        isShowDetail = !isShowDetail;
        if (isShowDetail) {
            ivArrow.setImageResource(R.drawable.shawn_icon_animation_up);
            hideOrShowListViewAnimator(llContainer1, 0, height);
            llContainer1.setVisibility(View.VISIBLE);
        }else {
            ivArrow.setImageResource(R.drawable.shawn_icon_animation_down);
            hideOrShowListViewAnimator(llContainer1, height, 0);
        }
    }

    /**
     * 隐藏或显示的动画
     */
    public void switchAnimation(final View view, final int startValue,final int endValue){
        //1.设置属性的初始值和结束值
        ValueAnimator mAnimator = ValueAnimator.ofInt(0,100);
        //2.为目标对象的属性变化设置监听器
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animatorValue = (Integer) animation.getAnimatedValue();
                float fraction = animatorValue/100f;
                IntEvaluator mEvaluator = new IntEvaluator();
                //3.使用IntEvaluator计算属性值并赋值给ListView的高
                view.getLayoutParams().width = mEvaluator.evaluate(fraction, startValue, endValue);
                view.requestLayout();
            }
        });
        //4.为ValueAnimator设置LinearInterpolator
        mAnimator.setInterpolator(new LinearInterpolator());
        //5.设置动画的持续时间
        mAnimator.setDuration(200);
        //6.为ValueAnimator设置目标对象并开始执行动画
        mAnimator.setTarget(view);
        mAnimator.start();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.llBack:
                finish();
                break;
            case R.id.ivSwitch:
                if (isGfs) {
                    ivSwitch.setImageResource(R.drawable.shawn_icon_switch_data_on);
                    windDataT639 = null;
                    OkHttpT639();
                }else {
                    ivSwitch.setImageResource(R.drawable.shawn_icon_switch_data_off);
                    windDataGFS = null;
                    OkHttpGFS();
                }
                isGfs = !isGfs;
                break;
            case R.id.ivLocation:
                if (zoom >= 12.f) {
                    ivLocation.setImageResource(R.drawable.icon_location_off);
                    zoom = 5.5f;
                    aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(locationLat, locationLng), zoom));
                }else {
                    ivLocation.setImageResource(R.drawable.icon_location_on);
                    zoom = 12.0f;
                    aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(locationLat, locationLng), zoom));
                }
                break;
            case R.id.ivHeight:
                isShowHeight = !isShowHeight;
                if (isShowHeight) {
                    switchAnimation(llHeight, swithWidth, 0);
                    ivHeight.setImageResource(R.drawable.shawn_icon_height_off);
                }else {
                    switchAnimation(llHeight, 0, swithWidth);
                    ivHeight.setImageResource(R.drawable.shawn_icon_height_on);
                    llHeight.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.tvHeight200:
                tvHeight200.setTextColor(getResources().getColor(R.color.blue));
                tvHeight500.setTextColor(getResources().getColor(R.color.text_color4));
                tvHeight1000.setTextColor(getResources().getColor(R.color.text_color4));
                dataHeight = "200";
                if (isGfs) {
                    windDataGFS = null;
                    OkHttpGFS();
                }else {
                    windDataT639 = null;
                    OkHttpT639();
                }
                break;
            case R.id.tvHeight500:
                tvHeight200.setTextColor(getResources().getColor(R.color.text_color4));
                tvHeight500.setTextColor(getResources().getColor(R.color.blue));
                tvHeight1000.setTextColor(getResources().getColor(R.color.text_color4));
                dataHeight = "500";
                if (isGfs) {
                    windDataGFS = null;
                    OkHttpGFS();
                }else {
                    windDataT639 = null;
                    OkHttpT639();
                }
                break;
            case R.id.tvHeight1000:
                tvHeight200.setTextColor(getResources().getColor(R.color.text_color4));
                tvHeight500.setTextColor(getResources().getColor(R.color.text_color4));
                tvHeight1000.setTextColor(getResources().getColor(R.color.blue));
                dataHeight = "1000";
                if (isGfs) {
                    windDataGFS = null;
                    OkHttpGFS();
                }else {
                    windDataT639 = null;
                    OkHttpT639();
                }
                break;
            case R.id.llDetail:
            case R.id.ivArrow:
                clickWindDetail();
                break;

            default:
                break;
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
    }

    /**
     * 获取当年的台风列表信息
     */
    private void OkHttpTyphoonList(int year) {
        final String url = "http://decision-admin.tianqi.cn/Home/other/gettyphoon/list/"+year;
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
                        final String requestResult = response.body().string();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!TextUtils.isEmpty(requestResult)) {
                                    String c = "(";
                                    String c2 = "})";
                                    String result = requestResult.substring(requestResult.indexOf(c)+c.length(), requestResult.indexOf(c2)+1);
                                    if (!TextUtils.isEmpty(result)) {
                                        try {
                                            JSONObject obj = new JSONObject(result);
                                            if (!obj.isNull("typhoonList")) {
                                                List<TyphoonDto> startList = new ArrayList<>();
                                                JSONArray array = obj.getJSONArray("typhoonList");
                                                for (int i = 0; i < array.length(); i++) {
                                                    JSONArray itemArray = array.getJSONArray(i);
                                                    TyphoonDto dto = new TyphoonDto();
                                                    dto.id = itemArray.getString(0);
                                                    dto.enName = itemArray.getString(1);
                                                    dto.name = itemArray.getString(2);
                                                    dto.code = itemArray.getString(4);
                                                    dto.status = itemArray.getString(7);

                                                    //把活跃台风过滤出来存放
                                                    if (TextUtils.equals(dto.status, "start")) {
                                                        startList.add(dto);
                                                    }
                                                }

                                                for (TyphoonDto dto : startList) {
                                                    String name;
                                                    if (TextUtils.equals(dto.enName, "nameless")) {
                                                        name = dto.code + " " + dto.enName;
                                                    }else {
                                                        name = dto.code + " " + dto.name + " " + dto.enName;
                                                    }
                                                    OkHttpTyphoonDetail(dto.id, name);
                                                }

                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                    }
                                }
                            }
                        });
                    }
                });
            }
        }).start();
    }

    /**
     * 获取台风详情
     */
    private void OkHttpTyphoonDetail(String typhoonId, final String name) {
        final String url = "http://decision-admin.tianqi.cn/Home/other/gettyphoon/view/"+typhoonId;
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
                        final String requestResult = response.body().string();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!TextUtils.isEmpty(requestResult)) {
                                    String c = "(";
                                    String result = requestResult.substring(requestResult.indexOf(c)+c.length(), requestResult.indexOf(")"));
                                    if (!TextUtils.isEmpty(result)) {
                                        try {
                                            JSONObject obj = new JSONObject(result);
                                            if (!obj.isNull("typhoon")) {
                                                JSONArray array = obj.getJSONArray("typhoon");
                                                JSONArray itemArray = array.getJSONArray(8);
                                                if (itemArray.length() > 0) {
                                                    JSONArray itemArray2 = itemArray.getJSONArray(itemArray.length()-1);
                                                    TyphoonDto dto = new TyphoonDto();
                                                    if (!TextUtils.isEmpty(name)) {
                                                        dto.name = name;
                                                    }
                                                    long longTime = itemArray2.getLong(2);
                                                    dto.time = sdf2.format(new Date(longTime));

                                                    dto.lng = itemArray2.getDouble(4);
                                                    dto.lat = itemArray2.getDouble(5);
                                                    dto.pressure = itemArray2.getString(6);
                                                    dto.max_wind_speed = itemArray2.getString(7);
                                                    dto.move_speed = itemArray2.getString(9);
                                                    String fx_string = itemArray2.getString(8);
                                                    if( !TextUtils.isEmpty(fx_string)){
                                                        String windDir = "";
                                                        for (int i = 0; i < fx_string.length(); i++) {
                                                            String item = fx_string.substring(i, i+1);
                                                            if (TextUtils.equals(item, "N")) {
                                                                item = "北";
                                                            }else if (TextUtils.equals(item, "S")) {
                                                                item = "南";
                                                            }else if (TextUtils.equals(item, "W")) {
                                                                item = "西";
                                                            }else if (TextUtils.equals(item, "E")) {
                                                                item = "东";
                                                            }
                                                            windDir = windDir+item;
                                                        }
                                                        dto.wind_dir = windDir;
                                                    }

                                                    String type = itemArray2.getString(3);
                                                    if (TextUtils.equals(type, "TD")) {//热带低压
                                                        type = "1";
                                                    }else if (TextUtils.equals(type, "TS")) {//热带风暴
                                                        type = "2";
                                                    }else if (TextUtils.equals(type, "STS")) {//强热带风暴
                                                        type = "3";
                                                    }else if (TextUtils.equals(type, "TY")) {//台风
                                                        type = "4";
                                                    }else if (TextUtils.equals(type, "STY")) {//强台风
                                                        type = "5";
                                                    }else if (TextUtils.equals(type, "SuperTY")) {//超强台风
                                                        type = "6";
                                                    }
                                                    dto.type = type;
                                                    dto.isFactPoint = true;

                                                    JSONArray array10 = itemArray2.getJSONArray(10);
                                                    for (int m = 0; m < array10.length(); m++) {
                                                        JSONArray itemArray10 = array10.getJSONArray(m);
                                                        if (m == 0) {
                                                            dto.radius_7 = itemArray10.getString(1);
                                                        }else if (m == 1) {
                                                            dto.radius_10 = itemArray10.getString(1);
                                                        }
                                                    }

                                                    MarkerOptions tOption = new MarkerOptions();
                                                    tOption.title(name+"|"+dto.content(mContext));
                                                    tOption.position(new LatLng(dto.lat, dto.lng));
                                                    tOption.anchor(0.5f, 0.5f);
                                                    tOption.icon(BitmapDescriptorFactory.fromAsset("typhoon/typhoon_icon1.png"));
//                                                    ArrayList<BitmapDescriptor> iconList = new ArrayList<>();
//                                                    for (int i = 1; i <= 9; i++) {
//                                                        iconList.add(BitmapDescriptorFactory.fromAsset("typhoon/typhoon_icon"+i+".png"));
//                                                    }
//                                                    tOption.icons(iconList);
//                                                    tOption.period(2);
                                                    aMap.addMarker(tOption);
                                                }
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        });
                    }
                });
            }
        }).start();
    }

}
