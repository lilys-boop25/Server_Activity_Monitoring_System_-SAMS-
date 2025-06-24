package screen;

import javafx.animation.TranslateTransition;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import oshi.SystemInfo;

import java.io.IOException;

import javax.swing.SwingUtilities;

import gui.ProcessPanel;

public class mainScreenController {
    
    @FXML
    private Label detailsbtn;

    @FXML
    private VBox menuBox;

    @FXML
    private ImageView menubtn;

    @FXML
    private AnchorPane paneforbtn;

    @FXML
    private Label performancebtn;

    @FXML
    private Label processbtn;

    @FXML
    private TextField searchbox;

    @FXML
    private Label servicesbtn;

    @FXML
    private Label startupbtn;

    @FXML
    private Label usersbtn;

    @FXML
    private boolean menuExpanded = false;

    private ProcessPanel processPanel;

    private void setCenter(String fxmlFile) {
        try {
            if ("process-view.fxml".equals(fxmlFile)) {
                // Load ProcessPanel as a Swing component
                SystemInfo si = new SystemInfo();
                processPanel = new ProcessPanel(si);
                JFXPanel fxPanel = new JFXPanel();
                fxPanel.setScene(new javafx.scene.Scene(new javafx.scene.layout.StackPane()));
                SwingUtilities.invokeLater(() -> {
                    fxPanel.add(processPanel);
                });
                paneforbtn.getChildren().clear();
                paneforbtn.getChildren().add(fxPanel);
                searchbox.setVisible(true); // Hiển thị searchbox khi vào Processes
            } else {
                Node content = FXMLLoader.load(getClass().getResource("/screen/fxml/" + fxmlFile));
                paneforbtn.getChildren().clear();
                paneforbtn.getChildren().add(content);
                searchbox.setVisible(false); // Ẩn searchbox cho các tab khác
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void expandMenu(MouseEvent event) {
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), menuBox);
        if (menuExpanded) {
            slide.setToX(-menuBox.getWidth()); // Ẩn
            menuExpanded = false;
        } else {
            slide.setToX(0); // Hiện
            menuExpanded = true;
        }
        slide.play();
    }


    @FXML
    void searchItem() {
        searchProcess();
    }

    private void searchProcess() {
        String keyword = searchbox.getText().toLowerCase();
        if (keyword.isEmpty()) {
            setCenter("process-view.fxml"); // Reset to full process view
        } else {
            try {
                // Load process view and filter content (assuming process-view.fxml supports dynamic content)
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/screen/fxml/process-view.fxml"));
                Node content = loader.load();
                // Here you would typically filter the TableView or ListView in process-view.fxml
                // This is a placeholder for filtering logic
                System.out.println("Filtering processes for: " + keyword);
                paneforbtn.getChildren().clear();
                paneforbtn.getChildren().add(content);
                // Example: Filter logic would go here to update the UI with matching processes
                processData.stream()
                    .filter(process -> process.toLowerCase().contains(keyword))
                    .forEach(System.out::println); // Replace with UI update logic
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void movetoPer(MouseEvent event) {
        setCenter("performance-view.fxml");
    }

    @FXML
    void movetoPro(MouseEvent event) {
        setCenter("process-view.fxml");
    }

    @FXML
    void movetoServices(MouseEvent event) {
        setCenter("services-view.fxml");
    }

    @FXML
    void movetoStart(MouseEvent event) {
        setCenter("startup-view.fxml");
    }

    @FXML
    void movetoUser(MouseEvent event) {
        setCenter("users-view.fxml");
    }

    @FXML
    void movetoDet(MouseEvent event) {
        setCenter("details-view.fxml");
    }

    public void initialize() {
        searchbox.setVisible(false);
    }
}
