package gui;

import com.sun.jna.Platform;
import oshi.PlatformEnum;
import oshi.SystemInfo;

import javax.swing.*;
import java.awt.*;

public class OshiGui {

    private JFrame mainFrame;
    private SystemInfo si = new SystemInfo();
    private static final PlatformEnum CURRENT_PLATFORM = PlatformEnum.getValue(Platform.getOSType());

    public static void main(String[] args) {
        System.out.println("Starting OshiGui...");
        System.out.println("Current platform: " + CURRENT_PLATFORM);

        if (!CURRENT_PLATFORM.equals(PlatformEnum.WINDOWS) && !CURRENT_PLATFORM.equals(PlatformEnum.LINUX)) {
            System.out.println("Platform not supported. Exiting...");
            return;
        }

        OshiGui gui = new OshiGui();
        gui.init();
        SwingUtilities.invokeLater(() -> {
            System.out.println("Setting visible...");
            gui.setVisible();
        });
    }

    private void setVisible() {
        System.out.println("Calling setVisible on mainFrame...");
        this.mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.mainFrame.setVisible(true);
    }

    private void init() {
        System.out.println("Initializing mainFrame...");
        this.mainFrame = new JFrame("Server Activity Monitoring System");
        this.mainFrame.setSize(1000, 600);
        this.mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.mainFrame.setResizable(true);
        this.mainFrame.setLocationByPlatform(true);
        this.mainFrame.setLayout(new BorderLayout());

        // Tạo JTabbedPane để chứa các tab
        System.out.println("Creating JTabbedPane...");
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(30, 30, 30));
        tabbedPane.setForeground(Color.WHITE);
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        // Thêm các tab với các panel tương ứng - Kiểm tra từng panel
        try {
            System.out.println("Adding Processes tab...");
            tabbedPane.addTab("Processes", new ProcessPanel(this.si));
        } catch (Exception e) {
            System.err.println("Error adding Process tab: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            System.out.println("Adding Performance tab...");
            tabbedPane.addTab("Performance", new PerformancePanel(this.si));
        } catch (Exception e) {
            System.err.println("Error adding Performance tab: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            System.out.println("Adding Services tab...");
            tabbedPane.addTab("Services", new ServicesPanel(this.si));
        } catch (Exception e) {
            System.err.println("Error adding Services tab: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            System.out.println("Adding OS & HW Info tab...");
            tabbedPane.addTab("OS & HW Info", new OsHwPanel(this.si));
        } catch (Exception e) {
            System.err.println("Error adding OS & HW Info tab: " + e.getMessage());
            e.printStackTrace();
        }

        // Thêm JTabbedPane vào JFrame
        System.out.println("Adding JTabbedPane to mainFrame...");
        this.mainFrame.add(tabbedPane, BorderLayout.CENTER);

        // Tùy chỉnh giao diện để giống hệ thống
        try {
            System.out.println("Setting look and feel...");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.err.println("Error setting look and feel: " + ex);
        }
    }
}