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
    private static final String[] COLUMNS = {"PID", "PPID", "Status", "User", "Threads", "% CPU", " Cumulative", "VSZ", "RSS", "% Memory", "Process Name"};
    private static final double[] COLUMN_WIDTH_PERCENT = {0.07, 0.07, 0.07, 0.07, 0.07, 0.07, 0.09, 0.1, 0.1, 0.08, 0.35};
    private transient Map<Integer, OSProcess> priorSnapshotMap = new HashMap<>();

    public ProcessPanel(SystemInfo si) {
        super();
        init(si);
    }

    private void init(SystemInfo si) {
        OperatingSystem os = si.getOperatingSystem();
        // Set layout for main panel
        this.setLayout(new BorderLayout());

        // Create Processes header label
        JLabel processLabel = new JLabel(PROCESSES);
        processLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        processLabel.setForeground(Color.BLACK);
        processLabel.setBorder(BorderFactory.createEmptyBorder(15, 2, 10, 0));

        // Create header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.add(processLabel, BorderLayout.WEST);

        // Add separator line
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(new Color(230, 230, 230));
        separator.setBackground(new Color(230, 230, 230));
        headerPanel.add(separator, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);

        TableModel model = new DefaultTableModel(parseProcesses(os.getProcesses(null, null, 0), si), COLUMNS);
        JTable processTable = new JTable(model);
        processTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        processTable.getTableHeader().setFont(new Font("SansSerif", Font.PLAIN, 14));
        processTable.setRowHeight(24);
        processTable.setGridColor(new Color(230, 230, 230));
        processTable.setShowVerticalLines(true);
        processTable.setShowHorizontalLines(false);
        processTable.setBorder(BorderFactory.createEmptyBorder());

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column == 2 && value != null) {
                    String status = value.toString();
                    c.setForeground(status.equalsIgnoreCase("running") ? new Color(220, 20, 60) : Color.DARK_GRAY);
                } else {
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        };
        processTable.getColumnModel().getColumn(2).setCellRenderer(renderer);

        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        for (int i : new int[]{0,1,4,5,6,7,8,9}) {
            sorter.setComparator(i, new SizeComparator());
        }
        sorter.setSortKeys(List.of(new RowSorter.SortKey(9, SortOrder.DESCENDING)));
        processTable.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(processTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        resizeColumns(processTable.getColumnModel());
        add(scroll, BorderLayout.CENTER);

        Timer timer = new Timer(Config.REFRESH_SLOW, e -> {
            DefaultTableModel tableModel = (DefaultTableModel) processTable.getModel();
            Object[][] newData = parseProcesses(os.getProcesses(null, null, 0), si);
            int rowCount = tableModel.getRowCount();
            for (int row = 0; row < newData.length; row++) {
                if (row < rowCount) {
                    for (int col = 0; col < newData[row].length; col++) {
                        tableModel.setValueAt(newData[row][col], row, col);
                    }
                } else {
                    tableModel.addRow(newData[row]);
                }
            }
            for (int row = rowCount - 1; row >= newData.length; row--) {
                tableModel.removeRow(row);
            }
            sorter.sort();
        });
        timer.start();
    }

    private Object[][] parseProcesses(List<OSProcess> list, SystemInfo si) {
        long totalMem = si.getHardware().getMemory().getTotal();
        List<OSProcess> procList = new ArrayList<>();
        for (OSProcess p : list) {
            if (p.getProcessID() == 0 && SystemInfo.getCurrentPlatform().equals(PlatformEnum.WINDOWS)) continue;
            procList.add(p);
        }

        Object[][] procArr = new Object[procList.size()][COLUMNS.length];
        for (int i = 0; i < procList.size(); i++) {
            OSProcess p = procList.get(i);
            int pid = p.getProcessID();
            procArr[i][0] = pid;
            procArr[i][1] = p.getParentProcessID();
            procArr[i][2] = p.getState().toString();
            procArr[i][3] = p.getUser().equals("unknown") ? "" : p.getUser();
            procArr[i][4] = p.getThreadCount();
            procArr[i][5] = String.format("%.1f", 100d * p.getProcessCpuLoadBetweenTicks(priorSnapshotMap.get(pid)));
            procArr[i][6] = String.format("%.1f", 100d * p.getProcessCpuLoadCumulative());
            procArr[i][7] = FormatUtil.formatBytes(p.getVirtualSize());
            procArr[i][8] = FormatUtil.formatBytes(p.getResidentSetSize());
            procArr[i][9] = String.format("%.1f", 100d * p.getResidentSetSize() / totalMem);
            procArr[i][10] = p.getName();
        }

        priorSnapshotMap.clear();
        for (OSProcess p : list) {
            priorSnapshotMap.put(p.getProcessID(), p);
        }
        return procArr;
    }

    private static void resizeColumns(TableColumnModel model) {
        int tW = model.getTotalColumnWidth();
        for (int i = 0; i < model.getColumnCount(); i++) {
            model.getColumn(i).setPreferredWidth((int) (COLUMN_WIDTH_PERCENT[i] * tW));
        }
    }

    private static class SizeComparator implements Comparator<Object> {
        private static float parseSize(String sizeString) {
            try {
                String[] parts = sizeString.split(" ");
                float value = Float.parseFloat(parts[0]);
                return switch (parts.length > 1 ? parts[1] : "") {
                    case "bytes" -> value;
                    case "KiB" -> value * 1024;
                    case "MiB" -> value * 1024 * 1024;
                    case "GiB" -> value * 1024 * 1024 * 1024;
                    default -> value;
                };
            } catch (Exception e) {
                return 0;
            }
        }
        public int compare(Object o1, Object o2) {
            return Float.compare(parseSize(o1.toString()), parseSize(o2.toString()));
        }
    }
}
