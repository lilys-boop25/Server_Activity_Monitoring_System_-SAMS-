package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;

import com.sun.jna.Platform;

import oshi.PlatformEnum;
import oshi.SystemInfo;
import oshi.driver.windows.perfmon.ProcessInformation.HandleCountProperty;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.software.os.OperatingSystem;
import oshi.util.tuples.Pair;

public class CPUinPer extends OshiJPanel {
    private static final long serialVersionUID = 1L;
    private SystemInfo si;
    private CentralProcessor processor;
    private OperatingSystem os;
    private long[] oldTicks;
    private long[][] oldProcTicks;
    private DynamicTimeSeriesCollection sysData;
    private List<DynamicTimeSeriesCollection> procData;
    private JPanel displayPanel; // Khai báo
    private JEditorPane utilValueText;
    private JEditorPane processesValueText;
    private JEditorPane threadValueText;
    private JEditorPane handleValueText;
    private static final String VALUE_HTML_HEAD = "<p style = \"font-size:23; font-family:Arial; color:%s\">";
    private static final String VALUE_HTML_TAIL = "</p>";
    private static final String JEDITOR_TYPE = "text/html";
    private static final PlatformEnum CURRENT_PLATFORM = PlatformEnum.getValue(Platform.getOSType());
    private static int nProcess = 0;
    private static int nThread = 0;
    private static long nHandle = 0;
    private static double load = 0d;
    private String currentView = "Overall utilization";

    public String getCurrentView() {
        return currentView;
    }

    public void setCurrentView(String currentView) {
        this.currentView = currentView;
    }

    public CPUinPer(SystemInfo si) {
        this.si = si;
        this.processor = si.getHardware().getProcessor();
        this.os = si.getOperatingSystem();
        this.oldTicks = new long[TickType.values().length];
        this.oldProcTicks = new long[processor.getLogicalProcessorCount()][TickType.values().length];
        this.displayPanel = new JPanel(new GridBagLayout()); // Khởi tạo displayPanel ngay trong constructor
        displayPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG); // Đặt màu nền ban đầu
        setLayout(new BorderLayout());
        initial();
    }

    private void initial() {
        // displayPanel đã được khởi tạo trong constructor, không cần khởi tạo lại
        add(displayPanel, BorderLayout.CENTER);

        // Thêm menu ngữ cảnh khi nhấn chuột phải
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem overallItem = new JMenuItem("Overall utilization");
        JMenuItem logicalItem = new JMenuItem("Logical processors");
        JMenuItem numaItem = new JMenuItem("NUMA nodes");
        popupMenu.add(overallItem);
        popupMenu.add(logicalItem);
        popupMenu.add(numaItem);

        overallItem.addActionListener(e -> {
            setCurrentView("Overall utilization");
            displayPanel.removeAll();
            displayPanel.add(initTotalCPU(), new GridBagConstraints());
            displayPanel.revalidate();
            displayPanel.repaint();
        });

        logicalItem.addActionListener(e -> {
            setCurrentView("Logical processors");
            displayPanel.removeAll();
            displayPanel.add(initSingleCPU(), new GridBagConstraints());
            displayPanel.revalidate();
            displayPanel.repaint();
        });

        numaItem.addActionListener(e -> {
            setCurrentView("NUMA nodes");
            displayPanel.removeAll();
            displayPanel.add(createNumaPanel(), new GridBagConstraints()); // Placeholder cho NUMA nodes
            displayPanel.revalidate();
            displayPanel.repaint();
        });

        // Thêm sự kiện chuột phải vào displayPanel
        displayPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        // update thông tin Processes, Threads, Handles
        updateCPUInfo();
    }

    private JPanel initTotalCPU() {
        JPanel cpuPanel = new JPanel(new GridBagLayout());
        cpuPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);

        GridBagConstraints textPanelConstraints = new GridBagConstraints();
        textPanelConstraints.gridwidth = 4;
        textPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        cpuPanel.add(createInfoPanel(), textPanelConstraints);

        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        sysData = new DynamicTimeSeriesCollection(1, 60, new Second());
        sysData.setTimeBase(new Second(date));
        sysData.addSeries(floatArrayPercent(cpuData()), 0, "All CPUs");

        JFreeChart systemCpuChart = ChartFactory.createTimeSeriesChart(null, null, "%", sysData, true, true, false);
        systemCpuChart.getXYPlot().getRangeAxis().setAutoRange(false);
        systemCpuChart.getXYPlot().getRangeAxis().setRange(0d, 100d);
        systemCpuChart.getXYPlot().getDomainAxis().setLowerMargin(0);
        systemCpuChart.getXYPlot().getDomainAxis().setUpperMargin(0);
        systemCpuChart.getXYPlot().getDomainAxis().setVisible(false);
        systemCpuChart.getXYPlot().getRangeAxis().setVisible(false);
        styleChart(systemCpuChart, Color.GREEN);
        systemCpuChart.removeLegend();

        ChartPanel systemCpuChartPanel = new ChartPanel(systemCpuChart);
        systemCpuChartPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        GridBagConstraints sysChartConstraints = new GridBagConstraints();
        sysChartConstraints.gridwidth = 2;
        sysChartConstraints.gridy = 2;
        sysChartConstraints.fill = GridBagConstraints.BOTH;
        sysChartConstraints.gridx = 0;
        cpuPanel.add(systemCpuChartPanel, sysChartConstraints);

        GridBagConstraints detaiPanelConstraints = new GridBagConstraints();
        detaiPanelConstraints.gridy = 3;
        detaiPanelConstraints.gridwidth = 2;
        cpuPanel.add(createDetailPanel(), detaiPanelConstraints);

        Timer timer = new Timer(1000, e -> {
            sysData.advanceTime();
            sysData.appendData(floatArrayPercent(getLoad()));
        });
        timer.start();

        return cpuPanel;
    }

    private JPanel initSingleCPU() {
        JPanel cpuinPer = new JPanel(new GridBagLayout());
        cpuinPer.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);

        GridBagConstraints textPanelConstraints = new GridBagConstraints();
        textPanelConstraints.gridwidth = 4;
        textPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        cpuinPer.add(createInfoPanel(), textPanelConstraints);

        JPanel chartPanel = new JPanel(new GridBagLayout());
        chartPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);

        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        double[] procUsage = procData();
        procData = new ArrayList<>();
        List<JFreeChart> procCpuChart = new ArrayList<>();
        List<ChartPanel> procCpuChartPanel = new ArrayList<>();

        for (int i = 0; i < procUsage.length; i++) {
            DynamicTimeSeriesCollection timeSeriesData = new DynamicTimeSeriesCollection(1, 60, new Second());
            timeSeriesData.setTimeBase(new Second(date));
            timeSeriesData.addSeries(floatArrayPercent(procUsage[i]), 0, "Core " + i);

            JFreeChart procChart = ChartFactory.createTimeSeriesChart(null, null, null, timeSeriesData, true, true, false);
            procChart.getXYPlot().getRangeAxis().setAutoRange(false);
            procChart.getXYPlot().getRangeAxis().setRange(0d, 100d);
            procChart.getXYPlot().getDomainAxis().setVisible(false);
            procChart.getXYPlot().getRangeAxis().setVisible(false);
            procChart.getXYPlot().getDomainAxis().setLowerMargin(0);
            procChart.getXYPlot().getDomainAxis().setUpperMargin(0);
            styleChart(procChart, Color.CYAN);
            procChart.removeLegend();

            procData.add(timeSeriesData);
            procCpuChart.add(procChart);
            procCpuChartPanel.add(new ChartPanel(procChart));
        }

        GridBagConstraints procConstraints = new GridBagConstraints();
        procConstraints.weightx = 1d;
        procConstraints.weighty = 1d;
        procConstraints.fill = GridBagConstraints.BOTH;
        int x = 0;
        for (int i = 0; i < procCpuChartPanel.size(); i++) {
            GridBagConstraints singleProcConstraints = (GridBagConstraints) procConstraints.clone();
            singleProcConstraints.gridx = x % 4;
            singleProcConstraints.gridy = x / 4;
            x++;
            chartPanel.add(procCpuChartPanel.get(i), singleProcConstraints);
        }

        GridBagConstraints chartPanelConstraints = new GridBagConstraints();
        chartPanelConstraints.gridy = 1;
        chartPanelConstraints.weighty = 1d;
        chartPanelConstraints.weightx = 1d;
        chartPanelConstraints.fill = GridBagConstraints.BOTH;
        cpuinPer.add(chartPanel, chartPanelConstraints);

        GridBagConstraints detaiPanelConstraints = new GridBagConstraints();
        detaiPanelConstraints.gridy = x / 4 + 1;
        cpuinPer.add(createDetailPanel(), detaiPanelConstraints);

        Timer timer = new Timer(1000, e -> {
            double[] procUsageData = procData();
            for (int i = 0; i < procUsageData.length; i++) {
                procData.get(i).advanceTime();
                procData.get(i).appendData(floatArrayPercent(procUsageData[i]));
            }
        });
        timer.start();

        return cpuinPer;
    }

    private JPanel createNumaPanel() {
        // Placeholder cho NUMA nodes 
        JPanel numaPanel = new JPanel(new GridBagLayout());
        numaPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        JLabel label = new JLabel("NUMA nodes (Not implemented)");
        label.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        numaPanel.add(label);
        return numaPanel;
    }

    private JPanel createDetailPanel() {
        JPanel detailPanel = new JPanel(new GridBagLayout());
        detailPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);

        GridBagConstraints sysCpuInfoConstraints = new GridBagConstraints();
        sysCpuInfoConstraints.fill = GridBagConstraints.HORIZONTAL;
        sysCpuInfoConstraints.gridx = 0;
        sysCpuInfoConstraints.gridy = 0;
        sysCpuInfoConstraints.ipadx = 20;

        String textColor = isDarkMode ? "white" : "gray";
        JEditorPane utilText = new JEditorPane(JEDITOR_TYPE, "<p style = \"font-size:15; font-family:Arial; color:" + textColor + "\">Utilization</p>");
        utilText.setEditable(false);
        utilText.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        detailPanel.add(utilText, sysCpuInfoConstraints);

        JEditorPane procText = new JEditorPane(JEDITOR_TYPE, "<p style = \"font-size:15; font-family:Arial; color:" + textColor + "\">Processes</p>");
        procText.setEditable(false);
        procText.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        sysCpuInfoConstraints.gridx++;
        detailPanel.add(procText, sysCpuInfoConstraints);

        JEditorPane threadText = new JEditorPane(JEDITOR_TYPE, "<p style = \"font-size:15; font-family:Arial; color:" + textColor + "\">Threads</p>");
        threadText.setEditable(false);
        threadText.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        sysCpuInfoConstraints.gridx++;
        detailPanel.add(threadText, sysCpuInfoConstraints);

        sysCpuInfoConstraints.gridx = 0;
        sysCpuInfoConstraints.gridy = 1;

        String valueColor = isDarkMode ? "white" : "black"; // Biến này không cần gán lại
        utilValueText = new JEditorPane(JEDITOR_TYPE, String.format(VALUE_HTML_HEAD, valueColor) + String.format("%.2f", getLoad() * 100d) + "%" + VALUE_HTML_TAIL);
        utilValueText.setEditable(false);
        utilValueText.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        detailPanel.add(utilValueText, sysCpuInfoConstraints);

        processesValueText = new JEditorPane(JEDITOR_TYPE, String.format(VALUE_HTML_HEAD, valueColor) + nProcess + VALUE_HTML_TAIL);
        processesValueText.setEditable(false);
        processesValueText.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        sysCpuInfoConstraints.gridx++;
        detailPanel.add(processesValueText, sysCpuInfoConstraints);

        threadValueText = new JEditorPane(JEDITOR_TYPE, String.format(VALUE_HTML_HEAD, valueColor) + nThread + VALUE_HTML_TAIL);
        threadValueText.setEditable(false);
        threadValueText.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        sysCpuInfoConstraints.gridx++;
        detailPanel.add(threadValueText, sysCpuInfoConstraints);

        JEditorPane handleText = new JEditorPane(JEDITOR_TYPE, "<p style = \"font-size:15; font-family:Arial; color:" + textColor + "\">                           </p>");
        handleText.setEditable(false);
        handleText.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        handleValueText = new JEditorPane(JEDITOR_TYPE, "<p style = \"font-size:23; font-family:Arial; color:" + valueColor + "\">                            </p>");
        handleValueText.setEditable(false);
        handleValueText.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);

        if (CURRENT_PLATFORM.equals(PlatformEnum.WINDOWS)) {
            handleText.setText("<p style = \"font-size:15; font-family:Arial; color:" + textColor + "\">Handles</p>");
            sysCpuInfoConstraints.gridy = 0;
            sysCpuInfoConstraints.gridx = 3;
            detailPanel.add(handleText, sysCpuInfoConstraints);
            sysCpuInfoConstraints.gridy = 1;

            handleValueText.setText(String.format(VALUE_HTML_HEAD, valueColor) + nHandle + VALUE_HTML_TAIL);
            detailPanel.add(handleValueText, sysCpuInfoConstraints);
        }
     // Thêm Speed và Up Time
        long uptimeSeconds = ((OperatingSystem) processor).getSystemUptime();
        long days = uptimeSeconds / (24 * 3600);
        long hours = (uptimeSeconds % (24 * 3600)) / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        String upTime = String.format("%d days, %d hours, %d minutes", days, hours, minutes);
        double cpuSpeed = processor.getMaxFreq() / 1_000_000_000.0; // GHz

        JEditorPane speedText = new JEditorPane(JEDITOR_TYPE, "<p style = \"font-size:15; font-family:Arial; color:" + textColor + "\">Speed</p>");
        speedText.setEditable(false);
        speedText.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        sysCpuInfoConstraints.gridx++;
        detailPanel.add(speedText, sysCpuInfoConstraints);

        JEditorPane upTimeText = new JEditorPane(JEDITOR_TYPE, "<p style = \"font-size:15; font-family:Arial; color:" + textColor + "\">Up Time</p>");
        upTimeText.setEditable(false);
        upTimeText.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        sysCpuInfoConstraints.gridx++;
        detailPanel.add(upTimeText, sysCpuInfoConstraints);

        sysCpuInfoConstraints.gridx = 4;
        sysCpuInfoConstraints.gridy = 1;
        JEditorPane speedValueText = new JEditorPane(JEDITOR_TYPE, String.format(VALUE_HTML_HEAD, valueColor) + String.format("%.2f GHz", cpuSpeed) + VALUE_HTML_TAIL);
        speedValueText.setEditable(false);
        speedValueText.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        detailPanel.add(speedValueText, sysCpuInfoConstraints);

        sysCpuInfoConstraints.gridx++;
        JEditorPane upTimeValueText = new JEditorPane(JEDITOR_TYPE, String.format(VALUE_HTML_HEAD, valueColor) + upTime + VALUE_HTML_TAIL);
        upTimeValueText.setEditable(false);
        upTimeValueText.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        detailPanel.add(upTimeValueText, sysCpuInfoConstraints);
        Timer timer = new Timer(1000, e -> {
            // Tạo biến tạm thời để lưu giá trị màu, thay vì gán lại valueColor
            String currentValueColor = isDarkMode ? "white" : "black";
            if (CURRENT_PLATFORM.equals(PlatformEnum.WINDOWS)) {
                handleValueText.setText(String.format(VALUE_HTML_HEAD, currentValueColor) + nHandle + VALUE_HTML_TAIL);
            }
            processesValueText.setText(String.format(VALUE_HTML_HEAD, currentValueColor) + nProcess + VALUE_HTML_TAIL);
            threadValueText.setText(String.format(VALUE_HTML_HEAD, currentValueColor) + nThread + VALUE_HTML_TAIL);
            utilValueText.setText(String.format(VALUE_HTML_HEAD, currentValueColor) + String.format("%.2f", getLoad() * 100d) + "%" + VALUE_HTML_TAIL);
            long newUptimeSeconds = ((OperatingSystem) processor).getSystemUptime();
            long newDays = newUptimeSeconds / (24 * 3600);
            long newHours = (newUptimeSeconds % (24 * 3600)) / 3600;
            long newMinutes = (newUptimeSeconds % 3600) / 60;
            upTimeValueText.setText(String.format(VALUE_HTML_HEAD, valueColor) + 
                    String.format("%d days, %d hours,%d minutes", newDays, newHours, newMinutes) + VALUE_HTML_TAIL);
        });
        timer.start();

        return detailPanel;
    }

    private JPanel createInfoPanel() {
        JPanel textPanel = new JPanel(new GridBagLayout());
        textPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);

        GridBagConstraints sysCpuTextConstraints = new GridBagConstraints();
        sysCpuTextConstraints.fill = GridBagConstraints.BOTH;
        sysCpuTextConstraints.gridx = 0;
        sysCpuTextConstraints.gridy = 0;
        sysCpuTextConstraints.weightx = 2d;
        sysCpuTextConstraints.gridheight = 2;
        sysCpuTextConstraints.gridwidth = 1;

        String textColor = isDarkMode ? "white" : "black";
        JEditorPane textCpuPane = new JEditorPane(JEDITOR_TYPE, "<b style = \"font-size:38; font-family:Arial; color:" + textColor + "\">CPU</b>");
        textCpuPane.setEditable(false);
        textCpuPane.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        textPanel.add(textCpuPane, sysCpuTextConstraints);

        JEditorPane textCpuNamePane = new JEditorPane(JEDITOR_TYPE, "<b style = \"font-size:16; font-family:Arial; color:" + textColor + "\">" + processor.getProcessorIdentifier().getName() + "</b>");
        textCpuNamePane.setEditable(false);
        textCpuNamePane.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);

        GridBagConstraints sysCpuNameConstraints = new GridBagConstraints();
        sysCpuNameConstraints.gridx = 1;
        sysCpuNameConstraints.gridy = 1;
        sysCpuNameConstraints.gridheight = 1;
        sysCpuNameConstraints.gridwidth = 1;
        sysCpuNameConstraints.fill = GridBagConstraints.HORIZONTAL;
        textPanel.add(textCpuNamePane, sysCpuNameConstraints);

        return textPanel;
    }

    void updateCPUInfo() {
        Thread thread = new Thread(() -> {
            while (true) {
                setLoad(cpuData());
                nProcess = os.getProcessCount();
                nThread = os.getThreadCount();
                if (CURRENT_PLATFORM.equals(PlatformEnum.WINDOWS)) {
                    Pair<List<String>, Map<HandleCountProperty, List<Long>>> hwdPair = oshi.driver.windows.perfmon.ProcessInformation.queryHandles();
                    nHandle = hwdPair.getB().values().iterator().next().get(0);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        thread.start();
    }

    private float[] floatArrayPercent(double d) {
        float[] f = new float[1];
        f[0] = (float) (100d * d);
        return f;
    }

    private double cpuData() {
        double d = processor.getSystemCpuLoadBetweenTicks(oldTicks);
        oldTicks = processor.getSystemCpuLoadTicks();
        return d;
    }

    private double[] procData() {
        double[] p = processor.getProcessorCpuLoadBetweenTicks(oldProcTicks);
        oldProcTicks = processor.getProcessorCpuLoadTicks();
        return p;
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
        plot.setRangeGridlinePaint(Color.BLACK);
        plot.setDomainGridlinePaint(Color.BLACK);
    }

    @Override
    public void updateTheme() {
        super.updateTheme();
        if (displayPanel != null) { // Kiểm tra null trước khi gọi
            displayPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        }
        revalidate();
        repaint();
    }

    public static void setLoad(double load) {
        CPUinPer.load = load;
    }

    public static double getLoad() {
        return load;
    }
}