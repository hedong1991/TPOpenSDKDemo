package com.tplink.sdk.tpopensdkdemo.device;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.tplink.sdk.tpopensdk.bean.IPCDeviceDefines.IPCDevInfo;
import com.tplink.sdk.tpopensdk.openctx.IPCDevice;
import com.tplink.sdk.tpopensdk.openctx.IPCDeviceContext;
import com.tplink.sdk.tpopensdk.openctx.IPCDiscoverContext;
import com.tplink.sdk.tpopensdk.openctx.IPCReqData;
import com.tplink.sdk.tpopensdk.openctx.IPCReqResponse;
import com.tplink.sdk.tpopensdkdemo.R;
import com.tplink.sdk.tpopensdkdemo.bean.IPCDiscoverDevBean;
import com.tplink.sdk.tpopensdkdemo.common.BaseActivity;
import com.tplink.sdk.tpopensdkdemo.util.DecodeUtils;
import com.zhy.http.okhttp.OkHttpUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends BaseActivity
        implements SwipeRefreshLayout.OnRefreshListener, DeviceAutoAddListAdapter.AddDeviceListener,
        View.OnClickListener {
    private IPCDiscoverContext mDiscoverCtx;
    private IPCDeviceContext mDevCtx;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecycleView;
    private Handler mHandler;

    private DeviceAutoAddListAdapter mDevListAdatper;
    private ArrayList<IPCDiscoverDevBean> mDevInfoList;
    private IPCDevice mToAddDev;
    private int mAddPosition;
    private boolean mModifyPort;
    private int mPort;
    private String mPwd;
    String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
            Manifest.permission.WRITE_SETTINGS,
            Manifest.permission.CHANGE_CONFIGURATION
    };
    private TextView tvMsg;
    private ArrayList<String> list = new ArrayList<>();
    HashMap<String, String> map = new HashMap<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        list.add("com.tplink.sdk.tpopensdkdemo.player.PreviewActivity");
        list.add("com.tplink.sdk.tpopensdkdemo.player.PreviewActivity2");
        list.add("com.tplink.sdk.tpopensdkdemo.player.PreviewActivity3");
        list.add("com.tplink.sdk.tpopensdkdemo.player.PreviewActivity4");
        list.add("com.tplink.sdk.tpopensdkdemo.player.PreviewActivity5");
        list.add("com.tplink.sdk.tpopensdkdemo.player.PreviewActivity6");
        list.add("com.tplink.sdk.tpopensdkdemo.player.PreviewActivity7");
        list.add("com.tplink.sdk.tpopensdkdemo.player.PreviewActivity8");
        list.add("com.tplink.sdk.tpopensdkdemo.player.PreviewActivity9");
        list.add("com.tplink.sdk.tpopensdkdemo.player.PreviewActivity10");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initData();
        setContentView(R.layout.activity_device_auto_discover);
        initView();
        initHttp();
        if (!EasyPermissions.hasPermissions(this, permissions)) {
            EasyPermissions.requestPermissions(this, "请授权读写相册相关权限",
                    1001, permissions);
        }

        DecodeUtils.keepScreenLongLight(this);
    }
    public void initHttp() {

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
//                .addInterceptor(new LoggerInterceptor("TAG"))
                .connectTimeout(30000L, TimeUnit.MILLISECONDS)
                .readTimeout(30000L, TimeUnit.MILLISECONDS)
                //其他配置
                .build();

        OkHttpUtils.initClient(okHttpClient);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case DeviceAddEnterInfoActivity.REQUEST_CODE_DEVICE_ADD_INPUT_INFO:
                if (resultCode == DeviceAddEnterInfoActivity.RESULT_CODE_PWD) {
                    mPwd = data.getStringExtra(DeviceAddEnterInfoActivity.DEVICE_ADD_ENTER_INFO);
                    addDevice(mToAddDev);
                }
                if (resultCode == DeviceAddEnterInfoActivity.RESULT_CODE_PORT) {
                    mPort = Integer.valueOf(data.getStringExtra(DeviceAddEnterInfoActivity.DEVICE_ADD_ENTER_INFO));
                    addDevice(mToAddDev);
                }
                break;
        }
    }

    /************
     * common
     ***********/
    private void initData() {
        /* UI线程应该不做太多阻塞操作，appRepStart暂时操作不算多，先同步启动 */
        mSDKCtx.appReqStart(true, null);
        mDiscoverCtx = mSDKCtx.getDiscoverCtx();
        mDevCtx = mSDKCtx.getDevCtx();
        mDevInfoList = new ArrayList<>();
        mDevListAdatper = new DeviceAutoAddListAdapter(this, mDevInfoList, this);
        mHandler = new Handler();
        mModifyPort = false;
        mPwd = "123456";
        mPort = 80;
    }

    private void initView() {
        ((TextView)findViewById(R.id.title_bar_center_tv)).setText("设备列表");
        findViewById(R.id.title_bar_right_tv).setVisibility(View.INVISIBLE);
        tvMsg = findViewById(R.id.tvMsg);
        mSwipeRefreshLayout = findViewById(R.id.device_add_auto_discover_swiperefreshlayout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.text_blue_dark);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.post(() -> {
            mSwipeRefreshLayout.setRefreshing(true);
            startDiscover();
        });
        mRecycleView = findViewById(R.id.device_auto_discover_recyclerview);
        mRecycleView.setAdapter(mDevListAdatper);
        mRecycleView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        startDiscover();
    }

    @Override
    public void onAddDeviceClicked(final int position) {
        IPCDevice dev = new IPCDevice(mDevCtx.initDevWithInfo(mDevInfoList.get(position).mDevInfo));
        mToAddDev = dev;
        mAddPosition = position;
        addDevice(dev);
    }

    private void addDevice(final IPCDevice dev) {
        mDevCtx.reqLogin(dev, mPwd, ipcReqResponse -> {
            if (ipcReqResponse.mError == 0) {
                mDevCtx.reqConnectDev(dev, ipcReqResponse1 -> {
                    if (ipcReqResponse1.mError == 0) {
                        mHandler.post(() -> {
                            dismissLoading();
                            boolean isNew = false;
                            String activity = map.get(dev.getAddress());
                            if (TextUtils.isEmpty(activity)) {
                                if (list.size() == 0) {
                                    Toast.makeText(this, "最多只能操作10台摄像机", Toast.LENGTH_SHORT);
                                    return;
                                }
                                activity = list.get(0);
                                map.put(dev.getAddress(), activity);
                                isNew = true;
                            }
                            Intent intent = new Intent(MainActivity.this, DecodeUtils.getclass(activity));
                            intent.putExtra("EXTRA_DEVICE", dev);
                            MainActivity.this.startActivity(intent);
                            if (isNew)
                                list.remove(0);
//                                        mDevInfoList.get(mAddPosition).mIsAdded = true;
//                                        mDevListAdatper.notifyItemChanged(mAddPosition);
                        });
                    } else {
                        runOnUiThread(() -> dismissLoading());
                    }
                    return 0;
                });
            } else if (ipcReqResponse.mRval == -40401) {
                // TODO: 底层的常量应该在SDK中暴露
                mHandler.post(() -> {
                    dismissLoading();
                    DeviceAddEnterInfoActivity.startActivity(MainActivity.this,
                            DeviceAddEnterInfoActivity.RESULT_CODE_PWD);
                });
            } else {
                runOnUiThread(() -> {
                    dismissLoading();
                    if (!mModifyPort) {
                        // TODO: 2018/10/21 底层login暂时没有提供修改端口的方法
                        /* 可能是端口错误，提示修改端口号 */
                        mModifyPort = true;
                        DeviceAddEnterInfoActivity.startActivity(MainActivity.this,
                                DeviceAddEnterInfoActivity.RESULT_CODE_PORT);
                    } else {
                        showToast("response: error: " + ipcReqResponse.mError + "\nrval: " + ipcReqResponse.mRval);
                    }
                });
            }
            return 0;
        });
        showLoading(null);
    }

    private void startDiscover() {
        mDevInfoList.clear();
        mDevListAdatper.notifyDataSetChanged();
        mDiscoverCtx.reqScannedList(ipcReqResponse -> {
            final IPCReqResponse response = ipcReqResponse;
            mHandler.post(() -> {
                if (response.mError == 0) {
                    final IPCReqData<ArrayList<IPCDevInfo>> reqData = (IPCReqData<ArrayList<IPCDevInfo>>)response;
                    for(IPCDevInfo dev : reqData.mData) {
                        mDevInfoList.add(new IPCDiscoverDevBean(dev, false));
                    }
                    mDevListAdatper.notifyDataSetChanged();
                }
                if (mDevInfoList.size() == 0) {
                    tvMsg.setVisibility(View.VISIBLE);
                } else {
                    tvMsg.setVisibility(View.GONE);
                }
                mSwipeRefreshLayout.setRefreshing(false);
            });
            return 0;
        }, 15000, "0.0.0.0");
    }

    /****************
     * Entrance
     ****************/
    public static void startActivity(Activity activity) {
        Intent intent = new Intent(activity, MainActivity.class);
        activity.startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent();// 创建Intent对象
            intent.setAction(Intent.ACTION_MAIN);// 设置Intent动作
            intent.addCategory(Intent.CATEGORY_HOME);// 设置Intent种类
            startActivity(intent);// 将
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
