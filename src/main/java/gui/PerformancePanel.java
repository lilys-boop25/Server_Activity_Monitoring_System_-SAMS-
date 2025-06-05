package gui;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;
import oshi.hardware.GraphicsCard;
import oshi.hardware.GlobalMemory;

public class PerformancePanel extends OshiJPanel {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(PerformancePanel.class);
    private JPanel displayPanel; // Khai báo
    private SystemInfo si;
    private JGradientButton cpuButton;
    private JGradientButton memButton;
    private JGradientButton[] diskButtons;
    private JGradientButton[] netButtons;
    private JGradientButton[] gpuButtons;

    public PerformancePanel() {
        super();
    }

    public PerformancePanel(SystemInfo si) {
        super(); // Gọi constructor cha trước
        this.si = si;
        OshiJPanel.isDarkMode = true; // Bật dark mode mặc định
        this.displayPanel = new JPanel(); // Khởi tạo displayPanel ngay trong constructor
        displayPanel.setLayout(new GridBagLayout());
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            logger.error("Error setting look and feel: ", ex);
        }
        initialize(si); // Gọi initialize sau khi khởi tạo displayPanel
    }

    private void initialize(SystemInfo si) {
        // Panel chính với layout null để định vị thủ công
        JPanel perfPanel = new JPanel();
        perfPanel.setLayout(null);
        perfPanel.setBackground(isDarkMode ? new Color(32, 32, 32) : Color.WHITE);

        // Khu vực hiển thị chi tiết (bên phải) - Sử dụng displayPanel đã khởi tạo
        displayPanel.setBounds(295, 0, 1205, 935);
        displayPanel.setBackground(isDarkMode ? new Color(32, 32, 32) : Color.WHITE);
        perfPanel.add(displayPanel);

        // Thanh menu bên trái (danh sách các nút)
        JPanel perfMenuBar = new JPanel();
        perfMenuBar.setLayout(new BoxLayout(perfMenuBar, BoxLayout.Y_AXIS));
        perfMenuBar.setBackground(isDarkMode ? new Color(32, 32, 32) : Color.WHITE);

        // Thêm CPU Button
        CentralProcessor cpu = si.getHardware().getProcessor();
        cpuButton = createButton("\nCPU\n0%\n", Color.GREEN, new CPUinPer(si), displayPanel);
        perfMenuBar.add(cpuButton);
        CPUinPer updatcpuInfo = new CPUinPer(si);
        updatcpuInfo.updateCPUInfo();

        // Thêm Memory Button
        memButton = createButton(
            PerformancePanel.buttonTextLines("\nMemory\n0/0 GB (0%)\n"),
            Color.GREEN,
            new MemoryinPer(si),
            displayPanel
        );
        perfMenuBar.add(memButton);
        MemoryinPer.updateMemoryInfo(si.getHardware().getMemory(), memButton);

        // Thêm Disk Buttons
        List<HWDiskStore> diskStores = si.getHardware().getDiskStores();
        diskButtons = new JGradientButton[diskStores.size()];
        for (int i = 0; i < diskStores.size(); i++) {
            HWDiskStore disk = diskStores.get(i);
            diskButtons[i] = createButton(
                DiskinPer.updateDisk(disk, i, 0, 0),
                Color.PINK.darker(),
                new DiskinPer(disk, i),
                displayPanel
            );
            perfMenuBar.add(diskButtons[i]);
        }
        DiskinPer.updateDiskInfo(diskStores, diskButtons);

        // Thêm Network Buttons
        List<NetworkIF> networkIFs = si.getHardware().getNetworkIFs(false);
        netButtons = new JGradientButton[networkIFs.size()];
        for (int i = 0; i < networkIFs.size(); i++) {
            NetworkIF net = networkIFs.get(i);
            netButtons[i] = createButton(
                NetworkinPer.updateNetwork(net, 0, 0),
                Color.CYAN.brighter(),
                new NetworkinPer(net, i),
                displayPanel
            );
            perfMenuBar.add(netButtons[i]);
        }
        NetworkinPer.updateNetworkInfo(networkIFs, netButtons);

        // Thêm GPU Buttons
        List<GraphicsCard> graphicsCards = si.getHardware().getGraphicsCards();
        gpuButtons = new JGradientButton[graphicsCards.size()];
        for (int i = 0; i < graphicsCards.size(); i++) {
            GraphicsCard gpu = graphicsCards.get(i);
            gpuButtons[i] = createButton(
                PerformancePanel.buttonTextLines("\nGPU " + i + "\n0%\n"),
                Color.CYAN,
                new GPUinPer(gpu),
                displayPanel
            );
            perfMenuBar.add(gpuButtons[i]);
        }

        // Cập nhật thông tin tóm tắt trên các nút
        Timer updateTimer = new Timer(1000, e -> {
            // Cập nhật CPU
            double cpuLoad = CPUinPer.getLoad() * 100;
            cpuButton.setText(PerformancePanel.buttonTextLines("\nCPU\n" + String.format("%.0f%%\n", cpuLoad)));
            cpuButton.setCustomColor(getColorByPercent(cpuLoad));

            // Cập nhật Memory
            GlobalMemory memory = si.getHardware().getMemory();
            MemoryinPer.updateMemoryInfo(memory, memButton);

            // Cập nhật GPU
            for (int i = 0; i < gpuButtons.length; i++) {
                double gpuUtilization = getGPUUtilization();
                gpuButtons[i].setText(PerformancePanel.buttonTextLines("\nGPU " + i + "\n" + String.format("%.0f%%\n", gpuUtilization)));
                gpuButtons[i].setCustomColor(getColorByPercent(gpuUtilization));
            }
        });
        updateTimer.start();

        // Thêm JScrollPane cho perfMenuBar
        JScrollPane scrollPerfPanel = new JScrollPane(perfMenuBar);
        scrollPerfPanel.getVerticalScrollBar().setUnitIncrement(30);
        scrollPerfPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPerfPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPerfPanel.setBounds(0, 0, 295, 935);
        scrollPerfPanel.setBackground(isDarkMode ? new Color(32, 32, 32) : Color.WHITE);
        scrollPerfPanel.getViewport().setBackground(isDarkMode ? new Color(32, 32, 32) : Color.WHITE);
        perfPanel.add(scrollPerfPanel);

        GridBagConstraints perfConstraints = new GridBagConstraints();
        perfConstraints.gridx = 0;
        perfConstraints.gridy = 0;
        perfConstraints.weightx = 1.0;
        perfConstraints.weighty = 1.0;
        perfConstraints.fill = GridBagConstraints.BOTH;
        perfConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(perfPanel, perfConstraints);

        // Mặc định hiển thị CPU panel
        cpuButton.doClick();
    }

    // Phương thức lấy GPU Utilization từ PowerShell
    private double getGPUUtilization() {
        try {
            String command = "powershell \"(Get-Counter -Counter '\\GPU Engine(pid_*_engtype_3D)\\Utilization Percentage' -ErrorAction SilentlyContinue).CounterSamples | Where-Object { $_.CookedValue -gt 0 } | Measure-Object -Property CookedValue -Sum | Select-Object -ExpandProperty Sum\"";
            String result = executeCommand(command);
            return Double.parseDouble(result.trim());
        } catch (Exception e) {
            return 0.0; // Nếu không lấy được, trả về 0
        }
    }

    // Phương thức thực thi lệnh PowerShell
    private String executeCommand(String command) throws Exception {
        @SuppressWarnings("deprecation")
		Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        process.waitFor();
        return output.toString();
    }

    public static void setChartRenderer(JFreeChart chart, Color color) {
        XYAreaRenderer renderer = new XYAreaRenderer();
        renderer.setSeriesPaint(0, new Color(color.getRed(), color.getGreen(), color.getBlue(), 128));
        renderer.setOutline(true);
        renderer.setSeriesOutlineStroke(0, new BasicStroke(1.0f));
        renderer.setSeriesOutlinePaint(0, color.darker());

        chart.getXYPlot().setRenderer(renderer);
        chart.getPlot().setBackgroundPaint(isDarkMode ? new Color(32, 32, 32) : Color.WHITE);
        chart.getXYPlot().setDomainGridlinesVisible(true);
        chart.getXYPlot().setRangeGridlinesVisible(true);
        chart.getXYPlot().setRangeGridlinePaint(isDarkMode ? Color.GRAY : Color.BLACK);
        chart.getXYPlot().setDomainGridlinePaint(isDarkMode ? Color.GRAY : Color.BLACK);
        chart.getXYPlot().getDomainAxis().setVisible(false);
        chart.getXYPlot().getRangeAxis().setVisible(false);
    }

    public static void setChartRenderer(JFreeChart chart, Color color1, Color color2) {
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
        chart.getPlot().setBackgroundPaint(isDarkMode ? new Color(32, 32, 32) : Color.WHITE);
        chart.getXYPlot().setDomainGridlinesVisible(true);
        chart.getXYPlot().setRangeGridlinesVisible(true);
        chart.getXYPlot().setRangeGridlinePaint(isDarkMode ? Color.GRAY : Color.BLACK);
        chart.getXYPlot().setDomainGridlinePaint(isDarkMode ? Color.GRAY : Color.BLACK);
        chart.getXYPlot().getDomainAxis().setVisible(false);
        chart.getXYPlot().getRangeAxis().setVisible(false);
    }

    public static Color getColorByPercent(double percent) {
        if (percent < 60.0) {
            return new Color(0, 120, 215);
        } else if (percent < 80.0) {
            return new Color(255, 165, 0);
        } else {
            return new Color(255, 0, 0);
        }
    }

    private JGradientButton createButton(String title, Color color, OshiJPanel panel, JPanel displayPanel) {
        JGradientButton button = new JGradientButton(title);
        button.setCustomColor(color);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        button.setHorizontalTextPosition(SwingConstants.LEFT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setPreferredSize(new Dimension(280, 80));
        button.setBackground(isDarkMode ? new Color(32, 32, 32) : Color.WHITE);
        button.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        button.addActionListener(e -> {
            int nComponents = displayPanel.getComponentCount();
            if (nComponents == 0 || displayPanel.getComponent(0) != panel) {
                resetMainGui(displayPanel);
                GridBagConstraints constraints = new GridBagConstraints();
                constraints.gridx = 0;
                constraints.gridy = 0;
                constraints.weightx = 1.0;
                constraints.weighty = 1.0;
                constraints.fill = GridBagConstraints.BOTH;
                displayPanel.add(panel, constraints);
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

    public static String buttonTextLines(String txt) {
        return "<html>" + txt.replace("\n", "<br>");
    }

    public static String htmlSpace(int num) {
        return " ".repeat(num);
    }

    @Override
    public void updateTheme() {
        super.updateTheme();
        if (displayPanel != null) { // Kiểm tra null trước khi gọi
            displayPanel.setBackground(isDarkMode ? new Color(32, 32, 32) : Color.WHITE);
        }
        if (cpuButton != null) {
            cpuButton.setBackground(isDarkMode ? new Color(32, 32, 32) : Color.WHITE);
            cpuButton.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        }
        if (memButton != null) {
            memButton.setBackground(isDarkMode ? new Color(32, 32, 32) : Color.WHITE);
            memButton.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        }
        if (diskButtons != null) {
            for (JGradientButton diskButton : diskButtons) {
                diskButton.setBackground(isDarkMode ? new Color(32, 32, 32) : Color.WHITE);
                diskButton.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            }
        }
        if (netButtons != null) {
            for (JGradientButton netButton : netButtons) {
                netButton.setBackground(isDarkMode ? new Color(32, 32, 32) : Color.WHITE);
                netButton.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            }
        }
        if (gpuButtons != null) {
            for (JGradientButton gpuButton : gpuButtons) {
                gpuButton.setBackground(isDarkMode ? new Color(32, 32, 32) : Color.WHITE);
                gpuButton.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            }
        }
        revalidate();
        repaint();
    }
}