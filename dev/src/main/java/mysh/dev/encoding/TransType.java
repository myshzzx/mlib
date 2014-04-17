
package mysh.dev.encoding;

/**
 * 转换类型.
 * 
 * @author Allen
 * 
 */
public enum TransType {
	GBK2UTF8WithoutBOM("GBK", "UTF-8"), UTF82GBK("UTF-8", "GBK");

	private String srcEncoding, desEncoding;

	private TransType(String srcEncoding, String desEncoding) {

		this.srcEncoding = srcEncoding;
		this.desEncoding = desEncoding;
	}

	public String getSrcEncoding() {

		return srcEncoding;
	}

	public String getDesEncoding() {

		return desEncoding;
	}

}
