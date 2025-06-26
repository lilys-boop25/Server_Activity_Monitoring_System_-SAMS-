package printtest;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.*;
import oshi.hardware.HardwareAbstractionLayer;

import oshi.util.FormatUtil;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;

public class HWDiskStoreInfoTest {
    private static final Logger logger = LoggerFactory.getLogger(HWDiskStoreInfoTest.class);
    public static void main(String[] args) {

        SystemInfo si = new SystemInfo();
        
        HardwareAbstractionLayer hal = si.getHardware();
        List<HWDiskStore> hwDiskStoreList = hal.getDiskStores();
        for (HWDiskStore disk: hwDiskStoreList)
        {
            print("Disk name: " + disk.getName());
            print("Model: " + disk.getModel());
            print("Size: " + FormatUtil.formatBytes(disk.getSize()));
            long prevReadBytes = disk.getReadBytes();
            long prevWriteBytes = disk.getWriteBytes();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                Thread.currentThread().interrupt();
                logger.error("Error occurred: ", e1);
            }
            disk.updateAttributes();
            print("Read speed in the last second: " + FormatUtil.formatBytes(disk.getReadBytes() - prevReadBytes));
            print("Write speed in the last second: " + FormatUtil.formatBytes(disk.getWriteBytes() - prevWriteBytes));
            List<HWPartition> hwPartitionList = disk.getPartitions();
            for (HWPartition partition: hwPartitionList)
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
