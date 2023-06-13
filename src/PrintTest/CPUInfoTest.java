package PrintTest;

import oshi.*;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;

import com.sun.jna.Platform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;
import oshi.util.tuples.Pair;

import oshi.driver.windows.perfmon.ProcessInformation.HandleCountProperty;

public class CPUInfoTest {
    private static final PlatformEnum CURRENT_PLATFORM = PlatformEnum.getValue(Platform.getOSType());

    public static void main(String[] args) throws IOException {

        SystemInfo si = new SystemInfo();
        
        HardwareAbstractionLayer hal = si.getHardware();
        OperatingSystem os = si.getOperatingSystem();

        print("\nChecking CPU\n");
        CentralProcessor cProcessor = hal.getProcessor();
        ProcessorIdentifier pIden = cProcessor.getProcessorIdentifier();
        print("CPU Name: " + pIden.getName());
        print("Base speed: " + FormatUtil.formatHertz(pIden.getVendorFreq()));
        print(space(4) + "Sockets:            " + String.valueOf(cProcessor.getPhysicalPackageCount()));
        print(space(4) + "Cores:              " + String.valueOf(cProcessor.getPhysicalProcessorCount()));
        print(space(4) + "Logical processors: " + String.valueOf(cProcessor.getLogicalProcessorCount()));
        
        print("Processes: " + String.valueOf(os.getProcessCount()));
        print("Threads:   " + String.valueOf(os.getThreadCount()));

        if (CURRENT_PLATFORM.equals(PlatformEnum.WINDOWS))
        {
            Pair<List<String>, Map<HandleCountProperty, List<Long>>> hwdPair = oshi.driver.windows.perfmon.ProcessInformation.queryHandles();
            long handleCount = (long)((List<Long>)(hwdPair.getB().values().iterator().next())).get(0);
            print("Handles:   " + handleCount);
        }

        print("CPU usage in % in the last second: " + String.format("%.2f",cProcessor.getSystemCpuLoad(1000)*100) + "%");
        List<Double> pCpuLoads = new ArrayList<Double>();
        for (double pCpuLoad: cProcessor.getProcessorCpuLoad(1000))
        {
            pCpuLoads.add(pCpuLoad);
        }
        print("CPU usage in % of each processor in the last second:");
        for (int i = 0; i < pCpuLoads.size(); i++)
        {
            print(space(4) + "CPU " + String.valueOf(i) + ": " + String.format("%.2f",pCpuLoads.get(i)*100) + "%");
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
