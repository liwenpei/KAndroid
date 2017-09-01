package me.keeganlee.kandroid.activity;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import me.keeganlee.kandroid.R;
import me.keeganlee.kandroid.bean.TestBean;
import me.keeganlee.kandroid.kinterface.KBaseServiceConnection;
import me.keeganlee.kandroid.service.LocalService;
import me.keeganlee.kandroid.tools.LogUtil;


public class ServiceMainActivity extends KBaseActivity {
    private Button startBtn;
    private Button stopBtn;
    private Button bindBtn;
    private Button unBindBtn;
    private Button startRemoteServiceBtn;
    private static final String TAG = "MainActivity";
    private LocalService myService;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void initView() {
        setContentView(R.layout.service);
        startBtn = (Button) findViewById(R.id.start);
        stopBtn = (Button) findViewById(R.id.stop);
        bindBtn = (Button) findViewById(R.id.bind);
        unBindBtn = (Button) findViewById(R.id.unbind);
        startRemoteServiceBtn = (Button) findViewById(R.id.startRemoteService);
        startBtn.setOnClickListener(new MyOnClickListener());
        stopBtn.setOnClickListener(new MyOnClickListener());
        bindBtn.setOnClickListener(new MyOnClickListener());
        unBindBtn.setOnClickListener(new MyOnClickListener());
        startRemoteServiceBtn.setOnClickListener(new MyOnClickListener());
    }

    @Override
    protected void initData() {

    }


    class MyOnClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {


            Intent intent = new Intent();
            intent.setClass(ServiceMainActivity.this, LocalService.class);
            switch (v.getId()) {
                case R.id.start:
                    // 启动Service
                    TestBean testBean = new TestBean();
                    testBean.setAge("111ddd");
                    gotoService(10004,testBean);
                    toast("startService");
                    break;
                case R.id.stop:
                    // 停止Service
                    LogUtil.debug(((TestBean)getLocalService().getTransferData()).getAge());
                    gotoService(10005,null);
                    //stopService(intent);
                    toast("stopService");
                    break;
                case R.id.bind:
                    // 绑定Service
                    TestBean testBean1 = new TestBean();
                    testBean1.setAge("111ddd");
                    gotoService(10004,testBean1);
                    gotoService(10006,testBean1, BIND_NOT_FOREGROUND);
                    //bindService(intent, conn, Service.BIND_AUTO_CREATE);
                    toast("bindService");
                    break;
                case R.id.unbind:
                    // 解除Service
                    gotoService(10007,null);
                    //unbindService(conn);
                    toast("unbindService");
                    break;
                case R.id.startRemoteService:
                    gotoService(10008,null,Service.BIND_AUTO_CREATE);
                    toast("startRemoteService");
                    new Thread(){
                        @Override
                        public void run(){
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            try {
                                int result = getRemoteService().plus(3, 5);
                                String upperStr = getRemoteService().toUpperCase("hello world");
                                LogUtil.debug("result is " + result);
                                LogUtil.debug("upperStr is " + upperStr);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                    break;
            }
        }
    }


    private void toast(final String tip){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), tip, Toast.LENGTH_SHORT).show();
            }
        });
    }
    public KBaseServiceConnection conn = new KBaseServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogUtil.debug("连接成功");
            // 当Service连接建立成功后，提供给客户端与Service交互的对象（根据Android Doc翻译的，不知道准确否。。。。）
            myService = ((LocalService.LocalBinder) service).getService();
        }


        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogUtil.debug("断开连接");
            myService = null;
        }
    };
}
