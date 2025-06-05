package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Parent class combining code common to the other panels.
 */
public class OshiJPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    protected JLabel msgLabel = new JLabel();
    protected JPanel msgPanel = new JPanel();

    // Biến tĩnh để chọn chế độ giao diện
    public static boolean isDarkMode = true;

    // Màu sắc mặc định cho hai chế độ
    public static final Color DARK_BG = new Color(30, 30, 30);    // #1E1E1E
    public static final Color DARK_PANEL_BG = new Color(45, 45, 45); // #2D2D2D
    public static final Color DARK_BORDER = new Color(60, 60, 60); // #3C3C3C
    public static final Color LIGHT_BG = new Color(240, 240, 240); // #F0F0F0
    public static final Color LIGHT_PANEL_BG = new Color(255, 255, 255); // #FFFFFF
    public static final Color LIGHT_BORDER = new Color(200, 200, 200); // #C8C8C8
    public static final Color TEXT_COLOR = Color.WHITE; // Chữ trắng trong chế độ tối, sẽ thay đổi trong chế độ sáng

    public OshiJPanel() {
        super();
        // Đặt kích thước tối đa nếu có
        Dimension maxSize = getMaximumSize();
        if (maxSize != null) {
            setSize(maxSize);
        }
        setLayout(new GridBagLayout());
        
        // Áp dụng giao diện dựa trên chế độ
        updateTheme();
    }

    // Phương thức để cập nhật giao diện theo chế độ
    public void updateTheme() {
        Color bgColor = isDarkMode ? DARK_BG : LIGHT_BG;
        Color panelBgColor = isDarkMode ? DARK_PANEL_BG : LIGHT_PANEL_BG;
        Color borderColor = isDarkMode ? DARK_BORDER : LIGHT_BORDER;
        Color textColor = isDarkMode ? Color.WHITE : Color.BLACK;

        setBackground(bgColor);
        msgLabel.setForeground(textColor);
        msgPanel.setBackground(panelBgColor);
        msgPanel.setBorder(BorderFactory.createLineBorder(borderColor));
    }

    // Phương thức để chuyển đổi chế độ giao diện
    public static void toggleTheme() {
        isDarkMode = !isDarkMode;
    }

    public static final class JGradientButton extends JButton {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Color customColor = null; // Thêm trường màu tùy chỉnh
		public JGradientButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setForeground(isDarkMode ? Color.WHITE : Color.BLACK); // Chữ trắng trong chế độ tối, đen trong chế độ sáng
            setFont(new Font("Arial", Font.PLAIN, 14));
            setBorderPainted(false);
            setFocusPainted(false);
            setHorizontalAlignment(SwingConstants.LEFT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Color bgColor = isDarkMode ? new Color(40, 40, 40) : new Color(220, 220, 220); // #282828 hoặc #DCDCDC
            Color hoverColor = isDarkMode ? new Color(50, 50, 50) : new Color(200, 200, 200); // #323232 hoặc #C8C8C8
            Color pressedColor = isDarkMode ? new Color(60, 60, 60) : new Color(180, 180, 180); // #3C3C3C hoặc #B4B4B4

            if (getModel().isPressed() || getModel().isSelected()) {
                g.setColor(pressedColor);
            } else if (getModel().isRollover()) {
                g.setColor(hoverColor);
            } else {
                g.setColor(bgColor);
            }
            g.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(g);
        }
		public void setCustomColor(Color customColor) {
			this.customColor = customColor;
		}
    }
}