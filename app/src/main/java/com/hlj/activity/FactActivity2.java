package com.hlj.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.GroundOverlay;
import com.amap.api.maps.model.GroundOverlayOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.model.Text;
import com.hlj.adapter.BaseViewPagerAdapter;
import com.hlj.adapter.FactMonitorAdapter;
import com.hlj.adapter.FactTimeAdapter;
import com.hlj.common.CONST;
import com.hlj.common.ColumnData;
import com.hlj.dto.AgriDto;
import com.hlj.dto.FactDto;
import com.hlj.fragment.FactCheckFragment;
import com.hlj.utils.CommonUtil;
import com.hlj.utils.OkHttpUtil;
import com.hlj.view.MainViewPager;

import net.tsz.afinal.FinalBitmap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import shawn.cxwl.com.hlj.R;

/**
 * 实况资料
 */
public class FactActivity2 extends BaseFragmentActivity implements View.OnClickListener, AMap.OnCameraChangeListener {

    private Context mContext = null;
    private LinearLayout llBack = null;
    private TextView tvTitle = null;
    private LinearLayout llContainer,llContainer1;
    private int width = 0;
    private float density = 0;
    private MapView mapView = null;//高德地图
    private AMap aMap = null;//高德地图
    private float zoom = 5.5f;
    private ScrollView scrollView = null;
    private ListView listView = null;
    private FactMonitorAdapter factAdapter = null;
    private List<FactDto> factList = new ArrayList<>();
    private LinearLayout listTitle = null;
    private TextView tv1, tv2, tv3;
    private TextView tvLayerName, tvIntro, tvToast;//图层名称
    private ImageView ivChart = null;//图例
    private ProgressBar progressBar = null;
    private List<Polygon> polygons = new ArrayList<>();//图层数据
    private List<Text> texts = new ArrayList<>();//等值线数值
    private List<Polyline> polylines = new ArrayList<>();//广西边界市县边界线
    private List<Marker> cityTexts = new ArrayList<>();//市县名称
    private List<Marker> autoTexts = new ArrayList<>();//自动站
    private List<FactDto> cityInfos = new ArrayList<>();//城市信息
    private List<FactDto> timeList = new ArrayList<>();//时间列表
    private TextView tvDetail = null;
    private TextView tvHistory = null;
    private LinearLayout llBottom = null;
    private List<FactDto> realDatas = new ArrayList<>();//全省站点列表
    private String stationName = "", area = "", val = "", timeString = "";
    private String dataUrl = "", childId = "";
    private MainViewPager viewPager = null;
    private LinearLayout llViewPager = null;
    private ArrayList<Fragment> fragments = new ArrayList<>();
    private Map<String, String> layerMap = new HashMap<>();
    private GroundOverlay factOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fact2);
        mContext = this;
        initAmap(savedInstanceState);
        initWidget();
        initListView();
    }

    /**
     * 初始化高德地图
     */
    private void initAmap(Bundle bundle) {
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.setVisibility(View.VISIBLE);
        mapView.onCreate(bundle);
        if (aMap == null) {
            aMap = mapView.getMap();
        }

        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CONST.guizhouLatLng, zoom));
        aMap.getUiSettings().setMyLocationButtonEnabled(false);// 设置默认定位按钮是否显示
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.getUiSettings().setRotateGesturesEnabled(false);
        aMap.showMapText(false);
        aMap.setOnCameraChangeListener(this);

        TextView tvMapNumber = findViewById(R.id.tvMapNumber);
        tvMapNumber.setText(aMap.getMapContentApprovalNumber());

        aMap.setOnMapTouchListener(new AMap.OnMapTouchListener() {
            @Override
            public void onTouch(MotionEvent arg0) {
                if (scrollView != null) {
                    if (arg0.getAction() == MotionEvent.ACTION_UP) {
                        scrollView.requestDisallowInterceptTouchEvent(false);
                    }else {
                        scrollView.requestDisallowInterceptTouchEvent(true);
                    }
                }
            }
        });

        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(new LatLng(1, -179))
                .include(new LatLng(89, 179))
                .build();
        aMap.addGroundOverlay(new GroundOverlayOptions()
                .anchor(0.5f, 0.5f)
                .positionFromBounds(bounds)
                .image(BitmapDescriptorFactory.fromResource(R.drawable.empty))
                .transparency(0.0f));
        aMap.runOnDrawFrame();
    }

    private void initWidget() {
        llBack = (LinearLayout) findViewById(R.id.llBack);
        llBack.setOnClickListener(this);
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        llContainer = (LinearLayout) findViewById(R.id.llContainer);
        llContainer1 = (LinearLayout) findViewById(R.id.llContainer1);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        listTitle = (LinearLayout) findViewById(R.id.listTitle);
        tv1 = (TextView) findViewById(R.id.tv1);
        tv2 = (TextView) findViewById(R.id.tv2);
        tv3 = (TextView) findViewById(R.id.tv3);
        tvLayerName = (TextView) findViewById(R.id.tvLayerName);
        tvIntro = (TextView) findViewById(R.id.tvIntro);
        tvToast = (TextView) findViewById(R.id.tvToast);
        ivChart = (ImageView) findViewById(R.id.ivChart);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        tvDetail = (TextView) findViewById(R.id.tvDetail);
        tvDetail.setOnClickListener(this);
        tvHistory = (TextView) findViewById(R.id.tvHistory);
        tvHistory.setOnClickListener(this);
        llBottom = (LinearLayout) findViewById(R.id.llBottom);
        llViewPager = (LinearLayout) findViewById(R.id.llViewPager);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        width = dm.widthPixels;
        density = dm.density;

        AgriDto data = getIntent().getExtras().getParcelable("data");
        if (!TextUtils.isEmpty(data.name)) {
            tvTitle.setText(data.name);
        }

        OkHttpLayer(data);

        String columnId = getIntent().getStringExtra(CONST.COLUMN_ID);
        CommonUtil.submitClickCount(columnId, data.name);
    }

    private void initListView() {
        listView = (ListView) findViewById(R.id.listView);
        factAdapter = new FactMonitorAdapter(mContext, factList);
        listView.setAdapter(factAdapter);
    }

    private void OkHttpLayer(final AgriDto data) {
        final String url = "https://decision-admin.tianqi.cn/Home/work2019/getHljSKImages";
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
                                layerMap.clear();
                                if (!TextUtils.isEmpty(result)) {
                                    try {
                                        JSONObject obj = new JSONObject(result);
                                        Iterator<String> iterator = obj.keys();
                                        while (iterator.hasNext() ) {
                                            String key = iterator.next();
                                            String value = obj.getString(key);
                                            layerMap.put(key, value);
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }

                                if (layerMap.size() > 0) {
                                    addColumn(data);
                                }
                            }
                        });
                    }
                });
            }
        }).start();
    }

    /**
     * 添加子栏目
     * @param data
     */
    private void addColumn(AgriDto data) {
        llContainer.removeAllViews();
        llContainer1.removeAllViews();
        int size = data.child.size();
        if (size <= 0) {
            return;
        }
        for (int i = 0; i < size; i++) {
            ColumnData itemDto = data.child.get(i);
            TextView tvName = new TextView(mContext);
            tvName.setGravity(Gravity.CENTER);
            tvName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            tvName.setPadding(0, (int)(density*10), 0, (int)(density*10));
            tvName.setMaxLines(1);

            TextView tvBar = new TextView(mContext);
            tvBar.setGravity(Gravity.CENTER);
            tvBar.setPadding((int)(density*10), 0, (int)(density*10), 0);

            if (!TextUtils.isEmpty(itemDto.name)) {
                tvName.setText(itemDto.name);
                tvName.setTag(itemDto.dataUrl+","+itemDto.id);
            }
            if (i == 0) {
                childId = itemDto.id;
                //1154降水任意时段查询
                if (TextUtils.equals(itemDto.id, "1154")) {
                    llViewPager.setVisibility(View.VISIBLE);
                    scrollView.setVisibility(View.GONE);
                    initViewPager();
                }else {
                    llViewPager.setVisibility(View.GONE);
                    scrollView.setVisibility(View.VISIBLE);
                    dataUrl = itemDto.dataUrl;
                    OkHttpFact(itemDto.dataUrl, false, "");
                }
                tvName.setTextColor(getResources().getColor(R.color.colorPrimary));
                tvBar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            }else {
                tvName.setTextColor(getResources().getColor(R.color.text_color3));
                tvBar.setBackgroundColor(getResources().getColor(R.color.transparent));
            }
            llContainer.addView(tvName);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tvName.getLayoutParams();
            if (size <= 4) {
                if (size == 2) {
                    params.width = width/2;
                }else if (size == 3) {
                    params.width = width/3;
                }else {
                    params.width = width/4;
                }
            }else {
                params.setMargins((int)(density*10), 0, (int)(density*10), 0);
            }
            tvName.setLayoutParams(params);

            int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            tvName.measure(w, h);
            llContainer1.addView(tvBar);
            LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams) tvBar.getLayoutParams();
            if (size <= 4) {
                if (size == 2) {
                    params1.width = width/2;
                }else if (size == 3) {
                    params1.width = width/3;
                }else {
                    params1.width = width/4;
                }
            }else {
                params1.setMargins((int)(density*10), 0, (int)(density*10), 0);
                params1.width = tvName.getMeasuredWidth();
            }
            params1.height = (int) (density*2);
            params1.gravity = Gravity.CENTER;
            tvBar.setLayoutParams(params1);

            tvName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (llContainer != null) {
                        for (int i = 0; i < llContainer.getChildCount(); i++) {
                            TextView tvName = (TextView) llContainer.getChildAt(i);
                            TextView tvBar = (TextView) llContainer1.getChildAt(i);
                            if (TextUtils.equals((String) arg0.getTag(), (String) tvName.getTag())) {
                                tvName.setTextColor(getResources().getColor(R.color.title_bg));
                                tvBar.setBackgroundColor(getResources().getColor(R.color.title_bg));
                                String[] tags = ((String) arg0.getTag()).split(",");
                                childId = tags[1];
                                //1154降水任意时段查询
                                if (TextUtils.equals(tags[1], "1154")) {
                                    llViewPager.setVisibility(View.VISIBLE);
                                    scrollView.setVisibility(View.GONE);
                                    initViewPager();
                                }else {
                                    llViewPager.setVisibility(View.GONE);
                                    scrollView.setVisibility(View.VISIBLE);
                                    dataUrl = tags[0];
                                    OkHttpFact(tags[0], false, "");
                                }
                            }else {
                                tvName.setTextColor(getResources().getColor(R.color.text_color4));
                                tvBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                            }
                        }
                    }
                }
            });

        }
    }

    /**
     * 获取实况信息
     * @param url
     */
    private void OkHttpFact(final String url, final boolean isHistory, final String timeParams) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
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

                                        if (!obj.isNull("zh")) {
                                            JSONObject itemObj = obj.getJSONObject("zh");
                                            if (!itemObj.isNull("stationName")) {
                                                tv3.setText(itemObj.getString("stationName"));
                                            }
                                            if (!itemObj.isNull("area")) {
                                                tv2.setText(itemObj.getString("area"));
                                            }
                                            if (!itemObj.isNull("val")) {
                                                tv1.setText(itemObj.getString("val"));
                                            }
                                        }

                                        if (!obj.isNull("title")) {
                                            tvLayerName.setText(obj.getString("title"));
                                            tvLayerName.setVisibility(View.VISIBLE);
                                        }

                                        if (!obj.isNull("cutlineUrl")) {
                                            FinalBitmap finalBitmap = FinalBitmap.create(mContext);
                                            finalBitmap.display(ivChart, obj.getString("cutlineUrl"), null, 0);
                                        }

                                        if (!obj.isNull("zx")) {
                                            tvIntro.setText(obj.getString("zx"));
                                        }

                                        if (!obj.isNull("t")) {
                                            cityInfos.clear();
                                            JSONArray array = obj.getJSONArray("t");
                                            for (int i = 0; i < array.length(); i++) {
                                                FactDto f = new FactDto();
                                                JSONObject o = array.getJSONObject(i);
                                                if (!o.isNull("name")) {
                                                    f.name = o.getString("name");
                                                    f.stationName = o.getString("name");
                                                }
                                                if (!o.isNull("lon")) {
                                                    f.lng = Double.parseDouble(o.getString("lon"));
                                                }
                                                if (!o.isNull("lat")) {
                                                    f.lat = Double.parseDouble(o.getString("lat"));
                                                }
                                                if (!o.isNull("value")) {
                                                    f.val = Double.parseDouble(o.getString("value"));
                                                }
                                                if (!o.isNull("val1")) {
                                                    f.val1 = Double.parseDouble(o.getString("val1"));
                                                }
                                                cityInfos.add(f);
                                            }
                                        }
                                        OkHttpLayer(isHistory, timeParams);

                                        //详情开始
                                        if (!obj.isNull("th")) {
                                            JSONObject itemObj = obj.getJSONObject("th");
                                            if (!itemObj.isNull("stationName")) {
                                                stationName = itemObj.getString("stationName");
                                            }
                                            if (!itemObj.isNull("area")) {
                                                area = itemObj.getString("area");
                                            }
                                            if (!itemObj.isNull("val")) {
                                                val = itemObj.getString("val");
                                            }
                                        }

                                        if (!obj.isNull("times")) {
                                            timeList.clear();
                                            JSONArray timeArray = obj.getJSONArray("times");
                                            for (int i = 0; i < timeArray.length(); i++) {
                                                FactDto f = new FactDto();
                                                JSONObject fo = timeArray.getJSONObject(i);
                                                if (!fo.isNull("timeString")) {
                                                    f.timeString = fo.getString("timeString");
                                                }
                                                if (!fo.isNull("timestart")) {
                                                    f.timeStart = fo.getString("timestart");
                                                }
                                                if (!fo.isNull("timeParams")) {
                                                    f.timeParams = fo.getString("timeParams");
                                                }
                                                timeList.add(f);

                                                if (i == 0) {
                                                    timeString = f.timeString;
                                                }
                                            }
                                        }

                                        realDatas.clear();
                                        if (!obj.isNull("realDatas")) {
                                            JSONArray array = new JSONArray(obj.getString("realDatas"));
                                            for (int i = 0; i < array.length(); i++) {
                                                JSONObject itemObj = array.getJSONObject(i);
                                                FactDto dto = new FactDto();
                                                if (!itemObj.isNull("stationCode")) {
                                                    dto.stationCode = itemObj.getString("stationCode");
                                                }
                                                if (!itemObj.isNull("stationName")) {
                                                    dto.stationName = itemObj.getString("stationName");
                                                }
                                                if (!itemObj.isNull("area")) {
                                                    dto.area = itemObj.getString("area");
                                                }
                                                if (!itemObj.isNull("area1")) {
                                                    dto.area1 = itemObj.getString("area1");
                                                }
                                                if (!itemObj.isNull("val")) {
                                                    dto.val = itemObj.getDouble("val");
                                                }
                                                if (!itemObj.isNull("val1")) {
                                                    dto.val1 = itemObj.getDouble("val1");
                                                }
                                                if (!itemObj.isNull("Lon")) {
                                                    dto.lng = Double.parseDouble(itemObj.getString("Lon"));
                                                }
                                                if (!itemObj.isNull("Lat")) {
                                                    dto.lat = Double.parseDouble(itemObj.getString("Lat"));
                                                }

                                                if (!TextUtils.isEmpty(dto.stationName) && !TextUtils.isEmpty(dto.area)) {
                                                    realDatas.add(dto);
                                                }
                                            }
                                        }
                                        //详情结束

                                        if (!obj.isNull("jb")) {
                                            factList.clear();
                                            JSONArray array = obj.getJSONArray("jb");
                                            for (int i = 0; i < array.length(); i++) {
                                                JSONObject itemObj = array.getJSONObject(i);
                                                FactDto data = new FactDto();
                                                if (!itemObj.isNull("lv")) {
                                                    data.rainLevel = itemObj.getString("lv");
                                                }
                                                if (!itemObj.isNull("count")) {
                                                    data.count = itemObj.getInt("count")+"";
                                                }
                                                if (!itemObj.isNull("xs")) {
                                                    JSONArray xsArray = itemObj.getJSONArray("xs");
                                                    List<FactDto> list = new ArrayList<>();
                                                    list.clear();
                                                    for (int j = 0; j < xsArray.length(); j++) {
                                                        FactDto d = new FactDto();
                                                        d.area = xsArray.getString(j);
                                                        list.add(d);
                                                    }
                                                    data.areaList.addAll(list);
                                                }
                                                factList.add(data);
                                            }
                                            if (factList.size() > 0 && factAdapter != null) {
                                                CommonUtil.setListViewHeightBasedOnChildren(listView);
                                                factAdapter.timeString = timeString;
                                                factAdapter.stationName = stationName;
                                                factAdapter.area = area;
                                                factAdapter.val = val;
                                                factAdapter.realDatas.clear();
                                                factAdapter.realDatas.addAll(realDatas);
                                                factAdapter.notifyDataSetChanged();
                                                tvIntro.setVisibility(View.VISIBLE);
                                                tvIntro.setBackgroundResource(R.drawable.bg_corner_black);
                                                listTitle.setVisibility(View.VISIBLE);
                                                listView.setVisibility(View.VISIBLE);
                                                llBottom.setVisibility(View.VISIBLE);
                                            }
                                        }else {
                                            tvIntro.setVisibility(View.GONE);
                                            listTitle.setVisibility(View.GONE);
                                            listView.setVisibility(View.GONE);
                                            llBottom.setVisibility(View.GONE);
                                        }

                                        tvLayerName.setFocusable(true);
                                        tvLayerName.setFocusableInTouchMode(true);
                                        tvLayerName.requestFocus();

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }else {
                                    removePolygons();
                                    progressBar.setVisibility(View.GONE);
                                    tvToast.setVisibility(View.VISIBLE);
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            tvToast.setVisibility(View.GONE);
                                        }
                                    }, 1000);
                                }
                            }
                        });
                    }
                });
            }
        }).start();
    }

    private void OkHttpLayer(final boolean isHistory, final String timeParams) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (layerMap.containsKey(childId)) {
                    String value = layerMap.get(childId);
                    if (!TextUtils.isEmpty(value)) {
                        try {
                            JSONObject obj = new JSONObject(value);
                            final double maxlat = obj.getDouble("maxlat");
                            final double maxlon = obj.getDouble("maxlon");
                            final double minlat = obj.getDouble("minlat");
                            final double minlon = obj.getDouble("minlon");
                            String imgurl = obj.getString("imgurl");
                            if (isHistory) {
                                imgurl = String.format("https://app.tianqi.cn/tile_img/weatherLayerDatas/out/hlj/%s/%s.png", childId, timeParams);
                                Log.e("imgUrl", imgurl);
                            }
                            OkHttpUtil.enqueue(new Request.Builder().url(imgurl).build(), new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                }
                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    if (!response.isSuccessful()) {
                                        return;
                                    }
                                    final byte[] bytes = response.body().bytes();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                            if (bitmap != null) {
                                                drawFactBitmap(bitmap, new LatLng(maxlat, maxlon), new LatLng(minlat, minlon));
                                            }
                                        }
                                    });
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    /**
     * 绘制实况图
     */
    private void drawFactBitmap(Bitmap bitmap, LatLng max, LatLng min) {
        if (bitmap == null) {
            return;
        }
        BitmapDescriptor fromView = BitmapDescriptorFactory.fromBitmap(bitmap);
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(max)
                .include(min)
                .build();

//        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

        if (factOverlay == null) {
            factOverlay = aMap.addGroundOverlay(new GroundOverlayOptions()
                    .anchor(0.5f, 0.5f)
                    .positionFromBounds(bounds)
                    .image(fromView)
                    .transparency(0.2f));
        } else {
            factOverlay.setImage(null);
            factOverlay.setPositionFromBounds(bounds);
            factOverlay.setImage(fromView);
        }

        drawDataToMap();
    }

    private void removeTexts() {
        for (int i = 0; i < texts.size(); i++) {
            texts.get(i).remove();
        }
        texts.clear();
    }

    /**
     * 清除图层
     */
    private void removePolygons() {
        for (int i = 0; i < polygons.size(); i++) {
            polygons.get(i).remove();
        }
        polygons.clear();
    }

    /**
     * 清除边界线
     */
    private void removePolylines() {
        for (int i = 0; i < polylines.size(); i++) {
            polylines.get(i).remove();
        }
        polylines.clear();
    }

    /**
     * 清除市县名称
     */
    private void removeCityTexts() {
        for (int i = 0; i < cityTexts.size(); i++) {
            cityTexts.get(i).remove();
        }
        cityTexts.clear();
    }

    /**
     * 清除自动站
     */
    private void removeAutoTexts() {
        for (int i = 0; i < autoTexts.size(); i++) {
            autoTexts.get(i).remove();
        }
        autoTexts.clear();
    }

    /**
     * 绘制图层
     */
    private void drawDataToMap() {
        if (aMap == null) {
            return;
        }
        removeTexts();
        removePolygons();
        removePolylines();
        removeCityTexts();
        removeAutoTexts();

        drawAllDistrict();
        progressBar.setVisibility(View.GONE);
    }

    /**
     * 绘制广西市县边界
     */
    private void drawAllDistrict() {
        if (aMap == null) {
            return;
        }
        String result = CommonUtil.getFromAssets(mContext, "heilongjiang.json");
        if (!TextUtils.isEmpty(result)) {
            try {
                JSONObject obj = new JSONObject(result);
                JSONArray array = obj.getJSONArray("features");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject itemObj = array.getJSONObject(i);

//					JSONObject properties = itemObj.getJSONObject("properties");
//                    String value = "数值";
//					String name = properties.getString("name");
//                    if (name.contains("市")) {
//                        name = name.replace("市", "");
//                    }
//                    JSONArray cp = properties.getJSONArray("cp");
//                    for (int m = 0; m < cp.length(); m++) {
//                        double lat = cp.getDouble(1);
//                        double lng = cp.getDouble(0);
////                        TextOptions options = new TextOptions();
////                        options.position(new LatLng(lat, lng));
////                        options.fontColor(Color.BLACK);
////                        options.fontSize(20);
////                        options.text(name);
////                        options.backgroundColor(Color.TRANSPARENT);
////                        Text text = aMap.addText(options);
////                        cityTexts.add(text);
//
//                        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//                        View view = inflater.inflate(R.layout.layout_fact_value, null);
//                        TextView tvValue = (TextView) view.findViewById(R.id.tvValue);
//                        TextView tvName = (TextView) view.findViewById(R.id.tvName);
//                        if (!TextUtils.isEmpty(value)) {
//                            tvValue.setText(value);
//                        }
//                        if (!TextUtils.isEmpty(name)) {
//                            tvName.setText(name);
//                        }
//                        MarkerOptions options = new MarkerOptions();
//                        options.anchor(0.5f, 0.5f);
//                        options.position(new LatLng(lat, lng));
//                        options.icon(BitmapDescriptorFactory.fromView(view));
//                        Marker marker = aMap.addMarker(options);
//                        cityTexts.add(marker);
//                    }

                    JSONObject geometry = itemObj.getJSONObject("geometry");
                    JSONArray coordinates = geometry.getJSONArray("coordinates");
                    for (int m = 0; m < coordinates.length(); m++) {
                        JSONArray array2 = coordinates.getJSONArray(m);
                        PolylineOptions polylineOption = new PolylineOptions();
                        polylineOption.width(3).color(0xffd9d9d9);
                        for (int j = 0; j < array2.length(); j++) {
                            JSONArray itemArray = array2.getJSONArray(j);
                            double lng = itemArray.getDouble(0);
                            double lat = itemArray.getDouble(1);
                            polylineOption.add(new LatLng(lat, lng));
                        }
                        Polyline polyLine = aMap.addPolyline(polylineOption);
                        polylines.add(polyLine);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        handler.removeMessages(1001);
        Message msg = handler.obtainMessage();
        msg.what = 1001;
        msg.obj = zoom;
        handler.sendMessageDelayed(msg, 1000);

    }

    @Override
    public void onCameraChange(CameraPosition arg0) {
    }

    @Override
    public void onCameraChangeFinish(CameraPosition arg0) {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        Point leftPoint = new Point(0, dm.heightPixels);
        Point rightPoint = new Point(dm.widthPixels, 0);
        LatLng leftlatlng = aMap.getProjection().fromScreenLocation(leftPoint);
        LatLng rightLatlng = aMap.getProjection().fromScreenLocation(rightPoint);

//        if (leftlatlng.latitude <= 3.9079 || rightLatlng.latitude >= 57.9079 || leftlatlng.longitude <= 71.9282
//                || rightLatlng.longitude >= 160 || arg0.zoom < 5.0f) {
//            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(CONST.guizhouLatLng, 5.5f));
//        }

        if ((arg0.zoom <= 8.5f && isPersonStation) || (arg0.zoom > 8.5f && !isPersonStation)) {
            zoom = arg0.zoom;
        }else {
            zoom = arg0.zoom;

            handler.removeMessages(1001);
            Message msg = handler.obtainMessage();
            msg.what = 1001;
            msg.obj = arg0.zoom;
            handler.sendMessageDelayed(msg, 1000);
        }
    }

    private boolean isPersonStation = true;
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    float msgZoom = (Float)msg.obj;
                    removeCityTexts();
                    removeAutoTexts();
                    if (msgZoom <= 8.5f) {
                        //绘制人工站
                        isPersonStation = true;
                        for (int i = 0; i < cityInfos.size(); i++) {
                            FactDto dto = cityInfos.get(i);
                            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            View view = inflater.inflate(R.layout.layout_fact_value, null);
                            TextView tvValue = (TextView) view.findViewById(R.id.tvValue);
                            TextView tvName = (TextView) view.findViewById(R.id.tvName);
                            ImageView ivWind = (ImageView) view.findViewById(R.id.ivWind);

                            if (dto.val >= 99999) {
                                tvValue.setText("");
                            }else {
                                tvValue.setText(dto.val+"");

                                if (dto.val1 != -1) {
                                    Bitmap b = CommonUtil.getWindMarker(mContext, dto.val);
                                    if (b != null) {
                                        Matrix matrix = new Matrix();
                                        matrix.postScale(1, 1);
                                        matrix.postRotate((float)dto.val1);
                                        Bitmap bitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                                        if (bitmap != null) {
                                            ivWind.setImageBitmap(bitmap);
                                            ivWind.setVisibility(View.VISIBLE);
                                        }
                                    }
                                }

                                if (!TextUtils.isEmpty(dto.stationName)) {
                                    tvName.setText(dto.stationName);
                                }
                                MarkerOptions options = new MarkerOptions();
                                options.title(dto.stationName);
                                options.anchor(0.5f, 0.5f);
                                options.position(new LatLng(dto.lat, dto.lng));
                                options.icon(BitmapDescriptorFactory.fromView(view));
                                Marker marker = aMap.addMarker(options);
                                marker.setVisible(true);
                                cityTexts.add(marker);
                            }
                        }
                    } else {//乡镇站点
                        isPersonStation = false;
                        for (int i = 0; i < realDatas.size(); i++) {
                            FactDto dto = realDatas.get(i);
                            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            View view = inflater.inflate(R.layout.layout_fact_value, null);
                            TextView tvValue = (TextView) view.findViewById(R.id.tvValue);
                            TextView tvName = (TextView) view.findViewById(R.id.tvName);
                            ImageView ivWind = (ImageView) view.findViewById(R.id.ivWind);

                            if (dto.val >= 99999) {
                                tvValue.setText("");
                            }else {
                                tvValue.setText(dto.val+"");

                                if (dto.val1 != -1) {
                                    Bitmap b = CommonUtil.getWindMarker(mContext, dto.val);
                                    if (b != null) {
                                        Matrix matrix = new Matrix();
                                        matrix.postScale(1, 1);
                                        matrix.postRotate((float)dto.val1);
                                        Bitmap bitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                                        if (bitmap != null) {
                                            ivWind.setImageBitmap(bitmap);
                                            ivWind.setVisibility(View.VISIBLE);
                                        }
                                    }
                                }

                                if (!TextUtils.isEmpty(dto.stationName)) {
                                    tvName.setText(dto.stationName);
                                }
                                MarkerOptions options = new MarkerOptions();
                                options.title(dto.stationName);
                                options.anchor(0.5f, 0.5f);
                                options.position(new LatLng(dto.lat, dto.lng));
                                options.icon(BitmapDescriptorFactory.fromView(view));
                                Marker marker = aMap.addMarker(options);
                                marker.setVisible(true);
                                autoTexts.add(marker);
                            }
                        }
                    }
                    break;
            }
        }
    };

    /**
     * 初始化viewPager
     */
    private void initViewPager() {
        if (viewPager != null) {
            viewPager.removeAllViewsInLayout();
            fragments.clear();
        }

        Bundle bundle = new Bundle();
        bundle.putString("childId", childId);
        Fragment fragment1 = new FactCheckFragment();
        fragment1.setArguments(bundle);
        fragments.add(fragment1);

        if (viewPager == null) {
            viewPager = (MainViewPager) findViewById(R.id.viewPager);
            viewPager.setSlipping(true);//设置ViewPager是否可以滑动
            viewPager.setOffscreenPageLimit(fragments.size());
        }
        viewPager.setAdapter(new BaseViewPagerAdapter(getSupportFragmentManager(), fragments));
    }

    private void dialogHistory() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_fact_history, null);
        TextView tvNegative = (TextView) view.findViewById(R.id.tvNegative);
        ListView listView = (ListView) view.findViewById(R.id.listView);
        FactTimeAdapter mAdapter = new FactTimeAdapter(mContext, timeList);
        listView.setAdapter(mAdapter);

        final Dialog dialog = new Dialog(mContext, R.style.CustomProgressDialog);
        dialog.setContentView(view);
        dialog.show();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                dialog.dismiss();
                FactDto dto = timeList.get(arg2);
                OkHttpFact(dataUrl+dto.timeParams, true, dto.timeParams);
            }
        });

        tvNegative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                dialog.dismiss();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.llBack:
                finish();
                break;
            case R.id.tvDetail:
                Intent intent = new Intent(mContext, FactDetailActivity.class);
                intent.putExtra("title", "详情数据");
                intent.putExtra("timeString", timeString);
                intent.putExtra("stationName", stationName);
                intent.putExtra("area", area);
                intent.putExtra("val", val);
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList("realDatas", (ArrayList<? extends Parcelable>) realDatas);
                intent.putExtras(bundle);
                mContext.startActivity(intent);
                break;
            case R.id.tvHistory:
                dialogHistory();
                break;
        }
    }
}
