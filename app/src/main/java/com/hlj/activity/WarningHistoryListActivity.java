package com.hlj.activity;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hlj.adapter.WarningHistoryListAdapter;
import com.hlj.adapter.WarningHistoryListScreenAdapter;
import com.hlj.dto.WarningDto;
import com.hlj.utils.OkHttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import shawn.cxwl.com.hlj.R;

/**
 * 历史预警-预警列表
 */
public class WarningHistoryListActivity extends BaseActivity implements View.OnClickListener {

    private Context mContext;
    private WarningHistoryListAdapter mAdapter;
    private List<WarningDto> groupList = new ArrayList<>();
    private List<List<WarningDto>> childList = new ArrayList<>();
    private String baseUrl = "http://testdecision.tianqi.cn/alarm12379/hisalarm.php?format=1";
    private int page = 1, pageSize = 20;
    private TextView tvTime;
    private boolean isDesc = false;//默认为升序

    private LinearLayout llContainer1;
    private GridView gridView1,gridView2;
    private WarningHistoryListScreenAdapter adapter1,adapter2;
    private List<WarningDto> list1 = new ArrayList<>();
    private List<WarningDto> list2 = new ArrayList<>();
    private String color = "999999", type= "999999";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warning_history_list);
        mContext = this;
        initWidget();
        initListView();
    }

    private void initWidget() {
        LinearLayout llBack = findViewById(R.id.llBack);
        llBack.setOnClickListener(this);
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTime = findViewById(R.id.tvTime);
        tvTime.setOnClickListener(this);
        TextView tvSelect = findViewById(R.id.tvSelect);
        tvSelect.setOnClickListener(this);
        llContainer1 = findViewById(R.id.llContainer1);
        TextView tvNegtive = findViewById(R.id.tvNegtive);
        tvNegtive.setOnClickListener(this);
        TextView tvPositive = findViewById(R.id.tvPositive);
        tvPositive.setOnClickListener(this);

        groupList.clear();
        childList.clear();
        if (getIntent().hasExtra("data")) {
            WarningDto data = getIntent().getExtras().getParcelable("data");
            if (data != null) {
                tvTitle.setText(data.areaName+"预警列表");
                if (!TextUtils.isEmpty(data.areaKey)) {
                    baseUrl = baseUrl+"&areaid="+data.areaKey;
                    if (!TextUtils.isEmpty(data.type)) {
                        baseUrl = baseUrl+"&areaid="+data.areaKey+"&type="+data.type;
                    }
                    OkHttpStatisticList(baseUrl);
                }
            }
        }
    }

    private ExpandableListView listView;
    private void initListView() {
        listView = findViewById(R.id.listView);
        mAdapter = new WarningHistoryListAdapter(mContext, groupList, childList);
        listView.setAdapter(mAdapter);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE && view.getLastVisiblePosition() == view.getCount() - 1) {
                    page += 1;
                    baseUrl = baseUrl+"&page="+page+"&pagesize="+pageSize;
                    OkHttpStatisticList(baseUrl);
                }
            }

            @Override
            public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
            }
        });
    }

    /**
     * 获取预警统计列表信息
     * @param url
     */
    private void OkHttpStatisticList(final String url) {
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
                                        JSONObject object = new JSONObject(result);
                                        if (!object.isNull("data")) {
                                            JSONArray array = object.getJSONArray("data");
                                            for (int i = 0; i < array.length(); i++) {
                                                WarningDto dto = new WarningDto();
                                                JSONObject obj = array.getJSONObject(i);
                                                if (!obj.isNull("sendTime")) {
                                                    dto.time = obj.getString("sendTime");
                                                }
                                                if (!obj.isNull("caceltime")) {
                                                    dto.time2 = obj.getString("caceltime");
                                                }
                                                if (!obj.isNull("headline")) {
                                                    dto.name = obj.getString("headline");
                                                }
                                                if (!obj.isNull("severity")) {
                                                    dto.color = obj.getString("severity");
                                                }
                                                if (!obj.isNull("eventType")) {
                                                    dto.type = obj.getString("eventType");
                                                }
                                                if (!obj.isNull("eventType")) {
                                                    dto.type = obj.getString("eventType");
                                                }
                                                if (!obj.isNull("eventTypeCn")) {
                                                    dto.eventTypeCn = obj.getString("eventTypeCn");
                                                }
                                                if (!obj.isNull("description")) {
                                                    dto.content = obj.getString("description");
                                                }

                                                List<WarningDto> list = new ArrayList<>();
                                                WarningDto d = new WarningDto();
                                                d.time = dto.time;
                                                d.time2 = dto.time2;
                                                d.name = dto.name;
                                                d.color = dto.color;
                                                d.type = dto.type;
                                                d.eventTypeCn = dto.eventTypeCn;
                                                d.content = dto.content;
                                                list.add(d);
                                                childList.add(list);

                                                groupList.add(dto);
                                            }
                                        }

                                        if (mAdapter != null) {
                                            mAdapter.notifyDataSetChanged();
                                        }
                                        cancelDialog();

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

    private void initGridView1() {
        list1.clear();
        String[] array1 = getResources().getStringArray(R.array.warningColor2);
        for (int i = 0; i < array1.length; i++) {
            HashMap<String, Integer> map = new HashMap<>();
            String[] value = array1[i].split(",");
            int count = 0;
            for (int j = 0; j < groupList.size(); j++) {
                if (TextUtils.equals(groupList.get(j).color, value[0])) {
                    map.put(groupList.get(j).color, count++);
                }
            }

            WarningDto dto = new WarningDto();
            dto.name = value[1];
            dto.color = value[0];
            dto.count = count;
            list1.add(dto);
        }
        if (gridView1 != null && adapter1 != null) {
            adapter1.notifyDataSetChanged();
        }else {
            gridView1 = findViewById(R.id.gridView1);
            adapter1 = new WarningHistoryListScreenAdapter(mContext, list1);
            gridView1.setAdapter(adapter1);
        }

        gridView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                WarningDto dto = list1.get(arg2);
                color = dto.color;

                if (arg2 != 0) {
                    if (dto.count == 0) {
                        return;
                    }
                }
                adapter1.setIndex(arg2);
                adapter1.notifyDataSetChanged();
            }
        });
    }

    private void initGridView2() {
        list2.clear();
        String[] array1 = getResources().getStringArray(R.array.warningType);
        for (int i = 0; i < array1.length; i++) {
            HashMap<String, Integer> map = new HashMap<>();
            String[] value = array1[i].split(",");
            int count = 0;
            for (int j = 0; j < groupList.size(); j++) {
                if (TextUtils.equals(groupList.get(j).type, value[0])) {
                    map.put(groupList.get(j).type, count++);
                }
            }

            WarningDto dto = new WarningDto();
            dto.name = value[1];
            dto.type = value[0];
            dto.count = count;
            list2.add(dto);
        }
        if (gridView2 != null && adapter2 != null) {
            adapter2.notifyDataSetChanged();
        }else {
            gridView2 = findViewById(R.id.gridView2);
            adapter2 = new WarningHistoryListScreenAdapter(mContext, list2);
            gridView2.setAdapter(adapter2);
        }

        gridView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                WarningDto dto = list2.get(arg2);
                type = dto.type;

                if (arg2 != 0) {
                    if (dto.count == 0) {
                        return;
                    }
                }
                adapter2.setIndex(arg2);
                adapter2.notifyDataSetChanged();
            }
        });
    }

    /**
     * @param flag false为显示map，true为显示list
     */
    private void startAnimation(boolean flag, final View view) {
        //列表动画
        AnimationSet animationSet = new AnimationSet(true);
        TranslateAnimation animation;
        if (!flag) {
            animation = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF,0f,
                    Animation.RELATIVE_TO_SELF,0f,
                    Animation.RELATIVE_TO_SELF,-1.0f,
                    Animation.RELATIVE_TO_SELF,0f);
        }else {
            animation = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF,0f,
                    Animation.RELATIVE_TO_SELF,0f,
                    Animation.RELATIVE_TO_SELF,0f,
                    Animation.RELATIVE_TO_SELF,-1.0f);
        }
        animation.setDuration(400);
        animationSet.addAnimation(animation);
        animationSet.setFillAfter(true);
        view.startAnimation(animationSet);
        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation arg0) {
            }
            @Override
            public void onAnimationRepeat(Animation arg0) {
            }
            @Override
            public void onAnimationEnd(Animation arg0) {
                view.clearAnimation();
            }
        });
    }

    private void bootAnimation(View view) {
        if (view.getVisibility() == View.GONE) {
            openList(view);
        }else {
            closeList(view);
        }
    }

    private void openList(View view) {
        startAnimation(false, view);
        view.setVisibility(View.VISIBLE);
        tvTime.setVisibility(View.INVISIBLE);
    }

    private void closeList(View view) {
        startAnimation(true, view);
        view.setVisibility(View.GONE);
        tvTime.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (llContainer1.getVisibility() == View.VISIBLE) {
                closeList(llContainer1);
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.llBack:
                finish();
                break;
            case R.id.tvTime:
                if (!isDesc) {
                    isDesc = true;
                    tvTime.setText("按发布时间⬆︎︎");
                    Collections.sort(groupList, new Comparator<WarningDto>() {
                        @Override
                        public int compare(WarningDto a, WarningDto b) {
                            return b.time.compareTo(a.time);
                        }
                    });
                    Collections.sort(childList, new Comparator<List<WarningDto>>() {
                        @Override
                        public int compare(List<WarningDto> a, List<WarningDto> b) {
                            return b.get(0).time.compareTo(a.get(0).time);
                        }
                    });
                }else {
                    isDesc = false;
                    tvTime.setText("按发布时间⬇︎");
                    Collections.sort(groupList, new Comparator<WarningDto>() {
                        @Override
                        public int compare(WarningDto a, WarningDto b) {
                            return a.time.compareTo(b.time);
                        }
                    });
                    Collections.sort(childList, new Comparator<List<WarningDto>>() {
                        @Override
                        public int compare(List<WarningDto> a, List<WarningDto> b) {
                            return a.get(0).time.compareTo(b.get(0).time);
                        }
                    });
                }
                if (mAdapter != null) {
                    mAdapter.notifyDataSetChanged();
                }
                break;
            case R.id.tvSelect:
                initGridView1();
                initGridView2();
                bootAnimation(llContainer1);
                break;
            case R.id.tvNegtive:
                adapter1.setIndex(0);
                adapter1.notifyDataSetChanged();

                adapter2.setIndex(0);
                adapter2.notifyDataSetChanged();
                break;
            case R.id.tvPositive:
                List<WarningDto> list = new ArrayList<>();
                list.clear();
                list.addAll(groupList);
                groupList.clear();
                for (int i = 0; i < list.size(); i++) {
                    WarningDto dto = list.get(i);
                    if (TextUtils.equals(color, "999999") && TextUtils.equals(type, "999999")) {
                        groupList.add(dto);
                    }else if (TextUtils.equals(color, "999999")) {
                        if (TextUtils.equals(dto.type, type)) {
                            groupList.add(dto);
                        }
                    }else if (TextUtils.equals(type, "999999")) {
                        if (TextUtils.equals(dto.color, color)) {
                            groupList.add(dto);
                        }
                    }else {
                        if (TextUtils.equals(dto.color, color) && TextUtils.equals(dto.type, type)) {
                            groupList.add(dto);
                        }
                    }

                }
                if (mAdapter != null) {
                    mAdapter.notifyDataSetChanged();
                }
                closeList(llContainer1);
                break;
        }
    }

}
