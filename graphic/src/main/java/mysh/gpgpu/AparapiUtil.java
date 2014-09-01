package mysh.gpgpu;

import com.amd.aparapi.device.Device;
import com.amd.aparapi.device.OpenCLDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mysh
 * @since 2014/8/25 1:08
 */
public class AparapiUtil {
	private static final Logger log = LoggerFactory.getLogger(AparapiUtil.class);

	/**
	 * 找最合适的 OpenCL 设备.
	 *
	 * @param selName 查找的平台名. 为 null 则自动选择.
	 * @param type    设备类型. 选 {@link Device.TYPE#CPU} 或 {@link Device.TYPE#GPU}
	 * @return 合适的设备, 找不到返回 null.
	 */
	public static OpenCLDevice getPropCLDevice(String selName, Device.TYPE type) {
		List<OpenCLDevice> devs = new ArrayList<>();
		OpenCLDevice select = OpenCLDevice.select(
						d -> {
							if (d.getType() == type) {
								devs.add(d);
								if (selName != null
												&& d.getOpenCLPlatform().getName().toLowerCase().contains(selName.toLowerCase()))
									return d;
							}
							return null;
						}
		);
		if (select != null)
			return select;
		log.warn("can't find an openCL platform with name:" + selName);

		if (devs.size() == 0) return null;

		for (String name : new String[]{"nv", "ati", "amd", "intel"}) {
			for (OpenCLDevice dev : devs) {
				if (dev.getOpenCLPlatform().getName().toLowerCase().contains(name)) {
					log.info("auto select OpenCL platform: " + dev.getOpenCLPlatform().getName());
					return dev;
				}
			}
		}

		OpenCLDevice best = (OpenCLDevice) OpenCLDevice.best();
		if (best != null)
			log.info("use default OpenCL platform: " + best.getOpenCLPlatform().getName());

		return best;
	}

}
