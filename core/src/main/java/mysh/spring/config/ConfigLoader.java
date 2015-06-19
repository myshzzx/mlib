
package mysh.spring.config;

import mysh.util.AESUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.Resource;

import java.util.Properties;

/**
 * Spring配置加载器。<br/>
 * 用于测试环境自动加载测试配置。JVM启动参数加 -Dtest=true 则认为是测试环境.<br/>
 * 支持配置自动 AES128 解密（需要 {@link #PROJ_AES_AUTO_DECRYPT} 配置为 true，配置名以 password 结尾）。<br/>
 *
 * @author 张智贤
 */
public class ConfigLoader extends PropertyPlaceholderConfigurer {

	private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);
	/**
	 * 测试环境声明 系统变量名。
	 */
	public static final String SYS_PROP_TEST = "test";
	/**
	 * 自动 AES128 解密配置名。
	 */
	protected static final String PROJ_AES_AUTO_DECRYPT = "aes.pw.autoDecrypt";
	/**
	 * AES 密钥长度.
	 */
	private static final int AES_KEY_SIZE = 128;
	/**
	 * AES加密种子.
	 */
	private static final String AES_ENCRYPT_SEED = "mysh";
	/**
	 * 自动解密 password 结尾的配置(AES)。
	 */
	private volatile Boolean isAESAutoDecrypt;
	private Resource[] testLocations = null;

	/**
	 * 当前是否测试环境.
	 */
	public static boolean isTestState() {

		return "true".equals(System.getProperty(SYS_PROP_TEST));
	}

	/**
	 * AES配置加密。
	 *
	 * @param placeholder 配置名
	 * @param content     配置明文内容。
	 */
	public static String aesEncrypt(String placeholder, String content) throws Exception {
		return AESUtil.encrypt(
						content.getBytes(), (AES_ENCRYPT_SEED + placeholder).toCharArray(),
						AES_ENCRYPT_SEED.getBytes(), AES_KEY_SIZE);
	}

	/**
	 * AES配置解密。
	 *
	 * @param placeholder 配置名。
	 * @param secContent  配置密文内容。
	 */
	public static String aesDecrypt(String placeholder, String secContent) throws Exception {
		return new String(AESUtil.decrypt(
						secContent, (AES_ENCRYPT_SEED + placeholder).toCharArray(),
						AES_ENCRYPT_SEED.getBytes(), AES_KEY_SIZE));
	}

	public void setTestLocations(Resource[] locations) {

		if (isTestState()) {
			super.setLocations(locations);
			this.testLocations = locations;
		}
	}

	@Override
	public void setLocation(Resource location) {

		if (!isTestState() || this.testLocations == null)
			super.setLocation(location);
	}

	@Override
	public void setLocations(Resource... locations) {

		if (!isTestState() || this.testLocations == null)
			super.setLocations(locations);
	}

	@Override
	protected String resolvePlaceholder(String placeholder, Properties props) {
		String value = super.resolvePlaceholder(placeholder, props);

//		aes start
		if (this.isAESAutoDecrypt == null) {
			this.isAESAutoDecrypt = "true".equals(super.resolvePlaceholder(PROJ_AES_AUTO_DECRYPT, props));
		}
		if (this.isAESAutoDecrypt && placeholder.endsWith("password")) {
			try {
				return aesDecrypt(placeholder, value);
			} catch (Exception e) {
				log.error("解密失败: " + placeholder + ", " + value, e);
			}
		}
//		aes end

		return value;
	}
}
