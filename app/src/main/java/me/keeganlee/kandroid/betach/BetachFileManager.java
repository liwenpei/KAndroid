package me.keeganlee.kandroid.betach;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import me.keeganlee.kandroid.tools.CommonUtils;
import me.keeganlee.kandroid.tools.ConvertUtil;
import me.keeganlee.kandroid.tools.LogUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import me.keeganlee.kandroid.tools.FileUtil;

public class BetachFileManager extends BaseBetach{
    private Context context;
    private int space = 1024 * 64;
    public static long copyFileCount = 0;
    public static long copyByteCount = 0;
    public static boolean isStopThread = false;
    private byte[] buffer = new byte[space];
    private ByteBuffer byteBuffer = ByteBuffer.allocate(space);
    private ByteBuffer headerByteBuff = ByteBuffer.allocateDirect(BaseBetach.HEADER_LENGTH);
    private JSONArray expDataObj = new JSONArray();

    public static final int RINGTONE = 0;                   //铃声
    public static final int NOTIFICATION = 1;               //通知音
    public static final int ALARM = 2;                      //闹钟
    public static final int ALL = 3;                        //所有声音

    public BetachFileManager(Context context){
        this.context = context;
    }


    /** 发送基本成功数据数据 **/
    public byte[] getReponseRootFileData() {
        byte[] rntValue = null;
        try {
            // 请求手机短信、联系人、通话记录的个数
            JSONObject obj = new JSONObject();
            JSONArray arr = new JSONArray();
            int rtnInt = 0;
            String reason = "";

            if(ConvertUtil.objToInt(android.os.Build.VERSION.SDK) >= 11){
                //支持 android  3.0 以上
                // 获取sdcard的路径：外置和内置
                try {
                    StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
                    String[] paths = (String[]) sm.getClass().getMethod("getVolumePaths").invoke(sm);
                    for(int i = 0; i < paths.length ; i++){
                        File tmpFile = new File(paths[i]);
                        File[] listFile = tmpFile.listFiles();
                        if(listFile != null && listFile.length > 0){
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("path", tmpFile.getPath());
                            arr.put(jsonObj);
                        }
                    }
                } catch (IllegalArgumentException e1) {
                    rtnInt = -1;
                    reason = e1.getMessage();
                } catch (SecurityException e1) {
                    rtnInt = -1;
                    reason = e1.getMessage();
                } catch (IllegalAccessException e1) {
                    rtnInt = -1;
                    reason = e1.getMessage();
                } catch (InvocationTargetException e1) {
                    rtnInt = -1;
                    reason = e1.getMessage();
                } catch (NoSuchMethodException e1) {
                    rtnInt = -1;
                    reason = e1.getMessage();
                }
            }else{
                //支持 android  3.0 以下
                File innerDir = Environment.getExternalStorageDirectory();
                File rootDir = innerDir.getParentFile();
                File[] files = rootDir.listFiles();
                for(int i = 0; i < files.length ; i++){
                    if(files[i].getName() != null && files[i].getName().toLowerCase().contains("sdcard")){
                        JSONObject jsonObj = new JSONObject();
                        jsonObj.put("path", files[i].getPath());
                        arr.put(jsonObj);
                    }
                }
            }

            obj.put("return", rtnInt);
            obj.put("reason", reason);
            obj.put("data", arr);

            int length = obj.toString().getBytes("UTF8").length;

            ByteBuffer byteBuff = ByteBuffer.allocate(HEADER_LENGTH + length);
            // 返回头部位坐标0:响应命令
            byteBuff.put((byte) I4ANDROID_E_CMD_FILES_ROOT_RESP);
            // 返回头部位坐标1-4:内容长度
            byteBuff.put(ConvertUtil.intToByteArray(length));
            // 返回头部位坐标5:内容类型
            byteBuff.put((byte) TYPE_JSON);
            // 返回头部位坐标6:拓展字节
            byteBuff.put((byte) 0);
            // 返回头部位坐标7:拓展字节
            byteBuff.put((byte) 0);
            // 返回数据部
            byteBuff.put(obj.toString().getBytes("UTF8"));
            LogUtil.debug("输出字符" + ":" + obj.toString());

            rntValue = byteBuff.array();
        } catch (Exception ex) {
            LogUtil.debug("发送sorket内容出错" + ex.getMessage());
        }

        return rntValue;
    }

    /**接收文件导入资源**/
    public void recFileResourceResp(String path,SocketChannel sc,RandomAccessFile randomFile){
        ByteBuffer byteBuff = ByteBuffer.allocateDirect(BaseBetach.HEADER_LENGTH);

        // 返回头部位坐标0:响应命令
        byteBuff.put((byte) BaseBetach.I4ANDROID_E_CMD_FILES_RESOURCE_IMPORT_RESP);
        // 返回头部位坐标1-4:内容长度

        int requestLength = 0;

        byteBuff.put(ConvertUtil.intToByteArray(requestLength));
        // 返回头部位坐标5:内容类型
        byteBuff.put((byte)BaseBetach.TYPE_APK);
        // 返回头部位坐标6:拓展字节
        byteBuff.put((byte) 0);
        // 返回头部位坐标7:拓展字节
        byteBuff.put((byte) 0);

        byteBuff.rewind();
        byteBuff.limit(8);
    }

    /****发送文件资源**********
     * @throws IOException ***/
    public void getFileResourceResp(String path,String range,SocketChannel sc,FileInputStream fIs) throws IOException{

        long startPos = ConvertUtil.objToLong(range.substring("bytes=".length(),range.indexOf("-")));
        long endPos = ConvertUtil.objToLong(range.substring(range.indexOf("-")+1));
        long qstCount = endPos - startPos + 1;
        //获取path的数据

        // 返回头部位坐标0:响应命令
        headerByteBuff.put((byte) BaseBetach.I4ANDROID_E_CMD_FILES_RESOURCE_EXPORT_RESP);
        // 返回头部位坐标1-4:内容长度
        long requestLength = qstCount;
        //if(ConvertUtil.objToLong(randomFile.length()) < qstCount){
        //requestLength = ConvertUtil.objToInt(randomFile.length());
        //}

        headerByteBuff.put(ConvertUtil.intToByteArray(requestLength));
        // 返回头部位坐标5:内容类型
        headerByteBuff.put((byte)BaseBetach.TYPE_APK);
        // 返回头部位坐标6:拓展字节
        headerByteBuff.put((byte) 0);
        // 返回头部位坐标7:拓展字节
        headerByteBuff.put((byte) 0);

        headerByteBuff.rewind();
        headerByteBuff.limit(8);

        LogUtil.debug("send 头部数据 ");
        while(headerByteBuff.hasRemaining()){
            sc.write(headerByteBuff);
        }
        headerByteBuff.clear();

        LogUtil.debug("send 头部数据结束");

        //int countSpace = 0;
        //if(endPos == 0){
        //endPos = ConvertUtil.objToLong(randomFile.length()) - 1;
        //}

        try {
            LogUtil.debug("send 数据体");
            //int space = 64*1024;
            //int spaceCount = (int)(qstCount/space);
            //int remainCount = (int)(qstCount % space);

            //byte[] dataBuffer =  new byte[space];
            //ByteBuffer tmpBuff = ByteBuffer.allocate(space);
            int size = 0;
            //randomFile.seek(startPos);
            fIs.skip(startPos);
            FileChannel inChannel = fIs.getChannel();
            while ((size = inChannel.read(byteBuffer)) != -1) {
//				if(countSpace == spaceCount){
//					if(remainCount != 0){
//						if(size <= remainCount){
//							byteBuffer.put(buffer,0,size);
//							byteBuffer.rewind();
//							byteBuffer.limit(size);
//
//						}else{
//							byteBuffer.put(buffer,0,remainCount);
//							byteBuffer.rewind();
//							byteBuffer.limit(remainCount);
//						}
//
//						while(byteBuffer.hasRemaining()){
//							sc.write(byteBuffer);
//						}
//						byteBuffer.clear();
//					}
//
//					break;
//				}else{
//					LogUtil.debug("read  文件数据  个数： " + size);
//					byteBuffer.put(buffer, 0, size);
//					byteBuffer.rewind();
//					byteBuffer.limit(size);
//				}

                byteBuffer.flip();
                while(byteBuffer.hasRemaining()){
                    sc.write(byteBuffer);
                }
                byteBuffer.clear();


                //countSpace ++;

            }

            inChannel.close();
        } catch(Exception ex){
            if(fIs != null && fIs.getChannel() != null){
                fIs.getChannel().close();
            }
            LogUtil.error(ex.getMessage());
        }

        LogUtil.debug("send 数据体结束");
    }

    /*** 发送reponseVersion数据 **/
    public byte[]  getFilesResp(String path ) {
        byte[] rntValue = null;
        try {
            // 请求手机短信、联系人、通话记录的个数
            JSONObject obj = new JSONObject();
            JSONArray dataObj = new JSONArray();
            obj.put("return", 0);
            obj.put("reason", "");
            if(path != null && !"".equals(path)){

                File file = new File(path);
                if(file.exists()){
                    if(file.isDirectory()){
                        File[] files = file.listFiles();
                        if(files != null && files.length > 0){
                            for(int i = 0;i<files.length;i++){
                                File tmpFile = files[i];
                                JSONObject tmpObj = new JSONObject();
                                tmpObj.put("canRead", tmpFile.canRead()?0:-1);
                                tmpObj.put("canWrite", tmpFile.canWrite()?0:-1);
                                tmpObj.put("type", tmpFile.isFile()?0:1);
                                tmpObj.put("updateTime", tmpFile.lastModified()/1000);
                                tmpObj.put("size", tmpFile.length());
                                tmpObj.put("name", tmpFile.getName());
                                tmpObj.put("path", tmpFile.getAbsolutePath());
                                tmpObj.put("isHidden", tmpFile.isHidden()?0:-1);

                                dataObj.put(tmpObj);
                            }
                        }
                    }else if(file.isFile()){
                        JSONObject tmpObj = new JSONObject();
                        tmpObj.put("canRead", file.canRead()?0:-1);
                        tmpObj.put("canWrite", file.canWrite()?0:-1);
                        tmpObj.put("type", file.isFile()?0:1);
                        tmpObj.put("updateTime", file.lastModified()/1000 );
                        tmpObj.put("size", file.length());
                        tmpObj.put("name", file.getName());
                        tmpObj.put("path", file.getAbsolutePath());
                        tmpObj.put("isHidden", file.isHidden()?0:-1);
                        dataObj.put(tmpObj);
                    }

                }
            }
            obj.put("data", dataObj);


            int length = obj.toString().getBytes().length;
            ByteBuffer byteBuff = null;
            byteBuff = ByteBuffer.allocate(HEADER_LENGTH + length);
            // 返回头部位坐标0:响应命令
            byteBuff.put((byte) I4ANDROID_E_CMD_DATANUM_RESP);
            // 返回头部位坐标1-4:内容长度
            byteBuff.put(ConvertUtil.intToByteArray(length));
            // 返回头部位坐标5:内容类型
            byteBuff.put((byte) TYPE_JSON);
            // 返回头部位坐标6:拓展字节
            byteBuff.put((byte) 0);
            // 返回头部位坐标7:拓展字节
            byteBuff.put((byte) 0);
            // 返回数据部
            byteBuff.put(obj.toString().getBytes());
            LogUtil.debug("输出字符" + ":" + obj.toString());

            rntValue = byteBuff.array();
        } catch (Exception ex) {
            //reponseErrData(socket,I4ANDROID_E_CMD_DATANUM_RESP,"发送sorket内容出错" + ex.getMessage());
            LogUtil.debug("发送sorket内容出错" + ex.getMessage());
        }

        return rntValue;
    }

    static JSONObject tmpObj = new JSONObject();
    private void getFileInfo(File file,int fileType,ArrayList<String> extraDirPaths) throws JSONException{

        switch(fileType){
            case 1:
                if(CommonUtils.isImage(file.getName())){
                    JSONObject tmpObj = new JSONObject();
                    tmpObj.put("canRead", file.canRead()?0:-1);
                    tmpObj.put("canWrite", file.canWrite()?0:-1);
                    tmpObj.put("type", file.isFile()?0:1);
                    tmpObj.put("updateTime", file.lastModified()/1000 );
                    tmpObj.put("size", file.length());
                    tmpObj.put("name", file.getName());
                    tmpObj.put("path", file.getAbsolutePath());
                    tmpObj.put("isHidden", file.isHidden()?0:-1);
                    expDataObj.put(tmpObj);
                }
                break;
            default:
                JSONObject tmpObj = new JSONObject();
                tmpObj.put("canRead", file.canRead()?0:-1);
                tmpObj.put("canWrite", file.canWrite()?0:-1);
                tmpObj.put("type", file.isFile()?0:1);
                tmpObj.put("updateTime", file.lastModified()/1000 );
                tmpObj.put("size", file.length());
                tmpObj.put("name", file.getName());
                tmpObj.put("path", file.getAbsolutePath());
                tmpObj.put("isHidden", file.isHidden()?0:-1);
                expDataObj.put(tmpObj);
                break;
        }

        if (file.isFile()) {
            return;
        }
        if (file.isDirectory()) {
            if(extraDirPaths != null){
                if(extraDirPaths.contains(file.getPath())){
                    LogUtil.debug(file.getPath() + "已经被过滤掉");
                    return;
                }
            }

            File[] childFile = file.listFiles();
            if (childFile == null || childFile.length == 0) {
                return;
            }
            for (File f : childFile) {
                getFileInfo(f,fileType,extraDirPaths);
            }
        }
    }
    /*** 发送reponseVersion数据 **/
    public byte[]  getExportFilesResp(String path ,int fileType,ArrayList<String> extraDirPaths) {
        byte[] rntValue = null;

        try {
            // 请求手机短信、联系人、通话记录的个数
            JSONObject obj = new JSONObject();
            obj.put("return", 0);
            obj.put("reason", "");
            LogUtil.debug("获取文件数据开始");
            if(path != null && !"".equals(path)){
                File file = new File(path);
                if(file.exists()){
                    getFileInfo(file,fileType,extraDirPaths);
                }
            }
            obj.put("data", expDataObj);
            LogUtil.debug("获取文件数据结束");
            byte[] bObj = obj.toString().getBytes();
            int length = bObj.length;
            ByteBuffer byteBuff = null;
            byteBuff = ByteBuffer.allocate(HEADER_LENGTH + length);
            // 返回头部位坐标0:响应命令
            byteBuff.put((byte) I4ANDROID_E_CMD_DATANUM_RESP);
            // 返回头部位坐标1-4:内容长度
            byteBuff.put(ConvertUtil.intToByteArray(length));
            // 返回头部位坐标5:内容类型
            byteBuff.put((byte) TYPE_JSON);
            // 返回头部位坐标6:拓展字节
            byteBuff.put((byte) 0);
            // 返回头部位坐标7:拓展字节
            byteBuff.put((byte) 0);
            // 返回数据部
            byteBuff.put(bObj);

            rntValue = byteBuff.array();
        } catch (Exception ex) {
            LogUtil.debug("发送sorket内容出错" + ex.getMessage());
        }

        return rntValue;
    }


/*	private JSONObject convertFileToJson(File file) throws JSONException{
		JSONObject allJsonObj = new JSONObject();
		allJsonObj.put("canRead", file.canRead()?0:-1);
		allJsonObj.put("canWrite", file.canWrite()?0:-1);
		allJsonObj.put("type", file.isFile()?0:1);
		allJsonObj.put("updateTime", CommonUtils.convertTime(file.lastModified()));
		allJsonObj.put("size", file.length());
		allJsonObj.put("name", file.getName());
		allJsonObj.put("path", file.getAbsolutePath());
		allJsonObj.put("isHidden", file.isHidden()?0:-1);
		return allJsonObj;
	}*/

    /**
     * 复制单个文件
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     * @return boolean
     */
    public boolean copyFile(String oldPath, String newPath) {
        boolean rntValue = true;
        try {
            File oldfile = new File(oldPath);
            if (oldfile.exists()) { //文件存在时
                copyFileCount ++;
                InputStream inStream = new FileInputStream(oldPath); //读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                int byteread;
                while ( ((byteread = inStream.read(buffer)) != -1) && !isStopThread) {
                    copyByteCount += byteread; //字节数 文件大小
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
                fs.close();
            }
        }
        catch (Exception e) {
            rntValue = false;
            LogUtil.error("复制单个文件操作出错 : " + e.getMessage());
        }

        return rntValue;

    }

    /**
     * 复制整个文件夹内容
     * @param oldPath String 原文件路径 如：c:/fqf
     * @param newPath String 复制后路径 如：f:/fqf/ff
     * @return boolean
     */
    public boolean copyFolder(String oldPath, String newPath) {
        boolean rntValue = true;
        try {
            (new File(newPath)).mkdirs(); //如果文件夹不存在 则建立新文件夹
            File a=new File(oldPath);
            String[] file=a.list();
            File temp=null;
            for (int i = 0; i < file.length; i++) {
                if(isStopThread){
                    return true;
                }

                if(oldPath.endsWith(File.separator)){
                    temp=new File(oldPath+file[i]);
                }else{
                    temp=new File(oldPath+File.separator+file[i]);
                }

                if(temp.isFile()){
                    copyFileCount ++;
                    FileInputStream input = new FileInputStream(temp);
                    FileOutputStream output = new FileOutputStream(newPath + "/" +
                            (temp.getName()).toString());
                    int len;
                    while ( ((len = input.read(buffer)) != -1) && !isStopThread) {
                        copyByteCount += len;
                        output.write(buffer, 0, len);
                    }
                    output.flush();
                    output.close();
                    input.close();
                }
                if(temp.isDirectory()){//如果是子文件夹
                    copyFolder(oldPath+"/"+file[i],newPath+"/"+file[i]);
                }
            }

        }catch (Exception e) {
            rntValue = false;
            LogUtil.error("复制整个文件夹内容操作出错: " + e.getMessage());
        }

        return rntValue;

    }

    /*** 发送复制个数数据 **/
    public byte[]  getCopyFilesResp() {
        byte[] rntValue = null;

        try {
            // 请求手机短信、联系人、通话记录的个数
            JSONObject obj = new JSONObject();
            obj.put("return", 0);
            obj.put("reason", "");

            JSONArray jsonArr = new JSONArray();
            JSONObject tmpObj = new JSONObject();
            tmpObj.put("copyFileCount", copyFileCount);
            tmpObj.put("copyByteCount", copyByteCount);
            jsonArr.put(tmpObj);
            obj.put("data", jsonArr);

            int length = obj.toString().getBytes().length;
            ByteBuffer byteBuff = null;
            byteBuff = ByteBuffer.allocate(HEADER_LENGTH + length);
            // 返回头部位坐标0:响应命令
            byteBuff.put((byte) I4ANDROID_E_CMD_FILE_GET_COPY_SIZE_RESP);
            // 返回头部位坐标1-4:内容长度
            byteBuff.put(ConvertUtil.intToByteArray(length));
            // 返回头部位坐标5:内容类型
            byteBuff.put((byte) TYPE_JSON);
            // 返回头部位坐标6:拓展字节
            byteBuff.put((byte) 0);
            // 返回头部位坐标7:拓展字节
            byteBuff.put((byte) 0);
            // 返回数据部
            byteBuff.put(obj.toString().getBytes());

            rntValue = byteBuff.array();
        } catch (Exception ex) {
            LogUtil.debug("发送sorket内容出错" + ex.getMessage());
        }

        return rntValue;
    }

    /**
     * @param file  video:0 audio:1 images :2
     * @throws IOException
     **/
    //-1：失败
    public int addInMediaStore(File file) throws Exception {
        if (!file.exists())
            return -1;
        int flag = CommonUtils.getFileType(file.getName());
        String DATA = Media.DATA;
        String DATE_MODIFIED = Media.DATE_MODIFIED;
        String SIZE = Media.SIZE;
        String DISPLAY_NAME = Media.DISPLAY_NAME;
        Uri EXTERNAL_CONTENT_URI = null;
        ContentValues values = new ContentValues();
        if (flag == -1) {
            return -1;
        } else if (flag == ConvertUtil.IMAGES) {
            EXTERNAL_CONTENT_URI = Media.EXTERNAL_CONTENT_URI;
        } else {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(file.getAbsolutePath());

            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);// 时长
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);// 艺术家
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);// 标题
            if(title == null || "".equals(title.trim())){
                title = FileUtil.getNameWithoutExtra(file.getName());
            }
            String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);// 专辑
            if(album == null || "".equals(album.trim())){
                album = "music";
            }

            switch (flag) {
                case ConvertUtil.VIDEO:
                    EXTERNAL_CONTENT_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    values.put(MediaStore.Video.Media.ARTIST, artist);
                    values.put(MediaStore.Video.Media.ALBUM, album);
                    values.put(MediaStore.Video.Media.TITLE, title);
                    values.put(MediaStore.Video.Media.DURATION, duration);


                    break;
                case ConvertUtil.AUDIO:
                    EXTERNAL_CONTENT_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    values.put(MediaStore.Audio.Media.ARTIST, artist);
                    values.put(MediaStore.Audio.Media.ALBUM, album);
                    values.put(MediaStore.Audio.Media.TITLE, title);
                    values.put(MediaStore.Audio.Media.DURATION, duration);
                    values.put(MediaStore.Audio.Media.IS_MUSIC, true);
                    values.put(MediaStore.Audio.Media.IS_RINGTONE, false);
                    values.put(MediaStore.Audio.Media.IS_ALARM, false);
                    values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);

                    break;
            }
        }

        values.put(DATA, file.getAbsolutePath());
        values.put(DATE_MODIFIED, file.lastModified());
        values.put(SIZE, file.length());
        values.put(DISPLAY_NAME, file.getName());

        context.getContentResolver().insert(EXTERNAL_CONTENT_URI, values);

        return 0;
    }

    //删除媒体资源时删除系统数据
    public int deleteFromMediaStore(File file){
        int flag = CommonUtils.getFileType(file.getName());
        Uri EXTERNAL_CONTENT_URI = null;
        switch(flag){
            case ConvertUtil.VIDEO:
                //删除图片资源
                EXTERNAL_CONTENT_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                break;
            case ConvertUtil.AUDIO:
                EXTERNAL_CONTENT_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                deleteVoice(file.getAbsolutePath());
                break;
            case ConvertUtil.IMAGES:
                EXTERNAL_CONTENT_URI = Media.EXTERNAL_CONTENT_URI;
                break;
            default:
                return -1;
        }
        String where = Media.DATA + " = ?";
        String[] selectionArgs = new String[]{file.getAbsolutePath()};

        return context.getContentResolver().delete(EXTERNAL_CONTENT_URI, where, selectionArgs);
    }

    public void addFile(File file){
        try {
            addInMediaStore(file);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 递归删除文件和文件夹
     *
     * @param file
     *            要删除的根目录
     */
    public void deleteFile(File file) {
        if (file.exists() == false) {
            return;
        } else {
            if (file.isFile()) {
                if(file.delete()){
                    deleteFromMediaStore(file);
                }

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

    public int deleteVoice(String path){
        Uri uri = MediaStore.Audio.Media.getContentUriForPath(path);
        if(uri != null){
            return context.getContentResolver().delete(uri, MediaStore.MediaColumns.DATA + "=?", new String[] { path });
        }else{
            return 0;
        }

    }

    public void setVoice(String path, int id) {

        ContentValues cv = new ContentValues();
        Uri newUri = null;
        Uri uri = MediaStore.Audio.Media.getContentUriForPath(path);

        // 查询音乐文件在媒体库是否存在
        Cursor cursor = context.getContentResolver().query(uri, null,
                MediaStore.MediaColumns.DATA + "=?", new String[] { path },
                null);

        if (cursor.moveToFirst() && cursor.getCount() > 0) {
            String _id = cursor.getString(0);
            switch (id) {
                case RINGTONE:
                    cv.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                    cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
                    cv.put(MediaStore.Audio.Media.IS_ALARM, false);
                    //cv.put(MediaStore.Audio.Media.IS_MUSIC, false);

                    break;
                case NOTIFICATION:
                    cv.put(MediaStore.Audio.Media.IS_RINGTONE, false);
                    cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
                    cv.put(MediaStore.Audio.Media.IS_ALARM, false);
                    //cv.put(MediaStore.Audio.Media.IS_MUSIC, false);

                    break;
                case ALARM:
                    cv.put(MediaStore.Audio.Media.IS_RINGTONE, false);
                    cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
                    cv.put(MediaStore.Audio.Media.IS_ALARM, true);
                    //cv.put(MediaStore.Audio.Media.IS_MUSIC, false);

                    break;
                case ALL:
                    cv.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                    cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
                    cv.put(MediaStore.Audio.Media.IS_ALARM, true);
                    //cv.put(MediaStore.Audio.Media.IS_MUSIC, false);

                    break;
                default:
                    break;
            }

            // 把需要设为铃声的歌曲更新铃声库

            context.getContentResolver().update(uri, cv,
                    MediaStore.MediaColumns.DATA + "=?", new String[] { path });

            newUri = ContentUris.withAppendedId(uri, Long.valueOf(_id));

            // 一下为关键代码：

            switch (id) {
                case RINGTONE:
                    RingtoneManager.setActualDefaultRingtoneUri(context,
                            RingtoneManager.TYPE_RINGTONE, newUri);

                    break;
                case NOTIFICATION:
                    RingtoneManager.setActualDefaultRingtoneUri(context,
                            RingtoneManager.TYPE_NOTIFICATION, newUri);

                    break;
                case ALARM:
                    RingtoneManager.setActualDefaultRingtoneUri(context,
                            RingtoneManager.TYPE_ALARM, newUri);

                    break;
                case ALL:
                    RingtoneManager.setActualDefaultRingtoneUri(context,
                            RingtoneManager.TYPE_ALL, newUri);

                    break;
                default:
                    break;

            }

            // 播放铃声
            // Ringtone rt = RingtoneManager.getRingtone(this, newUri);
            // rt.play();

        }
    }

    public String[] getRingTone(){
        String[] path = new String[3];
        Uri uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE);
        long _id = 0;
        Cursor cursor = null;
        try{
            if(uri != null){
                try{
                    _id = ContentUris.parseId(uri);
                    cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DATA}, MediaStore.MediaColumns._ID + "=?", new String[] { _id + "" },null);
                    if (cursor.moveToFirst() && cursor.getCount() > 0){
                        path[0] = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
                    }
                }catch(NumberFormatException ex){}

            }

            uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
            if(uri != null){
                try{
                    _id = ContentUris.parseId(uri);
                    cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DATA}, MediaStore.MediaColumns._ID + "=?", new String[] { _id + "" },null);
                    if (cursor.moveToFirst() && cursor.getCount() > 0){
                        path[1] = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
                    }
                }catch(NumberFormatException ex){}

            }

            uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM);
            if(uri != null){
                try{
                    _id = ContentUris.parseId(uri);
                    cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DATA}, MediaStore.MediaColumns._ID + "=?", new String[] { _id + "" },null);
                    if (cursor.moveToFirst() && cursor.getCount() > 0){
                        path[2] = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
                    }
                }catch(NumberFormatException ex){}
            }
        }finally{
            if(cursor != null){
                cursor.close();
            }
        }

        return path;
    }


    /**
     * @param filePath**/
    public boolean installFile(String filePath){
        boolean rntValue = false;
        File file = new File(filePath);
        if(file != null && file.exists()){
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Intent.ACTION_VIEW);
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
    public  void uninstallAPK(Context context, String packageName) {
        Uri uri = Uri.parse("package:" + packageName);
        Intent intent = new Intent(Intent.ACTION_DELETE, uri);
        context.startActivity(intent);
    }
}
