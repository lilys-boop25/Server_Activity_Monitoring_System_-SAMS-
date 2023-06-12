package GUI;

import com.sun.jna.Platform;
import oshi.PlatformEnum;
import oshi.SystemInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Gui {
    JButton selectedButton;
    private JFrame mainFrame;
    private JButton jMenu;
    private SystemInfo si = new SystemInfo();
    private static final PlatformEnum CURRENT_PLATFORM = PlatformEnum.getValue(Platform.getOSType());

    private final Color COLOR_DEFAULT = new Color(238,238,238);

    public Gui(){

    }

    public static void main(String[] args){
        if (!CURRENT_PLATFORM.equals(PlatformEnum.WINDOWS) && !CURRENT_PLATFORM.equals(PlatformEnum.LINUX))
        {
            return;
        }
        Gui gui = new Gui();
        gui.init();
        SwingUtilities.invokeLater(gui::setVisible);
    }

    private void setVisible(){
        this.mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.mainFrame.setVisible(true);
        this.jMenu.doClick();
    }

    private void init(){
        this.mainFrame = new JFrame("Operating System & Hardware Information");
        this.mainFrame.setSize(Config.GUI_WIDTH,Config.GUI_HEIGHT);
        this.mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.mainFrame.setResizable(false);
        this.mainFrame.setLocationByPlatform(true);
        this.mainFrame.setLayout(new BorderLayout());
        JPanel panel = new JPanel(new GridBagLayout());
        JLabel label = new JLabel("This is our project");
        label.setFont(new Font("Arial", Font.PLAIN, 24));

        // Thiết lập các ràng buộc và vị trí cho đoạn văn bản
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(label, gbc);

        this.mainFrame.add(panel);
        JMenuBar menuBar = new JMenuBar();
        this.mainFrame.setJMenuBar(menuBar);

        menuBar.add(this.getjMenu("Performance", "Performance",new PerformancePanel(this.si)));
        menuBar.add(this.getjMenu("File Systems","File Systems", new FileSystemPanel(this.si)));
        menuBar.add(this.getjMenu("Processes","Processes", new ProcessPanel(this.si)));
        menuBar.add(this.getjMenu("Services", "Services", new ServicesPanel(this.si)));
        menuBar.add(this.getjMenu("OS & HW Info", "Hardware & OS Summary", new OsHwPanel(this.si)));

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

    private JButton getjMenu(String title, String toolTip, OshiJPanel panel){
        JButton button = new JButton(title);
        Font font = new Font ("Helvetica", Font.PLAIN, 14);
        button.setFont(font);
        button.setToolTipText(toolTip);
        button.setBackground(COLOR_DEFAULT);

        button.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                setSelectedButton(button);
            }
        });

        button.addActionListener((e) -> {
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
