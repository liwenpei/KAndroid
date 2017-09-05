package me.keeganlee.kandroid.socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import me.keeganlee.kandroid.tools.LogUtil;

public class Server extends Thread{
    private ServerSocket serverSocket = null;
    private int port = 8142;

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            LogUtil.debug("server builded.....");
            OutputStream out = null;
            InputStream in = null;
            while(true){
                Socket socket = serverSocket.accept();
                try{
                    in = socket.getInputStream();
                    out = socket.getOutputStream();
                    while(true){
                        String reciverData = readFromInputStream(in);
                        LogUtil.debug(reciverData);
                        reciverData = "已接收--》\n" + reciverData;
                        writeToClient(out, reciverData);
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }finally{
                    in.close();
                    out.close();
                }
//				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//				PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
//				String a = null;
//				String b= null;
//				while((a = in.readLine()) != null){
//					b += a;
//					LogUtil.debug(a);
//					out.println("已接收。。。。。" + a);
//					out.flush();
//				}

            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally{
            try {
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            };
        }
    }

    public String readFromInputStream(InputStream in){
        int count = 0;
        byte[] inDatas = null;
        try{
            while(count == 0){
                count = in.available();
            }
            inDatas = new byte[count];
            in.read(inDatas);
            return new String(inDatas);
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public void writeToClient(OutputStream out, String data){
        try{
            out.write(data.getBytes());
            out.flush();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
