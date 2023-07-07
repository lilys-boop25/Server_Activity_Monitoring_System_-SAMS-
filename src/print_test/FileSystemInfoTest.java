package print_test;
import oshi.*;

import com.sun.jna.Platform;

import java.util.List;
import oshi.software.os.OperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;

public class FileSystemInfoTest {
    private static final PlatformEnum CURRENT_PLATFORM = PlatformEnum.getValue(Platform.getOSType());
    public static void main(String[] args) {

        print("\nChecking File Systems\n");

        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        FileSystem fileSystem = os.getFileSystem();
        List<OSFileStore> osFileStores = fileSystem.getFileStores();
        for (OSFileStore osFileStore: osFileStores)
        {
            if (CURRENT_PLATFORM.equals(PlatformEnum.LINUX)) 
                print("Device: " + osFileStore.getVolume());
            else print("Device: " + osFileStore.getName());
            print("Directory: " + osFileStore.getMount());
            print(space(4) + "Type: " + osFileStore.getType());
            print(space(4) + "Total:     " + ((double)osFileStore.getTotalSpace()/(1000*1000)) + " Mb.");
            print(space(4) + "Available: " + ((double)osFileStore.getUsableSpace()/(1000*1000)) + " Mb.");
            print(space(4) + "Used:      " + ((double)osFileStore.getTotalSpace()/(1000*1000)- (double)osFileStore.getFreeSpace()/(1000*1000)) + " Mb.");
        }
    }

    /**
     * Print anything.
     */
    public final static void print(Object... content)
    {
        for(Object element: content)
        {
            System.out.println(element);
        }
    }

    public final static String space(int cnt)
    {
        return " ".repeat(cnt);
    }
}
