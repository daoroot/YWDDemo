package com.example.dabo.ywddemo;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by dabo on 2018/3/17.
 */

public class DemoUtils {

    private static String Tag = "DemoUtils";

    private static MainActivity.UpdateCallBack updateCallBack;
    private static AtomicInteger progress;
    private static int progressMax;
    private static boolean isUpdating = false;

    static void startDownRes(String PATH, Context ctx) {
        progress = new AtomicInteger(0);
        isUpdating = true;
        try {
            URL url = new URL(PATH);
            // 获取连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // 通过获取连接定义文件名
            String[] str = PATH.split("/");
            String fileName = str[str.length - 1];
            // 获取下载文件大小
            int fileLength = conn.getContentLength();
            progressMax = fileLength;
            File file = new File(Constants.PATH_YWD_FILES);
            if (!file.exists()) {
                file.mkdirs();
            }
            // 在本地创建一个与服务器大小一致的可随机写入文件
            RandomAccessFile raf = new RandomAccessFile(Constants.PATH_YWD_FILES + fileName, "rwd");
            LogUtil.v(Tag, "创建资源更新包文件：" + fileLength);
            raf.setLength(fileLength);
            ShareUtil.setInt(ctx, ShareUtil.update_max, fileLength);
            int threadCount = 3;//自定义线程数
            // 计算每条线程下载数据的大小
            int blockSize = fileLength / threadCount;
            // 启动线程下载
            for (int threadId = 1; threadId <= threadCount; threadId++) {
                // 核心代码，定义每个线程开始以及结束的下载位置
                int startPos = (threadId - 1) * blockSize;// 开始下载的位置
                int endPos = (threadId * blockSize) - 1;// 结束下载的位置（不包含最后一块）
                if (threadCount == threadId) {
                    endPos = fileLength;
                }
                new Thread(new DownLoadThread(threadId, startPos, endPos, PATH))
                        .start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void setUpdateCallBack(MainActivity.UpdateCallBack ucb) {
        updateCallBack = ucb;
    }

    static boolean isUpdating() {
        return isUpdating;
    }

    private static class DownLoadThread implements Runnable {
        private int threadId;
        private int startPos;
        private int endPos;
        private String path;
        private int threadTotal = 0;

        DownLoadThread(int threadId, int startPos, int endPos, String path) {
            super();
            this.threadId = threadId;
            this.startPos = startPos;
            this.endPos = endPos;
            this.path = path;
            threadTotal = endPos - startPos;
        }

        public void run() {
            File file = null;
            InputStream is = null;
            RandomAccessFile raf = null;

            try {
                URL url = new URL(path);
                String[] str = path.split("/");
                String fileName = str[str.length - 1];
                HttpURLConnection conn = (HttpURLConnection) url
                        .openConnection();
                // 设置URL请求的方法（具体参考API）
                conn.setRequestMethod("GET");
                // 设置500毫秒为超时值
                conn.setReadTimeout(5000);

                file = new File(Constants.PATH_YWD_FILES + threadId + ".txt");
                LogUtil.v(Tag, "file.exists:" + file.exists() + " ,file.length:" + file.length());
                if (file.exists() && file.length() > 0) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(new FileInputStream(file)));
                    String saveStartPos = br.readLine();
                    LogUtil.v(Tag, "线程" + threadId + " 已写：" + saveStartPos);
                    if (saveStartPos != null && saveStartPos.length() > 0) {
                        startPos = Integer.parseInt(saveStartPos);
                    }
                }
                progress.getAndAdd(threadTotal - (endPos - startPos));
                // 注意双引号内的格式，不能包含空格（等其他字符），否则报416
                conn.setRequestProperty("Range", "bytes=" + startPos + "-"
                        + endPos);
                raf = new RandomAccessFile(Constants.PATH_YWD_FILES + fileName, "rwd");// 存储下载文件的随机写入文件
                raf.seek(startPos);// 设置开始下载的位置
                LogUtil.v(Tag, "线程" + threadId + ":" + startPos + "~~"
                        + endPos);
                is = conn.getInputStream();
                byte[] b = new byte[1024 * 1024 * 10];
                int len;
                int newPos = startPos;
                RandomAccessFile rr;
                while ((len = is.read(b)) != -1) {
                    rr = new RandomAccessFile(file, "rwd");// 存储下载标记的文件
                    raf.write(b, 0, len);
                    // 将下载标记存入指定文档
                    String savePoint = String.valueOf(newPos += len);
                    rr.write(savePoint.getBytes());
                    progress.getAndAdd(len);
                    updateCallBack.update(progress.get(), Constants.PATH_YWD_FILES + fileName, progressMax);
                    rr.close();
                }

                LogUtil.v(Tag, "线程" + threadId + "下载完成");
                if (progress.get() >= progressMax)
                    isUpdating = false;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (is != null)
                        is.close();
                    if (raf != null)
                        raf.close();
                    file.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }

    static void unzipFile(String targetPath, String zipFilePath) {

        try {
            File zipFile = new File(zipFilePath);
            InputStream is = new FileInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(is);
            ZipEntry entry;
            LogUtil.v(Tag, "开始解压:" + zipFile.getName() + "...");
            while ((entry = zis.getNextEntry()) != null) {
                String zipPath = entry.getName();
                try {

                    if (entry.isDirectory()) {
                        File zipFolder = new File(targetPath + File.separator
                                + zipPath);
                        if (!zipFolder.exists()) {
                            zipFolder.mkdirs();
                        }
                    } else {
                        File file = new File(targetPath + File.separator
                                + zipPath);
                        if (!file.exists()) {
                            File pathDir = file.getParentFile();
                            pathDir.mkdirs();
                            file.createNewFile();
                        }

                        FileOutputStream fos = new FileOutputStream(file);
                        int bread;
                        while ((bread = zis.read()) != -1) {
                            fos.write(bread);
                        }
                        fos.close();

                    }
                    LogUtil.d(Tag, "成功解压:" + zipPath);

                } catch (Exception e) {
                    LogUtil.d(Tag, "解压" + zipPath + "失败");
//                    continue;
                }
            }
            zis.close();
            is.close();
            LogUtil.d(Tag, "解压结束");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
