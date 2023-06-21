package GUI;

import static javax.swing.JFrame.EXIT_ON_CLOSE;

import java.awt.BorderLayout;
//import java.awt.Component;
import java.awt.Container;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.sun.jna.Platform;

import oshi.PlatformEnum;
import oshi.SystemInfo;

/**
 * Basic Swing class to demonstrate potential uses for OSHI in a monitoring GUI. Not ready for production use and
 * intended as inspiration/examples.
 */
public class OshiGui {

    private JFrame mainFrame;

    private SystemInfo si = new SystemInfo();
    private JButton jMenu = this.getJMenu("Performance", "Performance",new PerformancePanel(this.si));
    private JButton selectedButton = jMenu;
    private static final PlatformEnum CURRENT_PLATFORM = PlatformEnum.getValue(Platform.getOSType());

    public static void main(String[] args) {
        if (!CURRENT_PLATFORM.equals(PlatformEnum.WINDOWS) && !CURRENT_PLATFORM.equals(PlatformEnum.LINUX))
        {
            return;
        }
        OshiGui gui = new OshiGui();
        gui.init();
        SwingUtilities.invokeLater(gui::setVisible);
    }

    private void setVisible() {
        mainFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        mainFrame.setLocation(0, 0);
        mainFrame.setVisible(true);
        jMenu.doClick();
    }

    private void init(){
        this.mainFrame = new JFrame("Operating System & Hardware Information");
        this.mainFrame.setSize(Config.GUI_WIDTH,Config.GUI_HEIGHT);
        this.mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.mainFrame.setResizable(false);
        this.mainFrame.setLocationByPlatform(true);
        this.mainFrame.setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        mainFrame.setJMenuBar(menuBar);
        // Assign the first menu option to be clicked on visibility
        jMenu = getJMenu("Performance", 'M', "", new PerformancePanel(si));
        menuBar.add(jMenu);
        menuBar.add(this.getJMenu("File Systems", "File Systems", new FileSystemPanel(this.si)));
        menuBar.add(this.getJMenu("Processes","Processes", new ProcessPanel(this.si)));
        menuBar.add(this.getJMenu("Services", "Services", new ServicesPanel(this.si)));
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

        // Set a shortcut keyboard for this button, press Alt + mnemonic
        button.setMnemonic(mnemonic);
        // Set text to display when we move out mouse to the button
        button.setToolTipText(toolTip);

        // Set what to do when we push the button
        button.addActionListener(e -> {
            Container contentPane = this.mainFrame.getContentPane();
            // Check if the given panel is the first component in the contentPane and if it equal to our callback panel.
            int nComponents = (int)contentPane.getComponents().length;
            if (nComponents <= (int)0 || contentPane.getComponent(0) != panel) {
                resetMainGui();
                this.mainFrame.getContentPane().add(panel);
                refreshMainGui();
            }
        });

        return button;
    }

    private void resetMainGui() {
        this.mainFrame.getContentPane().removeAll();
    }

    private void refreshMainGui() {
        this.mainFrame.revalidate();
        this.mainFrame.repaint();
    }
}