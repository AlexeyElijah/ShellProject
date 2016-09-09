package dodola.patch;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by heng.liu on 16-8-16.
 */

public class AESHelper {
	public static final String SEED = "123qweasd";

	public boolean encrypt(String sourceFilePath, String targetFilePath) {
		boolean result = false;
		FileChannel sourceFC = null;
		FileChannel targetFC = null;
		try {

			Cipher mCipher = Cipher.getInstance("AES/CFB/NoPadding");

			byte[] rawkey = getRawKey(SEED.getBytes());
			String hexKeyString = bytesToHexString(rawkey);
			System.out.println("hexKeyString  =  " + hexKeyString);
			File sourceFile = new File(sourceFilePath);
			File targetFile = new File(targetFilePath);

			sourceFC = new RandomAccessFile(sourceFile, "r").getChannel();
			targetFC = new RandomAccessFile(targetFile, "rw").getChannel();

			SecretKeySpec secretKey = new SecretKeySpec(rawkey, "AES");

			mCipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(new byte[mCipher.getBlockSize()]));

			ByteBuffer byteData = ByteBuffer.allocate(1024);
			while (sourceFC.read(byteData) != -1) {
				byteData.flip();

				byte[] byteList = new byte[byteData.remaining()];
				byteData.get(byteList, 0, byteList.length);
				byte[] bytes = mCipher.doFinal(byteList);
				targetFC.write(ByteBuffer.wrap(bytes));
				byteData.clear();
			}
			result = true;
		} catch (Exception e) {
			System.out.println("加密错误  "+e.getMessage());

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

	private byte[] getRawKey(byte[] seed) throws NoSuchAlgorithmException

	{
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
		sr.setSeed(seed);
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(128, sr);
		SecretKey key = keyGen.generateKey();
		byte[] raw = key.getEncoded();
		return raw;
	}

	public static final String bytesToHexString(byte[] bArray) {
		StringBuffer sb = new StringBuffer(bArray.length);
		String sTemp;
		for (int i = 0; i < bArray.length; i++) {
			sTemp = Integer.toHexString(0xFF & bArray[i]);
			if (sTemp.length() < 2)
				sb.append(0);
			sb.append(sTemp.toUpperCase());
		}
		return sb.toString();
	}
}
