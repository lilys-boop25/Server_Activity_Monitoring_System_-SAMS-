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

    private JButton performanceButton = getJMenu("Performance", "Performance", new PerformancePanel(si), "/icons/performance.png");
    private JButton processButton = getJMenu("Processes", "Processes", new ProcessPanel(si), "/icons/Process.png");
    private JButton startupButton = getJMenu("Startup Apps", "Startup Apps", new StartupAppPanel(), "/icons/startup.png");
    private JButton startupPanelButton = getJMenu("Startup (All)", "Startup Panel", new StartupPanelSecure(), "/icons/startup_all.png");
    private JButton servicesButton = getJMenu("Services", "Services", new ServicesPanel(si), "/icons/services.png");
    private JButton osHwButton = getJMenu("OS/HW Info", "OS/HW Info", new OsHwPanel(si), "/icons/info.png");
    private JButton fsButton = getJMenu("File System", "File System", new FileSystemPanel(si), "/icons/Folder.png");

    JButton selectedButton = performanceButton;
    private static final PlatformEnum CURRENT_PLATFORM = PlatformEnum.getValue(Platform.getOSType());
    private final Color COLOR_DEFAULT = new Color(238, 238, 238);

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
        this.mainFrame.setSize(1000, 700);
        this.mainFrame.setLocationRelativeTo(null);
        this.mainFrame.setLayout(new BorderLayout());

        java.net.URL logoURL = getClass().getResource("/icons/logo.png");
        if (logoURL != null) {
            ImageIcon logoIcon = new ImageIcon(logoURL);
            mainFrame.setIconImage(logoIcon.getImage());
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
        menuPanel.add(performanceButton);
        menuPanel.add(fsButton);
        menuPanel.add(startupButton);
        menuPanel.add(startupPanelButton);
        menuPanel.add(processButton);
        menuPanel.add(servicesButton);
        menuPanel.add(osHwButton);

        this.mainFrame.add(menuPanel, BorderLayout.WEST);
        this.mainFrame.add(new PerformancePanel(this.si), BorderLayout.CENTER);
    }
    private JButton getToggleMenuButton() {
        String iconPath = "/icons/menu.png"; // Giả sử ảnh nằm trong src/main/resources/icons/menu.png
        java.net.URL iconURL = getClass().getResource(iconPath);
        JButton button;
    
        if (iconURL != null) {
            ImageIcon originalIcon = new ImageIcon(iconURL);           
            button = new JButton(new ImageIcon(originalIcon.getImage()));
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
        button.setToolTipText("Expand/Collapse menu");
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

    private JButton getJMenu(String text, String panelName, JPanel panel, String iconPath) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
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
                Image scaledImage = originalIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);           
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
