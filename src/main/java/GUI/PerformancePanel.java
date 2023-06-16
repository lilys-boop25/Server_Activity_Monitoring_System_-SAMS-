package GUI;

import javax.swing.*;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.util.List;

import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.NetworkIF;
import oshi.util.FormatUtil;

public class PerformancePanel extends OshiJPanel{

    public PerformancePanel() {
    }

    public PerformancePanel(SystemInfo si) {
        super();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        initial(si);
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

        JButton cpuButton = createButton(updateCPU(si), 'C', "Display CPU" , new CPUPanel(si), displayPanel);

        GridBagConstraints cpuC = new GridBagConstraints();
        cpuC.fill = GridBagConstraints.HORIZONTAL;
        cpuC.gridx = 0;
        cpuC.gridy = 0;
        perfMenuBar.add(cpuButton, cpuC);
        
        JButton memButton = createButton(updateMemory(si), 'M', "Display Memory" ,new MemoryPanel(si), displayPanel);

        GridBagConstraints memC = (GridBagConstraints)cpuC.clone();
        memC.gridx = 0;
        memC.gridy = 1;
        perfMenuBar.add(memButton, memC);

        int y = 2;
        List<NetworkIF> networkIFs = si.getHardware().getNetworkIFs(false);
        JButton[] netButton = new JButton[networkIFs.size()];
        for (int i = 0; i < networkIFs.size() ; i++)
        {
            NetworkIF net = networkIFs.get(i);
            netButton[i] = createNetworkButton(updateNetwork(net, 0, 0), 'N', "Display Network", net, displayPanel);
            GridBagConstraints netC = (GridBagConstraints)cpuC.clone();
            netC.gridy =  y;
            y++;
            perfMenuBar.add(netButton[i], netC);
        }

        GridBagConstraints perfMenuBarConstraints = new GridBagConstraints();
        perfMenuBarConstraints.gridx = 0;
        perfMenuBarConstraints.gridy = 0;
        perfMenuBarConstraints.weightx = 1d;
        perfMenuBarConstraints.weighty = 1d;
        perfMenuBarConstraints.anchor = GridBagConstraints.NORTHWEST;

        JScrollPane scrollPerfPanel = new JScrollPane(perfMenuBar);
        scrollPerfPanel.setMinimumSize(new Dimension(320, getSize().height));
        scrollPerfPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        perfPanel.add(scrollPerfPanel, perfMenuBarConstraints);
        
        GridBagConstraints perfConstraints = new GridBagConstraints();
        perfConstraints.gridx = 0;
        perfConstraints.gridy = 0;
        perfConstraints.weightx = 1d;
        perfConstraints.weighty = 1d;
        perfConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(perfPanel, perfConstraints);


        // Update up time every second
        Timer timer = new Timer(Config.REFRESH_FAST, e -> {
            
            Thread cpuThread = new Thread( ()->{
                cpuButton.setText(updateCPU(si));
            });
            cpuThread.start();
    
            memButton.setText(updateMemory(si));

        });
        
        timer.start();
        Thread thread = new Thread(() -> {
            while(true)
            {
                for (int i = 0; i < networkIFs.size() ; i++)
                {
                    NetworkIF net = networkIFs.get(i);
                    long timeNow = net.getTimeStamp();
                    long recvLast = net.getBytesRecv();
                    long sendLast = net.getBytesSent();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    net.updateAttributes();
                    long recvNow = net.getBytesRecv();
                    long sendNow = net.getBytesSent();
                    long sendSpeed = (sendNow - sendLast)*1000/(net.getTimeStamp()-timeNow);
                    long recvSpeed = (recvNow - recvLast)*1000/(net.getTimeStamp()-timeNow);
                    netButton[i].setText(updateNetwork(net, recvSpeed, sendSpeed));
                }
            }
        });
        thread.start();

        GridBagConstraints displayConstraints = new GridBagConstraints();
        displayConstraints.gridx = 1;
        displayConstraints.gridy = 0;
        displayConstraints.weightx = 4d;
        displayConstraints.weighty = 1d;
        displayConstraints.fill = GridBagConstraints.NONE;
        displayConstraints.anchor = GridBagConstraints.NORTHWEST;

        perfPanel.add(displayPanel, displayConstraints);

        cpuButton.doClick();
    }

    public static String updateNetwork(NetworkIF net, long recvSpeed, long sendSpeed)
    {
        String name = net.getDisplayName();
        if (name.length() > 30)
        {
            name = name.substring(0,30) + "...";
        }
        String alias = net.getIfAlias();
        if (alias.length() > 30)
        {
            alias = alias.substring(0,30) + "...";
        }
        String txt = name + "\n" + alias + "\nSend: " + FormatUtil.formatBytes(sendSpeed) + "\nReceive: " + FormatUtil.formatBytes(recvSpeed);
        return buttonTextLines(txt);
    }

    private String updateMemory(SystemInfo si) {
        GlobalMemory mem = si.getHardware().getMemory();
        double available = (double)mem.getAvailable()/(1024*1024*1024);
        double total = (double)mem.getTotal()/(1024*1024*1024);
        double used = total - available;
        return buttonTextLines("Memory\n" + (String.format("%.2f/%.2f GB (%.0f)", used, total, (used/total)*100) + "%"));
    }

    private String updateCPU(SystemInfo si)
    {
        double load = si.getHardware().getProcessor().getSystemCpuLoad(1000);
        return buttonTextLines("CPU\n" + (String.format("%.2f",load*100)) + "%");
    }

    private JButton createNetworkButton(String title, char mnemonic, String toolTip, NetworkIF net, JPanel displayPanel)
    {
        JButton button = new JButton(title, null);
        button.setFont(button.getFont().deriveFont(16f));
        button.setHorizontalTextPosition(JButton.LEFT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        OshiJPanel panel = new NetworkPanel(net, button);
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

    private JButton createButton(String title, char mnemonic, String toolTip, OshiJPanel panel, JPanel displayPanel)
    {
        JButton button = new JButton(title, null);
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
    
    private void resetMainGui(JPanel displayPanel) {
        displayPanel.removeAll();
    }

    private void refreshMainGui(JPanel displayPanel) {
        displayPanel.revalidate();
        displayPanel.repaint();
    }

    public static String buttonTextLines(String txt)
    {
        return "<html>" + htmlSpace(3) + txt.replaceAll("\\n", "<br>" + htmlSpace(3));
    }

    public static String htmlSpace(int num)
    {
        return "&nbsp;".repeat(num);
    }
}
