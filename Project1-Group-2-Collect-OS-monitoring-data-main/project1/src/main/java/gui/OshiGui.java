package gui;
import gui.ProcessPanel;

import com.sun.jna.Platform;
import oshi.PlatformEnum;
import oshi.SystemInfo;

import javax.swing.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class OshiGui {

    private JFrame mainFrame;
    private SystemInfo si = new SystemInfo();
    private JButton jMenu = this.getJMenu("Performance", "Performance", new PerformancePanel(this.si));
    private JButton processButton = this.getJMenu("Processes", "Processes", new ProcessPanel(this.si));
    private JButton startupButton = this.getJMenu("Startup Apps", "Startup Apps", new StartupAppPanel());
    private JButton startupPanelButton = this.getJMenu("Startup (All)", "Startup Panel", new StartupPanelSecure());

    JButton selectedButton = jMenu;
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
        this.mainFrame = new JFrame("System Monitor");
        this.mainFrame.setSize(900, 600);
        this.mainFrame.setLocationRelativeTo(null);
        this.mainFrame.setLayout(new BorderLayout());

        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setPreferredSize(new Dimension(150, 600));
        menuPanel.add(jMenu);
        menuPanel.add(startupButton);
        menuPanel.add(startupPanelButton);
        menuPanel.add(processButton);
        

        this.mainFrame.add(menuPanel, BorderLayout.WEST);
        this.mainFrame.add(new PerformancePanel(this.si), BorderLayout.CENTER);
    }

    private JButton getJMenu(String text, String panelName, JPanel panel) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(COLOR_DEFAULT);
        button.setPreferredSize(new Dimension(150, 50));
        button.setMaximumSize(new Dimension(150, 50));
        button.setHorizontalAlignment(SwingConstants.LEFT);
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
