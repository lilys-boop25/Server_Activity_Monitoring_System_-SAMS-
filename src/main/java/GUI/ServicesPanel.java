package GUI;

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
        JPanel servicesPanel = new JPanel();
        servicesPanel.setLayout(new GridBagLayout());
        GridBagConstraints titleConstraints = new GridBagConstraints();
        titleConstraints.anchor = GridBagConstraints.CENTER;

        titleConstraints.insets = new Insets(10, 10, 10, 10);
        JLabel serLabel = new JLabel(SERVICES);
        Font arialFont = new Font("Arial", Font.BOLD, 18);
        serLabel.setFont(arialFont);
        add(serLabel, titleConstraints);

        OperatingSystem os = si.getOperatingSystem();

        TableModel model = new DefaultTableModel(parseServices(os.getServices(), si), COLUMNS);
        JTable serTable = new JTable(model);
        Font sansSerifFont = new Font("SansSerif", Font.PLAIN, 12);
        serTable.setFont(sansSerifFont);
        serTable.getTableHeader().setFont(new Font("Arial", Font.PLAIN, 14));

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
        serTable.getColumnModel().getColumn(2).setCellRenderer(renderer);
        // make sorter for Table
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(serTable.getModel());
        sorter.setComparator(1, new NumericComparator());
        serTable.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(serTable);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        servicesPanel.setLayout(new GridBagLayout());
        GridBagConstraints servicesConstraints = new GridBagConstraints();
        servicesConstraints.gridx = 0;
        servicesConstraints.gridy = 1;
        servicesConstraints.weightx = 1d;
        servicesConstraints.weighty = 1d;
        servicesConstraints.fill = GridBagConstraints.BOTH;
        servicesConstraints.insets = new Insets(10, 10, 10, 10);
        add(scroll, servicesConstraints);


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
