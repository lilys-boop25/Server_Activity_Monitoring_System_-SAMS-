package GUI;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;

import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.util.FormatUtil;

/**
 * Displays physical and virtual (swap) memory stats.
 */
public class MemoryPanel extends OshiJPanel { // NOSONAR squid:S110


    private static final long serialVersionUID = 1L;

    private static final String PHYSICAL_MEMORY = "Physical Memory";
    private static final String VIRTUAL_MEMORY = "Virtual Memory (Swap)";

    public MemoryPanel(SystemInfo si) {
        super();
        init(si.getHardware().getMemory());
    }

    private void init(GlobalMemory memory) {

        JPanel memoryPanel = new JPanel();
        memoryPanel.setLayout(new GridBagLayout());

        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        DynamicTimeSeriesCollection ramData = new DynamicTimeSeriesCollection(1, 60, new Second());
        ramData.setTimeBase(new Second(date));
        ramData.addSeries(floatArrayPercent(getRAM(memory)), 0, "Used");

        JFreeChart ramChart = ChartFactory.createTimeSeriesChart(PHYSICAL_MEMORY, "", "% Memory", ramData, true, true, false);
        
        ramChart.getXYPlot().getRangeAxis().setAutoRange(false);
        ramChart.getXYPlot().getRangeAxis().setRange(0d, 100d);

        PerformancePanel.setChartRenderer(ramChart, Color.MAGENTA);

        GridBagConstraints ramConstraints = new GridBagConstraints();
        ramConstraints.weightx = 1d;
        ramConstraints.weighty = 1d;
        ramConstraints.fill = GridBagConstraints.BOTH;

        memoryPanel.add(new ChartPanel(ramChart), ramConstraints);

        DynamicTimeSeriesCollection virtualMemData = new DynamicTimeSeriesCollection(1, 60, new Second());
        virtualMemData.setTimeBase(new Second(date));
        virtualMemData.addSeries(floatArrayPercent(getVirtualMemory(memory)), 0, "Used");

        JFreeChart virChart = ChartFactory.createTimeSeriesChart(VIRTUAL_MEMORY, "", "% Memory", ramData, true, true, false);

        virChart.getXYPlot().getRangeAxis().setAutoRange(false);
        virChart.getXYPlot().getRangeAxis().setRange(0d, 100d);

        PerformancePanel.setChartRenderer(virChart, Color.MAGENTA);

        GridBagConstraints virConstraints = (GridBagConstraints)ramConstraints.clone();
        virConstraints.gridx = 1;

        ChartPanel virChartPanel = new ChartPanel(virChart);
        memoryPanel.add(virChartPanel, virConstraints);

        JTextArea textArea = new JTextArea();
        GridBagConstraints textConstraints = new GridBagConstraints();
        textConstraints.gridy = 1;
        textConstraints.gridwidth = 2;
        textConstraints.fill = GridBagConstraints.HORIZONTAL;
        textConstraints.weightx = 1d;
        textArea.setText("Total RAM: " + FormatUtil.formatBytes(memory.getTotal()) + "\nTotal virtual memory: " + FormatUtil.formatBytes(memory.getVirtualMemory().getSwapTotal()));
        memoryPanel.add(textArea, textConstraints);

        GridBagConstraints memoryPanelConstraints = new GridBagConstraints();
        memoryPanelConstraints.fill = GridBagConstraints.BOTH;
        memoryPanelConstraints.weightx = 1d;
        memoryPanelConstraints.weighty = 1d;
        memoryPanelConstraints.gridx = 1;
        memoryPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
        //memoryPanel.setMinimumSize(new Dimension(1365, 455));
        memoryPanel.setMinimumSize(new Dimension(795,515));
        add(memoryPanel, memoryPanelConstraints);

        Timer timer = new Timer(Config.REFRESH_FAST, e -> {
            ramData.advanceTime();
            ramData.appendData(floatArrayPercent(getRAM(memory)));
            virtualMemData.advanceTime();
            virtualMemData.appendData(floatArrayPercent(getVirtualMemory(memory)));
        });
        timer.start();

    }

    public static double getVirtualMemory(GlobalMemory memory)
    {
        return (double)(memory.getVirtualMemory().getSwapUsed())/memory.getVirtualMemory().getSwapTotal();
    }


    private double getRAM(GlobalMemory memory)
    {
        return 1d - (double)(memory.getAvailable())/memory.getTotal();
    }

    private static float[] floatArrayPercent(double d) {
        float[] f = new float[1];
        f[0] = (float) (100d * d);
        return f;
    }

    private static boolean run = false;

    public static void updateMemoryInfo(GlobalMemory mem, JGradientButton memButton)
    {
        if (run == true)
        {
            return;
        }
        run = true;
        Thread thread = new Thread(() -> {
            while(true)
            {
                double available = (double)mem.getAvailable()/(1024*1024*1024);
                double total = (double)mem.getTotal()/(1024*1024*1024);
                double used = total - available;
                memButton.setText(PerformancePanel.buttonTextLines("\nMemory\n" + (String.format("%.2f/%.2f GB (%.0f", used, total, (used/total)*100) + "%)\n")));
                memButton.color = PerformancePanel.getColorByPercent((used/total)*100);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        });
        thread.start();

    }
}
