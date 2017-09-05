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
package me.keeganlee.kandroid;

import android.app.Application;
import android.content.Context;

import com.squareup.leakcanary.LeakCanary;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import me.keeganlee.kandroid.bean.ActivityTransfer;
import me.keeganlee.kandroid.core.AppAction;
import me.keeganlee.kandroid.core.AppActionImpl;
import me.keeganlee.kandroid.tools.JackonUtil;
import me.keeganlee.kandroid.tools.LogUtil;

/**
 * Application类，应用级别的操作都放这里
 *
 * @version 1.0 创建时间：15/6/25
 */
public class KApplication extends Application {

    private AppAction appAction;
    private List<ActivityTransfer> mActivityList;
    private  final String TAG = this.getClass().getName();
    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
        appAction = new AppActionImpl(this);
    }

    public AppAction getAppAction() {
        return appAction;
    }

    public List<ActivityTransfer> getActivityList(){
        if(mActivityList != null){
            return mActivityList;
        }
        LogUtil.debug("start to init activity list config");
        String content = null;
        try {
            InputStream is = getAssets().open("activity_transfer.json");
            content = readDataFromInputStream(is);
            content = content.replace("\r\n", "");
            content = content.replace("\n", "");
            int index = content.indexOf("<!--");
            while (index > 0) {
                String tmpStr = content.substring(index, content.indexOf("-->") + "-->".length());
                content = content.replace(tmpStr, "");
                index = content.indexOf("<!--");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        mActivityList = JackonUtil.readListValue(content, ActivityTransfer.class);
        if(mActivityList == null){
            LogUtil.error("init activity list config was wrong");
        }else{
            LogUtil.debug("init activity list config sucess");
        }
        return mActivityList;
    }

    public String readDataFromInputStream(InputStream is) {
        BufferedInputStream bis = new BufferedInputStream(is);
        String str = "", s = "";

        int c = 0;
        byte[] buf = new byte[64];
        while (true) {
            try {
                c = bis.read(buf);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (c == -1)
                break;
            else {
                try {
                    s = new String(buf, 0, c, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                str += s;
            }
        }

        try {
            bis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return str;
    }

    /**
     * check the from class code
     *
     * @param c entry code
     */
    public boolean checkFromCls(String clsName, int c) {
        for (ActivityTransfer bean : getActivityList()) {
            if (c == 0 && clsName.equals(bean.getFrom())
                    && clsName.equals(bean.getTo())) {
                // 入口
                return true;
            }
            if (clsName.equals(bean.getTo()) && c == Integer.parseInt(bean.getId())) {
                return true;
            }
        }
        return false;
    }
}
