
package PrintTest;

import java.util.List;

import com.sun.jna.Platform;

import oshi.PlatformEnum;
import oshi.SystemInfo;
import oshi.hardware.Display;
import oshi.util.EdidUtil;

public class DisplayTest {
    private static final PlatformEnum CURRENT_PLATFORM = PlatformEnum.getValue(Platform.getOSType());

    public static void main(String[] args) {

        SystemInfo systemInfo = new SystemInfo();
        List<Display> displays = systemInfo.getHardware().getDisplays();

        System.out.println(CURRENT_PLATFORM.name() + '\n');

        System.out.println("Display: ");
        for (Display display : displays){
            byte[] edid = display.getEdid();

            int hSize = EdidUtil.getHcm(edid);
            int vSize = EdidUtil.getVcm(edid);
            System.out.println(String.format("%d x %d cm (%.1f x %.1f in)", hSize, vSize, hSize / 2.54, vSize / 2.54));
        }
    }
}
