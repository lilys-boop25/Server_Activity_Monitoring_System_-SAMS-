package component;

//import component.OshiJPanel;
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

public class ProcessPanel extends Panel {
    private static final long serialVersionUID = 1L;
    private static final String PROCESSES = "Processes";
    private static final String[] COLUMNS = {"PID", "PPID", "Status", "User", "Threads", "% CPU", " Cumulative", "VSZ", "RSS", "% Memory",
            "Process Name"};
    private static final double[] COLUMN_WIDTH_PERCENT = {0.07, 0.07, 0.07, 0.07, 0.07, 0.07, 0.09, 0.1, 0.1, 0.08, 0.35};
    private transient Map<Integer, OSProcess> priorSnapshotMap = new HashMap<>();
//    private transient ButtonGroup sortOption = new ButtonGroup();
    private transient ButtonGroup cpuOption = new ButtonGroup();
    private transient JRadioButton perProc = new JRadioButton("of one Processor");
    private transient JRadioButton perSystem = new JRadioButton("of System");

//    private transient JRadioButton memButton = new JRadioButton("% Memory");
//    private transient JRadioButton cpuButton = new JRadioButton("% CPU");


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

        add(procLabel, BorderLayout.NORTH);

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
        JTable procTable = new JTable(model);

        // make sorter for Table
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(procTable.getModel());
        sorter.setComparator(0, new NumericComparator());
        sorter.setComparator(1, new NumericComparator());
        sorter.setComparator(4, new NumericComparator());
        sorter.setComparator(5, new NumericComparator());
        sorter.setComparator(6, new NumericComparator());
        sorter.setComparator(9, new NumericComparator());


        procTable.setRowSorter(sorter);
        sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(9, SortOrder.DESCENDING)));
        sorter.setSortable(7,false);
        sorter.setSortable(8,false);

        JScrollPane scroll = new JScrollPane(procTable);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        resizeColumns(procTable.getColumnModel());

        add(scroll, BorderLayout.CENTER);
        add(settings, BorderLayout.SOUTH);

        Timer timer = new Timer(Config.REFRESH_SLOW, e -> {
            DefaultTableModel tableModel = (DefaultTableModel) procTable.getModel();
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

        });
        timer.start();
    }

    private Object[][] parseProcesses(List<OSProcess> list, SystemInfo si) {
        long totalMem = si.getHardware().getMemory().getTotal();
        int cpuCount = si.getHardware().getProcessor().getLogicalProcessorCount();

        int i = list.size();
        Object[][] procArr = new Object[i][COLUMNS.length];
        // These are in descending CPU order
        for (OSProcess p : list) {
            // Matches order of COLUMNS field
            i--;
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

