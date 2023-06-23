package GUI;

import javax.swing.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.List;

import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;

public class PerformancePanel extends OshiJPanel{

    /*
     * Create protected static attributes for CPU, RAM, Disk, Network.
     * All subclass will inherit (extends) from this PerformancePanel class.
     */

    //protected static List<Long> diskReadSpeed = new ArrayList<Long>(100);
    //protected static List<Long> diskWriteSpeed = new ArrayList<Long>(100);
  
    public PerformancePanel() {
        super();
    }

    public PerformancePanel(SystemInfo si) {
        super();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        initial(si);
        //fake_init(si);
    }
    
    class JVerticalMenuBar extends JMenuBar {
        private static final LayoutManager grid = new GridLayout(0,1);
        public JVerticalMenuBar() {
            setLayout(grid);
        }
    }
    

    private void initial(SystemInfo si) {
        JPanel perfPanel = new JPanel();
        perfPanel.setLayout(new GridBagLayout());

        JPanel displayPanel = new JPanel();
        displayPanel.setLayout(new GridBagLayout());

        JPanel perfMenuBar = new JPanel();
        perfMenuBar.setLayout(new GridBagLayout());

        JGradientButton cpuButton = createButton("\nCPU\n0%\n", 'C', "Display CPU", Color.GREEN, new CPUPanel(si), displayPanel);

        GridBagConstraints buttonC = new GridBagConstraints();
        buttonC.fill = GridBagConstraints.HORIZONTAL;
        buttonC.gridx = 0;
        buttonC.gridy = -1;

        buttonC.gridy++;
        perfMenuBar.add(cpuButton, buttonC);
        CPUPanel.updateCPUInfo(si.getHardware().getProcessor(), cpuButton);
        
        JGradientButton memButton = createButton(PerformancePanel.buttonTextLines("\nMemory\n0/0 GB (0%)\n"), 'M', "Display Memory", Color.GREEN,new MemoryPanel(si), displayPanel);

        buttonC.gridy++;
        perfMenuBar.add(memButton, buttonC);

        MemoryPanel.updateMemoryInfo(si.getHardware().getMemory(), memButton);

        List<HWDiskStore> hwDiskStore = si.getHardware().getDiskStores();
        JGradientButton[] diskButton = new JGradientButton[hwDiskStore.size()];
        for (int i = 0; i < hwDiskStore.size() ; i++)
        {
            HWDiskStore disk = hwDiskStore.get(i);
            diskButton[i] = createButton(DiskPanel.updateDisk(disk, i, 0, 0), 'D', "Display Disk",Color.PINK.darker(), new DiskPanel(disk, i), displayPanel);
            buttonC.gridy++;
            perfMenuBar.add(diskButton[i], buttonC);
        }
        DiskPanel.updateDiskInfo(si.getHardware().getDiskStores(), diskButton);
        
        List<NetworkIF> networkIFs = si.getHardware().getNetworkIFs(false);
        JGradientButton[] netButton = new JGradientButton[networkIFs.size()];
        for (int i = 0; i < networkIFs.size() ; i++)
        {
            NetworkIF net = networkIFs.get(i);
            netButton[i] = createButton(NetworkPanel.updateNetwork(net, 0, 0), 'N', "Display Network",Color.CYAN.brighter() , new NetworkPanel(net, i), displayPanel);
            buttonC.gridy++;
            perfMenuBar.add(netButton[i], buttonC);
        }
        NetworkPanel.updateNetWorkInfo(networkIFs, netButton);

        GridBagConstraints perfMenuBarConstraints = new GridBagConstraints();
        perfMenuBarConstraints.gridx = 0;
        perfMenuBarConstraints.gridy = 0;
        perfMenuBarConstraints.weightx = 0d;
        perfMenuBarConstraints.weighty = 0d;
        perfMenuBarConstraints.fill = GridBagConstraints.HORIZONTAL;
        perfMenuBarConstraints.anchor = GridBagConstraints.NORTHWEST;

        JScrollPane scrollPerfPanel = new JScrollPane(perfMenuBar);
        scrollPerfPanel.getVerticalScrollBar().setUnitIncrement(30);
        scrollPerfPanel.setMinimumSize(new Dimension(290, Math.min(buttonC.gridy * 102, 535)));
        //scrollPerfPanel.setMaximumSize(new Dimension(300, 950));
        scrollPerfPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        perfPanel.add(scrollPerfPanel, perfMenuBarConstraints);
        
        GridBagConstraints displayConstraints = new GridBagConstraints();
        displayConstraints.gridx = 1;
        displayConstraints.gridy = 0;
        displayConstraints.weightx = 0d;
        displayConstraints.weighty = 0d;
        displayConstraints.fill = GridBagConstraints.NONE;
        displayConstraints.anchor = GridBagConstraints.NORTHWEST;
        displayConstraints.insets = new Insets(0, 50, 0, 0);

        perfPanel.add(displayPanel, displayConstraints);


        GridBagConstraints perfConstraints = new GridBagConstraints();
        perfConstraints.gridx = 0;
        perfConstraints.gridy = 0;
        perfConstraints.weightx = 1d;
        perfConstraints.weighty = 1d;
        perfConstraints.fill = GridBagConstraints.NONE;
        perfConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(perfPanel, perfConstraints);

        cpuButton.doClick();
    }


    public static Color getColorByPercent(double percent)
    {
        if (percent < 60.d) {
            return Color.GREEN;
        }
        else if (percent < 80d) {
            return Color.YELLOW;
        }
        else {
            return Color.RED;
        }
    }



    private JGradientButton createButton(String title, char mnemonic, String toolTip, Color color, OshiJPanel panel, JPanel displayPanel)
    {
        JGradientButton button = new JGradientButton(title);
        button.color = color;
        button.setFont(button.getFont().deriveFont(16f));
        button.setHorizontalTextPosition(JButton.LEFT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        // Set what to do when we push the button
        button.addActionListener(e -> {
            int nComponents = (int)displayPanel.getComponents().length;
            if (nComponents <= (int)0 || displayPanel.getComponent(0) != panel) {
                resetMainGui(displayPanel);
                displayPanel.add(panel);
                refreshMainGui(displayPanel);
            }
        });
        return button;
    }
    
    public static void resetMainGui(JPanel displayPanel) {
        displayPanel.removeAll();
    }

    public static void refreshMainGui(JPanel displayPanel) {
        displayPanel.revalidate();
        displayPanel.repaint();
    }

    public static String buttonTextLines(String txt)
    {
        return "<html>" + htmlSpace(3) + txt.replaceAll("\n", "<br>" + htmlSpace(3));
    }

    public static String htmlSpace(int num)
    {
        return "&nbsp;".repeat(num);
    }
}
