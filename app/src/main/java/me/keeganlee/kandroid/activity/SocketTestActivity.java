package me.keeganlee.kandroid.activity;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Service;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import me.keeganlee.kandroid.R;
import me.keeganlee.kandroid.socket.BetachClient;
import me.keeganlee.kandroid.socket.ClientSocket;
import me.keeganlee.kandroid.tools.CommonUtils;
import me.keeganlee.kandroid.tools.ConvertUtil;


public class SocketTestActivity extends KBaseActivity implements OnClickListener, ClientSocket.OnSocketRecieveCallBack {
    public static final String SERVER_NAME = "192.168.1.104";
    public static final int PORT = 8142;

    Button btn_send;
    EditText ed_send;
    ListView mListView;
    ArrayAdapter<String> arryAdapter = null;
    List<String> mDatas = null;
    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            mDatas.add((String) msg.obj);
            arryAdapter.notifyDataSetChanged();
        }

    };
    ClientSocket mClientSocket = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void initView() {
        setContentView(R.layout.activity_main);

        mDatas = new ArrayList<String>();
        mDatas.add("Hello world");
        arryAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mDatas);

        ed_send = (EditText) this.findViewById(R.id.et_send);
        btn_send = (Button) this.findViewById(R.id.btn_send);
        btn_send.setOnClickListener(this);
        mListView = (ListView) this.findViewById(R.id.list);
        mListView.setAdapter(arryAdapter);
        mClientSocket = new ClientSocket(SERVER_NAME, PORT);
        mClientSocket.setOnSocketRecieveCallBack(this);
        gotoService(10010, null, Service.BIND_AUTO_CREATE);
    }

    @Override
    protected void initData() {

    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        String msg = ed_send.getText().toString();

        if (msg != null && mClientSocket.isSocketConnected()) {
            mClientSocket.addSendMsgToQueue(msg);
            mDatas.add(msg);
            arryAdapter.notifyDataSetChanged();
            ed_send.setText("");
        } else {
            //mClientSocket.start();
            new Thread() {
                @Override
                public void run() {
                    try {

                        BetachClient client = new BetachClient("192.168.1.105", 12521);
                        client.sendMsg(ByteBuffer.wrap("我爱你".getBytes()));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    @Override
    public void OnRecieveFromServerMsg(String msg) {
        // TODO Auto-generated method stub
        if (msg != null) {
            Message message = Message.obtain();
            message.obj = msg;
            mHandler.sendMessage(message);
        }

    }
}
