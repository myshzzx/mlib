package mysh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.Serializable;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

/**
 * @author Mysh
 * @since 2014/10/20 17:53
 */
public class RSAUtil {
	private static final Logger log = LoggerFactory.getLogger(RSAUtil.class);

	private static final String UN_SUPPORTED_MSG = "RSA unsupported.";
	private static KeyFactory keyFact;

	static {
		try {
			keyFact = KeyFactory.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			log.error(UN_SUPPORTED_MSG, e);
		}
	}

	public static class KPair implements Serializable {
		private static final long serialVersionUID = -3382789318351488100L;

		private final PublicKey pubKey;
		private final PrivateKey priKey;

		public KPair(PublicKey pubKey, PrivateKey priKey) {
			this.pubKey = pubKey;
			this.priKey = priKey;
		}

		public PublicKey getPubKey() {
			return pubKey;
		}

		public PrivateKey getPriKey() {
			return priKey;
		}

		public RSAPublicKeySpec getRSAPubKey() {
			try {
				return trans(pubKey);
			} catch (InvalidKeySpecException e) {
				return null;
			}
		}

		public RSAPrivateKeySpec getRSAPriKey() {
			try {
				return trans(priKey);
			} catch (InvalidKeySpecException e) {
				return null;
			}
		}
	}

	/**
	 * generate RSA key pair.
	 * return <code>null</code> if RSA unsupported.
	 */
	public static KPair genKeyPair(int keySize) {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(keySize);
			KeyPair kp = kpg.genKeyPair();
			return new KPair(kp.getPublic(), kp.getPrivate());
		} catch (NoSuchAlgorithmException e) {
			log.error(UN_SUPPORTED_MSG, e);
			return null;
		}
	}

	/**
	 * do rsa cipher.
	 */
	public static byte[] doCipher(Key key, byte[] data) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(key instanceof PublicKey ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key);
		return cipher.doFinal(data);
	}

	/**
	 * encrypt data with rsa public key.
	 */
	public static byte[] enc(RSAPublicKeySpec rsaPubKey, byte[] data)
					throws InvalidKeySpecException, NoSuchAlgorithmException, BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeyException {
		PublicKey pubKey = keyFact.generatePublic(rsaPubKey);
		return doCipher(pubKey, data);
	}

	/**
	 * decrypt data with rsa private key.
	 */
	public static byte[] unEnc(RSAPrivateKeySpec rsaPriKey, byte[] data)
					throws NoSuchAlgorithmException, BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidKeySpecException {
		PrivateKey priKey = keyFact.generatePrivate(rsaPriKey);
		return doCipher(priKey, data);
	}

	/**
	 * sign data with rsa private key.
	 */
	public static byte[] sign(RSAPrivateKeySpec rsaPriKey, byte[] data)
					throws NoSuchAlgorithmException, BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidKeySpecException {
		return enc(new RSAPublicKeySpec(rsaPriKey.getModulus(), rsaPriKey.getPrivateExponent()), data);
	}

	/**
	 * verify sign with rsa public key.
	 */
	public static byte[] unSign(RSAPublicKeySpec rsaPubKey, byte[] data)
					throws InvalidKeySpecException, NoSuchAlgorithmException, BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeyException {
		return unEnc(new RSAPrivateKeySpec(rsaPubKey.getModulus(), rsaPubKey.getPublicExponent()), data);
	}

	public static RSAPublicKeySpec trans(PublicKey pubKey) throws InvalidKeySpecException {
		return keyFact.getKeySpec(pubKey, RSAPublicKeySpec.class);
	}

	public static RSAPrivateKeySpec trans(PrivateKey priKey) throws InvalidKeySpecException {
		return keyFact.getKeySpec(priKey, RSAPrivateKeySpec.class);
	}

	public static PublicKey trans(RSAPublicKeySpec pubKey) throws InvalidKeySpecException {
		return keyFact.generatePublic(pubKey);
	}

	public static PrivateKey trans(RSAPrivateKeySpec priKey) throws InvalidKeySpecException {
		return keyFact.generatePrivate(priKey);
	}

}
