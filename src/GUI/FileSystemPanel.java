package GUI;


import oshi.PlatformEnum;
import oshi.SystemInfo;
import oshi.software.os.OSFileStore;
import oshi.util.FormatUtil;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;

public class FileSystemPanel extends OshiJPanel{
    private static final String[] COLUMNS = {"Device", "Type", "Total", "Available", "Used", "Diagram"};
    private static final double[] COLUMN_WIDTH_PERCENT = {0.03, 0.01, 0.01, 0.01, 0.01, 2};

    private static long parseSize(String sizeString) {
        String[] parts = sizeString.split(" ");
        float value = Float.parseFloat(parts[0]);
        String unit = parts[1];
        switch (unit) {
            case "B":
                return (long) value;
            case "KiB":
                return (long) (value * 1024);
            case "MiB":
                return (long) (value * 1024 * 1024);
            case "GiB":
                return (long) (value * 1024 * 1024 * 1024);
            default:
                throw new IllegalArgumentException("Unknown size unit: " + unit);
        }
    }

    private static class NumericComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            long size1 = parseSize(o1);
            long size2 = parseSize(o2);
            return Float.compare(size1,size2);
        }
    }

    public FileSystemPanel(SystemInfo si){
        super();
        init(si);
    }

    private void init(SystemInfo si){
        oshi.software.os.FileSystem fs = si.getOperatingSystem().getFileSystem();
        List<OSFileStore> fileStores = fs.getFileStores();

        TableModel model;
        model = new DefaultTableModel(parseFileSystem(fileStores,si), COLUMNS) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 5) {
                    return JProgressBar.class;
                }
                return super.getColumnClass(columnIndex);
            }
        };

        JTable systemTable = new JTable(model);
        Font sansSerifFont = new Font("SansSerif", Font.PLAIN, 12);
        systemTable.setFont(sansSerifFont);
        systemTable.getTableHeader().setFont(new Font("Arial", Font.PLAIN, 14));

        resizeColumns(systemTable.getColumnModel());

        systemTable.getColumnModel().getColumn(5).setCellRenderer(new ProgressRenderer());

        // make sorter for Table
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(systemTable.getModel());
        sorter.setComparator(2, new NumericComparator());
        sorter.setComparator(3, new NumericComparator());
        sorter.setComparator(4, new NumericComparator());
        sorter.setSortable(5,false);
        systemTable.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(systemTable);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        GridBagConstraints scrollConstraints = new GridBagConstraints();
        scrollConstraints.gridx = 0;
        scrollConstraints.gridy = 1;
        scrollConstraints.weightx = 1d;
        scrollConstraints.weighty = 1d;
        scrollConstraints.anchor = GridBagConstraints.NORTHWEST;
        scrollConstraints.fill = GridBagConstraints.BOTH;

        add(scroll, scrollConstraints);



        Timer timer = new Timer(Config.REFRESH_FAST, e -> {
            DefaultTableModel tableModel = (DefaultTableModel) systemTable.getModel();
            Object[][] newData = parseFileSystem(si.getOperatingSystem().getFileSystem().getFileStores(), si);
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
            TableRowSorter<TableModel> re_sorter = (TableRowSorter<TableModel>) systemTable.getRowSorter();
            List<RowSorter.SortKey> sortKeys = (List<RowSorter.SortKey>) re_sorter.getSortKeys();
            re_sorter.setModel(tableModel);
            re_sorter.setComparator(2, new NumericComparator());
            re_sorter.setComparator(3, new NumericComparator());
            re_sorter.setComparator(4, new NumericComparator());
            re_sorter.setSortable(5,false);

            re_sorter.setSortKeys(sortKeys);
            re_sorter.sort();
        });

        timer.start();
    }

    private Object[][] parseFileSystem(List<OSFileStore> fileStores, SystemInfo si) {

        Object[][] systemArr = new Object[fileStores.size()][COLUMNS.length];

        int i = 0;

        // These are in descending CPU order
        for (OSFileStore fileStore : fileStores) {
            if (SystemInfo.getCurrentPlatform().equals(PlatformEnum.WINDOWS)){
                systemArr[i][0] = fileStore.getName();
            } else if(SystemInfo.getCurrentPlatform().equals(PlatformEnum.LINUX)){
                systemArr[i][0] = fileStore.getVolume();
            }
            systemArr[i][1] = fileStore.getType();
            systemArr[i][2] = FormatUtil.formatBytes(fileStore.getTotalSpace());
            systemArr[i][3] = FormatUtil.formatBytes(fileStore.getUsableSpace());
            systemArr[i][4] = FormatUtil.formatBytes(fileStore.getTotalSpace()- fileStore.getUsableSpace());
            int used = (int) ((fileStore.getTotalSpace() - fileStore.getUsableSpace()) *100 / fileStore.getTotalSpace());
            systemArr[i][5] = used;
            i++;
        }

        return systemArr;
    }

    private static void resizeColumns (TableColumnModel tableColumnModel){
        TableColumn column;
        int tW = tableColumnModel.getTotalColumnWidth();
        int cantCols = tableColumnModel.getColumnCount();
        for (int i = 0; i < cantCols; i++){
            column = tableColumnModel.getColumn(i);
            int pWidth = (int) Math.round(COLUMN_WIDTH_PERCENT[i] * tW);
            column.setPreferredWidth(pWidth);
        }
    }

    private static class ProgressRenderer extends DefaultTableCellRenderer {
        private final JProgressBar progressBar = new JProgressBar();

        public ProgressRenderer() {
            super();
            setOpaque(true);
            progressBar.setStringPainted(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {

            Integer progress = (Integer) value;
            progressBar.setValue(progress);
            return progressBar;
        }
    }
}
