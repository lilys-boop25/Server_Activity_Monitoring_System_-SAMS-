import java.io.IOException;
import java.util.List;

import oshi.*;
import oshi.hardware.HardwareAbstractionLayer;

import oshi.util.FormatUtil;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;

public class HWDiskStoreInfoTest {

    public static void main(String[] args) throws IOException {

        SystemInfo si = new SystemInfo();
        
        HardwareAbstractionLayer hal = si.getHardware();
        List<HWDiskStore> HWDiskStoreList = hal.getDiskStores();
        for (HWDiskStore disk: HWDiskStoreList)
        {
            print("Disk name: " + disk.getName());
            print("Model: " + disk.getModel());
            print("Size: " + FormatUtil.formatBytes(disk.getSize()));
            long prevReadBytes = disk.getReadBytes();
            long prevWriteBytes = disk.getWriteBytes();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            disk.updateAttributes();
            print("Read speed in the last second: " + FormatUtil.formatBytes(disk.getReadBytes() - prevReadBytes));
            print("Write speed in the last second: " + FormatUtil.formatBytes(disk.getWriteBytes() - prevWriteBytes));
            List<HWPartition> HWPartitionList = disk.getPartitions();
            for (HWPartition partition: HWPartitionList)
            {
                print("");
                print(space(4) + "Directory: " + partition.getMountPoint());
                print(space(4) + "Size: " + FormatUtil.formatBytes(partition.getSize()));
                print(space(4) + "Type: " + partition.getType().replace(":", ""));
            }
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
