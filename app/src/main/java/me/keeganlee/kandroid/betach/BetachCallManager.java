package me.keeganlee.kandroid.betach;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import me.keeganlee.kandroid.tools.ConvertUtil;
import me.keeganlee.kandroid.tools.LogUtil;

public class BetachCallManager extends BaseBetach{
    private Context context;


    public BetachCallManager(Context context) {
        this.context = context;
    }

/*	// 删除通话记录
	private void DeleteCall() {
		context.getContentResolver().delete(CallLog.Calls.CONTENT_URI,
				CallLog.Calls.NUMBER + "=?", new String[] { "13078943473" });
	}

	// 修改通话记录

	private void ModifyCall()

	{

		ContentValues content = new ContentValues();

		content.put(CallLog.Calls.TYPE, CallLog.Calls.INCOMING_TYPE);

		content.put(CallLog.Calls.NUMBER, "13078945773");

		content.put(CallLog.Calls.DATE, 123123123);

		content.put(CallLog.Calls.NEW, "1");// 0已看1未看

		context.getContentResolver().update(CallLog.Calls.CONTENT_URI, content,
				CallLog.Calls.NUMBER + "=?", new String[] { "13078943473" });

	}*/

    // 添加通话记录
    public void addCall(ByteBuffer dataBuff) throws IOException {
        dataBuff.rewind();
        if(dataBuff != null){
            byte[] dst = new byte[0];
            byte[] content = new byte[0];
            int length = 0;
            //id
            length = 4;
            dst = new byte[length];
            dataBuff.get(dst, 0, length);
            int _id = ConvertUtil.byteArrayToint(dst);

            //cachedName
            length = 4;
            dst = new byte[length];
            dataBuff.get(dst, 0, length);
            int cachedNameLength =  ConvertUtil.byteArrayToint(dst); //Length

            length = cachedNameLength;
            content = new byte[length];
            dataBuff.get(content, 0,length);
            String cachedName = ConvertUtil.byteTOString(content); //name

            //cachedLabel
            length = 4;
            dst = new byte[length];
            dataBuff.get(dst, 0, length);
            int cachedLabelLength =  ConvertUtil.byteArrayToint(dst);//length

            length = cachedLabelLength;
            content = new byte[length];
            dataBuff.get(content, 0,length);
            String cachedLabel = ConvertUtil.byteTOString(content);//label

            //cachedType
            length = 1;
            dst = new byte[length];
            dataBuff.get(dst, 0, length);
            int cachedType =  ConvertUtil.byteArrayToint(dst);

            //phonenum
            length = 4;
            dst = new byte[length];
            dataBuff.get(dst, 0, length);
            int phoneLength =  ConvertUtil.byteArrayToint(dst);//length

            length = phoneLength;
            content = new byte[length];
            dataBuff.get(content, 0,length);
            String phoneNum = ConvertUtil.byteTOString(content);//phoneNum

            //type
            length = 1;
            dst = new byte[length];
            dataBuff.get(dst, 0, length);
            int type =  ConvertUtil.byteArrayToint(dst);

            //date
            length = 4;
            dst = new byte[length];
            dataBuff.get(dst, 0, length);
            int dateLength =  ConvertUtil.byteArrayToint(dst);//length

            length = dateLength;
            content = new byte[length];
            dataBuff.get(content, 0,length);
            String date = ConvertUtil.byteTOString(content);//date

            //new
            length = 1;
            dst = new byte[length];
            dataBuff.get(dst, 0, length);
            int isNew =  ConvertUtil.byteArrayToint(dst);

            //duration
            length = 4;
            dst = new byte[length];
            dataBuff.get(dst, 0, length);
            int duration =  ConvertUtil.byteArrayToint(dst);

            Cursor cur = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, CallLog.Calls._ID + " = ?", new String[]{_id +""}, null);
            if(cur != null && !cur.moveToFirst()){
                ContentValues contentValues = new ContentValues();
                contentValues.put(CallLog.Calls._ID, _id);
                contentValues.put(CallLog.Calls.CACHED_NAME, cachedName);
                contentValues.put(CallLog.Calls.CACHED_NUMBER_LABEL, cachedLabel);
                contentValues.put(CallLog.Calls.CACHED_NUMBER_TYPE, cachedType);
                contentValues.put(CallLog.Calls.NUMBER, phoneNum);
                contentValues.put(CallLog.Calls.TYPE, type);
                contentValues.put(CallLog.Calls.DATE, date);
                contentValues.put(CallLog.Calls.NEW, isNew);
                contentValues.put(CallLog.Calls.DURATION, duration);
                context.getContentResolver().insert(CallLog.Calls.CONTENT_URI, contentValues);
            }

            if(cur != null){
                cur.close();
            }
        }
    }

    // 查询通话记录

    public void getCall(SocketChannel sc, int response) throws JSONException, IOException{

        Cursor cur = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                null, null, null, CallLog.Calls.DEFAULT_SORT_ORDER);

        if (!cur.moveToFirst()){
            Log.i("通话记录", "目前没有通话记录");
            return ;
        }
        List list = new ArrayList();
        do{
            int idIndex = cur.getColumnIndex(CallLog.Calls._ID);
            int cachedNameIndex =  cur.getColumnIndex(CallLog.Calls.CACHED_NAME);
            int cachedLabelIndex =  cur.getColumnIndex(CallLog.Calls.CACHED_NUMBER_LABEL);
            int cachedTypeIndex =  cur.getColumnIndex(CallLog.Calls.CACHED_NUMBER_TYPE);
            int numIndex = cur.getColumnIndex(CallLog.Calls.NUMBER);
            int typeIndex = cur.getColumnIndex(CallLog.Calls.TYPE);
            int dateIndex = cur.getColumnIndex(CallLog.Calls.DATE);
            int newIndex =  cur.getColumnIndex(CallLog.Calls.NEW);
            int durationIndex = cur.getColumnIndex(CallLog.Calls.DURATION);

            int _id = cur.getInt(idIndex);
            String cachedName = cur.getString(cachedNameIndex);
            String cachedLabel = cur.getString(cachedLabelIndex);
            int cachedType = cur.getInt(cachedTypeIndex);
            String phoneNum = cur.getString(numIndex);
            int type = cur.getInt(typeIndex);
            long date = cur.getLong(dateIndex);
            int isNew = cur.getInt(newIndex);
            int duration = cur.getInt(durationIndex);


            list.clear();
            list.add(_id);//通话记录id
            list.add(cachedName);
            list.add(cachedLabel);
            list.add((byte)cachedType);
            list.add(phoneNum);//电话号码
            list.add((byte)type);//来电类型   1：来电   2拨出  3未接
            list.add(date + "");//通话记录时间
            list.add((byte)isNew);//0  已读   1未读
            list.add(duration);//通话时长

            this.sendFileDetail(sc, response, list);

            LogUtil.debug("获取到通讯录数据 ： " + "_id:" + _id+ "cachedName:" + cachedName+ "cachedLabel:" + cachedLabel+ "cachedType:" + cachedType+ "phoneNum:" + phoneNum+ "type:" + type+ "date:"+ date + "isNew:"+ isNew + "duration:"+ duration);
        } while (cur.moveToNext());

        list.clear();
        list = null;
        cur.close();

    }


}
