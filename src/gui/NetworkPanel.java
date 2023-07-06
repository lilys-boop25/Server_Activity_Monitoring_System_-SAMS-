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

import oshi.hardware.NetworkIF;
import oshi.util.FormatUtil;


public class NetworkPanel extends PerformancePanel{
    private static final Logger logger = LoggerFactory.getLogger(NetworkPanel.class);
    public NetworkPanel(NetworkIF net, int index) {
        super();
        initial(net, index);
    }


    private void initial(NetworkIF net, int index) {
        GridBagConstraints netConstraints = new GridBagConstraints();

        netConstraints.weightx = 1d;
        netConstraints.weighty = 1d;
        netConstraints.fill = GridBagConstraints.BOTH;
        netConstraints.anchor = GridBagConstraints.NORTHWEST;

        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        DynamicTimeSeriesCollection networkData = new DynamicTimeSeriesCollection(2, 60, new Second());
        networkData.setTimeBase(new Second(date));
        networkData.addSeries(floatArrayPercent(0d), 0, "Send");
        networkData.addSeries(floatArrayPercent(0d), 1, "Receive");
        JFreeChart netChart = ChartFactory.createTimeSeriesChart(net.getDisplayName(), "Time", "Kbps", networkData, true, true, false);

        netChart.getXYPlot().getRangeAxis().setAutoRange(false);
        netChart.getXYPlot().getRangeAxis().setRange(0d, 1000d);

        PerformancePanel.setChartRenderer(netChart, Color.ORANGE, Color.YELLOW);

        JPanel netPanel = new JPanel();

        netPanel.setLayout(new GridBagLayout());
        ChartPanel myChartPanel = new ChartPanel(netChart);
        netPanel.add(myChartPanel, netConstraints);

        GridBagConstraints netPanelConstraints = new GridBagConstraints();
        netPanelConstraints.fill = GridBagConstraints.BOTH;
        netPanelConstraints.weightx = 1d;
        netPanelConstraints.weighty = 1d;
        netPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
        netPanel.setMinimumSize(new Dimension(795,530));
        add(netPanel, netPanelConstraints);

        Thread thread = new Thread(() -> {
            while(true)
            {
                int newest = networkData.getNewestIndex();
                networkData.advanceTime();
                networkData.addValue(0, newest, (float)networkSentSpeed.get(index)/(float)1024);
                networkData.addValue(1, newest, (float)networkRecvSpeed.get(index)/(float)1024);
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

    protected static List<Long> networkSentSpeed = new ArrayList<>(
            Collections.nCopies(100, (long)0));
    protected static List<Long> networkRecvSpeed = new ArrayList<>(
            Collections.nCopies(100, (long)0));
    private static boolean run = false;


    public static void updateNetWorkInfo(List<NetworkIF> networkIFs, JGradientButton[] netButton){
        if (run)
        {
            return;
        }
        run = true;
        Thread thread = new Thread(() -> {
            while(true)
            {
                long[] timeNow = new long[networkIFs.size()];
                long[] recvLast = new long[networkIFs.size()];
                long[] sentLast = new long[networkIFs.size()];

                for (int i = 0; i < networkIFs.size() ; i++)
                {
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

                for (int i = 0; i < networkIFs.size() ; i++)
                {
                    NetworkIF net = networkIFs.get(i);
                    net.updateAttributes();
                    networkSentSpeed.set(i, (net.getBytesSent() - sentLast[i])*1000/(net.getTimeStamp()-timeNow[i]));
                    networkRecvSpeed.set(i, (net.getBytesRecv() - recvLast[i])*1000/(net.getTimeStamp()-timeNow[i]));
                    netButton[i].setText(updateNetwork(net, networkRecvSpeed.get(i), networkSentSpeed.get(i)));
                }
            }
        });
        thread.start();

    }

    public static String updateNetwork(NetworkIF net, long recvSpeed, long sendSpeed)
    {
        String name = net.getDisplayName();
        if (name.length() > 30)
        {
            name = name.substring(0,30) + "...";
        }
        String alias = net.getIfAlias();
        if (alias.length() > 30)
        {
            alias = alias.substring(0,30) + "...";
        }
        String txt = name + "\n" + alias + "\nSend: " + FormatUtil.formatBytes(sendSpeed) + "\nReceive: " + FormatUtil.formatBytes(recvSpeed);
        return PerformancePanel.buttonTextLines(txt);
    }
}
