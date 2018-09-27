package com.nis.nmsclient.thread.socket;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.log4j.Logger;

import com.nis.nmsclient.common.Contants;
import com.nis.nmsclient.util.DateUtil;
import com.nis.nmsclient.util.Utils;

public class SSLCertOper {
	static Logger logger = Logger.getLogger(SSLCertOper.class);

	public static SSLContext getSSLContext() throws Exception {
		// 初始化上下文
		SSLContext ctx = SSLContext.getInstance(Contants.SSL_JSSE_TYPE);
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		KeyStore ks = KeyStore.getInstance(Contants.SSL_KEYSTORE_TYPE);
		ks.load(new FileInputStream(Contants.SSL_KEY_STORE),
				Contants.SSL_KEY_STORE_PASS.toCharArray());// 载入keystore
		kmf.init(ks, Contants.SSL_KEY_PRIVATE_PASS.toCharArray());

		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		KeyStore tks = KeyStore.getInstance(Contants.SSL_KEYSTORE_TYPE);
		tks.load(new FileInputStream(Contants.SSL_TRUST_KEY_STORE),
				Contants.SSL_KEY_STORE_PASS.toCharArray());// 载入keystore
		tmf.init(tks);

		ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(),
				new SecureRandom());
		logger.debug("load keystore success.");

		return ctx;
	}

	/**
	 * 创建 密匙对（私钥和公钥）
	 * 
	 */
	public static String createKeyAndCert(String aliasName, String storePath,
			String localIp, String keyPass, String storePass, String certName) {
		BufferedReader bReader = null;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(
					"keytool -genkey -v -alias " + aliasName
							+ " -keyalg RSA -storetype "
							+ Contants.SSL_KEYSTORE_TYPE + " -keystore "
							+ storePath + " -validity 90 -dname \"CN="
							+ localIp
							+ ",OU=cn,O=cn,L=cn,ST=cn,C=cn\" -storepass "
							+ storePass + " -keypass " + keyPass);
			process.getOutputStream().close();
			bReader = new BufferedReader(new InputStreamReader(process
					.getInputStream()));
			process.getErrorStream().close();
			String line = null;
			while ((line = bReader.readLine()) != null) {
				System.out.println(line);
			}

			process = Runtime.getRuntime().exec(
					"keytool -export -alias " + aliasName + " -storetype "
							+ Contants.SSL_KEYSTORE_TYPE + " -keystore "
							+ storePath + " -file " + certName + " -storepass "
							+ storePass + "");

			bReader = new BufferedReader(new InputStreamReader(process
					.getInputStream()));
			while ((line = bReader.readLine()) != null) {
				System.out.println(line);
			}

			return certName;
		} catch (IOException e) {
			logger.error("Create a key pair error!");
			logger.error(Utils.printExceptionStack(e));
			return null;
		} finally {
			if (bReader != null) {
				try {
					bReader.close();
				} catch (IOException e) {
					logger.error(Utils.printExceptionStack(e));
				}
			}
		}

	}

	/**
	 * 将公钥引入KeyStore
	 * 
	 */
	public static boolean importCertToStore(String aliasName, String storePath,
			String certName, String storePass) {
		BufferedReader bReader = null;
		PrintWriter pw = null;
		try {
			Process process = Runtime.getRuntime().exec(
					"keytool -import -v -trustcacerts -alias " + aliasName
							+ " -keystore " + storePath + " -file " + certName
							+ " -storetype " + Contants.SSL_KEYSTORE_TYPE
							+ " -storepass " + storePass + "");

			bReader = new BufferedReader(new InputStreamReader(process
					.getInputStream()));
			pw = new PrintWriter(process.getOutputStream());

			pw.write("y");
			pw.flush();
			pw.close();

			String line = null;
			while ((line = bReader.readLine()) != null) {
				System.out.println(line);
			}

			return true;
		} catch (IOException e) {
			logger.error("Error of importing authentication certificate!");
			logger.error(Utils.printExceptionStack(e));
			return false;
		} finally {
			if (pw != null) {
				pw.close();
			}
			if (bReader != null) {
				try {
					bReader.close();
				} catch (IOException e) {
					logger.error(Utils.printExceptionStack(e));
				}
			}
		}

	}

	/**
	 * 删除KeyStore库中的密钥
	 * 
	 */
	public static boolean deleteKeyOrCertFromStore(String aliasName,
			String storePath, String storePass) {
		BufferedReader bReader = null;
		PrintWriter pw = null;
		try {
			Process process = Runtime.getRuntime().exec(
					"keytool -delete -v -alias " + aliasName + " -keystore "
							+ storePath + " -storetype "
							+ Contants.SSL_KEYSTORE_TYPE + " -storepass "
							+ storePass + "");

			bReader = new BufferedReader(new InputStreamReader(process
					.getInputStream()));
			pw = new PrintWriter(process.getOutputStream());

			// pw.write("y");
			pw.flush();
			pw.close();

			String line = null;
			while ((line = bReader.readLine()) != null) {
				System.out.println(line);
			}

			return true;
		} catch (IOException e) {
			logger.error("Delete" + storePath+ "library Key" + aliasName + "make a mistake！");
			logger.error(Utils.printExceptionStack(e));
			return false;
		} finally {
			if (pw != null) {
				pw.close();
			}
			if (bReader != null) {
				try {
					bReader.close();
				} catch (IOException e) {
					logger.error(Utils.printExceptionStack(e));
				}
			}
		}

	}

	/**
	 * test main
	 * 
	 * @time Aug 28, 2011-12:17:28 PM
	 * @param args
	 */
	public static void main(String args[]) {
		String newServerKeyName = "serverks"
				+ DateUtil.getCurrentDate(DateUtil.YYYYMMDD);
		String newServerKeyPsw = "123456";
		String newClientkeyName = "clientks"
				+ DateUtil.getCurrentDate(DateUtil.YYYYMMDD);
		String newClientkeyPsw = "123456";
		String filepath0 = SSLCertOper.createKeyAndCert(newServerKeyName,
				"D:\\workspace\\nms_client\\src\\key\\server_ks", "10.0.6.120",
				newServerKeyPsw, "server",
				"D:\\workspace\\nms_client\\src\\key\\server.cer");

		SSLCertOper.importCertToStore(newServerKeyName,
				"D:\\workspace\\nms_client\\src\\key\\client_ts",
				"D:\\workspace\\nms_client\\src\\key\\server.cer", "client");

		String filepath1 = SSLCertOper.createKeyAndCert(newClientkeyName,
				"D:\\workspace\\nms_client\\src\\key\\client_ks", "localhost",
				newClientkeyPsw, "client",
				"D:\\workspace\\nms_client\\src\\key\\client.cer");

		SSLCertOper.importCertToStore(newClientkeyName,
				"D:\\workspace\\nms_client\\src\\key\\server_ts",
				"D:\\workspace\\nms_client\\src\\key\\client.cer", "server");
		System.out.println(filepath0);
		System.out.println(filepath1);
		// Config.setValueByName("ssl.server.key.old",
		// Constants.SSL_SERVER_KEY_NEW);
		// Config.setValueByName("ssl.server.key.old.psw",
		// Constants.SSL_SERVER_KEY_NEW_PSW);
		// Config.setValueByName("ssl.server.key.new",newServerKeyName);
		// Config.setValueByName("ssl.server.key.new.psw", newServerKeyPsw);
		// Config.setValueByName("ssl.client.key",newClientkeyName);
		// Config.setValueByName("ssl.client.key.psw", newClientkeyPsw);
	}
}
