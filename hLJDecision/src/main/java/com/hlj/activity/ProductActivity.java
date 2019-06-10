package com.hlj.activity;

/**
 * 热点新闻
 */

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hlj.adapter.ProductFragmentAdapter;
import com.hlj.common.CONST;
import com.hlj.dto.NewsDto;
import com.hlj.utils.OkHttpUtil;
import com.hlj.view.RefreshLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import shawn.cxwl.com.hlj.R;

public class ProductActivity extends BaseActivity implements OnClickListener, RefreshLayout.OnRefreshListener, RefreshLayout.OnLoadListener {
	
	private Context mContext = null;
	private LinearLayout llBack = null;
	private TextView tvTitle = null;
	private GridView gridView = null;
	private ProductFragmentAdapter mAdapter = null;
	private List<NewsDto> mList = new ArrayList<>();
	private int countpage = 0;//总页数
	private int page = 1;
	private int pageSize = 20;
	private RefreshLayout refreshLayout = null;//下拉刷新布局
	private String appid = null;
	private String url = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.product);
		mContext = this;
		showDialog();
		initRefreshLayout();
		initWidget();
		initListView();
	}
	
	/**
	 * 初始化下拉刷新布局
	 */
	private void initRefreshLayout() {
		refreshLayout = (RefreshLayout) findViewById(R.id.refreshLayout);
		refreshLayout.setColor(com.hlj.common.CONST.color1, com.hlj.common.CONST.color2, com.hlj.common.CONST.color3, com.hlj.common.CONST.color4);
		refreshLayout.setMode(RefreshLayout.Mode.BOTH);
		refreshLayout.setLoadNoFull(false);
		refreshLayout.setOnRefreshListener(this);
		refreshLayout.setOnLoadListener(this);
	}
	
	@Override
	public void onRefresh() {
		page = 1;
		pageSize = 20;
		mList.clear();
		operate();
	}
	
	@Override
	public void onLoad() {
		if (page >= countpage) {
			refreshLayout.setLoading(false);
			return;
		}else {
			if (!TextUtils.isEmpty(url)) {
				page += 1;
				
				String url2 = url;
				if (url2.contains("pagesize")) {
					url2 = CONST.GUIZHOU_BASE+"/Work/getnewslist/p/"+page+"/pagesize/"+pageSize+"/type/";
					
					String[] urls = url2.split("/");
					asyncQuery(url2 + urls[urls.length-1]);
				}
			}
		}
	}

	
	private void initWidget() {
		llBack = (LinearLayout) findViewById(R.id.llBack);
		llBack.setOnClickListener(this);
		tvTitle = (TextView) findViewById(R.id.tvTitle);
		tvTitle.setText(getIntent().getStringExtra(CONST.ACTIVITY_NAME));
		
		appid = getIntent().getStringExtra(CONST.INTENT_APPID);
		
		operate();
	}
	
	private void operate() {
		url = getIntent().getStringExtra(CONST.WEB_URL);
		if (!TextUtils.isEmpty(url)) {
			asyncQuery(url);
		}
	}
	
	/**
	 * 初始化listview
	 */
	private void initListView() {
		gridView = (GridView) findViewById(R.id.gridView);
		mAdapter = new ProductFragmentAdapter(mContext, mList, appid);
		gridView.setAdapter(mAdapter);
		gridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				NewsDto dto = mList.get(arg2);
				Intent intent = null;
				if (TextUtils.equals(dto.showType, CONST.URL)) {//url
					intent = new Intent(mContext, HWebviewActivity.class);
				}else if (TextUtils.equals(dto.showType, CONST.PDF)) {//pdf
					intent = new Intent(mContext, HPDFActivity.class);
				}else if (TextUtils.equals(dto.showType, CONST.NEWS)) {//news
					intent = new Intent(mContext, NewsActivity.class);
				}else if (TextUtils.equals(dto.showType, CONST.PRODUCT)) {//product
					intent = new Intent(mContext, ProductActivity.class);
				}
				if (intent != null) {
					intent.putExtra(CONST.ACTIVITY_NAME, dto.title);
					intent.putExtra(CONST.WEB_URL, dto.detailUrl);
					intent.putExtra(CONST.INTENT_APPID, appid);
					intent.putExtra(CONST.INTENT_IMGURL, dto.imgUrl);
					startActivity(intent);
				}
			}
		});
	}
	
	private void asyncQuery(final String url) {
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
								cancelDialog();
								refreshLayout.setRefreshing(false);
								refreshLayout.setLoading(false);
								if (!TextUtils.isEmpty(result)) {
									try {
										JSONObject obj = new JSONObject(result);
										if (!obj.isNull("count")) {
											String num = obj.getString("countpage");
											if (!TextUtils.isEmpty(num)) {
												countpage = Integer.valueOf(obj.getString("countpage"));
											}
										}
										if (!obj.isNull("info")) {
											JSONArray array = new JSONArray(obj.getString("info"));
											for (int i = 0; i < array.length(); i++) {
												JSONObject itemObj = array.getJSONObject(i);
												NewsDto dto = new NewsDto();
												dto.imgUrl = itemObj.getString("icon");
												dto.title = itemObj.getString("name");
												dto.time = itemObj.getString("addtime");
												dto.detailUrl = itemObj.getString("urladdress");
												dto.showType = itemObj.getString("showtype");
												mList.add(dto);
											}

											if (mAdapter != null) {
												mAdapter.notifyDataSetChanged();
											}
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
	
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.llBack) {
			finish();
		}
	}
	
}
