package me.keeganlee.kandroid.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by Administrator on 2017/9/5.
 */

public class KBaseReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
    }
}
