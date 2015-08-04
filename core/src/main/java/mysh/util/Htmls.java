package mysh.util;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * html utils.
 * Created by mysh on 2015/7/14.
 */
public class Htmls {
	/**
	 * return html title (encapsulated by "title" tag).
	 * return blank string if fails.
	 */
	public static String getHtmlTitle(String html) {
		int from = html.indexOf("<title>");
		if (from < 0) return "";
		int end = html.indexOf("</title>", from);
		if (end < 0) return "";
		return StringEscapeUtils.unescapeHtml4(html.substring(from + 7, end));
	}

	/**
	 * unescape xml
	 */
	public static String unEscapeXml(String xml) {
		return xml
						.replace("&amp;", "&")
						.replace("&lt;", "<")
						.replace("&gt;", ">")
						.replace("&quot;", "\"")
						.replace("&apos;", "'");
	}
}
