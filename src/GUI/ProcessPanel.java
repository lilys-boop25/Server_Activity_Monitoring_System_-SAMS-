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
    private transient ButtonGroup cpuOption = new ButtonGroup();
    private transient JRadioButton perProc = new JRadioButton("of one Processor");
    private transient JRadioButton perSystem = new JRadioButton("of System");



    public ProcessPanel(SystemInfo si) {
        super();
        init(si);
    }

    private static class NumericComparator implements Comparator<Object> {
        @Override
        public int compare(Object o1, Object o2) {
            float i1 = Float.parseFloat(o1.toString());
            float i2 = Float.parseFloat(o2.toString());
            return Float.compare(i1,i2);
        }
    }

    private void init(SystemInfo si) {
        OperatingSystem os = si.getOperatingSystem();
        JLabel procLabel = new JLabel(PROCESSES);
        Font arialFont = new Font("Arial", Font.BOLD, 18);
        procLabel.setFont(arialFont);

        JPanel processPanel = new JPanel();
        processPanel.setLayout(new GridBagLayout());
        GridBagConstraints nameConstraints = new GridBagConstraints();
        nameConstraints.anchor = GridBagConstraints.CENTER;
//        nameConstraints.fill = GridBagConstraints.BOTH;
        nameConstraints.insets = new Insets(10, 10, 10, 10);
        procLabel.setMinimumSize(new Dimension(0,0));
        procLabel.setMaximumSize(new Dimension(50,50));
        add(procLabel, nameConstraints);

        JPanel settings = new JPanel();
        JLabel cpuChoice = new JLabel("          % CPU:");
        settings.add(cpuChoice);

        cpuOption.add(perProc);
        settings.add(perProc);
        cpuOption.add(perSystem);
        settings.add(perSystem);

        if (SystemInfo.getCurrentPlatform().equals(PlatformEnum.WINDOWS)) { // in Windows
            perSystem.setSelected(true);
        } else { // in Linux
            perProc.setSelected(true);
        }

        TableModel model = new DefaultTableModel(parseProcesses(os.getProcesses(null, null, 0), si), COLUMNS);
        JTable proccessTable = new JTable(model);

        Font sansSerifFont = new Font("SansSerif", Font.PLAIN, 12);
        proccessTable.setFont(sansSerifFont);
        proccessTable.getTableHeader().setFont(new Font("Arial", Font.PLAIN, 14));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Set font color for "Running" in status column
                if (column == 2) {
                    if(value != null) {
                        String status = value.toString();
                        if(status.equalsIgnoreCase("running")){
                            c.setForeground(Color.RED);
                        } else{
                            c.setForeground(Color.BLACK);
                        }
                    }
                }
                return c;
            }
        };
        proccessTable.getColumnModel().getColumn(2).setCellRenderer(renderer);

        // make sorter for Table
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(proccessTable.getModel());
        sorter.setComparator(0, new NumericComparator());
        sorter.setComparator(1, new NumericComparator());
        sorter.setComparator(4, new NumericComparator());
        sorter.setComparator(5, new NumericComparator());
        sorter.setComparator(6, new NumericComparator());
        sorter.setComparator(9, new NumericComparator());
        sorter.setSortable(7,false);
        sorter.setSortable(8,false);
        sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(9, SortOrder.DESCENDING)));

        proccessTable.setRowSorter(sorter);


        JScrollPane scroll = new JScrollPane(proccessTable);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        resizeColumns(proccessTable.getColumnModel());

        GridBagConstraints processConstraints = new GridBagConstraints();
        processConstraints.gridx = 0;
        processConstraints.gridy = 1;
        processConstraints.weightx = 1d;
        processConstraints.weighty = 1d;
        processConstraints.anchor = GridBagConstraints.NORTHWEST;
        processConstraints.fill = GridBagConstraints.BOTH;
        processConstraints.insets = new Insets(10, 10, 10, 10);

        add(scroll, processConstraints);
        GridBagConstraints settingsConstraints = new GridBagConstraints();
        settingsConstraints.gridx = 0;
        settingsConstraints.gridy = 2;

        settingsConstraints.anchor = GridBagConstraints.NORTHWEST;
        settingsConstraints.fill = GridBagConstraints.BOTH;
        add(settings, settingsConstraints);

        Timer timer = new Timer(Config.REFRESH_SLOW, e -> {
            DefaultTableModel tableModel = (DefaultTableModel) proccessTable.getModel();
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
            TableRowSorter<TableModel> re_sorter = (TableRowSorter<TableModel>) proccessTable.getRowSorter();
            List<RowSorter.SortKey> sortKeys = (List<RowSorter.SortKey>) re_sorter.getSortKeys();
            re_sorter.setModel(tableModel);
            re_sorter.setComparator(0, new NumericComparator());
            re_sorter.setComparator(1, new NumericComparator());
            re_sorter.setComparator(4, new NumericComparator());
            re_sorter.setComparator(5, new NumericComparator());
            re_sorter.setComparator(6, new NumericComparator());
            re_sorter.setSortable(7,false);
            re_sorter.setSortable(8,false);
            re_sorter.setComparator(9, new NumericComparator());
            re_sorter.setSortKeys(sortKeys);
            re_sorter.sort();
        });

        timer.start();
    }

    private Object[][] parseProcesses(List<OSProcess> list, SystemInfo si) {
        long totalMem = si.getHardware().getMemory().getTotal();
        int cpuCount = si.getHardware().getProcessor().getLogicalProcessorCount();

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
            if (perProc.isSelected()) {
                procArr[i][5] = String.format("%.1f",
                        100d * p.getProcessCpuLoadBetweenTicks(priorSnapshotMap.get(pid)) * cpuCount);
                procArr[i][6] = String.format("%.1f", 100d * p.getProcessCpuLoadCumulative() * cpuCount);
            } else {
                procArr[i][5] = String.format("%.1f",
                        100d * p.getProcessCpuLoadBetweenTicks(priorSnapshotMap.get(pid)));
                procArr[i][6] = String.format("%.1f", 100d * p.getProcessCpuLoadCumulative());
            }

//            procArr[i][7] = String.format("%.1f MB", (float)(p.getVirtualSize())/(1024*1024));
//            procArr[i][8] = String.format("%.1f MB", (float)(p.getResidentSetSize())/(1024*1024));
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
}
