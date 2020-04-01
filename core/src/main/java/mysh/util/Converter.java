package mysh.util;

import java.time.ZoneOffset;
import java.util.Date;

/**
 * @author mysh
 * @since 2020-04-01
 */
public abstract class Converter {
	public static Object stringConvert(Class<?> type, String value) throws Exception {
		
		// 是基本类型
		if (type == String.class) {
			return value;
		} else if (type == int.class || type == Integer.class) {
			return Integer.valueOf(value);
		} else if (type == long.class || type == Long.class) {
			return Long.valueOf(value);
		} else if (type == double.class || type == Double.class) {
			return Double.valueOf(value);
		} else if (type == byte.class || type == Byte.class) {
			return Byte.valueOf(value);
		} else if (type == short.class || type == Short.class) {
			return Short.valueOf(value);
		} else if (type == float.class || type == Float.class) {
			return Float.valueOf(value);
		} else if (type == boolean.class || type == Boolean.class) {
			return Boolean.valueOf(value);
		}
		
		// 是时间值
		if (Date.class.isAssignableFrom(type)) {
			Date d = (Date) type.getConstructor().newInstance();
			d.setTime(Times.parseDayTime(Times.Formats.DayTime, value).atZone(ZoneOffset.systemDefault()).toEpochSecond() * 1000);
			return d;
		}
		
		return null;
	}
}
