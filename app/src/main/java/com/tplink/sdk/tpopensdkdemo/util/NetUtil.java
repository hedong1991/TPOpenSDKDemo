package com.tplink.sdk.tpopensdkdemo.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * 网络操作类.
 * 
 * @Description 用于网络的POST 、 GET 、 MutilPart等操作
 * @author 何栋
 * @version 1.0
 * @date 2018年10月23日
 *
 */
public class NetUtil {

	/** 本地文件上传失败 */
	public static final String EXCEPTION_UPLOAD_ERROR_STATUS = "805";
//	private static OSSClient oss;

	/** 返回结果 */
	private String result;
	/** 输入流 */
	private BufferedInputStream bis;
	/** http链接 */
	private HttpURLConnection mConnection;
	/** 输出流 */
	private DataOutputStream dos;

	/**
	 * 获取当前网络链接状态.
	 * 
	 * @version 1.0
	 * @updateInfo (此处输入修改内容,若无修改可不写.)
	 * 
	 * @param context
	 *        上下文
	 * @return true网络可用，false网络不可用
	 */
	public static boolean isNetworkAvailable(Context context) {
		if (context == null) {
			return false;
		}
		ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE); // 获取网络服务
		if (connectivity == null) {
			// 判断网络服务是否为空
			return false;
		} else {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				NetworkCapabilities networkCapabilities = connectivity.getNetworkCapabilities(connectivity.getActiveNetwork());
				if (networkCapabilities != null) {//&& networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
					return true;
				}
			} else {
				// 判断当前是否有任意网络服务开启
				NetworkInfo[] info = connectivity.getAllNetworkInfo();
				if (info != null) {
					for (int i = 0; i < info.length; i++) {
						if (info[i].getState() == NetworkInfo.State.CONNECTED) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public static boolean isNetworkConnected(Context context) {
		if (context != null) {
			ConnectivityManager mConnectivityManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
			if (mNetworkInfo != null) {
				return mNetworkInfo.isAvailable();
			}
		}
		return false;
	}

	public static int getNetworkStatus() {
		Runtime runtime = Runtime.getRuntime();
		try {
			Process p = runtime.exec("ping -c 3 www.baidu.com");
			int ret = p.waitFor();
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 1;
	}

	public static void openWifi(Context context) {
		// 取得WifiManager对象
		WifiManager mWifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		if (!mWifiManager.isWifiEnabled()) {
			mWifiManager.setWifiEnabled(true);
		}
	}

	/**
	 * 将发送请求的参数构造为指定格式.
	 * 
	 * @version 1.0
	 * @updateInfo (此处输入修改内容,若无修改可不写.)
	 * 
	 * @param attribute
	 *        发送请求的参数,key为属性名,value为属性值.
	 * @return 指定格式的请求参数.
	 */
	private String getParams(HashMap<String, String> attribute) {
		Set<String> keys = attribute.keySet(); // 获取所有参数名
		Iterator<String> iterator = keys.iterator(); // 将所有参数名进行跌代
		StringBuffer params = new StringBuffer();
		// 取出所有参数进行构造
		while (iterator.hasNext()) {
			try {
				String key = iterator.next();
				@SuppressWarnings("deprecation")
                String param = key + "=" + URLEncoder.encode(attribute.get(key)) + "&";
				params.append(param);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		// 返回构造结果
		return params.toString().substring(0, params.toString().length() - 1);
	}

	/**
	 * 关闭链接与所有从链接中获得的流.
	 * 
	 * @version 1.0
	 * @updateInfo (此处输入修改内容,若无修改可不写.)
	 * 
	 * @throws IOException
	 *         关闭发生错误时抛出IOException.
	 */
	private void closeConnection() throws IOException {
		if (bis != null) {
			bis.close();
		}
		if (dos != null) {
			dos.close();
		}
		if (mConnection != null) {
			mConnection.disconnect();
		}
	}

	/**
	 * 下载文件,下载文件存储至指定路径.
	 * 
	 * @version 1.0
	 * @updateInfo (此处输入修改内容,若无修改可不写.)
	 * 
	 * @param path
	 *        下载路径.
	 * @param savePath
	 *        存储路径.
	 * @return 下载成功返回true,若下载失败则返回false.
	 * @throws MalformedURLException
	 *         建立连接发生错误抛出MalformedURLException.
	 * @throws IOException
	 *         下载过程产生错误抛出IOException.
	 */
	public boolean downloadFile(String path, String savePath) throws MalformedURLException, IOException {
		File file = null;
		InputStream input = null;
		OutputStream output = null;
		boolean success = false;
		try {
			URL url = new URL(path);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			conn.connect();
			int code = conn.getResponseCode();
			if (code == HttpURLConnection.HTTP_OK) {
				input = conn.getInputStream();
				file = new File(savePath);
				file.createNewFile(); // 创建文件
				output = new FileOutputStream(file);
				byte buffer[] = new byte[1024];
				int read = 0;
				while ((read = input.read(buffer)) != -1) { // 读取信息循环写入文件
					output.write(buffer, 0, read);
				}
				output.flush();
				success = true;
			} else {
				success = false;
			}
		} catch (MalformedURLException e) {
			success = false;
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
			success = false;
			throw e;
		} finally {
			try {
				output.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return success;
	}

}
