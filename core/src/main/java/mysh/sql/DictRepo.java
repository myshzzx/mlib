package mysh.sql;

/**
 * DictRepo
 *
 * @author mysh
 * @since 2016/2/24
 */
public interface DictRepo {
	/**
	 * @param dictKey   字典类
	 * @param itemValue 字典值
	 * @return 字典值描述
	 */
	String getDesc(String dictKey, Object itemValue);
}
