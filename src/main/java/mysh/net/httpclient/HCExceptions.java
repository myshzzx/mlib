
package mysh.net.httpclient;

/**
 * 若取得的页面状态码不是 200, 或内容不是文本, 则抛出此异常.
 *
 * @author ZhangZhx
 */

public class HCExceptions {

	/**
	 * 取页面异常.
	 */
	public static class GetPageException extends Exception {

		private static final long serialVersionUID = 4402316602820149487L;

		public GetPageException(String msg) {

			super("页面内容异常: " + msg);
		}
	}

	/**
	 * 取内容异常.
	 */
	public static class GetContentException extends Exception {

		private static final long serialVersionUID = 4402316602820149487L;

		public GetContentException(String msg) {

			super("内容异常: " + msg);
		}
	}


}
