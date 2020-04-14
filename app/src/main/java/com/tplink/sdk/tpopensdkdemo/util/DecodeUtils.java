package com.tplink.sdk.tpopensdkdemo.util;

import android.app.Activity;
import android.util.Base64;
import android.view.Window;
import android.view.WindowManager;

/**
 * 加密解密的工具
 *
 * Created by 何栋 on 2017/10/15.
 * 294663966@qq.com
 * 
 */
public class DecodeUtils {
	/**
	 * 
	 * 加密
	 * 
	 * 
	 * 
	 * @param targetStr
	 * 
	 * @return
	 */
	public static String encode(String targetStr) {
		return Base64.encodeToString(targetStr.getBytes(), Base64.DEFAULT);
	}

	/**
	 * 
	 * 解密
	 * 
	 * 
	 * 
	 * @param targetStr
	 * 
	 * @return
	 */
	public static String decode(String targetStr) {
		return new String(Base64.decode(targetStr.getBytes(), Base64.DEFAULT));
	}

	public static Class getclass(String className)// className是包括包路径的类名
	{
		Class obj = null; // 以String类型的className实例化类
		try {
			obj = Class.forName(className);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return obj;
	}

	/**
	 * 是否使屏幕常亮
	 *
	 * @param activity
	 */
	public static void keepScreenLongLight(Activity activity) {
		Window window = activity.getWindow();
//		if (isOpenLight) {
			window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//		} else {
//			window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//		}
	}
}
