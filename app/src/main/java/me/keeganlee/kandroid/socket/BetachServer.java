package me.keeganlee.kandroid.socket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract.Groups;
import android.provider.Telephony;
import android.telephony.TelephonyManager;

import me.keeganlee.kandroid.activity.ComposeSmsActivity;
import me.keeganlee.kandroid.betach.BaseBetach;
import me.keeganlee.kandroid.betach.BetachApkManager;
import me.keeganlee.kandroid.betach.BetachCallManager;
import me.keeganlee.kandroid.betach.BetachFileManager;
import me.keeganlee.kandroid.betach.BetachLinkManager;
import me.keeganlee.kandroid.betach.BetachMyPictureManager;
import me.keeganlee.kandroid.betach.ContactManager;
import me.keeganlee.kandroid.tools.AppConfig;
import me.keeganlee.kandroid.tools.ConvertUtil;
import me.keeganlee.kandroid.tools.FileUtil;
import me.keeganlee.kandroid.tools.LogUtil;
import me.keeganlee.kandroid.tools.MD5AndSignature;

public class BetachServer {
    private static final String TAG = "BetachServer";
    public static final int PC_ANDROID_PORT = 12521;
    ServerSocket server = null;
    private Context context;
    private String errMsg;
    private ContactManager contactManager = null;
    private BetachFileManager fileManager = null;//new BetachFileManager(context);

    private boolean isRun = true;

    private boolean isRntOk = true;


    public BetachServer(Context context) {
        this.context = context;
        contactManager = new ContactManager(context);
        start(PC_ANDROID_PORT);
    }

    void stop() {
        isRun = false;
    }


    void start(int port) {

        try {
            ServerSocketChannel s = ServerSocketChannel.open();
            ServerSocket socket = s.socket();
            final Selector selector = Selector.open();
            InetSocketAddress addr = new InetSocketAddress(port);
            socket.bind(addr);
            s.configureBlocking(false);
            s.register(selector, SelectionKey.OP_ACCEPT);
            new BetachServerThread(selector);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    class BetachServerThread extends Thread {
        private Selector selector;

        public BetachServerThread(Selector selector) {
            this.selector = selector;
            start();
        }

        @Override
        public void run() {
            try {
                while (isRun) {
                    int selCount = selector.select();
                    if (selCount <= 0) continue;

                    Iterator<SelectionKey> keys = selector.selectedKeys()
                            .iterator();
                    while (keys.hasNext()) {
                        final SelectionKey key = (SelectionKey) keys.next();
                        keys.remove();
                        if (key.isAcceptable()) {
                            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                            SocketChannel sc = ssc.accept();
                            sc.configureBlocking(false);
                            sc.register(selector, SelectionKey.OP_READ);
                        } else if (key.isReadable()) {
                            new Thread() {
                                @Override
                                public void run() {
                                    ByteBuffer resqBuff = null;
                                    try {
                                        RequestBean bean = SocketUtil.analyzeRequest((SocketChannel) key.channel());
                                        resqBuff = response((SocketChannel) key.channel(),bean);
                                        if (resqBuff != null) {
                                            SocketChannel sc = (SocketChannel) key.channel();
                                            while (resqBuff.hasRemaining()) {
                                                int count = sc.write(resqBuff);
                                                LogUtil.debug("发送数据个数：" + count);
                                            }
                                            resqBuff.clear();
                                        }

                                    } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }


                                }
                            }.start();
                            // 写完后取消可写事件，仅监听可读事件
                            //key.interestOps(SelectionKey.OP_READ);

							/*ByteBuffer bb = resqBuff.duplicate();
                            key.attach(bb);*/

							/*key.interestOps(SelectionKey.OP_WRITE
                                    | SelectionKey.OP_READ);*/

                            key.cancel();
                        }
                    }
                }

            } catch (Exception ex) {
            }
        }
    }

    public ByteBuffer read(SocketChannel sc, int length) throws IOException {
        ByteBuffer rntValue = ByteBuffer.allocate(length);
        int count = 0;
        do {
            int tmpCount = sc.read(rntValue);
            count = count + tmpCount;
        } while (count < length);

        return rntValue;
    }



    public ByteBuffer response(SocketChannel sc, RequestBean requestBean) throws IOException {

        ByteBuffer resqBuff = null;
        try {
            LogUtil.debug("接受到指令，指令code ： " + requestBean.getRequestCode() + ",data:" + requestBean.getJsonData());
            switch (requestBean.getRequestCode()) {
                case BaseBetach.I4ANDROID_E_CMD_ERROR:
                    sc.socket().shutdownInput();
                    break;
                case BaseBetach.I4ANDROID_E_CMD_VERSION_REQ:
                    resqBuff = ByteBuffer.wrap(contactManager.getReponseVersion());

                    break;
                case BaseBetach.I4ANDROID_E_CMD_DATANUM_REQ:
                    resqBuff = ByteBuffer.wrap(contactManager.getReponseDataNum());
                    break;
                case BaseBetach.I4ANDROID_E_CMD_FILES_REQ:
                    fileManager = new BetachFileManager(context);
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        JSONArray arrJson = json.getJSONArray("data");
                        JSONObject dataJson = arrJson.getJSONObject(0);
                        String path = dataJson.getString("path");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);

                            // 获取path的数据
                            resqBuff = ByteBuffer.wrap(fileManager
                                    .getFilesResp(path));

                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_FILES_RESP,
                                            "接收json数据出问题，理由 ： "
                                                    + msg);
                        }
                    } else {
                        // data为空，获取全部数据
                        resqBuff = ByteBuffer.wrap(fileManager.getFilesResp(Environment
                                .getExternalStorageDirectory().getAbsolutePath()));
                    }

                    break;
                case BaseBetach.I4ANDROID_E_CMD_FILES_CREATE_REQ:
                    // 文件格式
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        JSONArray arrJson = json.getJSONArray("data");
                        JSONObject dataJson = arrJson.getJSONObject(0);
                        String path = dataJson.getString("path");
                        int fileType = dataJson.getInt("type");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);
                            BetachFileManager fileManage = new BetachFileManager(
                                    context);
                            try {
                                File file = new File(path);
                                if (fileType == 1) {
                                    // 创建文件夹
                                    if (file.mkdirs()) {
                                        resqBuff = fileManage
                                                .getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_FILES_CREATE_RESP);
                                    } else {
                                        resqBuff = fileManage.getReponseErrData(BaseBetach.I4ANDROID_E_CMD_FILES_CREATE_RESP, "创建失败，文件夹已经存在");
                                    }
                                } else if (fileType == 0) {
                                    // 创建文件
                                    if (FileUtil.creatNewFile(file)) {
                                        resqBuff = fileManage
                                                .getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_FILES_CREATE_RESP);
                                    } else {
                                        resqBuff = fileManage
                                                .getReponseErrData(
                                                        BaseBetach.I4ANDROID_E_CMD_FILES_CREATE_RESP,
                                                        "创建失败，文件已经存在");
                                    }
                                } else {
                                    resqBuff = fileManage
                                            .getReponseErrData(
                                                    BaseBetach.I4ANDROID_E_CMD_FILES_CREATE_RESP,
                                                    "客户端文件格式不正确，type只能为0或1");
                                }
                            } catch (Exception e) {
                                resqBuff = fileManage
                                        .getReponseErrData(
                                                BaseBetach.I4ANDROID_E_CMD_FILES_CREATE_RESP,
                                                "创建文件夹失败,失败原因:"
                                                        + e.getMessage());
                            }

                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_FILES_CREATE_RESP,
                                            "接收json数据出问题，理由 ： " + msg);
                        }
                    }
                    break;
                case BaseBetach.I4ANDROID_E_CMD_FILES_ROOT_REQ:
                    fileManager = new BetachFileManager(context);
                    resqBuff = ByteBuffer
                            .wrap(fileManager.getReponseRootFileData());
                    break;
                case BaseBetach.I4ANDROID_E_CMD_FILES_DELETE_REQ:
                    fileManager = new BetachFileManager(context);
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        JSONArray arrJson = json.getJSONArray("data");
                        JSONObject dataJson = arrJson.getJSONObject(0);
                        String path = dataJson.getString("path");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);
                            try {
                                File file = new File(path);
                                fileManager.deleteFile(file);
                                resqBuff = fileManager
                                        .getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_FILES_DELETE_RESP);
                            } catch (Exception e) {
                                resqBuff = fileManager
                                        .getReponseErrData(
                                                BaseBetach.I4ANDROID_E_CMD_FILES_DELETE_RESP,
                                                "删除文件夹失败,失败原因:"
                                                        + e.getMessage());
                            }

                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = fileManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_FILES_DELETE_RESP,
                                            "接收json数据出问题，理由 ： " + msg);
                        }
                    }
                    break;
                case BaseBetach.I4ANDROID_E_CMD_FILES_RENAME_REQ:
                    fileManager = new BetachFileManager(context);
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        JSONArray arrJson = json.getJSONArray("data");
                        JSONObject dataJson = arrJson.getJSONObject(0);
                        String path = dataJson.getString("path");
                        String newName = dataJson.getString("newName");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);
                            try {
                                File file = new File(path);
                                File newFile = new File(file.getParent(),
                                        newName);
                                file.renameTo(newFile);
                                resqBuff = fileManager
                                        .getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_FILES_RENAME_RESP);
                            } catch (Exception e) {
                                resqBuff = fileManager
                                        .getReponseErrData(
                                                BaseBetach.I4ANDROID_E_CMD_FILES_RENAME_REQ,
                                                "删除文件夹失败,失败原因:"
                                                        + e.getMessage());
                            }

                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_FILES_RENAME_REQ,
                                            "接收json数据出问题，理由 ： " + msg);
                        }
                    }
                    break;
                case BaseBetach.I4ANDROID_E_CMD_FILE_RING_SETTING_REQ://:
                    fileManager = new BetachFileManager(context);
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        if (rntId == 0) {
                            JSONArray arrJson = json.getJSONArray("data");
                            JSONObject tmpJ = arrJson.getJSONObject(0);
                            String path = tmpJ.getString("path");
                            int rngType = tmpJ.getInt("type");// 0 ：铃声                  1：通知          2：闹钟
                            fileManager.setVoice(path, rngType);

                            // 响应客户端请求，已经收到请求
                            sc.write(contactManager
                                    .getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_FILE_RING_SETTING_RESP));
                            LogUtil.debug("响应客户端请求 ， 客户端code ： "
                                    + BaseBetach.I4ANDROID_E_CMD_FILE_RING_SETTING_RESP);
                        } else {
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_FILE_RING_SETTING_RESP,
                                            msg);
                        }
                    }

                    break;
                case BaseBetach.I4ANDROID_E_CMD_FILE_RING_REQ:
                    fileManager = new BetachFileManager(context);
                    String[] paths = fileManager.getRingTone();
                    // 响应客户端请求，已经收到请求
                    sc.write(contactManager.getReponseData(BaseBetach.I4ANDROID_E_CMD_FILE_RING_RESP, new String[]{"callRingPath", "MsmRingPath", "noticeRingPath"}, paths));
                    LogUtil.debug("响应客户端请求 ， 客户端code ： " + BaseBetach.I4ANDROID_E_CMD_FILE_RING_RESP);

                    break;
                case BaseBetach.I4ANDROID_E_CMD_FILES_IMPORT_REQ:
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        if (rntId == 0) {
                            JSONArray arrJson = json.getJSONArray("data");
                            HashMap<String, HashMap<String, String>> map = new HashMap<String, HashMap<String, String>>();
                            for (int i = 0; i < arrJson.length(); i++) {
                                JSONObject obj = arrJson.getJSONObject(i);
                                HashMap<String, String> fileInfo = new HashMap<String, String>();
                                fileInfo.put("path", obj.getString("path"));
                                fileInfo.put("name", obj.getString("path"));
                                fileInfo.put("size", obj.getString("size"));
                                fileInfo.put("updateTime", obj.getString("updateTime"));
                                fileInfo.put("type", obj.getString("type"));
                                map.put(obj.getString("tmpId"), fileInfo);
                            }

                            AppConfig.Session.put("data", map);

                            // 响应客户端请求，已经收到请求
                            sc.write(contactManager
                                    .getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_FILES_IMPORT_RESP));
                            LogUtil.debug("响应客户端请求 ， 客户端code ： "
                                    + BaseBetach.I4ANDROID_E_CMD_FILES_IMPORT_RESP);
                        } else {
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_FILES_IMPORT_RESP,
                                            msg);
                        }
                    }

                    break;
                case BaseBetach.I4ANDROID_E_CMD_FILES_RESOURCE_IMPORT_REQ:
                    fileManager = new BetachFileManager(context);
                    HashMap<String, HashMap<String, String>> data = (HashMap<String, HashMap<String, String>>) AppConfig.Session
                            .get("data");
                    if (data != null && data.get(requestBean.getExpand() + "") != null) {
                        HashMap<String, String> fileInfo = data
                                .get(requestBean.getExpand() + "");
                        File file = new File(fileInfo.get("path"));
                        LogUtil.debug("开始响应写文件 ，文件路径 ： " + fileInfo.get("path"));

                        if (ConvertUtil.objToInt(fileInfo.get("type")) == 1) {
                            // 文件夹
                            if (!file.exists()) {
                                file.mkdirs();
                            }

                        } else {
                            // 文件
                            if (file.exists()) {
                                file.delete();
                            } else if (!file.getParentFile().exists()) {
                                file.getParentFile().mkdirs();
                            }
                            file.createNewFile();

                            long fileSize = ConvertUtil.objToLong(fileInfo.get("size"));
                            FileOutputStream stream = new FileOutputStream(file);
                            FileChannel outChannel = stream.getChannel();

                            ByteBuffer tmpFileBuff = ByteBuffer.allocate(64 * 1024);
                            int size = 0;
                            long countSize = 0;
                            while (countSize < fileSize) {
                                size = sc.read(tmpFileBuff);
                                tmpFileBuff.flip();
                                //byte[] buff = new byte[size];
                                //tmpFileBuff.get(buff);
                                //stream.write(buff);
                                outChannel.write(tmpFileBuff);
                                countSize = countSize + size;
                                // 清空
                                tmpFileBuff.clear();
                            }
                            outChannel.close();
                            stream.close();
                        }

                        data.remove(fileInfo);
                        //fileManager.deleteFromMediaStore(file);
                        fileManager.addInMediaStore(file);

                        sc.write(fileManager
                                .getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_FILES_RESOURCE_IMPORT_REQ));

                        LogUtil.debug("写文件成功 ");
                    } else {
                        sc.write(fileManager
                                .getReponseErrData(
                                        BaseBetach.I4ANDROID_E_CMD_FILES_RESOURCE_IMPORT_REQ,
                                        "Code:I4ANDROID_E_CMD_FILES_IMPORT_REQ命令没有先执行或者执行出错，请检查"));

                        LogUtil.debug("Code:I4ANDROID_E_CMD_FILES_IMPORT_REQ命令没有先执行或者执行出错，请检查");
                    }

                    break;
                case BaseBetach.I4ANDROID_E_CMD_FILES_EXPORT_REQ:
                    LogUtil.debug("接收到指令：  I4ANDROID_E_CMD_FILES_RESOURCE_EXPORT_REQ");
                    fileManager = new BetachFileManager(context);
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        JSONArray arrJson = json.getJSONArray("data");
                        JSONObject dataJson = arrJson.getJSONObject(0);
                        String path = dataJson.getString("path");
                        int fileType = 0;
                        if (dataJson.has("fileType")) {
                            fileType = dataJson.getInt("fileType");
                        }
                        ArrayList<String> extraDirPaths = null;
                        if (dataJson.has("extraDirPaths")) {
                            //过滤文件目录
                            extraDirPaths = new ArrayList<String>();
                            JSONArray extraPathsArrJson = dataJson.getJSONArray("extraDirPaths");
                            for (int i = 0; i < extraPathsArrJson.length(); i++) {
                                extraDirPaths.add(extraPathsArrJson.getJSONObject(i).getString("path"));
                            }
                        }
                        LogUtil.debug("解析头部信息：成功");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);

                            // 获取path的数据
                            resqBuff = ByteBuffer.wrap(fileManager.getExportFilesResp(path, fileType, extraDirPaths));

                            LogUtil.debug("处理完毕");
                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_FILES_RESP,
                                            "接收json数据出问题，理由 ： " + msg);
                        }
                    }

                    break;
                case BaseBetach.I4ANDROID_E_CMD_FILES_RESOURCE_EXPORT_REQ:
                    fileManager = new BetachFileManager(context);
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        JSONArray arrJson = json.getJSONArray("data");
                        JSONObject dataJson = arrJson.getJSONObject(0);
                        String path = dataJson.getString("path");

                        String range = "bytes=0-";
                        if (dataJson.has("range")) {
                            range = dataJson.getString("range");
                        }
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);

                            //RandomAccessFile randomFile = null;
                            FileInputStream fIs = null;

                            try {

                                fIs = new FileInputStream(path);
                                //randomFile = new RandomAccessFile(path, "r");
                                fileManager.getFileResourceResp(path, range, sc,
                                        fIs);
                            } catch (Exception ex) {
                                LogUtil.error(ex.getMessage());
                            } finally {
                                try {
                                    if (fIs != null) {
                                        fIs.close();
                                    }
                                    //randomFile.close();
                                } catch (Exception ex) {
                                }

                            }

                            LogUtil.debug("send 结束 ");

                            // sendFile(sc,file);
                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_FILES_RESOURCE_EXPORT_RESP,
                                            "接收json数据出问题，理由 ： " + msg);
                        }
                    }
                    break;
                case BaseBetach.I4ANDROID_E_CMD_FILE_COPY_REQ:
                    fileManager = new BetachFileManager(context);
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        final JSONArray arrJson = json.getJSONArray("data");

                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);
                            errMsg = null;
                            BetachFileManager.isStopThread = false;
                            BetachFileManager.copyByteCount = 0;
                            BetachFileManager.copyFileCount = 0;

                            Thread thread = new Thread() {
                                @Override
                                public void run() {
                                    try {
                                        for (int i = 0; i < arrJson.length(); i++) {
                                            if (BetachFileManager.isStopThread) {
                                                LogUtil.debug("暂停复制文件 ");
                                                break;
                                            }

                                            JSONObject dataJson = arrJson.getJSONObject(i);
                                            final int fileType = dataJson.getInt("type");
                                            final String fromPath = dataJson.getString("fromPath");
                                            final String toPath = dataJson.getString("toPath");

                                            LogUtil.debug("开始复制文件 ,fromPath ：" + fromPath);

                                            if (fileType == 0) {
                                                if (!fileManager.copyFile(fromPath, toPath)) {
                                                    errMsg = "复制文件出错";
                                                    break;
                                                } else {
                                                    fileManager.addInMediaStore(new File(toPath));
                                                }

                                            } else if (fileType == 1) {
                                                if (!fileManager.copyFolder(fromPath, toPath)) {
                                                    errMsg = "复制文件夹出错";
                                                    break;
                                                }

                                            }

                                            LogUtil.debug("复制文件结束,toPath : " + toPath);
                                        }
                                    } catch (Exception ex) {
                                        LogUtil.error(ex.getMessage());
                                    }

                                }

                            };
                            thread.start();

                            resqBuff = contactManager.getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_FILE_COPY_RESP);

                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager.getReponseErrData(BaseBetach.I4ANDROID_E_CMD_FILES_RESOURCE_EXPORT_RESP, "接收json数据出问题，理由 ： " + msg);
                        }
                    }
                    break;
                case BaseBetach.I4ANDROID_E_CMD_FILE_GET_COPY_SIZE_REQ:
                    BetachFileManager file = new BetachFileManager(context);
                    if (errMsg == null) {
                        resqBuff = ByteBuffer.wrap(file.getCopyFilesResp());
                    } else {
                        resqBuff = file.getReponseErrData(
                                BaseBetach.I4ANDROID_E_CMD_FILE_GET_COPY_SIZE_RESP,
                                errMsg);
                        errMsg = null;
                    }

                    break;
                case BaseBetach.I4ANDROID_E_CMD_FILE_CANCEL_COPY_REQ:
                    fileManager = new BetachFileManager(context);
                    BetachFileManager.isStopThread = true;
                    BetachFileManager.copyByteCount = 0;
                    BetachFileManager.copyFileCount = 0;
                    resqBuff = fileManager.getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_FILE_CANCEL_COPY_RESP);
                    break;
                case BaseBetach.I4ANDROID_E_CMD_CONTACT_REQ:
                    contactManager = new ContactManager(context);
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        JSONArray arrJson = json.getJSONArray("data");

                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);
                            JSONObject jsonObj = new JSONObject();
                            if (!arrJson.isNull(0)) {
                                jsonObj = arrJson.getJSONObject(0);
                            }
                            String where = null;
                            if (jsonObj.has("where")) {
                                where = jsonObj.getString("where");
                            }
                            // 请求获取通讯录信息
                            resqBuff = ByteBuffer.wrap(contactManager.getResponseContacts(where));

                            //resqBuff = ByteBuffer.wrap(contactManager.getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_CONTACT_RESP));

                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager.getReponseErrData(BaseBetach.I4ANDROID_E_CMD_CONTACT_RESP, "接收json数据出问题，理由 ： " + msg);
                        }
                    }

                    break;
                case BaseBetach.I4ANDROID_E_CMD_CONTACT_GROUP_REQ:
                    contactManager = new ContactManager(context);
                    // 请求获取通讯录分组信息
                    resqBuff = ByteBuffer.wrap(contactManager.getResponseContactGroup());
                    break;
                case BaseBetach.I4ANDROID_E_CMD_CONTACT_ACCOUNT_REQ:
                    contactManager = new ContactManager(context);
                    // 请求获取通讯录分组信息
                    resqBuff = ByteBuffer.wrap(contactManager.getResponseContactAccount());
                    break;
                case BaseBetach.I4ANDROID_E_CMD_IMG_RESOURCE_EXPORT_REQ:
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        JSONArray arrJson = json.getJSONArray("data");
                        JSONObject dataJson = arrJson.getJSONObject(0);
                        int id = dataJson.getInt("id");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);
                            resqBuff = ByteBuffer.wrap(contactManager
                                    .getResponseContactPhoto(id));
                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_IMG_RESOURCE_EXPORT_RESP,
                                            "接收json数据出问题，理由 ： " + msg);
                        }
                    }
                    break;
                case BaseBetach.I4ANDROID_E_CMD_CONTACT_IMPORT_REQ:
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);

                            JSONArray arrJson = json.getJSONArray("data");
                            for (int i = 0; i < arrJson.length(); i++) {
                                JSONObject dataJson = arrJson.getJSONObject(i);
                                LogUtil.debug("开始插入第 " + i + "个通讯录");
                                int id = contactManager.updateContact(dataJson);
                                if (id < 0) {
                                    LogUtil.debug("插入第 " + i + "个通讯录失败");
                                    continue;
                                } else {
                                    LogUtil.debug("插入第 " + i + "个通讯录成功");
                                }
                                resqBuff = contactManager.getReponseData(BaseBetach.I4ANDROID_E_CMD_CONTACT_IMPORT_RESP, new String[]{"id"}, new Object[]{id});
                            }

                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_CONTACT_IMPORT_RESP,
                                            "接收json数据出问题，理由 ： " + msg);
                        }
                    }
                    break;
                case BaseBetach.I4ANDROID_E_CMD_CONTACT_GROUP_IMPORT_REQ:
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);

                            JSONArray arrJson = json.getJSONArray("data");
                            for (int i = 0; i < arrJson.length(); i++) {
                                JSONObject dataJson = arrJson.getJSONObject(i);
                                LogUtil.debug("开始插入第 " + i + "个组");
                                int groupId = contactManager.updateGroup(dataJson);
                                if (groupId < 0) {
                                    LogUtil.debug("更新第 " + i + "个组信息失败");
                                    continue;
                                } else {
                                    LogUtil.debug("更新第 " + i + "个组信息成功");
                                }
                                resqBuff = contactManager.getReponseData(BaseBetach.I4ANDROID_E_CMD_CONTACT_GROUP_IMPORT_RESP, new String[]{Groups._ID}, new Object[]{groupId});
                            }

                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_CONTACT_GROUP_IMPORT_RESP,
                                            "接收json数据出问题，理由 ： " + msg);
                        }
                    }
                    break;
                case BaseBetach.I4ANDROID_E_CMD_CONTACT_GROUP_DELETE_REQ:
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);

                            JSONArray arrJson = json.getJSONArray("data");
                            for (int i = 0; i < arrJson.length(); i++) {
                                JSONObject dataJson = arrJson.getJSONObject(i);
                                LogUtil.debug("开始插入第 " + i + "个组");
                                int groupId = contactManager.deleteContactGroup(dataJson);
                                if (groupId < 0) {
                                    LogUtil.debug("删除第 " + i + "个组信息失败");
                                    continue;
                                } else {
                                    LogUtil.debug("删除第 " + i + "个组信息成功");
                                }
                                resqBuff = contactManager.getReponseData(BaseBetach.I4ANDROID_E_CMD_CONTACT_GROUP_DELETE_RESP, new String[]{Groups._ID}, new String[]{groupId + ""});
                            }

                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_CONTACT_GROUP_DELETE_RESP,
                                            "接收json数据出问题，理由 ： " + msg);
                        }
                    }
                    break;
/*			case BaseBetach.I4ANDROID_E_CMD_CONTACT_UPDATE_REQ:
                jsonStr = ConvertUtil.byteTOString(dataBuff.array());
				json = new JSONObject(jsonStr);
				if (json != null) {
					int rntId = json.getInt("return");
					String msg = json.getString("reason");
					if (rntId == 0) {
						LogUtil.debug("接收json数据成功");
						LogUtil.debug(json);

						JSONArray arrJson = json.getJSONArray("data");
						for (int i = 0; i < arrJson.length(); i++) {
							JSONObject dataJson = arrJson.getJSONObject(i);
							LogUtil.debug("开始修改第 " + i + "个通讯录");
							if (contactManager.updateContact(context, dataJson)>=0) {
								LogUtil.debug("更新第 " + i + "个通讯录失败");
								// 插入通讯录失败
								resqBuff = ByteBuffer
										.wrap(contactManager
												.getReponseErrData(
														BaseBetach.I4ANDROID_E_CMD_CONTACT_IMPORT_RESP,
														"接收json数据出问题，理由 ： "
																+ msg));
								continue;
							} else {
								LogUtil.debug("更新第 " + i + "个通讯录成功");
							}

						}

						resqBuff = ByteBuffer
								.wrap(contactManager
										.getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_CONTACT_IMPORT_RESP));

					} else {
						LogUtil.debug("接收json数据出问题，理由 ： " + msg);
						resqBuff = ByteBuffer
								.wrap(contactManager
										.getReponseErrData(
												BaseBetach.I4ANDROID_E_CMD_CONTACT_IMPORT_RESP,
												"接收json数据出问题，理由 ： " + msg));
					}
				}
				break;*/
                case BaseBetach.I4ANDROID_E_CMD_CONTACT_DELETE_REQ:
                    boolean isExcOk = false;
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);

                            JSONArray arrJson = json.getJSONArray("data");
                            if (arrJson.length() > 0) {
                                JSONObject obj = arrJson.getJSONObject(0);
                                int rawContactId = obj.getInt("contactId");
                                isExcOk = contactManager.deleteContact(context,
                                        rawContactId);
                            }

                            if (isExcOk) {
                                resqBuff = contactManager
                                        .getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_CONTACT_DELETE_RESP);
                            } else {
                                resqBuff = contactManager
                                        .getReponseErrData(
                                                BaseBetach.I4ANDROID_E_CMD_CONTACT_DELETE_RESP,
                                                "删除失败");
                            }

                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_CONTACT_DELETE_RESP,
                                            "接收json数据出问题，理由 ： " + msg);
                        }
                    }
                    break;
                case BaseBetach.I4ANDROID_E_CMD_APK_REQ:
                    //獲取應用信息
                    //獲取應用信息
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        if (rntId == 0) {
                            BetachApkManager betachApkManager = new BetachApkManager(context);
                            JSONArray arrJson = null;
                            if (json.has("data")) {
                                arrJson = json.getJSONArray("data");
                            }
                            if (arrJson != null && arrJson.length() > 0) {
                                JSONObject obj = arrJson.getJSONObject(0);
                                String pkgName = obj.getString("packageName");
                                resqBuff = ByteBuffer.wrap(betachApkManager.getAppsInfo(pkgName));
                            } else {
                                //如果data为空，获取所有apk信息
                                resqBuff = ByteBuffer.wrap(betachApkManager.getAppsInfo(null));
                            }
                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_APK_ICON_RESP,
                                            "接收json数据出问题，理由 ： " + msg);
                        }
                    }

                    break;
                case BaseBetach.I4ANDROID_E_CMD_APK_ICON_REQ:
                    //獲取應用信息
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        if (rntId == 0) {
                            JSONArray arrJson = json.getJSONArray("data");
                            if (arrJson.length() > 0) {
                                JSONObject obj = arrJson.getJSONObject(0);
                                String pkgName = obj.getString("packageName");
                                BetachApkManager betachApkManager = new BetachApkManager(context);
                                resqBuff = betachApkManager.getIcon(pkgName);
                            }
                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_APK_ICON_RESP,
                                            "接收json数据出问题，理由 ： " + msg);
                        }
                    }
                    break;
                case BaseBetach.I4ANDROID_E_CMD_MEDIA_REQ:
                    // json
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        JSONArray arrJson = json.getJSONArray("data");
                        JSONObject dataJson = arrJson.getJSONObject(0);
                        String path = dataJson.getString("path");
                        //isAll : 0 根据路径获取  1 获取全部
                        int isAll = 0;
                        if (dataJson.has("isAll")) {
                            isAll = dataJson.getInt("isAll");
                        }


                        //VIDEO = 0;
                        //AUDIO = 1;
                        //IMAGES = 2;
                        int fileType = dataJson.getInt("fileType");

                        ArrayList<String> extraDirPaths = null;
                        if (dataJson.has("extraDirPaths")) {
                            //过滤文件目录
                            extraDirPaths = new ArrayList<String>();
                            JSONArray extraPathsArrJson = dataJson.getJSONArray("extraDirPaths");
                            for (int i = 0; i < extraPathsArrJson.length(); i++) {
                                extraDirPaths.add(extraPathsArrJson.getJSONObject(i).getString("path"));
                            }
                        }
                        LogUtil.debug("解析头部信息：成功");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);

                            // 获取path的数据
                            BetachMyPictureManager betachMPManager = new BetachMyPictureManager(context);
                            betachMPManager.getExportFilesResp(sc, path, fileType, extraDirPaths, isAll);

                            resqBuff = betachMPManager.getReponseEndData(BaseBetach.I4ANDROID_E_CMD_MEDIA_RESP);
                            LogUtil.debug("处理完毕");
                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_FILES_RESP,
                                            "接收json数据出问题，理由 ： " + msg);
                        }
                    }

                    break;
                case BaseBetach.I4ANDROID_E_CMD_MEDIA_RESOURCE_REQ:
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        JSONArray arrJson = json.getJSONArray("data");
                        JSONObject dataJson = arrJson.getJSONObject(0);
                        String path = dataJson.getString("path");
                        int fileType = dataJson.getInt("fileType");
                        LogUtil.debug("解析头部信息：成功");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);

                            // 获取path的数据
                            //resqBuff = ByteBuffer.wrap(fileManager.getExportFilesResp(path,fileType,extraDirPaths));

                            BetachMyPictureManager betachMPManager = new BetachMyPictureManager(context);
                            resqBuff = betachMPManager.getCaptureImage(path, fileType);

                            LogUtil.debug("处理完毕");
                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_MEDIA_RESOURCE_RESP,
                                            "接收json数据出问题，理由 ： " + msg);
                        }
                    }
                    break;
                case BaseBetach.I4ANDROID_E_CMD_MESSAGE_REQ:
                    System.out.println("获取短信内容--------------------------------------start");
                    //获取短信内容
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        JSONArray arrJson = json.getJSONArray("data");
                        JSONObject dataJson = arrJson.getJSONObject(0);

                        //0:all  1:thread_id  2:thread_id  _id
                        int type = 0;
                        if (dataJson.has("type")) {
                            type = dataJson.getInt("type");
                        }
                        String thread_id = "";
                        if (dataJson.has("thread_id")) {
                            thread_id = dataJson.getString("thread_id");
                        }
                        String _id = "";
                        if (dataJson.has("_id")) {
                            _id = dataJson.getString("_id");
                        }

                        String sortOrder = "date desc";
                        if (dataJson.has("sortOrder")) {
                            sortOrder = dataJson.getString("sortOrder");
                        }

                        LogUtil.debug("解析头部信息：成功");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);
                            String where = " 1=1 ";
                            if (type == 1) {
                                where = " thread_id = '" + thread_id + "'";
                            } else if (type == 2) {
                                where = " thread_id = '" + thread_id + "' and _id='" + _id + "'";
                            }

                            contactManager = new ContactManager(context);
                            contactManager.getSmsInPhone(sc, where, sortOrder);

                            resqBuff = contactManager.getReponseEndData(BaseBetach.I4ANDROID_E_CMD_MESSAGE_RESP);
                            //resqBuff = ByteBuffer.wrap(contactManager.getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_MESSAGE_RESP));
                            LogUtil.debug("处理完毕");
                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_MEDIA_RESOURCE_RESP,
                                            "接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager.getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_MESSAGE_RESP);
                        }
                    }
                    System.out.println("获取短信内容--------------------------------------end");
                    break;
                case BaseBetach.I4ANDROID_E_CMD_MESSAGE_SEND_REQ:
                    //发送短信信息
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        JSONArray arrJson = json.getJSONArray("data");
                        JSONObject dataJson = arrJson.getJSONObject(0);

                        String body = null;
                        if (dataJson.has("body")) {
                            body = dataJson.getString("body");
                        }
                        String address = null;
                        if (dataJson.has("address")) {
                            address = dataJson.getString("address");
                        }
                        int thread_id = 0;
                        if (dataJson.has("thread_id")) {
                            thread_id = dataJson.getInt("thread_id");
                        }
                        String person = null;
                        if (dataJson.has("person")) {
                            person = dataJson.getString("person");
                        }

                        int sendId = 0;
                        if (dataJson.has("sendId")) {
                            sendId = dataJson.getInt("sendId");
                        }

                        int isNewThreadId = 0;
                        if (dataJson.has("isNewThreadId")) {
                            isNewThreadId = dataJson.getInt("isNewThreadId");
                        }

                        LogUtil.debug("解析头部信息：成功");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);
                            // int _id, int thread_id, String address, String person, String body, int sendId
                            contactManager = new ContactManager(context);
                            contactManager.sendSms(sendId, isNewThreadId, thread_id, address, person, body);
                            resqBuff = contactManager.getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_MESSAGE_SEND_RESP);
                            LogUtil.debug("处理完毕");

                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_MESSAGE_SEND_RESP,
                                            "接收json数据出问题，理由 ： " + msg);
                        }
                    }
                    break;
                case BaseBetach.I4ANDROID_E_CMD_MESSAGE_DEL_REQ:
                    //发送短信信息
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        JSONArray arrJson = json.getJSONArray("data");
                        JSONObject dataJson = arrJson.getJSONObject(0);
                        // 删除 目标 0: all   1:thread_id  2: thread_id,_id
                        int type = 0;
                        if (dataJson.has("type")) {
                            type = dataJson.getInt("type");
                        }

                        int thread_id = 0;
                        if (dataJson.has("thread_id")) {
                            thread_id = dataJson.getInt("thread_id");
                        }

                        int _id = 0;
                        if (dataJson.has("_id")) {
                            _id = dataJson.getInt("_id");
                        }

                        LogUtil.debug("解析头部信息：成功");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);
                            // int _id, int thread_id, String address, String person, String body, int sendId
                            contactManager = new ContactManager(context);
                            contactManager.deleteSms(type, thread_id, _id);
                            resqBuff = contactManager.getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_MESSAGE_DEL_RESP);
                            LogUtil.debug("处理完毕");

                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_MESSAGE_DEL_RESP,
                                            "接收json数据出问题，理由 ： " + msg);
                        }
                    }
                    break;
                case BaseBetach.I4ANDROID_E_CMD_MESSAGE_IMPORT_REQ:
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        AppConfig.Session.put("importTotal", json.getInt("importTotal"));
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);
                            JSONArray arrJson = json.getJSONArray("data");
                            contactManager = new ContactManager(context);
                            for (int i = 0; i < arrJson.length(); i++) {
                                JSONObject obj = arrJson.getJSONObject(i);
                                int _id = obj.getInt("_id");
                                int thread_id = obj.getInt("thread_id");
                                String address = obj.getString("address");
                                String person = obj.getString("person");
                                String body = obj.getString("body");
                                int tmpType = obj.getInt("type");
                                int read = obj.getInt("read");
                                long date = obj.getLong("date");
                            /*
                             if(contactManager.hasRecord(ContactManager.SMS.SMS_URI_ALL,_id, thread_id)){
								contactManager.updateSms(ContactManager.SMS.SMS_URI_ALL, _id, thread_id, address, person, body, tmpType,read,date);
								LogUtil.debug("更新成功");
							}else{

							Uri uri = contactManager.insertSms(ContactManager.SMS.SMS_URI_ALL, _id, thread_id, address, person, body, tmpType,read,date,0);
							LogUtil.debug("插入成功");

							}*/
                                Uri uri = contactManager.insertSms(ContactManager.SMS.SMS_URI_ALL, _id, thread_id, address, person, body, tmpType, read, date, 0);
                                LogUtil.debug("插入成功");
                            }

                            // 响应客户端请求，已经收到请求
                            sc.write(contactManager
                                    .getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_MESSAGE_INPORT_RESP));
                            LogUtil.debug("响应客户端请求 ， 客户端code ： "
                                    + BaseBetach.I4ANDROID_E_CMD_MESSAGE_INPORT_RESP);
                        } else {
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_MESSAGE_INPORT_RESP,
                                            msg);
                        }
                    }
                    break;
                case BaseBetach.I4ANDROID_E_CMD_PHONE_CALL_REQ:
                    BetachCallManager betachCallManager = new BetachCallManager(context);
                    betachCallManager.getCall(sc, BaseBetach.I4ANDROID_E_CMD_PHONE_CALL_RESP);
                    resqBuff = betachCallManager.getReponseEndData(BaseBetach.I4ANDROID_E_CMD_PHONE_CALL_RESP);

                    //释放资源
                    betachCallManager = null;
                    break;
                case BaseBetach.I4ANDROID_E_CMD_PHONE_CALL_IMPORT_REQ:
                    betachCallManager = new BetachCallManager(context);
                    betachCallManager.addCall(requestBean.getData());
                    resqBuff = betachCallManager.getReponseEndData(BaseBetach.I4ANDROID_E_CMD_PHONE_CALL_IMPORT_RESP);

                    //释放资源
                    betachCallManager = null;
                    break;

                case BaseBetach.I4ANDROID_E_CMD_INSTALL_APK_REQ:
                    //fileManager = new BetachFileManager(context);
                    //调用安装应用
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        errMsg = json.getString("reason");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);
                            JSONArray arrJson = json.getJSONArray("data");
                            if (arrJson != null && arrJson.length() > 0) {
                                JSONObject jsonObj = arrJson.getJSONObject(0);
                                String filePath = jsonObj.getString("filePath");
                                String md5 = jsonObj.getString("md5");
                                FileUtil.installFile(context, filePath);
                                //String packageName = jsonObj.getString("packageName");
                                //int versionCode = jsonObj.getInt("versionCode");

                                isRntOk = true;
                            } else {
                                isRntOk = false;
                            }

                        } else {
                            isRntOk = false;
                        }
                    }

                    if (isRntOk) {
                        // 响应客户端请求，已经收到请求
                        sc.write(fileManager
                                .getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_INSTALL_APK_RESP));
                        LogUtil.debug("响应客户端请求 ， 客户端code ： "
                                + BaseBetach.I4ANDROID_E_CMD_INSTALL_APK_RESP);
                    } else {
                        resqBuff = fileManager
                                .getReponseErrData(
                                        BaseBetach.I4ANDROID_E_CMD_INSTALL_APK_RESP,
                                        errMsg);
                    }
                    break;
                case BaseBetach.I4ANDROID_E_CMD_UNINSTALL_APK_REQ:
                    //fileManager = new BetachFileManager(context);
                    //调用安装应用
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        errMsg = json.getString("reason");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);
                            JSONArray arrJson = json.getJSONArray("data");
                            if (arrJson != null && arrJson.length() > 0) {
                                JSONObject jsonObj = arrJson.getJSONObject(0);
                                String packageName = jsonObj.getString("packageName");
                                FileUtil.uninstallAPK(context, packageName);
                                //String packageName = jsonObj.getString("packageName");
                                //int versionCode = jsonObj.getInt("versionCode");

                                isRntOk = true;
                            } else {
                                isRntOk = false;
                            }

                        } else {
                            isRntOk = false;
                        }
                    }

                    if (isRntOk) {
                        // 响应客户端请求，已经收到请求
                        sc.write(fileManager
                                .getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_UNINSTALL_APK_RESP));
                        LogUtil.debug("响应客户端请求 ， 客户端code ： "
                                + BaseBetach.I4ANDROID_E_CMD_UNINSTALL_APK_RESP);
                    } else {
                        resqBuff = fileManager
                                .getReponseErrData(
                                        BaseBetach.I4ANDROID_E_CMD_UNINSTALL_APK_RESP,
                                        errMsg);
                    }
                    break;
/*			case BaseBetach.I4ANDROID_E_CMD_UNINSTALL_APK_RESP:
                fileManager = new BetachFileManager(context);
				//调用安装应用
				jsonStr = ConvertUtil.byteTOString(dataBuff.array());
				json = new JSONObject(jsonStr);
				errMsg = null;
				isRntOk = true;
				if (json != null) {
					int rntId = json.getInt("return");
					errMsg = json.getString("reason");
					if (rntId == 0) {
						LogUtil.debug("接收json数据成功");
						LogUtil.debug(json);
						JSONArray arrJson = json.getJSONArray("data");
						if(arrJson != null && arrJson.length() > 0){
							JSONObject jsonObj = arrJson.getJSONObject(0);
							String packageName = jsonObj.getString("packageName");
							fileManager.uninstallAPK(context, packageName);
						}else{
							isRntOk = false;
						}

					} else {
						isRntOk = false;
					}
				}

				if(isRntOk){
					// 响应客户端请求，已经收到请求
					sc.write( fileManager
							.getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_INSTALL_APK_RESP)) ;
					LogUtil.debug("响应客户端请求 ， 客户端code ： "
							+ BaseBetach.I4ANDROID_E_CMD_INSTALL_APK_RESP);
				}else{
					resqBuff =  fileManager
									.getReponseErrData(
											BaseBetach.I4ANDROID_E_CMD_INSTALL_APK_RESP,
											errMsg) ;
				}
				break;*/
                case BaseBetach.I4ANDROID_E_CMD_PC_LINK_PHONE_REQ:
                    fileManager = new BetachFileManager(context);
                    //调用安装应用
                    errMsg = null;
                    isRntOk = true;
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        errMsg = json.getString("reason");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);
                            JSONArray arrJson = json.getJSONArray("data");
                            if (arrJson != null && arrJson.length() > 0) {
                                JSONObject jsonObj = arrJson.getJSONObject(0);
                                String pcId = jsonObj.getString("pcId");
                                int mode = jsonObj.getInt("mode");
                                int state = jsonObj.getInt("state");
                                System.out.println("建立于pc的链接： 连接 mode ； " + mode + "; state:" + state);
//							Intent intent = new Intent("com.i4.action.LINK_MODE");
//					        intent.putExtra("mode", mode);
//					        intent.putExtra("state", state);
//					        context.sendBroadcast(intent);

                                ContentValues values = new ContentValues();
                                values.put("pcid", pcId);
                                values.put("mode", mode);
                                values.put("state", state);

                                if (betachLink == null) {
                                    betachLink = new BetachLinkManager(context);
                                }


                                //发送通知最高优先级
                                Intent intent = new Intent("com.i4.i4pcserverconnect.PCService");

                                if (state == -1) {
                                    //接收到pc端的断开命令
                                    betachLink.setIsRealSend(false);

                                    //关闭
                                    intent.putExtra("type", 5);
                                } else {
                                    //启动
                                    intent.putExtra("type", 4);
                                }
                                context.startService(intent);
                                betachLink.setLinkData(values);

                            } else {
                                isRntOk = false;
                            }

                        } else {
                            isRntOk = false;
                        }
                    }

                    if (isRntOk) {
                        // 响应客户端请求，已经收到请求   192.168.8.1     193.168.8.2
                        sc.write(fileManager
                                .getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_PC_LINK_PHONE_RESP));
                        LogUtil.debug("响应客户端请求 ， 客户端code ： "
                                + BaseBetach.I4ANDROID_E_CMD_PC_LINK_PHONE_RESP);
                    } else {
                        resqBuff = fileManager
                                .getReponseErrData(
                                        BaseBetach.I4ANDROID_E_CMD_PC_LINK_PHONE_RESP,
                                        errMsg);
                    }
                    break;

                case BaseBetach.I4ANDROID_E_CMD_PHONE_INFO_REQ:
                    // 获取手机基本信息
                    TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    String deviceId = tm.getDeviceId();
                    String model = android.os.Build.MODEL;
                    String facturer = android.os.Build.MANUFACTURER;
                    String versionRelease = android.os.Build.VERSION.RELEASE;

                    sc.write(fileManager.getReponseData(BaseBetach.I4ANDROID_E_CMD_PHONE_INFO_RESP,
                            new String[]{"deviceId", "model", "facturer", "versionRelease"},
                            new Object[]{deviceId, model, facturer, versionRelease}));
                    LogUtil.debug("响应客户端请求 ， 客户端code ： " + BaseBetach.I4ANDROID_E_CMD_PHONE_INFO_RESP);

                    break;
                case BaseBetach.I4ANDROID_E_CMD_MESSAGE_CHANGE_AUTHORITY_REQ:
                    BaseBetach baseManager = new BaseBetach();
                    //调用安装应用
                    errMsg = null;
                    isRntOk = true;
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        errMsg = json.getString("reason");
                        if (rntId == 0) {
                            LogUtil.debug("接收json数据成功");
                            LogUtil.debug(json);
                            JSONArray arrJson = json.getJSONArray("data");
                            if (arrJson != null && arrJson.length() > 0) {
                                JSONObject jsonObj = arrJson.getJSONObject(0);
                                int type = jsonObj.getInt("type"); // 1请求获取更改信息权限
                                String packageName = context.getPackageName();//jsonObj.getString("packageName");
                                int state = -1; // 0 打开    -1 关闭
                                if (jsonObj.has("state")) {
                                    state = jsonObj.getInt("state");
                                }

                                switch (type) {
                        /*	case 0:
                                //请求是否已经存在更改信息权限   isAuthority 为-1 没有权限   0 已经有权限
								if(!Telephony.Sms.getDefaultSmsPackage(context).equals(packageName)){
									sc.write(baseManager.getReponseData(BaseBetach.I4ANDROID_E_CMD_MESSAGE_CHANGE_AUTHORITY_RESP, new String[]{"type","isAuthority"}, new Object[]{0,-1}));
								}else{
									sc.write(baseManager.getReponseData(BaseBetach.I4ANDROID_E_CMD_MESSAGE_CHANGE_AUTHORITY_RESP, new String[]{"type","isAuthority"}, new Object[]{0,0}));
								}

								break;*/
                                    case 1:
                                        //请求获取更改信息权限
                                        switch (state) {
                                            case -1:
                                                //关闭操作
                                                Object[] values = null;
                                                if (Telephony.Sms.getDefaultSmsPackage(context).equals(packageName)) {
                                                    Intent intent = new Intent(context, ComposeSmsActivity.class);
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                    context.startActivity(intent);

                                                    //告诉pc 还没有关闭  继续等待关闭
                                                    values = new Object[]{1, 0};
                                                } else {
                                                    //告诉pc 已经关闭 不需要等待
                                                    values = new Object[]{1, -1};
                                                }

                                                sc.write(baseManager.getReponseData(BaseBetach.I4ANDROID_E_CMD_MESSAGE_CHANGE_AUTHORITY_RESP, new String[]{"type", "isAuthority"}, values));

                                                isRntOk = true;
                                                break;
                                            case 0:
                                                //打开操作
                                                if (!Telephony.Sms.getDefaultSmsPackage(context).equals(packageName)) {
                                                    Intent intent = new Intent(context, ComposeSmsActivity.class);
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                    intent.putExtra("isOpenAuthority", true);
                                                    context.startActivity(intent);

                                                    //告诉pc 还没打开 继续等待
                                                    values = new Object[]{1, -1};
                                                } else {
                                                    //告诉pc 已经打开  不需要等待
                                                    values = new Object[]{1, 0};
                                                }

                                                sc.write(baseManager.getReponseData(BaseBetach.I4ANDROID_E_CMD_MESSAGE_CHANGE_AUTHORITY_RESP, new String[]{"type", "isAuthority"}, values));

                                                isRntOk = true;
                                                break;
                                        }

                                        break;
                                }
                            }

                        } else {
                            isRntOk = false;
                        }

                    } else {
                        isRntOk = false;
                    }

                    if (!isRntOk) {
                        resqBuff = baseManager
                                .getReponseErrData(
                                        BaseBetach.I4ANDROID_E_CMD_PC_LINK_PHONE_RESP,
                                        errMsg);
                    }

                    break;
                case BaseBetach.I4ANDROID_E_CMD_GET_MD5_REQ:
                    //獲取應用信息
                    if (requestBean.getJsonData() != null) {
                        JSONObject json = requestBean.getJsonData();
                        int rntId = json.getInt("return");
                        String msg = json.getString("reason");
                        if (rntId == 0) {
                            JSONArray arrJson = json.getJSONArray("data");
                            if (arrJson.length() > 0) {
                                JSONObject obj = arrJson.getJSONObject(0);
                                String pkgName = null;
                                String filePath = null;
                                int type = obj.getInt("type");//1为获取应用图片缓存md5，参数为packageName ,,2 为获取图片库缩略图md5 参数为 filePath ,3 为其他文件MD5 参数为filePath
                                switch (type) {
                                    case 1:
                                        pkgName = obj.getString("packageName");
                                        BetachApkManager betachApkManager = new BetachApkManager(context);
                                        resqBuff = betachApkManager.getPkgIconMd5(BaseBetach.I4ANDROID_E_CMD_GET_MD5_RESP, pkgName);
                                        break;
                                    case 2:
                                        filePath = obj.getString("filePath");
                                        int fileType = obj.getInt("fileType");
                                        BetachMyPictureManager betachMPManager = new BetachMyPictureManager(context);
                                        resqBuff = betachMPManager.getCaptureImageMd5(BaseBetach.I4ANDROID_E_CMD_GET_MD5_RESP, filePath, fileType);
                                        break;
                                    case 3:
                                        filePath = obj.getString("filePath");
                                        fileManager = new BetachFileManager(context);
                                        MD5AndSignature md5 = new MD5AndSignature();
                                        BaseBetach baseBetach = new BaseBetach();
                                        resqBuff = baseBetach.getReponseData(BaseBetach.I4ANDROID_E_CMD_GET_MD5_RESP, new String[]{"path", "md5"}, new String[]{filePath, md5.getFileMD5(new File(filePath), MD5AndSignature.MD5)});
                                        break;
                                }

                            }
                        } else {
                            LogUtil.debug("接收json数据出问题，理由 ： " + msg);
                            resqBuff = contactManager
                                    .getReponseErrData(
                                            BaseBetach.I4ANDROID_E_CMD_APK_ICON_RESP,
                                            "接收json数据出问题，理由 ： " + msg);
                        }
                    }
                    break;
                case BaseBetach.I4ANDROID_E_CMD_SOCKET_LISTENER_REQ:
                    if (sc != null && sc.isOpen()) {
                        AppConfig.Session.put("SelectionKey", sc);
                        //this.key = key;
                        System.out.println("监听机制已经建立");
                        resqBuff = contactManager.getReponseSucessData(BaseBetach.I4ANDROID_E_CMD_SOCKET_LISTENER_RESP);

                        if (resqBuff != null) {
                            while (resqBuff.hasRemaining()) {
                                sc.write(resqBuff);
                            }
                            resqBuff.clear();
                            resqBuff = null;
                        }

                        if (betachLink == null) {
                            betachLink = new BetachLinkManager(context);
                        }
                        betachLink.setIsRealSend(true);
                        ContentValues values = new ContentValues();
                        values.put("action", BetachLinkManager.ACTION_LINK_PC);
                        values.put("state", 0);
                        betachLink.send(betachLink.getLongLinkJson(values));
                    } else {
                        System.out.println("监听机制建立失败");

                        resqBuff = contactManager.getReponseErrData(BaseBetach.I4ANDROID_E_CMD_SOCKET_LISTENER_RESP, "监听机制建立失败");
                    }

                    break;
                default:
                    LogUtil.debug("接受到的指令是错误的，指令code ： " + requestBean.getRequestCode());
                    resqBuff = contactManager.getReponseErrData(
                            BaseBetach.I4ANDROID_E_CMD_ERROR, "接受到错误指令，指令code ："
                                    + requestBean.getRequestCode());
                    break;
            }

        } catch (Exception e) {
            LogUtil.error(e.getMessage());
            sc.close();
            System.out.println("close!");
        }

        LogUtil.debug("处理完成");

        return resqBuff;
    }

    BetachLinkManager betachLink;

    /**
     * 随机读取文件内容
     */

    public static void readFileByRandomAccess(String fileName) {
        RandomAccessFile randomFile = null;
        try {
            System.out.println("随机读取一段文件内容：");
            // 打开一个随机访问文件流，按只读方式
            randomFile = new RandomAccessFile(fileName, "r");
            // 文件长度，字节数
            long fileLength = randomFile.length();
            // 读文件的起始位置
            int beginIndex = (fileLength > 4) ? 4 : 0;
            // 将读文件的开始位置移到beginIndex位置。
            randomFile.seek(beginIndex);
            byte[] bytes = new byte[10];
            int byteread = 0;
            // 一次读10个字节，如果文件内容不足10个字节，则读剩下的字节。
            // 将一次读取的字节数赋给byteread
            while ((byteread = randomFile.read(bytes)) != -1) {
                System.out.write(bytes, 0, byteread);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (randomFile != null) {
                try {
                    randomFile.close();
                } catch (IOException e1) {
                }
            }
        }
    }

    public static void log(ByteBuffer buf) {
        try {
            System.out.println(new String(buf.array(), 0, buf.limit(), "utf8"));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
