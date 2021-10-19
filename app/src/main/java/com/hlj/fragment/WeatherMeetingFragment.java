package com.hlj.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.hlj.activity.ShawnWeatherMeetingDetailActivity;
import com.hlj.activity.ShawnWeatherMeetingVideoActivity;
import com.hlj.adapter.ShawnWeatherMeetingAdapter;
import com.hlj.common.CONST;
import com.hlj.dto.WeatherMeetingDto;
import com.hlj.utils.OkHttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import shawn.cxwl.com.hlj.R;

/**
 * 天气会商本周安排
 */
public class WeatherMeetingFragment extends Fragment {

    private TextView tvDate = null;
    private ExpandableListView listView = null;
    private ShawnWeatherMeetingAdapter mAdapter = null;
    private List<WeatherMeetingDto> groupList = new ArrayList<>();
    private List<List<WeatherMeetingDto>> childList = new ArrayList<>();
    private SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
    private SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);
    private SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
    private SimpleDateFormat sdf4 = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);
    private String liveUrl1 = "http://10.0.86.110/rest/LiveService/getLiveList/10001";
    private String liveUrl2 = "http://106.120.82.240/rest/LiveService/getLiveList/10001";
    //	private String liveUrl3 = "http://111.205.114.31/rest/LiveService/getLiveList/10001";
    private String publicIp = "106.120.82.240";
    private List<WeatherMeetingDto> liveList = new ArrayList<>();//直播列表
    private List<WeatherMeetingDto> videoList = new ArrayList<>();//点播列表

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_weather_meeting, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initWidget(view);
        initListView(view);
    }

    private void initWidget(View view) {
        tvDate = view.findViewById(R.id.tvDate);

        videoList.clear();
        videoList.addAll(getArguments().<WeatherMeetingDto>getParcelableArrayList("videoList"));

        WeatherMeetingDto data = getArguments().getParcelable("data");
        if (!TextUtils.isEmpty(data.columnUrl)) {
            int index = getArguments().getInt("index");
            OkHttpMeetingList(data.columnUrl, index);
        }

    }

    private void initListView(View view) {
        listView = view.findViewById(R.id.listView);
        mAdapter = new ShawnWeatherMeetingAdapter(getActivity(), groupList, childList, videoList);
        listView.setAdapter(mAdapter);
        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                WeatherMeetingDto dto = childList.get(groupPosition).get(childPosition);
                Intent intent;
                if (dto.state == 1) {//直播
                    if (liveList.size() <= 0) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    OkHttpLive(liveUrl1);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    try {
                                        OkHttpLive(liveUrl2);
                                    } catch (Exception e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            }
                        }).start();
                    }else {
                        intent = new Intent(getActivity(), ShawnWeatherMeetingDetailActivity.class);
                        String hlsAddress = liveList.get(0).hlsAddress;
                        intent.putExtra(CONST.WEB_URL, hlsAddress);
                        startActivity(intent);
                    }
                }else if (dto.state == 2) {//点播
                    String hlsAddress = "";
                    try {
                        long start = Long.valueOf(dto.startTime);
                        long end = Long.valueOf(dto.endTime);
                        for (int i = 0; i < videoList.size(); i++) {
                            WeatherMeetingDto video = videoList.get(i);
                            long videoTime = Long.valueOf(sdf4.format(sdf3.parse(video.videoTime)));
                            if (videoTime >= start && videoTime <= end) {
                                hlsAddress = video.hlsAddress;
                                break;
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    intent = new Intent(getActivity(), ShawnWeatherMeetingVideoActivity.class);
                    intent.putExtra(CONST.WEB_URL, hlsAddress);
                    intent.putExtra(CONST.ACTIVITY_NAME, dto.title);
                    startActivity(intent);
                }
                return true;
            }
        });
    }

    private void OkHttpMeetingList(final String url, final int index) {
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
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!TextUtils.isEmpty(result)) {
                                    try {
                                        JSONArray arr = new JSONArray(result);
                                        JSONObject obj = arr.getJSONObject(index);
                                        if (!obj.isNull("DS")) {
                                            JSONArray array = obj.getJSONArray("DS");
                                            for (int i = 0; i < array.length(); i++) {
                                                JSONObject itemObj = array.getJSONObject(i);
                                                WeatherMeetingDto dto = new WeatherMeetingDto();
                                                if (!itemObj.isNull("Date")) {
                                                    dto.date = itemObj.getString("Date");
                                                }

                                                if (!itemObj.isNull("SubDS")) {
                                                    List<WeatherMeetingDto> list = new ArrayList<>();
                                                    list.clear();
                                                    JSONArray itemArray = itemObj.getJSONArray("SubDS");
                                                    for (int j = 0; j < itemArray.length(); j++) {
                                                        JSONObject subObj = itemArray.getJSONObject(j);
                                                        WeatherMeetingDto data = new WeatherMeetingDto();
                                                        if (!subObj.isNull("DateStart")) {
                                                            data.startTime = subObj.getString("DateStart");
                                                        }
                                                        if (!subObj.isNull("DateEnd")) {
                                                            data.endTime = subObj.getString("DateEnd");
                                                        }
                                                        if (!subObj.isNull("Content")) {
                                                            data.title = subObj.getString("Content");
                                                        }
                                                        list.add(data);
                                                    }
                                                    childList.add(list);
                                                }
                                                groupList.add(dto);
                                            }
                                        }

                                        if (groupList.size() > 0 && mAdapter != null) {
                                            mAdapter.notifyDataSetChanged();
                                            int count = listView.getCount();
                                            for (int i = 0; i < count; i++) {
                                                listView.expandGroup(i);
                                            }

                                            String startDate = groupList.get(0).date;
                                            String endDate = groupList.get(groupList.size()-1).date;
                                            try {
                                                tvDate.setText(sdf2.format(sdf1.parse(startDate))+" - "+sdf2.format(sdf1.parse(endDate)));
                                            } catch (ParseException e) {
                                                e.printStackTrace();
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

    private final OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS);
    private final OkHttpClient okHttpClient = builder.build();

    //获取直播地址
    private void OkHttpLive(String url) throws IOException{
        Request request = new Request.Builder().url(url).build();
        Response response = okHttpClient.newCall(request).execute();
        if (response.isSuccessful()) {
            String result = response.body().string();
            parseData(result, url);
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }

    private void parseData(final String result, final String url) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (result != null) {
                    try {
                        JSONArray array = new JSONArray(result);
                        liveList.clear();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            WeatherMeetingDto dto = new WeatherMeetingDto();
                            if (!obj.isNull("liveName")) {
                                dto.liveName = obj.getString("liveName");
                            }
                            if (!obj.isNull("liveStart")) {
                                dto.liveStart = obj.getString("liveStart");
                            }
                            if (!obj.isNull("liveEnd")) {
                                dto.liveEnd = obj.getString("liveEnd");
                            }
                            if (!obj.isNull("hlsAddress")) {
                                if (url.contains(publicIp)) {
                                    String addr = obj.getString("hlsAddress");
                                    if (addr.contains("http://")) {
                                        addr = addr.substring("http://".length(), addr.length());
                                        addr = addr.replace(addr.substring(0, addr.indexOf("/")), publicIp);
                                        dto.hlsAddress = "http://"+addr;
                                    }
                                }else {
                                    dto.hlsAddress = obj.getString("hlsAddress");
                                }
                            }
                            liveList.add(dto);
                        }

                        Intent intent = new Intent(getActivity(), ShawnWeatherMeetingDetailActivity.class);
                        String hlsAddress = liveList.get(0).hlsAddress;
                        intent.putExtra(CONST.WEB_URL, hlsAddress);
                        startActivity(intent);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

}
