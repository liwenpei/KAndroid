package me.keeganlee.kandroid.tools;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;

public class FileUtil {

    public static final int SIZETYPE_B = 1;// 获取文件大小单位为B的double值
    public static final int SIZETYPE_KB = 2;// 获取文件大小单位为KB的double值
    public static final int SIZETYPE_MB = 3;// 获取文件大小单位为MB的double值
    public static final int SIZETYPE_GB = 4;// 获取文件大小单位为GB的double值

    /**
     * 获取文件指定文件的指定单位的大小
     *
     * @param filePath 文件路径
     * @param sizeType 获取大小的类型1为B、2为KB、3为MB、4为GB
     * @return double值的大小
     */

    public static double getFileOrFilesSize(String filePath, int sizeType) {

        File file = new File(filePath);

        long blockSize = 0;

        try {

            if (file.isDirectory()) {

                blockSize = getFileSizes(file);

            } else {

                blockSize = getFileSize(file);

            }

        } catch (Exception e) {

            e.printStackTrace();

            Log.e("获取文件大小", "获取失败!");

        }

        return FormetFileSize(blockSize, sizeType);

    }

    /**
     * 调用此方法自动计算指定文件或指定文件夹的大小
     *
     * @param filePath 文件路径
     * @return 计算好的带B、KB、MB、GB的字符串
     */

    public static String getAutoFileOrFilesSize(String filePath) {

        File file = new File(filePath);

        long blockSize = 0;

        try {

            if (file.isDirectory()) {

                blockSize = getFileSizes(file);

            } else {

                blockSize = getFileSize(file);

            }

        } catch (Exception e) {

            e.printStackTrace();

            Log.e("获取文件大小", "获取失败!");

        }

        return FormetFileSize(blockSize);

    }

    /**
     * 获取指定文件大小
     *
     * @param file
     * @return
     * @throws Exception
     */

    private static long getFileSize(File file) throws Exception

    {

        long size = 0;

        if (file.exists()) {

            FileInputStream fis = null;

            fis = new FileInputStream(file);

            size = fis.available();
            fis.close();
        } else {

            file.createNewFile();

            Log.e("获取文件大小", "文件不存在!");

        }

        return size;

    }

    /**
     * 获取指定文件夹
     *
     * @param f
     * @return
     * @throws Exception
     */

    private static long getFileSizes(File f) throws Exception {

        long size = 0;

        File flist[] = f.listFiles();

        for (int i = 0; i < flist.length; i++) {

            if (CommonUtils.isStopGetFileSize) break;

            if (flist[i].isDirectory()) {
                size = size + getFileSizes(flist[i]);
            } else {

                size = size + getFileSize(flist[i]);

            }
        }

        return size;

    }

    /**
     * 转换文件大小
     *
     * @param fileS
     * @return
     */

    private static String FormetFileSize(long fileS)

    {

        DecimalFormat df = new DecimalFormat("#.00");

        String fileSizeString = "";

        String wrongSize = "0B";

        if (fileS == 0) {

            return wrongSize;

        }

        if (fileS < 1024) {

            fileSizeString = df.format((double) fileS) + "B";

        } else if (fileS < 1048576) {

            fileSizeString = df.format((double) fileS / 1024) + "KB";

        } else if (fileS < 1073741824) {

            fileSizeString = df.format((double) fileS / 1048576) + "MB";

        } else {

            fileSizeString = df.format((double) fileS / 1073741824) + "GB";

        }

        return fileSizeString;

    }

    /**
     * 转换文件大小,指定转换的类型
     *
     * @param fileS
     * @param sizeType
     * @return
     */

    private static double FormetFileSize(long fileS, int sizeType)

    {

        DecimalFormat df = new DecimalFormat("#.00");

        double fileSizeLong = 0;

        switch (sizeType) {

            case SIZETYPE_B:

                fileSizeLong = Double.valueOf(df.format((double) fileS));

                break;

            case SIZETYPE_KB:

                fileSizeLong = Double.valueOf(df.format((double) fileS / 1024));

                break;

            case SIZETYPE_MB:

                fileSizeLong = Double.valueOf(df.format((double) fileS / 1048576));

                break;

            case SIZETYPE_GB:

                fileSizeLong = Double.valueOf(df
                        .format((double) fileS / 1073741824));

                break;

            default:

                break;

        }

        return fileSizeLong;

    }

    /**
     * 递归删除文件和文件夹
     *
     * @param file 要删除的根目录
     */
    public static void deleteFile(File file) {
        if (file.exists() == false) {
            return;
        } else {
            if (file.isFile()) {
                file.delete();
                return;
            }
            if (file.isDirectory()) {
                File[] childFile = file.listFiles();
                if (childFile == null || childFile.length == 0) {
                    file.delete();
                    return;
                }
                for (File f : childFile) {
                    deleteFile(f);
                }

                file.delete();
            }
        }
    }

    /**
     * 创建文件
     *
     * @throws IOException
     **/
    public static boolean creatNewFile(File file) throws IOException {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        return file.createNewFile();
    }

    public static String getNameWithoutExtra(String fileName) {
        int typeIndex = fileName.lastIndexOf(".");
        if (typeIndex != -1) {
            return fileName.substring(0, typeIndex);
        }

        return fileName;
    }

    /**
     * @param context 句柄 filePath 安装文件路径
     **/
    public static boolean installFile(Context context, String filePath) {
        boolean rntValue = false;
        File file = new File(filePath);
        if (file != null && file.exists()) {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(filePath)),
                    "application/vnd.android.package-archive");
            context.startActivity(intent);

            rntValue = true;
        }

        return rntValue;
    }

    /**
     * 卸载应用程序 uninstall apk file
     *
     * @param packageName
     */
    public static void uninstallAPK(Context context, String packageName) {
        Uri uri = Uri.parse("package:" + packageName);
        Intent intent = new Intent(Intent.ACTION_DELETE, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

}
