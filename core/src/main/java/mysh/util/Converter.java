package mysh.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import mysh.codegen.CodeUtil;

import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mysh
 * @since 2020-04-01
 */
public abstract class Converter {
	public static Object stringConvert(Class<?> type, String value) throws Exception {
		
		// 是基本类型
		if (type == String.class) {
			return value;
		} else if (Strings.isBlank(value))
			return null;
		
		if (type == int.class || type == Integer.class) {
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
	
	public static Map<String, Object> object2Map(Object obj) {
		return object2Map(obj, false);
	}
	
	public static Map<String, Object> object2Map(Object obj, boolean toUnderlineKey) {
		JSONObject json = (JSONObject) JSON.toJSON(obj);
		if (toUnderlineKey) {
			Map<String, Object> map = new HashMap<>();
			for (Map.Entry<String, Object> je : json.entrySet()) {
				map.put(CodeUtil.camel2underline(je.getKey()).toLowerCase(), je.getValue());
			}
			return map;
		} else
			return json;
	}
	
	public static <T> T map2Object(Map<String, ?> map, Class<T> type) {
		return map2Object(map, false, type);
	}
	
	public static <T> T map2Object(Map<String, ?> map, boolean isUnderlineKey, Class<T> type) {
		if (isUnderlineKey) {
			Map<String, Object> newMap = new HashMap<>();
			for (Map.Entry<String, ?> me : map.entrySet()) {
				newMap.put(CodeUtil.underline2camel(me.getKey(), true), me.getValue());
			}
			map = newMap;
		}
		return JSON.toJavaObject((JSONObject) JSON.toJSON(map), type);
	}
	
}
