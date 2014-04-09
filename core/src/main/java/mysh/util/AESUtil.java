package mysh.util;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.AlgorithmParameters;
import java.security.spec.KeySpec;

/**
 * AES 加解密。
 *
 * @author Mysh
 * @since 13-7-2 下午4:10
 */
public class AESUtil {

	/**
	 * AES128 加密。
	 *
	 * @param content  明文。
	 * @param password 密码。
	 * @param salt     盐巴。
	 * @throws Exception
	 */
	public static String encrypt(byte[] content, char[] password, byte[] salt) throws Exception {
		return encrypt(content, password, salt, 128);
	}

	/**
	 * 加密。
	 *
	 * @param content  明文。
	 * @param password 密码。
	 * @param salt     盐巴。
	 * @param keySize  密钥长度. 可选 128,192,256.
	 * @throws Exception
	 */
	public static String encrypt(byte[] content, char[] password, byte[] salt, int keySize) throws Exception {

		SecretKey secret = genSecretKey(password, salt, keySize);
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, secret);

		AlgorithmParameters params = cipher.getParameters();
		byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();

		byte[] enc = cipher.doFinal(content);
		byte[] result = new byte[enc.length + iv.length];
		System.arraycopy(enc, 0, result, 0, enc.length);
		System.arraycopy(iv, 0, result, enc.length, iv.length);
		return Base64.encodeBase64String(result);
	}

	/**
	 * AES-128 解密。
	 *
	 * @param encContent 密文。
	 * @param password   密码。
	 * @param salt       盐巴。
	 * @throws Exception
	 */
	public static byte[] decrypt(String encContent, char[] password, byte[] salt) throws Exception {
		return decrypt(encContent, password, salt, 128);
	}

	/**
	 * 解密。
	 *
	 * @param encContent 密文。
	 * @param password   密码。
	 * @param salt       盐巴。
	 * @param keySize    密钥长度. 可选 128,192,256.
	 * @throws Exception
	 */
	public static byte[] decrypt(String encContent, char[] password, byte[] salt, int keySize) throws Exception {
		byte[] enc = Base64.decodeBase64(encContent);

		byte[] encContentByte = new byte[enc.length - 16];
		byte[] encIV = new byte[16];

		System.arraycopy(enc, 0, encContentByte, 0, encContentByte.length);
		System.arraycopy(enc, encContentByte.length, encIV, 0, encIV.length);

		SecretKey secret = genSecretKey(password, salt, keySize);

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(encIV));

		return cipher.doFinal(encContentByte);
	}

	/**
	 * 生成Key。
	 *
	 * @param password 密码。
	 * @param salt     盐巴。
	 * @param keySize  密钥长度.
	 * @throws Exception
	 */
	private static SecretKey genSecretKey(char[] password, byte[] salt, int keySize) throws Exception {
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		KeySpec spec = new PBEKeySpec(password, salt, 65536, keySize);
		return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
	}

}
