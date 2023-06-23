package GUI;

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

import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.util.FormatUtil;

/**
 * Displays physical and virtual (swap) memory stats.
 */
public class DiskPanel extends OshiJPanel { // NOSONAR squid:S110

    private static final long serialVersionUID = 1L;

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
        JFreeChart netChart = ChartFactory.createTimeSeriesChart(disk.getModel(), "Time", "Kbps", diskData, true, true, false);

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
                diskData.addValue(0, newest, (float)diskReadSpeed.get(index)/(float)1024);
                diskData.addValue(1, newest, (float)diskWriteSpeed.get(index)/(float)1024);
                try {
                    Thread.sleep(1000);
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

    protected static List<Long> diskReadSpeed = new ArrayList<>(
    Collections.nCopies(100, (long)0));
    protected static List<Long> diskWriteSpeed = new ArrayList<>(
    Collections.nCopies(100, (long)0));
    private static boolean run = false;
    
    protected static void updateDiskInfo(List<HWDiskStore> diskStores, JGradientButton[] diskButton)
    {
        if (run == true)
        {
            return;
        }
        run = true;
        Thread thread = new Thread(() -> {
            while(true)
            {
                long timeNow[] = new long[diskStores.size()];
                long readLast[] = new long[diskStores.size()];
                long writeLast[] = new long[diskStores.size()];
                long readNow[] = new long[diskStores.size()];
                long writeNow[] = new long[diskStores.size()];
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
                    e1.printStackTrace();
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
            StringBuffer nameBuffer = new StringBuffer();
            nameBuffer.append("Disk " + String.valueOf(index) + " (");
            for (HWPartition partition: disk.getPartitions())
            {
                nameBuffer.append(partition.getMountPoint() + " ");
            }
            nameBuffer.deleteCharAt(nameBuffer.length() - 1);
            nameBuffer.append(")");
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
