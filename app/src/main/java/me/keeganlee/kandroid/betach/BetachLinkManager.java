package me.keeganlee.kandroid.betach;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;

import me.keeganlee.kandroid.bean.LinkPcBean;
import me.keeganlee.kandroid.socket.BetachClient;
import me.keeganlee.kandroid.tools.AppConfig;
import me.keeganlee.kandroid.tools.CommonUtils;
import me.keeganlee.kandroid.socket.BetachServer;
import me.keeganlee.kandroid.tools.ConvertUtil;
import me.keeganlee.kandroid.tools.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class BetachLinkManager extends BaseBetach{
    public static final int PACKAGE_REMOVED = 0;
    public static final int PACKAGE_ADDED = 1;
    public static final int PACKAGE_REPLACED = 2;
    public static final int SMS_RECEIVED = 4;
    public static final int SMS_SEND_ACTIOIN = 3;
    public static final int ACTION_SDCARD_CHANGE = 5;
    public static final int ACTION_LINK_PC = 6;
    public static final int PACKAGE_CANCEL_INSTALL = 7;
    public static final int ACTION_CHANGE_AUTHORITY = 8;//监听短信授权
    private Context context;
    private boolean isRealSend = false;//是否实时发送
    private SendThread sendThread = null;

    public BetachLinkManager(Context context) {
        this.context = context;
    }

    public void sendUnLinkPc() {
        ContentValues values = new ContentValues();
        values.put("action", ACTION_LINK_PC);
        values.put("state", -1);
        send(getLongLinkJson(values));
    }

    public void sendLinkPc(String linkUrl) {
        // 开始连接pc服务器
        final LinkPcBean bean = new LinkPcBean();
        String url = linkUrl.toString();// "http://android.25pp.com/connect_device/index.html?pcid=werkjoikjwerk&ip=192.168.1.254&v=122&p=5555";
        LogUtil.debug("扫描到数据 ： " + url);
        bean.setUrl(url.substring(0, url.indexOf("?")));

        String param = url.substring(url.indexOf("?") + 1);
        String[] params = param.split("&");
        for (int i = 0; i < params.length; i++) {
            String tmpParam = params[i];
            String[] items = tmpParam.split("=");
            String key = (items != null && items.length > 0)?items[0]:null;
            String value = (items != null && items.length > 1)?items[1]:null;
            if ("pcid".equals(key)) {
                bean.setPcId(value);
            }
            if ("ip".equals(key)) {
                bean.setIp(value);
            }
            if ("v".equals(key)) {
                bean.setVersionCode(ConvertUtil.objToInt(value));
            }

            if ("p".equals(key)) {
                bean.setPort(ConvertUtil.objToInt(value));
            }
        }

        if(bean.getIp() != null && bean.getPort() > -1){
            final Handler handler = new Handler() {

                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                }

            };

            new Thread() {
                @Override
                public void run() {
                    try {
                        BaseBetach baseBetach = new BaseBetach();
                        BetachClient client = new BetachClient(bean.getIp(),
                                bean.getPort());
                        client.sendMsg(baseBetach.getReponseData(
                                BaseBetach.I4ANDROID_E_CMD_PHONE_LINK_PC_WIFI_REQ,
                                new String[] { "ip", "port" }, new Object[] {
                                        getDefaultIpAddresses(context),
                                        BetachServer.PC_ANDROID_PORT }));
                        //client.close();
                        handler.sendEmptyMessage(0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();

        }

    }

    public void updateLinkMode(ContentValues values , String _id){
        ContentResolver contentResovlver=context.getContentResolver();
        Uri url=Uri.parse("content://com.i4.entry.PCSerContentProvider/link_pc_info");
        Cursor cursor = contentResovlver.query(url, null, " _id = ? ", new String[]{_id}, null);
        if(cursor != null && cursor.getCount() > 0){
            update(values,_id);
        }else{
            insert(values);
        }

        cursor.close();
    }

    public void setIsRealSend(boolean isRealSend){
        this.isRealSend = isRealSend;
    }

    class SendThread extends Thread {
        private String jSon ;
        public SendThread(String jSon){
            this.jSon = jSon;
        }
        @Override
        public void run() {
            try {
                do{
                    SelectionKey key = (SelectionKey) AppConfig.Session.get("SelectionKey");
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

                    if(isRealSend){
                        Thread.sleep(500);
                    }

                }while(isRealSend);

                if(isRealSend){
                    LogUtil.debug("已经断掉长连接 ");
                }

            } catch (IOException e) {
                LogUtil.debug("监听与pc的链接： 异常断开，设置标志位  ，异常信息：" + e.getMessage());
                Intent intent = new Intent("com.i4.i4pcserverconnect.PCService");
                intent.putExtra("type", 3);
                context.startService(intent);

            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }finally{
                sendThread = null;
            }
        }
    };

    public  void send(String jSon) {
        if(sendThread == null){
            sendThread = new SendThread(jSon);
            sendThread.start();
        }
    }

    public void setLinkData(ContentValues values){
        updateLinkMode(values, "1");
    }

    private String getDefaultIpAddresses(Context context) {
        // 获取wifi服务
        WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        // 判断wifi是否开启
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        String ip = intToIp(ipAddress);
        return ip;
    }

    private String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
                + "." + (i >> 24 & 0xFF);
    }

    private void insert(ContentValues values)    {
        //访问内容提供者的步骤
        //需要内容解析者
        ContentResolver contentResovlver= context.getContentResolver();
        Uri url=Uri.parse("content://com.i4.entry.PCSerContentProvider/link_pc_info");
        contentResovlver.insert(url, values);
    }

    private void update(ContentValues values,String _id)    {
        //访问内容提供者的步骤
        //需要内容解析者
        ContentResolver contentResovlver= context.getContentResolver();
        Uri url=Uri.parse("content://com.i4.entry.PCSerContentProvider/link_pc_info");
        contentResovlver.update(url, values, " _id = ?", new String[]{_id});
    }


	/*public class LinkPcObserver extends ContentObserver {
		public LinkPcObserver(Handler handler) {
			super(handler);
		}

		public void onChange(boolean selfChange) {
			LogUtil.debug("连接服务检测到数据变换");

			int mode = 0;
			int state = -1;
			Cursor cursor = null;
			try{
				ContentResolver contentResovlver = context.getContentResolver();
				Uri url = Uri
						.parse("content://com.i4.entry.PCSerContentProvider/link_pc_info");

				cursor = contentResovlver.query(url, null, " _id = ? ", new String[]{"1"}, null);
				if (cursor != null && cursor.moveToFirst()) {
					mode = cursor.getInt(cursor.getColumnIndex("mode"));
					state = cursor.getInt(cursor.getColumnIndex("state"));
					LogUtil.debug(" mode " + mode + " state " + state);
					if(state == 0){
						//LogUtil.debug("监听已经连接服务 ，但还没 开启长连接 ； mode:" + mode + "; state:" + state);
					}else{
						LogUtil.debug("尝试 断掉长连接 ； mode:" + mode + "; state:" + state);
						setIsRealSend(false);
					}
				}

			}catch(IllegalArgumentException ex){
			}finally{
				if(cursor != null){
					cursor.close();
				}
			}

		}
	}*/



    public String getLongLinkJson(ContentValues values){
        JSONObject jObj = new JSONObject();
        try {
            jObj.put("return", 0);
            jObj.put("msg", "");
            JSONArray jArr = new JSONArray();
            JSONObject tmpObj = new JSONObject();

            if(values != null){
                Set<String> set = values.keySet();
                Iterator<String> iterator = set.iterator();
                while(iterator.hasNext()){
                    String key = iterator.next();
                    tmpObj.put(key, values.get(key));
                }
            }

            jArr.put(tmpObj);
            jObj.put("data", jArr);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jObj.toString();
    }

}
