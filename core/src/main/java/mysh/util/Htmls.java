package mysh.util;


import org.apache.commons.text.StringEscapeUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
				       .replace("&nbsp;", " ")
				       .replace("&amp;", "&")
				       .replace("&lt;", "<")
				       .replace("&gt;", ">")
				       .replace("&quot;", "\"")
				       .replace("&apos;", "'")
				       .replace("&ldquo;", "“")
				       .replace("&rdquo;", "”");
	}
	
	/**
	 * encode urls to str like "%2C1200&"
	 */
	public static String urlEncode(String url, String enc) {
		try {
			return URLEncoder.encode(url, enc);
		} catch (UnsupportedEncodingException e) {
			throw Exps.unchecked(e);
		}
	}
	
	/**
	 * decode urls like "%2C1200&"
	 */
	public static String urlDecode(String url, String enc) {
		try {
			return URLDecoder.decode(url, enc);
		} catch (UnsupportedEncodingException e) {
			throw Exps.unchecked(e);
		}
	}
	
	private static final Pattern paramsExp = Pattern.compile("(\\S+?)=(\\S+)");
	
	public static Map<String, String> parseQuery(String rawQuery) {
		Map<String, String> params = new HashMap<>();
		Matcher matcher = paramsExp.matcher(rawQuery.replace('&', ' '));
		while (matcher.find()) {
			params.put(matcher.group(1), Htmls.urlDecode(matcher.group(2), "UTF-8"));
		}
		return params;
	}
}
