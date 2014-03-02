
package mysh.net.httpclient;

import mysh.annotation.NotThreadSafe;

import java.io.Serializable;
import java.sql.Date;

@NotThreadSafe
public class Page implements Serializable {

	private static final long serialVersionUID = -3961094131912887358L;
	public static final String DefaultEncoding = "UTF-8";
	/**
	 * ID初始值, 若页面 ID 不为此初始值, 表示此页面已存在于数据库中.
	 */
	public static final int INIT_ID = -1;

	/**
	 * 页面的数据库 ID
	 */
	private volatile int id = INIT_ID;

	/**
	 * 页面地址.
	 */
	private volatile String url;

	/**
	 * 页面内容.
	 */
	private transient volatile String content;

	/**
	 * 页面上一次爬取的时间.
	 */
	private transient volatile Date lastmod;

	public Page(String url) {

		this.url = url;
	}

	public int getId() {

		return id;
	}

	public Page setId(int id) {

		this.id = id;
		return this;
	}

	public String getUrl() {

		return url;
	}

	public Page setUrl(String url) {

		this.url = url;
		return this;
	}

	public String getContent() {

		return content;
	}

	public Page setContent(String content) {

		this.content = content;
		return this;
	}

	public Date getLastmod() {

		return lastmod;
	}

	public Page setLastmod(Date lastmod) {

		this.lastmod = lastmod;
		return this;
	}

}
