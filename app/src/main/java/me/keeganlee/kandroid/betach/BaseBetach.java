package me.keeganlee.kandroid.betach;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;

import me.keeganlee.kandroid.socket.BetachClient;
import me.keeganlee.kandroid.tools.ConvertUtil;
import me.keeganlee.kandroid.tools.LogUtil;
public class BaseBetach {
    /** 命令 **/
    public static final int I4ANDROID_E_CMD_DATANUM_REQ = 0x01; // 请求手机短信、联系人、通话记录的个数
    public static final int I4ANDROID_E_CMD_DATANUM_RESP = 0x02; // 手机短信、联系人、通话记录的个数的响应
    public static final int I4ANDROID_E_CMD_CONTACT_REQ = 0x03; // 请求获取联系人信息
    public static final int I4ANDROID_E_CMD_CONTACT_RESP = 0x04; // 请求获取联系人信息的响应
    public static final int I4ANDROID_E_CMD_VERSION_REQ = 0x05; // 请求获取版本号
    public static final int I4ANDROID_E_CMD_VERSION_RESP = 0x06; // 请求获取版本号的响应
    public static final int I4ANDROID_E_CMD_CONTACT_GROUP_REQ = 0x07;// 请求获取联系人组信息
    public static final int I4ANDROID_E_CMD_CONTACT_GROUP_RESP = 0x08;// 请求获取联系人组信息的响应
    public static final int I4ANDROID_E_CMD_CONTACT_BELL_REQ = 0x09;// 请求获取联系人铃声信息
    public static final int I4ANDROID_E_CMD_CONTACT_BELKL_RESP = 0x10;// 请求获取联系人铃声信息的响应
    public static final int I4ANDROID_E_CMD_CONTACT_ADD_REQ = 0x11;// 请求添加联系人信息
    public static final int I4ANDROID_E_CMD_CONTACT_ADD_RESP = 0x12; // 请求添加联系人信息的响应
    public static final int I4ANDROID_E_CMD_CONTACT_EXPORT_REQ = 0x13;// 请求导出联系人信息
    public static final int I4ANDROID_E_CMD_CONTACT_EXPORT_RESP = 0x14;// 请求导入联系人信息的响应
    //	public static final int I4ANDROID_E_CMD_CONTACT_UPDATE_REQ = 0x15;// 请求修改联系人信息
//	public static final int I4ANDROID_E_CMD_CONTACT_UPDATE_RESP = 0x16;// 请求修改联系人信息的响应
    public static final int I4ANDROID_E_CMD_CONTACT_DELETE_REQ = 0x17; // 请求删除联系人信息
    public static final int I4ANDROID_E_CMD_CONTACT_DELETE_RESP = 0x18;// 请求删除联系人信息的响应
    public static final int I4ANDROID_E_CMD_MESSAGE_REQ = 0x19;// 请求短信内容
    public static final int I4ANDROID_E_CMD_MESSAGE_RESP = 0x20;// 请求短信内容的响应
    public static final int I4ANDROID_E_CMD_MESSAGE_SEND_REQ = 0x21;// 请求短信内容
    public static final int I4ANDROID_E_CMD_MESSAGE_SEND_RESP = 0x22;// 请求短信内容的响应
    public static final int I4ANDROID_E_CMD_MESSAGE_IMPORT_REQ = 0x23;// 请求短信内容
    public static final int I4ANDROID_E_CMD_MESSAGE_EXPORT_RESP = 0x24;// 请求短信内容的响应
    public static final int I4ANDROID_E_CMD_MESSAGE_READ_REQ = 0x25;// 请求标识短信内容为已读
    public static final int I4ANDROID_E_CMD_MESSAGE_READ_RESP = 0x26; // 请求标识短信内容为已读的响应
    /*public static final int I4ANDROID_E_CMD_MUSIC_REQ = 0x27;// 请求获取音乐
    public static final int I4ANDROID_E_CMD_MUSIC_RESP = 0x28; // 请求获取音乐的响应
    public static final int I4ANDROID_E_CMD_MUSIC_ADD_REQ = 0x29;// 请求添加音乐
    public static final int I4ANDROID_E_CMD_MUSIC_ADD_RESP = 0x30;// 请求添加音乐的响应
    public static final int I4ANDROID_E_CMD_MUSIC_DELETE_REQ = 0x31;// 请求删除音乐
    public static final int I4ANDROID_E_CMD_MUSIC_DELETE_RESP = 0x32;// 请求删除音乐的响应
*/	public static final int I4ANDROID_E_CMD_MEDIA_REQ = 0x33;// 请求获取图片列表信息
    public static final int I4ANDROID_E_CMD_MEDIA_RESP = 0x34;// 请求获取图片列表信息的响应
    public static final int I4ANDROID_E_CMD_MEDIA_RESOURCE_REQ = 0x35;// 请求获取图片资源
    public static final int I4ANDROID_E_CMD_MEDIA_RESOURCE_RESP = 0x36;// 请求获取图片资源的响应
    /*public static final int I4ANDROID_E_CMD_MEDIA_ADD_REQ = 0x37;// 请求添加图片信息
    public static final int I4ANDROID_E_CMD_MEDIA_ADD_RESP = 0x38;// 请求获取图片信息的响应
    public static final int I4ANDROID_E_CMD_MEDIA_RESOURCE_ADD_REQ = 0x39;// 请求添加图片资源
    public static final int I4ANDROID_E_CMD_MEDIA_RESOURCE_ADD_RESP = 0x40; // 请求添加图片资源的响应
    public static final int I4ANDROID_E_CMD_MEDIA_DELETE_REQ = 0x41;// 请求删除图片资源
    public static final int I4ANDROID_E_CMD_MEDIA_DELETE_RESP = 0x42; // 请求删除图片资源的响应
*//*	public static final int I4ANDROID_E_CMD_VIDEO_REQ = 0x43;// 请求获取视频列表信息
	public static final int I4ANDROID_E_CMD_VIDEO_RESP = 0x44;// 请求获取视频列表信息的响应
	public static final int I4ANDROID_E_CMD_VIDEO_RESOURCE_REQ = 0x45;// 请求获取视频资源
	public static final int I4ANDROID_E_CMD_VIDEO_RESOURCE_RESP = 0x46;// 请求获取视频资源的响应
	public static final int I4ANDROID_E_CMD_VIDEO_ADD_REQ = 0x47;// 请求添加视频信息
	public static final int I4ANDROID_E_CMD_VIDEO_ADD_RESP = 0x48;// 请求获取视频信息的响应
	public static final int I4ANDROID_E_CMD_VIDEO_RESOURCE_ADD_REQ = 0x49;// 请求添加视频资源
	public static final int I4ANDROID_E_CMD_VIDEO_RESOURCE_ADD_RESP = 0x50; // 请求添加视频资源的响应
	public static final int I4ANDROID_E_CMD_VIDEO_DELETE_REQ = 0x51;// 请求删除视频资源
	public static final int I4ANDROID_E_CMD_VIDEO_DELETE_RESP = 0x52; // 请求删除视频资源的响应
*/	public static final int I4ANDROID_E_CMD_APK_REQ = 0x53;// 请求APK信息
    public static final int I4ANDROID_E_CMD_APK_RESP = 0x54; // 请求APK信息的响应
    public static final int I4ANDROID_E_CMD_ERROR = 0x00;// sorket 解析失败
    public static final int I4ANDROID_E_CMD_FILES_REQ = 0x55; // 请求文件信息
    public static final int I4ANDROID_E_CMD_FILES_RESP = 0x56; // 请求文件信息的响应
    public static final int I4ANDROID_E_CMD_FILES_CREATE_REQ = 0X57;
    public static final int I4ANDROID_E_CMD_FILES_CREATE_RESP = 0X58;
    public static final int I4ANDROID_E_CMD_FILES_DELETE_REQ = 0X59;
    public static final int I4ANDROID_E_CMD_FILES_DELETE_RESP = 0X60;
    public static final int I4ANDROID_E_CMD_FILES_RENAME_REQ = 0X61;
    public static final int I4ANDROID_E_CMD_FILES_RENAME_RESP = 0X62;
    public static final int I4ANDROID_E_CMD_FILES_EXPORT_REQ = 0X63;
    public static final int I4ANDROID_E_CMD_FILES_EXPORT_RESP = 0X64;
    public static final int I4ANDROID_E_CMD_FILES_IMPORT_REQ = 0X65;
    public static final int I4ANDROID_E_CMD_FILES_IMPORT_RESP = 0X66;
    public static final int I4ANDROID_E_CMD_FILES_RESOURCE_IMPORT_REQ = 0X67; // 请求导入文件资源
    public static final int I4ANDROID_E_CMD_FILES_RESOURCE_IMPORT_RESP = 0X68;// 请求导入文件资源的响应
    public static final int I4ANDROID_E_CMD_FILES_RESOURCE_EXPORT_REQ = 0X69;// 请求导出文件资源
    public static final int I4ANDROID_E_CMD_FILES_RESOURCE_EXPORT_RESP = 0X70;// 请求导出文件资源的响应
    public static final int I4ANDROID_E_CMD_IMG_RESOURCE_EXPORT_REQ = 0X71;// 请求导出图片资源
    public static final int I4ANDROID_E_CMD_IMG_RESOURCE_EXPORT_RESP = 0X72;// 请求导出图片资源的响应
    public static final int I4ANDROID_E_CMD_CONTACT_IMPORT_REQ = 0x73;// 请求导出联系人信息
    public static final int I4ANDROID_E_CMD_CONTACT_IMPORT_RESP = 0x74;// 请求导入联系人信息的响应
    public static final int I4ANDROID_E_CMD_FILE_COPY_REQ = 0x75;
    public static final int I4ANDROID_E_CMD_FILE_COPY_RESP = 0x76;
    public static final int I4ANDROID_E_CMD_FILE_GET_COPY_SIZE_REQ = 0x77;		//请求获取复制个数
    public static final int I4ANDROID_E_CMD_FILE_GET_COPY_SIZE_RESP = 0x78;    //请求获取复制个数的响应


    public static final int I4ANDROID_E_CMD_FILE_CANCEL_COPY_REQ = 0x79;//请求取消复制
    public static final int I4ANDROID_E_CMD_FILE_CANCEL_COPY_RESP = 0x80;//请求取消复制的响应

    public static final int I4ANDROID_E_CMD_APK_ICON_REQ = 0x81;//请求获取图标
    public static final int I4ANDROID_E_CMD_APK_ICON_RESP = 0x82;//请求获取图标的响应

    public static final int I4ANDROID_E_CMD_SOCKET_LISTENER_REQ = 0x1A;//请求通知监测命令
    public static final int I4ANDROID_E_CMD_SOCKET_LISTENER_RESP = 0x1B;//请求通知监测命令的响应
    public static final int I4ANDROID_E_CMD_MESSAGE_DEL_REQ = 0x1C;//请求删除信息
    public static final int I4ANDROID_E_CMD_MESSAGE_DEL_RESP = 0x1D;//请求删除信息命令的响应
    public static final int I4ANDROID_E_CMD_MESSAGE_INPORT_RESP = 0x1E;// 请求短信内容

    public static final int I4ANDROID_E_CMD_FILES_ROOT_REQ = 0x1F;//请求获取文件夹根目录
    public static final int I4ANDROID_E_CMD_FILES_ROOT_RESP = 0x2A;// 请求获取文件夹根目录的响应

    public static final int I4ANDROID_E_CMD_CONTACT_GROUP_IMPORT_REQ = 0x15;// 请求导入组信息
    public static final int I4ANDROID_E_CMD_CONTACT_GROUP_IMPORT_RESP = 0x16;// 请求导入组信息的响应
    public static final int I4ANDROID_E_CMD_CONTACT_GROUP_DELETE_REQ = 0x2B;// 请求删除组信息
    public static final int I4ANDROID_E_CMD_CONTACT_GROUP_DELETE_RESP = 0x2C;// 请求删除组信息的响应

    public static final int I4ANDROID_E_CMD_CONTACT_ACCOUNT_REQ = 0x2D;// 请求获取账户信息
    public static final int I4ANDROID_E_CMD_CONTACT_ACCOUNT_RESP = 0x2E;// 请求获取账户信息的响应

    public static final int I4ANDROID_E_CMD_FILE_RING_REQ = 0x2F;// 请求获取铃声路径
    public static final int I4ANDROID_E_CMD_FILE_RING_RESP = 0x3A;// 请求获取铃声路径的响应

    public static final int I4ANDROID_E_CMD_FILE_RING_SETTING_REQ = 0x3B;// 请求设置铃声路径
    public static final int I4ANDROID_E_CMD_FILE_RING_SETTING_RESP = 0x3C;// 请求设置铃声路径的响应

    public static final int I4ANDROID_E_CMD_PHONE_CALL_REQ = 0x3D;// 请求获取通话记录
    public static final int I4ANDROID_E_CMD_PHONE_CALL_RESP = 0x3E;// 请求获取通话记录的响应

    public static final int I4ANDROID_E_CMD_PHONE_CALL_IMPORT_REQ = 0x3F;//请求导入记录
    public static final int I4ANDROID_E_CMD_PHONE_CALL_IMPORT_RESP = 0x4A;//请求导入记录的响应

    public static final int I4ANDROID_E_CMD_INSTALL_APK_REQ = 0x4B;//请求安装文件
    public static final int I4ANDROID_E_CMD_INSTALL_APK_RESP = 0x4C;//请求安装文件的响应

    public static final int I4ANDROID_E_CMD_UNINSTALL_APK_REQ = 0x4D;//请求卸载文件
    public static final int I4ANDROID_E_CMD_UNINSTALL_APK_RESP = 0x4E;//请求卸载文件的响应

    //手机连接模式  {data:[{mode :0 or 1; state:0 or -1}]}
    public static final int I4ANDROID_E_CMD_PC_LINK_PHONE_REQ = 0x4F;//请求连接模式
    public static final int I4ANDROID_E_CMD_PC_LINK_PHONE_RESP = 0x5A;//请求连接模式的响应

    //手机基本信息 {设备号 TelephonyManager.getDeviceId() ，厂商  android.os.Build.MODEL，系统版本 Build.VERSION_CODES}
    public static final int I4ANDROID_E_CMD_PHONE_INFO_REQ = 0x5B;//请求获取手机基本信息
    public static final int I4ANDROID_E_CMD_PHONE_INFO_RESP = 0x5C;//请求获取手机基本信息的响应

    //手机连接pc
    public static final int I4ANDROID_E_CMD_PHONE_LINK_PC_WIFI_REQ = 0x5D;//请求手机连接pc
    public static final int I4ANDROID_E_CMD_PHONE_LINK_PC_WIFI_RESP = 0x5E;//请求手机连接pc的响应
    public static final int I4ANDROID_E_CMD_PHONE_UNLINK_PC_WIFI_REQ = 0x5F;//请求手机连接pc
    public static final int I4ANDROID_E_CMD_PHONE_UNLINK_PC_WIFI_RESP = 0x6A;//请求手机连接pc的响应
    //pc连接手机
    //public static final int I4ANDROID_E_CMD_PC_LINK_PHONE_WIFI_REQ = 0x5F;//请求pc连接手机
    //public static final int I4ANDROID_E_CMD_PC_LINK_PHONE_WIFI_RESP = 0x6A;//请求pc连接手机的响应  authority
    //请求申请更改短信操作权限
    public static final int I4ANDROID_E_CMD_MESSAGE_CHANGE_AUTHORITY_REQ = 0x6B;//请求删除信息
    public static final int I4ANDROID_E_CMD_MESSAGE_CHANGE_AUTHORITY_RESP = 0x6C;//请求删除信息命令的响应
    public static final int I4ANDROID_E_CMD_GET_MD5_REQ = 0x6D;//请求指定路径的MD5值
    public static final int I4ANDROID_E_CMD_GET_MD5_RESP = 0x6E;//请求指定路径的MD5值
    /*** 头部长度 **/
    public static final int HEADER_LENGTH = 8;
    /** 类型 ***/
    public static final int TYPE_JSON = 0x01;
    public static final int TYPE_PNG = 0x02;
    public static final int TYPE_JPG = 0x03;
    public static final int TYPE_CSV = 0x04;
    public static final int TYPE_VCF = 0x05;
    public static final int TYPE_MP3 = 0x06;
    public static final int TYPE_MP4 = 0x07;
    public static final int TYPE_APK = 0X08;
    public static final int TYPE_BYTE = 0x09;
    public static final int SIZE = 8;
    private int space = 1024 * 10;
    private ByteBuffer byteBuffer = ByteBuffer.allocate(space);

    /** 发送错误数据数据 **/
    public ByteBuffer getReponseErrData(int response, String errMsg) {
        ByteBuffer byteBuff = null;
        try {
            // 请求手机短信、联系人、通话记录的个数
            JSONObject obj = new JSONObject();
            obj.put("return", -1);
            obj.put("reason", errMsg);
            //obj.put("data", new JSONObject());

            int length = obj.toString().getBytes("UTF8").length;

            byteBuff = ByteBuffer.allocate(HEADER_LENGTH + length);
            // 返回头部位坐标0:响应命令
            byteBuff.put((byte) response);
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

            byteBuff.rewind();
        } catch (Exception ex) {
            LogUtil.debug("发送sorket内容出错" + ex.getMessage());
        }

        return byteBuff;
    }

    /** 发送基本成功数据数据 **/
    public ByteBuffer getReponseSucessData(int response) {
        ByteBuffer byteBuff = null;
        try {
            // 请求手机短信、联系人、通话记录的个数
            JSONObject obj = new JSONObject();
            obj.put("return", 0);
            obj.put("reason", "");
            obj.put("data", "");

            int length = obj.toString().getBytes("UTF8").length;

            byteBuff = ByteBuffer.allocate(HEADER_LENGTH + length);
            // 返回头部位坐标0:响应命令
            byteBuff.put((byte) response);
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

            byteBuff.rewind();
        } catch (Exception ex) {
            LogUtil.debug("发送sorket内容出错" + ex.getMessage());
        }

        return byteBuff;
    }

    /** 发送基本成功数据数据 **/
    public ByteBuffer getReponseData(int response,String[] keys , Object[] values) {
        ByteBuffer byteBuff = null;
        try {
            JSONObject obj = new JSONObject();
            obj.put("return", 0);
            obj.put("reason", "");
            JSONArray jsonArr = new JSONArray();
            JSONObject tmpJ = new JSONObject();
            for(int i = 0; i < keys.length ; i++){
                Object tmpValue = values[i];
                if(tmpValue == null){
                    tmpValue = "";
                }

                tmpJ.put(keys[i], tmpValue);
            }
            jsonArr.put(tmpJ);
            obj.put("data", jsonArr);

            int length = obj.toString().getBytes("UTF8").length;
            byteBuff = ByteBuffer.allocate(HEADER_LENGTH + length);
            // 返回头部位坐标0:响应命令
            byteBuff.put((byte) response);
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

            byteBuff.rewind();
        } catch (Exception ex) {
            LogUtil.debug("发送sorket内容出错" + ex.getMessage());
        }

        return byteBuff;
    }

    /** 发送基本成功数据数据 **/
    public ByteBuffer getReponseEndData(int response) {
        ByteBuffer headerByteBuff = ByteBuffer.allocate(8);
        try {
            // 返回头部位坐标0:响应命令
            headerByteBuff.put((byte) response);

            // 返回头部位坐标1-4:内容长度
            headerByteBuff.put(ConvertUtil.intToByteArray( 0));
            // 返回头部位坐标5:内容类型
            headerByteBuff.put((byte) TYPE_BYTE);
            // 返回头部位坐标6:拓展字节
            headerByteBuff.put((byte) 0);
            // 返回头部位坐标7:拓展字节
            headerByteBuff.put((byte) 1);

            headerByteBuff.rewind();
            headerByteBuff.limit(8);

        } catch (Exception ex) {
            LogUtil.debug("发送sorket内容出错" + ex.getMessage());
        }

        return headerByteBuff;
    }


    public ByteBuffer getHeaderData(int requestCode,int length,int type,int extra1,int extra2){
        //添加头部包头信息
        ByteBuffer headerByte = ByteBuffer.allocate(8);
        //////////////////添加头部信息/////////////////////////
        // 返回头部位坐标0:响应命令
        headerByte.put((byte) requestCode);
        // 返回头部位坐标1-4:内容长度
        headerByte.put(ConvertUtil.intToByteArray(length));
        // 返回头部位坐标5:内容类型
        headerByte.put((byte) type);
        // 返回头部位坐标6:拓展字节
        headerByte.put((byte) extra1);
        // 返回头部位坐标7:拓展字节
        headerByte.put((byte) extra2);
        headerByte.rewind();
        return headerByte;
    }

    /**获取Json的ByteBuffer*/
    public ByteBuffer getByteBufferForJson(int requestCode,String json) throws UnsupportedEncodingException {
        byte[] buff = json.getBytes("UTF-8");
        int length = json.getBytes("UTF-8").length;
        ByteBuffer headerBuff = getHeaderData(requestCode,length,TYPE_JSON,1,1 );
        headerBuff = appendBytes(headerBuff,buff);
        headerBuff.rewind();
        headerBuff.limit(headerBuff.capacity());
        return headerBuff;
    }
    public ByteBuffer appendBytes(ByteBuffer byteBuffer, byte[] buff){
        if(byteBuffer != null){
            ByteBuffer tmpBuff = ByteBuffer.allocate(byteBuffer.capacity() + buff.length );
            tmpBuff.put(byteBuffer);
            tmpBuff.put(buff);
            return tmpBuff;
        }

        return byteBuffer;
    }

    public ByteBuffer putByte(ByteBuffer byteBuffer, byte[] buff){
        if(byteBuffer != null){
            if(byteBuffer.capacity() >= byteBuffer.position() + buff.length){
                byteBuffer.put(buff);
            }else{
                LogUtil.debug("分配空间不够，重新分配");
                ByteBuffer tmpBuff = ByteBuffer.allocate(byteBuffer.position() + buff.length );
                tmpBuff.put(byteBuffer);
                tmpBuff.put(buff);

                return tmpBuff;
            }
        }

        return byteBuffer;
    }

    public ByteBuffer sendFileDetail(SocketChannel sc, int response , List list) throws IOException {
        ByteBuffer dataBuffer = ByteBuffer.allocate(1024);
        int count = 0;
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                if (obj != null && !"".equals(obj)) {
                    if (obj instanceof String) {
                        byte[] bStr = obj.toString().getBytes("utf-8");// 4
                        count = count + 4 + bStr.length;
                        dataBuffer = putByte(dataBuffer,
                                ConvertUtil.intToByteArray(bStr.length));// 4
                        dataBuffer = putByte(dataBuffer, bStr);
                    }else if(obj instanceof Byte){
                        count = count + 1;
                        dataBuffer = putByte(dataBuffer,
                                new byte[] { (byte) ConvertUtil.objToInt(obj) });
                    } else if (obj instanceof Integer) {
                        count = count + 4;
                        dataBuffer = putByte(dataBuffer,ConvertUtil.intToByteArray(ConvertUtil.objToInt(obj)));
                    } else if (obj instanceof Boolean) {
                        count = count + 1;
                        dataBuffer = putByte(dataBuffer,
                                new byte[] { (byte) ConvertUtil.objToInt(obj) });
                    } else if (obj instanceof Long) {
                        count = count + 8;
                        dataBuffer = putByte(dataBuffer,ConvertUtil.longToByteArray(ConvertUtil.objToLong(obj)));
                    }
                }else{
                    count = count + 4 + 0;
                    dataBuffer = putByte(dataBuffer,ConvertUtil.intToByteArray(0));// 4
                }
            }

            dataBuffer.rewind();
            dataBuffer.limit(count);
        }

        // 添加头部包头信息
        if (HEADER_LENGTH + count > space) {
            // 如果内容长度大于预存控件，重新申请空间
            byteBuffer = ByteBuffer.allocate(HEADER_LENGTH + count);
        }

        byteBuffer.clear();
        // ////////////////添加头部信息/////////////////////////
        // 返回头部位坐标0:响应命令
        byteBuffer.put((byte) response);
        // 返回头部位坐标1-4:内容长度
        byteBuffer.put(ConvertUtil.intToByteArray(count));
        // 返回头部位坐标5:内容类型
        byteBuffer.put((byte) TYPE_BYTE);
        // 返回头部位坐标6:拓展字节
        byteBuffer.put((byte) 0);
        // 返回头部位坐标7:拓展字节
        byteBuffer.put((byte) 0);

        // 添加数据信息
        byteBuffer.put(dataBuffer);

        byteBuffer.rewind();
        byteBuffer.limit(HEADER_LENGTH + count);

        while (byteBuffer.hasRemaining()) {
            sc.write(byteBuffer);
        }

        return dataBuffer;

        // 释放内存
        //dataBuffer = null;
        //byteBuffer.clear();
    }
}
