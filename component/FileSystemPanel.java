package component;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DefaultPieDataset;
import oshi.PlatformEnum;
import oshi.SystemInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.FormatUtil;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class FileSystemPanel extends Panel {

    private static final String USED = "Used";
    private static final String AVAILABLE = "Available";

    private JProgressBar[] progressBars;

    public FileSystemPanel(SystemInfo si) {
        super();
        init(si.getOperatingSystem().getFileSystem());
    }

    private void init(FileSystem fs){
        List<OSFileStore> fileStores = fs.getFileStores();
        progressBars = new JProgressBar[fileStores.size()];

        JPanel fsPanel = new JPanel();
        fsPanel.setLayout(new GridBagLayout());
        GridBagConstraints fsConstraints = new GridBagConstraints();
        fsConstraints.weightx = 1d;
        fsConstraints.weighty = 1d;
        fsConstraints.fill = GridBagConstraints.BOTH;
        fsConstraints.insets = new Insets(10, 10, 10, 10);

        JScrollPane scrollPane = new JScrollPane();

        int modBase = 2;
        for (int i = 0; i < fileStores.size(); i++) {
            progressBars[i] = new JProgressBar();
            fsConstraints.gridx = i % modBase;
            fsConstraints.gridy = i / modBase;

            JPanel progressBarPanel = new JPanel(new BorderLayout());

            JPanel northPanel = new JPanel();
            northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));

            JLabel labelName = new JLabel();
            // check OS
            if(SystemInfo.getCurrentPlatform().equals(PlatformEnum.WINDOWS)){

                labelName.setText(String.format("%s %s",fileStores.get(i).getName(),fileStores.get(i).getLabel()));

            } else if (SystemInfo.getCurrentPlatform().equals(PlatformEnum.LINUX)){

                labelName.setText(String.format("%s\n", fileStores.get(i).getVolume()));

            }

            labelName.setFont(new Font ("Arial", Font.BOLD, 16));
            northPanel.add(labelName);
            JLabel type = new JLabel();
            type.setText(fileStores.get(i).getType());
            type.setFont(new Font ("Arial", Font.BOLD, 16));

            northPanel.add(type);
            progressBarPanel.add(northPanel, BorderLayout.NORTH);
            progressBarPanel.add(progressBars[i], BorderLayout.CENTER);
            progressBarPanel.setPreferredSize(new Dimension(Config.GUI_WIDTH/4, 20));

            fsPanel.add(progressBarPanel, fsConstraints);
            scrollPane.setViewportView(fsPanel);
        }
        updateData(fileStores);

        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane,BorderLayout.CENTER);

        Timer timer = new Timer(Config.REFRESH_SLOWER, e -> {
            if (!updateData(fs.getFileStores())) {
                ((Timer) e.getSource()).stop();
                fsPanel.removeAll();
                init(fs);
                fsPanel.revalidate();
                fsPanel.repaint();
            }
        });
        timer.start();
    }

    private boolean updateData(List<OSFileStore> fileStores) {
        if (fileStores.size() != progressBars.length) {
            return false;
        }
        int i = 0;
        for (OSFileStore fileStore : fileStores) {
            long usable = fileStore.getUsableSpace();
            long total = fileStore.getTotalSpace();
            int value = (int) (((double) total - usable) / total * 100);
            progressBars[i].setValue(value);
            progressBars[i].setString(String.format("Available: (%s/%s)", FormatUtil.formatBytes(usable), FormatUtil.formatBytes(total)));
            progressBars[i].setStringPainted(true);
            i++;
        }
        return true;
    }
}
