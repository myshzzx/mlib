
package mysh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 配置数据提取器.
 *
 * @author ZhangZhx
 */
public class PropConf {

	private static final Logger log = LoggerFactory.getLogger(PropConf.class);
	private Properties props;

	/**
	 * 取默认配置进行初始化: myshlib.properties.
	 *
	 * @param filename 配置文件名. 为空则使用默认.
	 */
	public PropConf(String filename) {

		Properties props = new Properties();
		// InputStream conf = new FileInputStream(DBConnPool.class.getClassLoader()
		// .getResource("mycrawler.properties").getPath());
		InputStream conf = null;
		try {
			filename = (filename == null) ? "myshlib.properties" : filename;
			conf = new FileInputStream(filename);
			props.load(conf);
			this.props = props;
		} catch (Exception e) {
			log.error("取默认配置异常, 使用空配置. " + e);
			this.props = new Properties();
		} finally {
			if (conf != null)
				try {
					conf.close();
				} catch (IOException e) {
				}
		}

	}

	/**
	 * 用给定配置初始化.
	 *
	 */
	public PropConf(Properties props) {

		this.props = (Properties) props.clone();
	}

	/**
	 * 取配置属性（不返回 null 值），若属性未定义，写日志.
	 *
	 * @param propName 属性名
	 */
	public String getPropString(String propName) {

		return this.getPropString(propName, "");
	}

	/**
	 * 取配置属性（不返回 null 值），若属性未定义，写日志.
	 *
	 * @param propName     属性名
	 * @param defaultValue 默认值，为 null 无效
	 */
	public String getPropString(String propName, String defaultValue) {

		String value = this.props.getProperty(propName);
		if (value == null) {
			log.info("属性 [" + propName + "] 未定义，使用默认值");
			return defaultValue == null ? "" : defaultValue;
		}
		return value;
	}

	/**
	 * 取配置属性的整型值. 若属性未定义，写日志, 返回默认值 0.
	 *
	 * @param propName 属性名
	 */
	public int getPropInt(String propName) {

		try {
			return Integer.parseInt(this.getPropString(propName));
		} catch (Exception e) {
			log.info("属性 [" + propName + "] 定义异常，使用默认值");
			return 0;
		}
	}

	/**
	 * 取配置属性的整型值. 若属性未定义，写日志, 返回默认值.
	 *
	 * @param propName     属性名
	 * @param defaultValue 默认值
	 */
	public int getPropInt(String propName, int defaultValue) {

		try {
			return Integer.parseInt(this.getPropString(propName));
		} catch (Exception e) {
			log.info("属性 [" + propName + "] 定义异常，使用默认值");
			return defaultValue;
		}
	}

	/**
	 * 取配置属性的长整型值，若属性未定义，写日志.
	 *
	 * @param propName 属性名
	 */
	public long getPropLong(String propName) {

		try {
			return Long.parseLong(this.getPropString(propName));
		} catch (Exception e) {
			log.info("属性 [" + propName + "] 定义异常，使用默认值");
			return 0L;
		}
	}
}
