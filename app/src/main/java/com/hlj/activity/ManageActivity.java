package com.hlj.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hlj.adapter.ManageAdapter;
import com.hlj.common.ColumnData;
import com.hlj.common.MyApplication;
import com.hlj.stickygridheaders.StickyGridHeadersGridView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import shawn.cxwl.com.hlj.R;

/**
 * 服务产品定制
 */
public class ManageActivity extends BaseActivity implements View.OnClickListener {

    private Context mContext;
    private List<ColumnData> dataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage);
        MyApplication.addDestoryActivity(this, "ManageActivity");
        mContext = this;
        initWidget();
        initGridView();
    }

    private void initWidget() {
        LinearLayout llBack = findViewById(R.id.llBack);
        llBack.setOnClickListener(this);
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("模块管理");
        TextView tvControl = findViewById(R.id.tvControl);
        tvControl.setOnClickListener(this);
        tvControl.setText("保存");
        tvControl.setVisibility(View.VISIBLE);
    }

    private int section = 1;
    private void initGridView() {
        String columnIds = MyApplication.getColumnIds(this);
        dataList.clear();
        for (int i = 0; i < MyApplication.columnDataList.size(); i++) {
            ColumnData dto = MyApplication.columnDataList.get(i);
            if (TextUtils.equals(dto.id, "8") || TextUtils.equals(dto.id, "9") || TextUtils.equals(dto.id, "10")) {
                if (dto.child.size() <= 0) {
                    dto.headerName = dto.name;
                    if (!TextUtils.isEmpty(columnIds)) {
                        if (!TextUtils.isEmpty(dto.columnId)) {
                            if (!columnIds.contains(dto.columnId)) {//已经有保存的栏目
                                dto.isSelected = true;
                            }else {
                                dto.isSelected = false;
                            }
                        }else {
                            dto.isSelected = true;
                        }
                    }else {
                        dto.isSelected = true;
                    }
                    dataList.add(dto);
                }else {
                    for (int j = 0; j < dto.child.size(); j++) {
                        ColumnData data = dto.child.get(j);
                        data.headerName = dto.name;
                        data.icon = dto.icon;
                        if (!TextUtils.isEmpty(columnIds)) {
                            if (!TextUtils.isEmpty(data.columnId)) {
                                if (!columnIds.contains(data.columnId)) {//已经有保存的栏目
                                    data.isSelected = true;
                                }else {
                                    data.isSelected = false;
                                }
                            }else {
                                data.isSelected = true;
                            }
                        }else {
                            data.isSelected = true;
                        }
                        dataList.add(data);
                    }
                }
            }
        }

        Map<String, Integer> sectionMap = new LinkedHashMap<>();
        for (int i = 0; i < dataList.size(); i++) {
            ColumnData dto = dataList.get(i);
            if (!sectionMap.containsKey(dto.headerName)) {
                dto.section = section;
                sectionMap.put(dto.headerName, section);
                section++;
            }else {
                dto.section = sectionMap.get(dto.headerName);
            }
            dataList.set(i, dto);
        }

        StickyGridHeadersGridView pGridView = findViewById(R.id.pGridView);
        final ManageAdapter pAdapter = new ManageAdapter(mContext, dataList);
        pGridView.setAdapter(pAdapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.llBack:
                finish();
                break;
            case R.id.tvControl:
                String columnIds = "";
                for (int i = 0; i < dataList.size(); i++) {
                    ColumnData dto = dataList.get(i);
                    if (!dto.isSelected) {
                        columnIds += dto.columnId+"--,";
                    }
                }

                Map<String, List<ColumnData>> map = new LinkedHashMap<>();
                for (int i = 0; i < dataList.size(); i++) {
                    ColumnData dto = dataList.get(i);
                    if (!map.containsKey(dto.groupColumnId)) {
                        List<ColumnData> list = new ArrayList<>();
                        list.add(dto);
                        map.put(dto.groupColumnId, list);
                    }else {
                        map.get(dto.groupColumnId).add(dto);
                    }
                }
                for (String key: map.keySet()) {
                    List<ColumnData> list = map.get(key);
                    boolean isAllSelect = false;
                    for (int i = 0; i < list.size(); i++) {
                        ColumnData dto = list.get(i);
                        if (dto.isSelected) {
                            isAllSelect = true;
                            break;
                        }
                    }
                    if (!isAllSelect) {
                        columnIds += key+"--,";
                    }
                }
                Log.e("columnIds", columnIds);
                MyApplication.saveColumnIds(this, columnIds);
                Intent intent = new Intent();
                intent.putExtra("columnIds", columnIds);
                setResult(RESULT_OK, intent);
                finish();
                break;
        }
    }

}
