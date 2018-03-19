package com.example.dabo.ywddemo;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;

public class MainActivity extends AppCompatActivity {

    Thread updateThread;
    ProgressBar pBar;
    WebView webView;
    Thread unZipThread;
    int state_index = 0;
    File[] files;
    ProgressDialog waitingDialog;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                Toast.makeText(getApplication(), "更新完成", Toast.LENGTH_SHORT).show();
            } else if (msg.what == 2)
                Toast.makeText(getApplication(), "更新资源校验失败", Toast.LENGTH_SHORT).show();
            else if (msg.what == 3) {
                state_index = 4;
                if (waitingDialog != null && waitingDialog.isShowing()) {
                    waitingDialog.cancel();
                    ShareUtil.setBoolean(getApplication(), ShareUtil.unzip, true);
                    DemoUtils.unzip = true;
                    Toast.makeText(getApplication(), "解压完成", Toast.LENGTH_SHORT).show();
                }
                String fileDir = Constants.RES_URL.substring(Constants.RES_URL.lastIndexOf("/") + 1, Constants.RES_URL.lastIndexOf("."));
                webView.loadUrl("file:///" + Constants.PATH_YWD_FILES + fileDir + "/" + msg.obj);
            } else if (msg.what == 4) {
                state_index = 5;
                String fileDir = Constants.RES_URL.substring(Constants.RES_URL.lastIndexOf("/") + 1, Constants.RES_URL.lastIndexOf("."));
                webView.loadUrl("file:///" + Constants.PATH_YWD_FILES + fileDir + "/" + msg.obj);
            } else if (msg.what == 5) {
                state_index = 3;
                String fileDir = Constants.RES_URL.substring(Constants.RES_URL.lastIndexOf("/") + 1, Constants.RES_URL.lastIndexOf("."));
                webView.loadUrl("file:///" + Constants.PATH_YWD_FILES + fileDir + "/" + msg.obj);
            } else
                Toast.makeText(getApplication(), "还没有用到", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pBar = (ProgressBar) findViewById(R.id.pb);
        DemoUtils.update = ShareUtil.getBoolean(getApplication(), ShareUtil.update, true);
        DemoUtils.unzip = ShareUtil.getBoolean(getApplication(), ShareUtil.unzip, false);
        int progress = ShareUtil.getInt(getApplication(), ShareUtil.update_progress, 0);
        int max = ShareUtil.getInt(getApplication(), ShareUtil.update_max, 0);
        pBar.setMax(max);
        pBar.setProgress(progress);
        initFiles();
        DemoUtils.setUpdateCallBack(new UpdateCallBack() {

            @Override
            public void update(int progress, String filePath) {
                System.out.println("下载总进度：" + progress);
                pBar.setProgress(progress);
                ShareUtil.setInt(getApplication(), ShareUtil.update_progress, progress);
                if (progress >= pBar.getMax()) {
                    ShareUtil.setBoolean(getApplication(), ShareUtil.update, false);
                    DemoUtils.update = false;
                    if (MD5.checkMd5(DemoUtils.RES_MD5, filePath))
                        mHandler.sendEmptyMessage(1);
                    else
                        mHandler.sendEmptyMessage(2);
                }
            }
        }, pBar);
        updateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                DemoUtils.startSychRes(Constants.RES_URL, MainActivity.this);
            }
        });

        unZipThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String filePath = Constants.RES_URL.substring(Constants.RES_URL.lastIndexOf("/") + 1);
                DemoUtils.unzipFile(Constants.PATH_YWD_FILES, Constants.PATH_YWD_FILES + "/" + filePath);
                initFiles();
                sen3Msg();
            }
        });

        webView = (WebView) findViewById(R.id.wv);
        //加上下面这段代码可以使网页中的链接不以浏览器的方式打开
        webView.setWebViewClient(new WebViewClient());
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);//滚动条风格，为0指滚动条不占用空间，直接覆盖在网页上
        //得到webview设置
        WebSettings webSettings = webView.getSettings();
        //允许使用javascript
        webSettings.setJavaScriptEnabled(true);
        //设置字符编码
        webSettings.setDefaultTextEncodingName("UTF-8");
        //支持缩放
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webView.loadUrl("file:///android_asset/搜狗搜索引擎 - 上网从搜狗开始.htm");
    }

    private void initFiles() {
        String fileName = Constants.PATH_YWD_FILES + Constants.RES_URL.substring(Constants.RES_URL.lastIndexOf("/") + 1, Constants.RES_URL.lastIndexOf("."));
        files = new File(fileName).listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File file, String s) {
                return s.contains(".");
            }
        });

    }

    private void sen3Msg() {
        if (files != null && files.length > 0) {
            Message msg = Message.obtain();
            msg.obj = files[0].getName();
            msg.what = 3;
            mHandler.sendMessage(msg);
        }
    }

    public void startRes(View v) {
        if (DemoUtils.isUpdating()) {
            Toast.makeText(getApplication(), "正在更新...", Toast.LENGTH_SHORT).show();
        } else {
            if (DemoUtils.update)
                updateThread.start();
            else
                Toast.makeText(getApplication(), "已经更新过", Toast.LENGTH_SHORT).show();
        }
    }

    public void unzip(View v) {
        if (DemoUtils.update) {
            Toast.makeText(getApplication(), "还没更新完成", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!DemoUtils.unzip) {
            showWaitingDialog();
            unZipThread.start();
        } else {
            Toast.makeText(getApplication(), "已经解压完成", Toast.LENGTH_SHORT).show();
        }
    }

    public void showRes(View v) {
        if (files == null || files.length <= 0) {
            Toast.makeText(getApplication(), "没有新的资源", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (state_index) {
            case 0:
                sen3Msg();
                break;
            case 3:
                Message msg1 = Message.obtain();
                msg1.obj = files[0].getName();
                msg1.what = 3;
                mHandler.sendMessage(msg1);//显示第一个更新网页
                break;
            case 4:
                Message msg2 = Message.obtain();
                msg2.obj = files[1].getName();
                msg2.what = 4;
                mHandler.sendMessage(msg2);//显示第二个更新网页
                break;
            case 5:
                Message msg3 = Message.obtain();
                msg3.obj = files[2].getName();
                msg3.what = 5;
                mHandler.sendMessage(msg3);//显示第三个更新网页
                break;
            default:
                Toast.makeText(getApplication(), "正在更新", Toast.LENGTH_SHORT).show();
        }

    }

    private void showWaitingDialog() {
    /* 等待Dialog具有屏蔽其他控件的交互能力
     * @setCancelable 为使屏幕不可点击，设置为不可取消(false)
     * 下载等事件完成后，主动调用函数关闭该Dialog
     */
        waitingDialog =
                new ProgressDialog(MainActivity.this);
        waitingDialog.setTitle("正在解压");
        waitingDialog.setMessage("解压完成后弹窗自动消失");
        waitingDialog.setIndeterminate(true);
        waitingDialog.setCancelable(false);
        waitingDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public interface UpdateCallBack {
        void update(int progress, String filePath);
    }
}
