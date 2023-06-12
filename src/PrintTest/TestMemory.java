
import com.sun.jna.Platform;

import oshi.PlatformEnum;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.VirtualMemory;

public class TestMemory{

    private static final PlatformEnum CURRENT_PLATFORM = PlatformEnum.getValue(Platform.getOSType());

    public static void main(String[] args){
        SystemInfo systemInfo = new SystemInfo();
        GlobalMemory memory = systemInfo.getHardware().getMemory();

        System.out.println(CURRENT_PLATFORM.name() + '\n');

        System.out.println("Memory: " + String.valueOf(memory.getTotal() / (1024*1024)) + " MB");
        System.out.println(memory.toString());
        long swapTotal = memory.getVirtualMemory().getSwapTotal();
        long swapUsed = memory.getVirtualMemory().getSwapUsed();
        if (CURRENT_PLATFORM.name().equals("WINDOWS")){
            System.out.println("Total page files: " + String.valueOf(swapTotal/(1024*1024)) + " MB");
            System.out.println("Used page files: " + String.valueOf(swapUsed/(1024*1024)) + " MB");
        }
        else if (CURRENT_PLATFORM.name().equals("LINUX")){
            System.out.println("Total swap memory: " + String.valueOf(swapTotal/(1024*1024)) + " MB");
            System.out.println("Used  swap memory" + String.valueOf(swapUsed/(1024*1024)) + " MB");
        }
        VirtualMemory virtualMemory = memory.getVirtualMemory();
        System.out.println(virtualMemory.toString());
    }
}
