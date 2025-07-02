package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
        // Set layout for the main panel
        setLayout(new GridBagLayout());
        
        GridBagConstraints diskConstraints = new GridBagConstraints();
        diskConstraints.weightx = 1.0;
        diskConstraints.weighty = 1.0;
        diskConstraints.fill = GridBagConstraints.BOTH;
        diskConstraints.gridx = 0;
        diskConstraints.gridy = 0;

        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        DynamicTimeSeriesCollection diskData = new DynamicTimeSeriesCollection(2, 60, new Second());
        diskData.setTimeBase(new Second(date));
        diskData.addSeries(floatArrayPercent(0d), 0, "Read");
        diskData.addSeries(floatArrayPercent(0d), 1, "Write");
        JFreeChart diskChart = ChartFactory.createTimeSeriesChart(disk.getModel(), "Time", "Kbps", diskData, true, true, false);

        diskChart.getXYPlot().getRangeAxis().setAutoRange(false);
        diskChart.getXYPlot().getRangeAxis().setRange(0d, 1000d);

        PerformancePanel.setChartRenderer(diskChart, Color.CYAN, Color.GREEN);

        // Create chart panel with responsive sizing
        ChartPanel myChartPanel = new ChartPanel(diskChart);
        
        // Calculate responsive dimensions based on screen size
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int chartWidth = Math.max(600, (int)(screenSize.width * 0.6)); // Minimum 600px or 60% of screen width
        int chartHeight = Math.max(300, (int)(screenSize.height * 0.4)); // Minimum 300px or 40% of screen height
        
        // Set preferred size for better initial display
        myChartPanel.setPreferredSize(new Dimension(chartWidth, chartHeight));
        
        // Set minimum size to prevent chart from becoming too small
        myChartPanel.setMinimumSize(new Dimension(400, 250));
        
        // Enable automatic scaling and resizing
        myChartPanel.setMouseWheelEnabled(true);
        myChartPanel.setRangeZoomable(true);
        myChartPanel.setDomainZoomable(true);
        
        // Add the chart panel directly to this panel
        add(myChartPanel, diskConstraints);

        // Start the data update thread
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
        thread.setDaemon(true); // Make thread daemon so it doesn't prevent application exit
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
        thread.setDaemon(true); // Make thread daemon
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