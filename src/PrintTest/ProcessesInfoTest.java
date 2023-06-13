package PrintTest;

import oshi.*;

import java.util.List;

import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;
import oshi.software.os.OSProcess;

public class ProcessesInfoTest {
        
    public static void main(String[] args) {

        print("\nChecking Processes\n");

        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        List<OSProcess> pList = os.getProcesses();
        for (OSProcess process: pList)
        {
            print("Process ID: " + process.getProcessID());
            print("Process name: "+ process.getName());
            if (process.getParentProcessID() != 0)
                print("PPID: "+ process.getParentProcessID());
            print("Status: " + process.getState().toString());
            if (!process.getUser().equals("unknown"))
                print("Username: " + process.getUser());
            if (!process.getGroup().equals("None") && !process.getGroup().equals("unknown"))
                print("Group: " + process.getGroup());
            print("Cumulative: " + String.format("%.1f", 100d * process.getProcessCpuLoadCumulative()) + "%");
            print("VSZ: " + FormatUtil.formatBytes(process.getVirtualSize()));
            print("RSS: " + FormatUtil.formatBytes(process.getResidentSetSize()));
            long totalMem = si.getHardware().getMemory().getTotal();
            print("Memory(%): " + String.format("%.1f", 100d * process.getResidentSetSize() / totalMem));
            print("Threads: " + process.getThreadCount());
            if (!process.getCommandLine().isEmpty())
                print("Command Line: " + process.getCommandLine());
            if (!process.getPath().isEmpty())
                print("Filesystem path: " + process.getPath());
            print("Archietexture: " + String.valueOf(process.getBitness()) + " bits");
            print("");
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
