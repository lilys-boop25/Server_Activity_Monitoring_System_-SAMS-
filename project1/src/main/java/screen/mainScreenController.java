package screen;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
// import screen.processController;
import java.io.IOException;

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

    private boolean menuExpanded = true; // Menu mở rộng mặc định
    private String currentTab = "process"; // Tab hiện tại
    private processController ProcessController; // Reference to process controller for search

    /**
     * Khởi tạo controller - được gọi tự động khi FXML được load
     */
    public void initialize() {
        // Thiết lập trạng thái ban đầu
        setupInitialState();
        
        // Load tab Process làm mặc định
        loadProcessTab();
        
        // Highlight process button là selected
        highlightSelectedButton(processbtn);
    }

    /**
     * Thiết lập trạng thái ban đầu của UI
     */
    private void setupInitialState() {
        // Ẩn searchbox ban đầu
        searchbox.setVisible(false);
        
        // Set placeholder text cho searchbox
        searchbox.setPromptText("Type a name, publisher, or PID to search");
        
        // Clear searchbox khi focus bị mất và không có text
        searchbox.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && searchbox.getText().trim().isEmpty()) {
                searchbox.clear();
            }
        });
    }

    /**
     * Load tab Process và thiết lập controller
     */
    private void loadProcessTab() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/screen/fxml/process-view.fxml"));
            Node content = loader.load();
            
            // Lưu reference đến ProcessController để có thể search
            ProcessController = loader.getController();
            
            // Clear và add content mới
            paneforbtn.getChildren().clear();
            paneforbtn.getChildren().add(content);
            
            // Anchor content to fill the pane
            AnchorPane.setTopAnchor(content, 0.0);
            AnchorPane.setBottomAnchor(content, 0.0);
            AnchorPane.setLeftAnchor(content, 0.0);
            AnchorPane.setRightAnchor(content, 0.0);
            
            currentTab = "process";
            searchbox.setVisible(true);
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading process-view.fxml: " + e.getMessage());
        }
    }

    /**
     * Thiết lập nội dung trung tâm với file FXML được chỉ định
     */
    private void setCenter(String fxmlFile, String tabName) {
        try {
            Node content;
            
            if ("process-view.fxml".equals(fxmlFile)) {
                // Load Process tab with controller reference
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/screen/fxml/" + fxmlFile));
                content = loader.load();
                ProcessController = loader.getController();
            } else {
                // Load other tabs normally
                content = FXMLLoader.load(getClass().getResource("/screen/fxml/" + fxmlFile));
                ProcessController = null; // Clear reference when not in process tab
            }
            
            paneforbtn.getChildren().clear();
            paneforbtn.getChildren().add(content);
            
            // Anchor content to fill the pane
            AnchorPane.setTopAnchor(content, 0.0);
            AnchorPane.setBottomAnchor(content, 0.0);
            AnchorPane.setLeftAnchor(content, 0.0);
            AnchorPane.setRightAnchor(content, 0.0);
            
            currentTab = tabName;
            
            // Hiển thị/ẩn searchbox dựa trên tab
            searchbox.setVisible("process".equals(tabName));
            
            // Clear search text khi chuyển tab
            if (!"process".equals(tabName)) {
                searchbox.clear();
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading " + fxmlFile + ": " + e.getMessage());
        }
    }

    /**
     * Highlight button được chọn và unhighlight các button khác
     */
    private void highlightSelectedButton(Label selectedButton) {
        // Reset tất cả buttons về trạng thái normal
        resetButtonHighlight(processbtn);
        resetButtonHighlight(performancebtn);
        resetButtonHighlight(servicesbtn);
        resetButtonHighlight(startupbtn);
        resetButtonHighlight(usersbtn);
        resetButtonHighlight(detailsbtn);
        
        // Highlight button được chọn
        selectedButton.setStyle("-fx-background-color: #B3B1B2; -fx-background-radius: 3px;");
    }

    /**
     * Reset highlight của button
     */
    private void resetButtonHighlight(Label button) {
        button.setStyle("-fx-background-color: transparent;");
    }

    @FXML
    void expandMenu(MouseEvent event) {
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), menuBox);
        if (menuExpanded) {
            slide.setToX(-menuBox.getWidth() + 50); // Chỉ hiện một phần
            menuExpanded = false;
        } else {
            slide.setToX(0); // Hiển thị đầy đủ
            menuExpanded = true;
        }
        slide.play();
    }

    @FXML
    void searchItem() {
        String searchText = searchbox.getText().trim();
        
        // Chỉ search khi đang ở tab process và có ProcessController
        if ("process".equals(currentTab) && ProcessController != null) {
            ProcessController.filterProcesses(searchText);
        }
    }

    @FXML
    void movetoPer(MouseEvent event) {
        setCenter("performance-view.fxml", "performance");
        highlightSelectedButton(performancebtn);
    }

    @FXML
    void movetoPro(MouseEvent event) {
        setCenter("process-view.fxml", "process");
        highlightSelectedButton(processbtn);
    }

    @FXML
    void movetoServices(MouseEvent event) {
        setCenter("services-view.fxml", "services");
        highlightSelectedButton(servicesbtn);
    }

    @FXML
    void movetoStart(MouseEvent event) {
        setCenter("startup-view.fxml", "startup");
        highlightSelectedButton(startupbtn);
    }

    @FXML
    void movetoUser(MouseEvent event) {
        setCenter("users-view.fxml", "users");
        highlightSelectedButton(usersbtn);
    }

    @FXML
    void movetoDet(MouseEvent event) {
        setCenter("details-view.fxml", "details");
        highlightSelectedButton(detailsbtn);
    }

    /**
     * Getter cho current tab - có thể hữu ích cho testing hoặc debugging
     */
    public String getCurrentTab() {
        return currentTab;
    }

    /**
     * Kiểm tra xem searchbox có đang hiển thị không
     */
    public boolean isSearchBoxVisible() {
        return searchbox.isVisible();
    }

    /**
     * Programmatically set search text - hữu ích cho testing
     */
    public void setSearchText(String text) {
        searchbox.setText(text);
        searchItem(); // Trigger search
    }

    /**
     * Get current search text
     */
    public String getSearchText() {
        return searchbox.getText();
    }

    /**
     * Cleanup resources when controller is destroyed
     */
    public void cleanup() {
        if (ProcessController != null) {
            ProcessController.cleanup();
        }
    }
}