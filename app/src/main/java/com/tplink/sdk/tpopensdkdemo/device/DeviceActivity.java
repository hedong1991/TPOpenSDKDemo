package com.tplink.sdk.tpopensdkdemo.device;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.tplink.sdk.tpopensdk.openctx.IPCDevice;
import com.tplink.sdk.tpopensdk.openctx.IPCDeviceContext;
import com.tplink.sdk.tpopensdk.openctx.IPCReqListener;
import com.tplink.sdk.tpopensdk.openctx.IPCReqResponse;
import com.tplink.sdk.tpopensdkdemo.R;
import com.tplink.sdk.tpopensdkdemo.bean.IPCDeviceNew;
import com.tplink.sdk.tpopensdkdemo.common.BaseActivity;
import com.tplink.sdk.tpopensdkdemo.player.PreviewActivity;
import com.tplink.sdk.tpopensdkdemo.util.ACache;
import com.tplink.sdk.tpopensdkdemo.util.SharedPreferencesUtils;

import java.util.ArrayList;

/**
 * Copyright (C), 2018, TP-LINK TECHNOLOGIES CO., LTD.
 *
 * @author caizhenghe
 * @ClassName: DeviceActivity
 * @Description: Version 1.0.0, 2018-09-30, caizhenghe create file.
 */

public class DeviceActivity extends BaseActivity implements DeviceListAdapter.OnDeviceClickListner,
        DeviceListAdapter.OnDeviceSetClickListener, DeviceListAdapter.onNVRMoreClickListener {

    private RecyclerView mDeviceListView;
    private DeviceListAdapter mDeviceListAdapter;
    public static ArrayList<IPCDevice> deviceList;
    private IPCDeviceContext mDevCtx;
    private SharedPreferencesUtils preferencesUtils;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferencesUtils = SharedPreferencesUtils.getInstance(this);
        String deviceSize = preferencesUtils.loadString("deviceSize");
        if (!TextUtils.isEmpty(deviceSize) && Integer.parseInt(deviceSize) > 0) {
            deviceList = new ArrayList<>();
            for (int i=0; i<Integer.parseInt(deviceSize); i++) {
                deviceList.add(preferencesUtils.loadObjectData(IPCDevice.class));
            }
        }
        initData();
        setContentView(R.layout.activity_device);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDeviceListAdapter.notifyDataSetChanged();
    }

    private void initData() {
        /* UI线程应该不做太多阻塞操作，appRepStart暂时操作不算多，先同步启动 */
        mSDKCtx.appReqStart(true, null);
        mDevCtx = mSDKCtx.getDevCtx();
    }

    private void initView() {
        findViewById(R.id.title_bar_left_tv).setVisibility(View.INVISIBLE);
        TextView title = findViewById(R.id.title_bar_center_tv);
        title.setText("设备列表");
        TextView mRightTv = findViewById(R.id.title_bar_right_tv);
        mRightTv.setText(getString(R.string.device_add_device));
        mRightTv.setVisibility(View.VISIBLE);
        findViewById(R.id.title_bar_right_tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeviceAddActivity.startActivity(DeviceActivity.this);
            }
        });
        mDeviceListView = findViewById(R.id.device_list_recyclerview);
        if (deviceList == null) {
            deviceList = new ArrayList<>();
        }
        mDeviceListAdapter = new DeviceListAdapter(this, deviceList, this, this, this);
        mDeviceListView.setAdapter(mDeviceListAdapter);
        mDeviceListView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public void onDeviceClicked(int position) {
        PreviewActivity.startActivity(this, deviceList.get(position));
    }

    @Override
    public void onDevSetClick(int position) {
        final int iPosition = position;
        mDevCtx.reqLoadSetting(deviceList.get(iPosition), new IPCReqListener() {
            @Override
            public int callBack(final IPCReqResponse ipcReqResponse) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissLoading();
                        if (ipcReqResponse.mError == 0) {
                            DeviceSettingActivity.startActivity(DeviceActivity.this, deviceList.get(iPosition));
                        }
                        else {
                           showToast("response: error: " + ipcReqResponse.mError + "\nrval: " + ipcReqResponse.mRval);
                        }
                    }
                });
                return 0;
            }
        });
        showLoading(null);
    }

    @Override
    public void onNVRMoreClick(int position) {
        DevChannelActivity.startActivity(this, deviceList.get(position));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (deviceList != null && deviceList.size()>0) {
            for (int i=0; i<deviceList.size(); i++) {

            }
//            aCache.put("deviceList", list, 900000000);
        }
    }
}
