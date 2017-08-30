package me.keeganlee.kandroid.activity;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import me.keeganlee.kandroid.R;
import me.keeganlee.kandroid.bean.TestBean;
import me.keeganlee.kandroid.service.LocalService;


public class ServiceMainActivity extends KBaseActivity {
    private Button startBtn;
    private Button stopBtn;
    private Button bindBtn;
    private Button unBindBtn;
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
        startBtn.setOnClickListener(new MyOnClickListener());
        stopBtn.setOnClickListener(new MyOnClickListener());
        bindBtn.setOnClickListener(new MyOnClickListener());
        unBindBtn.setOnClickListener(new MyOnClickListener());
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
                    stopService(intent);
                    toast("stopService");
                    break;
                case R.id.bind:
                    // 绑定Service
                    bindService(intent, conn, Service.BIND_AUTO_CREATE);
                    toast("bindService");
                    break;
                case R.id.unbind:
                    // 解除Service
                    unbindService(conn);
                    toast("unbindService");
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
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e(TAG, "连接成功");
            // 当Service连接建立成功后，提供给客户端与Service交互的对象（根据Android Doc翻译的，不知道准确否。。。。）
            myService = ((LocalService.LocalBinder) service).getService();
        }


        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "断开连接");
            myService = null;
        }
    };
}
