package me.keeganlee.kandroid.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import me.keeganlee.kandroid.betach.BaseBetach;
import me.keeganlee.kandroid.tools.ConvertUtil;
import me.keeganlee.kandroid.tools.LogUtil;


public class BetachClient {
    // 信道选择器
    private Selector selector;
    // 与服务器通信的信道
    SocketChannel socketChannel;

    // 要连接的服务器Ip地址
    private String hostIp;

    // 要连接的远程服务器在监听的端口
    private int hostListenningPort;

    public BetachClient(String HostIp, int HostListenningPort) throws IOException {
        this.hostIp = HostIp;
        this.hostListenningPort = HostListenningPort;

        initialize();
    }

    /**
     * 初始化
     *
     * @throws IOException
     */
    private void initialize() throws IOException {
        // 打开监听信道并设置为非阻塞模式
        socketChannel = SocketChannel.open(new InetSocketAddress(hostIp, hostListenningPort));
        socketChannel.configureBlocking(false);

        // 打开并注册选择器到信道
        selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_READ);

        // 启动读取线程
        new BetachReadThread(selector);
    }

    /**
     * 发送字符串到服务器
     *
     * @param writeBuffer
     * @throws IOException
     */
    public void sendMsg(ByteBuffer writeBuffer) throws IOException {
        if (socketChannel != null) {
            while (writeBuffer.hasRemaining()) {
                socketChannel.write(writeBuffer);
            }
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

            if (socketChannel != null) {
                socketChannel.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    class BetachReadThread1 implements Runnable {
        private Selector selector;

        public BetachReadThread1(Selector selector) {
            this.selector = selector;

            new Thread(this).start();
        }

        @SuppressWarnings("static-access")
        public void run() {
            try {
                while (selector.select() > 0) {//select()方法只能使用一次，用了之后就会自动删除,每个连接到服务器的选择器都是独立的
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
                                dataBuff = read(sc, length);
                            }

                            if (count < BaseBetach.SIZE) {
                                // 已经读完

                            } else if (count == BaseBetach.SIZE) {
                                // 如果count等于缓冲区大小，则说明此条消息没读完//TODO

                            } else if (count == -1) {
                                // 当客户端断开连接时会触发read事件，返回值-1，关闭通道
                            }
                            close();

                            switch (request) {
                                case 0:
                                    LogUtil.debug("pc正确接收到信息");
                                    String jsonStr1 = ConvertUtil.byteTOString(dataBuff.array());
                                    LogUtil.debug(jsonStr1);
                                    break;
                                default:
                                    String jsonStr = ConvertUtil.byteTOString(dataBuff.array());
                                    LogUtil.debug(jsonStr);
                                    LogUtil.debug("pc非正确接收到信息,request:" + request);
                                    break;
                            }


                            // 为下一次读取作准备
                            sk.interestOps(SelectionKey.OP_READ);
                        }
                        // 删除正在处理的SelectionKey
                        selector.selectedKeys().remove(sk);
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
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

}

