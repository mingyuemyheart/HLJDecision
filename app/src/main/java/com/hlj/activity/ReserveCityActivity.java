package com.hlj.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.hlj.adapter.ReserveCityAdapter;
import com.hlj.dto.CityDto;
import com.hlj.dto.WarningDto;
import com.hlj.manager.DBManager;
import com.hlj.swipemenulistview.SwipeMenu;
import com.hlj.swipemenulistview.SwipeMenuCreator;
import com.hlj.swipemenulistview.SwipeMenuItem;
import com.hlj.swipemenulistview.SwipeMenuListView;
import com.hlj.utils.CommonUtil;
import com.hlj.utils.OkHttpUtil;
import com.hlj.utils.WeatherUtil;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.com.weather.api.WeatherAPI;
import cn.com.weather.beans.Weather;
import cn.com.weather.constants.Constants;
import cn.com.weather.listener.AsyncResponseHandler;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import shawn.cxwl.com.hlj.R;

/**
 * 城市预定
 */
public class ReserveCityActivity extends BaseActivity implements View.OnClickListener, AMapLocationListener {

    private Context mContext;
    private LinearLayout llBack = null;
    private TextView tvTitle = null;
    private SwipeMenuListView mListView;
    private ReserveCityAdapter mAdapter;
    private List<CityDto> cityList = new ArrayList<>();
    private TextView tvPrompt;
    private LinearLayout llAdd;
    private AMapLocationClientOption mLocationOption = null;//声明mLocationOption对象
    private AMapLocationClient mLocationClient = null;//声明AMapLocationClient类对象
    private List<WarningDto> warningList = new ArrayList<>();//预警列表

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reserve_city);
        mContext = this;
        showDialog();
        initWidget();
        initListView();
    }

    private void initWidget() {
        llBack = (LinearLayout) findViewById(R.id.llBack);
        llBack.setOnClickListener(this);
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvTitle.setText("城市订阅");
        llAdd = (LinearLayout) findViewById(R.id.llAdd);
        llAdd.setOnClickListener(this);
        tvPrompt = (TextView) findViewById(R.id.tvPrompt);

        //获取预警信息
        OkHttpWarning("http://decision-admin.tianqi.cn/Home/extra/getwarns?order=0");
    }

    /**
     * 获取预警id
     */
    private String queryWarningIdByCityId(String cityId) {
        DBManager dbManager = new DBManager(mContext);
        dbManager.openDateBase();
        dbManager.closeDatabase();
        SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(DBManager.DB_PATH + "/" + DBManager.DB_NAME, null);
        Cursor cursor = null;
        cursor = database.rawQuery("select * from " + DBManager.TABLE_NAME3 + " where cid = " + "\"" + cityId + "\"",null);
        String warningId = null;
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            warningId = cursor.getString(cursor.getColumnIndex("wid"));
        }
        return warningId;
    }

    /**
     * 获取预警信息
     */
    private void OkHttpWarning(String url) {
        OkHttpUtil.enqueue(new Request.Builder().url(url).build(), new Callback() {
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
                        JSONObject object = new JSONObject(result);
                        if (object != null) {
                            if (!object.isNull("data")) {
                                warningList.clear();
                                JSONArray jsonArray = object.getJSONArray("data");
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONArray tempArray = jsonArray.getJSONArray(i);
                                    WarningDto dto = new WarningDto();
                                    dto.html = tempArray.optString(1);
                                    String[] array = dto.html.split("-");
                                    String item0 = array[0];
                                    String item1 = array[1];
                                    String item2 = array[2];

                                    dto.item0 = item0;
                                    dto.provinceId = item0.substring(0, 2);
                                    dto.type = item2.substring(0, 5);
                                    dto.color = item2.substring(5, 7);
                                    dto.time = item1;
                                    dto.lng = tempArray.getDouble(2);
                                    dto.lat = tempArray.getDouble(3);
                                    dto.name = tempArray.optString(0);

                                    if (!dto.name.contains("解除")) {
                                        warningList.add(dto);
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

        startLocation();
    }

    /**
     * 开始定位
     */
    private void startLocation() {
        mLocationOption = new AMapLocationClientOption();//初始化定位参数
        mLocationClient = new AMapLocationClient(mContext);//初始化定位
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
        if (amapLocation != null && amapLocation.getErrorCode() == 0) {
            String cityName = amapLocation.getDistrict()+" "+"("+amapLocation.getCity()+")";
            getGeo(amapLocation.getLongitude(), amapLocation.getLatitude(), cityName);
        }
    }

    /**
     * 初始化listview
     */
    private void initListView() {
        mListView = (SwipeMenuListView) findViewById(R.id.listView);
        mAdapter = new ReserveCityAdapter(mContext, cityList);
        mListView.setAdapter(mAdapter);
        SwipeMenuCreator creator = new SwipeMenuCreator() {
            @Override
            public void create(SwipeMenu menu) {
                switch (menu.getViewType()) {
                    case ReserveCityAdapter.notShowRemove:
                        break;
                    case ReserveCityAdapter.showRemove:
                        createMenu1(menu);
                        break;
                }
            }
            private void createMenu1(SwipeMenu menu) {
                SwipeMenuItem item1 = new SwipeMenuItem(mContext);
                item1.setBackground(new ColorDrawable(Color.RED));
                item1.setWidth((int) CommonUtil.dip2px(mContext, 70));
                item1.setTitle("删除");
                item1.setTitleColor(getResources().getColor(R.color.white));
                item1.setTitleSize(14);
                menu.addMenuItem(item1);
            }
        };
        mListView.setMenuCreator(creator);
        mListView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public void onMenuItemClick(int position, SwipeMenu menu, int index) {
                CityDto dto = cityList.get(position);
                switch (menu.getViewType()) {
                    case ReserveCityAdapter.notShowRemove:
                        break;
                    case ReserveCityAdapter.showRemove:
                        cityList.remove(position);
                        if (cityList.size() > 1) {
                            tvPrompt.setVisibility(View.VISIBLE);
                        }else {
                            tvPrompt.setVisibility(View.GONE);
                        }
                        if (mAdapter != null) {
                            mAdapter.notifyDataSetChanged();
                        }
                        break;
                }
            }
        });
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                CityDto data = cityList.get(arg2);
                Bundle bundle = new Bundle();
                bundle.putParcelable("data", data);
                Intent intent = new Intent(mContext, WeatherDetailActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
    }

    private void getGeo(double lng, double lat, final String cityName) {
        WeatherAPI.getGeo(mContext,String.valueOf(lng), String.valueOf(lat), new AsyncResponseHandler(){
            @Override
            public void onComplete(JSONObject content) {
                super.onComplete(content);
                if (!content.isNull("geo")) {
                    try {
                        JSONObject geoObj = content.getJSONObject("geo");
                        if (!geoObj.isNull("id")) {
                            String cityId = geoObj.getString("id");
                            String warningId = queryWarningIdByCityId(cityId);
                            getWeatherInfos(cityId, cityName, warningId);
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

    /**
     * 获取定位城市和保存本地城市信息
     * @param locationCityId
     * @param locationCityName
     */
    private void getWeatherInfos(String locationCityId, String locationCityName, String locationWarningId) {
        SharedPreferences sharedPreferences = getSharedPreferences("RESERVE_CITY", Context.MODE_PRIVATE);
        String cityInfo = sharedPreferences.getString("cityInfo", "");
        cityList.clear();
        final List<String> cityIds = new ArrayList<>();
        cityIds.add(locationCityId);
        CityDto dto = new CityDto();
        dto.cityId = locationCityId;
        dto.areaName = locationCityName;
        dto.warningId = locationWarningId;
        cityList.add(dto);
        if (!TextUtils.isEmpty(cityInfo)) {
            String[] array = cityInfo.split(";");
            for (int i = 0; i < array.length; i++) {
                String item = array[i];
                if (!TextUtils.isEmpty(item) && item.contains(",")) {
                    String[] itemArray = item.split(",");
                    dto = new CityDto();
                    dto.cityId = itemArray[0];
                    dto.areaName = itemArray[1];
                    dto.warningId = itemArray[2];
                    cityList.add(dto);
                    cityIds.add(itemArray[0]);
                }
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < cityIds.size(); i++) {
                    final CityDto dto = cityList.get(i);
                    final String cityId = cityIds.get(i);
                    final String url = String.format("http://api.weatherdt.com/common/?area=%s&type=forecast|observe|alarm|air&key=eca9a6c9ee6fafe74ac6bc81f577a680", cityId);
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
                                    cancelDialog();
                                    if (cityList.size() > 1) {
                                        tvPrompt.setVisibility(View.VISIBLE);
                                    }else {
                                        tvPrompt.setVisibility(View.GONE);
                                    }

                                    if (!TextUtils.isEmpty(result)) {
                                        try {
                                            JSONObject obj = new JSONObject(result);

                                            //实况信息
                                            if (!obj.isNull("observe")) {
                                                JSONObject observe = obj.getJSONObject("observe");
                                                if (!observe.isNull(cityId)) {
                                                    JSONObject object = observe.getJSONObject(cityId);
                                                    if (!object.isNull("1001002")) {
                                                        JSONObject o = object.getJSONObject("1001002");

                                                        if (!o.isNull("001")) {
                                                            String weatherCode = o.getString("001");
                                                            if (!TextUtils.isEmpty(weatherCode) && !TextUtils.equals(weatherCode, "?") && !TextUtils.equals(weatherCode, "null")) {
                                                                dto.highPheCode = Integer.parseInt(weatherCode);
                                                            }
                                                        }
                                                        if (!o.isNull("002")) {
                                                            String factTemp = o.getString("002");
                                                            dto.highTemp = factTemp;
                                                        }

                                                        List<WarningDto> list = new ArrayList<>();
                                                        for (int j = 0; j < warningList.size(); j++) {
                                                            WarningDto data = warningList.get(j);
                                                            if (TextUtils.equals(data.item0, dto.warningId)) {
                                                                list.add(data);
                                                            }
                                                        }
                                                        dto.warningList.addAll(list);
                                                    }
                                                }
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    if (mAdapter != null) {
                                        mAdapter.notifyDataSetChanged();
                                    }
                                    llAdd.setVisibility(View.VISIBLE);

                                }
                            });
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * 获取天气信息
     * @param cityId
     * @param cityName
     */
    private void getWeatherInfo(final String cityId, final String cityName, final String warningId) {
        if (TextUtils.isEmpty(cityId) || TextUtils.isEmpty(cityName)) {
            Toast.makeText(mContext, "选择的城市信息有误", Toast.LENGTH_SHORT).show();
            return;
        }
        WeatherAPI.getWeather2(mContext, cityId, Constants.Language.ZH_CN, new AsyncResponseHandler() {
            @Override
            public void onComplete(Weather content) {
                super.onComplete(content);
                if (content != null) {
                    //实况信息
                    JSONObject object = content.getWeatherFactInfo();
                    try {
                        CityDto dto = new CityDto();
                        dto.areaName = cityName;
                        dto.cityId = cityId;
                        if (!object.isNull("l5")) {
                            String weatherCode = WeatherUtil.lastValue(object.getString("l5"));
                            if (!TextUtils.isEmpty(weatherCode)) {
                                dto.highPheCode = Integer.parseInt(weatherCode);
                            }
                        }
                        if (!object.isNull("l1")) {
                            String factTemp = WeatherUtil.lastValue(object.getString("l1"));
                            dto.highTemp = factTemp;
                        }

                        dto.warningId = warningId;
                        List<WarningDto> list = new ArrayList<>();
                        for (int i = 0; i < warningList.size(); i++) {
                            WarningDto data = warningList.get(i);
                            if (TextUtils.equals(data.item0, dto.warningId)) {
                                list.add(data);
                            }
                        }
                        dto.warningList.addAll(list);

                        cityList.add(dto);

                        if (cityList.size() > 1) {
                            tvPrompt.setVisibility(View.VISIBLE);
                        }else {
                            tvPrompt.setVisibility(View.GONE);
                        }

                        if (mAdapter != null) {
                            mAdapter.notifyDataSetChanged();
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

    /**
     * 保存订阅城市信息
     */
    private void saveCityInfo() {
        //保存预定城市信息
        String cityInfo = "";
        for (int i = 1; i < cityList.size(); i++) {//从1开始是为了过滤掉定位城市
            cityInfo += (cityList.get(i).cityId+","+cityList.get(i).areaName+","+cityList.get(i).warningId+";");
        }
        SharedPreferences sharedPreferences = getSharedPreferences("RESERVE_CITY", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("cityInfo", cityInfo);
        editor.apply();
        Log.e("cityInfo", cityInfo);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        saveCityInfo();
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.llBack:
                saveCityInfo();
                finish();
                break;
            case R.id.llAdd:
                if (cityList.size() >= 10) {
                    Toast.makeText(mContext, "最多只能关注10个城市", Toast.LENGTH_SHORT).show();
                    return;
                }
                startActivityForResult(new Intent(mContext, CityActivity.class), 1001);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 1001:
                    if (data != null) {
                        CityDto dto = data.getExtras().getParcelable("data");
                        if (dto != null) {

                            boolean isDuplicate = false;//是否重复
                            for (int i = 0; i < cityList.size(); i++) {
                                if (TextUtils.equals(cityList.get(i).cityId, dto.cityId)) {//防止重复添加同一个城市
                                    isDuplicate = true;
                                    break;
                                }
                            }
                            if (isDuplicate) {
                                Toast.makeText(mContext, "该城市已关注", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            String warningId = queryWarningIdByCityId(dto.cityId);
                            String cityName = "";
                            if (!TextUtils.isEmpty(dto.sectionName)) {
                                if (dto.sectionName.contains(dto.areaName)) {
                                    cityName = dto.areaName;
                                }else {
                                    cityName = dto.areaName+" "+"("+dto.sectionName+")";
                                }
                            }else {
                                cityName = dto.areaName;
                            }
                            getWeatherInfo(dto.cityId, cityName, warningId);
                        }
                    }
                    break;
            }
        }
    }
}
