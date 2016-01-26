package mysh.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * DbType
 *
 * @author mysh
 * @since 2015/10/9
 */
public class DbUtil {
	private static final Logger log = LoggerFactory.getLogger(DbUtil.class);

	private static boolean isOracle;

	static {
		try {
			Properties prop = new Properties();
			prop.load(DbUtil.class.getClassLoader().getResourceAsStream("config.properties"));
			String dbType = prop.getProperty("db.type");
			isOracle = dbType.equalsIgnoreCase("oracle");
		} catch (IOException e) {
			log.error("load properties error.", e);
		}
	}

	public static boolean isOracle() {
		return isOracle;
	}
}
