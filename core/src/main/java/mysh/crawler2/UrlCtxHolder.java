package mysh.crawler2;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Stream;

public class UrlCtxHolder<CTX extends UrlContext> implements Serializable {
	private static final long serialVersionUID = 2643572390637842239L;

	String url;
	CTX ctx;

	public UrlCtxHolder(String url) {
		this(url, null);
	}

	public UrlCtxHolder(String url, CTX ctx) {
		Objects.requireNonNull(url, "url should not be null");
		this.url = url;
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

		if (!url.equals(that.url)) return false;
		return Objects.equals(ctx, that.ctx);
	}

	@Override
	public int hashCode() {
		return url.hashCode();
	}

	// get and set

	public CTX getCtx() {
		return ctx;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setCtx(CTX ctx) {
		this.ctx = ctx;
	}
}
