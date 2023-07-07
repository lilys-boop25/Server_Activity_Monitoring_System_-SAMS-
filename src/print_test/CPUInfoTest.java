package print_test;

import oshi.*;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;

import com.sun.jna.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;
import oshi.util.tuples.Pair;

import oshi.driver.windows.perfmon.ProcessInformation.HandleCountProperty;

public class CPUInfoTest {
    private static final PlatformEnum CURRENT_PLATFORM = PlatformEnum.getValue(Platform.getOSType());

    public static void main(String[] args) {

        SystemInfo si = new SystemInfo();
        
        HardwareAbstractionLayer hal = si.getHardware();
        OperatingSystem os = si.getOperatingSystem();

        print("\nChecking CPU\n");
        CentralProcessor cProcessor = hal.getProcessor();
        ProcessorIdentifier pIden = cProcessor.getProcessorIdentifier();
        print("CPU Name: " + pIden.getName());
        print("Base speed: " + FormatUtil.formatHertz(pIden.getVendorFreq()));
        print(space(4) + "Sockets:            " + (cProcessor.getPhysicalPackageCount()));
        print(space(4) + "Cores:              " + (cProcessor.getPhysicalProcessorCount()));
        print(space(4) + "Logical processors: " + (cProcessor.getLogicalProcessorCount()));
        
        print("Processes: " + (os.getProcessCount()));
        print("Threads:   " + (os.getThreadCount()));

        if (CURRENT_PLATFORM.equals(PlatformEnum.WINDOWS))
        {
            Pair<List<String>, Map<HandleCountProperty, List<Long>>> hwdPair = oshi.driver.windows.perfmon.ProcessInformation.queryHandles();
            long handleCount = (hwdPair.getB().values().iterator().next()).get(0);
            print("Handles:   " + handleCount);
        }

        print("CPU usage in % in the last second: " + String.format("%.2f",cProcessor.getSystemCpuLoad(1000)*100) + "%");
        List<Double> pCpuLoads = new ArrayList<>();
        for (double pCpuLoad: cProcessor.getProcessorCpuLoad(1000))
        {
            pCpuLoads.add(pCpuLoad);
        }
        print("CPU usage in % of each processor in the last second:");
        for (int i = 0; i < pCpuLoads.size(); i++)
        {
            print(space(4) + "CPU " + (i) + ": " + String.format("%.2f",pCpuLoads.get(i)*100) + "%");
        }
    }

    /**
     * Print anything.
     */
    public static final void print(Object... content)
    {
        for(Object element: content)
        {
            System.out.println(element);
        }
    }

    public static final String space(int cnt)
    {
        return " ".repeat(cnt);
    }
}
