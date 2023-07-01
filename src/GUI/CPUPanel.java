package GUI;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.Timer;

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

    static final PlatformEnum CURRENT_PLATFORM = PlatformEnum.getValue(Platform.getOSType());

    private static final long serialVersionUID = 1L;

    private long[] oldTicks;
    private long[][] oldProcTicks;

    private static int nProcess = 0;
    private static int nThread = 0;
    private static long nHandle = 0;


    public CPUPanel(SystemInfo si) {
        super();
        CentralProcessor cpu = si.getHardware().getProcessor();
        oldTicks = new long[TickType.values().length];
        oldProcTicks = new long[cpu.getLogicalProcessorCount()][TickType.values().length];
        init(cpu);
        //initfake(cpu);
    }

    private void initfake(CentralProcessor processor){

        JPanel cpuPanel = new JPanel();
        cpuPanel.setLayout(new GridBagLayout());
        cpuPanel.setBackground(Color.WHITE);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new GridBagLayout());
        textPanel.setBackground(Color.black);

        GridBagConstraints sysCpuTextConstraints = new GridBagConstraints();
        sysCpuTextConstraints.fill = GridBagConstraints.BOTH;
        sysCpuTextConstraints.gridx = 0;
        sysCpuTextConstraints.gridy = 0;
        sysCpuTextConstraints.weightx = 2d;
        sysCpuTextConstraints.gridheight = 2;
        sysCpuTextConstraints.gridwidth = 1;

        JEditorPane textCpuPane = new JEditorPane("text/html", "");
        textCpuPane.setText("<b style = \"font-size:38; font-family:SansSerif\">CPU</b>");

        textCpuPane.setEditable(false);
        textPanel.add(textCpuPane, sysCpuTextConstraints);

        JEditorPane textCpuNamePane = new JEditorPane("text/html", "");
        textCpuNamePane.setText("<b style = \"font-size:16\">" + processor.getProcessorIdentifier().getName() + "</b>");
        textCpuNamePane.setEditable(false);
        
        GridBagConstraints sysCpuNameConstraints = new GridBagConstraints();
        sysCpuNameConstraints.gridx = 1;
        sysCpuNameConstraints.gridy = 1;
        sysCpuNameConstraints.gridheight = 1;
        sysCpuNameConstraints.gridwidth = 1;
        sysCpuNameConstraints.fill = GridBagConstraints.HORIZONTAL;
        sysCpuNameConstraints.anchor = GridBagConstraints.SOUTHEAST;
        textPanel.add(textCpuNamePane, sysCpuNameConstraints);

        GridBagConstraints textPanelConstraints = new GridBagConstraints();
        textPanelConstraints.gridwidth = 4;
        textPanelConstraints.fill = GridBagConstraints.NONE;
        cpuPanel.add(textPanel, textPanelConstraints);


        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());

        double[] procUsage = procData(processor);

        ArrayList<DynamicTimeSeriesCollection> procData = new ArrayList<DynamicTimeSeriesCollection>();
        ArrayList<JFreeChart> procCpuChart = new ArrayList<JFreeChart>();
        ArrayList<ChartPanel> procCpuChartPanel = new ArrayList<ChartPanel>();
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

        int x = 4;

        for (int i = 0; i < procCpuChartPanel.size(); i++)
        {
            GridBagConstraints singleProcConstraints = (GridBagConstraints)procConstraints.clone();
            singleProcConstraints.gridx = x % 4;
            singleProcConstraints.gridy = x / 4;
            x++;
            cpuPanel.add(procCpuChartPanel.get(i), singleProcConstraints);
        }

        GridBagConstraints cpuPanelConstraints = new GridBagConstraints();
        cpuPanelConstraints.fill = GridBagConstraints.BOTH;
        cpuPanelConstraints.anchor = GridBagConstraints.CENTER;
        //cpuPanel.setMinimumSize(new Dimension(1365,420));
        cpuPanel.setMinimumSize(new Dimension(800,515));
        add(cpuPanel, cpuPanelConstraints);

        

        Timer timer = new Timer(Config.REFRESH_FAST, e -> {
            double[] procUsageData = procData(processor);
            for (int i = 0; i < procUsageData.length; i++) {
                procData.get(i).advanceTime();
                procData.get(i).appendData(floatArrayPercent(procUsageData[i]));
            }
        });
        timer.start();

    }

    private void init(CentralProcessor processor) {
        JPanel cpuPanel = new JPanel();
        cpuPanel.setLayout(new GridBagLayout());
        cpuPanel.setBackground(Color.WHITE);

        GridBagConstraints sysCpuTextConstraints = new GridBagConstraints();
        sysCpuTextConstraints.fill = GridBagConstraints.BOTH;
        sysCpuTextConstraints.gridx = 0;
        sysCpuTextConstraints.gridy = 0;
        sysCpuTextConstraints.weightx = 2d;
        //sysCpuConstraints.weighty = 1d;
        sysCpuTextConstraints.gridheight = 2;
        sysCpuTextConstraints.gridwidth = 1;

        JEditorPane textCpuPane = new JEditorPane("text/html", "");
        textCpuPane.setText("<b style = \"font-size:38; font-family:SansSerif\">CPU</b>");

        textCpuPane.setEditable(false);
        cpuPanel.add(textCpuPane, sysCpuTextConstraints);

        JEditorPane textCpuNamePane = new JEditorPane("text/html", "");
        textCpuNamePane.setText("<b style = \"font-size:16\">" + processor.getProcessorIdentifier().getName() + "</b>");
        textCpuNamePane.setEditable(false);
        
        GridBagConstraints sysCpuNameConstraints = new GridBagConstraints();
        sysCpuNameConstraints.gridx = 1;
        sysCpuNameConstraints.gridy = 1;
        sysCpuNameConstraints.gridheight = 1;
        sysCpuNameConstraints.fill = GridBagConstraints.HORIZONTAL;
        sysCpuNameConstraints.anchor = GridBagConstraints.SOUTHEAST;
        cpuPanel.add(textCpuNamePane, sysCpuNameConstraints);


        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        DynamicTimeSeriesCollection sysData = new DynamicTimeSeriesCollection(1, 60, new Second());
        sysData.setTimeBase(new Second(date));
        sysData.addSeries(floatArrayPercent(cpuData(processor)), 0, "All cpus");
        JFreeChart systemCpuChart = ChartFactory.createTimeSeriesChart(null, null, "%", sysData, true, true, false);

        systemCpuChart.getXYPlot().getRangeAxis().setAutoRange(false);
        systemCpuChart.getXYPlot().getRangeAxis().setRange(0d, 100d);
        
        //systemCpuChart.getXYPlot().getDomainAxis().setVisible(false);
        //systemCpuChart.getXYPlot().getRangeAxis().setVisible(false);
        systemCpuChart.getXYPlot().getDomainAxis().setLowerMargin(0);
        systemCpuChart.getXYPlot().getDomainAxis().setUpperMargin(0);

        PerformancePanel.setChartRenderer(systemCpuChart, Color.CYAN);
        systemCpuChart.removeLegend();

        ChartPanel systemCpuChartPanel = new ChartPanel(systemCpuChart);
        GridBagConstraints sysChartConstraints = new GridBagConstraints();
        sysChartConstraints.gridwidth = 2;
        sysChartConstraints.gridy = 2;
        sysChartConstraints.fill = GridBagConstraints.BOTH;
        sysChartConstraints.gridx = 0;
        cpuPanel.add(systemCpuChartPanel, sysChartConstraints);

        GridBagConstraints cpuPanelConstraints = new GridBagConstraints();
        cpuPanelConstraints.fill = GridBagConstraints.BOTH;
        cpuPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
        //cpuPanel.setMinimumSize(new Dimension(1365,420));
        cpuPanel.setMinimumSize(new Dimension(1000,715));
        add(cpuPanel, cpuPanelConstraints);

        GridBagConstraints sysCpuInfoConstraints = new GridBagConstraints();
        sysCpuInfoConstraints.fill = GridBagConstraints.HORIZONTAL;
        sysCpuInfoConstraints.gridx = 0;
        sysCpuInfoConstraints.gridy = 0;
        sysCpuInfoConstraints.ipadx = 20;

        JPanel detaiPanel = new JPanel();
        detaiPanel.setLayout(new GridBagLayout());
        detaiPanel.setMinimumSize(new Dimension(1000,715));

        JEditorPane utilText = new JEditorPane("text/html", "<p style = \"font-size:15; font-family:SansSerif; color:gray\">Utilization</p>");
        utilText.setEditable(false);
        detaiPanel.add(utilText, sysCpuInfoConstraints);

        JEditorPane procText = new JEditorPane("text/html", "<p style = \"font-size:15; font-family:SansSerif; color:gray\">Processes</p>");
        procText.setEditable(false);
        sysCpuInfoConstraints.gridx++;
        detaiPanel.add(procText, sysCpuInfoConstraints);

        JEditorPane threadText = new JEditorPane("text/html", "<p style = \"font-size:15; font-family:SansSerif; color:gray\">Threads</p>");
        threadText.setEditable(false);
        sysCpuInfoConstraints.gridx++;
        detaiPanel.add(threadText, sysCpuInfoConstraints);

        sysCpuInfoConstraints.gridx = 0;
        sysCpuInfoConstraints.gridy = 1;

        JEditorPane utilValueText = new JEditorPane("text/html", "<p style = \"font-size:23; font-family:SansSerif; color:black\">" + String.format("%.2f",cpuData(processor)) + "%</p>");
        utilText.setEditable(false);
        detaiPanel.add(utilValueText, sysCpuInfoConstraints);

        JEditorPane processesValueText = new JEditorPane("text/html", "<p style = \"font-size:23; font-family:SansSerif; color:black\">" + String.valueOf(nProcess) + "%</p>");
        processesValueText.setEditable(false);
        sysCpuInfoConstraints.gridx++;
        detaiPanel.add(processesValueText, sysCpuInfoConstraints);

        JEditorPane threadValueText = new JEditorPane("text/html", "<p style = \"font-size:23; font-family:SansSerif; color:black\">" + String.valueOf(nThread) + "</p>");
        threadValueText.setEditable(false);
        sysCpuInfoConstraints.gridx++;
        detaiPanel.add(threadValueText, sysCpuInfoConstraints);

        JEditorPane handleText = new JEditorPane("text/html", "<p style = \"font-size:15; font-family:SansSerif; color:gray\">                           </p>");
        handleText.setEditable(false);
        JEditorPane handleValueText = new JEditorPane("text/html", "<p style = \"font-size:23; font-family:SansSerif; color:black\">                            </p>");
        handleValueText.setEditable(false);

        if (CURRENT_PLATFORM.equals(PlatformEnum.WINDOWS))
        {
            handleText.setText("<p style = \"font-size:15; font-family:SansSerif; color:gray\">Handles</p>");
            sysCpuInfoConstraints.gridy = 0;
            sysCpuInfoConstraints.gridx = 3;
            detaiPanel.add(handleText, sysCpuInfoConstraints);
            sysCpuInfoConstraints.gridy = 1;
            
            handleValueText.setText("<p style = \"font-size:23; font-family:SansSerif; color:black\">" + String.valueOf(nHandle) + "</p>");
            detaiPanel.add(handleValueText, sysCpuInfoConstraints);
        }

        GridBagConstraints detaiPanelConstraints = new GridBagConstraints();
        detaiPanelConstraints.gridy = 3;

        cpuPanel.add(detaiPanel, detaiPanelConstraints);

        Timer timer = new Timer(Config.REFRESH_FAST, e -> {
            double load = cpuData(processor);
            sysData.advanceTime();
            sysData.appendData(floatArrayPercent(load));
            if (CURRENT_PLATFORM.equals(PlatformEnum.WINDOWS))
            {
                handleValueText.setText("<p style = \"font-size:23; font-family:SansSerif; color:black\">" + String.valueOf(nHandle) + "</p>");
            }
            processesValueText.setText("<p style = \"font-size:23; font-family:SansSerif; color:black\">" + String.valueOf(nProcess) + "</p>");
            threadValueText.setText("<p style = \"font-size:23; font-family:SansSerif; color:black\">" + String.valueOf(nThread) + "</p>");
            utilValueText.setText("<p style = \"font-size:23; font-family:SansSerif; color:black\">" + String.format("%.2f",load * 100d) + "%</p>");

        });
        timer.start();
    }

    private static boolean run = false;

    static private double load = 0d;

    public static void updateCPUInfo(CentralProcessor cen, JGradientButton memButton, OperatingSystem os)
    {
        if (run == true)
        {
            return;
        }
        run = true;
        Thread thread = new Thread(() -> {
            while(true)
            {
                load = cen.getSystemCpuLoad(1000);
                memButton.setText(PerformancePanel.buttonTextLines("\nCPU\n" + (String.format("%.2f",load*100)) + "%\n"));
                memButton.color = PerformancePanel.getColorByPercent((int)((load)*100));
                nProcess = os.getProcessCount();
                nThread = os.getThreadCount();
                if (CURRENT_PLATFORM.equals(PlatformEnum.WINDOWS)){
                    Pair<List<String>, Map<HandleCountProperty, List<Long>>> hwdPair = oshi.driver.windows.perfmon.ProcessInformation.queryHandles();
                    nHandle = (long)((List<Long>)(hwdPair.getB().values().iterator().next())).get(0);
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

    private double cpuData(CentralProcessor proc) {
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
