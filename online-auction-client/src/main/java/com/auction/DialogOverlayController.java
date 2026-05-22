package com.auction;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class DialogOverlayController {

    @FXML private StackPane overlayRoot;
    @FXML private Group popupContainer;

    public void setContent(Node content, Runnable onClose) {
        popupContainer.getChildren().add(content);
        
        overlayRoot.setOnMouseClicked(e -> {
            if (e.getTarget() == overlayRoot && onClose != null) {
                onClose.run();
            }
        });
    }
}