package component;

import oshi.SystemInfo;
import oshi.software.os.OSService;
import oshi.software.os.OperatingSystem;

import java.util.Comparator;
import java.util.List;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;


public class ServicesPanel extends Panel {
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
        OperatingSystem os = si.getOperatingSystem();
        JLabel serLabel = new JLabel(SERVICES);
        Font arialFont = new Font("Arial", Font.BOLD, 18);
        serLabel.setFont(arialFont);
        add(serLabel, BorderLayout.NORTH);

        TableModel model = new DefaultTableModel(parseServices(os.getServices(), si), COLUMNS);
        JTable serTable = new JTable(model);

        // make sorter for Table
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(serTable.getModel());
        sorter.setComparator(1, new NumericComparator());
        serTable.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(serTable);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scroll, BorderLayout.CENTER);


        Timer timer = new Timer(Config.REFRESH_SLOW, e -> {
            DefaultTableModel tableModel = (DefaultTableModel) serTable.getModel();
            Object[][] newData = parseServices(os.getServices(), si);
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

    private Object[][] parseServices(List<OSService> osServiceList, SystemInfo si){
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
