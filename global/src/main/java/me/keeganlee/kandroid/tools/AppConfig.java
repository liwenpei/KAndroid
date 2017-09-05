package me.keeganlee.kandroid.tools;

import java.util.HashMap;
import java.util.Map;

import android.os.Environment;

public class AppConfig {
	public static Map<String,Object> Session = new HashMap<String,Object>();
	public final static String i4RootDir = Environment.getExternalStorageDirectory() + "/i4/"; 
	public final static String i4ImageDir = Environment.getExternalStorageDirectory() + "/i4/images/";
	public final static String i4ApkDir = Environment.getExternalStorageDirectory() + "/i4/apk/"; 
	public final static String i4LogDir = Environment.getExternalStorageDirectory() + "/i4/log/";
}
