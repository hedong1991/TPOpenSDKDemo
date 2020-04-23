package com.tplink.sdk.tpopensdkdemo.util;

import android.app.Activity;
import android.app.Dialog;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.json.JSONObject;

import java.io.File;
import java.util.Map;

import okhttp3.Call;
import okhttp3.MediaType;


/**
 * 网络请求的工具类 1.请求方式 2.请求参数 3.response结果码 4.response message(信息) 5.response data
 * (数据)
 */
public class HttpRequestUtils {


    private static Dialog dialog;

    /**
     * 公共请求接口方法
     *
     * @param context       上下文(Activity)
     * @param URL           请求的网络地址
     * @param map           请求的参数(没有就传null)
     * @param RequestMethod 请求的方式(GET,PUT,POST三种)
     * @param tag           实现网络请求的方法名称
     */
    public static void httpRequest(final Activity context, final String tag,
                                   final String URL, final Map<String, String> map, final String RequestMethod,
                                   final ResponseListener listen) {
        if (!NetUtil.isNetworkAvailable(context)) {
            try {
                listen.returnException(new Exception(), "当前设备网络异常，请检查您的网络");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            // 1.首先判断请求的URL是否为空
            if (TextUtils.isEmpty(URL)) {
                return;
            }

            // 2.判断请求的方式
            if (RequestMethod == "GET") {
                OkHttpUtils.get().url(URL).params(map).addHeader("phonesystem", "ANDROID").build().execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
//                        LLog.eNetError(e.toString());
                        if (e.toString().toLowerCase().contains("timeout")) {
                            listen.returnException(e, "请求超时，请稍后重试！");
                        } else {
                            listen.returnException(e, "请求失败，请稍后重试！");
                        }
                    }

                    @Override
                    public void onResponse(String response, int id) {
//                        LLog.iNetSucceed(" 方法:" + tag + ",结果：" + response);
                        if (!TextUtils.isEmpty(response) && !response.trim().startsWith("<html>")) {
                            if (!URL.contains("?imageInfo")) {
                                response(response, context, listen);
                            } else {
                                listen.getResponseData(response);
                            }
                        } else {
                            listen.returnException(new Exception(), "请求失败，请检查您的网络是否正常");
                        }
                    }
                });
            } else if (RequestMethod == "POST") {
                OkHttpUtils.post().url(URL).params(map).addHeader("phonesystem", "ANDROID").build().execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
//                        LLog.eNetError(e.toString());
                        if (e.toString() != null && e.toString().toLowerCase().contains("timeout")) {
                            listen.returnException(e, "请求超时，请稍后重试！");
                        } else {
                            listen.returnException(e, "请求失败，请稍后重试！");
                        }
                    }

                    @Override
                    public void onResponse(String response, int id) {
//                        LLog.iNetSucceed(" 方法:" + tag + ",结果：" + response);
                        if (!TextUtils.isEmpty(response) && !response.trim().startsWith("<html>")) {
                            response(response, context, listen);
                        } else {
                            listen.returnException(new Exception(), "请求失败，请检查您的网络是否正常");
                        }
                    }
                });
            } else if (RequestMethod == "PUT") {
            } else if (RequestMethod == "DELETE") {
                String body = "";
                if (map != null) {
                    body = map.toString();
                }
                OkHttpUtils.delete().url(URL).requestBody(body).addHeader("phonesystem", "ANDROID").build().execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
//                        LLog.eNetError(e.toString());
                        if (e.toString().toLowerCase().contains("timeout")) {
                            listen.returnException(e, "请求超时，请稍后重试！");
                        } else {
                            listen.returnException(e, "请求失败，请稍后重试！");
                        }
                    }

                    @Override
                    public void onResponse(String response, int id) {
//                        LLog.iNetSucceed(" 方法:" + tag + ",结果：" + response);
                        if (!TextUtils.isEmpty(response) && !response.trim().startsWith("<html>")) {
                            response(response, context, listen);
                        } else {
                            listen.returnException(new Exception(), "请求失败，请检查您的网络是否正常");
                        }
                    }
                });
            } else {
                toast(context, "请求方式错误");
                return;
            }
        } catch (final Exception e) {
            listen.returnException(new Exception(), "系统异常，请联系工作人员！");
        }
    }

    /**
     * 使用okhttp-utils上传多个或者单个文件
     */
    public static void multiFileUpload(final Activity context, File file, String url, String name, final ResponseListener listen)
    {
        if (!file.exists())
        {
            context.runOnUiThread(() -> {
                Toast.makeText(context, "文件不存在，请修改文件路径", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        OkHttpUtils.post()//
                .addFile("file", "lc"+name+".jpg", file)//
                .url(url)
                .build()//
                .execute(new StringCallback() {

                    @Override
                    public void onError(Call call, Exception e, int id) {
                        listen.returnException(e, "上次截图失败");
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        listen.getResponseData(response);
                    }
                });
    }

    private static void response(String response, Activity context, ResponseListener listen) {
        listen.getResponseData(response);
    }

    /**
     * 新的接口请求
     *
     * @param context       上下文(Activity)
     * @param URL           请求的网络地址
     * @param RequestMethod 请求的方式(GET,PUT,POST三种)
     * @param tag           实现网络请求的方法名称
     */
    public static void httpRequestNew(final Activity context, final String tag,
                                      final String URL, JSONObject object, final String RequestMethod,
                                      final ResponseListener listen) {
        if (!NetUtil.isNetworkAvailable(context)) {
            try {
                listen.returnException(new Exception(), "当前设备网络异常，请检查您的网络");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

            // 2.判断请求的方式
            String mediaType = "application/x-www-form-urlencoded";
//            String sign = getUrlString(map);
//            long currentTime = System.currentTimeMillis();
            String content = "";
            if (object != null) {
                mediaType = "application/json";
                content = object.toString();
            }
//            LLog.iNetSucceed(" 方法:" + tag + ",URL:" + URL + ",方式：" + RequestMethod + ",参数：" + content);
//            LLog.iNetSucceed("sign="+(sign+"timestamp="+ currentTime));
//            ToastUtil.showLongToast(context, "请求接口");
            OkHttpUtils.postString().url(URL).content(content).mediaType(MediaType.parse(mediaType)).addHeader("phoneSystem", "ANDROID").build().execute(new StringCallback() {
                @Override
                public void onError(Call call, Exception e, int id) {
//                    LLog.eNetError(e.toString());
                    if (e.toString() != null && e.toString().toLowerCase().contains("timeout")) {
                        listen.returnException(e, "请求超时，请稍后重试！");
                    } else {
                        listen.returnException(e, "请求失败，请稍后重试！");
                    }
                }

                @Override
                public void onResponse(String response, int id) {
//                    LLog.iNetSucceed(" 方法:" + tag + ",结果：" + response);
                    if (!TextUtils.isEmpty(response) && !response.trim().startsWith("<html>"))
                        response(response, context, listen);
                    else
                        listen.returnException(new Exception(), "请求失败，请检查您的网络是否正常");
                }
            });
    }

    public interface ResponseListener {
        /**
         * @param response 返回的JSONObject数据
         */
        public void getResponseData(String response);

        public void returnException(Exception e, String msg);
    }

    protected static void toast(final Activity context, final String id) {
        context.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(context, id, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
