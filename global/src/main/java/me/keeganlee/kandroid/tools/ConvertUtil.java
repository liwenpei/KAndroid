package me.keeganlee.kandroid.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Calendar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class ConvertUtil {

    public static final int DOWNLOADID = 1000001;
    public static final int DOWNLOADEDID = 1000002;
    //媒体信息
    public static final int VIDEO = 0;
    public static final int AUDIO = 1;
    public static final int IMAGES = 2;

    final static int BUFFER_SIZE = 4096;

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap
                .createBitmap(
                        drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight(),
                        drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                                : Bitmap.Config.RGB_565);
        return bitmap;
    }

    public static Bitmap bytesToBimap(byte[] b) {
        if (b != null && b.length != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } else {
            return null;
        }
    }

    public static Drawable bitmapToDrawable(Context context, Bitmap bit) {
        BitmapDrawable bd = new BitmapDrawable(context.getResources(), bit);
        return bd;
    }

    public static byte[] bitmapToBytes(Bitmap bm) {
        if (bm != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
            return baos.toByteArray();
        } else {
            return null;
        }

    }


    public static Object autoConvert(String str) {
        if (isNumeric(str)) {
            return objToInt(str);
        } else {
            return str;
        }
    }

    public static boolean isNumeric(String str) {
        if (str == null || "".equals(str.trim())) {
            return false;
        }
        for (int i = str.length(); --i >= 0; ) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    //判断是否在指定之间内有效
    public static boolean isValiteTime(int startHour, int endHour) {
        boolean isValite = false;
        Calendar cal = Calendar.getInstance();// 当前日期
        int hour = cal.get(Calendar.HOUR_OF_DAY);// 获取小时
        int minute = cal.get(Calendar.MINUTE);// 获取分钟
        int minuteOfDay = hour * 60 + minute;// 从0:00分开是到目前为止的分钟数
        final int start = startHour * 60 + 00;// 起始时间
        final int end = endHour * 60;// 结束时间

        if (minuteOfDay >= start && minuteOfDay <= end) {
            isValite = true;
        } else {
            isValite = false;
        }

        return isValite;
    }

    /**
     * byte数转成Milion数
     */
    public static String convertByteToMilion(Object b) {
        String rntValue = "";
        try {
            DecimalFormat df = new DecimalFormat("##0.00");
            rntValue = df.format((float) (Integer.parseInt(b.toString())) / (1024 * 1024)) + "M";
        } catch (Exception ex) {
        }

        return rntValue;

    }

    /**
     * byte数转成Milion数 没有单位
     */
    public static String convertByteToMilionWithoutM(Object b) {
        String rntValue = "";
        try {
            DecimalFormat df = new DecimalFormat("##0.00");
            rntValue = df.format((float) (Integer.parseInt(b.toString())) / (1024 * 1024));
        } catch (Exception ex) {
        }

        return rntValue;

    }

    /***
     * byte数转成kilo数
     * **/
    public static String convertByteToKilo(int b) {

        String rntValue = "";
        try {
            DecimalFormat df = new DecimalFormat("##0.00");
            rntValue = df.format((float) (b) / (1024)) + "K";
        } catch (Exception ex) {
        }

        return rntValue;
    }

    /***
     * 个数转成万数
     * **/
    public static String convertGeShuToWang(Object b) {
        String rntValue = "";
        try {
            int tmpB = Integer.parseInt(b.toString());
            DecimalFormat df = new DecimalFormat("##0.0");
            if (tmpB >= 10000 && tmpB < 100000000) {
                rntValue = df.format((float) (tmpB) / (10000)) + "万";
            } else if (tmpB >= 100000000) {
                rntValue = df.format((float) (tmpB) / (100000000)) + "亿";
            } else {
                rntValue = tmpB + "";
            }

        } catch (Exception ex) {
        }

        return rntValue;
    }

    /**
     * 将InputStream转换成String
     *
     * @param in InputStream
     * @return String
     * @throws Exception
     */
    public static String InputStreamTOString(InputStream in) throws IOException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] data = new byte[BUFFER_SIZE];
        int count = -1;
        while ((count = in.read(data, 0, BUFFER_SIZE)) != -1)
            outStream.write(data, 0, count);

        data = null;
        return new String(outStream.toByteArray(), "UTF-8");
    }

    /**
     * 将InputStream转换成某种字符编码的String
     *
     * @param in
     * @param encoding
     * @return
     * @throws Exception
     */
    public static String InputStreamTOString(InputStream in, String encoding)
            throws Exception {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] data = new byte[BUFFER_SIZE];
        int count = -1;
        while ((count = in.read(data, 0, BUFFER_SIZE)) != -1)
            outStream.write(data, 0, count);

        data = null;
        return new String(outStream.toByteArray(), "ISO-8859-1");
    }

    /**
     * 将String转换成InputStream
     *
     * @param in
     * @return
     * @throws Exception
     */
    public static InputStream StringTOInputStream(String in) throws Exception {

        ByteArrayInputStream is = new ByteArrayInputStream(
                in.getBytes("ISO-8859-1"));
        return is;
    }

    /**
     * 将InputStream转换成byte数组
     *
     * @param in InputStream
     * @return byte[]
     * @throws IOException
     */
    public static byte[] InputStreamTOByte(InputStream in) throws IOException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] data = new byte[BUFFER_SIZE];
        int count = -1;
        while ((count = in.read(data, 0, BUFFER_SIZE)) != -1)
            outStream.write(data, 0, count);

        data = null;
        return outStream.toByteArray();
    }

    /**
     * 将byte数组转换成InputStream
     *
     * @param in
     * @return
     * @throws Exception
     */
    public static InputStream byteTOInputStream(byte[] in) throws IOException {

        ByteArrayInputStream is = new ByteArrayInputStream(in);
        return is;
    }

    /**
     * 将byte数组转换成String
     *
     * @param in
     * @return
     * @throws Exception
     */
    public static String byteTOString(byte[] in) throws IOException {

        InputStream is = byteTOInputStream(in);
        return InputStreamTOString(is);
    }

    public static int objToInt(Object obj) {
        int rntValue = 0;
        try {
            if (obj != null) {
                if (obj instanceof Boolean) {
                    rntValue = Boolean.getBoolean(obj.toString()) ? 0 : 1;
                } else {
                    rntValue = Integer.parseInt(obj.toString());
                }
            }
        } catch (Exception e) {
        }

        return rntValue;
    }

    public static int doubleToInt(double d) {
        Double D = d;
        return D.intValue();
    }

    public static Long objToLong(Object str) {
        if (str != null) {
            return Long.parseLong(str.toString());
        } else {
            return 0L;
        }
    }

    /**
     * 转换成16进制,获取颜色参数
     */
    public static int getColorInt(String colorStr) {
        if (colorStr != null && "".equals(colorStr.trim())) {
            return new java.math.BigInteger(colorStr, 16).intValue();
        } else {
            return 0;
        }

    }

    public static String objToString(Object obj) {
        if (obj != null) {
            return obj.toString();
        }

        return null;
    }


    /**
     * stream to byte
     **/
    public static byte[] readBytes(InputStream in, long length)
            throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read = 0;
        while (read < length) {
            int cur = in.read(buffer, 0, (int) Math.min(1024, length - read));
            if (cur < 0) {
                break;
            }
            read += cur;
            bo.write(buffer, 0, cur);
        }
        return bo.toByteArray();
    }

    /**
     * int to byte
     **/
    public static byte[] integerToBytes(int integer, int len) {
        // if (integer < 0) { throw new
        // IllegalArgumentException("Can not cast negative to bytes : " +
        // integer); }
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        for (int i = 0; i < len; i++) {
            bo.write(integer);
            integer = integer >> 8;
        }
        return bo.toByteArray();
    }

    /**
     * int to byte  将int值转成一个4字节的byte数组
     **/
    public static byte[] intToByteArray(long i) {
        byte[] result = new byte[4];
        result[3] = (byte) ((i >> 24) & 0xFF);
        result[2] = (byte) ((i >> 16) & 0xFF);
        result[1] = (byte) ((i >> 8) & 0xFF);
        result[0] = (byte) (i & 0xFF);
        return result;
    }

    /**
     * long to byte  将int值转成一个4字节的byte数组
     **/
    public static byte[] longToByteArray(long i) {
        byte[] result = new byte[8];
        result[7] = (byte) ((i >> 56) & 0xFF);
        result[6] = (byte) ((i >> 48) & 0xFF);
        result[5] = (byte) ((i >> 40) & 0xFF);
        result[4] = (byte) ((i >> 32) & 0xFF);
        result[3] = (byte) ((i >> 24) & 0xFF);
        result[2] = (byte) ((i >> 16) & 0xFF);
        result[1] = (byte) ((i >> 8) & 0xFF);
        result[0] = (byte) (i & 0xFF);
        return result;
    }

    /**
     * 将4字节的byte数组转成一个int值
     *
     * @param bRefArr
     * @return
     */
    public static int byteArrayToint(byte[] bRefArr) {
        byte[] a = new byte[4];
        int i = a.length - 1, j = 0;//bRefArr.length - 1;
        for (; i >= 0; i--, j++) {//从b的尾部(即int值的低位)开始copy数据
            if (j < bRefArr.length)
                a[i] = bRefArr[j];
            else
                a[i] = 0;//如果b.length不足4,则将高位补0
        }
        int v0 = (a[0] & 0xff) << 24;//&0xff将byte值无差异转成int,避免Java自动类型提升后,会保留高位的符号位
        int v1 = (a[1] & 0xff) << 16;
        int v2 = (a[2] & 0xff) << 8;
        int v3 = (a[3] & 0xff);
        return v0 + v1 + v2 + v3;
    }

    /**
     * 将8字节的byte数组转成一个long值
     *
     * @param bRefArr
     * @return
     */
    public static long byteArrayToLong(byte[] bRefArr) {
        byte[] a = new byte[8];
        int i = a.length - 1, j = 0;//bRefArr.length - 1;
        for (; i >= 0; i--, j++) {//从b的尾部(即int值的低位)开始copy数据
            if (j < bRefArr.length)
                a[i] = bRefArr[j];
            else
                a[i] = 0;//如果b.length不足4,则将高位补0
        }
        long v0 = (a[0] & 0xff) << 56;
        long v1 = (a[1] & 0xff) << 48;
        long v2 = (a[2] & 0xff) << 40;
        long v3 = (a[3] & 0xff) << 32;
        long v4 = (a[4] & 0xff) << 24;
        long v5 = (a[5] & 0xff) << 16;
        long v6 = (a[6] & 0xff) << 8;
        long v7 = (a[7] & 0xff);
        return v0 + v1 + v2 + v3 + v4 + v5 + v6 + v7;
    }

    /**
     * String or null -> String
     */
    public static String noEmptyString(String str) {
        if (str == null) {
            return "";
        } else {
            return str;
        }
    }


    // 将byte[]转换成InputStream
    public static InputStream Byte2InputStream(byte[] b) {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        return bais;
    }

    // 将InputStream转换成byte[]
    public static byte[] InputStream2Bytes(InputStream is) {
        String str = "";
        byte[] readByte = new byte[1024];
        int readCount = -1;
        try {
            while ((readCount = is.read(readByte, 0, 1024)) != -1) {
                str += new String(readByte).trim();
            }
            return str.getBytes();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 将Bitmap转换成InputStream
    public static InputStream Bitmap2InputStream(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        return is;
    }

    // 将Bitmap转换成InputStream
    public static InputStream Bitmap2InputStream(Bitmap bm, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, quality, baos);
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        return is;
    }

    // 将InputStream转换成Bitmap
    public static Bitmap InputStream2Bitmap(InputStream is) {
        return BitmapFactory.decodeStream(is);
    }

    // Drawable转换成InputStream
    public static InputStream Drawable2InputStream(Drawable d) {
        Bitmap bitmap = drawable2Bitmap(d);
        return Bitmap2InputStream(bitmap);
    }

    // InputStream转换成Drawable
    public static Drawable InputStream2Drawable(InputStream is) {
        Bitmap bitmap = InputStream2Bitmap(is);
        return bitmap2Drawable(bitmap);
    }

    // Drawable转换成byte[]
    public static byte[] Drawable2Bytes(Drawable d) {
        Bitmap bitmap = drawable2Bitmap(d);
        return Bitmap2Bytes(bitmap);
    }

    // byte[]转换成Drawable
    public static Drawable Bytes2Drawable(byte[] b) {
        Bitmap bitmap = Bytes2Bitmap(b);
        return bitmap2Drawable(bitmap);
    }

    // Bitmap转换成byte[]
    public static byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    // byte[]转换成Bitmap
    public static Bitmap Bytes2Bitmap(byte[] b) {
        if (b.length != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        }
        return null;
    }

    // Drawable转换成Bitmap
    public static Bitmap drawable2Bitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap
                .createBitmap(
                        drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight(),
                        drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                                : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    // Bitmap转换成Drawable
    public static Drawable bitmap2Drawable(Bitmap bitmap) {
        BitmapDrawable bd = new BitmapDrawable(bitmap);
        Drawable d = (Drawable) bd;
        return d;
    }

}

