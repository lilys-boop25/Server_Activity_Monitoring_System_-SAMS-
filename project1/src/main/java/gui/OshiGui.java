package gui;

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
    private JButton jMenu = this.getJMenu("Performance", "Performance",new PerformancePanel(this.si));
    private JButton selectedButton = jMenu;
    private static final PlatformEnum CURRENT_PLATFORM = PlatformEnum.getValue(Platform.getOSType());
    private final Color COLOR_DEFAULT = new Color(238,238,238);

    public static void main(String[] args){
        if (!CURRENT_PLATFORM.equals(PlatformEnum.WINDOWS) && !CURRENT_PLATFORM.equals(PlatformEnum.LINUX))
        {
            return;
        }
        OshiGui gui = new OshiGui();
        gui.init();
        SwingUtilities.invokeLater(gui::setVisible);
    }

    private void setVisible(){
        this.mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setLocation(0, 0);
        this.mainFrame.setVisible(true);
        this.jMenu.setBackground(new Color(179, 177, 178));
        this.jMenu.doClick();
    }

    private void init(){
        this.mainFrame = new JFrame("Operating System & Hardware Information");
        this.mainFrame.setSize(Config.GUI_WIDTH,Config.GUI_HEIGHT);
        this.mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.mainFrame.setResizable(true);

        this.mainFrame.setLocationByPlatform(true);
        this.mainFrame.setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        this.mainFrame.setJMenuBar(menuBar);

        menuBar.add(jMenu);
        menuBar.add(this.getJMenu("Processes","Processes", new ProcessPanel(this.si)));
        menuBar.add(this.getJMenu("Services", "Services", new ServicesPanel(this.si)));
        menuBar.add(this.getJMenu("Startup apps", "Startup apps", new StartupAppPanel()));
        menuBar.add(this.getJMenu("OS & HW Info", "Hardware & OS Summary", new OsHwPanel(this.si)));
    }

    public void setSelectedButton(JButton button) {
        if (selectedButton != button) {
            if (selectedButton != null) {
                selectedButton.setBackground(COLOR_DEFAULT); // đặt lại màu ban đầu cho JButton được chọn trước đó
            }
            button.setBackground(new Color(179, 177, 178));
            selectedButton = button; // lưu trạng thái của JButton mới được chọn
        }
    }

    private JButton getJMenu(String title, String toolTip, OshiJPanel panel){
        JButton button = new JButton(title);
        Font font = new Font ("Helvetica", Font.PLAIN, 14);
        button.setFont(font);
        button.setToolTipText(toolTip);
        button.setBackground(COLOR_DEFAULT);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setSelectedButton(button);
            }
        });

        button.addActionListener(e -> {
            Container contentPane = this.mainFrame.getContentPane();
            int nComponents = contentPane.getComponents().length;
            if (nComponents <= 0 || contentPane.getComponent(0) != panel) {
                this.resetMainGui();
                this.mainFrame.getContentPane().add(panel);
                this.refreshMainGui();
            }
        });

        return button;
    }

    public void resetMainGui(){
        this.mainFrame.getContentPane().removeAll();
    }

    private void refreshMainGui(){
        this.mainFrame.revalidate();
        this.mainFrame.repaint();
    }
}