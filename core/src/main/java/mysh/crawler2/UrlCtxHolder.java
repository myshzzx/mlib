package mysh.crawler2;

import lombok.Data;
import okhttp3.Request;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Stream;

@Data
public class UrlCtxHolder<CTX extends UrlContext> implements Serializable {
	private static final long serialVersionUID = 2643572390637842239L;
	
	String url;
	CTX ctx;
	
	public UrlCtxHolder(String url) {
		this(url, null);
	}
	
	public UrlCtxHolder(String url, CTX ctx) {
		Objects.requireNonNull(url, "url should not be null");
		// ensure CrawlerSeed#accept.url and CrawlerSeed#onGet.ue.getReqUrl() get the same value, e.g. http://a.com/中文
		this.url = new Request.Builder().url(url).build().url().toString();
		this.ctx = ctx;
	}
	
	public static <T extends UrlContext> Stream<UrlCtxHolder<T>> ofAll(Stream<String> urls) {
		return urls.map(UrlCtxHolder::new);
	}
	
	public static <CTX extends UrlContext> UrlCtxHolder<CTX> of(String url) {
		return new UrlCtxHolder<>(url);
	}
	
	public static <CTX extends UrlContext> UrlCtxHolder<CTX> of(String url, CTX ctx) {
		return new UrlCtxHolder<>(url, ctx);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		
		UrlCtxHolder<?> that = (UrlCtxHolder<?>) o;
		
		return Objects.equals(url, that.url);
	}
	
	@Override
	public int hashCode() {
		return url.hashCode();
	}
	
}
