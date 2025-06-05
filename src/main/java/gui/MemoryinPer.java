package gui;

import javax.swing.*;
import java.awt.*;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

public class MemoryinPer extends OshiJPanel {
    private static final long serialVersionUID = 1L;
    private SystemInfo si;
    private GlobalMemory memory;
    private JPanel displayPanel;
    private JLabel valueLabel; // Khai báo valueLabel

    public MemoryinPer(SystemInfo si) {
        this.si = si;
        this.memory = si.getHardware().getMemory();
        this.displayPanel = new JPanel(new GridBagLayout());
        this.valueLabel = new JLabel(); // Khởi tạo valueLabel ngay trong constructor
        valueLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK); // Đặt màu ban đầu
        setLayout(new BorderLayout());
        initial();
    }

    private void initial() {
        displayPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        add(displayPanel, BorderLayout.CENTER);

        // Thêm valueLabel vào displayPanel (giả định)
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        displayPanel.add(valueLabel, constraints);

        // Cập nhật thông tin ban đầu (giả định)
        updateMemoryInfo(memory, null); // Thay null bằng button nếu cần
    }

    // Phương thức tĩnh để cập nhật thông tin (giả định)
    public static void updateMemoryInfo(GlobalMemory memory, JGradientButton button) {
        // Logic cập nhật thông tin memory, ví dụ:
        long total = memory.getTotal() / (1024 * 1024 * 1024); // GB
        long available = memory.getAvailable() / (1024 * 1024 * 1024); // GB
        double usagePercent = ((double) (total - available) / total) * 100;
        if (button != null) {
            button.setText(PerformancePanel.buttonTextLines("\nMemory\n" + available + "/" + total + " GB (" + String.format("%.0f%%", usagePercent) + ")\n"));
            button.setCustomColor(PerformancePanel.getColorByPercent(usagePercent));
        }
    }

    @Override
    public void updateTheme() {
        super.updateTheme();
        if (displayPanel != null) {
            displayPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        }
        if (valueLabel != null) { // Kiểm tra null trước khi gọi
            valueLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        }
        revalidate();
        repaint();
    }
}