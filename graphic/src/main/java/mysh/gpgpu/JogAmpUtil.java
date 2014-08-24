package mysh.gpgpu;

import com.jogamp.opencl.CLPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mysh
 * @since 2014/8/25 0:53
 */
public class JogAmpUtil {
	private static final Logger log = LoggerFactory.getLogger(JogAmpUtil.class);

	/**
	 * 找最合适的 OpenCL 平台.
	 *
	 * @param selName 查找的平台名. 为 null 则自动选择.
	 * @return 合适的平台, 找不到返回 null.
	 */
	public static CLPlatform getPropCLPlat(String selName) {
		CLPlatform[] plats = CLPlatform.listCLPlatforms();
		if (plats == null || plats.length == 0)
			return null;

		if (selName != null) {
			selName = selName.toLowerCase();
			for (CLPlatform p : plats) {
				if (p.getName().toLowerCase().contains(selName)) {
					return p;
				}
			}
			log.warn("can't find an openCL device with name:" + selName);
		}

		for (String name : new String[]{"nv", "ati", "amd", "intel"}) {
			for (CLPlatform p : plats) {
				if (p.getName().toLowerCase().contains(name)) {
					log.info("auto select OpenCL platform: " + p.getName());
					return p;
				}
			}
		}

		CLPlatform p = CLPlatform.getDefault();
		if (p != null)
			log.info("use default OpenCL platform: " + p.getName());
		return p;
	}
}
