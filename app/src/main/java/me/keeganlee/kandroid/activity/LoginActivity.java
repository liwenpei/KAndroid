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

import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import me.keeganlee.kandroid.R;
import me.keeganlee.kandroid.bean.TestBean;
import me.keeganlee.kandroid.core.ActionCallbackListener;
import me.keeganlee.kandroid.tools.LogUtil;
import me.keeganlee.kandroid.widget.CustomDialog;

/**
 * 登录
 *
 * @version 1.0 创建时间：15/6/26
 */
public class LoginActivity extends KBaseActivity {

    private EditText mPhoneEdit;
    private EditText mPasswordEdit;
    private Button mLoginBtn;

    @Override
    protected void initView() {
        setContentView(R.layout.activity_login);
        // 初始化View
        initViews();
    }

    @Override
    protected void initData() {
    }

    // 初始化View
    private void initViews() {
        mPhoneEdit = (EditText) findViewById(R.id.edit_phone);
        mPasswordEdit = (EditText) findViewById(R.id.edit_password);
        mLoginBtn = (Button) findViewById(R.id.btn_login);
        //setActionBar("测试头部111");
    }

    // 准备登录
    public void toLogin(View view) {
        String loginName = mPhoneEdit.getText().toString();
        String password = mPasswordEdit.getText().toString();
        mLoginBtn.setEnabled(false);
        this.appAction.login(loginName, password, new ActionCallbackListener<Void>() {
            @Override
            public void onSuccess(Void data) {
                Toast.makeText(context, R.string.toast_login_success, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(context, CouponListActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorEvent, String message) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                mLoginBtn.setEnabled(true);
            }
        });
    }



    // 进入注册页
    public void toRegister(View view) {
      /*  CustomDialog.Builder builder = new CustomDialog.Builder(this);
        builder.setMessage("这个就是自定义的提示框");
        builder.setTitle("提示");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                //设置你的操作事项
            }
        });

        builder.setNegativeButton("取消",
                new android.content.DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
        if(1==1){return;}*/
        LogUtil.debug("正在点击");
        TestBean bean = new TestBean();
        bean.setAge("100");
        bean.setName("lwp");
        gotoActivity(10002, bean);
    }

}
