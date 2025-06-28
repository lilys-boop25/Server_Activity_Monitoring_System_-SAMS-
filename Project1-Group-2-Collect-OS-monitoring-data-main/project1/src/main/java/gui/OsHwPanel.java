package gui;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.software.os.OperatingSystem;
import oshi.util.EdidUtil;
import oshi.util.FormatUtil;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.util.List;

public class OsHwPanel extends OshiJPanel {
    private static final String OPERATING_SYSTEM = "Operating System ";
    private static final String HARDWARE_INFORMATION = "Hardware Information";
    private static final String PROCESSOR = "Processor";
    private static final String DISPLAYS = "Displays";
    private String osPrefix;
    public OsHwPanel(SystemInfo si){
        super();
        init(si);
    }

    private void init(SystemInfo si){
        osPrefix = getOsPrefix(si);
        // Set layout for main panel
        GridBagConstraints osLabel = new GridBagConstraints();
        GridBagConstraints osConstraints = new GridBagConstraints();
        osConstraints.gridy = 1;
        osConstraints.fill = GridBagConstraints.BOTH;
        osConstraints.insets = new Insets(0, 0, 20, 25);

        GridBagConstraints procLabel = (GridBagConstraints) osLabel.clone();
        procLabel.gridy = 2;
        GridBagConstraints procConstraints = (GridBagConstraints) osConstraints.clone();
        procConstraints.gridy = 3;

        GridBagConstraints displayLabel = (GridBagConstraints) procLabel.clone();
        displayLabel.gridy = 4;
        GridBagConstraints displayConstraints = (GridBagConstraints) osConstraints.clone();
        displayConstraints.gridy = 5;


        GridBagConstraints csLabel = (GridBagConstraints) osLabel.clone();
        csLabel.gridx = 1;
        GridBagConstraints csConstraints = new GridBagConstraints();
        csConstraints.gridx = 1;
        csConstraints.gridheight = 6;
        csConstraints.fill = GridBagConstraints.BOTH;

        Font SeFontBig = new Font("SansSerif", Font.PLAIN, 16);
        Font SeFontSmall = new Font("SansSerif", Font.BOLD, 18);

        JPanel oshwPanel = new JPanel();
        oshwPanel.setFont(SeFontBig);
        oshwPanel.setBackground(Color.WHITE);
        oshwPanel.setBorder(BorderFactory.createEmptyBorder());
        oshwPanel.setLayout(new GridBagLayout());

        JTextArea osArea = new JTextArea(0, 0);
        osArea.setBackground(Color.white);
        osArea.setText(updateOsData(si));
        osArea.setFont(SeFontBig);
        JLabel operatingTitle = new JLabel(OPERATING_SYSTEM);
        operatingTitle.setFont(SeFontSmall);
        oshwPanel.add(operatingTitle, osLabel);
        oshwPanel.add(osArea, osConstraints);

        JTextArea procArea = new JTextArea(0, 0);
        procArea.setText(getProcessor(si));
        procArea.setFont(SeFontBig);
        procArea.setBackground(Color.WHITE);
        JLabel processorTitle = new JLabel(PROCESSOR);
        processorTitle.setFont(SeFontSmall);
        oshwPanel.add(processorTitle, procLabel);
        oshwPanel.add(procArea, procConstraints);

        JTextArea displayArea = new JTextArea(0, 0);
        displayArea.setText(getDisplay(si));
        displayArea.setFont(SeFontBig);
        displayArea.setBackground(Color.white);
        JLabel displayTitle = new JLabel(DISPLAYS);
        displayTitle.setFont(SeFontSmall);
        oshwPanel.add(displayTitle, displayLabel);
        oshwPanel.add(displayArea, displayConstraints);

        JTextArea csArea = new JTextArea(0, 0);
        csArea.setText(getHw(si));
        csArea.setFont(SeFontBig);
        csArea.setBackground(Color.white);
        JLabel hardwareTitle = new JLabel(HARDWARE_INFORMATION);
        hardwareTitle.setFont(SeFontSmall);
        oshwPanel.add(hardwareTitle, csLabel);
        oshwPanel.add(csArea, csConstraints);

        add(oshwPanel);

        // Update up time every second
        Timer timer = new Timer(Config.REFRESH_FAST, e -> osArea.setText(updateOsData(si)));
        timer.start();
    }

    private static String getOsPrefix (SystemInfo si){
        StringBuilder sb = new StringBuilder(OPERATING_SYSTEM);

        OperatingSystem os = si.getOperatingSystem();
        sb.append((os));
        sb.append("\n").append("Booted: "). append(Instant.ofEpochSecond(os.getSystemBootTime())).append("\n");
        sb.append("Uptime: ");
        return sb.toString();
    }

    private static String getHw(SystemInfo si){
        StringBuilder sb = new StringBuilder();
        ComputerSystem computerSystem = si.getHardware().getComputerSystem();

        sb.append("SYSTEM \n");
        sb.append("     HardwareUUID: " + computerSystem.getHardwareUUID()+"\n");
        sb.append("     Model: " + computerSystem.getModel()+"\n");
        sb.append("     Serial number: " + computerSystem.getSerialNumber() + "\n\n");

        sb.append("FIRMWARE \n");
        sb.append("     Name: " + computerSystem.getFirmware().getName() + "\n");
        sb.append("     Version: " + computerSystem.getFirmware().getVersion() + "\n");
        sb.append("     ReleaseDate:  " + computerSystem.getFirmware().getReleaseDate() + "\n");
        sb.append("     Manufacturer: " + computerSystem.getFirmware().getManufacturer() + "\n\n");

        sb.append("BASEBOARD\n");
        sb.append("     Version: " + computerSystem.getBaseboard().getVersion() + "\n");
        sb.append("     Model: " + computerSystem.getBaseboard().getModel() + "\n");
        sb.append("     Manufacturer:  " + computerSystem.getBaseboard().getManufacturer() + "\n");
        sb.append("     Serial Number: " + computerSystem.getBaseboard().getSerialNumber());

        return sb.toString();
    }

    private static String getDisplay(SystemInfo si){
        StringBuilder sb = new StringBuilder();
        List<Display> displayList = si.getHardware().getDisplays();
        if (displayList.isEmpty()){
            sb.append("None detected.");
        } else{
            int i = 0;
            for (Display display : displayList){
                byte[] edid = display.getEdid();
                byte[][] desc = EdidUtil.getDescriptors(edid);

                String name = "Display " + i ;
                for (byte[] b : desc) {
                    if (EdidUtil.getDescriptorType(b) == 0xfc) {
                        name = EdidUtil.getDescriptorText(b);
                    }
                }
                if (i++ > 0) {
                    sb.append('\n');
                }

                sb.append(name);
                sb.append(": ");
                int hSize = EdidUtil.getHcm(edid);
                int vSize = EdidUtil.getVcm(edid);
                sb.append(String.format("%d x %d cm (%.1f x %.1f in)", hSize, vSize, hSize / 2.54, vSize / 2.54));

            }
        }
        return sb.toString();
    }

    private static String getProcessor(SystemInfo si){
        StringBuilder sb = new StringBuilder();
        CentralProcessor processor = si.getHardware().getProcessor();
        sb.append(processor.toString());

        return sb.toString();
    }


    private String updateOsData(SystemInfo si){
        return osPrefix + FormatUtil.formatElapsedSecs(si.getOperatingSystem().getSystemUptime());
    }
}
