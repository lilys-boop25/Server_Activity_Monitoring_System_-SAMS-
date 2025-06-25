package gui;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class StartupScanner {

    public static List<String> collectStartupEntries() {
        List<String> results = new ArrayList<>();

        runCommand("reg query HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", results, "REGISTRY: HKCU\\Run");
        runCommand("reg query HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", results, "REGISTRY: HKLM\\Run");
        runCommand("powershell \"Get-ScheduledTask | Where { $_.Triggers -match 'AtLogon|AtStartup' }\"", results, "TASK SCHEDULER");
        runCommand("powershell \"Get-Service | Where { $_.StartType -eq 'Automatic' }\"", results, "SERVICES");
        runCommand("reg query \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Windows\" /v AppInit_DLLs", results, "AppInit_DLLs");
        runCommand("reg query \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Winlogon\" /v Shell", results, "Winlogon Shell");
        runCommand("reg query \"HKLM\\Software\\Microsoft\\Active Setup\\Installed Components\"", results, "Active Setup");
        runCommand("powershell \"Get-WmiObject -Namespace root\\subscription -Class __EventConsumer\"", results, "WMI Consumers");

        // Startup folders
        results.add("== STARTUP FOLDERS ==");
        listStartupFolder(results, System.getenv("APPDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup");
        listStartupFolder(results, System.getenv("ProgramData") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup");

        return results;
    }

    private static void runCommand(String cmd, List<String> output, String title) {
        try {
            output.add(">> " + title);
            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
            }
        } catch (Exception e) {
            output.add("ERROR running " + title + ": " + e.getMessage());
        }
    }

    private static void listStartupFolder(List<String> output, String path) {
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                output.add("Startup File: " + f.getAbsolutePath());
            }
        } else {
            output.add("Missing Folder: " + path);
        }
    }
}
