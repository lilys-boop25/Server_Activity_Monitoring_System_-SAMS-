package gui;
// import gui.ProcessPanel;
// import gui.StartupAppPanel;
// import gui.StartupPanelSecure;
// import gui.PerformancePanel;
// import gui.ServicesPanel;
// import gui.OsHwPanel;
// import gui.CPUPanel;
// import gui.MemoryPanel;
// import gui.DiskPanel;
// import gui.NetworkPanel;
// import gui.FileSystemPanel;
// import gui.SignatureChecker;

import com.sun.jna.Platform;
import oshi.PlatformEnum;
import oshi.SystemInfo;

import javax.swing.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class OshiGui {

    private JPanel menuPanel;
    private boolean menuExpanded = true;

    private JFrame mainFrame;
    private SystemInfo si = new SystemInfo();

    private JButton performanceButton = getJMenu("Performance", "Performance", "Performance", wrapInRoundedPanel(new PerformancePanel(si)), "/icons/performance.png");
    private JButton processButton = getJMenu("Processes", "Processes", "Processes", wrapInRoundedPanel(new ProcessPanel(si)), "/icons/Process.png");
    private JButton startupButton = getJMenu("Startup Apps", "Startup Apps", "Startup Apps", wrapInRoundedPanel(new StartupAppPanel()), "/icons/startup.png");
    private JButton startupPanelButton = getJMenu("Startup (All)", "Startup (All)", "Startup Panel", wrapInRoundedPanel(new StartupPanelSecure()), "/icons/startup_all.png");
    private JButton servicesButton = getJMenu("Services", "Services", "Services", wrapInRoundedPanel(new ServicesPanel(si)), "/icons/services.png");
    private JButton osHwButton = getJMenu("OS/HW Info", "OS/HW Info", "OS/HW Info", wrapInRoundedPanel(new OsHwPanel(si)), "/icons/info.png");
    private JButton fsButton = getJMenu("File System", "File System", "File System", wrapInRoundedPanel(new FileSystemPanel(si)), "/icons/Folder.png");

    JButton selectedButton = performanceButton;
    private static final PlatformEnum CURRENT_PLATFORM = PlatformEnum.getValue(Platform.getOSType());
    private final Color COLOR_DEFAULT = new Color(245, 242, 239);

    public static void main(String[] args) {
        if (!CURRENT_PLATFORM.equals(PlatformEnum.WINDOWS) && !CURRENT_PLATFORM.equals(PlatformEnum.LINUX)) {
            return;
        }
        OshiGui gui = new OshiGui();
        gui.init();
        SwingUtilities.invokeLater(gui::setVisible);
    }

    private void setVisible() {
        this.mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.mainFrame.setVisible(true);
    }

    private void init() {
        // Thiết lập giao diện chính
        this.mainFrame = new JFrame("System Monitor");
        this.mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.mainFrame.setResizable(true);  // Cho phép thay đổi kích thước 
        this.mainFrame.setMinimumSize(new Dimension(800, 600)); // Kích thước tối thiểu
        this.mainFrame.setPreferredSize(new Dimension(800, 600)); // Kích thước ưu tiên
        this.mainFrame.setLocationByPlatform(true); // Đặt vị trí theo nền tảng
        this.mainFrame.setTitle("System Monitor - " + CURRENT_PLATFORM.getName());
        // Thiết lập kích thước và nền cho frame
        this.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Mở rộng
        this.mainFrame.setUndecorated(false); // Hiển thị thanh tiêu đề
        this.mainFrame.setResizable(true); // Cho phép thay đổi kích thước  
        this.mainFrame.setSize(800, 600);
        this.mainFrame.setBackground(COLOR_DEFAULT);
        this.mainFrame.setLocationRelativeTo(null);
        this.mainFrame.setLayout(new BorderLayout());

        java.net.URL logoURL = getClass().getResource("/icons/logo.png");
        if (logoURL != null) {
            ImageIcon logoIcon = new ImageIcon(logoURL);
            Image scaledLogo = logoIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            mainFrame.setIconImage(scaledLogo);
        } else {
            System.err.println("⚠ Không tìm thấy logo: /icons/logo.png");
        }

        menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(COLOR_DEFAULT); // Nền đồng nhất
        menuPanel.setPreferredSize(new Dimension(180, 700));

        // 🔽 Nút đầu tiên: Toggle menu
        menuPanel.add(getToggleMenuButton());

        // Add all menu buttons
        menuPanel.add(processButton);
        menuPanel.add(performanceButton);
        menuPanel.add(fsButton);
        menuPanel.add(startupButton);
        menuPanel.add(startupPanelButton);
        menuPanel.add(servicesButton);
        menuPanel.add(osHwButton);

        this.mainFrame.add(menuPanel, BorderLayout.WEST);
        this.mainFrame.add(wrapInRoundedPanel(new ProcessPanel(this.si)), BorderLayout.CENTER);
    }
    private JButton getToggleMenuButton() {
        String iconPath = "/icons/menu.png";
        java.net.URL iconURL = getClass().getResource(iconPath);
        JButton button;
    
        if (iconURL != null) {
            ImageIcon originalIcon = new ImageIcon(iconURL);       
            Image scaledImage = originalIcon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);      
            button = new JButton(new ImageIcon(scaledImage));
        } else {
            // fallback nếu không có icon
            System.err.println("⚠ Không tìm thấy icon: " + iconPath);    
            button = new JButton("≡");
            button.setFont(new Font("SansSerif", Font.BOLD, 20));
        }
        button.setFocusPainted(false);
        button.setBackground(COLOR_DEFAULT);
        button.setPreferredSize(new Dimension(180, 50));
        button.setMaximumSize(new Dimension(180, 50));
        button.setBorderPainted(false);
        button.setToolTipText("Open Navigation");
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleMenu();
            }
        });
        return button;

    }
    private void toggleMenu() {
        menuExpanded = !menuExpanded;
        int width = menuExpanded ? 180 : 50;
        // Update width
        menuPanel.setPreferredSize(new Dimension(width, 700));
        menuPanel.revalidate();
        menuPanel.repaint();
        // Ẩn/hiện text trên các nút
        updateMenuButtonText(menuExpanded);
    }
    private void updateMenuButtonText(boolean showText) {
        updateButton(performanceButton, "Performance", showText);
        updateButton(processButton, "Processes", showText);
        updateButton(startupButton, "Startup Apps", showText);
        updateButton(startupPanelButton, "Startup (All)", showText);
        updateButton(servicesButton, "Services", showText);
        updateButton(osHwButton, "OS/HW Info", showText);
        updateButton(fsButton, "File System", showText);
    }
    private void updateButton(JButton button, String text, boolean showText) {
        button.setText(showText ? text : "");
        button.setHorizontalAlignment(showText ? SwingConstants.LEFT : SwingConstants.CENTER);
    }

        private JPanel wrapInRoundedPanel(JPanel innerPanel) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        wrapper.add(innerPanel, BorderLayout.CENTER);
        return wrapper;
    }

    private JButton getJMenu(String text,String setToolTipText, String panelName, JPanel panel, String iconPath) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setToolTipText(setToolTipText);
        button.setBackground(COLOR_DEFAULT);
        button.setPreferredSize(new Dimension(180, 50));
        button.setMaximumSize(new Dimension(180, 50));
        button.setBorderPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(10); // Khoảng cách giữa icon và text
        button.setFont(new Font("SansSerif", Font.PLAIN, 14));
        if (iconPath != null) {
            java.net.URL iconURL = getClass().getResource(iconPath);       
            if (iconURL != null) {          
                ImageIcon originalIcon = new ImageIcon(iconURL);            
                Image scaledImage = originalIcon.getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH);           
                button.setIcon(new ImageIcon(scaledImage));       
            } else {           
                System.err.println("⚠️ Không tìm thấy icon: " + iconPath);   
            }
        }

    button.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            selectedButton.setBackground(COLOR_DEFAULT);
            selectedButton = button;
            selectedButton.setBackground(Color.LIGHT_GRAY);
            mainFrame.getContentPane().remove(1);
            mainFrame.add(panel, BorderLayout.CENTER);
            mainFrame.validate();
            mainFrame.repaint();
        }
    });
    
    return button;
}

}
