package me.keeganlee.kandroid.service;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import me.keeganlee.kandroid.tools.LogUtil;


public class LocalService extends KBaseService {
    private final IBinder myBinder =  new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        LogUtil.debug("onBind()");
        //Toast.makeText(this, "onBind()", Toast.LENGTH_SHORT).show();
        return myBinder;
    }


    // 调用startService方法或者bindService方法时创建Service时（当前Service未创建）调用该方法
    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.debug("onCreate()");
        //Toast.makeText(this, "onCreate()", Toast.LENGTH_SHORT).show();
    }


    // 调用startService方法启动Service时调用该方法
    @Override
    public void onStart(Intent intent, int startId) {
        LogUtil.debug("onStart()");
        LogUtil.debug(getTransferData().toString());
    }


    // Service创建并启动后在调用stopService方法或unbindService方法时调用该方法
    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.debug("onDestroy()");
        //Toast.makeText(this, "onDestroy()", Toast.LENGTH_SHORT).show();
    }
    //提供给客户端访问
    public class LocalBinder extends Binder {
        public LocalService getService() {
            return LocalService.this;
        }
    }
}
