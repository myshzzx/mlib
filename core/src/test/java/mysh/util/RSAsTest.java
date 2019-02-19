package mysh.util;

import org.junit.Before;
import org.junit.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import static mysh.util.RSAs.*;
import static org.junit.Assert.*;

public class RSAsTest {

	byte[] kPairByte;
	byte[] c = "mysh is a great man".getBytes();

	byte[] enc;
	byte[] rsaEnc;

	byte[] rsaSign;

	@Before
	public void init() throws IOException, NoSuchAlgorithmException, BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidKeySpecException {
		KPair kp = genKeyPair(1034);
		kPairByte = Serializer.BUILD_IN.serialize(kp);

		assertEquals(kp.getRSAPubKey().getModulus(), trans(kp.getPubKey()).getModulus());
		assertEquals(kp.getRSAPubKey().getPublicExponent(), trans(kp.getPubKey()).getPublicExponent());
		assertEquals(kp.getRSAPriKey().getModulus(), trans(kp.getPriKey()).getModulus());
		assertEquals(kp.getRSAPriKey().getPrivateExponent(), trans(kp.getPriKey()).getPrivateExponent());

		enc = doCipher(kp.getPubKey(), c);
		rsaEnc = enc(kp.getRSAPubKey(), c);
		assertFalse(Arrays.equals(enc, rsaEnc));

		rsaSign = sign(kp.getRSAPriKey(), c);
	}

	private KPair getKPair() throws IOException, ClassNotFoundException {
		return Serializer.BUILD_IN.deserialize(kPairByte, null);
	}

	@Test
	public void encTest() throws IOException, ClassNotFoundException, BadPaddingException,
					NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidKeySpecException {
		KPair kp = getKPair();
		assertArrayEquals(c, unEnc(kp.getRSAPriKey(), enc));
		assertArrayEquals(c, unEnc(kp.getRSAPriKey(), rsaEnc));
		assertArrayEquals(c, doCipher(kp.getPriKey(), enc));
		assertArrayEquals(c, doCipher(kp.getPriKey(), rsaEnc));
	}

	@Test
	public void signTest() throws IOException, ClassNotFoundException, BadPaddingException,
					NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidKeySpecException {
		KPair kp = getKPair();
		assertArrayEquals(c, unSign(kp.getRSAPubKey(), rsaSign));
	}
}
