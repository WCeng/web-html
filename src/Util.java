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
        //mPath??????????????????
        Log.d(TAG, "outImageBitmap: "+path);
        mMetadataRetriever.setDataSource(path);
        //???????????????????????????mMetadataRetriever?????????????????????????????????????????????
//        String duration = mMetadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);//??????(??????)
//        String width = mMetadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);//???
//        String height = mMetadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);//???
//        Log.d(TAG, "outImageBitmap: "+duration);
        //?????????????????????????????????????????????????????????????????????
        //?????????????????????????????????????????????????????????????????????(??????*1000)?????????
        Bitmap bitmap = mMetadataRetriever.getFrameAtTime((timeUS * 1000000), MediaMetadataRetriever.OPTION_CLOSEST);
        //????????????????????????????????????????????????bitmap???
        return bitmap;
    }

    public static String getLongTime() {
        String ms = mMetadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
        int ms1 = Integer.parseInt(ms);
        return convertMillis(ms1);//??????(??????)
    }

    public static Bitmap cropBitmap(Bitmap bitmap) {//??????????????????????????????
        int w = bitmap.getWidth(); // ????????????????????????
        int h = bitmap.getHeight();
        int cropWidth = w >= h ? h : w;// ???????????????????????????????????????

        return Bitmap.createBitmap(bitmap, (bitmap.getWidth() - cropWidth) / 2,
                (bitmap.getHeight() - cropWidth) / 2, cropWidth, cropWidth);
    }

    public static Bitmap getCircleBitmap(Bitmap bitmap) {//????????????????????????
        if (bitmap == null) {
            return null;
        }
        bitmap = cropBitmap(bitmap);//??????????????????
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
        //???????????????????????????????????????"mm:ss"
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        //?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String hms = formatter.format(ms);
        return hms;
    }


    /**
     *
     * @param filepath m3u8??????????????????DLManager
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
