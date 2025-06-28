package gui;

import oshi.SystemInfo;
import oshi.software.os.OSService;
import oshi.software.os.OperatingSystem;

import java.util.Comparator;
import java.util.List;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;


public class ServicesPanel extends OshiJPanel {
    private static final long serialVersionUID = 1L;
    private static final String SERVICES = "Services";
    private static final String[] COLUMNS = {"Name", "PID", "Status"};


    public ServicesPanel(SystemInfo si){
        super();
        init(si);
    }

    private static class NumericComparator implements Comparator<Object> {

        @Override
        public int compare(Object o1, Object o2) {
            Integer i1 = Integer.parseInt(o1.toString());
            Integer i2 = Integer.parseInt(o2.toString());
            return i1.compareTo(i2);
        }
    }

    private void init(SystemInfo si){
        JLabel serLabel = new JLabel(SERVICES);
        serLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
        serLabel.setOpaque(true);
        serLabel.setBackground(Color.WHITE);
        serLabel.setForeground(Color.BLACK);
        serLabel.setBorder(BorderFactory.createEmptyBorder(15, 2, 10, 0));

        // Create header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.add(serLabel, BorderLayout.WEST);
        
        // Add separator line
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(new Color(230, 230, 230));
        separator.setBackground(new Color(230, 230, 230));
        headerPanel.add(separator, BorderLayout.SOUTH);

        GridBagConstraints titleConstraints = new GridBagConstraints();
        titleConstraints.gridx = 0;
        titleConstraints.gridy = 0;
        titleConstraints.fill = GridBagConstraints.HORIZONTAL;
        titleConstraints.weightx = 1;
        add(serLabel, titleConstraints);


        OperatingSystem os = si.getOperatingSystem();

        TableModel model = new DefaultTableModel(parseServices(os.getServices()), COLUMNS);
        JTable serTable = new JTable(model);
        Font seFont = new Font("SansSerif", Font.PLAIN, 12);
        serTable.setBorder(BorderFactory.createEmptyBorder());
        serTable.setBackground(Color.WHITE);
        serTable.setOpaque(true);
        serTable.setFont(seFont);
        serTable.getTableHeader().setFont(new Font("SansSerif", Font.PLAIN, 14));
        serTable.setGridColor(new Color(230, 230, 230));
        serTable.setShowHorizontalLines(false);
        serTable.setShowVerticalLines(true);
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
        serTable.getColumnModel().getColumn(2).setCellRenderer(renderer);

        // make sorter for Table
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(serTable.getModel());
        sorter.setComparator(1, new NumericComparator());
        serTable.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(serTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(Color.WHITE);
        scroll.setOpaque(true);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getViewport().setOpaque(true);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        GridBagConstraints servicesConstraints = new GridBagConstraints();
        servicesConstraints.gridx = 0;
        servicesConstraints.gridy = 1;
        servicesConstraints.weightx = 1d;
        servicesConstraints.weighty = 1d;
        servicesConstraints.anchor = GridBagConstraints.NORTHWEST;
        
        scroll.getViewport().setBackground(Color.WHITE);
        servicesConstraints.fill = GridBagConstraints.BOTH;
        add(scroll, servicesConstraints);


        Timer timer = new Timer(Config.REFRESH_SLOW, e -> {
            DefaultTableModel tableModel = (DefaultTableModel) serTable.getModel();
            Object[][] newData = parseServices(os.getServices());
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

    private Object[][] parseServices(List<OSService> osServiceList){
        int i = osServiceList.size();
        Object[][] servicesArr = new Object[i][COLUMNS.length];

        for (OSService osService : osServiceList){
            i--;
            int pid = osService.getProcessID();
            servicesArr[i][0] = osService.getName();
            if (pid != 0){
                servicesArr[i][1] = pid;
            }

            servicesArr[i][2] = osService.getState().toString();
        }

        return servicesArr;
    }
}
