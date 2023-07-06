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
import java.util.*;
import java.util.List;

public class ProcessPanel extends OshiJPanel {
    private static final long serialVersionUID = 1L;
    private static final String PROCESSES = "Processes";
    private static final String[] COLUMNS = {"PID", "PPID", "Status", "User", "Threads", "% CPU", " Cumulative", "VSZ", "RSS", "% Memory",
            "Process Name"};
    private static final double[] COLUMN_WIDTH_PERCENT = {0.07, 0.07, 0.07, 0.07, 0.07, 0.07, 0.09, 0.1, 0.1, 0.08, 0.35};
    private transient Map<Integer, OSProcess> priorSnapshotMap = new HashMap<>();


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
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(processTable.getModel());
        sorter.setComparator(0, new SizeComparator());
        sorter.setComparator(1, new SizeComparator());
        sorter.setComparator(4, new SizeComparator());
        sorter.setComparator(5, new SizeComparator());
        sorter.setComparator(6, new SizeComparator());
        sorter.setComparator(7, new SizeComparator());
        sorter.setComparator(8, new SizeComparator());
        sorter.setComparator(9, new SizeComparator());
        sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(9, SortOrder.DESCENDING)));

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
            TableRowSorter<DefaultTableModel> reSorter = (TableRowSorter<DefaultTableModel>) processTable.getRowSorter();
            List<? extends RowSorter.SortKey> sortKeys = reSorter.getSortKeys();
            reSorter.setModel(tableModel);
            sorter.setComparator(0, new SizeComparator());
            sorter.setComparator(1, new SizeComparator());
            sorter.setComparator(4, new SizeComparator());
            sorter.setComparator(5, new SizeComparator());
            sorter.setComparator(6, new SizeComparator());
            sorter.setComparator(7, new SizeComparator());
            sorter.setComparator(8, new SizeComparator());
            sorter.setComparator(9, new SizeComparator());
            reSorter.setSortKeys(sortKeys);
            reSorter.sort();

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
            int pid = p.getProcessID();
            procArr[i][0] = pid;
            procArr[i][1] = p.getParentProcessID();
            procArr[i][2] = p.getState().toString();
            if (!p.getUser().equals("unknown")) {
                procArr[i][3] = p.getUser();
            }
            procArr[i][4] = p.getThreadCount();
            procArr[i][5] = String.format("%.1f",
                    100d * p.getProcessCpuLoadBetweenTicks(priorSnapshotMap.get(pid)));
            procArr[i][6] = String.format("%.1f", 100d * p.getProcessCpuLoadCumulative());
            procArr[i][7] = FormatUtil.formatBytes(p.getVirtualSize());
            procArr[i][8] = FormatUtil.formatBytes(p.getResidentSetSize());
            procArr[i][9] = String.format("%.1f", 100d * p.getResidentSetSize() / totalMem);
            procArr[i][10] = p.getName();
        }
        // Re-populate snapshot map
        priorSnapshotMap.clear();
        for (OSProcess p : list) {
            priorSnapshotMap.put(p.getProcessID(), p);
        }
        return procArr;
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
            String[] parts = sizeString.split(" ");
            float value = Float.parseFloat(parts[0]);
            if(parts.length == 1){
                return value;
            }
            else{
                String unit = parts[1];
                switch (unit) {
                    case "bytes":
                        return value;
                    case "KiB":
                        return (value * 1024);
                    case "MiB":
                        return (value * 1024 * 1024);
                    case "GiB":
                        return (value * 1024 * 1024 * 1024);
                    default:
                        throw new IllegalArgumentException("Unknown size unit: " + unit);
                }
            }
        }

        @Override
        public int compare(Object o1, Object o2) {
            float size1 = parseSize(o1.toString());
            float size2 = parseSize(o2.toString());
            return Float.compare(size1,size2);
        }
    }
}
