package gui;

import oshi.PlatformEnum;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.List;

public class ProcessPanel extends OshiJPanel {
    private static final long serialVersionUID = 1L;
    private static final String PROCESSES = "Processes";
    private static final String[] COLUMNS = {"Name", "CPU", "Memory", "Disk", "Network",
            "PPID", "Status", "User", "Threads", "Cumulative", "VSZ", "RSS", "PID"};
    private static final double[] COLUMN_WIDTH_PERCENT = {0.20, 0.07, 0.10, 0.07, 0.07, 0.07,
         0.07, 0.08, 0.07, 0.07, 0.07, 0.07, 0.07};


    public ProcessPanel(SystemInfo si) {
        super();
        init(si);
    }

    private void init(SystemInfo si) {
        OperatingSystem os = si.getOperatingSystem();
        JLabel processLabel = new JLabel(PROCESSES);
        Font arialFont = new Font("Arial", Font.BOLD, 18);
        processLabel.setFont(arialFont);

        JPanel processPanel = new JPanel();
        processPanel.setLayout(new GridBagLayout());
        GridBagConstraints nameConstraints = new GridBagConstraints();
        nameConstraints.anchor = GridBagConstraints.CENTER;
        nameConstraints.insets = new Insets(10, 10, 10, 10);
        processLabel.setMinimumSize(new Dimension(0,0));
        processLabel.setMaximumSize(new Dimension(50,50));
        add(processLabel, nameConstraints);

        TableModel model = new DefaultTableModel(parseProcesses(os.getProcesses(null, null, 0), si), COLUMNS);
        JTable processTable = new JTable(model);

        Font sansSerifFont = new Font("SansSerif", Font.PLAIN, 12);
        processTable.setFont(sansSerifFont);
        processTable.getTableHeader().setFont(new Font("Arial", Font.PLAIN, 14));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Set font color for "Running" in status column
                if (column == 2 && value != null) {
                    String status = value.toString();
                    c.setForeground(status.equalsIgnoreCase("running") ? Color.RED : Color.BLACK);
                }
                return c;
            }
        };
        processTable.getColumnModel().getColumn(2).setCellRenderer(renderer);

        // make sorter for Table
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        sorter.setComparator(1, new SizeComparator());  // CPU column
        sorter.setComparator(2, new SizeComparator());  // Memory column
        sorter.setComparator(3, new SizeComparator());  // Disk column
        sorter.setComparator(4, new SizeComparator());  // Network column
        sorter.setComparator(5, new NumericComparator()); // PPID column
        sorter.setComparator(8, new NumericComparator()); // Threads column
        sorter.setComparator(9, new SizeComparator());  // Cumulative
        sorter.setComparator(10, new SizeComparator()); // VSZ
        sorter.setComparator(11, new SizeComparator()); // RSS
        sorter.setComparator(12, new NumericComparator()); // PID
        
        processTable.setRowSorter(sorter);


        JScrollPane scroll = new JScrollPane(processTable);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        resizeColumns(processTable.getColumnModel());

        GridBagConstraints processConstraints = new GridBagConstraints();
        processConstraints.gridx = 0;
        processConstraints.gridy = 1;
        processConstraints.weightx = 1d;
        processConstraints.weighty = 1d;
        processConstraints.anchor = GridBagConstraints.NORTHWEST;
        processConstraints.fill = GridBagConstraints.BOTH;
        processConstraints.insets = new Insets(10, 10, 10, 10);

        add(scroll, processConstraints);

        Timer timer = new Timer(Config.REFRESH_SLOW, e -> {
            DefaultTableModel tableModel = (DefaultTableModel) processTable.getModel();
            Object[][] newData = parseProcesses(os.getProcesses(null, null, 0), si);
            int rowCount = tableModel.getRowCount();
            for (int row = 0; row < newData.length; row++) {
                if (row < rowCount) {
                    // Overwrite row
                    for (int col = 0; col < newData[row].length; col++) {
                        tableModel.setValueAt(newData[row][col], row, col);
                    }
                } else {
                    // Add row
                    tableModel.addRow(newData[row]);
                }
            }
            // Delete any extra rows
            for (int row = rowCount - 1; row >= newData.length; row--) {
                tableModel.removeRow(row);
            }


            // Reset row sorter and maintain current sorting
            List<? extends RowSorter.SortKey> sortKeys = sorter.getSortKeys();
            sorter.setModel(tableModel);
            sorter.setComparator(10, new SizeComparator());
            sorter.setComparator(2, new SizeComparator());
            sorter.setComparator(3, new SizeComparator());
            sorter.setComparator(1, new SizeComparator());
            sorter.setComparator(4, new SizeComparator());
            sorter.setComparator(5, new SizeComparator());
            sorter.setComparator(11, new SizeComparator());
            sorter.setComparator(12, new SizeComparator());
            sorter.setComparator(8, new SizeComparator());
            sorter.setComparator(9, new SizeComparator());
            sorter.setSortKeys(sortKeys);
            sorter.sort();

        });
        timer.start();
    }

    private Object[][] parseProcesses(List<OSProcess> list, SystemInfo si) {
        long totalMem = si.getHardware().getMemory().getTotal();

        List<OSProcess> procList = new ArrayList<>();
        for (OSProcess p : list) {

            // Ignore the Idle process on Windows
            if (p.getProcessID() == 0 && SystemInfo.getCurrentPlatform().equals(PlatformEnum.WINDOWS)) {
                continue;
            }

            procList.add(p);
        }

        Object[][] procArr = new Object[procList.size()][COLUMNS.length];
        for (int i = 0; i < procList.size(); i++) {
            OSProcess p = procList.get(i);
            procArr[i][0] = p.getName();
;
            procArr[i][1] = String.format("%.1f%%", 100d * p.getProcessCpuLoadBetweenTicks(null));
            procArr[i][2] = FormatUtil.formatBytes(p.getResidentSetSize()) + " (" + String.format("%.1f%%", 100d * p.getResidentSetSize() / totalMem) + ")";

            // Disk I/O via PowerShell (non-blocking fallback handled)
            String diskIo = "N/A";
            try {
                String diskIoCommand = String.format("powershell \"(Get-Counter -Counter '\\Process(%s)\\IO Data Bytes/sec' -ErrorAction SilentlyContinue).CounterSamples[0].CookedValue\"", 
                    p.getName().replace("'", "''"));
                diskIo = executeCommand(diskIoCommand);
                double diskBytesPerSec = Double.parseDouble(diskIo.trim());
                diskIo = String.format("%.1f MB/s", diskBytesPerSec / 1024.0 / 1024.0);
            } catch (Exception ignored) {
            }
            procArr[i][3] = diskIo;
            procArr[i][4] = String.format("%.1f Mbps", p.getSoftOpenFileLimit() / 125000.0); // Approximate network usage
            procArr[i][5] = p.getParentProcessID();
            procArr[i][6] = p.getState().toString();
            procArr[i][7] = p.getUser().equals("unknown") ? "N/A" : p.getUser();
            procArr[i][8] = p.getThreadCount();
            procArr[i][9] = String.format("%.1f", 100d * p.getProcessCpuLoadCumulative());
            procArr[i][10] = FormatUtil.formatBytes(p.getVirtualSize());
            procArr[i][11] = FormatUtil.formatBytes(p.getResidentSetSize());
            procArr[i][12] = p.getProcessID();
        }
        // Re-populate snapshot map
        return procArr;
    }
    private String executeCommand(String command) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            processBuilder.command("cmd.exe", "/c", command);
        } else {
            processBuilder.command("sh", "-c", command);
        }
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        process.waitFor();
        return output.toString();
    }

    private static void resizeColumns (TableColumnModel tableColumnModel){
        TableColumn column;
        int tW = tableColumnModel.getTotalColumnWidth();
        int cantCols = tableColumnModel.getColumnCount();
        for (int i = 0; i < cantCols; i++) {
            column = tableColumnModel.getColumn(i);
            int pWidth = (int) Math.round(COLUMN_WIDTH_PERCENT[i] * tW);
            column.setPreferredWidth(pWidth);
        }
    }
    



    private static class SizeComparator implements Comparator<Object> {

        private static float parseSize(String sizeString) {
            if (sizeString == null || sizeString.equals("N/A")) return 0;
            try {
                if (sizeString.endsWith("%")) {
                    return Float.parseFloat(sizeString.replace("%", ""));
                } else if (sizeString.contains("MB/s")) {
                    return Float.parseFloat(sizeString.replace(" MB/s", ""));
                } else if (sizeString.contains("Mbps")) {
                    return Float.parseFloat(sizeString.replace(" Mbps", ""));
                }
                String[] parts = sizeString.split(" ");
                float value = Float.parseFloat(parts[0]);
                if (parts.length == 1) return value;
                switch (parts[1]) {
                    case "bytes": return value;
                    case "KiB": return value * 1024;
                    case "MiB": return value * 1024 * 1024;
                    case "GiB": return value * 1024 * 1024 * 1024;
                    default: return 0; // fallback an toàn
                    }
                } catch (Exception e) {
                    return 0; // fallback nếu format sai
                    }
                }
        
        @Override
        public int compare(Object o1, Object o2) {
            float size1 = parseSize(o1.toString());
            float size2 = parseSize(o2.toString());
            return Float.compare(size1,size2);
        }
    }
    
    private static class NumericComparator implements Comparator<Object> {
        @Override
        public int compare(Object o1, Object o2) {
            try {
                Integer i1 = Integer.parseInt(o1.toString());
                Integer i2 = Integer.parseInt(o2.toString());
                return Integer.compare(i1, i2);
            } catch (NumberFormatException e) {
                return 0; // fallback nếu không thể parse
            }
        }
    }
    public static class ProcessDataExtractor {
        private final SystemInfo systemInfo;
        private static final String[] COLUMNS = {"Name", "CPU", "Memory", "Disk", "Network",
            "PPID", "Status", "User", "Threads", "Cumulative", "VSZ", "RSS", "PID"};
        public ProcessDataExtractor(SystemInfo systemInfo) {
            this.systemInfo = systemInfo;
        }
        public Object[][] parseProcesses() {
            OperatingSystem os = systemInfo.getOperatingSystem();
            return parseProcesses(os.getProcesses(null, null, 0), systemInfo);
        }
            
        private Object[][] parseProcesses(List<OSProcess> list, SystemInfo si) {
            long totalMem = si.getHardware().getMemory().getTotal();
            List<OSProcess> procList = new ArrayList<>();
            for (OSProcess p : list) {
                // Ignore the Idle process on Windows
                if (p.getProcessID() == 0 && SystemInfo.getCurrentPlatform().equals(PlatformEnum.WINDOWS)) {
                    continue;
                }
                procList.add(p);
            }
            Object[][] procArr = new Object[procList.size()][COLUMNS.length];
            for (int i = 0; i < procList.size(); i++) {
                OSProcess p = procList.get(i);
                procArr[i][0] = p.getName();
                procArr[i][1] = String.format("%.1f%%", 100d * p.getProcessCpuLoadBetweenTicks(null));
                procArr[i][2] = FormatUtil.formatBytes(p.getResidentSetSize()) + " (" + String.format("%.1f%%", 100d * p.getResidentSetSize() / totalMem) + ")";
                // Disk I/O via PowerShell (non-blocking fallback handled)
                String diskIo = "N/A";
                try {
                    String diskIoCommand = String.format("powershell \"(Get-Counter -Counter '\\Process(%s)\\IO Data Bytes/sec' -ErrorAction SilentlyContinue).CounterSamples[0].CookedValue\"", 
                    p.getName().replace("'", "''"));
                    diskIo = executeCommand(diskIoCommand);
                    double diskBytesPerSec = Double.parseDouble(diskIo.trim());
                    diskIo = String.format("%.1f MB/s", diskBytesPerSec / 1024.0 / 1024.0);
                } catch (Exception ignored) {   
                }
                procArr[i][3] = diskIo;
                procArr[i][4] = String.format("%.1f Mbps", p.getSoftOpenFileLimit() / 125000.0); // Approximate network usage
                procArr[i][5] = p.getParentProcessID();
                procArr[i][6] = p.getState().toString();
                procArr[i][7] = p.getUser().equals("unknown") ? "N/A" : p.getUser();
                procArr[i][8] = p.getThreadCount();
                procArr[i][9] = String.format("%.1f", 100d * p.getProcessCpuLoadCumulative());
                procArr[i][10] = FormatUtil.formatBytes(p.getVirtualSize());
                procArr[i][11] = FormatUtil.formatBytes(p.getResidentSetSize());
                procArr[i][12] = p.getProcessID();
            }
            return procArr;
        }
        private String executeCommand(String command) throws Exception {
            ProcessBuilder processBuilder = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                processBuilder.command("cmd.exe", "/c", command);
            } else {
                processBuilder.command("sh", "-c", command);
            }
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
            return output.toString();
        }
    }
}
