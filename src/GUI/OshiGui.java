package GUI;

import static javax.swing.JFrame.EXIT_ON_CLOSE;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

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
    private JButton jMenu;

    private SystemInfo si = new SystemInfo();

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

    private void init() {
        // Create the external frame
        mainFrame = new JFrame(Config.GUI_TITLE);
        mainFrame.setSize(Config.GUI_WIDTH, Config.GUI_HEIGHT);
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        //mainFrame.setResizable(true);
        mainFrame.setResizable(false);
        mainFrame.setLocationByPlatform(true);
        mainFrame.setLayout(new BorderLayout());
        // Add a menu bar
        JMenuBar menuBar = new JMenuBar();
        mainFrame.setJMenuBar(menuBar);
        // Assign the first menu option to be clicked on visibility
        jMenu = getJMenu("Performance", 'M', "", new PerformancePanel(si));
        menuBar.add(jMenu);
        menuBar.add(getJMenu("File Systems", 'F',"File Systems", new FileSystemPanel(si)));
        menuBar.add(getJMenu("Processes", 'P',"Processes", new ProcessPanel(si)));
        menuBar.add(getJMenu("Services", 'S', "Services", new ServicesPanel(si)));
        menuBar.add(getJMenu("OS & HW Info", 'O', "Hardware & OS Summary", new OsHwPanel(si)));


        // Add later menu items
    }

    private JButton getJMenu(String title, char mnemonic, String toolTip, OshiJPanel panel) {
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
