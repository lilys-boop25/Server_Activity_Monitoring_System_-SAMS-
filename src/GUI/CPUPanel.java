package GUI;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.swing.JPanel;
import javax.swing.Timer;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;

/**
 * Shows system and per-processor CPU usage every second in a time series chart.
 */
public class CPUPanel extends OshiJPanel { // NOSONAR squid:S110

    
    private static final long serialVersionUID = 1L;

    private long[] oldTicks;
    private long[][] oldProcTicks;

    public CPUPanel(SystemInfo si) {
        super();
        CentralProcessor cpu = si.getHardware().getProcessor();
        oldTicks = new long[TickType.values().length];
        oldProcTicks = new long[cpu.getLogicalProcessorCount()][TickType.values().length];
        init(cpu);
    }

    private void init(CentralProcessor processor) {

        GridBagConstraints sysConstraints = new GridBagConstraints();

        sysConstraints.weightx = 1d;
        sysConstraints.weighty = 1d;
        sysConstraints.fill = GridBagConstraints.BOTH;

        GridBagConstraints procConstraints = (GridBagConstraints) sysConstraints.clone();
        procConstraints.gridx = 1;

        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        DynamicTimeSeriesCollection sysData = new DynamicTimeSeriesCollection(1, 60, new Second());
        sysData.setTimeBase(new Second(date));
        sysData.addSeries(floatArrayPercent(cpuData(processor)), 0, "All cpus");
        JFreeChart systemCpuChart = ChartFactory.createTimeSeriesChart("System CPU Usage", "Time", "% CPU", sysData, true, true, false);

        systemCpuChart.getXYPlot().getRangeAxis().setAutoRange(false);
        systemCpuChart.getXYPlot().getRangeAxis().setRange(0d, 100d);
        
        XYAreaRenderer renderer = new XYAreaRenderer();
        renderer.setSeriesPaint(0, new Color(0, 255, 255, 128));
        renderer.setOutline(true);
        renderer.setSeriesOutlineStroke(0, new BasicStroke(1.0f));
        renderer.setSeriesOutlinePaint(0, Color.CYAN.darker());

        systemCpuChart.getXYPlot().setRenderer(renderer);
        systemCpuChart.getPlot().setBackgroundPaint( Color.WHITE );
        systemCpuChart.getXYPlot().setDomainGridlinesVisible(true);
        systemCpuChart.getXYPlot().setRangeGridlinesVisible(true);
        systemCpuChart.getXYPlot().setRangeGridlinePaint(Color.black);
        systemCpuChart.getXYPlot().setDomainGridlinePaint(Color.black);

        double[] procUsage = procData(processor);
        DynamicTimeSeriesCollection procData = new DynamicTimeSeriesCollection(procUsage.length, 60, new Second());
        procData.setTimeBase(new Second(date));
        for (int i = 0; i < procUsage.length; i++) {
            procData.addSeries(floatArrayPercent(procUsage[i]), i, "cpu" + i);
        }
        JFreeChart procCpuChart = ChartFactory.createTimeSeriesChart("Processor CPU Usage", "Time", "% CPU", procData, true, true, false);

        procCpuChart.getXYPlot().getRangeAxis().setAutoRange(false);
        procCpuChart.getXYPlot().getRangeAxis().setRange(0d, 100d);
        
        JPanel cpuPanel = new JPanel();
        cpuPanel.setLayout(new GridBagLayout());
        ChartPanel systemCpuChartPanel = new ChartPanel(systemCpuChart);
        cpuPanel.add(systemCpuChartPanel, sysConstraints);
        ChartPanel procCpuChartPanel = new ChartPanel(procCpuChart);
        cpuPanel.add(procCpuChartPanel, procConstraints);

        GridBagConstraints cpuPanelConstraints = new GridBagConstraints();
        cpuPanelConstraints.fill = GridBagConstraints.BOTH;
        cpuPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
        //cpuPanel.setMinimumSize(new Dimension(1365,420));
        cpuPanel.setMinimumSize(new Dimension(800,515));
        add(cpuPanel, cpuPanelConstraints);

        Timer timer = new Timer(Config.REFRESH_FAST, e -> {
            sysData.advanceTime();
            sysData.appendData(floatArrayPercent(cpuData(processor)));
            procData.advanceTime();
            int newest = procData.getNewestIndex();
            double[] procUsageData = procData(processor);
            for (int i = 0; i < procUsageData.length; i++) {
                procData.addValue(i, newest, (float) (100 * procUsageData[i]));
            }
        });
        timer.start();
    }

    private static boolean run = false;

    static private double load = 0d;

    public static void updateCPUInfo(CentralProcessor cen, JGradientButton memButton)
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
