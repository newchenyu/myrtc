package com.chenyu.rtc;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.chenyu.rtc.utils.Constant;
import com.qiniu.droid.rtc.QNLogLevel;
import com.qiniu.droid.rtc.QNRTCEnv;
import com.tencent.bugly.Bugly;

public class RTCApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private static RTCApplication baseApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        super.onCreate();
        QNRTCEnv.setLogLevel(QNLogLevel.INFO);
        /**
         * init must be called before any other func
         */
        QNRTCEnv.init(getApplicationContext());
        QNRTCEnv.setLogFileEnabled(true);
        Bugly.init(this, Constant.BUGLY_APPID, false);
        baseApplication = this;
    }

    public static RTCApplication getInstance() {
        return baseApplication;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
