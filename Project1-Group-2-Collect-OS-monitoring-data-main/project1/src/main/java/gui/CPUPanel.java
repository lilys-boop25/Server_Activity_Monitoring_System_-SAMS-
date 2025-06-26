package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.BorderFactory;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
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

/**
 * Shows system and per-processor CPU usage every second in a time series chart.
 */
public class CPUPanel extends OshiJPanel { // NOSONAR squid:S110

    private static final String VALUE_HTML_HEAD = "<p style = \"font-size:23; font-family:SansSerif; color:black\">";
    private static final String VALUE_HTML_TAIL = "</p>";
    private static final String JEDITOR_TYPE = "text/html";
    static final PlatformEnum CURRENT_PLATFORM = PlatformEnum.getValue(Platform.getOSType());

    private static final long serialVersionUID = 1L;

    private static long[] oldTicks = new long[TickType.values().length];
    private long[][] oldProcTicks;

    private static int nProcess = 0;
    private static int nThread = 0;
    private static long nHandle = 0;
    private static double load = 0d;

    public CPUPanel(SystemInfo si) {
        super();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        CentralProcessor cpu = si.getHardware().getProcessor();
        oldProcTicks = new long[cpu.getLogicalProcessorCount()][TickType.values().length];

        // Create button panel at the top
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create content panel for displaying charts
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);

        // Create buttons
        JButton overallButton = createButton("Overall Utilization", initTotalCPU(cpu), contentPanel);
        JButton logicalButton = createButton("Logical Processors", initSingleCPU(cpu), contentPanel);
        
        // Style buttons
        styleButton(overallButton);
        styleButton(logicalButton);
        
        buttonPanel.add(overallButton);
        buttonPanel.add(logicalButton);

        // Add components to main panel
        add(buttonPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        // Click the overall button by default
        overallButton.doClick();
    }

    private void styleButton(JButton button) {
        button.setBackground(new Color(240, 240, 240));
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setPreferredSize(new Dimension(150, 30));
        button.setFocusPainted(false);
    }

    private JButton createButton(String title, JPanel newPanel, JPanel contentPanel){
        JButton button = new JButton(title);
        
        // Set what to do when we push the button
        button.addActionListener(e -> {
            contentPanel.removeAll();
            contentPanel.add(newPanel, BorderLayout.CENTER);
            contentPanel.revalidate();
            contentPanel.repaint();
        });
        return button;
    }

    private JPanel initSingleCPU(CentralProcessor processor){
        JPanel cpuPanel = new JPanel();
        cpuPanel.setLayout(new GridBagLayout());
        cpuPanel.setBackground(Color.WHITE);

        GridBagConstraints textPanelConstraints = new GridBagConstraints();
        textPanelConstraints.gridwidth = 4;
        textPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        textPanelConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        cpuPanel.add(createInfoPanel(processor), textPanelConstraints);

        JPanel chartPanel = new JPanel();
        chartPanel.setLayout(new GridBagLayout());
        chartPanel.setBackground(Color.WHITE);

        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());

        double[] procUsage = procData(processor);

        ArrayList<DynamicTimeSeriesCollection> procData = new ArrayList<>();
        ArrayList<JFreeChart> procCpuChart = new ArrayList<>();
        ArrayList<ChartPanel> procCpuChartPanel = new ArrayList<>();
        for (int i = 0; i < procUsage.length; i++) {
            DynamicTimeSeriesCollection timeSeriesData = new DynamicTimeSeriesCollection(1, 60, new Second());
            timeSeriesData.setTimeBase(new Second(date));
            timeSeriesData.addSeries(floatArrayPercent(procUsage[i]), 0, null);

            JFreeChart procChart = ChartFactory.createTimeSeriesChart(null, null, null, timeSeriesData, true, true, false);
            procChart.getXYPlot().getRangeAxis().setAutoRange(false);
            procChart.getXYPlot().getRangeAxis().setRange(0d, 100d);
            procChart.getXYPlot().getDomainAxis().setVisible(false);
            procChart.getXYPlot().getRangeAxis().setVisible(false);
            procChart.getXYPlot().getDomainAxis().setLowerMargin(0);
            procChart.getXYPlot().getDomainAxis().setUpperMargin(0);

            procChart.removeLegend();

            PerformancePanel.setChartRenderer(procChart, Color.CYAN);

            procData.add(timeSeriesData);
            procCpuChart.add(i, procChart);
            procCpuChartPanel.add(new ChartPanel(procChart));
        }

        GridBagConstraints procConstraints = new GridBagConstraints();
        procConstraints.weightx = 1d;
        procConstraints.weighty = 1d;
        procConstraints.fill = GridBagConstraints.BOTH;
        procConstraints.gridx = 0;
        procConstraints.gridy = 0;

        int x = 0;

        for (int i = 0; i < procCpuChartPanel.size(); i++)
        {
            GridBagConstraints singleProcConstraints = (GridBagConstraints)procConstraints.clone();
            singleProcConstraints.gridx = x % 4;
            singleProcConstraints.gridy = x / 4;
            x++;
            chartPanel.add(procCpuChartPanel.get(i), singleProcConstraints);
        }
        
        chartPanel.setPreferredSize(new Dimension(1100, 500));
        GridBagConstraints chartPanelConstraints = new GridBagConstraints();
        chartPanelConstraints.gridy = 1;
        chartPanelConstraints.weighty = 0.8d;
        chartPanelConstraints.weightx = 1d;
        chartPanelConstraints.fill = GridBagConstraints.BOTH;
        chartPanelConstraints.anchor = GridBagConstraints.CENTER;
        chartPanelConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        cpuPanel.add(chartPanel, chartPanelConstraints);

        GridBagConstraints detailPanelConstraints = new GridBagConstraints();
        detailPanelConstraints.gridy = 2;
        detailPanelConstraints.weighty = 0.2d;
        detailPanelConstraints.weightx = 1d;
        detailPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        detailPanelConstraints.anchor = GridBagConstraints.SOUTH;
        detailPanelConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        cpuPanel.add(createDetailPanel(), detailPanelConstraints);

        Timer timer = new Timer(Config.REFRESH_FAST, e -> {
            double[] procUsageData = procData(processor);
            for (int i = 0; i < procUsageData.length; i++) {
                procData.get(i).advanceTime();
                procData.get(i).appendData(floatArrayPercent(procUsageData[i]));
            }
        });
        timer.start();

        return cpuPanel;
    }

    private JPanel initTotalCPU(CentralProcessor processor) {
        JPanel cpuPanel = new JPanel();
        cpuPanel.setLayout(new GridBagLayout());
        cpuPanel.setBackground(Color.WHITE);

        GridBagConstraints textPanelConstraints = new GridBagConstraints();
        textPanelConstraints.gridwidth = 2;
        textPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        textPanelConstraints.weighty = 0.1d;
        textPanelConstraints.weightx = 1d;
        textPanelConstraints.gridx = 0;
        textPanelConstraints.gridy = 0;
        textPanelConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        cpuPanel.add(createInfoPanel(processor), textPanelConstraints);

        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        DynamicTimeSeriesCollection sysData = new DynamicTimeSeriesCollection(1, 60, new Second());
        sysData.setTimeBase(new Second(date));
        sysData.addSeries(floatArrayPercent(cpuData(processor)), 0, "All cpus");
        JFreeChart systemCpuChart = ChartFactory.createTimeSeriesChart(null, null, "%", sysData, true, true, false);

        systemCpuChart.getXYPlot().getRangeAxis().setAutoRange(false);
        systemCpuChart.getXYPlot().getRangeAxis().setRange(0d, 100d);

        systemCpuChart.getXYPlot().getDomainAxis().setLowerMargin(0);
        systemCpuChart.getXYPlot().getDomainAxis().setUpperMargin(0);

        PerformancePanel.setChartRenderer(systemCpuChart, Color.CYAN);
        systemCpuChart.removeLegend();

        ChartPanel systemCpuChartPanel = new ChartPanel(systemCpuChart);
        systemCpuChartPanel.setPreferredSize(new Dimension(1100, 500));
        
        GridBagConstraints sysChartConstraints = new GridBagConstraints();
        sysChartConstraints.gridwidth = 2;
        sysChartConstraints.gridy = 1;
        sysChartConstraints.weighty = 0.7d;
        sysChartConstraints.weightx = 1d;
        sysChartConstraints.fill = GridBagConstraints.BOTH;
        sysChartConstraints.gridx = 0;
        sysChartConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        cpuPanel.add(systemCpuChartPanel, sysChartConstraints);

        GridBagConstraints detailPanelConstraints = new GridBagConstraints();
        detailPanelConstraints.gridy = 2;
        detailPanelConstraints.gridwidth = 2;
        detailPanelConstraints.weighty = 0.2d;
        detailPanelConstraints.weightx = 1d;
        detailPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        detailPanelConstraints.anchor = GridBagConstraints.SOUTH;
        detailPanelConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        cpuPanel.add(createDetailPanel(), detailPanelConstraints);

        Timer timer = new Timer(Config.REFRESH_FAST, e -> {
            sysData.advanceTime();
            sysData.appendData(floatArrayPercent(load));
        });
        timer.start();

        return cpuPanel;
    }

    private static boolean run = false;

    private JPanel createDetailPanel(){
        JPanel detailPanel = new JPanel();
        detailPanel.setLayout(new GridBagLayout());
        detailPanel.setBackground(Color.WHITE);

        GridBagConstraints sysCpuInfoConstraints = new GridBagConstraints();
        sysCpuInfoConstraints.fill = GridBagConstraints.HORIZONTAL;
        sysCpuInfoConstraints.gridx = 0;
        sysCpuInfoConstraints.gridy = 0;
        sysCpuInfoConstraints.ipadx = 20;
        sysCpuInfoConstraints.insets = new java.awt.Insets(5, 5, 5, 5);

        JEditorPane utilText = new JEditorPane(JEDITOR_TYPE, "<p style = \"font-size:15; font-family:SansSerif; color:gray\">Utilization</p>");
        utilText.setEditable(false);
        detailPanel.add(utilText, sysCpuInfoConstraints);

        JEditorPane procText = new JEditorPane(JEDITOR_TYPE, "<p style = \"font-size:15; font-family:SansSerif; color:gray\">Processes</p>");
        procText.setEditable(false);
        sysCpuInfoConstraints.gridx++;
        detailPanel.add(procText, sysCpuInfoConstraints);

        JEditorPane threadText = new JEditorPane(JEDITOR_TYPE, "<p style = \"font-size:15; font-family:SansSerif; color:gray\">Threads</p>");
        threadText.setEditable(false);
        sysCpuInfoConstraints.gridx++;
        detailPanel.add(threadText, sysCpuInfoConstraints);

        sysCpuInfoConstraints.gridx = 0;
        sysCpuInfoConstraints.gridy = 1;

        JEditorPane utilValueText = new JEditorPane(JEDITOR_TYPE, VALUE_HTML_HEAD + String.format("%.2f",load) + "%" + VALUE_HTML_TAIL);
        utilValueText.setEditable(false);
        detailPanel.add(utilValueText, sysCpuInfoConstraints);

        JEditorPane processesValueText = new JEditorPane(JEDITOR_TYPE, VALUE_HTML_HEAD + nProcess + VALUE_HTML_TAIL);
        processesValueText.setEditable(false);
        sysCpuInfoConstraints.gridx++;
        detailPanel.add(processesValueText, sysCpuInfoConstraints);

        JEditorPane threadValueText = new JEditorPane(JEDITOR_TYPE, VALUE_HTML_HEAD + nThread + VALUE_HTML_TAIL);
        threadValueText.setEditable(false);
        sysCpuInfoConstraints.gridx++;
        detailPanel.add(threadValueText, sysCpuInfoConstraints);

        JEditorPane handleText = new JEditorPane(JEDITOR_TYPE, "<p style = \"font-size:15; font-family:SansSerif; color:gray\">                           </p>");
        handleText.setEditable(false);
        JEditorPane handleValueText = new JEditorPane(JEDITOR_TYPE, "<p style = \"font-size:23; font-family:SansSerif; color:black\">                            </p>");
        handleValueText.setEditable(false);

        if (CURRENT_PLATFORM.equals(PlatformEnum.WINDOWS))
        {
            handleText.setText("<p style = \"font-size:15; font-family:SansSerif; color:gray\">Handles</p>");
            sysCpuInfoConstraints.gridy = 0;
            sysCpuInfoConstraints.gridx = 3;
            detailPanel.add(handleText, sysCpuInfoConstraints);
            sysCpuInfoConstraints.gridy = 1;

            handleValueText.setText(VALUE_HTML_HEAD + nHandle + VALUE_HTML_TAIL);
            detailPanel.add(handleValueText, sysCpuInfoConstraints);
        }
        
        Timer timer = new Timer(Config.REFRESH_FAST, e -> {
            if (CURRENT_PLATFORM.equals(PlatformEnum.WINDOWS))
            {
                handleValueText.setText(VALUE_HTML_HEAD + nHandle + VALUE_HTML_TAIL);
            }
            processesValueText.setText(VALUE_HTML_HEAD + nProcess + VALUE_HTML_TAIL);
            threadValueText.setText(VALUE_HTML_HEAD + nThread + VALUE_HTML_TAIL);
            utilValueText.setText(VALUE_HTML_HEAD + String.format("%.2f",load * 100d) + "%</p>");
        });
        timer.start();
        return detailPanel;
    }

    private JPanel createInfoPanel(CentralProcessor processor){
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new GridBagLayout());
        textPanel.setBackground(Color.WHITE);

        GridBagConstraints sysCpuTextConstraints = new GridBagConstraints();
        sysCpuTextConstraints.fill = GridBagConstraints.BOTH;
        sysCpuTextConstraints.gridx = 0;
        sysCpuTextConstraints.gridy = 0;
        sysCpuTextConstraints.weightx = 2d;
        sysCpuTextConstraints.gridheight = 2;
        sysCpuTextConstraints.gridwidth = 1;

        JEditorPane textCpuPane = new JEditorPane(JEDITOR_TYPE, "");
        textCpuPane.setText("<b style = \"font-size:38; font-family:SansSerif\">CPU</b>");
        textCpuPane.setEditable(false);
        textCpuPane.setOpaque(false);
        textPanel.add(textCpuPane, sysCpuTextConstraints);

        JEditorPane textCpuNamePane = new JEditorPane(JEDITOR_TYPE, "");
        textCpuNamePane.setText("<b style = \"font-size:16\">" + processor.getProcessorIdentifier().getName() + "</b>");
        textCpuNamePane.setEditable(false);
        textCpuNamePane.setOpaque(false);

        GridBagConstraints sysCpuNameConstraints = new GridBagConstraints();
        sysCpuNameConstraints.gridx = 1;
        sysCpuNameConstraints.gridy = 1;
        sysCpuNameConstraints.gridheight = 1;
        sysCpuNameConstraints.gridwidth = 1;
        sysCpuNameConstraints.fill = GridBagConstraints.HORIZONTAL;
        textPanel.add(textCpuNamePane, sysCpuNameConstraints);

        return textPanel;
    }

    public static void updateCPUInfo(CentralProcessor cen, JGradientButton memButton, OperatingSystem os)
    {
        if (run)
        {
            return;
        }
        run = true;
        Thread thread = new Thread(() -> {
            while(true)
            {
                load = cpuData(cen);
                memButton.setText(PerformancePanel.buttonTextLines("\nCPU\n" + (String.format("%.2f",load*100)) + "%\n"));
                memButton.color = PerformancePanel.getColorByPercent((int)((load)*100));
                nProcess = os.getProcessCount();
                nThread = os.getThreadCount();
                if (CURRENT_PLATFORM.equals(PlatformEnum.WINDOWS)){
                    Pair<List<String>, Map<HandleCountProperty, List<Long>>> hwdPair = oshi.driver.windows.perfmon.ProcessInformation.queryHandles();
                    nHandle = hwdPair.getB().values().iterator().next().get(0);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    // Restore interrupted state...
                    Thread.currentThread().interrupt();
                }
            }
        });
        thread.start();
    }

    private static float[] floatArrayPercent(double d) {
        float[] f = new float[1];
        f[0] = (float) (100d * d);
        return f;
    }

    private static double cpuData(CentralProcessor proc) {
        double d = proc.getSystemCpuLoadBetweenTicks(oldTicks);
        oldTicks = proc.getSystemCpuLoadTicks();
        return d;
    }

    private double[] procData(CentralProcessor proc) {
        double[] p = proc.getProcessorCpuLoadBetweenTicks(oldProcTicks);
        oldProcTicks = proc.getProcessorCpuLoadTicks();
        return p;
    }
}