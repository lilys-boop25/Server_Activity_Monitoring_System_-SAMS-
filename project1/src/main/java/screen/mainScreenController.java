package screen;

import javafx.animation.TranslateTransition;
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

import java.io.IOException;

public class mainScreenController {

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
    void searchItem(InputMethodEvent event) {

    }
    @FXML
    private boolean menuExpanded = false;

    private void setCenter(String fxmlFile) {
        try {
            Node content = FXMLLoader.load(getClass().getResource("/screen/fxml/" + fxmlFile));
            paneforbtn.getChildren().clear();
            paneforbtn.getChildren().add(content);
        } catch (IOException e) {
            e.printStackTrace();
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
    void searchItem() {
        String keyword = searchbox.getText();
        System.out.println("Searching for: " + keyword);
    }
}
