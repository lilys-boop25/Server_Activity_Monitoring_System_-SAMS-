package GUI;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.swing.JButton;
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
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;
import oshi.util.FormatUtil;

/**
 * Displays physical and virtual (swap) memory stats.
 */
public class DiskPanel extends OshiJPanel { // NOSONAR squid:S110


    private static final long serialVersionUID = 1L;

    private static final String PHYSICAL_MEMORY = "Physical Memory";
    private static final String VIRTUAL_MEMORY = "Virtual Memory (Swap)";

    public DiskPanel(HWDiskStore disk, int index) {
        super();
        init(disk, index);
    }

    private void init(HWDiskStore disk, int index) {
        GridBagConstraints netConstraints = new GridBagConstraints();

        netConstraints.weightx = 1d;
        netConstraints.weighty = 1d;
        netConstraints.fill = GridBagConstraints.NONE;
        netConstraints.anchor = GridBagConstraints.NORTHWEST;

        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        DynamicTimeSeriesCollection diskData = new DynamicTimeSeriesCollection(2, 60, new Second());
        diskData.setTimeBase(new Second(date));
        diskData.addSeries(floatArrayPercent(0d), 0, "Read");
        diskData.addSeries(floatArrayPercent(0d), 1, "Write");
        JFreeChart netChart = ChartFactory.createTimeSeriesChart("Throughput", "Time", "Kbps", diskData, true, true, false);

        netChart.getXYPlot().getRangeAxis().setAutoRange(false);
        netChart.getXYPlot().getRangeAxis().setRange(0d, 1000d);

        JPanel netPanel = new JPanel();
        netPanel.setLayout(new GridBagLayout());
        ChartPanel myChartPanel = new ChartPanel(netChart);
        myChartPanel.setMinimumSize(new Dimension(700, 350));
        netPanel.add(myChartPanel, netConstraints);

        GridBagConstraints netPanelConstraints = new GridBagConstraints();
        netPanelConstraints.fill = GridBagConstraints.NONE;
        netPanelConstraints.weightx = 3;
        netPanelConstraints.weighty = 1;
        netPanelConstraints.gridx = 1;
        netPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
        netPanel.setMinimumSize(new Dimension(685, 420));
        add(netPanel, netPanelConstraints);

        Thread thread = new Thread(() -> {
            while(true)
            {
                int newest = diskData.getNewestIndex();
                diskData.advanceTime();
                diskData.addValue(0, newest, (float)PerformancePanel.diskReadSpeed.get(index)/1024);
                diskData.addValue(1, newest, (float)PerformancePanel.diskWriteSpeed.get(index)/1024);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        });
        thread.start();

    }

    private static float[] floatArrayPercent(double d) {
        float[] f = new float[1];
        f[0] = (float) (d);
        return f;
    }
}
