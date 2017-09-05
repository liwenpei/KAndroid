package me.keeganlee.kandroid.tools;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

public final class MD5AndSignature {
	public static final String SHA1 = "sha";
	public static final String MD5 = "MD5";

	public final String getMessageDigest(byte[] paramArrayOfByte,
			String algorithm) {
		char[] arrayOfChar1 = { 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 97, 98,
				99, 100, 101, 102 };
		try {
			MessageDigest localMessageDigest = MessageDigest
					.getInstance(algorithm);
			localMessageDigest.update(paramArrayOfByte);
			byte[] arrayOfByte = localMessageDigest.digest();
			int i = arrayOfByte.length;
			char[] arrayOfChar2 = new char[i * 2];
			int j = 0;
			int k = 0;
			while (true) {
				if (j >= i)
					return new String(arrayOfChar2);
				int m = arrayOfByte[j];
				int n = k + 1;
				arrayOfChar2[k] = arrayOfChar1[(0xF & m >>> 4)];
				k = n + 1;
				arrayOfChar2[n] = arrayOfChar1[(m & 0xF)];
				j++;
			}
		} catch (Exception localException) {
		}
		return null;
	}

	/**
	 * ��ȡ�����ļ���MD5ֵ��
	 * 
	 * @param file
	 * @return
	 */

	public String getFileMD5(File file, String algorithm) {
		if (!file.isFile()) {
			return null;
		}
		MessageDigest digest = null;
		FileInputStream in = null;
		byte buffer[] = new byte[1024];
		int len;
		try {
			digest = MessageDigest.getInstance("MD5");
			in = new FileInputStream(file);
			while ((len = in.read(buffer, 0, 1024)) != -1) {
				digest.update(buffer, 0, len);
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		BigInteger bigInt = new BigInteger(1, digest.digest());
		return bigInt.toString(16);
	}
	

	public final byte[] getRawDigest(byte[] paramArrayOfByte,
			String algorithm) {
		try {
			MessageDigest localMessageDigest = MessageDigest
					.getInstance(algorithm);
			localMessageDigest.update(paramArrayOfByte);
			byte[] arrayOfByte = localMessageDigest.digest();
			return arrayOfByte;
		} catch (Exception localException) {
		}
		return null;
	}
}