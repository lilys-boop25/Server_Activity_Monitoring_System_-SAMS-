package gui;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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

import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.util.FormatUtil;

public class DiskinPer extends OshiJPanel { // NOSONAR squid:S110

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(DiskinPer.class);
    private JLabel titleLabel;
    private JLabel infoLabel;
    private DynamicTimeSeriesCollection diskData;
    private int index;

    public DiskinPer(HWDiskStore disk, int index) {
        super();
        this.index = index;
        init(disk, index);
    }

    private void init(HWDiskStore disk, int index) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(isDarkMode ? DARK_BORDER : LIGHT_BORDER));

        // Tiêu đề
        String diskName = buildDiskName(disk);
        titleLabel = new JLabel(diskName + "  " + disk.getModel());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(titleLabel, BorderLayout.NORTH);

        // Đồ thị Active time
        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        diskData = new DynamicTimeSeriesCollection(1, 60, new Second());
        diskData.setTimeBase(new Second(date));
        diskData.addSeries(floatArrayPercent(0d), 0, "Active time");
        JFreeChart diskChart = ChartFactory.createTimeSeriesChart("", "", "%", diskData, true, true, false);
        styleChart(diskChart, Color.GREEN);
        diskChart.getXYPlot().getRangeAxis().setAutoRange(false);
        diskChart.getXYPlot().getRangeAxis().setRange(0d, 100d);

        ChartPanel chartPanel = new ChartPanel(diskChart);
        chartPanel.setPreferredSize(new Dimension(300, 150));
        chartPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        add(chartPanel, BorderLayout.CENTER);

        // Thông tin bổ sung
        infoLabel = new JLabel(buildInfoText(disk));
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        infoLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(infoLabel, BorderLayout.SOUTH);

        // Cập nhật dữ liệu theo thời gian
        new Timer(1000, e -> update(disk)).start();
    }

    private void update(HWDiskStore disk) {
        disk.updateAttributes();
        int newest = diskData.getNewestIndex();
        diskData.advanceTime();

        // Cập nhật Active time (giả lập dựa trên hoạt động đọc/ghi)
        long totalActivity = disk.getReadBytes() + disk.getWriteBytes();
        double activeTime = Math.min(100.0, (totalActivity > 0 ? (totalActivity / (1024 * 1024)) : 0)); // Ước lượng
        diskData.addValue(0, newest, (float) activeTime);

        // Cập nhật thông tin
        infoLabel.setText(buildInfoText(disk));
    }

    private String buildDiskName(HWDiskStore disk) {
        StringBuilder nameBuffer = new StringBuilder();
        nameBuffer.append("Disk ");
        for (HWPartition partition : disk.getPartitions()) {
            nameBuffer.append(partition.getMountPoint()).append(" ");
        }
        if (!disk.getPartitions().isEmpty()) {
            nameBuffer.setLength(nameBuffer.length() - 1); // Xóa dấu cách cuối
            nameBuffer.append(")");
        }
        return "Disk " + (nameBuffer.length() > 0 ? nameBuffer.toString() : "");
    }

    private String buildInfoText(HWDiskStore disk) {
        long totalCapacity = 0;
        for (HWPartition partition : disk.getPartitions()) {
            totalCapacity += partition.getSize();
        }
        String capacity = FormatUtil.formatBytes(totalCapacity);

        // Lấy dữ liệu tốc độ đọc/ghi từ diskReadSpeed và diskWriteSpeed
        long readSpeed = diskReadSpeed.get(index);
        long writeSpeed = diskWriteSpeed.get(index);

        // Giả lập Average response time (cần dữ liệu thực tế nếu có)
        // Có thể tính dựa trên sự khác biệt thời gian và hoạt động, nhưng hiện tại dùng giá trị mẫu
        String avgResponseTime = String.format("%.1f ms", calculateAvgResponseTime(disk));

        return String.format("<html>Active time: %.0f%%<br>"
        		+ "Disk transfer rate: %s (Read), %s (Write)<br>"
        		+ "Average response time: %s<br>"
        		+ "Capacity: %s<br>Formatted: %s<br>"
        		+ "System disk: Yes<br>"
        		+ "Page file: Yes<br>"
        		+ "Type: SSD (NVMe)</html>",
                Math.min(100.0, (disk.getReadBytes() + disk.getWriteBytes()) / (1024 * 1024)), // Active time
                FormatUtil.formatBytes(readSpeed), FormatUtil.formatBytes(writeSpeed),
                avgResponseTime, capacity, capacity);
    }

    private double calculateAvgResponseTime(HWDiskStore disk) {
        // Giả lập Average response time dựa trên sự thay đổi thời gian và hoạt động
        // Đây là giá trị mẫu, cần tích hợp dữ liệu thực tế từ oshi hoặc WMI nếu có
        long currentTime = disk.getTimeStamp();
        long lastRead = disk.getReadBytes();
        long lastWrite = disk.getWriteBytes();
        // Giả sử response time tỷ lệ nghịch với tốc độ (cần dữ liệu thực tế)
        double activityLevel = (lastRead + lastWrite) / (1024.0 * 1024); // MB
        return activityLevel > 0 ? Math.max(10.0, 50.0 / (activityLevel + 1)) : 41.7; // Giá trị mẫu
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

    protected static List<Long> diskReadSpeed = new ArrayList<>(Collections.nCopies(100, 0L));
    protected static List<Long> diskWriteSpeed = new ArrayList<>(Collections.nCopies(100, 0L));
    private static boolean run = false;

    protected static void updateDiskInfo(List<HWDiskStore> diskStores, JGradientButton[] diskButton) {
        if (run) return;
        run = true;
        Thread thread = new Thread(() -> {
            long[] timeNow = new long[diskStores.size()];
            long[] readLast = new long[diskStores.size()];
            long[] writeLast = new long[diskStores.size()];
            while (true) {
                for (int i = 0; i < diskStores.size(); i++) {
                    HWDiskStore disk = diskStores.get(i);
                    timeNow[i] = disk.getTimeStamp();
                    readLast[i] = disk.getReadBytes();
                    writeLast[i] = disk.getWriteBytes();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    logger.error("Error occurred: ", e1);
                }
                for (int i = 0; i < diskStores.size(); i++) {
                    HWDiskStore disk = diskStores.get(i);
                    disk.updateAttributes();
                    long readNow = disk.getReadBytes();
                    long writeNow = disk.getWriteBytes();
                    diskReadSpeed.set(i, (readNow - readLast[i]) * 1000 / (disk.getTimeStamp() - timeNow[i]));
                    diskWriteSpeed.set(i, (writeNow - writeLast[i]) * 1000 / (disk.getTimeStamp() - timeNow[i]));
                    diskButton[i].setText(updateDisk(disk, i, diskReadSpeed.get(i), diskWriteSpeed.get(i)));
                }
            }
        });
        thread.start();
    }

    public static String updateDisk(HWDiskStore disk, int index, long recvSpeed, long sendSpeed) {
        StringBuilder nameBuffer = new StringBuilder();
        nameBuffer.append("Disk ").append(index).append(" (");
        for (HWPartition partition : disk.getPartitions()) {
            nameBuffer.append(partition.getMountPoint()).append(" ");
        }
        if (!disk.getPartitions().isEmpty()) {
            nameBuffer.setLength(nameBuffer.length() - 1);
            nameBuffer.append(")");
        }
        String name = nameBuffer.length() > 30 ? nameBuffer.substring(0, 30) + "..." : nameBuffer.toString();
        String txt = name + "\nRead: " + FormatUtil.formatBytes(sendSpeed) + "\nWrite: " + FormatUtil.formatBytes(recvSpeed) + '\n';
        return PerformancePanel.buttonTextLines(txt);
    }

    @Override
    public void updateTheme() {
        super.updateTheme();
        titleLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        infoLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        revalidate();
        repaint();
    }
}