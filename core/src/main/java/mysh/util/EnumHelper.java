package mysh.util;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 枚举帮助类.
 * 注意: 枚举可以作为 SDK 组件提供, 但不应作为 API 接口的一部分, 因存在序列化兼容问题.
 * <p>
 * 基本用法示例:
 * <pre>
 * public enum CustomEnum implements EnumHelper<Integer> {
 *   AGREE(1), NONE(0);
 *
 *   CustomEnum(int code, String desc) {
 *     EnumHelper.register(this.getClass(), this, code, desc);
 *   }
 *
 *   public static CustomEnum fromCode(Integer code) {
 *     return EnumHelper.fromCode(CustomEnum.class, code, NONE);
 *   }
 * }
 * </pre>
 *
 * @since 2018/01/31
 */
public interface EnumHelper<CT> {
	
	/**
	 * 禁止访问
	 */
	@Deprecated
	Map<Class<? extends Enum>, Map<Object, Enum>> codeCache = new ConcurrentHashMap<>();
	/**
	 * 禁止访问
	 */
	@Deprecated
	Map<Class<? extends Enum>, Map<Enum, Pair<Object, String>>> infoCache = new ConcurrentHashMap<>();
	
	static void register(Class<? extends Enum> type, Enum inst, int code, String desc) {
		Enum prev = codeCache.computeIfAbsent(type, t -> new ConcurrentHashMap<>()).put(code, inst);
		if (prev != null) {
			throw new RuntimeException("code-conflict:" + type + "." + inst.toString());
		}
		infoCache.computeIfAbsent(type, t -> new ConcurrentHashMap<>()).put(inst, Pair.of(code, desc));
	}
	
	static <T extends Enum> T fromCode(Class<? extends Enum> type, Object code, Enum defaultVal) {
		Map<Object, Enum> codeMap = codeCache.get(type);
		if (codeMap == null) {
			throw new RuntimeException("type-not-registered:" + type);
		} else {
			return (T) (code == null ? defaultVal : ObjectUtils.firstNonNull(codeMap.get(code), defaultVal));
		}
	}
	
	default CT getCode() {
		Map<Enum, Pair<Object, String>> enumInfoMap = infoCache.get(getClass());
		if (enumInfoMap == null) {
			throw new RuntimeException("type-not-registered:" + getClass());
		} else {
			Pair<Object, String> info = enumInfoMap.get(this);
			if (info == null) {
				throw new RuntimeException("inst-not-registered:" + getClass() + "." + toString());
			} else {
				return (CT) info.getKey();
			}
		}
	}
	
	default String getDesc() {
		Map<Enum, Pair<Object, String>> enumInfoMap = infoCache.get(getClass());
		if (enumInfoMap == null) {
			throw new RuntimeException("type-not-registered:" + getClass());
		} else {
			Pair<Object, String> info = enumInfoMap.get(this);
			if (info == null) {
				throw new RuntimeException("inst-not-registered:" + getClass() + "." + toString());
			} else {
				return info.getValue() != null ? info.getValue() : toString();
			}
		}
	}
	
}
