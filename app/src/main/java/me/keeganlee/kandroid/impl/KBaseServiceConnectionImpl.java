package me.keeganlee.kandroid.impl;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import me.keeganlee.kandroid.IKBaseAidlInterface;
import me.keeganlee.kandroid.kinterface.KBaseServiceConnection;
import me.keeganlee.kandroid.service.LocalService;
import me.keeganlee.kandroid.tools.LogUtil;

/**
 * Created by Administrator on 2017/8/30.
 */

public class KBaseServiceConnectionImpl implements KBaseServiceConnection {
    private IKBaseAidlInterface mRemoteService;
    public IKBaseAidlInterface getRemoteService(){
        return mRemoteService;
    }
    private LocalService myService;
    public LocalService getMyService(){
        return myService;
    };
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        LogUtil.debug("连接成功111");
        if(service instanceof LocalService.LocalBinder){
            myService = ((LocalService.LocalBinder) service).getService();
        }else{
            mRemoteService = IKBaseAidlInterface.Stub.asInterface(service);
        }
    }

    /**
     * alled when a connection to the Service has been lost*/
    @Override
    public void onServiceDisconnected(ComponentName name) {
        LogUtil.debug("连接断开");
    }
}
