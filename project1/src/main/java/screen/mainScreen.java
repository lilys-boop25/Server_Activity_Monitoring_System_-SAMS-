package screen;

import java.io.IOException;

import javax.swing.JFrame;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class mainScreen extends JFrame {

    public mainScreen() {
        super();

        JFXPanel fxPanel = new JFXPanel(); // tạo bridge giữa Swing và JavaFX
        this.add(fxPanel);

        this.setTitle("Server Activity Monitoring System");
        this.setSize(1100, 700); // thiết lập kích thước ban đầu tương tự Task Manager
        this.setVisible(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // dừng app khi tắt

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
                Parent root = loader.load();
                fxPanel.setScene(new Scene(root));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}

