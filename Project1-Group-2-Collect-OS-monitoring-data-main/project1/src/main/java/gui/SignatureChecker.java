package gui;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SignatureChecker {

    public static String getSignatureStatus(String exePath) {
        try {
            String command = String.format(
                "powershell -Command \"(Get-AuthenticodeSignature '%s').Status\"", exePath.replace("\\", "\\\\")
            );

            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) return line;
            }
        } catch (Exception e) {
            return "Error";
        }
        return "Unknown";
    }
}
