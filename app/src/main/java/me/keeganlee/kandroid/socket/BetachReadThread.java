package me.keeganlee.kandroid.socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import me.keeganlee.kandroid.betach.BaseBetach;
import me.keeganlee.kandroid.tools.ConvertUtil;
import me.keeganlee.kandroid.tools.LogUtil;

public class BetachReadThread implements Runnable {
    private Selector selector;

    public BetachReadThread(Selector selector) {
        this.selector = selector;

        new Thread(this).start();
    }

    @SuppressWarnings("static-access")
    public void run() {
        try {
            while (selector.isOpen() && selector.select() > 0) {//select()方法只能使用一次，用了之后就会自动删除,每个连接到服务器的选择器都是独立的
                // 遍历每个有可用IO操作Channel对应的SelectionKey
                for (SelectionKey sk : selector.selectedKeys()) {
                    // 如果该SelectionKey对应的Channel中有可读的数据
                    if (sk.isReadable()) {
                        // 使用NIO读取Channel中的数据
                        SocketChannel sc = (SocketChannel) sk.channel();//获取通道信息
                        ByteBuffer buffer = ByteBuffer.allocate(8);//分配缓冲区大小
                        //接收到服务端的信息

                        int count = sc.read(buffer);
                        System.out.println("Read byte:" + count);

                        // 解析头部
                        int request = buffer.get(0) & 0xff; // 0
                        byte[] dst = {buffer.get(1), buffer.get(2), buffer.get(3),
                                buffer.get(4)};
                        int length = ConvertUtil.byteArrayToint(dst); // 1-4
                        int type = buffer.get(5) & 0xff; // 5

                        byte[] dstReserve = {buffer.get(6), buffer.get(7)};
                        int bUsReserve = ConvertUtil.byteArrayToint(dstReserve); // 6-7

                        ByteBuffer dataBuff = null;

                        if (length > 0) {
                            dataBuff = SocketUtil.read(sc, length);
                        }

                        if (count < BaseBetach.SIZE) {
                            // 已经读完

                        } else if (count == BaseBetach.SIZE) {
                            // 如果count等于缓冲区大小，则说明此条消息没读完//TODO

                        } else if (count == -1) {
                            // 当客户端断开连接时会触发read事件，返回值-1，关闭通道
                        }
                        //close();

                        switch (request) {
                            case BaseBetach.I4ANDROID_E_CMD_PHONE_LINK_PC_WIFI_RESP:
                                LogUtil.debug("pc正确接收到信息");
                                String jsonStr1 = ConvertUtil.byteTOString(dataBuff.array());
                                LogUtil.debug(jsonStr1);
                                break;
                            default:
                                String jsonStr = ConvertUtil.byteTOString(dataBuff.array());
                                LogUtil.debug("pc非正确接收到信息,request:" + request);
                                LogUtil.debug("json内容："+jsonStr);
                                break;
                        }


                        // 为下一次读取作准备
                        if (sk.isValid()) {
                            System.out.println("这是最新的..................");
                            sk.interestOps(SelectionKey.OP_READ);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 关闭请求通道
     **/
    public void close() {
        try {

            if (selector != null) {
                selector.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
