package mysh.util;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Mysh
 * @since 2015/1/26 22:04
 */
public class Strings {
	
	/**
	 * check whether str is null or "".
	 */
	public static boolean isEmpty(String str) {
		return str == null || str.length() == 0;
	}
	
	/**
	 * opposite to {@link #isEmpty(String)}
	 */
	public static boolean isNotEmpty(String str) {
		return !isEmpty(str);
	}
	
	/**
	 * check whether str is null or "", or consists of \s.
	 */
	public static boolean isBlank(String s) {
		if (isEmpty(s)) {
			return true;
		}
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isWhitespace(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * opposite to {@link #isBlank(String)}
	 */
	public static boolean isNotBlank(String str) {
		return !isBlank(str);
	}
	
	private static final String EMPTY_STR = "";
	
	/**
	 * return str.intern(), or null for str==null
	 */
	public static String intern(String str) {
		return str == null ? null : (str.length() == 0 ? EMPTY_STR : str.intern());
	}
	
	
	/**
	 * 计算中文字串余弦相似度
	 */
	public static double chineseCosineSimilarity(String str1, String str2) {
		if (Strings.isBlank(str1) || Strings.isBlank(str2)) {
			return Strings.isBlank(str1) && Strings.isBlank(str2) ? 1 : 0;
		}
		
		Set<Integer> cs = new HashSet<>();
		Multiset<Integer> c1 = HashMultiset.create();
		Multiset<Integer> c2 = HashMultiset.create();
		str1.trim().chars().filter(Encodings::isChinese).forEach(c -> {
			c1.add(c);
			cs.add(c);
		});
		str2.trim().chars().filter(Encodings::isChinese).forEach(c -> {
			c2.add(c);
			cs.add(c);
		});
		
		long denominator = 0, sq1 = 0, sq2 = 0;
		for (Integer c : cs) {
			long count1 = c1.count(c);
			long count2 = c2.count(c);
			denominator += count1 * count2;
			sq1 += count1 * count1;
			sq2 += count2 * count2;
		}
		return denominator == 0 ? 0 : denominator / Math.sqrt(sq1) / Math.sqrt(sq2);
	}
}
