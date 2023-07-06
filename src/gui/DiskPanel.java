package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.util.FormatUtil;

/**
 * Displays physical and virtual (swap) memory stats.
 */
public class DiskPanel extends OshiJPanel { // NOSONAR squid:S110

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(DiskPanel.class);

    public DiskPanel(HWDiskStore disk, int index) {
        super();
        init(disk, index);
    }

    private void init(HWDiskStore disk, int index) {
        GridBagConstraints diskConstraints = new GridBagConstraints();

        diskConstraints.weightx = 1d;
        diskConstraints.weighty = 1d;
        diskConstraints.fill = GridBagConstraints.NONE;

        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        DynamicTimeSeriesCollection diskData = new DynamicTimeSeriesCollection(2, 60, new Second());
        diskData.setTimeBase(new Second(date));
        diskData.addSeries(floatArrayPercent(0d), 0, "Read");
        diskData.addSeries(floatArrayPercent(0d), 1, "Write");
        JFreeChart diskChart = ChartFactory.createTimeSeriesChart(disk.getModel(), "Time", "Kbps", diskData, true, true, false);

        diskChart.getXYPlot().getRangeAxis().setAutoRange(false);
        diskChart.getXYPlot().getRangeAxis().setRange(0d, 1000d);

        PerformancePanel.setChartRenderer(diskChart, Color.CYAN, Color.GREEN);

        JPanel diskPanel = new JPanel();
        diskPanel.setLayout(new GridBagLayout());
        ChartPanel myChartPanel = new ChartPanel(diskChart);
        myChartPanel.setMinimumSize(new Dimension(700, 350));
        diskPanel.add(myChartPanel, diskConstraints);

        GridBagConstraints diskPanelConstraints = new GridBagConstraints();
        diskPanelConstraints.fill = GridBagConstraints.BOTH;
        diskPanelConstraints.weightx = 1d;
        diskPanelConstraints.weighty = 1d;
        diskPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
        diskPanel.setMinimumSize(new Dimension(685, 420));
        add(diskPanel, diskPanelConstraints);

        Thread thread = new Thread(() -> {
            while(true)
            {
                int newest = diskData.getNewestIndex();
                diskData.advanceTime();
                diskData.addValue(0, newest, (float)diskReadSpeed.get(index)/(float)1024);
                diskData.addValue(1, newest, (float)diskWriteSpeed.get(index)/(float)1024);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    logger.error("Error occurred: ", e1);
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

    protected static List<Long> diskReadSpeed = new ArrayList<>(
            Collections.nCopies(100, (long)0));
    protected static List<Long> diskWriteSpeed = new ArrayList<>(
            Collections.nCopies(100, (long)0));
    private static boolean run = false;

    protected static void updateDiskInfo(List<HWDiskStore> diskStores, JGradientButton[] diskButton)
    {
        if (run)
        {
            return;
        }
        run = true;
        Thread thread = new Thread(() -> {
            while(true)
            {
                long[] timeNow = new long[diskStores.size()];
                long[] readLast = new long[diskStores.size()];
                long[] writeLast = new long[diskStores.size()];
                long[] readNow = new long[diskStores.size()];
                long[] writeNow = new long[diskStores.size()];
                for (int i = 0; i < diskStores.size() ; i++)
                {
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
                for (int i = 0; i < diskStores.size() ; i++)
                {
                    HWDiskStore disk = diskStores.get(i);
                    disk.updateAttributes();
                    readNow[i] = disk.getReadBytes();
                    writeNow[i] = disk.getWriteBytes();
                    diskReadSpeed.set(i, (readNow[i] - readLast[i])*1000/(disk.getTimeStamp()-timeNow[i]));
                    diskWriteSpeed.set(i, (writeNow[i] - writeLast[i])*1000/(disk.getTimeStamp()-timeNow[i]));
                    diskButton[i].setText(updateDisk(disk, i, diskReadSpeed.get(i), diskWriteSpeed.get(i)));
                }
            }
        });
        thread.start();
    }

    public static String updateDisk(HWDiskStore disk, int index, long recvSpeed, long sendSpeed)
    {
        StringBuilder nameBuffer = new StringBuilder();
        nameBuffer.append("Disk " + index + " (");
        for (HWPartition partition: disk.getPartitions())
        {
            nameBuffer.append(partition.getMountPoint() + " ");
        }
        nameBuffer.deleteCharAt(nameBuffer.length() - 1);
        if (!disk.getPartitions().isEmpty()){
            nameBuffer.append(")");
        }
        String name;
        if (nameBuffer.length() > 30) {
            name = nameBuffer.substring(0,30) + "...";
        }
        else{
            name = nameBuffer.toString();
        }
        String txt = name + "\nRead: " + FormatUtil.formatBytes(sendSpeed) + "\nWrite: " + FormatUtil.formatBytes(recvSpeed) + '\n';
        return PerformancePanel.buttonTextLines(txt);
    }

}
