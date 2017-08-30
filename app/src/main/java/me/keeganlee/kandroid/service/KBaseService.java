package me.keeganlee.kandroid.service;


import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;

import me.keeganlee.kandroid.KApplication;
import me.keeganlee.kandroid.R;
import me.keeganlee.kandroid.activity.KBaseActivity;
import me.keeganlee.kandroid.bean.ActivityTransfer;
import me.keeganlee.kandroid.core.AppAction;
import me.keeganlee.kandroid.exception.KAndroidException;
import me.keeganlee.kandroid.tools.CommonUtils;
import me.keeganlee.kandroid.tools.LogUtil;

/**
 * Activity抽象基类
 *
 * @version 1.0 创建时间：15/6/26
 */
public abstract class KBaseService extends Service {
    // 上下文实例
    public Context context;
    // 应用全局的实例
    public KApplication application;
    // 核心层的Action实例
    public AppAction appAction;
    private List<ActivityTransfer>  mActivityList;
    private Intent mIntent;
    @Override
    public void onCreate() {
       super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mIntent = intent;
        context = getApplicationContext();
        application = (KApplication) this.getApplication();
        // 校验
        int c = mIntent.getIntExtra(KBaseActivity.TRANSFER_CODE_KEY, 0);
        if (!application.checkFromCls(getClass().getName(),c)) {
            throw new KAndroidException("the service entry is not valide");
        }
        return super.onStartCommand(intent, flags, startId);
    }




    /**
     * get data when jump to another activity
     **/
    protected Object getTransferData() {
        return mIntent.getSerializableExtra(KBaseActivity.TRANSFER_KEY);
    }

}
