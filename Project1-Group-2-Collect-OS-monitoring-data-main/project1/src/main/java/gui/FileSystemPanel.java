package gui;


import oshi.PlatformEnum;
import oshi.SystemInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.FormatUtil;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;

public class FileSystemPanel extends OshiJPanel{
    private static final String[] COLUMNS = {"Device", "Type", "Total", "Available", "Used", "Diagram"};
    private static final double[] COLUMN_WIDTH_PERCENT = {0.1, 0.01, 0.01, 0.01, 0.01, 1.2};
    private static final String FILE_SYSTEM_LABEL = "File System";
    private static class SizeComparator implements Comparator<String> {
        private static long parseSize(String sizeString) {
            String[] parts = sizeString.split(" ");
            float value = Float.parseFloat(parts[0]);
            String unit = parts[1];
            switch (unit) {
                case "bytes":
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
        FileSystem fs = si.getOperatingSystem().getFileSystem();
        JLabel fileSystemLabel = new JLabel(FILE_SYSTEM_LABEL);
        fileSystemLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        fileSystemLabel.setOpaque(true);
        fileSystemLabel.setBackground(new Color(100, 149, 237)); // CornflowerBlue
        fileSystemLabel.setForeground(Color.WHITE);
        fileSystemLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 0;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.weightx = 1;
        add(fileSystemLabel, labelConstraints);
        
        List<OSFileStore> fileStores = fs.getFileStores();

        TableModel model;
        model = new DefaultTableModel(parseFileSystem(fileStores), COLUMNS) {
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
        sorter.setComparator(2, new SizeComparator());
        sorter.setComparator(3, new SizeComparator());
        sorter.setComparator(4, new SizeComparator());
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
            Object[][] newData = parseFileSystem(si.getOperatingSystem().getFileSystem().getFileStores());
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
            sorter.setComparator(2, new SizeComparator());
            sorter.setComparator(3, new SizeComparator());
            sorter.setComparator(4, new SizeComparator());
            sorter.setSortable(5,false);

            sorter.setSortKeys(sortKeys);
            sorter.sort();
        });

        timer.start();
    }

    private Object[][] parseFileSystem(List<OSFileStore> fileStores) {

        Object[][] systemArr = new Object[fileStores.size()][COLUMNS.length];

        int i = 0;

        // These are in descending CPU order
        for (OSFileStore fileStore : fileStores) {
            int used;
            if (SystemInfo.getCurrentPlatform().equals(PlatformEnum.WINDOWS)){
                systemArr[i][0] = fileStore.getName();systemArr[i][2] = FormatUtil.formatBytes(fileStore.getTotalSpace());

            } else if(SystemInfo.getCurrentPlatform().equals(PlatformEnum.LINUX)){
                systemArr[i][0] = fileStore.getVolume();
            }
            systemArr[i][1] = fileStore.getType();
            systemArr[i][2] = FormatUtil.formatBytes(fileStore.getTotalSpace());
            systemArr[i][3] = FormatUtil.formatBytes(fileStore.getUsableSpace());
            systemArr[i][4] = FormatUtil.formatBytes(fileStore.getTotalSpace()- fileStore.getFreeSpace());
            used = (int) ((fileStore.getTotalSpace() - fileStore.getFreeSpace()) *100 / fileStore.getTotalSpace());
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

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {

            Integer progress = (Integer) value;
            progressBar.setValue(progress);
            return progressBar;
        }
    }
}
