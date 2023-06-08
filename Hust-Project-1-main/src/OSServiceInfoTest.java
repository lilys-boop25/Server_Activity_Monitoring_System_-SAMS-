import oshi.*;

import com.sun.jna.Platform;

import java.util.List;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OSService;

public class OSServiceInfoTest { // NOSONAR squid:S5786
    private static final PlatformEnum CURRENT_PLATFORM = PlatformEnum.getValue(Platform.getOSType());
    public static void main(String[] args) {

        SystemInfo si = new SystemInfo();

        OperatingSystem os = si.getOperatingSystem();

        print(CURRENT_PLATFORM.name() + '\n');

        List<OSService> osServices = os.getServices();
        print("Number of OS Services: " + osServices.size());
        for (OSService osService: osServices)
        {
            print("Service name: " + osService.getName());
            int pid = osService.getProcessID();
            print(space(4) + "Process ID: " + String.valueOf(pid));
            print(space(4) + "Status: " + osService.getState().toString());
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