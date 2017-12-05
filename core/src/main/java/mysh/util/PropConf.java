
package mysh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

/**
 * 配置数据提取器.
 *
 * @author ZhangZhx
 */
public class PropConf {

    private static final Logger log = LoggerFactory.getLogger(PropConf.class);
    private Properties props;

    public PropConf() {
        this.props = new Properties();
    }

    /**
     * 取配置进行初始化.
     *
     * @param filePath 配置文件.
     * @throws java.io.IOException 文件不存在或不可读.
     */
    public PropConf(String filePath) throws IOException {
        this();

        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            try (Reader i = new FileReader(file)) {
                props.load(i);
            }
        }
    }

    /**
     * 用给定配置初始化.
     */
    public PropConf(Properties props) {

        this.props = (Properties) props.clone();
    }

    /**
     * save properties to file.
     *
     * @throws java.io.IOException file write error.
     */
    public void save2File(String filePath) throws IOException {
        try (Writer o = new FileWriter(filePath)) {
            props.store(o, null);
        }
    }

    /**
     * put prop value.
     */
    public void putProp(String propName, Object value) {
        this.props.put(propName, value == null ? "" : value.toString());
    }

    /**
     * 取配置property（不返回 null 值），若property未定义，写日志.
     *
     * @param propName property名
     */
    public String getPropString(String propName) {

        return this.getPropString(propName, "");
    }

    /**
     * 取配置property（不返回 null 值），若property未定义，写日志.
     *
     * @param propName     property名
     * @param defaultValue 默认值，为 null 无效
     */
    public String getPropString(String propName, String defaultValue) {

        String value = this.props.getProperty(propName);
        if (value == null) {
            defaultValue = defaultValue == null ? "" : defaultValue;
            log.info("property [" + propName + "] undefined, use default value: " + defaultValue);
            return defaultValue;
        }
        return value;
    }

    /**
     * 取配置property的整型值. 若property未定义，写日志, 返回默认值 0.
     *
     * @param propName property名
     */
    public int getPropInt(String propName) {

        return getPropInt(propName, 0);
    }

    /**
     * 取配置property的整型值. 若property未定义，返回默认值.
     *
     * @param propName     property名
     * @param defaultValue 默认值
     */
    public int getPropInt(String propName, int defaultValue) {

        try {
            final String p = this.getPropString(propName);
            if (p.length() == 0) return defaultValue;
            else return Integer.parseInt(p);
        } catch (Exception e) {
            log.info("property [" + propName + "] defined error, use default value: " + defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * 取配置property的长整型值，若property未定义，写日志, 返回默认值 0.
     *
     * @param propName property名
     */
    public long getPropLong(String propName) {

        return getPropLong(propName, 0);
    }

    public long getPropLong(String propName, long defaultValue) {

        try {
            final String p = this.getPropString(propName);
            if (Strings.isBlank(p))
                return defaultValue;
            else
                return Long.parseLong(p);
        } catch (Exception e) {
            log.info("property [" + propName + "] defined error, use default value: " + defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * 取配置property的bool值，若property未定义，写日志, 返回默认值 false.
     *
     * @param propName property名
     */
    public boolean getPropBoolean(String propName) {
        return getPropBoolean(propName, false);
    }

    public boolean getPropBoolean(String propName, boolean defaultValue) {

        try {
            final String p = this.getPropString(propName);
            if (Strings.isBlank(p))
                return defaultValue;
            else
                return Boolean.parseBoolean(p);
        } catch (Exception e) {
            log.info("property [" + propName + "] defined error, use default value: " + defaultValue, e);
            return defaultValue;
        }
    }
}
