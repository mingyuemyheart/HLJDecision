package com.hlj.activity;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hlj.adapter.SixHourRainfallAdapter;
import com.hlj.common.CONST;
import com.hlj.dto.AgriDto;
import com.hlj.dto.RangeDto;
import com.hlj.manager.StationSecretManager;
import com.hlj.stickygridheaders.StickyGridHeadersGridView;
import com.hlj.utils.CommonUtil;
import com.hlj.utils.OkHttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import shawn.cxwl.com.hlj.R;

/**
 * 6小时降水量
 */
public class SixHourRainActivity extends BaseActivity implements OnClickListener {

    private Context mContext = null;
    private SixHourRainfallAdapter mAdapter = null;
    private List<RangeDto> dataList = new ArrayList<>();
    private int section = 1;
    private HashMap<String, Integer> sectionMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_6hour_rain);
        mContext = this;
        showDialog();
        initWidget();
        initGridView();
    }

    private void initWidget() {
        LinearLayout llBack = findViewById(R.id.llBack);
        llBack.setOnClickListener(this);
        TextView tvTitle = findViewById(R.id.tvTitle);

        AgriDto data = getIntent().getExtras().getParcelable("data");
        if (data != null) {
            tvTitle.setText(data.name);
        }

        parseCityInfo(this);
    }

    /**
     * 初始化initGridView
     */
    private void initGridView() {
        StickyGridHeadersGridView mGridView = findViewById(R.id.stickyGridView);
        mAdapter = new SixHourRainfallAdapter(mContext, dataList);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
//				RangeDto dto = dataList.get(arg2);
//				Intent intent = new Intent(mContext, TrendDetailActivity.class);
//				Bundle bundle = new Bundle();
//				bundle.putParcelable("data", dto);
//				intent.putExtras(bundle);
//				startActivity(intent);
            }
        });
    }

    /**
     * 解析城市信息
     *
     * @param context
     */
    private void parseCityInfo(Context context) {
        String result = CommonUtil.getFromAssets(context, "city_info.json");
        if (!TextUtils.isEmpty(result)) {
            try {
                dataList.clear();
                JSONObject obj = new JSONObject(result);

                JSONArray array1 = obj.getJSONArray("黑河市");
                for (int i = 0; i < array1.length(); i++) {
                    JSONArray itemArray1 = array1.getJSONArray(i);
                    RangeDto dto = new RangeDto();
                    dto.cityName = itemArray1.getString(1);
                    dto.areaName = itemArray1.getString(2);
                    dto.stationId = itemArray1.getString(4);
                    dataList.add(dto);
                }

                JSONArray array2 = obj.getJSONArray("哈尔滨市");
                for (int i = 0; i < array2.length(); i++) {
                    JSONArray itemArray2 = array2.getJSONArray(i);
                    RangeDto dto = new RangeDto();
                    dto.cityName = itemArray2.getString(1);
                    dto.areaName = itemArray2.getString(2);
                    dto.stationId = itemArray2.getString(4);
                    dataList.add(dto);
                }

                JSONArray array3 = obj.getJSONArray("齐齐哈尔市");
                for (int i = 0; i < array3.length(); i++) {
                    JSONArray itemArray3 = array3.getJSONArray(i);
                    RangeDto dto = new RangeDto();
                    dto.cityName = itemArray3.getString(1);
                    dto.areaName = itemArray3.getString(2);
                    dto.stationId = itemArray3.getString(4);
                    dataList.add(dto);
                }

                JSONArray array4 = obj.getJSONArray("绥化市");
                for (int i = 0; i < array4.length(); i++) {
                    JSONArray itemArray4 = array4.getJSONArray(i);
                    RangeDto dto = new RangeDto();
                    dto.cityName = itemArray4.getString(1);
                    dto.areaName = itemArray4.getString(2);
                    dto.stationId = itemArray4.getString(4);
                    dataList.add(dto);
                }

                JSONArray array5 = obj.getJSONArray("七台河市");
                for (int i = 0; i < array5.length(); i++) {
                    JSONArray itemArray5 = array5.getJSONArray(i);
                    RangeDto dto = new RangeDto();
                    dto.cityName = itemArray5.getString(1);
                    dto.areaName = itemArray5.getString(2);
                    dto.stationId = itemArray5.getString(4);
                    dataList.add(dto);
                }

                JSONArray array6 = obj.getJSONArray("伊春市");
                for (int i = 0; i < array6.length(); i++) {
                    JSONArray itemArray6 = array6.getJSONArray(i);
                    RangeDto dto = new RangeDto();
                    dto.cityName = itemArray6.getString(1);
                    dto.areaName = itemArray6.getString(2);
                    dto.stationId = itemArray6.getString(4);
                    dataList.add(dto);
                }

                JSONArray array7 = obj.getJSONArray("双鸭山市");
                for (int i = 0; i < array7.length(); i++) {
                    JSONArray itemArray7 = array7.getJSONArray(i);
                    RangeDto dto = new RangeDto();
                    dto.cityName = itemArray7.getString(1);
                    dto.areaName = itemArray7.getString(2);
                    dto.stationId = itemArray7.getString(4);
                    dataList.add(dto);
                }

                JSONArray array8 = obj.getJSONArray("佳木斯市");
                for (int i = 0; i < array8.length(); i++) {
                    JSONArray itemArray8 = array8.getJSONArray(i);
                    RangeDto dto = new RangeDto();
                    dto.cityName = itemArray8.getString(1);
                    dto.areaName = itemArray8.getString(2);
                    dto.stationId = itemArray8.getString(4);
                    dataList.add(dto);
                }

                JSONArray array9 = obj.getJSONArray("鸡西市");
                for (int i = 0; i < array9.length(); i++) {
                    JSONArray itemArray9 = array9.getJSONArray(i);
                    RangeDto dto = new RangeDto();
                    dto.cityName = itemArray9.getString(1);
                    dto.areaName = itemArray9.getString(2);
                    dto.stationId = itemArray9.getString(4);
                    dataList.add(dto);
                }

                JSONArray array10 = obj.getJSONArray("大庆市");
                for (int i = 0; i < array10.length(); i++) {
                    JSONArray itemArray10 = array10.getJSONArray(i);
                    RangeDto dto = new RangeDto();
                    dto.cityName = itemArray10.getString(1);
                    dto.areaName = itemArray10.getString(2);
                    dto.stationId = itemArray10.getString(4);
                    dataList.add(dto);
                }

                JSONArray array11 = obj.getJSONArray("鹤岗市");
                for (int i = 0; i < array11.length(); i++) {
                    JSONArray itemArray11 = array11.getJSONArray(i);
                    RangeDto dto = new RangeDto();
                    dto.cityName = itemArray11.getString(1);
                    dto.areaName = itemArray11.getString(2);
                    dto.stationId = itemArray11.getString(4);
                    dataList.add(dto);
                }

                JSONArray array12 = obj.getJSONArray("大兴安岭市");
                for (int i = 0; i < array12.length(); i++) {
                    JSONArray itemArray12 = array12.getJSONArray(i);
                    RangeDto dto = new RangeDto();
                    dto.cityName = itemArray12.getString(1);
                    dto.areaName = itemArray12.getString(2);
                    dto.stationId = itemArray12.getString(4);
                    dataList.add(dto);
                }

                JSONArray array13 = obj.getJSONArray("牡丹江市");
                for (int i = 0; i < array13.length(); i++) {
                    JSONArray itemArray13 = array13.getJSONArray(i);
                    RangeDto dto = new RangeDto();
                    dto.cityName = itemArray13.getString(1);
                    dto.areaName = itemArray13.getString(2);
                    dto.stationId = itemArray13.getString(4);
                    dataList.add(dto);
                }

                String stationIds = "";
                for (int i = 0; i < dataList.size(); i++) {
                    RangeDto sectionDto = dataList.get(i);
                    if (!sectionMap.containsKey(sectionDto.cityName)) {
                        sectionDto.section = section;
                        sectionMap.put(sectionDto.cityName, section);
                        section++;
                    } else {
                        sectionDto.section = sectionMap.get(sectionDto.cityName);
                    }
                    dataList.set(i, sectionDto);
                    stationIds = stationIds + dataList.get(i).stationId + ",";
                }

//				String stationIds = "" +
//						"50136,50246,50247,50349,50353,50442,50468,50557,50564,50566," +
//						"50646,50655,50656,50658,50659,50673,50674,50739,50741,50742," +
//						"50745,50749,50750,50755,50756,50758,50767,50772,50774,50775," +
//						"50776,50778,50779,50787,50788,50842,50844,50850,50851,50852," +
//						"50853,50854,50858,50859,50861,50862,50867,50871,50873,50877," +
//						"50878,50879,50880,50884,50888,50892,50950,50953,50954,50955," +
//						"50956,50958,50960,50962,50963,50964,50965,50968,50971,50973," +
//						"50978,50979,50983,50985,50987,54080,54092,54093,54094,54096," +
//						"54098,54099,50137";
                OkHttpStationInfo(stationIds);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取最近10个站点的信息
     *
     * @param stationIds
     */
    private void OkHttpStationInfo(final String stationIds) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String[] ids = stationIds.split(",");
                String url = StationSecretManager.getStationUrl("http://61.4.184.171:8080/weather/rgwst/NewestDataNew", ids[0]);
                FormBody.Builder builder = new FormBody.Builder();
                builder.add("ids", stationIds);
                RequestBody body = builder.build();
                OkHttpUtil.enqueue(new Request.Builder().post(body).url(url).build(), new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                cancelDialog();
                            }
                        });
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
                                cancelDialog();
                                if (!TextUtils.isEmpty(result)) {
                                    try {
                                        JSONArray array = new JSONArray(result);
                                        for (int i = 0; i < array.length(); i++) {
                                            JSONObject obj = array.getJSONObject(i);
                                            RangeDto dto = dataList.get(i);
                                            if (!obj.isNull("rainfall6")) {
                                                String value = obj.getString("rainfall6");
                                                if (!TextUtils.isEmpty(value)) {
                                                    dto.sixRain = value;
                                                }
                                            }
                                        }
                                        if (mAdapter != null) {
                                            mAdapter.notifyDataSetChanged();
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.llBack:
                finish();
                break;

            default:
                break;
        }
    }

}
