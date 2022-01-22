package com.hlj.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.InfoWindowAdapter;
import com.amap.api.maps.AMap.OnCameraChangeListener;
import com.amap.api.maps.AMap.OnMapClickListener;
import com.amap.api.maps.AMap.OnMarkerClickListener;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.animation.ScaleAnimation;
import com.hlj.common.CONST;
import com.hlj.dto.CityDto;
import com.hlj.utils.CommonUtil;
import com.hlj.utils.OkHttpUtil;
import com.hlj.utils.SecretUrlUtil;
import com.hlj.utils.WeatherUtil;
import com.hlj.view.ExpandableTextView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.com.weather.api.WeatherAPI;
import cn.com.weather.beans.Weather;
import cn.com.weather.constants.Constants.Language;
import cn.com.weather.listener.AsyncResponseHandler;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import shawn.cxwl.com.hlj.R;

/**
 * 全省预报
 */
public class ProvinceForecastActivity extends BaseActivity implements OnClickListener, OnMarkerClickListener, OnMapClickListener, InfoWindowAdapter, OnCameraChangeListener {

    private boolean isBigText = false;
    private Context mContext = null;
    private LinearLayout llBack = null;
    private TextView tvControl = null;
    private MapView mapView = null;//高德地图
    private AMap aMap = null;//高德地图
    private float zoom = 5.5f;
    private Marker selectMarker = null;
    private SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMdd");
    private SimpleDateFormat sdf2 = new SimpleDateFormat("MM月dd日");
    private SimpleDateFormat sdf3 = new SimpleDateFormat("HH");
    private ExpandableTextView llContainer = null;

    private List<CityDto> cityList = new ArrayList<>();//市级
    private List<CityDto> districtList = new ArrayList<>();//县级
    private List<Marker> cityMarkers = new ArrayList<>();
    private List<Marker> disMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_province_forecast);
        mContext = this;
        showDialog();
        initAmap(savedInstanceState);
        initWidget();
    }

    /**
     * 初始化控件
     */
    private void initWidget() {
        llBack = (LinearLayout) findViewById(R.id.llBack);
        llBack.setOnClickListener(this);
        TextView tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvControl = findViewById(R.id.tvControl);
        tvControl.setText("放大字体");
        tvControl.setVisibility(View.VISIBLE);
        tvControl.setOnClickListener(this);
        llContainer = (ExpandableTextView) findViewById(R.id.llContainer);

        String title = getIntent().getStringExtra(CONST.ACTIVITY_NAME);
        if (title != null) {
            tvTitle.setText(title);
        }

        OkHttpText();
        OkHttpRank();//获取站点信息
    }

    /**
     * 初始化高德地图
     */
    private void initAmap(Bundle bundle) {
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(bundle);
        if (aMap == null) {
            aMap = mapView.getMap();
        }

        LatLng guizhouLatLng = new LatLng(49.302915,128.121040);
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(guizhouLatLng, zoom));
        aMap.getUiSettings().setMyLocationButtonEnabled(false);// 设置默认定位按钮是否显示
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.getUiSettings().setRotateGesturesEnabled(false);
        aMap.setOnMarkerClickListener(this);
        aMap.setOnMapClickListener(this);
        aMap.setInfoWindowAdapter(this);
        aMap.setOnCameraChangeListener(this);
        aMap.setOnMapLoadedListener(new AMap.OnMapLoadedListener() {
            @Override
            public void onMapLoaded() {
                TextView tvMapNumber = findViewById(R.id.tvMapNumber);
                tvMapNumber.setText(aMap.getMapContentApprovalNumber());
                CommonUtil.drawHLJJson(mContext, aMap);
            }
        });
    }

    /**
     * 异步请求
     */
    private void OkHttpText() {
        final String url = getIntent().getStringExtra(CONST.WEB_URL);
        if (TextUtils.isEmpty(url)) {
            return;
        }
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
                        if (!TextUtils.isEmpty(result)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        JSONObject obj = new JSONObject(result);
                                        String c1 = obj.getString("c1");
                                        c1 = c1.replace("<p>", "");
                                        c1 = c1.replace("</p>", "")+"    ";
                                        String c2 = obj.getString("c2");
                                        c2 = c2.replace("<p>", "");
                                        c2 = c2.replace("</p>", "")+"\n";
                                        String c3 = obj.getString("c3");
                                        c3 = c3.replace("<p>", "");
                                        c3 = c3.replace("</p>", "").trim()+"\n";
                                        String content = "发布单位：黑龙江省气象台\n"+c1+c3+c2;
                                        llContainer.setText(content);
                                        llContainer.setVisibility(View.VISIBLE);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * 获取空气质量排行
     */
    private void OkHttpRank() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpUtil.enqueue(new Request.Builder().url(SecretUrlUtil.airpollution()).build(), new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                    }
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            return;
                        }
                        String result = response.body().string();
                        if (result != null) {
                            cityList.clear();
                            parseStationInfo(result, "level1", cityList);
                            parseStationInfo(result, "level2", cityList);
                            districtList.clear();
                            parseStationInfo(result, "level3", districtList);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    cancelDialog();
                                    removeMarkers();

                                    for (int i = 0; i < cityList.size(); i++) {
                                        CityDto dto = cityList.get(i);
                                        getWeatherInfos(dto, cityMarkers, true);
                                    }

                                    for (int i = 0; i < districtList.size(); i++) {
                                        CityDto dto = districtList.get(i);
                                        getWeatherInfos(dto, disMarkers, false);
                                    }
                                }
                            });
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * 解析数据
     */
    private void parseStationInfo(String result, String level, List<CityDto> list) {
        try {
            JSONObject obj = new JSONObject(result);
            if (!obj.isNull("data")) {
                JSONObject dataObj = obj.getJSONObject("data");
                if (!dataObj.isNull(level)) {
                    JSONArray array = new JSONArray(dataObj.getString(level));
                    for (int i = 0; i < array.length(); i++) {
                        CityDto dto = new CityDto();
                        JSONObject itemObj = array.getJSONObject(i);
                        if (!itemObj.isNull("name")) {
                            dto.areaName = itemObj.getString("name");
                        }
                        if (!itemObj.isNull("level")) {
                            dto.level = itemObj.getString("level");
                        }
                        if (!itemObj.isNull("areaid")) {
                            dto.areaId = itemObj.getString("areaid");
                        }
                        if (!itemObj.isNull("lat")) {
                            dto.lat = Double.valueOf(itemObj.getString("lat"));
                        }
                        if (!itemObj.isNull("lon")) {
                            dto.lng = Double.valueOf(itemObj.getString("lon"));
                        }
                        if (dto.areaId.startsWith("10105")) {
                            list.add(dto);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取多个城市天气信息
     */
    private void getWeatherInfos(final CityDto dto, final List<Marker> markers, final boolean isVisible) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String url = String.format("https://hfapi.tianqi.cn/getweatherdata.php?area=%s&type=forecast&key=AErLsfoKBVCsU8hs", dto.areaId);
                OkHttpUtil.enqueue(new Request.Builder().url(url).build(), new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    }
                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
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

                                        //获取明天预报信息
                                        if (!obj.isNull("forecast")) {
                                            JSONObject forecast = obj.getJSONObject("forecast");

                                            //15天预报信息
                                            if (!forecast.isNull("24h")) {
                                                JSONObject object = forecast.getJSONObject("24h");
                                                if (!object.isNull(dto.areaId)) {
                                                    JSONObject object1 = object.getJSONObject(dto.areaId);
                                                    if (!object1.isNull("1001001")) {
                                                        JSONArray f1 = object1.getJSONArray("1001001");

                                                        //预报内容
                                                        JSONObject weeklyObj = f1.getJSONObject(1);

                                                        //晚上
                                                        String two = weeklyObj.getString("002");
                                                        if (!TextUtils.isEmpty(two) && !TextUtils.equals(two, "?") && !TextUtils.equals(two, "null")) {
                                                            dto.lowPheCode = Integer.valueOf(two);
                                                        }
                                                        String four = weeklyObj.getString("004");
                                                        if (!TextUtils.isEmpty(two) && !TextUtils.equals(two, "?") && !TextUtils.equals(two, "null")) {
                                                            dto.lowTemp = four;
                                                        }

                                                        //白天
                                                        String one = weeklyObj.getString("001");
                                                        if (!TextUtils.isEmpty(one) && !TextUtils.equals(one, "?") && !TextUtils.equals(one, "null")) {
                                                            dto.highPheCode = Integer.valueOf(one);
                                                        }
                                                        String three = weeklyObj.getString("003");
                                                        if (!TextUtils.isEmpty(three) && !TextUtils.equals(three, "?") && !TextUtils.equals(three, "null")) {
                                                            dto.highTemp = three;
                                                        }

                                                        addMarker(dto, markers, isVisible);
                                                    }
                                                }
                                            }
                                        }

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    }
                });
            }
        }).start();
    }

    private void removeMarkers() {
        for (int i = 0; i < cityMarkers.size(); i++) {
            Marker m = cityMarkers.get(i);
            markerColloseAnimation(m);
            m.remove();
        }
        cityMarkers.clear();

        for (int i = 0; i < disMarkers.size(); i++) {
            Marker m = disMarkers.get(i);
            markerColloseAnimation(m);
            m.remove();
        }
        disMarkers.clear();
    }

    private void addMarker(CityDto dto, List<Marker> markers, boolean isVisible) {
        MarkerOptions options = new MarkerOptions();
        options.title(dto.areaId);
        options.snippet(dto.areaName);
        options.anchor(0.5f, 0.5f);
        options.position(new LatLng(dto.lat, dto.lng));
        options.icon(BitmapDescriptorFactory.fromView(getTextBitmap1(dto)));
        Marker marker = aMap.addMarker(options);
        if (marker != null) {
            marker.setVisible(isVisible);
            markers.add(marker);
            markerExpandAnimation(marker);
        }
    }

    private void markerExpandAnimation(Marker marker) {
        ScaleAnimation animation = new ScaleAnimation(0,1,0,1);
        animation.setInterpolator(new LinearInterpolator());
        animation.setDuration(300);
        marker.setAnimation(animation);
        marker.startAnimation();
    }

    private void markerColloseAnimation(Marker marker) {
        ScaleAnimation animation = new ScaleAnimation(1,0,1,0);
        animation.setInterpolator(new LinearInterpolator());
        animation.setDuration(300);
        marker.setAnimation(animation);
        marker.startAnimation();
    }

    /**
     * 给marker添加文字
     * @return
     */
    private View getTextBitmap1(CityDto dto) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.layout_weather1, null);
        if (view == null) {
            return null;
        }
        TextView tvName = (TextView) view.findViewById(R.id.tvName);
        ImageView ivPhe = (ImageView) view.findViewById(R.id.ivPhe);
        TextView tvTemp = (TextView) view.findViewById(R.id.tvTemp);

        tvName.setText(dto.areaName);
        Drawable drawable = getResources().getDrawable(R.drawable.phenomenon_drawable);
        try {
            long zao8 = sdf3.parse("06").getTime();
            long wan8 = sdf3.parse("18").getTime();
            long current = sdf3.parse(sdf3.format(new Date())).getTime();
            if (current >= zao8 && current < wan8) {
                drawable = getResources().getDrawable(R.drawable.phenomenon_drawable);
                drawable.setLevel(dto.highPheCode);
            }else {
                drawable = getResources().getDrawable(R.drawable.phenomenon_drawable_night);
                drawable.setLevel(dto.lowPheCode);
            }
            ivPhe.setBackground(drawable);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        tvTemp.setText(dto.lowTemp+"~"+dto.highTemp+"℃");

        return view;
    }

    @Override
    public void onMapClick(LatLng arg0) {
        if (selectMarker != null) {
            selectMarker.hideInfoWindow();
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker != null) {
            selectMarker = marker;
            selectMarker.showInfoWindow();
        }
        return true;
    }

    @Override
    public View getInfoContents(final Marker marker) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.weather_marker_info, null);
        TextView tvContent = (TextView) view.findViewById(R.id.tvContent);
        ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        TextView tvDetail = (TextView) view.findViewById(R.id.tvDetail);

        tvDetail.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                CityDto data = new CityDto();
                data.areaName = marker.getSnippet();
                data.cityId = marker.getTitle();
                Intent intent = new Intent(mContext, WeatherDetailActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelable("data", data);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
        tvContent.setText("");
        getWeatherInfo(marker.getTitle(), marker.getSnippet(), tvContent, progressBar, tvDetail);
        return view;
    }

    @Override
    public View getInfoWindow(Marker arg0) {
        return null;
    }

    @Override
    public void onCameraChange(CameraPosition arg0) {
    }

    @Override
    public void onCameraChangeFinish(CameraPosition arg0) {
        if (zoom == arg0.zoom) {//如果是地图缩放级别不变，并且点击就不做处理
            return;
        }

        zoom = arg0.zoom;
        if (arg0.zoom <= 8.0f) {
            for (int i = 0; i < cityMarkers.size(); i++) {
                Marker m = cityMarkers.get(i);
                m.setVisible(true);
                markerExpandAnimation(m);
            }
            for (int i = 0; i < disMarkers.size(); i++) {
                Marker m = disMarkers.get(i);
                m.setVisible(false);
                markerColloseAnimation(m);
            }
        }if (arg0.zoom > 8.0f) {
            for (int i = 0; i < cityMarkers.size(); i++) {
                Marker m = cityMarkers.get(i);
                m.setVisible(true);
                markerExpandAnimation(m);
            }
            for (int i = 0; i < disMarkers.size(); i++) {
                Marker m = disMarkers.get(i);
                m.setVisible(true);
                markerExpandAnimation(m);
            }
        }

    }

    /**
     * 获取天气数据
     */
    private void getWeatherInfo(String cityId, final String cityName, final TextView tvContent, final ProgressBar progressBar, final TextView tvDetail) {
        WeatherAPI.getWeather2(mContext, cityId, Language.ZH_CN, new AsyncResponseHandler() {
            @Override
            public void onComplete(Weather content) {
                super.onComplete(content);
                String result = content.toString();
                if (!TextUtils.isEmpty(result)) {
                    try {
                        JSONObject obj = new JSONObject(result);

                        String factContent = cityName+"预报";

                        //实况信息
                        if (!obj.isNull("l")) {
                            JSONObject l = obj.getJSONObject("l");
                            if (!l.isNull("l7")) {
                                String time = l.getString("l7");
                                if (time != null) {
                                    factContent = factContent + "（"+time+"）"+"发布：\n";
                                }
                            }
                        }

                        //获取明天预报信息
                        if (!obj.isNull("f")) {
                            JSONObject f = obj.getJSONObject("f");
                            String f0 = f.getString("f0");
                            JSONArray f1 = f.getJSONArray("f1");
                            int i = 1;
                            JSONObject weeklyObj = f1.getJSONObject(i);

                            String week = CommonUtil.getWeek(f0, i);//星期几
                            String date = CommonUtil.getDate(f0, i);//日期
                            try {
                                factContent = factContent + sdf2.format(sdf1.parse(date)) + "（"+week+"），";
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }

                            String lowPhe = getString(WeatherUtil.getWeatherId(Integer.valueOf(weeklyObj.getString("fb"))));
                            String highPhe = getString(WeatherUtil.getWeatherId(Integer.valueOf(weeklyObj.getString("fa"))));
                            if (highPhe != null && lowPhe != null) {
                                String phe = lowPhe;
                                if (!TextUtils.equals(highPhe, lowPhe)) {
                                    phe = lowPhe + "转" + highPhe;
                                }else {
                                    phe = lowPhe;
                                }
                                factContent = factContent + phe + "，";
                            }

                            String lowTemp = weeklyObj.getString("fd");
                            String highTemp = weeklyObj.getString("fc");
                            if (lowTemp != null && highTemp != null) {
                                factContent = factContent + lowTemp + " ~ " + highTemp + "℃，";
                            }

                            String lowDir = getString(WeatherUtil.getWindDirection(Integer.valueOf(weeklyObj.getString("ff"))));
                            String lowForce = WeatherUtil.getDayWindForce(Integer.valueOf(weeklyObj.getString("fh")));
                            String highDir = getString(WeatherUtil.getWindDirection(Integer.valueOf(weeklyObj.getString("fe"))));
                            String highForce = WeatherUtil.getDayWindForce(Integer.valueOf(weeklyObj.getString("fg")));
                            if (!TextUtils.equals(lowDir+lowForce, highDir+highForce)) {
                                factContent = factContent + lowDir + lowForce + "转" + highDir + highForce;
                            }else {
                                factContent = factContent + lowDir + lowForce;
                            }

                            tvContent.setText(factContent);
                            tvDetail.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }

            @Override
            public void onError(Throwable error, String content) {
                super.onError(error, content);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.llBack:
                finish();
                break;
            case R.id.tvControl:
                isBigText = !isBigText;
                if (isBigText) {
                    tvControl.setText("缩小字体");
                    llContainer.setTextSize(18);
                } else {
                    tvControl.setText("放大字体");
                    llContainer.setTextSize(14);
                }
                break;

            default:
                break;
        }
    }

}
