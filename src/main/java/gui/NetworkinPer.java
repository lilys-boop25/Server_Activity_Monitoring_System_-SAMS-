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

import oshi.hardware.NetworkIF;
import oshi.util.FormatUtil;

public class NetworkinPer extends OshiJPanel { // NOSONAR squid:S110

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(NetworkinPer.class);
    private JLabel titleLabel;
    private JLabel infoLabel;
    private DynamicTimeSeriesCollection networkData;
    private int index;
    private NetworkIF net;

    public NetworkinPer(NetworkIF net, int index) {
        super();
        this.net = net;
        this.index = index;
        init(net, index);
    }

    private void init(NetworkIF net, int index) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(isDarkMode ? DARK_BORDER : LIGHT_BORDER));

        // Tiêu đề
        String networkName = net.getDisplayName();
        titleLabel = new JLabel(networkName + "  " + net.getIfAlias());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(titleLabel, BorderLayout.NORTH);

        // Đồ thị Network utilization
        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        networkData = new DynamicTimeSeriesCollection(1, 60, new Second());
        networkData.setTimeBase(new Second(date));
        networkData.addSeries(floatArrayPercent(0d), 0, "Network utilization");
        JFreeChart netChart = ChartFactory.createTimeSeriesChart("", "", "%", networkData, true, true, false);
        styleChart(netChart, Color.GREEN);
        netChart.getXYPlot().getRangeAxis().setAutoRange(false);
        netChart.getXYPlot().getRangeAxis().setRange(0d, 100d);

        ChartPanel chartPanel = new ChartPanel(netChart);
        chartPanel.setPreferredSize(new Dimension(300, 150));
        chartPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        add(chartPanel, BorderLayout.CENTER);

        // Thông tin bổ sung
        infoLabel = new JLabel(buildInfoText(net));
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        infoLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(infoLabel, BorderLayout.SOUTH);

        // Cập nhật dữ liệu theo thời gian
        new Timer(1000, e -> update(net)).start();
    }

    private void update(NetworkIF net) {
        net.updateAttributes();
        int newest = networkData.getNewestIndex();
        networkData.advanceTime();

        // Tính toán Network utilization dựa trên tốc độ gửi/nhận và Link speed
        long sendSpeed = networkSentSpeed.get(index);
        long recvSpeed = networkRecvSpeed.get(index);
        long linkSpeed = net.getSpeed(); // bps
        double utilization = linkSpeed > 0 ? ((sendSpeed + recvSpeed) * 8.0 * 100) / linkSpeed : 0; // Chuyển byte/s sang bit/s
        utilization = Math.min(100.0, utilization); // Giới hạn tối đa 100%
        networkData.addValue(0, newest, (float) utilization);

        // Cập nhật thông tin
        infoLabel.setText(buildInfoText(net));
    }

    private String buildInfoText(NetworkIF net) {
        long sendSpeed = networkSentSpeed.get(index);
        long recvSpeed = networkRecvSpeed.get(index);
        long linkSpeed = net.getSpeed() / 1_000_000; // Chuyển từ bps sang Mbps
        String[] ipAddresses = net.getIPv4addr();
        String ipv4 = ipAddresses.length > 0 ? ipAddresses[0] : "N/A";
        String[] ipv6Addresses = net.getIPv6addr();
        String ipv6 = ipv6Addresses.length > 0 ? ipv6Addresses[0] : "N/A";
        String ssid = "N/A"; // oshi không cung cấp trực tiếp SSID, cần dùng API hệ thống
        String signalQuality = "N/A"; // oshi không cung cấp trực tiếp, cần dùng API hệ thống

        return String.format("<html>Network utilization: %.0f%%<br>Throughput: %s (Send), %s (Receive)<br>Link speed: %d Mbps<br>Link state: %s<br>Adapter name: %s<br>Connection type: %s<br>IPv4 address: %s<br>IPv6 address: %s<br>SSID: %s<br>Signal quality: %s</html>",
                calculateUtilization(net), FormatUtil.formatBytes(sendSpeed), FormatUtil.formatBytes(recvSpeed),
                linkSpeed, net.isConnectorPresent() ? "Connected" : "Disconnected",
                net.getIfAlias(), net.getName().contains("Wi-Fi") ? "Wi-Fi" : "Ethernet",
                ipv4, ipv6, ssid, signalQuality);
    }

    private double calculateUtilization(NetworkIF net) {
        long sendSpeed = networkSentSpeed.get(index);
        long recvSpeed = networkRecvSpeed.get(index);
        long linkSpeed = net.getSpeed(); // bps
        return linkSpeed > 0 ? ((sendSpeed + recvSpeed) * 8.0 * 100) / linkSpeed : 0; // Chuyển byte/s sang bit/s
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

    protected static List<Long> networkSentSpeed = new ArrayList<>(Collections.nCopies(100, 0L));
    protected static List<Long> networkRecvSpeed = new ArrayList<>(Collections.nCopies(100, 0L));
    private static boolean run = false;

    public static void updateNetworkInfo(List<NetworkIF> networkIFs, JGradientButton[] netButton) {
        if (run) return;
        run = true;
        Thread thread = new Thread(() -> {
            long[] timeNow = new long[networkIFs.size()];
            long[] recvLast = new long[networkIFs.size()];
            long[] sentLast = new long[networkIFs.size()];
            while (true) {
                for (int i = 0; i < networkIFs.size(); i++) {
                    NetworkIF net = networkIFs.get(i);
                    timeNow[i] = net.getTimeStamp();
                    recvLast[i] = net.getBytesRecv();
                    sentLast[i] = net.getBytesSent();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    logger.error("Error occurred: ", e1);
                }
                for (int i = 0; i < networkIFs.size(); i++) {
                    NetworkIF net = networkIFs.get(i);
                    net.updateAttributes();
                    long timeDelta = net.getTimeStamp() - timeNow[i];
                    if (timeDelta > 0) {
                        networkSentSpeed.set(i, (net.getBytesSent() - sentLast[i]) * 1000 / timeDelta);
                        networkRecvSpeed.set(i, (net.getBytesRecv() - recvLast[i]) * 1000 / timeDelta);
                    }
                    netButton[i].setText(updateNetwork(net, networkRecvSpeed.get(i), networkSentSpeed.get(i)));
                }
            }
        });
        thread.start();
    }

    public static String updateNetwork(NetworkIF net, long recvSpeed, long sendSpeed) {
        String name = net.getDisplayName();
        if (name.length() > 30) {
            name = name.substring(0, 30) + "...";
        }
        String alias = net.getIfAlias();
        if (alias.length() > 30) {
            alias = alias.substring(0, 30) + "...";
        }
        String txt = name + "\n" + alias + "\nSend: " + FormatUtil.formatBytes(sendSpeed) + "\nReceive: " + FormatUtil.formatBytes(recvSpeed);
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