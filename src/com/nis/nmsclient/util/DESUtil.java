package com.nis.nmsclient.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * 通过DES加密解密实现一个String字符串的加密和解密.
 * 
 */
public class DESUtil {
	private final static String key = "longstar";
	
	/**
	 * DES加密方法
	 * 
	 */
	public static String desEncrypt(String message) throws Exception {
		Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");

		DESKeySpec desKeySpec = new DESKeySpec(key.getBytes("ASCII"));

		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
		IvParameterSpec iv = new IvParameterSpec(key.getBytes("ASCII"));
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

		byte data[] = message.getBytes("ASCII");
		byte[] encryptedData = cipher.doFinal(data);

		return getBASE64(encryptedData);
	}

	/**
	 * DES解密方法
	 * 
	 */
	public static String desDecrypt(String message) throws Exception {
		Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");

		DESKeySpec desKeySpec = new DESKeySpec(key.getBytes("ASCII"));

		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
		IvParameterSpec iv = new IvParameterSpec(key.getBytes("ASCII"));
		cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

		byte data[] = getFromBASE64(message);
		byte[] encryptedData = cipher.doFinal(data);

		return new String(encryptedData);
	}

	// 对base64编码的string解码成byte数组
	private static byte[] getFromBASE64(String s) {
		if (s == null)
			return null;
		BASE64Decoder decoder = new BASE64Decoder();
		try {
			byte[] b = decoder.decodeBuffer(s);
			return b;
		} catch (Exception e) {
			return null;
		}
	}

	//将 byte数组 进行 BASE64 编码
	private static String getBASE64(byte[] b) {
		if (b == null)
			return null;
		try {
			String returnstr = (new BASE64Encoder()).encode(b);
			return returnstr;
		} catch (Exception e) {
			return null;
		}
	}
}
