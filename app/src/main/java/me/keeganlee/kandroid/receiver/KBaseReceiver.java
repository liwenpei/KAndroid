package me.keeganlee.kandroid.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.List;

import me.keeganlee.kandroid.KApplication;
import me.keeganlee.kandroid.activity.KBaseActivity;
import me.keeganlee.kandroid.bean.ActivityTransfer;
import me.keeganlee.kandroid.exception.KAndroidException;

/**
 * Created by Administrator on 2017/9/5.
 */

public class KBaseReceiver extends BroadcastReceiver {
    // 应用全局的实例
    private KApplication application;
    private List<ActivityTransfer> mActivityList;
    private Intent mIntent;
    private Context mContext;
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        mIntent = intent;
        checkFrom();
    }

    private void checkFrom(){
        application = (KApplication) mContext.getApplicationContext();
        // 校验
        int c = mIntent.getIntExtra(KBaseActivity.TRANSFER_CODE_KEY, 0);
        if (!application.checkFromCls(getClass().getName(),c)) {
            throw new KAndroidException("the receiver entry is not valide");
        }
    }
}
