package gui;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.profesorfalken.wmi4java.WMI4Java;

import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Displays GPU performance stats similar to Task Manager with system-updated info.
 */
public class GPUinPer extends OshiJPanel { // NOSONAR squid:S110

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(GPUinPer.class);
    private JLabel titleLabel;
    private JLabel infoLabel;
    private DynamicTimeSeriesCollection gpuData;
    private String driverVersion = "N/A";
    private String driverDate = "N/A";
    private String directXVersion = "N/A";
    private String physicalLocation = "N/A";
    private double totalMemoryGB;
    private JComboBox<String> graphSelector;
    private JFreeChart gpuChart;
    private ChartPanel chartPanel;

    // Dữ liệu giả lập cho các loại đồ thị
    private double utilization3D = 0.0;
    private double sharedMemoryUsagePercent = 0.0;
    private double videoEncodeUsage = 0.0;
    private double videoDecodeUsage = 0.0;
	private GraphicsCard gpu;

    public GPUinPer(GraphicsCard gpu) {
        super();
        this.gpu = gpu;
        init(gpu);
    }

    private void init(GraphicsCard gpu) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(isDarkMode ? DARK_BORDER : LIGHT_BORDER));

        // Lấy thông tin tĩnh một lần khi khởi tạo
        initializeStaticData(gpu);

        // Tiêu đề và dropdown menu
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);

        titleLabel = new JLabel("GPU  " + gpu.getName());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Dropdown menu để chọn loại đồ thị
        String[] graphOptions = {"3D", "Shared GPU memory", "Video Encode", "Video Decode"};
        graphSelector = new JComboBox<>(graphOptions);
        graphSelector.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        graphSelector.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        graphSelector.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        graphSelector.addActionListener(e -> updateChart());
        headerPanel.add(graphSelector, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Đồ thị
        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        gpuData = new DynamicTimeSeriesCollection(1, 60, new Second());
        gpuData.setTimeBase(new Second(date));
        gpuData.addSeries(floatArrayPercent(0d), 0, "3D");
        gpuChart = ChartFactory.createTimeSeriesChart("", "", "%", gpuData, true, true, false);
        styleChart(gpuChart, Color.CYAN);
        gpuChart.getXYPlot().getRangeAxis().setAutoRange(false);
        gpuChart.getXYPlot().getRangeAxis().setRange(0d, 100d);

        chartPanel = new ChartPanel(gpuChart);
        chartPanel.setPreferredSize(new Dimension(300, 150));
        chartPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        add(chartPanel, BorderLayout.CENTER);

        // Thông tin bổ sung
        infoLabel = new JLabel(buildInfoText(gpu, 0, 0));
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        infoLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(infoLabel, BorderLayout.SOUTH);

        // Cập nhật dữ liệu động theo thời gian
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> updateDynamicData(gpu), 0, 2, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
	private void initializeStaticData(GraphicsCard gpu) {
        // Lấy thông tin tĩnh từ WMI (Win32_VideoController)
        try {
            // Truy vấn WMI class Win32_VideoController
            List<Map<String, String>> gpuWmiInfoList = (List<Map<String, String>>) WMI4Java.get()
                    .VBSEngine()
                    .getWMIObject("Win32_VideoController");

            if (gpuWmiInfoList.isEmpty()) {
                logger.warn("Không tìm thấy thông tin GPU qua WMI.");
                return;
            }

            // Duyệt danh sách GPU
            for (Map<String, String> gpuWmiInfo : gpuWmiInfoList) {
                String name = gpuWmiInfo.getOrDefault("Name", "");
                if (name.equalsIgnoreCase(gpu.getName())) {
                    driverVersion = gpuWmiInfo.getOrDefault("DriverVersion", "N/A");
                    driverDate = gpuWmiInfo.getOrDefault("DriverDate", "N/A");
                    physicalLocation = gpuWmiInfo.getOrDefault("PNPDeviceID", "N/A");
                    // Lấy VRAM từ AdapterRAM
                    String memoryStr = gpuWmiInfo.getOrDefault("AdapterRAM", "0");
                    try {
                        long vramBytes = Long.parseLong(memoryStr);
                        totalMemoryGB = vramBytes / (1024.0 * 1024 * 1024); // Chuyển từ bytes sang GB
                        if (totalMemoryGB <= 0) {
                            logger.warn("VRAM lấy được không hợp lệ, sử dụng giá trị mặc định 8GB.");
                            totalMemoryGB = 8.0; // Giá trị mặc định nếu không hợp lệ
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("Không thể chuyển đổi AdapterRAM thành số, sử dụng giá trị mặc định 8GB.");
                        totalMemoryGB = 8.0; // Giá trị mặc định nếu lỗi
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Lỗi khi truy xuất WMI: ", e);
            totalMemoryGB = 8.0; // Giá trị mặc định nếu WMI thất bại
        }

        // Lấy DirectX version từ registry
        directXVersion = getDirectXVersionFromRegistry();
    }

    private String getDirectXVersionFromRegistry() {
        try {
            // Chạy lệnh reg query để lấy DirectX version từ registry
            ProcessBuilder pb = new ProcessBuilder("reg", "query", "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\DirectX", "/v", "Version");
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Version")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length > 2) {
                            String version = parts[parts.length - 1].trim();
                            // Ánh xạ phiên bản
                            if (version.startsWith("4.09")) {
                                return "9.0c";
                            } else if (version.startsWith("4.10")) {
                                return "10";
                            } else if (version.startsWith("4.11")) {
                                return "11";
                            } else if (version.startsWith("4.12")) {
                                return "12";
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error retrieving DirectX version from registry: ", e);
        }
        return "N/A"; // Trả về giá trị mặc định nếu lỗi
    }

    private void updateDynamicData(GraphicsCard gpu) {
        gpuData.advanceTime();

        // Giả lập dữ liệu cho các loại đồ thị
        try {
            utilization3D = Math.random() * 100; // Giả lập Utilization 3D
            long usedMemory = (long) (Math.random() * gpu.getVRam()); // Giả lập Used Memory
            double sharedMemoryUsageGB = usedMemory / (1024.0 * 1024 * 1024); // bytes -> GB
            sharedMemoryUsagePercent = (sharedMemoryUsageGB / totalMemoryGB * 100);
            videoEncodeUsage = Math.random() * 100; // Giả lập Video Encode
            videoDecodeUsage = Math.random() * 100; // Giả lập Video Decode

            // Cập nhật đồ thị dựa trên lựa chọn
            updateChart();

            // Cập nhật thông tin
            infoLabel.setText(buildInfoText(gpu, utilization3D, usedMemory));
        } catch (Exception e) {
            logger.error("Error retrieving GPU performance data: ", e);
        }
    }

    private void updateChart() {
        String selectedGraph = (String) graphSelector.getSelectedItem();
        gpuData= new DynamicTimeSeriesCollection(1, 200);
        
        switch (selectedGraph) {
            case "3D":
                gpuData.addSeries(floatArrayPercent(utilization3D), 0, "3D");
                styleChart(gpuChart, Color.CYAN);
                break;
            case "Shared GPU memory":
                gpuData.addSeries(floatArrayPercent(sharedMemoryUsagePercent), 0, "Shared GPU memory");
                styleChart(gpuChart, Color.MAGENTA);
                break;
            case "Video Encode":
                gpuData.addSeries(floatArrayPercent(videoEncodeUsage), 0, "Video Encode");
                styleChart(gpuChart, Color.GREEN);
                break;
            case "Video Decode":
                gpuData.addSeries(floatArrayPercent(videoDecodeUsage), 0, "Video Decode");
                styleChart(gpuChart, Color.YELLOW);
                break;
        }

        chartPanel.repaint();
    }

    private String buildInfoText(GraphicsCard gpu, double utilization, long usedMemory) {
        double usedMemoryGB = usedMemory / (1024.0 * 1024 * 1024); // bytes -> GB

        return String.format("<html>Utilization: %.0f%%<br>"
                + "Shared GPU memory: %.1f/%.1f GB<br>"
                + "Memory: %.1f/%.1f GB<br>"
                + "Driver version: %s<br>"
                + "Driver date: %s<br>"
                + "DirectX version: %s<br>"
                + "Physical location: %s</html>",
                utilization,
                usedMemoryGB, totalMemoryGB, // Shared GPU memory
                usedMemoryGB, totalMemoryGB, // Memory
                driverVersion, driverDate, directXVersion, physicalLocation);
    }

    private void styleChart(JFreeChart chart, Color color) {
        XYPlot plot = chart.getXYPlot();
        XYAreaRenderer renderer = new XYAreaRenderer();
        Color baseColor = isDarkMode ? color : color.brighter();
        renderer.setSeriesPaint(0, new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 128));
        renderer.setOutline(true);
        renderer.setSeriesOutlineStroke(0, new BasicStroke(1.0f));
        renderer.setSeriesOutlinePaint(0, baseColor.darker());
        plot.setRenderer(renderer);
        plot.setBackgroundPaint(isDarkMode ? DARK_PANEL_BG : LIGHT_PANEL_BG);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.GRAY);
        plot.setDomainGridlinePaint(Color.GRAY);
        plot.getDomainAxis().setVisible(false);
        plot.getRangeAxis().setVisible(false);
    }

    private float[] floatArrayPercent(double d) {
        float[] f = new float[1];
        f[0] = (float) d;
        return f;
    }

    @Override
    public void updateTheme() {
        super.updateTheme();
        titleLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        infoLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        graphSelector.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        graphSelector.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        revalidate();
        repaint();
    }
}