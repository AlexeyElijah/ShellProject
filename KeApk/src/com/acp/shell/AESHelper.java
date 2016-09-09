package com.acp.shell;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by alexey on 16-8-16.
 */

public class AESHelper {               
	public final String HEX_KEY ;

	public boolean decrypt(String sourceFilePath, String targetFilePath) {
		HEX_KEY = NDKUtils.getKeyFormC();
		boolean result = false;
		FileChannel sourceFC = null;
		FileChannel targetFC = null;
		try {

			Cipher mCipher = Cipher.getInstance("AES/CFB/NoPadding");

			byte[] rawkey = hexStringToByte(HEX_KEY);
			File sourceFile = new File(sourceFilePath);
			File targetFile = new File(targetFilePath);

			sourceFC = new RandomAccessFile(sourceFile, "r").getChannel();
			targetFC = new RandomAccessFile(targetFile, "rw").getChannel();

			SecretKeySpec secretKey = new SecretKeySpec(rawkey, "AES");

			mCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(new byte[mCipher.getBlockSize()]));

			ByteBuffer byteData = ByteBuffer.allocate(1024);
			while (sourceFC.read(byteData) != -1) {
				// 通过通道读写交叉进行。
				// 将缓冲区准备为数据传出状态
				byteData.flip();

				byte[] byteList = new byte[byteData.remaining()];
				byteData.get(byteList, 0, byteList.length);
				// 此处，若不使用数组加密解密会失败，因为当byteData达不到1024个时，加密方式不同对空白字节的处理也不相同，从而导致成功与失败。
				byte[] bytes = mCipher.doFinal(byteList);
				targetFC.write(ByteBuffer.wrap(bytes));
				byteData.clear();
			}
			result = true;
		} catch (Exception e) {
			System.out.println(e.getMessage());

		} finally {
			try {
				if (sourceFC != null) {
					sourceFC.close();
				}
				if (targetFC != null) {
					targetFC.close();
				}
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
		return result;
	}


	/**
	 * 结合密钥生成加密后的密文
	 *
	 * @param raw
	 * @param input
	 * @return
	 * @throws Exception
	 */
	private byte[] encrypt(byte[] raw, byte[] input) throws Exception {
		// 根据上一步生成的密匙指定一个密匙
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		// Cipher cipher = Cipher.getInstance("AES");
		// 加密算法，加密模式和填充方式三部分或指定加密算
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		// 初始化模式为加密模式，并指定密匙
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(new byte[cipher.getBlockSize()]));
		byte[] encrypted = cipher.doFinal(input);
		return encrypted;
	}
	
	/**
	 * 把16进制字符串转换成字节数组
	 * 
	 * @param hexString
	 * @return byte[]
	 */
	public static byte[] hexStringToByte(String hex) {
		int len = (hex.length() / 2);
		byte[] result = new byte[len];
		char[] achar = hex.toCharArray();
		for (int i = 0; i < len; i++) {
			int pos = i * 2;
			result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
		}
		return result;
	}

	private static int toByte(char c) {
		byte b = (byte) "0123456789ABCDEF".indexOf(c);
		return b;
	}
}
