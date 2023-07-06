package gui;

import javax.swing.*;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYAreaRenderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.util.List;

import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;

public class PerformancePanel extends OshiJPanel{

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
    }

    class JVerticalMenuBar extends JMenuBar {
        private static final LayoutManager grid = new GridLayout(0,1);
        public JVerticalMenuBar() {
            setLayout(grid);
        }
    }

    public static void setChartRenderer(JFreeChart chart, Color color){
        XYAreaRenderer renderer = new XYAreaRenderer();
        renderer.setSeriesPaint(0, new Color(color.getRed(), color.getGreen(), color.getBlue(), 128));
        renderer.setOutline(true);
        renderer.setSeriesOutlineStroke(0, new BasicStroke(1.0f));
        renderer.setSeriesOutlinePaint(0, color.darker());

        chart.getXYPlot().setRenderer(renderer);
        chart.getPlot().setBackgroundPaint( Color.WHITE );
        chart.getXYPlot().setDomainGridlinesVisible(true);
        chart.getXYPlot().setRangeGridlinesVisible(true);
        chart.getXYPlot().setRangeGridlinePaint(Color.black);
        chart.getXYPlot().setDomainGridlinePaint(Color.black);
    }

    public static void setChartRenderer(JFreeChart chart, Color color1, Color color2){
        XYAreaRenderer renderer = new XYAreaRenderer();
        renderer.setSeriesPaint(0, new Color(color1.getRed(), color1.getGreen(), color1.getBlue(), 128));
        renderer.setOutline(true);
        renderer.setSeriesOutlineStroke(0, new BasicStroke(1.0f));
        renderer.setSeriesOutlinePaint(0, color1.darker());

        renderer.setSeriesPaint(1, new Color(color2.getRed(), color2.getGreen(), color2.getBlue(), 128));
        renderer.setOutline(true);
        renderer.setSeriesOutlineStroke(1, new BasicStroke(1.0f));
        renderer.setSeriesOutlinePaint(1, color2.darker());

        chart.getXYPlot().setRenderer(renderer);
        chart.getPlot().setBackgroundPaint( Color.WHITE );
        chart.getXYPlot().setDomainGridlinesVisible(true);
        chart.getXYPlot().setRangeGridlinesVisible(true);
        chart.getXYPlot().setRangeGridlinePaint(Color.black);
        chart.getXYPlot().setDomainGridlinePaint(Color.black);
    }


    private void initial(SystemInfo si) {
        JPanel perfPanel = new JPanel();
        perfPanel.setLayout(null);

        JPanel displayPanel = new JPanel();
        displayPanel.setLayout(new GridBagLayout());

        JPanel perfMenuBar = new JPanel();
        perfMenuBar.setLayout(new BoxLayout(perfMenuBar, BoxLayout.Y_AXIS));

        JGradientButton cpuButton = createButton("\nCPU\n0%\n", 'C', "Display CPU", Color.GREEN, new CPUPanel(si), displayPanel);

        perfMenuBar.add(cpuButton);
        CPUPanel.updateCPUInfo(si.getHardware().getProcessor(), cpuButton, si.getOperatingSystem());

        JGradientButton memButton = createButton(PerformancePanel.buttonTextLines("\nMemory\n0/0 GB (0%)\n"), 'M', "Display Memory", Color.GREEN,new MemoryPanel(si), displayPanel);

        perfMenuBar.add(memButton);

        MemoryPanel.updateMemoryInfo(si.getHardware().getMemory(), memButton);

        List<HWDiskStore> hwDiskStore = si.getHardware().getDiskStores();
        JGradientButton[] diskButton = new JGradientButton[hwDiskStore.size()];
        for (int i = 0; i < hwDiskStore.size() ; i++)
        {
            HWDiskStore disk = hwDiskStore.get(i);
            diskButton[i] = createButton(DiskPanel.updateDisk(disk, i, 0, 0), 'D', "Display Disk",Color.PINK.darker(), new DiskPanel(disk, i), displayPanel);
            perfMenuBar.add(diskButton[i]);
        }
        DiskPanel.updateDiskInfo(si.getHardware().getDiskStores(), diskButton);

        List<NetworkIF> networkIFs = si.getHardware().getNetworkIFs(true);

        JGradientButton[] netButton = new JGradientButton[networkIFs.size()];
        for (int i = 0; i < networkIFs.size() ; i++)
        {
            NetworkIF net = networkIFs.get(i);
            netButton[i] = createButton(NetworkPanel.updateNetwork(net, 0, 0), 'N', "Display Network",Color.CYAN.brighter() , new NetworkPanel(net, i), displayPanel);
            perfMenuBar.add(netButton[i]);
        }
        NetworkPanel.updateNetWorkInfo(networkIFs, netButton);

        JScrollPane scrollPerfPanel = new JScrollPane(perfMenuBar);
        scrollPerfPanel.getVerticalScrollBar().setUnitIncrement(30);
        scrollPerfPanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPerfPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        scrollPerfPanel.setBounds(0, 0, 295, 935);
        perfPanel.add(scrollPerfPanel);

        displayPanel.setBounds(295, 0, 1205, 935);
        displayPanel.setBackground(Color.WHITE);
        perfPanel.add(displayPanel);

        GridBagConstraints perfConstraints = new GridBagConstraints();
        perfConstraints.gridx = 0;
        perfConstraints.gridy = 0;
        perfConstraints.weightx = 1d;
        perfConstraints.weighty = 1d;
        perfConstraints.fill = GridBagConstraints.BOTH;
        perfConstraints.anchor = GridBagConstraints.NORTHWEST;
        perfPanel.setBackground(Color.WHITE);
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
            int nComponents = displayPanel.getComponents().length;
            if (nComponents <= 0 || displayPanel.getComponent(0) != panel) {
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
