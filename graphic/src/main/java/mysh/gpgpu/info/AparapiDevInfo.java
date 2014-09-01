package mysh.gpgpu.info;

import com.amd.aparapi.device.Device;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.opencl.OpenCLPlatform;

import java.util.List;


/**
 * @author Mysh
 * @since 14-1-11 上午12:02
 */
public class AparapiDevInfo {

	public static void main(String[] args) {
		System.out.println("AparapiDevInfo");
		List<OpenCLPlatform> platforms =new OpenCLPlatform().getOpenCLPlatforms();
		System.out.println("Machine contains " + platforms.size() + " OpenCL platforms");
		int platformc = 0;
		for (OpenCLPlatform platform : platforms) {
			System.out.println("Platform " + platformc + "{");
			System.out.println("   Name    : \"" + platform.getName() + "\"");
			System.out.println("   Vendor  : \"" + platform.getVendor() + "\"");
			System.out.println("   Version : \"" + platform.getVersion() + "\"");
			List<OpenCLDevice> devices = platform.getOpenCLDevices();
			System.out.println("   Platform contains " + devices.size() + " OpenCL devices");
			int devicec = 0;
			for (OpenCLDevice device : devices) {
				System.out.println("   Device " + devicec + "{");
				System.out.println("       Type                  : " + device.getType());
				System.out.println("       GlobalMemSize         : " + device.getGlobalMemSize());
				System.out.println("       LocalMemSize          : " + device.getLocalMemSize());
				System.out.println("       MaxComputeUnits       : " + device.getMaxComputeUnits());
				System.out.println("       MaxWorkGroupSizes     : " + device.getMaxWorkGroupSize());
				System.out.println("       MaxWorkItemDimensions : " + device.getMaxWorkItemDimensions());
				System.out.println("   }");
				devicec++;
			}
			System.out.println("}");
			platformc++;
		}

		Device bestDevice = OpenCLDevice.best();
		if (bestDevice == null) {
			System.out.println("OpenCLDevice.best() returned null!");
		} else {
			System.out.println("OpenCLDevice.best() returned { ");
			System.out.println("   Type                  : " + bestDevice.getType());
			System.out.println("   GlobalMemSize         : " + ((OpenCLDevice) bestDevice).getGlobalMemSize());
			System.out.println("   LocalMemSize          : " + ((OpenCLDevice) bestDevice).getLocalMemSize());
			System.out.println("   MaxComputeUnits       : " + ((OpenCLDevice) bestDevice).getMaxComputeUnits());
			System.out.println("   MaxWorkGroupSizes     : " + ((OpenCLDevice) bestDevice).getMaxWorkGroupSize());
			System.out.println("   MaxWorkItemDimensions : " + ((OpenCLDevice) bestDevice).getMaxWorkItemDimensions());
			System.out.println("}");
		}

		Device firstCPU = OpenCLDevice.firstCPU();
		if (firstCPU == null) {
			System.out.println("OpenCLDevice.firstCPU() returned null!");
		} else {
			System.out.println("OpenCLDevice.firstCPU() returned { ");
			System.out.println("   Type                  : " + firstCPU.getType());
			System.out.println("   GlobalMemSize         : " + ((OpenCLDevice) firstCPU).getGlobalMemSize());
			System.out.println("   LocalMemSize          : " + ((OpenCLDevice) firstCPU).getLocalMemSize());
			System.out.println("   MaxComputeUnits       : " + ((OpenCLDevice) firstCPU).getMaxComputeUnits());
			System.out.println("   MaxWorkGroupSizes     : " + ((OpenCLDevice) firstCPU).getMaxWorkGroupSize());
			System.out.println("   MaxWorkItemDimensions : " + ((OpenCLDevice) firstCPU).getMaxWorkItemDimensions());
			System.out.println("}");
		}

		Device firstGPU = OpenCLDevice.firstGPU();
		if (firstGPU == null) {
			System.out.println("OpenCLDevice.firstGPU() returned null!");
		} else {
			System.out.println("OpenCLDevice.firstGPU() returned { ");
			System.out.println("   Type                  : " + firstGPU.getType());
			System.out.println("   GlobalMemSize         : " + ((OpenCLDevice) firstGPU).getGlobalMemSize());
			System.out.println("   LocalMemSize          : " + ((OpenCLDevice) firstGPU).getLocalMemSize());
			System.out.println("   MaxComputeUnits       : " + ((OpenCLDevice) firstGPU).getMaxComputeUnits());
			System.out.println("   MaxWorkGroupSizes     : " + ((OpenCLDevice) firstGPU).getMaxWorkGroupSize());
			System.out.println("   MaxWorkItemDimensions : " + ((OpenCLDevice) firstGPU).getMaxWorkItemDimensions());
			System.out.println("}");
		}

	}
}
