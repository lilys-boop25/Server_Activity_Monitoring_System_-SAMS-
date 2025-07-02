package gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.List;

public class StartupPanelSecure extends JPanel {

    private final DefaultTableModel tableModel;

    public StartupPanelSecure() {
        super();
        this.setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Details Startup", JLabel.LEFT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLabel.setOpaque(true);
        titleLabel.setBackground(Color.WHITE);
        titleLabel.setForeground(Color.BLACK);
        titleLabel.setBorder(new EmptyBorder(15, 2, 10, 0)); 

        String[] columns = {"Source", "App Name", "Executable", "Signature", "Trust", "Detail"};
        tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.setFont(new Font("Consolas", Font.PLAIN, 13));
        table.setGridColor(new Color(230, 230, 230));
        table.getTableHeader().setFont(new Font("SansSerif", Font.PLAIN, 14));

        // Highlight trust level
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, isSelected, hasFocus, row, col);
                String trust = (String) tbl.getValueAt(row, 4);
                if (!isSelected) {
                    if ("Danger".equals(trust)) c.setBackground(new Color(255, 200, 200));
                    else if ("Suspicious".equals(trust)) c.setBackground(new Color(255, 255, 180));
                    else if ("Trusted".equals(trust)) c.setBackground(new Color(200, 255, 200));
                    else c.setBackground(Color.WHITE);
                }
                return c;
            }
        });

        JButton refreshBtn = new JButton("Refresh Startup Entries");
        refreshBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        refreshBtn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        refreshBtn.setBackground(Color.WHITE);
        refreshBtn.addActionListener(e -> {
            tableModel.setRowCount(0);
            List<String> entries = StartupScanner.collectStartupEntries();
            if (entries == null || entries.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No startup entries found or an error occurred.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            for (String line : entries) {
                if (line.startsWith(">>") || line.trim().isEmpty()) continue;

                String source = detectSource(line);
                String exePath = extractExePath(line);
                String appName = extractAppName(exePath, line);
                String signature = exePath.isEmpty() ? "N/A" : SignatureChecker.getSignatureStatus(exePath);
                String trust = classifyTrust(exePath, signature, line);

                tableModel.addRow(new Object[]{source, appName, exePath, signature, trust, line});
            }
        });

        java.net.URL logoURL = getClass().getResource("/icons/Refresh.png");
        if (logoURL != null) {
            ImageIcon logoIcon = new ImageIcon(logoURL);
            Image scaledLogo = logoIcon.getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH);
            refreshBtn.setIcon(new ImageIcon(scaledLogo)); // ✅ Gán icon, KHÔNG ghi đè nút
        }


        // Create header panel
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(Color.WHITE);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(refreshBtn, BorderLayout.EAST);
        headerPanel.add(topPanel);
        
        // Add separator line
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(new Color(230, 230, 230));
        separator.setBackground(new Color(230, 230, 230));
        headerPanel.add(separator, BorderLayout.SOUTH);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private String detectSource(String line) {
        String lc = line.toLowerCase();
        if (lc.contains("run") && lc.contains("microsoft\\windows\\currentversion\\run")) return "Registry Run";
        if (lc.contains("schtasks") || lc.contains("task") || lc.contains("scheduled")) return "Scheduled Task";
        if (lc.contains("service") || lc.startsWith("running") || lc.startsWith("stopped")) return "Service";
        if (lc.contains("appinit") || lc.contains("winlogon") || lc.contains("active setup")) return "Advanced Registry";
        if (lc.contains("eventconsumer") || lc.contains("wmi")) return "WMI Event";
        if (lc.contains("startup file") || lc.contains(".lnk")) return "Startup Folder";
        return "Other";
    }

    private String extractExePath(String line) {
        line = line.replace("\"", "").trim().toLowerCase();

        // Resolve .lnk shortcuts
        if (line.contains(".lnk")) {
            int idx = line.indexOf(".lnk");
            int start = line.lastIndexOf(":", idx);
            if (start != -1) {
                String shortcut = line.substring(start).trim();
                if (new File(shortcut).exists()) {
                    return resolveShortcut(shortcut);
                }
            }
        }

        // Resolve service PathName
        if (line.startsWith("running") || line.startsWith("stopped")) {
            String[] tokens = line.split("\\s+");
            if (tokens.length >= 2) {
                return resolveServicePath(tokens[1]);
            }
        }

        // Extract .exe path
        int exeIdx = line.indexOf(".exe");
        if (exeIdx != -1) {
            int start = line.lastIndexOf("c:", exeIdx);
            if (start == -1) start = line.lastIndexOf("\\", exeIdx);
            if (start != -1 && exeIdx + 4 <= line.length()) {
                String path = line.substring(start, exeIdx + 4).trim();
                if (new File(path).exists()) return path;
            }
        }

        return "";
    }

    private String extractAppName(String exePath, String detailLine) {
        if (exePath != null && !exePath.isEmpty()) {
            File f = new File(exePath);
            return f.getName();
        }

        if (detailLine.toLowerCase().startsWith("running") || detailLine.toLowerCase().startsWith("stopped")) {
            String[] tokens = detailLine.trim().split("\\s+");
            if (tokens.length >= 2) return tokens[1];
        }

        if (detailLine.contains("REG_SZ")) {
            String[] parts = detailLine.split("REG_SZ");
            if (parts.length >= 1) {
                String candidate = parts[0].trim();
                if (candidate.length() < 30 && candidate.length() > 2) return candidate;
            }
        }

        String[] keywords = {"powershell", "cmd", "wscript", "regsvr32", "mshta", "python", "java"};
        for (String keyword : keywords) {
            if (detailLine.toLowerCase().contains(keyword)) return keyword + ".exe";
        }

        if (detailLine.toLowerCase().contains(".lnk")) {
            int idx = detailLine.toLowerCase().indexOf(".lnk");
            int start = detailLine.lastIndexOf("\\", idx);
            if (start != -1) return detailLine.substring(start + 1, idx + 4);
        }

        String[] words = detailLine.split("\\s+");
        String best = "";
        int maxUpper = 0;
        for (String word : words) {
            int count = 0;
            for (char c : word.toCharArray()) {
                if (Character.isUpperCase(c)) count++;
            }
            if (count > maxUpper && word.length() <= 25) {
                best = word;
                maxUpper = count;
            }
        }
        return best;
    }

    private String resolveShortcut(String shortcutPath) {
        try {
            String powershellCmd = "powershell -Command \"$s=(New-Object -ComObject WScript.Shell).CreateShortcut('" + shortcutPath + "');$s.TargetPath\"";
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", "$s=(New-Object -ComObject WScript.Shell).CreateShortcut('" + shortcutPath + "');$s.TargetPath");
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF-8"));
            String target = reader.readLine();
            if (target != null && new File(target).exists()) return target.trim();
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    private String resolveServicePath(String serviceName) {
        try {
            String[] cmd = {
                "powershell",
                "-Command",
                String.format("(Get-WmiObject -Class Win32_Service -Filter \\\"Name='%s'\\\").PathName", serviceName)
            };
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF-8"));
            String path = reader.readLine();
            if (path != null && path.toLowerCase().contains(".exe")) {
                path = path.replace("\"", "").trim();
                if (new File(path).exists()) return path;
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    private String classifyTrust(String path, String signature, String line) {
        String lc = line.toLowerCase();
        boolean isScripted = lc.contains("powershell") || lc.contains("cmd /c") || lc.contains("wscript") || lc.contains("regsvr32") || lc.contains("mshta");
        boolean isBadPath = lc.contains("appdata") || lc.contains("temp") || lc.contains("public");

        if ((signature == null || signature.contains("NotSigned") || signature.contains("Unknown")) && (isBadPath || isScripted)) {
            return "Danger";
        } else if (signature != null && (signature.contains("Valid") || signature.contains("Trusted"))) {
            return "Trusted";
        } else {
            return "Suspicious";
        }
    }
}
