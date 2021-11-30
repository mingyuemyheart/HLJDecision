package com.hlj.utils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by shawn on 2017/8/3.
 */
public class OkHttpUtil {

    private final static OkHttpClient.Builder builder = new OkHttpClient.Builder();
    private final static OkHttpClient okHttpClient = builder.build();
    private static int timeOut = 60;//超时时间

    static{
//        builder.connectTimeout(timeOut, TimeUnit.SECONDS);//设置请求超时时间
//        builder.readTimeout(timeOut, TimeUnit.SECONDS);//设置读取超时时间
//        builder.writeTimeout(timeOut, TimeUnit.SECONDS);//设置写入超时时间
        setConnectTimeOut(timeOut);
        setReadTimeOut(timeOut);
        setWriteTimeOut(timeOut);
    }

    /**
     * //设置请求超时时间
     * @param timeOut
     */
    public static void setConnectTimeOut(long timeOut) {
        builder.connectTimeout(timeOut, TimeUnit.SECONDS);
    }

    /**
     * //设置读取超时时间
     * @param timeOut
     */
    public static void setReadTimeOut(long timeOut) {
        builder.readTimeout(timeOut, TimeUnit.SECONDS);
    }

    /**
     * //设置写入超时时间
     * @param timeOut
     */
    public static void setWriteTimeOut(long timeOut) {
        builder.writeTimeout(timeOut, TimeUnit.SECONDS);
    }

    /**
     * get请求，UI线程访问
     * @param url
     * @return
     * @throws IOException
     */
    public static String getExecute(String url) throws IOException{
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(url);
        Request request = requestBuilder.build();
        Call call = okHttpClient.newCall(request);
        Response response = call.execute();
        return response.body().string();
    }

    /**
     * post请求，UI线程访问
     * @param url
     * @param array
     * @return
     */
    public static String postExecute(String url, String[] array) {
        try {
            FormBody.Builder formBodyBuilder = new FormBody.Builder();
            for (int i = 0; i < array.length; i++) {
                String[] itemArray = array[i].split(";");
                for (int j = 0; j < itemArray.length; j++) {
                    formBodyBuilder.add(itemArray[0], itemArray[1]);
                }
            }
            formBodyBuilder.build();
            RequestBody requestBody = formBodyBuilder.build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            Call call = okHttpClient.newCall(request);
            Response response = call.execute();
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 该不会开启异步线程。
     * @param request
     * @return
     * @throws IOException
     */
    public static Response execute(Request request) throws IOException{
        return okHttpClient.newCall(request).execute();
    }

    /**
     * 开启异步线程访问网络
     * @param request
     * @param responseCallback
     */
    public static void enqueue(Request request, Callback responseCallback){
        okHttpClient.newCall(request).enqueue(responseCallback);
    }

}
