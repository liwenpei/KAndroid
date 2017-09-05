package me.keeganlee.kandroid.betach;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

import me.keeganlee.kandroid.tools.ConvertUtil;
import me.keeganlee.kandroid.tools.LogUtil;
import me.keeganlee.kandroid.tools.MD5AndSignature;

public class BetachApkManager extends BaseBetach{
    private Context context;
    private JSONArray expDataObj = null;

    public BetachApkManager(Context context){
        this.context = context;
    }

    /*** 发送reponseVersion数据 **/
    public byte[]  getAppsInfo(String packageName) {
        byte[] rntValue = null;

        try {
            // 请求手机短信、联系人、通话记录的个数
            JSONObject obj = new JSONObject();
            obj.put("return", 0);
            obj.put("reason", "");
            expDataObj = new JSONArray();
            PackageManager pkManager = context.getPackageManager();
            List<PackageInfo> packageInfos = pkManager.getInstalledPackages(0);
            for (int i = 0; i < packageInfos.size(); i++) {
                PackageInfo pInfo = pkManager.getPackageInfo(packageInfos.get(i).packageName, PackageManager.GET_PERMISSIONS);
                if(packageName != null && !"".equals(packageName)){
                    if(!packageName.equals(pInfo.packageName)){
                        continue;
                    }
                }

                JSONObject tmpObj = new JSONObject();

                tmpObj.put("appName", pInfo.applicationInfo.loadLabel(
                        context.getPackageManager()).toString());
                tmpObj.put("packageName", pInfo.packageName);
                tmpObj.put("versionCode", pInfo.versionCode);
                tmpObj.put("versionName", pInfo.versionName);

                //设置大小
                String fileName = pInfo.applicationInfo.sourceDir;
                File file = new File(fileName);
                long size = file.length();
                tmpObj.put("size", size);
                tmpObj.put("installPath", pInfo.applicationInfo.sourceDir);

                //tmpObj.put("iconSize", getIcon(pInfo.packageName).length);
                boolean isSysApp = (pInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                tmpObj.put("isSysApp", isSysApp);
                boolean isSdcard = (pInfo.applicationInfo.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0;
                tmpObj.put("isSdcard", isSdcard);
                JSONArray jsonArray = new JSONArray();
                String sharedPkgList[] = pInfo.requestedPermissions;//得到权限列表
                if(sharedPkgList != null){
                    for (int j = 0; j < sharedPkgList.length; j++) {
                        try{
                            String permName = sharedPkgList[j];
                            JSONObject permissionObj = new JSONObject();
                            PermissionInfo tmpPermInfo = pkManager.getPermissionInfo(permName, 0);//通过permName得到该权限的详细信息
                            //PermissionGroupInfo pgi = pkManager.getPermissionGroupInfo( tmpPermInfo.group, 0);//权限分为不同的群组，通过权限名，我们得到该权限属于什么类型的权限。

                            permissionObj.put("title", tmpPermInfo.loadLabel(pkManager).toString());
                            permissionObj.put("description",tmpPermInfo.loadDescription(pkManager).toString());
                            jsonArray.put(permissionObj);
                        }catch(Exception ex){
                        }

                    }
                    tmpObj.put("permissionData", jsonArray);
                }

                expDataObj.put(tmpObj);
            }
            obj.put("data", expDataObj);

            byte[] bObj = obj.toString().getBytes();
            int length = bObj.length;
            ByteBuffer byteBuff = null;
            byteBuff = ByteBuffer.allocate(HEADER_LENGTH + length);
            // 返回头部位坐标0:响应命令
            byteBuff.put((byte) I4ANDROID_E_CMD_APK_RESP);
            // 返回头部位坐标1-4:内容长度
            byteBuff.put(ConvertUtil.intToByteArray(length));
            // 返回头部位坐标5:内容类型
            byteBuff.put((byte) TYPE_JSON);
            // 返回头部位坐标6:拓展字节
            byteBuff.put((byte) 0);
            // 返回头部位坐标7:拓展字节
            byteBuff.put((byte) 0);
            // 返回数据部
            byteBuff.put(bObj);

            rntValue = byteBuff.array();
        } catch (Exception ex) {
            LogUtil.debug("发送sorket内容出错" + ex.getMessage());
        }

        return rntValue;
    }

    /**获取icon*/
    public ByteBuffer getIcon(String pkgName){
        try {
            ByteBuffer tmpBuff = ByteBuffer.wrap(ConvertUtil.Drawable2Bytes(context.getPackageManager().getPackageInfo(pkgName, 0).applicationInfo.loadIcon(context.getPackageManager())));
            ByteBuffer byteBuffer = ByteBuffer.allocate(8 + tmpBuff.capacity());
            // 返回头部位坐标0:响应命令
            byteBuffer.put((byte) BaseBetach.I4ANDROID_E_CMD_APK_ICON_RESP);
            // 返回头部位坐标1-4:内容长度
            byteBuffer.put(ConvertUtil.intToByteArray(tmpBuff.capacity()));
            // 返回头部位坐标5:内容类型
            byteBuffer.put((byte)BaseBetach.TYPE_PNG);
            // 返回头部位坐标6:拓展字节
            byteBuffer.put((byte) 0);
            // 返回头部位坐标7:拓展字节
            byteBuffer.put((byte) 0);
            byteBuffer.put(tmpBuff);
            byteBuffer.rewind();
            byteBuffer.limit(8 + tmpBuff.capacity());
            return byteBuffer;
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    /**获取icon md5值*/
    public ByteBuffer getPkgIconMd5(int response , String pkgName){
        try {
            MD5AndSignature md5 = new MD5AndSignature();
            String md5Sign = md5.getMessageDigest(ConvertUtil.Drawable2Bytes(context.getPackageManager().getPackageInfo(pkgName, 0).applicationInfo.loadIcon(context.getPackageManager())), MD5AndSignature.MD5);
            System.out.println("===md5 : "+md5Sign);
            return getReponseData(response,new String[]{"pkgName","md5"},new String[]{pkgName,md5Sign});
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }
}
