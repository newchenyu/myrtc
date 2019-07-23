package com.chenyu.rtc.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.chenyu.rtc.R;
import com.chenyu.rtc.utils.Config;
import com.chenyu.rtc.utils.ConnectionDetector;
import com.chenyu.rtc.utils.Constant;
import com.chenyu.rtc.utils.PermissionChecker;
import com.chenyu.rtc.utils.ToastUtils;
import com.google.gson.Gson;
import com.qiniu.droid.rtc.QNScreenCaptureUtil;
import com.qiniu.rtc.model.RoomAccess;
import com.qiniu.util.Auth;
import com.qiniu.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private Button mMeeting;
    private EditText mRoomNum;
    private EditText mUserName;
    private String roomNum;
    private String userName;
    private static final int USERNAME_REQUEST_CODE = 0;
    private boolean mIsScreenCaptureEnabled;
    private int mCaptureMode = 3;
    private Gson gson = new Gson();
    private static final String PATH_ROOM_NUM = "[a-zA-Z0-9_]{3,64}";
    private static final String PATH_USER_NAME = "[a-zA-Z0-9_]{3,50}";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMeeting = findViewById(R.id.meeting);
        mRoomNum = findViewById(R.id.edit_room_num);
        mUserName = findViewById(R.id.edit_user_name);
        mMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                roomNum = mRoomNum.getText().toString().trim();
                userName = mUserName.getText().toString().trim();
                if (StringUtils.isNullOrEmpty(roomNum)) {
                    ToastUtils.showToast("会议号不能为空");
                    return;
                }
                if (!Pattern.matches(PATH_ROOM_NUM, roomNum)) {
                    ToastUtils.showToast("会议号只允许3-64个字符的大小写字母,数字和下划线");
                    return;
                }
                if (StringUtils.isNullOrEmpty(userName)) {
                    userName = UUID.randomUUID().toString().replaceAll("-", "");
                } else {
                    if (!Pattern.matches(PATH_USER_NAME, userName)) {
                        ToastUtils.showToast("昵称只允许3-50个字符的大小写字母,数字和下划线");
                        return;
                    }
                }
                //检查网络连接
                ConnectionDetector cd = new ConnectionDetector(getApplicationContext());
                if (!cd.isConnectingToInternet()) {
                    ToastUtils.showToast("网络连接失败，请检查网络");
                    return;
                }
                onClickConference();
            }
        });
        if (!isPermissionOK()) {
            return;
        }
    }

    /**
     * 获取七牛的roomtoken
     *
     * @return
     */
    private String getRoomToken() {
        Auth auth = Auth.create(Constant.QN_AK, Constant.QN_SK);
        long nowTime = (new Date().getTime() + 60 * 60 * 24 * 1000) / 1000;
        RoomAccess access = new RoomAccess(Constant.APP_ID, roomNum, userName, nowTime, "user");
        String json = gson.toJson(access);
        String token = null;
        try {
            token = auth.signRoomToken(json);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return token;
    }

    private void startConference() {
        String token = getRoomToken();
        if (token == null) {
            ToastUtils.showToast("加入失败");
            return;
        }
        Intent intent = new Intent(MainActivity.this, MeetingActivity.class);
        intent.putExtra(Constant.EXTRA_ROOM_ID, roomNum);
        intent.putExtra(Constant.EXTRA_ROOM_TOKEN, token);
        intent.putExtra(Constant.EXTRA_USER_ID, userName);
        startActivity(intent);
    }

    /**
     * 检查权限
     *
     * @return
     */
    private boolean isPermissionOK() {
        PermissionChecker checker = new PermissionChecker(this);
        boolean isPermissionOK = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checker.checkPermission();
        if (!isPermissionOK) {
            ToastUtils.showToast("Some permissions is not approved !!!");
        }
        return isPermissionOK;
    }

    /**
     * 调用系统请求获取屏幕的时候，检查用户是否同意的回调
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == USERNAME_REQUEST_CODE) {
            SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit();
            editor.putString(Config.USER_NAME, Constant.ADMIN_USER);
            editor.apply();
        } else if (requestCode == QNScreenCaptureUtil.SCREEN_CAPTURE_PERMISSION_REQUEST_CODE &&
                QNScreenCaptureUtil.onActivityResult(requestCode, resultCode, data)) {
            startConference();
        }
    }

    public void onClickConference() {
        mIsScreenCaptureEnabled = (mCaptureMode == Config.SCREEN_CAPTURE || mCaptureMode == Config.MUTI_TRACK_CAPTURE);
        if (mIsScreenCaptureEnabled) {
            QNScreenCaptureUtil.requestScreenCapture(this);
        } else {
            startConference();
        }
    }
}
