package PrintTest;

import com.sun.jna.Platform;

import oshi.PlatformEnum;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;

public class Processor {
    private static final PlatformEnum CURRENT_PLATFORM = PlatformEnum.getValue(Platform.getOSType());

    public static void main(String[] args){
        SystemInfo si = new SystemInfo();
        CentralProcessor processor = si.getHardware().getProcessor();
        ProcessorIdentifier identifier = processor.getProcessorIdentifier();

        System.out.println(CURRENT_PLATFORM.name() + '\n');

        System.out.println("Name: " + identifier.getName());
        System.out.println("Model: " + identifier.getModel());
        System.out.println(identifier.getVendorFreq());
        System.out.println("Microarchitecture: " + identifier.getMicroarchitecture());
        System.out.println("CPU package: " + processor.getPhysicalPackageCount());
        System.out.println("Logical CPU: " + processor.getLogicalProcessorCount());
        System.out.println("Physical CPU cores: " + processor.getPhysicalProcessorCount());

    }
}
