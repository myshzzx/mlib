package mysh.util;


import com.google.common.net.HttpHeaders;
import com.sun.net.httpserver.Headers;
import mysh.collect.Colls;
import org.apache.commons.text.StringEscapeUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * html utils.
 * Created by mysh on 2015/7/14.
 */
public class Htmls {
	private static final Map<String, String> mimeTypes;
	
	public static final String MIME_TEXT = "text/plain";
	public static final String MIME_HTML = "text/html";
	public static final String MIME_FORM = "multipart/form-data";
	public static final String MIME_STREAM = "application/octet-stream";
	
	static {
		mimeTypes = Colls.ofHashMap(
				"aac", "audio/aac",
				"abw", "application/x-abiword",
				"arc", "application/x-freearc",
				"avi", "video/x-msvideo",
				"azw", "application/vnd.amazon.ebook",
				"bin", "application/octet-stream",
				"bmp", "image/bmp",
				"bz", "application/x-bzip",
				"bz2", "application/x-bzip2",
				"csh", "application/x-csh",
				"css", "text/css",
				"csv", "text/csv",
				"doc", "application/msword",
				"docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
				"eot", "application/vnd.ms-fontobject",
				"epub", "application/epub+zip",
				"gif", "image/gif",
				"htm", "text/html",
				"html", "text/html",
				"ico", "image/vnd.microsoft.icon",
				"ics", "text/calendar",
				"jar", "application/java-archive",
				"jpeg", "image/jpeg",
				"jpg", "image/jpeg",
				"js", "text/javascript",
				"json", "application/json",
				"jsonld", "application/ld+json",
				"mht", "multipart/related",
				"mid", "audio/midi",
				"midi", "audio/midi",
				"mjs", "text/javascript",
				"mp3", "audio/mpeg",
				"mpeg", "video/mpeg",
				"mpkg", "application/vnd.apple.installer+xml",
				"odp", "application/vnd.oasis.opendocument.presentation",
				"ods", "application/vnd.oasis.opendocument.spreadsheet",
				"odt", "application/vnd.oasis.opendocument.text",
				"oga", "audio/ogg",
				"ogv", "video/ogg",
				"ogx", "application/ogg",
				"otf", "font/otf",
				"png", "image/png",
				"pdf", "application/pdf",
				"ppt", "application/vnd.ms-powerpoint",
				"pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
				"rar", "application/x-rar-compressed",
				"rtf", "application/rtf",
				"sh", "application/x-sh",
				"svg", "image/svg+xml",
				"swf", "application/x-shockwave-flash",
				"tar", "application/x-tar",
				"tif", "image/tiff",
				"tiff", "image/tiff",
				"ttf", "font/ttf",
				"txt", "text/plain",
				"vsd", "application/vnd.visio",
				"wav", "audio/wav",
				"weba", "audio/webm",
				"webm", "video/webm",
				"webp", "image/webp",
				"woff", "font/woff",
				"woff2", "font/woff2",
				"xhtml", "application/xhtml+xml",
				"xls", "application/vnd.ms-excel",
				"xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				"xml", "application/xml",
				"xul", "application/vnd.mozilla.xul+xml",
				"zip", "application/zip",
				"3gp", "video/3gpp",
				"7z", "application/x-7z-compressed"
		);
	}
	
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
	
	public static String escapeXml(String text) {
		return text
				.replace(" ", "&nbsp;")
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;")
				.replace("“", "&ldquo;")
				.replace("”", "&rdquo;");
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
	public static String urlDecode(String url, Charset enc) {
		try {
			return URLDecoder.decode(url, enc.name());
		} catch (UnsupportedEncodingException e) {
			throw Exps.unchecked(e);
		}
	}
	
	private static final Pattern paramsExp = Pattern.compile("(\\S+?)=(\\S+)");
	
	public static Map<String, String> parseQuery(String rawQuery, Charset enc) {
		Map<String, String> params = new HashMap<>();
		if (Strings.isNotBlank(rawQuery)) {
			Matcher matcher = paramsExp.matcher(rawQuery.replace('&', ' '));
			while (matcher.find()) {
				params.put(matcher.group(1), Htmls.urlDecode(matcher.group(2), enc));
			}
		}
		return params;
	}
	
	/**
	 * @param ext file extension with dot. e.g. <code>.html</code>
	 * @return mime type like <code>text/html</code>
	 */
	public static String getMimeType(String ext, String defaultType) {
		return mimeTypes.getOrDefault(ext, Strings.firstNonBlank(defaultType, MIME_STREAM));
	}
	
	public static void putDownloadRspHeader(Headers rspHeaders, String fileName) {
		rspHeaders.set(HttpHeaders.CONTENT_TYPE, Htmls.MIME_STREAM);
		rspHeaders.set(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + Htmls.urlEncode(fileName, "UTF-8") + "\"");
	}
}
