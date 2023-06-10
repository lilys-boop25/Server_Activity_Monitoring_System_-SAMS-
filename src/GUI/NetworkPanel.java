package GUI;

import java.awt.BorderLayout;
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
import oshi.util.FormatUtil;



public class NetworkPanel extends OshiJPanel{

    public NetworkPanel(NetworkIF net, JButton button) {
        super();
        initial(net, button);
    }
    

    private void initial(NetworkIF net, JButton button) {
        GridBagConstraints sysConstraints = new GridBagConstraints();

        sysConstraints.weightx = 1d;
        sysConstraints.weighty = 1d;
        sysConstraints.fill = GridBagConstraints.NONE;

        GridBagConstraints procConstraints = (GridBagConstraints) sysConstraints.clone();
        procConstraints.gridx = 1;

        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        DynamicTimeSeriesCollection networkData = new DynamicTimeSeriesCollection(2, 60, new Second());
        networkData.setTimeBase(new Second(date));
        networkData.addSeries(floatArrayPercent(0d), 0, "Send");
        networkData.addSeries(floatArrayPercent(0d), 1, "Receive");
        JFreeChart systemCpuChart = ChartFactory.createTimeSeriesChart("Throughput", "Time", "Kbps", networkData, true, true, false);

        systemCpuChart.getXYPlot().getRangeAxis().setAutoRange(false);
        systemCpuChart.getXYPlot().getRangeAxis().setRange(0d, 1000d);

        JPanel cpuPanel = new JPanel();
        cpuPanel.setLayout(new GridBagLayout());
        cpuPanel.add(new ChartPanel(systemCpuChart), sysConstraints);

        add(cpuPanel, BorderLayout.EAST);
        
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

    private String updateNetwork(NetworkIF net, long recvSpeed, long sendSpeed)
    {
        String name = net.getDisplayName();
        if (name.length() > 20)
        {
            name = name.substring(0,20) + "...";
        }
        String txt = name + "\n" + net.getIfAlias() + "\nSend: " + FormatUtil.formatBytes(sendSpeed) + "\nReceive: " + FormatUtil.formatBytes(recvSpeed);
        return buttonTextLines(txt);
    }
    
    public String buttonTextLines(String txt)
    {
        return "<html>" + htmlSpace(3) + txt.replaceAll("\\n", "<br>" + htmlSpace(3));
    }

    private String htmlSpace(int num)
    {
        return "&nbsp;".repeat(num);
    }

}
