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
        // Sử dụng GridBagLayout cho toàn bộ panel để responsive
        this.setLayout(new GridBagLayout());
        
        GridBagConstraints mainConstraints = new GridBagConstraints();
        mainConstraints.weightx = 1.0;
        mainConstraints.weighty = 1.0;
        mainConstraints.fill = GridBagConstraints.BOTH;
        mainConstraints.anchor = GridBagConstraints.NORTHWEST;

        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        DynamicTimeSeriesCollection networkData = new DynamicTimeSeriesCollection(2, 60, new Second());
        networkData.setTimeBase(new Second(date));
        networkData.addSeries(floatArrayPercent(0d), 0, "Send");
        networkData.addSeries(floatArrayPercent(0d), 1, "Receive");
        JFreeChart netChart = ChartFactory.createTimeSeriesChart(net.getDisplayName(), "Time", "Kbps", networkData, true, true, false);

        netChart.getXYPlot().getRangeAxis().setAutoRange(false);
        netChart.getXYPlot().getRangeAxis().setRange(0d, 1000d);

        PerformancePanel.setChartRenderer(netChart, Color.ORANGE, Color.YELLOW);

        // Tạo ChartPanel với responsive sizing
        ChartPanel myChartPanel = new ChartPanel(netChart);
        
        // Thiết lập kích thước tối thiểu và tối đa cho ChartPanel
        myChartPanel.setMinimumDrawWidth(200);
        myChartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
        myChartPanel.setMinimumDrawHeight(150);
        myChartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
        
        // Cho phép chart scale theo kích thước container
        myChartPanel.setFillZoomRectangle(true);
        myChartPanel.setMouseWheelEnabled(true);
        
        // Thiết lập preferred size dựa trên tỷ lệ aspect ratio
        myChartPanel.setPreferredSize(new Dimension(800, 400));
        
        // Thêm ChartPanel trực tiếp vào NetworkPanel
        this.add(myChartPanel, mainConstraints);

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