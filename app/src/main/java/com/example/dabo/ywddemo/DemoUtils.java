package com.example.dabo.ywddemo;

import android.content.Context;
import android.widget.ProgressBar;

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

    static String RES_MD5 = "902eee7540c34c6ce1ef6646109daf80";//资源文件md5
    static boolean update;//资源文件是否要更新
    static boolean unzip;//资源文件是否压缩

    private static String fileName;
    private static int threadCount = 0;// 声明线程数量
    private static MainActivity.UpdateCallBack updateCallBack;
    private static AtomicInteger progress;
    private static ProgressBar pBar;
    private static boolean isUpdating = false;

    /**
     * @param PATH 下载地址    test:ftp://qxu1146240051@121.42.89.50/htdocs/res_new.zip
     */
    public static void startSychRes(String PATH, Context ctx) {
        progress = new AtomicInteger(0);
        isUpdating = true;
        try {
            URL url = new URL(PATH);
            // 获取连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setRequestMethod("POST");
//            conn.setRequestProperty("Accept-Encoding", "identity");
            // 通过获取连接定义文件名
            String[] str = PATH.split("/");
            fileName = str[str.length - 1];
            // 获取下载文件大小
            int fileLength = conn.getContentLength();
            pBar.setMax(fileLength);
//            System.out.println(fileName);
            File file = new File(Constants.PATH_YWD_FILES);
            if (!file.exists()) {
                file.mkdirs();
            }

            // 在本地创建一个与服务器大小一致的可随机写入文件
            RandomAccessFile raf = new RandomAccessFile(Constants.PATH_YWD_FILES + fileName, "rwd");
            System.out.println(fileLength);// 测试用
            raf.setLength(fileLength);
            ShareUtil.setInt(ctx, ShareUtil.update_max, fileLength);
            // 自定义线程数量
            threadCount = 3;
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

    public static void setUpdateCallBack(MainActivity.UpdateCallBack ucb, ProgressBar pb) {
        updateCallBack = ucb;
        pBar = pb;
    }

    public static boolean isUpdating() {
        return isUpdating;
    }

    static class DownLoadThread implements Runnable {
        private int threadId;
        private int startPos;
        private int endPos;
        private String path;
        private int threadTotal = 0;

        public DownLoadThread(int threadId, int startPos, int endPos, String path) {
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
                System.out.println("file.exists:" + file.exists() + " ,file.length:" + file.length());
                if (file.exists() && file.length() > 0) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(new FileInputStream(file)));
                    String saveStartPos = br.readLine();
                    System.out.println("线程" + threadId + " 已写：" + saveStartPos);
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
                System.out.println("线程" + threadId + ":" + startPos + "~~"
                        + endPos);
                is = conn.getInputStream();
                byte[] b = new byte[1024 * 1024 * 10];
                int len = -1;
                int newPos = startPos;
                RandomAccessFile rr;
                while ((len = is.read(b)) != -1) {
                    rr = new RandomAccessFile(file, "rwd");// 存储下载标记的文件
                    raf.write(b, 0, len);
                    // 将下载标记存入指定文档
                    String savaPoint = String.valueOf(newPos += len);
                    rr.write(savaPoint.getBytes());
                    progress.getAndAdd(len);
                    updateCallBack.update(progress.get(), Constants.PATH_YWD_FILES + fileName);
                    rr.close();
                }

                System.out.println("线程" + threadId + "下载完成");
                if (progress.get() >= pBar.getMax())
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

    public static void unzipFile(String targetPath, String zipFilePath) {

        try {
            File zipFile = new File(zipFilePath);
            InputStream is = new FileInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(is);
            ZipEntry entry = null;
            System.out.println("开始解压:" + zipFile.getName() + "...");
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
                    System.out.println("成功解压:" + zipPath);

                } catch (Exception e) {
                    System.out.println("解压" + zipPath + "失败");
                    continue;
                }
            }
            zis.close();
            is.close();
            System.out.println("解压结束");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
