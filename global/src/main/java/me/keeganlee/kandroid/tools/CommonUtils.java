package me.keeganlee.kandroid.tools;

import android.content.Context;
import android.util.TypedValue;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts.Photo;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
/**
 * Created by Administrator on 2017/8/7 0007.
 */

public class CommonUtils {
    public static boolean isStopGetFileSize = false;
    private static final String[] PHONES_PROJECTION = new String[] { Phone.DISPLAY_NAME,
            Phone.NUMBER, Photo.PHOTO_ID, Phone.CONTACT_ID };
    /**
     * 如果是自定义大小，dp转像素
     *
     * @param context
     * @return
     */
    public static int parsePxByDp(Context context, int dpValue) {
        int pxValue = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue,
                context.getResources().getDisplayMetrics());
        return pxValue;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static String convertTime(long value) {
        // 输出：修改时间[2] 2009-08-17 10:32:38
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = formatter.format(new java.util.Date(value));
        return time;
    }

    /**获取当前应用的版本号*/
    public static int getVersionCode(Context context,String pkgName) {
        try {
            return context.getPackageManager().getPackageInfo(
                    pkgName, 0).versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /*	public static void send(final String jSon) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        SelectionKey key = (SelectionKey)AppConfig.Session.get("SelectionKey");
                        if(key != null && key.channel() != null && key.channel().isOpen()){
                            SocketChannel sc = (SocketChannel) key.channel();
                            if(sc != null){
                                ByteBuffer contentBuff = ByteBuffer.wrap(jSon.getBytes("utf-8"));

                                BaseBetach baseBetach = new BaseBetach();
                                ByteBuffer headerBuff = baseBetach.getHeaderData(BaseBetach.I4ANDROID_E_CMD_SOCKET_LISTENER_RESP, contentBuff.capacity(), 2, 0, 0);
                                ByteBuffer buff = ByteBuffer.allocate(headerBuff.capacity() + contentBuff.capacity());
                                buff.put(headerBuff);
                                buff.put(contentBuff);
                                buff.rewind();

                                while(buff.hasRemaining()){
                                    sc.write(buff);
                                }

                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();

        }*/
    public static  int getNewSmsCount(Context context) {
        int result = 0;
        Cursor csr = context.getContentResolver().query(Uri.parse("content://sms"), null,
                "type = 1 and read = 0", null, null);
        if (csr != null) {
            result = csr.getCount();
            csr.close();
        }
        return result;
    }


    //***@param context**//

    public static int getAllSmsCount(Context context ) {
        int result = 0;
        Cursor csr = context.getContentResolver().query(Uri.parse("content://sms"), null,
                null , null, null);
        if (csr != null) {
            result = csr.getCount();
            csr.close();
        }
        return result;
    }

    //**读取通话记录个数**//
    public static int readCallRecordCount(Context context) {
        int result = 0;
        Cursor cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                null, null, null, null);
        if (cursor != null) {
            result = cursor.getCount();
            cursor.close();
        }
        return result;
    }



    /**根据联系人id获取图像**/
    public static Bitmap getContactPhoto(Context context, int contactId) {
        ContentResolver resolver = context.getContentResolver();
        // 得到联系人头像Bitamp
        Bitmap contactPhoto = null;
        // photoid 大于0 表示联系人有头像 如果没有给此人设置头像则给他一个默认的
        Uri uri = ContentUris.withAppendedId(
                ContactsContract.Contacts.CONTENT_URI, contactId);
        InputStream input = ContactsContract.Contacts
                .openContactPhotoInputStream(resolver, uri);
        contactPhoto = BitmapFactory.decodeStream(input);
//		if (contactPhoto == null) {
//			contactPhoto = BitmapFactory.decodeResource(context.getResources(),
//					R.drawable.logo);
//		}

        getPhoneContacts(context);

        return contactPhoto;
    }

    /**得到手机通讯录联系人信息**/
    private static void getPhoneContacts(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();

        // 获取手机联系人
        Cursor phoneCursor = resolver.query(Phone.CONTENT_URI,PHONES_PROJECTION, null, null, null);


        if (phoneCursor != null) {
            while (phoneCursor.moveToNext()) {

                //得到手机号码
                String phoneNumber = phoneCursor.getString(1);


                //得到联系人名称
                String contactName = phoneCursor.getString(0);

                //得到联系人ID
                Long contactid = phoneCursor.getLong(3);

                //得到联系人头像ID
                Long photoid = phoneCursor.getLong(2);

                //得到联系人头像Bitamp
                Bitmap contactPhoto = null;

                //photoid 大于0 表示联系人有头像 如果没有给此人设置头像则给他一个默认的
                if(photoid > 0 ) {
                    Uri uri =ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,contactid);
                    InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(resolver, uri);
                    contactPhoto = BitmapFactory.decodeStream(input);
                }

            }

            phoneCursor.close();
        }
    }

    /**
     * 根据后缀名判断是否是图片文件
     *
     * @param fileName
     * @return 是否是图片结果true or false
     */
    public static boolean isImage(String fileName) {
        if (fileName != null) {
            int typeIndex = fileName.lastIndexOf(".");
            if (typeIndex != -1) {
                String type = fileName.substring(typeIndex + 1).toLowerCase();
                if (type != null
                        && (type.equals("jpg") || type.equals("png")
									/*|| type.equals("gif") || type.equals("jpeg")
									|| type.equals("bmp") || type.equals("wbmp")
									|| type.equals("ico") || type.equals("jpe")*/)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static String getExtraName(String fileName){
        int typeIndex = fileName.lastIndexOf(".");
        if (typeIndex != -1) {
            return fileName.substring(typeIndex + 1).toLowerCase();
        }

        return fileName;
    }

    /**
     * 根据后缀名判断是否是图片文件
     *
     * @param fileName
     * @return 是否是图片结果true or false
     */
    public static boolean isVideo(String fileName) {
        if (fileName != null) {
            int typeIndex = fileName.lastIndexOf(".");
            if (typeIndex != -1) {
                String type = fileName.substring(typeIndex + 1).toLowerCase();
                if (type != null
                        && (type.equals("mp4")
									/*|| type.equals("gif") || type.equals("jpeg")
									|| type.equals("bmp") || type.equals("wbmp")
									|| type.equals("ico") || type.equals("jpe")*/)) {
                    return true;
                }
            }
        }

        return false;
    }
    /**
     * 获取文件类型
     * @return  VIDEO = 0;  AUDIO = 1;  IMAGES = 2;
     */
    public static int getFileType(String fileName) {
        if (fileName != null) {
            int typeIndex = fileName.lastIndexOf(".");
            if (typeIndex != -1) {
                String type = fileName.substring(typeIndex + 1).toLowerCase();

                if ("mp4".equals(type) || "avi".equals(type) || "3gp".equals(type) || "m4v".equals(type)) {
                    return 0;
                } else if ("mp3".equals(type) || "wma".equals(type) || "aac".equals(type) || "ogg".equals(type) ||
                        "wav".equals(type) || "m4a".equals(type) || "amr".equals(type) || "ape".equals(type) ||
                        "cue".equals(type) || "flac".equals(type) || "wac".equals(type)) {
                    return 1;
                } else if ("jpg".equals(type) || "jpeg".equals(type) || "png".equals(type) || "bmp".equals(type) ||
                        "gif".equals(type)) {
                    return 2;
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    /**
     * 根据指定的图像路径和大小来获取缩略图
     * 此方法有两点好处：
     *     1. 使用较小的内存空间，第一次获取的bitmap实际上为null，只是为了读取宽度和高度，
     *        第二次读取的bitmap是根据比例压缩过的图像，第三次读取的bitmap是所要的缩略图。
     *     2. 缩略图对于原图像来讲没有拉伸，这里使用了2.2版本的新工具ThumbnailUtils，使
     *        用这个工具生成的图像不会被拉伸。
     * @param imagePath 图像的路径
     * @param width 指定输出图像的宽度
     * @param height 指定输出图像的高度
     * @return 生成的缩略图
     */
    public static Bitmap getImageThumbnail(String imagePath, int width, int height) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        // 获取这个图片的宽和高，注意此处的bitmap为null
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        options.inJustDecodeBounds = false; // 设为 false
        // 计算缩放比
        int h = options.outHeight;
        int w = options.outWidth;
        int beWidth = w / width;
        int beHeight = h / height;
        int be = 1;
        if (beWidth < beHeight) {
            be = beWidth;
        } else {
            be = beHeight;
        }
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;
        // 重新读入图片，读取缩放后的bitmap，注意这次要把options.inJustDecodeBounds 设为 false
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        // 利用ThumbnailUtils来创建缩略图，这里要指定要缩放哪个Bitmap对象
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    /**
     * 获取视频的缩略图
     * 先通过ThumbnailUtils来创建一个视频的缩略图，然后再利用ThumbnailUtils来生成指定大小的缩略图。
     * 如果想要的缩略图的宽和高都小于MICRO_KIND，则类型要使用MICRO_KIND作为kind的值，这样会节省内存。
     * @param videoPath 视频的路径
     * @param width 指定输出视频缩略图的宽度
     * @param height 指定输出视频缩略图的高度度
     * @param kind 参照MediaStore.Images.Thumbnails类中的常量MINI_KIND和MICRO_KIND。
     *            其中，MINI_KIND: 512 x 384，MICRO_KIND: 96 x 96
     * @return 指定大小的视频缩略图
     */
    public static Bitmap getVideoThumbnail(String videoPath, int width, int height,
                                           int kind) {
        Bitmap bitmap = null;
        // 获取视频的缩略图
        bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }


    /**
     * @param context
     * @param cr
     * @param Videopath
     * @return
     */
    public static Bitmap getVideoThumbnail(Context context, ContentResolver cr, String Videopath) {
        ContentResolver testcr = context.getContentResolver();
        String[] projection = { MediaStore.Video.Media.DATA, MediaStore.Video.Media._ID, };
        String whereClause = MediaStore.Video.Media.DATA + " = '" + Videopath + "'";
        Cursor cursor = testcr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, whereClause,
                null, null);
        int _id = 0;
        String videoPath = "";
        if (cursor == null || cursor.getCount() == 0) {
            return null;
        }
        if (cursor.moveToFirst()) {

            int _idColumn = cursor.getColumnIndex(MediaStore.Video.Media._ID);
            int _dataColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
            do {
                _id = cursor.getInt(_idColumn);
                videoPath = cursor.getString(_dataColumn);
            } while (cursor.moveToNext());
        }
        cursor.close();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = MediaStore.Video.Thumbnails.getThumbnail(cr, _id, Images.Thumbnails.MINI_KIND,
                options);
        return bitmap;
    }

    // 获取应用程序
    public static int getAppNum(Context context,
                                boolean hasSystemPackage , boolean hasSelfApp) {
        int rntValue = 0;
        Log.d("i4", context.getPackageName());

        PackageManager pkManager = context.getPackageManager();

        List<PackageInfo> packageInfos = pkManager.getInstalledPackages(0);
        for (int i = 0; i < packageInfos.size(); i++) {
            PackageInfo pInfo = packageInfos.get(i);
            if (!hasSystemPackage) {
                // 不包含系统应用程序
                if ((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                    continue;
            }

            if (!hasSelfApp && pInfo.packageName.equalsIgnoreCase(context.getPackageName()))
                continue;

            rntValue ++;
        }
        return rntValue;
    }

}
