package gui;

import javax.swing.*;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYAreaRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;

public class PerformancePanel extends OshiJPanel{
    private static final Logger logger = LoggerFactory.getLogger(PerformancePanel.class);
    private JPanel displayPanel;
    private JScrollPane scrollPerfPanel;
    
    public PerformancePanel() {
        super();
    }

    public PerformancePanel(SystemInfo si) {
        super();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            logger.error("Error occurred: ", ex);
        }
        initial(si);
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
        chart.getXYPlot().setRangeGridlinePaint(new Color(230, 230, 230));
        chart.getXYPlot().setDomainGridlinePaint(new Color(230, 230, 230));
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
        chart.getXYPlot().setRangeGridlinePaint(new Color(230, 230, 230));
        chart.getXYPlot().setDomainGridlinePaint(new Color(230, 230, 230));
    }

    private void initial(SystemInfo si) {
        // Set layout for main panel
        this.setLayout(new BorderLayout());
        
        // Create Performance header label
        JLabel performanceLabel = new JLabel("Performance");
        performanceLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        performanceLabel.setForeground(Color.BLACK);
        performanceLabel.setBorder(BorderFactory.createEmptyBorder(15, 2, 10, 0));
        
        // Create header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.add(performanceLabel, BorderLayout.WEST);
        
        // Add separator line
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(new Color(230, 230, 230));
        separator.setBackground(new Color(230, 230, 230));
        headerPanel.add(separator, BorderLayout.SOUTH);

        // Create main content panel with responsive layout
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);
        
        // Create left sidebar for performance metrics
        JPanel perfMenuBar = new JPanel();
        perfMenuBar.setLayout(new BoxLayout(perfMenuBar, BoxLayout.Y_AXIS));
        perfMenuBar.setBorder(BorderFactory.createEmptyBorder());
        perfMenuBar.setBackground(Color.WHITE);

        // Create right display panel with responsive sizing
        displayPanel = new JPanel();
        displayPanel.setLayout(new GridBagLayout());
        displayPanel.setBackground(Color.WHITE);

        // Create CPU button
        JGradientButton cpuButton = createButton("\nCPU\n0%\n", Color.GREEN, new CPUPanel(si), displayPanel);
        perfMenuBar.add(cpuButton);
        CPUPanel.updateCPUInfo(si.getHardware().getProcessor(), cpuButton, si.getOperatingSystem());

        // Create Memory button
        JGradientButton memButton = createButton(PerformancePanel.buttonTextLines("\nMemory\n0/0 GB (0%)\n"), Color.GREEN,new MemoryPanel(si), displayPanel);
        perfMenuBar.add(memButton);
        MemoryPanel.updateMemoryInfo(si.getHardware().getMemory(), memButton);

        // Create Disk buttons
        List<HWDiskStore> hwDiskStore = si.getHardware().getDiskStores();
        JGradientButton[] diskButton = new JGradientButton[hwDiskStore.size()];
        for (int i = 0; i < hwDiskStore.size() ; i++)
        {
            HWDiskStore disk = hwDiskStore.get(i);
            diskButton[i] = createButton(DiskPanel.updateDisk(disk, i, 0, 0), Color.PINK.darker(), new DiskPanel(disk, i), displayPanel);
            perfMenuBar.add(diskButton[i]);
        }
        DiskPanel.updateDiskInfo(si.getHardware().getDiskStores(), diskButton);

        // Create Network buttons
        List<NetworkIF> networkIFs = si.getHardware().getNetworkIFs(false);
        JGradientButton[] netButton = new JGradientButton[networkIFs.size()];
        for (int i = 0; i < networkIFs.size() ; i++)
        {
            NetworkIF net = networkIFs.get(i);
            netButton[i] = createButton(NetworkPanel.updateNetwork(net, 0, 0),Color.CYAN.brighter() , new NetworkPanel(net, i), displayPanel);
            perfMenuBar.add(netButton[i]);
        }
        NetworkPanel.updateNetWorkInfo(networkIFs, netButton);

        // Create scroll pane for left sidebar with responsive width
        scrollPerfPanel = new JScrollPane(perfMenuBar);
        scrollPerfPanel.getVerticalScrollBar().setUnitIncrement(30);
        scrollPerfPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPerfPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPerfPanel.setBorder(BorderFactory.createEmptyBorder());
        scrollPerfPanel.getViewport().setBorder(null);
        scrollPerfPanel.setOpaque(false);
        scrollPerfPanel.getViewport().setOpaque(false);
        
        // Set responsive preferred size for sidebar
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int sidebarWidth = Math.min(300, Math.max(200, (int)(screenSize.width * 0.2))); // 20% of screen width, min 200px, max 300px
        scrollPerfPanel.setPreferredSize(new Dimension(sidebarWidth, 0));
        scrollPerfPanel.setMinimumSize(new Dimension(200, 0));

        // Add components to content panel using BorderLayout for automatic resizing
        contentPanel.add(scrollPerfPanel, BorderLayout.WEST);
        contentPanel.add(displayPanel, BorderLayout.CENTER);

        // Add component listener for responsive behavior
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateResponsiveLayout();
            }
        });

        // Add components to main panel
        this.add(headerPanel, BorderLayout.NORTH);
        this.add(contentPanel, BorderLayout.CENTER);
        
        // Set background
        this.setBackground(Color.WHITE);

        // Click CPU button by default
        cpuButton.doClick();
    }

    private void updateResponsiveLayout() {
        // Update sidebar width based on current panel size
        int currentWidth = this.getWidth();
        if (currentWidth > 0) {
            int sidebarWidth = Math.min(300, Math.max(200, (int)(currentWidth * 0.2)));
            scrollPerfPanel.setPreferredSize(new Dimension(sidebarWidth, 0));
            
            // Force layout update
            this.revalidate();
            this.repaint();
        }
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

    private JGradientButton createButton(String title, Color color, OshiJPanel panel, JPanel displayPanel)
    {
        JGradientButton button = new JGradientButton(title);
        
        // Make button width responsive to sidebar width
        int width = 200; // Base width
        int height = 90;
        button.setPreferredSize(new Dimension(width, height));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, height)); // Allow horizontal expansion
        button.setMinimumSize(new Dimension(150, height));

        button.setMargin(new Insets(5, 10, 5, 10));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBackground(new Color(255, 255, 255, 20));

        button.color = color;
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setHorizontalTextPosition(SwingConstants.LEFT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(220, 220, 220, 50));
                button.setOpaque(true);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setOpaque(false);
            }
        });

        // Click action
        button.addActionListener(e -> {
            int nComponents = displayPanel.getComponents().length;
            if (nComponents <= 0 || displayPanel.getComponent(0) != panel) {
                resetMainGui(displayPanel);
                displayPanel.add(panel, createResponsiveConstraints());
                refreshMainGui(displayPanel);
            }
        });
        return button;
    }

    private GridBagConstraints createResponsiveConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(10, 10, 10, 10); // Add some padding
        return constraints;
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
        return "<html>" + htmlSpace(3) + txt.replace("\n", "<br>" + htmlSpace(3));
    }

    public static String htmlSpace(int num)
    {
        return "&nbsp;".repeat(num);
    }
}