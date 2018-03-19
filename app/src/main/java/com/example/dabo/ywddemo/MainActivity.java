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

    private static final int MSG_WHAT_UPDATE_SUCCESS = 1;//更新资源下载成功
    private static final int MSG_WHAT_UPDATE_FAILED = 2;//更新资源下载失败
    private static final int MSG_WHAT_CHECK_SUCCESS = 3;//更新资源校验成功
    private static final int MSG_WHAT_CHECK_FAILED = 4;//更新资源校验失败
    private static final int MSG_WHAT_CHANGE_PAGE = 5;//切换网页

    private ProgressBar mProgressBar;
    private WebView mWebView;
    private Thread mUpdateThread;//更新线程
    private Thread mUnZipThread;//解压线程
    private int mState_index = 0;//页面显示状态
    private File[] mFiles;//解压完成后html文件列表
    private ProgressDialog mWaitingDialog;//解压显示

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_WHAT_UPDATE_SUCCESS:
                    if (MD5.checkMd5(YWDApp.RES_MD5, (String) msg.obj))
                        mHandler.sendEmptyMessage(MSG_WHAT_CHECK_SUCCESS);
                    else
                        mHandler.sendEmptyMessage(MSG_WHAT_CHECK_FAILED);
                    Toast.makeText(getApplication(), "更新完成", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_WHAT_UPDATE_FAILED:
                    Toast.makeText(getApplication(), "更新失败", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_WHAT_CHECK_SUCCESS:
                    ShareUtil.setBoolean(getApplication(), ShareUtil.update, false);
                    YWDApp.update = false;
                    Toast.makeText(getApplication(), "更新资源校验成功", Toast.LENGTH_SHORT).show();
                    sendChangeMsg();
                    break;
                case MSG_WHAT_CHECK_FAILED:
                    Toast.makeText(getApplication(), "更新资源校验失败", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_WHAT_CHANGE_PAGE:
                    if (mFiles != null && mFiles.length > 0 && mState_index >= mFiles.length)
                        mState_index = 0;//切换到最后一个网页，再次切换回复到第一个更新页
                    if (mState_index == 0 && (mFiles != null && mFiles.length <= 0 || mFiles == null))
                        initWVData();
                    else {
                        if (mWaitingDialog != null && mWaitingDialog.isShowing()) {
                            mWaitingDialog.cancel();
                            ShareUtil.setBoolean(getApplication(), ShareUtil.unzip, true);
                            YWDApp.unzip = true;
                            Toast.makeText(getApplication(), "解压完成", Toast.LENGTH_SHORT).show();
                        }
                        String fileDir = Constants.RES_URL.substring(Constants.RES_URL.lastIndexOf("/") + 1, Constants.RES_URL.lastIndexOf("."));
                        mWebView.loadUrl("file:///" + Constants.PATH_YWD_FILES + fileDir + "/" + mFiles[mState_index].getName());
                        mState_index++;
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void initWVData() {
        mWebView.loadUrl("file:///android_asset/搜狗搜索引擎 - 上网从搜狗开始.htm");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        YWDApp.update = ShareUtil.getBoolean(getApplication(), ShareUtil.update, true);
        YWDApp.unzip = ShareUtil.getBoolean(getApplication(), ShareUtil.unzip, false);
        int progress = ShareUtil.getInt(getApplication(), ShareUtil.update_progress, 0);
        int max = ShareUtil.getInt(getApplication(), ShareUtil.update_max, 0);

        setContentView(R.layout.activity_main);
        mProgressBar = (ProgressBar) findViewById(R.id.pb);
        mProgressBar.setMax(max);
        mProgressBar.setProgress(progress);
        initFiles();
        DemoUtils.setUpdateCallBack(new UpdateCallBack() {

            @Override
            public void update(int progress, String filePath, int progressMax) {
                LogUtil.i("下载资源文件", "下载总进度：" + progress);
                if (mProgressBar.getMax() != progressMax)
                    mProgressBar.setMax(progressMax);
                mProgressBar.setProgress(progress);
                ShareUtil.setInt(getApplication(), ShareUtil.update_progress, progress);
                if (progress >= mProgressBar.getMax()) {
                    Message msg = Message.obtain();
                    msg.what = MSG_WHAT_UPDATE_SUCCESS;
                    msg.obj = filePath;
                    mHandler.sendMessage(msg);
                }
            }
        });
        mUpdateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                DemoUtils.startDownRes(Constants.RES_URL, MainActivity.this);
            }
        });

        mUnZipThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String filePath = Constants.RES_URL.substring(Constants.RES_URL.lastIndexOf("/") + 1);
                DemoUtils.unzipFile(Constants.PATH_YWD_FILES, Constants.PATH_YWD_FILES + "/" + filePath);
                initFiles();
                sendChangeMsg();
            }
        });

        mWebView = (WebView) findViewById(R.id.wv);
        //加上下面这段代码可以使网页中的链接不以浏览器的方式打开
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);//滚动条风格，为0指滚动条不占用空间，直接覆盖在网页上
        //得到webview设置
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDefaultTextEncodingName("UTF-8");
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        initWVData();
    }

    private void initFiles() {
        String fileName = Constants.PATH_YWD_FILES + Constants.RES_URL.substring(Constants.RES_URL.lastIndexOf("/") + 1, Constants.RES_URL.lastIndexOf("."));
        mFiles = new File(fileName).listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File file, String s) {
                return s.contains(".");
            }
        });
    }

    private void sendChangeMsg() {
        mHandler.sendEmptyMessage(MSG_WHAT_CHANGE_PAGE);
    }

    public void startRes(View v) {
        if (DemoUtils.isUpdating()) {
            Toast.makeText(getApplication(), "正在更新...", Toast.LENGTH_SHORT).show();
        } else {
            if (YWDApp.update)
                mUpdateThread.start();
            else
                Toast.makeText(getApplication(), "已经更新过", Toast.LENGTH_SHORT).show();
        }
    }

    public void unzip(View v) {
        if (YWDApp.update) {
            Toast.makeText(getApplication(), "还没更新完成", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!YWDApp.unzip) {
            showWaitingDialog();
            mUnZipThread.start();
        } else {
            Toast.makeText(getApplication(), "已经解压完成", Toast.LENGTH_SHORT).show();
        }
    }

    public void showRes(View v) {
        if (mFiles == null || mFiles.length <= 0) {
            Toast.makeText(getApplication(), "没有新的资源", Toast.LENGTH_SHORT).show();
            return;
        }
        sendChangeMsg();
    }

    private void showWaitingDialog() {
    /* 等待Dialog具有屏蔽其他控件的交互能力
     * @setCancelable 为使屏幕不可点击，设置为不可取消(false)
     * 下载等事件完成后，主动调用函数关闭该Dialog
     */
        mWaitingDialog =
                new ProgressDialog(MainActivity.this);
        mWaitingDialog.setTitle("正在解压");
        mWaitingDialog.setMessage("解压完成后弹窗自动消失");
        mWaitingDialog.setIndeterminate(true);
        mWaitingDialog.setCancelable(false);
        mWaitingDialog.show();
    }

    public interface UpdateCallBack {
        void update(int progress, String filePath, int progressMax);
    }
}
