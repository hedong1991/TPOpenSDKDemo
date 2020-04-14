package com.tplink.sdk.tpopensdkdemo.player;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.tplink.applibs.util.TPByteArrayJNI;
import com.tplink.foundation.TPLog;
import com.tplink.media.TPAudioRecorder;
import com.tplink.media.jni.JNITPAVFrameQueue;
import com.tplink.sdk.tpopensdk.TPOpenSDK;
import com.tplink.sdk.tpopensdk.TPPlayer;
import com.tplink.sdk.tpopensdk.bean.IPCDeviceDefines;
import com.tplink.sdk.tpopensdk.common.TPSDKCommon;
import com.tplink.sdk.tpopensdk.openctx.IPCDevice;
import com.tplink.sdk.tpopensdkdemo.R;
import com.tplink.sdk.tpopensdkdemo.common.TPUtils;
import com.tplink.sdk.tpopensdkdemo.common.TPViewUtils;
import com.tplink.sdk.tpopensdkdemo.common.TouchButton;
import com.tplink.sdk.tpopensdkdemo.device.MainActivity;
import com.tplink.sdk.tpopensdkdemo.util.HttpRequestUtils;
import com.tplink.sdk.tpopensdkdemo.util.SharedPreferencesUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import pub.devrel.easypermissions.EasyPermissions;


/**
 * Copyright (C), 2018, TP-LINK TECHNOLOGIES CO., LTD.
 *
 * @author caizhenghe
 * @ClassName: PreviewActivity
 * @Description: Version 1.0.0, 2018-09-30, caizhenghe create file.
 */

public class PreviewActivity extends PlayerActivity {
    private static final int PAGE_TYPE_INVALID = 0;
    private static final int PAGE_TYPE_TALK = 1;
    private static final int PAGE_TYPE_SPEAK = 2;
    private static final int PAGE_TYPE_CLOUD = 3;
    private static final int PAGE_TYPE_PRESET = 4;

    private TPPlayer mPlayerForMicrophone;
    private TPAudioRecorder mAudioRecorder;
    private PresetAdapter mPresetAdapter;
    private List<IPCDeviceDefines.IPCPreset> mPresetList;

    private int mDuplexState;
    private boolean mIsDuplexConnected;
    private int mSecondPageType;

    private ImageView mPresetBtn, mCloudBtn, mCruiseBtn, mTalkBtn, mSpeakBtn;
    private RecyclerView mPresetListView;
    String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
            Manifest.permission.WRITE_SETTINGS,
            Manifest.permission.CHANGE_CONFIGURATION
    };
    private Timer timer;
    private SharedPreferencesUtils preferencesUtils;

    public static void startActivity(Context context, IPCDevice device) {
        Intent intent = new Intent(context, PreviewActivity.class);
        intent.putExtra(EXTRA_DEVICE, device);
        context.startActivity(intent);
    }

    @Override
    public void doClick(View v) {
        super.doClick(v);
        int xCoord = 0, yCoord = 0;
        boolean isCloudOpt = false;
        switch (v.getId()) {
            case R.id.title_bar_left_tv:
                MainActivity.startActivity(this);
//                moveTaskToBack(true);
                break;
            case R.id.player_play_btn:
                togglePlay();
                break;
            case R.id.player_sound_btn:
                toggleSound();
                break;
            case R.id.player_quality_btn:
                toggleQuality();
                break;
            case R.id.player_snapshot_btn:
                if (mPlayer == null)
                    return;
                String snapshotUri = mSdkDir.toString() + getString(R.string.prefix_snapshot) + System.currentTimeMillis() / 1000 + getString(R.string.suffix_jpg);
                mPlayer.snapshot(snapshotUri);
                Toast.makeText(this, "snapshot uri: " + snapshotUri, Toast.LENGTH_SHORT).show();
                try {
                    Thread.sleep(1000l);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                uploadFile(new File(snapshotUri));
                break;
            case R.id.player_record_btn:
                toggleRecord();
                break;
            case R.id.player_cruise_btn:
                toggleCruise();
                break;
            case R.id.player_cloud_btn:
                setSecondPageVisible(PAGE_TYPE_CLOUD, true);
                break;
            case R.id.player_preset_btn:
                setSecondPageVisible(PAGE_TYPE_PRESET, true);
                break;
            case R.id.title_bar_right_tv:
                PlaybackActivity.startActivity(this, mDevice);
                break;
            case R.id.player_talk_btn:
                initMicrophonePlayer();
                setSecondPageVisible(PAGE_TYPE_TALK, true);
                // start full duplex
                mPlayerForMicrophone.startVoiceTalk(mDevice, TPSDKCommon.TalkMode.TPPLAYER_TALK_HALF_DUPLEX);
                mDuplexState = TPSDKCommon.TalkMode.TPPLAYER_TALK_HALF_DUPLEX;
                break;
            case R.id.player_speak_btn:
                initMicrophonePlayer();
                setSecondPageVisible(PAGE_TYPE_SPEAK, true);
                // start half duplex
                mPlayerForMicrophone.startVoiceTalk(mDevice, TPSDKCommon.TalkMode.TPPLAYER_TALK_FULL_DUPLEX);
                mDuplexState = TPSDKCommon.TalkMode.TPPLAYER_TALK_FULL_DUPLEX;
                break;
            case R.id.player_second_pack_up_iv:
                if (mSecondPageType == PAGE_TYPE_TALK || mSecondPageType == PAGE_TYPE_SPEAK) {
                    stopMicrophonePlayer();
                } else {
                    setSecondPageVisible(mSecondPageType, false);
                }
                break;
            case R.id.player_second_speaking_btn:
                stopMicrophonePlayer();
                break;
            case R.id.player_second_up_btn:
                yCoord = 10;
                isCloudOpt = true;
                break;
            case R.id.player_second_down_btn:
                yCoord = -10;
                isCloudOpt = true;
                break;
            case R.id.player_second_left_btn:
                xCoord = -10;
                isCloudOpt = true;
                break;
            case R.id.player_second_right_btn:
                xCoord = 10;
                isCloudOpt = true;
                break;
        }
        if (isCloudOpt)
            mDeviceContext.reqMotorMoveBy(mDevice, ipcReqResponse -> 0, xCoord, yCoord, mChannelId);
        if (v instanceof ImageView)
            updateBtnStatus((ImageView) v, true);
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_preview;
    }

    @Override
    protected void initData() {
        super.initData();
        if (!EasyPermissions.hasPermissions(this, permissions)) {
            EasyPermissions.requestPermissions(this, "请授权读写相册相关权限",
                    1001, permissions);
        }
        preferencesUtils = SharedPreferencesUtils.getInstance(this);
        fileNameStr = preferencesUtils.loadString(this.mDevice.getAddress()+"_name");
        String intervalStr = preferencesUtils.loadString(this.mDevice.getAddress()+"_interval");
        if (!TextUtils.isEmpty(intervalStr)) {
            interval = Integer.parseInt(intervalStr);
        }
        // set scan tour
        mDeviceContext.reqSetScanTour(mDevice, ipcReqResponse -> {
            if (ipcReqResponse.mError == 0) {
                mIsSetScanTour = true;
            }
            return 0;
        }, new IPCDeviceDefines.IPCScanTour(0, 100, 0, 24 * 60));

        // just get video port in whether preview or playback
        mDeviceContext.reqGetVideoPort(mDevice, ipcReqResponse -> {
            if (ipcReqResponse.mError == 0) {
                Log.d("TAG", "reqGetVideoPort success!");
                mIsGetVideoPort = true;
                mMainHandler.post(() -> play());
            }
            return 0;
        });

        // preset list
        mPresetList = new ArrayList<>();
        mPresetAdapter = new PresetAdapter(this, mPresetList, position -> {
            Toast.makeText(PreviewActivity.this, "Preset " + position, Toast.LENGTH_SHORT).show();
            mDeviceContext.reqPresetGoto(mDevice, ipcReqResponse -> {
                TPLog.d("TAG", "reqPresetGoto::Error = " + ipcReqResponse.mError);
                return 0;
            }, mPresetList.get(position).getID(), mChannelId);
        });
    }

    @Override
    protected void startCruise() {
        super.startCruise();
        /** tourType：0：未设置， 1：扫描巡航 2：多点布防 */
        mDeviceContext.reqSetTourInfo(mDevice, ipcReqResponse -> {
            if (ipcReqResponse.mError == 0) {
                mIsCruise = true;
                updateBtnStatus(mCruiseBtn, true);
            }
            return 0;
        }, 1, TPSDKCommon.TourType.IPC_TOUR_TYPE_SCAN_TOUR);

    }

    @Override
    protected void stopCruise() {
        super.stopCruise();
        mDeviceContext.reqSetTourInfo(mDevice, ipcReqResponse -> {
            if (ipcReqResponse.mError == 0) {
                mIsCruise = false;
                updateBtnStatus(mCruiseBtn, true);
            }
            return 0;
        }, 0, 1);

    }

    @Override
    protected void play() {
        super.play();
        if (mIsGetVideoPort && !mIsPlay) {
            mIsPlay = true;
            updateBtnStatus(mPlayBtn, true);
            mPlayer.startRealPlay(mDevice, TPSDKCommon.Quality.QUALITY_CLEAR);
        } else {
            TPLog.d("TAG", "Debug! not get video port");
        }
    }

    @Override
    protected void stop() {
        // 在关闭player之前先关闭microphonePlayer
        stopMicrophonePlayer();
        super.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        automaticScreenshot = false;
//        handle = null;
        timer.cancel();
        timer = null;
    }

    private void initMicrophonePlayer() {
        mPlayerForMicrophone = TPOpenSDK.getInstance().createPlayer(this);
        mPlayerForMicrophone.setPlayerCallback(new TPPlayer.PlayerCallback() {
            @Override
            public int onPlayStatusChange(int status, int errorCode) {
                switch (status) {
                    case TPSDKCommon.MPStatus.IPCMP_STATUS_PLAYING:
                        boolean isCreateFail = setAudioRecorderEnable(true);
                        if (isCreateFail) {
                            mMainHandler.post(() -> Toast.makeText(PreviewActivity.this, getString(R.string.talk_connection_error), Toast.LENGTH_SHORT).show());
                        } else {
                            if (mDuplexState == TPSDKCommon.TalkMode.TPPLAYER_TALK_FULL_DUPLEX) {
                                updateDuplexButtonStatus(true, true);
                                // auto start speak in full duplex
                                mPlayerForMicrophone.startSpeak();
                            } else {
                                updateDuplexButtonStatus(false, true);
                            }
                            // record talk connect status in half duplex
                            mIsDuplexConnected = true;
                        }

                        break;
                    default:
                        setAudioRecorderEnable(false);
                        updateDuplexButtonStatus(mDuplexState == TPSDKCommon.TalkMode.TPPLAYER_TALK_FULL_DUPLEX, false);
                        mIsDuplexConnected = false;
                        break;
                }

                return 0;
            }

            @Override
            public int onRecordStatusChange(int i, int i1, String s) {
                return 0;
            }

            @Override
            public int onSnapshot(int i, String s) {
                return 0;
            }

            @Override
            public int onPlayTimeUpdate(long l) {
                return 0;
            }

            @Override
            public int onRecordDurationUpdate(long l) {
                return 0;
            }

            @Override
            public int onDataStatistics(long l, double v) {
                return 0;
            }

            @Override
            public int onChangeQuality(int i) {
                return 0;
            }

            @Override
            public int onDataRecv(TPByteArrayJNI byteArray) {
                return 0;
            }
        });
    }

    private void stopMicrophonePlayer() {
        if (mPlayerForMicrophone == null)
            return;
        mPlayerForMicrophone.stopSpeak();
        mPlayerForMicrophone.stopVoiceTalk();
        setSecondPageVisible(mSecondPageType, false);
    }

    private boolean setAudioRecorderEnable(boolean enable) {
        boolean createFail = false;
        if (enable) {
            TPLog.d("TAG", "microphone playing");
            try {
                if (mAudioRecorder != null) {
                    mAudioRecorder.stop();
                    mAudioRecorder = null;
                }
                if (mAudioRecorder == null) {
                    int sampleRate = mPlayerForMicrophone.getAudioSampleRate4Talk();
                    mAudioRecorder = TPAudioRecorder.newInstanse(new JNITPAVFrameQueue(mPlayerForMicrophone.getFrameQueue4Talk()), sampleRate);
                    mAudioRecorder.start();
                }
            } catch (Exception e) {
                TPLog.e("TAG", "Failed to create audio recorder");
                mAudioRecorder = null;
                createFail = true;
            }
        } else {
            TPLog.d("TAG", "microphone stop ");
            if (mAudioRecorder != null) {
                mAudioRecorder.stop();
                mAudioRecorder = null;
            }
        }
        return createFail;
    }

    @Override
    protected void initView() {
        super.initView();
        if (TPUtils.isLandscape(this))
            return;
        mPlayBtn = findViewById(R.id.player_play_btn);
        mSoundBtn = findViewById(R.id.player_sound_btn);
        mQualityBtn = findViewById(R.id.player_quality_btn);
        mOrientationBtn = findViewById(R.id.player_orientation_btn);

        mTimeTv = findViewById(R.id.player_time_tv);
        mFlowTv = findViewById(R.id.player_flow_tv);
        mRecordDurationTv = findViewById(R.id.player_record_duration_tv);

        mSnapshotBtn = findViewById(R.id.player_snapshot_btn);
        mRecordBtn = findViewById(R.id.player_record_btn);
        mCloudBtn = findViewById(R.id.player_cloud_btn);
        mPresetBtn = findViewById(R.id.player_preset_btn);
        mTalkBtn = findViewById(R.id.player_talk_btn);
        mSpeakBtn = findViewById(R.id.player_speak_btn);
        mCruiseBtn = findViewById(R.id.player_cruise_btn);

        // preset recycler view
        mPresetListView = findViewById(R.id.player_second_preset_list);
        RecyclerView.LayoutManager manager = new GridLayoutManager(this, 3);
        mPresetListView.setLayoutManager(manager);
        mPresetListView.setAdapter(mPresetAdapter);

        TouchButton touchButton = findViewById(R.id.player_second_talking_btn);
        if (touchButton != null) {
            touchButton.setCallback(new TouchButton.OnUpdateButtonStatus() {
                @Override
                public void onReleaseButton(View v) {
                    if (mDuplexState == TPSDKCommon.TalkMode.TPPLAYER_TALK_HALF_DUPLEX) {
                        // in half duplex, stop speak when touch up
                        mPlayerForMicrophone.stopSpeak();
                    }
                }

                @Override
                public void onTouchButton(View v) {
                    if (mDuplexState == TPSDKCommon.TalkMode.TPPLAYER_TALK_HALF_DUPLEX && mIsDuplexConnected) {
                        // in half duplex, start speak if connected when touch down
                        mPlayerForMicrophone.startSpeak();
                    }
                }
            });
        }


        // 显示控制台支持的按钮
        updateSupportedBtn();

        // 刷新所有按钮状态
        updateBtnStatus();

        // 刷新二级页面
        setSecondPageVisible(mSecondPageType, mSecondPageType != PAGE_TYPE_INVALID);

    }


    @Override
    protected void initTitleBar() {
        super.initTitleBar();
        TPViewUtils.setText(mTitleCenter, getString(R.string.preview));
        TPViewUtils.setVisible(mTitleRight, View.VISIBLE);
        TPViewUtils.setText(mTitleRight, "开启自动截图");
        mTitleRight.setOnClickListener(view -> {
            if (mTitleRight.getText().toString().trim().equals("开启自动截图")) {
                showDialog();
            } else {
                TPViewUtils.setText(mTitleRight, "开启自动截图");
                automaticScreenshot = false;
                Toast.makeText(PreviewActivity.this, "自动截图已关闭", Toast.LENGTH_LONG).show();
            }
        });
    }

    private Dialog dialog;
    private int interval = 10000;
    private String fileNameStr = "";
    private boolean automaticScreenshot = false;
    private void showDialog() {
        dialog = new Dialog(this, R.style.DialogTheme);
        dialog.setContentView(R.layout.dialog_setting);
        dialog.setCancelable(true);
        Window win = dialog.getWindow();
        win.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams lp = win.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        win.setGravity(Gravity.CENTER);
        win.setAttributes(lp);
        final EditText etType = dialog.findViewById(R.id.etType);
        etType.setText(""+(interval/1000));
        final EditText etFileName = dialog.findViewById(R.id.etFileName);
        if (!TextUtils.isEmpty(fileNameStr)) {
            etFileName.setText(fileNameStr);
        }
        Button tbSubmit = dialog.findViewById(R.id.tbSubmit);
        tbSubmit.setOnClickListener(view -> {
                String type = etType.getText().toString().trim();
                if (!TextUtils.isEmpty(type) && Integer.parseInt(type) > 0) {
                    interval = Integer.parseInt(type) * 1000;
                } else {
//                    if (!TextUtils.isEmpty(type) && Integer.parseInt(type) <= 0) {
//                        interval = Integer.parseInt(type) * 1000;
//                        dialog.dismiss();
//                    } else if (!TextUtils.isEmpty(type)){
//                        Toast.makeText(PreviewActivity.this, "间隔时间要大于0，单位秒", Toast.LENGTH_LONG).show();
//                    }
                }
                String fileName = etFileName.getText().toString().trim();
                if (!TextUtils.isEmpty(fileName)) {
                    fileNameStr = fileName;
                }

                preferencesUtils.saveString(this.mDevice.getAddress()+"_name", fileName);
                preferencesUtils.saveString(this.mDevice.getAddress()+"_interval", interval+"");

                dialog.dismiss();
                mTitleRight.setText("关闭自动截图");
                automaticScreenshot = true;
                Toast.makeText(PreviewActivity.this, "自动截图已开启", Toast.LENGTH_LONG).show();
                if (timer == null)
                    startTimer();
            });

        dialog.show();
    }

    private void startTimer() {
        if (interval <= 2000) {
            interval = 4000;
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {

            public void run() {
                if (!automaticScreenshot) {
                    return;
                }
                if (mPlayer == null) {
                    mIsPlay = false;
                    playTest();
                    try {
                        Thread.sleep(2000l);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (mPlayer != null) {
                    if (TextUtils.isEmpty(fileNameStr)) {
                        fileNameStr = "snapshot_"+ System.currentTimeMillis() / 1000;
                    }

                    String snapshotUri = mSdkDir.toString() +"/"+ fileNameStr + getString(R.string.suffix_jpg);
                    mPlayer.snapshot(snapshotUri);
                    Log.e("联网 截图", "文件:"+snapshotUri);
                    try {
                        Thread.sleep(2000l);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    uploadFile(new File(snapshotUri));

                    mIsPlay = true;
                    playTest();
                }
                //Your code will be here
            }
        }, 1000, interval+5000);
    }

    private void playTest() {
        runOnUiThread(() -> {
            if (mIsPlay)
                togglePlay();
            else {
                mIsGetVideoPort = true;
                mMainHandler.post(() -> play());
//                try {
//                    Thread.sleep(1000l);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                mIsClear = false;
//                toggleQuality();
            }
        });
    }

//    /**
//     * 验证码倒计时
//     */
//    Handler handle = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            if (!automaticScreenshot) {
//                return;
//            }
//            if (mPlayer != null) {
//                if (TextUtils.isEmpty(fileNameStr)) {
//                    fileNameStr = "snapshot_"+ System.currentTimeMillis() / 1000;
//                }
//
//                String snapshotUri = mSdkDir.toString() +"/"+ fileNameStr + getString(R.string.suffix_jpg);
//                mPlayer.snapshot(snapshotUri);
//                Log.e("联网 截图", "文件:"+snapshotUri);
//                try {
//                    Thread.sleep(2000l);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                uploadFile(new File(snapshotUri));
//            }
//            handle.sendEmptyMessageDelayed(0, interval);
//        }
//    };

    private void uploadFile(final File file) {
        Log.e("联网 上传图片", "文件:"+file.getAbsolutePath());
        HttpRequestUtils.multiFileUpload(this, file, "http://119.23.219.202/api/sys/uploadImg", fileNameStr, new HttpRequestUtils.ResponseListener() {
            @Override
            public void getResponseData(String response) {
                Toast.makeText(PreviewActivity.this, "上传截图"+fileNameStr+"成功", Toast.LENGTH_SHORT).show();
                Log.e("联网 上传图片 ", "上传成功"+response);
                deleteFile(file.getPath(), PreviewActivity.this);
            }

            @Override
            public void returnException(Exception e, String msg) {
                Toast.makeText(PreviewActivity.this, "上传截图失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void deleteFile(String path, Context context) {
        if (!TextUtils.isEmpty(path)) {
            try {
                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver mContentResolver = context.getContentResolver();
                String where = MediaStore.Images.Media.DATA + "='" + path + "'";
                //删除图片
                mContentResolver.delete(uri, where, null);

                //发送广播
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                File file = new File(path);
                Uri uri1 = Uri.fromFile(file);
                intent.setData(uri1);
                context.sendBroadcast(intent);
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void updateBtnStatus(ImageView v, boolean enable) {
        v.setEnabled(enable);
        switch (v.getId()) {
            case R.id.player_play_btn:
                if (mIsPlay) {
                    v.setImageResource(enable ? R.drawable.tabbar_pause : R.drawable.tabbar_pause_dis);
                } else {
                    v.setImageResource(enable ? R.drawable.tabbar_play : R.drawable.tabbar_play_dis);
                }
                break;
            case R.id.player_sound_btn:
                if (mIsSoundOn) {
                    v.setImageResource(enable ? R.drawable.tabbar_sound : R.drawable.tabbar_sound_dis);
                } else {
                    v.setImageResource(enable ? R.drawable.tabbar_mute : R.drawable.tabbar_mute_dis);
                }
                break;

            case R.id.player_quality_btn:
                if (mIsClear) {
                    v.setImageResource(enable ? R.drawable.tabbar_quality_clear : R.drawable.tabbar_quality_clear_dis);
                } else {
                    v.setImageResource(enable ? R.drawable.tabbar_quality_fluency : R.drawable.tabbar_quality_fluency_dis);
                }
                break;
            case R.id.player_record_btn:
                if (mIsRecord) {
                    v.setImageResource(enable ? R.drawable.recording : R.drawable.recording_dis);
                } else {
                    v.setImageResource(enable ? R.drawable.record : R.drawable.record_dis);
                }
                break;

            case R.id.player_cruise_btn:
                if (mIsCruise) {
                    v.setImageResource(enable ? R.drawable.cruising : R.drawable.cruising_dis);
                } else {
                    v.setImageResource(enable ? R.drawable.cruise : R.drawable.cruise_dis);
                }
                break;
        }
    }


    private void updateSupportedBtn() {
        boolean supportMotor = mDevice.supportMotor(mListType, mChannelId);
        boolean supportPreset = mDevice.supportPreset(mListType, mChannelId);
        boolean supportHalfDuplex = mDevice.supportAudio(mListType, mChannelId);
        boolean supportFullDuplex = mDevice.supportFullDuplexTalk(mListType, mChannelId);
        boolean supportCruise = mDevice.supportScanTour(mListType, mChannelId);

        TPViewUtils.setVisible(mCloudBtn, supportMotor ? View.VISIBLE : View.GONE);
        TPViewUtils.setVisible(mPresetBtn, supportPreset ? View.VISIBLE : View.GONE);
        TPViewUtils.setVisible(mTalkBtn, supportHalfDuplex ? View.VISIBLE : View.GONE);
        TPViewUtils.setVisible(mSpeakBtn, supportFullDuplex ? View.VISIBLE : View.GONE);
        TPViewUtils.setVisible(mCruiseBtn, supportCruise ? View.VISIBLE : View.GONE);
    }

    private void setSecondPageVisible(int pageType, boolean isVisible) {
        View layout = findViewById(R.id.player_controller_second_page);

        View controllerBtnTop = findViewById(R.id.player_controller_btn_top);
        View controllerBtnBottom = findViewById(R.id.player_controller_btn_bottom);

        if (TPUtils.isLandscape(this))
            return;

        if (isVisible) {
            layout.setVisibility(View.VISIBLE);
            controllerBtnBottom.setVisibility(View.GONE);
            controllerBtnTop.setVisibility(View.GONE);
            mSecondPageType = pageType;
        } else {
            layout.setVisibility(View.GONE);
            controllerBtnBottom.setVisibility(View.VISIBLE);
            controllerBtnTop.setVisibility(View.VISIBLE);
            mSecondPageType = PAGE_TYPE_INVALID;
        }

        initSecondPage();
        switch (pageType) {
            case PAGE_TYPE_TALK:
                findViewById(R.id.player_second_talking_btn).setVisibility(View.VISIBLE);
                break;
            case PAGE_TYPE_SPEAK:
                findViewById(R.id.player_second_speaking_btn).setVisibility(View.VISIBLE);
                break;
            case PAGE_TYPE_CLOUD:
                findViewById(R.id.player_second_cloud_layout).setVisibility(View.VISIBLE);
                break;
            case PAGE_TYPE_PRESET:
                showPresetListView(isVisible);
                break;
        }
    }

    private void showPresetListView(boolean visible) {
        mPresetListView.setVisibility(visible ? View.VISIBLE : View.GONE);
        // req preset list
        if (visible) {
            mDeviceContext.reqPresetList(mDevice, ipcReqResponse -> {
                if (ipcReqResponse.mError == 0) {
                    mMainHandler.post(() -> {
                        mPresetList.clear();
                        mPresetList.addAll(mDevice.getPresetList(mChannelId));
                        mPresetAdapter.update(mPresetList);
                    });
                }
                return 0;
            }, mChannelId);
        } else {
            mPresetList.clear();
            mPresetAdapter.update(mPresetList);
        }
    }

    private void initSecondPage() {
        findViewById(R.id.player_second_speaking_btn).setVisibility(View.GONE);
        findViewById(R.id.player_second_talking_btn).setVisibility(View.GONE);
        findViewById(R.id.player_second_cloud_layout).setVisibility(View.GONE);
        findViewById(R.id.player_second_preset_list).setVisibility(View.GONE);
    }

    private void updateDuplexButtonStatus(final boolean isFullDuplex, final boolean enable) {
        mMainHandler.post(() -> {
            ImageView duplexBtn = findViewById(isFullDuplex ? R.id.player_second_speaking_btn : R.id.player_second_talking_btn);
            duplexBtn.setEnabled(enable);
        });
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent event) {
        if (keycode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            // 右键处理
            MainActivity.startActivity(this);
        }
        return true;
    }
}
