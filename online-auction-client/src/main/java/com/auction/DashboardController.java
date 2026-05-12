package com.auction;

import com.auction.model.Item;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @FXML private Button btnLogout;
    @FXML private javafx.scene.layout.FlowPane itemGrid;

    @FXML private Button btnAddItem;

    @FXML
    public void initialize() {
        loadDataFromServer();

        if (Session.role == null || !Session.role.equalsIgnoreCase("seller")) {
            btnAddItem.setVisible(false);
        }
    }

    @FXML
    private void handleAddItem() {
        try {
            Parent currentRoot = btnAddItem.getScene().getRoot();
            
            StackPane rootPane = (StackPane) currentRoot;
            Node mainContent = rootPane.getChildren().get(0);

            if (rootPane.lookup("#dark-overlay") != null) {
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("add_item.fxml"));
            Parent addItemGroup = loader.load();
            AddItemController addItemCtrl = loader.getController();

            GaussianBlur blur = new GaussianBlur(15);
            mainContent.setEffect(blur);

            Region darkOverlay = new Region();
            darkOverlay.setId("dark-overlay");
            darkOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");
            darkOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            darkOverlay.setOnMouseClicked(e -> addItemCtrl.closePopup());

            if (addItemGroup instanceof Region) {
                ((Region) addItemGroup).setMaxSize(
                        Region.USE_PREF_SIZE, 
                        Region.USE_PREF_SIZE
                );
            }

            rootPane.getChildren().addAll(darkOverlay, addItemGroup);

            addItemCtrl.setOnCloseCallback(() -> {
                mainContent.setEffect(null);
                rootPane.getChildren().removeAll(darkOverlay, addItemGroup);
                loadDataFromServer();
            });

        } catch (Exception e) {
            logger.error("Failed to open Add Item dialog: {}", e.getMessage(), e);
        }
    }

    private void openBidRoom(Item item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("bid_room.fxml"));
            Parent root = loader.load();

            BidRoomController bidRoomCtrl = loader.getController();

            int myUserId = Session.userId;

            bidRoomCtrl.setAuctionData(
                    item.getId(),
                    item.getName(),
                    item.getCurrentPrice(),
                    myUserId
            );

            Stage stage = (Stage) itemGrid.getScene().getWindow();
            itemGrid.getScene().setRoot(root);
            stage.setTitle("Phòng Đấu Giá: " + item.getName());

        } catch (Exception e) {
            logger.error("Failed to open bid room: {}", e.getMessage(), e);
        }
    }

    private VBox createItemCard(Item item) {
        VBox card = new VBox();
        card.setSpacing(10);
        card.getStyleClass().add("item-card");
        card.setPadding(new Insets(15));
        card.setPrefWidth(280);

        HBox badgeBox = new HBox();
        Label badge = new Label("LIVE");
        badge.getStyleClass().add("badge-live");
        badgeBox.getChildren().add(badge);

        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(150);
        imageContainer.setStyle("-fx-background-color: #2D3748; -fx-background-radius: 10;");

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            try {
                ImageView imageView = new ImageView(new Image(item.getImageUrl(), true));
                imageView.setFitHeight(140);
                imageView.setFitWidth(240);
                imageView.setPreserveRatio(true);
                imageContainer.getChildren().add(imageView);
            } catch (Exception e) {
                logger.warn("Could not load image: {}", item.getImageUrl());
            }
        }

        Label subtitle = new Label("LÔ-" + item.getId() + " • " + item.getItemType());
        subtitle.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");

        Label title = new Label(item.getName());
        title.getStyleClass().add("card-title");
        title.setPrefHeight(50);
        title.setWrapText(true);

        HBox priceHBox = new HBox();
        priceHBox.setAlignment(Pos.BOTTOM_LEFT);

        VBox priceVBox = new VBox();
        Label priceLabel = new Label("GIÁ HIỆN TẠI");
        priceLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");

        Label priceValue = new Label("$" + item.getCurrentPrice());
        priceValue.getStyleClass().add("card-price");

        priceVBox.getChildren().addAll(priceLabel, priceValue);
        priceHBox.getChildren().add(priceVBox);

        Button btnEnter = new Button("Vào Phòng");
        btnEnter.setMaxWidth(Double.MAX_VALUE);
        btnEnter.setPrefHeight(40);
        btnEnter.getStyleClass().add("btn-orange");

        VBox.setMargin(btnEnter, new Insets(10, 0, 0, 0));
        btnEnter.setOnAction(e -> openBidRoom(item));

        card.getChildren().addAll(
                badgeBox,
                imageContainer,
                subtitle,
                title,
                priceHBox,
                btnEnter
        );

        return card;
    }

    private void loadDataFromServer() {
        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            JsonObject request = new JsonObject();
            request.addProperty("action", "GET_ALL_ITEMS");
            out.println(request.toString());

            String responseStr = in.readLine();

            logger.info("Dashboard received response from server: {}", responseStr);

            if (responseStr != null) {

                JsonObject response = JsonParser.parseString(responseStr).getAsJsonObject();

                if (response.get("status").getAsString().equals("SUCCESS")) {
                    JsonArray dataArray = response.getAsJsonArray("data");
                    List<Item> items = new ArrayList<>();

                    Map<String, String> typeMap = new LinkedHashMap<>();
                    typeMap.put("warranty", "ELECTRONICS");
                    typeMap.put("engineType", "VEHICLE");
                    typeMap.put("author", "ART");

                    for (int i = 0; i < dataArray.size(); i++) {
                        JsonObject obj = dataArray.get(i).getAsJsonObject();

                        String name = obj.has("name") ? obj.get("name").getAsString() : "Chưa có tên";
                        double startPrice = obj.has("startingPrice") ? obj.get("startingPrice").getAsDouble() : 0;
                        String endTime = obj.has("endTime") ? obj.get("endTime").getAsString() : "";
                        int sellerId = obj.has("sellerId") ? obj.get("sellerId").getAsInt() : 0;

                        logger.info("Raw Item JSON: {}", obj.toString());

                        String type = "ART";
                        String extraInfo = "N/A";
                        
                        for (Map.Entry<String, String> entry : typeMap.entrySet()) {
                            if (obj.has(entry.getKey())) {
                                type = entry.getValue();
                                extraInfo = obj.get(entry.getKey()).getAsString();
                                break;
                            }
                        }

                        Item item = com.auction.factory.ItemFactory.createItem(
                                type, name, startPrice, endTime, sellerId, extraInfo
                        );

                        int id = obj.get("id").getAsInt();
                        item.setId(id);
                        if (obj.has("currentPrice")) {
                            item.setCurrentPrice(obj.get("currentPrice").getAsDouble());
                        }
                        if (obj.has("imageUrl")) {
                            item.setImageUrl(obj.get("imageUrl").getAsString());
                        }

                        items.add(item);
                    }

                    Platform.runLater(() -> {
                        itemGrid.getChildren().clear();
                        for (Item item : items) {
                            itemGrid.getChildren().add(createItemCard(item));
                        }
                    });
                }
            }

        } catch (Exception e) {
            logger.error("Network error while loading items: {}", e.getMessage(), e);
        }
    }

    @FXML
    public void handleLogout() {
        try {
            Stage stage = (Stage) btnLogout.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Parent root = loader.load();
            btnLogout.getScene().setRoot(root);
            stage.setTitle("Hệ Thống Đấu Giá Trực Tuyến");
        } catch (IOException e) {
            logger.error("Failed to handle logout: {}", e.getMessage(), e);
        }
    }
}
