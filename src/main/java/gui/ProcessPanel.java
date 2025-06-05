package gui;

import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.util.FormatUtil;
import java.io.BufferedReader;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ProcessPanel extends OshiJPanel {
    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"Name", "CPU", "Memory", "Disk", "Network"};
    private static final double[] COLUMN_WIDTH_PERCENT = {0.35, 0.15, 0.20, 0.15, 0.15};
    private JTable processTable;

    public ProcessPanel(SystemInfo si) {
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 30));

        // Sidebar
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(40, 40, 40));
        sidebar.setPreferredSize(new Dimension(150, 0));

        JLabel processesLabel = new JLabel("Processes");
        processesLabel.setFont(new Font("Arial", Font.BOLD, 18));
        processesLabel.setForeground(Color.WHITE);
        sidebar.add(processesLabel);

        add(sidebar, BorderLayout.WEST);

        // Main panel with table
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(30, 30, 30));

        // Table model and data
        DefaultTableModel model = null;
		try {
			model = new DefaultTableModel(parseProcesses(si.getOperatingSystem().getProcesses()), COLUMNS) {
			    /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				@Override
			    public boolean isCellEditable(int row, int column) {
			        return false;
			    }
			};
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        processTable = new JTable(model);
        processTable.setBackground(new Color(45, 45, 45));
        processTable.setForeground(Color.WHITE);
        processTable.getTableHeader().setBackground(new Color(60, 60, 60));
        processTable.getTableHeader().setForeground(Color.WHITE);
        processTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        processTable.getTableHeader().setFont(new Font("Arial", Font.PLAIN, 14));

        // Customize renderer for status icons
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                          boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(new Color(45, 45, 45));
                c.setForeground(Color.WHITE);
                if (column == 0) {
                    JLabel label = new JLabel(value.toString());
                    label.setIcon(new ImageIcon(new ImageIcon("path/to/green_leaf.png").getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH)));
                    return label;
                }
                return c;
            }
        };
        processTable.getColumnModel().getColumn(0).setCellRenderer(renderer);

        // Resize columns
        resizeColumns(processTable.getColumnModel());

        JScrollPane scrollPane = new JScrollPane(processTable);
        scrollPane.setBackground(new Color(30, 30, 30));
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        // Timer for updates
        new Timer(1000, e -> {
            DefaultTableModel tableModel = (DefaultTableModel) processTable.getModel();
            Object[][] newData = null;
			try {
				newData = parseProcesses(si.getOperatingSystem().getProcesses());
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
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
        }).start();
    }

    private Object[][] parseProcesses(List<OSProcess> list) throws Exception {
        long totalMem = new SystemInfo().getHardware().getMemory().getTotal();
        List<Object[]> procArr = new ArrayList<>();
        for (OSProcess p : list) {
            if (p.getProcessID() == 0 && SystemInfo.getCurrentPlatform().equals(oshi.PlatformEnum.WINDOWS)) {
                continue;
            }
            Object[] row = new Object[COLUMNS.length];
            row[0] = p.getName();
            row[1] = String.format("%.1f%%", 100d * p.getProcessCpuLoadBetweenTicks(null));
            row[2] = FormatUtil.formatBytes(p.getResidentSetSize()) + " (" + String.format("%.1f%%", 100d * p.getResidentSetSize() / totalMem) + ")";
            
            // Lấy disk I/O từ PowerShell
            String diskIoCommand = String.format("powershell \"(Get-Counter -Counter '\\Process(%s)\\IO Data Bytes/sec' -ErrorAction SilentlyContinue).CounterSamples[0].CookedValue\"", p.getName().replace("'", "''"));
            String diskIoResult = executeCommand(diskIoCommand);
            double diskBytesPerSec;
            try {
                diskBytesPerSec = Double.parseDouble(diskIoResult.trim());
                row[3] = String.format("%.1f MB/s", diskBytesPerSec / 1024.0 / 1024.0);
            } catch (Exception e) {
                row[3] = "N/A"; // Nếu không lấy được disk I/O
            }
            
            row[4] = String.format("%.1f Mbps", p.getSoftOpenFileLimit() / 125000.0); // Convert bytes to Mbps
            procArr.add(row);
        }
        return procArr.toArray(new Object[0][0]);
    }

    // Hàm thực thi lệnh (tương tự như trong câu hỏi trước)
    private String executeCommand(String command) throws Exception {
        @SuppressWarnings("deprecation")
		Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        process.waitFor();
        return output.toString();
    }

    private void resizeColumns(TableColumnModel tableColumnModel) {
        int tW = tableColumnModel.getTotalColumnWidth();
        for (int i = 0; i < COLUMNS.length; i++) {
            tableColumnModel.getColumn(i).setPreferredWidth((int) Math.round(COLUMN_WIDTH_PERCENT[i] * tW));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("SAMS");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new ProcessPanel(new SystemInfo()));
            frame.setSize(800, 600);
            frame.setVisible(true);
        });
    }
}