package GUI;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;

import oshi.hardware.NetworkIF;


public class NetworkPanel extends PerformancePanel{

    public NetworkPanel(NetworkIF net, JButton button) {
        super();
        initial(net, button);
    }


    private void initial(NetworkIF net, JButton button) {
        GridBagConstraints netConstraints = new GridBagConstraints();

        netConstraints.weightx = 1d;
        netConstraints.weighty = 1d;
        netConstraints.fill = GridBagConstraints.NONE;
        netConstraints.anchor = GridBagConstraints.NORTHWEST;

        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        DynamicTimeSeriesCollection networkData = new DynamicTimeSeriesCollection(2, 60, new Second());
        networkData.setTimeBase(new Second(date));
        networkData.addSeries(floatArrayPercent(0d), 0, "Send");
        networkData.addSeries(floatArrayPercent(0d), 1, "Receive");
        JFreeChart netChart = ChartFactory.createTimeSeriesChart("Throughput", "Time", "Kbps", networkData, true, true, false);

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
                long timeNow = net.getTimeStamp();
                long recvLast = net.getBytesRecv();
                long sendLast = net.getBytesSent();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                net.updateAttributes();
                long recvNow = net.getBytesRecv();
                long sendNow = net.getBytesSent();
                int newest = networkData.getNewestIndex();
                long sendSpeed = (sendNow - sendLast)*1000/(net.getTimeStamp()-timeNow);
                long recvSpeed = (recvNow - recvLast)*1000/(net.getTimeStamp()-timeNow);
                button.setText(updateNetwork(net, recvSpeed, sendSpeed));
                networkData.advanceTime();
                networkData.addValue(0, newest, (float)sendSpeed/1024);
                networkData.addValue(1, newest, (float)recvSpeed/1024);
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