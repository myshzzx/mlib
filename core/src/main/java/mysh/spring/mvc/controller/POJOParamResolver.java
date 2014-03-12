package mysh.spring.mvc.controller;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * POJO参数自动装配.
 * User: Allen
 * Time: 13-5-29 上午8:46
 */
public class POJOParamResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter param) {
		return param.getParameterAnnotation(mysh.spring.mvc.controller.POJOResolve.class) != null;
	}

	@Override
	public Object resolveArgument(MethodParameter param, ModelAndViewContainer mav,
	                              NativeWebRequest req, WebDataBinderFactory bf) throws Exception {

		mysh.spring.mvc.controller.POJOResolve anno = param.getParameterAnnotation(mysh.spring.mvc.controller.POJOResolve.class);
		String paramPrefix = (anno.value() == null || anno.value().isEmpty() ? param.getParameterName() : anno.value());

		Object obj = param.getParameterType().newInstance();

		String[] paramPath;
		for (Map.Entry<String, String[]> e : req.getParameterMap().entrySet()) {
			paramPath = e.getKey().split("\\.");
			if (paramPath.length > 1 && paramPath[0].equals(paramPrefix) && e.getValue().length > 0) {
				this.setProperty(paramPath, 1, obj, e.getValue());
			}
		}

		if (anno.validate()) {

		}

		return obj;
	}

	/**
	 * 为 path 描述的 obj 递归属性设置值.<br/>
	 * 私有方法，为执行效率的关系不做参数有效性检查。
	 *
	 * @param path  对象属性路径
	 * @param i     当前 path 索引位置
	 * @param obj   对象
	 * @param value 值
	 */
	private void setProperty(String[] path, int i, Object obj, String[] value) throws Exception {
		if ("class".equals(path[i]))
			throw new RuntimeException("无效属性:" + path[i]);

		final PropertyDescriptor desc = new PropertyDescriptor(path[i], obj.getClass());
		Method readMethod = desc.getReadMethod();
		Method writeMethod = desc.getWriteMethod();
		Class<?> type = readMethod.getReturnType();

		if (i == path.length - 1) {

			Object paramValue = null;

			// 是多值
			if (value.length > 1) {
				if (Collection.class.isAssignableFrom(type)) { // 是集合属性
					Collection<String> tColl = Arrays.asList(value);
					if (type.isInterface()) { // 是接口属性
						if (Set.class.isAssignableFrom(type)) {
							paramValue = new HashSet(tColl);
						} else if (List.class.isAssignableFrom(type)) {
							paramValue = new ArrayList(tColl);
						}
					} else { // 不是接口属性
						paramValue = type.newInstance();
						((Collection<String>) paramValue).addAll(tColl);
					}
				} else { // 不是集合属性
					throw new RuntimeException("集合值装配失败：" + Arrays.toString(path) + " = " + Arrays.toString(value));
				}
			}

			String tValue = value[0];

			// 是基本类型
			if (type == String.class) {
				paramValue = tValue;
			} else if (type == int.class || type == Integer.class) {
				paramValue = Integer.valueOf(tValue);
			} else if (type == long.class || type == Long.class) {
				paramValue = Long.valueOf(tValue);
			} else if (type == double.class || type == Double.class) {
				paramValue = Double.valueOf(tValue);
			} else if (type == byte.class || type == Byte.class) {
				paramValue = Byte.valueOf(tValue);
			} else if (type == short.class || type == Short.class) {
				paramValue = Short.valueOf(tValue);
			} else if (type == float.class || type == Float.class) {
				paramValue = Float.valueOf(tValue);
			}

			// 是时间值
			if (Date.class.isAssignableFrom(type)) {
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
				paramValue = type.newInstance();
				((Date) paramValue).setTime(dateFormat.parse(tValue).getTime());
			}

			if (paramValue == null) {
				throw new RuntimeException("不支持的装配类型：" + type.toString());
			}
			writeMethod.invoke(obj, paramValue);
		} else {
			Object param = readMethod.invoke(obj);
			if (param == null) {
				param = type.newInstance();
				writeMethod.invoke(obj, param);
			}
			this.setProperty(path, i + 1, param, value);
		}
	}
}
