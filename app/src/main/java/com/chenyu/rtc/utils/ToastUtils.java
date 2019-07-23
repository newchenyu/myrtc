package com.chenyu.rtc.utils;

import android.text.TextUtils;
import android.widget.Toast;

import com.chenyu.rtc.RTCApplication;

public class ToastUtils {
    private static final ToastUtils instance = new ToastUtils();
    private static Toast toast;

    private ToastUtils() {
    }

    public static ToastUtils getInstance() {
        return instance;
    }

    public static Toast getToast() {
        return toast;
    }

    public static void showToast(String text) {
        if (!TextUtils.isEmpty(text)) {
            if (toast == null) {
                toast = Toast.makeText(RTCApplication.getInstance(), text, Toast.LENGTH_SHORT);
            } else {
                toast.setText(text);
            }
            toast.show();
        }
    }
}
