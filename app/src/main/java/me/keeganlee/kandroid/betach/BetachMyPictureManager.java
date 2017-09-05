package me.keeganlee.kandroid.betach;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import me.keeganlee.kandroid.tools.CommonUtils;
import me.keeganlee.kandroid.tools.ConvertUtil;
import me.keeganlee.kandroid.tools.LogUtil;
import me.keeganlee.kandroid.tools.MD5AndSignature;

public class BetachMyPictureManager extends BaseBetach{
    private Context context;
    private int space = 1024;
    public static long copyFileCount = 0;
    public static long copyByteCount = 0;
    public static boolean isStopThread = false;

    private ByteBuffer byteBuffer = ByteBuffer.allocate(space);
    //private ByteBuffer headerByteBuff = ByteBuffer.allocateDirect(BaseBetach.HEADER_LENGTH);
    public BetachMyPictureManager(Context context){
        this.context = context;
    }

    public ByteBuffer getCaptureImage(String path,int fileType){
        File file = new File(path);
        if(!file.exists())return null;
        Bitmap bit = null;
        //1 : 图片   2：视频
        switch(fileType){
            case 2:
                bit = CommonUtils.getVideoThumbnail(path, 130, 130,MediaStore.Images.Thumbnails.MICRO_KIND);
                break;
            default:
                bit = CommonUtils.getImageThumbnail(path, 130, 130);
        }
        byte[] tmpByte = ConvertUtil.bitmapToBytes(bit);

        int cContent = (tmpByte != null)?tmpByte.length:0;//内容长度
        if(HEADER_LENGTH + cContent > space){
            //如果内容长度大于预存控件，重新申请空间
            byteBuffer = ByteBuffer.allocate(HEADER_LENGTH + cContent);
        }
        //添加头部包头信息
        byteBuffer.clear();
        //////////////////添加头部信息/////////////////////////
        // 返回头部位坐标0:响应命令
        byteBuffer.put((byte) I4ANDROID_E_CMD_MEDIA_RESP);
        // 返回头部位坐标1-4:内容长度
        byteBuffer.put(ConvertUtil.intToByteArray(cContent));
        // 返回头部位坐标5:内容类型
        byteBuffer.put((byte) TYPE_BYTE);
        // 返回头部位坐标6:拓展字节
        byteBuffer.put((byte) 0);
        // 返回头部位坐标7:拓展字节
        byteBuffer.put((byte) 0);

        //////////////////添加数据信息/////////////////////////
        if(tmpByte != null){
            byteBuffer.put(tmpByte);//1位
        }

        byteBuffer.rewind();
        byteBuffer.limit(HEADER_LENGTH + cContent);

        return byteBuffer;
    }

    public ByteBuffer getCaptureImageMd5(int response , String path,int fileType){
        File file = new File(path);
        if(!file.exists())return null;
        Bitmap bit = null;
        //1 : 图片   2：视频
        switch(fileType){
            case 2:
                bit = CommonUtils.getVideoThumbnail(path, 130, 130,MediaStore.Images.Thumbnails.MICRO_KIND);
                break;
            default:
                bit = CommonUtils.getImageThumbnail(path, 130, 130);
        }
        byte[] tmpByte = ConvertUtil.bitmapToBytes(bit);
        MD5AndSignature md5 = new MD5AndSignature();
        return getReponseData(response,new String[]{"path","md5"},new String[]{path,md5.getMessageDigest(tmpByte, MD5AndSignature.MD5)});
    }

    /*** 发送reponseVersion数据 **/
    public void  getExportFilesResp(SocketChannel sc,String path ,int fileType,ArrayList<String> extraDirPaths,int isAll) {
        try {
            LogUtil.debug("获取文件数据开始");

            String DATA = Media.DATA;//文件路径
            String DATE_MODIFIED = Media.DATE_MODIFIED;//修改时间
            String SIZE = Media.SIZE;//大小
            String DISPLAY_NAME = Media.DISPLAY_NAME;//名字
            String _ID = Media._ID;
            String TITLE =  Media.TITLE;
            String DURATION = null;//时长
            String ARTIST = null;//歌手
            String ALBUM = null;//专辑
            Uri EXTERNAL_CONTENT_URI = null;
            String[] columns = null;//
            switch(fileType){
                case ConvertUtil.VIDEO:
                    EXTERNAL_CONTENT_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    DURATION = MediaStore.Video.Media.DURATION;
                    ARTIST = MediaStore.Video.Media.ARTIST;
                    ALBUM = MediaStore.Video.Media.ALBUM;

                    columns = new String[]{ DATA, _ID, TITLE,DISPLAY_NAME ,DATE_MODIFIED,Media.SIZE,DURATION,ARTIST,ALBUM};
                    break;
                case ConvertUtil.AUDIO:
                    EXTERNAL_CONTENT_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    DURATION = MediaStore.Audio.Media.DURATION;
                    ARTIST = MediaStore.Audio.Media.ARTIST;
                    ALBUM = MediaStore.Audio.Media.ALBUM;
                    columns = new String[]{ DATA, _ID, TITLE,DISPLAY_NAME ,DATE_MODIFIED,Media.SIZE,DURATION,ARTIST,ALBUM};
                    break;
                case ConvertUtil.IMAGES:
                    EXTERNAL_CONTENT_URI = Media.EXTERNAL_CONTENT_URI;
                    columns = new String[]{ DATA, _ID, TITLE,DISPLAY_NAME ,DATE_MODIFIED,Media.SIZE};
                    break;
            }
            //设置要返回的字段


            String selection = "";
            String[] selectionArgs = null;

            if(isAll == 0){
                if(extraDirPaths != null && extraDirPaths.size() > 0){
                    selectionArgs = new String[extraDirPaths.size() + 1];
                    for(int i = 0 ; i < extraDirPaths.size();i++){
                        selection += Media.DATA + " not like ? ";
                        selection = selection + " and ";
                        selectionArgs[i] = extraDirPaths.get(i) + "%";
                    }

                    selection +=  Media.DATA + " like ?";
                    selectionArgs[extraDirPaths.size()] = path + "%";
                }else{
                    selection =  Media.DATA + " like ?";
                    selectionArgs = new String[1];
                    selectionArgs[0] = path + "%";
                }
            }

            //执行查询，返回一个cursor
            Cursor cursor = context.getContentResolver().query(EXTERNAL_CONTENT_URI, columns, selection, selectionArgs,null);
            int fileColumn = 0;
            int lastModifiedColumn = 0;
            int sizeColumn = 0;
            int displayColumn = 0;
            int durationColumn =  0;
            int artistColumn =   0;
            int albumColumn =   0;
            int titleColumn =  0;
            List list = new ArrayList();
            if(cursor != null){
                if(fileType == ConvertUtil.IMAGES){
                    fileColumn = cursor.getColumnIndexOrThrow(DATA);
                    lastModifiedColumn = cursor.getColumnIndexOrThrow(DATE_MODIFIED);
                    sizeColumn = cursor.getColumnIndexOrThrow(SIZE);
                    displayColumn = cursor.getColumnIndexOrThrow(DISPLAY_NAME);
                    int count = 0;
                    while (cursor.moveToNext()) {
						/*sendFileDetail(sc,true,true,true,cursor.getLong(lastModifiedColumn),
								cursor.getLong(sizeColumn),ConvertUtil.noEmptyString(cursor.getString(displayColumn)),
								ConvertUtil.noEmptyString(cursor.getString(fileColumn)),true);*/
                        long size = cursor.getLong(sizeColumn);
                        if(size <= 0){
                            continue;
                        }

                        list.clear();
                        list.add((byte)0);
                        list.add((byte)0);
                        list.add((byte)0);
                        list.add(cursor.getLong(lastModifiedColumn) + "");
                        list.add(size);
                        list.add(ConvertUtil.noEmptyString(cursor.getString(displayColumn)));
                        list.add(ConvertUtil.noEmptyString(cursor.getString(fileColumn)));
                        list.add((byte)0);
                        this.sendFileDetail(sc, I4ANDROID_E_CMD_MEDIA_RESP, list);
                        count ++;
                    }

                    System.out.println("图片库总数  === " + count);
                }else{
                    fileColumn = cursor.getColumnIndexOrThrow(DATA);
                    lastModifiedColumn = cursor.getColumnIndexOrThrow(DATE_MODIFIED);
                    sizeColumn = cursor.getColumnIndexOrThrow(SIZE);
                    displayColumn = cursor.getColumnIndexOrThrow(DISPLAY_NAME);
                    durationColumn =  cursor.getColumnIndexOrThrow(DURATION);
                    artistColumn =   cursor.getColumnIndexOrThrow(ARTIST);
                    albumColumn =   cursor.getColumnIndexOrThrow(ALBUM);
                    titleColumn =   cursor.getColumnIndexOrThrow(TITLE);

                    while (cursor.moveToNext()) {
						/*sendFileDetail(sc,true,true,true,cursor.getLong(lastModifiedColumn),
								cursor.getLong(sizeColumn),ConvertUtil.noEmptyString(cursor.getString(displayColumn)),
								ConvertUtil.noEmptyString(cursor.getString(fileColumn)),true,ConvertUtil.noEmptyString(cursor.getString(durationColumn)),
								ConvertUtil.noEmptyString(cursor.getString(artistColumn)),ConvertUtil.noEmptyString(cursor.getString(albumColumn)),ConvertUtil.noEmptyString(cursor.getString(titleColumn)));*/
                        long size = cursor.getLong(sizeColumn);
                        if(size <= 0){
                            continue;
                        }

                        list.clear();
                        list.add((byte)0);
                        list.add((byte)0);
                        list.add((byte)0);
                        list.add(cursor.getLong(lastModifiedColumn) + "");
                        list.add(size);
                        list.add(ConvertUtil.noEmptyString(cursor.getString(displayColumn)));
                        list.add(ConvertUtil.noEmptyString(cursor.getString(fileColumn)));
                        list.add((byte)0);
                        list.add(ConvertUtil.noEmptyString(cursor.getString(durationColumn)));
                        list.add(ConvertUtil.noEmptyString(cursor.getString(artistColumn)));
                        list.add(ConvertUtil.noEmptyString(cursor.getString(albumColumn)));
                        list.add(ConvertUtil.noEmptyString(cursor.getString(titleColumn)));
                        this.sendFileDetail(sc, I4ANDROID_E_CMD_MEDIA_RESP, list);

                    }
                }

                cursor.close();
            }

            //headerByteBuff.clear();
			/*// 返回头部位坐标0:响应命令
			headerByteBuff.put((byte) I4ANDROID_E_CMD_MEDIA_RESP);

			// 返回头部位坐标1-4:内容长度
			headerByteBuff.put(ConvertUtil.intToByteArray( 0));
			// 返回头部位坐标5:内容类型
			headerByteBuff.put((byte) TYPE_BYTE);
			// 返回头部位坐标6:拓展字节
			headerByteBuff.put((byte) 0);
			// 返回头部位坐标7:拓展字节
			headerByteBuff.put((byte) 1);

			headerByteBuff.rewind();
			headerByteBuff.limit(8);*/
            //while(headerByteBuff.hasRemaining()){
            //	sc.write(headerByteBuff);
            //}

            LogUtil.debug("获取文件数据结束");

        } catch (Exception ex) {
            LogUtil.debug("发送sorket内容出错" + ((ex != null)?ex.getMessage():""));
        }
    }
	/*
	public void sendFileDetail(SocketChannel sc,File file,ArrayList<String> fileTypes) throws IOException{
		if(fileTypes != null && !fileTypes.contains(CommonUtils.getExtraName(file.getName())))return;

		byte bCanRead = (byte)(file.canRead()?0:-1);//1
		byte bCanWrite = (byte)(file.canWrite()?0:-1);//1
		byte bType = (byte)(file.isFile()?0:1);//1
		byte[] bUpdateTime = (file.lastModified()+"").getBytes("utf-8");//4
		byte[] bSize = ConvertUtil.longToByteArray(file.length());//8
		byte[] bName = file.getName().getBytes("utf-8");//4
		byte[] bPath = file.getAbsolutePath().getBytes("utf-8");///4
		byte bIsHidden = (byte)(file.isHidden()?0:-1);//1

		int cContent = 1+1+1+(4+bUpdateTime.length)+bSize.length+(4+bName.length)+(4+bPath.length)+1;//内容长度
		if(HEADER_LENGTH + cContent > space){
			//如果内容长度大于预存控件，重新申请空间
			byteBuffer = ByteBuffer.allocate(HEADER_LENGTH + cContent);
		}
		//添加头部包头信息
		byteBuffer.clear();
		//////////////////添加头部信息/////////////////////////
		// 返回头部位坐标0:响应命令
		byteBuffer.put((byte) I4ANDROID_E_CMD_MEDIA_RESP);
		// 返回头部位坐标1-4:内容长度
		byteBuffer.put(ConvertUtil.intToByteArray(cContent));
		// 返回头部位坐标5:内容类型
		byteBuffer.put((byte) TYPE_BYTE);
		// 返回头部位坐标6:拓展字节
		byteBuffer.put((byte) 0);
		// 返回头部位坐标7:拓展字节
		byteBuffer.put((byte) 0);

		//////////////////添加数据信息/////////////////////////
		byteBuffer.put(bCanRead);//1位
		byteBuffer.put(bCanWrite);//1
		byteBuffer.put(bType);//1
		byteBuffer.put(ConvertUtil.intToByteArray(bUpdateTime.length));//4
		byteBuffer.put(bUpdateTime);
		byteBuffer.put(bSize);//8
		byteBuffer.put(ConvertUtil.intToByteArray(bName.length));//4
		byteBuffer.put(bName);
		byteBuffer.put(ConvertUtil.intToByteArray(bPath.length));//4
		byteBuffer.put(bPath);
		byteBuffer.put(bIsHidden);//1

		byteBuffer.rewind();
		byteBuffer.limit(HEADER_LENGTH + cContent);


		while(byteBuffer.hasRemaining()){
			sc.write(byteBuffer);
		}
		//byteBuffer.clear();

		//释放内存
		bUpdateTime = null;
		bSize = null;
		bName = null;
		bPath = null;
	}

	public void sendFileDetail(SocketChannel sc,boolean canRead,boolean canWrite,boolean isFile,long lastModified,long size,String name,String absolutePath,boolean isHidden) throws IOException{

		byte bCanRead = (byte)(canRead?0:-1);//1
		byte bCanWrite = (byte)(canWrite?0:-1);//1
		byte bType = (byte)(isFile?0:1);//1
		byte[] bUpdateTime = (lastModified+"").getBytes("utf-8");//4
		byte[] bSize = ConvertUtil.longToByteArray(size);//8
		byte[] bName = name.getBytes("utf-8");//4
		byte[] bPath = absolutePath.getBytes("utf-8");///4
		byte bIsHidden = (byte)(isHidden?0:-1);//1

		int cContent = 1+1+1+(4+bUpdateTime.length)+bSize.length+(4+bName.length)+(4+bPath.length)+1;//内容长度
		if(HEADER_LENGTH + cContent > space){
			//如果内容长度大于预存控件，重新申请空间
			byteBuffer = ByteBuffer.allocate(HEADER_LENGTH + cContent);
		}
		//添加头部包头信息
		byteBuffer.clear();
		//////////////////添加头部信息/////////////////////////
		// 返回头部位坐标0:响应命令
		byteBuffer.put((byte) I4ANDROID_E_CMD_MEDIA_RESP);
		// 返回头部位坐标1-4:内容长度
		byteBuffer.put(ConvertUtil.intToByteArray(cContent));
		// 返回头部位坐标5:内容类型
		byteBuffer.put((byte) TYPE_BYTE);
		// 返回头部位坐标6:拓展字节
		byteBuffer.put((byte) 0);
		// 返回头部位坐标7:拓展字节
		byteBuffer.put((byte) 0);

		//////////////////添加数据信息/////////////////////////
		byteBuffer.put(bCanRead);//1位
		byteBuffer.put(bCanWrite);//1
		byteBuffer.put(bType);//1
		byteBuffer.put(ConvertUtil.intToByteArray(bUpdateTime.length));//4
		byteBuffer.put(bUpdateTime);
		byteBuffer.put(bSize);//8
		byteBuffer.put(ConvertUtil.intToByteArray(bName.length));//4
		byteBuffer.put(bName);
		byteBuffer.put(ConvertUtil.intToByteArray(bPath.length));//4
		byteBuffer.put(bPath);
		byteBuffer.put(bIsHidden);//1

		byteBuffer.rewind();
		byteBuffer.limit(HEADER_LENGTH + cContent);


		while(byteBuffer.hasRemaining()){
			sc.write(byteBuffer);
		}

		//释放内存
		bUpdateTime = null;
		bSize = null;
		bName = null;
		bPath = null;
	}

	public void sendFileDetail(SocketChannel sc,boolean canRead,boolean canWrite,
			boolean isFile,long lastModified,long size,
			String name,String absolutePath,boolean isHidden,
			String duration,String artist ,String album,String title) throws IOException{

		byte bCanRead = (byte)(canRead?0:-1);//1
		byte bCanWrite = (byte)(canWrite?0:-1);//1
		byte bType = (byte)(isFile?0:1);//1
		byte[] bUpdateTime = (lastModified+"").getBytes("utf-8");//4
		byte[] bSize = ConvertUtil.longToByteArray(size);//8
		byte[] bName = name.getBytes("utf-8");//4
		byte[] bPath = absolutePath.getBytes("utf-8");///4
		byte bIsHidden = (byte)(isHidden?0:-1);//1
		byte[] bDuration = duration.getBytes("utf-8");
		byte[] bArtist = artist.getBytes("utf-8");
		byte[] bAlbum = album.getBytes("utf-8");
		byte[] bTitle = title.getBytes("utf-8");

		int cContent = 1+1+1+(4+bUpdateTime.length)+bSize.length+(4+bName.length)+(4+bPath.length)+1 + (4+bDuration.length) + (4+bArtist.length) + (4+bAlbum.length) + (4+bTitle.length);//内容长度
		if(HEADER_LENGTH + cContent > space){
			//如果内容长度大于预存控件，重新申请空间
			byteBuffer = ByteBuffer.allocate(HEADER_LENGTH + cContent);
		}
		//添加头部包头信息
		byteBuffer.clear();
		//////////////////添加头部信息/////////////////////////
		// 返回头部位坐标0:响应命令
		byteBuffer.put((byte) I4ANDROID_E_CMD_MEDIA_RESP);
		// 返回头部位坐标1-4:内容长度
		byteBuffer.put(ConvertUtil.intToByteArray(cContent));
		// 返回头部位坐标5:内容类型
		byteBuffer.put((byte) TYPE_BYTE);
		// 返回头部位坐标6:拓展字节
		byteBuffer.put((byte) 0);
		// 返回头部位坐标7:拓展字节
		byteBuffer.put((byte) 0);

		//////////////////添加数据信息/////////////////////////
		byteBuffer.put(bCanRead);//1位
		byteBuffer.put(bCanWrite);//1
		byteBuffer.put(bType);//1
		byteBuffer.put(ConvertUtil.intToByteArray(bUpdateTime.length));//4
		byteBuffer.put(bUpdateTime);
		byteBuffer.put(bSize);//8
		byteBuffer.put(ConvertUtil.intToByteArray(bName.length));//4
		byteBuffer.put(bName);
		byteBuffer.put(ConvertUtil.intToByteArray(bPath.length));//4
		byteBuffer.put(bPath);
		byteBuffer.put(bIsHidden);//1
		byteBuffer.put(ConvertUtil.intToByteArray(bDuration.length));//4
		byteBuffer.put(bDuration);
		byteBuffer.put(ConvertUtil.intToByteArray(bArtist.length));//4
		byteBuffer.put(bArtist);
		byteBuffer.put(ConvertUtil.intToByteArray(bAlbum.length));//4
		byteBuffer.put(bAlbum);
		byteBuffer.put(ConvertUtil.intToByteArray(bTitle.length));//4
		byteBuffer.put(bTitle);
		byteBuffer.rewind();
		byteBuffer.limit(HEADER_LENGTH + cContent);

		while(byteBuffer.hasRemaining()){
			sc.write(byteBuffer);
		}

		//释放内存
		bUpdateTime = null;
		bSize = null;
		bName = null;
		bPath = null;
	}


	*/
}
