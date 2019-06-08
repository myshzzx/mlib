package mysh.util;

import com.alibaba.fastjson.annotation.JSONField;
import mysh.codegen.CodeUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;


/**
 * 扁平数据对象到自定义类型对象拼装器.
 * 如 resultSet -> CustomVO
 * CustomVO 属性对应的键名默认格式为小写下划线, 支持 com.alibaba.fastjson.annotation.JSONField
 * 自定义对象的属性填充支持继承和组合属性.
 *
 * @since 2018/04/01
 */
public abstract class FlatObjectAssembler<F> {
	/**
	 * list all flat keys
	 */
	protected abstract Iterable<String> flatKeys(F f);
	
	/**
	 * get flat value from F and String
	 */
	protected abstract BiFunction<F, String, Object> flatValueGetter(Class fieldType);
	
	protected String getFlatKey(Field f) {
		JSONField jsonAnno = f.getAnnotation(JSONField.class);
		return jsonAnno != null ?
				       jsonAnno.name() :
				       CodeUtil.camel2underline(f.getName()).toLowerCase();
	}
	
	public <T> T convert(F f, Class<T> type)
			throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		if (f == null) {
			return null;
		}
		Map<String, FieldWriter> typeFields = getTypeFields(type);
		T m = type.getConstructor().newInstance();
		flatAssemble(f, m, typeFields);
		return m;
	}
	
	/**
	 * @param f          flat object
	 * @param model      instance
	 * @param typeFields include fields defined in recurring super classes,
	 *                   which can be primitive type or complex type.
	 */
	private void flatAssemble(F f, Object model, Map<String, FieldWriter> typeFields)
			throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		for (String key : flatKeys(f)) {
			FieldWriter fw = typeFields.get(key);
			if (fw != null) {
				if (fw.isWritable()) {
					fw.writeField(f, model);
				} else {
					Object mf = fw.f.getType().getConstructor().newInstance();
					fw.f.set(model, mf);
					flatAssemble(f, mf, getTypeFields(fw.f.getType()));
				}
			}
		}
	}
	
	private class FieldWriter {
		Field f;
		String flatKey;
		BiFunction<F, String, Object> valueGetter;
		
		void writeField(F r, Object model) throws IllegalAccessException {
			if (valueGetter != null) {
				Object value = valueGetter.apply(r, flatKey);
				if (value != null) {
					f.set(model, value);
				}
			}
		}
		
		boolean isWritable() {
			return valueGetter != null;
		}
	}
	
	private FieldWriter toFieldWriter(Field f) {
		FieldWriter fw = new FieldWriter();
		fw.f = f;
		fw.flatKey = this.getFlatKey(f);
		fw.valueGetter = this.flatValueGetter(f.getType());
		return fw;
	}
	
	private Map<Class, Map<String, FieldWriter>> classFieldWriters = new ConcurrentHashMap<>();
	
	/**
	 * get fields defined by type (and recurred super classes).
	 * fields with same name in super classes will be covered  by sub class.
	 */
	private Map<String, FieldWriter> getTypeFields(final Class<?> type) {
		Map<String, FieldWriter> fws = classFieldWriters.get(type);
		if (fws == null) {
			Stack<Class<?>> types = new Stack<>();
			Class<?> t = type;
			while (t != null) {
				types.push(t);
				t = t.getSuperclass();
			}
			
			fws = new HashMap<>();
			while (!types.isEmpty()) {
				t = types.pop();
				Field[] dfs = t.getDeclaredFields();
				for (Field df : dfs) {
					if ((df.getModifiers() & Modifier.FINAL) > 0
							    || (df.getModifiers() & Modifier.STATIC) > 0) {
						continue;
					}
					df.setAccessible(true);
					FieldWriter fw = toFieldWriter(df);
					fws.put(fw.flatKey, fw);
				}
			}
			classFieldWriters.put(type, fws);
		}
		return fws;
	}
}
