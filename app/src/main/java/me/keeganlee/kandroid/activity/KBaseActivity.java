/**
 * Copyright (C) 2015. Keegan小钢（http://keeganlee.me）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.keeganlee.kandroid.activity;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;

import me.keeganlee.kandroid.IKBaseAidlInterface;
import me.keeganlee.kandroid.KApplication;
import me.keeganlee.kandroid.R;
import me.keeganlee.kandroid.bean.ActivityTransfer;
import me.keeganlee.kandroid.core.AppAction;
import me.keeganlee.kandroid.exception.KAndroidException;
import me.keeganlee.kandroid.impl.KBaseServiceConnectionImpl;
import me.keeganlee.kandroid.service.LocalService;
import me.keeganlee.kandroid.tools.CommonUtils;
import me.keeganlee.kandroid.tools.LogUtil;

/**
 * Activity抽象基类
 *
 * @version 1.0 创建时间：15/6/26
 */
public abstract class KBaseActivity extends FragmentActivity {
    public static final String TRANSFER_KEY = "service_transfer_key";
    public static final String TRANSFER_CODE_KEY = "service_transfer_code_key";
    // 上下文实例
    public Context context;
    // 应用全局的实例
    public KApplication application;
    // 核心层的Action实例
    public AppAction appAction;
    private List<ActivityTransfer> mActivityList;
    private static AtomicBoolean mIsRunEnd = new AtomicBoolean(true);
    private KBaseServiceConnectionImpl mKBaseServiceConnection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        application = (KApplication) this.getApplication();
        appAction = application.getAppAction();
        // 获取acitivity配置文件
        mActivityList = application.getActivityList();
        // 校验
        int c = getIntent().getIntExtra(TRANSFER_CODE_KEY, 0);
        if (!application.checkFromCls(getClass().getName(),c)) {
            throw new KAndroidException("the activity entry is not valide");
        }
        initView();
        initData();
    }

    /**
     * goto to activity when you configurationed
     * 
     * @param id
     *            code
     * @param o
     *            object
     **/
    public void gotoActivity(int id, Object o) {
        boolean isFind = false;
        try {
            for (ActivityTransfer transfer : mActivityList) {
                if (id == Integer.parseInt(transfer.getId())
                        && getClass().getName().equals(transfer.getFrom())) {
                    if ("true".equals(transfer.getCheckRunEnd())) {
                        if (!mIsRunEnd.get()) {
                            // 检查acitivity是否执行完毕
                            return;
                        }
                        mIsRunEnd.set(false);
                    }
                    Class c = Class.forName(transfer.getTo());
                    Intent intent = new Intent(this, c);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(TRANSFER_KEY, (Serializable) o);
                    intent.putExtras(bundle);
                    intent.putExtra(TRANSFER_CODE_KEY, id);
                    if ("startActivity".equals(transfer.getMethod())) {
                        startActivity(intent);
                    } else {
                        startActivityForResult(intent, Integer.parseInt(transfer.getRequestCode()));
                    }
                    if ("true".equals(transfer.getFinish())) {
                        finish();
                    }
                    isFind = true;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new KAndroidException(e.getMessage());
        }
        if (!isFind) {
            throw new KAndroidException("can not find your activity ,please check your config");
        } else {
            LogUtil.debug("goto activity sucessfull");
        }
    }

    public void gotoService(int id, Object o){
        gotoService(id,o,0);
    }


    protected LocalService getLocalService(){
        if(mKBaseServiceConnection != null){
            return mKBaseServiceConnection.getMyService();
        }
        return null;
    }

    protected IKBaseAidlInterface getRemoteService(){
        if(mKBaseServiceConnection != null){
            return mKBaseServiceConnection.getRemoteService();
        }
        return null;
    }

    /**
     * goto to service when you configurationed
     *
     * @param id code
     * @param o  object
     **/
    public void gotoService(int id, Object o,   int flags) {
        boolean isFind = false;
        try {
            for (ActivityTransfer transfer : mActivityList) {
                if (id == Integer.parseInt(transfer.getId())
                        && getClass().getName().equals(transfer.getFrom())) {
                    Class c = Class.forName(transfer.getTo());
                    Intent intent = new Intent(this, c);
                    Bundle bundle = new Bundle();
                    if(o != null){
                        bundle.putSerializable(KBaseActivity.TRANSFER_KEY, (Serializable) o);
                    }
                    intent.putExtras(bundle);
                    intent.putExtra(KBaseActivity.TRANSFER_CODE_KEY, id);
                    if(mKBaseServiceConnection == null && transfer.getConnectionCls() != null){
                        Class connectionCls = Class.forName(transfer.getConnectionCls());
                        mKBaseServiceConnection = (KBaseServiceConnectionImpl) connectionCls.newInstance();
                    }
                    if("startService".equals(transfer.getMethod())){
                        startService(intent);
                    }else if("stopService".equals(transfer.getMethod())){
                        stopService(intent);
                    }else if("bindService".equals(transfer.getMethod())){
                        bindService(intent,mKBaseServiceConnection,flags);
                    }else if("unbindService".equals(transfer.getMethod())){
                        unbindService(mKBaseServiceConnection);
                    }else{
                        LogUtil.debug("your method is wrong");
                    }
                    isFind = true;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new KAndroidException(e.getMessage());
        }
        if (!isFind) {
            throw new KAndroidException("can not find your service ,please check your config");
        } else {
            LogUtil.debug("goto service sucessfull");
        }
    }

    /**
     * get data when jump to another activity
     **/
    protected Object getTransferData() {
        return getIntent().getSerializableExtra(TRANSFER_KEY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsRunEnd.set(true);
    }

    /**
     * get data when come back the activity
     **/
    protected void setResult(int resultCode, Object o) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(TRANSFER_KEY, (Serializable) o);
        Intent intent = getIntent().putExtras(bundle);
        setResult(resultCode, intent);
    }

    protected abstract void initView();

    protected abstract void initData();

    protected void setActionBar(String title) {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setCustomView(R.layout.action_bar);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            actionBar.setElevation(CommonUtils.parsePxByDp(this, 2));
        }
        TextView txt = (TextView) actionBar.getCustomView().findViewById(R.id.title);
        txt.setText(title);
    }
}
