package me.keeganlee.kandroid.global;

import android.util.Log;

/**
 * Created by Administrator on 2017/7/10 0010.
 */

public class LogUtil {
    private final static String TAG  = "me.keeganlee.kandroid";
    private final static String SECONDETAG  = "main";

    public static void debug(String o){
        Log.d(TAG+"_"+SECONDETAG,o);
    }

    public static void debug(String tag,String o){
        Log.d(TAG+"_"+tag,o);
    }

    public static void error(String o){
        Log.d(TAG+"_"+SECONDETAG,o);
    }

    public static void error(String tag,String o){
        Log.d(TAG+"_"+tag,o);
    }

}
