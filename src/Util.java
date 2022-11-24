package com.example.translation.netUtil;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.SequenceInputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

public class Util {
    static MediaMetadataRetriever mMetadataRetriever;
    static String key;
    static String secretFolder = Environment.getExternalStorageDirectory()+"/video secret";

    public static String getFileSize(long size) {
        StringBuffer bytes = new StringBuffer();
        DecimalFormat format = new DecimalFormat("###.00");
        if (size >= 1024 * 1024 * 1024) {
            double i = (size / (1024.0 * 1024.0 * 1024.0));
            bytes.append(format.format(i)).append("GB");
        } else if (size >= 1024 * 1024) {
            double i = (size / (1024.0 * 1024.0));
            bytes.append(format.format(i)).append("MB");
        } else if (size >= 1024) {
            double i = (size / (1024.0));
            bytes.append(format.format(i)).append("KB");
        } else if (size < 1024) {
            if (size <= 0) {
                bytes.append("0B");
            } else {
                bytes.append((int) size).append("B");
            }
        }
        return bytes.toString();
    }

    public static Bitmap outImageBitmap(String path, long timeUS) {
        if (mMetadataRetriever == null)
            mMetadataRetriever = new MediaMetadataRetriever();
        //mPath本地视频地址
        Log.d(TAG, "outImageBitmap: "+path);
        mMetadataRetriever.setDataSource(path);
        //这个时候就可以通过mMetadataRetriever来获取这个视频的一些视频信息了
//        String duration = mMetadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);//时长(毫秒)
//        String width = mMetadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);//宽
//        String height = mMetadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);//高
//        Log.d(TAG, "outImageBitmap: "+duration);
        //上面三行代码可以获取这个视频的宽高和播放总时长
        //下面这行代码才是关键，用来获取当前视频某一时刻(毫秒*1000)的一帧
        Bitmap bitmap = mMetadataRetriever.getFrameAtTime((timeUS * 1000000), MediaMetadataRetriever.OPTION_CLOSEST);
        //这时就可以获取这个视频的某一帧的bitmap了
        return bitmap;
    }

    public static String getLongTime() {
        String ms = mMetadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
        int ms1 = Integer.parseInt(ms);
        return convertMillis(ms1);//时长(毫秒)
    }

    public static Bitmap cropBitmap(Bitmap bitmap) {//从中间截取一个正方形
        int w = bitmap.getWidth(); // 得到图片的宽，高
        int h = bitmap.getHeight();
        int cropWidth = w >= h ? h : w;// 裁切后所取的正方形区域边长

        return Bitmap.createBitmap(bitmap, (bitmap.getWidth() - cropWidth) / 2,
                (bitmap.getHeight() - cropWidth) / 2, cropWidth, cropWidth);
    }

    public static Bitmap getCircleBitmap(Bitmap bitmap) {//把图片裁剪成圆形
        if (bitmap == null) {
            return null;
        }
        bitmap = cropBitmap(bitmap);//裁剪成正方形
        try {
            Bitmap circleBitmap = Bitmap.createBitmap(bitmap.getWidth(),
                    bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(circleBitmap);
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, bitmap.getWidth(),
                    bitmap.getHeight());
            final RectF rectF = new RectF(new Rect(0, 0, bitmap.getWidth(),
                    bitmap.getHeight()));
            float roundPx = 0.0f;
            roundPx = bitmap.getWidth();
            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(Color.WHITE);
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            final Rect src = new Rect(0, 0, bitmap.getWidth(),
                    bitmap.getHeight());
            canvas.drawBitmap(bitmap, src, rect, paint);
            return circleBitmap;
        } catch (Exception e) {
            return bitmap;
        }
    }

    public static String convertMillis(long ms) {
        //这里想要只保留分秒可以写成"mm:ss"
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        //这里很重要，如果不设置时区的话，输出结果就会是几点钟，而不是毫秒值对应的时分秒数量了。
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String hms = formatter.format(ms);
        return hms;
    }


    /**
     *
     * @param filepath m3u8位于的夫目录DLManager
     */
    public static void readAndWrite(String filepath) {
        if(!new File(filepath).exists())
            return;
        File outfile = new File(secretFolder, new File(filepath).getName().split(".m3u8")[0] + ".secret");

        if(outfile.exists())
            return;

        List<File> smallFiles = outSmallFileList(filepath);
        try (FileOutputStream fos = new FileOutputStream(outfile, true);) {

            for (File f : smallFiles){
                FileInputStream fileInputStream = new FileInputStream(f);
                byte b[]=new byte[4096];
                int size=-1;
                ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
                while((size=fileInputStream.read(b,0,b.length))!=-1) {
                    byteArrayOutputStream.write(b,0,size);
                }
                fileInputStream.close();
                byte[] bytes = byteArrayOutputStream.toByteArray();
                byteArrayOutputStream.close();
                byte [] newbyte;
                    newbyte = AesUtil.aesDecry(bytes, key);
                fos.write(newbyte);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<File> outSmallFileList(String filePath) {
        List<File> files = new ArrayList<>();
        String path = filePath;

        File indexFile = new File(path);
        try(FileReader fs = new FileReader(indexFile);
            BufferedReader br = new BufferedReader(fs)){
            String line = "";
            while ((line = br.readLine()) != null){
                if(line.startsWith("#EXT-X-KEY:")){
                    String keyFilePath = line.split("\"")[1];
                    try(BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(keyFilePath)))){
                        key = bufferedReader.readLine();
                    }
                }
                if (line.startsWith("file://")){
                    String tsPath = line.split("file://")[1];
                    files.add(new File(tsPath));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return files;
    }

}
