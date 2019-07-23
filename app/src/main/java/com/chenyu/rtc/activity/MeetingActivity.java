package com.chenyu.rtc.activity;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.chenyu.rtc.R;
import com.chenyu.rtc.utils.Config;
import com.chenyu.rtc.utils.Constant;
import com.chenyu.rtc.utils.OnDoubleClickListener;
import com.google.gson.Gson;
import com.qiniu.droid.rtc.QNRTCEngine;
import com.qiniu.droid.rtc.QNRTCEngineEventListener;
import com.qiniu.droid.rtc.QNRTCSetting;
import com.qiniu.droid.rtc.QNRoomState;
import com.qiniu.droid.rtc.QNSourceType;
import com.qiniu.droid.rtc.QNStatisticsReport;
import com.qiniu.droid.rtc.QNSurfaceView;
import com.qiniu.droid.rtc.QNTrackInfo;
import com.qiniu.droid.rtc.QNVideoFormat;
import com.qiniu.droid.rtc.model.QNAudioDevice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author chenyu
 * 会议界面
 */
public class MeetingActivity extends AppCompatActivity implements QNRTCEngineEventListener {
    private static final String TAG = MeetingActivity.class.getSimpleName();
    private static final int BITRATE_FOR_SCREEN_VIDEO = (int) (1.5 * 1000 * 1000);
    private List<String> mHWBlackList = new ArrayList<>();
    private int mScreenWidth = 0;
    private int mScreenHeight = 0;
    private QNRTCEngine mEngine;
    private Toast mLogToast;
    private static final String[] MANDATORY_PERMISSIONS = {
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET"
    };
    private QNTrackInfo mLocalVideoTrack;
    private QNTrackInfo mLocalAudioTrack;
    private QNTrackInfo mLocalScreenTrack;
    private List<QNTrackInfo> mLocalTrackList;
    private List<QNTrackInfo> mLocalScreenTrackList;
    private Map<String, List<QNTrackInfo>> mUserTrackInfos;
    private QNSurfaceView mTopView;
    private ConstraintLayout mTopLayout;
    private List<QNSurfaceView> mViews;
    private List<QNSurfaceView> mSmallCameraViews;
    private List<QNSurfaceView> mSmallScreenViews;
    private QNSurfaceView mFullScreen;
    private QNSurfaceView mFullSmallCamera;
    private QNSurfaceView mFullSmallScreen;
    private List<ConstraintLayout> mLayouts;
    private Gson gson = new Gson();
    private String roomToken;
    private ImageButton mLocalAudio;
    private ImageButton mLocalCamera;
    private ImageButton mShareScreen;
    private ImageButton mExitMeeting;
    private boolean mMuteLocalAudio = false;
    private boolean mMuteLocalCamera = false;
    private boolean mIsShareScreen = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());
        final WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(outMetrics);
        mScreenWidth = outMetrics.widthPixels;
        mScreenHeight = outMetrics.heightPixels;
        setContentView(R.layout.activity_meeting);
        Intent intent = getIntent();
        //获取roomtoken
        roomToken = intent.getStringExtra(Constant.EXTRA_ROOM_TOKEN);
        //初始化一些属性
        initView();
        // 检查一些权限
        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                logAndToast("Permission " + permission + " is not granted");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }
        //初始化七牛sdk
        initQNRTCEngine();
        //初始化本地track
        initLocalTrackInfoList();
    }

    private void initView() {
        mTopView = findViewById(R.id.top_view);
        mTopLayout = findViewById(R.id.top_layout);
        mViews = new ArrayList<>(Arrays.asList(
                (QNSurfaceView) findViewById(R.id.view1),
                (QNSurfaceView) findViewById(R.id.view2),
                (QNSurfaceView) findViewById(R.id.view3),
                (QNSurfaceView) findViewById(R.id.view4),
                (QNSurfaceView) findViewById(R.id.view5)
        ));
        mLayouts = new ArrayList<>(Arrays.asList(
                (ConstraintLayout) findViewById(R.id.layout1),
                (ConstraintLayout) findViewById(R.id.layout2),
                (ConstraintLayout) findViewById(R.id.layout3),
                (ConstraintLayout) findViewById(R.id.layout4),
                (ConstraintLayout) findViewById(R.id.layout5)
        ));
        mSmallCameraViews = new ArrayList<>(Arrays.asList(
                (QNSurfaceView) findViewById(R.id.small_camera_view1),
                (QNSurfaceView) findViewById(R.id.small_camera_view2),
                (QNSurfaceView) findViewById(R.id.small_camera_view3),
                (QNSurfaceView) findViewById(R.id.small_camera_view4),
                (QNSurfaceView) findViewById(R.id.small_camera_view5)
        ));
        mSmallScreenViews = new ArrayList<>(Arrays.asList(
                (QNSurfaceView) findViewById(R.id.small_screen_view1),
                (QNSurfaceView) findViewById(R.id.small_screen_view2),
                (QNSurfaceView) findViewById(R.id.small_screen_view3),
                (QNSurfaceView) findViewById(R.id.small_screen_view4),
                (QNSurfaceView) findViewById(R.id.small_screen_view5)
        ));
        mFullScreen = findViewById(R.id.full_screen);
        mFullSmallCamera = findViewById(R.id.full_small_camera);
        mFullSmallScreen = findViewById(R.id.full_small_screen);
        mLocalAudio = findViewById(R.id.local_audio);
        mLocalCamera = findViewById(R.id.local_camera);
        mShareScreen = findViewById(R.id.share_screen);
        mExitMeeting = findViewById(R.id.exit_meeting);
        for (final QNSurfaceView view : mViews) {
            //双击全屏
            view.setOnTouchListener(new OnDoubleClickListener(new OnDoubleClickListener.DoubleClickCallback() {
                @Override
                public void onDoubleClick() {
                    if (null != view.getTag()) {
                        mFullScreen.release();
                        mFullScreen.setVisibility(View.VISIBLE);
                        mFullScreen.setTag(view.getTag());
                        mFullScreen.setId(view.getId());
                        mFullScreen.setZOrderOnTop(true);
                        mEngine.setRenderWindow((QNTrackInfo) view.getTag(), mFullScreen);
                    }
                }
            }));
        }
        for (final QNSurfaceView view : mSmallCameraViews) {
            //双击全屏
            view.setOnTouchListener(new OnDoubleClickListener(new OnDoubleClickListener.DoubleClickCallback() {
                @Override
                public void onDoubleClick() {
                    if (null != view.getTag()) {
                        mFullSmallCamera.release();
                        mFullSmallCamera.setVisibility(View.VISIBLE);
                        mFullSmallCamera.setTag(view.getTag());
                        mFullSmallCamera.setId(view.getId());
                        mFullSmallCamera.setZOrderOnTop(true);
                        mEngine.setRenderWindow((QNTrackInfo) view.getTag(), mFullSmallCamera);
                    }
                }
            }));
        }
        for (final QNSurfaceView view : mSmallScreenViews) {
            //双击全屏
            view.setOnTouchListener(new OnDoubleClickListener(new OnDoubleClickListener.DoubleClickCallback() {
                @Override
                public void onDoubleClick() {
                    if (null != view.getTag()) {
                        mFullSmallScreen.release();
                        mFullSmallScreen.setVisibility(View.VISIBLE);
                        mFullSmallScreen.setTag(view.getTag());
                        mFullSmallScreen.setId(view.getId());
                        mFullSmallScreen.setZOrderOnTop(true);
                        mEngine.setRenderWindow((QNTrackInfo) view.getTag(), mFullSmallScreen);
                    }
                }
            }));
        }
        //双击取消全屏
        mFullScreen.setOnTouchListener(new OnDoubleClickListener(new OnDoubleClickListener.DoubleClickCallback() {
            @Override
            public void onDoubleClick() {
                mFullScreen.setVisibility(View.GONE);
                mViews.get(mFullScreen.getId()).release();
                mEngine.setRenderWindow((QNTrackInfo) mFullScreen.getTag(), mViews.get(mFullScreen.getId()));
            }
        }));
        mFullSmallCamera.setOnTouchListener(new OnDoubleClickListener(new OnDoubleClickListener.DoubleClickCallback() {
            @Override
            public void onDoubleClick() {
                mFullSmallCamera.setVisibility(View.GONE);
                mSmallCameraViews.get(mFullSmallCamera.getId()).release();
                mEngine.setRenderWindow((QNTrackInfo) mFullSmallCamera.getTag(), mSmallCameraViews.get(mFullSmallCamera.getId()));
            }
        }));
        mFullSmallScreen.setOnTouchListener(new OnDoubleClickListener(new OnDoubleClickListener.DoubleClickCallback() {
            @Override
            public void onDoubleClick() {
                mFullSmallScreen.setVisibility(View.GONE);
                mSmallScreenViews.get(mFullSmallScreen.getId()).release();
                mEngine.setRenderWindow((QNTrackInfo) mFullSmallScreen.getTag(), mSmallScreenViews.get(mFullSmallScreen.getId()));
            }
        }));
        //打开/关闭麦克风
        mLocalAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMuteLocalAudio = !mMuteLocalAudio;
                mEngine.muteLocalAudio(mMuteLocalAudio);
                mLocalAudio.setImageResource(mMuteLocalAudio ? R.drawable.mic_unchoose : R.drawable.mic);
                logAndToast(mMuteLocalAudio ? "麦克风已关闭" : "麦克风已打开");
            }
        });
        //打开/关闭本地摄像头
        mLocalCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMuteLocalCamera = !mMuteLocalCamera;
                mLocalVideoTrack.setMuted(mMuteLocalCamera);
                mEngine.muteTracks(Collections.singletonList(mLocalVideoTrack));
                mLocalCamera.setImageResource(mMuteLocalCamera ? R.drawable.camera_unchoose : R.drawable.camera);
                logAndToast(mMuteLocalCamera ? "摄像头已关闭" : "摄像头已打开");
                mTopLayout.setVisibility(mMuteLocalCamera ? View.VISIBLE : View.GONE);
            }
        });
        //打开/关闭屏幕共享
        mShareScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsShareScreen = !mIsShareScreen;
                if (mIsShareScreen) {
                    addScreenTrack();
                } else {
                    removeScreenTrack();
                }
                logAndToast(mIsShareScreen ? "正在共享屏幕" : "屏幕共享已关闭");
                mShareScreen.setImageResource(mIsShareScreen ? R.drawable.share_screen : R.drawable.share_screen_unchoose);
            }
        });
        //退出会议
        mExitMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEngine != null) {
                    mEngine.leaveRoom();
                    mEngine.destroy();
                    mEngine = null;
                }
                finish();
            }
        });
    }

    @TargetApi(19)
    private static int getSystemUiVisibility() {
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        return flags;
    }

    /**
     * 取消发布屏幕流
     */
    private void removeScreenTrack() {
        mEngine.unPublishTracks(mLocalScreenTrackList);
    }

    /**
     * 发布屏幕流
     */
    private void addScreenTrack() {
        if (mLocalScreenTrack == null) {
            QNVideoFormat screenEncodeFormat = new QNVideoFormat(mScreenWidth / 2, mScreenHeight / 2, 15);
            mLocalScreenTrack = mEngine.createTrackInfoBuilder()
                    .setSourceType(QNSourceType.VIDEO_SCREEN)
                    .setVideoPreviewFormat(screenEncodeFormat)
                    .setBitrate(BITRATE_FOR_SCREEN_VIDEO)
                    .setMaster(true)
                    .setTag(Constant.TAG_SCREEN).create();
        }
        mLocalScreenTrackList = new ArrayList<>();
        mLocalScreenTrackList.add(mLocalScreenTrack);
        mEngine.publishTracks(mLocalScreenTrackList);
    }

    private void initLocalTrackInfoList() {
        mLocalTrackList = new ArrayList<>();
        //音频
        mLocalAudioTrack = mEngine.createTrackInfoBuilder()
                .setSourceType(QNSourceType.AUDIO)
                .setMaster(true)
                .create();
        mLocalTrackList.add(mLocalAudioTrack);
        //本地的摄像头
        mLocalVideoTrack = mEngine.createTrackInfoBuilder()
                .setSourceType(QNSourceType.VIDEO_CAMERA)
                .setTag(Constant.TAG_CAMERA).create();
        mLocalTrackList.add(mLocalVideoTrack);
    }

    private void logAndToast(final String msg) {
        if (mLogToast != null) {
            mLogToast.cancel();
        }
        mLogToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        mLogToast.show();
    }

    private void initQNRTCEngine() {
        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        int videoWidth = preferences.getInt(Config.WIDTH, Config.DEFAULT_RESOLUTION[3][0]);
        int videoHeight = preferences.getInt(Config.HEIGHT, Config.DEFAULT_RESOLUTION[3][1]);
        int fps = preferences.getInt(Config.FPS, Config.DEFAULT_FPS[3]);
        boolean isHwCodec = preferences.getInt(Config.CODEC_MODE, Config.HW) == Config.HW;
        int videoBitrate = preferences.getInt(Config.BITRATE, Config.DEFAULT_BITRATE[3]);
        boolean isMaintainRes = preferences.getBoolean(Config.MAINTAIN_RES, false);
        // get the items in hw black list, and set isHwCodec false forcibly
        String[] hwBlackList = getResources().getStringArray(R.array.hw_black_list);
        mHWBlackList.addAll(Arrays.asList(hwBlackList));
        if (mHWBlackList.contains(Build.MODEL)) {
            isHwCodec = false;
        }
        QNVideoFormat format = new QNVideoFormat(videoWidth, videoHeight, fps);
        QNRTCSetting setting = new QNRTCSetting();
        setting.setCameraID(QNRTCSetting.CAMERA_FACING_ID.FRONT)
                .setHWCodecEnabled(isHwCodec)
                .setMaintainResolution(isMaintainRes)
                .setVideoBitrate(videoBitrate)
                .setVideoEncodeFormat(format)
                .setVideoPreviewFormat(format);
        mEngine = QNRTCEngine.createEngine(getApplicationContext(), setting, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mEngine != null) {
            mEngine.leaveRoom();
            mEngine.destroy();
            mEngine = null;
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mEngine != null) {
            mTopLayout.setVisibility(View.GONE);
            //设置本地摄像头渲染窗口
            mEngine.setRenderWindow(mLocalVideoTrack, mTopView);
            mEngine.joinRoom(roomToken);
        }
    }

    @Override
    public void onRoomStateChanged(QNRoomState state) {
        Log.i(TAG, "onRoomStateChanged:" + state.name());
        switch (state) {
            case RECONNECTING:
                logAndToast("正在重连......");
                break;
            case CONNECTED:
                mEngine.publishTracks(mLocalTrackList);
                logAndToast("连接成功");
                break;
            case RECONNECTED:
                logAndToast("连接成功");
                break;
            case CONNECTING:
                logAndToast("正在连接......");
                break;
        }
    }

    @Override
    public void onRemoteUserJoined(String s, String s1) {

    }

    @Override
    public void onRemoteUserLeft(String s) {

    }

    @Override
    public void onLocalPublished(List<QNTrackInfo> list) {

    }

    @Override
    public void onRemotePublished(String s, List<QNTrackInfo> list) {

    }

    @Override
    public void onRemoteUnpublished(String remoteUserId, List<QNTrackInfo> list) {
        List<QNTrackInfo> list1 = mUserTrackInfos.get(remoteUserId);
        list1.removeAll(list);
        if (list1.size() == 0) {
            //用户离开
            mUserTrackInfos.remove(remoteUserId);
        } else {
            //用户取消发布屏幕流
            mUserTrackInfos.put(remoteUserId, list1);
        }
        updateWindow();
    }

    @Override
    public void onRemoteUserMuted(String s, List<QNTrackInfo> list) {

    }

    @Override
    public void onSubscribed(String remoteUserId, List<QNTrackInfo> list) {
        if (mUserTrackInfos == null) {
            mUserTrackInfos = new HashMap<>();
        }
        if (mUserTrackInfos.containsKey(remoteUserId)) {
            List<QNTrackInfo> list1 = mUserTrackInfos.get(remoteUserId);
            list1.addAll(list);
            mUserTrackInfos.put(remoteUserId, list1);
        } else {
            mUserTrackInfos.put(remoteUserId, list);
        }
        updateWindow();
    }

    /**
     * 更新渲染窗口
     */
    private void updateWindow() {
        //重置所有窗口的状态
        for (QNSurfaceView v : mViews) {
            v.release();
            v.setTag(null);
            v.setVisibility(View.VISIBLE);
        }
        for (QNSurfaceView v : mSmallCameraViews) {
            v.release();
            v.setTag(null);
            v.setVisibility(View.VISIBLE);
        }
        for (QNSurfaceView v : mSmallScreenViews) {
            v.release();
            v.setTag(null);
            v.setVisibility(View.VISIBLE);
        }
        for (ConstraintLayout l : mLayouts) {
            l.setVisibility(View.VISIBLE);
        }
        int i = 0;
        for (String userId : mUserTrackInfos.keySet()) {
            QNTrackInfo screenTrack = null;
            QNTrackInfo cameraTrack = null;
            for (QNTrackInfo trackInfo : mUserTrackInfos.get(userId)) {
                if (Constant.TAG_SCREEN.equals(trackInfo.getTag())) {
                    screenTrack = trackInfo;
                }
                if (Constant.TAG_CAMERA.equals(trackInfo.getTag())) {
                    cameraTrack = trackInfo;
                }
            }
            //用户分享屏幕的时候设置两个渲染窗口，摄像头流和屏幕流
            if (screenTrack != null) {
                mViews.get(i).setVisibility(View.GONE);
                mEngine.setRenderWindow(screenTrack, mSmallScreenViews.get(i));
                mEngine.setRenderWindow(cameraTrack, mSmallCameraViews.get(i));
                mSmallScreenViews.get(i).setTag(screenTrack);
                mSmallScreenViews.get(i).setId(i);
                mSmallCameraViews.get(i).setTag(cameraTrack);
                mSmallCameraViews.get(i).setId(i);
            } else {
                mSmallScreenViews.get(i).setVisibility(View.GONE);
                mSmallCameraViews.get(i).setVisibility(View.GONE);
                mEngine.setRenderWindow(cameraTrack, mViews.get(i));
                mViews.get(i).setTag(cameraTrack);
                mViews.get(i).setId(i);
            }
            mLayouts.get(i).setVisibility(View.GONE);
            i++;
        }
    }

    @Override
    public void onKickedOut(String s) {

    }

    @Override
    public void onStatisticsUpdated(QNStatisticsReport qnStatisticsReport) {

    }

    @Override
    public void onAudioRouteChanged(QNAudioDevice qnAudioDevice) {

    }

    @Override
    public void onCreateMergeJobSuccess(String s) {

    }

    @Override
    public void onError(int i, String s) {

    }
}