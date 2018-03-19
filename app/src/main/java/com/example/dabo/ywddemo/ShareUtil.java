package com.example.dabo.ywddemo;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by dabo on 2018/3/18.
 */

public class ShareUtil {
    private final static String preferencesName = "setting";
    final static String update = "update";
    final static String unzip = "unzip";
    final static String update_progress = "progress";
    final static String update_max = "max";

    static boolean getBoolean(Context context, String key, boolean defValue) {
        boolean value = false;
        try {
            SharedPreferences prefs = context.getSharedPreferences(preferencesName, 0);
            value = prefs.getBoolean(key, defValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    static void setBoolean(Context context, String key, boolean value) {
        SharedPreferences.Editor prefsEditor = context.getSharedPreferences(preferencesName, 0).edit();
        prefsEditor.putBoolean(key, value);
        prefsEditor.commit();
    }

    static void setInt(Context context, String key, int value) {
        SharedPreferences.Editor prefsEditor = context.getSharedPreferences(preferencesName, 0).edit();
        prefsEditor.putInt(key, value);
        prefsEditor.commit();
    }

    static int getInt(Context context, String key, int defValue) {
        int value = 0;
        try {
            SharedPreferences prefs = context.getSharedPreferences(preferencesName, 0);
            value = prefs.getInt(key, defValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

}
