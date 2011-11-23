
package mysh.net.httpclient;

/**
 * 若取得的页面状态码不是 200, 或内容不是文本, 则抛出此异常.
 * 
 * @author ZhangZhx
 * 
 */
public class GetPageException extends Exception {

	private static final long serialVersionUID = 4402316602820149487L;

	public GetPageException(String msg) {

		super("内容异常: " + msg);
	}
}
