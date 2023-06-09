package GUI;

import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JPanel;
import javax.swing.Timer;

import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JMenuBar;

import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.NetworkIF;
import oshi.util.FormatUtil;

public class PerformancePanel extends OshiJPanel{

    public PerformancePanel(SystemInfo si) {
        super();
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
        perfPanel.setLayout(new BorderLayout());

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
        List<NetworkIF> networkIFs = si.getHardware().getNetworkIFs();
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

        perfPanel.add(perfMenuBar, BorderLayout.WEST);
        add(perfPanel, BorderLayout.WEST);

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

        perfPanel.add(displayPanel, BorderLayout.CENTER);
        cpuButton.doClick();
    }

    private String updateNetwork(NetworkIF net, long recvSpeed, long sendSpeed)
    {
        String txt = net.getDisplayName() + "\n" + net.getIfAlias() + "\nSend: " + FormatUtil.formatBytes(sendSpeed) + "\nReceive: " + FormatUtil.formatBytes(recvSpeed);
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

    public String buttonTextLines(String txt)
    {
        return "<html>" + htmlSpace(3) + txt.replaceAll("\\n", "<br>" + htmlSpace(3));
    }

    private String htmlSpace(int num)
    {
        return "&nbsp;".repeat(num);
    }
}
