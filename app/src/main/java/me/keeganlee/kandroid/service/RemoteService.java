package me.keeganlee.kandroid.service;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import me.keeganlee.kandroid.IKBaseAidlInterface;
import me.keeganlee.kandroid.socket.BetachServer;
import me.keeganlee.kandroid.socket.Server;
import me.keeganlee.kandroid.tools.LogUtil;


public class RemoteService extends KBaseService {
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // 调用startService方法或者bindService方法时创建Service时（当前Service未创建）调用该方法
    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.debug("onCreate()");
        //Toast.makeText(this, "onCreate()", Toast.LENGTH_SHORT).show();
       // new Server().start();
        new BetachServer(this);
    }


    // 调用startService方法启动Service时调用该方法
    @Override
    public void onStart(Intent intent, int startId) {
        LogUtil.debug("onStart()");
    }


    // Service创建并启动后在调用stopService方法或unbindService方法时调用该方法
    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.debug("onDestroy()");
        //Toast.makeText(this, "onDestroy()", Toast.LENGTH_SHORT).show();
    }

    IKBaseAidlInterface.Stub mBinder = new IKBaseAidlInterface.Stub() {

        @Override
        public String toUpperCase(String str) throws RemoteException {
            if (str != null) {
                return str.toUpperCase();
            }
            return null;
        }

        @Override
        public int plus(int a, int b) throws RemoteException {
            return a + b;
        }
    };
}
