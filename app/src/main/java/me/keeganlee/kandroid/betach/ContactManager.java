package me.keeganlee.kandroid.betach;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.telephony.SmsManager;
import android.util.Log;

import me.keeganlee.kandroid.tools.CommonUtils;
import me.keeganlee.kandroid.tools.ConvertUtil;
import me.keeganlee.kandroid.tools.LogUtil;
import me.keeganlee.kandroid.tools.MD5AndSignature;


public class ContactManager extends BaseBetach {
    // private List<Contacts> list;
    private Context context;
    private int space = 1024;
    private ByteBuffer byteBuffer = ByteBuffer.allocate(space);
    /* 自定义ACTION常数，作为广播的Intent Filter识别常数 */
    private static String SMS_SEND_ACTIOIN = "SMS_SEND_ACTIOIN";
    // private static String SMS_DELIVERED_ACTION = "SMS_DELIVERED_ACTION";
    private static int requestCode = 0;

    public static final class SMS {
        public static final int ALL = 0;
        public static final int INBOX = 1;
        public static final int SENT = 2;
        public static final int DRAFT = 3;
        public static final int OUTBOX = 4;
        public static final int FAILED = 5;
        public static final int QUEUED = 6;
        public static final String SMS_URI_ALL = "content://sms/";
        public static final String SMS_URI_INBOX = "content://sms/inbox";
        public static final String SMS_URI_SEND = "content://sms/sent";
        public static final String SMS_URI_DRAFT = "content://sms/draft";
        public static final int status_inorge = -1;// 不考虑
        public static final int status_ok = 0;
        public static final int status_pending = 64;
        public static final int status_failed = 128;
        public static final int no_read = 0;// 未读
        public static final int has_read = 1;// 已读
        public static int smsCount = -1;
    }


    private static final int NOCHECK = -2;
    private static final int DEFAULT = -1;

    private static final int ADDSTATES = 0;
    private static final int UPDATESTATES = 1;
    private static final int DELETESTATES = 2;
    private static final int IMPORTSTATES = 3;

    public ContactManager(Context context) {
        this.context = context;
    }

    /*** 发送通讯录数据 **/
    public byte[] getResponseContacts(String where) {
        byte[] rntValue = null;

        try {
            // 请求手机短信、联系人、通话记录的信息
            String obj = getContactInfo(where);

            int length = obj.getBytes("UTF-8").length;
            ByteBuffer byteBuff = null;
            byteBuff = ByteBuffer.allocate(HEADER_LENGTH + length);
            // 返回头部位坐标0:响应命令
            byteBuff.put((byte) I4ANDROID_E_CMD_CONTACT_RESP);
            // 返回头部位坐标1-4:内容长度
            byteBuff.put(ConvertUtil.intToByteArray(length));
            // 返回头部位坐标5:内容类型
            byteBuff.put((byte) TYPE_JSON);
            // 返回头部位坐标6:拓展字节
            byteBuff.put((byte) 0);
            // 返回头部位坐标7:拓展字节
            byteBuff.put((byte) 0);
            // 返回数据部
            byteBuff.put(obj.toString().getBytes("UTF-8"));
            LogUtil.debug("输出字符" + ":" + obj.toString());

            rntValue = byteBuff.array();
        } catch (Exception ex) {
            // reponseErrData(socket,I4ANDROID_E_CMD_DATANUM_RESP,"发送sorket内容出错"
            // + ex.getMessage());
            LogUtil.debug("发送sorket内容出错" + ex.getMessage());
            ex.printStackTrace();
        }

        return rntValue;
    }

    /*** 发送通讯录分组数据 **/
    public byte[] getResponseContactGroup() {
        byte[] rntValue = null;

        try {
            // 获取组信息
            String obj = getGroup();

            int length = obj.getBytes("UTF-8").length;
            ByteBuffer byteBuff = null;
            byteBuff = ByteBuffer.allocate(HEADER_LENGTH + length);
            // 返回头部位坐标0:响应命令
            byteBuff.put((byte) I4ANDROID_E_CMD_CONTACT_GROUP_REQ);
            // 返回头部位坐标1-4:内容长度
            byteBuff.put(ConvertUtil.intToByteArray(length));
            // 返回头部位坐标5:内容类型
            byteBuff.put((byte) TYPE_JSON);
            // 返回头部位坐标6:拓展字节
            byteBuff.put((byte) 0);
            // 返回头部位坐标7:拓展字节
            byteBuff.put((byte) 0);
            // 返回数据部
            byteBuff.put(obj.toString().getBytes("UTF-8"));
            LogUtil.debug("输出字符" + ":" + obj.toString());

            rntValue = byteBuff.array();
        } catch (Exception ex) {
            LogUtil.debug("发送sorket内容出错" + ex.getMessage());
        }

        return rntValue;
    }

    /*** 发送通讯录账户数据 **/
    public byte[] getResponseContactAccount() {
        byte[] rntValue = null;

        try {
            // 获取组信息
            String obj = getAccounts();

            int length = obj.getBytes("UTF-8").length;
            ByteBuffer byteBuff = null;
            byteBuff = ByteBuffer.allocate(HEADER_LENGTH + length);
            // 返回头部位坐标0:响应命令
            byteBuff.put((byte) I4ANDROID_E_CMD_CONTACT_ACCOUNT_REQ);
            // 返回头部位坐标1-4:内容长度
            byteBuff.put(ConvertUtil.intToByteArray(length));
            // 返回头部位坐标5:内容类型
            byteBuff.put((byte) TYPE_JSON);
            // 返回头部位坐标6:拓展字节
            byteBuff.put((byte) 0);
            // 返回头部位坐标7:拓展字节
            byteBuff.put((byte) 0);
            // 返回数据部
            byteBuff.put(obj.toString().getBytes("UTF-8"));
            LogUtil.debug("输出字符" + ":" + obj.toString());

            rntValue = byteBuff.array();
        } catch (Exception ex) {
            LogUtil.debug("发送sorket内容出错" + ex.getMessage());
        }

        return rntValue;
    }

    /** 发送通讯录图片 */
    public byte[] getResponseContactPhoto(int id) {
        byte[] rntValue = null;

        try {
            // 请求手机短信、联系人、通话记录的个数
            byte[] photo = ConvertUtil.bitmapToBytes(CommonUtils.getContactPhoto(
                    context, id));

            int length = photo.length;
            ByteBuffer byteBuff = null;
            byteBuff = ByteBuffer.allocate(HEADER_LENGTH + length);
            // 返回头部位坐标0:响应命令
            byteBuff.put((byte) I4ANDROID_E_CMD_IMG_RESOURCE_EXPORT_RESP);
            // 返回头部位坐标1-4:内容长度
            byteBuff.put(ConvertUtil.intToByteArray(length));
            // 返回头部位坐标5:内容类型
            byteBuff.put((byte) TYPE_JPG);
            // 返回头部位坐标6:拓展字节
            byteBuff.put((byte) 0);
            // 返回头部位坐标7:拓展字节
            byteBuff.put((byte) 0);
            // 返回数据部
            byteBuff.put(photo);

            rntValue = byteBuff.array();
        } catch (Exception ex) {
            // reponseErrData(socket,I4ANDROID_E_CMD_DATANUM_RESP,"发送sorket内容出错"
            // + ex.getMessage());
            LogUtil.debug("发送sorket内容出错" + ex.getMessage());
        }

        return rntValue;
    }

    public int getContactNum(){
        int count = 0;
        // 获取手机联系人
        Cursor cursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getCount();
        }

        if(cursor != null){
            cursor.close();
        }

        return count;
    }

    /** 发送DataNum数据 **/
    public byte[] getReponseVersion() {
        byte[] rntValue = null;
        try {
            // 请求手机短信、联系人、通话记录的个数
            int versionCode = CommonUtils.getVersionCode(context,
                    context.getPackageName());
            MD5AndSignature md5 = new MD5AndSignature();
            String filePath = context.getPackageResourcePath();
            String sign = md5.getFileMD5(new File(filePath), MD5AndSignature.MD5);

            JSONObject obj = new JSONObject();
            JSONObject dataObj = new JSONObject();
            dataObj.put("md5", sign);
            dataObj.put("versionCode", versionCode);
            obj.put("return", 0);
            obj.put("reason", "");
            obj.put("data", dataObj);

            int length = obj.toString().getBytes("UTF8").length;

            ByteBuffer byteBuff = ByteBuffer.allocate(HEADER_LENGTH + length);
            // 返回头部位坐标0:响应命令
            byteBuff.put((byte) I4ANDROID_E_CMD_VERSION_RESP);
            // 返回头部位坐标1-4:内容长度
            byteBuff.put(ConvertUtil.intToByteArray(length));
            // 返回头部位坐标5:内容类型
            byteBuff.put((byte) 1);
            // 返回头部位坐标6:拓展字节
            byteBuff.put((byte) 0);
            // 返回头部位坐标7:拓展字节
            byteBuff.put((byte) 0);
            // 返回数据部
            byteBuff.put(obj.toString().getBytes("UTF8"));
            LogUtil.debug("输出字符" + ":" + obj.toString());

            rntValue = byteBuff.array();
        } catch (Exception ex) {
            // reponseErrData(socket,I4ANDROID_E_CMD_VERSION_RESP,"发送sorket内容出错"
            // + ex.getMessage());
            LogUtil.debug("发送sorket内容出错" + ex.getMessage());
        }

        return rntValue;
    }

    /*** 发送reponseVersion数据 **/
    public byte[] getReponseDataNum() {
        byte[] rntValue = null;
        try {
            // 请求手机短信、联系人、通话记录的个数
            int contactNum = getContactNum();//CommonUtils.getPhoneContactsCount(context) + CommonUtils.getSIMContactsCount(context);
            int messageNum = CommonUtils.getAllSmsCount(context);
            int recordNum = CommonUtils.readCallRecordCount(context);
            int apkNum = CommonUtils.getAppNum(context, false, false);
            JSONObject obj = new JSONObject();
            JSONObject dataObj = new JSONObject();
            dataObj.put("contactNum", contactNum);
            dataObj.put("messageNum", messageNum);
            dataObj.put("recordNum", recordNum);
            dataObj.put("apkNum", apkNum);
            obj.put("return", 0);
            obj.put("reason", "");
            obj.put("data", dataObj);

            int length = obj.toString().getBytes("UTF8").length;
            ByteBuffer byteBuff = null;
            byteBuff = ByteBuffer.allocate(HEADER_LENGTH + length);
            // 返回头部位坐标0:响应命令
            byteBuff.put((byte) I4ANDROID_E_CMD_DATANUM_RESP);
            // 返回头部位坐标1-4:内容长度
            byteBuff.put(ConvertUtil.intToByteArray(length));
            // 返回头部位坐标5:内容类型
            byteBuff.put((byte) 1);
            // 返回头部位坐标6:拓展字节
            byteBuff.put((byte) 0);
            // 返回头部位坐标7:拓展字节
            byteBuff.put((byte) 0);
            // 返回数据部
            byteBuff.put(obj.toString().getBytes("UTF8"));
            LogUtil.debug("输出字符" + ":" + obj.toString());

            rntValue = byteBuff.array();
        } catch (Exception ex) {
            // reponseErrData(socket,I4ANDROID_E_CMD_DATANUM_RESP,"发送sorket内容出错"
            // + ex.getMessage());
            LogUtil.debug("发送sorket内容出错" + ex.getMessage());
        }

        return rntValue;
    }


    private String[] getDefaultAccountNameAndType() {
        String[] rntValue = null;
        String accountType = "";
        String accountName = "";

        long rawContactId = 0;
        Uri rawContactUri = null;
        ContentProviderResult[] results = null;

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI).withValue(RawContacts.ACCOUNT_NAME, null).withValue(RawContacts.ACCOUNT_TYPE, null).build());

        try {
            results = context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            ops.clear();
        }

        for (ContentProviderResult result : results) {
            rawContactUri = result.uri;
            rawContactId = ContentUris.parseId(rawContactUri);
        }

        Cursor c = context.getContentResolver().query(
                RawContacts.CONTENT_URI
                , new String[] {RawContacts.ACCOUNT_TYPE, RawContacts.ACCOUNT_NAME}
                , RawContacts._ID+"=?"
                , new String[] {String.valueOf(rawContactId)}
                , null);

        if(c.moveToFirst()) {
            if(!c.isAfterLast()) {
                accountType = c.getString(c.getColumnIndex(RawContacts.ACCOUNT_TYPE));
                accountName = c.getString(c.getColumnIndex(RawContacts.ACCOUNT_NAME));

                if(accountType != null && accountName != null){
                    rntValue = new String[2];
                    rntValue[0] = accountType;
                    rntValue[1] = accountName;
                }
            }
        }

        context.getContentResolver().delete(rawContactUri, null, null);

        c.close();
        c = null;

        return rntValue;
    }


    public String getAccounts(){
        JSONObject contactData = new JSONObject();
        JSONArray dataObj = new JSONArray();
        try {
            contactData.put("return", 0);
            contactData.put("reason", "");


            AccountManager manager = AccountManager.get(context);
            Account[] accounts = manager.getAccounts();

            String[] defaultAccount = getDefaultAccountNameAndType();
            String defaultName = null;
            if(defaultAccount != null){
                defaultName = defaultAccount[1];
            }else if(accounts != null && accounts.length > 0){
                defaultName = accounts[0].name;
            }else{
                defaultName = "Phone";
            }

            if(accounts != null && accounts.length > 0){
                for(int i = 0; i < accounts.length ; i ++){
                    int isDefault = 0;
                    if(accounts[i].name.equals(defaultName)){
                        isDefault = 1;
                    }else{
                        isDefault = 0;
                    }

                    JSONObject jObj = new JSONObject();
                    jObj.put("accountType", accounts[i].type);
                    jObj.put("accountName", accounts[i].name);
                    jObj.put("isDefault", isDefault);

                    dataObj.put(jObj);
                }
            }else if(defaultAccount != null){
                JSONObject jObj = new JSONObject();
                jObj.put("accountType", defaultAccount[0]);
                jObj.put("accountName", defaultAccount[1]);
                jObj.put("isDefault", 1);

                dataObj.put(jObj);
            }

            contactData.put("data", dataObj);

        } catch (Exception ex) {
        }

		/*JSONObject contactData = new JSONObject();
		JSONArray dataObj = new JSONArray();
		try {
			contactData.put("return", 0);
			contactData.put("reason", "");
			//content://com.android.contacts/contacts/lookup/

			Account[] accounts = AccountManager.get(context).getAccounts();
			for(int i = 0; i < accounts.length ; i ++ ){
				JSONObject jObj = new JSONObject();
				jObj.put("accountType", accounts[i].type);
				jObj.put("accountName", accounts[i].name);
				dataObj.put(jObj);
			}
			contactData.put("data", dataObj);

		} catch (Exception ex) {
		}*/

        return contactData.toString();

    }

    private String getGroup() {
        JSONObject contactData = new JSONObject();
        JSONArray dataObj = new JSONArray();
        Cursor cursor = null;
        try {
            contactData.put("return", 0);
            contactData.put("reason", "");
            cursor = context.getContentResolver().query(Groups.CONTENT_URI,
                    null, null, null, null);
            while (cursor.moveToNext()) {

                int groupId = cursor.getInt(cursor.getColumnIndex(Groups._ID)); // 组id
                String groupName = null;
                if (cursor.getColumnIndex(Groups.TITLE) != -1) {
                    groupName = cursor.getString(cursor
                            .getColumnIndex(Groups.TITLE)); // 组名
                }
                String accountType = null;
                if (cursor.getColumnIndex(Groups.ACCOUNT_TYPE) != -1) {
                    accountType = cursor.getString(cursor
                            .getColumnIndex(Groups.ACCOUNT_TYPE)); // 账户类型
                }

                String accountName = null;
                if (cursor.getColumnIndex(Groups.ACCOUNT_NAME) != -1) {
                    accountName = cursor.getString(cursor
                            .getColumnIndex(Groups.ACCOUNT_NAME)); // 账户名
                }

                int deleted = 0;
                if (cursor.getColumnIndex(Groups.DELETED) != -1) {
                    deleted = cursor.getInt(cursor
                            .getColumnIndex(Groups.DELETED)); // 是否删除
                }
                int visible = 0;
                if (cursor.getColumnIndex(Groups.GROUP_VISIBLE) != -1) {
                    cursor.getInt(cursor.getColumnIndex(Groups.GROUP_VISIBLE)); // 是否可见
                }

                int isReadOnly = 0;
                if (cursor.getColumnIndex(Groups.GROUP_IS_READ_ONLY) != -1) {
                    isReadOnly = cursor.getInt(cursor
                            .getColumnIndex(Groups.GROUP_IS_READ_ONLY)); // 是否只读
                }

                int dirty = 0;
                if (cursor.getColumnIndex(Groups.DIRTY) != -1) {
                    cursor.getInt(cursor.getColumnIndex(Groups.DIRTY)); //
                }

                String systemId = null;
                if (cursor.getColumnIndex(Groups.SYSTEM_ID) != -1) {
                    systemId = cursor.getString(cursor
                            .getColumnIndex(Groups.SYSTEM_ID)); // 系统id
                }
                long sourceId = 0;
                if (cursor.getColumnIndex(Groups.SOURCE_ID) != -1) {
                    sourceId = cursor.getLong(cursor
                            .getColumnIndex(Groups.SOURCE_ID)); // 资源id
                }

                JSONObject jObj = new JSONObject();
                jObj.put("groupRowId", groupId);
                jObj.put("groupRowName", groupName);
                jObj.put("accountType", accountType);
                jObj.put("accountName", accountName);
                jObj.put("deleted", deleted);
                jObj.put("visible", visible);
                jObj.put("isReadOnly", isReadOnly);
                jObj.put("dirty", dirty);
                jObj.put("systemId", systemId);
                jObj.put("sourceId", sourceId);
                dataObj.put(jObj);
            }

            cursor.close();
            contactData.put("data", dataObj);

        } catch (Exception ex) {
            cursor.close();
        }

        return contactData.toString();
    }

    public int updateGroup(JSONObject jsonObj) {
        int rntValue = 0;
        try {
            String groupId = jsonObj.has("groupRowId") ? jsonObj
                    .getString("groupRowName") : "-1";
            if (hasContactGroup(groupId)) {
                rntValue = updateContactGroup(jsonObj);
            } else {
                rntValue = insertContactGroup(jsonObj);
            }

        } catch (Exception ex) {
        }

        return rntValue;
    }

    public int insertContactGroup(JSONObject jsonObj) {
        int rntValue = 0;
        try {
            String groupName = jsonObj.has("groupRowName") ? jsonObj
                    .getString("groupRowName") : null;
            String accountType = jsonObj.has("accountType") ? jsonObj
                    .getString("accountType") : null;
            String accountName = jsonObj.has("accountName") ? jsonObj
                    .getString("accountName") : null;
            ContentValues values = new ContentValues();
            values.put(Groups.TITLE, groupName);
            values.put(Groups.ACCOUNT_TYPE, accountType);
            values.put(Groups.ACCOUNT_NAME, accountName);

            Uri uri = context.getContentResolver().insert(Groups.CONTENT_URI,
                    values);

            rntValue = (int)ContentUris.parseId(uri);

            LogUtil.debug("成功添加组信息 ：grouName : " + groupName + " accountType : "
                    + accountType + "accountName : " + accountName);
        } catch (Exception ex) {
        }

        return rntValue;
    }

    public int updateContactGroup(JSONObject jsonObj) {
        int rntValue = 0;
        try {
            String groupName = jsonObj.has("groupRowName") ? jsonObj
                    .getString("groupRowName") : null;
            String groupRowId = jsonObj.has("groupRowId") ? jsonObj
                    .getString("groupRowId") : null;

            ContentValues values = new ContentValues();
            values.put(Groups.TITLE, groupName);

            rntValue = context.getContentResolver().update(Groups.CONTENT_URI,
                    values, Groups._ID + "=?", new String[] { groupRowId });

            LogUtil.debug("成功更改组信息 ：grouName : " + groupName);
        } catch (Exception ex) {
        }

        return rntValue;
    }

    public int deleteContactGroup(JSONObject jsonObj) {
        int rntValue = 0;
        try {
            String groupRowId = jsonObj.getString("groupRowId");
            rntValue = context.getContentResolver().delete(Groups.CONTENT_URI,
                    Groups._ID + "=?", new String[] { groupRowId });
            LogUtil.debug("成功删除组信息 ：groupId : " + groupRowId);
        } catch (Exception ex) {
        }

        return rntValue;
    }

    /** 判断是否存在通讯录组信息 ***/
    public boolean hasContactGroup(String groupRowId) {
        boolean rntValue = false;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Groups.CONTENT_URI,
                    null, Groups._ID + "=?", new String[] { groupRowId }, null);
            if (cursor.moveToNext()) {
                rntValue = true;
            }
            cursor.close();
        } catch (Exception e) {
            if (cursor != null) {
                cursor.close();
            }
            e.printStackTrace();
        }

        return rntValue;
    }

    public String getContactInfo(String where) throws JSONException {
        // 获得通讯录信息 ，URI是ContactsContract.Contacts.CONTENT_URI
        // list = new ArrayList<Contacts>();
        JSONObject contactData = new JSONObject();
        JSONArray dataObj = new JSONArray();
        JSONObject jsonObject = null;
        contactData.put("return", 0);
        contactData.put("reason", "");

        String mimetype = "";
        int oldrid = -1;
        int contactId = -1;
        int _id = -1;
        Cursor cursor = context.getContentResolver().query(Data.CONTENT_URI,
                null, where, null, Data.RAW_CONTACT_ID);
        while (cursor.moveToNext()) {
            contactId = cursor.getInt(cursor
                    .getColumnIndex(Data.RAW_CONTACT_ID));
            _id = cursor.getInt(cursor.getColumnIndex(Data._ID));
            if (oldrid != contactId) {
                if (jsonObject != null) {
                    dataObj.put(jsonObject);
                    jsonObject = null;
                }

                jsonObject = new JSONObject();
                jsonObject.put("id", contactId);
                oldrid = contactId;

                // 相片id
                int photoId = cursor.getInt(cursor
                        .getColumnIndex(Data.PHOTO_ID));
                jsonObject.put("photoId", photoId);

                // 联系人id
                int tmpContactId = cursor.getInt(cursor
                        .getColumnIndex(Data.CONTACT_ID));
                jsonObject.put("contactId", tmpContactId);

                // 账户名
                String accountName = cursor
                        .getString(cursor
                                .getColumnIndex(RawContacts.ACCOUNT_NAME));
                jsonObject.put("accountName", accountName);

                // 账户类型
                String accountType = cursor
                        .getString(cursor
                                .getColumnIndex(RawContacts.ACCOUNT_TYPE));
                jsonObject.put("accountType", accountType);

            }

            // 电话号码 mimetype = 'vnd.android.cursor.item/phone_v2' and data1 =
            // '110'
            // 取得mimetype类型
            mimetype = cursor.getString(cursor.getColumnIndex(Data.MIMETYPE));
            // 获得通讯录中每个联系人的ID
            // 获得通讯录中联系人的名字
            if (StructuredName.CONTENT_ITEM_TYPE.equals(mimetype)) {
                // String display_name =
                // cursor.getString(cursor.getColumnIndex(StructuredName.DISPLAY_NAME));
                String prefix = cursor.getString(cursor
                        .getColumnIndex(StructuredName.PREFIX));
                jsonObject.put("prefix", prefix);
                String firstName = cursor.getString(cursor
                        .getColumnIndex(StructuredName.FAMILY_NAME));
                jsonObject.put("firstName", firstName);
                String middleName = cursor.getString(cursor
                        .getColumnIndex(StructuredName.MIDDLE_NAME));
                jsonObject.put("middleName", middleName);
                String lastName = cursor.getString(cursor
                        .getColumnIndex(StructuredName.GIVEN_NAME));
                jsonObject.put("lastName", lastName);
                String suffix = cursor.getString(cursor
                        .getColumnIndex(StructuredName.SUFFIX));
                jsonObject.put("suffix", suffix);
                String sortKey = cursor.getString(cursor
                        .getColumnIndex(StructuredName.SORT_KEY_PRIMARY));
                jsonObject.put("sortKey", sortKey);
                String displayName = cursor.getString(cursor
                        .getColumnIndex(StructuredName.DISPLAY_NAME));
                jsonObject.put("displayName", displayName);

            }

            // 获取备注信息
            if (Note.CONTENT_ITEM_TYPE.equals(mimetype)) {
                String remark = cursor.getString(cursor
                        .getColumnIndex(Note.NOTE));
                jsonObject.put("remark", remark);
            }

            int typeIndexValue = 3;
            int typeIndexTitle = 2;
            int count = 0;
            Object value = null;
            for(int i = 0; i < ItemTypes.length ; i ++ ){
                if(ItemTypes[i][ItemTypeIndex].equals(mimetype)){
                    for(int j = 0; j < ContactsInfo[i].length ; j ++){
                        if(ItemTypes[i][typeIndexTitle] != null){//ContactsInfo[i][j].length > typeIndexValue){
                            //除了组信息的其他信息
                            int type = cursor.getInt(cursor.getColumnIndex(ItemTypes[i][typeIndexTitle]));
                            if( ContactsInfo[i][j][typeIndexValue].equals(type + "")){
                                count = 0;
                                while(jsonObject.has(ContactsInfo[i][j][0] + "_" + count)){
                                    count ++;
                                }
                                jsonObject.put(ContactsInfo[i][j][0] + "_" + count, _id);

                                for(int k = 2; k < ContactsInfo[i][j].length;k++){
                                    if(k != typeIndexValue && ItemTypes[i][k-1] != null){
                                        if(ItemValueVar[i][k-1] == 0){
                                            //string 类型
                                            value = cursor.getString(cursor.getColumnIndex(ItemTypes[i][k-1]));
                                        }else if(ItemValueVar[i][k-1] == 1){
                                            //int 类型
                                            value = cursor.getInt(cursor.getColumnIndex(ItemTypes[i][k-1]));
                                        }

                                        count = 0;
                                        while(jsonObject.has(ContactsInfo[i][j][k] + "_" + count)){
                                            count ++;
                                        }
                                        jsonObject.put(ContactsInfo[i][j][k] + "_" + count, value);
                                    }
                                }
                            }
                        }else{
                            //获取组信息
                            count = 0;
                            while(jsonObject.has(ContactsInfo[i][j][0] + "_" + count)){
                                count ++;
                            }
                            jsonObject.put(ContactsInfo[i][j][0] + "_" + count, _id);

                            for(int k = 2; k < ContactsInfo[i][j].length;k++){
                                if(ItemTypes[i][k-1] != null){
                                    if(ItemValueVar[i][k-1] == 0){
                                        //string 类型
                                        value = cursor.getString(cursor.getColumnIndex(ItemTypes[i][k-1]));
                                    }else if(ItemValueVar[i][k-1] == 1){
                                        //int 类型
                                        value = cursor.getInt(cursor.getColumnIndex(ItemTypes[i][k-1]));
                                    }
                                    count = 0;
                                    while(jsonObject.has(ContactsInfo[i][j][k] + "_" + count)){
                                        count ++;
                                    }
                                    jsonObject.put(ContactsInfo[i][j][k] + "_" + count, value);
                                }
                            }
                        }
                    }
                }
            }

        }

        cursor.close();
        if (jsonObject != null) {
            dataObj.put(jsonObject);
            jsonObject = null;
        }

        contactData.put("data", dataObj);

        Log.i("contactData", contactData.toString());
        return contactData.toString();
    }

    /** 判断是否存在通讯录信息 ***/
    public boolean hasContact(int contactId) {
        boolean rntValue = false;
        Cursor cursor = null;
        try {

            cursor = context.getContentResolver().query(Data.CONTENT_URI, null,
                    Data.RAW_CONTACT_ID + " =? ",
                    new String[] { "" + contactId }, Data.RAW_CONTACT_ID);
            if (cursor.moveToNext()) {
                rntValue = true;
            }
            cursor.close();
        } catch (Exception e) {
            if (cursor != null) {
                cursor.close();
            }
            e.printStackTrace();
        }

        return rntValue;
    }


    Builder getBuilder(int contactId, int flag, int insertId, String itemType,
                       int dataId) {
        Builder builder = null;
        if (flag == 0) {
            builder = ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, insertId)
                    .withValue(Data.MIMETYPE, itemType);
        } else {

            if (dataId == DEFAULT) {
                // dataId = (int)insertContentValues(itemType,contactId);
                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValue(Data.RAW_CONTACT_ID, contactId)
                        .withValue(Data.MIMETYPE, itemType);
            } else {
                String where = "";
                String[] data = null;
                if (dataId == NOCHECK) {
                    where = Data.RAW_CONTACT_ID + "=?" + " AND "
                            + Data.MIMETYPE + " = ? ";
                    data = new String[] { String.valueOf(contactId), itemType };
                } else {
                    where = Data.RAW_CONTACT_ID + "=?" + " AND "
                            + Data.MIMETYPE + " = ? " + "and " + Data._ID
                            + " = ?";
                    data = new String[] { String.valueOf(contactId), itemType,
                            String.valueOf(dataId) };
                }

                // 修改联系人信息
                builder = ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                        .withSelection(where, data);
            }

            builder.withYieldAllowed(true);
        }

        return builder;
    }
    Builder getInsertBuilder(int insertId,int dataId,String itemType){
        return ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, insertId)
                .withValue(Data.MIMETYPE, itemType);
    }

    Builder getUpdateBuilder(int contactId, int dataId,String itemType){
        Builder builder = null;
        String where = "";
        String[] data = null;
        if(dataId == DEFAULT){
            builder = ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValue(Data.RAW_CONTACT_ID, contactId)
                    .withValue(Data.MIMETYPE, itemType);
        }else if (dataId == NOCHECK) {
            where = Data.RAW_CONTACT_ID + "=?" + " AND "
                    + Data.MIMETYPE + " = ? ";
            data = new String[] { String.valueOf(contactId), itemType };
            builder = ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                    .withSelection(where, data);
        } else {
            where = Data.RAW_CONTACT_ID + "=?" + " AND "
                    + Data.MIMETYPE + " = ? " + "and " + Data._ID
                    + " = ?";
            data = new String[] { String.valueOf(contactId), itemType,
                    String.valueOf(dataId) };
            builder = ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                    .withSelection(where, data);
        }

        // 修改联系人信息
        return builder;
    }

    Builder getDeleteBuilder(int contactId, int dataId) {
        String where = "";
        String[] data = null;
        where = Data.RAW_CONTACT_ID + "=?" + " AND " + Data._ID + " = ?";
        data = new String[] { String.valueOf(contactId), String.valueOf(dataId) };

        return ContentProviderOperation.newDelete(Data.CONTENT_URI)
                .withSelection(where, data);
    }

    public long insertContentValues(String itemType, int contactId) {

        ContentValues values = new ContentValues();
        values.put(Data.RAW_CONTACT_ID, contactId);
        values.put(Data.MIMETYPE, itemType);
        Uri uri = context.getContentResolver().insert(Data.CONTENT_URI, values);
        if (uri != null) {
            return ContentUris.parseId(uri);
        } else {
            return -1;
        }
    }

    Object formatDefault(JSONObject jsonObj, String key, String defaultNum)
            throws JSONException {
        return jsonObj.has(key) ? jsonObj.get(key) : defaultNum;
    }
    int formatDefaultId(JSONObject jsonObj, String key, int defaultNum) throws JSONException{
        return jsonObj.has(key) ? jsonObj.getInt(key) : defaultNum;
    }

    public int updateContact(JSONObject jsonObj) {
        int rntValue = 0;
        try {

            ContentProviderResult[] results = null;
            int id = formatDefaultId(jsonObj, "id", DEFAULT);
            // boolean hasContact = this.hasContact(id);
            if (id != -1) {
                results = updateContact(jsonObj, 1);
            } else {
                results = updateContact(jsonObj, 0);
            }

            if (id == -1) {
                for (int j = 0; j < results.length; j++) {
                    ContentProviderResult result = results[j];
                    String path = result.uri.getPath();
                    if (path != null && path.contains("raw_contacts")) {
                        rntValue = ConvertUtil.objToInt(path.substring(path
                                .lastIndexOf("/") + 1));
                        break;
                    }
                }
            } else {
                // 更新发送返回值1
                rntValue = id;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return rntValue;
    }

    String[] getPreExtName(String name) {
        String[] rntValue = { "", "-1" };
        if (name != null) {
            int lastIndex = name.lastIndexOf("_");
            if (lastIndex != -1) {
                rntValue[0] = name.substring(0, lastIndex);
                rntValue[1] = name.substring(lastIndex + 1);
            }
        }

        return rntValue;
    }


    //Phone.CONTENT_ITEM_TYPE
    private final String[][][] ContactsInfo = {
            {
                    //手机
                    {"mobileId","mobileStates","mobile",Phone.TYPE_MOBILE + "","手机号"},
                    // 自定义手机
                    {"customNumId","customNumStates","customNum",Phone.TYPE_CUSTOM + "","customNumName"},
                    // 其他电话
                    {"otherNumId","otherNumStates","otherNum",Phone.TYPE_OTHER + "","其他电话"},
                    //住宅电话
                    {"homeNumId","homeNumStates","homeNum",Phone.TYPE_HOME + "","住宅电话"},
                    //工作电话
                    {"jobNumId","jobNumStates","jobNum",Phone.TYPE_WORK + "","工作电话"},
                    //单位传真
                    {"workFaxId","workFaxStates","workFax",Phone.TYPE_FAX_WORK + "","单位传真"},
                    //住宅传真
                    {"homeFaxId","homeFaxStates","homeFax",Phone.TYPE_FAX_HOME + "","住宅传真"},
                    //寻呼机
                    {"pagerId","pagerStates_","pager",Phone.TYPE_PAGER + "","寻呼机"},
                    //回拨号码
                    {"quickNumId","quickNumStates","quickNum",Phone.TYPE_CALLBACK + "","回拨号码"},
                    //公司总机
                    {"jobTelId","jobTelStates","jobTel",Phone.TYPE_COMPANY_MAIN + "","公司总机"},
                    //车载电话
                    {"carNumId","carNumStates","carNum",Phone.TYPE_CAR + "","车载电话"},
                    //ISDN
                    {"isdnId","isdnStates","isdn",Phone.TYPE_ISDN + "","ISDN"},
                    //总机
                    {"telId","telStates","tel",Phone.TYPE_MAIN + "","总机"},
                    //无线装置
                    {"wirelessDevId","wirelessDevStates","wirelessDev",Phone.TYPE_RADIO + "","无线装置"},
                    //电报
                    {"telegramId","telegramStates","telegram",Phone.TYPE_TELEX + "","电报"},
                    //TTY_TDD
                    {"tty_tddId","tty_tddStates","tty_tdd",Phone.TYPE_TTY_TDD + "","TTY_TDD"},
                    //单位手机
                    {"jobMobileId","jobMobileStates","jobMobile",Phone.TYPE_WORK_MOBILE + "","单位手机"},
                    //单位寻呼机
                    {"jobPagerId","jobPagerStates","jobPager",Phone.TYPE_WORK_PAGER + "","单位寻呼机"},
                    //助理
                    {"assistantNumId","assistantNumStates","assistantNum",Phone.TYPE_ASSISTANT + "","助理"},
                    //彩信
                    {"mmsId","mmsStates","mms",Phone.TYPE_MMS + "","彩信"},
            },

            //邮件类型
            {
                    //家庭邮箱
                    {"homeEmailId","homeEmailStates","homeEmail",Email.TYPE_HOME + "",""},
                    //工作邮箱
                    {"jobEmailId","jobEmailStates","jobEmail",Email.TYPE_WORK + "",""},
                    //自定义邮箱
                    {"customEmailId","customEmailStates","customEmail",Email.TYPE_CUSTOM + "","customEmailName"},
                    //其他邮箱
                    {"otherEmailId","otherEmailStates","otherEmail",Email.TYPE_OTHER + "",""},

            },

            //即时通讯类型
            {
                    //msn
                    {"msnImId","msnImStates","msnIm",Im.PROTOCOL_MSN + "",null},
                    //qq
                    {"qqImId","qqImStates","qqIm",Im.PROTOCOL_QQ + "",null},
                    //yaHoo
                    {"yaHooImId","yaHooImStates","yaHooIm",Im.PROTOCOL_YAHOO + "",null},
                    //aim
                    {"aimImId","aimImStates","aimIm",Im.PROTOCOL_AIM+ "",null},
                    //skypeIm
                    {"skypeImImId","skypeImImStates","skypeImIm",Im.PROTOCOL_SKYPE + "",null},
                    //gtalk
                    {"gtalkImId","gtalkImStates","msnIm",Im.PROTOCOL_GOOGLE_TALK + "",null},
                    //icq
                    {"icqImId","icqImStates","icqIm",Im.PROTOCOL_ICQ + "",null},
                    //jabber
                    {"jabberImId","jabberImStates","jabberIm",Im.PROTOCOL_JABBER + "",null},
                    //custom
                    {"customImId","customImStates","customIm",Im.PROTOCOL_CUSTOM + "","customImName"},
            },

            //组织类型
            {
                    //自定义组织
                    {"customCompanyId","customCompanyStates","customCompany",Organization.TYPE_CUSTOM + "","customCompanyName","customJobTitle","customDepartment"},
                    //其他组织
                    {"otherCompanyId","otherCompanyStates","otherCompany",Organization.TYPE_OTHER + "",null,"otherJobTitle","otherDepartment"},
                    //单位组织
                    {"workCompanyId","workCompanyStates","workCompany",Organization.TYPE_WORK + "",null,"workJobTitle","workDepartment"},
            },

            //通讯地址
            {
                    //地址
                    {"streetId","streetStates","street",StructuredPostal.TYPE_WORK + "",null,"city","box","area","state","zip","country"},
                    //家庭通讯地址
                    {"homeStreetId","homeStreetStates","homeStreet",StructuredPostal.TYPE_HOME + "",null,"homeCity","homeBox","homeArea","homeState","homeZip","homeCountry"},
                    //其他通讯地址
                    {"otherStreetId","otherStreetStates","otherStreet",StructuredPostal.TYPE_OTHER + "",null,"otherCity","otherBox","otherArea","otherState","otherZip","otherCountry"},
                    //自定通讯地址
                    {"customStreetId","customStreetStates","customStreet",StructuredPostal.TYPE_CUSTOM + "","customStreetName","customCity","customBox","customArea","customState","customZip","customCountry"},
            },

            //组信息
            {
                    {"groupRowIdId","groupRowIdStates","groupRowId",null}
            }



    };

    //记录类型
    private final String[][] ItemTypes = {
            {Phone.CONTENT_ITEM_TYPE,             Phone.NUMBER,                       Phone.TYPE,                  Phone.LABEL},
            {Email.CONTENT_ITEM_TYPE,             Email.DATA,                         Email.TYPE,                  Email.LABEL},
            {Im.CONTENT_ITEM_TYPE,                Im.DATA,                            Im.PROTOCOL,                 Im.LABEL},
            {Organization.CONTENT_ITEM_TYPE,      Organization.COMPANY,               Organization.TYPE,           Organization.LABEL,     Organization.TITLE,       Organization.DEPARTMENT   },
            {StructuredPostal.CONTENT_ITEM_TYPE,  StructuredPostal.STREET,            StructuredPostal.TYPE,       StructuredPostal.LABEL, StructuredPostal.CITY,    StructuredPostal.POBOX,     StructuredPostal.NEIGHBORHOOD,     StructuredPostal.REGION,     StructuredPostal.POSTCODE,    StructuredPostal.COUNTRY},
            {ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,      ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,null}
    };
    //数据库类型字段定义
    //string : 0  int : 1
    private final int[][] ItemValueVar = {
            {0,0,1,0},
            {0,0,1,0},
            {0,0,1,0},
            {0,0,1,0,0, 0},
            {0,0,1,0,0,0,0,0,0,0},
            {0,1,0}
    };

    static final int Id =0 ,States = 1;
    static final int ItemTypeIndex = 0 , TypeIndex = 2 ;


    Builder updateEachData(JSONObject jsonObj,String keys,int contactId,int flag,int rawContactInsertIndex) throws JSONException{
        Builder builder = null;
        String[] splits = getPreExtName(keys);
        String key = splits[0];
        int count = ConvertUtil.objToInt(splits[1]);
        for(int item = 0; item < ItemTypes.length ; item++){
            for(int i = 0; i < ContactsInfo[item].length ; i ++){
                if(ContactsInfo[item][i][Id].equals(key)){
                    // 手机
                    int dataId = formatDefaultId(jsonObj, ContactsInfo[item][i][Id] + "_" + count,DEFAULT);
                    int states = formatDefaultId(jsonObj, ContactsInfo[item][i][States] + "_" + count,ADDSTATES);

                    switch (states) {
                        case DELETESTATES:
                            builder = getDeleteBuilder(contactId, dataId);
                            break;
                        default:
                            switch(flag){
                                case 0:
                                    builder = getInsertBuilder(rawContactInsertIndex, dataId, ItemTypes[item][0]);
                                    break;
                                case 1:
                                    builder = getUpdateBuilder(contactId,dataId,ItemTypes[item][0]);
                                    break;
                            }
                            //添加或者修改设置值
                            //j为每个字段下面的个数 ， j = 1则是从第一个开始算起，如Phone.CONTENT_ITEM_TYPE ， j + 1则是从states开始算起
                            for(int j = 1 ; j<ItemTypes[item].length;j++){
                                if(ItemTypes[item][j] != null){
                                    Object data = formatDefault(jsonObj,ContactsInfo[item][i][j + 1] + "_" + count,ContactsInfo[item][i][j + 1]);
                                    builder.withValue(ItemTypes[item][j],data);
                                }
                            }
                            return builder;
                    }
                }
            }
        }


        return builder;
    }

    /**
     * 批量更新
     */
    public ContentProviderResult[] updateContact(JSONObject jsonObj, int flag) {
        try {
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
            int rawContactInsertIndex = ops.size();
            int contactId = formatDefaultId(jsonObj, "id", 0);
            Builder builder = null;
            int states = ADDSTATES;
            int dataId = -1;
            if (flag == 0) {
                // 添加联系人信息
                // 文档位置：reference/android/provider/ContactsContract.RawContacts.html
                ops.add(ContentProviderOperation
                        .newInsert(RawContacts.CONTENT_URI)
                        .withValue(
                                RawContacts.ACCOUNT_TYPE,
                                jsonObj.has("accountType") ? jsonObj.get("accountType") : null)
                        .withValue(
                                RawContacts.ACCOUNT_NAME,
                                jsonObj.has("accountName") ? jsonObj.get("accountName") : null)
                        .withYieldAllowed(true).build());

                LogUtil.debug("通讯录账户");
            } else if (flag == 1) {
                // 更新
                ops.add(ContentProviderOperation
                        .newUpdate(RawContacts.CONTENT_URI)
                        .withSelection(RawContacts.CONTACT_ID + "=?",
                                new String[] { "" + contactId })
                        .withValue(
                                RawContacts.ACCOUNT_TYPE,jsonObj.has("accountType") ? jsonObj
                                        .get("accountType") : null)
                        .withValue(
                                RawContacts.ACCOUNT_NAME,
                                jsonObj.has("accountName") ? jsonObj.get("accountName") : null)
                        .withYieldAllowed(true).build());
                // 修改联系人信息
                // rawContactInsertIndex = jsonObj.getInt("id");
            }

            // 文档位置：reference/android/provider/ContactsContract.Data.html
            // 添加通讯录中联系人的名字
            ops.add(getBuilder(contactId, flag, rawContactInsertIndex,
                    StructuredName.CONTENT_ITEM_TYPE, NOCHECK)
                    .withValue(
                            StructuredName.PREFIX,
                            jsonObj.has("prefix") ? jsonObj.get("prefix")
                                    : null)
                    .withValue(
                            StructuredName.FAMILY_NAME,
                            jsonObj.has("firstName") ? jsonObj.get("firstName")
                                    : null)
                    .withValue(
                            StructuredName.MIDDLE_NAME,
                            jsonObj.has("middleName") ? jsonObj
                                    .get("middleName") : null)
                    .withValue(
                            StructuredName.GIVEN_NAME,
                            jsonObj.has("lastName") ? jsonObj.get("lastName")
                                    : null)
                    .withValue(
                            StructuredName.SUFFIX,
                            jsonObj.has("suffix") ? jsonObj.get("suffix")
                                    : null).build());
            LogUtil.debug("通讯录名字");


            // 备注信息
            if (jsonObj.has("remark")) {
                // 备注信息
                ops.add(getBuilder(contactId, flag, rawContactInsertIndex,
                        Note.CONTENT_ITEM_TYPE, NOCHECK).withValue(Note.NOTE,
                        jsonObj.get("remark")).build());
            }

            int count = 0;
            Iterator ite = jsonObj.keys();
            while (ite.hasNext()) {
                String keys = (String) ite.next();
                Builder tmpBuild = updateEachData(jsonObj,keys,contactId,flag,rawContactInsertIndex);
                if(tmpBuild != null){
                    ops.add(tmpBuild.build());
                }
            }

            // 执行插入操作
            ContentProviderResult[] results = context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

            return results;
        } catch (Exception ex) {
            LogUtil.debug(ex.getMessage());
        }

        return null;
    }

    /** 删除通讯录信息 */
    public boolean deleteContact(Context context, int rawContactId)
            throws Exception {
        boolean rntValue = false;
        try {
            ContentResolver resolver = context.getContentResolver();

            // 根据id删除raw_contacts中的相应数据
            Uri uri = Uri.parse("content://com.android.contacts/raw_contacts");
            resolver.delete(uri, "_id=?", new String[] { rawContactId + "" });

            // 根据id删除data中的相应数据
            uri = Uri.parse("content://com.android.contacts/data");
            resolver.delete(uri, "raw_contact_id=?",
                    new String[] { rawContactId + "" });

            rntValue = true;
        } catch (Exception ex) {
        }

        return rntValue;
    }

    /*
     * 　sms主要结构： 　　 　　_id：短信序号，如100 　　
     * 　　thread_id：对话的序号，如100，与同一个手机号互发的短信，其序号是相同的 　　
     * 　　address：发件人地址，即手机号，如+8613811810000 　　
     * 　　person：发件人，如果发件人在通讯录中则为具体姓名，陌生人为null 　　
     * 　　date：日期，long型，如1256539465022，可以对日期显示格式进行设置 　　
     * 　　protocol：协议0SMS_RPOTO短信，1MMS_PROTO彩信 　　 　　read：是否阅读0未读，1已读 　　
     * 　　status：短信状态-1接收，0complete,64pending,128failed 　　
     * 　　type：短信类型1是接收到的，2是已发出 　　 { ALL = 0; INBOX = 1; SENT = 2; DRAFT = 3;
     * OUTBOX = 4; FAILED = 5; QUEUED = 6; } 　　body：短信具体内容 　　
     * 　　service_center：短信服务中心号码编号，如+8613800755500
     */
    public void getSmsInPhone(SocketChannel sc, String where, String sortOrder) {
        try {
            ContentResolver cr = context.getContentResolver();
            String[] projection = new String[] { "_id", "thread_id", "address",
                    "person", "body", "date", "type", "status", "read" };
            Uri uri = Uri.parse(SMS.SMS_URI_ALL);
            Cursor cur = cr.query(uri, projection, where, null, sortOrder);

            // 统计sms数据条数
            if (SMS.smsCount == -1) {
                SMS.smsCount = cur.getCount();
            }

            if (cur.moveToFirst()) {
                int _id;
                int thread_id;
                String name = null;
                String phoneNumber;
                String smsbody;
                String date;
                int type;
                int status;
                int read;

                int _idColumn = cur.getColumnIndex("_id");
                int threadIdColumn = cur.getColumnIndex("thread_id");
                int phoneNumberColumn = cur.getColumnIndex("address");
                int smsbodyColumn = cur.getColumnIndex("body");
                int dateColumn = cur.getColumnIndex("date");
                int typeColumn = cur.getColumnIndex("type");
                int statusColumn = cur.getColumnIndex("status");
                int readColumn = cur.getColumnIndex("read");
                do {
                    _id = cur.getInt(_idColumn);
                    thread_id = cur.getInt(threadIdColumn);
                    phoneNumber = cur.getString(phoneNumberColumn);
                    smsbody = cur.getString(smsbodyColumn);
                    date = cur.getLong(dateColumn) + "";
                    type = cur.getInt(typeColumn);
                    status = cur.getInt(statusColumn);
                    read = cur.getInt(readColumn);

                    if (phoneNumber != null) {
                        Cursor pCur = cr
                                .query(Phone.CONTENT_URI,
                                        null,
                                        Phone.NUMBER
                                                + " = ?",
                                        new String[] { phoneNumber }, null);

                        if (pCur != null) {
                            if (pCur.moveToFirst()) {
                                name = pCur
                                        .getString(pCur
                                                .getColumnIndex(Phone.DISPLAY_NAME));
                            }
                            pCur.close();
                        }

                    }

                    byte[] bThreadId;
                    byte[] bName = null;
                    byte[] bPhoneNumber = null;
                    byte[] bSmsbody = null;
                    byte[] bDate = null;
                    byte bType;
                    byte bStatus;
                    int cContent = 0;//
                    byte[] bId = null;
                    byte bRead;

                    cContent = cContent + 4;// 内容长度
                    bThreadId = ConvertUtil.intToByteArray(thread_id);

                    cContent = cContent + 4;
                    if (name != null) {
                        bName = name.getBytes("utf-8");
                        cContent = cContent + bName.length;
                    }

                    cContent = cContent + 4;
                    if (phoneNumber != null) {
                        bPhoneNumber = phoneNumber.getBytes("utf-8");
                        cContent = cContent + bPhoneNumber.length;// 电话号码内容长度
                    }

                    cContent = cContent + 4;
                    if (smsbody != null) {
                        bSmsbody = smsbody.getBytes("utf-8");
                        cContent = cContent + bSmsbody.length;// 信息内容长度
                    }

                    cContent = cContent + 4;
                    if (date != null) {
                        bDate = date.getBytes("utf-8");
                        cContent = cContent + bDate.length;// 时间内容长度
                    }

                    cContent = cContent + 1;// 内容长度
                    bType = (byte) type;
                    cContent = cContent + 1;// 内容长度
                    bStatus = (byte) status;

                    cContent = cContent + 4;
                    bId = ConvertUtil.intToByteArray(_id);// 信息标识Id内容长度

                    cContent = cContent + 1;
                    bRead = (byte) read;

                    if (HEADER_LENGTH + cContent > space) {
                        // 如果内容长度大于预存控件，重新申请空间
                        byteBuffer = ByteBuffer.allocate(HEADER_LENGTH
                                + cContent);
                    }

                    ByteBuffer header = this.getHeaderData(
                            I4ANDROID_E_CMD_MESSAGE_RESP, cContent, TYPE_BYTE,
                            0, 0);
                    // 添加头部包头信息
                    byteBuffer.clear();
                    byteBuffer.put(header);

                    // ////////////////添加数据信息/////////////////////////
                    // thread_id
                    byteBuffer.put(bThreadId);
                    // 名称
                    if (bName != null) {
                        byteBuffer.put(ConvertUtil.intToByteArray(bName.length));
                        byteBuffer.put(bName);
                    } else {
                        byteBuffer.put(ConvertUtil.intToByteArray(0));
                    }
                    // 电话号码
                    if (bPhoneNumber != null) {
                        byteBuffer
                                .put(ConvertUtil.intToByteArray(bPhoneNumber.length));
                        byteBuffer.put(bPhoneNumber);
                    } else {
                        byteBuffer.put(ConvertUtil.intToByteArray(0));
                    }
                    // 信息内容
                    if (bSmsbody != null) {
                        byteBuffer.put(ConvertUtil.intToByteArray(bSmsbody.length));
                        byteBuffer.put(bSmsbody);
                    } else {
                        byteBuffer.put(ConvertUtil.intToByteArray(0));
                    }
                    // 日期
                    if (bDate != null) {
                        byteBuffer.put(ConvertUtil.intToByteArray(bDate.length));
                        byteBuffer.put(bDate);
                    } else {
                        byteBuffer.put(ConvertUtil.intToByteArray(0));
                    }
                    // 类型
                    byteBuffer.put(bType);
                    // 状态
                    byteBuffer.put(bStatus);
                    // 信息标识id
                    byteBuffer.put(bId);
                    // 添加是否已读
                    byteBuffer.put(bRead);

                    byteBuffer.rewind();
                    byteBuffer.limit(HEADER_LENGTH + cContent);

                    while (byteBuffer.hasRemaining()) {
                        sc.write(byteBuffer);
                    }

                } while (cur.moveToNext());

                cur.close();
                ByteBuffer endData = getHeaderData(
                        I4ANDROID_E_CMD_MESSAGE_RESP, 0, TYPE_BYTE, 0, 1);
                while (endData.hasRemaining()) {
                    sc.write(endData);
                }

            }

        } catch (SQLiteException ex) {
            LogUtil.error(ex.getMessage());
        } catch (UnsupportedEncodingException ex) {
            LogUtil.error(ex.getMessage());
        } catch (IOException ex) {
            LogUtil.error(ex.getMessage());
        }
    }

    public void sendSms(int sendId, int isNewThreadId, int thread_id,
                        String address, String person, String body) {
        int _id = sendId;
        int sendThreadId = thread_id;
        // 当数据库，即手机正在发送信息，pc端还没开始发送信息的时候，更新pc的id
        ContentResolver cr = context.getContentResolver();
        String[] projection = new String[] { "_id", "thread_id" };
        Uri uri = Uri.parse(SMS.SMS_URI_ALL);
        Cursor cursor = null;

        Uri insertedUri = insertSms(SMS.SMS_URI_ALL, _id, thread_id, address,
                person, body, SMS.OUTBOX, SMS.no_read,
                System.currentTimeMillis(), 0);
        long rntId = ContentUris.parseId(insertedUri);
        System.out.println("测试返回值 ： "+rntId);

        _id = ConvertUtil.objToInt(insertedUri.getPath().replace("/", ""));

        cursor = cr.query(uri, projection, "_id = ?",
                new String[] { _id + "" }, "thread_id desc");
        if (cursor != null && cursor.moveToFirst()) {
            thread_id = cursor.getInt(1);
        }

        if (cursor != null) {
            cursor.close();
        }

        Intent itSend = new Intent(SMS_SEND_ACTIOIN);

        Bundle bundle = new Bundle();
        bundle.putInt("sendId", sendId);
        bundle.putInt("_id", _id);
        bundle.putInt("thread_id", thread_id);
        bundle.putInt("sendThreadId", sendThreadId);
        itSend.putExtras(bundle);
        if (requestCode == 999999999) {
            requestCode = 0;
        } else {
            requestCode++;
        }
        // Intent itDeliver = new Intent(SMS_DELIVERED_ACTION);
		/* sentIntent参数为传送后接受的广播信息PendingIntent */
        PendingIntent mSendPI = PendingIntent.getBroadcast(
                context.getApplicationContext(), requestCode, itSend,
                PendingIntent.FLAG_UPDATE_CURRENT);
		/* deliveryIntent参数为送达后接受的广播信息PendingIntent */
        // PendingIntent mDeliverPI =
        // PendingIntent.getBroadcast(context.getApplicationContext(), 0,
        // itDeliver, 0);

        SmsManager smsManager = SmsManager.getDefault();
        // 如果字数超过50,需拆分成多条短信发送
        if (body.length() > 70) {
            ArrayList<String> msgs = smsManager.divideMessage(body);
            for (String msg : msgs) {
                smsManager.sendTextMessage(address, null, msg, mSendPI, null);
            }
        } else {
            smsManager.sendTextMessage(address, null, body, mSendPI, null);
        }

    }

    public Uri insertSms(String smsUrl, int _id, int thread_id, String address,
                         String person, String body, int type, int read, long date,
                         int locked) {
        /** 将发送的短信插入数据库 **/
        ContentValues values = new ContentValues();
        // _id

        // values.put("_id", _id);
        // thread_id
        // values.put("thread_id", thread_id);
        // address
        values.put("address", address);
        // person
        values.put("person", person);
        // body
        values.put("body", body);
        // 发送时间
        values.put("date", date);
        // type
        values.put("type", type);
        // status
        values.put("read", read);
        // lock
        values.put("locked", locked);
        // 插入短信库
        return context.getContentResolver().insert(Uri.parse(smsUrl), values);
    }

    public void updateSms(String smsUrl, int _id, int thread_id,
                          String address, String person, String body, int type, int read,
                          long date) {
        /** 将发送的短信插入数据库 **/
        ContentValues values = new ContentValues();
        // _id
        values.put("_id", _id);
        // thread_id
        values.put("thread_id", thread_id);
        // address
        values.put("address", address);
        // person
        values.put("person", person);
        // body
        values.put("body", body);
        // 发送时间
        values.put("date", date);
        // type
        values.put("type", type);
        // status
        values.put("read", read);

        // 插入短信库
        context.getContentResolver().update(Uri.parse(smsUrl), values,
                "_id = ? and thread_id = ?",
                new String[] { "" + _id, "" + thread_id });
    }

    public boolean hasRecord(String smsUrl, int _id, int thread_id) {
        boolean rntValue = false;
        Cursor cursor = context.getContentResolver().query(Uri.parse(smsUrl),
                new String[] { "_id", "thread_id" },
                "_id = ? and thread_id = ?",
                new String[] { _id + "", thread_id + "" }, null);
        if (cursor != null && cursor.getCount() > 0) {
            rntValue = true;
        }
        if (cursor != null) {
            cursor.close();
        }

        return rntValue;
    }

    public void deleteSms(int type, int thread_id, int _id) {
        String where = " 1=1";
        switch (type) {
            case 0:
                // 删除全部短信
                break;
            case 1:
                // 删除单个聊天框的所有信息
                where = "thread_id = '" + thread_id + "'";
                break;
            case 2:
                // 删除单条短信
                where = "thread_id = '" + thread_id + "'" + " and _id = '" + _id
                        + "'";
                break;
        }
        int rntValue = context.getContentResolver().delete(Uri.parse(SMS.SMS_URI_ALL), where,
                null);

        System.out.println(rntValue);
    }

}
