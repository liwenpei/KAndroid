package me.keeganlee.kandroid.api.dao;

import android.content.Context;

/**
 * Created by Administrator on 2016/12/5 0005.
 */

public class TestDBHelper extends DataBaseHelper {

    private static TestDBHelper mTestDBHelper;

    private TestDBHelper(Context context){
        super(context);
    }

    public static TestDBHelper getInstance(Context context){
        if (mTestDBHelper==null){
            synchronized (DataBaseHelper.class){
                if (mTestDBHelper==null){
                    mTestDBHelper = new TestDBHelper(context);
                    if (mTestDBHelper.getDB()==null||!mTestDBHelper.getDB().isOpen()){
                        mTestDBHelper.open();
                    }
                }
            }
        }
        return mTestDBHelper;
    }

    @Override
    protected int getMDbVersion(Context context) {
        return 1;
    }

    @Override
    protected String getDbName(Context context) {
        return "test.db";
    }

    @Override
    protected String[] getDbCreateSql(Context context) {
        String[] a = new String[1];
        a[0] = "CREATE TABLE user (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,gender TEXT,age INTEGER)";
        return a;
    }

    @Override
    protected String[] getDbUpdateSql(Context context) {
        return new String[0];
    }
}
