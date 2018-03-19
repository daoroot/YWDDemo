package com.example.dabo.ywddemo;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * 日志管理类
 *
 * @author Bruce
 */
public class LogUtil {
    /**
     * 是否在LogCat中显示Log  IS_LOG
     */
    public static final boolean IS_LOG = false;
    private static final String TAG = "YWD_Demo";

    private static final String PATH_LOGS = Environment.getExternalStorageDirectory()
            + "/ywdLogs/" + "ywdLog.txt";

    synchronized private static void wToFile(String msg) {
        FileWriter fileWriter;
        try {
            File file = new File(PATH_LOGS);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }
            fileWriter = new FileWriter(PATH_LOGS, true);
            fileWriter.write((getDate() + " " + msg + "\n"));
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA);

    private static String getDate() {
        Calendar ca = Calendar.getInstance();
        return sdf.format(ca.getTime());
    }

    public static void i(String tag, String msg) {
        if (IS_LOG) {
            android.util.Log.i(TAG, tag + "." + msg);
            wToFile(tag + "." + msg);
        }
    }

    public static void v(String tag, String msg) {
        if (IS_LOG) {
            android.util.Log.v(TAG, tag + "." + msg);
            wToFile(tag + "." + msg);
        }
    }

    public static void w(String tag, String msg) {
        if (IS_LOG) {
            android.util.Log.w(TAG, tag + "." + msg);
            wToFile(tag + "." + msg);
        }
    }

    public static void d(String tag, String msg) {
        if (IS_LOG) {
            android.util.Log.d(TAG, tag + "." + msg);
            wToFile(tag + "." + msg);
        }
    }

    public static void e(String tag, String msg) {
        if (IS_LOG) {
            android.util.Log.e(TAG, tag + "." + msg);
            wToFile(tag + "." + msg);
        }
    }
}

