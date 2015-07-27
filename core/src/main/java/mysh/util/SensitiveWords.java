package mysh.util;

import java.util.HashMap;
import java.util.Map;

/**
 * SensitiveWords.
 *
 * @author mysh
 * @since 2015/7/27
 */
public class SensitiveWords {
	private static final int BLANK_CHAR_LIMIT = 3;
	private boolean isEnd = false;
	private Map<Character, SensitiveWords> suffix = new HashMap<>();

	public static SensitiveWords create() {
		return new SensitiveWords();
	}

	/**
	 * insert a sensitive word to lib.
	 */
	public void insert(String word) {
		SensitiveWords t = this, tNew = null;
		for (int i = 0; i < word.length(); i++) {
			char w = word.charAt(i);
			tNew = t.suffix.get(w);
			if (tNew == null) {
				tNew = new SensitiveWords();
				t.suffix.put(w, tNew);
			}
			t = tNew;
		}
		if (tNew != null) tNew.isEnd = true;
	}

	/**
	 * index of sensitive word found. -1 if not found.
	 */
	public int contains(char[] content) {
		GO:
		for (int i = 0; i < content.length; i++) {
			SensitiveWords t = this, nt;

			for (int j = i, blankLimit = BLANK_CHAR_LIMIT; !t.isEnd && j < content.length; j++) {
				nt = t.suffix.get(content[j]);
				if (nt != null) {
					blankLimit = BLANK_CHAR_LIMIT;
					t = nt;
				} else {
					if (t != this && blankLimit > 0 && isBlankChar(content[j])) {
						blankLimit--;
					} else {
						continue GO;
					}
				}
			}

			if (t.isEnd) return i;
		}

		return -1;
	}

	/**
	 * check whether current char is a blank char, which should be ignored between chars of sensitive words.
	 */
	boolean isBlankChar(char c) {
		return c < '\u4e00' || c > '\u9fa5';
	}
}
