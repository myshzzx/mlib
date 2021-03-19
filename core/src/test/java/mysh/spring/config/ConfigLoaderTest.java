package mysh.spring.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mysh
 * @since 13-7-2 下午3:58
 */
public class ConfigLoaderTest {
	private static final Logger log = LoggerFactory.getLogger(ConfigLoaderTest.class);

	@Test
	public void aesTest() throws Exception {
		aesTestHelper("", "holder");
		aesTestHelper("content abd", "holder2");
		aesTestHelper("test测试明文。", "holder");
		aesTestHelper("很长很长的测试明文啊1234abcd很长很长的测试明文啊1234abcd很长很长的测试明文啊1234abcd。", "holder");
	}

	private void aesTestHelper(String content, String holder) throws Exception {
		String enc = ConfigLoader.aesEncrypt(holder, content);
		String dec = ConfigLoader.aesDecrypt(holder, enc);
		log.info("AES: 原文: " + content + ", 密文: " + enc + ", 解密文: " + dec);
		Assertions.assertEquals(content, dec);
	}


}
