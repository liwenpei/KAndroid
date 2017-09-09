package me.keeganlee.kandroid.socket;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import me.keeganlee.kandroid.betach.BaseBetach;
import me.keeganlee.kandroid.tools.ConvertUtil;
import me.keeganlee.kandroid.tools.LogUtil;

/**
 * Created by Administrator on 2017/9/9.
 */

public class SocketUtil {

    public static ByteBuffer read(SocketChannel sc, int length) throws IOException {
        if(sc == null || !sc.isConnected()){
            return null;
        }

        ByteBuffer rntValue = ByteBuffer.allocate(length);
        int count = 0;
        do {
            int tmpCount = sc.read(rntValue);
            if(tmpCount < 0){
                //已经断开
                return null;
            }
            count = count + tmpCount;
        } while (count < length);

        return rntValue;
    }

    /**
     * 解析请求
     */
    public static RequestBean analyzeRequest(SocketChannel sc) {
        //SocketChannel sc = (SocketChannel) key.channel();
        RequestBean bean = new RequestBean();
        try {
            //int count = sc.read(buffer);
            ByteBuffer buffer = SocketUtil.read(sc, BaseBetach.SIZE);
            if (buffer == null) {
                //通道已经关闭
                sc.close();
                return bean;
            }
            LogUtil.debug("接收到地址" + sc.socket().getRemoteSocketAddress() + "的信息" + ",头部信息：" + buffer.array());
            // 解析头部
            int requestCode = buffer.get(0) & 0xff; // 0
            byte[] dst = {buffer.get(1), buffer.get(2), buffer.get(3),
                    buffer.get(4)};
            int length = ConvertUtil.byteArrayToint(dst); // 1-4
            int type = buffer.get(5) & 0xff; // 5

            byte[] dstReserve = {buffer.get(6), buffer.get(7)};
            int bUsReserve = ConvertUtil.byteArrayToint(dstReserve); // 6-7
            //赋值头部信息
            bean.setRequestCode(requestCode);
            bean.setType(type);
            bean.setExpand(bUsReserve);
            JSONObject json = null;
            if (length > 0) {
                buffer = SocketUtil.read(sc, length);
                bean.setData(buffer);
            }

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.error(e.getMessage());
            try {
                if (sc != null) {
                    sc.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return bean;
    }
}
